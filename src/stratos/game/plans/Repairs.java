


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.PavingMap;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.base.BaseFinance.*;



//  TODO:  USE A CONVERSION SPECIFIC TO THE BUILDING TYPE.


public class Repairs extends Plan {
  
  private static boolean
    evalVerbose  = false,
    stepsVerbose = false;
  
  final static int
    REPAIR_REPAIR  = 0,
    REPAIR_BUILD   = 1,
    REPAIR_UPGRADE = 2,
    REPAIR_SALVAGE = 3;
  final static String REPAIR_DESC[] = {
    "Repairing", "Building", "Upgrading", "Salvaging"
  };
  
  final public static float
    TIME_PER_25_HP     = Stage.STANDARD_HOUR_LENGTH / 5,
    MIN_SERVICE_DAMAGE = 0.25f,
    MAX_HELP_PER_25_HP = 0.5f ,
    BUILDS_COST_MULT   = 1.0f ,
    SALVAGE_COST_MULT  = 0.5f ,
    REPAIR_COST_MULT   = 0.25f;
  
  final Structure.Basis built;
  final Skill skillUsed;
  private int repairType = REPAIR_REPAIR;
  
  
  public Repairs(Actor actor, Structure.Basis repaired) {
    this(actor, repaired, ASSEMBLY);
  }
  
  
  public Repairs(Actor actor, Structure.Basis repaired, Skill skillUsed) {
    super(actor, (Target) repaired, MOTIVE_NONE, REAL_HELP);
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
    
    final float wear = 1 - structure.repairLevel(), min = MIN_SERVICE_DAMAGE;
    
    float needRepair;
    if (! structure.intact()) needRepair = 1.0f;
    else if (wear < min) needRepair = wear * 0.5f / min;
    else needRepair = 0.5f + ((wear - min) / (1 - min));
    
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
      if (asDuty) b.addMotives(Plan.MOTIVE_JOB);
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
      if (asDuty) r.addMotives(Plan.MOTIVE_JOB, Plan.CASUAL);
      choice.add(r);
      if (report) I.say("  Next tile to pave: "+toPave);
    }
    //
    //  Evaluate the most urgent task and return-
    if (report) I.say("\n  Total choices: "+choice.size());
    return (Plan) choice.pickMostUrgent();
  }
  

  /**  Target evaluation and prioritisation-
    */
  final static Trait BASE_TRAITS[] = { URBANE, ENERGETIC };
  
  protected float getPriority() {
    final boolean report = evalVerbose && (
      I.talkAbout == actor || I.talkAbout == built
    );
    
    float urgency = needForRepair(built), helpLimit;
    if (urgency <= 0) return 0;
    helpLimit = built.structure().maxIntegrity() * MAX_HELP_PER_25_HP / 25;
    
    setCompetence(successChanceFor(actor));
    final float priority = PlanUtils.jobPlanPriority(
      actor, this,
      urgency, competence(), (int) helpLimit, MILD_HARM, BASE_TRAITS
    );
    if (report) {
      I.say("\nRepair priority was: "+priority);
      I.say("  Repair level: "+built.structure().repairLevel());
      I.say("  Urgency:      "+urgency  );
      I.say("  Help limit:   "+helpLimit);
      //I.say("  Skill level:  "+actor.traits.usedLevel(skillUsed  ));
      //I.say("  Labour level: "+actor.traits.usedLevel(HARD_LABOUR));
      I.say("  Competence    "+competence());
    }
    return priority;
  }
  
  
  public float successChanceFor(Actor actor) {
    float chance = 1;
    //  TODO:  Base this on the conversion associated with the structure type.
    chance += actor.skills.chance(HARD_LABOUR, ROUTINE_DC );
    chance += actor.skills.chance(skillUsed  , MODERATE_DC);
    return chance / 3;
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
    if (built instanceof Venue && built.structure().intact()) {
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
    RoadsRepair.updatePavingAround(actor.origin(), built.base());
    
    //  TODO:  Double the rate of repair again if you have proper tools and
    //  materials.
    final Structure structure = built.structure();
    final boolean salvage = structure.needsSalvage();
    final boolean free = GameSettings.buildFree;
    final Base base = built.base();
    
    float success = actor.skills.test(HARD_LABOUR, ROUTINE_DC, 1) ? 1 : 0;
    success *= 25f / TIME_PER_25_HP;
    float cost = 0;
    
    //  TODO:  Base assembly DC (or other skills) on a Conversion for the
    //  structure.  Require construction materials for full efficiency.
    if (salvage) {
      success *= actor.skills.test(skillUsed, 5, 1) ? 1 : 0.5f;
      final float amount = 0 - structure.repairBy(0 - success);
      if (! free) {
        cost = amount * structure.buildCost() * SALVAGE_COST_MULT;
        base.finance.incCredits(cost, SOURCE_SALVAGE);
      }
    }
    
    else {
      success *= actor.skills.test(skillUsed, 10, 0.5f) ? 1 : 0.5f;
      success *= actor.skills.test(skillUsed, 20, 0.5f) ? 2 : 1;
      final boolean intact = structure.intact();
      final float amount = structure.repairBy(success);
      if (! free) {
        cost = amount * structure.buildCost();
        cost *= -1 * (intact ? REPAIR_COST_MULT : BUILDS_COST_MULT);
        base.finance.incCredits(cost, intact ? SOURCE_REPAIRS : SOURCE_INSTALL);
      }
    }
    
    if (report) {
      I.say("Repairing structure: "+built);
      I.say("  Repair type:     "+REPAIR_DESC[repairType]);
      I.say("  Repair success:  "+success);
      I.say("  Repair level:    "+structure.repairLevel());
      I.say("  Full build cost: "+built.structure().buildCost());
      I.say("  Repair cost:     "+(0 - cost));
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
    built.base().finance.incCredits((0 - cost), SOURCE_UPGRADE);
    return true;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    d.append(REPAIR_DESC[repairType]+" ");
    d.append(built);
  }
}








