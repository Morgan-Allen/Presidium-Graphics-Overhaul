

package stratos.game.wild;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.user.*;
import stratos.util.*;
import stratos.graphics.sfx.TalkFX;
import stratos.graphics.widgets.Composite;



public class SupplyCache extends Fixture implements Item.Dropped {
  
  
  final static CutoutModel MODEL = CutoutModel.fromImage(
    SupplyCache.class, "media/Items/supply_cache.png", 1.2f, 0.5f
  );
  
  final Inventory stored = new Inventory(this);
  private TalkFX chat = new TalkFX();
  
  
  public SupplyCache() {
    super(1, 1);
    attachModel(MODEL);
  }
  
  
  public SupplyCache(Session s) throws Exception {
    super(s);
    stored.loadState(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    stored.saveState(s);
  }
  
  
  public int pathType() {
    return Tile.PATH_BLOCKS;
  }
  
  
  public int owningType() {
    return Element.VENUE_OWNS;
  }
  
  
  
  /**  Satisfying the Owner interface-
    */
  public Inventory inventory()              { return stored; }
  public boolean   privateProperty()        { return false ; }
  public float     priceFor(Traded service) { return 0     ; }
  public int       spaceFor(Traded good)    { return 10    ; }
  public TalkFX    chat() { return chat; }
  
  
  public void afterTransaction(Item item, float amount) {
    if (stored.empty()) setAsDestroyed();
  }
  
  
  public void onGrowth(Tile t) {
    if (stored.empty()) setAsDestroyed();
  }
  
  
  public boolean enterWorldAt(int x, int y, Stage world) {
    if (! super.enterWorldAt(x, y, world)) return false;
    world.presences.togglePresence(this, origin(), true, Item.Dropped.class);
    return true;
  }
  
  
  public void exitWorld() {
    world.presences.togglePresence(this, origin(), false, Item.Dropped.class);
    super.exitWorld();
  }
  
  
  
  /**  Rendering and interface-
    */
  public void renderFor(Rendering rendering, Base base) {
    super.renderFor(rendering, base);
    this.viewPosition(chat.position);
    chat.readyFor(rendering);
  }
  
  
  public String toString() {
    return fullName();
  }
  

  public String fullName() {
    return "Supply Cache";
  }
  
  
  public String helpInfo() {
    return
      "Often left behind by advance scouts or air-dropped for colonists, "+
      "a supply cache may provide useful supplies to your settlement.";
  }
  
  
  public String objectCategory() {
    return UIConstants.TYPE_TERRAIN;
  }


  public Composite portrait(BaseUI UI) {
    //  TODO:  Fix this.
    return null;//new Composite(UI);
  }
  
  
  public void whenClicked() {
    BaseUI.current().selection.pushSelection(this, false);
  }
  

  public SelectionInfoPane configPanel(SelectionInfoPane panel, BaseUI UI) {
    if (panel == null) panel = new SelectionInfoPane(UI, this, null, true);
    
    final Description d = panel.detail(), l = panel.listing();
    d.append(helpInfo());
    
    l.append("Contains:");
    for (Item i : stored.allItems()) {
      l.append("\n  "+i);
    }
    
    return panel;
  }
  
  
  public TargetOptions configInfo(TargetOptions info, BaseUI UI) {
    if (info == null) info = new TargetOptions(UI, this);
    return info;
  }


  public Target selectionLocksOn() {
    return this;
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    if (destroyed() || ! inWorld()) return;
    
    BaseUI.current().selection.renderPlane(
      rendering, world, position(null), 1,
      Colour.transparency(hovered ?  0.25f : 0.5f),
      Selection.SELECT_CIRCLE,
      true, this
    );
  }
}




