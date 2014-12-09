

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



//  They sell stolen (or manufactured) goods offworld, and import their own
//  specialties.  That's easy to regulate, and makes good sense.

//  TODO:  IMPLEMENT UPGRADES:

//  Silver Geist Lodge
//  IV Punks Lodge
//  Hudzin Baru Lodge

//  ...Once you invest in one gang/clan, you can't invest in others.

//  Silver Geist upgrades:  (+ Geist chips)
//    Cyberware Studio  (Drone hacks or attribute bonuses)
//    Simstims Studio   (VR entertainment)
//    Virtual Currency  (Extra cash and easy loaning)

//  IV Punks upgrades:      (+ Kill contracts)
//    Fast Toxin Lab    (Poison for physical weapons)
//    Slow Burn Lab     (Boosts reflexes, but addictive)
//    Heavy Enforcement (Boost to combat training)

//  Hudzin Baru upgrades:   (+ Gene samples)
//    Cosmetic Clinic   (Disguise or beautify)
//    G-Mod Clinic      (Trait bonus with mutation)
//    Fugitive Trade    (Kidnapping and cheap labour)

//  ...They can offer some of these specialties at the Cantina as well.


//  Sniper Kit.  Sticky Bombs.
//  Night Optics.  Urban Camo.
//  Loaning.  Public Bounties.



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
  //  TODO:  Include corresponding upgrades and techniques for all of these!
  
  final static Traded
    NEURAL_IMPLANT = new Traded(
      RunnerLodge.class, "Neural Implant", null, Economy.FORM_USABLE, 100,
      "Neural implants boost cognitive ability and may allow hacking of "+
      "simple drones and cybrids."
    ),
    KINETIC_IMPLANT = new Traded(
      RunnerLodge.class, "Kinetic Implant", null, Economy.FORM_USABLE, 120,
      "Kinetic implants boost strength and dexterity, together with a degree "+
      "of natural armour."
    ),
    SIMSTIMS = new Traded(
      RunnerLodge.class, "Simstims", null, Economy.FORM_SPECIAL, 45,
      "Simstims provide voyeuristic virtual entertainment to the masses."
    ),
    
    FAST_TOXIN = new Traded(
      RunnerLodge.class, "Fast Toxin", null, Economy.FORM_USABLE, 85,
      "A fast-acting poison suitable for application to melee or kinetic "+
      "weaponry."
    ),
    SLOW_BURN = new Traded(
      RunnerLodge.class, "Slow Burn", null, Economy.FORM_USABLE, 55,
      "An addictive narcotic that greatly accelerates reaction times and "+
      "perception."
    ),
    
    //  Disguise, beauty, and cognitive/sensitive/physical DNA treatments are
    //  reserved for the Hudzin Baru.
    
    //  TODO:  Maybe these should be special abilities for the runner class?
    //  Yes.  Work that out.
    
    SNIPER_KIT = new Traded(
      RunnerLodge.class, "Sniper Kit", null, Economy.FORM_USABLE, 90,
      "Allows ranged attacks at far greater distances, particularly if the "+
      "target is surprised."
    ),
    STICKY_BOMB = new Traded(
      RunnerLodge.class, "Sticky Bomb", null, Economy.FORM_USABLE, 35,
      "Deals heavy damage to vehicles and buildings, if attached at point-"+
      "blank range."
    ),
    NIGHT_OPTICS = new Traded(
      RunnerLodge.class, "Night Optics", null, Economy.FORM_USABLE, 25,
      "Allows extended sight range in space or nocturnal conditions."
    ),
    GHOST_CAMO = new Traded(
      RunnerLodge.class, "Ghost Camo", null, Economy.FORM_USABLE, 40,
      "Improves stealth and cover in daytime or outdoor environments."
    );
  
  
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null;
    
    ///I.say("\nGetting next runner job for "+actor);
    final Choice choice = new Choice(actor);
    //  TODO:  Only loot from distant areas of the city, or from other
    //  settlements- just collect protection money nearby.
    choice.add(Looting.nextLootingFor(actor, this));
    
    return choice.weightedPick();
  }
  
  
  public void addServices(Choice choice, Actor forActor) {
    //  For the IV Punks, add Slow Burn or Fast Toxin purchases.
    //  For the Silver Geist, add Implants surgery or Simstim purchase/recording.
    //  For the Hudzin Baru, add Disguise or G-Mods purchases/surgery.
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates);
    
    //  Demand either parts or reagents, depending on what you're making.
    //  Register as a producer of whatever you're making.
  }
  
  
  public Traded[] services() {
    return null;
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






