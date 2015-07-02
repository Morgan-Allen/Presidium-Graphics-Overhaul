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
      Qudu.SPECIES, Hareen.SPECIES, Lictovore.SPECIES//, Yamagur.SPECIES
    },
    VERMIN_SPECIES[] = {
      Roach.SPECIES, Roachman.SPECIES
    },
    ARTILECT_SPECIES[] = {
      Drone.SPECIES, Tripod.SPECIES, Cranial.SPECIES
    }
  ;
  
  final public static Blueprint NEST_BLUEPRINTS[];
  static {
    final Species nesting[] = ANIMAL_SPECIES;
    NEST_BLUEPRINTS = new Blueprint[nesting.length];
    for (int n = nesting.length ; n-- > 0;) {
      NEST_BLUEPRINTS[n] = nesting[n].nestBlueprint();
    }
  }
  
  
  /**  Fields and constructors.
    */
  final public String name, info;
  final public ImageAsset portrait;
  final public ModelAsset model;
  
  final public Type type;
  private Item stageNutrients[][];
  //final public Item nutrients[];
  
  //  TODO:  Use a table filled with generic string keys, so that it's more
  //  self-descriptive.
  final public float
    baseBulk, speedMult, baseSight;
  
  
  public Species(
    Class baseClass,
    String name, String info, String portraitTex, ModelAsset model,
    Type type,
    float bulk, float speedMult, float sight
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
    
    this.type      = type     ;
    this.baseBulk  = bulk     ;
    this.speedMult = speedMult;
    this.baseSight = sight    ;
    stageNutrients = new Item[4][0];
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
    this.speedMult = 0;
    this.baseSight = 0;
    
    int amount = 0;
    Batch <Item> n = new Batch <Item> ();
    for (Object o : args) {
      if (o instanceof Integer) amount = (Integer) o;
      if (o instanceof Traded) n.add(Item.withAmount((Traded) o, amount));
    }
    
    final Item nutrients[] = n.toArray(Item.class);
    this.stageNutrients = new Item[4][nutrients.length];
    for (int i = 4; i-- > 0;) for (int l = nutrients.length; l-- > 0;) {
      final Item base = nutrients[l];
      final float mult = (i + 1) / 4f;
      stageNutrients[i][l] = Item.withAmount(base, base.amount * mult);
    }
  }
  
  
  protected Actor init(Actor f) {
    f.health.setupHealth(Rand.num(), 0.9f, 0.1f);
    f.health.setCaloryLevel((1.5f + Rand.num()) / 2);
    f.relations.setRelation(f.base(), 0.5f, 0);
    return f;
  }
  
  
  public Blueprint nestBlueprint() { return null; }
  
  public boolean browser () { return type == Type.BROWSER ; }
  public boolean predator() { return type == Type.PREDATOR; }
  public boolean sapient () { return type == Type.SAPIENT ; }
  public boolean vermin  () { return type == Type.VERMIN  ; }
  public boolean artilect() { return type == Type.ARTILECT; }
  public boolean floral  () { return type == Type.FLORA   ; }
  
  public boolean animal  () { return browser() || predator() || vermin(); }
  public boolean living  () { return sapient() || animal(); }
  
  public Item[] nutrients(int stage) { return stageNutrients[stage]; }
  public float metabolism() { return baseBulk * speedMult; }
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
