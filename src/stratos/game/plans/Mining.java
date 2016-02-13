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
    TYPE_MINING    = 0,
    TYPE_DUMPING   = 1,
    TYPE_FORMING   = 2,
    TYPE_STRIPPING = 3;
  
  final public static float
    MAX_SAMPLE_STORE      = 50,
    MAXIMUM_DIG_DEPTH     = Outcrop.MAX_DIG_DEEP,
    EXAMPLE_DAILY_OUTPUT  = 10,
    EXAMPLE_NUM_WORKERS   = 4 ,
    DEFAULT_MINE_LIFESPAN = Stage.DAYS_PER_YEAR,
    EXAMPLE_MINED_AREA    = (12 * 12) / 2f,
    EXAMPLE_TAILING_SPACE = EXAMPLE_MINED_AREA / 3f,
    AVG_RAW_DIG_YIELD     = Outcrop.MAX_MINERALS / 2f,
    SLAG_RATIO            = 2.5f,
    
    EXAMPLE_DAY_WORKTIME  = EXAMPLE_NUM_WORKERS   * Stage.STANDARD_SHIFT_LENGTH,
    DEFAULT_LIFE_WORKTIME = EXAMPLE_DAY_WORKTIME  * DEFAULT_MINE_LIFESPAN,
    DEFAULT_DIG_VOLUME    = EXAMPLE_MINED_AREA    * MAXIMUM_DIG_DEPTH,
    TILE_DIG_TIME         = DEFAULT_LIFE_WORKTIME / DEFAULT_DIG_VOLUME,
    DAILY_DIG_FREQUENCY   = EXAMPLE_DAY_WORKTIME  / TILE_DIG_TIME,
    AVG_RAW_DAILY_YIELD   = DAILY_DIG_FREQUENCY   * AVG_RAW_DIG_YIELD,
    HARVEST_MULT          = EXAMPLE_DAILY_OUTPUT  / AVG_RAW_DAILY_YIELD,
    
    TOTAL_RAW_LIFE_YIELD  = AVG_RAW_DIG_YIELD * DEFAULT_DIG_VOLUME,
    TOTAL_LIFE_SLAG       = SLAG_RATIO * HARVEST_MULT * TOTAL_RAW_LIFE_YIELD,
    TAILING_LIMIT         = (int) (TOTAL_LIFE_SLAG / EXAMPLE_TAILING_SPACE);
  
  final static Trait
    MINE_TRAITS[] = { PATIENT, METICULOUS };
  
  
  final public int type;
  private Traded oreType;
  
  
  private Mining(
    Actor actor, HarvestVenue depot, int type, Traded oreType,
    Target toAssess[]
  ) {
    super(
      actor, depot, true, toAssess,
      oreType != null ? new Traded[] { oreType, SLAG } : new Traded[0]
    );
    this.type    = type;
    this.oreType = oreType;
  }
  
  
  public Mining(Session s) throws Exception {
    super(s);
    this.type    = s.loadInt();
    this.oreType = (Traded) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt   (type   );
    s.saveObject(oreType);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Mining(
      actor, (HarvestVenue) depot, type, oreType, assessed
    );
  }
  
  
  public static Mining asMining(Actor actor, ExcavationSite site, Traded ore) {
    final Mining mining = new Mining(
      actor, site, TYPE_MINING, ore, null
    );
    mining.coop = false;
    return mining;
  }
  
  
  public static Mining asStripping(
    Actor actor, ExcavationSite site, Traded ore
  ) {
    final Batch <Outcrop> outcrops = new Batch();
    site.world().presences.sampleFromMap(
      site, site.world(), 5, outcrops, Outcrop.class
    );
    if (outcrops.empty()) return null;
    
    final Mining mining = new Mining(
      actor, site, TYPE_STRIPPING, ore, outcrops.toArray(Outcrop.class)
    );
    mining.coop = true;
    return mining;
  }
  
  
  public static Mining asDumping(Actor actor, ExcavationSite site) {
    final Mining mining = new Mining(
      actor, site, TYPE_DUMPING, null, null
    );
    mining.coop = true;
    return mining;
  }
  
  
  public boolean matchesPlan(Behaviour p) {
    if (! super.matchesPlan(p)) return false;
    return ((Mining) p).type == type;
  }
  
  
  public Traded oreType() {
    return oreType;
  }
  
  
  
  /**  Methods overrides to customise for mining behaviour-
    */
  protected Trait[] enjoyTraits() {
    return MINE_TRAITS;
  }
  
  
  protected Conversion tendProcess() {
    return ExcavationSite.LAND_TO_METALS;
  }
  
  
  protected Target[] targetsToAssess(boolean fromDepot) {
    if (assessed != null) return assessed;
    else return super.targetsToAssess(fromDepot);
  }
  

  protected Target nextToTend() {
    if (type == TYPE_DUMPING) {
      if (oreType == null) oreType = nextSlagFor(depot);
      if (oreType == null) return null;
      final Item slag = actor.gear.bestSample(SLAG, oreType, -1);
      if (slag == null && stage() > STAGE_PICKUP) return null;
    }
    else {
      if (oreType == null) return null;
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
    final StageTerrain terrain = depot.world().terrain();
    
    //  TODO:  Screw it.  I just want to strip the area evenly, working out
    //  from the centre.
    
    if (type == TYPE_MINING) {
      final Tile   face = (Tile) t;
      final Traded type = Outcrop.oreType(face);
      
      if (type != oreType    ) return -1;
      if (! site.canDig(face)) return -1;
      
      final float amount = Outcrop.oreAmount(face);
      final int   height = terrain.digLevel (face);
      if (height < 0 - MAXIMUM_DIG_DEPTH) return -1;
      
      float rating = MAXIMUM_DIG_DEPTH + height;
      rating += amount / Outcrop.MAX_MINERALS;
      if (! terrain.isStripped(face)) rating += MAXIMUM_DIG_DEPTH;
      
      return rating;
    }
    if (type == TYPE_STRIPPING) {
      final Outcrop face = (Outcrop) t;
      if (face.oreType() != oreType) return -1;
      if (Spacing.distance(face, site) > Stage.ZONE_SIZE) return -1;
      return face.oreAmount() / (face.bulk() * Outcrop.MAX_MINERALS);
    }
    if (type == TYPE_DUMPING) {
      final Tile face = (Tile) t;
      if (! site.canDump(face)) return -1;
      final Tailing dump = Tailing.foundAt(face);
      if (dump == null) return 1;
      if (dump.fillLevel() >= 1 || dump.wasteType() != oreType) return 0;
      return dump.fillLevel() + 1;
    }
    if (type == TYPE_FORMING) {
      
    }
    return 0;
  }
  
  
  public boolean actionCollectTools(Actor actor, Venue depot) {
    if (! super.actionCollectTools(actor, depot)) return false;
    
    if (type == TYPE_MINING) {
      
    }
    if (type == TYPE_STRIPPING) {
      
    }
    if (type == TYPE_DUMPING) {
      final Item slag = depot.stocks.bestSample(SLAG, oreType, 10);
      if (slag != null) depot.stocks.transfer(slag, actor);
    }
    if (type == TYPE_FORMING) {
      
    }
    return true;
  }
  
  
  protected Item[] afterHarvest(Target t) {
    final boolean report = verbose && I.talkAbout == actor;
    final ExcavationSite site = (ExcavationSite) depot;
    
    final StageTerrain terrain = depot.world().terrain();
    
    if (type == TYPE_MINING) {
      final Tile  face    = (Tile) t;
      final Item  ore     = Outcrop.mineralsAt(t);
      final int   height  = terrain.digLevel(face);
      if (ore == null) return null;
      
      float breakChance = 1f / TILE_DIG_TIME;
      float yield = breakChance / 2f;
      if (Rand.num() < breakChance) {
        for (Tile n : face.vicinity(null)) if (n != null && site.canDig(n)) {
          terrain.setRoadType(n, StageTerrain.ROAD_STRIP);
          n.clearUnlessOwned();
        }
        yield += 0.5f;
        terrain.setDigLevel(face, height - 1);
      }
      
      yield *= ore.amount * HARVEST_MULT * site.extractMultiple(ore.type);
      return new Item[] {
        Item.with(ore.type, null    , yield             , Item.AVG_QUALITY),
        Item.with(SLAG    , ore.type, yield * SLAG_RATIO, Item.AVG_QUALITY)
      };
    }
    if (type == TYPE_STRIPPING) {
      final Outcrop face = (Outcrop) t;
      float inc = -2f / (TILE_DIG_TIME * face.bulk());
      face.incCondition(inc);
      
      final Traded type = face.oreType();
      float amount = face.oreAmount() * (0 - inc);
      amount *= HARVEST_MULT * site.extractMultiple(type);
      float slagged = amount * SLAG_RATIO / 2f;
      
      return new Item[] {
        Item.with(type, null, amount , Item.AVG_QUALITY),
        Item.with(SLAG, type, slagged, Item.AVG_QUALITY)
      };
      
    }
    if (type == TYPE_DUMPING) {
      final Tile face = (Tile) t;
      Tailing dumps = Tailing.foundAt(face);
      float space = 10;
      
      if (dumps == null) {
        dumps = new Tailing(oreType);
        dumps.enterWorldAt(face.x, face.y, face.world, true);
      }
      else space = (1 - dumps.fillLevel()) * TAILING_LIMIT;
      
      final Item slag = actor.gear.bestSample(SLAG, oreType, space);
      if (slag != null) {
        actor.gear.removeItem(slag);
        dumps.takeFill(slag.amount);
      }
      
      return null;
    }
    if (type == TYPE_FORMING) {
      
    }
    return null;
  }
  
  
  protected void afterDepotDisposal() {
    if (type == TYPE_MINING) {
      actor.gear.transfer(SLAG, depot);
    }
    if (type == TYPE_STRIPPING) {
      actor.gear.transfer(SLAG, depot);
    }
    if (type == TYPE_FORMING) {
      
    }
    if (type == TYPE_DUMPING) {
      
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (stage() != STAGE_TEND) {
      super.describeBehaviour(d);
      return;
    }
    if (type == TYPE_MINING) {
      d.append("Mining ");
      d.append(tended);
    }
    if (type == TYPE_STRIPPING) {
      d.append("Stripping ");
      d.append(tended);
    }
    if (type == TYPE_DUMPING) {
      d.append("Dumping slag");
    }
  }
}




//TODO:  Try to find a place for these activities:

/*
public boolean actionReassemble(Actor actor, Venue site) {
//  TODO:  Test and restore
/*
final Item sample = Item.withReference(SAMPLES, ARTIFACTS);
final Structure s = site.structure();
final float
AAU = s.upgradeLevel(ExcavationSite.ARTIFACT_ASSEMBLY),
SPU = s.upgradeLevel(ExcavationSite.SAFETY_PROTOCOL);

float success = 1;
if (actor.skills.test(ASSEMBLY, 10, 1)) success++;
else success--;
if (actor.skills.test(ANCIENT_LORE, 5, 1)) success++;
else success--;

site.stocks.removeItem(Item.withAmount(sample, 1.0f));
if (success >= 0) {
success *= 1 + (AAU / 2f);
final Item result = Item.with(ARTIFACTS, null, 0.1f, success * 2);
site.stocks.addItem(result);
}
if (site.stocks.amountOf(ARTIFACTS) >= 10) {
site.stocks.removeItem(Item.withAmount(ARTIFACTS, 10));
final Item match = site.stocks.matchFor(Item.withAmount(ARTIFACTS, 1));
final float quality = (match.quality + AAU + 2) / 2f;
if (Rand.num() < 0.1f * match.quality / (1 + SPU)) {
  final boolean hostile = Rand.num() < 0.9f / (1 + SPU);
  releaseArtilect(actor, hostile, quality);
}
else createArtifact(site, quality);
}
//*/
/*
return true;
}


private void releaseArtilect(Actor actor, boolean hostile, float quality) {
//  TODO:  TEST AND RESTORE THIS
/*
final int roll = (int) (Rand.index(5) + quality);
final Artilect released = roll >= 5 ? new Tripod() : new Drone();

final World world = actor.world();
if (hostile) {
released.assignBase(world.baseWithName(Base.KEY_ARTILECTS, true, true));
}
else {
released.assignBase(actor.base());
released.mind.assignMaster(actor);
}
released.enterWorldAt(actor.aboard(), world);
released.goAboard(actor.aboard(), world);
//*/
/*
}


private void createArtifact(Venue site, float quality) {
final TradeType basis = Rand.yes() ?
(TradeType) Rand.pickFrom(ALL_DEVICES) :
(TradeType) Rand.pickFrom(ALL_OUTFITS);
//
//  TODO:  Deliver to artificer for sale or recycling!
final Item found = Item.with(ARTIFACTS, basis, 1, quality);
site.stocks.addItem(found);
}
//*/






