


package src.game.campaign ;
import src.game.common.* ;
import src.user.*;



public class TutorialScenario extends Scenario {
  
  
  
  public TutorialScenario(String saveFile) {
    super(saveFile, false) ;
  }
  
  
  public TutorialScenario(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  //
  //  TODO:  Guide the player through the basics of setting up a settlement,
  //  using missions to explore, defend, and either attack or contact natives
  //  and other local threats.
  //
  //  TODO:  Take this functionality out of the MainMenu class and into this
  //  class.  (Likewise for the campaign-style scenario.)
  
  
  @Override
  protected World createWorld() {
    // TODO Auto-generated method stub
    return null;
  }


  @Override
  protected Base createBase(World world) {
    // TODO Auto-generated method stub
    return null;
  }


  @Override
  protected void configureScenario(World world, Base base, BaseUI UI) {
    // TODO Auto-generated method stub
    
  }


  @Override
  protected String saveFilePrefix(World world, Base base) {
    // TODO Auto-generated method stub
    return null;
  }


  @Override
  protected void afterCreation() {
    // TODO Auto-generated method stub
    
  }
}




