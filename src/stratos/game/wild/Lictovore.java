/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.game.wild.Species.Type;
import stratos.graphics.common.ModelAsset;
import stratos.graphics.cutout.CutoutModel;
import stratos.graphics.solids.MS3DModel;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



//  TODO:  Create pack-hunting behaviours, so they can tackle larger prey,
//         like the Yamagur.


public class Lictovore extends Fauna {
  
  
  
  /**  Constructors, setup and save/load methods-
    */
  final public static ModelAsset
    MODEL_NEST_MICOVORE = CutoutModel.fromImage(
      Lictovore.class, LAIR_DIR+"nest_micovore.png", 3.5f, 3
    ),
    MODEL_MIDDENS[] = CutoutModel.fromImages(
      Lictovore.class, LAIR_DIR, 1.0f, 1, false,
      "midden_a.png",
      "midden_b.png",
      "midden_c.png"
    );
  
  final public static Species SPECIES = new Species(
    Lictovore.class,
    "Lictovore",
    "The Lictovore is an imposing obligate carnivore capable of tackling the "+
    "most stubborn of prey.  They defend large territories which they mark "+
    "with spyce middens, and sometimes raid in packs.",
    FILE_DIR+"MicovorePortrait.png",
    MS3DModel.loadFrom(
      FILE_DIR, "Micovore.ms3d", Lictovore.class,
      XML_FILE, "Micovore"
    ),
    Type.PREDATOR,
    2.50f, //bulk
    0.75f, //speed
    1.50f  //sight
  ) {
    final Blueprint BLUEPRINT = NestUtils.constructBlueprint(
      3, 2, this, MODEL_NEST_MICOVORE
    );
    public Actor sampleFor(Base base) { return init(new Lictovore(base)); }
    public Blueprint nestBlueprint() { return BLUEPRINT; }
  };
  
  
  public Lictovore(Base base) {
    super(SPECIES, base);
  }
  
  
  public Lictovore(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  protected void initStats() {
    
    health.initStats(
      20,  //lifespan
      species.baseBulk ,//bulk bonus
      species.baseSight,//sight range
      species.speedMult,//move speed,
      ActorHealth.ANIMAL_METABOLISM
    );
    gear.setBaseDamage(15);
    gear.setBaseArmour(5);
    
    //
    //  TODO:  PUT ALL THESE ATTRIBUTES IN THE SPECIES FIELDS
    traits.initAtts(20, 15, 5);
    traits.setLevel(HAND_TO_HAND     , 15);
    traits.setLevel(STEALTH_AND_COVER, 10);
    
    traits.setLevel(DEFENSIVE, 2);
    traits.setLevel(FEARLESS , 1);
    
    skills.addTechnique(MAUL  );
    skills.addTechnique(DEVOUR);
  }
  
  
  public float radius() {
    return 0.5f;
  }
  
  
  
  /**  Supplemental behaviour methods-
    */
  protected void addReactions(Target seen, Choice choice) {
    if (seen == null) return;
    if (seen instanceof Actor) {
      choice.add(new Retreat(this));
      choice.add(new Combat(this, (Actor) seen));
    }
  }
  

  protected Behaviour nextHunting() {
    if (mind.home() instanceof Venue) {
      final Venue nest = (Venue) mind.home();
      if (health.juvenile() && nest.stocks.amountOf(PROTEIN) > 0) {
        final Action homeFeed = new Action(
          this, nest,
          this, "actionHomeFeeding",
          Action.REACH_DOWN, "Feeding at nest"
        );
        homeFeed.setPriority((1 - health.caloryLevel()) * Action.PARAMOUNT);
        return homeFeed;
      }
    }
    return super.nextHunting();
  }
  
  
  protected void addChoices(Choice choice) {
    super.addChoices(choice);
    //
    //  Determine whether you should fight with others of your kind-
    float crowding = NestUtils.crowding(this) + 0.5f - health.caloryLevel();
    final Fauna fights = findCompetition();
    
    if (fights != null && crowding > 1) {
      final Plan fighting = new Combat(this, fights).addMotives(
        Plan.MOTIVE_EMERGENCY,
        (crowding - 1) * Plan.PARAMOUNT
      );
      choice.add(fighting);
    }
    
    //  TODO:  I'm removing fancier activities for the moment.
    /*
    //
    //  Determine whether to regurgitate meat at home-
    choice.add(nextRegurgitation());
    //
    //  Determine whether you should mark your territory-
    final Tile toMark = findTileToMark();
    if (toMark != null && ! health.juvenile()) {
      //I.sayAbout(this, "Tile to mark: "+toMark);
      final Action marking = new Action(
        this, toMark,
        this, "actionMarkTerritory",
        Action.STAND, "Marking Territory"
      );
      marking.setPriority(Action.CASUAL);
      choice.add(marking);
    }
    //*/
  }
  
  /*
  private Action nextRegurgitation() {
    if (health.juvenile() || ! (mind.home() instanceof Venue)) return null;
    final Venue nest = (Venue) mind.home();
    
    int numYoung = 0;
    for (Actor a : nest.staff().lodgers()) {
      if (a.health.juvenile()) numYoung++;
    }
    final float excessFood = health.caloryLevel() - 1;
    if (numYoung == 0 || excessFood <= 0) return null;
    if (nest.inventory().amountOf(PROTEIN) >= (numYoung * 5)) return null;
    
    final Action deposit = new Action(
      this, nest,
      this, "actionDepositFood",
      Action.REACH_DOWN, "Returning with food for young"
    );
    deposit.setPriority(excessFood * Action.PARAMOUNT * numYoung);
    return deposit;
  }
  
  
  /*
  public boolean actionDepositFood(Fauna actor, Venue nest) {
    float energy = health.caloryLevel() - 0.5f;
    if (energy <= 0) return false;
    actor.health.loseSustenance(energy);
    energy *= actor.health.maxHealth() / MEAT_CONVERSION;
    nest.stocks.addItem(Item.withAmount(PROTEIN, energy));
    return true;
  }
  
  
  public boolean actionHomeFeeding(Fauna actor, Venue nest) {
    float hunger = 1 - health.caloryLevel();
    hunger *= actor.health.maxHealth() / MEAT_CONVERSION;
    final float amountTaken = Nums.min(hunger, nest.stocks.amountOf(PROTEIN));
    if (amountTaken <= 0) return false;
    nest.stocks.removeItem(Item.withAmount(PROTEIN, amountTaken));
    actor.health.takeCalories(amountTaken * MEAT_CONVERSION, 1);
    return true;
  }
  //*/
  
  
  private Fauna findCompetition() {
    final Batch <Fauna> tried = new Batch <Fauna> ();
    for (Target e : senses.awareOf()) if (e instanceof Lictovore) {
      if (e == this) continue;
      final Lictovore m = (Lictovore) e;
      tried.add(m);
    }
    return (Fauna) Rand.pickFrom(tried);
  }
  
  
  /*
  private Tile findTileToMark() {
    if (! (mind.home() instanceof Venue)) return null;
    final Venue lair = (Venue) mind.home();
    float angle = Rand.num() * (float) Nums.PI * 2;
    final Vec3D p = lair.position(null);
    final int range = Nest.forageRange(species) / 2;
    
    final Tile tried = world.tileAt(
      p.x + (float) (Nums.cos(angle) * range),
      p.y + (float) (Nums.sin(angle) * range)
    );
    if (tried == null) return null;
    final Tile free = Spacing.nearestOpenTile(tried, tried);
    if (free == null) return null;
    
    final PresenceMap markMap = world.presences.mapFor(SpyceMidden.class);
    final SpyceMidden near = (SpyceMidden) markMap.pickNearest(free, range);
    final float dist = near == null ? 10 : Spacing.distance(near, free);
    if (dist < 5) return null;
    
    return free;
  }
  
  
  public boolean actionMarkTerritory(Lictovore actor, Tile toMark) {
    if (toMark.onTop() != null || toMark.blocked()) return false;
    final SpyceMidden midden = new SpyceMidden();
    midden.enterWorldAt(toMark.x, toMark.y, world);
    return true;
  }
  //*/
}














