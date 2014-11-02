


package stratos.game.base;
import org.apache.commons.math3.util.FastMath;

import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.building.Economy.*;



//  I've decided to put this functionality in a separate class for the sake of
//  de-cluttering and headspace.  And for now, I'm limiting myself to the 5
//  basic housing types.


public class HoldingUpgrades {
  

  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
  );
  final public static Upgrade
    TENT_LEVEL = new Upgrade(
      "Seal Tent", "", 0, null, 0, null,
      Holding.class, ALL_UPGRADES
    ),
    PYON_LEVEL = new Upgrade(
      "Pyon Shacks", "", 0, null, 0, null,
      Holding.class, ALL_UPGRADES
    ),
    FREEBORN_LEVEL = new Upgrade(
      "Freeborn Holding", "", 0, null, 0, PYON_LEVEL,
      Holding.class, ALL_UPGRADES
    ),
    CITIZEN_LEVEL = new Upgrade(
      "Citizen Apartment", "", 0, null, 0, FREEBORN_LEVEL,
      Holding.class, ALL_UPGRADES
    ),
    GELDER_LEVEL = new Upgrade(
      "Gelder Manse", "", 0, null, 0, CITIZEN_LEVEL,
      Holding.class, ALL_UPGRADES
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
    LEVEL_GUILDER  = 4,
    NUM_LEVELS     = 5;
  final public static int
    OCCUPANCIES[] = { 4, 4, 4, 4, 4 },
    TAX_LEVELS[]  = { 0, 5, 10, 20, 35 },
    INTEGRITIES[] = { 15, 35, 80, 125, 200 },
    BUILD_COSTS[] = { 25, 60, 135, 225, 350 };
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
      2, PLASTICS, 1, PARTS,
      ROUTINE_DC, ASSEMBLY
    ),
    new Conversion(
      Holding.class, "Level 4",
      2, PLASTICS, 2, PARTS, 1, POWER, 1, MEDICINE,
      MODERATE_DC, ASSEMBLY
    ),
    new Conversion(
      Holding.class, "Level 5",
      2, PLASTICS, 3, PARTS, 2, POWER, 1, MEDICINE, 1, OPEN_WATER, 1, DATALINKS,
      DIFFICULT_DC, ASSEMBLY
    ),
  };
  
  
  public static Upgrade upgradeFor(int upgradeLevel) {
    if (upgradeLevel >= 0 && upgradeLevel <= 4) {
      return UPGRADE_ARRAY[upgradeLevel];
    }
    return null;
  }
  
  
  public static Conversion materials(int upgradeLevel) {
    if (upgradeLevel >= 0 && upgradeLevel <= 4) {
      return MATERIALS[upgradeLevel];
    }
    return null;
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
  
  
  protected static float supportNeed(Holding holding, int upgradeLevel) {
    final float
      population = holding.personnel.residents().size(),
      popDemand  = population * upgradeLevel * 1f / NUM_LEVELS;
    final float
      biomass  = holding.world().ecology().globalBiomass(),
      bioBonus = (float) FastMath.sqrt(biomass) * BIOMASS_SUPPORT;
    return Visit.clamp(popDemand - bioBonus, 0, population);
  }
  
  
  protected static Object checkSupport(
    Holding holding, int upgradeLevel, boolean verbose
  ) {
    //
    //  TODO:  This absence needs to be more prolongued before it becomes a
    //  real problem.
    final float need = supportNeed(holding, upgradeLevel);
    if (holding.stocks.amountOf(LIFE_SUPPORT) < need - 0.5f) {
      if (verbose) return
        "This holding needs more "+LIFE_SUPPORT+" to sustain it's population "+
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
  
  
  //  TODO:  Try to unify these more closely.
  protected static Batch <Item> rationNeeds(Holding holding, int upgradeLevel) {
    final Batch <Item> needed = new Batch <Item> ();
    float foodNeed = holding.personnel.residents().size() * 1.5f;
    float sumFood = 0.1f; for (Traded f : FOOD_TYPES) {
      sumFood += holding.stocks.amountOf(f);
    }
    final float min = 0.1f;
    //
    //  TODO:  Only demand minimal amounts of a given food if it's not strictly
    //  needed for housing evolution...
    for (Traded f : FOOD_TYPES) {
      final float amount = holding.stocks.amountOf(f);
      needed.add(Item.withAmount(f, min + (foodNeed * amount / sumFood)));
    }
    return needed;
  }
  
  
  protected static Object checkRations(
    Holding holding, int upgradeLevel, boolean verbose
  ) {
    if (upgradeLevel == LEVEL_TENT) return NEEDS_MET;
    final boolean NV = ! verbose;
    //
    //  1 unit of food per resident is the standard.  However, you need to have
    //  a reasonable 'balance' of food types, in terms of no less than half an
    //  equal share of the total.
    float foodNeed = holding.personnel.residents().size();
    int numFoods = 0; for (Traded f : FOOD_TYPES) {
      if (holding.stocks.amountOf(f) > 0) numFoods++;
    }
    final float min = foodNeed / (2 * numFoods);
    numFoods = 0; for (Traded f : FOOD_TYPES) {
      if (holding.stocks.amountOf(f) > min) numFoods++;
    }
    //
    //  Now, check against the correct upgrade level.
    if (upgradeLevel >= LEVEL_PYON) {
      if (numFoods < 1) return NV ? NOT_MET :
        "Your pyons need enough of at least one food type before they will "+
        "feel confident about settling down.";
      else return NEEDS_MET;
    }
    if (upgradeLevel >= LEVEL_FREEBORN) {
      if (numFoods < 2) return NV ? NOT_MET :
        "Your freeborn need to have a more varied diet before they will "+
        "consider their claim here settled.";
      else return NEEDS_MET;
    }
    if (upgradeLevel >= LEVEL_CITIZEN) {
      if (numFoods < 2) return NV ? NOT_MET :
        "Your citizens demand a more varied diet as part of their "+
        "modern lifestyle.";
      else return NEEDS_MET;
    }
    if (upgradeLevel >= LEVEL_GUILDER) {
      if (numFoods < 3) return NV ? NOT_MET :
        "Your guilders need at least three food types to satisfy the demands "+
        "of an upper-class lifestyle.";
      else return NEEDS_MET;
    }
    
    return null;
  }
  
  
  
  /**  Venues access-
    */
  protected static Object checkAccess(
    Holding holding, int upgradeLevel, boolean verbose
  ) {
    if (upgradeLevel == LEVEL_TENT) return NEEDS_MET;
    final boolean NV = ! verbose;
    //  TODO:  RESTORE THE LIST OF REQUIREMENTS HERE!  IF NEEDED.
    
    if (upgradeLevel >= LEVEL_PYON) {
      if (
        lacksAccess(holding, Bastion.class         ) &&
        lacksAccess(holding, PhysicianStation.class)
      ) return NV ? NOT_MET :
        "Your pyons will need access to a Bastion or Sickbay to provide "+
        "life support or health services before they will feel safe enough "+
        "to settle down.";
    }
    if (upgradeLevel >= LEVEL_CITIZEN) {
      if (
        lacksAccess(holding, StockExchange.class) &&
        lacksAccess(holding, Cantina.class      )
      ) return NV ? NOT_MET :
        "Your citizens want access to a Cantina or Stock Exchange to allow "+
        "access to luxury goods or services.";
    }
    if (upgradeLevel >= LEVEL_GUILDER) {
      if (
        lacksAccess(holding, Archives.class) &&
        true //lacksAccess(holding, CounselChamber.class)
      ) return NV ? NOT_MET :
        "Your upwardly-mobile gelders require access to an Archives or "+
        "Counsel Chamber for the sake of education or political access.";
    }
    return NEEDS_MET;
  }
  
  
  protected static boolean lacksAccess(Holding holding, Class venueClass) {
    for (Object o : holding.world().presences.matchesNear(
      venueClass, holding, Stage.SECTOR_SIZE * 1.414f
    )) {
      final Venue v = (Venue) o;
      if (v.base() != holding.base() || ! v.structure.intact()) continue;
      return false;
    }
    return true;
  }
  
  
  
  /**  Special good demands, for pressfeed and inscriptions-
    */
  protected static Batch <Item> specialGoods(Holding holding, int targetLevel) {
    final Batch <Item> needed = new Batch <Item> ();
    
    if (targetLevel >= LEVEL_FREEBORN) {
      needed.add(Item.withAmount(PRESSFEED, 1));
    }
    
    if (targetLevel >= LEVEL_GUILDER) {
      needed.add(Item.withAmount(DATALINKS, 1));
    }
    
    return needed;
  }
  
  
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
    return NEEDS_MET;
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
    Holding holding, int upgradeLevel, boolean verbose
  ) {
    final boolean NV = ! verbose;
    
    final Tile t = holding.world().tileAt(holding);
    float safety = 0 - holding.base().dangerMap.sampleAt(t.x, t.y);
    if (holding.stocks.amountOf(PRESSFEED) > 0.5f) safety += 1.5f;
    if (safety < SAFETY_NEEDS[upgradeLevel]) return NV ? NOT_MET :
      "This area feels too unsettled for your subjects' comfort, hindering "+
      "further investment in a life here.";
    
    float ambience = holding.world().ecology().ambience.valueAt(holding);
    ambience += holding.extras().size() / 2f;
    if (ambience < AMBIENCE_NEEDS[upgradeLevel]) return NV ? NOT_MET :
      "The aesthetics of the area could stand improvement before the "+
      "residents will commit to improving their property.";
    
    return NEEDS_MET;
  }
}







