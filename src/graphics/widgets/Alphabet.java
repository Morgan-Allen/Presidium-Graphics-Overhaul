/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package graphics.widgets ;
import util.* ;
import graphics.common.* ;
import java.io.* ;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.* ;
import com.badlogic.gdx.files.*;



public class Alphabet {
  
  
  Texture fontTex ;
  Letter letters[], map[] ;
  
  public static class Letter {
    public char map ;
    public float
      umin,
      vmin,
      umax,
      vmax,
      width,
      height ;
  }
  
  
  public static Alphabet loadAlphabet(String path, String mmlFile) {
    path = LoadService.safePath(path) ;
    XML info = (XML.load(path + mmlFile)).child(0) ;
    String
      texFile = info.value("texture"),
      alphaFile = info.value("relAlpha"),
      mapFile = info.value("mapping") ;
    final int
      numLines = Integer.parseInt(info.value("lines")),
      lineHigh = Integer.parseInt(info.value("lhigh")) 
      ;
    return new Alphabet(path, texFile, alphaFile, mapFile, numLines, lineHigh) ;
  }
  
  
  Alphabet(
      String path,
      String valueFile, String alphaFile,
      String mapFile, int numLines, int lineHigh
  ) {
    //
    //  Our first task is to load the colour and alpha values for the alphabet
    //  texture, and merge them together.
    final Pixmap
      valueMap = new Pixmap(Gdx.files.internal(path+valueFile)),
      alphaMap = new Pixmap(Gdx.files.internal(path+alphaFile));
    final int
      wide = valueMap.getWidth(),
      high = valueMap.getHeight();
    
    for (Coord c : Visit.grid(0, 0, wide, high, 1)) {
      final int
        value = valueMap.getPixel(c.x, c.y) & 0xffffff00,
        alpha = (alphaMap.getPixel(c.x, c.y) & 0xff000000) >> 24,
        blend = value | alpha;
      valueMap.drawPixel(c.x, c.y, blend);
    }
    fontTex = new Texture(valueMap);
    fontTex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
    
    //
    //  Secondly, we load up the sequence of characters that the alphabet is
    //  mapped to-
    int charMap[] = null ;
    try {
      final InputStream mapInput = Gdx.files.internal(path+mapFile).read();
      charMap = new int[mapInput.available()] ;
      for(int n = 0 ; n < charMap.length ; n++) charMap[n] = mapInput.read() ;
    }
    catch (IOException e) { I.report(e); return;}
    
    //
    //  Finally, we scan the alpha values of the alphabet texture to establish
    //  where breaks occur between letters, and set up UV mappings accordingly.
    List <Letter> scanned = new List <Letter> ();
    boolean scan = true;
    Letter letter = null;
    int maxMap = 0;
    
    for (int line = 0 ; line < numLines ; line++) {
      final int y = line * lineHigh;
      for (int x = 0 ; x < wide ; x++) {
        final int alpha = alphaMap.getPixel(x, y) & 0xff;
        if (alpha != 0) {
          if (scan) {
            //...a letter starts here.
            scan = false ;
            letter = new Letter() ;
            letter.umin = ((float) x) / wide ;
            letter.map = (char) (charMap[scanned.size()]) ;
            if (letter.map > maxMap) maxMap = letter.map ;
          }
        }
        else {
          if(! scan) {
            //...and ends afterward on the first transparent pixel.
            scan = true ;
            letter.umax = ((float) x) / wide ;
            letter.vmin = ((float) y) / high ;
            letter.vmax = ((float) y + lineHigh) / high ;
            letter.height = lineHigh ;
            letter.width = (int) ((letter.umax - letter.umin) * wide) ;
            scanned.addLast(letter) ;
            I.say("UV for: "+letter.map+", "+letter.umin+"|"+letter.vmin);
          }
        }
      }
    }
    
    map = new Letter[maxMap + 1] ;
    letters = new Letter[scanned.size()] ;
    int ind = 0 ;
    for (Letter sLetter : scanned) {
      map[(int) (sLetter.map)] = letters[ind++] = sLetter ;
    }
  }
}



  //  TODO:  Read it in as a Pixmap initially, then convert to a texture.
  
  /*
  public Alphabet(
    String path,
    String texFile, String alphaFile,
    String mapFile, int numLines, int lineHigh
  ) {
    //  Basic initialisation routine here.  The list of individual characters
    //  must be loaded first for comparison with scanned character blocks.
    int charMap[] = null ;
    final int mask[] = LoadService.getRGBA(path + texFile) ;
    try {
      FileInputStream mapS = new FileInputStream(new File(path + mapFile)) ;
      charMap = new int[mapS.available()] ;
      for(int n = 0 ; n < charMap.length ; n++) charMap[n] = mapS.read() ;
    }
    catch(IOException e) { I.add(" " + e) ; }
    fontTex = Texture.loadTexture(path+texFile, path+alphaFile) ;
    //
    //  This is where the actual scanning is done.  A single texture is used for
    //  all characters to avoid frequent texture re-bindings during rendering.
    //  The procedure looks for 'gaps' in the initial texture relAlpha as cues for
    //  where to demarcate individual letters.
    final int trueSize = fontTex.trueSize() ;
    int x, y, ind, line = 0, maxMap = 0 ;
    boolean scan = true ;
    List <Letter> scanned = new List <Letter> () ;
    Letter letter = null ;
    
    for( ; line < numLines ; line++) {
      y = lineHigh * line ;
      ind = (y * trueSize) + (x = 0) ;
      for( ; x < fontTex.xdim() ; x++, ind++) {
        if((mask[ind] & 0xff000000) != 0) {  //relAlpha value of pixel is nonzero.
          if(scan) {
            scan = false ;  //a letter starts here.
            
            letter = new Letter() ;
            letter.umin = ((float) x) / trueSize ;
            letter.map = (char) (charMap[scanned.size()]) ;
            if (letter.map > maxMap) maxMap = letter.map ;
          }
        }
        else {
          if(! scan) {
            //...and ends afterward on the first transparent pixel.
            scan = true ;
            letter.umax = ((float) x) / trueSize ;
            letter.vmin = ((float) y) / trueSize ;
            letter.vmax = ((float) y + lineHigh) / trueSize ;
            letter.height = lineHigh ;
            letter.width = (int) ((letter.umax - letter.umin) * trueSize) ;
            scanned.addLast(letter) ;
          }
        }
      }
    }
    
    map = new Letter[maxMap + 1] ;
    letters = new Letter[scanned.size()] ;
    ind = 0 ;
    for (Letter sLetter : scanned)
      map[(int) (sLetter.map)] = letters[ind++] = sLetter ;
  }
}
//*/



