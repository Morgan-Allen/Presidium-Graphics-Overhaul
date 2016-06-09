/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.graphics.widgets;
import stratos.util.*;

import com.badlogic.gdx.*;



//  TODO:  Arrange for dedicated bindings of these keys, to ensure there are no
//         conflicts and that all bindings can be listed from a single point (
//         for, e.g, UI purposes.)


public class KeyInput {
  
  
  public static void updateInputs() {
    
    final Batch <Integer> c = lastCodes;
    lastCodes = codes;
    codes = c;
    codes.clear();
    
    final Batch <Character> t = lastTyped;
    lastTyped = typed;
    typed = t;
    typed.clear();
  }
  
  
  private static Batch <Character>
    typed     = new Batch <Character> (),
    lastTyped = new Batch <Character> ();
  private static Batch <Integer>
    codes     = new Batch <Integer> (),
    lastCodes = new Batch <Integer> ();
  
  
  public static char[] keysTyped() {
    char k[] = new char[lastTyped.size()];
    int i = 0;
    for (Character c : lastTyped) k[i++] = c;
    return k;
  }
  
  
  public static boolean isPressed(char k) {
    final String name = (""+k).toUpperCase();
    final int keyCode = Input.Keys.valueOf(name);
    return isPressed(keyCode);
  }
  
  
  public static boolean isPressed(int keyCode) {
    return Gdx.input.isKeyPressed(keyCode);
  }
  
  
  public static boolean wasTyped(char k) {
    for (Character c : lastTyped) {
      if (c == k) return true;
    }
    return false;
  }
  
  
  public static boolean wasTyped(int keyCode) {
    for (Integer i : lastCodes) {
      if (i == keyCode) return true;
    }
    return false;
  }
  
  
  final static InputProcessor IP = new InputProcessor() {
    
    public boolean keyDown(int keycode) {
      codes.add(keycode);
      return false;
    }
    
    public boolean keyTyped(char character) {
      typed.add(character);
      return false;
    }
    
    public boolean keyUp(int keycode) {
      return false;
    }
    
    public boolean mouseMoved(int screenX, int screenY) {
      return false;
    }
    
    public boolean scrolled(int amount) {
      return false;
    }
    
    public boolean touchDown(
      int screenX, int screenY, int pointer, int button
    ) { return false; }
    
    public boolean touchUp(
      int screenX, int screenY, int pointer, int button
    ) { return false; }
    
    public boolean touchDragged(
      int screenX, int screenY, int pointer
    ) { return false; }
  };
  
  static { Gdx.input.setInputProcessor(IP); }
}



