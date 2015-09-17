/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
import stratos.game.actors.*;
import stratos.game.economic.*;
import stratos.content.civic.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



public class Mining extends ResourceTending {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  private static boolean
    verbose = false;
  
  final public static int
    TYPE_MINING  = 0,
    TYPE_DUMPING = 1,
    TYPE_FORMING = 2,
    TYPE_BORING  = 3;
  
  final public static float
    MAX_SAMPLE_STORE      = 50,
    MAXIMUM_DIG_DEPTH     = 4 ,
    EXAMPLE_DAILY_OUTPUT  = 10,
    EXAMPLE_NUM_WORKERS   = 4 ,
    DEFAULT_MINE_LIFESPAN = Stage.DAYS_PER_YEAR,
    EXAMPLE_MINED_AREA    = (12 * 12) / 2,
    EXAMPLE_TAILING_SPACE = EXAMPLE_MINED_AREA / 3,
    AVG_RAW_DIG_YIELD     = StageTerrain.MAX_MINERAL_AMOUNT / 5,
    SLAG_RATIO            = 2.5f,
    
    EXAMPLE_DAY_WORKTIME  = EXAMPLE_NUM_WORKERS * Stage.STANDARD_SHIFT_LENGTH,
    DEFAULT_LIFE_WORKTIME = EXAMPLE_DAY_WORKTIME * DEFAULT_MINE_LIFESPAN,
    DEFAULT_DIG_VOLUME    = EXAMPLE_MINED_AREA * MAXIMUM_DIG_DEPTH,
    TILE_DIG_TIME         = DEFAULT_LIFE_WORKTIME / DEFAULT_DIG_VOLUME,
    DAILY_DIG_FREQUENCY   = EXAMPLE_DAY_WORKTIME / TILE_DIG_TIME,
    AVG_RAW_DAILY_YIELD   = DAILY_DIG_FREQUENCY * AVG_RAW_DIG_YIELD,
    HARVEST_MULT          = EXAMPLE_DAILY_OUTPUT / AVG_RAW_DAILY_YIELD,
    
    TOTAL_RAW_LIFE_YIELD  = AVG_RAW_DIG_YIELD * DEFAULT_DIG_VOLUME,
    TOTAL_LIFE_SLAG       = SLAG_RATIO * HARVEST_MULT * TOTAL_RAW_LIFE_YIELD,
    TAILING_LIMIT         = TOTAL_LIFE_SLAG / EXAMPLE_TAILING_SPACE;
  
  final static Trait
    MINE_TRAITS[] = { PATIENT, METICULOUS };

  final public static Traded
    MINED_TYPES[] = { FOSSILS, POLYMER, METALS, FUEL_RODS, SLAG };
  
  
  final public int type;
  private Traded oreDumped = null;
  
  
  private Mining(
    Actor actor, HarvestVenue depot, int type, Traded extracts[]
  ) {
    super(actor, depot, extracts);
    this.type = type;
  }
  
  
  public Mining(Session s) throws Exception {
    super(s);
    this.type      = s.loadInt();
    this.oreDumped = (Traded) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt   (type     );
    s.saveObject(oreDumped);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Mining(actor, (HarvestVenue) depot, type, harvestTypes);
  }
  
  
  public static Mining asMining(Actor actor, ExcavationSite site) {
    final Mining mining = new Mining(
      actor, site, TYPE_MINING, MINED_TYPES
    );
    mining.coop = true;
    return mining;
  }
  
  
  public static Mining asDumping(Actor actor, ExcavationSite site) {
    final Mining mining = new Mining(
      actor, site, TYPE_DUMPING, new Traded[0]
    );
    return mining;
  }
  
  
  public boolean matchesPlan(Behaviour p) {
    if (! super.matchesPlan(p)) return false;
    return ((Mining) p).type == type;
  }
  
  
  
  protected Trait[] enjoyTraits() {
    return MINE_TRAITS;
  }
  
  
  protected Conversion tendProcess() {
    return ExcavationSite.LAND_TO_METALS;
  }
  

  protected Target nextToTend() {
    if (type == TYPE_DUMPING) {
      if (oreDumped == null) oreDumped = nextSlagFor(depot);
      if (oreDumped == null) return null;
      final Item slag = actor.gear.bestSample(SLAG, oreDumped, -1);
      if (slag == null && stage() > STAGE_PICKUP) return null;
    }
    return super.nextToTend();
  }
  
  
  private Traded nextSlagFor(Owner carries) {
    final Pick <Item> pick = new Pick();
    for (Item i : carries.inventory().matches(SLAG)) {
      pick.compare(i, i.amount);
    }
    final Item slag = pick.result();
    return slag == null ? null : (Traded) slag.refers;
  }
  
  
  protected float rateTarget(Target t) {
    
    final ExcavationSite site = (ExcavationSite) depot;
    final Tile at = (Tile) t;
    final StageTerrain terrain = at.world.terrain();
    
    if (type == TYPE_MINING) {
      if (! site.canDig(at)) return -1;
      return MAXIMUM_DIG_DEPTH + terrain.flatHeight(at);
    }
    if (type == TYPE_DUMPING) {
      if (! site.canDump(at)) return -1;
      final Tailing dump = Tailing.foundAt(at);
      if (dump == null) return 1;
      if (dump.fillLevel() >= 1 || dump.wasteType() != oreDumped) return 0;
      return dump.fillLevel() + 1;
    }
    if (type == TYPE_FORMING) {
      
    }
    if (type == TYPE_BORING) {
      
    }
    return 0;
  }
  
  
  public boolean actionCollectTools(Actor actor, Venue depot) {
    if (! super.actionCollectTools(actor, depot)) return false;
    
    if (type == TYPE_MINING) {
      
    }
    if (type == TYPE_DUMPING) {
      final Item slag = depot.stocks.bestSample(SLAG, oreDumped, 10);
      if (slag != null) depot.stocks.transfer(slag, actor);
    }
    if (type == TYPE_BORING) {
      
    }
    if (type == TYPE_FORMING) {
      
    }
    return true;
  }
  
  
  protected Item[] afterHarvest(Target t) {
    
    final boolean report = verbose && I.talkAbout == actor;
    final ExcavationSite site = (ExcavationSite) depot;
    final Tile at = (Tile) t;
    final StageTerrain terrain = at.world.terrain();
    
    if (type == TYPE_MINING) {
      final float  breakChance = 1f / TILE_DIG_TIME;
      final byte   typeID      = terrain.mineralType(at);
      final int    height      = terrain.flatHeight (at);
      final Traded oreType     = MINED_TYPES[typeID];
      final int    oreAmount   = (int) terrain.mineralsAt(at);

      terrain.setHabitat(at, Habitat.STRIP_MINING);
      at.clearUnlessOwned();
      
      if (report) I.say("\nMining tile: "+at+", break chance: "+breakChance);
      
      float yield = breakChance / 2f;
      if (Rand.num() < breakChance) {
        final int remains = Nums.max(1, oreAmount - 1);
        terrain.setMinerals(at, typeID, remains);
        yield += 0.5f;
        terrain.hardTerrainLevel(height - 1, at);
      }
      yield *= oreAmount * HARVEST_MULT * site.extractMultiple(oreType);
      
      if (report) I.say("  final yield: "+yield+", ore abundance: "+oreAmount);
      
      return new Item[] {
        Item.with(oreType, null   , yield               , Item.AVG_QUALITY),
        Item.with(SLAG   , oreType, yield * SLAG_RATIO  , Item.AVG_QUALITY)
      };
    }
    if (type == TYPE_DUMPING) {
      
      Tailing dumps = Tailing.foundAt(at);
      float space = 10;
      if (dumps == null) {
        dumps = new Tailing(oreDumped);
        dumps.enterWorldAt(at.x, at.y, at.world, true);
      }
      else space = (1 - dumps.fillLevel()) * TAILING_LIMIT;
      
      final Item slag = actor.gear.bestSample(SLAG, oreDumped, space);
      if (slag != null) {
        actor.gear.removeItem(slag);
        dumps.takeFill(slag.amount);
      }
      
      return null;
    }
    if (type == TYPE_BORING) {
      
    }
    if (type == TYPE_FORMING) {
      
    }
    return null;
  }
  
  
  protected void afterDepotDisposal() {
    if (type == TYPE_MINING) {
      actor.gear.transfer(SLAG, depot);
    }
    if (type == TYPE_FORMING) {
      
    }
    if (type == TYPE_DUMPING) {
      
    }
    if (type == TYPE_BORING) {
      
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    final Tile at = (Tile) tended;
    final Tailing t = Tailing.foundAt(at);
    
    if (stage() != STAGE_TEND) {
      super.describeBehaviour(d);
      return;
    }
    
    if (type == TYPE_MINING) {
      d.append("Mining ");
      d.append(at.habitat());
    }
    if (type == TYPE_DUMPING) {
      d.append("Dumping slag");
    }
  }
}







