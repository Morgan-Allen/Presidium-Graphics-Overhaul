/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.hooks;
import stratos.game.common.*;
import stratos.game.verse.*;
import stratos.game.craft.*;
import stratos.game.wild.*;
import stratos.user.*;
import stratos.user.notify.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.util.*;



public class ScenarioTerra extends SectorScenario {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final static CutoutModel
    MODEL_WRECK = CutoutModel.fromImage(
      ScenarioTerra.class, "terra_wreckage_model",
      "media/Scenario/terra_wreckage.png", 2, 2
    );
  
  Batch <Item> artifacts = new Batch();
  
  
  public ScenarioTerra() {
    super(
      StratosSetting.SECTOR_TERRA,
      ""//"media/Scenario/ScriptTerra.xml"
    );
  }
  
  
  public ScenarioTerra(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Script and setup methods-
    */
  public static class ArtifactSite extends SupplyCache {
    
    protected ArtifactSite() {
      super(2, 2, MODEL_WRECK);
    }
    
    
    public ArtifactSite(Session s) throws Exception {
      super(s);
    }
    
    
    public void saveState(Session s) throws Exception {
      super.saveState(s);
    }
    
    
    public String fullName() {
      return "Ancient Wreckage";
    }
    
    
    public Composite portrait(HUD UI) {
      return null;
    }
    
    
    public String helpInfo() {
      return "A piece of wreckage from the crash of the Tsedar.";
    }
  }
  
  
  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    super.configureScenario(world, base, UI);
    
    //  TODO:  Scatter the artifacts within the landscape, along with the
    //  wreckage, and a few within some ruins, including a central wreck.
    //  (Later.  Just the basics for now.)
    
    
  }
  
  
  protected boolean checkShowIntro() {
    return true;
  }
  
  
  protected boolean checkScenarioSuccess() {
    final Venue HQ = (Venue) base().HQ();
    if (HQ == null || HQ.destroyed()) return false;
    for (Item i : artifacts) {
      if (! HQ.stocks.hasItem(i)) return false;
    }
    return true;
  }
  
  
  
  /**  Update methods for when off-stage:
    */
  public void updateOffstage() {
  }
}







