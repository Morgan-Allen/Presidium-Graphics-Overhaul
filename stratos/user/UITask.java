/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.user ;
import stratos.graphics.common.*;


public interface UITask {
  
  void doTask() ;
  void cancelTask() ;
  
  ImageAsset cursorImage() ;
}