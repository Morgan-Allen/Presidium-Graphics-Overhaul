

package stratos.user;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.util.*;



//  TODO:  This isn't actually being used at the moment.  Revisit later.
/*
public class MissionsList extends UIGroup {
  
  
  
  final BaseUI UI;
  List <Button> listing;
  
  
  public MissionsList(BaseUI UI) {
    super(UI);
    this.UI = UI;
  }
  
  
  
  protected void updateState() {
    
    //
    //  Only do this if the missions-list has changed?  Animate positional
    //  changes?
    
    for (Button button : listing) button.detach();
    
    float down = 0;
    for (final Mission mission : UI.played().allMissions()) {
      //Texture t = mission.flagTex();
      
      final CutoutSprite s = mission.flagSprite();
      final Texture t = ((CutoutModel) s.model()).texture();
      
      final Button button = new Button(
        UI, mission.flagTexPath(), mission.fullName()
      ) {
        protected void whenHovered() {
        }
        protected void whenClicked() {
          UI.selection.pushSelection(mission, true);
        }
      };
      
      button.absBound.set(0, down, 40, 40);
      button.relBound.set(0, 1, 0, 0);
      button.attachTo(this);
      down += 40;
    }
  }
}
//*/












