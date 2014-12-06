


package stratos.game.actors;
import org.apache.commons.math3.util.FastMath;

import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.util.*;



//  TODO:  Allow this to store relations with arbitrary saveables.


public class ActorRelations {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final public static float
    INIT_NOVELTY     = 2.0f,
    NOVELTY_INTERVAL = Stage.STANDARD_DAY_LENGTH  * 2,
    FORGET_INTERVAL  = Stage.STANDARD_DAY_LENGTH  * 5,
    BASE_NUM_FRIENDS = 5 ,
    MAX_RELATIONS    = 10;
  
  final static int
    DEFAULT_MAX_MEMORY = 10,
    DEFAULT_MAX_ASSOC  = 100;
  
  private static boolean verbose = false;
  
  
  final protected Actor actor;
  final Table <Accountable, Relation> relations = new Table();
  
  
  public ActorRelations(Actor actor) {
    this.actor = actor;
  }
  
  
  protected void loadState(Session s) throws Exception {
    for (int n = s.loadInt(); n-- > 0;) {
      final Relation r = Relation.loadFrom(s);
      relations.put((Accountable) r.subject, r);
    }
  }
  
  
  protected void saveState(Session s) throws Exception {
    s.saveInt(relations.size());
    for (Relation r : relations.values()) Relation.saveTo(s, r);
  }
  
  
  
  /**  Update and decay methods-
    */
  public void updateValues(int numUpdates) {
    final boolean report = verbose && I.talkAbout == actor;
    if (numUpdates % 10 != 0) return;
    if (report) I.say("Updating relations, num. updates: "+numUpdates);

    for (Relation r : relations.values()) updateRelation(r, 10);
    
    //  Get a running total of relations with all actors belonging to a given
    //  base, so you can average that for the base itself.
    final List <Base> bases = actor.world().bases();
    final float
      baseSum[]   = new float[bases.size()],
      baseCount[] = new float[bases.size()];
    
    //  Sort relations in order of importance while doing so.
    final List <Relation> sorting = new List <Relation> () {
      protected float queuePriority(Relation r) {
        return Math.abs(r.value()) - r.novelty();
      }
    };
    for (Relation r : relations.values()) {
      if (! (r.subject instanceof Actor)) continue;
      sorting.add(r);
      final int BID = bases.indexOf(r.subject.base());
      if (BID != -1) {
        if (report) I.say("  Relation is: "+r+" ("+r.value()+")");
        baseSum[BID] += r.value();
        baseCount[BID] += 1;
      }
    }
    sorting.queueSort();
    
    //  Cull the least important relationships, and set up relations with the
    //  bases.
    while (sorting.size() > MAX_RELATIONS) {
      final Relation r = sorting.removeLast();
      relations.remove(r.subject);
    }
    int BID = 0; for (Base base : bases) {
      final float
        sum = baseSum[BID], count = baseCount[BID],
        relation = count == 0 ? 0 : (sum / count);
      if (report) {
        I.say("Relation with "+base+" is "+relation);
        I.say("Base sum/base count: "+sum+"/"+count);
      }
      incRelation(base, relation, 10f / Stage.STANDARD_DAY_LENGTH, 0);
      BID++;
    }
  }
  
  
  protected void updateRelation(Relation r, int period) {
    
    float value = r.value(), novelty = r.novelty();
    final float magnitude = 1 - FastMath.abs(value);
    
    float noveltyInc = period * 1f;
    if (novelty <= 1) {
      noveltyInc /= NOVELTY_INTERVAL;
      r.incNovelty(noveltyInc);
    }
    else {
      noveltyInc *= (magnitude * magnitude);
      noveltyInc /= FORGET_INTERVAL;
      r.incValue(0, noveltyInc);
      r.incNovelty(noveltyInc);
    }
    
    if (r.novelty() - magnitude >= INIT_NOVELTY) {
      relations.remove(r.subject);
    }
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
    float relVal = 0;
    if (other == actor.mind.home) relVal += 1.0f;
    if (other == actor.mind.work) relVal += 0.5f;
    if (other.base() == actor.base()) {
      relVal += actor.base().relations.communitySpirit() / 2;
    }
    return (baseVal + Visit.clamp(relVal, 0, 2)) / 3f;
  }
  
  
  public float noveltyFor(Object other) {
    final Relation r = relations.get(other);
    if (r == null) return INIT_NOVELTY;
    return r.novelty();
  }
  
  
  public void incRelation(
    Accountable other, float toLevel, float weight, float novelty
  ) {
    Relation r = relations.get(other);
    if (r == null) {
      final float baseVal = valueFor(other);
      r = setRelation(other, baseVal, INIT_NOVELTY);
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




  /*
  public float valueFor(Base base) {
    final Base AB = actor.base();
    if (AB != null) {
      if (base == AB) return 1;
      if (base == null) return 0;
      return AB.relations.relationWith(base);
    }
    else return 0;
  }
  
  
  public float valueFor(Installation venue) {
    if (venue == null) return 0;
    //  TODO:  Cache this and modify slowly over time.
    
    final float baseVal = valueFor(venue.base());
    float relVal = 0;
    if (venue.base() == actor.base()) {
      relVal += actor.base().relations.communitySpirit() / 2;
    }
    if (venue == actor.mind.home) relVal += 1.0f;
    if (venue == actor.mind.work) relVal += 0.5f;
    
    return (baseVal + Visit.clamp(relVal, 0, 1)) / 2f;
  }
  
  
  public float valueFor(Actor other) {
    if (other == actor) return 1;
    final Relation r = relations.get(other);
    if (r == null) return valueFor(other.base()) / 2;
    if (r.subject == actor) return Visit.clamp(r.value() + 1, 0, 1);
    return r.value() + (valueFor(other.base()) / 4);
  }
  
  
  public float valueFor(Object other) {
    if (other instanceof Installation) {
      return valueFor((Installation) other);
    }
    if (other instanceof Actor) return valueFor((Actor) other);
    if (other instanceof Base ) return valueFor((Base ) other);
    return 0;
  }
  //*/


