


package stratos.graphics.common;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import stratos.graphics.widgets.*;
import stratos.util.*;



public class LoadingScreen {
  
  
  final static String
    TITLE_IMG_PATH     = "media/GUI/title_image.png",
    BLANK_IMG_PATH     = "media/GUI/blank_back.png",
    PROG_FILL_IMG_PATH = "media/GUI/prog_fill.png",
    PROG_BACK_IMG_PATH = "media/GUI/prog_back.png";
  
  final public static HUD HUD = new HUD();
  final static ProgressBar progBar;
  static {
    
    final Image blankImage = new Image(HUD, BLANK_IMG_PATH);
    blankImage.relBound.set(0, 0, 1, 1);
    blankImage.stretch = true;
    blankImage.attachTo(HUD);
    
    final Image titleImage = new Image(HUD, TITLE_IMG_PATH);
    titleImage.relBound.set(0.5f, 0.5f, 0, 0);
    titleImage.expandToTexSize(1, true);
    titleImage.lockToPixels = true;
    titleImage.attachTo(HUD);
    
    progBar = new ProgressBar(
      HUD, PROG_FILL_IMG_PATH, PROG_BACK_IMG_PATH
    );
    progBar.relBound.set(0.15f, 0.25f, 0.7f, 0);
    progBar.absBound.set(0, 0, 0, 25);
    progBar.attachTo(HUD);
    
    initDone = true;
  }
  
  private static boolean initDone = false;
  
  
  public static void update(String label, float progress) {
    progBar.progress = progress;
  }
}



