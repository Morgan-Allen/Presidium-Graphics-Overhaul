/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.maps;
import stratos.game.common.*;
import stratos.util.*;


//  TODO:  Use Sector Size for grid resolution, and enhance pathfinding with
//         enemy fog-of-war?

public class DangerMap extends BlurMap {
  
  
  /**  Data fields, construction and save/load methods-
    */
  private boolean verbose = false;
  
  final Stage world;
  final Base  base ;
  
  
  public DangerMap(Stage world, Base base) {
    super(world.size, world.sections.resolution, null, "Danger");
    this.world = world;
    this.base  = base;
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
    super.updateAllValues(1f / Stage.STANDARD_DAY_LENGTH);
    
    if (verbose) {
      I.say("\nDanger map updated for "+base.title()+" ("+base.hashCode()+")");
      final int o = patchSize / 2;
      for (Coord c : Visit.grid(o, o, world.size, world.size, patchSize)) {
        final float value = patchValue(c.x, c.y);
        if (value > 0) {
          I.say("  Positive at "+c.x+" "+c.y+":    "+value);
          I.say("    (Sample is: "+sampleValue(c.x, c.y)+")");
        }
      }
    }
  }
  
  
  public void accumulate(float value, float duration, int x, int y) {
    value *= duration / Stage.STANDARD_DAY_LENGTH;
    super.impingeValue(value, x, y);
  }
  
  
  public float sampleAround(float x, float y, float radius) {
    float value = sampleValue(x, y);
    if (radius > 0) value *= (radius * radius) / (patchSize * patchSize);
    return value;
  }
  
  
  public float sampleAround(Target t, float radius) {
    Vec3D pos = t.position(null);
    return sampleAround(pos.x, pos.y, radius);
  }
  
  
  public int globalValue() {
    return super.globalValue();
  }
  
  
  
  /**  TODO:  Include generalised methods for estimating distance/danger totals
    *  associated with routes between different sectors!
    */
}














