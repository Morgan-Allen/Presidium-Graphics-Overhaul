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




public abstract class Species extends Background {
  
  
  /**  Type, instance and media definitions-
    */
  //  TODO:  Include these as arguments as one would for normal Backgrounds!
  protected static enum Stat {
    BULK, SIGHT, SPEED,
    HEALTH, SENSES, COGNITION,
    ARMOUR, DAMAGE
  }
  public static enum Type {
    BROWSER ,
    PREDATOR,
    VERMIN  ,
    FLORA   ,
    SAPIENT ,
    ARTILECT,
  }
  final public static String
    KEY_BROWSER  = Type.BROWSER .name(),
    KEY_PREDATOR = Type.PREDATOR.name();
  
  
  
  /**  Lists and enumeration-
    */
  final public static Species
    HUMANOID_SPECIES[] = {
      Human.SPECIES
    },
    ANIMAL_SPECIES[] = {
      Qudu.SPECIES, Hareen.SPECIES, Lictovore.SPECIES, Yamagur.SPECIES
    },
    VERMIN_SPECIES[] = {
      Roach.SPECIES, Roachman.SPECIES
    },
    ARTILECT_SPECIES[] = {
      Drone.SPECIES, Tripod.SPECIES, Cranial.SPECIES
    }
  ;
  
  final public static VenueProfile NEST_PROFILES[];
  static {
    final Species nesting[] = ANIMAL_SPECIES;
    NEST_PROFILES = new VenueProfile[nesting.length];
    for (int n = nesting.length ; n-- > 0;) {
      NEST_PROFILES[n] = nesting[n].nestProfile();
    }
  }
  
  
  /**  Fields and constructors.
    */
  final public String name, info;
  final public ImageAsset portrait;
  final public ModelAsset model;
  
  final public Type type;
  final public Item nutrients[];
  
  //  TODO:  Use a table filled with generic string keys, so that it's more
  //  self-descriptive.
  final public float
    baseBulk, baseSpeed, baseSight;
  
  
  public Species(
    Class baseClass,
    String name, String info, String portraitTex, ModelAsset model,
    Type type,
    float bulk, float speed, float sight
  ) {
    super(
      baseClass,
      name, info, null, null,
      NOT_A_CLASS, NOT_A_GUILD
    );
    
    if (portraitTex == null) this.portrait = null;
    else this.portrait = ImageAsset.fromImage(baseClass, portraitTex);
    this.name  = name ;
    this.info  = info ;
    this.model = model;
    
    this.type      = type ;
    this.baseBulk  = bulk ;
    this.baseSpeed = speed;
    this.baseSight = sight;
    nutrients = new Item[0];
  }
  
  
  protected Species(Class baseClass, String name, Type type, Object... args) {
    super(
      baseClass,
      name, null, null, null,
      NOT_A_CLASS, NOT_A_GUILD
    );
    this.name     = name;
    this.info     = name;
    this.portrait = null;
    this.model    = null;
    
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
  }
  
  
  protected Actor init(Actor f) {
    f.health.setupHealth(Rand.num(), 0.9f, 0.1f);
    f.relations.setRelation(f.base(), 0.5f, 0);
    
    if (predator()) {
      I.say("STARTING INJURY IS: "+f.health.injuryLevel());
    }
    return f;
  }
  
  
  public VenueProfile nestProfile() { return null; }
  
  public boolean browser () { return type == Type.BROWSER ; }
  public boolean predator() { return type == Type.PREDATOR; }
  public boolean artilect() { return type == Type.ARTILECT; }
  public boolean sapient () { return type == Type.SAPIENT ; }
  public boolean vermin  () { return type == Type.VERMIN  ; }
  public boolean floral  () { return type == Type.FLORA   ; }
  
  public boolean animal  () { return browser() || predator() || vermin(); }
  public boolean living  () { return sapient() || animal(); }
  
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
