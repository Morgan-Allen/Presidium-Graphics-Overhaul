

package code.user;
import code.game.common.*;
import code.graphics.common.*;
import code.graphics.terrain.*;
import code.graphics.widgets.*;
import code.util.*;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.*;



public class MapsPanel extends UIGroup {
  

  final BaseUI UI;
  final World world;
  private Base base;
  
  final Minimap minimap;
  
  
  public MapsPanel(BaseUI UI, World world, Base base) {
    super(UI) ;
    this.UI = UI ;
    this.world = world ;
    this.base = base ;
    
    minimap = new Minimap();
    final int WS = world.size, RGBA[][] = new int[WS][WS];
    for (Coord c : Visit.grid(0, 0, WS, WS, 1)) {
      final Tile t = world.tileAt(c.x, c.y);
      final Colour avg = t.habitat().baseTex.average();
      RGBA[c.x][c.y] = avg.getRGBA();
    }
    minimap.updateTexture(WS, RGBA);
  }
  

  public void setBase(Base base) {
    this.base = base ;
  }
  
  
  public void updateAt(Tile t) {
    //final Colour avg = t.minimapHue() ;
    //mapImage.putColour(avg, t.x, t.y) ;
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
    UI.camera.lockOn(world.tileAt(c.x, c.y));
  }
  
  
  public void render(SpriteBatch batch2d) {
    //  TODO:  Calling begin/end here is a bit of a hack.  Fix?
    batch2d.end();
    minimap.updateGeometry(bounds);
    minimap.renderWith(base.intelMap.fogOver());
    batch2d.begin();
    super.render(batch2d);
  }
}









