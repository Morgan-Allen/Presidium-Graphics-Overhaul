/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.user ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;
import src.util.*;



public interface Selectable extends Text.Clickable, Session.Saveable {
  
  //
  //  I don't think all of this is really needed.  fullName(), writeInfo()
  //  and portrait() are the only essentials, really.  Maybe configPanel()?
  
  
  String fullName() ;
  String helpInfo() ;
  
  String[] infoCategories() ;
  Composite portrait(HUD UI) ;
  void writeInformation(Description description, int categoryID, HUD UI) ;
  
  void whenClicked() ;
  InfoPanel createPanel(BaseUI UI) ;
  Target subject() ;
  void renderSelection(Rendering rendering, boolean hovered) ;
}



