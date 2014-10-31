/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.plans;
import stratos.game.common.*;
import stratos.game.base.*;
import stratos.game.building.*;
import stratos.game.maps.*;
import stratos.game.actors.*;
import stratos.user.*;
import stratos.util.*;

import static stratos.game.actors.Qualities.*;
import static stratos.game.building.Economy.*;



public class StripMining extends Plan {
  
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
    DEFAULT_TILE_DIG_TIME = Stage.STANDARD_HOUR_LENGTH;
  
  final public static Traded MINED_TYPES[] = {
    ORES, FUEL_RODS, ARTIFACTS
  };
  
  private static boolean
    evalVerbose  = false,
    picksVerbose = false,
    eventVerbose = false;
  
  
  final ExcavationSite site;
  final Target face;
  private int stage = STAGE_INIT;
  
  
  public StripMining(Actor actor, Target face, ExcavationSite site) {
    super(actor, site, true);
    this.site = site;
    this.face = face;
  }
  
  
  public StripMining(Session s) throws Exception {
    super(s);
    site = (ExcavationSite) s.loadObject();
    face = s.loadTarget();
    stage = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(site);
    s.saveTarget(face);
    s.saveInt(stage);
  }
  
  
  public Plan copyFor(Actor other) {
    return new StripMining(other, face, site);
  }
  
  
  public boolean matchesPlan(Plan p) {
    if (! super.matchesPlan(p)) return false;
    final StripMining m = (StripMining) p;
    return m.face == this.face && m.site == this.site;
  }
  
  
  
  /**  Static location methods and priority evaluation-
    */
  public static Tile[] getTilesAround(Venue site) {
    //  TODO:  Re-implement this.
    return null;
  }
  
  
  public static Target nextMineFace(Venue site) {
    //  TODO:  Re-implement this.
    return null;
  }
  
  
  
  /**  Priority evaluation-
    */
  final static Skill BASE_SKILLS[] = { GEOPHYSICS, HARD_LABOUR };
  final static Trait BASE_TRAITS[] = { ENERGETIC, URBANE };
  
  
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    
    final float priority = priorityForActorWith(
      actor, site, ROUTINE,
      NO_MODIFIER, NO_HARM,
      MILD_COOPERATION, BASE_SKILLS,
      BASE_TRAITS, NORMAL_DISTANCE_CHECK, MILD_FAIL_RISK,
      report
    );
    return priority;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    //
    //  If you've successfully extracted enough ore, or the target is exhausted,
    //  then deliver to the smelters near the site.
    final boolean report = evalVerbose && I.talkAbout == actor && hasBegun();
    
    if (report) I.say("  Getting new mine action.");
    boolean
      shouldQuit   = stage == STAGE_DONE,
      shouldReturn = stage == STAGE_RETURN;
    final float carried = Mining.oresCarried(actor);
    final boolean onShift = site.personnel.onShift(actor);
    
    /*
    if (mineralsLeft(face) == null || ! onShift) {
      if (report) {
        I.say("  QUITTING MINING");
        I.say("  Minerals left: "+mineralsLeft(face));
        I.say("  On shift: "+site.personnel.onShift(actor));
      }
      shouldQuit = true;
      if (carried > 0 && ! onShift) shouldReturn = true;
    }
    //*/
    if (carried >= 5) shouldReturn = true;
    
    if (stage == STAGE_DUMP) {
      final Tailing dumps = Tailing.nextTailingFor(site, actor);
      
      //  TODO:  Elaborate here.  If there's no space to dump, you have a
      //  physics problem.
      if (dumps == null) return null;
      final Action dump = new Action(
        actor, dumps,
        this, "actionDumpTailings",
        Action.REACH_DOWN, "dumping tailings"
      );
      dump.setMoveTarget(Spacing.nearestOpenTile(dumps, actor));
      return dump;
    }
    
    if (shouldReturn) {
      stage = STAGE_RETURN;
      return new Action(
        actor, site,
        this, "actionDeliverOres",
        Action.REACH_DOWN, "returning ores"
      );
    }
    
    if (shouldQuit) return null;
    
    final Action mines = new Action(
      actor, face,
      this, "actionMineSurface",
      Rand.yes() ? Action.STRIKE_BIG : Action.BUILD, "Mining"
    );
    
    boolean shouldMove = ! Spacing.adjacent(actor, face);
    if (Rand.index(10) == 0) shouldMove = true;
    if (shouldMove) {
      mines.setMoveTarget(Spacing.pickFreeTileAround(face, actor));
    }
    else mines.setMoveTarget(actor.origin());
    
    return mines;
  }
  
  
  public boolean actionDeliverOres(Actor actor, Venue venue) {
    for (Traded type : MINED_TYPES) {
      actor.gear.transfer(type, venue);
    }
    if (Mining.oresCarried(actor) == 0) stage = STAGE_DONE;
    return true;
  }
  
  
  public boolean actionDumpTailings(Actor actor, Tailing dumps) {
    if (! dumps.inWorld()) {
      if (! dumps.canPlace()) return false;
      else dumps.enterWorld();
    }
    if (! dumps.takeFill(1)) return false;
    this.stage = STAGE_MINE;
    return true;
  }
  
  
  public boolean actionMineSurface(Actor actor, Target face) {
    
    //  TODO:  Re-implement this...
    return true;
    /*
    final Item left = mineralsLeft(face);
    if (left == null) return false;
    
    final float oldAmount = face.mineralAmount();
    float progress = successCheck(actor, Habitat.MESA) / face.bulk();
    progress /= DEFAULT_TILE_DIG_TIME;
    final float bonus = site.extractionBonus(left.type);
    progress *= 1 + (bonus / 2f);
    
    face.incCondition(0 - progress);
    if (face.condition() == 0) face.setAsDestroyed();
    final float taken = oldAmount - face.mineralAmount();
    if (taken == 0) return false;
    
    final Item mined = Item.withAmount(left.type, taken);
    actor.gear.addItem(mined);
    return true;
    //*/
  }
  
  
  /*
  private static float successCheck(Actor actor, Habitat h) {
    //  Progress is slower in harder soils....
    float success = 1;
    success += actor.skills.test(GEOPHYSICS , 5 , 1) ? 1 : 0;
    success *= actor.skills.test(HARD_LABOUR, 15, 1) ? 2 : 1;
    if (h != null) success *= (0.5f + 1 - (h.minerals() / 10f));
    ///I.say("Base success: "+success);
    return success / 4f;
  }
  //*/
  
  /*
  private static Item mineralsLeft(Target face) {
    byte type = -1;
    float amount = -1;
    else if (face instanceof Outcrop) {
      final Outcrop o = (Outcrop) face;
      type = o.mineralType();
      amount = o.mineralAmount();
      if (type == WorldTerrain.TYPE_NOTHING) return null;
    }
    return Item.withAmount(mineral, amount);
  }
  //*/
  
  
  
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
      d.append("Returning ores to "+actor.focusFor(StripMining.class));
    }
  }
}







