/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.common;
import stratos.game.craft.*;
import stratos.util.*;


public abstract class Fixture extends Element {
  
  
  /**  Field definitions, constructors, and save/load methods-
    */
  final public int size, high;
  final Box2D area = new Box2D();
  
  
  public Fixture(int size, int high) {
    this.size = size;
    this.high = high;
  }
  
  
  public Fixture(Session s) throws Exception {
    super(s);
    size = s.loadInt();
    high = s.loadInt();
    area.loadFrom(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt(size);
    s.saveInt(high);
    area.saveTo(s.output());
  }
  
  
  
  /**  Life cycle, ownership and positioning-
    */
  public boolean canPlace() {
    if (origin() == null) return false;
    final Stage world = origin().world;
    for (Tile t : world.tilesIn(area, false)) {
      if (t.blocked() || t.reserved()) return false;
    }
    return true;
  }
  
  
  public boolean enterWorldAt(int x, int y, Stage world, boolean intact) {
    if (! super.enterWorldAt(x, y, world, intact)) return false;
    if (intact) for (Tile t : world.tilesIn(area, false)) {
      final Element old = t.above();
      if (old != null && old != this) old.setAsDestroyed(false);
      t.setAbove(this, owningTier() >= Owner.TIER_PRIVATE);
    }
    return true;
  }
  
  
  public boolean setPosition(float x, float y, Stage world) {
    if (! super.setPosition(x, y, world)) return false;
    final Tile o = origin();
    area.set(o.x - 0.5f, o.y - 0.5f, size, size);
    return true;
  }
  
  
  public void exitWorld() {
    for (Tile t : world().tilesIn(area, false)) {
      t.setAbove(null, t.reserves() == this);
    }
    super.exitWorld();
  }
  
  
  public int xdim() { return size; }
  public int ydim() { return size; }
  public int zdim() { return high; }
  public float height() { return zdim(); }
  public float radius() { return size / 2f; }
  public Box2D footprint() { return area; }
  
  
  public Box2D area(Box2D put) {
    if (put == null) put = new Box2D();
    return put.setTo(area);
  }
  
  
  public Vec3D position(Vec3D v) {
    final Tile o = origin();
    if (o == null) return null;
    if (v == null) v = new Vec3D();
    v.set(
      o.x + (size / 2f) - 0.5f,
      o.y + (size / 2f) - 0.5f,
      o.elevation()
    );
    return v;
  }
  
  
  public Tile[] surrounds() {
    final Box2D around = new Box2D().setTo(footprint()).expandBy(1);
    final Stage world = origin().world;
    final Tile result[] = new Tile[(int) (around.xdim() * around.ydim())];
    int i = 0; for (Tile t : world.tilesIn(around, false)) {
      result[i++] = t;
    }
    return result;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected float fogFor(Base base) {
    float maxFog = Float.NEGATIVE_INFINITY;
    final Tile o = origin();
    for (Coord c : Visit.grid(o.x, o.y, size, size, 1)) {
      final Tile t = world.tileAt(c.x, c.y);
      final float fog = base.intelMap.displayFog(t, t == o ? this : null);
      if (fog > maxFog) maxFog = fog;
    }
    
    if (base == this.base()) maxFog = Nums.max(maxFog, 0.25f);
    return maxFog;
  }
}













