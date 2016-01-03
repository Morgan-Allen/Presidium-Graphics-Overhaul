/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.common;
import stratos.util.*;




public class StagePatch implements Target {
  
  
  /**  Data fields, construction, and save/load methods-
    */
  final public Stage world;
  final public Box3D bounds = new Box3D();
  final public Box2D area   = new Box2D();
  final public int x, y, absX, absY, depth, size;
  
  protected StagePatch kids[], parent;
  protected boolean updateBounds = true;
  
  private Object flagged = null;
  
  
  StagePatch(StagePatches w, int x, int y, int d) {
    this.world = w.world;
    this.x = x;
    this.y = y;
    this.depth = d;
    this.size = (1 << depth) * w.resolution;
    this.absX = x * size;
    this.absY = y * size;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(world);
    s.saveInt((int) (area.xpos() + 1));
    s.saveInt((int) (area.ypos() + 1));
  }
  
  
  public static StagePatch loadConstant(Session s) throws Exception {
    final Stage world = (Stage) s.loadObject();
    return world.patches.patchAt(s.loadInt(), s.loadInt());
  }
  
  
  
  /**  Satisfying the target contract-
    */
  public Stage world() { return world; }
  public Base base() { return null; }
  
  public boolean inWorld() { return true; }
  public boolean destroyed() { return false; }
  public boolean indoors() { return false; }
  public boolean isMobile() { return false; }
  
  public Vec3D position(Vec3D v) {
    if (v == null) v = new Vec3D();
    return v.set((x + 0.5f) * size, (y + 0.5f) * size, 0);
  }
  
  public float radius() {
    return size / 2f;
  }
  
  public float height() {
    return bounds.zdim();
  }
  
  public void flagWith(Object f) { flagged = f; }
  public Object flaggedWith() { return flagged; }
  
  
  
  /**  Diagnosis and feedback methods-
    */
  public String toString() {
    return "Section at "+absX+"|"+absY+" (size "+size+")";
  }
}




