/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.start;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.user.notify.*;
import stratos.util.*;
import stratos.graphics.widgets.Text.Clickable;


//
//  Alright.  How do I introduce these more gradually?

//  Using the arrow or WASD keys to move around.  Or the minimap.


/*

*  Use a linear sequence with much more detail on specific, simple steps.  You
   want to cover-
   * Basic Navigation and UI Features (access to game inputs and readouts.)
   * Building & Upgrades (space management.)
   * Recruitment & Missions (indirect control, motivation.)
   * Inital objectives (security, basic housing, turning a profit.)
   
   Just don't railroad the player if they want to wander off and explore the
   game's mechanics themselves.

*  More feedback on needs and settlement demands.  (Notifications or an RCI
   indicator of some sort are needed.)  You need feedback for-
     *  Citizen needs   / no food source (imports or grown.)
     *  Low funding     / no income source (exports or taxes.)
     *  Citizen housing / no holdings built.
     *  Specific goods  / no factory source built.
     *  Citizen health  / no medical team (sickbay and archives.)
     *  Danger          / no security force (walls and military.)
   Emphasise the emotive component here?
//*/


public class TutorialScript {
  
  final static String
    SCRIPT_FILE = "media/Help/TutorialScript.xml";
  
  final static List <String> BIG_KEYS = new List <String> ();
  
  
  final static String
    EVENT_WELCOME         = BIG_KEYS.pass("Welcome!"                   ),
    EVENT_SECURITY_DONE   = BIG_KEYS.pass("Security Objective Complete"),
    EVENT_CONTACT_DONE    = BIG_KEYS.pass("Contact Objective Complete" ),
    EVENT_ECONOMY_DONE    = BIG_KEYS.pass("Economy Objective Complete" ),
    EVENT_CONGRATULATIONS = BIG_KEYS.pass("Tutorial Complete!"         );
  
  final TutorialScenario tutorial;
  private BaseUI UI = null;
  final Table <String, Object> allTopics = new Table <String, Object> ();
  final Table <String, Boolean> allFlags = new Table <String, Boolean> ();
  
  
  protected TutorialScript(TutorialScenario tutorial) {
    this.tutorial = tutorial;
    final XML xml = XML.load(SCRIPT_FILE);
    
    for (XML topicNode : xml.allChildrenMatching("topic")) {
      final String titleKey = topicNode.value("name");
      allTopics.put(titleKey, topicNode);
      I.say("\nFOUND BASE TOPIC: "+titleKey);
    }
    
    for (String key : BIG_KEYS) if (allTopics.get(key) == null) I.say(
      "\nWARNING: No matching topic found for "+key+" in "+SCRIPT_FILE
    );
  }
  
  
  protected DialoguePane messageFor(String title, final BaseUI UI) {
    
    final Object cached = allTopics.get(title);
    if (cached instanceof DialoguePane) return (DialoguePane) cached;
    if (cached == null) return null;
    
    final XML topicNode = (XML) cached;
    final String content = topicNode.child("content").content();
    final Batch <Clickable> links = new Batch <Clickable> ();
    
    for (XML linkNode : topicNode.allChildrenMatching("link")) {
      final String linkKey  = linkNode.value("name");
      final String linkName = linkNode.content();
      
      links.add(new Clickable() {
        
        public String fullName() {
          return linkName;
        }
        
        public void whenClicked() {
          final DialoguePane message = messageFor(linkKey, UI);
          if (message != null) {
            UI.setInfoPanels(message, null);
          }
          else I.say("\nNO TOPIC MATCHING: "+linkKey);
        }
      });
    }
    
    final DialoguePane panel = new DialoguePane(
      UI, null, title, content, null, links
    );
    allTopics.put(title, panel);
    return panel;
  }
  
  
  
  /**  Updated methods for progress-conditions.
    */
  protected void checkForFlags() {
    UI = tutorial.UI();
    pushMessage(EVENT_WELCOME, true, true);
    
    checkMotion();
    if (motionDone) pushMessage("Navigation Done", true, true);
    
    if (isViewing("The Bastion")) {
      UI.tracking.lockOn(tutorial.bastion);
      pushMessage("Exploring"   , true, false);
      pushMessage("Construction", true, false);
    }
  }
  
  
  private void pushMessage(String eventKey, boolean urgent, boolean viewNow) {
    final ReminderListing reminders = UI.reminders();
    if (! reminders.hasMessageEntry(eventKey)) {
      final DialoguePane message = messageFor(eventKey, UI);
      if (message == null) return;
      I.say("PUSHING NEW MESSAGE: "+eventKey);
      if (viewNow) UI.setInfoPanels(message, null);
      reminders.addEntry(message, urgent);
    }
  }
  
  
  private boolean isViewing(String messageKey) {
    return messageKey.equals(UI.reminders().onScreenMessageKey());
  }
  
  
  
  private boolean motionDone = false;
  private Tile startAt = null;
  
  protected boolean checkMotion() {
    if (motionDone) return true;
    
    final Vec3D lookPoint = tutorial.UI().rendering.view.lookedAt;
    final Tile  lookTile  = tutorial.world().tileAt(lookPoint.x, lookPoint.y);
    if (lookTile == null) return false;
    
    if (startAt == null) startAt = lookTile;
    if (Spacing.distance(lookTile, startAt) < 4) return false;
    return motionDone = true;
  }
  
  
  
  
  
  protected boolean checkFood() {
    return false;
  }
  
  
  protected boolean checkFunding() {
    return false;
  }
  
  
  protected boolean checkSafety() {
    return false;
  }
  
  
  protected boolean checkHealth() {
    return false;
  }
  
  
  protected boolean checkHousing(int houseLevel) {
    return false;
  }
  
  
  protected boolean checkSupply(Traded traded) {
    return false;
  }
}

