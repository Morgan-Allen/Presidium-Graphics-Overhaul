/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.graphics.widgets;
import stratos.util.*;
import com.badlogic.gdx.Gdx;
//import org.lwjgl.input.Keyboard ;


public class KeyInput {
  
  
  static void updateInputs() {
    //Gdx.input.
  }
}
  /*
  final static List <Listener> listeners = new List <Listener> () ;
  final static Table <Character, Integer> pressed = new Table() ;
  private static boolean firstUpdate = true ;
  
  
  public static void clearInputs() {
    pressed.clear() ;
    listeners.clear() ;
    firstUpdate = true ;
  }
  
  
  public static boolean wasKeyPressed(char c) {
    final Integer time = pressed.get(c) ;
    if (time != null && time == 0) return true ;
    return false ;
  }
  
  
  public static boolean isKeyDown(int ID) {
    return Keyboard.isKeyDown(ID) ;
  }
  
  
  public static void updateKeyboard() {
    if (firstUpdate) { firstUpdate = false ; return ; }
    final Batch <Character> justPressed = new Batch(), expired = new Batch() ;
    
    while (Keyboard.next()) {
      final int key = Keyboard.getEventKey() ;
      final boolean down = Keyboard.getEventKeyState() ;
      final char c = Keyboard.getEventCharacter() ;
      
      if (down) {
        Integer time = pressed.get(c) ;
        if (time == null) pressed.put(c, 0) ;
        else pressed.put(c, time + 1) ;
        justPressed.add(c) ;
        
        for (Listener listens : listeners) {
          boolean match = false ;
          if (listens.keys == Listener.ALL_KEYS) match = true ;
          else for (char k : listens.keys) if (k == c) match = true ;
          if (match) listens.pressEvent(c, key) ;
        }
      }
    }
    
    for (Character c : pressed.keySet()) {
      if (! justPressed.includes(c)) expired.add(c) ;
    }
    for (Character c : expired) pressed.remove(c) ;
  }
  
  
  public abstract static class Listener {
    
    final private static char ALL_KEYS[] = {} ;
    
    protected abstract boolean pressEvent(char key, int keyID) ;
    final private char keys[] ;
    
    public Listener() { keys = ALL_KEYS ; }
    public Listener(char k) { keys = new char[] {k} ; }
    public Listener(char k1, char k2) { keys = new char[] {k1, k2} ; }
    public Listener(char k1, char k2, char k3) { keys = new char[] { k1, k2, k3 } ; }
  }
  
  
  public static void removeListener(Listener l) {
    listeners.remove(l) ;
  }
  
  public static void addListener(Listener l) {
    if (! listeners.includes(l)) listeners.addLast(l) ;
  }
  //*/
//}
















