/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.wild.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.solids.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.economic.Economy.*;




/*
 Crops and Flora include:
   Durwheat                     (primary carbs on land)
   Bulrice                      (primary carbs in water)
   Broadfruits                  (secondary greens on land)
   Tuber lily                   (secondary greens in water)
   Ant/termite/bee/worm cells   (tertiary protein on land)
   Fish/mussel/clam farming     (tertiary protein in water)
   
   Vapok Canopy/Broadleaves  (tropical)
   Mixtaob Tree/Glass Cacti  (desert)
   Redwood/Cushion Plants    (tundra)
   Strain XV97/Mycon Bloom   (wastes)
   Lichens/Annuals           (pioneer species)
   Coral Beds/Algal Forest   (rivers/oceans)
   
   Lumen forest (changer) + Rhizome (glaive knight) + Manna tree (collective)
   Albedan ecology:  Carpets + Metastases + Amoeba Clade
//*/


//  TODO:  This class probably needs to be moved to the wild package.  It
//  should not be referring to objects outside the planet package.

public abstract class Species implements Session.Saveable {
  
  
  /**  Type, instance and media definitions-
    */
  //  TODO:  Allow Species to be defined within their own Actor sub-classes, in
  //  a fashion similar to Sprites and ModelAssets.
  
  //  TODO:  Define basic attributes using a Table, keyed with either the stats
  //  below, or using Skills/Traits.
  protected static enum Stat {
    BULK, SIGHT, SPEED,
    HEALTH, SENSES, COGNITION,
    ARMOUR, DAMAGE
  }
  
  
  final static String
    FILE_DIR = "media/Actors/fauna/",
    LAIR_DIR = "media/Buildings/lairs and ruins/",
    XML_FILE = "FaunaModels.xml";
  final public static ModelAsset
    MODEL_NEST_QUUD = CutoutModel.fromImage(
      Species.class, LAIR_DIR+"nest_quud.png", 2.5f, 2
    ),
    MODEL_NEST_VAREEN = CutoutModel.fromImage(
      Species.class, LAIR_DIR+"nest_vareen.png", 2.5f, 3
    ),
    MODEL_NEST_MICOVORE = CutoutModel.fromImage(
      Species.class, LAIR_DIR+"nest_micovore.png", 3.5f, 3
    ),
    MODEL_MIDDENS[] = CutoutModel.fromImages(
      Species.class, LAIR_DIR, 1.35f, 1, false,
      "midden_a.png",
      "midden_b.png",
      "midden_c.png"
    );
  
  public static enum Type {
    BROWSER,
    PREDATOR,
    HUMANOID,
    FLORA,
    ARTILECT
  }
  
  
  
  /**  Lists and enumeration-
    */
  private static Batch <Species>
    soFar      = new Batch <Species> (),
    allSpecies = new Batch <Species> ();
  
  private static Species[] speciesSoFar() {
    final Species s[] = soFar.toArray(Species.class);
    soFar.clear();
    return s;
  }
  
  private static Species[] allSpecies() {
    return allSpecies.toArray(Species.class);
  }
  
  
  final public static Species
    
    HUMAN = new Species(
      "Human",
      "Humans are the most common intelligent space-faring species in the "+
      "known systems of the local cluster.  According to homeworld records, "+
      "they owe their excellent visual perception, biped gait and manual "+
      "dexterity to arboreal ancestry, but morphology and appearance vary "+
      "considerably in response to a system's climate and gravity, sexual "+
      "dimorphism, mutagenic factors and history of eugenic practices. "+
      "Generally omnivorous, they make capable endurance hunters, but most "+
      "populations have shifted to agriponics and vats-culture to sustain "+
      "their numbers.  Though inquisitive and gregarious, human cultures are "+
      "riven by clannish instincts and long-running traditions of feudal "+
      "governance, spurring conflicts that may threaten their ultimate "+
      "survival.",
      null,
      null,
      Type.HUMANOID, 1, 1, 1
    ) {
      public Actor newSpecimen(Base base) { return null; }
      public Nest createNest() { return null; }
    },
    HUMANOID_SPECIES[] = speciesSoFar();
  
    
  final public static Species
    QUDU = new Species(
      "Qudu",
      "Qudu are placid, slow-moving, vegetarian browsers that rely on their "+
      "dense, leathery hides and intractable grip on the ground to protect "+
      "themselves from most predators.",
      "QuudPortrait.png",
      MS3DModel.loadFrom(
        FILE_DIR, "Quud.ms3d", Species.class,
        XML_FILE, "Quud"
      ),
      Type.BROWSER,
      1.00f, //bulk
      0.15f, //speed
      0.65f  //sight
    ) {
      public Actor newSpecimen(Base base) { return new Qudu(base); }
      public Nest createNest() { return new Nest(
        2, 2, Venue.ENTRANCE_EAST, this, MODEL_NEST_QUUD
      ); }
    },
    
    HAREEN = new Species(
      "Hareen",
      "Hareen are sharp-eyed aerial omnivores active by day, with a twinned "+
      "pair of wings that makes them highly maneuverable flyers.  Their "+
      "diet includes fruit, nuts, insects and carrion, but symbiotic algae "+
      "in their skin also allow them to subsist partially on sunlight.",
      "VareenPortrait.png",
      MS3DModel.loadFrom(
        FILE_DIR, "Vareen.ms3d", Species.class,
        XML_FILE, "Vareen"
      ),
      Type.BROWSER,
      0.50f, //bulk
      1.60f, //speed
      1.00f  //sight
    ) {
      public Actor newSpecimen(Base base) { return new Vareen(base); }
      public Nest createNest() { return new Nest(
        2, 2, Venue.ENTRANCE_EAST, this, MODEL_NEST_VAREEN
      ); }
    },
    
    LICTOVORE = new Species(
      "Lictovore",
      "The Lictovore is an imposing bipedal obligate carnivore capable of "+
      "substantial bursts of speed and tackling even the most stubborn prey. "+
      "They defend established nest sites where they tend their young, using "+
      "scented middens, rich in spice, to mark the limits of their territory.",
      "MicovorePortrait.png",
      MS3DModel.loadFrom(
        FILE_DIR, "Micovore.ms3d", Species.class,
        XML_FILE, "Micovore"
      ),
      Type.PREDATOR,
      2.50f, //bulk
      1.30f, //speed
      1.50f  //sight
    ) {
      public Actor newSpecimen(Base base) { return new Lictovore(base); }
      public Nest createNest() { return new Nest(
        3, 2, Venue.ENTRANCE_EAST, this, MODEL_NEST_MICOVORE
      ); }
    },
    
    //  TODO:  Include Yamagur, Maws et cetera!
    
    ANIMAL_SPECIES[] = Species.speciesSoFar(),
    
    
    //  TODO:  These probably need a dedicated class of their own.
    ONI_RICE    = new Species("Oni Rice"   , Type.FLORA, 2, CARBS  ) {},
    DURWHEAT    = new Species("Durwheat"   , Type.FLORA, 2, CARBS  ) {},
    SABLE_OAT   = new Species("Sable Oat"  , Type.FLORA, 1, CARBS  ) {},
    
    TUBER_LILY  = new Species("Tuber Lily" , Type.FLORA, 2, GREENS ) {},
    BROADFRUITS = new Species("Broadfruits", Type.FLORA, 2, GREENS ) {},
    HIBERNUTS   = new Species("Hibernuts"  , Type.FLORA, 1, GREENS ) {},
    
    HIVE_GRUBS  = new Species("Hive Grubs" , Type.FLORA, 1, PROTEIN) {},
    BLUE_VALVES = new Species("Blue Valves", Type.FLORA, 1, PROTEIN) {},
    CLAN_BORE   = new Species("Clan Bore"  , Type.FLORA, 1, PROTEIN) {},
    
    GORG_APHID  = new Species("Gorg Aphid" , Type.FLORA, 1, SPYCE_T  ) {},
    
    PIONEERS    = new Species("Pioneers"   , Type.FLORA) {},
    TIMBER      = new Species("Timber"     , Type.FLORA) {},
    
    CROP_SPECIES[] = Species.speciesSoFar(),
    
    
    //  TODO:  Include descriptive text and other details.
    SPECIES_DRONE = new Species(
      "Drone",
      "<THIS SPACE RESERVED>",
      null,
      null,
      Type.ARTILECT, 1, 1, 1
    ) {
      public Actor newSpecimen(Base base) { return new Drone(base); }
    },
    
    SPECIES_TRIPOD = new Species(
      "Tripod",
      "<THIS SPACE RESERVED>",
      null,
      null,
      Type.ARTILECT, 1, 1, 1
    ) {
      public Actor newSpecimen(Base base) { return new Tripod(base); }
    },
    
    SPECIES_CRANIAL = new Species(
      "Cranial",
      "<THIS SPACE RESERVED>",
      null,
      null,
      Type.ARTILECT, 1, 1, 1
    ) {
      public Actor newSpecimen(Base base) { return new Cranial(base); }
    },
    
    ARTILECT_SPECIES[] = { SPECIES_DRONE, SPECIES_TRIPOD, SPECIES_CRANIAL },
    
    ALL_SPECIES[] = Species.allSpecies()
 ;
  
  
  /**  Fields and constructors.
    */
  final public String name, info;
  final public ImageAsset portrait;
  final public ModelAsset model;
  
  private static int nextID = 0;
  final public int ID = nextID++;
  
  final public Type type;
  final public Item nutrients[];
  
  //  TODO:  Use a table filled with generic string keys, so that it's more
  //  self-descriptive.
  final public float
    baseBulk, baseSpeed, baseSight;
  
  
  Species(
    String name, String info, String portraitTex, ModelAsset model,
    Type type,
    float bulk, float speed, float sight
  ) {
    this.name = name;
    this.info = info;
    if (portraitTex == null) this.portrait = null;
    else this.portrait = ImageAsset.fromImage(
      Species.class, FILE_DIR+portraitTex
    );
    this.model = model;
    
    this.type = type;
    this.baseBulk = bulk;
    this.baseSpeed = speed;
    this.baseSight = sight;
    nutrients = new Item[0];
    
    soFar.add(this);
    allSpecies.add(this);
  }
  
  
  Species(String name, Type type, Object... args) {
    this.name = name;
    this.info = name;
    this.portrait = null;
    this.model = null;
    
    this.type = type;
    this.baseBulk = 1;
    this.baseSpeed = 0;
    this.baseSight = 0;
    
    int amount = 0;
    Batch <Item> n = new Batch <Item> ();
    for (Object o : args) {
      if (o instanceof Integer) amount = (Integer) o;
      if (o instanceof Traded) n.add(Item.withAmount((Traded) o, amount));
    }
    nutrients = n.toArray(Item.class);
    
    soFar.add(this);
    allSpecies.add(this);
  }
  
  
  public static Session.Saveable loadConstant(Session s) throws Exception {
    return ALL_SPECIES[s.loadInt()];
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(ID);
  }
  
  
  public Actor newSpecimen(Base base) { return null; };
  public Nest createNest() { return null; };
  
  
  public boolean browser () { return type == Type.BROWSER; }
  public boolean predator() { return type == Type.PREDATOR; }
  public boolean animal  () { return browser() || predator(); }
  
  public Item[] nutrients() { return nutrients; }
  public float metabolism() { return baseBulk * baseSpeed; }
  
  
  
  /**  Feedback and diagnostics-
    */
  public String toString() { return name; }
}




/*
final public static Species
  //
  //  Friendlies first-
  HUMAN       = new Species("Human"),
  CHANGELING  = new Species("Changeling"),
  KRECH       = new Species("Krech"),
  JOVIAN      = new Species("Jovian"),
  //
  //  Insectiles-
  GIANT_ROACH = new Species("Giant Roach"),
  ROACHMAN    = new Species("Roachman"),
  ARAK_LANCER = new Species("Arak Lancer"),
  TERMAGANT   = new Species("Termagant"),
  //
  //  Browsers and Predators-
  QUD         = new Species("Qud"), //X
  HIREX       = new Species("Hirex"),
  LORGOX      = new Species("Trigox"),
  HAREEN      = new Species("Hareen"), //X
  DRIVODIL    = new Species("Drivodil"),
  GIGANS      = new Species("Gigans"), //Y
  LICTOVORE   = new Species("Lictovore"), //X
  DESERT_MAW  = new Species("Desert Maw"),
  CYAN_CLADE  = new Species("Cyan Clade"),
  //
  //  Artilects-
  DRONE       = new Species("Drone"), //Y
  TRIPOD      = new Species("Tripod"), //Y
  CRANIAL     = new Species("Cranial"), //Y
  MANIFOLD    = new Species("Manifold"),
  ORACLE      = new Species("Oracle"),
  OBELISK     = new Species("Obelisk"),
  //
  //  Silicates-
  REM_LEECH   = new Species("Rem Leech"),
  SILVER_HULK = new Species("Silver Hulk"),
  AGGREGANT   = new Species("Aggregant"),
  ARCHON      = new Species("Archon")
;
//*/
