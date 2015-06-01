


package stratos.game.actors;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.util.*;



//  TODO:  Allow this to store relations with arbitrary saveables?  And then
//  average relations with their properties.


public class ActorRelations {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  private static boolean
    verbose      = false,
    extraVerbose = false;
  
  final public static float
    INIT_NOVELTY     = 1.0f,
    MAX_NOVELTY      = 2.0f,
    NOVELTY_INTERVAL = Stage.STANDARD_DAY_LENGTH  * 2,
    FORGET_INTERVAL  = Stage.STANDARD_DAY_LENGTH  * 5,
    BASE_NUM_FRIENDS = 5 ,
    GENERALISE_RATIO = 4 ,
    MAX_RELATIONS    = 10;
  
  final static int
    DEFAULT_MAX_MEMORY = 10,
    DEFAULT_MAX_ASSOC  = 100,
    UPDATE_PERIOD      = Stage.STANDARD_HOUR_LENGTH,
    OBSERVE_PERIOD     = Stage.STANDARD_HOUR_LENGTH;
  
  
  final protected Actor actor;
  final Table <Accountable, Relation> relations = new Table();
  
  
  public ActorRelations(Actor actor) {
    this.actor = actor;
  }
  
  
  public void loadState(Session s) throws Exception {
    for (int n = s.loadInt(); n-- > 0;) {
      final Relation r = Relation.loadFrom(s);
      relations.put((Accountable) r.subject, r);
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(relations.size());
    for (Relation r : relations.values()) Relation.saveTo(s, r);
  }
  
  
  
  /**  Update and decay methods-
    */
  //  TODO:  With the 'surprise' factor implemented here, you might not need
  //  the range-damping mechanisms within the Relation class itself?
  
  protected void updateFromObservations() {
    final boolean report = extraVerbose && I.talkAbout == actor;
    if (report) I.say("\n"+actor+" updating relations based on observation.");
    
    for (Target t : actor.senses.awareOf()) if (t instanceof Actor) {
      final Actor  other   = (Actor) t;
      final Target affects = other.planFocus(null, true);
      //
      //  TODO:  Have 'spite' factor into this- e.g, so that helping an enemy
      //  counts as harm?
      final float
        harm   = other.harmIntended(affects),
        cares  = Nums.clamp(valueFor(affects), 0, 1),
        impact = Nums.abs(harm * cares);
      if (report) {
        I.say("\n  Observed "+other+" doing "+other.currentAction());
        I.say("    Affected:   "+affects);
        I.say("    Harm done:  "+harm   );
        I.say("    Cares?      "+cares  );
        I.say("    Impact:     "+impact );
      }
      //
      //  We gradually decrease the weirdness of inoffensive subjects.
      if (impact == 0 || (impact <= 0.5f && ! hasRelation(other))) {
        incRelation(other.base(), 0, 0, -0.5f / OBSERVE_PERIOD);
        if (report) I.say("  Meh.  Big deal.");
        continue;
      }
      final float
        weight    = 1f / OBSERVE_PERIOD,
        increment = 0 - harm * cares,
        beforeVal = valueFor(other),
        surprise  = Nums.clamp(Nums.abs(increment - beforeVal), 0, 1);
      //
      //  Otherwise, we modify our relationship with an individual based on
      //  observation of their recent behaviour:
      incRelation(other, increment, weight, surprise * weight);
      if (report) {
        I.say("\n  Actions speak louder...");
        I.say("    Relation before: "+beforeVal);
        I.say("    Weight assigned: "+weight   );
        I.say("    Surprise level:  "+surprise );
        I.say("    Increment:       "+increment);
        I.say("    Relation after:  "+valueFor(other));
      }
    }
  }
  
  
  public void updateValues(int numUpdates) {
    //
    //  For the moment, I'm disabling these for animals, artilects, etc.
    if (! actor.species().sapient()) return;
    final boolean report = I.talkAbout == actor && verbose;
    updateFromObservations();
    if (numUpdates % UPDATE_PERIOD != 0) return;
    if (report) I.say("\nDecaying and culling relations for "+actor);
    //
    //  Firstly, sort relations in order of importance (based on the strength
    //  of the relationship, good or bad, and freshness in memory)-
    final List <Relation> sorting = new List <Relation> () {
      protected float queuePriority(Relation r) {
        return r.novelty() - Nums.abs(r.value());
      }
    };
    for (Relation r : relations.values()) sorting.add(r);
    sorting.queueSort();
    //
    //  Then incrementally update each relation, and determine which are no
    //  longer important enough to remember (only personal relations are
    //  considered 'disposable' for this purpose:
    int count = 0; for (Relation r : sorting) {
      final boolean
        okay     = updateRelation(r, UPDATE_PERIOD),
        excess   = ++count > MAX_RELATIONS,
        personal = r.subject instanceof Actor;
      
      if (report) {
        I.say("  Have updated relation with "+r.subject);
        I.say("    ("+count+"/"+MAX_RELATIONS+", okay: "+okay+")");
        I.say("    Value/novelty: "+r.value()+"/"+r.novelty());
      }
      if (personal) {
        //
        //  We generalise about the subject's base of origin based on relations
        //  with known members.  And regardless of like or dislike, we
        //  decrement the 'strangeness' of the other's base.
        final float valueWeight = 1f / GENERALISE_RATIO;
        final float novelWeight = UPDATE_PERIOD * -1f / NOVELTY_INTERVAL;
        incRelation(r.subject.base(), r.value(), valueWeight, novelWeight);
        //
        //  And finally, discard if surplus to requirements.
        if (excess || ! okay) {
          relations.remove(r.subject);
          if (report) I.say("    EXPIRED");
        }
      }
    }
  }
  
  
  protected boolean updateRelation(
    Relation r, int period
  ) {
    float value = r.value(), novelty = r.novelty();
    final float magnitude = Nums.clamp(Nums.abs(value), 0, 1);
    
    //  If the memory is relatively fresh, just decay novelty by a fixed rate
    //  over time, and leave the value itself alone.
    float noveltyInc = period * 1f;
    if (novelty <= INIT_NOVELTY) {
      noveltyInc /= NOVELTY_INTERVAL;
      r.incNovelty(noveltyInc);
    }
    
    //  More distant memories decay more slowly, based on the strength of the
    //  relation itself, but allow the relation value to decay in the process.
    else {
      noveltyInc *= (1 - magnitude) * (1 - magnitude);
      noveltyInc *= 1f / FORGET_INTERVAL;
      r.incValue(0, noveltyInc);
      r.incNovelty(noveltyInc);
    }
    
    //  If the memory has expired entirely, flag it for removal:
    if (r.novelty() - magnitude >= MAX_NOVELTY) return false;
    return true;
  }
  
  
  
  /**  Handling relationships and attitudes-
    */
  public float valueFor(Object object) {
    if (! (object instanceof Accountable)) return 0;
    final Accountable other = (Accountable) object;
    
    //  If we have an existing relationship, then return its value (this is
    //  always assumed to start at 1.0 for self.)
    final Relation r = relations.get(other);
    if (r != null) return r.value();
    if (other == actor) return 1;
    
    //  Otherwise, we calculate what the starting relationship with this entity
    //  should be.  We base this off a weighted average depending on home/work
    //  identity, community spirit, and relations with the parent base:
    final float baseVal = actor.base().relations.relationWith(other.base());
    if (other == other.base()) return baseVal;
    
    if (other.base() == actor.base() || baseVal > 0) {
      float relVal = 0;
      if (other == actor.mind.home) relVal += 1.0f;
      if (other == actor.mind.work) relVal += 0.5f;
      if (other.base() == actor.base()) {
        relVal += actor.base().relations.communitySpirit() / 2;
      }
      return (baseVal + Nums.clamp(relVal, 0, 2)) / 3f;
    }
    else return baseVal;
  }
  
  
  public float noveltyFor(Object object) {
    if (! (object instanceof Accountable)) return 0;
    final Accountable other = (Accountable) object;

    final Relation r = relations.get(other);
    if (r == null) {
      final Base belongs = other.base();
      final float baseNov = belongs == other ? 0 : noveltyFor(belongs);
      return baseNov + INIT_NOVELTY;
    }
    return r.novelty();
  }
  
  
  public void incRelation(
    Accountable other, float toLevel, float weight, float novelty
  ) {
    final boolean report = I.talkAbout == actor && verbose && extraVerbose;
    if (report) {
      I.say("\nIncrementing relation with "+other);
      I.say("  To level: "+toLevel);
      I.say("  Weight:   "+weight );
      I.say("  Novelty:  "+novelty);
    }
    
    Relation r = relations.get(other);
    if (r == null) {
      final float baseVal = valueFor  (other);
      final float baseNov = noveltyFor(other);
      r = setRelation(other, baseVal, baseNov);
    }
    r.incValue(toLevel, weight);
    r.incNovelty(novelty);
  }

  
  
  public Batch <Relation> relations() {
    final Batch <Relation> all = new Batch <Relation> ();
    for (Relation r : relations.values()) all.add(r);
    return all;
  }
  
  
  public Relation relationWith(Object other) {
    return relations.get(other);
  }
  
  
  public Relation setRelation(Accountable other, float value, float novelty) {
    Relation r = relations.get(other);
    if (r == null) {
      r = new Relation(actor, other, value, novelty);
      relations.put(other, r);
    }
    else r.setValue(value, novelty);
    return r;
  }
  
  
  public boolean hasRelation(Object other) {
    return relations.get(other) != null;
  }
  
  
  public boolean likes(Object other) {
    return valueFor(other) > 0;
  }
  
  
  public boolean dislikes(Object other) {
    return valueFor(other) < 0;
  }
}



