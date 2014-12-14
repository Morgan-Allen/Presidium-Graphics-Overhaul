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
  
  
  public Fabricator(Base base) {
    super(3, 2, ENTRANCE_NORTH, base);
    structure.setupStats(
      125, 2, 200,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    );
    personnel.setShiftType(SHIFTS_BY_DAY);
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
    
    ADVANCED_PLASTICS  = new Upgrade(
      "Advanced Plastics",
      "Speeds the production of standard plastics and functional clothing.",
      200, PLASTICS, 2, null,
      Fabricator.class, ALL_UPGRADES
    ),
    POLYMER_CONVERSION = new Upgrade(
      "Polymer Conversion",
      "Allows for conversion of Carbs to LCHC, and reduces overall squalor.",
      250, CARBS, 1, null,
      Fabricator.class, ALL_UPGRADES
    ),
    FINERY_FLOOR       = new Upgrade(
      "Finery Production",
      "Allows production of fine garments and decor for the upper classes.",
      100, null, 1, ADVANCED_PLASTICS,
      Fabricator.class, ALL_UPGRADES
    ),
    CAMOUFLAGE_FLOOR   = new Upgrade(
      "Camouflage Production",
      "Allows production of stealth-based protection for guerilla agents.",
      150, null, 2, ADVANCED_PLASTICS,
      Fabricator.class, ALL_UPGRADES
    )
    
    //  TODO:  Level 2 Upgrade.
  ;
  
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    
    final int levelPC = structure.upgradeLevel(POLYMER_CONVERSION);
    if (levelPC > 0) stocks.translateDemands(1, CARBS_TO_LCHC, this);
    structure.setAmbienceVal(levelPC - 3);
    
    stocks.incDemand(LCHC, 5, TIER_CONSUMER, 1, this);
    stocks.translateDemands(1, LCHC_TO_PLASTICS, this);
    final float powerNeed = 2 + (structure.numUpgrades() / 2f);
    stocks.forceDemand(POWER, powerNeed, TIER_CONSUMER);
  }
  
  
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null;
    
    final Choice choice = new Choice(actor);
    
    final Manufacture c = stocks.nextManufacture(actor, CARBS_TO_LCHC);
    if (c != null) {
      c.setBonusFrom(this, true, POLYMER_CONVERSION);
      choice.add(c);
    }
    
    final Manufacture m = stocks.nextManufacture(actor, LCHC_TO_PLASTICS);
    if (m != null) {
      m.setBonusFrom(this, false, ADVANCED_PLASTICS);
      choice.add(m);
    }
    
    for (Manufacture o : stocks.specialOrders()) {
      final Traded type = o.made().type;
      if (type == ARTWORKS || type == FINERY) {
        o.setBonusFrom(this, true, FINERY_FLOOR);
      }
      else if (type == STEALTH_SUIT || type == SEALSUIT) {
        o.setBonusFrom(this, true, CAMOUFLAGE_FLOOR);
      }
      else {
        o.setBonusFrom(this, false, ADVANCED_PLASTICS);
      }
      choice.add(o);
    }
    
    if (choice.empty()) choice.add(new Supervision(actor, this));
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
      PLASTICS, OVERALLS, SEALSUIT, STEALTH_SUIT, ARTWORKS, FINERY
    };
  }
  
  
  public Background[] careers() {
    return new Background[] { Backgrounds.FABRICATOR };
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected Traded[] goodsToShow() {
    return new Traded[] { CARBS, LCHC, ARTWORKS, PLASTICS };
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "fabricator");
  }
  
  
  public String fullName() {
    return "Fabricator";
  }
  
  
  public String helpInfo() {
    return
      "The Fabricator manufactures plastics, pressfeed, decor and outfits "+
      "for your citizens.";
  }
  
  
  public String objectCategory() {
    return InstallTab.TYPE_ARTIFICER;
  }
}




