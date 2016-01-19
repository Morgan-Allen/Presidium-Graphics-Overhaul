/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.hooks;
import stratos.game.common.*;
import stratos.game.verse.*;
import stratos.user.notify.MessageScript;
import stratos.util.*;



public class ScenarioTerra extends SectorScenario {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final MessageScript script;
  
  
  public ScenarioTerra(Verse verse) {
    super(StratosSetting.SECTOR_TERRA, verse);
    this.script = new MessageScript(
      this, "src/stratos/content/hooks/ScriptTerra.xml"
    );
  }
  
  
  public ScenarioTerra(Session s) throws Exception {
    super(s);
    script = (MessageScript) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(script);
  }
  
  
  
  /**  Update methods for when off-stage:
    */
  public void updateOffstage() {
  }
  
  
  
  /**  Rendering, debug and interface methods-
    */
  public void describeHook(Description d) {
    final String summary = script.contentForTopic("Summary");
    d.append(summary);
  }
  
}




