/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.terrain;



public class CliffOverlay {
  
  
  //  Okay.  You have a piece of geometry for each possible facing (corner-out,
  //  corner-in, full-side and half-side.)  Then you simply rotate or stitch
  //  those together to suit your needs.
  
  //  The default-geometry for each is a simple plane (based off LayerPattern
  //  or something similar.)
  
  
  void doStuff() {
    
    boolean near[] = new boolean[8];
    int indices[] = LayerPattern.outerFringeIndices(near);
    
    //  Okay.  I need to re-do this.
  }
  
  
  
  
  
}
