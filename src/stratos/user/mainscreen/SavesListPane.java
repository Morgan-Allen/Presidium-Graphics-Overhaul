/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user.mainscreen;
import stratos.graphics.widgets.*;
import stratos.start.*;
import stratos.util.*;
import stratos.util.Description.*;



public class SavesListPane extends MenuPane {
  
  
  final String prefix;

  
  public SavesListPane(HUD UI) {
    this(UI, null);
  }
  
  
  public SavesListPane(HUD UI, String prefixFilter) {
    super(UI, MainScreen.MENU_SAVES_LIST);
    this.prefix = prefixFilter;
  }
  
  
  protected void fillListing(List <UINode> listing) {
    final String saves[] = prefix == null ?
      SaveUtils.latestSaves()     :
      SaveUtils.savedFiles(prefix);
    
    listing.add(createTextItem("Saved Games:", 1.2f, null, 1));
    
    for (final String path : saves) {
      final String titlePath;
      if (prefix == null) titlePath = SaveUtils.prefixFor(path);
      else                titlePath = SaveUtils.suffixFor(path);
      
      listing.add(createTextButton("  "+titlePath, 1, new Link() {
        public void whenClicked(Object context) {
          SaveUtils.loadGame(path, true);
        }
      }));
    }
  }
  
}







