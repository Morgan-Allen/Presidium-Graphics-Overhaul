/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user.notify;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import stratos.game.base.*;
import stratos.user.notify.MessagePane.MessageSource;


public class ReminderListing extends UIGroup {
  
  
  final BaseUI UI;
  final List <Entry> entries = new List <Entry> ();
  final List <MessagePane>
    oldMessages = new List <MessagePane> (),
    newMessages = new List <MessagePane> ();
  
  private MessagePane lastOpen = null;
  
  
  public ReminderListing(BaseUI UI) {
    super(UI);
    this.UI = UI;
  }
  
  
  public void loadState(Session s) throws Exception {
    for (int n = s.loadInt(); n-- > 0;) {
      oldMessages.add(loadMessage(s));
    }
    for (int n = s.loadInt(); n-- > 0;) {
      newMessages.add(loadMessage(s));
    }
    UI.setMessagePane(loadMessage(s));
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(oldMessages.size());
    for (MessagePane m : oldMessages) saveMessage(m, s);
    s.saveInt(newMessages.size());
    for (MessagePane m : newMessages) saveMessage(m, s);
    saveMessage(UI.currentMessage(), s);
  }
  
  
  private void saveMessage(MessagePane message, Session s) throws Exception {
    if (message == null) { s.saveObject(null); return; }
    if (message.source == null) {
      I.complain("\nNO SOURCE FOR MESSAGE: "+message.title);
    }
    s.saveObject(message.source   );
    s.saveString(message.title    );
    s.saveFloat (message.receipt());
  }
  
  
  private MessagePane loadMessage(Session s ) throws Exception {
    final MessageSource source = (MessageSource) s.loadObject();
    if (source == null) return null;
    final String titleKey = s.loadString();
    final float  receipt  = s.loadFloat ();
    final MessagePane pane = source.configMessage(titleKey, UI);
    pane.assignReceiptDate(receipt);
    return pane;
  }
  
  
  
  /**  Maintaining the list of reminders-
    */
  protected static class Entry extends UIGroup implements UIConstants {
    
    final Object refers;
    boolean active;
    
    final int high, wide;
    float fadeVal, down;
    
    
    protected Entry(BaseUI UI, Object refers, int wide, int high) {
      super(UI);
      this.refers = refers;
      this.high   = high  ;
      this.wide   = wide  ;
      active  = true ;
      fadeVal =  0   ;
      down    = -1   ;
    }
  }
  
  
  private Entry entryThatRefers(Object refers) {
    for (Entry e : entries) if (e.refers == refers) return e;
    return null;
  }
  
  
  private boolean hasEntryRefers(Object refers) {
    return entryThatRefers(refers) != null;
  }
  
  
  private Entry addEntry(Object refers, int atIndex) {
    //
    //  We first determine the kind of reminder-entry appropriate for the
    //  object being referred to-
    final Base played = UI.played();
    Entry entry = null;
    if (refers instanceof Mission) {
      entry = new MissionReminder(UI, (Mission) refers);
    }
    if (refers instanceof Upgrade) {
      entry = new ResearchReminder(UI, (Upgrade) refers);
    }
    if (refers instanceof MessagePane) {
      entry = new MessageReminder(UI, refers, (MessagePane) refers);
    }
    if (refers == oldMessages) {
      entry = new CommsPane.Reminder(UI, oldMessages, this);
    }
    if (refers instanceof BaseAdvice.Topic) {
      final MessagePane advicePane = played.advice.configAdvicePanel(
        null, refers, UI
      );
      entry = new MessageReminder(UI, refers, advicePane);
    }
    if (entry == null) {
      I.complain("\nNO SUPPORTED ENTRY FOR "+refers);
      return null;
    }
    //
    //  Then we must insert the new entry at the right position in the list
    //  (skipping over anything inactive.)
    Entry before = null;
    int index = 0;
    for (Entry e : entries) if (e.active && (index++ == atIndex - 1)) {
      before = e; break;
    }
    if      (atIndex == 0  ) entries.addFirst(entry);
    else if (before == null) entries.addLast (entry);
    else entries.addAfter(entries.match(before), entry);
    entry.attachTo(this);
    return entry;
  }
  
  
  protected void updateState() {
    //
    //  Include all currently ongoing missions and any special messages:
    List <Object> needShow = new List <Object> ();
    final Base played = UI.played();
    for (final Mission mission : played.tactics.allMissions()) {
      needShow.add(mission);
    }
    for (Upgrade u : played.research.underResearch()) {
      needShow.add(u);
    }
    if (oldMessages.size() > 0) {
      needShow.add(oldMessages);
    }
    for (MessagePane o : newMessages) {
      needShow.add(o);
    }
    for (Object o : played.advice.adviceTopics()) {
      needShow.add(o);
    }
    //
    //  Now, in essence, insert entries for anything not currently listed, and
    //  delete entries for anything listed that shouldn't be.
    int index = 0; for (Object s : needShow) {
      if (! hasEntryRefers(s)) addEntry(s, index);
      index++;
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
    
    checkMessageOpened();
    updateAdvicePanes();
    super.updateState();
  }
  

  
  /**  Utility methods for needs summaries-
    */
  private void updateAdvicePanes() {
    for (Entry e : entries) if (e.refers instanceof BaseAdvice.Topic) {
      final MessagePane pane = ((MessageReminder) e).message;
      if (UI.currentMessage() != pane) continue;
      UI.played().advice.configAdvicePanel(pane, e.refers, UI);
    }
  }
  
  
  
  /**  Utility methods for message dialogues:
    */
  private void checkMessageOpened() {
    final MessagePane open = UI.currentMessage();
    if (open == lastOpen || open == null) return;
    lastOpen = open;
    if (open.source != null) open.source.messageWasOpened(open.title, UI);
  }
  
  
  private MessagePane messageEntryFor(String messageKey, int urgent) {
    if (urgent != 0) for (MessagePane message : newMessages) {
      if (message.title.equals(messageKey)) return message;
    }
    if (urgent != 1) for (MessagePane message : oldMessages) {
      if (message.title.equals(messageKey)) return message;
    }
    return null;
  }
  
  
  public boolean hasMessageEntry(String key, boolean urgent) {
    return messageEntryFor(key, urgent ? 1 : 0) != null;
  }
  
  
  public boolean hasMessageEntry(String key) {
    return hasMessageEntry(key, false) || hasMessageEntry(key, true);
  }
  
  
  public void addMessageEntry(
    MessagePane message, boolean urgent, float receiptDate
  ) {
    if (message.source == null) {
      I.complain("\nMESSAGE "+message.title+" MUST HAVE SOURCE!");
    }
    message.assignReceiptDate(receiptDate);
    if (urgent) newMessages.include(message);
    else        oldMessages.include(message);
  }
  
  
  public void retireMessage(MessagePane message) {
    final ListEntry <MessagePane> match = newMessages.match(message);
    if (match == null) return;
    newMessages.removeEntry(match);
    oldMessages.include(message);
  }
  
  
  public void retireNewMessages() {
    for (MessagePane m : newMessages) retireMessage(m);
  }
}


