/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.civic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.maps.Siting;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.content.civic.HoldingUpgrades.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.craft.Economy.*;



//  Requirements come under three headings-
//  Building materials (parts, plastics, inscriptions, decor.)
//  Health and Entertainment (averaged over all occupants.)
//  Safety and Ambience (by location.)
//
//  TODO:  Allow actors to study, relax, make love, play with kids, etc. at
//  home.

//  TODO:  Massive mega-block tower apartments need to operate under different
//  rules, I think.  Save that for expansions with hyperstructures built in.


public class Holding extends Venue {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  private static boolean
    verbose     = false,
    rateVerbose = false;

  final static String
    IMG_DIR = "media/Buildings/civilian/";
  
  final static ImageAsset ICONS[] = ImageAsset.fromImages(
    Holding.class, "media/GUI/Buttons/",
    "housing0_button.gif",
    "housing1_button.gif",
    "housing2_button.gif",
    "housing3_button.gif",
    "housing4_button.gif"
  );
  final public static ModelAsset
    SEAL_TENT_MODEL = CutoutModel.fromImage(
      Holding.class, IMG_DIR+"field_tent.png", 2, 2
    ),
    LOWER_CLASS_MODELS[][] = CutoutModel.fromImageGrid(
      Holding.class, IMG_DIR+"lower_class_housing.png",
      3, 3, 2, 1, false
    ),
    MIDDLE_CLASS_MODELS[][] = CutoutModel.fromImageGrid(
      Holding.class, IMG_DIR+"middle_class_housing.png",
      3, 3, 2, 1, false
    ),
    UPPER_CLASS_MODELS[][] = null;
  
  final public static int
    MAX_SIZE   = 2,
    MAX_HEIGHT = 4,
    NUM_VARS   = 3;
  final static float
    CHECK_INTERVAL = 10,
    TEST_INTERVAL  = Stage.STANDARD_DAY_LENGTH,
    UPGRADE_THRESH = 0.66f,
    DEVOLVE_THRESH = 0.66f;
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    Holding.class, "housing",
    "Housing", Target.TYPE_PHYSICIAN, ICONS[0],
    "Housing provides comfort, sanitation and other domestic benefits to "+
    "your subjects.",
    2, 1, Structure.IS_PUBLIC,
    Owner.TIER_PRIVATE, INTEGRITIES[0],
    5
  );
  
  final public static Upgrade
    TENT_LEVEL = new Upgrade(
      "Seal Tent", "",
      20, Upgrade.THREE_LEVELS, null, Holding.BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null
    ),
    PYON_LEVEL = new Upgrade(
      "Pyon Shacks", "",
      0, Upgrade.THREE_LEVELS, null, Holding.BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null
    ),
    FREEBORN_LEVEL = new Upgrade(
      "Freeborn Holding", "",
      0, Upgrade.THREE_LEVELS, PYON_LEVEL, Holding.BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null
    ),
    CITIZEN_LEVEL = new Upgrade(
      "Citizen Apartment", "",
      0, Upgrade.THREE_LEVELS, FREEBORN_LEVEL, Holding.BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null
    ),
    GELDER_LEVEL = new Upgrade(
      "Gelder Manse", "",
      0, Upgrade.THREE_LEVELS, CITIZEN_LEVEL, Holding.BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null
    ),
    UPGRADE_ARRAY[] = Holding.BLUEPRINT.assignVenueLevels(
      TENT_LEVEL, PYON_LEVEL, FREEBORN_LEVEL, CITIZEN_LEVEL, GELDER_LEVEL
    );
  
  
  final static Conversion
    PROVIDE_HOUSING = new Conversion(
      BLUEPRINT, "provide_housing",
      TO, 4, SERVICE_HOUSING
    )
  ;
  
  final static Conversion MATERIALS[] = {
    new Conversion(
      Holding.BLUEPRINT, "holding_level_1",
      TRIVIAL_DC, ASSEMBLY
    ).attachName("Level 1"),
    new Conversion(
      Holding.BLUEPRINT, "holding_level_2",
      1, PLASTICS,
      SIMPLE_DC, ASSEMBLY
    ).attachName("Level 2"),
    new Conversion(
      Holding.BLUEPRINT, "holding_level_3",
      2, PLASTICS, 1, PARTS, 1, POWER,
      ROUTINE_DC, ASSEMBLY
    ).attachName("Level 3"),
    new Conversion(
      Holding.BLUEPRINT, "holding_level_4",
      2, PLASTICS, 2, PARTS, 1, POWER, 1, MEDICINE,
      MODERATE_DC, ASSEMBLY
    ).attachName("Level 4"),
    new Conversion(
      Holding.BLUEPRINT, "holding_level_5",
      2, PLASTICS, 3, PARTS, 2, POWER, 1, MEDICINE, 1, WATER, 1, DATALINKS,
      DIFFICULT_DC, ASSEMBLY
    ).attachName("Level 5"),
  };
  
  
  private int varID, currentLevel = 0;
  private int numTests = 0, upgradeCounter, devolveCounter;
  
  
  
  public Holding(Base belongs) {
    super(BLUEPRINT, belongs);
    //this.upgradeLevel = 0;
    this.varID = Rand.index(NUM_VARS);
    attachSprite(modelFor(this).makeSprite());
  }
  
  
  public Holding(Session s) throws Exception {
    super(s);
    currentLevel   = s.loadInt();
    varID          = s.loadInt();
    numTests       = s.loadInt();
    upgradeCounter = s.loadInt();
    devolveCounter = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt(currentLevel  );
    s.saveInt(varID         );
    s.saveInt(numTests      );
    s.saveInt(upgradeCounter);
    s.saveInt(devolveCounter);
  }
  
  
  
  
  /**  Upgrade listings-
    */
  final static Siting SITING = new Siting(BLUEPRINT) {
    
    public float rateSettlementDemand(Base base) {
      return base.demands.globalShortage(SERVICE_HOUSING, false);
    }
    
    public float ratePointDemand(
      Base base, Target point, boolean exact, int claimRadius
    ) {
      final boolean report = rateVerbose && BaseUI.currentPlayed() == base;
      float baseDemand = base.demands.globalShortage(SERVICE_HOUSING, false);
      final Base claims = point.world().claims.baseClaiming(point);
      
      if (report) {
        I.say("\nGetting place-rating for Holding at "+point);
        I.say("  Current base: "+base+", base claiming area: "+claims);
        I.say("  Demand for housing: "+baseDemand);
      }
      if (baseDemand <= 0 || claims != base) return -1;
      float rating = 1;
      
      if (exact) {
        final Tile at = (Tile) point;
        final float range = Stage.ZONE_SIZE;
        Target near = null;
        
        near = at.world.presences.nearestMatch(base, at, range);
        if (near != null) rating *= 1f / (1 + Spacing.distance(near, at));
        
        near = at.world.presences.nearestMatch(SERVICE_HOUSING, at, range);
        if (near != null) rating *= 1f / (1 + Spacing.distance(near, at));
      }
      else {
        for (int level = 0 ; level < NUM_LEVELS; level++) {
          final float access = rateAccessFrom(point, level, base);
          if (access <= 0) break;
          rating *= 1 + access;
        }
        final float maxRating = 1 << NUM_LEVELS;
        if (report) I.say("  Rating from level: "+rating+"/"+maxRating);
        rating = baseDemand * Plan.PARAMOUNT * rating / maxRating;
      }
      
      if (report) I.say("  Final rating: "+rating);
      return rating;
    }
  };
  
  //  What you actually need here is a supplyFor(Object service) method- that
  //  way you can measure precisely how much of that service you allow for.
  
  
  protected void impingeSupply(boolean onEntry) {
    super.impingeSupply(onEntry);
    final int maxHoused = OCCUPANCIES[currentLevel];
    final int period = onEntry ? -1 : 1;
    base.demands.impingeSupply(SERVICE_HOUSING, maxHoused, period, this);
  }

  
  
  
  /**  Moderating upgrades-
    */
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    
    if (instant || ! structure.intact()) return;
    int upgradeLevel = 0;
    
    for (int i = UPGRADE_ARRAY.length; i-- > 0;) {
      Upgrade u = UPGRADE_ARRAY[i];
      if (structure.hasUpgrade(u)) { upgradeLevel = i; break; }
    }
    
    consumeMaterials(currentLevel);
    updateDemands(currentLevel + 1);
    stocks.updateStockDemands(1, NO_GOODS);
    impingeSqualor(currentLevel);

    final int CHECK_TIME = 10;
    if (numUpdates % CHECK_TIME == 0) {
      int targetLevel = checkTargetLevel(CHECK_TIME, currentLevel);
      if (targetLevel != -1) {
        final Object HU[] = UPGRADE_ARRAY;
        if (targetLevel > currentLevel) {
          final Upgrade target = (Upgrade) HU[targetLevel ];
          structure.beginUpgrade(target, true);
        }
        else {
          final Upgrade target = (Upgrade) HU[currentLevel];
          structure.resignUpgrade(target, false);
        }
      }
    }
    
    if (
      (upgradeLevel != currentLevel) &&
      (! structure.needsUpgrade()) &&
      structure.goodCondition()
    ) {
      currentLevel = upgradeLevel;
      structure.updateStats(INTEGRITIES[upgradeLevel], 5, 0);
      world.ephemera.addGhost(this, MAX_SIZE, sprite(), 2.0f, 1);
      attachModel(modelFor(this));
      refreshIncept(world, false);
    }
  }
  
  
  public int upgradeLevel() {
    return currentLevel;
  }
  
  
  private boolean needsMet(int meetLevel) {
    if (staff.lodgers().size() == 0) return false;
    if (meetLevel <= LEVEL_TENT  ) return true ;
    if (meetLevel >  LEVEL_GELDER) return false;
    final Object met = NEEDS_MET;
    return
      checkAccess   (this, meetLevel, false) == met &&
      checkMaterials(this, meetLevel, false) == met &&
      checkSupport  (this, meetLevel, false) == met &&
      checkRations  (this, meetLevel, false) == met &&
      checkSurrounds(this, meetLevel, false) == met;
  }
  
  
  private int checkTargetLevel(int CHECK_TIME, int upgradeLevel) {
    
    boolean devolve = false, upgrade = false;
    if      (! needsMet(upgradeLevel    )) devolve = true;
    else if (  needsMet(upgradeLevel + 1)) upgrade = true;
    
    final boolean empty = staff.lodgers().size() == 0;
    if (empty) { devolve = true; upgrade = false; }
    
    numTests += CHECK_TIME;
    if (devolve) devolveCounter += CHECK_TIME;
    if (upgrade) upgradeCounter += CHECK_TIME;
    
    int targetLevel = upgradeLevel;
    final boolean freeUp = GameSettings.freeHousingLevel > upgradeLevel;
    
    if (numTests >= TEST_INTERVAL || freeUp) {
      targetLevel = upgradeLevel;
      if (devolveCounter * 1f / numTests > DEVOLVE_THRESH) devolve = true;
      if (upgradeCounter * 1f / numTests > UPGRADE_THRESH) upgrade = true;
      if (devolve) targetLevel--;
      if (upgrade) targetLevel++;
      
      if (freeUp) {
        targetLevel = upgradeLevel + 1;
        upgrade = true;
        devolve = false;
      }
      numTests = devolveCounter = upgradeCounter = 0;
      targetLevel = Nums.clamp(targetLevel, NUM_LEVELS);
      
      if (verbose && I.talkAbout == this) {
        if (numTests == 0) I.say("HOUSING TEST INTERVAL COMPLETE");
        I.say("Upgrade/Target levels: "+upgradeLevel+"/"+targetLevel);
        I.say("Could upgrade? "+upgrade+", devolve? "+devolve);
        I.say("Is Empty? "+empty);
      }
      return targetLevel;
    }
    return -1;
  }
  
  
  private void consumeMaterials(int upgradeLevel) {
    //
    //  Decrement stocks and update demands-
    float wear = 1f / (GameSettings.ITEM_WEAR_DAYS * Stage.STANDARD_DAY_LENGTH);
    final int maxPop = OCCUPANCIES[upgradeLevel];
    float count = 0;
    for (Actor r : staff.lodgers()) if (r.aboard() == this) count++;
    count = 0.5f + (count / maxPop);
    
    //  If upgrades are free, make sure it includes rations:
    int maxFree = Nums.clamp(GameSettings.freeHousingLevel, upgradeLevel + 1);
    for (Item i : rationNeeds(this, maxFree)) {
      stocks.setAmount(i.type, i.amount + 1);
    }
    for (Item i : HoldingUpgrades.materials(maxFree).raw) {
      stocks.setAmount(i.type, i.amount + 1);
    }
    
    //  Power, water and life support are consumed at a fixed rate, but other
    //  materials wear out depending on use (and more slowly.)
    for (Item i : HoldingUpgrades.materials(upgradeLevel).raw) {
      if (i.type.form == FORM_PROVISION) continue;
      stocks.bumpItem(i.type, i.amount * count * -wear);
    }
  }
  
  
  private void impingeSqualor(int upgradeLevel) {
    float ambience = AMBIENCES[upgradeLevel];
    structure.setAmbienceVal(ambience);
  }
  
  
  private void updateDemands(int targetLevel) {
    targetLevel = Nums.clamp(targetLevel, NUM_LEVELS);
    stocks.clearDemands();
    
    float materialNeed = 1f / GameSettings.ITEM_WEAR_DAYS;
    float rationNeed   = ActorHealth.FOOD_PER_DAY;
    
    for (Item i : HoldingUpgrades.materials(targetLevel).raw) {
      stocks.forceDemand(i.type, i.amount + 0.5f, 0);
      stocks.setDailyDemand(i.type, i.amount * materialNeed, 0);
    }
    
    for (Item i : rationNeeds(this, targetLevel)) {
      stocks.forceDemand(i.type, i.amount, 0);
      stocks.setDailyDemand(i.type, (i.amount - 0.5f) * rationNeed, 0);
    }
    
    final float supportNeed = supportNeed(this, targetLevel);
    stocks.forceDemand(ATMO, supportNeed, 0);
  }
  
  
  public Traded[] goodsNeeded() {
    
    final Batch <Traded> needed = new Batch <Traded> ();
    int targetLevel = currentLevel + 1;
    targetLevel = Nums.clamp(targetLevel, NUM_LEVELS);
    
    //  Combine the listing of non-provisioned materials and demand for rations.
    //  (Note special goods, like pressfeed and datalinks, are delivered to the
    //  holding externally, and so are not included here.)
    for (Item i : HoldingUpgrades.materials(targetLevel).raw) {
      if (i.type.form == FORM_PROVISION) continue;
      needed.add(i.type);
    }
    for (Item i : rationNeeds(this, targetLevel)) {
      needed.add(i.type);
    }
    return needed.toArray(Traded.class);
  }
  
  
  public void addTasks(Choice choice, Actor actor, Background background) {
    final boolean report = I.talkAbout == this && verbose;
    
    if (background == Backgrounds.AS_RESIDENT && structure.intact()) {
      //
      //  Otherwise, see if it's possible to make any purchases nearby-
      final Batch <Venue> salePoints = new Batch <Venue> ();
      world.presences.sampleFromMap(
        this, world, 5, salePoints, Economy.SERVICE_COMMERCE
      );
      
      final Choice buying = new Choice(actor) {
        public boolean add(Behaviour b) {
          if (b instanceof Bringing) {
            if (report) I.say("  Delivery is: "+b);
            return super.add(b);
          }
          else return false;
        }
      };
      buying.isVerbose = report;
      
      if (report) {
        I.say("\nGetting next upkeep task for "+actor);
        I.say("  Sale points are: ");
        for (Venue v : salePoints) I.say("    "+v);
      }
      for (Venue v : salePoints) {
        v.addTasks(buying, actor, Backgrounds.AS_VISITOR);
      }
      
      choice.add(buying.weightedPick());
    }
    else super.addTasks(choice, actor, background);
  }
  
  
  protected Behaviour jobFor(Actor actor) {
    //  TODO:  Include special orders for servants/minders?
    return null;
  }
  
  
  public float crowdRating(Actor actor, Background background) {
    if (background == Backgrounds.AS_RESIDENT) {
      final int maxPop = OCCUPANCIES[currentLevel];
      return staff.lodgers().size() * 1f / maxPop;
    }
    else return super.crowdRating(actor, background);
  }
  
  
  public Background[] careers () { return null; }
  public Traded[]     services() { return null; }
  
  
  
  /**  Rendering and interface methods-
    */
  private static ModelAsset modelFor(Holding holding) {
    final int level = holding.currentLevel, VID = holding.varID;
    if (level == 0) return SEAL_TENT_MODEL;
    if (level <= 1) {
      return LOWER_CLASS_MODELS[VID][level + 1];
    }
    if (level >= 5) return UPPER_CLASS_MODELS[VID][level - 5];
    return MIDDLE_CLASS_MODELS[VID][level - 2];
  }
  
  
  public String fullName() {
    while (staff.lodgers().size() > 0) {
      Actor owns = staff.lodgers().first();
      if (owns.fullName() == null) break;
      final String prefix = (String) Visit.last(owns.fullName().split(" "));
      if (prefix == null) break;
      return prefix+"'s "+LEVEL_SUFFIX[currentLevel + 2];
    }
    return LEVEL_NAMES[currentLevel + 2];
  }
  
  
  protected boolean showHoverStockIcons() {
    return true;
  }
  
  
  public Composite portrait(HUD UI) {
    return Composite.withImage(ICONS[currentLevel], "holding"+currentLevel);
  }
  
  
  public String helpInfo() {
    if (inWorld()) {
      final String
        uS = needMessage(currentLevel    ),
        tS = needMessage(currentLevel + 1);
      if (uS != null) return uS;
      if (tS != null) return tS;
    }
    return super.helpInfo();
  }
  
  
  //  TODO:  Merge this with the needsMet method above!
  private String needMessage(int meetLevel) {
    meetLevel = Nums.clamp(meetLevel, NUM_LEVELS);
    final Object met = NEEDS_MET;
    final Object
      access    = checkAccess   (this, meetLevel, true),
      materials = checkMaterials(this, meetLevel, true),
      support   = checkSupport  (this, meetLevel, true),
      rations   = checkRations  (this, meetLevel, true),
      surrounds = checkSurrounds(this, meetLevel, true);
    if (access    != met) return (String) access   ;
    if (materials != met) return (String) materials;
    if (support   != met) return (String) support  ;
    if (rations   != met) return (String) rations  ;
    if (surrounds != met) return (String) surrounds;
    return null;
  }
}








