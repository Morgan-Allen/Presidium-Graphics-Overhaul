/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.civic;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.game.actors.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;
import static stratos.game.actors.Qualities.*;



public class Nursery extends HarvestVenue implements TileConstants {
  
  
  /**  Constructors, data fields, setup and save/load methods-
    */
  protected static boolean verbose = false;
  
  final static String IMG_DIR = "media/Buildings/ecologist/";
  final static ImageAsset ICON = ImageAsset.fromImage(
    Nursery.class, "media/GUI/Buttons/nursery_button.gif"
  );
  final static ModelAsset
    NURSERY_MODEL = CutoutModel.fromImage(
      Nursery.class, IMG_DIR+"nursery.png", 2, 1
    );
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    Nursery.class, "nursery",
    "Nursery", UIConstants.TYPE_UNPLACED, ICON,
    "Nurseries secure a high-quality food source from plant crops, but need "+
    "space, hard labour and fertile soils.",
    2, 1, Structure.IS_ZONED,
    Owner.TIER_FACILITY, 25,
    5
  );
  
  final static int
    MIN_CLAIM_SIDE = BLUEPRINT.size + 4,
    MAX_CLAIM_SIDE = BLUEPRINT.size + 8;
  
  final public static Conversion
    LAND_TO_CARBS = new Conversion(
      BLUEPRINT, "land_to_carbs",
      10, HARD_LABOUR, 5, CULTIVATION, TO, 1, CARBS
    ),
    LAND_TO_GREENS = new Conversion(
      BLUEPRINT, "land_to_greens",
      10, HARD_LABOUR, 5, CULTIVATION, TO, 1, GREENS
    );
  
  
  
  
  public Nursery(Base base) {
    super(BLUEPRINT, base, MIN_CLAIM_SIDE, MAX_CLAIM_SIDE);
    staff.setShiftType(SHIFTS_BY_DAY);
    attachModel(NURSERY_MODEL);
  }
  
  
  public Nursery(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Placement and supply-demand functions-
    */
  final static Siting SITING = new Siting(BLUEPRINT) {
    
    public float ratePointDemand(Base base, Target point, boolean exact) {
      final Stage world = point.world();
      final Tile under = world.tileAt(point);
      
      final Venue station = (Venue) world.presences.nearestMatch(
        BotanicalStation.class, point, -1
      );
      if (station == null || station.base() != base) return -1;
      final float distance = Spacing.distance(point, station);
      
      float rating = super.ratePointDemand(base, point, exact);
      rating *= world.terrain().fertilitySample(under) * 2;
      rating /= 1 + (distance / Stage.ZONE_SIZE);
      return rating;
    }
  };
  
  
  public boolean preventsClaimBy(Venue other) {
    if (other instanceof BotanicalStation) return false;
    return super.preventsClaimBy(other);
  }
  
  
  public int owningTier() {
    return Owner.TIER_PRIVATE;
  }
  
  
  
  /**  Other general update methods-
    */
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    structure.setAmbienceVal(Ambience.MILD_AMBIENCE);
  }
  
  
  public boolean couldPlant(Tile t) {
    return claimDivision().useType(t, areaClaimed()) > 0;
  }
  
  
  public boolean shouldCover(Tile t) {
    return claimDivision().useType(t, areaClaimed()) == 2;
  }
  
  
  public Crop plantedAt(Tile t) {
    if (t == null || ! (t.above() instanceof Crop)) return null;
    return (Crop) t.above();
  }
  
  
  protected Gathering nextHarvestFor(Actor actor) {
    final Gathering g = Gathering.asFarming(actor, this);
    return needForTending(g) > 0 ? g : null;
  }
  
  
  protected boolean needsTending(Tile t) {
    final Element e = ((Tile) t).above();
    if (! (e instanceof Crop)) return true;
    return ((Crop) e).needsTending();
  }
  
  
  
  /**  Rendering and interface methods-
    */
  final static String
    POOR_SOILS_INFO =
      "The poor soils around this Nursery will hamper growth and yield a "+
      "stingy harvest.",
    WAITING_ON_SEED_INFO =
      "The land around this Nursery will have to be seeded by your "+
      ""+Backgrounds.CULTIVATOR+"s.",
    POOR_HEALTH_INFO =
      "The crops around this Nursery are sickly.  Try to improve seed stock "+
      "at the "+BotanicalStation.BLUEPRINT+".",
    AWAITING_GROWTH_INFO =
      "The crops around this Nursery have yet to mature.  Allow them a few "+
      "days to bear fruit.";

  private String compileOutputReport() {
    final StringBuffer s = new StringBuffer();
    final int numTiles = reserved().length;
    float
      health = 0, growth = 0, fertility = 0,
      numPlant = 0, numCarbs = 0, numGreens = 0;
    
    for (Tile t : reserved()) {
      final Crop c = plantedAt(t);
      fertility += t.habitat().moisture();
      if (c == null) continue;
      
      final float perDay = c.dailyYieldEstimate(t);
      final Item yield[] = c.materials();
      numPlant++;
      health += c.health   ();
      growth += c.growStage();
      for (Item i : yield ) {
        if (i.type == CARBS ) numCarbs  += perDay;
        if (i.type == GREENS) numGreens += perDay;
      }
    }
    
    if      (fertility < (numTiles * 0.5f)) s.append(POOR_SOILS_INFO     );
    else if (numPlant == 0                ) s.append(WAITING_ON_SEED_INFO);
    else if (health    < (numPlant * 0.5f)) s.append(POOR_HEALTH_INFO    );
    else if (growth    < (numPlant * 0.5f)) s.append(AWAITING_GROWTH_INFO);
    else s.append(BLUEPRINT.description);
    
    if (numCarbs  > 0) {
      s.append("\n  Estimated "+CARBS +" per day: "+I.shorten(numCarbs , 1));
    }
    if (numGreens > 0) {
      s.append("\n  Estimated "+GREENS+" per day: "+I.shorten(numGreens, 1));
    }
    return s.toString();
  }
  
  
  public SelectionPane configSelectPane(SelectionPane panel, BaseUI UI) {
    return VenuePane.configSimplePanel(this, panel, UI, null);
  }
  
  
  public String helpInfo() {
    if (inWorld() && structure.intact()) return compileOutputReport();
    else return super.helpInfo();
  }
}



