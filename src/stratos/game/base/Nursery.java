/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.base;

import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.building.Economy.*;



public class Nursery extends Venue implements TileConstants {
  
  
  /**  Constructors, data fields, setup and save/load methods-
    */
  private static boolean verbose = false;
  
  final static String IMG_DIR = "media/Buildings/ecologist/";
  final static ModelAsset
    NURSERY_MODEL = CutoutModel.fromImage(
      Nursery.class, IMG_DIR+"nursery.png", 2, 2
    );
  
  final public static float
    MATURE_DURATION = World.STANDARD_DAY_LENGTH * 5,
    GROW_INCREMENT  = World.GROWTH_INTERVAL / MATURE_DURATION,
    
    EXTRA_CLAIM_SIZE = 4,
    
    MAX_HEALTH_BONUS     = 2.0f,
    INFEST_GROW_PENALTY  = 0.5f,
    POLLUTE_GROW_PENALTY = 0.5f,
    UPGRADE_GROW_BONUS   = 0.25f,
    
    CEREAL_BONUS = 2.00f,
    DRYLAND_MULT = 0.75f,
    WETLAND_MULT = 1.25f,
    
    NURSERY_CARBS   = 1,
    NURSERY_GREENS  = 0.5f,
    NURSERY_PROTEIN = 0.5f;
  
  
  private Box2D areaClaimed = new Box2D();
  private Tile toPlant[] = new Tile[0];
  private float needsTending = 0;
  
  
  public Nursery(Base base) {
    super(2, 2, ENTRANCE_SOUTH, base);
    structure.setupStats(
      25,  //integrity
      5,  //armour
      15,  //build cost
      0,  //max upgrades
      Structure.TYPE_FIXTURE
    );
    personnel.setShiftType(SHIFTS_BY_DAY);
    this.attachModel(NURSERY_MODEL);
  }
  
  
  public Nursery(Session s) throws Exception {
    super(s);
    areaClaimed.loadFrom(s.input());
    type    = s.loadInt();
    toPlant = (Tile[]) s.loadObjectArray(Tile.class);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    areaClaimed.saveTo(s.output());
    s.saveInt        (type);
    s.saveObjectArray(toPlant);
  }
  
  
  public int owningType() {
    return VENUE_OWNS;
  }
  
  
  public boolean privateProperty() {
    return true;
  }
  
  
  
  /**  Placement and supply-demand functions-
    */
  public boolean setPosition(float x, float y, World world) {
    final boolean okay = super.setPosition(x, y, world);
    if (okay) areaClaimed.setTo(footprint()).expandBy((int) EXTRA_CLAIM_SIZE);
    return okay;
  }


  protected Box2D areaClaimed() {
    return areaClaimed;
  }
  
  
  protected boolean canTouch(Element e) {
    return e.owningType() < this.owningType();
  }
  
  /*
  public boolean preventsClaimBy(Venue other) {
    //if (other instanceof EcologistStation) return false;
    return super.preventsClaimBy(other);
  }
  //*/
  
  
  public float ratePlacing(Target point) {
    //
    //  TODO:  base on proximity to an ecologist station and demand for food.
    //      ...also, fertility & insolation.
    
    final Presences presences = point.world().presences;
    float supply = presences.mapFor(Nursery.class).population();
    float demand = presences.mapFor(EcologistStation.class).population();
    demand *= 3;
    
    if (demand <= 0) return 0;
    final Venue nearest = (Venue) presences.nearestMatch(
      EcologistStation.class, point, -1
    );
    
    float rating = (demand - supply) / demand;
    if (inWorld()) rating += 1;
    rating /= (1f + Spacing.sectorDistance(point, nearest));
    
    return rating;
  }
  
  
  private void scanForCropTiles() {
    final boolean report = verbose && I.talkAbout == this;
    
    final Box2D cropArea = new Box2D().setTo(areaClaimed).expandBy(-1);
    final Batch <Tile> grabbed = new Batch <Tile> ();
    if (report) I.say("\nCROP AREA: "+cropArea);
    
    for (Tile t : world.tilesIn(cropArea, true)) {
      if (world.terrain().isRoad(t)) continue;
      if (base.paveRoutes.map.roadCounter(t) > 0) continue;
      if (t.owningType() > Element.ELEMENT_OWNS ) continue;
      grabbed.add(t);
      if (report && plantedAt(t) == null) I.say("  ADDING TILE: "+t);
    }
    
    //  TODO:  Grab contiguous areas and put 'covered' crops along one edge.
    
    toPlant = grabbed.toArray(Tile.class);
  }
  
  
  protected void checkCropStates() {
    final boolean report = verbose && I.talkAbout == this;
    
    if (report) I.say("CHECKING CROP STATES");
    needsTending = 0;
    for (Tile t : toPlant) {
      final Crop c = plantedAt(t);
      if (c == null || c.needsTending()) needsTending++;
    }
    
    if (report) I.say("NEEDS TENDING: "+needsTending);
    needsTending /= toPlant.length;
  }
  
  
  
  /**  Establishing crop areas-
    */
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates);
    if (! structure.intact()) return;
    
    structure.setAmbienceVal(2);
    
    if (numUpdates % 10 == 0) {
      scanForCropTiles();
      checkCropStates();
      
      final float
        growth = 10 * 1f / MATURE_DURATION,
        decay = growth / 10;
      for (Item seed : stocks.matches(SAMPLES)) {
        stocks.removeItem(Item.withAmount(seed, decay));
      }
      stocks.bumpItem(CARBS  , growth * NURSERY_CARBS  );
      stocks.bumpItem(GREENS , growth * NURSERY_GREENS );
      stocks.bumpItem(PROTEIN, growth * NURSERY_PROTEIN);
    }
  }
  
  
  public Tile[] toPlant() {
    return toPlant;
  }
  
  
  public Crop plantedAt(Tile t) {
    if (t == null || ! (t.onTop() instanceof Crop)) return null;
    return (Crop) t.onTop();
  }
  
  
  public float needForTending() {
    return needsTending;
  }
  
  
  
  /**  Economic functions-
    */
  public int numOpenings(Background v) {
    int num = super.numOpenings(v);
    if (v == CULTIVATOR) return num + 2;
    return 0;
  }
  
  
  public TradeType[] services() { return new TradeType[] {
    CARBS, PROTEIN, GREENS
  }; }
  
  
  public Background[] careers() { return new Background[] {
    CULTIVATOR
  }; }
  
  
  public Behaviour jobFor(Actor actor) {
    final Choice choice = new Choice(actor);
    
    //  If you're really short on food, consider foraging in the surrounds-
    final float shortages = (
      stocks.shortagePenalty(CARBS) +
      stocks.shortagePenalty(GREENS)
    ) / 2f;
    if (shortages > 0) {
      final Foraging foraging = new Foraging(actor, this);
      foraging.setMotive(Plan.MOTIVE_EMERGENCY, Plan.PARAMOUNT * shortages);
      choice.add(foraging);
    }
    //
    
    //  If the harvest is really coming in, pitch in regardless-
    if (! Planet.isNight(world)){
      if (needForTending() > 0.5f) choice.add(new Farming(actor, this));
    }
    
    if (choice.size() > 0) return choice.pickMostUrgent();
    
    if (! personnel.onShift(actor)) return null;
    final Delivery d = DeliveryUtils.bestBulkDeliveryFrom(
      this, services(), 2, 10, 5
    );
    choice.add(d);
    
    
    final Batch <Item> seedTypes = new Batch <Item> ();
    for (Species s : Crop.ALL_VARIETIES) {
      final Item seed = Item.withReference(SAMPLES, s);
      if (stocks.hasItem(seed)) continue;
      seedTypes.add(seed);
    }
    
    for (Object t : world.presences.sampleFromMap(
      this, world, 3, null, EcologistStation.class
    )) {
      final EcologistStation station = (EcologistStation) t;
      final Delivery seedD = new Delivery(seedTypes, station, this);
      choice.add(seedD);
    }
    
    choice.add(new Farming(actor, this));
    
    return choice.pickMostUrgent();
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(EcologistStation.ICON, "plantation");
  }
  
  
  public String fullName() {
    return "Nursery";
  }
  
  
  public String helpInfo() {
    //if (type == TYPE_NURSERY) return
    return
      "Nurseries allow young plants to be cultivated in a secure environment "+
      "prior to outdoor planting, and provide a small but steady food yield "+
      "regardless of outside conditions.";
    /*
    return
      "Plantations of managed, mixed-culture cropland secure a high-quality "+
      "food source for your base, but require space and constant attention.";
    //*/
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_ECOLOGIST;
  }
}




/*
public SelectionInfoPane configPanel(SelectionInfoPane panel, BaseUI UI) {
  final StringBuffer d = new StringBuffer();

  d.append("\n");
  boolean any = false;
  for (Item seed : stocks.matches(SAMPLES)) {
    final Species s = (Species) seed.refers;
    d.append("\n  Seed for "+s+" (");
    d.append(Crop.HEALTH_NAMES[(int) seed.quality]+" quality)");
    any = true;
  }
  if (! any) d.append("\nNo seed stock.");
  
  //  TODO:  Summarise the amount of crops planted and their overall health!
  panel = VenueDescription.configSimplePanel(this, panel, UI, d.toString());
  return panel;
}


public void renderSelection(Rendering rendering, boolean hovered) {
  BaseUI.current().selection.renderTileOverlay(
    rendering, world,
    hovered ? Colour.transparency(0.5f) : Colour.WHITE,
    Selection.SELECT_OVERLAY, true, this, this
  );
}
//*/
