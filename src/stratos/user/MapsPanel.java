

package stratos.user;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.wild.*;
import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.graphics.terrain.*;
import stratos.graphics.widgets.*;
import stratos.util.*;

import com.badlogic.gdx.math.*;



public class MapsPanel extends UIGroup {
  

  final BaseUI UI;
  final Stage world;
  private Base base;
  
  private float lastTime = -1;
  final Minimap minimap;
  final int RGBA[][];
  
  
  public MapsPanel(BaseUI UI, Stage world, Base base) {
    super(UI);
    this.UI = UI;
    this.world = world;
    this.base = base;
    minimap = new Minimap();
    RGBA = new int[world.size][world.size];
  }
  

  public void setBase(Base base) {
    this.base = base;
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
    UI.tracking.lockOn(world.tileAt(c.x, c.y));
  }
  
  
  public void render(WidgetsPass batch2d) {
    //  TODO:  Calling begin/end here is a bit of a hack.  Fix?
    batch2d.end();
    
    //  TODO:  Try and have this fade in gradually...
    final float time = Rendering.activeTime();
    if (((int) lastTime) != ((int) time)) {
      final int WS = world.size;
      for (Coord c : Visit.grid(0, 0, WS, WS, 1)) {
        RGBA[c.x][c.y] = colourFor(c);
      }
      minimap.updateTexture(WS, RGBA);
      cleanupTemps();
    }
    lastTime = time;
    
    minimap.updateGeometry(bounds);
    minimap.renderWith(base.intelMap.fogOver());
    batch2d.begin();
    super.render(batch2d);
  }
  
  
  private Colour avg;
  private Tile near[];
  
  
  private void cleanupTemps() {
    avg = null;
    near = null;
  }
  
  
  private int colourFor(Coord c) {
    if (avg == null) { avg = new Colour(); near = new Tile[8]; }
    
    final Tile t = world.tileAt(c.x, c.y);
    avg.set(tileTone(t));
    
    //  TODO:  Save this for various display modes...
    /*
    float f = world.terrain().fertilitySample(t);
    avg.blend(Colour.greyscale(f), 0.85f);
    //*/
    
    final Base border = baseWithBorder(t);
    if (border != null) {
      final Colour badge = border.colour();
      avg.set(badge).blend(Colour.WHITE, 0.5f);
    }
    
    return avg.getRGBA();
  }
  
  
  private Colour tileTone(Tile t) {
    
    if (t.onTop() instanceof Venue) {
      final Base b = ((Venue) t.onTop()).base();
      return b == null ? Colour.LITE_GREY : b.colour();
    }
    
    for (Mobile m : t.inside()) {
      final Base b = m.base();
      return b == null ? Colour.LITE_GREY : b.colour();
    }
    
    if (world.terrain().isRoad(t)) return Habitat.ROAD_TEXTURE.average();
    return t.habitat().baseTex.average();
  }
  
  
  private Base baseWithBorder(Tile t) {
    if (t.blocked()) return null;
    final Base owns = world.claims.baseClaiming(t);
    if (owns == null) return null;
    for (Boarding b : t.canBoard()) {
      if (b == null || b.boardableType() != Boarding.BOARDABLE_TILE) continue;
      final Base borders = world.claims.baseClaiming((Tile) b);
      if (borders != owns) return owns;
    }
    return null;
  }
}









