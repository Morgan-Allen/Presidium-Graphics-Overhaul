

package stratos.game.civic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.game.wild.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.economic.Economy.*;



public class KommandoLodge extends Venue {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  private static boolean
    verbose = false;
  
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    KommandoLodge.class, "media/Buildings/ecologist/kommando_lodge.png", 4, 2
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    KommandoLodge.class, "media/GUI/Buttons/kommando_lodge_button.gif"
  );
  
  final static int CLAIM_RADIUS = Stage.SECTOR_SIZE / 2;

  final public static Conversion
    LAND_TO_PROTEIN = new Conversion(
      KommandoLodge.class, "land_to_protein",
      TO, 1, PROTEIN
    );
  
  final static VenueProfile PROFILE = new VenueProfile(
    KommandoLodge.class, "kommando_lodge", "Kommando Lodge",
    4, 2, IS_NORMAL,
    EcologistStation.PROFILE, Owner.TIER_FACILITY,
    LAND_TO_PROTEIN
  );
  
  
  private Venue fleshStill = null;
  private GroupSprite camouflaged;
  
  
  
  public KommandoLodge(Base base) {
    super(PROFILE, base);
    structure.setupStats(
      150, 4, 150,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_FIXTURE
    );
    staff.setShiftType(SHIFTS_BY_HOURS);
    attachSprite(MODEL.makeSprite());
    
    camouflaged = new GroupSprite();
    camouflaged.attach(
      Habitat.SPIRE_MODELS[Rand.index(3)][0], 0, 0, 0
    );
  }
  
  
  public KommandoLodge(Session s) throws Exception {
    super(s);
    this.fleshStill = (Venue) s.loadObject();
    this.camouflaged = (GroupSprite) ModelAsset.loadSprite(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(fleshStill);
    ModelAsset.saveSprite(camouflaged, s.output());
  }
  
  
  
  /**  Placement and area-claims:
    */
  public Box2D areaClaimed() {
    final Box2D area = new Box2D(footprint());
    area.expandBy(CLAIM_RADIUS);
    return area;
  }
  
  
  public boolean preventsClaimBy(Venue other) {
    if (other instanceof Nest) return false;
    //  TODO:  Use this!
    //if (other instanceof FleshStill) return false;
    return super.preventsClaimBy(other);
  }


  public float crowdRating(Actor actor, Background background) {
    if (background == Backgrounds.AS_RESIDENT) {
      if (! staff.isWorker(actor)) return 1;
      return 0;
    }
    else return super.crowdRating(actor, background);
  }
  
  
  
  /**  Upgrades, economic functions and behaviour implementations-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
  );
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES; }
  final public static Upgrade
    NATIVE_MISSION = new Upgrade(
      "Native Mission",
      "Improves recruitment from local tribal communities and raises the odds "+
      "of peaceful contact.",
      300,
      Upgrade.THREE_LEVELS, null, 1,
      null, KommandoLodge.class
    ),
    THERMAL_CAMOUFLAGE = new Upgrade(
      "Thermal Camouflage",
      "Reduces the Kommando Lodge's thermal signature and light output, "+
      "making it harder for outsiders to detect.",
      200, Upgrade.TWO_LEVELS, null, 1, null,
      KommandoLodge.class
    ),
    MAW_TRAINING = new Upgrade(
      "Maw Training",
      "Trains your Kommandos to tame and ride Desert Maws, relatives of the "+
      "Lictovore selectively bred as war mounts.",
      300,
      Upgrade.TWO_LEVELS, null, 1,
      null, KommandoLodge.class
    ),
    FLESH_STILL = new Upgrade(
      "Flesh Still",
      "Improves the effiency of spyce and protein extraction from rendered-"+
      "down kills.",
      150,
      Upgrade.TWO_LEVELS, null, 1,
      null, KommandoLodge.class
    ),
    VENDETTA_VERMIN = new Upgrade(
      "Vendetta: Vermin",
      "Trains your Kommandos to efficiently dispatch silicates, insectiles "+
      "and other dangerous, inedible pests.",
      100,
      Upgrade.SINGLE_LEVEL, null, 1,
      null, KommandoLodge.class
    ),
    VENDETTA_HUMAN = new Upgrade(
      "Vendetta: Humans",
      "Trains your Kommandos to efficiently dispatch human (or human-like) "+
      "adversaries.",
      200,
      Upgrade.SINGLE_LEVEL, null, 1,
      null, KommandoLodge.class
    ),
    VENDETTA_ARTILECT = new Upgrade(
      "Vendetta: Artilects",
      "Trains your Kommandos to efficiently dispatch machines and cybrids.",
      150,
      Upgrade.SINGLE_LEVEL, null, 1,
      null, KommandoLodge.class
    );
  
  
  public Behaviour jobFor(Actor actor, boolean onShift) {
    if (! onShift) return null;
    final Choice choice = new Choice(actor);
    final boolean report = verbose && I.talkAbout == actor;
    
    //  TODO:  Consider using 'joining' behaviours to actively recruit other
    //         actors for a given purpose.  Then you can use that for hunting,
    //         building, recreation, or what have you.
    
    //  TODO:  Include mounting behaviours, and placing skins nearby as a
    //         warning to others.
    
    //  TODO:  Include Training behaviours that cover learning vendetta-based
    //         techniques- as you would for a School!
    
    //  TODO:  Weight the likelihood of hunting based on vendettas, and add
    //         special Techniques to deal extra damage.
    
    final Exploring e = Exploring.nextExploration(actor);
    if (e != null) {
      e.addMotives(Plan.MOTIVE_JOB, Plan.ROUTINE);
      choice.add(e);
    }
    
    final float food = stocks.amountOf(CARBS) + stocks.amountOf(GREENS);
    if (food < 10) {
      final Foraging f = new Foraging(actor, this);
      f.addMotives(Plan.MOTIVE_JOB, ((10 - food) / 10) * Plan.ROUTINE);
      choice.add(f);
    }
    
    for (Target t : world.presences.sampleFromMap(
      actor, world, 5, null, Mobile.class
    )) {
      if (Hunting.validPrey(t, actor)) {
        choice.add(Hunting.asHarvest(actor, (Actor) t, this, false));
      }
    }
    for (Target t : actor.senses.awareOf()) {
      if (Hunting.validPrey(t, actor)) {
        choice.add(Hunting.asHarvest(actor, (Actor) t, this, false));
      }
    }
    choice.isVerbose = report;
    
    
    final Behaviour pick = choice.weightedPick();
    if (report) I.say("\n  Next survey station job: "+pick);
    return pick;
  }
  
  
  public Background[] careers() {
    return new Background[] { KOMMANDO, NATIVE_AUXILIARY };
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v);
    if (v == Backgrounds.KOMMANDO) {
      return nO + 3;
    }
    if (v == Backgrounds.NATIVE_AUXILIARY) {
      return nO + structure.upgradeLevel(NATIVE_MISSION);
    }
    return 0;
  }
  
  
  public Traded[] services() {
    return new Traded[] { PROTEIN, SPYCE_T };
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    stocks.forceDemand(CARBS, 5, false);
    stocks.incDemand(PROTEIN, 10, 1, true);
    
    //stocks.forceDemand(PROTEIN, 10, false);
    //stocks.setAmount(PROTEIN, 15);
    
    //  TODO:  Use Survey Redoubt as home?
    /*
    if (still == null || still.destroyed()) {
      final Tile o = Spacing.pickRandomTile(this, 4, world);
      still = (FleshStill) Placement.establishVenue(
        new FleshStill(this), o.x, o.y, GameSettings.buildFree, world
      );
    }
    //*/
  }
  
  
  protected void updatePaving(boolean inWorld) {
  }
  


  /**  Rendering and interface-
    */
  public void renderFor(Rendering rendering, Base base) {
    if (base == this.base()) super.renderFor(rendering, base);
    else {
      //
      //  Render a bunch of rocks instead.  Also, make this non-selectable.
      this.position(camouflaged.position);
      camouflaged.fog = this.fogFor(base);
      camouflaged.readyFor(rendering);
    }
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "survey_station");
  }
  
  
  public String helpInfo() {
    return
      "The Kommando Lodge allows you to recruit the tough, ruthless and self-"+
      "sufficient Kommandos to patrol your untamed hinterlands.";
  }
  
  
  public String objectCategory() {
    return UIConstants.TYPE_ECOLOGIST;
  }
}




