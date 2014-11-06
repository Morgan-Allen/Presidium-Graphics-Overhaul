


package stratos.start;
import stratos.game.campaign.*;
import stratos.game.common.Base;
import stratos.game.common.Session;
import stratos.game.common.Stage;
import stratos.user.BaseUI;



//  Ensure that jobseeking is still working after the changes made.

//  Re-work the culture lab- produce either food/organs OR soma/reagents.
//  Set up exclusion zone around kommando lodge, and try to restore animal
//  breeding.
//  Try to arrange that actors will seek out new Holdings if anything existing
//  is at or above half crowding.


//  Introduce the Air Field?  (Bring back the Landing Strip at least.)
//  Have solar banks primarily exist as water supply, not power per se.  The
//  reactor complex can provide most of that.
//  Fix bug with stripping/paving of roads happening at same time.  It doesn't
//  show up often, so just keep an eye out for it.
//  Divide venue description into two panes- one for general status, and the
//  other for specific sub-headings.  Don't bother with spill-over.
//  Rework art for roads, the shield wall, the physician station, the engineer
//  station, the solar bank, and the archives.



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
  
  
  protected String saveFilePrefix(Stage world, Base base) {
    return "debug_tutorial";
  }
  

  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    super.configureScenario(world, base, UI);
    UI.selection.pushSelection(base.ruler(), true);
  }
  
  
  protected boolean showMessages() { return false; }
}


