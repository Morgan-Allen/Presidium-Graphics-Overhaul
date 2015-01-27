

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
  
  
  final static int
    MODE_NORMAL        = 0,
    MODE_AMBIENCE_MAP  = 1,
    MODE_FERTILITY_MAP = 2,
    MODE_MINERALS_MAP  = 3,
    NUM_MODES          = 4;
  final static ImageAsset MODE_ICONS[] = ImageAsset.fromImages(
    MapsPanel.class, "media/GUI/Panels/",
    "minimap_normal.png"   ,
    "minimap_ambience.png" ,
    "minimap_fertility.png",
    "minimap_minerals.png"
  );
  final static String MODE_DESC[] = new String[] {
    "Normal map mode"   ,
    "Ambience map mode" ,
    "Fertility map mode",
    "Minerals map mode" ,
  };
  final static int MODE_BUTTON_SIZE = 20;
  
  final BaseUI UI;
  final Stage world;
  private Base base;
  
  private int mapMode = MODE_NORMAL;
  final Button modeButtons[];
  
  private float lastTime = -1;
  final Minimap minimap;
  final int RGBA[][];
  
  
  public MapsPanel(BaseUI UI, Stage world, Base base) {
    super(UI);
    this.UI = UI;
    this.world = world;
    this.base = base;
    
    modeButtons = new Button[NUM_MODES];
    final int MBS = MODE_BUTTON_SIZE, HS = (int) (MBS * 1.5f);
    
    for (int n = NUM_MODES; n-- > 0;) {
      final int modeID = n;
      final Button b = new Button(
        UI, MODE_ICONS[n], Button.CIRCLE_LIT, MODE_DESC[n]
      ) {
        public void whenClicked() { setMapMode(modeID); }
      };
      b.alignVertical  (0   , MBS, 0 - MBS       );
      b.alignHorizontal(0.5f, MBS, (n * MBS) - HS);
      b.attachTo(this);
      modeButtons[n] = b;
    }
    
    minimap = new Minimap();
    RGBA = new int[world.size][world.size];
  }
  
  
  public void setMapMode(int modeID) {
    if (modeID == mapMode) return;
    this.mapMode = modeID;
    this.lastTime = -1;
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
    }
    lastTime = time;
    
    minimap.updateGeometry(bounds);
    minimap.renderWith(base.intelMap.fogOver());
    batch2d.begin();
    super.render(batch2d);
  }
  
  
  
  /**  Utility methods for getting appropriate tile-colours for various display
    *  modes...
    */
  private Colour
    avg      = new Colour(),
    modeTone = new Colour(),
    baseTone = new Colour();
  
  
  private int colourFor(Coord c) {
    final Tile t = world.tileAt(c.x, c.y);
    
    final Colour baseTone = baseToneFor(t);
    if (baseTone != null) return baseTone.withOpacity(1).getRGBA();
    
    avg.set(terrainTone(t));
    if      (mapMode == MODE_NORMAL       ) modeTone.set(avg);
    else if (mapMode == MODE_AMBIENCE_MAP ) modeTone.set(ambientTone  (t));
    else if (mapMode == MODE_FERTILITY_MAP) modeTone.set(fertilityTone(t));
    else if (mapMode == MODE_MINERALS_MAP ) modeTone.set(mineralsTone (t));
    avg.blend(modeTone, modeTone.a);
    return avg.withOpacity(1).getRGBA();
  }
  
  
  private Colour baseToneFor(Tile t) {
    
    if (t.onTop() instanceof Venue) {
      final Base b = ((Venue) t.onTop()).base();
      return b == null ? Colour.LITE_GREY : b.colour();
    }
    for (Mobile m : t.inside()) {
      final Base b = m.base();
      return b == null ? Colour.LITE_GREY : b.colour();
    }
    
    if (t.blocked()) return null;
    final Base owns = world.claims.baseClaiming(t);
    if (owns == null) return null;
    
    Colour borderTone = null;
    for (Boarding b : t.canBoard()) {
      if (b == null || b.boardableType() != Boarding.BOARDABLE_TILE) continue;
      final Base borders = world.claims.baseClaiming((Tile) b);
      if (borders != owns) { borderTone = owns.colour(); break; }
    }
    if (borderTone == null) return null;
    return baseTone.set(borderTone).blend(Colour.SOFT_WHITE, 0.5f);
  }
  
  
  private Colour terrainTone(Tile t) {
    if (world.terrain().isRoad(t)) return Habitat.ROAD_TEXTURE.average();
    return t.habitat().baseTex.average();
  }
  
  
  private float safeRange(float scale) {
    return Nums.clamp(scale, 0, 1);
  }
  
  
  private Colour ambientTone(Tile t) {
    float
      a = world.ecology().ambience.valueAt(t),
      d = base.dangerMap.sampleAround(t.x, t.y, -1),
      s = 0 - d,
      x = Nums.max(Nums.abs(a), Nums.abs(d));
    
    a = Nums.clamp((a + 10) / 20 , 0, 1); //ambience in blue
    d = Nums.clamp( d / 10       , 0, 1);  //danger in red
    s = Nums.clamp( s / 10       , 0, 1);  //safety in green
    x = Nums.clamp((x + 0.5f) / 2, 0, 1);   //alpha
    
    return modeTone.set(d, s, a, x);
  }
  
  
  private Colour fertilityTone(Tile t) {
    //  TODO:  Include radiation, et cetera.
    final float
      f = world.terrain().fertilitySample(t),
      s = safeRange(0 - world.ecology().ambience.valueAt(t) / 5);
    return modeTone.set(s, f, s, Nums.max(f, s));
  }
  
  
  private Colour mineralsTone(Tile t) {
    int type = world.terrain().mineralType(t);
    float amount = world.terrain().mineralsAt(t);
    amount /= StageTerrain.MAX_MINERAL_AMOUNT;
    
    Colour hue = Colour.SOFT_GREY;
    if (type == StageTerrain.TYPE_METALS  ) hue = Colour.LITE_RED  ;
    if (type == StageTerrain.TYPE_ISOTOPES) hue = Colour.LITE_GREEN;
    if (type == StageTerrain.TYPE_RUBBLE  ) hue = Colour.LITE_BLUE ;
    return modeTone.set(hue).blend(Colour.SOFT_GREY, 1 - amount);
  }
  
  
  
  /**  Getting associated tooltips:
    */
  //  TODO:  Implement these for each display mode!
  
}









