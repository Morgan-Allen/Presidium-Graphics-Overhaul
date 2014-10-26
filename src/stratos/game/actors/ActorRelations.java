


package stratos.game.actors;
import stratos.game.building.*;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.util.*;



public class ActorRelations {
  
  
  /**  Data fields, constructors and save/load methods-
    */
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
  
  
  
  public void updateValues(int numUpdates) {
    final boolean report = verbose && I.talkAbout == actor;
    if (report) I.say("Updating relations, num. updates: "+numUpdates);
    
    for (Relation r : relations.values()) {
      r.update();
    }
    
    if (numUpdates % 10 == 0) {
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
      while (sorting.size() > Relation.MAX_RELATIONS) {
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
        incRelation(base, relation, 10f / World.STANDARD_DAY_LENGTH);
        BID++;
      }
    }
  }
  
  
  
  /**  Handling relationships and attitudes-
    */
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
    if (venue == actor.mind.home) return 1.0f;
    if (venue == actor.mind.work) return 0.5f;
    return valueFor(venue.base()) / 4f;
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
  
  
  public float noveltyFor(Accountable other) {
    final Relation r = relations.get(other);
    if (r == null) return 1;
    return r.novelty();
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
  
  
  public void incRelation(Accountable other, float level, float weight) {
    Relation r = relations.get(other);
    if (r == null) {
      final float baseVal = valueFor(other) / 2;
      r = setRelation(other, baseVal + (level * weight), 1);
    }
    r.incValue(level, weight);
  }
  
  
  public Batch <Relation> relations() {
    final Batch <Relation> all = new Batch <Relation> ();
    for (Relation r : relations.values()) all.add(r);
    return all;
  }
  
  
  public Relation relationWith(Object other) {
    return relations.get(other);
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
  
  
  
  //  TODO:  Restore this later.
  /**  Modification and updates-
    */
  /*
  //
  //  TODO:  Allow for varying degrees of association.
  public void associateWithCurrentBehaviour(Saveable s) {
    final Behaviour current = actor.mind.rootBehaviour();
    if (current == null) return;
    final List <Saveable> assoc = associations.get(current);
    assoc.include(s);
  }
  
  
  public Series <Plan> remembered() {
    return remembered;
  }
  
  
  public Series <Saveable> associationsFor(Plan p) {
    return associations.get(p);
  }
  //*/
}





