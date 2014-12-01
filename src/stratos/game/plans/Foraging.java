/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */



package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



//  TODO:  Consider merging this with forestry (or maybe merging forest-planting
//  with farming, and forest-cutting/sampling with this.)


public class Foraging extends Plan {
  
  
  private static boolean
    evalVerbose  = false,
    stepsVerbose = false;
  
  
  final Venue store;
  private Flora source = null;
  private boolean done = false;
  
  
  public Foraging(Actor actor, Venue store) {
    super(actor, actor, true);
    if (store == null && actor.mind.home() instanceof Venue) {
      this.store = (Venue) actor.mind.home();
    }
    else this.store = store;
  }
  
  
  public Foraging(Session s) throws Exception {
    super(s);
    store = (Venue) s.loadObject();
    source = (Flora) s.loadObject();
    done = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(store);
    s.saveObject(source);
    s.saveBool(done);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Foraging(other, store);
  }

  
  
  /**  Behaviour implementation-
    */
  final static Trait BASE_TRAITS[] = { NATURALIST, ENERGETIC, ACQUISITIVE };
  final static Skill BASE_SKILLS[] = { CULTIVATION, HARD_LABOUR };
  
  
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    
    if (storeShortage() <= 0) {
      if (sumHarvest() > 0) return Plan.ROUTINE;
      else done = true;
    }
    final float hunger = actor.health.hungerLevel();
    if (store == null && hunger <= 0) done = true;
    if (done) return 0;
    
    if (source == null || source.destroyed()) {
      source = Forestry.findCutting(actor);
      if (source == null) return 0;
    }
    final float modifier = NO_MODIFIER + (hunger * ROUTINE);
    
    final float priority = priorityForActorWith(
      actor, source, hunger * PARAMOUNT,
      modifier, NO_HARM,
      FULL_COMPETITION, MILD_FAIL_RISK,
      BASE_SKILLS, BASE_TRAITS, NORMAL_DISTANCE_CHECK,
      report
    );
    if (report) {
      I.say("  Hunger level was: "+hunger);
      I.say("  Final priority: "+priority);
    }
    return priority;
  }
  
  
  protected float successChance() {
    float chance = 1;
    chance *= actor.skills.chance(HARD_LABOUR, ROUTINE_DC);
    chance *= actor.skills.chance(CULTIVATION, MODERATE_DC);
    return chance;
  }
  
  
  
  private float storeShortage() {
    if (store == null) return 1;
    return 10 - (store.stocks.amountOf(GREENS) + store.stocks.amountOf(CARBS));
  }
  
  
  private float sumHarvest() {
    return actor.gear.amountOf(CARBS) + actor.gear.amountOf(GREENS);
  }
  
  
  
  public Behaviour getNextStep() {
    if (done) return null;
    final float shortage = storeShortage();
    if (shortage > 0 && (source == null || source.destroyed())) {
      source = Forestry.findCutting(actor);
      if (source == null) return null;
    }
    final float harvest = sumHarvest();
    if (shortage > 0 && harvest < 1 && source != null) {
      final Action forage = new Action(
        actor, source,
        this, "actionForage",
        Action.BUILD, "Foraging"
      );
      forage.setMoveTarget(Spacing.nearestOpenTile(source, actor));
      return forage;
    }
    else if (store != null && harvest > 0) {
      final Action returns = new Action(
        actor, store,
        this, "actionReturnHarvest",
        Action.REACH_DOWN, "Returning forage"
      );
      return returns;
    }
    return null;
  }
  
  
  
  public boolean actionForage(Actor actor, Flora source) {
    if (source == null || source.destroyed()) {
      source = null;
      return false;
    }
    
    float labour = 0, skill = 0;
    if (actor.skills.test(HARD_LABOUR, ROUTINE_DC, 1.0f)) labour++;
    if (actor.skills.test(CULTIVATION, ROUTINE_DC, 1.0f)) skill++;
    if (actor.skills.test(CULTIVATION, DIFFICULT_DC, 1.0f)) {
      labour++;
      skill++;
    }
    Resting.dineFrom(actor, actor);
    
    if (labour > 0 && skill > 0) {
      actor.gear.bumpItem(CARBS, labour * 0.1f);
      actor.gear.bumpItem(GREENS, skill * labour * 0.1f);
      source.incGrowth(-0.1f * (skill + labour), actor.world(), false);
      source = null;
      return true;
    }
    else source.incGrowth(-0.1f / 2f, actor.world(), false);
    source = null;
    return false;
  }
  
  
  /*
  public boolean actionFell(Actor actor, Flora cut) {
    //
    //  TODO:  Allow for wood-cutting behaviours as well?
    return true;
  }
  //*/
  
  
  public boolean actionReturnHarvest(Actor actor, Venue depot) {
    actor.gear.transfer(GREENS, depot);
    actor.gear.transfer(CARBS, depot);
    actor.gear.transfer(Item.withReference(SAMPLES, Species.TIMBER), depot);
    done = true;
    return true;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    if (super.needsSuffix(d, "Foraging"));
  }
}






