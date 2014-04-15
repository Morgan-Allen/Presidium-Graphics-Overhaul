


package stratos.start;
import stratos.game.campaign.*;
import stratos.game.common.Base;
import stratos.game.common.Session;
import stratos.game.common.World;
import stratos.user.BaseUI;



public class DebugTutorial extends TutorialScenario {
  
  
  public static void main(String s[]) {
    PlayLoop.setupAndLoop(new DebugTutorial());
  }
  
  
  private DebugTutorial() {
    super();
  }
  
  
  public DebugTutorial(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  public void beginGameSetup() {
    super.initScenario("debug_tutorial");
  }
  
  
  protected String saveFilePrefix(World world, Base base) {
    return "debug_tutorial";
  }
  

  protected void configureScenario(World world, Base base, BaseUI UI) {
    super.configureScenario(world, base, UI);
    UI.selection.pushSelection(base.ruler(), true);
  }
  
  
  protected boolean showMessages() { return false; }
}


