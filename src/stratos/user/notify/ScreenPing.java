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
  
  
  private Image centre, rings;
  private String widgetID;
  private float baseSize, timeActive = 0, lifespan;

  
  
  public static boolean addPingFor(String widgetID, float lifespan) {
    
    final BaseUI UI = BaseUI.current();
    if (UI == null) return false;
    
    final UINode widget = UI.activeWidgetWithID(widgetID);
    if (widget == null) return false;
    
    final String pingID = "ping_for_"+widgetID;
    final UINode matches = UI.activeWidgetWithID(pingID);
    if (matches != null) return false;
    
    final ScreenPing ping = new ScreenPing(UI);
    ping.widgetID = pingID;
    ping.lifespan = lifespan;
    
    ping.centre = new Image(UI, CENTRE_IMG);
    ping.rings  = new Image(UI, RINGS_IMG );
    ping.centre.alignToCentre();
    ping.rings .alignToCentre();
    ping.centre.attachTo(ping);
    ping.rings .attachTo(ping);
    
    ping.baseSize = Nums.max(widget.xdim(), widget.ydim()) / 2;
    ping.absBound.setTo(widget.trueBounds());
    ping.absBound.incX(widget.xdim() / 3);
    ping.absBound.incY(widget.ydim() / 3);
    ping.attachTo(UI);
    
    I.say("ADDING NEW PING: "+pingID+", position: "+ping.absBound);
    I.reportStackTrace();
    
    return true;
  }
  
  
  protected String widgetID() {
    return widgetID;
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
  
}









