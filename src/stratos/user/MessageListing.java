

package stratos.user;
import stratos.game.common.*;
import stratos.game.politic.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.util.*;
import stratos.game.common.Session.Saveable;



//  TODO:  This needs to list tutorial-messages as well as ongoing missions,
//         and other status-updates.

//  TODO:  In principle, this could then replace the Comms panel.


public class MessageListing extends UIGroup {
  
  
  final BaseUI UI;
  List <Entry> entries = new List <Entry> ();
  
  private class Entry {
    Saveable refers;
    boolean active;
    
    UINode shown;
    float fadeVal;
    Coord oldPos, newPos;
  }
  
  
  public MessageListing(BaseUI UI) {
    super(UI);
    this.UI = UI;
  }
  
  
  private void addEntry(Saveable refers, int afterIndex) {
    
    final BaseUI BUI = (BaseUI) UI;
    final Entry entry = new Entry();
    entry.refers  = refers;
    entry.active  = true  ;
    entry.fadeVal = 0     ;
    
    if (refers instanceof Mission) {
      final Mission m = (Mission) refers;
      final UIGroup group = new UIGroup(UI);
      
      final Composite portrait = m.portrait(UI);
      final Button button = new Button(
        UI, portrait.texture(), Button.CIRCLE_LIT.asTexture(), m.fullName()
      ) {
        protected void whenClicked() {
          BUI.selection.pushSelection(m);
        }
      };
      button.alignVertical  (0, 0);
      button.alignHorizontal(0, 0);
      button.attachTo(group);
      //  TODO:  List the portraits of current applicants, and a red-green-
      //         amber indicator for approval status.
      
      final BorderedLabel label = new BorderedLabel(UI);
      label.alignLeft  (0, 0);
      label.alignBottom(0, 0);
      label.text.scale = 0.75f;
      label.setMessage(m.fullName(), false, 0);
      label.attachTo(group);
      
      entry.shown = group;
    }
    
    final Entry after = entries.atIndex(afterIndex);
    if (after == null) entries.addLast(entry);
    else entries.addAfter(entries.match(after), entry);
    entry.shown.attachTo(this);
  }
  
  
  private boolean hasEntry(Saveable refers) {
    for (Entry e : entries) if (e.refers == refers) return true;
    return false;
  }
  
  
  protected void updateState() {
    //
    //  Only do this if the missions-list has changed?  Animate positional
    //  changes?
    List <Saveable> needShow = new List <Saveable> ();
    for (final Mission mission : UI.played().tactics.allMissions()) {
      needShow.add(mission);
    }
    
    //  Now, in essence, insert entries for anything not currently listed, and
    //  delete entries for anything listed that shouldn't be.
    
    final int size = 40; int index = 0;
    
    for (Saveable s : needShow) {
      if (! hasEntry(s)) addEntry(s, entries.size() - 1);
    }
    for (Entry e : entries) {
      if (e.active && ! needShow.includes(e.refers)) {
        e.active = false;
        e.fadeVal = 1;
      }
      if (e.fadeVal <= 0 && ! e.active) {
        entries.remove(e);
      }
    }
    
    for (Entry e : entries) {
      if (e.active) {
        e.fadeVal = Nums.clamp(e.fadeVal + DEFAULT_FADE_INC, 0, 1);
      }
      else {
        e.fadeVal = Nums.clamp(e.fadeVal - DEFAULT_FADE_INC, 0, 1);
      }
      e.shown.relAlpha = e.fadeVal;
      e.shown.alignLeft(0, size);
      e.shown.alignTop(size * index, size);
      index++;
    }
    
    super.updateState();
  }
}














