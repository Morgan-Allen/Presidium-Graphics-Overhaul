

package stratos.user;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.terrain.*;
import stratos.graphics.widgets.*;
import stratos.util.*;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.*;



public class MapsPanel extends UIGroup {
  

  final BaseUI UI;
  final World world;
  private Base base;
  
  private float lastTime = -1;
  final Minimap minimap;
  
  
  public MapsPanel(BaseUI UI, World world, Base base) {
    super(UI) ;
    this.UI = UI ;
    this.world = world ;
    this.base = base ;
    minimap = new Minimap();
  }
  

  public void setBase(Base base) {
    this.base = base ;
  }
  
  
  protected UINode selectionAt(Vector2 mousePos) {
    final UINode kid = super.selectionAt(mousePos);
    if (kid != null) return kid;
    if (! bounds.contains(mousePos.x, mousePos.y)) return null;
    final Coord c = minimap.getMapPosition(UI.mousePos(), bounds, world.size);
    final Tile t = world.tileAt(c.x, c.y);
    return (t == null) ? null : this;
  }
  
  
  protected void whenClicked() {
    final Coord c = minimap.getMapPosition(UI.mousePos(), bounds, world.size);
    if (c == null) return;
    UI.viewTracking.lockOn(world.tileAt(c.x, c.y));
  }
  
  
  public void render(SpriteBatch batch2d) {
    //  TODO:  Calling begin/end here is a bit of a hack.  Fix?
    batch2d.end();
    
    final float time = Rendering.activeTime();
    if (((int) lastTime) != ((int) time)) {
      final int WS = world.size, RGBA[][] = new int[WS][WS];
      for (Coord c : Visit.grid(0, 0, WS, WS, 1)) {
        final Tile t = world.tileAt(c.x, c.y);
        final Colour avg = t.minimapTone();
        RGBA[c.x][c.y] = avg.getRGBA();
      }
      minimap.updateTexture(WS, RGBA);
    }
    lastTime = time;
    
    minimap.updateGeometry(bounds);
    minimap.renderWith(base.intelMap.fogOver());
    batch2d.begin();
    super.render(batch2d);
  }
}









