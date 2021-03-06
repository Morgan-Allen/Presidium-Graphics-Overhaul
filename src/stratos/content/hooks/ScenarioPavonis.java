/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.hooks;
import stratos.game.common.*;
import stratos.game.verse.*;
import stratos.game.wild.*;
import stratos.game.craft.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;




public class ScenarioPavonis extends SectorScenario {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final static CutoutModel
    MODEL_SF = CutoutModel.fromImage(
      ScenarioPavonis.class, "pavonis_flora",
      "media/Scenario/pavonis_flora.png", 1, 1
    );
  
  
  final static int
    TOTAL_FLORA    = 5,
    SAMPLES_NEEDED = 3;
  
  Batch <Element> specialFlora = new Batch();
  
  
  
  public ScenarioPavonis() {
    super(
      StratosSetting.SECTOR_PAVONIS,
      ""//"media/Scenario/ScriptPavonis.xml"
    );
  }
  
  
  public ScenarioPavonis(Session s) throws Exception {
    super(s);
    s.loadObjects(specialFlora);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObjects(specialFlora);
  }
  
  
  
  /**  Script and setup methods-
    */
  public static class SpecialFlora extends Element {
    
    
    protected SpecialFlora() {
      super();
    }
    
    
    public SpecialFlora(Session s) throws Exception {
      super(s);
    }
    
    
    public void saveState(Session s) throws Exception {
      super.saveState(s);
    }
    
    
    public int owningTier() {
      return Owner.TIER_PRIVATE;
    }
    
    
    public String fullName() {
      return "Special Flora";
    }
    
    
    public Composite portrait(HUD UI) {
      return null;
    }
    
    
    public String helpInfo() {
      return "A rare species of flora with unique pharmaceutical properties.";
    }
  }
  
  
  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    super.configureScenario(world, base, UI);
    
    for (int n = TOTAL_FLORA; n-- > 0;) {
      
      Tile at = world.tileAt(Rand.index(world.size), Rand.index(world.size));
      Flora replace = (Flora) world.presences.nearestMatch(Flora.class, at, -1);
      if (replace == null) continue;
      
      at = replace.origin();
      replace.exitWorld();
      
      Element flora = new SpecialFlora();
      flora.attachModel(MODEL_SF);
      flora.enterWorldAt(at.x, at.y, world, true);
      specialFlora.add(flora);
    }
  }
  
  
  protected boolean checkShowIntro() {
    return true;
  }
  
  
  protected boolean checkFloraFound() {
    for (Element e : specialFlora) {
      if (e.visibleTo(base())) return true;
    }
    return false;
  }
  
  
  protected void zoomToFloraFound() {
    for (Element e : specialFlora) {
      if (e.visibleTo(base())) {
        Selection.pushSelection(e, null);
        break;
      }
    }
  }
  
  
  private int numSamplesTaken() {
    final Venue HQ = (Venue) base().HQ();
    if (HQ == null || HQ.destroyed()) return 0;
    int numSampled = 0;
    for (Item match : HQ.stocks.matches(Economy.SAMPLES)) {
      if (((Batch) specialFlora).includes(match.refers)) numSampled++;
    }
    return numSampled;
  }
  
  
  protected boolean checkFloraSampled() {
    return numSamplesTaken() > 0;
  }
  
  
  protected boolean checkScenarioSuccess() {
    return numSamplesTaken() >= SAMPLES_NEEDED;
  }
  
  
  
  /**  Update methods for when off-stage:
    */
  public void updateOffstage() {
  }
  
}














