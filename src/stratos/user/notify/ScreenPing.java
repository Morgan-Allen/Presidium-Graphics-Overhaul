/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user.notify;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



public class ScreenPing extends UIGroup {
  
  
  final static ImageAsset
    CENTRE_IMG = ImageAsset.fromImage(
      ScreenPing.class, "media/GUI/Front/ping_centre.png"
    ),
    RINGS_IMG = ImageAsset.fromImage(
      ScreenPing.class, "media/GUI/Front/ping_rings.png"
    );
  final static String
    PING_ID_PREFIX = "ping_for_";
  final static float
    DEFAULT_SIZE = 40,
    DEFAULT_LIFE =  1,
    DEFAULT_OFFX = 0.33f,
    DEFAULT_OFFY = 0.33f;
  
  
  private Image centre, rings;
  private float baseSize, timeActive = 0, lifespan;
  
  
  public static boolean addPingFor(String widgetID) {
    return addPingFor(
      widgetID, DEFAULT_LIFE, DEFAULT_SIZE, DEFAULT_OFFX, DEFAULT_OFFY
    );
  }
  
  
  public static boolean addPingFor(
    String widgetID, float lifespan, float baseSize, float offX, float offY
  ) {
    final BaseUI UI = BaseUI.current();
    if (UI == null) return false;
    
    final UINode widget = UI.activeWidgetWithID(widgetID);
    if (widget == null) return false;
    
    final String pingID = PING_ID_PREFIX+widgetID;
    final UINode matches = UI.activeWidgetWithID(pingID);
    if (matches != null) return false;
    
    final ScreenPing ping = new ScreenPing(UI);
    ping.setWidgetID(pingID);
    ping.lifespan = lifespan;
    
    ping.centre = new Image(UI, CENTRE_IMG);
    ping.rings  = new Image(UI, RINGS_IMG );
    ping.centre.alignToCentre();
    ping.rings .alignToCentre();
    ping.centre.attachTo(ping);
    ping.rings .attachTo(ping);
    
    ping.baseSize = baseSize;
    ping.absBound.setTo(widget.trueBounds());
    ping.absBound.incX(widget.xdim() * offX);
    ping.absBound.incY(widget.ydim() * offY);
    ping.attachTo(UI);
    
    return true;
  }
  
  
  private ScreenPing(BaseUI UI) {
    super(UI);
  }
  
  
  protected void updateState() {
    
    timeActive += SLOW_FADE_INC;
    final float time = timeActive % 1;
    final float alpha = 4 * time * (1 - time);
    if (timeActive > lifespan && time < SLOW_FADE_INC) { detach(); return; }
    
    final float centreSize = (1 + time) * baseSize / 2;
    centre.alignHorizontal(0, centreSize, 0);
    centre.alignVertical  (0, centreSize, 0);
    centre.relAlpha = alpha;
    final float ringsSize  = (1 + time) * baseSize / 1;
    rings .alignHorizontal(0, ringsSize , 0);
    rings .alignVertical  (0, ringsSize , 0);
    rings .relAlpha = alpha;
    
    super.updateState();
  }
  
  
  
  /**  A few associated utility methods-
    */
  public static boolean checkWidgetActive(String widgetID) {
    final BaseUI UI = BaseUI.current();
    if (UI == null) return false;
    return UI.activeWidgetWithID(widgetID) != null;
  }
  
  
  public static boolean checkCategoryActive(String categoryID) {
    final BaseUI UI = BaseUI.current();
    if (UI == null) return false;
    if (! (UI.currentInfoPane() instanceof SelectionPane)) return false;
    final SelectionPane pane = (SelectionPane) UI.currentInfoPane();
    return categoryID.equals(pane.category());
  }
}










