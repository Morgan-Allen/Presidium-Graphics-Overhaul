/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.plans;
import stratos.game.civic.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.actors.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;
import static stratos.game.maps.StageTerrain.*;


//  TODO:  This is proceeding too quickly.  (And there needs to be a better
//  method for determining how much apparent ore goes into tailings.)





public class Mining extends Plan {
  
  /**  Fields, constructors and save/load methods-
    */
  final public static int
    STAGE_INIT   = -1,
    STAGE_MINE   =  0,
    STAGE_RETURN =  1,
    STAGE_DUMP   =  2,
    STAGE_DONE   =  3;
  final public static int
    MAX_SAMPLE_STORE = 50,
    DEFAULT_TILE_DIG_TIME = Stage.STANDARD_SHIFT_LENGTH,
    HARVEST_MULT = 5 ,
    SLAG_RATIO   = 10;
  
  final public static Traded MINED_TYPES[] = {
    SLAG, METALS, FUEL_RODS, CURIO
  };
  final static Table TYPE_MAP = Table.make(
    TYPE_RUBBLE  , SLAG     ,
    TYPE_METALS  , METALS   ,
    TYPE_ISOTOPES, FUEL_RODS,
    TYPE_RUINS   , CURIO
  );
  
  
  private static boolean
    evalVerbose  = false,
    picksVerbose = false,
    eventVerbose = true ;
  
  
  final ExcavationSite site;
  final Tile face;
  private int stage = STAGE_INIT;
  private Tailing dumpSite = null;
  
  
  public Mining(Actor actor, Tile face, ExcavationSite site) {
    super(actor, site, MOTIVE_JOB, NO_HARM);
    this.site = site;
    this.face = face;
  }
  
  
  public Mining(Session s) throws Exception {
    super(s);
    site     = (ExcavationSite) s.loadObject();
    face     = (Tile) s.loadTarget();
    stage    = s.loadInt();
    dumpSite = (Tailing) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(site);
    s.saveTarget(face);
    s.saveInt   (stage);
    s.saveObject(dumpSite);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Mining(other, face, site);
  }
  
  
  public boolean matchesPlan(Behaviour p) {
    if (! super.matchesPlan(p)) return false;
    final Mining m = (Mining) p;
    return m.face == this.face && m.site == this.site;
  }
  
  
  
  /**  Static location methods and priority evaluation-
    */
  //  TODO:  Move this to the MineFace class.
  
  public static Tile[] getTilesUnder(final ExcavationSite site) {
    final boolean report = picksVerbose && I.talkAbout == site;
    if (report) I.say("\nGetting tiles beneath "+site);
    
    final Stage world = site.world();
    final int range = site.digLimit(), SS = Stage.ZONE_SIZE;
    final Box2D area = new Box2D().setTo(site.footprint());

    //
    //  Firstly, we spread out from beneath the excavation site and claim all
    //  tiles which are either mined out, or directly under the structure, up
    //  to a certain distance limit.
    final TileSpread spread = new TileSpread(world.tileAt(site)) {
      
      protected boolean canAccess(Tile t) {
        if (area.distance(t.x, t.y) > range   ) return false;
        if (area.contains(t.x, t.y)           ) return true ;
        if (world.terrain().mineralsAt(t) == 0) return true ;
        return false;
      }
      
      protected boolean canPlaceAt(Tile t) { return false; }
    };
    spread.doSearch();
    
    //
    //  We then get all adjacent tiles which are *not* mined out, sort them
    //  based on distance and promise, and return-
    final Tile open[] = spread.allSearched(Tile.class);
    final Batch <Tile> touched = new Batch <Tile> ();

    final Sorting <Tile> sorting = new Sorting <Tile> () {
      public int compare(Tile a, Tile b) {
        final float
          rA = (Float) a.flaggedWith(),
          rB = (Float) b.flaggedWith();
        if (rA > rB) return  1;
        if (rB > rA) return -1;
        return 0;
      }
    };
    for (Tile o : open) for (Tile n : o.edgeAdjacent(Spacing.tempT4)) {
      if (n == null || n.flaggedWith() != null) continue;
      
      final float rating = rateFace(site, n);
      n.flagWith(rating);
      touched.add(n);
      if (rating > 0) sorting.add(n);
    }
    
    for (Tile t : touched) t.flagWith(null);
    return sorting.toArray(Tile.class);
  }
  
  
  private static float rateFace(ExcavationSite site, Tile face) {
    final int SS = Stage.ZONE_SIZE;
    
    final float dist = Spacing.distance(face, site);
    if (dist > site.digLimit() + (SS / 2)) return -1;
    
    final Item left = mineralsAt(face);
    float rating = left == null ? 0.1f : site.extractMultiple(left.type);
    rating *= SS / (SS + dist);
    return rating;
  }
  
  
  public static Tile nextMineFace(ExcavationSite site, Tile under[]) {
    if (under == null || under.length == 0) return null;
    
    final Pick <Tile> pick = new Pick <Tile> (null, 0);
    for (int n = 5; n-- > 0;) {
      final Tile face = (Tile) Rand.pickFrom(under);
      pick.compare(face, rateFace(site, face));
    }
    return pick.result();
  }
  

  public static Item mineralsAt(Tile face) {
    final StageTerrain terrain = face.world().terrain();
    if (terrain.mineralsAt(face) == 0) return null;
    
    final byte type = terrain.mineralType(face);
    final float amount = terrain.mineralsAt(face, type);
    if (type == StageTerrain.TYPE_RUBBLE) return null;
    
    final Traded minType = (Traded) TYPE_MAP.get(type);
    if (minType == null || amount <= 0) return null;
    
    return Item.withAmount(minType, amount);
  }
  
  
  
  /**  Priority evaluation-
    */
  final static Trait BASE_TRAITS[] = { ENERGETIC, URBANE };
  
  protected float getPriority() {
    
    float urgency = 0, sumW = 0;
    for (Traded t : MINED_TYPES) {
      final float weight = 1 + site.structure.upgradeBonus(t);
      urgency += site.stocks.relativeShortage(t) * weight;
      sumW += weight;
    }
    urgency /= sumW;

    setCompetence(successChanceFor(actor));
    return PlanUtils.jobPlanPriority(
      actor, this, urgency, competence(), -1, REAL_FAIL_RISK, BASE_TRAITS
    );
  }
  
  
  public float successChanceFor(Actor actor) {
    float chance = 1;
    chance += actor.skills.chance(GEOPHYSICS , SIMPLE_DC  );
    chance += actor.skills.chance(HARD_LABOUR, MODERATE_DC);
    return chance / 3;
  }
  
  
  public static float oresCarried(Actor actor) {
    float total = 0;
    for (Traded type : MINED_TYPES) total += actor.gear.amountOf(type);
    return total;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    //
    //  If you've successfully extracted enough ore, or the target is exhausted,
    //  then deliver to the smelters near the site.
    final boolean report = evalVerbose && I.talkAbout == actor && hasBegun();
    
    if (report) I.say("  Getting new mine action.");
    
    if (stage == STAGE_DONE) return null;
    
    //  If there's no dump site available, then physics precludes any real
    //  progress here.  Otherwise, you can take the tailings out once a given
    //  face is exhausted.  (NOTE:  We call nextTailing twice to ensure
    //  proximity relative to the mine entrance).
    if (stage == STAGE_INIT) {
      dumpSite = Tailing.nextTailingFor(site, actor);
      if (dumpSite == null) return null;
      stage = STAGE_MINE;
    }
    if (stage == STAGE_DUMP) {
      dumpSite = Tailing.nextTailingFor(site, actor);
      if (dumpSite == null) return null;
      final Action dump = new Action(
        actor, dumpSite,
        this, "actionDumpTailings",
        Action.REACH_DOWN, "dumping tailings"
      );
      dump.setMoveTarget(Spacing.nearestOpenTile(dumpSite, actor));
      return dump;
    }
    
    if (stage == STAGE_RETURN) {
      return new Action(
        actor, site,
        this, "actionDeliverOres",
        Action.REACH_DOWN, "returning ores"
      );
    }
    
    if (stage == STAGE_MINE) {
      if (actor.aboard() == site) {
        final Action mines = new Action(
          actor, face,
          this, "actionMineFace",
          Action.STRIKE_BIG, "Mining"
        );
        mines.setMoveTarget(site);
        return mines;
      }
      else {
        final Action entry = new Action(
          actor, site,
          this, "actionEnterShaft",
          Action.STAND, "Entering shaft"
        );
        return entry;
      }
    }
    
    return null;
  }
  
  
  public boolean actionEnterShaft(Actor actor, ExcavationSite site) {
    //
    //  NOTE:  The purpose of this detour is to ensure that the pathing-cache
    //  (which is optimised for above-ground structures) doesn't have to strain
    //  itself dealing with underground topography.
    actor.goAboard(site, site.world());
    //
    //  TODO:  Consider introducing security/safety measures here?
    return true;
  }
  
  
  public boolean actionDeliverOres(Actor actor, Venue venue) {
    for (Traded type : MINED_TYPES) {
      actor.gear.transfer(type, venue);
    }
    if (mineralsAt(face) == null || ! site.staff.onShift(actor)) {
      stage = STAGE_DONE;
    }
    else stage = STAGE_MINE;
    return true;
  }
  
  
  public boolean actionDumpTailings(Actor actor, Tailing dumps) {
    this.dumpSite = null;
    
    if (! dumps.inWorld()) {
      if (! dumps.canPlace()) return false;
      else dumps.enterWorld();
    }
    if (! dumps.takeFill(1)) return false;
    
    actor.gear.removeAllMatches(SLAG);
    stage = STAGE_RETURN;
    return true;
  }
  
  
  public boolean actionMineFace(Actor actor, Tile face) {
    final boolean report = eventVerbose && I.talkAbout == actor;
    
    final Item left = mineralsAt(face);
    if (left == null) { stage = STAGE_DUMP; return false; }
    
    final float rate = site.extractMultiple(left.type);
    float success = 1;
    success += actor.skills.test(GEOPHYSICS , 5 , 1) ? 1 : 0;
    success *= actor.skills.test(HARD_LABOUR, 15, 1) ? 2 : 1;
    success *= rate;
    success /= DEFAULT_TILE_DIG_TIME;
    
    if (report) I.say("\nMINERALS LEFT: "+left);
    final Item
      mined = Item.withAmount(left.type, left.amount * HARVEST_MULT * success),
      slag =  Item.withAmount(SLAG     , left.amount * SLAG_RATIO   * success);
    actor.gear.addItem(mined);
    actor.gear.addItem(slag );
    
    if (report) {
      I.say("  Dig success was: "+success);
      I.say("  Extraction rate: "+rate   );
      I.say("  Ore extracted:   "+mined  );
    }
    
    if (Rand.num() < success) {
      face.world.terrain().setMinerals(face, (byte) 0, 0);
    }
    
    final boolean offShift = ! site.staff.onShift(actor);
    final float
      oresLoad = oresCarried(actor),
      slagLoad = actor.gear.amountOf(SLAG);
    if (slagLoad >= 1 || (slagLoad > 0 && offShift)) {
      stage = STAGE_DUMP;
    }
    else if (oresLoad >= 5 || (oresLoad > 0 && offShift)) {
      stage = STAGE_RETURN;
    }
    return true;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (stage <= STAGE_MINE) {
      d.append("Mining ");
      if (face instanceof Tile) d.append(((Tile) face).habitat().name);
      else d.append(face);
    }
    if (stage == STAGE_DUMP) {
      d.append("Dumping tailings");
    }
    if (stage == STAGE_RETURN) {
      d.append("Returning ores to "+site);
    }
  }
}







