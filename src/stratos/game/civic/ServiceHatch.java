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
    HATCH_MODEL = CutoutModel.fromSplatImage(
      ServiceHatch.class, "media/Buildings/civilian/access_hatch_2.png", 2
    ),
    //  TODO:  Read these in from a single image-map for efficiency.
    LINE_MODELS[] = CutoutModel.fromImages(
      ServiceHatch.class, IMG_DIR, 2, 0.25f, true,
      "causeway_cap_west.png" ,
      "causeway_x_axis.png"   ,
      "causeway_cap_east.png" ,
      "causeway_cap_south.png",
      "causeway_y_axis.png"   ,
      "causeway_cap_north.png",
      "causeway_hub.png"
    ),
    NM[] = LINE_MODELS,
    HUB_MODEL = HATCH_MODEL,
    MODELS_X_AXIS[] = { NM[0], NM[1], NM[2] },
    MODELS_Y_AXIS[] = { NM[3], NM[4], NM[5] };
  
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
      0,   //max upogrades
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
    
    final Object model = Placement.setupMergingSegment(
      this, position, area, others,
      MODELS_X_AXIS, MODELS_Y_AXIS, HUB_MODEL, ServiceHatch.class
    );
    attachModel((ModelAsset) model);
    return true;
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (numUpdates % 10 != 0) return;
    
    final Object model = Placement.setupMergingSegment(
      this, origin(), footprint(), new Coord[0],
      MODELS_X_AXIS, MODELS_Y_AXIS, HUB_MODEL, ServiceHatch.class
    );
    //  TODO:  Salvage and rebuild?  See how the shield-wall does it.
    if (model != buildSprite.baseSprite().model()) {
      attachModel((ModelAsset) model);
    }
    
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
  
  
  protected void updatePaving(boolean inWorld) {
    base.transport.updatePerimeter(this, inWorld);
  }
  
  
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
  
  
  public SelectionPane configPanel(SelectionPane panel, BaseUI UI) {
    return VenuePane.configSimplePanel(this, panel, UI, null);
  }
  
  
  public String helpInfo() {
    return
      "Service Hatches allow for power distribution and road connections, "+
      "but can admit passage to dangerous vermin.";
  }
}


