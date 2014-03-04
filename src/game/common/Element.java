/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.common ;
import src.graphics.common.* ;
import src.util.* ;



public class Element implements
  Target, Session.Saveable, World.Visible
{
  
  
  /**  Common fields, basic constructors, and save/load methods
    */
  final public static int
    NOTHING_OWNS     = 0,
    ELEMENT_OWNS = 1,
    FIXTURE_OWNS     = 2,
    VENUE_OWNS       = 3,
    TERRAIN_OWNS     = 4 ;
  
  final protected static int
    PROP_IN_WORLD  = 1 << 0,
    PROP_DESTROYED = 1 << 2 ;
  

  private Sprite sprite ;
  private Object flagged ;  //This is used for temporary searches, not saved.
  
  protected World world ;
  private Tile location ;
  private float inceptTime ;
  private int properties ;
  
  
  
  public Element(Tile tile, Sprite sprite) {
    if (tile != null) setPosition(tile.x, tile.y, tile.world) ;
    if (sprite != null) attachSprite(sprite) ;
  }
  
  
  protected Element() {}
  
  
  public Element(Session s) throws Exception {
    s.cacheInstance(this) ;
    world = s.loadBool() ? s.world() : null ;
    location = (Tile) s.loadTarget() ;
    inceptTime = s.loadFloat() ;
    properties = s.loadInt() ;
    
    sprite = ModelAsset.loadSprite(s.input()) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveBool(world != null) ;
    s.saveTarget(location) ;
    s.saveFloat(inceptTime) ;
    s.saveInt(properties) ;
    
    ModelAsset.saveSprite(sprite, s.output()) ;
  }
  
  
  
  /**  Life-cycle methods-
    */
  public boolean canPlace() {
    if (location == null) return false ;
    if (location.blocked()) return false ;
    if (Spacing.isEntrance(location)) return false ;
    return true ;
  }
  
  
  public boolean enterWorldAt(int x, int y, World world) {
    if (inWorld()) I.complain("Already in world...") ;
    if (! setPosition(x, y, world)) return false ;
    this.toggleProperty(PROP_IN_WORLD, true) ;
    this.world = world ;
    this.inceptTime = world.currentTime() ;
    if (owningType() != NOTHING_OWNS && ! isMobile()) {
      location.setOwner(this) ;
    }
    return true ;
  }
  
  
  public void setAsDestroyed() {
    if (! inWorld()) {
      I.say(this+" never entered world...") ;
      return ;
    }
    this.toggleProperty(PROP_DESTROYED, true) ;
    world.ephemera.addGhost(this, radius() * 2, sprite, 2.0f) ;
    exitWorld() ;
  }
  
  
  public void exitWorld() {
    if (! inWorld()) {
      I.say(this+" never entered world...") ;
      return ;
    }
    if (owningType() != NOTHING_OWNS && ! isMobile()) {
      location.setOwner(null) ;
    }
    this.toggleProperty(PROP_IN_WORLD, false) ;
  }
  
  
  public boolean setPosition(float x, float y, World world) {
    this.location = world.tileAt(x, y) ;
    if (location == null) return false ;
    else return true ;
  }
  
  
  public void enterWorld() {
    if (location == null) I.complain("Position never set!") ;
    enterWorldAt(location.x, location.y, location.world) ;
  }
  
  
  public boolean enterWorldAt(Target t, World world) {
    final Vec3D p = t.position(null) ;
    if (! setPosition(p.x, p.y, world)) return false ;
    enterWorld() ;
    return true ;
  }
  
  
  public boolean destroyed() {
    return hasProperty(PROP_DESTROYED) ;
  }
  
  
  public boolean inWorld() {
    return hasProperty(PROP_IN_WORLD) ;
  }
  
  
  public boolean isMobile() {
    return false ;
  }
  
  
  public World world() {
    return world ;
  }
  
  
  
  /**  Properties, both hard-wired and custom.
    */
  public int owningType() {
    return ELEMENT_OWNS ;
  }
  
  
  public int pathType() {
    return Tile.PATH_BLOCKS ;
  }
  
  
  protected void toggleProperty(int prop, boolean has) {
    if (has) properties |= prop ;
    else properties &= ~prop ;
  }
  
  
  protected boolean hasProperty(int prop) {
    return (properties & prop) == prop ;
  }
  
  
  public final void flagWith(final Object f) {
    flagged = f ;
  }
  
  
  //  TODO:  This is remarkably slow for some reason, based on profiling.  Look
  //  into creating a common superclass for all Targets, avoid the need for
  //  method calls?
  public final Object flaggedWith() {
    return flagged ;
  }
  
  
  
  /**  Timing-associated methods-
    */
  public void onGrowth(Tile t) {}
  
  
  public void setAsEstablished(boolean isGrown) {
    if (isGrown) inceptTime = -10 ;
    else inceptTime = world.currentTime() ;
  }
  
  
  
  /**  Methods related to specifying position and size-
    */
  public Tile origin() {
    return location ;
  }
  
  
  public int xdim() { return 1 ; }
  public int ydim() { return 1 ; }
  public int zdim() { return 1 ; }
  
  
  public Box2D area(Box2D put) {
    if (put == null) put = new Box2D() ;
    return put.set(
      location.x - 0.5f, location.y - 0.5f,
      xdim(), ydim()
    ) ;
  }
  
  
  public Vec3D position(Vec3D v) {
    return location.position(v) ;
  }
  
  
  public float radius() {
    return 0.5f ;
  }
  
  
  public float height() {
    return 1 ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Vec3D viewPosition(Vec3D v) {
    v = position(v);
    return v;
  }
  
  
  protected float fogFor(Base base) {
    return base.intelMap.displayFog(origin()) ;
  }
  
  
  public boolean visibleTo(Base base) {
    final float fog = base == null ? 1 : fogFor(base) ;
    if (fog == 0) return false ;
    else sprite.fog = fog ;
    return true ;
  }
  
  
  public void renderFor(Rendering rendering, Base base) {
    float timeGone = world.currentTime() - inceptTime;
    timeGone += Rendering.frameAlpha();
    if (timeGone < 1) sprite.colour = Colour.transparency(timeGone);
    else sprite.colour = null;
    viewPosition(sprite.position);
    sprite.update() ;
    sprite.registerFor(rendering);
    //rendering.addClient(sprite);
  }
  
  
  public void attachSprite(Sprite sprite) {
    this.sprite = sprite ;
  }
  
  
  public void attachModel(ModelAsset model) {
    attachSprite(model.makeSprite()) ;
  }
  
  
  public Sprite sprite() {
    return sprite ;
  }
}



