/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.actors;
import stratos.game.common.*;



//  TODO:  MERGE THIS WITH THE CAREER CLASS- WHICH IS IS ESSENTIALLY A SET OF
//  MEMORIES ABOUT YOUR PAST?


public class Memory implements Session.Saveable, Accountable {
  
  
  Class planClass;
  Session.Saveable subject;
  //  TODO:  Also time and emotional impact.
  
  
  
  public static Memory loadConstant(Session s) throws Exception {
    final Memory m = new Memory();
    m.planClass = s.loadClass();
    m.subject   = s.loadObject();
    return m;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveClass(planClass);
    s.saveObject(subject);
  }
  
  
  public Base base() {
    return null;
  }
  
  
}





