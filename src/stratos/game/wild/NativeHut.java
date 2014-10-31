


package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.maps.*;

import static stratos.game.actors.Backgrounds.*;  //  TODO:  Work on this.
import stratos.game.campaign.Sectors;

import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;


//  You need openings for Hunters, Gatherers, and Chieftains.
//  Plus the Medicine Man, Marked One and Cargo Cultist.

//  Do I need a wider selection of structures?  ...For the moment.  For the
//  sake of safety.  Might expand on functions later.


public class NativeHut extends Venue {
  
  
  final static String IMG_DIR = "media/Buildings/lairs and ruins/";
  final static ModelAsset
    HUT_MODELS[][] = CutoutModel.fromImageGrid(
      NativeHut.class, IMG_DIR+"all_native_huts.png", 3, 3, 1, 1
    );
  
  final static String TRIBE_NAMES[] = {
    //"Homaxquin (Cloud Eaters)",
    "Hqon (Children of Rust)",
    "Ai Baru (Sand Runners)",
    "Ybetsi (The Painted)"
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
    TYPE_HUT    = 0,
    TYPE_HALL   = 1,
    TYPE_SHRINE = 2,
    HUT_OCCUPANCY  = 2;
  
  final int type, tribeID;
  
  
  
  public static NativeHut newHut(NativeHall parent) {
    final NativeHut hut = new NativeHut(
      3, 2, TYPE_HUT, parent.tribeID, parent.base()
    );
    parent.children.include(hut);
    return hut;
  }
  
  
  public static NativeHall newHall(int tribeID, Base base) {
    return new NativeHall(tribeID, base);
  }
  
  
  
  protected NativeHut(
    int size, int height, int type, int tribeID, Base base
  ) {
    super(size, height, ENTRANCE_SOUTH, base);
    this.type = type;
    this.tribeID = tribeID;
    personnel.setShiftType(SHIFTS_ALWAYS);
    
    final int varID = nextVar++ % 2;
    ModelAsset model = null;
    if (type == TYPE_HUT ) model = HUT_MODELS[tribeID][varID];
    if (type == TYPE_HALL) model = HUT_MODELS[tribeID][2];
    attachModel(model);
    sprite().scale = size;
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
  
  
  
  /**  Placement and construction-
    */
  
  //  Chieftain's Halls need to assess the total fertility of the surrounding
  //  area and constrain populations accordingly (or go to war.)
  
  //  TODO:  ADAPT THIS CODE
  /*
  public static Batch <Ruins> placeRuins(
    final World world, final int maxPlaced
  ) {
    final Presences presences = world.presences;
    final Batch <Ruins> placed = new Batch <Ruins> ();
    
    final SitingPass siting = new SitingPass() {
      int numSited = 0;
      
      
      protected float rateSite(Tile centre) {
        if (verbose) I.say("Rating site at: "+centre);
        final Venue nearest = (Venue) presences.nearestMatch(
          Venue.class, centre, -1
        );
        if (nearest != null) {
          final float distance = Spacing.distance(nearest, centre);
          if (verbose) I.say("Neighbour is: "+nearest+", distance: "+distance);
          if (distance < MIN_RUINS_SPACING) return -1;
        }
        float rating = 2;
        rating -= world.terrain().fertilitySample(centre);
        rating += world.terrain().habitatSample(centre, Habitat.CURSED_EARTH);
        return rating;
      }
      
      
      protected boolean createSite(Tile centre) {
        final float rating = rateSite(centre);
        if (verbose) {
          I.say("Trying to place ruins at "+centre+", rating "+rating);
        }
        if (rating <= 0) return false;
        
        final boolean minor = numSited >= maxPlaced / 2;
        int maxRuins = (minor ? 3 : 1) + Rand.index(3);
        final Batch <Venue> ruins = new Batch <Venue> ();
        while (maxRuins-- > 0) {
          final Ruins r = new Ruins();
          Placement.establishVenue(r, centre.x, centre.y, true, world);
          if (r.inWorld()) {
            if (verbose) I.say("  Ruin established at: "+r.origin());
            ruins.add(r);
            placed.add(r);
          }
        }
        
        //  TODO:  Slag/wreckage must be done in a distinct pass...
        for (Venue r : ruins) for (Tile t : world.tilesIn(r.area(), true)) {
          Habitat h = Rand.yes() ? Habitat.CURSED_EARTH : Habitat.DUNE;
          world.terrain().setHabitat(t, h);
        }
        populateArtilects(world, ruins, minor);
        numSited++;
        return ruins.size() > 0;
      }
    };
    siting.applyPassTo(world, maxPlaced);
    return placed;
  }
  //*/
  
  public static Batch <NativeHut> establishSites(
    final int tribeID, final Stage world
  ) {
    final Base natives = Base.baseWithName(world, Base.KEY_NATIVES, true);
    final int res = Stage.SECTOR_SIZE / 2;
    final Batch <NativeHut> placed = new Batch <NativeHut> ();
    
    final RandomScan scan = new RandomScan(world.size / res) {
      protected void scanAt(int x, int y) {
        Tile free = world.tileAt(x * res, y * res);
        free = Spacing.pickRandomTile(free, res / 2, world);
        free = Spacing.nearestOpenTile(free, free);
        if (free == null) return;
        NativeHall hall = NativeHut.newHall(tribeID, natives);
        
        if (! Placement.findClearanceFor(hall, free, world)) return;
        hall.updatePopEstimate(world);
        final int numHuts = hall.idealNumHuts();
        
        if (numHuts <= 0) return;
        if (Placement.establishVenue(hall, free, true, world) != null) {
          populateHut(hall, hall);
          
          for (int n = numHuts; n-- > 0;) {
            final NativeHut hut = NativeHut.newHut(hall);
            Placement.establishVenue(hut, hall.origin(), true, world);
            
            if (hut.inWorld()) {
              populateHut(hut, hall);
              placed.add(hut);
            }
          }
          hall = null;
        }
        
        //  TODO:  Also set up initial relationships.
      }
    };
    scan.doFullScan();
    
    return placed;
  }
  
  
  public static Batch <Actor> populateHut(NativeHut hut, NativeHall parent) {
    final Batch <Actor> populace = new Batch <Actor> ();
    final Stage world = hut.world();
    final Background NB = NATIVE_BIRTH, NH = Sectors.PLANET_DIAPSOR;
    Background cleric = Backgrounds.SHAMAN;
    /*
    if (parent.tribeID == TRIBE_WASTES || Rand.index(5) == 0) {
      cleric = Rand.yes() ? Backgrounds.C CARGO_CULTIST : MUTANT_PARIAH;
    }
    //*/
    
    int numHab = 1 + Rand.index(3);
    while (numHab-- > 0) {
      boolean male = Rand.index(6) < 2;
      Background b = null;
      if (parent == hut) {
        b = male ? CHIEFTAIN : cleric;
      }
      else {
        b = male ? HUNTER : GATHERER;
      }
      if (Rand.index(5) != 0) male = ! male;
      
      final Career c = new Career(b, NB, NH, male ? MALE_BIRTH : FEMALE_BIRTH);
      final Human lives = new Human(c, hut.base());
      lives.mind.setHome(hut);
      lives.mind.setWork(hut);
      lives.enterWorldAt(hut, world);
      lives.goAboard(hut, world);
      populace.add(lives);
    }
    return populace;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public Behaviour jobFor(Actor actor) {
    return null;
  }
  
  
  public Background[] careers() { return null; }
  public Traded[] services() { return null; }
  
  
  protected void updatePaving(boolean inWorld) {
    if (! inWorld) {
      base().paveRoutes.updatePerimeter(this, null, false);
      return;
    }
    
    final Batch <Tile> toPave = new Batch <Tile> ();
    for (Tile t : Spacing.perimeter(footprint(), world)) {
      if (t.blocked()) continue;
      boolean between = false;
      for (int n : N_INDEX) {
        final int o = (n + 4) % 8;
        final Tile
          a = world.tileAt(t.x + N_X[n], t.y + N_Y[n]),
          b = world.tileAt(t.x + N_X[o], t.y + N_Y[o]);
        between =
          (a != null && a.onTop() instanceof NativeHut) &&
          (b != null && b.onTop() instanceof NativeHut);
        if (between) break;
      }
      if (between) toPave.add(t);
    }
    
    base().paveRoutes.updatePerimeter(this, toPave, true);
  }



  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Native Hutment";
  }
  
  
  public Composite portrait(BaseUI UI) {
    return null;
  }
  
  
  public String helpInfo() {
    return
      "Native Hutments are simple but robust shelters constructed from local "+
      "materials by indigenous primitives.";
  }
  
  
  public String buildCategory() {
    return UIConstants.TYPE_HIDDEN;
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    if (destroyed() || ! inWorld()) return;
    Selection.renderPlane(
      rendering, position(null), (xdim() / 2f) + 1,
      hovered ? Colour.transparency(0.5f) : Colour.WHITE,
      Selection.SELECT_CIRCLE
    );
  }
}





