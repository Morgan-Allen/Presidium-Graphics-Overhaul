


package stratos.util;

import java.io.*;
import org.apache.commons.math3.util.FastMath;


public class Bitmap {
  
  
  final int wide, high;
  final byte data[];
  
  
  public Bitmap(int wide, int high) {
    this.wide = wide;
    this.high = high;
    this.data = new byte[(int) FastMath.ceil(wide * high / 8f)];
  }
  
  
  public void loadFrom(DataInputStream in) throws Exception {
    in.read(data);
  }
  
  
  public void saveTo(DataOutputStream out) throws Exception {
    out.write(data);
  }
  
  
  public boolean getVal(int x, int y) {
    final int
      scan  = (x * high) + y,
      index = scan / 8,
      mask  = 1 << (scan % 8);
    return (data[index] & mask) != 0;
  }
  
  
  public void setVal(int x, int y, boolean val) {
    final int
      scan  = (x * high) + y,
      index = scan / 8,
      mask  = 1 << (scan % 8);
    
    byte chunk = data[index];
    chunk = (byte) ((chunk & ~ mask) | (val ? mask : 0));
    data[index] = chunk;
  }
}  






