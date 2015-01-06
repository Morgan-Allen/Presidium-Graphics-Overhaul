/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.maps;
import stratos.game.common.*;
import stratos.util.*;



public class DangerMap extends FadingMap {
  
  
  /**  Data fields, construction and save/load methods-
    */
  private boolean verbose = false;
  
  final Base base;
  
  
  public DangerMap(Stage world, Base base) {
    super(world, world.sections.resolution, -1);
    //TODO:  Use Sector Size for grid resolution, and enhance pathfinding with
    //       enemy fog-of-war.
    this.base = base;
  }
  
  
  public void loadState(Session s) throws Exception {
    super.loadState(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Methods for regularly updating, adjusting and querying danger values-
    */
  public void update() {
    super.update();
    
    if (verbose) {
      I.say("\nDanger map updated for "+base.title()+" ("+base.hashCode()+")");
      
      final int o = resolution / 2;
      for (Coord c : Visit.grid(o, o, world.size, world.size, resolution)) {
        final float value = patchValue(c.x, c.y);
        if (value > 0) {
          I.say("  Positive at "+c.x+" "+c.y+":    "+value);
          I.say("    (Sample is: "+sampleAt(c.x, c.y)+")");
        }
      }
    }
    
    //if (base == PlayLoop.currentScenario().base()) {
      //I.present(shortTermVals, "Danger map", 200, 200, 20, -20);
    //}
  }
  
  
  public void accumulate(float value, float duration, int x, int y) {
    super.accumulate(value, duration, x, y);
    /*
    if (value > 0) {
      I.say("Getting danger from ...");
      new Exception().printStackTrace();
    }
    //*/
  }
  
  
  
  
  
  /**  TODO:  Include generalised methods for estimating distance/danger totals
    *  associated with routes between different sectors!
    */
}














