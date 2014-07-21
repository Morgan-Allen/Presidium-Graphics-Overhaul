/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.base;
import stratos.game.common.*;
import stratos.game.tactical.*;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



public class BlastDoors extends ShieldWall implements TileConstants {
  
  
  /**  Fields, constants, constructors and save/load methods-
    */
  
  public BlastDoors(Base base, int facing) {
    super(TYPE_DOORS, 4, 2, base);
    this.facing = facing;
    //personnel.setShiftType(SHIFTS_BY_HOURS);
    if (facing == X_AXIS)
      attachSprite(ShieldWall.DOORS_MODEL_LEFT.makeSprite());
    if (facing == Y_AXIS)
      attachSprite(ShieldWall.DOORS_MODEL_RIGHT.makeSprite());
  }
  
  
  public BlastDoors(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Life cycle and placement-
    */
  protected void updatePaving(boolean inWorld) {
    entrances();
    base().paving.updatePerimeter(this, inWorld);
    for (Boarding b : entrances()) if (b instanceof Tile) {
      base().paving.updateJunction(this, (Tile) b, inWorld);
    }
  }
  
  
  public boolean enterWorldAt(int x, int y, World world) {
    if (! super.enterWorldAt(x, y, world)) return false;
    world.terrain().maskAsPaved(Spacing.under(area(), world), true);
    return true;
  }
  
  
  public void exitWorld() {
    world.terrain().maskAsPaved(Spacing.under(area(), world), false);
    super.exitWorld();
  }
  
  
  
  /**  Behaviour implementation-
    */
  /*
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null;
    final Choice choice = new Choice(actor);
    
    final int hour = ((int) (world.currentTime() * 12)) % 12;
    final int patrolDir = TileConstants.N_ADJACENT[hour % 4];
    final Patrolling p = Patrolling.sentryDuty(actor, this, patrolDir);
    if (p != null) {
      p.setMotive(Plan.MOTIVE_DUTY, Plan.ROUTINE);
      choice.add(p);
    }
    
    return choice.weightedPick();
  }
  
  
  public Background[] careers() {
    return new Background[] { Backgrounds.VOLUNTEER };
  }
  
  
  public int numOpenings(Background b) {
    final int nO = super.numOpenings(b);
    if (b == Backgrounds.VOLUNTEER) return nO + 2;
    return 0;
  }
  //*/
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Blast Doors";
  }


  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ShieldWall.ICON, "blast_doors");
  }


  public String helpInfo() {
    return
      "Blast Doors grant your citizens access to enclosed sectors of your "+
      "base, and can be used to restrict passage to undesireables.";
  }
  
  
  //
  //  TODO:  Allow the player to control access to a given area by restricting
  //  what kind of personnel are allowed through given doors.
  
  
  public String buildCategory() {
    return InstallTab.TYPE_MILITANT;
  }
}









