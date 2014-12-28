

package stratos.game.base;
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
  private static boolean verbose = true;
  
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    KommandoLodge.class, "media/Buildings/ecologist/kommando_lodge.png", 4, 2
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    KommandoLodge.class, "media/GUI/Buttons/kommando_lodge_button.gif"
  );
  
  
  final static int CLAIM_RADIUS = Stage.SECTOR_SIZE / 2;
  
  private Venue fleshStill = null;
  private GroupSprite camouflaged;
  
  
  public KommandoLodge(Base base) {
    super(4, 2, Venue.ENTRANCE_NORTH, base);
    structure.setupStats(
      150, 4, 150,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
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
  protected Box2D areaClaimed() {
    final Box2D area = new Box2D(footprint());
    area.expandBy(CLAIM_RADIUS);
    return area;
  }
  
  
  public float homeCrowding(Actor actor) {
    if (! staff.isWorker(actor)) return super.homeCrowding(actor);
    return 0;
  }
  
  
  
  /**  Upgrades, economic functions and behaviour implementations-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
  );
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES; }
  final public static Upgrade
    /*
    THERMAL_CAMOUFLAGE = new Upgrade(
      "Thermal Camouflage",
      "Reduces the Surveillance Post's thermal signature and light output, "+
      "making it harder for outsiders to detect.",
      200,
      null, 1, null,
      KommandoLodge.class, ALL_UPGRADES
    ),
    SENSOR_PERIMETER = new Upgrade(
      "Sensor Perimeter",
      "Installs automatic sensors attuned to sound and motion, making it "+
      "difficult for intruders to approach unannounced.",
      100,
      null, 1, THERMAL_CAMOUFLAGE,
      KommandoLodge.class, ALL_UPGRADES
    ),
    //*/
    NATIVE_MISSION = new Upgrade(
      "Native Mission",
      "Improves recruitment from local tribal communities and raises the odds "+
      "of peaceful contact.",
      300,
      null, 1, null,
      KommandoLodge.class, ALL_UPGRADES
    ),
    MAW_TRAINING = new Upgrade(
      "Maw Training",
      "Trains your Kommandos to tame and ride Desert Maws, relatives of the "+
      "Lictovore selectively bred as war mounts.",
      300,
      null, 1, null,
      KommandoLodge.class, ALL_UPGRADES
    ),
    KOMMANDO_STATION = new Upgrade(
      "Kommando Station",
      KOMMANDO.info,
      100,
      Backgrounds.KOMMANDO, 1, null,
      KommandoLodge.class, ALL_UPGRADES
    ),
    FAVOURED_ENEMY_VERMIN = new Upgrade(
      "Vendetta: Vermin",
      "Trains your Kommandos to efficiently dispatch silicates, insectiles "+
      "and other dangerous, inedible pests.",
      100,
      null, 1, KOMMANDO_STATION,
      KommandoLodge.class, ALL_UPGRADES
    ),
    FAVOURED_ENEMY_HUMAN = new Upgrade(
      "Vendetta: Humans",
      "Trains your Kommandos to efficiently dispatch human (or human-like) "+
      "adversaries.",
      200,
      null, 1, KOMMANDO_STATION,
      KommandoLodge.class, ALL_UPGRADES
    ),
    FAVOURED_ENEMY_ARTILECT = new Upgrade(
      "Vendetta: Artilects",
      "Trains your Kommandos to efficiently dispatch machines and cybrids.",
      150,
      null, 1, KOMMANDO_STATION,
      KommandoLodge.class, ALL_UPGRADES
    );
  
  
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! staff.onShift(actor))) return null;
    final Choice choice = new Choice(actor);
    final boolean report = verbose && I.talkAbout == actor;
    
    final Exploring e = Exploring.nextExploration(actor);
    if (e != null) {
      e.setMotive(Plan.MOTIVE_DUTY, Plan.ROUTINE);
      choice.add(e);
    }
    
    //  TODO:  You'll also need to import carbs/greens and export protein/spyce,
    //  so that the residents can eat.  (That, or find a better way to site
    //  those tents.)
    
    //  TODO:  Add mounting behaviours, massed raiding against specified
    //  enemies, and placing their skins as a warning.  (And possibly animal
    //  breeding for mounts?)  Hunting, certainly.
    
    for (Target t : actor.senses.awareOf()) {
      //  TODO:  Weight the likelihood of this based on Favoured Enemies- and
      //  try to get multiple participants!
      
      if (Hunting.validPrey(t, actor, true)) {
        choice.add(Hunting.asHarvest(actor, (Actor) t, this, false));
      }
    }
    
    
    final Behaviour pick = choice.weightedPick();
    if (report) I.say("\n  Next survey station job: "+pick);
    return pick;
  }
  
  
  public Background[] careers() {
    return new Background[] { KOMMANDO, NATIVE_AUXILIARY, SLAYER };
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v);
    if (v == Backgrounds.KOMMANDO) {
      return nO + 3 + structure.upgradeLevel(KOMMANDO_STATION);
    }
    if (v == Backgrounds.SLAYER) {
      return nO + (1 + structure.upgradeLevel(KOMMANDO_STATION)) / 2;
    }
    if (v == Backgrounds.NATIVE_AUXILIARY) {
      return nO + structure.upgradeLevel(NATIVE_MISSION);
    }
    return 0;
  }
  
  
  public Traded[] services() {
    return null; //new Service[] { WATER, PROTEIN, SPICE };
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    stocks.forceDemand(CARBS, 5, TIER_CONSUMER);
    
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
  
  
  public String fullName() {
    return "Kommando Lodge";
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





/*
final boolean hasStill = still != null && ! still.destroyed();
for (Target t : actor.senses.awareOf()) {
  if (hasStill && Hunting.validPrey(t, actor, true)) {
    choice.add(Hunting.asHarvest(actor, (Actor) t, still, false));
  }
  if (t instanceof Fauna) {
    final Hunting h = Hunting.asSample(actor, (Fauna) t, this);
    if (! stocks.hasItem(h.sample())) choice.add(h);
  }
}

if (hasStill) {
  final Delivery d = DeliveryUtils.bestBulkDeliveryFrom(
    still, still.services(), 2, 10, 5
  );
  choice.add(d);
}
//*/
/*
if (hasStill) choice.add(Deliveries.nextDeliveryFor(
  actor, still, still.services(), 5, world
));
//*/

/*
if (structure.upgradeLevel(CAPTIVE_BREEDING) > 0) {
  final Fauna toTend = AnimalHusbandry.nextHandled(this);
  if (toTend != null) {
    choice.add(new AnimalHusbandry(actor, this, toTend));
  }
}

final SensorPost newPost = SensorPost.locateNewPost(this);
if (newPost != null) {
  final Action collects = new Action(
    actor, newPost,
    this, "actionCollectSensor",
    Action.REACH_DOWN, "Collecting sensor"
  );
  collects.setMoveTarget(this);
  final Action plants = new Action(
    actor, newPost.origin(),
    this, "actionPlantSensor",
    Action.REACH_DOWN, "Planting sensor"
  );
  plants.setMoveTarget(Spacing.pickFreeTileAround(newPost, actor));
  choice.add(new Steps(actor, this, Plan.ROUTINE, collects, plants));
}
//*/

/*
public boolean actionCollectSensor(Actor actor, SensorPost post) {
  actor.gear.addItem(Item.withReference(SAMPLES, post));
  return true;
}


public boolean actionPlantSensor(Actor actor, Tile t) {
  SensorPost post = null;
  for (Item i : actor.gear.matches(SAMPLES)) {
    if (i.refers instanceof SensorPost) {
      post = (SensorPost) i.refers;
      actor.gear.removeItem(i);
    }
  }
  if (post == null) return false;
  post.setPosition(t.x, t.y, world);
  if (! Spacing.perimeterFits(post)) return false;
  post.enterWorld();
  return true;
}
//*/





/*
package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.game.tactical.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



public class FleshStill extends Venue implements Economy {
  
  
  /**  Data fields, constructors and save/load methods-
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    FleshStill.class, "media/Buildings/ecologist/flesh_still.png", 3, 1
  );
  
  
  final KommandoLodge parent;
  GroupSprite camouflaged;
  
  
  public FleshStill(KommandoLodge parent) {
    super(3, 1, Venue.ENTRANCE_EAST, parent.base());
    structure.setupStats(
      100, 4, 150, 0, Structure.TYPE_FIXTURE
    );
    this.parent = parent;
    attachSprite(MODEL.makeSprite());
    camouflaged = new GroupSprite();
  }
  
  
  public FleshStill(Session s) throws Exception {
    super(s);
    parent = (KommandoLodge) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(parent);
  }
  
  
  
  /**  Upgrades, economic functions and behaviour implementations-
  public Behaviour jobFor(Actor actor) {
    return null;
  }
  
  public Background[] careers() { return null; }
  
  public Service[] services() {
    return new Service[] { WATER, PROTEIN, SPYCE };
  }
  
  
  protected void updatePaving(boolean inWorld) {
  }
  
  
  public boolean canPlace() {
    if (! super.canPlace()) return false;
    for (Tile t : Spacing.perimeter(area(), origin().world)) if (t != null) {
      if (t.owningType() >= this.owningType()) return false;
    }
    return true;
  }
  
  
  
  /**  Rendering and interface-
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
  
  
  public String fullName() {
    return "Flesh Still";
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(KommandoLodge.ICON, "flesh_still");
  }
  
  
  public String helpInfo() {
    return
      "Flesh Stills help to extract protein and spice from culled specimens "+
      "of native wildlife.";
  }
  
  
  public String buildCategory() {
    return UIConstants.TYPE_ECOLOGIST;
  }
}

//*/
























