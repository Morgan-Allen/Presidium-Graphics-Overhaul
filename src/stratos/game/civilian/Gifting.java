

package stratos.game.civilian;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.base.*;
import stratos.util.*;



public class Gifting extends Plan implements Qualities {
  
  
  final static int 
    STAGE_INIT  = -1,
    STAGE_GETS  =  0,
    STAGE_GIVES =  1,
    STAGE_DONE  =  2;
  
  private static boolean verbose = true;
  
  
  final Item gift;
  final Actor receives;
  final Plan getting;
  private int stage = STAGE_INIT;
  
  
  
  protected Gifting(Actor actor, Actor receives, Item gift, Plan getting) {
    super(actor, receives);
    this.gift = gift;
    this.receives = receives;
    this.getting = getting;
  }
  
  
  public Gifting(Session s) throws Exception {
    super(s);
    gift = Item.loadFrom(s);
    receives = (Actor) s.loadObject();
    getting = (Plan) s.loadObject();
    stage = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    Item.saveTo(s, gift);
    s.saveObject(receives);
    s.saveObject(getting);
    s.saveInt(stage);
  }
  
  
  final static Trait BASE_TRAITS[] = { EMPATHIC, GENEROUS, EXCITABLE };
  
  
  protected float getPriority() {
    final boolean report = verbose && I.talkAbout == actor;

    float modifier = NO_MODIFIER - ROUTINE;
    final float
      novelty = receives.memories.relationNovelty(actor),
      rating = rateGift(gift, actor, receives);
    if (! hasBegun()) {
      modifier += (novelty + rating) * ROUTINE;
    }
    
    final float priority = priorityForActorWith(
      actor, receives, CASUAL,
      MILD_HELP, NO_COMPETITION,
      NO_SKILLS, BASE_TRAITS,
      modifier, NORMAL_DISTANCE_CHECK, NO_FAIL_RISK,
      report
    );
    if (report) {
      I.say("  Novelty /gift rating: "+novelty+"/"+rating);
      I.say("  Final priority: "+priority);
    }
    return priority;
  }
  
  
  protected Behaviour getNextStep() {
    //  TODO:  This needs to be coupled with the Dialogue class in order to
    //  work.  Think about that.
    return getting;
  }
  
  
  public void describeBehaviour(Description d) {
    
  }
  
  
  
  public static Gifting nextGiftFor(Actor buys, Actor receives) {
    if (buys.mind.hasToDo(Gifting.class)) return null;
    
    float rating, bestRating = 0;
    Item gift = null;
    Plan getting = null;
    
    for (Service f : Economy.ALL_FOOD_TYPES) {
      final Item food = Item.withAmount(f, 1);
      rating = rateGift(food, buys, receives);
      if (rating > bestRating) { bestRating = rating; gift = food; }
    }
    
    if (receives.mind.home() instanceof Venue) {
      final Venue home = (Venue) receives.mind.home();
      for (Item needs : home.stocks.shortages()) {
        rating = rateGift(needs, buys, receives);
        if (rating > bestRating) { bestRating = rating; gift = needs; }
      }
    }
    
    final Commission
      forDevice = Commission.forItem(buys, receives.gear.deviceEquipped()),
      forOutfit = Commission.forItem(buys, receives.gear.outfitEquipped());
    
    if (forDevice != null) {
      rating = rateGift(forDevice.item, buys, receives);
      if (rating > bestRating) {
        bestRating = rating;
        gift = forDevice.item;
        getting = forDevice;
      }
    }
    if (forOutfit != null) {
      rating = rateGift(forOutfit.item, buys, receives);
      if (rating > bestRating) {
        bestRating = rating;
        gift = forOutfit.item;
        getting = forOutfit;
      }
    }
    if (getting == null) getting = Deliveries.nextCollectionFor(
      buys, buys, new Service[] {gift.type}, 1, buys, buys.world()
    );
    
    if (gift == null || getting == null) return null;
    return new Gifting(buys, receives, gift, getting);
  }
  
  
  
  static float rateGift(Item item, Actor buys, Actor receives) {
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
    
    final float pricedAt = rating * item.amount * item.type.basePrice;
    rating = Plan.greedLevel(receives, pricedAt);
    if (buys != null) rating -= Plan.greedLevel(buys, pricedAt);
    return rating;
  }
}




