/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user.notify;
import stratos.game.common.*;
import stratos.user.*;



public interface Messaging extends Session.Saveable {
  
  
  MessagePane loadMessage(Session s, BaseUI UI) throws Exception;
  void saveMessage(MessagePane message, Session s) throws Exception;
  
  void messageWasOpened(MessagePane message, BaseUI UI);
}
