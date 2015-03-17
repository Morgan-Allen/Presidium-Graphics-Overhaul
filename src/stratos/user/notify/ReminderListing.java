/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user.notify;
import stratos.game.common.*;
import stratos.game.politic.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import stratos.game.common.Session.Saveable;



//  TODO:  This needs to list tutorial-messages as well as ongoing missions
//         and other status-updates.

//  TODO:  Animate positional changes for new/expired entries?

//  TODO:  In principle, this could then replace the Comms panel.

//  TODO:  These also need to save and load!


public class ReminderListing extends UIGroup {
  
  
  final BaseUI UI;
  final List <Entry> entries = new List <Entry> ();
  final List <DialoguePane> messages = new List <DialoguePane> ();
  
  
  public ReminderListing(BaseUI UI) {
    super(UI);
    this.UI = UI;
  }
  
  
  
  /**  Maintaining the list of reminders-
    */
  protected static class Entry extends UIGroup implements UIConstants {
    
    final Object refers;
    boolean active;
    boolean urgent;
    
    final int high, wide;
    float fadeVal;
    Coord oldPos, newPos;
    
    protected Entry(BaseUI UI, Object refers, int wide, int high) {
      super(UI);
      this.refers = refers;
      this.high   = high  ;
      this.wide   = wide  ;
      active  = true ;
      urgent  = false;
      fadeVal = 0    ;
    }
  }
  
  
  private boolean hasEntry(Object refers) {
    for (Entry e : entries) if (e.refers == refers) return true;
    return false;
  }
  
  
  private Entry addEntry(Object refers, int afterIndex) {
    
    Entry entry = null;
    if (refers instanceof Mission) {
      entry = new MissionReminder(UI, (Mission) refers);
    }
    if (refers instanceof DialoguePane) {
      entry = new CommsReminder(UI, (DialoguePane) refers);
    }
    if (entry == null) {
      I.complain("\nNO SUPPORTED ENTRY FOR "+refers);
      return null;
    }
    
    final Entry after = entries.atIndex(afterIndex);
    if (after == null) entries.addLast(entry);
    else entries.addAfter(entries.match(after), entry);
    entry.attachTo(this);
    return entry;
  }
  
  
  protected void updateState() {
    //
    //  Include all currently ongoing missions and any special messages:
    List <Object> needShow = new List <Object> ();
    for (final Mission mission : UI.played().tactics.allMissions()) {
      needShow.add(mission);
    }
    for (Object o : messages) needShow.add(o);
    
    //  Now, in essence, insert entries for anything not currently listed, and
    //  delete entries for anything listed that shouldn't be.
    for (Object s : needShow) {
      if (! hasEntry(s)) addEntry(s, entries.size() - 1);
    }
    for (Entry e : entries) {
      if (e.active && ! needShow.includes(e.refers)) {
        e.active = false;
        e.fadeVal = 1;
      }
      if (e.fadeVal <= 0 && ! e.active) {
        e.detach();
        entries.remove(e);
      }
    }
    
    final int padding = 20;
    int down = 0;
    for (Entry e : entries) {
      if (e.active) {
        e.fadeVal = Nums.clamp(e.fadeVal + DEFAULT_FADE_INC, 0, 1);
      }
      else {
        e.fadeVal = Nums.clamp(e.fadeVal - DEFAULT_FADE_INC, 0, 1);
      }
      e.relAlpha = e.fadeVal;
      e.alignLeft(0   , e.wide);
      e.alignTop (down, e.high);
      down += e.high + padding;
    }
    
    super.updateState();
  }
  
  
  
  /**  Utility methods for message dialogues:
    */
  public boolean hasEntryFor(String messageKey) {
    for (DialoguePane message : messages) {
      if (message.title.equals(messageKey)) return true;
    }
    return false;
  }
  
  
  public void addEntry(DialoguePane message, boolean urgent) {
    messages.include(message);
  }
}













