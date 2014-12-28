

package stratos.game.actors;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.politic.Pledge;
import stratos.util.*;
import stratos.game.common.Session.Saveable;
import static stratos.game.actors.Qualities.*;



//  TODO:  This piggybacks off the Relations class, but determines primary
//         'desires'.

//  TODO:  Move greed, ambition, and contentment-evaluation over here.
//  TODO:  Record memories?


public class ActorMotives {
  
  
  private static boolean
    rateVerbose = false;
  
  final static int
    UPDATE_INTERVAL  = Stage.STANDARD_HOUR_LENGTH,
    MOTIVE_EVAL_TIME = Stage.STANDARD_DAY_LENGTH * 5;
  
  
  final Actor actor;
  private float solitude = 0.0f;
  
  
  
  protected ActorMotives(Actor actor) {
    this.actor = actor;
  }
  
  
  public void loadState(Session s) throws Exception {
    solitude = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveFloat(solitude);
  }
  
  
  
  public void updateValues(int numUpdates) {
    if ((numUpdates % UPDATE_INTERVAL) != 0) return;
    final float inc = (1f * UPDATE_INTERVAL) / MOTIVE_EVAL_TIME;
    
    float RS = rateSolitude();
    solitude = (solitude * (1 - inc)) + (RS * inc);
  }
  
  
  private float rateSolitude() {
    //  TODO:  Only count positive relations!
    final float
      trait = 1.25f + actor.traits.relativeLevel(OUTGOING),
      baseF = ActorRelations.BASE_NUM_FRIENDS * trait,
      numF  = actor.relations.relations().size();
    return (baseF - numF) / baseF;
  }
  
  
  
  public float solitude() {
    return solitude;
  }
  
  
  //  TODO:  Merge this with the supply-and-demand system for Holdings?
  
  public static float rateDesire(Item item, Actor buys, Actor receives) {
    final boolean report = rateVerbose && I.talkAbout == buys;
    
    float rating = 0;
    if (Visit.arrayIncludes(Economy.ALL_FOOD_TYPES, item.type)) {
      final float hunger = receives.health.hungerLevel();
      if (hunger > 0.5f) rating += (hunger - 0.5f) * 10;
    }
    
    if (receives.mind.home() instanceof Venue) {
      final Venue home = (Venue) receives.mind.home();
      final float need = home.stocks.shortageOf(item.type);
      if (need > 0) rating += need;
    }
    
    if (item.type == receives.gear.deviceType()) {
      final Item device = receives.gear.deviceEquipped();
      if (item.quality > device.quality) {
        rating += 2 * (item.quality - device.quality);
      }
    }
    
    if (item.type == receives.gear.outfitType()) {
      final Item outfit = receives.gear.outfitEquipped();
      if (item.quality > outfit.quality) {
        rating += 2 * (item.quality - outfit.quality);
      }
    }
    
    final float pricedAt = item.defaultPrice();
    rating += Pledge.greedPriority(receives, pricedAt);
    if (report) I.say("  Rating for "+item+" is: "+rating);
    
    if (buys != null) {
      rating /= 1 + (Pledge.greedPriority(buys, pricedAt));
      if (report) I.say("    After pricing? "+rating);
    }
    return rating;
  }
}
