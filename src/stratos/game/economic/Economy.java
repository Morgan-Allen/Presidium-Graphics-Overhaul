/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.economic;
import stratos.util.*;



public final class Economy {
  
  
  private Economy() {}
  final static Class <Economy> BC = Economy.class;
  
  final public static int
    FORM_RESOURCE  = 0,
    FORM_MATERIAL  = 1,
    FORM_PROVISION = 2,
    FORM_SERVICE   = 3,
    FORM_DEVICE    = 4,
    FORM_OUTFIT    = 5,
    FORM_USED_ITEM = 6,
    FORM_SPECIAL   = 7;
  
  
  final static Table <Traded, Integer> CATEGORY_TABLE, CT;
  static { CATEGORY_TABLE = CT = new Table <Traded, Integer> (); }
  final public static int
    CATEGORY_OTHER    = -1,
    CATEGORY_FOOD     =  0,
    CATEGORY_SPYCE    =  1,
    CATEGORY_DRUG     =  2,
    CATEGORY_MINERAL  =  3,
    CATEGORY_WARES    =  4,
    CATEGORY_SECURED  =  5;  //  TODO:  Merge with the Forms above?
  private static Traded[] label(int ID, Traded... members) {
    for (Traded t : members) CT.put(t, ID);
    return members;
  }
  public static int categoryFor(Traded service) {
    final Integer i = CT.get(service);
    return i == null ? CATEGORY_OTHER : i;
  }
  
  
  final public static Traded
    MINERALS   = new Traded(BC, "Minerals"  , null, FORM_RESOURCE, 0),
    MOISTURE   = new Traded(BC, "Moisture"  , null, FORM_RESOURCE, 0),
    INSOLATION = new Traded(BC, "Insolation", null, FORM_RESOURCE, 0),
    LAND_AREA  = new Traded(BC, "Land Area" , null, FORM_RESOURCE, 0);
  final public static Traded
    ALL_RESOURCES[] = Traded.INDEX.soFar(Traded.class);
  
  
  final public static Traded
    CARBS = new Traded(
      BC, "Carbs"    , "carbs.gif"    , FORM_MATERIAL, 10,
      "Carbohydrate-based and oily foodstuffs"
    ),
    GREENS = new Traded(
      BC, "Greens"   , "greens.gif"   , FORM_MATERIAL, 40,
      "Fresh vegetable and fibrous foodstuffs"
    ),
    PROTEIN = new Traded(
      BC, "Protein"  , "protein.gif"  , FORM_MATERIAL, 20,
      "Game meat and cultured yeast foodstuffs"
    ),
    //  Rations
    ALL_FOOD_TYPES[] = label(CATEGORY_FOOD, CARBS, GREENS, PROTEIN),

    REAGENTS = new Traded(
      BC, "Reagents" , "reagents.gif" , FORM_MATERIAL, 40,
      "Chemical ingredients and metabolic tracers used in lab work"
    ),
    SOMA = new Traded(
      BC, "Soma"     , "soma.gif"     , FORM_MATERIAL, 60,
      "A mild recreational narcotic with minimal side-effects"
    ),
    MEDICINE = new Traded(
      BC, "Medicine" , "medicine.gif" , FORM_MATERIAL, 120,
      "Drugs and supplements tailored to treat common diseases"
    ),
    ALL_DRUG_TYPES[] = label(CATEGORY_DRUG, SOMA, REAGENTS, MEDICINE),
    
    POLYMER = new Traded(
      BC, "Polymer"  , "polymer.gif"   , FORM_MATERIAL, 8,
      "Long-chain hydrocarbons, used in plastics production"
    ),
    METALS = new Traded(
      BC, "Metals"   , "metals.gif"    , FORM_MATERIAL, 12,
      "Common metal ores, used in construction and industry"
    ),
    FUEL_RODS = new Traded(
      BC, "Fuel Rods" , "fuel_rods.gif", FORM_MATERIAL, 30,
      "Heavy or unstable elements used in nuclear synthesis"
    ),
    ALL_MINERAL_TYPES[] = label(CATEGORY_MINERAL, POLYMER, METALS, FUEL_RODS),
    
    PLASTICS = new Traded(
      BC, "Plastics" , "plastics.gif" , FORM_MATERIAL, 25,
      "Flexible and light-weight, used for cloth and home fittings"
    ),
    PARTS = new Traded(
      BC, "Parts"    , "parts.gif"    , FORM_MATERIAL, 40,
      "Durable and heat-resistant, needed for heavy engineering"
    ),
    CIRCUITRY = new Traded(
      BC, "Circuitry", "circuitry.gif", FORM_MATERIAL, 80,
      "Used to manufacture terminals and other advanced devices"
    ),
    ALL_WARES_TYPES[] = label(CATEGORY_WARES, PLASTICS, PARTS, CIRCUITRY);
  
  
  final public static Traded
    ALL_MATERIALS [] = Traded.INDEX.soFar(Traded.class);
  
  final public static Traded
    SLAG = new Traded(
      BC, "Slag"    ,
      "Waste products from mining operations.",
      FORM_SPECIAL, 0
    ),
    SAMPLES = new Traded(
      BC, "Samples" , null, FORM_SPECIAL, 0
    ),
    
    //  TODO:  You need the ability to have an otherwise-identical resource
    //  look (and even be named and described) differently, such as for food
    //  types, fossils/hides/wood-sections, et cetera.
    //  This could be named differently for resources from different planets,
    //  for example...
    FOSSILS = new Traded(
      BC, "Fossils" , null, FORM_SPECIAL, 200,
      "The preserved remains of animals, plants, ancient artifacts or rare "+
      "geological strata, coveted for their rarity, beauty and insight."
    ),
    
    SPYCES = new Traded(
      BC, "Spyces", "spyce.gif"  , FORM_SPECIAL, 200,
      "Spyces are exotic baryonic compounds known to heighten psyonic "+
      "abilities."
    ),
    
    TREATMENT = new Traded(
      BC, "Treatment"  , null, FORM_SPECIAL, 0
    ),
    GENE_SEED = new Traded(
      BC, "Gene Seed"  , null, FORM_SPECIAL, 0
    ),
    REPLICANTS = new Traded(
      BC, "Replicants" , null, FORM_SPECIAL, 0
    ),
    SPYCE_BLUE = new Traded(
      BC, "Spyce Blue", "spyce.gif"  , FORM_SPECIAL, 200,
      "Natrizoral, a refined form of 'spyce wine' that can be doctored to "+
      "enhance particular forms of psyonic powers."
    ),
    
    //  TODO:  Replace directly with Atomics.  Ships have their own on-board
    //  reactors.
    ANTIMASS = new Traded(
      BC, "Antimass", "antimass.gif" , FORM_SPECIAL, 1000,
      "A concentrated energy source needed for atomics and space travel"
    ),
    PRESSFEED = new Traded(
      BC, "Pressfeed", "pressfeed.gif", FORM_SPECIAL, 50,
      "Disposable propaganda used to raise morale and sway the masses"
    ),
    DATALINKS = new Traded(
      BC, "Datalinks", "datalinks.gif", FORM_SPECIAL, 125,
      "Encrypted terminals used to store and carry sensitive information"
    ),
    DECOR = new Traded(
      BC, "Decor"    , "decor.gif"    , FORM_SPECIAL, 250,
      "Interior decoration for the homes of the upper crust"
    );
  final public static Traded
    ALL_SPECIAL_ITEMS[] = Traded.INDEX.soFar(Traded.class);
  
  
  final public static Traded
    ATMO  = new Traded(
      BC, "Atmo" , "atmo.png" , FORM_PROVISION, 5
    ),
    POWER = new Traded(
      BC, "Power", "power.png", FORM_PROVISION, 10
    ),
    WATER = new Traded(
      BC, "Water", "water.png", FORM_PROVISION, 20
    );
  final public static Traded
    ALL_PROVISIONS[] = Traded.INDEX.soFar(Traded.class);
  
  
  final public static Traded
    //  TODO:  Also, Transport.
    
    SERVICE_ENTERTAIN  = new Traded(
      BC, "Entertainment", null, FORM_SERVICE, 0
    ),
    SERVICE_HEALTHCARE = new Traded(
      BC, "Healthcare"   , null, FORM_SERVICE, 0
    ),
    SERVICE_HOUSING    = new Traded(
      BC, "Housing"      , null, FORM_SERVICE, 0
    ),
    SERVICE_REFUGE     = new Traded(
      BC, "Refuge"       , null, FORM_SERVICE, 0
    ),
    SERVICE_SECURITY   = new Traded(
      BC, "Security"     , null, FORM_SERVICE, 0
    ),
    SERVICE_ADMIN      = new Traded(
      BC, "Admin"        , null, FORM_SERVICE, 0
    ),
    SERVICE_RESEARCH   = new Traded(
      BC, "Research"     , null, FORM_SERVICE, 0
    ),
    SERVICE_COMMERCE   = new Traded(
      BC, "Commerce"     , null, FORM_SERVICE, 0
    ),
    SERVICE_REPAIRS    = new Traded(
      BC, "Repairs"      , null, FORM_SERVICE, 0
    ),
    SERVICE_ARMAMENT   = new Traded(
      BC, "Armament"     , null, FORM_SERVICE, 0
    );
  final public static Traded
    ALL_SERVICES[] = Traded.INDEX.soFar(Traded.class);
  
  
  final public static Object
    TO = new Object();
}





