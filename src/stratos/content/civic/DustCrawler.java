


package stratos.content.civic;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.*;
import stratos.graphics.common.*;
import stratos.graphics.solids.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.economic.Economy.*;





//*
//  TODO:  Adapt this to sand-panning instead?

public class DustCrawler extends Vehicle {
  
  /**  Fields, constants, constructors and save/load methods-
    */
//*
  final static String
    FILE_DIR = "media/Vehicles/",
    XML_FILE = "VehicleModels.xml";
  final static ModelAsset
    MODEL = MS3DModel.loadFrom(
      FILE_DIR, "DustCrawler.ms3d",
      DustCrawler.class, XML_FILE, "DustCrawler"
    );
  
  
  
  public DustCrawler() {
    super();
    attachModel(MODEL);
  }
  
  
  public DustCrawler(Session s) throws Exception {
    super(s);
    toggleSoilDisplay();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  public float height() { return 0.3f; }
  public float radius() { return 0.3f; }
  
  public float homeCrowding(Actor actor) { return 1; }
  public float visitCrowding(Actor actor) { return 1; }
  public Traded[] services() { return null; }
  
  
  
  /**  Behavioural methods-
    */
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    
    if (! pathing.checkPathingOkay()) pathing.refreshFullPath();
    
    //  TODO:  Restore once building/salvage of vehicles is complete-
    ///if (! structure.intact()) return;
    
    /*
    base().intelMap.liftFogAround(this, 3.0f);
    
    final FormerPlant plant = (FormerPlant) hangar();
    final Target going = pathing.target();
    
    if (going == null || going == aboard()) {
      if (plant == aboard()) {
        for (Item sample : cargo.matches(SAMPLES)) {
          final Tile t = (Tile) sample.refers;
          plant.soilSamples += (t.habitat().minerals() / 10f) + 0.5f;
          cargo.removeItem(sample);
        }
        if (plant.soilSamples < 10) {
          pathing.updateTarget(plant.pickSample());
        }
      }
      else {
        cargo.addItem(Item.withReference(SAMPLES, origin()));
        pathing.updateTarget(plant);
      }
      toggleSoilDisplay();
    }
    //*/
  }
  
  
  protected float baseMoveRate() {
    return 1.0f;
  }
  
  
  protected void pathingAbort() {
    super.pathingAbort();
    /*
    if (! pathing.checkPathingOkay()) {
      final FormerPlant plant = (FormerPlant) hangar();
      if (cargo.amountOf(SAMPLES) > 0) pathing.updateTarget(plant);
      else pathing.updateTarget(plant.pickSample());
    }
    //*/
  }
  
  
  
  /**  Rendering and interface methods-
    */
//*
  private void toggleSoilDisplay() {
    final SolidSprite sprite = (SolidSprite) sprite();
    sprite.togglePart("soil bed", cargo.amountOf(SAMPLES) > 0);
  }
  
  
  public String fullName() {
    return "Dust Crawler ";
  }
  
  
  public Composite portrait(BaseUI UI) {
    return null;
  }
  
  
  public String helpInfo() {
    return
      "Dust crawlers perform automatic soil-sampling and terraforming "+
      "duties with the barest minimum of human supervision.\n\n"+
      "  'This one's called Bett, and this one's Jordi.  Jordi is pretty "+
      "friendly, but you just want to stand well back when Bett starts "+
      "making that humming noise- means she don't like you.'\n"+
      "  -Former Engineer prior to recall";
  }
}

//*/

