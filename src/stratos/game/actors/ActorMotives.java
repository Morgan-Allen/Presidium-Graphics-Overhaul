

package stratos.game.actors;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.tactical.Pledge;
import stratos.util.*;
import stratos.game.common.Session.Saveable;



//  TODO:  This piggybacks off the Relations class, but determines primary
//         'desires'.


public class ActorMotives {
  
  
  private static boolean
    rateVerbose = false;
  
  
  final Actor actor;
  //  TODO:  Include overall contentment-assessment here.
  //  TODO:  Move ambition-evaluation here?
  //  TODO:  Record memories?
  
  
  
  protected ActorMotives(Actor actor) {
    this.actor = actor;
  }
  
  
  
  public void updateDesires() {
    
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
