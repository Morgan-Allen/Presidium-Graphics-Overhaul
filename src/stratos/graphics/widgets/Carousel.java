/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.widgets;
import stratos.graphics.common.*;
import stratos.util.*;



public class Carousel extends UIGroup {
  
  
  final static float
    SPIN_RATE = 90f / Rendering.FRAMES_PER_SECOND;
  
  
  final List <UINode> entries = new List();
  final List <Object> refers  = new List();
  float spinAngle = 0, targetAngle = 0;
  
  
  public Carousel(HUD UI) {
    super(UI);
  }
  
  
  protected void updateState() {
    
    final float spinDiff = Vec2D.degreeDif(targetAngle, spinAngle);
    if (Nums.abs(spinDiff) <= SPIN_RATE) {
      spinAngle = targetAngle;
    }
    else {
      spinAngle += SPIN_RATE * (spinDiff > 0 ? 1 : -1);
    }
    
    int index = 0; for (UINode b : entries) {
      float angle = index * 360f / entries.size();
      angle = Nums.toRadians(angle + spinAngle);
      float across = (1 + Nums.cos(angle)) / 2;
      float depth  = (1 + Nums.sin(angle)) / 2;
      
      float halfW = 1f / entries.size();
      b.alignVertical(0, 0);
      b.alignAcross(across - (halfW * across), across + (halfW * (1 - across)));
      b.relDepth = depth;
      index++;
      //  TODO:  Shrink the rearward nodes to give a fake 'perspective' effect-
      //         and darken them a bit..?
    }
    
    super.updateState();
  }
  
  
  public void addEntryFor(Object ref, UINode entry) {
    refers .add(ref  );
    entries.add(entry);
    entry.attachTo(this);
  }
  
  
  public void setSpinTarget(Object ref) {
    targetAngle = refers.indexOf(ref) * 360f / refers.size();
  }
  
  
}












