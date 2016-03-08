/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.actors;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.game.verse.Faction;
import stratos.util.*;



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
  protected Actor master;
  final Table <Accountable, Relation> relations = new Table();
  
  
  public ActorRelations(Actor actor) {
    this.actor = actor;
  }
  
  
  public void loadState(Session s) throws Exception {
    master = (Actor) s.loadObject();
    for (int n = s.loadInt(); n-- > 0;) {
      final Relation r = Relation.loadFrom(s);
      relations.put((Accountable) r.subject, r);
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(master);
    s.saveInt(relations.size());
    for (Relation r : relations.values()) Relation.saveTo(s, r);
  }
  
  
  
  /**  Master/servant relations-
    */
  public void assignMaster(Actor master) {
    final Actor oldM = this.master;
    if (oldM == master) return;
    
    this.master = master;
    
    if (oldM != null) {
      final Relation r = relations.get(oldM);
      if (r != null) r.setType(Relation.TYPE_GENERIC);
      oldM.relations.toggleAsServant(actor, false);
    }
    
    if (master != null) {
      final Relation r = relations.get(master);
      if (r == null) setupRelation(master, 0, 0, Relation.TYPE_MASTER, false);
      else r.setType(Relation.TYPE_MASTER);
      master.relations.toggleAsServant(actor, true);
    }
  }
  
  
  protected void toggleAsServant(Actor servant, boolean is) {
    final Relation r = relations.get(servant);
    
    if (is) {
      if (r == null) setupRelation(servant, 0, 0, Relation.TYPE_SERVANT, false);
      else r.setType(Relation.TYPE_SERVANT);
    }
    else if (r != null) {
      r.setType(Relation.TYPE_GENERIC);
    }
  }
  
  
  public void clearMaster() {
    assignMaster(null);
  }
  
  
  public Actor master() {
    return master;
  }
  
  
  public Series <Actor> servants() {
    final Batch <Actor> matches = new Batch();
    for (Relation r : relations.values()) {
      if (r.type() != Relation.TYPE_SERVANT) continue;
      matches.add((Actor) r.subject);
    }
    return matches;
  }
  
  
  public void onWorldExit() {
    clearMaster();
    final Object with[] = relations.keySet().toArray();
    for (Object o : with) if (o instanceof Target) {
      final Target t = (Target) o;
      if (t.world() != null) relations.remove(t);
    }
  }
  
  
  
  /**  Update and decay methods-
    */
  //  TODO:  With the 'surprise' factor implemented here, you might not need
  //  the range-damping mechanisms within the Relation class itself?
  
  protected void updateFromObservations() {
    final boolean report = extraVerbose && I.talkAbout == actor;
    if (report) I.say("\n"+actor+" updating relations based on observation.");
    
    final Target awareOf[] = actor.senses.awareOf     ();
    final float threats [] = actor.senses.awareThreats();
    for (int i = awareOf.length; i-- > 0;) {
      final Target t = awareOf[i];
      if (! (t instanceof Actor)) continue;
      float
        dislike = 0 - valueFor(t),
        threat  = threats[i],
        diff    = threat - dislike;
      incRelation(t, 0 - diff, 0.5f / OBSERVE_PERIOD, 0);
    }
  }
  
  
  public void updateValues(int numUpdates) {
    //
    //  For the moment, I'm disabling these for animals, artilects, etc.
    ///if (! actor.species().sapient()) return;
    final boolean report = I.talkAbout == actor && verbose;
    updateFromObservations();
    if (numUpdates % UPDATE_PERIOD != 0) return;
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
    if (report) I.say("\nAssessing personal relations for "+actor);
    int personCount = 0;
    float   factionVals  [] = new float  [Base.MAX_BASES];
    int     factionCounts[] = new int    [Base.MAX_BASES];
    Faction factions     [] = new Faction[Base.MAX_BASES];
    
    for (Relation r : sorting) {
      if (report) I.say("  Updating relation for "+r.subject);
      final boolean
        okay     = updateRelation(r, UPDATE_PERIOD),
        person   = r.subject instanceof Actor,
        excess   = person && personCount++ > MAX_RELATIONS;
      if (report) {
        I.say("    Value/Novelty: "+r.value()+"/"+r.novelty());
      }
      //
      //  And finally, discard if surplus to requirements.
      if (excess || ! okay) {
        relations.remove(r.subject);
        if (report) I.say("    EXPIRED");
      }
      else {
        final Base b = r.subject.base();
        if (b == null) continue;
        final Faction f = b.faction();
        factionVals  [b.baseID()] += r.value();
        factionCounts[b.baseID()] ++;
        factions     [b.baseID()] = f;
      }
    }

    if (report) I.say("\nUpdating faction relations for "+actor);
    for (int i = Base.MAX_BASES; i-- > 0;) {
      final Faction f = factions[i];
      if (f == null) continue;
      float avgValue  = factionVals[i] / factionCounts[i];
      float weighting = UPDATE_PERIOD * 1f / NOVELTY_INTERVAL;
      float novelty   = noveltyFor(f);
      setupRelation(f, avgValue, novelty - weighting);
      if (report) {
        I.say("  Updating relation for "+f);
        I.say("    Avg value: "+avgValue);
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
    //
    //  If we have an existing relationship, then return its value (this is
    //  always assumed to start at 1.0 for self.)
    Relation r = relations.get(other);
    if (r != null) return r.value();
    else return initRelationValue(other);
  }
  
  
  protected float initRelationValue(Accountable other) {
    if (other == actor) return 1;
    //
    //  Otherwise, we calculate what the starting relationship with this entity
    //  should be.  We base this off a weighted average depending on home/work
    //  identity, community spirit, and relations with their parent base:
    final Base belongs = other.base();
    Relation r = belongs == null ? null : relations.get(belongs.faction());
    float initVal = 0;
    if (r != null) initVal = r.value();
    else initVal = Faction.factionRelation(actor, other);
    
    if (other.base() == actor.base() || initVal > 0) {
      float relVal = 0;
      if (other == actor.mind.home) relVal += 1.0f;
      if (other == actor.mind.work) relVal += 0.5f;
      return (initVal + Nums.clamp(relVal, 0, 2)) / 2;
    }
    else return initVal;
  }
  
  
  public float noveltyFor(Object object) {
    if (! (object instanceof Accountable)) return 0;
    final Accountable other = (Accountable) object;
    
    final Relation r = relations.get(other);
    if (r != null) return r.novelty();
    else return initRelationNovelty(other);
  }
  
  
  protected float initRelationNovelty(Accountable other) {
    if (other == actor) return 0;
    final Base belongs = other.base();
    final float baseNov = belongs == other ? 0 : noveltyFor(belongs);
    return baseNov + INIT_NOVELTY;
  }
  
  
  public void incRelation(
    Accountable other, float toLevel, float weight, float novelty
  ) {
    if (other == null || (weight == 0 && novelty == 0)) return;
    final boolean report =
      I.talkAbout == actor && weight > 0 && verbose && extraVerbose
    ;
    
    Relation r = relations.get(other);
    if (r == null) r = setupRelation(other, 0, 0, Relation.TYPE_GENERIC, false);
    r.incValue(toLevel, weight);
    r.incNovelty(novelty);
    
    if (report) {
      I.say("\nIncrementing relation with "+other);
      I.say("  To level: "+toLevel);
      I.say("  Weight:   "+weight );
      I.say("  Novelty:  "+novelty);
      I.say("  Value after:   "+r.value  ());
      I.say("  Novelty after: "+r.novelty());
      I.add("");
    }
  }

  
  
  public Batch <Relation> allRelations() {
    final Batch <Relation> all = new Batch <Relation> ();
    for (Relation r : relations.values()) all.add(r);
    return all;
  }
  
  
  public Batch <Relation> factionRelations() {
    final Batch <Relation> all = new Batch <Relation> ();
    for (Relation r : relations.values()) {
      if (r.subject instanceof Faction) all.add(r);
    }
    return all;
  }
  
  
  public Batch <Relation> personRelations() {
    final Batch <Relation> all = new Batch <Relation> ();
    for (Relation r : relations.values()) {
      if (r.subject instanceof Actor) all.add(r);
    }
    return all;
  }
  
  
  public Relation relationWith(Object other) {
    return relations.get(other);
  }
  
  
  public Relation setupRelation(
    Accountable other, float baseValue, float baseNovelty,
    int type, boolean exact
  ) {
    Relation r = relations.get(other);
    if (r == null) {
      relations.put(other, r = new Relation(actor, other, 0, 0));
    }
    if (! exact) {
      baseValue   += initRelationValue  (other);
      baseNovelty += initRelationNovelty(other);
    }
    r.setValue(baseValue, baseNovelty);
    r.setType(type);
    return r;
  }
  
  
  public Relation setupRelation(Accountable other, float value, float novelty) {
    return setupRelation(other, value, novelty, Relation.TYPE_GENERIC, true);
  }
  
  
  public Relation setupRelation(Accountable other, float value) {
    return setupRelation(other, value, 0, Relation.TYPE_GENERIC, false);
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





