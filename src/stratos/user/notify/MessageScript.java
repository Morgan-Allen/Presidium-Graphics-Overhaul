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
  Session.Saveable, MessagePane.MessageSource, UIConstants
{
  
  private class Topic {
    
    String titleKey;
    XML sourceNode;
    MessagePane asMessage;
    
    boolean urgent    = false;
    boolean triggered = false;
    boolean completed = false;
    Method triggers    = null ;
    Method onOpen     = null ;
    Method completes  = null ;
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
      topic.triggers   = extractMethod(topicNode, "triggers" , baseClass);
      topic.onOpen     = extractMethod(topicNode, "onOpen"   , baseClass);
      topic.completes  = extractMethod(topicNode, "completes", baseClass);

      I.say("\nADDING BASE TOPIC: "+topic.titleKey);
      allTopics.put(topic.titleKey, topic);
      //
      //  Finally, any embedded images will need to loaded on the render
      //  thread, so we cache these for later reference:
      for (XML imgNode : topicNode.allChildrenMatching("image")) {
        final ImageAsset asset = ImageAsset.fromImage(
          basis.getClass(), imgNode.content()
        );
        Assets.loadNow(asset);
      }
    }
  }
  
  
  private Method extractMethod(XML node, String varName, Class baseClass) {
    final String methodName = node.value(varName);
    if (methodName == null) return null;
    final Method method = I.findMethod(baseClass, methodName);
    if (method != null) return method;
    I.say("  WARNING:  No matching "+varName+" method '"+methodName+"()'");
    return null;
  }
  
  
  public MessageScript(Session s) throws Exception {
    s.cacheInstance(this);
    this.basis           = s.loadObject();
    this.pathToScriptXML = s.loadString();
    this.loadScriptXML();
    
    for (int n = s.loadInt(); n-- > 0;) {
      final String key  = s.loadString();
      final Topic topic = allTopics.get(key);
      topic.triggered   = s.loadBool();
      topic.completed   = s.loadBool();
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(basis          );
    s.saveString(pathToScriptXML);
    
    s.saveInt(allTopics.size());
    for (Topic t : allTopics.values()) {
      s.saveString(t.titleKey );
      s.saveBool  (t.triggered);
      s.saveBool  (t.completed);
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
    allTopics.values().toArray(topics);
    
    for (Topic topic : topics) {
      if (topic.completes != null && ! topic.completed) {
        boolean didComplete = false; try {
          final Object completeVal = topic.completes.invoke(basis);
          didComplete = Boolean.TRUE.equals(completeVal);
        }
        catch (Exception e) {
          e.printStackTrace();
          allTopics.remove(topic.titleKey);
        }
        if (didComplete) {
          BaseUI.current().reminders().retireMessage(topic.asMessage);
        }
      }
      
      if (topic.triggers != null && ! topic.triggered) {
        boolean didTrigger = false; try {
          final Object triggerVal = topic.triggers.invoke(basis);
          didTrigger = Boolean.TRUE.equals(triggerVal);
        }
        catch (Exception e) {
          e.printStackTrace();
          allTopics.remove(topic.titleKey);
        }
        if (didTrigger) {
          pushTopicMessage(topic, true);
        }
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
    final int maxWide = INFO_PANEL_WIDE - (
      (DEFAULT_MARGIN * 2) + SCROLLBAR_WIDE + DEFAULT_MARGIN
    );
    pane.header().setText(title);
    
    for (XML node : topic.sourceNode.children()) {
      
      if (node.tag().equals("content")) {
        d.append("\n");
        d.append(node.content());
      }
      
      if (node.tag().equals("image")) {
        final String path = node.content();
        ImageAsset asset = ImageAsset.fromImage(basis.getClass(), path);
        if (asset == null) continue;
        Text.insert(asset.asTexture(), maxWide, false, d);
      }
      
      if (node.tag().equals("link")) {
        final String linkKey   = node.value("name");
        final String linkName  = node.content();
        final Topic  linkTopic = allTopics.get(linkKey);
        
        if (linkTopic == null) I.say(
          "\n  WARNING: NO TOPIC MATCHING "+linkKey
        );
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












