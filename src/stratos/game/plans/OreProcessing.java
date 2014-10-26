/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.plans;
import stratos.game.common.*;
import stratos.game.base.*;
import stratos.game.building.*;
import stratos.game.maps.*;
import stratos.game.actors.*;
import stratos.game.wild.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.building.Economy.*;



//  TODO:  Consider doing this directly at the excavation site instead.


public class OreProcessing extends Plan {
  
  
  
  /**  Fields, constructors and save/load methods-
    */
  final static int BASE_AMOUNT = Smelter.SMELT_AMOUNT;
  final Smelter venue;
  final TradeType output;
  final Item sample, tailing;
  
  
  OreProcessing(Actor actor, Smelter smelter, TradeType output) {
    super(actor, smelter, true);
    this.venue = smelter;
    this.output = output;
    sample = Item.withReference(SAMPLES, output);
    tailing = Item.withReference(SAMPLES, venue);
  }
  
  
  public OreProcessing(Session s) throws Exception {
    super(s);
    venue = (Smelter) s.loadObject();
    output = (TradeType) s.loadObject();
    sample = Item.withReference(SAMPLES, output);
    tailing = Item.withReference(SAMPLES, venue);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(venue);
    s.saveObject(output);
  }
  
  
  public Plan copyFor(Actor other) {
    return new OreProcessing(other, venue, output);
  }
  
  
  
  /**  Static location methods and priority evaluation-
    */
  protected float getPriority() {
    final float bonus = venue.stocks.amountOf(sample) / 5f;
    return Visit.clamp(CASUAL + bonus + 1, 0, URGENT);
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    final Item match = Item.asMatch(SAMPLES, output);
    final float
      rawAmount = venue.stocks.amountOf(match),
      newAmount = venue.stocks.amountOf(output);
    //final Tailing dumpsAt = venue.parent.nextTailing();
    //if (dumpsAt == null) return null;
    
    final Venue parent = ((Smelter) venue).belongs();
    if ((rawAmount == 0 && newAmount > 0) || newAmount >= BASE_AMOUNT) {
      final Delivery d = new Delivery(output, venue, parent);
      if (Plan.competition(d, parent, actor) == 0) return d;
    }
    
    if (parent.stocks.amountOf(match) > 0) {
      final Item carried = Item.with(SAMPLES, output, BASE_AMOUNT, 0);
      final Delivery d = new Delivery(carried, parent, venue);
      if (Plan.competition(d, parent, actor) == 0) return d;
    }
    
    if (rawAmount + newAmount >= BASE_AMOUNT) {
      final Action smelt = new Action(
        actor, venue,
        this, "actionSmelt",
        Action.REACH_DOWN, "Smelting "+output.name
      );
      return smelt;
    }
    return null;
  }
  
  
  public boolean actionSmelt(Actor actor, Smelter smelter) {
    int success = 0;
    if (actor.skills.test(HARD_LABOUR, 10, 1)) success++;
    if (! actor.skills.test(CHEMISTRY, 5, 1)) success--;
    if (actor.skills.test(GEOPHYSICS, 15, 1)) success++;
    if (success <= 0) return false;
    
    final Item sample = Item.withReference(SAMPLES, output);
    final float
      sampleAmount = smelter.stocks.amountOf(sample),
      outputAmount = smelter.stocks.amountOf(output),
      smeltLimit = Math.min(sampleAmount, Smelter.SMELT_AMOUNT - outputAmount);
    if (smeltLimit > 0) {
      final float bump = Math.min(smeltLimit, 0.1f * success);
      smelter.stocks.removeItem(Item.withAmount(sample, bump));
      smelter.stocks.bumpItem(output, bump);
      smelter.stocks.addItem(Item.withAmount(tailing, bump));
    }
    return true;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (super.needsSuffix(d, "Processing ore at ")) {
      d.append(venue);
    }
  }
}





/*
if (actor.gear.amountOf(tailing) > 0) {
  final Action dump = new Action(
    actor, dumpsAt,
    this, "actionDumpTailings",
    Action.REACH_DOWN, "Dumping tailings"
  );
  dump.setMoveTarget(Spacing.nearestOpenTile(dumpsAt, actor));
  return dump;
}
if (venue.stocks.amountOf(tailing) >= BASE_AMOUNT) {
  final Action collect = new Action(
    actor, venue,
    this, "actionCollectTailings",
    Action.REACH_DOWN, "Collecting tailings"
  );
  //collect.setPriority(Action.ROUTINE);
  return collect;
}
//*/


//  TODO:  RESTORE/ADD THIS, -AND TEST
/*
if (output == ARTIFACTS) {
  //  TODO:  Either deliver to the nearest Artificer for closer inspection,
  //  or attempt re-assembly here.
  
  //  TODO:  Deliver artifacts to where they can either be sold or re-used.
}
else {
//*/
  
//}



/*
public boolean actionCollectTailings(Actor actor, Smelter smelter) {
  for (Item match : smelter.stocks.matches(tailing)) {
    I.say("  MATCH IS: "+match);
    smelter.stocks.transfer(match, actor);
  }
  return true;
}


public boolean actionDumpTailings(Actor actor, Tailing dump) {
  I.say("DUMPING TAILINGS");
  
  if (! dump.inWorld()) {
    if (! dump.canPlace()) {
      I.say("CANNOT PLACE DUMP!");
      return false;
    }
    else dump.enterWorld();
  }
  final float amount = actor.gear.amountOf(tailing);
  dump.incFill(amount);
  actor.gear.removeItem(actor.gear.matchFor(tailing));
  return true;
}
//*/

/*
public boolean actionReassemble(Actor actor, Venue site) {
  //  TODO:  Test and restore
  /*
  final Item sample = Item.withReference(SAMPLES, ARTIFACTS);
  final Structure s = site.structure();
  final float
    AAU = s.upgradeLevel(ExcavationSite.ARTIFACT_ASSEMBLY),
    SPU = s.upgradeLevel(ExcavationSite.SAFETY_PROTOCOL);
  
  float success = 1;
  if (actor.skills.test(ASSEMBLY, 10, 1)) success++;
  else success--;
  if (actor.skills.test(ANCIENT_LORE, 5, 1)) success++;
  else success--;

  site.stocks.removeItem(Item.withAmount(sample, 1.0f));
  if (success >= 0) {
    success *= 1 + (AAU / 2f);
    final Item result = Item.with(ARTIFACTS, null, 0.1f, success * 2);
    site.stocks.addItem(result);
  }
  if (site.stocks.amountOf(ARTIFACTS) >= 10) {
    site.stocks.removeItem(Item.withAmount(ARTIFACTS, 10));
    final Item match = site.stocks.matchFor(Item.withAmount(ARTIFACTS, 1));
    final float quality = (match.quality + AAU + 2) / 2f;
    if (Rand.num() < 0.1f * match.quality / (1 + SPU)) {
      final boolean hostile = Rand.num() < 0.9f / (1 + SPU);
      releaseArtilect(actor, hostile, quality);
    }
    else createArtifact(site, quality);
  }
  //*/
/*
  return true;
}


private void releaseArtilect(Actor actor, boolean hostile, float quality) {
  //  TODO:  TEST AND RESTORE THIS
  /*
  final int roll = (int) (Rand.index(5) + quality);
  final Artilect released = roll >= 5 ? new Tripod() : new Drone();
  
  final World world = actor.world();
  if (hostile) {
    released.assignBase(world.baseWithName(Base.KEY_ARTILECTS, true, true));
  }
  else {
    released.assignBase(actor.base());
    released.mind.assignMaster(actor);
  }
  released.enterWorldAt(actor.aboard(), world);
  released.goAboard(actor.aboard(), world);
  //*/
/*
}


private void createArtifact(Venue site, float quality) {
  final TradeType basis = Rand.yes() ?
    (TradeType) Rand.pickFrom(ALL_DEVICES) :
    (TradeType) Rand.pickFrom(ALL_OUTFITS);
  //
  //  TODO:  Deliver to artificer for sale or recycling!
  final Item found = Item.with(ARTIFACTS, basis, 1, quality);
  site.stocks.addItem(found);
}
//*/