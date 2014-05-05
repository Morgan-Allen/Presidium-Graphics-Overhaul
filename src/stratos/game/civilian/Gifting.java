

package stratos.game.civilian;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.base.*;
import stratos.util.*;



//  TODO:  Consider having this extend Dialogue.  The other guy needs to
//  actively receive the gift, not just stand there like a dumbass.  (Or just
//  apply the action?  Yeah.  It's only one action.  Just check that they're
//  willing.)


public class Gifting extends Plan implements Qualities {
  
  
  final static int
    STAGE_INIT  = -1,
    STAGE_GETS  =  0,
    STAGE_GIVES =  1,
    STAGE_DONE  =  2;
  
  private static boolean
    evalVerbose = false,
    rateVerbose = false,
    stepVerbose = true ;
  
  
  final Item gift;
  final Actor receives;
  final Plan getting;
  private int stage = STAGE_INIT;
  
  
  
  public Gifting(Actor actor, Actor receives, Item gift, Plan getting) {
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
    final boolean report = evalVerbose && I.talkAbout == actor;
    
    //  TODO:  Include pricing check!
    float modifier = NO_MODIFIER;
    final float
      novelty = receives.memories.relationNovelty(actor),
      rating = rateGift(gift, actor, receives);
    if (! hasBegun()) {
      modifier -= ROUTINE;
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
      I.say("  Novelty/gift-rating: "+novelty+"/"+rating);
      I.say("  Final priority: "+priority);
    }
    return priority;
  }
  
  
  protected Behaviour getNextStep() {
    
    //  If the recipient has accepted the item, your job is complete.  (If it's
    //  refused, abort the behaviour.)
    if (this.stage == STAGE_DONE) return null;
    
    //  If you've acquired the item, present it to the recipient, and await
    //  their response.
    if (actor.gear.hasItem(gift)) {
      this.stage = STAGE_GIVES;
      final Action offer = new Action(
        actor, receives,
        this, "actionOffer",
        Action.REACH_DOWN, "Giving "
      );
      offer.setProperties(Action.RANGED | Action.NO_LOOP);
      return offer;
    }
    
    //  If the 'get' action hasn't completed, carry it out.
    if (! getting.finished()) {
      this.stage = STAGE_GETS;
      return getting;
    }
    
    return null;
  }
  
  
  private Action receiptAction(
    Actor receives, Actor from, float value, float success
  ) {
    final float priority = value * success * PARAMOUNT;
    if (priority <= 0) return null;
    
    final Action receipt = new Action(
      receives, from,
      this, "actionReceives",
      Action.TALK_LONG, "Receiving a gift of "+gift
    );
    receipt.setPriority(priority);
    if (receives.mind.mustIgnore(receipt)) return null;
    
    return receipt;
  }
  
  
  public boolean actionOffer(Actor actor, Actor receives) {
    if (receives.isDoingAction("actionReceives", actor)) return true;
    Dialogue.utters(actor, "I have a gift for you...", 0);
    
    //  TODO:  Modify DC by the greed and honour of the subject.
    final float value = rateGift(gift, null, receives) / 10f;
    float acceptDC = (1 - value) * ROUTINE_DC;
    float success = Dialogue.talkResult(SUASION, acceptDC, actor, receives);
    
    final Action receipt = receiptAction(receives, actor, value, success);
    if (receipt == null) {
      Dialogue.utters(receives, "I can't accept that.", -1);
      abortBehaviour();
      return false;
    }
    else receives.mind.assignBehaviour(receipt);
    return true;
  }
  
  
  public boolean actionReceives(Actor receives, Actor from) {
    final float value = rateGift(gift, null, receives);
    receives.memories.incRelation(actor, value / 2, 0.5f);
    from.gear.transfer(gift, receives);
    Dialogue.utters(receives, "Thank you for the "+gift.type+"!", value);
    this.stage = STAGE_DONE;
    return true;
  }
  
  
  
  /**  Returns the next suitable gift between the given actors-
    */
  public static Gifting nextGiftFor(Actor buys, Actor receives) {
    final boolean report = rateVerbose && I.talkAbout == buys;
    if (report) I.say("\nGetting next gift from "+buys+" for "+receives);
    
    if (buys.mind.hasToDo(Gifting.class)) return null;
    final Dialogue d = new Dialogue(buys, receives, Dialogue.TYPE_CASUAL);
    if (d.priorityFor(buys) <= 0) return null;
    
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
    
    if (gift != null && getting == null) {
      final World world = buys.world();
      final Batch <Inventory.Owner> origins = new Batch <Inventory.Owner> ();
      world.presences.sampleFromMaps(buys, world, 5, origins, gift.type);
      origins.include(buys);
      origins.include(buys.mind.home());
      
      if (report) {
        I.say("\n  Potential vendors for "+gift+" are:");
        for (Object o : origins) I.say("    "+o);
      }
      getting = Deliveries.bestOrigin(
        buys, new Service[] { gift.type }, receives,
        origins, 1, Delivery.TYPE_SHOPS
      );
    }

    if (report) {
      I.say("\n  Gift is: "+gift);
      I.say("  Acquired by: "+getting);
    }
    if (gift == null || getting == null) return null;
    return new Gifting(buys, receives, gift, getting);
  }
  
  
  
  /**  Return a rating for a given gift between 0 and 10.
    */
  static float rateGift(Item item, Actor buys, Actor receives) {
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
    rating += Plan.greedLevel(receives, pricedAt / Relation.NOVELTY_DAYS);
    if (report) I.say("  Rating for "+item+" is: "+rating);
    
    if (buys != null) {
      rating /= 1 + (Plan.greedLevel(buys, pricedAt) * ROUTINE);
      if (report) I.say("    After pricing? "+rating);
    }
    return rating;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    d.append("Getting gift of "+gift+" for ");
    d.append(receives);
  }
}




