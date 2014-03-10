

package src.user;
import src.game.common.*;
import src.graphics.common.*;
import src.graphics.widgets.*;
import src.graphics.terrain.*;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.*;
import src.util.*;



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
    if (super.selectionAt(mousePos) == null) return null ;
    final Coord c = minimap.getMapPosition(UI.mousePos(), bounds, world.size);
    return (c == null) ? null : this ;
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









