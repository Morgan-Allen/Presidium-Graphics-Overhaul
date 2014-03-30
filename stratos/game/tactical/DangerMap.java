


package stratos.game.tactical ;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.util.*;



public class DangerMap extends FadingMap {
  
  
  /**  Data fields, construction and save/load methods-
    */
  final Base base ;
  
  
  public DangerMap(World world, Base base) {
    super(world, world.sections.resolution, -1) ;
    //TODO:  Use Sector Size for grid resolution, and enhance pathfinding with
    //       enemy fog-of-war.
    this.base = base ;
  }
  
  
  public void loadState(Session s) throws Exception {
    super.loadState(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Methods for regularly updating, adjusting and querying danger values-
    */
  public void update() {
    super.update();
    //if (base == PlayLoop.currentScenario().base()) {
      //I.present(shortTermVals, "Danger map", 200, 200, 20, -20) ;
    //}
  }
  
  
  
  /**  TODO:  Include generalised methods for estimating distance/danger totals
    *  associated with routes between different sectors!
    */
}














