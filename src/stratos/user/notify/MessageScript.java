/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user.notify;
import stratos.user.*;
import stratos.game.common.Session;
import stratos.graphics.widgets.Text.Clickable;
import stratos.util.*;
import java.lang.reflect.*;



//  TODO:  Show time and date as well?...


public class MessageScript implements
  Session.Saveable, MessagePane.MessageSource
{
  
  private class Topic {
    
    String titleKey;
    XML sourceNode;
    MessagePane asMessage;
    
    boolean urgent    = false;
    boolean triggered = false;
    Method trigger    = null ;
    Method onOpen     = null ;
  }
  
  
  final Session.Saveable basis;
  final String pathToScriptXML;
  final Table <String, Topic> allTopics = new Table <String, Topic> ();
  
  
  
  public MessageScript(Session.Saveable basis, String pathToScriptXML) {
    this.basis = basis;
    this.pathToScriptXML = pathToScriptXML;
    this.loadScriptXML();
  }
  
  
  private void loadScriptXML() {
    final Class <? extends Session.Saveable> baseClass = basis.getClass();
    final XML xml = XML.load(pathToScriptXML);
    for (XML topicNode : xml.allChildrenMatching("topic")) {
      //
      //  We initialise the topic with a unique title, source material and
      //  other type information-
      final Topic topic = new Topic();
      topic.sourceNode = topicNode;
      topic.titleKey   = topicNode.value("name");
      topic.urgent     = topicNode.getBool("urgent");
      //
      //  Trigger-methods are used to decide when the topic in question should
      //  be presented to the player, and don't always need to be included.
      final String triggerName = topicNode.value("trigger");
      if (triggerName == null) topic.trigger = null;
      else {
        topic.trigger = I.findMethod(baseClass, triggerName);
        if (topic.trigger == null) {
          I.say("\nWARNING:  No matching trigger method '"+triggerName+"()'");
        }
      }
      //
      //  On-open methods, as the name suggests, are called when a message is
      //  first viewed (and not before.)
      final String onOpenName = topicNode.value("onOpen");
      if (onOpenName == null) topic.onOpen = null;
      else {
        topic.onOpen = I.findMethod(baseClass, onOpenName);
        if (topic.onOpen == null) {
          I.say("\nWARNING:  No matching on-open method '"+onOpenName+"()'");
        }
      }
      allTopics.put(topic.titleKey, topic);
      I.say("\nADDED BASE TOPIC: "+topic.titleKey);
    }
  }
  
  
  public MessageScript(Session s) throws Exception {
    s.cacheInstance(this);
    this.basis           = s.loadObject();
    this.pathToScriptXML = s.loadString();
    this.loadScriptXML();
    
    for (int n = s.loadInt(); n-- > 0;) {
      final String key = s.loadString();
      final Topic topic = allTopics.get(key);
      topic.triggered = s.loadBool();
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(basis          );
    s.saveString(pathToScriptXML);
    
    s.saveInt(allTopics.size());
    for (Topic t : allTopics.values()) {
      s.saveString(t.titleKey );
      s.saveBool  (t.triggered);
    }
  }
  
  
  public MessagePane configMessage(String titleKey, BaseUI UI) {
    return messageFor(titleKey, UI);
  }
  
  
  public void messageWasOpened(String titleKey, BaseUI UI) {
    final Topic topic = allTopics.get(titleKey);
    if (topic == null) return;
    if (topic.onOpen != null) try { topic.onOpen.invoke(basis); }
    catch (Exception e) { e.printStackTrace(); }
  }
  


  /**  Regular updates to check for script-events:
    */
  public void checkForEvents() {
    //
    //  (We use an array here to allow deletions mid-iteration if something
    //  goes wrong, which java would not otherwise allow.)
    final Topic topics[] = new Topic[allTopics.size()];
    for (Topic topic : allTopics.values().toArray(topics)) {
      if (topic.trigger == null || topic.triggered) continue;
      boolean didTrigger = false;
      try {
        final Object triggerVal = topic.trigger.invoke(basis);
        didTrigger = Boolean.TRUE.equals(triggerVal);
      }
      catch (Exception e) {
        I.report(e);
        allTopics.remove(topic.titleKey);
      }
      
      if (didTrigger) {
        pushTopicMessage(topic, true);
      }
    }
  }
  
  
  private void pushTopicMessage(Topic topic, boolean viewNow) {

    final BaseUI UI = BaseUI.current();
    final MessagePane message = messageFor(topic.titleKey, UI);
    
    if (message == null) return;
    else topic.triggered = true;
    
    final ReminderListing reminders = UI.reminders();
    if (topic.urgent && ! reminders.hasMessageEntry(topic.titleKey)) {
      reminders.addMessageEntry(message, true);
    }
    if (viewNow) UI.setInfoPanels(message, null);
  }
  
  
  private MessagePane messageFor(String title, final BaseUI UI) {
    final Topic topic = allTopics.get(title);
    if (topic == null) return null;
    if (topic.asMessage != null) return topic.asMessage;
    
    
    //  TODO:  Also, image insertion!
    
    final String content = topic.sourceNode.child("content").content();
    final Batch <Clickable> links = new Batch <Clickable> ();
    
    for (XML node : topic.sourceNode.children()) {
      if (node.tag().equals("content")) {
        
      }
      if (node.tag().equals("image")) {
        
      }
      if (node.tag().equals("link")) {
        final String linkKey   = node.value("name");
        final String linkName  = node.content();
        final Topic  linkTopic = allTopics.get(linkKey);
        
        if (linkTopic == null) {
          I.say("\n  WARNING: NO TOPIC MATCHING "+linkKey);
        }
        else links.add(new Description.Link(linkName) {
          public void whenClicked() {
            UI.reminders().retireMessage(topic.asMessage);
            pushTopicMessage(linkTopic, true);
          }
        });
      }
    }
    
    links.add(new Description.Link("Dismiss") {
      public void whenClicked() {
        UI.reminders().retireMessage(topic.asMessage);
        UI.setInfoPanels(null, null);
      }
    });
    
    topic.asMessage = new MessagePane(UI, null, topic.titleKey, null, this);
    topic.asMessage.assignContent(content, links);
    return topic.asMessage;
  }
}












