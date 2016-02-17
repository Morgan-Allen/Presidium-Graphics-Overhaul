/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.util.*;
import stratos.game.wild.Headstone;




public class Burial extends Plan {
  
  
  /**  Data fields, constructors, and save/load methods:
    */
  final Actor corpse;
  final Boarding store;
  
  
  private Burial(Actor actor, Actor corpse, Boarding store) {
    super(actor, corpse, NO_PROPERTIES, NO_HARM);
    this.corpse = corpse;
    this.store  = store ;
  }
  
  
  public Burial(Session s) throws Exception {
    super(s);
    this.corpse = (Actor) subject;
    this.store  = (Boarding) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(store);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Burial(other, corpse, store);
  }

  
  
  /**  Target selection and priority evaluation-
    */
  public static interface Services extends Property {
    float corpseCrowding();
    float corpseHarmLevel(Actor corpse);
    boolean acceptsCorpse(Actor corpse);
  }
  
  
  public static Burial burialFor(Actor actor, Actor corpse) {
    if (! canBury(actor, corpse)) return null;
    
    final Stage world = actor.world();
    final Batch <Boarding> stores = new Batch();
    final Tile grave = findGrave(actor, corpse);
    if (grave != null) stores.add(grave);
    final Object key = Economy.SERVICE_BURIAL;
    world.presences.sampleFromMap(corpse, world, 3, stores, key);
    
    final Pick <Boarding> pick = new Pick <Boarding> (0);
    for (Boarding b : stores) pick.compare(b, ratingFor(actor, corpse, b));
    
    final Boarding store = pick.result();
    if (store == null) return null;
    return new Burial(actor, corpse, store);
  }
  
  
  private static Tile findGrave(Actor actor, Actor corpse) {
    
    //  TODO:  There should definitely be something similar to SiteUtils for
    //         this...
    
    final Tile at = corpse.origin();
    if (ratingFor(actor, corpse, at  ) > 0) return at  ;
    final Tile open = Spacing.nearestOpenTile(corpse, actor);
    if (ratingFor(actor, corpse, open) > 0) return open;
    final Tile pick = Spacing.pickRandomTile(corpse, 3, actor.world());
    if (ratingFor(actor, corpse, pick) > 0) return pick;
    return null;
  }
  
  
  private static boolean canBury(Actor actor, Actor corpse) {
    
    if (corpse.health.alive() || ! corpse.inWorld()           ) return false;
    if (! BringPerson.canCarry(actor, corpse)                 ) return false;
    if (PlanUtils.competition(Burial.class, corpse, actor) > 0) return false;
    
    final boolean inStasis = corpse.health.suspended();
    if (corpse.aboard() instanceof Services && inStasis) return false;
    
    return true;
  }
  
  
  private static float ratingFor(Actor actor, Actor corpse, Boarding store) {
    float rating = 1;
    
    if (store instanceof Services) {
      final Services s = (Services) store;
      if (! s.acceptsCorpse(corpse)) return 0;
      
      final float liking = actor.relations.valueFor(corpse);
      final float harmFactor = s.corpseHarmLevel(corpse);
      rating += liking;
      rating += actor.relations.valueFor(store) - 0.5f;
      rating -= Nums.abs(liking + harmFactor) / 2;
      rating *= 1 - s.corpseCrowding();
    }
    else if (store instanceof Tile) {
      final Tile t = (Tile) store;
      if (t.pathType() != Tile.PATH_CLEAR) return 0;
      
      final float liking = actor.relations.valueFor(corpse);
      rating += liking;
      rating /= 2;
    }
    else return 0;
    
    rating /= 1 + Spacing.zoneDistance(corpse, store);
    return rating;
  }
  
  
  protected float getPriority() {
    if (! canBury(actor, corpse)) return -1;
    return ratingFor(actor, corpse, store) * ROUTINE;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    if (store instanceof Tile) {
      if (corpse.aboard() == store) {
        final Action buries = new Action(
          actor, corpse,
          this, "actionBury",
          Action.BUILD, "Burying"
        );
        return buries;
      }
      else {
        return new BringPerson(actor, corpse, store);
      }
    }
    else {
      final boolean stasis = corpse.health.suspended();
      if (corpse.aboard() == store) {
        if (stasis) return null;
        final Action freeze = new Action(
          actor, corpse,
          this, "actionFreeze",
          Action.BUILD, "Freezing"
        );
        return freeze;
      }
      else return new BringPerson(actor, corpse, store);
    }
  }
  
  
  public boolean actionBury(Actor actor, Actor corpse) {
    final Tile point = (Tile) store;
    final Stage world = actor.world();
    final Headstone marker = new Headstone(corpse);
    
    world.ephemera.addGhost(point, 1, corpse.sprite(), 1, 1);
    corpse.exitWorld();
    marker.enterWorldAt(point, world);
    
    return true;
  }
  
  
  public boolean actionFreeze(Actor actor, Actor corpse) {
    corpse.health.setState(ActorHealth.STATE_SUSPEND);
    return true;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (store instanceof Tile) d.appendAll("Burying ", corpse, " at ", store);
    else d.appendAll("Storing ", corpse, " at ", store);
  }
}











