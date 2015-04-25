/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user.notify;
import stratos.start.Assets;
import stratos.user.*;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;

import java.lang.reflect.*;



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
      I.say("\nADDING BASE TOPIC: "+topic.titleKey);
      //
      //  Trigger-methods are used to decide when the topic in question should
      //  be presented to the player, and don't always need to be included.
      final String triggerName = topicNode.value("trigger");
      if (triggerName == null) topic.trigger = null;
      else {
        topic.trigger = I.findMethod(baseClass, triggerName);
        if (topic.trigger == null) {
          I.say("  WARNING:  No matching trigger method '"+triggerName+"()'");
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
          I.say("  WARNING:  No matching on-open method '"+onOpenName+"()'");
        }
      }
      allTopics.put(topic.titleKey, topic);
      //
      //  Finally, any embedded images will need to loaded on the render
      //  thread, so we cache these for later reference:
      for (XML imgNode : topicNode.allChildrenMatching("image")) {
        ImageAsset.fromImage(basis.getClass(), imgNode.content());
      }
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
  
  
  public void clearScript() {
    for (Topic topic : allTopics.values()) {
      topic.asMessage = null;
      topic.triggered = false;
    }
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
  
  
  public void messageWasOpened(String titleKey, BaseUI UI) {
    final Topic topic = allTopics.get(titleKey);
    if (topic == null) return;
    if (topic.onOpen != null) try { topic.onOpen.invoke(basis); }
    catch (Exception e) { e.printStackTrace(); }
  }
  
  
  
  /**  Pushing and constructing MessagePanes for topic-presentation:
    */
  private void pushTopicMessage(Topic topic, boolean viewNow) {

    final BaseUI UI = BaseUI.current();
    final MessagePane message = messageFor(topic.titleKey, UI);
    
    if (message == null) return;
    else topic.triggered = true;
    
    final ReminderListing reminders = UI.reminders();
    if (topic.urgent && ! reminders.hasMessageEntry(topic.titleKey)) {
      final float receiptTime = UI.played().world.currentTime();
      reminders.addMessageEntry(message, true, receiptTime);
    }
    if (viewNow) UI.setInfoPanels(message, null);
  }
  
  
  private MessagePane messageFor(String title, final BaseUI UI) {
    final Topic topic = allTopics.get(title);
    if (topic == null) return null;
    if (topic.asMessage != null) return topic.asMessage;
    
    final MessagePane pane = topic.asMessage = new MessagePane(
      UI, null, topic.titleKey, null, this
    );
    final Description d = pane.detail();
    
    for (XML node : topic.sourceNode.children()) {
      if (node.tag().equals("content")) {
        d.append("\n");
        d.append(node.content());
      }
      if (node.tag().equals("image")) {
        final String path = node.content();
        ImageAsset asset = ImageAsset.fromImage(basis.getClass(), path);
        if (asset == null) continue;
        ((Text) d).insert(asset.asTexture(), 240, 50, false);
      }
      if (node.tag().equals("link")) {
        final String linkKey   = node.value("name");
        final String linkName  = node.content();
        final Topic  linkTopic = allTopics.get(linkKey);
        
        if (linkTopic == null) {
          I.say("\n  WARNING: NO TOPIC MATCHING "+linkKey);
        }
        else d.append(new Description.Link("\n  "+linkName) {
          public void whenClicked() {
            UI.reminders().retireMessage(topic.asMessage);
            pushTopicMessage(linkTopic, true);
          }
        });
      }
    }
    
    return topic.asMessage;
  }
}












