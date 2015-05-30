

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
  final static Blueprint BLUEPRINT = new Blueprint(
    Fractal.class, "fractal",
    "Fractal", UIConstants.TYPE_AESTHETIC, ICON,
    "Fractals are decorative paving structures composed of interlocking "+
    "geometric forms.",
    2, 0, Structure.IS_FIXTURE | Structure.IS_LINEAR,
    Fabricator.BLUEPRINT, Owner.TIER_PRIVATE,
    40,  // integrity
    8 ,  // armour
    30,  // build cost
    Structure.NO_UPGRADES
  );
  
  
  public Fractal(Base base) {
    super(BLUEPRINT, base);
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
}













