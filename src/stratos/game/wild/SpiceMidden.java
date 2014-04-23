


package stratos.game.wild ;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;


//
//  TODO:  Try refurbishing this into a more general 'item-drop' class, where
//  you supply the content, sprite and decay rate externally.
//
//  ...What about the flavour text and title?  Or just have it extend the
//  ItemDrop class?


public class SpiceMidden extends Fixture implements Selectable {
  
  
  /**  Construction and save/load methods-
    */
  private float spiceAmount ;
  
  
  protected SpiceMidden() {
    super(1, 1) ;
    spiceAmount = 1 + (Rand.num() * 2) ;
    attachSprite(Species.MODEL_MIDDENS[Rand.index(3)].makeSprite()) ;
  }
  
  
  public SpiceMidden(Session s) throws Exception {
    super(s) ;
    spiceAmount = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveFloat(spiceAmount) ;
  }
  
  
  
  /**  Registration, life cycle and physical properties-
    */
  public boolean enterWorldAt(int x, int y, World world) {
    if (! super.enterWorldAt(x, y, world)) return false ;
    world.presences.togglePresence(this, origin(), true, SpiceMidden.class) ;
    return true ;
  }
  
  
  public void exitWorld() {
    world.presences.togglePresence(this, origin(), false, SpiceMidden.class) ;
    super.exitWorld() ;
  }
  
  
  public int owningType() {
    return Element.ELEMENT_OWNS ;
  }
  
  
  public int pathType() {
    return Tile.PATH_HINDERS ;
  }
  
  
  public void onGrowth(Tile t) {
    spiceAmount -= Rand.num() / 2 ;
    if (spiceAmount <= 0) setAsDestroyed() ;
  }
  
  
  public Item spice() {
    return Item.withAmount(Economy.TRUE_SPICE, spiceAmount) ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Spice Midden" ;
  }
  
  
  public String helpInfo() {
    return
      "Spice ingestion becomes concentrated in the upper echelons of the "+
      "food chain, and is often used as a territorial marker by top "+
      "predators." ;
  }


  public Composite portrait(BaseUI UI) {
    //  TODO:  Fix this.
    return null;//new Composite(UI) ;
  }
  
  
  public void whenTextClicked() {
    BaseUI.current().selection.pushSelection(this, false);
  }
  

  public InfoPanel configPanel(InfoPanel panel, BaseUI UI) {
    if (panel == null) panel = new InfoPanel(UI, this, null);
    final Description d = panel.detail();
    d.append(helpInfo());
    d.append("\n\nContains: "+spice());
    return panel;
  }
  
  
  public TargetInfo configInfo(TargetInfo info, BaseUI UI) {
    if (info == null) info = new TargetInfo(UI, this);
    return info;
  }


  public Target selectionLocksOn() {
    return this ;
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    if (destroyed() || ! inWorld()) return ;
    Selection.renderPlane(
      rendering, position(null), (xdim() / 2f) + 0.5f,
      Colour.transparency(hovered ?  0.25f : 0.5f),
      Selection.SELECT_CIRCLE
    ) ;
  }
}








