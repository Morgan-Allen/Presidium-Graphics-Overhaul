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



public class Plantation extends Venue implements TileConstants {
  
  
  /**  Constructors, data fields, setup and save/load methods-
    */
  final static String IMG_DIR = "media/Buildings/ecologist/";
  final static ModelAsset
    NURSERY_MODEL = CutoutModel.fromImage(
      Plantation.class, IMG_DIR+"nursery.png", 2, 2
    );
  
  final public static float
    MATURE_DURATION = World.STANDARD_DAY_LENGTH * 5,
    GROW_INCREMENT  = World.GROWTH_INTERVAL / MATURE_DURATION,
    
    MIN_AREA_SIDE = 4,
    MAX_AREA_SIDE = 8,
    
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
  
  
  public Plantation(Base base) {
    super(2, 2, ENTRANCE_SOUTH, base);
    structure.setupStats(
      25,  //integrity
      5,  //armour
      15,  //build cost
      0,  //max upgrades
      Structure.TYPE_FIXTURE
    );
    this.attachModel(NURSERY_MODEL);
  }
  

  public Plantation(Session s) throws Exception {
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
    return FIXTURE_OWNS;
  }
  
  
  public boolean privateProperty() {
    return true;
  }
  
  
  public boolean setPosition(float x, float y, World world) {
    final boolean okay = super.setPosition(x, y, world);
    if (okay) areaClaimed.set(x - 3.5f, y - 3.5f, 8, 8);
    return okay;
  }


  protected Box2D areaClaimed() {
    return areaClaimed;
  }
  
  
  protected boolean canTouch(Element e) {
    return e.owningType() < this.owningType();
  }
  
  
  protected boolean claimConflicts(Venue other) {
    return super.claimConflicts(other);
  }
  
  
  //  TODO:  You need to be able to efficiently rate placement against an
  //  array of possible locations.  ...I think.
  
  public float ratePlacing(Target point) {
    //
    //  TODO:  base on proximity to an ecologist station and demand for food.
    //      ...also, fertility & insolation.
    
    final Presences presences = point.world().presences;
    float supply = presences.mapFor(Plantation.class).population();
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
    if (toPlant == null || toPlant.length == 0) {
      final Box2D plots[] = world.claims.placeLatticeWithin(
        areaClaimed, this, 2, 4, true
      );
      I.say("Getting plots to farm for plantation: "+plots.length);
      
      final Batch <Tile> plantB = new Batch <Tile> ();
      for (Box2D plot : plots) for (Tile t : world.tilesIn(plot, false)) {
        plantB.add(t);
      }
      toPlant = (Tile[]) plantB.toArray(Tile.class);
    }
  }
  
  
  
  /**  Establishing crop areas-
    */
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates);
    if (! structure.intact()) return;
    
    structure.setAmbienceVal(2);
    
    if (numUpdates % 10 == 0) {
      scanForCropTiles();
      
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
  
  
  public void onGrowth(Tile t) {
    if (! structure.intact()) return;
    //  TODO:  Also update growth for any plants inside the nursery.
    checkCropStates();
  }
  
  
  protected void checkCropStates() {
    needsTending = 0;
    for (Tile t : toPlant) {
      final Crop c = plantedAt(t);
      if (c == null || c.needsTending()) needsTending++;
    }
    needsTending /= toPlant.length;
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
    if (v == CULTIVATOR) return num + 1;
    return 0;
  }
  
  //  TODO:  Consider changing these to employ 1 or 2 cultivators each.
  public TradeType[] services() { return new TradeType[] {
      CARBS, PROTEIN, GREENS
  }; }
  
  
  public Background[] careers() { return new Background[] {
      CULTIVATOR
  }; }
  
  
  public Behaviour jobFor(Actor actor) {
    final Choice choice = new Choice(actor);
    //
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

    final Forestry f = new Forestry(actor, this);
    f.setMotive(Plan.MOTIVE_DUTY, Plan.ROUTINE);
    f.configureFor(Forestry.STAGE_GET_SEED);
    choice.add(f);
    
    //  TODO:  Collect seeds from the nearest ecologist station if you can!
    
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
}




