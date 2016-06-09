/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.widgets;
import stratos.start.*;
import stratos.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Pixmap.Blending;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import java.io.*;



public class Alphabet extends Assets.Loadable {
  
  
  private String path, mmlFile;
  private Texture fontTex;
  private Letter letters[], map[];
  
  public static class Letter {
    public char map;
    public float
      umin,
      vmin,
      umax,
      vmax,
      width,
      height;
  }
  
  
  private Alphabet(
    Class sourceClass, String fontID, String path, String mmlFile
  ) {
    super(fontID, sourceClass, false);
    this.path    = path   ;
    this.mmlFile = mmlFile;
  }
  
  
  public static Alphabet loadAlphabet(
    Class sourceClass, String fontID, String path, String mmlFile
  ) {
    return new Alphabet(sourceClass, fontID, path, mmlFile);
  }
  

  protected State loadAsset() {
    path = Assets.safePath(path);
    XML info = (XML.load(path + mmlFile)).child(0);
    String
      texFile   = info.value("texture"),
      alphaFile = info.value("alpha"  ),
      mapFile   = info.value("mapping");
    final int
      numLines = Integer.parseInt(info.value("lines")),
      lineHigh = Integer.parseInt(info.value("lhigh"));
    //
    //  Our first task is to load the colour and alpha values for the alphabet
    //  texture, and merge them together.
    final Pixmap
      valueMap = new Pixmap(Gdx.files.internal(path+texFile  )),
      alphaMap = new Pixmap(Gdx.files.internal(path+alphaFile));
    final int
      wide = valueMap.getWidth(),
      high = valueMap.getHeight();
    
    Pixmap.setBlending(Blending.None);
    for (Coord c : Visit.grid(0, 0, wide, high, 1)) {
      int value = valueMap.getPixel(c.x, c.y);
      int alpha = alphaMap.getPixel(c.x, c.y);
      value =  value & 0xffffff00;
      alpha = (alpha & 0xff000000) >> 24;
      value = value | alpha;
      valueMap.drawPixel(c.x, c.y, value);
    }
    fontTex = new Texture(valueMap);
    fontTex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
    
    //
    //  Secondly, we load up the sequence of characters that the alphabet is
    //  mapped to-
    int charMap[] = null;
    try {
      final InputStream mapInput = Gdx.files.internal(path+mapFile).read();
      charMap = new int[mapInput.available()];
      for(int n = 0; n < charMap.length; n++) charMap[n] = mapInput.read();
    }
    catch (IOException e) { I.report(e); return state = State.ERROR; }
    
    //
    //  Finally, we scan the alpha values of the alphabet texture to establish
    //  where breaks occur between letters, and set up UV mappings accordingly.
    List <Letter> scanned = new List <Letter> ();
    boolean scan = true;
    Letter letter = null;
    int maxMap = 0;
    
    for (int line = 0; line < numLines; line++) {
      final int y = line * lineHigh;
      for (int x = 0; x < wide; x++) {
        final int alphaBits = alphaMap.getPixel(x, y) & 0xff;
        if (alphaBits != 0) {
          if (scan) {
            //...a letter starts here.
            scan = false;
            letter = new Letter();
            letter.umin = ((float) x) / wide;
            letter.map = (char) (charMap[scanned.size()]);
            if (letter.map > maxMap) maxMap = letter.map;
          }
        }
        else {
          if(! scan) {
            //...and ends afterward on the first transparent pixel.
            scan = true;
            letter.umax = ((float) x) / wide;
            letter.vmin = ((float) y) / high;
            letter.vmax = ((float) y + lineHigh) / high;
            letter.height = lineHigh;
            letter.width = (int) ((letter.umax - letter.umin) * wide);
            scanned.addLast(letter);
          }
        }
      }
    }
    
    map     = new Letter[maxMap + 1];
    letters = new Letter[scanned.size()];
    int ind = 0;
    for (Letter sLetter : scanned) {
      map[(int) (sLetter.map)] = letters[ind++] = sLetter;
    }
    return state = State.LOADED;
  }
  
  
  protected State disposeAsset() {
    if (fontTex != null) fontTex.dispose();
    map     = new Letter[0];
    letters = new Letter[0];
    return state = State.DISPOSED;
  }
  
  


  public Letter letterFor(char c) {
    if (c < 0 || c >= map.length) return null;
    return map[c];
  }
  
  
  public float lineHeight() {
    return letterFor(' ').height;
  }
  
  
  public Texture texture() {
    return fontTex;
  }
}







