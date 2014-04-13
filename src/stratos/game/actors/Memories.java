


package stratos.game.actors ;
import stratos.game.building.Venue;
import stratos.game.civilian.Accountable;
import stratos.game.common.*;
import stratos.game.common.Session.Saveable;
import stratos.util.*;



public class Memories {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final static int
    DEFAULT_MAX_MEMORY = 10,
    DEFAULT_MAX_ASSOC  = 100 ;
  
  private static boolean verbose = false ;
  
  
  final Actor actor ;
  final Table <Accountable, Relation> relations = new Table() ;
  //final List <Plan> remembered ;
  //final Table <Plan, List <Saveable>> associations ;
  //
  //  TODO:  You might want to use classes here instead of plans, at least for
  //  the association table.
  
  
  Memories(Actor actor) {
    this.actor = actor ;
    //remembered = new List <Plan> () ;
    //associations = new Table <Plan, List <Saveable>> () ;
  }
  
  
  protected void loadState(Session s) throws Exception {

    for (int n = s.loadInt() ; n-- > 0 ;) {
      final Relation r = Relation.loadFrom(s) ;
      relations.put((Actor) r.subject, r) ;
    }
    /*
    s.loadObjects(remembered) ;
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final Plan key = (Plan) s.loadObject() ;
      final List <Saveable> associated = new List <Saveable> () ;
      s.loadObjects(associated) ;
      associations.put(key, associated) ;
    }
    //*/
  }
  
  
  protected void saveState(Session s) throws Exception {
    
    s.saveInt(relations.size()) ;
    for (Relation r : relations.values()) Relation.saveTo(s, r) ;
    /*
    s.saveObjects(remembered) ;
    s.saveInt(associations.size()) ;
    for (Plan p : associations.keySet()) {
      s.saveObject(p) ;
      s.saveObjects(associations.get(p)) ;
    }
    //*/
  }
  
  
  

  public void updateValues(int numUpdates) {
    for (Relation r : relations.values()) {
      r.update();
    }
  }
  
  
  public float relationValue(Base base) {
    final Base AB = actor.base() ;
    if (AB != null) {
      if (base == AB) return 1 ;
      if (base == null) return 0 ;
      return AB.relationWith(base) ;
    }
    else return 0 ;
  }
  
  
  public float relationValue(Venue venue) {
    if (venue == null) return 0 ;
    if (venue == actor.mind.home) return 1.0f ;
    if (venue == actor.mind.work) return 0.5f ;
    return relationValue(venue.base()) / 2f ;
  }
  
  
  public float relationValue(Actor other) {
    final Relation r = relations.get(other) ;
    if (r == null) {
      return relationValue(other.base()) / 2 ;
    }
    if (r.subject == actor) return Visit.clamp(r.value() + 1, 0, 1) ;
    return r.value() + (relationValue(other.base()) / 2) ;
  }
  
  
  public float relationValue(Object other) {
    if (other instanceof Venue) return relationValue((Venue) other);
    if (other instanceof Actor) return relationValue((Actor) other);
    if (other instanceof Base ) return relationValue((Base ) other);
    return 0 ;
  }
  
  
  public float relationNovelty(Accountable other) {
    final Relation r = relations.get(other) ;
    if (r == null) return 1 ;
    return r.novelty() ;
  }
  
  
  public Relation initRelation(Accountable other, float value, float novelty) {
    final Relation r = new Relation(actor, other, value, novelty);
    relations.put(other, r) ;
    return r ;
  }
  
  
  public void incRelation(Accountable other, float level, float weight) {
    Relation r = relations.get(other) ;
    if (r == null) {
      final float baseVal = relationValue(other);
      r = initRelation(other, baseVal + (level * weight), 1) ;
    }
    r.incValue(level, weight) ;
  }
  
  
  public Batch <Relation> relations() {
    final Batch <Relation> all = new Batch <Relation> () ;
    for (Relation r : relations.values()) all.add(r) ;
    return all ;
  }
  
  
  public boolean hasRelation(Accountable other) {
    return relations.get(other) != null ;
  }
  
  
  
  //  TODO:  Restore this later.
  /**  Modification and updates-
    */
  /*
  //
  //  TODO:  Allow for varying degrees of association.
  public void associateWithCurrentBehaviour(Saveable s) {
    final Behaviour current = actor.mind.rootBehaviour() ;
    if (current == null) return ;
    final List <Saveable> assoc = associations.get(current) ;
    assoc.include(s) ;
  }
  
  
  public Series <Plan> remembered() {
    return remembered ;
  }
  
  
  public Series <Saveable> associationsFor(Plan p) {
    return associations.get(p) ;
  }
  //*/
}





