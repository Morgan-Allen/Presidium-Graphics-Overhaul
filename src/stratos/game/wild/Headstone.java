/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.util.*;



public class Headstone extends Element {
  
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final public static CutoutModel
    HEADSTONE_MODELS[][] = CutoutModel.fromImageGrid(
      Headstone.class, "headstone_models",
      "media/Buildings/lairs and ruins/all_headstones.png",
      2, 2, 0.66f, 0.5f, false
    );
  
  final Actor buried;
  float decayTime = -1;
  
  
  public Headstone(Actor buried) {
    this.buried    = buried;
    this.decayTime = Stage.STANDARD_DAY_LENGTH;
    
    //  TODO:  Consider deriving the headstone model from the Species (or
    //  Background) of the actor!
    attachModel(HEADSTONE_MODELS[0][0]);
  }
  
  
  public Headstone(Session s) throws Exception {
    super(s);
    this.buried    = (Actor) s.loadObject();
    this.decayTime = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(buried   );
    s.saveFloat (decayTime);
  }
  
  
  
  /**  Properties and physical life-cycle-
    */
  public int pathType() {
    return Tile.PATH_HINDERS;
  }
  
  
  public void onGrowth(Tile t) {
    super.onGrowth(t);
    decayTime -= Stage.GROWTH_INTERVAL;
    if (decayTime <= 0) setAsDestroyed(false);
  }
  
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    //  TODO:  Include time/cause of death, birth date, etc.!
    return "Grave of "+buried.fullName();
  }
  
  
  public Composite portrait(HUD UI) {
    return null;
  }
  
  
  public String helpInfo() {
    return null;
  }
}










