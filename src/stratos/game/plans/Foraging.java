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
import stratos.game.wild.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



//  TODO:  Consider merging this with forestry (or maybe merging forest-planting
//  with farming, and forest-cutting/sampling with this.)

//  TODO:  Merge Foraging, Forestry and Farming into a single plan (or closely
//  related set of plans.)

public class Foraging extends Plan {
  
  
  private static boolean
    evalVerbose  = false,
    stepsVerbose = false;
  
  
  final Venue store;
  private Flora source = null;
  private boolean done = false;
  
  
  public Foraging(Actor actor, Venue store) {
    super(actor, actor, MOTIVE_PERSONAL, NO_HARM);
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
    final boolean report = I.talkAbout == actor && evalVerbose;
    final boolean useHunger = store == null || store == actor.mind.home();
    
    if (storeShortage() <= 0) {
      if (sumHarvest() > 0) return Plan.ROUTINE;
      else done = true;
    }
    final float hunger = actor.health.hungerLevel();
    if (useHunger && hunger <= 0) done = true;
    if (done) return 0;
    
    if (! sourceValid()) {
      source = Forestry.findCutting(actor, store, null);
      if (! sourceValid()) return 0;
    }
    
    float modifier = NO_PRIORITY;
    if (hunger > 0.5f && useHunger) {
      modifier += PARAMOUNT * (hunger - 0.5f) * 2;
    }
    
    setCompetence(successChanceFor(actor));
    final float priority = PlanUtils.jobPlanPriority(
      actor, this, modifier / PARAMOUNT, competence(),
      -1, REAL_FAIL_RISK, BASE_TRAITS
    );
    return priority;
  }
  
  
  public float successChanceFor(Actor actor) {
    if (isBrowsing()) return 1;
    float chance = 1;
    chance += actor.skills.chance(HARD_LABOUR, ROUTINE_DC );
    chance += actor.skills.chance(CULTIVATION, MODERATE_DC);
    return chance / 3;
  }
  
  
  //  TODO:  Make this an internal type.  And allow animals to just consume
  //  what they gather immediately.
  private boolean isBrowsing() {
    return actor.species().animal();
  }
  
  
  private boolean sourceValid() {
    return source != null && source.inWorld() && ! source.destroyed();
  }
  
  
  private float storeShortage() {
    if (store == null) return 1;
    return 10 - (store.stocks.amountOf(GREENS) + store.stocks.amountOf(CARBS));
  }
  
  
  private float sumHarvest() {
    return actor.gear.amountOf(CARBS) + actor.gear.amountOf(GREENS);
  }
  
  
  
  public Behaviour getNextStep() {
    final boolean report = I.talkAbout == actor && stepsVerbose;
    if (report) I.say("\nGetting next foraging step for "+actor);
    if (done) return null;

    final float harvest = sumHarvest();
    final boolean gone = ! sourceValid();
    
    if (gone || harvest >= 1) {
      if (store == null) return null;
      
      final Action returns = new Action(
        actor, store,
        this, "actionReturnHarvest",
        Action.REACH_DOWN, "Returning forage"
      );
      return returns;
    }
    
    final float shortage = storeShortage();
    if (shortage <= 0) return null;
    
    if (gone) {
      source = Forestry.findCutting(actor, store, null);
      if (! sourceValid()) return null;
    }

    final Action forage = new Action(
      actor, source,
      this, "actionForage",
      Action.BUILD, "Foraging"
    );
    forage.setMoveTarget(Spacing.nearestOpenTile(source, actor));
    return forage;
  }
  
  
  
  public boolean actionForage(Actor actor, Flora source) {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (source == null || source.destroyed()) {
      this.source = null;
      return false;
    }
    
    final boolean instinct = isBrowsing();
    float labour = 0, skill = 0;
    
    if (instinct) {
      labour = actor.health.healthLevel() * actor.health.baseBulk();
      skill  = Rand.num() * labour;
    }
    else {
      if (actor.skills.test(HARD_LABOUR, ROUTINE_DC, 1.0f)) labour++;
      if (actor.skills.test(CULTIVATION, ROUTINE_DC, 1.0f)) skill++;
      if (actor.skills.test(CULTIVATION, DIFFICULT_DC, 1.0f)) {
        labour++;
        skill++;
      }
    }
    Resting.dineFrom(actor, actor);
    
    if (labour > 0 && skill > 0) {
      //  TODO:  Have flora come into fruiting condition when mature, and only
      //  allow human foraging then.
      
      actor.gear.bumpItem(CARBS , labour         * 0.1f);
      actor.gear.bumpItem(GREENS, skill * labour * 0.1f);
      source.incGrowth(-0.1f * (skill + labour), actor.world(), false);
      return true;
    }
    else {
      source.incGrowth(-0.1f / 2f, actor.world(), false);
      actor.traits.incLevel(Conditions.POISONING, 0.1f);
    }
    
    if (report) I.reportVars("\nPerformed forage at "+source, "  ",
      "Labour", labour,
      "Skill" , skill ,
      "Carbs" , actor.gear.amountOf(CARBS ),
      "Greens", actor.gear.amountOf(GREENS)
    );
    return false;
  }
  
  
  /*
  public boolean actionFell(Actor actor, Flora cut) {
    //
    //  TODO:  Allow for wood-cutting behaviours as well?  ...Yeah.  Maybe.
    return true;
  }
  //*/
  
  
  public boolean actionReturnHarvest(Actor actor, Venue depot) {
    actor.gear.transfer(GREENS, depot);
    actor.gear.transfer(CARBS, depot);
    actor.gear.transfer(Item.withReference(SAMPLES, Flora.WILD_FLORA), depot);
    done = true;
    return true;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    if (super.needsSuffix(d, "Foraging"));
  }
}






