


package stratos.user;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.util.*;



public abstract class TargetTask implements UITask {
  
  
  final BaseUI UI;
  final ImageAsset cursor;
  
  //Target from, to;
  //boolean valid;
  
  
  protected TargetTask(BaseUI UI, ImageAsset cursor) {
    this.UI = UI;
    this.cursor = cursor;
  }
  
  
  
  abstract boolean validPick(Target pick);
  abstract void previewAt(Target picked, boolean valid);
  abstract void performAt(Target picked);
  
  

  public void doTask() {
    //
    //  Get the currently picked tile, fixture and mobile.  See if they are
    //  valid as targets.  If so, preview in green.  Otherwise, preview in
    //  red.  If the user clicks, perform the placement.
    final Tile PT = UI.selection.pickedTile();
    final Fixture PF = UI.selection.pickedFixture();
    final Mobile PM = UI.selection.pickedMobile();
    
    boolean valid = true;
    Target picked = null;
    if      (validPick(PM)) picked = PM;
    else if (validPick(PF)) picked = PF;
    else if (validPick(PT)) picked = PT;
    else { picked = PT; valid = false; }
    
    if (picked != null) {
      previewAt(picked, valid);
      if (UI.mouseClicked() && valid) {
        performAt(picked);
        UI.endCurrentTask();
      }
    }
  }
  
  
  
  public void cancelTask() {
    UI.endCurrentTask();
  }
  
  
  public ImageAsset cursorImage() {
    return cursor;
  }
}







