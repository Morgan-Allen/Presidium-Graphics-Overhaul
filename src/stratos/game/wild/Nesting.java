/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.plans.*;
import stratos.user.*;
import stratos.util.*;



//  TODO:  Merge this with FindHome.


public class Nesting extends Plan {
  
  
  /**  Data fields, construction and save/load methods-
    */
  private Target site;
  
  
  public Nesting(Fauna actor) {
    super(actor, actor, MOTIVE_PERSONAL, NO_HARM);
  }
  
  
  public Nesting(Session s) throws Exception {
    super(s);
    site = (Target) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(site);
  }
  
  
  public Plan copyFor(Actor other) {
    return null;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected float getPriority() {
    if (! actor.species().fixedNesting()) return 0;
    if (NestUtils.crowding(actor) < 1   ) return 0;
    return ROUTINE;
  }
  
  
  protected Behaviour getNextStep() {
    if (site == null) site = pickNestSite();
    final Tile around = Spacing.pickFreeTileAround(site, actor);
    if (around == null) return null;
    
    final Action migrates = new Action(
      actor, site,
      this, "actionMigrate",
      Action.LOOK, "Migrating"
    );
    migrates.setMoveTarget(around);
    return migrates;
  }
  
  
  private Target pickNestSite() {
    final float homeCrowding = NestUtils.crowding(actor) / 1.5f;
    final float maxDist      = Fauna.PREDATOR_SEPARATION;
    final Stage world        = actor.world();
    
    final boolean report = I.talkAbout == actor;
    if (report) {
      I.say("\nChecking migration for "+actor+" ("+hashCode()+")");
      I.say("  Home crowding is: "+homeCrowding);
    }

    final Nest nearby = (Nest) world.presences.randomMatchNear(
      Nest.class, actor, maxDist
    );
    if (NestUtils.crowding(actor.species(), nearby, world) < homeCrowding) {
      return nearby;
    }
    else {
      return Spacing.pickRandomTile(actor, maxDist, world);
    }
  }
  
  
  public boolean actionMigrate(Fauna actor, Target point) {
    final Stage world = actor.world();
    
    if (point instanceof Nest) {
      final Nest  nest  = (Nest) point;
      
      if (NestUtils.crowding(actor.species, nest, world) >= 1) {
        return false;
      }
      actor.mind.setHome(nest);
      return true;
    }
    else {
      final Blueprint print = actor.species.nestBlueprint();
      final Nest nest = (Nest) print.createVenue(actor.base());
      nest.setupWith(world.tileAt(point), null);
      
      if (NestUtils.localCrowding(actor.species, point) >= 1) {
        return false;
      }
      if (! nest.canPlace()) {
        return false;
      }
      
      nest.assignBase(actor.base());
      nest.enterWorld();
      nest.structure.setState(Structure.STATE_INTACT, 0.1f);
      actor.goAboard(nest, world);
      return true;
    }
  }
  
  
  
  
  /**  Rendering, interface and debug methods-
    */
  public void describeBehaviour(Description d) {
    d.appendAll("Migrating");
  }
}













