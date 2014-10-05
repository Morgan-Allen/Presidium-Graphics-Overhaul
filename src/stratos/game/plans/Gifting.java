

package stratos.game.plans;
import stratos.game.civilian.Pledge;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.base.*;
import stratos.util.*;



//  TODO:  There's a problem with the 'greeting' phase here if the other actor
//  isn't disposed to talk (e.g, is hostile, busy or asleep, etc.)


public class Gifting extends Plan implements Qualities {
  
  
  final static int
    STAGE_INIT  = -1,
    STAGE_GETS  =  0,
    STAGE_GIVES =  1,
    STAGE_DONE  =  2;
  
  private static boolean
    evalVerbose   = false,
    rateVerbose   = false,
    eventsVerbose = true;
  
  
  final Item gift;
  final Actor receives;
  final Plan getting;
  final Dialogue giving;
  private int stage = STAGE_INIT;
  
  
  
  public Gifting(Actor actor, Actor receives, Item gift, Plan getting) {
    super(actor, receives, true);
    this.gift = gift;
    this.receives = receives;
    this.getting = getting;
    this.giving = new Dialogue(actor, receives, Dialogue.TYPE_CONTACT);
  }
  
  
  public Gifting(Session s) throws Exception {
    super(s);
    gift = Item.loadFrom(s);
    receives = (Actor) s.loadObject();
    getting = (Plan) s.loadObject();
    stage = s.loadInt();
    giving = (Dialogue) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    Item.saveTo(s, gift);
    s.saveObject(receives);
    s.saveObject(getting);
    s.saveInt(stage);
    s.saveObject(giving);
  }
  
  
  public Plan copyFor(Actor other) {
    return null;
  }
  
  
  
  
  /**  Priority evaluation-
    */
  final static Trait BASE_TRAITS[] = { EMPATHIC, GENEROUS, EXCITABLE };
  
  
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if ((! getting.finished()) && getting.priorityFor(actor) < 0) return 0;
    
    float modifier = NO_MODIFIER;
    final float
      novelty = receives.relations.relationNovelty(actor),
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
  
  
  public boolean finished() {
    if (! getting.finished()) return false;
    return super.finished();
  }
  
  
  public boolean valid() {
    if (! super.valid()) return false;
    return getting.finished() || getting.valid();
  }
  
  
  protected Behaviour getNextStep() {
    final boolean report = eventsVerbose && I.talkAbout == actor;
    if (report) I.say("\nGetting next gifting step:");
    
    //  If the recipient has accepted the item, your job is complete.  (If it's
    //  refused, abort the behaviour.)
    if (this.stage == STAGE_DONE) {
      if (report) I.say("  Gifting complete!");
      return null;
    }
    
    //  If you've acquired the item, present it to the recipient, and await
    //  their response.
    if (actor.gear.hasItem(gift)) {
      if (giving.finished()) return null;
      this.stage = STAGE_GIVES;
      giving.setMotiveFrom(this, 0);
      giving.attachGift(gift);
      if (report) {
        I.say("  Entering dialogue mode for gift-giving.");
        I.say("  Priority: "+giving.priorityFor(actor));
      }
      return giving;
    }
    
    //  If the 'get' action hasn't completed, carry it out.
    if (! getting.finished()) {
      this.stage = STAGE_GETS;
      final Behaviour nextS = getting.nextStepFor(actor);
      if (report) I.say("  Still getting, next step: "+nextS);
      if (nextS == null) return null;
      return getting;
    }
    
    if (report) I.say("  No next step!");
    return null;
  }
  
  
  
  /**  Returns the next suitable gift between the given actors-
    */
  public static Gifting nextGifting(Plan parent, Actor buys, Actor receives) {
    final boolean report = rateVerbose && I.talkAbout == buys;
    if (report) I.say("\nGetting next gift from "+buys+" for "+receives);
    
    if (buys.mind.hasToDo(Gifting.class)) return null;
    final Dialogue d = new Dialogue(buys, receives, Dialogue.TYPE_CASUAL);
    if (parent != null) d.setMotiveFrom(parent, 0);
    if (d.priorityFor(buys) <= 0) return null;
    
    float rating, bestRating = 0;
    Item gift = null;
    Plan getting = null;
    
    for (TradeType f : Economy.ALL_FOOD_TYPES) {
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
      //origins.include(buys.mind.home());
      //  TODO:  Don't offer things the actor already has!
      
      if (report) {
        I.say("\n  Potential vendors for "+gift+" are:");
        for (Object o : origins) I.say("    "+o);
      }
      
      getting = DeliveryUtils.bestCollectionFor(
        buys, gift.type, 1, buys, 5, false
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
    
    final float pricedAt = item.defaultPrice() / Relation.NOVELTY_DAYS;
    rating += Pledge.greedLevel(receives, pricedAt) * ROUTINE;
    if (report) I.say("  Rating for "+item+" is: "+rating);
    
    if (buys != null) {
      rating /= 1 + (Pledge.greedLevel(buys, pricedAt) * ROUTINE);
      if (report) I.say("    After pricing? "+rating);
    }
    return rating;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (super.needsSuffix(d, "Getting gift for ")) {
      d.append(receives);
    }
  }
}




