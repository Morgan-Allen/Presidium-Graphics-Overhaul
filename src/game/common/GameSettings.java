/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.common ;



public class GameSettings {
  
  //
  //  TODO:  It's possible that these should be base or scenario-specific,
  //  rather than global...
  
  public static boolean
    
    buildFree = false,
    hireFree  = false,
    psyFree   = false,
    fogFree   = false,
    
    pathFree  = false,
    
    hardCore  = false ;
  
  
  
  protected static void saveSettings(Session s) throws Exception {
    s.saveBool(buildFree) ;
    s.saveBool(hireFree) ;
    s.saveBool(psyFree) ;
    s.saveBool(fogFree) ;
    s.saveBool(pathFree) ;
    s.saveBool(hardCore) ;
  }
  
  protected static void loadSettings(Session s) throws Exception {
    buildFree = s.loadBool() ;
    hireFree = s.loadBool() ;
    psyFree = s.loadBool() ;
    fogFree = s.loadBool() ;
    pathFree = s.loadBool() ;
    hardCore = s.loadBool() ;
  }
}

