

package stratos.game.base;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.economic.Economy.*;


public class RunnerLodge extends Venue {
  
  
  /**  Setup and constructors-
    */
  final static String IMG_DIR = "media/Buildings/merchant/";
  final static ImageAsset ICON = ImageAsset.fromImage(
    EcologistStation.class, "media/GUI/Buttons/runner_market_button.gif"
  );
  final static ModelAsset MODEL = CutoutModel.fromImage(
    RunnerLodge.class, IMG_DIR+"runner_market.png", 4, 3
  );
  
  
  public RunnerLodge(Base base) {
    super(4, 2, ENTRANCE_EAST, base);
    structure.setupStats(
      150, 3, 250,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    );
    personnel.setShiftType(SHIFTS_BY_HOURS);
    attachModel(MODEL);
  }
  
  
  public RunnerLodge(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Economic and behavioural overrides-
    */
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null;
    
    ///I.say("\nGetting next runner job for "+actor);
    final Choice choice = new Choice(actor);
    //  TODO:  Figure this out.  What would the Yakuza get up to on a regular
    //         day?
    
    //  TODO:  Only loot from distant areas of the city, or from other
    //  settlements- just collect protection money nearby.
    choice.add(Looting.nextLootingFor(actor, this));
    
    //  smuggling, extortion, assassination
    //  cyber tech, hard stims, flesh trade
    
    //  The Ghost Hand   (extortion, black market tech, hacking)
    //  The Hudzeena     (smuggling, the flesh trade, wealth)
    //  The IV Punks     (hard stims, assassination, combat)
    //  The Free Traders (ships only)
    
    return choice.weightedPick();
  }
  
  
  public int numOpenings(Background b) {
    int nO = super.numOpenings(b);
    if (b == RUNNER) return nO + 3;
    return 0;
  }
  
  
  public Background[] careers() {
    //  TODO:  Introduce the different gangs.
    return new Background[] { RUNNER };
  }
  
  
  public Traded[] services() {
    return null;
  }
  
  
  
  /**  Rendering and interface-
    */
  public String fullName() {
    return "Runner Market";
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "runner_market");
  }
  
  
  public String helpInfo() {
    return
      "Runner Markets afford the benefits of black market technology and "+
      "other clandestine services to settlements willing to overlook their "+
      "criminal connections.";
  }
  
  
  public String objectCategory() {
    return InstallTab.TYPE_MERCHANT;
  }
}






