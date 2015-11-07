/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user.notify;
import stratos.start.*;
import stratos.game.common.*;
import stratos.user.*;
import stratos.util.*;
import stratos.graphics.widgets.*;



public abstract class MessageTopic extends Index.Entry implements Messaging {
  
  
  final static Index <MessageTopic> TOPIC_INDEX = new Index();
  
  final boolean isKept;
  final Class argTypes[];
  
  
  public MessageTopic(
    String uniqueID, boolean isKept, Class... argTypes
  ) {
    super(TOPIC_INDEX, uniqueID);
    this.isKept   = isKept;
    this.argTypes = argTypes;
  }
  
  
  public static MessageTopic loadConstant(Session s) throws Exception {
    return TOPIC_INDEX.loadEntry(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    TOPIC_INDEX.saveEntry(this, s.output());
  }
  
  
  public MessagePane loadMessage(Session s, BaseUI UI) throws Exception {
    final String title  = s.loadString();
    final Object args[] = s.loadWithTypes(argTypes);
    return initMessage(UI, title, args);
  }
  
  
  public void saveMessage(MessagePane message, Session s) throws Exception {
    s.saveString(message.title);
    s.saveWithTypes(message.arguments(), argTypes);
  }
  
  
  public void dispatchMessage(String title, Object... args) {
    final Scenario    context = Scenario.current();
    final BaseUI      UI      = context.UI();
    final Stage       world   = context.world();
    final MessagePane pane    = initMessage(UI, title, args);
    UI.reminders().addMessageEntry(pane, true, world.currentTime(), isKept);
  }
  
  
  protected MessagePane initMessage(BaseUI UI, String title, Object... args) {
    if (args.length != argTypes.length) {
      I.complain("PROBLEM: ARGS AND TYPES DO NOT MATCH- "+args+"/"+argTypes);
      return null;
    }
    
    final MessagePane pane = new MessagePane(UI, title, this, args);
    pane.header().setText(title);
    pane.detail().setText("");
    configMessage(UI, pane.detail(), args);
    return pane;
  }
  
  
  public void messageWasOpened(MessagePane message, BaseUI UI) {
    UI.reminders().retireMessage(message);
  }
  
  
  protected abstract void configMessage(BaseUI UI, Text d, Object... args);
}













