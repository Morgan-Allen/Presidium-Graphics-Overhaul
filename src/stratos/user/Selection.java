/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.user;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.tactical.*;
import stratos.graphics.common.*;
import stratos.graphics.terrain.*;
import stratos.graphics.sfx.*;
import stratos.graphics.widgets.*;
import stratos.util.*;



public class Selection implements UIConstants {
  
  
  /**  Field definitions and accessors-
    */
  final public static PlaneFX.Model
    SELECT_CIRCLE = new PlaneFX.Model(
      "select_circle_fx", Selection.class,
      "media/GUI/selectCircle.png", 1, 0, 0, false, false
    ),
    SELECT_SQUARE = new PlaneFX.Model(
      "select_square_fx", Selection.class,
      "media/GUI/selectSquare.png", 1, 0, 0, false, false
    );
  final public static ImageAsset
    SELECT_OVERLAY = ImageAsset.fromImage(
      "media/GUI/selectOverlay.png", Selection.class
    ),
    PLACE_OVERLAY = ImageAsset.fromImage(
      "media/GUI/placeOverlay.png", Selection.class
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
  boolean updateSelection(World world, Viewport port, UIGroup infoPanel) {
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
    hovered = null;
    pickTile = world.pickedTile(UI, port, base);
    pickFixture = world.pickedFixture(UI, port, base);
    pickMobile = world.pickedMobile(UI, port, base);
    pickMission = UI.played().pickedMission(UI, port);
    
    if (
      verbose && pickTile != null &&
      Gdx.input.isKeyPressed(Input.Keys.SPACE)
    ) {
      I.say("Picked tile is: "+pickTile);
      I.say("  Owner is: "+pickTile.owner());
      I.say("  Path type is: "+pickTile.pathType());
      I.say("  Minimap tone is: "+pickTile.minimapTone());
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
    
    if (panel != null) {
      final int SI = navStack.indexOf(selected);
      Selectable previous = null;
      if (SI != -1) {
        if (selected == navStack.getLast()) previous = null;
        else previous = navStack.atIndex(SI + 1);
        while (navStack.getFirst() != selected) navStack.removeFirst();
        panel.setPrevious(previous);
      }
      else {
        previous = navStack.getFirst();
        navStack.addFirst(selected);
        panel.setPrevious(previous);
      }
      
      if (verbose) {
        I.say("Navigation stack is: ");
        for (Selectable n : navStack) I.add("\n  "+n);
        I.add("\n");
      }
    }
    
    I.talkAbout = selected;
  }
  
  
  
  /**  Rendering FX-
    */
  final static int MAX_CACHE = 5;
  private Table <Element, TerrainChunk> overlayCache = new Table();
  private List <Element> recentOverlays = new List();
  
  
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
  
  
  public static void renderPlane(
    Rendering r, Vec3D pos, float radius, Colour c, PlaneFX.Model texModel
  ) {
    final PlaneFX ring = (PlaneFX) texModel.makeSprite();
    ring.colour = c;
    ring.scale = radius;
    ring.position.setTo(pos);
    ring.readyFor(r);
  }
  
  
  public void renderTileOverlay(
    Rendering r, final World world,
    Colour c, ImageAsset tex, boolean cache,
    final Fixture key, final Fixture... group
  ) {
    //  Use a glow-colour:
    c = new Colour().set(c);
    c.a *= -1;
    
    if (cache && recentOverlays.includes(key)) {
      final TerrainChunk overlay = overlayCache.get(key);
      overlay.colour = c;
      overlay.readyFor(r);
      return;
    }
    if (cache && recentOverlays.size() > MAX_CACHE) {
      final Element oldest = recentOverlays.removeLast();
      overlayCache.remove(oldest);
    }
    
    final Box2D limit = key.area(null);
    final Batch <Tile> under = new Batch <Tile> ();
    for (Fixture f : group) {
      if (f == null) continue;
      for (Tile t : world.tilesIn(f.area(), true)) {
        limit.include(t.x, t.y, 0.5f);
        under.add(t);
        t.flagWith(under);
      }
    }
    
    //  TODO:  Try using an inner-fringe here instead, and mask using the
    //  empty tiles around the perimeter!
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
    
    final TerrainChunk overlay = world.terrain().createOverlay(limit, layer);
    overlay.colour = c;
    overlay.readyFor(r);
    
    for (Tile t : under) t.flagWith(null);
    if (cache) {
      recentOverlays.addFirst(key);
      overlayCache.put(key, overlay);
    }
  }
}







