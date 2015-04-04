/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user.notify;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import stratos.graphics.widgets.Text.Clickable;
import stratos.game.base.*;
import stratos.game.common.Session.Saveable;



//  TODO:  This needs to list tutorial-messages as well as ongoing missions
//         and other status-updates.

//  TODO:  These also need to save and load!


public class ReminderListing extends UIGroup {
  
  
  final BaseUI UI;
  final List <Entry> entries = new List <Entry> ();
  final List <DialoguePane>
    oldMessages = new List <DialoguePane> (),
    newMessages = new List <DialoguePane> ();
  
  
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
    float fadeVal, down;
    
    
    protected Entry(BaseUI UI, Object refers, int wide, int high) {
      super(UI);
      this.refers = refers;
      this.high   = high  ;
      this.wide   = wide  ;
      active  = true ;
      urgent  = false;
      fadeVal =  0   ;
      down    = -1   ;
    }
  }
  
  
  private Entry entryFor(Object refers) {
    for (Entry e : entries) if (e.refers == refers) return e;
    return null;
  }
  
  
  private boolean hasEntry(Object refers) {
    return entryFor(refers) != null;
  }
  
  
  private Entry addEntry(Object refers, int afterIndex) {
    
    Entry entry = null;
    if (refers instanceof Mission) {
      entry = new MissionReminder(UI, (Mission) refers);
    }
    if (refers instanceof DialoguePane) {
      entry = new CommsReminder(UI, refers, (DialoguePane) refers);
    }
    if (refers == oldMessages) {
      entry = new CommsReminder(UI, oldMessages, forOldMessages());
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
    if (oldMessages.size() > 0) {
      needShow.add(oldMessages);
    }
    for (DialoguePane o : newMessages) {
      needShow.add(o);
    }
    //
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
    //
    //  Then iterate across all current entries and make sure their appearance
    //  is in order-
    final int padding = 20;
    int down = 0;
    for (Entry e : entries) {
      //
      //  Adjust the entry's transparency-
      if (e.active) {
        e.fadeVal = Nums.clamp(e.fadeVal + SLOW_FADE_INC, 0, 1);
      }
      else {
        e.fadeVal = Nums.clamp(e.fadeVal - SLOW_FADE_INC, 0, 1);
      }
      e.relAlpha = e.fadeVal;
      //
      //  Have it drift into the correct position-
      final float gap = down - e.down;
      float drift = Nums.min(DEFAULT_DRIFT_RATE, Nums.abs(gap));
      if (gap == 0 || e.down == -1) e.down = down;
      else e.down += (gap > 0 ? 1 : -1) * drift;
      e.alignLeft(0           , e.wide);
      e.alignTop ((int) e.down, e.high);
      //
      //  Increment for the next entry, and proceed.
      down += e.high + padding;
    }
    
    checkMessageDismissed();
    updateOldMessages();
    super.updateState();
  }
  
  
  
  /**  Utility methods for message dialogues:
    */
  private String lastViewedMessageKey = null;
  
  
  private DialoguePane forOldMessages() {
    final DialoguePane pane = new DialoguePane(
      UI, null, "Old Messages", "", null
    );
    return pane;
  }
  
  
  private void checkMessageDismissed() {
    String keyNow  = onScreenMessageKey();
    String lastKey = lastViewedMessageKey;
    if (lastKey != null && ! lastKey.equals(keyNow)) {
      I.say("\nRETIRING MESSAGE FOR: "+lastKey);
      retireMessage(messageEntryFor(lastKey));
    }
    if (keyNow != null) {
      final DialoguePane  pane  = messageEntryFor(keyNow);
      final CommsReminder entry = (CommsReminder) entryFor(pane);
      if (entry != null) entry.setFlash(false);
    }
    lastViewedMessageKey = hasMessageEntry(keyNow) ? keyNow : null;
  }
  
  
  private void updateOldMessages() {
    final CommsReminder entry = (CommsReminder) entryFor(oldMessages);
    if (entry == null) return;
    final String label = oldMessages.size()+" old messages";
    entry.setLabel(label);
    entry.setFlash(false);
    
    final Batch <Clickable> links = new Batch <Clickable> ();
    for (final DialoguePane panel : oldMessages) {
      final Clickable link = new Clickable() {
        
        public String fullName() {
          return panel.title;
        }
        
        public void whenClicked() {
          UI.setInfoPanels(panel, null);
        }
      };
      links.add(link);
    }
    entry.message.assignContent("", links);
  }
  
  
  public DialoguePane messageEntryFor(String messageKey) {
    for (DialoguePane message : newMessages) {
      if (message.title.equals(messageKey)) return message;
    }
    for (DialoguePane message : oldMessages) {
      if (message.title.equals(messageKey)) return message;
    }
    return null;
  }
  
  
  public boolean hasMessageEntry(String messageKey) {
    return messageEntryFor(messageKey) != null;
  }
  
  
  public String onScreenMessageKey() {
    if (! (UI.currentPane() instanceof DialoguePane)) return null;
    return ((DialoguePane) UI.currentPane()).title;
  }
  
  
  public void addMessageEntry(DialoguePane message, boolean urgent) {
    if (urgent) newMessages.include(message);
    else        oldMessages.include(message);
  }
  
  
  public void retireMessage(DialoguePane message) {
    newMessages.remove (message);
    oldMessages.include(message);
  }
}





