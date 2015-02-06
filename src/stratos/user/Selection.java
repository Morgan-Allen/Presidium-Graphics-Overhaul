/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.user;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.politic.*;
import stratos.graphics.common.*;
import stratos.graphics.terrain.*;
import stratos.graphics.sfx.*;
import stratos.graphics.widgets.*;
import stratos.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;



public class Selection implements UIConstants {
  
  
  /**  Field definitions and accessors-
    */
  final public static PlaneFX.Model
    SIMPLE_SELECT_MODEL = new PlaneFX.Model(
      "select_circle_fx", Selection.class,
      "media/GUI/selectCircle.png", 1, 0, 0, false, false
    );
  final public static ImageAsset
    SELECT_CIRCLE = ImageAsset.fromImage(
      Selection.class, "media/GUI/selectCircle.png"
    ),
    SELECT_SQUARE = ImageAsset.fromImage(
      Selection.class, "media/GUI/selectSquare.png"
    ),
    SELECT_OVERLAY = ImageAsset.fromImage(
      Selection.class, "media/GUI/selectOverlay.png"
    ),
    PLACE_OVERLAY = ImageAsset.fromImage(
      Selection.class, "media/GUI/placeOverlay.png"
    );
  
  
  private static boolean verbose = false;
  
  final BaseUI UI;
  
  private Tile pickTile;
  private Fixture pickFixture;
  private Mobile pickMobile;
  private Mission pickMission;
  
  private Selectable hovered, selected;
  private Stack <Selectable> navStack = new Stack <Selectable> ();
  
  
  Selection(BaseUI UI) {
    this.UI = UI;
  }
  
  
  public void loadState(Session s) throws Exception {
    selected = (Selectable) s.loadObject();
  }
  

  public void saveState(Session s) throws Exception {
    s.saveObject(selected);
  }
  
  
  public Selectable hovered()  { return hovered ; }
  public Selectable selected() { return selected; }
  
  public Tile    pickedTile   () { return pickTile   ; }
  public Fixture pickedFixture() { return pickFixture; }
  public Mobile  pickedMobile () { return pickMobile ; }
  public Mission pickedMission() { return pickMission; }
  
  
  
  /**  
    */
  boolean updateSelection(Stage world, Viewport port, UIGroup infoPanel) {
    if (
      selected != null &&
      UI.currentPane() == null &&
      UI.currentInfo() == null
    ) {
      pushSelection(selected, true);
    }
    //
    //  If a UI element is selected, don't pick anything else-
    if (UI.selected() != null) {
      pickTile = null;
      pickMobile = null;
      pickFixture = null;
      hovered = null;
      return false;
    }
    //
    //  Our first task to see what the different kinds of object currently
    //  being hovered over are-
    final Base base = UI.played();
    hovered     = null;
    pickTile    = world.pickedTile   (UI, port, base);
    pickFixture = world.pickedFixture(UI, port, base);
    pickMobile  = world.pickedMobile (UI, port, base);
    pickMission = Base.pickedMission(world, UI, port, base);
    
    if (
      verbose && pickTile != null &&
      Gdx.input.isKeyPressed(Input.Keys.SPACE)
    ) {
      I.say("\nPicked tile is: "+pickTile);
      I.say("  Owner is:     "+pickTile.onTop());
      I.say("  Path type is: "+pickTile.pathType());
    }
    
    //
    //  Then, we see which type is given priority-
    if (pickMission != null) {
      hovered = pickMission;
    }
    else if (pickMobile instanceof Selectable) {
      hovered = (Selectable) pickMobile;
    }
    else if (pickFixture instanceof Selectable) {
      hovered = (Selectable) pickFixture;
    }
    else {
      hovered = pickTile;
    }
    
    if (UI.mouseClicked() && UI.currentTask() == null) {
      pushSelection(hovered, true);
    }
    I.talkAbout = selected;
    return true;
  }
  

  public void pushSelection(Selectable s, boolean asRoot) {
    
    if (s == null) {
      navStack.clear();
      selected = null;
      UI.tracking.lockOn(null);
      UI.setInfoPanels(null, null);
      return;
    }
    else if (asRoot) navStack.clear();
    
    selected = s;
    final Target locks = s.selectionLocksOn();
    if (locks.inWorld()) UI.tracking.lockOn(locks);
    final SelectionInfoPane panel = s.configPanel(null, UI);
    final TargetOptions info = s.configInfo(null, UI);
    UI.setInfoPanels(panel, info);
    
    I.talkAbout = selected;
  }
  
  
  
  /**  Rendering FX-
    */
  final static int MAX_CACHE = 5;
  private Table <String, TerrainChunk> overlayCache = new Table();
  private List <String> recentOverlays = new List();
  
  
  protected void renderWorldFX(Rendering rendering) {
    final Target
      HS = (hovered  == null) ? null : hovered.selectionLocksOn(),
      SS = (selected == null) ? null : selected.selectionLocksOn();
    if (HS != null && HS != SS) {
      hovered.renderSelection(rendering, true);
    }
    if (SS != null) {
      selected.renderSelection(rendering, false);
    }
  }
  
  
  public static void renderSimpleCircle(
    Target target, Vec3D pos, Rendering r, Colour c
  ) {
    final PlaneFX ring = (PlaneFX) SIMPLE_SELECT_MODEL.makeSprite();
    ring.colour = c;
    ring.scale = target.radius();
    ring.position.setTo(pos);
    ring.readyFor(r);
    ring.passType = Sprite.PASS_SPLAT;
  }
  
  
  private void addToCache(TerrainChunk overlay, String key) {
    recentOverlays.addFirst(key);
    overlayCache.put(key, overlay);

    if (recentOverlays.size() > MAX_CACHE) {
      final Object oldest = recentOverlays.removeLast();
      final TerrainChunk gone = overlayCache.get(oldest);
      gone.dispose();
      overlayCache.remove(oldest);
    }
  }
  
  
  private boolean haveCached(String key) {
    for (String s : recentOverlays) if (s.equals(key)) return true;
    return false;
  }
  
  
  public void renderTileOverlay(
    Rendering r, final Stage world,
    Colour c, ImageAsset tex,
    final String key, boolean cache, final Object... group
  ) {
    TerrainChunk overlay = null;
    
    if (cache && haveCached(key)) {
      overlay = overlayCache.get(key);
    }
    else {
      //  Otherwise, put together a fresh overlay-
      Box2D limit = null;
      final Batch <Tile> under = new Batch <Tile> ();
      
      for (Object o : group) {
        Box2D area = null;
        if (o instanceof Fixture) area = ((Fixture) o).footprint();
        if (o instanceof Box2D  ) area = (Box2D) o;
        if (area != null) for (Tile t : world.tilesIn(area, true)) {
          if (limit == null) limit = new Box2D(t.x, t.y, 0, 0);
          limit.include(t.x, t.y, 0.5f);
          under.add(t);
          t.flagWith(under);
        }
      }
      
      final LayerType layer = new LayerType(tex, false, -1) {
        
        protected boolean maskedAt(int tx, int ty, TerrainSet terrain) {
          final Tile t = world.tileAt(tx, ty);
          return t != null && t.flaggedWith() == under;
        }
        
        protected int variantAt(int tx, int ty, TerrainSet terrain) {
          final Tile t = world.tileAt(tx, ty);
          return t.blocked() ? -1 : 0;
        }
      };
      limit.expandBy(1);
      
      overlay = world.terrain().createOverlay(limit, layer);
      for (Tile t : under) t.flagWith(null);
      if (cache) addToCache(overlay, key);
    }
    
    //  Use a glow-colour, and ready for rendering-
    c = new Colour(c).withGlow(c.a);
    overlay.colour = c;
    overlay.readyFor(r);
  }
  
  
  public void renderCircleOnGround(
    Rendering rendering, Element e, boolean hovered
  ) {
    if (e.origin() == null) I.complain(
      "MUST SET LOCATION BEFORE RENDERING SELECTION..."
    );
    
    final String key = e.origin()+"_plane_"+I.tagHash(e);
    final Vec3D pos = (e instanceof Mobile) ?
      ((Mobile) e).viewPosition(null) :
      e.position(null);
    
    renderPlane(
      rendering, e.origin().world, pos, (e.xdim() / 2f) + 1,
      hovered ? Colour.transparency(0.5f) : Colour.WHITE,
      Selection.SELECT_CIRCLE, true, key
    );
  }
  
  
  public void renderPlane(
    Rendering r, Stage world,
    Vec3D pos, float radius,
    Colour c, ImageAsset texture,
    boolean cache, String key
  ) {
    final boolean report = false;
    TerrainChunk overlay = null;
    
    if (cache && haveCached(key)) {
      overlay = overlayCache.get(key);
    }
    else {
      final Box2D area = new Box2D(
        pos.x - radius,
        pos.y - radius,
        radius * 2,
        radius * 2
      );
      if (report) {
        I.say("\nOverlay area is: "+area);
        I.say("  Position: "+pos+", radius: "+radius);
      }
      
      final float
        xp = area.xpos(), yp = area.ypos(),
        xd = area.ydim(), yd = area.ydim();
      
      final LayerType layer = new LayerType(texture, true, -1) {
        
        protected boolean maskedAt(int tx, int ty, TerrainSet terrain) {
          return true;
        }
        
        protected int variantAt(int tx, int ty, TerrainSet terrain) {
          return 0;
        }
        
        protected void addFringes(
          int tx, int ty, TerrainSet terrain,
          Batch <Coord> gridBatch,
          Batch <float[]> textBatch
        ) {
          final int len = LayerPattern.UV_PATTERN.length;
          final float UV[] = new float[len];

          //  Subtract the origin coordinates from the lower corner of each
          //  tile, add the UV offset, and divide by the size of area.
          for (int i = len; i-- > 0;) {
            final float f = LayerPattern.UV_PATTERN[i];
            UV[i] = (i % 2 == 0) ?
              (((tx - 0.5f - xp) + f    ) / xd) :
              (((ty - 0.5f - yp) + 1 - f) / yd) ;
          }
          gridBatch.add(new Coord(tx, ty));
          textBatch.add(UV);
        }
      };
      
      area.incHigh(1);
      area.incWide(1);
      overlay = world.terrain().createOverlay(area, layer);
      if (cache) addToCache(overlay, key);
    }
    
    //  Use a glow-colour and ready for rendering-
    c = new Colour().set(c);
    c.a *= -1;
    overlay.colour = c;
    overlay.readyFor(r);
  }
}




