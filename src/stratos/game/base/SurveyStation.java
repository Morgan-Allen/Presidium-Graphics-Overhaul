

package stratos.game.base;

import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.tactical.*;
import stratos.game.wild.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



public class SurveyStation extends Venue implements Economy {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    SurveyStation.class, "media/Buildings/ecologist/surveyor.png", 4, 1
  ) ;
  final static ImageAsset ICON = ImageAsset.fromImage(
    "media/GUI/Buttons/redoubt_button.gif", SurveyStation.class
  );
  private static boolean verbose = true;
  
  
  private GroupSprite camouflaged;
  private FleshStill still;
  
  
  public SurveyStation(Base base) {
    super(4, 1, Venue.ENTRANCE_NORTH, base) ;
    structure.setupStats(
      150, 4, 150,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    ) ;
    personnel.setShiftType(SHIFTS_BY_HOURS) ;
    attachSprite(MODEL.makeSprite()) ;
    camouflaged = new GroupSprite() ;
  }
  
  
  public SurveyStation(Session s) throws Exception {
    super(s) ;
    still = (FleshStill) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(still) ;
  }
  
  
  
  /**  Upgrades, economic functions and behaviour implementations-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    SurveyStation.class, "surveillance_post_upgrades"
  ) ;
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES ; }
  final public static Upgrade
    THERMAL_CAMOUFLAGE = new Upgrade(
      "Thermal Camouflage",
      "Reduces the Surveillance Post's thermal signature and light output, "+
      "making it harder for outsiders to detect.",
      200,
      null, 1, null, ALL_UPGRADES
    ),
    SENSOR_PERIMETER = new Upgrade(
      "Sensor Perimeter",
      "Installs automatic sensors attuned to sound and motion, making it "+
      "difficult for intruders to approach unannounced.",
      100,
      null, 1, null, ALL_UPGRADES
    ),
    NATIVE_MISSION = new Upgrade(
      "Native Mission",
      "Improves outreach to local tribal communities, raising the odds of "+
      "peaceful contact and recruitment from their ranks.",
      300,
      null, 1, null, ALL_UPGRADES
    ),
    CAPTIVE_BREEDING = new Upgrade(
      "Animal Breeding",
      "Breeds new specimens of local wildlife for use as food stock or "+
      "personal companions.",
      300,
      null, 1, SENSOR_PERIMETER, ALL_UPGRADES
    ),
    GUERILLA_TRAINING = new Upgrade(
      "Guerilla Training",
      "Emphasises combat, stealth and survival exercises relevant in a "+
      "military capacity.",
      200,
      null, 1, THERMAL_CAMOUFLAGE, ALL_UPGRADES
    ),
    EXPLORER_STATION = new Upgrade(
      "Explorer Station",
      "Explorers are rugged outdoorsman that combine scientific curiosity "+
      "with a respect for natural ecosystems and basic self-defence training.",
      100,
      Backgrounds.EXPLORER, 1, null, ALL_UPGRADES
    ) ;
  
  
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null;
    final Choice choice = new Choice(actor);
    final boolean report = verbose && I.talkAbout == actor;
    
    final Exploring e = Exploring.nextExplorationFor(actor);
    if (e != null) {
      e.setMotive(Plan.MOTIVE_DUTY, Plan.ROUTINE);
      choice.add(e);
    }
    
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
    /*
    if (hasStill) choice.add(Deliveries.nextDeliveryFor(
      actor, still, still.services(), 5, world
    ));
    //*/
    
    //  TODO:  Incorporate sensor-placement into recon missions.
    choice.add(AnimalBreeding.nextBreeding(actor, this));
    
    final Behaviour pick = choice.weightedPick();
    if (report) I.say("\n  Next survey station job: "+pick);
    return pick;
  }
  
  
  public Background[] careers() {
    return new Background[] { Backgrounds.EXPLORER } ;
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v);
    if (v == Backgrounds.EXPLORER) {
      return nO + 3 + structure.upgradeLevel(EXPLORER_STATION);
    }
    return 0;
  }
  
  
  public Service[] services() {
    return null ; //new Service[] { WATER, PROTEIN, SPICE } ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (! structure.intact()) return ;
    stocks.forceDemand(CARBS, 5, Stocks.TIER_CONSUMER) ;
    
    if (still == null || still.destroyed()) {
      final Tile o = Spacing.pickRandomTile(this, 4, world) ;
      still = (FleshStill) Placement.establishVenue(
        new FleshStill(this), o.x, o.y, GameSettings.buildFree, world
      ) ;
    }
  }
  
  
  protected void updatePaving(boolean inWorld) {
  }
  


  /**  Rendering and interface-
    */
  public void renderFor(Rendering rendering, Base base) {
    if (base == this.base()) super.renderFor(rendering, base) ;
    else {
      //
      //  Render a bunch of rocks instead.  Also, make this non-selectable.
      this.position(camouflaged.position) ;
      camouflaged.fog = this.fogFor(base) ;
      camouflaged.readyFor(rendering);
    }
  }
  
  
  public String fullName() {
    return "Survey Station" ;
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "survey_station");
  }
  
  
  public String helpInfo() {
    return
      "Survey Stations are responsible for exploring the hinterland of your "+
      "settlement, scouting for danger and regulating animal populations." ;
  }
  
  
  public String buildCategory() {
    return UIConstants.TYPE_ECOLOGIST ;
  }
}





/*
if (structure.upgradeLevel(CAPTIVE_BREEDING) > 0) {
  final Fauna toTend = AnimalHusbandry.nextHandled(this) ;
  if (toTend != null) {
    choice.add(new AnimalHusbandry(actor, this, toTend)) ;
  }
}

final SensorPost newPost = SensorPost.locateNewPost(this) ;
if (newPost != null) {
  final Action collects = new Action(
    actor, newPost,
    this, "actionCollectSensor",
    Action.REACH_DOWN, "Collecting sensor"
  ) ;
  collects.setMoveTarget(this) ;
  final Action plants = new Action(
    actor, newPost.origin(),
    this, "actionPlantSensor",
    Action.REACH_DOWN, "Planting sensor"
  ) ;
  plants.setMoveTarget(Spacing.pickFreeTileAround(newPost, actor)) ;
  choice.add(new Steps(actor, this, Plan.ROUTINE, collects, plants)) ;
}
//*/

/*
public boolean actionCollectSensor(Actor actor, SensorPost post) {
  actor.gear.addItem(Item.withReference(SAMPLES, post)) ;
  return true ;
}


public boolean actionPlantSensor(Actor actor, Tile t) {
  SensorPost post = null ;
  for (Item i : actor.gear.matches(SAMPLES)) {
    if (i.refers instanceof SensorPost) {
      post = (SensorPost) i.refers ;
      actor.gear.removeItem(i) ;
    }
  }
  if (post == null) return false ;
  post.setPosition(t.x, t.y, world) ;
  if (! Spacing.perimeterFits(post)) return false ;
  post.enterWorld() ;
  return true ;
}
//*/



