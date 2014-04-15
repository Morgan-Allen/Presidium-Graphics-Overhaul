/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.user ;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;



public interface Selectable extends Text.Clickable, Session.Saveable {
  
  String fullName();
  Composite portrait(BaseUI UI);
  
  InfoPanel configPanel(InfoPanel panel, BaseUI UI);
  TargetInfo configInfo(TargetInfo info, BaseUI UI);
  
  Target selectionLocksOn();
  void renderSelection(Rendering rendering, boolean hovered);
  
  //boolean configDetail(InfoPanel panel);
  //boolean configControl(TargetInfo panel);
  /*
  //  TODO:  Just return a DetailPanel and/or TargetInfo instead.
  String[] infoCategories() ;
  Composite portrait(HUD UI) ;
  void writeInformation(Description description, int categoryID, HUD UI) ;
  InfoPanel createPanel(BaseUI UI) ;
  Target subject() ;
  //*/
  //void whenClicked();
}






