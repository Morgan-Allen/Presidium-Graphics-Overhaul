

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
    placeVerbose  = true ,
    updateVerbose = false;
  
  final static ModelAsset MODEL_RUINS[] = CutoutModel.fromImages(
    Ruins.class, "media/Buildings/lairs and ruins/", 4, 2, false,
    "ruins_a.png",
    "ruins_b.png",
    "ruins_c.png"
  );
  private static int NI = (int) (Rand.unseededNum() * 3);
  
  final static int
    MIN_RUINS_SPACING = (int) (Stage.SECTOR_SIZE * 0.75f);
  
  
  public Ruins(Base base) {
    super(4, 2, ENTRANCE_EAST, base);
    structure.setupStats(500, 25, 0, 0, Structure.TYPE_ANCIENT);
    structure.setState(Structure.STATE_INTACT, Rand.avgNums(2));
    staff.setShiftType(SHIFTS_ALWAYS);
    final int index = (NI++ + Rand.index(1)) % 3;
    attachSprite(MODEL_RUINS[index].makeSprite());
  }
  
  
  public Ruins(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Behavioural routines-
    */
  public Behaviour jobFor(Actor actor) {
    //  TODO:  Fill this in?
    return null;
  }
  
  
  protected Box2D areaClaimed() {
    return new Box2D(footprint()).expandBy(MIN_RUINS_SPACING);
  }
  
  
  public boolean preventsClaimBy(Venue other) {
    if (other instanceof Ruins) return false;
    return super.preventsClaimBy(other);
  }
  
  
  protected void updatePaving(boolean inWorld) {}
  public Background[] careers() { return null; }
  public Traded[] services() { return null; }
  
  
  
  /**  Siting and placement-
    */
  public static Batch <Ruins> placeRuins(
    final Stage world, final int maxPlaced
  ) {
    final boolean report = placeVerbose;
    
    final Presences presences = world.presences;
    final Batch <Ruins> placed = new Batch <Ruins> ();
    final Base artilects = Base.artilects(world);
    
    final SitingPass siting = new SitingPass() {
      int numSited = 0;
      
      protected float rateSite(Tile centre) {
        if (report) I.say("Rating site at: "+centre);
        final Venue nearest = (Venue) presences.nearestMatch(
          Venue.class, centre, -1
        );
        if (nearest != null) {
          final float distance = Spacing.distance(nearest, centre);
          if (report) I.say("Neighbour is: "+nearest+", distance: "+distance);
          if (distance < MIN_RUINS_SPACING) return -1;
        }
        float rating = 2;
        rating -= world.terrain().fertilitySample(centre);
        rating += world.terrain().habitatSample(centre, Habitat.CURSED_EARTH);
        return rating;
      }
      
      
      protected boolean createSite(Tile centre) {
        final float rating = rateSite(centre);
        if (report) {
          I.say("Trying to place ruins at "+centre+", rating "+rating);
        }
        
        final boolean minor = numSited >= maxPlaced / 2;
        int maxRuins = (minor ? 3 : 1) + Rand.index(3);
        final Batch <Ruins> ruins = new Batch <Ruins> ();
        
        while (maxRuins-- > 0 && numSited < maxPlaced) {
          final Ruins r = new Ruins(artilects);
          Placement.establishVenue(r, centre.x, centre.y, true, world);
          if (r.inWorld()) {
            if (report) I.say("  Ruin established at: "+r.origin());
            ruins.add(r);
            placed.add(r);
          }
          numSited++;
        }
        
        //  TODO:  Slag/wreckage must be done in a distinct pass...
        for (Ruins r : ruins) {
          for (Tile t : world.tilesIn(r.footprint(), true)) {
            Habitat h = Rand.yes() ? Habitat.CURSED_EARTH : Habitat.DUNE;
            world.terrain().setHabitat(t, h);
          }
          populateArtilects(world, r, true);
        }
        return ruins.size() > 0;
      }
    };
    siting.applyPassTo(world, maxPlaced);
    return placed;
  }
  
  
  public static Batch <Artilect> populateArtilects(
    Stage world, Ruins ruins, boolean fillSpaces, Artilect... living
  ) {
    final boolean report = placeVerbose;
    final Batch <Artilect> populace = new Batch <Artilect> ();
    
    //  Add any artilects passed as arguments, or that there's room for
    //  afterwards.
    for (Artilect a : living) {
      a.mind.setHome(ruins);
      populace.add(a);
    }
    
    if (living == null || living.length == 0 || fillSpaces) {
      for (Species s : Species.ARTILECT_SPECIES) {
        final int space = ruins.spaceFor(s);
        if (report) I.say("  SPACE FOR "+s+" is "+space);
        for (int n = space; n-- > 0;) {
          populace.add((Artilect) s.newSpecimen(ruins.base()));
        }
      }
    }
    
    //  Then have each enter the world at the given location-
    for (Artilect a : populace) {
      a.mind.setHome(ruins);
      a.enterWorldAt(ruins, world);
      a.goAboard(ruins, world);
    }
    return populace;
  }
  
  
  protected int spaceFor(Species s) {
    final boolean report = updateVerbose;
    
    //  We 'salt' this estimate in a semi-random but deterministic way by
    //  referring to terrain variation.
    float spaceLevel = structure.repairLevel();
    spaceLevel *= 1 + world.terrain().varAt(origin());
    spaceLevel *= 1f / StageTerrain.TILE_VAR_LIMIT;
    
    int numLiving = 0;
    for (Actor a : staff.residents()) if (a.species() == s) numLiving++;
    
    int space = 0;
    if (s == Species.SPECIES_CRANIAL) space = spaceLevel > 0.5f ? 1 : 0;
    if (s == Species.SPECIES_TRIPOD ) space = 1 + (int) (spaceLevel * 1);
    if (s == Species.SPECIES_DRONE  ) space = 2 + (int) (spaceLevel * 2);
    
    if (report) I.say("\n  BASE-SPACE/NUM-LIVING: "+space+"/"+numLiving);
    return space - numLiving;
  }
  
  
  
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







