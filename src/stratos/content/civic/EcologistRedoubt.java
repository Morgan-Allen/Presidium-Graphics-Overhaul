/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.civic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.plans.*;
import stratos.game.wild.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.craft.Economy.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.content.abilities.EcologistTechniques.*;



public class EcologistRedoubt extends Venue implements Captivity {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  private static boolean
    verbose = false;
  
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    EcologistRedoubt.class, "ecologist_redoubt_model",
    "media/Buildings/ecologist/xeno_lodge.png",
    4, 2
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    EcologistRedoubt.class, "ecologist_redoubt_icon",
    "media/GUI/Buttons/xeno_lodge_button.gif"
  );
  
  final static int CLAIM_RADIUS = (Stage.ZONE_SIZE / 2) - 2;
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    EcologistRedoubt.class, "ecologist_redoubt",
    "Ecologist Redoubt", Target.TYPE_ECOLOGIST, ICON,
    "Your Ecologists help adapt plant and animal species to a human presence, "+
    "and often prove valuable in guerilla warfare.",
    4, 2, Structure.IS_NORMAL, Owner.TIER_FACILITY, 150, 4,
    PROTEIN, SPYCES, ECOLOGIST
  );
  
  final public static Upgrade
    LEVELS[] = BLUEPRINT.createVenueLevels(
      Upgrade.SINGLE_LEVEL, null,
      new Object[] { 5, XENOZOOLOGY, 5, ASSEMBLY },
      400
    ),
    THERMAL_CAMO = new Upgrade(
      "Thermal Camo",
      "Reduces the "+BLUEPRINT+"'s thermal signature, making it harder for "+
      "outsiders to detect.  Makes "+PATTERN_CAMO+" available to staff.",
      200, Upgrade.THREE_LEVELS, LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null,
      10, EVASION, 5, XENOZOOLOGY
    ),
    CAPTIVE_BREEDING = new Upgrade(
      "Captive Breeding",
      "Improves the effiency of animal taming and breeding programs.",
      150,
      Upgrade.TWO_LEVELS, null, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null,
      15, XENOZOOLOGY
    ),
    PROTEIN_STILL = new Upgrade(
      "Protein Still",
      "Improves the effiency of spyce and protein extraction from scavenged "+
      "remains.",
      250,
      Upgrade.TWO_LEVELS, null, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null
    ),
    MOUNT_TRAINING_UPGRADE = new Upgrade(
      "Mount Training",
      "Allows captive animals to be trained as mounts for use in patrols and "+
      "exploration.",
      400,
      Upgrade.SINGLE_LEVEL, CAPTIVE_BREEDING, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null,
      15, XENOZOOLOGY, 5, BATTLE_TACTICS
    );
  
  final public static Conversion
    LAND_TO_PROTEIN = new Conversion(
      BLUEPRINT, "land_to_protein",
      TO, 1, PROTEIN
    ),
    LAND_TO_SPYCE = new Conversion(
      BLUEPRINT, "land_to_spyce",
      TO, 1, SPYCES
    );
  
  final static Species
    REARED_SPECIES[] = { Qudu.SPECIES, Hareen.SPECIES },
    MOUNT_SPECIES [] = { Lictovore.SPECIES };
  
  
  private Venue fleshStill = null;
  private Outcrop disguise = null;
  
  
  
  public EcologistRedoubt(Base base) {
    super(BLUEPRINT, base);
    staff.setShiftType(SHIFTS_BY_HOURS);
    structure.updateStats(blueprint.integrity, blueprint.armour, 10);
    attachSprite(MODEL.makeSprite());
  }
  
  
  public EcologistRedoubt(Session s) throws Exception {
    super(s);
    this.fleshStill = (Venue  ) s.loadObject();
    this.disguise   = (Outcrop) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(fleshStill);
    s.saveObject(disguise  );
  }
  
  
  
  /**  Placement and area-claims:
    */
  public boolean setPosition(float x, float y, Stage world) {
    if (! super.setPosition(x, y, world)) return false;
    if (disguise == null) disguise = new Outcrop(4, 2, Outcrop.TYPE_MESA);
    disguise.setPosition(x, y, world);
    return true;
  }
  
  
  public boolean enterWorldAt(int x, int y, Stage world, boolean intact) {
    if (! super.enterWorldAt(x, y, world, intact)) return false;
    disguise.refreshIncept(world, intact);
    return true;
  }
  
  
  public Box2D areaClaimed() {
    final Box2D area = new Box2D(footprint());
    area.expandBy(CLAIM_RADIUS);
    return area;
  }
  
  
  public boolean preventsClaimBy(Venue other) {
    if (other instanceof Nest && other.base() == base) return false;
    return true;
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
  public Behaviour jobFor(Actor actor) {
    if (staff.offDuty(actor)) return null;
    final Choice choice = new Choice(actor);
    //
    //  Consider hunting and sampling from non-domesticated animals-
    final Batch <Target> sampled = new Batch();
    world.presences.sampleFromMap(actor, world, 5, sampled, Mobile.class);
    Visit.appendTo(sampled, inside());
    
    float proteinMult = (1 + structure.upgradeLevel(PROTEIN_STILL   )) / 2f;
    float spyceMult   = (1 + structure.upgradeLevel(PROTEIN_STILL   )) / 2f;
    float trainMult   = (1 + structure.upgradeLevel(CAPTIVE_BREEDING)) / 2f;
    
    for (Target t : sampled) if (t instanceof Fauna) {
      final Fauna fauna = (Fauna) t;
      if (fauna.base() == base) continue;
      Hunting sample  = Hunting.asSample (actor, fauna, this);
      Hunting harvest = Hunting.asHarvest(actor, fauna, this);
      harvest.setProductionLevels(proteinMult, spyceMult);
      choice.add(sample );
      choice.add(harvest);
    }
    //
    //  Consider rearing both prey species and predators as mounts & companions-
    if (trainMult > 0) {
      addBreedingPlans(REARED_SPECIES, choice, trainMult);
    }
    if (
      structure.hasUpgrade(MOUNT_TRAINING_UPGRADE) &&
      actor.relations.servants().empty()
    ) {
      addBreedingPlans(MOUNT_SPECIES, choice, trainMult);
    }
    //
    //  Consider learning new skills-
    choice.add(Studying.asTechniqueTraining(actor, this, 0, canLearn()));
    //
    //  And last but not least, consider general exploration-
    final Exploring e = Exploring.nextExploration(actor);
    if (e != null) choice.add(e.addMotives(Plan.MOTIVE_JOB, 0));
    return choice.weightedPick();
  }
  
  
  private void addBreedingPlans(Species b[], Choice choice, float trainMult) {
    for (Species s : b) {
      float crowding = NestUtils.localCrowding(s, this);
      if (crowding >= 0.5f) continue;
      final Culturing t = new Culturing(choice.actor, this, s);
      t.setSpeedMult(trainMult);
      t.addMotives(Plan.NO_PROPERTIES, Plan.CASUAL * 1 - crowding);
      choice.add(t);
    }
  }
  
  
  private Technique[] canLearn() {
    final Batch <Technique> can = new Batch();
    can.add(TRANQUILLISE);
    can.add(XENO_CALL   );
    if (structure.hasUpgrade(THERMAL_CAMO          )) can.add(PATTERN_CAMO  );
    if (structure.hasUpgrade(MOUNT_TRAINING_UPGRADE)) can.add(MOUNT_TRAINING);
    return can.toArray(Technique.class);
  }
  
  
  public int numPositions(Background v) {
    final int nO = super.numPositions(v);
    if (v == Backgrounds.ECOLOGIST) return nO + 3;
    return 0;
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    
    stocks.setConsumption(CARBS  , 5);
    stocks.setConsumption(PROTEIN, 2);
    stocks.updateStockDemands(1, services(), LAND_TO_PROTEIN, LAND_TO_SPYCE);
    
    world.ecology().includeSpecies(REARED_SPECIES);
    world.ecology().includeSpecies(MOUNT_SPECIES );
    
    final int cloaking = 10 + (structure.upgradeLevel(THERMAL_CAMO) * 5);
    structure.updateStats(blueprint.integrity, blueprint.armour, cloaking);
  }
  
  
  protected void updatePaving(boolean inWorld) {
    return;
  }
  
  
  
  /**  Captivity-implementation-
    */
  public Property mountStoresAt() {
    return this;
  }
  
  
  public boolean setMounted(Actor mounted, boolean is) {
    if (is) mounted.goAboard(this, world);
    return true;
  }
  
  
  public boolean allowsActivity(Plan activity) {
    if (! (activity instanceof Resting)) return false;
    final Plan resting = new Resting(activity.actor(), this);
    return activity.matchesPlan(resting);
  }
  
  
  public void describeActor(Actor mounted, Description d) {
    d.append("Penned up at ");
    d.append(this);
  }
  


  /**  Rendering and interface-
    */
  public void renderFor(Rendering rendering, Base base) {
    if (base == this.base() || true) {
      super.renderFor(rendering, base);
    }
    else {
      //
      //  Render a bunch of rocks instead.  Also, make this non-selectable.
      disguise.renderFor(rendering, base);
    }
  }
  

  public void renderSelection(
    Rendering rendering, boolean hovered
  ) {
    if (base == this.base() || true) {
      super.renderSelection(rendering, hovered);
    }
    else {
      disguise.renderSelection(rendering, hovered);
    }
  }
  
  
  public boolean actorVisible(Actor mounted) {
    return false;
  }
  
  
  public void configureSpriteFrom(
    Actor mounted, Action action, Sprite actorSprite, Rendering r
  ) {
  }
}



