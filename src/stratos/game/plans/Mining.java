/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.content.civic.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
import stratos.game.actors.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;
import static stratos.game.maps.StageTerrain.*;


//  TODO:  This is proceeding too quickly.  (And there needs to be a better
//  method for determining how much apparent ore goes into tailings.


public class Mining extends Plan {
  
  /**  Fields, constructors and save/load methods-
    */
  final public static int
    STAGE_INIT   = -1,
    STAGE_MINE   =  0,
    STAGE_RETURN =  1,
    STAGE_DUMP   =  2,
    STAGE_DONE   =  3;
  final public static float
    MAX_SAMPLE_STORE      = 50,
    DEFAULT_TILE_DIG_TIME = Stage.STANDARD_HOUR_LENGTH,
    HARVEST_MULT          = 1.0f,
    SLAG_RATIO            = 2.5f;
  
  private static boolean
    evalVerbose  = false,
    picksVerbose = false,
    stepsVerbose = false;
  
  
  final ExcavationSite site;
  final Tile face;
  
  private int stage = STAGE_INIT;
  private Tailing dumpSite = null;
  private Suspensor suspensor = null;
  
  
  public Mining(Actor actor, Tile face, ExcavationSite site) {
    super(actor, site, MOTIVE_JOB, NO_HARM);
    this.site = site;
    this.face = face;
  }
  
  
  public Mining(Session s) throws Exception {
    super(s);
    site      = (ExcavationSite) s.loadObject();
    face      = (Tile) s.loadTarget();
    stage     = s.loadInt();
    dumpSite  = (Tailing) s.loadObject();
    suspensor = (Suspensor) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(site     );
    s.saveTarget(face     );
    s.saveInt   (stage    );
    s.saveObject(dumpSite );
    s.saveObject(suspensor);
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
  final public static Traded MINED_TYPES[] = {
    FOSSILS, POLYMER, METALS, FUEL_RODS
  };
  
  
  public static Tile[] getOpenFaces(final ExcavationSite site) {
    final Batch <Tile> open = new Batch();
    Tile batch[] = new Tile[4];
    final StageTerrain terrain = site.world().terrain();
    
    for (Tile t : site.reserved()) {
      if (terrain.mineralsAt(t) > 0) continue;
      for (Tile n : t.edgeAdjacent(batch)) {
        if (n == null || n.flaggedWith() != null) continue;
        if (rateFace(site, n) <= 0) continue;
        n.flagWith(open);
        open.add(n);
      }
    }
    
    for (Tile n : open) n.flagWith(null);
    return open.toArray(Tile.class);
  }
  
  
  public static Tile nextMineFace(final ExcavationSite site, Tile open[]) {
    if (Visit.empty(open)) open = site.reserved();
    final boolean report = evalVerbose && I.talkAbout == site;
    if (report) I.say("\nGETTING NEXT MINE FACE?");
    
    final Pick <Tile> pick = new Pick <Tile> (0) {
      public void compare(Tile next, float rating) {
        if (report) I.say("  Rating for: "+next+" is "+rating);
        super.compare(next, rating);
      }
    };
    for (Tile t : open) {
      pick.compare(t, rateFace(site, t));
    }
    return pick.result();
  }
  
  
  private static float rateFace(ExcavationSite site, Tile face) {
    if (! site.canDig(face)) return -1;
    final Item left = Outcrop.mineralsAt(face);
    float rating = left == null ? -1 : site.extractMultiple(left.type);
    rating /= 1 + Spacing.zoneDistance(face, site);
    return rating;
  }
  
  
  static Tailing nextDumpPoint(
    final ExcavationSite site, Actor actor, Traded waste
  ) {
    final Pick <Tile> pick = new Pick(0);
    
    for (Tile t : site.reserved()) if (site.canDump(t)) {
      float rating = 1;
      if (t.above() instanceof Tailing) {
        final Tailing d = (Tailing) t.above();
        if (waste != null && d.wasteType() != waste) continue;
        if (d.fillLevel() >= 1) continue;
        rating *= 2;
      }
      else if (t.reserves() != site) continue;
      rating /= 1 + Spacing.zoneDistance(actor, t);
      pick.compare(t, rating);
    }
    
    final Tile point = pick.result();
    if (point == null) return null;
    if (point.above() instanceof Tailing) return (Tailing) point.above();
    
    final Tailing d = new Tailing(waste);
    d.setPosition(point.x, point.y, point.world);
    return d;
  }
  
  
  private static Traded oreTypeCarried(Actor actor) {
    for (Traded type : Mining.MINED_TYPES) if (type != SLAG) {
      if (actor.gear.amountOf(type) > 0) return type;
    }
    return (actor.gear.amountOf(SLAG) > 0) ? SLAG : null;
  }
  
  
  public static float totalOresCarried(Actor actor) {
    float total = 0;
    for (Traded type : MINED_TYPES) total += actor.gear.amountOf(type);
    return total;
  }
  
  
  
  /**  Priority evaluation-
    */
  final static Trait BASE_TRAITS[] = { ENERGETIC, URBANE };
  
  protected float getPriority() {
    final boolean report = I.talkAbout == actor && evalVerbose;
    
    float urgency = 0, sumW = 0;
    for (Traded t : MINED_TYPES) {
      final float weight = 1 + site.structure.upgradeLevel(t);
      urgency += site.stocks.relativeShortage(t) * weight;
      sumW += weight;
    }
    urgency /= sumW;
    urgency = (urgency + 1) / 2;
    
    setCompetence(successChanceFor(actor));
    final float priority = PlanUtils.jobPlanPriority(
      actor, this, urgency, competence(), -1, REAL_FAIL_RISK, BASE_TRAITS
    );
    if (report) {
      I.say("\nGetting mining priority for "+actor+"...?");
      I.say("  Basic urgency:  "+urgency);
      I.say("  Final priority: "+priority);
    }
    return priority;
  }
  
  
  public float successChanceFor(Actor actor) {
    float chance = 1;
    chance += actor.skills.chance(GEOPHYSICS , SIMPLE_DC  );
    chance += actor.skills.chance(HARD_LABOUR, MODERATE_DC);
    return chance / 3;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    //
    //  If you've successfully extracted enough ore, or the target is exhausted,
    //  then deliver to the smelters near the site.
    final boolean report = I.talkAbout == actor && stepsVerbose;
    if (report) {
      I.say("\nGetting new mine action...");
    }
    if (stage == STAGE_DONE) {
      if (report) I.say("  Mining complete!");
      return null;
    }
    
    //  If there's no dump site available, then physics precludes any real
    //  progress here.  Otherwise, you can take the tailings out once a given
    //  face is exhausted.  (NOTE:  We call nextTailing twice to ensure
    //  proximity relative to the mine entrance).
    if (stage == STAGE_INIT) {
      dumpSite = nextDumpPoint(site, actor, null);
      if (dumpSite == null) {
        if (report) I.say("  No dump site!");
        return null;
      }
      stage = STAGE_MINE;
    }
    if (stage == STAGE_DUMP) {
      dumpSite = nextDumpPoint(site, actor, oreTypeCarried(actor));
      if (dumpSite == null) {
        if (report) I.say("  No dump site!");
        return null;
      }
      if (report) I.say("  Dumping tailings.");
      final Action dump = new Action(
        actor, dumpSite,
        this, "actionDumpTailings",
        Action.REACH_DOWN, "dumping tailings"
      );
      dump.setMoveTarget(Spacing.pickFreeTileAround(dumpSite, actor));
      return dump;
    }
    
    if (stage == STAGE_RETURN) {
      if (report) I.say("  Returning with ores!");
      return new Action(
        actor, site,
        this, "actionDeliverOres",
        Action.REACH_DOWN, "returning ores"
      );
    }
    
    if (stage == STAGE_MINE && suspensor == null) {
      if (report) I.say("  Picking up tools!");
      final Action pickup = new Action(
        actor, site, this, "actionPickupTools",
        Action.REACH_DOWN, "Picking up tools"
      );
      return pickup;
    }
    
    if (stage == STAGE_MINE) {
      if (report) I.say("  Mining at face: "+face);
      final Action mines = new Action(
        actor, face,
        this, "actionMineFace",
        Action.STRIKE_BIG, "Mining"
      );
      if (Rand.num() < 0.1f || ! Spacing.adjacent(actor, face)) {
        mines.setMoveTarget(Spacing.pickFreeTileAround(face, actor));
      }
      else mines.setMoveTarget(actor.origin());
      return mines;
    }
    
    if (report) I.say("  No next step...");
    return null;
  }
  
  
  public boolean actionPickupTools(Actor actor, Venue site) {
    this.suspensor = new Suspensor(actor, this);
    suspensor.enterWorldAt(site, actor.world());
    return true;
  }
  
  
  private float depleteTile(Tile face, Item contains) {
    final StageTerrain terrain = face.world().terrain();
    final Habitat h = face.habitat();
    
    if (face.above() != null) {
      face.above().setAsDestroyed();
      if (! Visit.arrayIncludes(MINED_TYPES, contains.type)) return 0;
      else return contains.amount;
    }
    else if (h.biomass() > 0) {
      terrain.setHabitat(face, Habitat.STRIP_MINING);
      return h.biomass();
    }
    else {
      final int leftAmount = (int) (contains.amount - 1);
      terrain.setMinerals(face, terrain.mineralType(face), leftAmount);
      terrain.setHabitat(face, Habitat.STRIP_MINING);
      return 1;
    }
  }
  
  
  public boolean actionMineFace(Actor actor, Tile face) {
    final boolean report = I.talkAbout == actor;// && stepsVerbose;
    
    Item left = Outcrop.mineralsAt(face);
    final boolean done = left == null;
    if (left == null) left = Item.withAmount(SLAG, 0);
    
    if (report) {
      I.say("\nMining at face: "+face);
      I.say("  Minerals left: "+left);
    }
    
    final float rate = site.extractMultiple(left.type) / DEFAULT_TILE_DIG_TIME;
    float success = 1;
    success += actor.skills.test(GEOPHYSICS , 5 , 1) ? 1 : 0;
    success *= actor.skills.test(HARD_LABOUR, 15, 1) ? 2 : 1;
    success *= rate;
    Item mined = null, slag = null;
    
    if (done || Rand.num() < success) {
      if (report) {
        I.say("  ...CRACKED IT!");
      }
      final float amount = depleteTile(face, left);
      mined = Item.withAmount(left.type, amount * HARVEST_MULT);
      slag  = Item.withAmount(SLAG     , amount * SLAG_RATIO  );
      actor.gear.addItem(mined);
      actor.gear.addItem(slag );
    }
    
    if (report) {
      I.say("  Digging done?    "+done   );
      I.say("  Dig success was: "+success);
      I.say("  Extraction rate: "+rate   );
      I.say("  Ore extracted:   "+mined  );
      I.say("  Slag left over:  "+slag   );
    }
    
    final boolean
      offShift = done || site.staff.offDuty(actor);
    final float
      oresLoad = totalOresCarried(actor),
      slagLoad = actor.gear.amountOf(SLAG);
    
    if (slagLoad >= 5 || (slagLoad > 0 && offShift)) {
      stage = STAGE_DUMP;
    }
    else if (oresLoad >= 5 || offShift) {
      stage = STAGE_RETURN;
    }
    return true;
  }
  
  
  public boolean actionDumpTailings(Actor actor, Tailing dumps) {
    this.dumpSite = null;
    
    if (! dumps.inWorld()) {
      if (! dumps.canPlace()) return false;
      else dumps.enterWorld();
    }
    
    final float fill = actor.gear.amountOf(SLAG);
    dumps.takeFill(fill);
    actor.gear.removeAllMatches(SLAG);
    stage = STAGE_RETURN;
    return true;
  }
  
  
  public boolean actionDeliverOres(Actor actor, Venue venue) {
    for (Traded type : MINED_TYPES) {
      actor.gear.transfer(type, venue);
    }
    if (Outcrop.mineralsAt(face) == null || site.staff.offDuty(actor)) {
      stage = STAGE_DONE;
      if (suspensor != null) suspensor.exitWorld();
      this.suspensor = null;
    }
    else stage = STAGE_MINE;
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



