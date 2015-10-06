/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.user;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.terrain.*;
import stratos.graphics.sfx.*;
import stratos.graphics.widgets.*;
import stratos.start.PlayLoop;
import stratos.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;



public class Selection implements UIConstants {
  
  
  /**  Field definitions and accessors-
    */
  final public static PlaneFX.Model
    SIMPLE_SELECT_MODEL = PlaneFX.imageModel(
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
  
  private Tile    pickTile;
  private Element pickElement;
  private Mobile  pickMobile;
  private Mission pickMission;
  
  private Selectable hovered, selected;
  
  
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
  public Element pickedElement() { return pickElement; }
  public Mobile  pickedMobile () { return pickMobile ; }
  public Mission pickedMission() { return pickMission; }
  
  
  
  /**  
    */
  boolean updateSelection(Stage world, Viewport port, UIGroup infoPanel) {
    if (
      selected != null &&
      UI.currentInfoPane() == null &&
      UI.currentOptions() == null
    ) {
      pushSelection(selected);
    }
    //
    //  If a UI element is selected, don't pick anything else-
    if (UI.selected() != null) {
      pickTile = null;
      pickMobile = null;
      pickElement = null;
      hovered = null;
      return false;
    }
    //
    //  Our first task to see what the different kinds of object currently
    //  being hovered over are-
    final Base base = UI.played();
    hovered     = null;
    pickTile    = world.pickedTile   (UI, port, base);
    pickElement = world.pickedFixture(UI, port, base);
    pickMobile  = world.pickedMobile (UI, port, base);
    pickMission = Base.pickedMission(world, UI, port, base);
    
    if (verbose && I.used60Frames) {
      I.say("\nPicked tile is: "+pickTile);
      I.say("  Owner is:       "+pickTile.reserves());
      I.say("  Path type is:   "+pickTile.pathType());
      I.say("  Picked element: "+pickElement);
    }
    
    //
    //  Then, we see which type is given priority-
    if (pickMission != null) {
      hovered = pickMission;
    }
    else if (pickMobile instanceof Selectable) {
      hovered = (Selectable) pickMobile;
    }
    else if (pickElement instanceof Selectable) {
      hovered = (Selectable) pickElement;
    }
    else {
      hovered = pickTile;
    }
    
    if (UI.mouseClicked() && UI.currentTask() == null) {
      pushSelection(hovered);
    }
    I.talkAbout = selected;
    return true;
  }
  

  public void pushSelection(Selectable s) {
    
    if (s == null) {
      selected = null;
      UI.tracking.lockOn(null);
      return;
    }
    
    selected = s;
    I.talkAbout = selected;
    if (! PlayLoop.onMainThread()) return;
    
    final Target locks = s.selectionLocksOn();
    if (locks != null && locks.inWorld()) UI.tracking.lockOn(locks);
    else UI.tracking.lockOn(null);
    
    final SelectionPane    pane    = s.configSelectPane   (null, UI);
    final SelectionOptions options = s.configSelectOptions(null, UI);
    if (pane != null || options != null) {
      UI.setInfoPane   (pane   );
      UI.setOptionsList(options);
    }
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
    Colour c, ImageAsset tex, boolean innerFringe,
    final String key, boolean cache, final Object... group
  ) {
    //
    //  If we've already cached an overlay with this key, then use that.
    //  Otherwise, we generate a fresh instance.
    TerrainChunk overlay = null;
    if (cache && haveCached(key)) {
      overlay = overlayCache.get(key);
    }
    else {
      //
      //  The first step is to flag any tiles underneath the affected area (
      //  which can include tiles, fixtures, or arbitrary boxes.)
      final Batch <Tile> under = new Batch <Tile> ();
      Box2D limit = null;
      
      for (Object o : group) {
        Box2D area = null;
        if (o instanceof Fixture) area = ((Fixture) o).footprint();
        if (o instanceof Box2D  ) area = (Box2D) o;
        if (o instanceof Tile   ) area = ((Tile) o).area(null);
        
        if (area != null) for (Tile t : world.tilesIn(area, true)) {
          under.add(t);
          t.flagWith(under);
          if (limit == null) limit = new Box2D(t.x, t.y, 0, 0);
          limit.include(t.x, t.y, 0.5f);
        }
      }
      //
      //  Then we create a terrain-layer to present this visually-
      final LayerType layer = new LayerType(tex, innerFringe, -1, "overlay") {
        
        protected boolean maskedAt(int tx, int ty, TerrainSet terrain) {
          final Tile t = world.tileAt(tx, ty);
          return t != null && t.flaggedWith() == under;
        }
        
        protected int variantAt(int tx, int ty, TerrainSet terrain) {
          return 0;
        }
      };
      if (! innerFringe) limit.expandBy(1);
      //
      //  Paint this over the world, unflag the tiles, and add to the cache for
      //  later reference as needed.
      overlay = world.terrain().createOverlay(limit, layer);
      for (Tile t : under) t.flagWith(null);
      if (cache) addToCache(overlay, key);
      else overlay.throwAway = true;
    }
    //
    //  Finally, apply a glow-colour and ready for rendering...
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
      rendering, e.origin().world, pos, (e.xdim() + 0.5f) / 2,
      Colour.transparency(hovered ? 0.25f : 0.5f),
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
      
      final LayerType layer = new LayerType(texture, true, -1, "plane") {
        
        protected boolean maskedAt(int tx, int ty, TerrainSet terrain) {
          return true;
        }
        
        protected int variantAt(int tx, int ty, TerrainSet terrain) {
          return 0;
        }
        
        protected void addFringes(
          int tx, int ty, TerrainSet terrain,
          Batch <Vec3D  > offsBatch,
          Batch <Integer> faceBatch,
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
          offsBatch.add(new Vec3D(tx, ty, 0));
          faceBatch.add(-1);
          textBatch.add(UV);
        }
      };
      
      area.incHigh(1);
      area.incWide(1);
      area.cropBy(world.area());
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




