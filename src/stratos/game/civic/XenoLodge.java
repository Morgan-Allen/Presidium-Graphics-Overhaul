/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
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




//  TODO:  Save this for the stage when the Schools get introduced!  Create a
//  Xeno Lodge instead for animal-taming & breeding purposes.


public class XenoLodge extends Venue implements Captivity {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  private static boolean
    verbose = false;
  
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    XenoLodge.class, "media/Buildings/ecologist/xeno_lodge.png",
    4, 2
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    XenoLodge.class, "media/GUI/Buttons/xeno_lodge_button.gif"
  );
  
  final static int CLAIM_RADIUS = Stage.ZONE_SIZE / 2;
  
  final static Blueprint BLUEPRINT = new Blueprint(
    XenoLodge.class, "xeno_lodge",
    "Xeno Lodge", UIConstants.TYPE_ECOLOGIST, ICON,
    "The Xeno Lodge allows for the capture and rehabilitation of animal "+
    "species that might otherwise prove hostile to your citizens.",
    4, 2, Structure.IS_NORMAL,
    EcologistStation.BLUEPRINT, Owner.TIER_FACILITY,
    150, 4, 550, Structure.NORMAL_MAX_UPGRADES
  );
  
  
  private Venue fleshStill = null;
  private GroupSprite camouflaged;
  
  
  
  public XenoLodge(Base base) {
    super(BLUEPRINT, base);
    staff.setShiftType(SHIFTS_BY_HOURS);
    attachSprite(MODEL.makeSprite());
    
    camouflaged = new GroupSprite();
    camouflaged.attach(
      Habitat.SPIRE_MODELS[Rand.index(3)][0], 0, 0, 0
    );
  }
  
  
  public XenoLodge(Session s) throws Exception {
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
  final public static Upgrade
    NATIVE_MISSION = new Upgrade(
      "Native Mission",
      "Improves recruitment from local tribal communities and raises the odds "+
      "of peaceful contact.",
      300,
      Upgrade.THREE_LEVELS, null, 1,
      null, BLUEPRINT
    ),
    THERMAL_CAMOUFLAGE = new Upgrade(
      "Thermal Camouflage",
      "Reduces the Kommando Lodge's thermal signature and light output, "+
      "making it harder for outsiders to detect.",
      200, Upgrade.TWO_LEVELS, null, 1,
      null, BLUEPRINT
    ),
    RIDER_TRAINING = new Upgrade(
      "Maw Training",
      "Trains your Ecologists to tame and ride Desert Maws, relatives of the "+
      "Lictovore bred as war mounts.",
      300,
      Upgrade.TWO_LEVELS, null, 1,
      null, BLUEPRINT
    ),
    FLESH_STILL = new Upgrade(
      "Flesh Still",
      "Improves the effiency of spyce and protein extraction from rendered-"+
      "down culls.",
      150,
      Upgrade.TWO_LEVELS, null, 1,
      null, BLUEPRINT
    );
  
  final public static Conversion
    LAND_TO_PROTEIN = new Conversion(
      BLUEPRINT, "land_to_protein",
      TO, 1, PROTEIN
    );
  
  
  public Behaviour jobFor(Actor actor) {
    if (staff.offDuty(actor)) return null;
    final boolean report = verbose && I.talkAbout == actor;

    final Choice choice = new Choice(actor);
    final Exploring e = Exploring.nextExploration(actor);
    if (e != null) choice.add(e.addMotives(Plan.MOTIVE_JOB, 0));
    
    //  TODO:  Include recruitment efforts among natives.
    //  TODO:  Include animal-contact efforts.
    //  TODO:  Include animal-breeding and culling if necessary.
    //  TODO:  Allow agents to drop off wounded animals here.
    
    final Batch <Target> animals = new Batch();
    //  TODO:  Allow automatic sampling to include actor-senses.
    world.presences.sampleFromMap(actor, world, 5, animals, Mobile.class);

    for (Target t : animals) if (Hunting.validPrey(t, actor)) {
      final Fauna f = (Fauna) t;
      
      final Item sample = Item.withReference(GENE_SEED, f.species());
      if (stocks.hasItem(sample)) continue;
      else choice.add(Hunting.asSample(actor, f, this));
    }
    choice.add(AnimalTending.nextTending(actor, this));
    
    return choice.weightedPick();
  }
  
  
  public Background[] careers() {
    return new Background[] { SURVEYOR };
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v);
    if (v == Backgrounds.SURVEYOR) return nO + 2;
    return 0;
  }
  
  
  public Traded[] services() {
    return new Traded[] { PROTEIN, DRY_SPYCE };
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    
    stocks.incDemand(CARBS  , 5, 1, false);
    stocks.incDemand(PROTEIN, 5, 1, true );
  }
  
  
  protected void updatePaving(boolean inWorld) {
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
  
  
  public boolean actorVisible(Actor mounted) {
    return false;
  }
  
  
  public void configureSpriteFrom(
    Actor mounted, Action action, Sprite actorSprite
  ) {
  }
}




