/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.widgets;
import stratos.util.*;



public class Carousel extends UIGroup {
  
  
  final static float
    SHRINK_MIN = 0.75f,
    GROW_MAX   = 1.50f;
  
  
  final List <UINode> entries = new List();
  final List <Object> refers  = new List();
  float spinAngle = 0, targetAngle = 0;
  
  
  public Carousel(HUD UI) {
    super(UI);
  }
  
  
  protected void updateState() {
    //
    //  Update the displacement, size, depth and opacity of each entry-
    int index = 0; for (UINode b : entries) {
      float angle = index * 360f / entries.size();
      angle = Nums.toRadians(angle - spinAngle);
      
      final float
        across = (1 + Nums.sin(angle)) / 2,
        depth  = (1 - Nums.cos(angle)) / 2,
        scale  = SHRINK_MIN + ((1 - depth) * (GROW_MAX - SHRINK_MIN)),
        size   = scale * Nums.min(ydim(), xdim() / entries.size()),
        offX   = size * (0.5f - across) / xdim();
      
      b.alignVertical(0.5f, size, 0);
      b.alignHorizontal(across + offX, size, 0);
      b.relDepth = depth;
      if (depth <= 0.5f) b.relAlpha = 1;
      else b.relAlpha = 1.5f - depth;
      
      index++;
    }
    sortKidsByDepth();
    super.updateState();
  }
  
  
  protected void render(WidgetsPass pass) {
    //
    //  Firstly, update our current spin position (interpolating toward the
    //  target angle as needed.)
    final float
      spinDiff = Vec2D.degreeDif(targetAngle, spinAngle),
      spinRate = 180f / pass.rendering.frameRate();
    if (Nums.abs(spinDiff) <= spinRate) spinAngle = targetAngle;
    else spinAngle += spinRate * (spinDiff > 0 ? 1 : -1);
    super.render(pass);
  }


  public void addEntryFor(Object ref, UINode entry) {
    refers .add(ref  );
    entries.add(entry);
    entry.attachTo(this);
  }
  
  
  public void setSelection(Object ref) {
    final int index = refers.indexOf(ref);
    if (index >= 0) targetAngle = index * 360f / refers.size();
  }
  
  
}



