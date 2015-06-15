/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.common;
import stratos.game.economic.Owner;
import stratos.graphics.common.*;
import stratos.util.*;



public class Element implements Target, Session.Saveable, Stage.Visible {
  
  /**  Common fields, basic constructors, and save/load methods
    */
  final protected static int
    PROP_IN_WORLD  = 1 << 0,
    PROP_DESTROYED = 1 << 2;
  

  private Sprite sprite;
  private Object flagged;  //This is used for temporary searches, not saved.
  
  protected Stage world;
  private Tile location;
  private float inceptTime;
  private int properties;
  
  
  
  public Element(Tile tile, Sprite sprite) {
    if (tile != null) setPosition(tile.x, tile.y, tile.world);
    if (sprite != null) attachSprite(sprite);
  }
  
  
  protected Element() {}
  
  
  public Element(Session s) throws Exception {
    s.cacheInstance(this);
    world      = s.loadBool() ? s.world() : null;
    location   = (Tile) s.loadTarget();
    inceptTime = s.loadFloat();
    properties = s.loadInt();
    
    sprite = ModelAsset.loadSprite(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveBool  (world != null);
    s.saveTarget(location     );
    s.saveFloat (inceptTime   );
    s.saveInt   (properties   );
    
    ModelAsset.saveSprite(sprite, s.output());
  }
  
  
  
  /**  Life-cycle methods-
    */
  public boolean canPlace() {
    if (location == null || ! location.buildable()) return false;
    return true;
  }
  
  
  public boolean enterWorldAt(int x, int y, Stage world, boolean intact) {
    if (inWorld()) I.complain("Already in world...");
    if (! setPosition(x, y, world)) return false;
    
    this.toggleProperty(PROP_IN_WORLD, true);
    this.world = world;
    this.inceptTime = world.currentTime();
    
    if (intact && ! isMobile()) {
      if (location.above() != null) location.above().setAsDestroyed();
      location.setAbove(this, owningTier() >= Owner.TIER_PRIVATE);
    }
    return true;
  }
  
  
  public boolean enterWorldAt(int x, int y, Stage world) {
    return enterWorldAt(x, y, world, true);
  }
  
  
  public boolean enterWorldAt(Target t, Stage world) {
    final Vec3D p = t.position(null);
    if (! setPosition(p.x, p.y, world)) return false;
    if (location.blocked()) {
      final Tile entry = Spacing.nearestOpenTile(location, location);
      if (entry != null) setPosition(entry.x, entry.y, world);
    }
    enterWorld();
    return true;
  }
  
  
  public void enterWorld() {
    if (location == null) I.complain("Position never set!");
    enterWorldAt(location.x, location.y, location.world, true);
  }
  
  
  public void setAsDestroyed() {
    if (! inWorld()) {
      I.say(this+" never entered world...");
      return;
    }
    this.toggleProperty(PROP_DESTROYED, true);
    world.ephemera.addGhost(this, radius() * 2, sprite, 2.0f);
    exitWorld();
  }
  
  
  public void exitWorld() {
    if (! inWorld()) {
      I.say(this+" never entered world...");
      return;
    }
    if (! isMobile()) {
      location.setAbove(null, location.reserves() == this);
    }
    this.toggleProperty(PROP_IN_WORLD, false);
  }
  
  
  public boolean setPosition(float x, float y, Stage world) {
    this.location = world.tileAt(x, y);
    if (location == null) return false;
    else return true;
  }
  
  
  public boolean destroyed() {
    return hasProperty(PROP_DESTROYED);
  }
  
  
  public boolean inWorld() {
    return hasProperty(PROP_IN_WORLD);
  }
  
  
  public boolean indoors() {
    return false;
  }
  
  
  public boolean isMobile() {
    return false;
  }
  
  
  public Stage world() {
    return world;
  }
  
  
  public Base base() {
    return null;
  }
  
  
  
  /**  Properties, both hard-wired and custom.
    */
  public int pathType() {
    return Tile.PATH_BLOCKS;
  }
  
  
  public int owningTier() {
    return Owner.TIER_TERRAIN;
  }
  
  
  protected void toggleProperty(int prop, boolean has) {
    if (has) properties |= prop;
    else properties &= ~prop;
  }
  
  
  protected boolean hasProperty(int prop) {
    return (properties & prop) == prop;
  }
  
  
  public final void flagWith(final Object f) {
    flagged = f;
  }
  
  
  //  TODO:  This is remarkably slow for some reason, based on profiling.  Look
  //  into creating a common superclass for all Targets, avoid the need for
  //  method calls?
  public final Object flaggedWith() {
    return flagged;
  }
  
  
  
  /**  Timing-associated methods-
    */
  public void onGrowth(Tile t) {}
  
  
  //  TODO:  Get rid of this, I think?  The 'intact' argument in enterWorld()
  //         should do the trick.
  public void setAsEstablished(boolean isGrown) {
    if (isGrown) inceptTime = -10;
    else inceptTime = world.currentTime();
  }
  
  
  
  /**  Methods related to specifying position and size-
    */
  public Tile origin() {
    return location;
  }
  
  
  public int xdim() { return 1; }
  public int ydim() { return 1; }
  public int zdim() { return 1; }
  
  
  public Box2D area(Box2D put) {
    if (put == null) put = new Box2D();
    return put.set(
      location.x - 0.5f, location.y - 0.5f,
      xdim(), ydim()
    );
  }
  
  
  public Vec3D position(Vec3D v) {
    return location.position(v);
  }
  
  
  public float radius() {
    return 0.5f;
  }
  
  
  public float height() {
    return 1;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Vec3D viewPosition(Vec3D v) {
    v = position(v);
    return v;
  }
  
  
  protected float fogFor(Base base) {
    return base.intelMap.displayFog(origin(), this);
  }
  
  
  public boolean visibleTo(Base base) {
    if (! inWorld()) return base == base();
    final float fog = base == null ? 1 : fogFor(base);
    if (fog <= 0 || sprite == null) return false;
    else sprite.fog = fog;
    return true;
  }
  
  
  public void renderFor(Rendering rendering, Base base) {
    final float timeGone = world.timeMidRender() - inceptTime;
    if (timeGone < 1) sprite.colour = Colour.transparency(timeGone);
    else sprite.colour = null;
    viewPosition(sprite.position);
    sprite.readyFor(rendering);
  }
  
  
  public void attachSprite(Sprite sprite) {
    this.sprite = sprite;
  }
  
  
  public void attachModel(ModelAsset model) {
    attachSprite(model.makeSprite());
  }
  
  
  public Sprite sprite() {
    return sprite;
  }
}



