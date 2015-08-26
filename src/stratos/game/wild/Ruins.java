

package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;




public class Ruins extends Venue {
  
  
  
  /**  Construction and save/load methods-
    */
  private static boolean
    placeVerbose  = false,
    updateVerbose = false;
  
  final static ModelAsset MODEL_RUINS[] = CutoutModel.fromImages(
    Ruins.class, "media/Buildings/lairs and ruins/", 4, 2, false,
    "ruins_a.png",
    "ruins_b.png",
    "ruins_c.png"
  );
  
  final static int
    MIN_RUINS_SPACING = (int) (Stage.ZONE_SIZE * 0.75f);
  
  private static int NI = (int) (Rand.unseededNum() * 3);
  
  
  public Ruins(Base base) {
    super(VENUE_BLUEPRINTS[0], base);
    staff.setShiftType(SHIFTS_BY_24_HOUR);
    final int index = (NI++ + Rand.index(1)) % 3;
    attachSprite(MODEL_RUINS[index].makeSprite());
  }
  
  
  public Ruins(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Situation and claims-management-
    */
  final public static Blueprint VENUE_BLUEPRINTS[];
  static {
    VENUE_BLUEPRINTS = new Blueprint[1];
    VENUE_BLUEPRINTS[0] = new Blueprint(
      Ruins.class, "ruins",
      "Ancient Ruins", UIConstants.TYPE_RUINS, null,
      "Ancient ruins cover the landscape of many worlds in regions irradiated "+
      "by nuclear fire or blighted by biological warfare.  Strange and "+
      "dangerous beings often haunt such forsaken places.",
      4, 2, Structure.IS_ANCIENT,
      Owner.TIER_FACILITY, 500,
      25
    ) {
      public Venue createVenue(Base base) {
        final Venue sample = new Ruins(base);
        sample.structure.setState(Structure.STATE_INTACT, Rand.avgNums(2));
        return sample;
      }
    };
    
    final Siting siting = new Siting(VENUE_BLUEPRINTS[0]) {
      public float rateSettlementDemand(Base base) {
        return 0;
      }
      
      public float ratePointDemand(Base base, Target point, boolean exact) {
        final boolean report = placeVerbose && (point instanceof StageRegion);
        
        final Stage world = point.world();
        final Tile under = world.tileAt(point);
        float rating = 2;
        rating -= world.terrain().fertilitySample(under);
        rating += world.terrain().habitatSample(under, Habitat.CURSED_EARTH);
        rating *= SiteUtils.worldOverlap(point, world, MIN_RUINS_SPACING);
        
        if (report) {
          I.say("\nRating ruins: "+this);
          I.say("  Point evaluated: "+point);
          I.say("  Rating is:       "+rating);
        }
        return rating;
      }
    };
    VENUE_BLUEPRINTS[0].linkWith(siting);
  }
  
  
  public Box2D areaClaimed() {
    return new Box2D(footprint()).expandBy(MIN_RUINS_SPACING);
  }
  
  
  public boolean preventsClaimBy(Venue other) {
    if (other instanceof Ruins) return false;
    return true;
  }
  
  
  protected void updatePaving(boolean inWorld) {}
  
  
  
  /**  Behavioural routines-
    */
  public Behaviour jobFor(Actor actor) {
    //  TODO:  Fill this in.
    return null;
  }
  
  
  public float crowdRating(Actor actor, Background b) {
    if (b == Backgrounds.AS_RESIDENT) {
      if (staff.isWorker(actor)) return 0;
      else return 1;
    }
    return super.crowdRating(actor, b);
  }
  
  
  protected int numOpenings(Background b) {
    final boolean report = updateVerbose && I.talkAbout == this;
    //
    //  We 'salt' this estimate in a semi-random but deterministic way by
    //  referring to terrain variation.
    
    //  TODO:  What is going on here?
    
    final int hired = staff.numHired(b);
    float spaceLevel = structure.repairLevel();
    spaceLevel *= 1 + world.terrain().varAt(origin());
    spaceLevel *= 1f / StageTerrain.TILE_VAR_LIMIT;
    
    int space = 0;
    if (b == Cranial.SPECIES) space = 0;// (int) (spaceLevel * 1);
    if (b == Tripod .SPECIES) space = 1;// (int) (spaceLevel * 3);
    if (b == Drone  .SPECIES) space = 2;// (int) (spaceLevel * 5);
    if (report) I.say("  "+space+" openings for "+b+" at "+this);
    return space - hired;
  }
  
  
  //  TODO:  Allow for mutants, etc.?
  public Background[] careers() { return Species.ARTILECT_SPECIES; }
  public Traded[] services() { return null; }
  
  
  public static void populateRuins(
    Stage world, int numRuins, Species... inhabit
  ) {
    final Base artilects = Base.artilects(world);
    final Batch <Venue> placed = artilects.setup.doPlacementsFor(
      Ruins.VENUE_BLUEPRINTS[0], numRuins
    );
    artilects.setup.fillVacancies(placed, true);
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public SelectionPane configSelectPane(SelectionPane panel, BaseUI UI) {
    return VenuePane.configSimplePanel(this, panel, UI, null);
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    if (destroyed() || origin() == null) return;
    BaseUI.current().selection.renderCircleOnGround(rendering, this, hovered);
  }
}



