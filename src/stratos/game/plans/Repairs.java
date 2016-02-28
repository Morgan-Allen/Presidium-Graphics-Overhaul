/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.maps.*;
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
    MIN_SERVICE_DAMAGE = 0.1f ,
    MAX_HELP_PER_25_HP = 0.5f ,
    BUILDS_COST_MULT   = 1.0f ,
    SALVAGE_COST_MULT  = 0.5f ,
    REPAIR_COST_MULT   = 0.25f;
  
  final Placeable built;
  final Skill skillUsed;
  private int repairType = REPAIR_REPAIR;
  
  
  public Repairs(Actor actor, Placeable repaired, boolean asJob) {
    this(actor, repaired, ASSEMBLY, asJob);
  }
  
  
  public Repairs(
    Actor actor, Placeable repaired, Skill skillUsed, boolean asJob
  ) {
    super(actor, (Target) repaired, NO_PROPERTIES, REAL_HELP);
    this.built     = repaired ;
    this.skillUsed = skillUsed;
    if (asJob) toggleMotives(MOTIVE_JOB, true);
  }
  
  
  public Repairs(Session s) throws Exception {
    super(s);
    built      = (Placeable) s.loadObject();
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
    return new Repairs(other, built, skillUsed, isJob());
  }
  
  
  
  /**  Assessing targets and priority-
    */
  public static float needForRepair(Placeable built) {
    final Structure structure = built.structure();
    if (! structure.takesWear()) return 0;
    
    final float wear = 1 - structure.repairLevel(), min = MIN_SERVICE_DAMAGE;
    
    float needRepair;
    if (! structure.intact()) needRepair = 1.0f;
    else if (wear < min) needRepair = wear * 0.5f / min;
    else needRepair = 0.5f + ((wear - min) / (1 - min));
    
    if (structure.burning     ()) needRepair += 1.0f;
    if (structure.needsUpgrade()) needRepair = Nums.max(needRepair, 1);

    if (structure.isFixture()) needRepair *= 0.75f;
    if (structure.isLinear ()) needRepair *= 0.75f;
    
    return needRepair;
  }
  
  
  public static Plan getNextRepairFor(
    Actor client, boolean asDuty, float minDamage
  ) {
    final Stage  world  = client.world();
    final Choice choice = new Choice(client);
    final boolean report = I.talkAbout == client && evalVerbose;
    choice.isVerbose = report;
    //
    //  First, sample any nearby venues that require repair, and add them to
    //  the list.
    final Batch <Placeable> toRepair = new Batch();
    world.presences.sampleFromMaps(
      client, world, 3, toRepair, Structure.DAMAGE_KEY
    );
    for (Placeable near : toRepair) {
      if (! near.structure().isMechanical()) continue;
      if (near.base() != client.base()) continue;
      final float need = needForRepair(near);
      if (need <= minDamage) continue;
      
      final Repairs r = new Repairs(client, near, asDuty);
      choice.add(r);
      if (report) {
        I.say("\n  "+near+" needs repair?");
        I.say("  Need is:       "+need);
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
  final static Trait BASE_TRAITS[] = { METICULOUS, PERSISTENT };
  
  protected float getPriority() {
    final boolean report = (
      I.talkAbout == actor || I.talkAbout == built
    ) && evalVerbose;
    
    float urgency = needForRepair(built), helpLimit;
    if (urgency <= 0) return 0;
    helpLimit = built.structure().maxIntegrity() * MAX_HELP_PER_25_HP / 25;

    float chance = 1;
    //  TODO:  Base this on the conversion associated with the structure type.
    chance += actor.skills.chance(HARD_LABOUR, ROUTINE_DC );
    chance += actor.skills.chance(skillUsed  , MODERATE_DC);
    setCompetence(chance / 3);
    
    final float priority = PlanUtils.jobPlanPriority(
      actor, this,
      urgency, competence(), (int) helpLimit, MILD_HARM, BASE_TRAITS
    );
    if (report) {
      I.say("\nRepair priority was: "+priority);
      I.say("  Repair level: "+built.structure().repairLevel());
      I.say("  Urgency:      "+urgency  );
      I.say("  Help limit:   "+helpLimit);
      I.say("  Competence    "+competence());
    }
    return priority;
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
    final boolean report = I.talkAbout == actor && stepsVerbose && hasBegun();
    if (report) {
      I.say("\nGetting next build step for "+built);
    }
    
    final Structure structure = built.structure();
    this.repairType = REPAIR_REPAIR;
    
    final float   repair = structure.repairLevel();
    final Element basis  = (Element) built;
    final int     state  = structure.currentState();
    final boolean venue  = ! basis.isMobile();
    
    if (venue && state == Structure.STATE_INSTALL) {
      final Tile laid = repair > 0 ? null : nextFoundationTile(actor, true);
      repairType = REPAIR_BUILD;
      if (report) I.say("  Performing installation...");
      
      if (laid != null) {
        final Action lays = new Action(
          actor, laid,
          this, "actionLayFoundation",
          Action.BUILD, "Laying foundation"
        );
        lays.setMoveTarget(Spacing.pickFreeTileAround(laid, actor));
        if (report) I.say("  Laying foundation at "+laid);
        return lays;
      }
    }
    if (venue && state == Structure.STATE_SALVAGE) {
      final Tile strip = repair > 0 ? null : nextFoundationTile(actor, false);
      repairType = REPAIR_SALVAGE;
      if (report) I.say("  Performing salvage...");
      
      if (strip != null) {
        final Action strips = new Action(
          actor, strip,
          this, "actionStripFoundation",
          Action.BUILD, "Stripping foundation"
        );
        strips.setMoveTarget(Spacing.pickFreeTileAround(strip, actor));
        if (report) I.say("  Stripping foundation at "+strip);
        return strips;
      }
    }
    
    if (structure.needsUpgrade() && structure.repairLevel() >= 1) {
      final Action upgrades = new Action(
        actor, built,
        this, "actionUpgrade",
        Action.BUILD, "Upgrading "
      );
      repairType = REPAIR_UPGRADE;
      if (report) I.say("  Returning next upgrade action.");
      return upgrades;
    }
    
    final Action builds = new Action(
      actor, built,
      this, "actionBuild",
      Action.BUILD, "Assembling"
    );
    final Boarding movesTo = nextBuildPosition(actor);
    if (movesTo == null) return null;
    else builds.setMoveTarget(movesTo);
    
    if (repairType == REPAIR_REPAIR && report) {
      I.say("  Returning next building action.");
    }
    return builds;
  }
  
  
  private Tile nextFoundationTile(Actor actor, boolean lays) {
    final Tile entry = ((Venue) built).mainEntrance();
    if (entry == null) return null;
    
    final Stage world = actor.world();
    final Pick <Tile> pick = new Pick <Tile> ();
    
    final Plan repairing[] = world.activities.activePlanMatches(
      built, Repairs.class
    ).toArray(Plan.class);
    //
    //  To ensure that tiles in need of paving don't become inaccessible, we
    //  work backwards toward the venue's entrance.  (We also try to avoid more
    //  than one worker attached to the same tile.)
    for (Tile t : world.tilesIn(built.footprint(), false)) {
      for (Plan r : repairing) if (r != this) {
        if (r.stepFocus() == t || r.actor().origin() == t) continue;
      }
      final boolean laid = t.above() == built;
      if (laid == lays) continue;
      final float dist = Spacing.distance(t, entry);
      pick.compare(t, dist * (lays ? 1 : -1));
    }
    return pick.result();
  }
  
  
  public boolean actionLayFoundation(Actor actor, Tile at) {
    RoadsRepair.updatePavingAround(actor.origin(), built.base());
    flagSpriteForChange();
    //
    //  First, we check to ensure that the tile still needs laying-
    at = nextFoundationTile(actor, true);
    if (at == null || ! Spacing.adjacent(actor, at)) return false;
    //
    //  Then, clear out any competition and assert occupation of the tile.
    for (Tile n : at.vicinity(null)) if (n != null) {
      n.clearUnlessOwned();
    }
    at.setAbove((Element) built, true);
    //
    //  For shame, Montressor.  Don't brick up the workmen.
    for (Mobile m : at.inside()) {
      final Tile exit = Spacing.nearestOpenTile(m, m);
      if (exit != null) m.setPosition(exit.x, exit.y, exit.world);
    }
    return true;
  }
  
  
  public boolean actionStripFoundation(Actor actor, Tile at) {
    RoadsRepair.updatePavingAround(actor.origin(), built.base());
    flagSpriteForChange();
    if (at.above() == built) {
      at.setAbove(null, false);
    }
    if (nextFoundationTile(actor, false) == null) {
      built.structure().completeSalvage();
    }
    return true;
  }
  
  
  private Boarding nextBuildPosition(Actor actor) {
    //
    //  Vehicles are built from within their hangars-
    if (built instanceof Vehicle) {
      final Vehicle m = (Vehicle) built;
      if (m.indoors()) return m.aboard();
      else return null;
    }
    //
    //  Venues can be built from within or at random points around their
    //  perimeter.
    final Venue venue  = (Venue) built;
    final Stage world  = venue.world();
    final Tile  corner = venue.origin();
    final int   size   = venue.size;
    final float repair = venue.structure().repairLevel();
    //
    //  We only build from inside if the purpose is upgrading-
    if (repairType == REPAIR_UPGRADE) {
      if (Rand.num() < 0.2f || ! Spacing.adjacent(actor, built)) {
        return Spacing.pickFreeTileAround(built, actor);
      }
      return venue;
    }
    if (built.structure().intact()) {
      if (Rand.num() < 0.2f || ! Spacing.adjacent(actor, built)) {
        return Spacing.pickFreeTileAround(built, actor);
      }
      return actor.aboard();
    }
    //
    //  For installation & salvage, we try to move from the back of the
    //  structure forward (so as to match up with the construct/salvage build-
    //  sprite animation.)
    final Plan repairing[] = world.activities.activePlanMatches(
      built, Repairs.class
    ).toArray(Plan.class);
    final Pick <Tile> pick = new Pick();
    Tile atBack  = world.tileAt(corner.x + size - 1, corner.y);
    Tile atFront = world.tileAt(corner.x, corner.y + size - 1);
    
    for (Tile t : Spacing.perimeter(venue.footprint(), venue.world())) {
      if (t == null || t.blocked()) continue;
      for (Plan r : repairing) if (r != this) {
        if (r.stepFocus() == t || r.actor().origin() == t) continue;
      }
      float rating = 0;
      rating += Spacing.distance(t, atBack ) * (0 + repair);
      rating += Spacing.distance(t, atFront) * (1 - repair);
      rating -= Spacing.distance(t, actor  ) / (size + 1  );
      rating /= Nums.max(1, t.inside().size());
      pick.compare(t, rating);
    }
    return pick.result();
  }
  
  
  public boolean actionBuild(Actor actor, Placeable built) {
    final boolean report = stepsVerbose && I.talkAbout == actor && hasBegun();
    RoadsRepair.updatePavingAround(actor.origin(), built.base());
    flagSpriteForChange();
    //
    //  TODO:  Double the rate of repair again if you have proper tools and
    //  materials.
    final Structure structure = built.structure();
    final boolean   salvage   = structure.needsSalvage();
    final boolean   free      = GameSettings.buildFree;
    final Base      base      = built.base();
    final Action    a         = action();
    
    float success = actor.skills.test(HARD_LABOUR, ROUTINE_DC, 1, a) ? 1 : 0;
    success *= 25f / TIME_PER_25_HP;
    float cost = 0;
    //
    //  TODO:  Base assembly DC (or other skills) on a Conversion for the
    //  structure.  And require construction materials for full efficiency.
    if (salvage) {
      success *= actor.skills.test(skillUsed, 5, 1, a) ? 1 : 0.5f;
      final float amount = 0 - structure.repairBy(0 - success);
      if (! free) {
        cost = amount * structure.buildCost() * SALVAGE_COST_MULT;
        base.finance.incCredits(cost, SOURCE_SALVAGE);
      }
    }
    else {
      success *= actor.skills.test(skillUsed, ROUTINE_DC  , 0.5f, a) ? 1 : 0.5f;
      success *= actor.skills.test(skillUsed, DIFFICULT_DC, 0.5f, a) ? 2 : 1   ;
      final boolean intact = structure.intact();
      final float amount = structure.repairBy(success);
      if (! free) {
        cost = amount * structure.buildCost();
        cost *= -1 * (intact ? REPAIR_COST_MULT : BUILDS_COST_MULT);
        base.finance.incCredits(cost, intact ? SOURCE_REPAIRS : SOURCE_INSTALL);
      }
    }
    
    final Upgrade basis = structure.blueprintUpgrade();
    if (basis != null) {
      final float progress = success / structure.maxIntegrity();
      advancePrototypeResearch(basis, progress);
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
  
  
  public boolean actionUpgrade(Actor actor, Placeable built) {
    final Structure structure = built.structure();
    final Upgrade upgrade = structure.upgradeInProgress();
    if (upgrade == null) return false;
    
    final Action a = action();
    int success = 1;
    success *= actor.skills.test(skillUsed, 10, 0.5f, a) ? 2 : 1;
    success *= actor.skills.test(skillUsed, 20, 0.5f, a) ? 2 : 1;
    final float amount = structure.advanceUpgrade(success * 1f / 100);
    final float cost   = amount * upgrade.buildCost(actor.base());
    built.base().finance.incCredits((0 - cost), SOURCE_UPGRADE);
    advancePrototypeResearch(upgrade, Nums.abs(amount));
    return true;
  }
  
  
  private void advancePrototypeResearch(Upgrade u, float progress) {
    actor.base().research.incResearchFor(
      u, progress / BaseResearch.PROTOTYPE_COST_MULT, BaseResearch.LEVEL_PRAXIS
    );
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (repairType == REPAIR_UPGRADE) {
      Upgrade u = built.structure().upgradeInProgress();
      d.append("Installing ");
      d.append(u);
      d.append(" at ");
    }
    else d.append(REPAIR_DESC[repairType]+" ");
    d.append(built);
  }
  
  
  private void flagSpriteForChange() {
    if (! (built instanceof Venue)) return;
    //
    //  A slight kluge to improve efficiency.  (See the renderFor method in
    //  Venue.)
    ((Venue) built).buildSprite().flagChange = true;
  }
}









