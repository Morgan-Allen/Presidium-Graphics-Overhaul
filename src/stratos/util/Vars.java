/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.util;

public class Vars {
  
  public static class Bool {
    public boolean val = false;
    public Bool() {}
    public Bool(boolean b) { val = b; }
  }
  
  public static class Num {
    public float value = 0;
    public Num() {}
    public Num(float f) { value = f; }
  }
  
  public static class Int {
    public int val = 0;
    public Int() {}
    public Int(int i) { val = i; }
  }
  
  public static class Ref <T> {
    public T value = null;
    public Ref() {}
    public Ref(T t) { value = t; }
  }
}
