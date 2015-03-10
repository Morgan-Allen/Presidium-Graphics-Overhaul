/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.economic.Economy.*;



public class Fabricator extends Venue {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    Fabricator.class, "media/Buildings/aesthete/fabricator_new.png", 3, 2
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    Fabricator.class, "media/GUI/Buttons/fabricator_button.gif"
  );
  
  final public static Conversion
    LCHC_TO_PLASTICS = new Conversion(
      Fabricator.class, "lchc_to_plastics",
      1, LCHC, TO, 1, PLASTICS,
      ROUTINE_DC, CHEMISTRY, SIMPLE_DC, HANDICRAFTS
    ),
    PLASTICS_TO_DECOR = new Conversion(
      Fabricator.class, "plastics_to_decor",
      2, PLASTICS, TO, 1, DECOR,
      STRENUOUS_DC, GRAPHIC_DESIGN, MODERATE_DC, HANDICRAFTS
    )
  ;
  
  final static VenueProfile PROFILE = new VenueProfile(
    Fabricator.class, "fabricator", "Fabricator",
    3, 2, ENTRANCE_WEST, EngineerStation.PROFILE,
    LCHC_TO_PLASTICS, PLASTICS_TO_DECOR
  );
  
  
  public Fabricator(Base base) {
    super(PROFILE, base);
    structure.setupStats(
      125, 2, 200,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    );
    staff.setShiftType(SHIFTS_BY_DAY);
    attachSprite(MODEL.makeSprite());
  }
  
  
  public Fabricator(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Implementation of employee behaviour-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> ();
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES; }
  final public static Upgrade
    POLYMER_LOOM = new Upgrade(
      "Polymer Loom",
      "Speeds the production of standard plastics and functional clothing.",
      250, Upgrade.THREE_LEVELS, CARBS, 1,
      null, Fabricator.class, ALL_UPGRADES
    ),
    FINERY_FLOOR = new Upgrade(
      "Finery Production",
      "Allows production of fine garments and decor for the upper classes.",
      500, Upgrade.THREE_LEVELS, null, 1,
      POLYMER_LOOM, Fabricator.class, ALL_UPGRADES
    ),
    CAMOUFLAGE_FLOOR = new Upgrade(
      "Camouflage Production",
      "Allows production of stealth-based protection for guerilla agents.",
      350, Upgrade.THREE_LEVELS, null, 2,
      POLYMER_LOOM, Fabricator.class, ALL_UPGRADES
    )
    //  TODO:  Level 2 Upgrade.  And pressfeed?
  ;
  
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    
    final int levelPC = structure.upgradeLevel(POLYMER_LOOM);
    //if (levelPC > 0) stocks.translateDemands(CARBS_TO_LCHC, 1);
    structure.setAmbienceVal(levelPC - 3);
    
    stocks.incDemand(LCHC, 5, Tier.CONSUMER, 1);
    stocks.translateDemands(LCHC_TO_PLASTICS, 1);
    final float powerNeed = 2 + (structure.numUpgrades() / 2f);
    stocks.forceDemand(POWER, powerNeed, Tier.CONSUMER);
  }
  
  
  public Behaviour jobFor(Actor actor, boolean onShift) {
    if (! onShift) return null;
    
    final Choice choice = new Choice(actor);
    /*
    final Manufacture c = stocks.nextManufacture(actor, CARBS_TO_LCHC);
    if (c != null) {
      c.setBonusFrom(this, true, POLYMER_LOOM);
      choice.add(c);
    }
    //*/
    
    final Manufacture m = stocks.nextManufacture(actor, LCHC_TO_PLASTICS);
    if (m != null) {
      m.setBonusFrom(this, false, POLYMER_LOOM);
      choice.add(m);
    }
    
    for (Manufacture o : stocks.specialOrders()) {
      final Traded type = o.made().type;
      if (type == DECOR || type == FINERY) {
        o.setBonusFrom(this, true, FINERY_FLOOR);
      }
      else if (type == STEALTH_SUIT || type == SEALSUIT) {
        o.setBonusFrom(this, true, CAMOUFLAGE_FLOOR);
      }
      else {
        o.setBonusFrom(this, false, POLYMER_LOOM);
      }
      choice.add(o);
    }
    
    if (choice.empty()) choice.add(Supervision.oversight(this, actor));
    return choice.weightedPick();
  }
  
  
  public void addServices(Choice choice, Actor forActor) {
    //  TODO:  Disallow commisions for finery or camouflage if you don't have
    //  the needed upgrades.
    Commission.addCommissions(
      forActor, this, choice,
      OVERALLS, SEALSUIT, STEALTH_SUIT, FINERY
    );
  }
  
  
  public int numOpenings(Background v) {
    int nO = super.numOpenings(v);
    if (v == Backgrounds.FABRICATOR) return nO + 2;
    return 0;
  }
  
  
  public Traded[] services() {
    return new Traded[] {
      PLASTICS, OVERALLS, SEALSUIT, STEALTH_SUIT, DECOR, FINERY
    };
  }
  
  
  public Background[] careers() {
    return new Background[] { Backgrounds.FABRICATOR };
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected Traded[] goodsToShow() {
    return new Traded[] { CARBS, LCHC, DECOR, PLASTICS };
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "fabricator");
  }
  
  
  public String helpInfo() {
    return
      "The Fabricator manufactures plastics, pressfeed, decor and outfits "+
      "for your citizens.";
  }
  
  
  public String objectCategory() {
    return InstallTab.TYPE_ENGINEER;
  }
}




