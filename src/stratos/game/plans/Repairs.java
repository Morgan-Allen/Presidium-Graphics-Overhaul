


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.PavingMap;
import stratos.game.politic.BaseFinance;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;



//  TODO:  USE A CONVERSION SPECIFIC TO THE BUILDING TYPE.


public class Repairs extends Plan {
  
  private static boolean
    evalVerbose  = false,
    stepsVerbose = true ;
  
  final static int
    REPAIR_REPAIR  = 0,
    REPAIR_BUILD   = 1,
    REPAIR_UPGRADE = 2,
    REPAIR_SALVAGE = 3;
  final static String REPAIR_DESC[] = {
    "Repairing", "Building", "Upgrading", "Salvaging"
  };
  
  final public static float
    TIME_PER_25_HP     = Stage.STANDARD_HOUR_LENGTH / 2,
    MIN_SERVICE_DAMAGE = 0.25f;
  
  final Structure.Basis built;
  final Skill skillUsed;
  private int repairType = REPAIR_REPAIR;
  
  
  public Repairs(Actor actor, Structure.Basis repaired) {
    this(actor, repaired, ASSEMBLY);
  }
  
  
  public Repairs(Actor actor, Structure.Basis repaired, Skill skillUsed) {
    super(actor, (Target) repaired, MOTIVE_JOB, REAL_HELP);
    this.built = repaired;
    this.skillUsed = skillUsed;
  }
  
  
  public Repairs(Session s) throws Exception {
    super(s);
    built      = (Structure.Basis) s.loadObject();
    skillUsed  = (Skill) s.loadObject();
    repairType = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(built     );
    s.saveObject(skillUsed );
    s.saveInt   (repairType);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Repairs(other, built, skillUsed);
  }
  
  
  
  /**  Assessing targets and priority-
    */
  public static float needForRepair(Structure.Basis built) {
    final Structure structure = built.structure();
    if (! structure.takesWear()) return 0;
    float needRepair;
    if (! structure.intact()) needRepair = 1.0f;
    else needRepair = (1 - structure.repairLevel()) * 1.5f;
    if (structure.burning()) needRepair += 1.0f;
    if (structure.needsUpgrade()) needRepair = Nums.max(needRepair, 1);
    return needRepair;
  }
  
  
  public static Plan getNextRepairFor(Actor client, boolean asDuty) {
    final Stage world = client.world();
    final Choice choice = new Choice(client);
    final boolean report = evalVerbose && I.talkAbout == client;
    choice.isVerbose = report;
    //
    //  First, sample any nearby venues that require repair, and add them to
    //  the list.
    final Batch <Structure.Basis> toRepair = new Batch <Structure.Basis> ();
    world.presences.sampleFromMaps(
      client, world, 3, toRepair, Structure.DAMAGE_KEY
    );
    for (Structure.Basis near : toRepair) {
      if (near.base() != client.base()) continue;
      if (needForRepair(near) <= 0) continue;
      final Repairs b = new Repairs(client, near);
      if (asDuty) b.setMotive(Plan.MOTIVE_JOB, Plan.ROUTINE);
      choice.add(b);
      
      if (report) {
        I.say("\n  "+near+" needs repair?");
        I.say("  Need is: "+needForRepair(near));
        I.say("  Needs upgrade? "+near.structure().needsUpgrade());
      }
    }
    //
    //  Then, see if there are tiles which require paving nearby-
    final PavingMap p = client.base().transport.map;
    final Tile toPave = p.nextTileToPave(client, null);
    if (toPave != null) {
      final RoadsRepair r = new RoadsRepair(client, toPave);
      if (asDuty) r.setMotive(Plan.MOTIVE_JOB, Plan.CASUAL);
      choice.add(r);
    }
    //
    //  Evaluate the most urgent task and return-
    if (report) I.say("\n  Total choices: "+choice.size());
    return (Plan) choice.pickMostUrgent();
  }
  

  /**  Target evaluation and prioritisation-
    */
  final static Trait BASE_TRAITS[] = { URBANE, ENERGETIC };
  //final static Skill BASE_SKILLS[] = { skillUsed, HARD_LABOUR };
  
  
  protected float getPriority() {
    final boolean report = I.talkAbout == actor && evalVerbose;
    
    float urgency = needForRepair(built);
    if (urgency <= 0) return 0;
    float competition = MILD_COOPERATION;
    
    final float priority = priorityForActorWith(
      actor, (Target) built,
      ROUTINE * Nums.clamp(urgency, 0, 1), NO_MODIFIER,
      REAL_HELP, competition, MILD_FAIL_RISK,
      NO_SKILLS, BASE_TRAITS, NORMAL_DISTANCE_CHECK,
      report
    );
    if (report) {
      I.say("  Repairing "+built+", base: "+built.base()+"?");
      I.say("  Intrinsic urgency: "+urgency);
      I.say("  PRIORITY:          "+(ROUTINE * Nums.clamp(urgency, 0, 1)));
      I.say("  Community spirit:  "+actor.base().relations.communitySpirit());
      I.say("  Competition:       "+competition);
      I.say("  Final priority:    "+priority);
    }
    return priority;
  }
  
  
  public float successChanceFor(Actor actor) {
    float chance = 1;
    //  TODO:  Base this on the conversion associated with the structure type.
    chance *= actor.skills.chance(HARD_LABOUR, 0);
    chance *= actor.skills.chance(skillUsed   , 5);
    return (chance + 1) / 2;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public boolean finished() {
    if (super.finished()) return true;
    if (built.structure().hasWear()) return false;
    if (built.structure().needsUpgrade()) return false;
    return true;
  }
  
  
  protected Behaviour getNextStep() {
    final boolean report = stepsVerbose && I.talkAbout == actor && hasBegun();
    if (report) I.say("\nGetting next build step?");
    final Structure structure = built.structure();
    final Element basis = (Element) built;
    Action building = null;
    
    //
    //  Determine the type of action that needs to be taken:
    if (structure.needsUpgrade() && structure.goodCondition()) {
      building = new Action(
        actor, built,
        this, "actionUpgrade",
        Action.BUILD, "Upgrading "
      );
      repairType = REPAIR_UPGRADE;
      if (report) I.say("  Returning next upgrade action.");
    }
    
    if (structure.hasWear()) {
      building = new Action(
        actor, built,
        this, "actionBuild",
        Action.BUILD, "Assembling "
      );
      repairType = structure.needsSalvage() ? REPAIR_SALVAGE : (
        structure.intact() ? REPAIR_REPAIR : REPAIR_BUILD
      );
      if (report) I.say("  Returning next build action.");
    }
    
    if (report && building == null) I.say("NOTHING TO BUILD");
    if (building == null) return null;
    //
    //  Make sure that you can find a suitable spot to begin building at:
    final boolean near = Spacing.adjacent(actor.origin(), basis);
    boolean moveSet = false;
    final boolean roll = Rand.num() < 0.2f;
    
    if (built instanceof Vehicle) {
      final Vehicle m = (Vehicle) built;
      if (m.indoors()) {
        building.setMoveTarget(m.aboard());
        moveSet = true;
      }
      else if (m.pilot() != null) return null;
    }
    if (built instanceof Venue && built.structure().intact() && ! roll) {
      if (((Venue) built).mainEntrance() != null) moveSet = true;
    }
    //
    //  If none is found, try to use an adjacent tile:
    if ((! moveSet) && (roll || ! near)) {
      final Tile t = Spacing.pickFreeTileAround(built, actor);
      if (t == null) return null;
      building.setMoveTarget(t);
      moveSet = true;
    }
    if (! moveSet) {
      building.setMoveTarget(actor.origin());
    }
    return building;
  }
  
  
  public boolean actionEstablish(Actor actor, Structure.Basis built) {
    final Element e = (Element) built;
    e.enterWorld();
    return true;
  }
  
  
  public boolean actionBuild(Actor actor, Structure.Basis built) {
    final boolean report = stepsVerbose && I.talkAbout == actor && hasBegun();
    
    //  TODO:  Double the rate of repair again if you have proper tools and
    //  materials.
    final Structure structure = built.structure();
    final boolean salvage = structure.needsSalvage();
    final boolean free = GameSettings.buildFree;
    final Base base = built.base();
    
    float success = actor.skills.test(HARD_LABOUR, ROUTINE_DC, 1) ? 1 : 0;
    success *= 25f / TIME_PER_25_HP;
    
    //  TODO:  Base assembly DC (or other skills) on a Conversion for the
    //  structure.  Require construction materials for full efficiency.
    if (salvage) {
      success *= actor.skills.test(skillUsed, 5, 1) ? 1 : 0.5f;
      final float amount = structure.repairBy(0 - success);
      if (! free) {
        final float cost = amount * structure.buildCost();
        base.finance.incCredits(cost * 0.5f, BaseFinance.SOURCE_REPAIRS);
      }
      if (report) I.say("Salvage sucess: "+success);
      if (report) I.say("Repair level: "+structure.repairLevel());
    }
    
    else {
      success *= actor.skills.test(skillUsed, 10, 0.5f) ? 1 : 0.5f;
      success *= actor.skills.test(skillUsed, 20, 0.5f) ? 2 : 1;
      final boolean intact = structure.intact();
      final float amount = structure.repairBy(success);
      if (! free) {
        float cost = amount * structure.buildCost();
        cost *= -1 * (intact ? 0.5f : 1);
        base.finance.incCredits(cost, BaseFinance.SOURCE_REPAIRS);
      }
    }
    return true;
  }
  
  
  public boolean actionUpgrade(Actor actor, Structure.Basis built) {
    final Structure structure = built.structure();
    final Upgrade upgrade = structure.upgradeInProgress();
    ///if (upgrade == null) I.say("NO UPGRADE!");
    if (upgrade == null) return false;
    ///I.say("Advancing upgrade: "+upgrade.name);
    int success = 1;
    success *= actor.skills.test(skillUsed, 10, 0.5f) ? 2 : 1;
    success *= actor.skills.test(skillUsed, 20, 0.5f) ? 2 : 1;
    final float amount = structure.advanceUpgrade(success * 1f / 100);
    final float cost = amount * upgrade.buildCost;
    built.base().finance.incCredits((0 - cost), BaseFinance.SOURCE_REPAIRS);
    return true;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    d.append(REPAIR_DESC[repairType]+" ");
    d.append(built);
  }
}








