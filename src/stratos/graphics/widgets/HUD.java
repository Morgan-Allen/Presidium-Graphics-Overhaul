/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.widgets;
import stratos.graphics.common.*;
import stratos.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.math.*;



/**  This is the 'master' UI class.
  */
public class HUD extends UIGroup {
  
  
  /**  Data fields, constructors and setup methods-
    */
  private static boolean
    verbose = false;
  
  
  final public Rendering rendering;
  
  private long
    hoverStart = -1;
  private Vector2
    nextMP = new Vector2(),
    dragMP = new Vector2();
  private boolean
    nextMB;
  
  private UINode
    selected,
    dragsFrom;
  private boolean
    mouseB;
  private byte
    mouseState = HOVERED;
  private Vector2
    mousePos = new Vector2();
  
  private Table <String, UINode> widgetsByID = new Table();
  
  
  public HUD(Rendering rendering) {
    super(null);
    this.rendering = rendering;
  }
  
  
  
  /**  Regular updates, input records and rendering cycle-
    */
  public void updateInput() {
    nextMB = Gdx.input.isButtonPressed(Buttons.LEFT);
    nextMP.x = Gdx.input.getX();
    nextMP.y = Gdx.graphics.getHeight() - Gdx.input.getY();
    
    mouseState = HOVERED;
    
    if ((! mouseB) && nextMB) {
      mouseState = CLICKED;
      dragMP.set(nextMP);
    }
    if (mouseB && nextMB) {
      mouseState = DRAGGED;
    }
    mousePos.set(nextMP);
    mouseB = nextMB;
    //  TODO:  Perform the selection mechanics here?
  }
  
  
  //  This is used for two-dimensional GUI elements in the conventional drawing
  //  hierarchy.
  public void renderHUD(Rendering rendering) {
    final boolean report = verbose;
    relBound.set(0, 0, 1, 1);
    absBound.set(0, 0, 0, 0);
    final Box2D size = new Box2D();
    size.xdim(Gdx.graphics.getWidth ());
    size.ydim(Gdx.graphics.getHeight());
    updateAsBase(size);
    
    final UINode oldSelect = selected;
    selected = selectionAt(mousePos);
    
    if (mouseState == HOVERED && ! selectionMatch(selected, oldSelect)) {
      hoverStart = System.currentTimeMillis();
      if (report) {
        I.say("Hover begun at "+hoverStart);
        I.say("  Old/new selections: "+oldSelect+"/"+selected);
      }
    }
    
    if (selected != null) switch (mouseState) {
      case (HOVERED) : selected.whenHovered(); break;
      case (CLICKED) : selected.whenClicked(); break;
      case (PRESSED) : selected.whenPressed(); break;
    }
    if (mouseState == CLICKED && dragsFrom == null) {
      dragsFrom = selected;
      if (report) {
        I.say("Drag begun: "+selected);
      }
    }
    else if (dragsFrom != null && mouseState == DRAGGED) {
      dragsFrom.whenDragged();
    }
    else dragsFrom = null;
    
    widgetsByID.clear();
    recordActiveWidgetsFrom(this);
    
    render(rendering.widgetsPass);
  }
  
  
  private void recordActiveWidgetsFrom(UINode node) {
    final String ID = node.widgetID();
    if (ID != null) widgetsByID.put(ID, node);
    if (node instanceof UIGroup) for (UINode kid : ((UIGroup) node).kids) {
      if (! kid.hidden) recordActiveWidgetsFrom(kid);
    }
  }
  
  
  public UINode activeWidgetWithID(String ID) {
    return widgetsByID.get(ID);
  }
  
  
  //  NOTE:  This a placeholder method intended for override by subclasses, and
  //  called by the main PlayLoop.
  public void renderWorldFX() {
    return;
  }
  
  
  
  /**  Handling mouse activity-
    */
  public Vector2 mousePos() { return mousePos; }
  public Vector2 dragOrigin() { return dragMP; }
  
  public int mouseX() { return (int) mousePos.x; }
  public int mouseY() { return (int) mousePos.y; }
  
  
  public boolean mouseDown() { return mouseB;  }
  public boolean mouseClicked() { return isMouseState(CLICKED); }
  public boolean mouseHovered() { return isMouseState(HOVERED); }
  public boolean mouseDragged() { return isMouseState(DRAGGED); }
  public boolean mousePressed() { return isMouseState(PRESSED); }

  
  public boolean isMouseState(final byte state) {
    return mouseState == state;
  }
  
  
  
  /**  Handling hovers and selections-
    */
  private boolean selectionMatch(UINode a, UINode b) {
    if (a == b) return true;
    if (a == null && b != null) return false;
    if (b == null && a != null) return false;
    return a.equals(b);
  }
  
  
  public boolean amSelected(UINode node, byte state) {
    return (selected == node) && (mouseState == state);
  }
  
  
  public float timeHovered() {
    final long time = System.currentTimeMillis() - hoverStart;
    return time / 1000f;
  }
  
  
  public UINode selected() {
    return selected;
  }
  
  
  public UINode dragsFrom() {
    return dragsFrom;
  }
  
  
  public Box2D screenBounds() {
    return bounds;
  }
  
  
  
  /**  These are some generic stub methods for common-but-not-universal UI
    *  functions that a particular HUD might or might not use:
    */
  public void setInfoPane(UIGroup info) {}
  public void clearInfoPane() {}
  public UIGroup currentInfoPane() { return null; }
  
  public void setOptionsList(UIGroup options) {}
  public void clearOptionsList() {}
  public UIGroup currentOptions() { return null; }
}











