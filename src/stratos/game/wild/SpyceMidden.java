/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;



public class SpyceMidden extends SupplyCache {
  
  
  /**  Construction and save/load methods-
    */
  
  
  protected SpyceMidden() {
    super();
    final float spiceAmount = 1 + (Rand.num() * 2);
    this.stored.addItem(Item.withAmount(SPYCES, spiceAmount));
    attachSprite(Lictovore.MODEL_MIDDENS[Rand.index(3)].makeSprite());
  }
  
  
  public SpyceMidden(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Registration, life cycle and physical properties-
    */
  public boolean enterWorldAt(int x, int y, Stage world, boolean intact) {
    if (! super.enterWorldAt(x, y, world, intact)) return false;
    world.presences.togglePresence(this, origin(), true, SpyceMidden.class);
    return true;
  }
  
  
  public void exitWorld() {
    world.presences.togglePresence(this, origin(), false, SpyceMidden.class);
    super.exitWorld();
  }
  
  
  public int pathType() {
    return Tile.PATH_HINDERS;
  }
  
  
  public void onGrowth(Tile t) {
    stored.removeItem(Item.withAmount(SPYCES, Rand.num() / 2));
    if (stored.amountOf(SPYCES) <= 0) setAsDestroyed();
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Spice Midden";
  }
  
  
  public String helpInfo() {
    return
      "Spice ingestion becomes concentrated in the upper echelons of the "+
      "food chain, and is often used as a territorial marker by top "+
      "predators.";
  }
  
  
  public Composite portrait(BaseUI UI) {
    //  TODO:  Fix this.
    return null;//new Composite(UI);
  }
}








