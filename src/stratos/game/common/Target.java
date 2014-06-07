/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.common ;
import stratos.util.*;



public interface Target {
  
  boolean inWorld() ;
  boolean destroyed() ;
  World world() ;
  
  Vec3D position(Vec3D v) ;
  float height() ;
  float radius() ;
  boolean isMobile();
  
  void flagWith(Object f) ;
  Object flaggedWith() ;
  
  
  //  TODO:  Move this elsewhere?
  public static class Dummy implements Target {
    
    public Vec3D position = new Vec3D();
    public float height = 1, radius = 0.5f;
    final World world;
    private Object flag;
    
    public Dummy(World world) { this.world = world; }
    
    public boolean inWorld() { return true; }
    public boolean destroyed() { return false; }
    public World world() { return world; }
    public Vec3D position(Vec3D v) { return position; }
    
    public float height() { return height; }
    public float radius() { return radius; }
    public boolean isMobile() { return false; }
    
    public void flagWith(Object f) { flag = f; }
    public Object flaggedWith() { return flag; }
  }
}


