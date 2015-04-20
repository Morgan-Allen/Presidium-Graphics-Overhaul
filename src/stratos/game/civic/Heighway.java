/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.civic;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



public class Heighway extends Venue {
  
  
  final static String
    IMG_DIR = "media/Buildings/civilian/";
  final static CutoutModel
  
  //  TODO:  Read these in from a single image-map for efficiency.
    NODE_MODELS[] = CutoutModel.fromImages(
      Heighway.class, IMG_DIR, 2, 0.25f, true,
      "causeway_cap_west.png" ,
      "causeway_x_axis.png"   ,
      "causeway_cap_east.png" ,
      "causeway_cap_south.png",
      "causeway_y_axis.png"   ,
      "causeway_cap_north.png",
      "causeway_hub.png"
    ),
    NM[] = NODE_MODELS,
    HUB_MODEL = NM[6],
    MODELS_X_AXIS[] = { NM[0], NM[1], NM[2] },
    MODELS_Y_AXIS[] = { NM[3], NM[4], NM[5] };
  final public static ImageAsset
    ICON = ImageAsset.fromImage(
      Heighway.class, "media/GUI/Buttons/mag_line_button.gif"
    );
  
  final public static VenueProfile PROFILE = new VenueProfile(
    Heighway.class, "heighway", "Heighway",
    2, 0, IS_FIXTURE | IS_LINEAR | IS_GRIDDED,
    NO_REQUIREMENTS, Owner.TIER_FACILITY
  );
  
  
  public Heighway(Base base) {
    super(PROFILE, base);
    structure.setupStats(
      10,  //integrity
      25,  //armour
      20,  //build cost
      0,   //max upogrades
      Structure.TYPE_FIXTURE
    );
  }
  
  
  public Heighway(Session s) throws Exception {
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
      MODELS_X_AXIS, MODELS_Y_AXIS, HUB_MODEL, Heighway.class
    );
    attachModel((ModelAsset) model);
    return true;
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (numUpdates % 10 != 0) return;
    
    final Object model = Placement.setupMergingSegment(
      this, origin(), footprint(), new Coord[0],
      MODELS_X_AXIS, MODELS_Y_AXIS, HUB_MODEL, Heighway.class
    );
    //  TODO:  Salvage and rebuild?
    if (model != buildSprite.baseSprite().model()) {
      attachModel((ModelAsset) model);
    }
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
    return Composite.withImage(ICON, "causeway");
  }
  
  
  public SelectionPane configPanel(SelectionPane panel, BaseUI UI) {
    return VenuePane.configSimplePanel(this, panel, UI, null);
  }
  
  
  public String helpInfo() {
    return
      "Heighways facilitate long-range transport of goods and personnel, "+
      "along with water, power and life support.";
  }
  
  
  public String objectCategory() {
    return UIConstants.TYPE_ENGINEER;
  }
}


