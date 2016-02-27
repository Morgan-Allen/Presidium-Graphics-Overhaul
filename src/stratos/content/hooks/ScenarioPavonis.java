/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.hooks;
import stratos.game.common.*;
import stratos.game.verse.*;
import stratos.game.craft.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.user.notify.*;
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
  
  final MessageScript script;
  
  Batch <Element> specialFlora = new Batch();
  
  
  
  public ScenarioPavonis() {
    super();
    this.script = new MessageScript(
      this, "src/stratos/content/hooks/ScriptPavonis.xml"
    );
  }
  
  
  public ScenarioPavonis(Session s) throws Exception {
    super(s);
    script = (MessageScript) s.loadObject();
    s.loadObjects(specialFlora);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(script);
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
    
    
    //  TODO:  Make sure none of these appear within a certain range of the
    //  bastion.
    
    for (int n = TOTAL_FLORA; n-- > 0;) {
      Tile at = world.tileAt(Rand.index(world.size), Rand.index(world.size));
      at = Spacing.nearestOpenTile(at, at);
      if (at == null) continue;
      
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
  
  
  protected boolean checkScenarioSuccess() {
    final Venue HQ = (Venue) base().HQ();
    if (HQ == null || HQ.destroyed()) return false;
    
    int numSampled = 0;
    for (Item match : HQ.stocks.matches(Economy.SAMPLES)) {
      if (((Batch) specialFlora).includes(match.refers)) numSampled++;
    }
    return numSampled >= SAMPLES_NEEDED;
  }
  
  
  
  /**  Update methods for when off-stage:
    */
  public void updateOffstage() {
  }
  
  
  
  /**  Rendering, debug and interface methods-
    */
  public void describeHook(Description d) {
    final String summary = script.contentForTopic("Summary");
    d.append(summary);
  }
  
}














