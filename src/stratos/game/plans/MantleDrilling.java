/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.plans;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.base.*;
import stratos.game.maps.*;
import stratos.game.actors.*;
import stratos.game.wild.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;




//  TODO:  Heavily rework and re-implement this once the game is more finished


public class MantleDrilling extends Plan {
  
  
  
  /**  Fields, constructors and save/load methods-
    */
  final static int BASE_AMOUNT = Smelter.SMELT_AMOUNT;
  final Smelter venue;
  final Traded output;
  final Item sample, tailing;
  
  
  MantleDrilling(Actor actor, Smelter smelter, Traded output) {
    super(actor, smelter, true, NO_HARM);
    this.venue = smelter;
    this.output = output;
    sample = Item.withReference(SAMPLES, output);
    tailing = Item.withReference(SAMPLES, venue);
  }
  
  
  public MantleDrilling(Session s) throws Exception {
    super(s);
    venue = (Smelter) s.loadObject();
    output = (Traded) s.loadObject();
    sample = Item.withReference(SAMPLES, output);
    tailing = Item.withReference(SAMPLES, venue);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(venue);
    s.saveObject(output);
  }
  
  
  public Plan copyFor(Actor other) {
    return new MantleDrilling(other, venue, output);
  }
  
  
  
  /**  Static location methods and priority evaluation-
    */
  protected float getPriority() {
    final float bonus = venue.stocks.amountOf(sample) / 5f;
    return Nums.clamp(CASUAL + bonus + 1, 0, URGENT);
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
      smeltLimit = Nums.min(sampleAmount, Smelter.SMELT_AMOUNT - outputAmount);
    if (smeltLimit > 0) {
      final float bump = Nums.min(smeltLimit, 0.1f * success);
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


