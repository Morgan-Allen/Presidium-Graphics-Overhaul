


package stratos.game.base;
import stratos.game.common.*;
import stratos.game.building.*;
import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



//  TODO:  Get rid of this?  Too much complication.

public class SensorPost extends Fixture implements Selectable {
  
  
  /**  Construction and save/load methods-
    */
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    KommandoLodge.class, "media/Buildings/ecologist/sensor_array.png", 0.75f, 3
  );
  
  final KommandoLodge parent;
  private float batteryLife;
  protected Sprite camouflage;
  
  
  protected SensorPost(KommandoLodge parent, float batteryLife) {
    super(1, 3);
    this.parent = parent;
    this.batteryLife = batteryLife;
    attachModel(MODEL);
  }
  
  
  public SensorPost(Session s) throws Exception {
    super(s);
    parent = (KommandoLodge) s.loadObject();
    batteryLife = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(parent);
    s.saveFloat(batteryLife);
  }
  
  
  
  /**  Registration, life cycle and physical properties-
    */
  protected static SensorPost locateNewPost(KommandoLodge parent) {
    final World world = parent.world();
    final int numAttempts = 5, bonus = parent.structure.upgradeLevel(
      KommandoLodge.SENSOR_PERIMETER
    );
    
    float range = World.SECTOR_SIZE / 2f, spacing = 10;
    range *= 1 + bonus;
    final SensorPost post = new SensorPost(parent, 2 * (1 + bonus));
    
    Tile picked = null;
    float bestRating = 0;
    for (int n = numAttempts; n-- > 0;) {
      
      final Tile t = Spacing.pickRandomTile(parent, range, world);
      if (t.blocked()) continue;
      
      final SensorPost nearest = (SensorPost) world.presences.nearestMatch(
        SensorPost.class, t, spacing
      );
      if (nearest != null) continue;
      
      post.setPosition(t.x, t.y, world);
      if (! Spacing.perimeterFits(post)) continue;
      
      float rating = 1;
      final Venue nearV = (Venue) world.presences.nearestMatch(
        Venue.class, t, -1
      );
      if (nearV != null) rating += Spacing.distance(t, nearV);
      if (rating > bestRating) { picked = t; bestRating = rating; }
    }
    
    if (picked == null) return null;
    post.setPosition(picked.x, picked.y, world);
    return post;
  }
  
  
  public boolean enterWorldAt(int x, int y, World world) {
    if (! super.enterWorldAt(x, y, world)) return false;
    world.presences.togglePresence(this, origin(), true, SensorPost.class);
    return true;
  }
  
  
  public void exitWorld() {
    world.presences.togglePresence(this, origin(), false, SensorPost.class);
    super.exitWorld();
  }
  
  
  public int owningType() {
    return Element.ELEMENT_OWNS;
  }
  
  
  public int pathType() {
    return Tile.PATH_BLOCKS;
  }
  
  
  public void onGrowth(Tile t) {
    float inc = World.GROWTH_INTERVAL * 1f / World.STANDARD_DAY_LENGTH;
    batteryLife -= Rand.num() * inc;
    if (batteryLife <= 0) setAsDestroyed();
    else parent.base().intelMap.liftFogAround(this, 10);
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Sensor Post";
  }
  
  
  public String helpInfo() {
    return
      "Sensor Posts help to ensure that approaching enemies do not go "+
      "unchecked, and also keep tabs on local animal populations.";
  }
  
  
  public String toString() {
    return fullName();
  }


  public Composite portrait(BaseUI UI) {
    return Composite.withImage(KommandoLodge.ICON, "sensor_post");
  }
  
  
  public SelectionInfoPane configPanel(SelectionInfoPane panel, BaseUI UI) {
    if (panel == null) panel = new SelectionInfoPane(UI, this, portrait(UI));
    panel.detail().append(helpInfo());
    return panel;
  }
  
  
  public TargetOptions configInfo(TargetOptions info, BaseUI UI) {
    if (info == null) info = new TargetOptions(UI, this);
    return info;
  }
  
  
  public Target selectionLocksOn() {
    return this;
  }
  
  
  public void whenTextClicked() {
    BaseUI.current().selection.pushSelection(this, true);
  }


  public SelectionInfoPane createPanel(BaseUI UI) {
    return new SelectionInfoPane(UI, this, portrait(UI));
  }


  public Target subject() {
    return this;
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    if (destroyed() || ! inWorld()) return;
    Selection.renderPlane(
      rendering, position(null), (xdim() / 2f) + 0.5f,
      Colour.transparency(hovered ?  0.25f : 0.5f),
      Selection.SELECT_CIRCLE
    );
  }
}








