/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.content.civic.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.*;
import stratos.game.wild.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;




//             Tools.  Skills.  Plant.  Harvest.  Species.  Extract.
//  Browsing-   No.      No.     No.    Partial.    No.      Eats.
//  Foraging-   No.      Yes.    No.    Partial.    No.      Foods + Eats.
//  Farming-    Yes.     Yes.    Yes.    Full.     Crops.    Foods.
//  Forestry-   Yes.     Yes.    Yes.    None.     Trees.    No.
//  Logging-    Yes.     Yes.    No.     Full.     Trees.    Carbons.
//  Samples-    No.      Yes.    No.     None.      Any.     Gene Seed.

//  Species is only needed if you 'plant'.
//  Tools implies carry-limit.
//  Conversions determine skills needed for final processing.
//  Motives are different for each (I think.)

//  TODO:  You might consider creating one or two other custom-plans for this?


public class Gathering extends ResourceTending {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final static int
    TYPE_BROWSING  = 0,
    TYPE_FARMING   = 1,
    TYPE_FORAGING  = 2,
    TYPE_FORESTING = 3;
  
  final public static float
    FLORA_PROCESS_TIME = Stage.STANDARD_HOUR_LENGTH,
    GROW_STAGE_POLYMER = 5;
  
  final static Traded
    FARM_EXTRACTS[] = { CARBS, GREENS, PROTEIN },
    LOGS_EXTRACTS[] = { POLYMER   },
    SAMP_EXTRACTS[] = { GENE_SEED };
  
  final static Trait
    FARM_TRAITS  [] = { ENERGETIC, NATURALIST, PATIENT     },
    FOREST_TRAITS[] = { ENERGETIC, NATURALIST, PATIENT     },
    FORAGE_TRAITS[] = { ENERGETIC, NATURALIST, ACQUISITIVE },
    SAMPLE_TRAITS[] = { NATURALIST, ACQUISITIVE, CURIOUS   };
  
  final int type;
  Flora toPlant = null;
  
  
  private Gathering(
    Actor actor, Venue depot, Target assessed[], int type,
    Traded... extracts
  ) {
    super(actor, depot, assessed == null, assessed, extracts);
    this.type = type;
  }
  
  
  public Gathering(Session s) throws Exception {
    super(s);
    this.type    = s.loadInt();
    this.toPlant = (Flora) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt   (type   );
    s.saveObject(toPlant);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Gathering(other, (Nursery) depot, assessed(), type);
  }
  
  
  
  /**  Assorted external factory methods for convenience-
    */
  public static Gathering asFarming(Actor actor, Nursery depot) {
    return new Gathering(actor, depot, null, TYPE_FARMING, FARM_EXTRACTS);
  }
  
  
  public static Gathering asBrowsing(Actor fauna, float range) {
    Venue store = null;
    if (fauna.mind.home() instanceof Venue) {
      store = (Venue) fauna.mind.home();
    }
    final Tile points[] = sampleFloraPoints(fauna, range);
    final Gathering browse = new Gathering(fauna, store, points, TYPE_BROWSING);
    browse.useTools = false;
    return browse;
  }
  
  
  public static Gathering asForaging(Actor actor, Venue store) {
    if (store == null && actor.mind.home() instanceof Venue) {
      store = (Venue) actor.mind.home();
    }
    final Tile points[] = sampleFloraPoints(actor, -1);
    final Gathering forage = new Gathering(
      actor, store, points, TYPE_FORAGING, FARM_EXTRACTS
    );
    forage.useTools = false;
    return forage;
  }
  
  
  public static Gathering asForestCutting(Actor actor, Venue depot) {
    final Tile points[] = sampleFloraPoints(actor, -1);
    return new Gathering(actor, depot, points, TYPE_FORESTING, LOGS_EXTRACTS);
  }
  
  
  public static Gathering asForestPlanting(Actor actor, Venue depot) {
    final Batch <Tile> points = new Batch <Tile> ();
    final Stage world = depot.world();
    for (int n = 5; n-- > 0;) {
      final Tile point = Spacing.pickRandomTile(depot, Stage.ZONE_SIZE, world);
      if (Flora.hasSpace(point)) points.include(point);
    }
    
    final Tile PA[] = points.toArray(Tile.class);
    return new Gathering(actor, depot, PA, TYPE_FORESTING);
  }
  
  
  private static Tile[] sampleFloraPoints(Target from, float range) {
    final Stage world = from.world();
    final Series <Target> sampled = world.presences.sampleFromMap(
      from, world, 5, null, Flora.class
    );
    final Tile points[] = new Tile[sampled.size()];
    int index = 0;
    for (Target o : sampled) {
      if (range > 0 && Spacing.distance(from, o) > range) continue;
      points[index++] = ((Flora) o).origin();
    }
    return points;
  }
  
  
  private Species[] forPlanting() {
    if (type == TYPE_FARMING  ) return Crop.ALL_VARIETIES;
    if (type == TYPE_FORESTING) return Flora.BASE_VARIETY;
    return null;
  }
  
  
  protected Trait[] enjoyTraits() {
    return FARM_TRAITS;
  }
  
  
  protected Conversion tendProcess() {
    if (type == TYPE_BROWSING) {
      return null;
    }
    else if (type == TYPE_FORESTING) {
      return Nursery.LAND_TO_GREENS;
    }
    else if (type == TYPE_FORAGING || type == TYPE_FARMING) {
      return Nursery.LAND_TO_CARBS;
    }
    else return null;
  }
  
  
  
  /**  Priority and step evaluation-
    */
  protected Behaviour getNextStep() {
    final Behaviour step = super.getNextStep();
    //
    //  In the event that we're at the 'tending' stage, determine exactly what
    //  specimen we're looking at.  (And if that's impossible, quit.)
    if (stage() == STAGE_TEND) {
      toPlant = pickPlantedSpecimen((Tile) tended(), forPlanting());
      if (toPlant == null) return null;
    }
    return step;
  }
  
  
  private Flora pickPlantedSpecimen(Tile t, Species planted[]) {
    final Flora found = Flora.foundAt(t);
    if (planted == null) return found;
    final boolean forests = type == TYPE_FORESTING;
    //
    //  If there's a pre-existing specimen, check to see if it matches the
    //  plan-type.
    if (found != null && forests != found.species().domesticated) return found;
    //
    //  Otherwise, create a new specimen to fill the spot.
    final Flora plants;
    if (forests) {
      plants = new Flora(Flora.WILD_FLORA);
    }
    else {
      final Pick <Species> pick = new Pick <Species> ();
      for (Species s : planted) {
        final Item seed = actor.gear.bestSample(GENE_SEED, s, 1);
        float chance = Flora.growthBonus(t, s, seed);
        chance *= (1 + s.growRate) / 2;
        pick.compare(s, chance + Rand.num());
      }
      if (pick.empty()) return null;
      else plants = new Crop((Nursery) depot, pick.result());
    }
    //
    //  Either way, set up position and check if placement is possible.
    plants.setPosition(t.x, t.y, t.world);
    return plants.canPlace() ? plants : null;
  }
  
  
  protected float rateTarget(Target t) {
    final Flora c = Flora.foundAt(t);
    final boolean forests = type == TYPE_FORESTING;
    //
    //  If you plant, then empty or sick tiles are valid, and only ripe targets
    //  should be harvested.
    if (forPlanting() != null) {
      if (c == null || c.species().domesticated == forests) return 1;
      else if (c.blighted() || c.ripe()) return 1 + c.growStage();
      else return -1;
    }
    //
    //  Otherwise, favour whatever is most mature.
    else return c == null ? -1 : c.growStage();
  }
  
  
  
  /**  Action-implementations-
    */
  protected Item[] afterHarvest(Target t) {
    final Flora c = Flora.foundAt(t);
    
    if (type == TYPE_FARMING) {
      if (c == null   ) { seedTile((Tile) t); return null         ; }
      if (c != toPlant) { c.setAsDestroyed(); return c.materials(); }
      if (c.blighted()) { c.disinfest()     ; return null         ; }
      if (c.ripe()    ) { c.setAsDestroyed(); return c.materials(); }
    }
    if (type == TYPE_FORESTING) {
      if (c == null   ) { seedTile((Tile) t); return null;          }
      if (c != toPlant) { c.setAsDestroyed(); return c.materials(); }
      else              { c.setAsDestroyed(); return c.materials(); }
    }
    if (type == TYPE_BROWSING) {
      float bite = 0.1f * actor.health.maxHealth() / 10;
      c.incGrowth(0 - bite, t.world());
      actor.health.takeCalories(bite * Fauna.PLANT_CONVERSION, 1);
      return null;
    }
    if (type == TYPE_FORAGING) {
      c.incGrowth(-0.5f, t.world());
      Resting.dineFrom(actor, actor);
      return c.species().nutrients(0);
    }
    return null;
  }
  
  
  private void seedTile(Tile t) {
    if (toPlant == null) return;
    final Species s = toPlant.species();
    //
    //  TODO:  Just base seed quality off upgrades at the source depot!
    //
    //  Assuming that's possible, we then determine the health of the seedling
    //  based on stock quality and planting skill-
    final Item seed = actor.gear.bestSample(Item.asMatch(GENE_SEED, s), 0.1f);
    float health = 0;
    if (seed != null) health += seed.quality * 1f / Item.MAX_QUALITY;
    health += tendProcess().performTest(actor, 0, 1);
    //
    //  Then put the thing in the dirt-
    if (t.above() != null) t.above().setAsDestroyed();
    toPlant.enterWorldAt(t.x, t.y, t.world, true);
    toPlant.seedWith(s, health);
  }
  
  
  public boolean actionCollectTools(Actor actor, Venue depot) {
    if (! super.actionCollectTools(actor, depot)) return false;
    
    for (Species s : forPlanting()) {
      final Item seed = depot.stocks.bestSample(GENE_SEED, s, 1);
      if (seed == null || actor.gear.amountOf(seed) > 0) continue;
      actor.gear.addItem(seed);
    }
    return true;
  }
  
  
  protected void afterDepotDisposal() {
    actor.gear.removeAllMatches(GENE_SEED);
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (stage() == STAGE_TEND && toPlant != null) {
      Flora c = Flora.foundAt(tended());
      Species s = toPlant.species();
      if      (c == null   )   d.append("Planting "  );
      else if (c != toPlant) { d.append("Clearing "  ); s = c.species(); }
      else if (c.blighted())   d.append("Weeding "   );
      else if (c.ripe    ())   d.append("Harvesting ");
      d.append(s);
    }
    else super.describeBehaviour(d); 
  }
}








