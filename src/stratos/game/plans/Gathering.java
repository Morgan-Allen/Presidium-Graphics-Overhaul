/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.content.civic.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.actors.*;
import stratos.game.wild.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.craft.Economy.*;



//  Intended Summary:
//             Tools.  Skills.  Plant.  Harvest.  Species.  Extract.
//  Browsing-   No.      No.     No.    Partial.    No.      Eats.
//  Foraging-   No.      Yes.    No.    Partial.    No.      Foods + Eats.
//  Farming-    Yes.     Yes.    Yes.    Full.     Crops.    Foods.
//  Forestry-   Yes.     Yes.    Yes.    None.     Trees.    No.
//  Logging-    Yes.     Yes.    No.     Full.     Trees.    Carbons.
//  Samples-    No.      Yes.    No.     None.      Any.     Gene Seed.


public class Gathering extends ResourceTending {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final public static int
    TYPE_BROWSING  = 0,
    TYPE_FARMING   = 1,
    TYPE_FORAGING  = 2,
    TYPE_LOGGING   = 3,
    TYPE_FORESTING = 4,
    TYPE_SAMPLE    = 5;
  final public static float
    FLORA_PROCESS_TIME = Stage.STANDARD_HOUR_LENGTH,
    GROW_STAGE_POLYMER = 2.5f;
  
  final static Traded
    FARM_EXTRACTS[] = { CARBS, GREENS, PROTEIN },
    LOGS_EXTRACTS[] = { POLYMER },
    SAMP_EXTRACTS[] = SeedTailoring.SAMPLE_TYPES;
  
  final static Trait
    FARM_TRAITS  [] = { PERSISTENT, RUGGED },
    FOREST_TRAITS[] = { PERSISTENT, RUGGED },
    FORAGE_TRAITS[] = { PERSISTENT, RUGGED },
    SAMPLE_TRAITS[] = { CURIOUS   , RUGGED };
  
  
  final public int type;
  
  
  private Gathering(
    Actor actor, Venue depot, boolean depotAssess, int type,
    Traded... extracts
  ) {
    super(actor, depot, depotAssess, null, extracts);
    this.type = type;
  }
  
  
  public Gathering(Session s) throws Exception {
    super(s);
    this.type = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt(type);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Gathering(other, depot, assessFromDepot, type);
  }
  
  
  public boolean matchesPlan(Behaviour p) {
    if (! super.matchesPlan(p)) return false;
    final Gathering g = (Gathering) p;
    return g.type == this.type;
  }
  
  
  
  /**  Assorted external factory methods for convenience-
    */
  public static Gathering asFarming(Actor actor, BotanicalStation depot) {
    return new Gathering(actor, depot, true, TYPE_FARMING, FARM_EXTRACTS);
  }
  
  
  public static Gathering asBrowsing(Actor fauna, float range) {
    Venue store = null;
    if (fauna.mind.home() instanceof Venue) {
      store = (Venue) fauna.mind.home();
    }
    final Gathering browse = new Gathering(
      fauna, store, false, TYPE_BROWSING, FARM_EXTRACTS
    );
    browse.clearMotives();
    browse.useTools = false;
    return browse;
  }
  
  
  public static Gathering asForaging(Actor actor, Venue store) {
    if (store == null && actor.mind.home() instanceof Venue) {
      store = (Venue) actor.mind.home();
    }
    final Gathering forage = new Gathering(
      actor, store, false, TYPE_FORAGING, FARM_EXTRACTS
    );
    forage.useTools = false;
    return forage;
  }
  
  
  public static Gathering asForestCutting(Actor actor, Venue depot) {
    return new Gathering(actor, depot, false, TYPE_LOGGING, LOGS_EXTRACTS);
  }
  
  
  public static Gathering asForestPlanting(Actor actor, Venue depot) {
    return new Gathering(actor, depot, false, TYPE_FORESTING);
  }
  
  
  public static Gathering asFloraSample(Actor actor, Venue depot) {
    final Gathering samples = new Gathering(
      actor, depot, true, TYPE_SAMPLE, SAMP_EXTRACTS
    );
    samples.useTools = false;
    return samples;
  }
  
  
  public static Tile[] sampleSeedingPoints(Target from, float range) {
    final Batch <Tile> points = new Batch <Tile> ();
    final Stage world = from.world();
    for (int n = 5; n-- > 0;) {
      final Tile p = Spacing.pickRandomTile(from, range, world);
      if (Flora.hasSpace(p)) points.include(p);
    }
    return points.toArray(Tile.class);
  }
  
  
  public static Tile[] sampleFloraPoints(Target from, float range) {
    final Stage world = from.world();
    final List <Target> sampled = new List <Target> ();
    world.presences.sampleFromMap(from, world, 5, sampled, Flora.class);
    
    final Batch <Tile> points = new Batch <Tile> ();
    for (Target o : sampled) {
      if (range > 0 && Spacing.distance(from, o) > range) continue;
      final Flora f = (Flora) o;
      if (f.species().domesticated) continue;
      points.add(f.origin());
    }
    return points.toArray(Tile.class);
  }
  
  
  private Species[] forPlanting() {
    if (type == TYPE_FARMING  ) return Crop.ALL_VARIETIES;
    if (type == TYPE_FORESTING) return Flora.BASE_VARIETY;
    return null;
  }
  
  
  protected Trait[] enjoyTraits() {
    //  TODO:  Diversify a little here.
    return FARM_TRAITS;
  }
  
  
  protected Conversion tendProcess() {
    if (type == TYPE_FORESTING || type == TYPE_LOGGING) {
      return BotanicalStation.LAND_TO_GREENS;
    }
    else if (type == TYPE_FORAGING || type == TYPE_FARMING) {
      return BotanicalStation.LAND_TO_CARBS;
    }
    else if (type == TYPE_SAMPLE) {
      return BotanicalStation.SAMPLE_EXTRACT;
    }
    else return null;
  }
  
  
  
  /**  Priority and step evaluation-
    */
  protected Target[] targetsToAssess(boolean fromDepot) {
    if (fromDepot) return super.targetsToAssess(true);
    if (assessed != null) return assessed;
    //
    //  In the event that you're planting, make sure you have adequate seed
    //  stocks-
    if (forPlanting() != null) {
      return sampleSeedingPoints(subject, Stage.ZONE_SIZE);
    }
    else {
      return sampleFloraPoints(subject, Stage.ZONE_SIZE);
    }
  }
  
  
  protected boolean willBeBlocked(Target toTend) {
    if (toTend instanceof Tile && depot instanceof BotanicalStation) {
      BotanicalStation station = (BotanicalStation) depot;
      if (station.shouldCover((Tile) toTend)) return true;
    }
    return super.willBeBlocked(toTend);
  }
  
  
  protected float rateTarget(Target t) {
    final Flora c = Flora.foundAt(t);
    
    if (type == TYPE_FARMING) {
      if (c == null || ! c.species().domesticated) return 1;
      if (c.blighted() || c.ripe()) return 1 + c.growStage();
    }
    if (type == TYPE_FORESTING) {
      if (c == null && Flora.hasSpace((Tile) t)) return 1;
      if (c != null && c.species().domesticated) return 1;
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
    if (type == TYPE_SAMPLE) {
      if (c != null) return SeedTailoring.sampleValue(c, actor, depot);
    }
    return -1;
  }
  
  
  
  /**  Action-implementations-
    */
  protected Item[] afterHarvest(Target t) {
    final Flora c = Flora.foundAt(t);
    final boolean isCrop = c != null && c.species().domesticated;
    this.assessed = null;
    final Action a = action();
    
    if (type == TYPE_FARMING) {
      if (c == null   ) { seedTile((Tile) t, a)  ; return null; }
      if (! isCrop    ) { c.setAsDestroyed(false); return null; }
      if (c.blighted()) { c.disinfest()          ; return null; }
      if (c.ripe()) {
        c.setAsDestroyed(false);
        seedTile(c.origin(), a);
        return c.materials();
      }
    }
    if (type == TYPE_FORESTING) {
      if (c == null   ) { seedTile((Tile) t, a)  ; return null; }
      if (isCrop      ) { c.setAsDestroyed(false); return null; }
    }
    if (type == TYPE_LOGGING) {
      if (c != null   ) { c.setAsDestroyed(false); return c.materials(); }
    }
    if (type == TYPE_BROWSING) {
      float bite = 0.1f * actor.health.maxHealth() / 10;
      c.incGrowth(0 - bite, t.world(), false);
      actor.health.takeCalories(bite * Fauna.PLANT_CONVERSION, 1);
      return null;
    }
    if (type == TYPE_FORAGING) {
      //
      //  TODO:  Base this off fruiting-materials for the species in question!
      final Item gathered[] = {
        Item.withAmount(CARBS , 0.1f / 1),
        Item.withAmount(GREENS, 0.1f / 2)
      };
      c.incGrowth(-0.1f, t.world(), false);
      Resting.dineFrom(actor, actor);
      return gathered;
    }
    if (type == TYPE_SAMPLE) {
      actor.gear.addItem(SeedTailoring.sampleFrom(c));
    }
    return null;
  }
  
  
  private boolean seedTile(Tile t, Action action) {
    //
    //  TODO:  Use a better method of tracking progress here.
    //if (Rand.index(10) != 0 && ! GameSettings.buildFree) return;
    Flora plants = null;
    
    if (type == TYPE_FARMING) {
      final HarvestVenue HV = (HarvestVenue) depot;
      final Pick <Crop> pick = new Pick();
      
      for (Species s : Crop.ALL_VARIETIES) {
        Crop c = new Crop(HV, s);
        c.setPosition(t.x, t.y, t.world);
        float chance = c.dailyGrowthEstimate(t);
        pick.compare(c, chance * Rand.num());
      }
      plants = pick.result();
    }
    else if (type == TYPE_FORESTING) {
      plants = new Flora(Flora.BASE_SPECIES);
    }
    if (plants == null) return false;
    
    plants.setPosition(t.x, t.y, t.world);
    if (depot != null) RoadsRepair.updatePavingAround(t, depot.base());
    
    float health = 0;
    health += tendProcess().performTest(actor, 0, 1, action);
    //
    //  Then put the thing in the dirt-
    if (t.above() != null) t.above().setAsDestroyed(false);
    plants.enterWorldAt(t.x, t.y, t.world, true);
    plants.seedWith(plants.species(), health);
    return true;
  }
  
  
  protected void afterDepotDisposal() {
    return;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    final Flora c = Flora.foundAt(tended);
    final boolean isCrop = c != null && c.species().domesticated;
    
    if (stage() != STAGE_TEND) {
      super.describeBehaviour(d);
      return;
    }
    if (type == TYPE_FARMING) {
      if      (c == null   ) d.append("Planting "  );
      else if (! isCrop    ) d.append("Clearing "  );
      else if (c.blighted()) d.append("Tending "   );
      else if (c.ripe    ()) d.append("Harvesting ");
      else                   d.append("Tending "   );
      Object s = tended();
      if (s instanceof Tile) s = ((Tile) s).habitat();
      d.append(s);
    }
    if (type == TYPE_FORESTING) {
      if      (c == null  ) d.append("Planting "  );
      else if (isCrop     ) d.append("Clearing "  );
      Object s = tended();
      if (s instanceof Tile) s = ((Tile) s).habitat();
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
    if (type == TYPE_SAMPLE) {
      d.append("Sampling from ");
      d.append(c);
    }
  }
}












