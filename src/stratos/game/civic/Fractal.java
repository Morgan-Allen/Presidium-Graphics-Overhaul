

package stratos.game.civic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



public class Fractal extends Venue {
  
  
  final static String IMG_DIR = "media/Buildings/aesthete/";
  final static ImageAsset ICON = ImageAsset.fromImage(
    ServiceHatch.class, "media/GUI/Buttons/fractal_button.gif"
  );
  final static CutoutModel
    FRACTAL_MODELS[][] = CutoutModel.fromImageGrid(
      Fractal.class, IMG_DIR+"all_fractals.png",
      2, 3, 2, 0, true
    ),
    MODELS_X_AXIS[] = FRACTAL_MODELS[0],
    MODELS_Y_AXIS[] = FRACTAL_MODELS[1];
  
  
  /**  Definition, setup and save/load methods:
    */
  final static VenueProfile PROFILE = new VenueProfile(
    Fractal.class, "fractal", "Fractal",
    2, 0, IS_FIXTURE | IS_LINEAR,
    Fabricator.PROFILE, Owner.TIER_PRIVATE
  );
  
  
  public Fractal(Base base) {
    super(PROFILE, base);
    structure.setupStats(
      40,  // integrity
      8 ,  // armour
      30,  // build cost
      0 ,  // max. upgrades
      Structure.TYPE_FIXTURE
    );
  }
  
  
  public Fractal(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Economic and behavioural functions-
    */
  public Background[] careers() { return null; }
  public Traded[] services() { return null; }
  public int pathType() { return Tile.PATH_ROAD; }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (structure.intact()) {
      structure.setAmbienceVal(Ambience.HIGH_AMBIENCE);
    }
    else {
      structure.setAmbienceVal(Ambience.NO_AMBIENCE_FX);
    }
  }
  

  public boolean setupWith(Tile position, Box2D area, Coord... others) {
    if (! super.setupWith(position, area, others)) return false;
    final Object model = Placement.setupSegment(
      this, position, area, others, MODELS_X_AXIS, MODELS_Y_AXIS
    );
    attachModel((ModelAsset) model);
    return true;
  }
  
  
  
  /**  Rendering and interface methods:
    */
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "fractal_icon");
  }
  
  
  public String helpInfo() {
    return
      "Fractals are decorative paving structures composed of interlocking "+
      "geometric forms.";
  }
  
  
  public String objectCategory() {
    return UIConstants.TYPE_AESTHETIC;
  }
}













