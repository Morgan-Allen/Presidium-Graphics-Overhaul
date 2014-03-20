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
  
  void flagWith(Object f) ;
  Object flaggedWith() ;
}