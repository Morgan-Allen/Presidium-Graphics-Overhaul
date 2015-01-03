

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
    MIN_RUINS_SPACING = (int) (Stage.SECTOR_SIZE * 0.75f);
  
  private static int NI = (int) (Rand.unseededNum() * 3);
  
  
  public Ruins(Base base) {
    super(4, 2, ENTRANCE_EAST, base);
    structure.setupStats(500, 25, 0, 0, Structure.TYPE_ANCIENT);
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
  final public static VenueProfile VENUE_PROFILES[];
  static {
    VENUE_PROFILES = new VenueProfile[1];
    VENUE_PROFILES[0] = new VenueProfile(Ruins.class, Ruins.class) {
      public Venue sampleVenue(Base base) {
        final Venue sample = new Ruins(base);
        sample.structure.setState(Structure.STATE_INTACT, Rand.avgNums(2));
        return sample;
      }
    };
  }
  
  
  protected Box2D areaClaimed() {
    return new Box2D(footprint()).expandBy(MIN_RUINS_SPACING);
  }
  
  
  public boolean preventsClaimBy(Venue other) {
    if (other instanceof Ruins) return false;
    return super.preventsClaimBy(other);
  }
  
  
  public float ratePlacing(Target point, boolean exact) {
    final boolean report = placeVerbose && (point instanceof StageSection);
    if (report) {
      I.say("\nRating are for "+this);
      I.say("  SECTION IS: "+point);
    }
    
    final Stage world = point.world();
    final Tile under = world.tileAt(point);
    float rating = 2;
    rating -= world.terrain().fertilitySample(under);
    rating += world.terrain().habitatSample(under, Habitat.CURSED_EARTH);
    
    if (report) {
      I.say("  Rating is: "+rating);
    }
    return rating;
  }
  
  
  protected void updatePaving(boolean inWorld) {}
  
  
  
  /**  Behavioural routines-
    */
  public Behaviour jobFor(Actor actor, boolean onShift) {
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
    float spaceLevel = structure.repairLevel();
    spaceLevel *= 1 + world.terrain().varAt(origin());
    spaceLevel *= 1f / StageTerrain.TILE_VAR_LIMIT;
    int space = 0;
    if (b == Species.CRANIAL) space = 0;// (int) (spaceLevel * 1);
    if (b == Species.TRIPOD ) space = 1;// (int) (spaceLevel * 3);
    if (b == Species.DRONE  ) space = 2;//(int) (spaceLevel * 5);
    if (report) I.say("  "+space+" openings for "+b+" at "+this);
    return space;
  }
  
  
  //  TODO:  Allow for mutants, etc.?
  public Background[] careers() { return Species.ARTILECT_SPECIES; }
  public Traded[] services() { return null; }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Ancient Ruins";
  }
  
  
  public Composite portrait(BaseUI UI) {
    return null;
  }
  
  
  public String helpInfo() {
    return
      "Ancient ruins cover the landscape of many worlds in regions irradiated "+
      "by nuclear fire or blighted by biological warfare.  Strange and "+
      "dangerous beings often haunt such forsaken places.";
  }
  
  
  public String objectCategory() { return UIConstants.TYPE_HIDDEN; }
  
  
  public SelectionInfoPane configPanel(SelectionInfoPane panel, BaseUI UI) {
    //return VenueDescription.configStandardPanel(this, panel, UI);
    return VenueDescription.configSimplePanel(this, panel, UI, null);
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    if (destroyed() || ! inWorld()) return;

    BaseUI.current().selection.renderPlane(
      rendering, world,
      position(null), (xdim() / 2f) + 1,
      hovered ? Colour.transparency(0.5f) : Colour.WHITE,
      Selection.SELECT_CIRCLE,
      true, this
    );
  }
}



