


package stratos.game.civic;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.economic.Economy.*;



//  I've decided to put this functionality in a separate class for the sake of
//  de-cluttering and headspace.  And for now, I'm limiting myself to the 5
//  basic housing types.


public class HoldingUpgrades {
  

  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
  );
  final public static Upgrade
    TENT_LEVEL = new Upgrade(
      "Seal Tent", "", 0, Upgrade.THREE_LEVELS, null, 0,
      null, Holding.class, ALL_UPGRADES
    ),
    PYON_LEVEL = new Upgrade(
      "Pyon Shacks", "", 0, Upgrade.THREE_LEVELS, null, 0,
      null, Holding.class, ALL_UPGRADES
    ),
    FREEBORN_LEVEL = new Upgrade(
      "Freeborn Holding", "", 0, Upgrade.THREE_LEVELS, null, 0,
      PYON_LEVEL, Holding.class, ALL_UPGRADES
    ),
    CITIZEN_LEVEL = new Upgrade(
      "Citizen Apartment", "", 0, Upgrade.THREE_LEVELS, null, 0,
      FREEBORN_LEVEL, Holding.class, ALL_UPGRADES
    ),
    GELDER_LEVEL = new Upgrade(
      "Gelder Manse", "", 0, Upgrade.THREE_LEVELS, null, 0,
      CITIZEN_LEVEL, Holding.class, ALL_UPGRADES
    ),
    UPGRADE_ARRAY[] = Upgrade.upgradesFor(Holding.class);
  
  final static Object
    NEEDS_MET = "OKAY",
    NOT_MET   = "NOT OKAY";
  final public static int
    LEVEL_TENT     = 0,
    LEVEL_PYON     = 1,
    LEVEL_FREEBORN = 2,
    LEVEL_CITIZEN  = 3,
    LEVEL_GELDER  = 4,
    NUM_LEVELS     = 5;
  final public static int
    OCCUPANCIES[] = { 4, 4, 4, 4, 4 },
    INTEGRITIES[] = { 15, 35, 80, 125, 200 },
    AMBIENCES  [] = { -2, -1, 0, 2, 4 },
    BUILD_COSTS[] = { 25, 60, 135, 225, 350 },
    TAX_PER_DAY[] = { 0, 5, 15, 35, 75 };
  final static float
    BIOMASS_SUPPORT = 5;
  
  final static String LEVEL_NAMES[] = {
    
    "Dreg Towers",
    "Scavenger Slums",
    "Seal Tent",
    
    "Pyon Shacks",
    "Freeborn Holding",
    "Citizen Apartment",
    "Gelder Manse",
    
    "Knighted Estate",
    "Highborn Villa",
    "Forbidden Palace"
  };
  
  /**  Building materials/furnishings-
    */
  final static Conversion MATERIALS[] = {
    new Conversion(
      Holding.class, "Level 1",
      TRIVIAL_DC, ASSEMBLY
    ),
    new Conversion(
      Holding.class, "Level 2",
      1, PLASTICS,
      SIMPLE_DC, ASSEMBLY
    ),
    new Conversion(
      Holding.class, "Level 3",
      2, PLASTICS, 1, PARTS, 1, POWER,
      ROUTINE_DC, ASSEMBLY
    ),
    new Conversion(
      Holding.class, "Level 4",
      2, PLASTICS, 2, PARTS, 1, POWER, 1, MEDICINE,
      MODERATE_DC, ASSEMBLY
    ),
    new Conversion(
      Holding.class, "Level 5",
      2, PLASTICS, 3, PARTS, 2, POWER, 1, MEDICINE, 1, WATER, 1, DATALINKS,
      DIFFICULT_DC, ASSEMBLY
    ),
  };
  
  
  private static boolean checks(int targetLevel, int upgradeLevel) {
    if (GameSettings.freeHousingLevel >= upgradeLevel) return false;
    return (targetLevel >= upgradeLevel);
  }
  
  
  
  
  public static Upgrade upgradeFor(int upgradeLevel) {
    if (upgradeLevel >= 0 && upgradeLevel <= 4) {
      return UPGRADE_ARRAY[upgradeLevel];
    }
    return null;
  }
  
  
  public static Conversion materials(int upgradeLevel) {
    return MATERIALS[Nums.clamp(upgradeLevel, NUM_LEVELS)];
  }
  
  
  protected static Object checkMaterials(
    Holding holding, int upgradeLevel, boolean verbose
  ) {
    final Conversion materials = materials(upgradeLevel);
    
    for (Item needed : materials.raw) {
      if (holding.stocks.amountOf(needed) < needed.amount - 0.5f) {
        if (! verbose) return NOT_MET;
        if (upgradeLevel > holding.upgradeLevel()) return
          "This holding needs more "+needed.type+" before further development "+
          "can proceed.";
        else return
          "This holding is short of "+needed.type+", needed for maintenance "+
          "and repairs.";
      }
    }
    return NEEDS_MET;
  }
  
  
  protected static float supportNeed(Holding holding, int targetLevel) {
    final float
      population = holding.staff.lodgers().size(),
      popDemand  = population * targetLevel * 1f / NUM_LEVELS,
      biomass    = holding.world().ecology().globalBiomass(),
      bioBonus   = Nums.sqrt(biomass) * BIOMASS_SUPPORT,
      need       = Nums.clamp(popDemand - bioBonus, 0, population);
    return need;
  }
  
  
  protected static Object checkSupport(
    Holding holding, int targetLevel, boolean verbose
  ) {
    final float need = supportNeed(holding, targetLevel);
    if (holding.stocks.amountOf(ATMO) < need - 0.5f) {
      if (verbose) return
        "This holding needs more "+ATMO+" to sustain it's population "+
        "comfortably.";
      return NOT_MET;
    }
    else return NEEDS_MET;
  }
  
  
  
  /**  Rations/foodstuffs-
    */
  final static Traded FOOD_TYPES[] = {
    CARBS, PROTEIN, GREENS, MEDICINE
  };
  final static int LEVEL_TYPES_NEEDED[] = {
    0, 1, 2, 2, 3
  };
  
  
  //  TODO:  Try to unify these more closely.
  protected static Batch <Item> rationNeeds(Holding holding, int upgradeLevel) {
    
    //  TODO:  Get pricing-levels for the base as a whole, and use that to
    //  adjust what's demanded?
    final int typesNeeded = LEVEL_TYPES_NEEDED[upgradeLevel];
    final float foodNeed = holding.staff.lodgers().size() * 1.5f;
    
    final Batch <Item> needed = new Batch <Item> ();
    for (Traded type : FOOD_TYPES) {
      if (needed.size() >= typesNeeded) break;
      float access = rateAccessTo(type.supplyKey, holding, holding.base());
      if (access <= 0) continue;
      needed.add(Item.withAmount(type, foodNeed / typesNeeded));
    }
    
    return needed;
  }
  
  
  protected static Object checkRations(
    Holding holding, int targetLevel, boolean verbose
  ) {
    if (targetLevel == LEVEL_TENT) return NEEDS_MET;
    final boolean NV = ! verbose;
    //
    //  1 unit of food per resident is the standard.  However, you need to have
    //  a reasonable 'balance' of food types, in terms of no less than half an
    //  equal share of the total.
    float foodNeed = holding.staff.lodgers().size();
    int numFoods = 0; for (Traded f : FOOD_TYPES) {
      if (holding.stocks.amountOf(f) > 0) numFoods++;
    }
    final float min = foodNeed / (2 * numFoods);
    numFoods = 0; for (Traded f : FOOD_TYPES) {
      if (holding.stocks.amountOf(f) > min) numFoods++;
    }
    //
    //  Now, check against the correct upgrade level.
    if (checks(targetLevel, LEVEL_PYON)) {
      if (numFoods < LEVEL_TYPES_NEEDED[1]) return NV ? NOT_MET :
        "Your pyons need enough of at least one food type before they will "+
        "feel confident about settling down.";
      else return NEEDS_MET;
    }
    if (checks(targetLevel, LEVEL_FREEBORN)) {
      if (numFoods < LEVEL_TYPES_NEEDED[2]) return NV ? NOT_MET :
        "Your freeborn need to have a more varied diet before they will "+
        "consider their claim here settled.";
      else return NEEDS_MET;
    }
    if (checks(targetLevel, LEVEL_CITIZEN)) {
      if (numFoods < LEVEL_TYPES_NEEDED[3]) return NV ? NOT_MET :
        "Your citizens demand a more varied diet as part of their "+
        "modern lifestyle.";
      else return NEEDS_MET;
    }
    if (checks(targetLevel, LEVEL_GELDER)) {
      if (numFoods < LEVEL_TYPES_NEEDED[4]) return NV ? NOT_MET :
        "Your guilders need at least three food types to satisfy the demands "+
        "of an upper-class lifestyle.";
      else return NEEDS_MET;
    }
    
    return null;
  }
  

  protected static Object checkAccess(
    Holding holding, int targetLevel, boolean verbose
  ) {
    return checkAccess(holding, holding, targetLevel, verbose);
  }
  
  
  
  /**  Venues access-
    */
  final static float ACCESS_RANGE = Stage.SECTOR_SIZE * 1.414f;
  
  
  protected static float rateAccessFrom(
    Target point, int targetLevel, Base base
  ) {
    final Object report = reportAccessFrom(point, targetLevel, base, false);
    if (report instanceof Float) return (Float) report;
    else return 0;
  }
  
  
  protected static Object checkAccess(
    Holding holding, Target point, int targetLevel, boolean verbose
  ) {
    if (targetLevel == LEVEL_TENT) return NEEDS_MET;
    final Object report = reportAccessFrom(
      point, targetLevel, holding.base(), verbose
    );
    if (report instanceof Float) return NEEDS_MET;
    else return report;
  }
  
  
  private static Object reportAccessFrom(
    Target point, int targetLevel, Base base, boolean verbose
  ) {
    final boolean NV = ! verbose;
    float rating = 1, r;
    
    if (targetLevel >= LEVEL_PYON) {
      r = Nums.max(
        rateAccessTo(Bastion.class         , point, base),
        rateAccessTo(PhysicianStation.class, point, base)
      );
      if (r <= 0) return NV ? NOT_MET :
        "Your pyons will need access to a Bastion or Sickbay to provide "+
        "life support or health services before they will feel safe enough "+
        "to settle down.";
      rating *= r;
    }
    
    if (targetLevel >= LEVEL_CITIZEN) {
      r = Nums.max(
        rateAccessTo(Cantina.class         , point, base),
        rateAccessTo(StockExchange.class   , point, base)
      );
      if (r <= 0) return NV ? NOT_MET :
        "Your citizens want access to a Cantina or Stock Exchange to allow "+
        "access to luxury goods or services.";
      rating *= r;
    }
    
    if (targetLevel >= LEVEL_GELDER) {
      r = Nums.max(
        rateAccessTo(Archives.class         , point, base),
        0//rateAccessTo(CounselChamber.class, point, base)
      );
      if (r <= 0) return NV ? NOT_MET :
        "Your upwardly-mobile gelders require access to an Archives or "+
        "Counsel Chamber for the sake of education or political access.";
      rating *= r;
    }
    
    if (rating <= 0) return NOT_MET;
    return (Float) rating;
  }
  
  
  protected static float rateAccessTo(Object service, Target point, Base base) {
    for (Object o : point.world().presences.matchesNear(
      service, point, ACCESS_RANGE
    )) {
      final Venue v = (Venue) o;
      if (v.base() != base || ! v.structure.intact()) continue;
      final float dist = Spacing.distance(point, v.mainEntrance());
      return Nums.clamp(1 - (dist / ACCESS_RANGE), 0, 1);
    }
    return 0;
  }
  
  
  
  /**  Ambience requirements-
    */
  final static int SAFETY_NEEDS[] = {
    -20,
    -5,
    -2,
    0,
    2
  };
  final static int AMBIENCE_NEEDS[] = {
    -15,
    -5,
    -2,
    2,
    5
  };
  
  
  protected static Object checkSurrounds(
    Holding holding, int targetLevel, boolean verbose
  ) {
    final boolean NV = ! verbose;
    if (targetLevel <= GameSettings.freeHousingLevel) return NEEDS_MET;
    
    final Tile t = holding.world().tileAt(holding);
    float safety = 0 - holding.base().dangerMap.sampleAround(
      t.x, t.y, Stage.SECTOR_SIZE
    );
    if (holding.stocks.amountOf(PRESSFEED) > 0.5f) safety += 1.5f;
    if (safety < SAFETY_NEEDS[targetLevel]) return NV ? NOT_MET :
      "This area feels too unsettled for your subjects' comfort, hindering "+
      "further investment in a life here.";
    
    float ambience = holding.world().ecology().ambience.valueAt(holding);
    ambience += holding.extras().size() / 2f;
    if (ambience < AMBIENCE_NEEDS[targetLevel]) return NV ? NOT_MET :
      "The aesthetics of the area could stand improvement before the "+
      "residents will commit to improving their property.";
    
    return NEEDS_MET;
  }
}








/**  Special good demands, for pressfeed and inscriptions-
  */
/*
protected static Batch <Item> specialGoods(Holding holding, int targetLevel) {
  final Batch <Item> needed = new Batch <Item> ();
  
  if (checks(targetLevel, LEVEL_FREEBORN)) {
    needed.add(Item.withAmount(PRESSFEED, 1));
  }
  
  if (checks(targetLevel, LEVEL_GUILDER)) {
    needed.add(Item.withAmount(DATALINKS, 1));
  }
  
  return needed;
}
//*/
/*

protected static Object checkSpecial(
  Holding holding, int upgradeLevel, boolean verbose
) {
  /*
  if (upgradeLevel >= LEVEL_FREEBORN) {
    if (verbose && holding.stocks.shortagePenalty(PRESSFEED) > 0) {
      return
        "Your freeborn would like a supply of pressfeed to keep up to "+
        "date with current events.  An Audit Office should supply this.";
    }
  }
  if (upgradeLevel >= LEVEL_GUILDER) {
    if (holding.stocks.shortagePenalty(DATALINKS) > 0) {
      return (! verbose) ? NOT_MET :
        "Your wealthy guilders need access to the privileged information "+
        "that resides in an Archives' encrypted datalinks.";
    }
  }
  //*/
  //return NEEDS_MET;
//}


