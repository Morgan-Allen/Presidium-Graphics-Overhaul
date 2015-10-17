/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.civic;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.economic.Economy.*;
import static stratos.content.abilities.RunnerTechniques.*;



public class RunnerMarket extends Venue {
  
  
  /**  Setup and constructors-
    */
  final static String IMG_DIR = "media/Buildings/merchant/";
  final static ImageAsset ICON = ImageAsset.fromImage(
    BotanicalStation.class, "media/GUI/Buttons/runner_market_button.gif"
  );
  final static ModelAsset MODEL = CutoutModel.fromImage(
    RunnerMarket.class, IMG_DIR+"runner_market.png", 4, 1
  );
  
  final static int
    GANG_NONE       = -1,
    GANG_SILVERFISH =  0,
    GANG_IV_PUNKS   =  1,
    GANG_HUDZENA    =  2;
  final static int
    CLAIM_SIZE = 8;
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    RunnerMarket.class, "runner_market",
    "Runner Market", Target.TYPE_COMMERCE, ICON,
    "Runner Markets can offer black market technology and other "+
    "clandestine services to settlements willing to overlook their "+
    "criminal connections.",
    4, 1, Structure.IS_NORMAL,
    Owner.TIER_FACILITY, 150,
    3, SERVICE_SECURITY, RUNNER, FIXER
  );
  
  
  public RunnerMarket(Base base) {
    super(BLUEPRINT, base);
    staff.setShiftType(SHIFTS_BY_HOURS);
    attachModel(MODEL);
  }
  
  
  public RunnerMarket(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Economic and behavioural overrides-
    */
  //  TODO:  Include corresponding upgrades and techniques for all of these!
  final static Upgrade
    LEVELS[] = BLUEPRINT.createVenueLevels(
      Upgrade.TWO_LEVELS, StockExchange.LEVELS[0],
      new Object[] { 5, STEALTH_AND_COVER, 5, ACCOUNTING },
      350,
      650
    );
  
  
  public Behaviour jobFor(Actor actor) {
    
    //  TODO:  It seems a little odd that runners would be working strictly 'on
    //  the clock'.  Fudge this a little..?
    if (staff.offDuty(actor)) return null;
    final Background job = actor.mind.vocation();
    final Choice choice = new Choice(actor);
    
    if (job == RUNNER) {
      //
      //  Either collect protection money from nearby businesses, or loot from
      //  more distant properties:
      final Box2D territory = areaClaimed();
      final Batch <Venue> venues = new Batch <Venue> ();
      world.presences.sampleFromMaps(this, world, 5, venues, Venue.class);
      for (Venue venue : venues) {
        if (territory.contains(venue.position(null))) {
          choice.add(Audit.nextExtortionAudit(actor, venue));
        }
        else {
          choice.add(new Looting(actor, venue, null, this));
        }
      }
      //
      //  You also need to perform enforcement duties in the neighbourhood:
      choice.add(Arrest.nextOfficialArrest(this, actor));
      //
      //  Next, consider smuggling goods out of the settlement-
      for (Dropship ship : world.offworld.journeys.allDropships()) {
        if (! ship.landed()) continue;
        final Smuggling s = Smuggling.bestSmugglingFor(this, ship, actor, 5);
        if (s != null && staff.assignedTo(s) == 0) choice.add(s);
      }
    }
    //
    //  And lastly, consider manufacturing contraband from scratch:
    if (job == FIXER) {
      for (Item ordered : stocks.specialOrders()) {
        final Manufacture mO = new Manufacture(actor, this, ordered);
        final Upgrade forType = upgradeFor(ordered.type);
        choice.add(mO.setBonusFrom(this, true, forType));
      }
    }
    return choice.weightedPick();
  }
  
  
  public Box2D areaClaimed() {
    return new Box2D(footprint()).expandBy(CLAIM_SIZE);
  }
  
  
  public void addServices(Choice choice, Actor client) {
    if (I.talkAbout == client) {
      ///choice.isVerbose = true;
    }
    
    final Traded services[] = { SLOW_BURN_ITEM };
    for (Traded t : services) {
      final Upgrade limit = upgradeFor(t);
      final Item gets = GearPurchase.nextGearToPurchase(client, this, t);
      choice.add(GearPurchase.nextCommission(client, this, gets, limit));
    }
    choice.add(BringUtils.nextPersonalPurchase(client, this));
  }
  
  
  private Upgrade upgradeFor(Traded type) {
    if (type == SLOW_BURN_ITEM) {
      return null;
    }
    return null;
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    
    for (Conversion c : BLUEPRINT.production()) {
      final Upgrade u = upgradeFor(c.out.type);
      if (u != null && structure.upgradeLevel(u) <= 0) continue;
      else stocks.translateRawDemands(c, 1);
    }
  }
  
  
  public int numPositions(Background b) {
    final int level = structure.mainUpgradeLevel();
    if (b == FIXER ) return level;
    if (b == RUNNER) return level * 2;
    return 0;
  }
}











