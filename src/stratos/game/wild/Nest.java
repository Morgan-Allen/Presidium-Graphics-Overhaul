/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.base.BaseDemands;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.SiteUtils;
import stratos.game.maps.Siting;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



//  TODO:  Use the FindHome behaviour here?

//  TODO:  Move some of these methods out to a NestUtils class to reduce
//  clutter.


public class Nest extends Venue {
  
  /**  Fields, constructors, and save/load methods-
    */
  private static boolean
    ratingVerbose = false,
    updateVerbose = false;
  
  final public static int
    DEFAULT_FORAGE_DIST = Stage.ZONE_SIZE / 2,
    PREDATOR_SEPARATION = Stage.ZONE_SIZE * 2,
    MIN_SEPARATION      = 2,
    
    BROWSER_TO_FLORA_RATIO   = 50,
    DEFAULT_BROWSER_SPECIES  = 4 ,
    PREDATOR_TO_PREY_RATIO   = 4 ,
    DEFAULT_PREDATOR_SPECIES = 2 ,
    
    DEFAULT_BREED_INTERVAL = Stage.STANDARD_DAY_LENGTH;
  
  
  final Species species;
  private float cachedIdealPop = -1;
  
  
  /**  More typical construction and save/load methods-
    */
  protected Nest(
    Blueprint blueprint, Base base,
    Species species, ModelAsset lairModel
  ) {
    super(blueprint, base);
    this.species = species;
    attachSprite(lairModel.makeSprite());
  }
  
  
  public Nest(Session s) throws Exception {
    super(s);
    species = (Species) s.loadObject();
    cachedIdealPop = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(species);
    s.saveFloat(cachedIdealPop);
  }
  
  
  
  /**  Behavioural assignments (all null in this case.)
    */
  protected Behaviour jobFor(Actor actor) { return null; }
  public Traded[] services() { return null; }
  
  
  public Background[] careers() {
    return new Background[] { species };
  }
  
  
  //  TODO:  Get rid of these.  Fauna are residents, not employees.
  
  public int numOpenings(Background b) {
    final int nO = super.numOpenings(b);
    if (cachedIdealPop == -1) return 0;
    if (b == species) return nO + (int) cachedIdealPop;
    return 0;
  }
  
  
  public float crowdRating(Actor a, Background b) {
    if (b == species) return super.crowdRating(a, b);
    if (b == Backgrounds.AS_RESIDENT) return 0;
    return 1;
  }
  
  
  
  //  Only allow entry to the same species.
  public boolean allowsEntry(Mobile m) {
    if (! structure.intact()) return false;
    return (m instanceof Actor) && ((Actor) m).species() == species;
  }
  
  //  Nests have no road connections.
  protected void updatePaving(boolean inWorld) {}
  
  
  
  /**  Methods for determining crowding and site placement-
    */
  private static float idealPopulation(
    Target point, Species species, Stage world
  ) {
    //
    //  If possible, we return the cached value associated with a given nest:
    final Nest n = (point instanceof Nest) ? ((Nest) point) : null;
    if (n != null) {
      final float estimate = n.cachedIdealPop;
      if (estimate != -1) return estimate;
    }
    //
    //  Otherwise, either use local fertility for browsers, or the abundance of
    //  browsers themselves for predators.
    final Base  base        = Base.wildlife(world);
    final float forageRange = forageRange(species);
    
    float foodSupply = 0;
    if (species.browser()) {
      if (point == null) {
        foodSupply = world.terrain().globalFertility();
        foodSupply /= BROWSER_TO_FLORA_RATIO * DEFAULT_BROWSER_SPECIES;
      }
      else {
        foodSupply = world.terrain().fertilitySample(world.tileAt(point));
        foodSupply *= (forageRange * forageRange * 4) / BROWSER_TO_FLORA_RATIO;
      }
    }
    else {
      if (point == null) {
        foodSupply = base.demands.globalSupply(Species.KEY_BROWSER);
        foodSupply /= DEFAULT_PREDATOR_SPECIES;
      }
      else foodSupply = base.demands.supplyAround(
        point, Species.KEY_BROWSER, forageRange
      );
      foodSupply /= PREDATOR_TO_PREY_RATIO;
    }
    
    //
    //  Include the effects of border-positioning-
    if (point != null) {
      foodSupply *= SiteUtils.worldOverlap(point, world, (int) forageRange);
    }
    //
    //  If possible, we cache the result obtained for later use:
    final float estimate = (foodSupply / species.metabolism());
    if (n != null) n.cachedIdealPop = estimate;
    return estimate;
  }
  

  public static float crowdingFor(Target point, Species species, Stage world) {
    if (point == null || species == null) return 1;
    final boolean report = ratingVerbose && I.talkAbout == point;
    
    final float idealPop = idealPopulation(point, species, world);
    if ((int) idealPop <= 0) return 1;
    
    final Base   base     = Base.wildlife(world);
    final float  mass     = species.metabolism();
    final String category = species.type.name() ;
    final float
      allType     = base.demands.supplyAround(point, category, -1),
      allSpecies  = base.demands.supplyAround(point, species , -1),
      rarity      = Nums.clamp(1 - (allSpecies / allType), 0, 1),
      competition = allType / ((1 + rarity) * mass);
    
    if (report) I.reportVars(
      "\nGetting crowding at "+point+" for "+species, "  ",
      "all of type"     , allType    ,
      "all of species"  , allSpecies ,
      "rarity"          , rarity     ,
      "competition"     , competition,
      "metabolic mass"  , mass       ,
      "ideal population", idealPop   
    );
    return competition / (int) idealPop;
  }
  
  
  public static float crowdingFor(Actor fauna) {
    final Target home = fauna.mind.home();
    if (! (home instanceof Nest)) return 0.49f;
    final Nest nest = (Nest) home;
    if (nest.cachedIdealPop == -1) return 0;
    if (nest.cachedIdealPop ==  0) return 1;
    return nest.staff.lodgers().size() * 1f / nest.cachedIdealPop;
  }
  
  
  protected void impingeDemands(BaseDemands demands, int period) {
    final float  idealPop = idealPopulation(this, species, world);
    final float  mass     = species.metabolism() * idealPop;
    final String category = species.type.name();
    demands.impingeSupply(species , mass, period, this);
    demands.impingeSupply(category, mass, period, this);
  }
  
  
  public static int forageRange(Species s) {
    return s.predator() ?
      PREDATOR_SEPARATION :
      DEFAULT_FORAGE_DIST ;
  }
  
  
  
  /**  Utility methods for Nest-establishment.
    */
  protected static Blueprint constructBlueprint(
    int size, int high, final Species s, final ModelAsset model
  ) {
    final Blueprint blueprint = new Blueprint(
      Nest.class, s.name+"_nest",
      s.name+" Nest", UIConstants.TYPE_WIP, null, s.info,
      size, high, Structure.IS_WILD,
      Owner.TIER_PRIVATE, 100,
      5
    ) {
      public Venue createVenue(Base base) {
        return new Nest(this, base, s, model);
      }
    };
    
    //  TODO:  Just have a fixed population-size per nest?
    
    //  TODO:  Consider moving the Sitings out to the individual Species'
    //  class-implementations.  (Blueprints too.)
    
    final Siting siting = new Siting(blueprint) {
      
      public float rateSettlementDemand(Base base) {
        return idealPopulation(null, s, base.world);
      }
      
      public float ratePointDemand(Base base, Target point, boolean exact) {
        
        final Stage world = point.world();
        Target other = world.presences.nearestMatch(Venue.class, point, -1);
        final float distance;
        if (other == null) distance = world.size;
        else distance = Spacing.distance(point, other);
        
        if (distance <= MIN_SEPARATION) return -1;
        float distMod = 1;
        if (other instanceof Nest) {
          final Nest near = (Nest) other;
          final float spacing = Nums.max(
            forageRange(s),
            forageRange(near.species)
          );
          if (distance < spacing) distMod *= 1 - (distance / spacing);
        }
        else if (distance <= PREDATOR_SEPARATION) return -1;

        final float
          idealPop = idealPopulation(point, s, world),
          crowding = crowdingFor    (point, s, world),
          mass     = s.metabolism(),
          rating   = ((int) idealPop) * mass * (1 - crowding);
        
        return rating * distMod;
      }
    };
    blueprint.linkWith(siting);
    return blueprint;
  }
  
  
  private static void populate(Stage world, Species with[], Species.Type type) {
    final Base wildlife = Base.wildlife(world);
    
    final Batch <Blueprint> blueprints = new Batch <Blueprint> ();
    for (Species s : with) if (s.type == type) {
      blueprints.add(s.nestBlueprint());
    }
    final Batch <Venue> placed = wildlife.setup.doFullPlacements(
      blueprints.toArray(Blueprint.class)
    );
    wildlife.setup.fillVacancies(placed, true);
  }
  
  
  public static void populateFauna(Stage world, Species... available) {
    populate(world, available, Species.Type.BROWSER );
    populate(world, available, Species.Type.PREDATOR);
  }
  
  
  public static Nest findNestFor(Fauna fauna) {
    final Pick <Nest> pick = new Pick <Nest> (null, 0);
    
    final Stage world = fauna.world();
    final Species species = fauna.species;
    final Batch <Nest> nests = new Batch <Nest> ();
    world.presences.sampleFromMap(fauna, world, 5, nests, Nest.class);
    //
    //  First, we check the rating for our current home-
    if (fauna.mind.home() instanceof Nest) {
      final Nest home = (Nest) fauna.mind.home();
      final float rating = 1 - Nest.crowdingFor(home, species, world);
      pick.compare(home, rating * 1.1f);
    }
    //
    //  Then compare against any suitable nests nearby.
    for (Nest v : nests) if (v.species == species) {
      pick.compare(v, 1 - Nest.crowdingFor(v, species, world));
    }
    //
    //  And finally, we consider the attraction of establishing an entirely new
    //  nest.
    final Base  wild    = Base.wildlife(world);
    final float range   = Nest.forageRange(species) * 2;
    final Tile  at      = Spacing.pickRandomTile(fauna, range, world);
    final Nest  newNest = (Nest) fauna.species.nestBlueprint().createVenue(wild);
    newNest.setPosition(at.x, at.y, world);
    if (newNest.canPlace()) {
      pick.compare(newNest, 1 - Nest.crowdingFor(at, species, world));
    }
    
    return pick.result();
  }
  
  
  
  /**  Overrides for standard venue methods-
    */
  public boolean enterWorldAt(int x, int y, Stage world, boolean intact) {
    if (! super.enterWorldAt(x, y, world, intact)) return false;
    impingeDemands(base.demands, -1);
    return true;
  }
  
  
  public Box2D areaClaimed() {
    return new Box2D(footprint()).expandBy(forageRange(species));
  }
  
  
  public boolean preventsClaimBy(Venue other) {
    final float distance = Spacing.distance(this, other);
    if (other instanceof Nest) return false;
    else return distance <= PREDATOR_SEPARATION;
  }

  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    final int INTERVAL = 10;
    if (numUpdates % INTERVAL == 0 && ! instant) {
      cachedIdealPop = -1;
      cachedIdealPop = idealPopulation(this, species, world);
      impingeDemands(base.demands, INTERVAL);
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public SelectionPane configSelectPane(SelectionPane panel, BaseUI UI) {
    panel = VenuePane.configSimplePanel(this, panel, UI, null);
    
    final Description d = panel.detail(), l = panel.listing();
    float idealPop = cachedIdealPop;
    int actualPop = staff.lodgers().size();
    l.append("\n\n  Nesting: ("+actualPop+"/"+idealPop+")");
    return panel;
  }
  

  public void renderSelection(Rendering rendering, boolean hovered) {
    if (destroyed() || origin() == null) return;
    BaseUI.current().selection.renderCircleOnGround(rendering, this, hovered);
  }
}



