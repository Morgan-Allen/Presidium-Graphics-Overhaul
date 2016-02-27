


package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.craft.Economy.*;



//  You need openings for Hunters, Gatherers, and Chieftains.
//  Plus the Medicine Man, Marked One and Cargo Cultist.

//  Do I need a wider selection of structures?  ...For the moment.  For the
//  sake of safety.  Might expand on functions later.


public class NativeHut extends Venue {
  
  
  /**  Data fields and special constants-
    */
  private static boolean
    placeVerbose = false;
  
  final static String IMG_DIR = "media/Buildings/lairs and ruins/";
  final static ModelAsset
    HUT_MODELS[][] = CutoutModel.fromImageGrid(
      NativeHut.class, "native_huts_models",
      IMG_DIR+"all_native_huts.png",
      3, 3, 1, 1, false
    );
  
  final public static String TRIBE_NAMES[] = {
    //"Homaxquin " , //-"Cloud Eaters"
    "H'Kon Tribe"  , //-"Children of Rust"
    "Ai Amru Tribe", //-"Sand Runners"
    "Ybetse Tribe" , //-"The Painted"
  };
  
  final static CutoutModel[][][] HABITAT_KEYS = {
    //Habitat.TUNDRA_FLORA_MODELS,
    Habitat.WASTES_FLORA_MODELS,
    Habitat.DESERT_FLORA_MODELS,
    Habitat.FOREST_FLORA_MODELS,
  };
  private static int nextVar = 0;
  
  
  final public static int
    TRIBE_WASTES = 0,
    TRIBE_DESERT = 1,
    TRIBE_FOREST = 2,
    NUM_TRIBES   = 3,
    
    TYPE_PLACES  = -1,
    TYPE_HUT     =  0,
    TYPE_HALL    =  1,
    TYPE_SHRINE  =  2,
    
    HUT_OCCUPANCY  = 2,
    HALL_OCCUPANCY = 4,
    HALL_EXCLUSION = Stage.ZONE_SIZE / 2,
    RADIUS_CLAIMED = Stage.ZONE_SIZE / 2;
  
  
  private int type, tribeID;
  
  
  
  /**  Standard constructors and save/load functions-
    */
  protected NativeHut(
    Blueprint blueprint, int type, int tribeID, Base base
  ) {
    super(blueprint, base);
    staff.setShiftType(SHIFTS_BY_DAY);
    this.type = type;
    this.tribeID = tribeID;
    
    if (type != TYPE_PLACES && tribeID >= 0) {
      final int varID = nextVar++ % 2;
      ModelAsset model = null;
      if (type == TYPE_HUT ) model = HUT_MODELS[tribeID][varID];
      if (type == TYPE_HALL) model = HUT_MODELS[tribeID][2];
      attachModel(model);
      sprite().scale = size;
    }
  }
  
  
  
  public NativeHut(Session s) throws Exception {
    super(s);
    this.type = s.loadInt();
    this.tribeID = s.loadInt();
    sprite().scale = size;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt(type);
    s.saveInt(tribeID);
  }
  
  
  protected int tribeID() {
    return tribeID;
  }
  
  
  protected int hutType() {
    return type;
  }

  
  
  /**  Specialised constructors for use during base setup-
    */
  final public static Blueprint VENUE_BLUEPRINTS[][];
  static {
    final Batch <Blueprint> allProfiles = new Batch <Blueprint> ();
    VENUE_BLUEPRINTS = new Blueprint[NUM_TRIBES][];
    
    for (int n = NUM_TRIBES; n-- > 0;) {
      final int tribeID = n;
      final String tribeName = TRIBE_NAMES[tribeID];
      
      allProfiles.add(new Blueprint(
        NativeHut.class, "hut_"+tribeID,
        "Native Hut ("+tribeName+")", Target.TYPE_NATIVE, null,
        "Native Hutments are simple but robust shelters constructed by "+
        "indigenous primitives.",
        2, 2, Structure.IS_CRAFTED,
        Owner.TIER_FACILITY, 75,
        3
      ) {
        public Venue createVenue(Base base) {
          return newHall(tribeID, base);
        }
      });
      allProfiles.add(new Blueprint(
        NativeHut.class, "all_"+tribeID,
        "Chief's Hall ("+tribeName+")", Target.TYPE_NATIVE, null,
        "Native settlements will often have a central meeting place where "+
        "the tribe's leadership and elders will gather to make decisions.",
        3, 2, Structure.IS_CRAFTED,
        Owner.TIER_FACILITY, 150,
        5
      ) {
        public Venue createVenue(Base base) {
          return newHut(tribeID, base);
        }
      });
      VENUE_BLUEPRINTS[tribeID] = allProfiles.toArray(Blueprint.class);
      allProfiles.clear();
    }
  }
  final public static Blueprint
    TRIBE_WASTES_BLUEPRINTS[] = VENUE_BLUEPRINTS[TRIBE_WASTES],
    TRIBE_DESERT_BLUEPRINTS[] = VENUE_BLUEPRINTS[TRIBE_DESERT],
    TRIBE_FOREST_BLUEPRINTS[] = VENUE_BLUEPRINTS[TRIBE_FOREST];
  
  
  public static NativeHut newHut(int tribeID, Base base) {
    return new NativeHut(
      VENUE_BLUEPRINTS[tribeID][0], TYPE_HUT, tribeID, base
    );
  }
  
  
  public static NativeHall newHall(int tribeID, Base base) {
    return new NativeHall(tribeID, base);
  }
  
  
  
  /**  Placement and construction-
    */
  public Box2D areaClaimed() {
    return new Box2D(footprint()).expandBy(RADIUS_CLAIMED);
  }
  
  
  public boolean preventsClaimBy(Venue other) {
    if (other instanceof NativeHut) return false;
    return super.preventsClaimBy(other);
  }
  
  
  protected void updatePaving(boolean inWorld) {}
  
  
  public float ratePlacing(Target point, boolean exact) {
    //
    //  Allocate some basic reference variables-
    final boolean report = placeVerbose && ! exact;
    final Stage world = point.world();
    final boolean isHall = this instanceof NativeHall;
    if (report) I.say("\nRating site for native hut at: "+point);
    //
    //  Native halls should not be too close together, and native huts should
    //  be close to a hall.
    final int HE = HALL_EXCLUSION;
    final NativeHall hall = (NativeHall) world.presences.nearestMatch(
      NativeHall.class, point, RADIUS_CLAIMED * 2
    );
    final float distance = hall == null ? 0 : Spacing.distance(point, hall);
    if (isHall) {
      if (report) I.say("  Nearest hall to hall: "+hall);
      if (hall != null && distance < HE) return -1;
    }
    else {
      if (report) I.say("  Nearest hall to hut: "+hall);
      if (hall == null || (exact && distance > HE)) return -1;
    }
    //
    //  Favour non-radioactive areas with fertile land:
    final Tile under = world.tileAt(point);
    float rating = 2;
    if (isHall || ! exact) {
      rating += world.terrain().fertilitySample(under);
      rating -= world.terrain().habitatSample(under, Habitat.CURSED_EARTH);
    }
    //
    //  And finally, favour clustering closer to the central hall, and sites
    //  with lower crowding:
    if (! isHall) {
      rating /= 1 + (Spacing.distance(point, hall) / HE);
      if (! exact) {
        final PresenceMap map = world.presences.mapFor(NativeHut.class);
        rating /= 1 + (map.samplePopulation(point, HE) / 2f);
      }
    }
    if (report) I.say("  Rating is: "+rating);
    return rating;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public Behaviour jobFor(Actor actor) {
    if (staff.offDuty(actor)) return null;
    final Choice choice = new Choice(actor);
    final int numHome = staff.lodgers().size();
    
    if (actor.mind.vocation() == HUNTER) {
      final boolean needMeat = stocks.amountOf(PROTEIN) < numHome;
      if (needMeat) for (Target t : actor.senses.awareOf()) {
        if (Hunting.validPrey(t, actor)) {
          final Hunting hunt = Hunting.asHarvest(actor, (Actor) t, this);
          choice.add(hunt.addMotives(Plan.MOTIVE_JOB, Plan.CASUAL));
        }
      }
      if (staff.onShift(actor)) {
        choice.add(Patrolling.nextGuardPatrol(actor, this, Plan.CASUAL));
      }
    }
    
    if (actor.mind.vocation() == GATHERER) {
      final float needFood = (numHome * 2.0f) - Nums.max(
        stocks.amountOf(CARBS ),
        stocks.amountOf(GREENS)
      );
      if (needFood > 0) {
        choice.add(Gathering.asForaging(actor, this));
      }
      if (staff.onShift(actor)) {
        choice.add(new Repairs(actor, this, Qualities.HANDICRAFTS, true));
      }
    }
    
    return choice.weightedPick();
  }
  
  
  public Background[] careers() {
    return new Background[] { HUNTER, GATHERER };
  }
  
  
  public float crowdRating(Actor actor, Background background) {
    if (background == AS_RESIDENT) {
      if (staff.isWorker(actor)) return 0;
      else return 1;
    }
    return super.crowdRating(actor, background);
  }
  
  
  public int numPositions(Background b) {
    final int nO = super.numPositions(b);
    final int space = this instanceof NativeHall ?
      HALL_OCCUPANCY : HUT_OCCUPANCY
    ;
    if (b == HUNTER  ) return nO + (space / 2);
    if (b == GATHERER) return nO + (space / 2);
    return 0;
  }
  
  
  public Traded[] services() { return null; }



  /**  Rendering and interface methods-
    */
  public void renderSelection(Rendering rendering, boolean hovered) {
    if (destroyed() || origin() == null) return;
    BaseUI.current().selection.renderCircleOnGround(rendering, this, hovered);
  }
}



//  TODO:  Maybe you could use this once dirt roads are implemented?

    /*
    if (! inWorld) {
      base().paveRoutes.updatePerimeter(this, null, false);
      return;
    }
    
    final Batch <Tile> toPave = new Batch <Tile> ();
    for (Tile t : Spacing.perimeter(footprint(), world)) {
      if (t.blocked()) continue;
      boolean between = false;
      for (int n : T_INDEX) {
        final int o = (n + 4) % 8;
        final Tile
          a = world.tileAt(t.x + T_X[n], t.y + T_Y[n]),
          b = world.tileAt(t.x + T_X[o], t.y + T_Y[o]);
        between =
          (a != null && a.onTop() instanceof NativeHut) &&
          (b != null && b.onTop() instanceof NativeHut);
        if (between) break;
      }
      if (between) toPave.add(t);
    }
    
    base().paveRoutes.updatePerimeter(this, toPave, true);
    //*/

