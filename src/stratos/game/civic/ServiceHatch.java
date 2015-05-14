/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.civic;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.economic.*;
import static stratos.game.economic.Economy.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;


//  Problem:  The 2x2 gridlock system doesn't play nicely with other
//            structures, so you need to find a smoother joining-system.

//  The art here also probably needs revisions.  And it should be tough enough
//  to withstand a typical siege, or act as an emergency refuge.


public class ServiceHatch extends Venue {
  
  final static String
    IMG_DIR = "media/Buildings/civilian/";
  final static CutoutModel
    ALL_MODELS[][] = CutoutModel.fromImageGrid(
      ServiceHatch.class,
      IMG_DIR+"all_big_roads.png", 2, 2,
      2, 0, true
    ),
    /*
    ALL_MODELS[] = CutoutModel.fromImages(
      ServiceHatch.class, IMG_DIR, 2, 0, true,
      "access_hatch_2.png",
      "causeway_x_axis.png",
      "causeway_y_axis.png"
    ),
    //*/
    //NM[] = ALL_MODELS[0][0],
    NM[][] = ALL_MODELS, HUB_MODEL = NM[0][0],
    MODELS_X_AXIS[] = { HUB_MODEL, NM[0][1], HUB_MODEL },
    MODELS_Y_AXIS[] = { HUB_MODEL, NM[1][1], HUB_MODEL };
  
  final public static ImageAsset
    LINE_ICON = ImageAsset.fromImage(
      ServiceHatch.class, "media/GUI/Buttons/mag_line_button.gif"
    ),
    HATCH_ICON = ImageAsset.fromImage(
      ServiceHatch.class, "media/GUI/Buttons/access_hatch_button.gif"
    );
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    ServiceHatch.class, "service_hatch",
    "Service Hatch", UIConstants.TYPE_ENGINEER,
    2, 0, IS_FIXTURE | IS_LINEAR,
    Bastion.BLUEPRINT, Owner.TIER_FACILITY
  );
  
  
  public ServiceHatch(Base base) {
    super(BLUEPRINT, base);
    structure.setupStats(
      10,  //integrity
      25,  //armour
      20,  //build cost
      Structure.SMALL_MAX_UPGRADES,
      Structure.TYPE_FIXTURE
    );
  }
  
  
  public ServiceHatch(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Economic and behavioural methods-
    */
  public boolean setupWith(Tile position, Box2D area, Coord... others) {
    if (! super.setupWith(position, area, others)) return false;
    
    final Object model = faceModel(position, area, others);
    attachModel((ModelAsset) model);
    return true;
  }
  
  
  private Object faceModel(Tile position, Box2D area, Coord... others) {
    Object model = Placement.setupMergingSegment(
      this, position, area, others,
      MODELS_X_AXIS, MODELS_Y_AXIS, HUB_MODEL, ServiceHatch.class
    );
    /*
    if (model == MODELS_X_AXIS[1]) {
      final int step = (position.y / 2) % 4;
      if (step == 0) model = HUB_MODEL;
    }
    if (model == MODELS_Y_AXIS[1]) {
      final int step = (position.x / 2) % 4;
      if (step == 0) model = HUB_MODEL;
    }
    //*/
    return model;
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (numUpdates % 10 != 0) return;
    
    final Object oldModel = buildSprite.baseSprite().model();
    
    final Object model = faceModel(origin(), null);
    final Upgrade FC = Structure.FACING_CHANGE;
    boolean canChange = false;
    if (model != oldModel) {
      if (GameSettings.buildFree || structure.hasUpgrade(FC)) {
        canChange = true;
      }
      else {
        structure.beginUpgrade(FC, true);
      }
    }
    if (canChange) {
      structure.resignUpgrade(FC, true);
      world.ephemera.addGhost(this, size, sprite(), 0.5f);
      attachModel((ModelAsset) model);
    }
    /*
    //  TODO:  Salvage and rebuild?  See how the shield-wall does it.
    if (model != buildSprite.baseSprite().model()) {
      attachModel((ModelAsset) model);
    }
    //*/
    
    if (model == HUB_MODEL) {
      structure.assignOutputs(
        Item.withAmount(ATMO , 2),
        Item.withAmount(POWER, 2)
      );
      structure.setAmbienceVal(-2);
      //  TODO:  Introduce the vermin-check here!
    }
  }
  
  
  protected boolean canBuildOn(Tile t) {
    //  Heighways can be used to span deserts and bodies of water, so they can
    //  be placed over anything.
    return true;
  }
  
  
  //protected void updatePaving(boolean inWorld) {
  //}
  
  
  public boolean allowsEntry(Mobile m) {
    if (m instanceof Vermin) return true;
    else return false;
  }
  
  
  public int pathType() {
    return Tile.PATH_ROAD;
  }
  
  
  public Background[] careers() {
    return null;
  }
  
  
  public Traded[] services() {
    return null;
  }
  


  /**  Rendering and interface methods-
    */
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(HATCH_ICON, "service_hatch");
  }
  
  
  public String fullName() {
    if (inWorld()) {
      final Object model = buildSprite.baseSprite().model();
      if (model != HUB_MODEL) return "Heighway";
    }
    return super.fullName();
  }
  
  
  public SelectionPane configPanel(SelectionPane panel, BaseUI UI) {
    return VenuePane.configSimplePanel(this, panel, UI, null);
  }
  
  
  public String helpInfo() {
    return
      "Service Hatches allow for power distribution and road connections, "+
      "but can admit passage to dangerous vermin.";
  }
}


