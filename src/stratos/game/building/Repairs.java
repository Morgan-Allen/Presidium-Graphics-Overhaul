


package stratos.game.building ;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.user.*;
import stratos.util.*;





public class Repairs extends Plan implements Qualities {
  
  
  
  /**  Field definitions, constants and save/load methods-
    */
  final static float
    TIME_PER_25_HP = World.STANDARD_HOUR_LENGTH,
    MIN_SERVICE_DAMAGE = 0.25f;
  
  private static boolean verbose = false;
  final Venue built ;
  
  
  public Repairs(Actor actor, Venue repaired) {
    super(actor, repaired) ;
    this.built = repaired ;
  }
  
  
  public Repairs(Session s) throws Exception {
    super(s) ;
    built = (Venue) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(built) ;
  }
  
  
  
  /**  Assessing targets and priority-
    */
  //  TODO:  Move this to the Structure class?
  public static float needForRepair(Installation built) {
    float needRepair;
    final Structure structure = built.structure();
    if (! structure.intact()) needRepair = 1.0f ;
    else needRepair = (1 - structure.repairLevel()) * 1.5f ;
    if (structure.needsUpgrade()) needRepair += 0.5f ;
    if (structure.burning()) needRepair += 1.0f ;
    return needRepair;
  }
  
  
  //  TODO:  Get rid of this?  Just use actor awareness?
  public static Repairs getNextRepairFor(Actor client, float motiveBonus) {
    final World world = client.world() ;
    final Batch <Venue> toRepair = new Batch <Venue> () ;
    world.presences.sampleFromMaps(
      client, world, 5, toRepair, "damaged"//, Venue.class
    ) ;
    final Choice choice = new Choice(client) ;
    for (Venue near : toRepair) {
      if (near.base() != client.base()) continue ;
      if (near.structure.goodCondition()) {
        I.say(near+" SHOULD NOT BE REGISTERED FOR REPAIRS");
        continue;
      }
      if (needForRepair(near) <= 0) continue;
      final Repairs b = new Repairs(client, near) ;
      b.setMotive(Plan.MOTIVE_DUTY, motiveBonus);
      choice.add(b) ;
    }
    
    return (Repairs) choice.pickMostUrgent() ;
  }
  
  
  final Trait BASE_TRAITS[] = { METICULOUS, ENERGETIC };
  final Skill BASE_SKILLS[] = { ASSEMBLY, HARD_LABOUR };
  

  protected float getPriority() {
    final boolean report = verbose && I.talkAbout == actor && hasBegun();

    float urgency = needForRepair(built);
    if (report) I.say("Urgency for repair of "+built+" is "+urgency);
    
    urgency *= actor.memories.relationValue(built.base());
    final float debt = 0 - built.base().credits();
    if (debt > 0 && urgency > 0) urgency -= debt / 500f;
    if (urgency <= 0) {
      if (report) I.say("Urgency after debt and relations: "+urgency);
      return 0;
    }
    
    float competition = FULL_COMPETITION;
    competition /= 1 + (built.structure.maxIntegrity() / 100f);
    final float help = REAL_HELP + (actor.base().communitySpirit() / 2);
    
    final float priority = priorityForActorWith(
      actor, built, ROUTINE * Visit.clamp(urgency, 0, 1),
      help, competition,
      BASE_SKILLS, BASE_TRAITS,
      NO_MODIFIER, NORMAL_DISTANCE_CHECK, MILD_FAIL_RISK,
      report
    );
    if (report) {
      I.say("Repairing "+built);
      I.say("  Basic urgency: "+urgency+", debt level: "+debt);
      I.say("  Help/Competition: "+help+"/"+competition);
      I.say("  Final priority: "+priority);
    }
    return priority;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public boolean finished() {
    if (super.finished()) return true ;
    if (built.structure.hasWear()) return false ;
    if (built.structure.needsUpgrade()) return false ;
    return true ;
  }
  
  
  protected Behaviour getNextStep() {
    final boolean report = verbose && I.talkAbout == actor && hasBegun();
    if (report) I.say("\nGetting next build step?") ;
    
    if (built.structure.hasWear()) {
      final Action building = new Action(
        actor, built,
        this, "actionBuild",
        Action.BUILD, "Assembling "+built
      ) ;
      if (! Spacing.adjacent(actor.origin(), built) || Rand.num() < 0.2f) {
        final Tile t = Spacing.pickFreeTileAround(built, actor) ;
        if (t == null) {
          abortBehaviour() ;
          return null ;
        }
        building.setMoveTarget(t) ;
      }
      else building.setMoveTarget(actor.origin()) ;
      if (report) I.say("  Returning next build action.");
      return building ;
    }
    if (built.structure.needsUpgrade()) {
      final Action upgrades = new Action(
        actor, built,
        this, "actionUpgrade",
        Action.BUILD, "Upgrading "+built
      ) ;
      return upgrades ;
    }
    
    return null ;
  }
  
  
  public boolean actionBuild(Actor actor, Venue built) {
    //
    //  Double the rate of repair again if you have proper tools and materials.
    final boolean salvage = built.structure.needsSalvage() ;
    final boolean free = GameSettings.buildFree ;
    final Base base = built.base();
    
    float success = actor.traits.test(HARD_LABOUR, ROUTINE_DC, 1) ? 1 : 0 ;
    success *= 25f / TIME_PER_25_HP;
    
    //  TODO:  Base assembly DC (or other skills) on a Conversion for the
    //  structure.  Require construction materials for full efficiency.
    
    if (salvage) {
      success *= actor.traits.test(ASSEMBLY, 5, 1) ? 1 : 0.5f ;
      final float amount = 0 - built.structure.repairBy(success) ;
      final float cost = amount * built.structure.buildCost() ;
      if (! free) base.incCredits(cost * 0.5f) ;
    }
    
    else {
      success *= actor.traits.test(ASSEMBLY, 10, 0.5f) ? 1 : 0.5f ;
      success *= actor.traits.test(ASSEMBLY, 20, 0.5f) ? 2 : 1 ;
      final boolean intact = built.structure.intact() ;
      final float amount = built.structure.repairBy(success) ;
      final float cost = amount * built.structure.buildCost() ;
      if (! free) base.incCredits((0 - cost) * (intact ? 0.5f : 1)) ;
    }
    return true ;
  }
  
  
  public boolean actionUpgrade(Actor actor, Venue built) {
    final Upgrade upgrade = built.structure.upgradeInProgress() ;
    ///if (upgrade == null) I.say("NO UPGRADE!") ;
    if (upgrade == null) return false ;
    ///I.say("Advancing upgrade: "+upgrade.name) ;
    int success = 1 ;
    success *= actor.traits.test(ASSEMBLY, 10, 0.5f) ? 2 : 1 ;
    success *= actor.traits.test(ASSEMBLY, 20, 0.5f) ? 2 : 1 ;
    final float amount = built.structure.advanceUpgrade(success * 1f / 100) ;
    final float cost = amount * upgrade.buildCost ;
    built.base().incCredits((0 - cost)) ;
    return true ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    d.append("Repairing ") ;
    d.append(built) ;
  }
}








