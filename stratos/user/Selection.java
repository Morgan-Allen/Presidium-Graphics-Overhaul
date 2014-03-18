/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.user ;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.tactical.*;
import stratos.graphics.common.*;
import stratos.graphics.sfx.*;
import stratos.graphics.widgets.*;
import stratos.util.*;



public class Selection implements UIConstants {
  
  
  /**  Field definitions and accessors-
    */
  private static boolean verbose = false;
  
  final BaseUI UI ;
  
  private Tile pickTile ;
  private Fixture pickFixture ;
  private Mobile pickMobile ;
  private Mission pickMission ;
  
  private Selectable hovered, selected ;
  private Stack <Selectable> navStack = new Stack <Selectable> () ;
  
  
  Selection(BaseUI UI) {
    this.UI = UI ;
  }

  public void loadState(Session s) throws Exception {
    pushSelection((Selectable) s.loadObject(), false) ;
  }
  

  public void saveState(Session s) throws Exception {
    s.saveObject(selected) ;
  }
  
  
  public Selectable hovered()  { return hovered  ; }
  public Selectable selected() { return selected ; }
  
  public Tile    pickedTile   () { return pickTile    ; }
  public Fixture pickedFixture() { return pickFixture ; }
  public Mobile  pickedMobile () { return pickMobile  ; }
  public Mission pickedMission() { return pickMission ; }
  
  
  
  /**  
    */
  boolean updateSelection(World world, Viewport port, UIGroup infoPanel) {
    //
    //  If a UI element is selected, don't pick anything else-
    if (UI.selected() != null) {
      pickTile = null ;
      pickMobile = null ;
      pickFixture = null ;
      hovered = null ;
      return false ;
    }
    //
    //  Our first task to see what the different kinds of object currently
    //  being hovered over are-
    final Base base = UI.played() ;
    hovered = null ;
    pickTile = world.pickedTile(UI, port, base) ;
    pickFixture = world.pickedFixture(UI, port, base) ;
    pickMobile = world.pickedMobile(UI, port, base) ;
    pickMission = UI.played().pickedMission(UI, port) ;
    //
    //  Then, we see which type is given priority-
    if (pickMission != null) {
      hovered = pickMission ;
    }
    else if (pickMobile instanceof Selectable) {
      hovered = (Selectable) pickMobile ;
    }
    else if (pickFixture instanceof Selectable) {
      hovered = (Selectable) pickFixture ;
    }
    else {
      hovered = null ;
    }
    return true ;
  }
  

  public void pushSelection(Selectable s, boolean asRoot) {
    if (asRoot) navStack.clear() ;
    
    if (s != null) {
      selected = s ;
      if (s.subject().inWorld()) UI.viewTracking.lockOn(s.subject()) ;
      final InfoPanel panel = s.createPanel(UI) ;
      
      final int SI = navStack.indexOf(selected) ;
      Selectable previous = null ;
      if (SI != -1) {
        if (selected == navStack.getLast()) previous = null ;
        else previous = navStack.atIndex(SI + 1) ;
        while (navStack.getFirst() != selected) navStack.removeFirst() ;
        panel.setPrevious(previous) ;
      }
      else {
        previous = navStack.getFirst() ;
        navStack.addFirst(selected) ;
        panel.setPrevious(previous) ;
      }
      
      if (verbose) {
        I.say("Navigation stack is: ") ;
        for (Selectable n : navStack) I.add("\n  "+n) ;
      }
      
      final TargetInfo info = (s instanceof Target) ?
        new TargetInfo(UI, (Target) s) :
        null;
      UI.setInfoPanel(panel, info);
    }
    else if (selected != null) {
      navStack.clear() ;
      selected = null ;
      UI.viewTracking.lockOn(null) ;
      UI.setInfoPanel(null, null) ;
    }
  }
  
  
  
  /**  Rendering FX-
    */
  void renderWorldFX(Rendering rendering) {
    final Target
      HS = (hovered  == null) ? null : hovered .subject(),
      SS = (selected == null) ? null : selected.subject() ;
    if (HS != null && HS != SS) {
      hovered.renderSelection(rendering, true) ;
    }
    if (SS != null) {
      selected.renderSelection(rendering, false) ;
    }
  }
  
  
  public static void renderPlane(
    Rendering r, Vec3D pos, float radius, Colour c, PlaneFX.Model texModel
  ) {
    final PlaneFX ring = (PlaneFX) texModel.makeSprite();
    ring.colour = c;
    ring.scale = radius;
    ring.position.setTo(pos);
    ring.readyFor(r);
  }
}










