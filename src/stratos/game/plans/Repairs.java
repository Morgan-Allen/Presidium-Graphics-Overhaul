


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.building.Installation;
import stratos.game.building.Structure;
import stratos.game.building.Upgrade;
import stratos.game.common.*;
import stratos.user.*;
import stratos.util.*;





public class Repairs extends Plan implements Qualities {
  
  
  
  /**  Field definitions, constants and save/load methods-
    */
  final public static float
    TIME_PER_25_HP = World.STANDARD_HOUR_LENGTH,
    MIN_SERVICE_DAMAGE = 0.25f;
  
  private static boolean
    evalVerbose   = false,
    eventsVerbose = false;
  
  final Installation built;
  
  
  public Repairs(Actor actor, Installation repaired) {
    super(actor, (Target) repaired);
    this.built = repaired;
  }
  
  
  public Repairs(Session s) throws Exception {
    super(s);
    built = (Installation) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(built);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Repairs(other, built);
  }
  
  
  
  /**  Assessing targets and priority-
    */
  public static float needForRepair(Installation built) {
    float needRepair;
    final Structure structure = built.structure();
    if (! structure.intact()) needRepair = 1.0f;
    else needRepair = (1 - structure.repairLevel()) * 1.5f;
    if (structure.burning()) needRepair += 1.0f;
    if (structure.needsUpgrade()) needRepair = Math.max(needRepair, 1);
    return needRepair;
  }
  
  
  public static Repairs getNextRepairFor(Actor client, float motiveBonus) {
    
    final World world = client.world();
    final Batch <Installation> toRepair = new Batch <Installation> ();
    world.presences.sampleFromMaps(
      client, world, 3, toRepair, "damaged"
    );
    final Choice choice = new Choice(client);
    for (Installation near : toRepair) {
      if (near.base() != client.base()) continue;
      if (needForRepair(near) <= 0) continue;
      final Repairs b = new Repairs(client, near);
      b.setMotive(Plan.MOTIVE_DUTY, motiveBonus);
      choice.add(b);
    }
    choice.isVerbose = evalVerbose && I.talkAbout == client;
    return (Repairs) choice.pickMostUrgent();
  }
  
  
  final Trait BASE_TRAITS[] = { URBANE, ENERGETIC };
  final Skill BASE_SKILLS[] = { ASSEMBLY, HARD_LABOUR };
  
  
  protected float getPriority() {
    final boolean report = I.talkAbout == actor && evalVerbose && hasBegun();
    
    float urgency = needForRepair(built);
    urgency *= actor.relations.relationValue(built.base());
    
    final float debt = 0 - built.base().credits();
    if (debt > 0 && urgency > 0) urgency -= debt / 500f;
    if (urgency <= 0) return 0;
    
    float competition = FULL_COMPETITION;
    competition /= 1 + (built.structure().maxIntegrity() / 100f);
    final float help = REAL_HELP * actor.base().communitySpirit();
    
    final float priority = priorityForActorWith(
      actor, (Target) built, CASUAL * Visit.clamp(urgency, 0, 1),
      help, competition,
      BASE_SKILLS, BASE_TRAITS,
      NO_MODIFIER, NORMAL_DISTANCE_CHECK, MILD_FAIL_RISK,
      report
    );
    if (report) {
      I.say("Repairing "+built+", base: "+built.base());
      I.say("  Basic urgency: "+urgency+", debt level: "+debt);
      I.say("  Help/Competition: "+help+"/"+competition);
      I.say("  Final priority: "+priority);
    }
    return priority;
  }
  
  
  protected float successChance() {
    float chance = 1;
    //  TODO:  Base this on the conversion associated with the structure type.
    chance *= actor.traits.chance(HARD_LABOUR, 0);
    chance *= actor.traits.chance(ASSEMBLY, 5);
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
    final boolean report = eventsVerbose && I.talkAbout == actor && hasBegun();
    if (report) I.say("\nGetting next build step?");
    final Structure structure = built.structure();
    final Element basis = (Element) built;
    
    if (structure.needsUpgrade() && structure.goodCondition()) {
      final Action upgrades = new Action(
        actor, built,
        this, "actionUpgrade",
        Action.BUILD, "Upgrading "+built
      );
      if (report) I.say("  Returning next upgrade action.");
      return upgrades;
    }
    
    if (structure.hasWear()) {
      final Action building = new Action(
        actor, built,
        this, "actionBuild",
        Action.BUILD, "Assembling "+built
      );
      if (! Spacing.adjacent(actor.origin(), basis) || Rand.num() < 0.2f) {
        final Tile t = Spacing.pickFreeTileAround(built, actor);
        if (t == null) {
          abortBehaviour();
          return null;
        }
        building.setMoveTarget(t);
      }
      else building.setMoveTarget(actor.origin());
      if (report) I.say("  Returning next build action.");
      return building;
    }
    
    if (report) I.say("NOTHING TO BUILD");
    return null;
  }
  
  
  public boolean actionBuild(Actor actor, Installation built) {
    final boolean report = eventsVerbose && I.talkAbout == actor && hasBegun();
    
    //  TODO:  Double the rate of repair again if you have proper tools and
    //  materials.
    final Structure structure = built.structure();
    final boolean salvage = structure.needsSalvage();
    final boolean free = GameSettings.buildFree;
    final Base base = built.base();
    
    float success = actor.traits.test(HARD_LABOUR, ROUTINE_DC, 1) ? 1 : 0;
    success *= 25f / TIME_PER_25_HP;
    
    //  TODO:  Base assembly DC (or other skills) on a Conversion for the
    //  structure.  Require construction materials for full efficiency.
    if (salvage) {
      success *= actor.traits.test(ASSEMBLY, 5, 1) ? 1 : 0.5f;
      final float amount = structure.repairBy(0 - success);
      final float cost = amount * structure.buildCost();
      if (! free) base.incCredits(cost * 0.5f);
      if (report) I.say("Salvage sucess: "+success);
      if (report) I.say("Repair level: "+structure.repairLevel());
    }
    
    else {
      success *= actor.traits.test(ASSEMBLY, 10, 0.5f) ? 1 : 0.5f;
      success *= actor.traits.test(ASSEMBLY, 20, 0.5f) ? 2 : 1;
      final boolean intact = structure.intact();
      final float amount = structure.repairBy(success);
      final float cost = amount * structure.buildCost();
      if (! free) base.incCredits((0 - cost) * (intact ? 0.5f : 1));
    }
    return true;
  }
  
  
  public boolean actionUpgrade(Actor actor, Installation built) {
    final Structure structure = built.structure();
    final Upgrade upgrade = structure.upgradeInProgress();
    ///if (upgrade == null) I.say("NO UPGRADE!");
    if (upgrade == null) return false;
    ///I.say("Advancing upgrade: "+upgrade.name);
    int success = 1;
    success *= actor.traits.test(ASSEMBLY, 10, 0.5f) ? 2 : 1;
    success *= actor.traits.test(ASSEMBLY, 20, 0.5f) ? 2 : 1;
    final float amount = structure.advanceUpgrade(success * 1f / 100);
    final float cost = amount * upgrade.buildCost;
    built.base().incCredits((0 - cost));
    return true;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    d.append("Repairing ");
    d.append(built);
  }
}








