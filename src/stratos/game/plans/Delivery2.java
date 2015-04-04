

package stratos.game.plans;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.economic.*;
import stratos.util.*;



//  TODO:  There must be some way to automatically inject these standardised
//  save/load methods.


public class Delivery2 extends Plan {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final static int
    STAGE_INIT    = -1,
    STAGE_PICKUP  =  0,
    STAGE_DELIVER =  1,
    STAGE_RETURN  =  2,
    STAGE_DONE    =  3;
  
  
  int stage = STAGE_INIT;
  private Item items[];
  final Owner buyer, seller;
  private float credsFromBuyer, credsToSeller, goodsValue, goodsBulk;
  
  
  public Delivery2(Item items[], Owner seller, Owner buyer) {
    super(null, seller, MOTIVE_JOB, NO_HARM);
    this.items  = items ;
    this.seller = seller;
    this.buyer  = buyer ;
  }
  
  
  public Delivery2(Session s) throws Exception {
    super(s);
    stage  = s.loadInt();
    items  = Item.loadItemsFrom(s);
    buyer  = (Owner) s.loadObject();
    seller = (Owner) s.loadObject();
    credsFromBuyer = s.loadFloat();
    credsToSeller  = s.loadFloat();
    goodsValue     = s.loadFloat();
    goodsBulk      = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt(stage);
    Item.saveItemsTo(s, items);
    s.saveObject(buyer );
    s.saveObject(seller);
    s.saveFloat(credsFromBuyer);
    s.saveFloat(credsToSeller );
    s.saveFloat(goodsValue    );
    s.saveFloat(goodsBulk     );
  }
  
  
  public Plan copyFor(Actor other) { return null; }
  
  
  
  /**  Debugging, feedback and interface-
    */
  public void describeBehaviour(Description d) {
    // TODO Auto-generated method stub
  }
  
  
  
  /**  Priority and target-evaluation:
    */
  protected float getPriority() {
    return ROUTINE;
  }
  
  
  
  /**  Step evaluation-
    */
  protected Behaviour getNextStep() {
    //
    //  Rather than specifying an exact order
    Owner settles = null, carries = null;
    final Batch <Action> mustDo = new Batch <Action> ();
    
    if (stage <= STAGE_PICKUP ) carries = seller;
    if (stage == STAGE_DELIVER) carries = actor ;
    final boolean hasItems = hasItemsFrom(carries);
    
    if (credsFromBuyer != goodsValue) settles = buyer ;
    if (credsToSeller  != goodsValue) settles = seller;
    if (settles != null) mustDo.add(settleAccounts(actor, settles));
    
    if (hasItems) {
      if (stage <= STAGE_PICKUP ) mustDo.add(pickup (actor, seller));
      if (stage == STAGE_DELIVER) mustDo.add(deliver(actor, buyer ));
    }
    
    final Pick <Action> pick = new Pick <Action> ();
    for (Action a : mustDo) {
      float rating = 1f / (1 + Spacing.distance(actor, a.movesTo()));
      pick.compare(a, rating);
    }
    return pick.result();
  }
  
  
  private boolean hasItemsFrom(Owner carries) {
    final Batch <Item> has = new Batch <Item> ();
    goodsValue = goodsBulk = 0;
    final Inventory c = carries.inventory();
    
    for (Item i : items) {
      final float amount = Nums.min(i.amount, c.amountOf(i));
      if (amount <= 0) continue;
      has.add(i = Item.withAmount(i, amount));
      goodsValue += i.priceAt(seller);
      goodsBulk  += i.amount;
    }
    if (has.size() == 0) return false;
    this.items = has.toArray(Item.class);
    return true;
  }
  
  
  public Action settleAccounts(Actor actor, Owner point) {
    final Action settle = new Action(
      actor, point,
      this, "actionSettleAccounts",
      Action.TALK_LONG, "Settling accounts"
    );
    return settle;
  }
  
  
  public boolean actionSettleAccounts(Actor actor, Owner point) {
    if (point == seller) {
      final float diff = goodsValue - credsToSeller;
      credsToSeller += diff;
      seller.inventory().incCredits(diff);
    }
    if (point == buyer) {
      final float diff = goodsValue - credsFromBuyer;
      credsFromBuyer += diff;
      buyer.inventory().incCredits(0 - diff);
    }
    return true;
  }
  
  
  public Action pickup(Actor actor, Owner point) {
    final Action pickup = new Action(
      actor, point,
      this, "actionPickup",
      Action.REACH_DOWN, "Performing pickup"
    );
    return pickup;
  }
  
  
  public boolean actionPickup(Actor actor, Owner point) {
    for (Item i : items) {
      point.inventory().transfer(i, actor);
    }
    this.stage = STAGE_DELIVER;
    return true;
  }
  
  
  public Action deliver(Actor actor, Owner point) {
    final Action deliver = new Action(
      actor, point,
      this, "actionDeliver",
      Action.REACH_DOWN, "Performing delivery"
    );
    return deliver;
  }
  
  
  public boolean actionDeliver(Actor actor, Owner point) {
    for (Item i : items) {
      actor.inventory().transfer(i, point);
    }
    this.stage = STAGE_RETURN;
    return true;
  }
}









