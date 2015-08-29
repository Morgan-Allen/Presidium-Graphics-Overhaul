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
    TYPE_LOGGING   = 3,
    TYPE_FORESTING = 4;
  
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
  Flora toTend = null;
  
  
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
    this.toTend = (Flora) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt   (type   );
    s.saveObject(toTend);
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
    final Tile points[] = sampleFloraPoints(store == null ? actor : store, -1);
    final Gathering forage = new Gathering(
      actor, store, points, TYPE_FORAGING, FARM_EXTRACTS
    );
    forage.useTools = false;
    return forage;
  }
  
  
  public static Gathering asForestCutting(Actor actor, Venue depot) {
    final Tile points[] = sampleFloraPoints(depot, -1);
    return new Gathering(actor, depot, points, TYPE_LOGGING, LOGS_EXTRACTS);
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
    else if (type == TYPE_FORESTING || type == TYPE_LOGGING) {
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
      toTend = pickPlantedSpecimen((Tile) tended(), forPlanting());
      if (toTend == null) return null;
    }
    return step;
  }
  
  
  private Flora pickPlantedSpecimen(Tile t, Species planted[]) {
    final Flora found = Flora.foundAt(t);

    if (type == TYPE_FARMING) {
      if (found != null && found.species().domesticated) return found;

      final Pick <Species> pick = new Pick <Species> ();
      for (Species s : planted) {
        final Item seed = actor.gear.bestSample(GENE_SEED, s, 1);
        float chance = Flora.growthBonus(t, s, seed);
        chance *= (1 + s.growRate) / 2;
        pick.compare(s, chance + Rand.num());
      }
      if (pick.empty()) return null;
      
      final Crop plants = new Crop((Nursery) depot, pick.result());
      plants.setPosition(t.x, t.y, t.world);
      return plants.canPlace() ? plants : null;
    }
    else if (type == TYPE_FORESTING) {
      if (found != null && ! found.species().domesticated) return found;
      
      final Flora plants = new Flora(Flora.WILD_FLORA);
      plants.setPosition(t.x, t.y, t.world);
      return plants.canPlace() ? plants : null;
    }
    else return found;
  }
  
  
  protected float rateTarget(Target t) {
    final Flora c = Flora.foundAt(t);
    
    if (type == TYPE_FARMING) {
      if (c == null || ! c.species().domesticated) return 1;
      if (c.blighted() || c.ripe()) return 1 + c.growStage();
    }
    if (type == TYPE_FORESTING) {
      if (c == null || c.species().domesticated) return 1;
    }
    if (type == TYPE_LOGGING) {
      if (c != null) return 1 + c.growStage();
    }
    if (type == TYPE_BROWSING) {
      if (c != null) return c.growStage();
    }
    if (type == TYPE_FORAGING) {
      if (c != null) return c.growStage();
    }
    return -1;
  }
  
  
  
  /**  Action-implementations-
    */
  protected Item[] afterHarvest(Target t) {
    final Flora c = Flora.foundAt(t);
    
    if (type == TYPE_FARMING) {
      if (c == null   ) { seedTile((Tile) t); return null         ; }
      if (c != toTend) { c.setAsDestroyed(); return c.materials(); }
      if (c.blighted()) { c.disinfest()     ; return null         ; }
      if (c.ripe()    ) { c.setAsDestroyed(); return c.materials(); }
    }
    if (type == TYPE_FORESTING) {
      if (c == null   ) { seedTile((Tile) t); return null;          }
      if (c != toTend) { c.setAsDestroyed(); return c.materials(); }
    }
    if (type == TYPE_LOGGING) {
      if (c != null   ) { c.setAsDestroyed(); return c.materials(); }
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
    if (toTend == null) return;
    final Species s = toTend.species();
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
    toTend.enterWorldAt(t.x, t.y, t.world, true);
    toTend.seedWith(s, health);
  }
  
  
  public boolean actionCollectTools(Actor actor, Venue depot) {
    if (! super.actionCollectTools(actor, depot)) return false;
    
    final Species planted[] = forPlanting();
    if (planted != null) for (Species s : planted) {
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
    final Flora c = Flora.foundAt(tended());
    
    if (stage() != STAGE_TEND || toTend == null) {
      super.describeBehaviour(d);
      return;
    }
    if (type == TYPE_FARMING) {
      Object s = toTend.species();
      if      (c == null   )   d.append("Planting "  );
      else if (c != toTend) { d.append("Clearing "  ); s = c; }
      else if (c.blighted())   d.append("Weeding "   );
      else if (c.ripe    ())   d.append("Harvesting ");
      d.append(s);
    }
    if (type == TYPE_FORESTING) {
      Object s = toTend.species();
      if      (c == null   )   d.append("Planting "  );
      else if (c != toTend) { d.append("Clearing "  ); s = c; }
      d.append(s);
    }
    if (type == TYPE_LOGGING) {
      d.append("Harvesting ");
      d.append(c);
    }
    if (type == TYPE_BROWSING) {
      d.append("Browsing on ");
      d.append(c);
    }
    if (type == TYPE_FORAGING) {
      d.append("Foraging from ");
      d.append(c);
    }
  }
}












