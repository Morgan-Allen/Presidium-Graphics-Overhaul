


package stratos.start;

import stratos.graphics.common.*;
import stratos.graphics.solids.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import com.badlogic.gdx.*;
import com.badlogic.gdx.Input.Keys;
import java.io.*;



public class SkinsPreview extends VisualDebug {
  
  
  public static void main(String a[]) {
    PlayLoop.setupAndLoop(new SkinsPreview(), "stratos.graphics");
  }
  

  final static int MAX_PATH_LEN = 100;
  final static char[] VALID_KEYS =
    "abcdefghijklmnopqrstuvwxyz._/01234567890"
  .toCharArray();
  
  private HUD UI;
  private Text modelPathEntry;
  private String lastValidPath = "";
  private String currentPath = "media/Actors/artilects/ArtilectModels.xml";
  
  private XML currentXML;
  private SolidModel currentModel;
  private String currentAnim;
  private boolean shouldLoop;
  
  private Table <String, String> assetStamps = new Table <String, String> ();
  private boolean willReload = false, reloadNow = false;
  
  
  
  protected void loadVisuals() {
    UI = new HUD(PlayLoop.rendering());
    
    modelPathEntry = new Text(UI, BaseUI.INFO_FONT);
    modelPathEntry.alignTop(0, 500);
    modelPathEntry.alignHorizontal(0, 0);
    modelPathEntry.attachTo(UI);
    
    shouldLoop = true;
  }
  
  
  public HUD UI() {
    return UI;
  }
  
  
  public void renderVisuals(Rendering rendering) {
    updatePath();
    updateModel();
    checkAssetsChange();
    setupText();
    super.renderVisuals(rendering);
  }
  
  
  protected void onRendering(Sprite sprite) {
    sprite.setAnimation(currentAnim, Rendering.activeTime() % 1, shouldLoop);
  }
  
  
  
  
  /**  Helper method implementations-
    */
  private void updatePath() {
    //
    //  Firstly, filter the set of pressed keys for valid path-characters, and
    //  tack them on at the end of the path.
    for (char k : KeyInput.keysTyped()) {
      boolean valid = false;
      for (char c : VALID_KEYS) {
        if (c == k) valid = true;
        if (c == Character.toLowerCase(k)) valid = true;
      }
      if (currentPath.length() >= MAX_PATH_LEN || ! valid) continue;
      currentPath = currentPath+""+k;
    }
    //
    //  If backspace is typed, trim the path down again.
    if (KeyInput.wasTyped(Keys.DEL) && currentPath.length() > 0) {
      currentPath = currentPath.substring(0, currentPath.length() - 1);
    }
  }
  
  
  private void updateModel() {
    //
    //  First of all, see if a valid file has been specified with this path-
    final File match = new File(currentPath);
    if (currentPath.equals(lastValidPath) || ! match.exists()) return;
    
    if (currentPath.endsWith(".ms3d")) {
      if (switchModel(match.getParent()+"/", match.getName(), null, null)) {
        lastValidPath = currentPath;
        currentXML = null;
      };
    }
    
    if (currentPath.endsWith(".xml")) {
      try {
        currentXML = XML.load(currentPath);
        lastValidPath = currentPath;
        switchToEntry(currentXML.allChildrenMatching("model")[0]);
      }
      catch(Exception e) { I.report(e); currentPath = ""; }
    }
  }
  
  
  private void setupText() {
    final Text t = modelPathEntry;
    t.setText("Enter File Path: "+currentPath);
    t.append("\n  Last Valid Path: "+lastValidPath);
    
    //
    //  If the current path refers to an xml entry, list the available files-
    if (currentXML != null) {
      t.append("\n\nFile entries:");
      
      for (final XML entry : currentXML.allChildrenMatching("model")) {
        t.append("\n  ");
        final String name = entry.value("name");
        t.append(new Text.Clickable() {
          public void whenClicked() { switchToEntry(entry); }
          public String fullName() { return name; }
        });
      }
    }
    
    //
    //  If the current model has animations, list those too-
    if (currentModel != null) {
      t.append("\n\nModel animations:");
      
      t.append("\n  ");
      t.append(new Text.Clickable() {
        public void whenClicked() { currentAnim = null; }
        public String fullName() { return "(NONE)"; }
      });
      
      for (final String animName : currentModel.animNames()) {
        t.append("\n  ");
        t.append(new Text.Clickable() {
          public void whenClicked() { currentAnim = animName; }
          public String fullName() { return animName; }
        });
      }
    }
    
    //
    //  
    t.append("\n\nShould loop: ");
    t.append(new Text.Clickable() {
      public void whenClicked() { shouldLoop = ! shouldLoop; }
      public String fullName() { return shouldLoop ? "TRUE" : "FALSE"; }
    });
  }
  
  
  private boolean switchToEntry(XML entry) {
    final File match = new File(lastValidPath);
    final String
      path    = match.getParent()+"/",
      fileXML = match.getName();
    final String
      name = entry.value("name"),
      file = entry.value("file");
    return switchModel(path, file, fileXML, name);
  }
  
  
  private boolean switchModel(
    String path, String file, String fileXML, String nameXML
  ) {
    if (path == null || file == null) return false;
    final File match = new File(path+file);
    if (! match.exists()) return false;
    
    //
    //  Firstly, see if it's possible to load a new model from the given path
    //  and file strings.
    SolidModel newModel = null;
    try {
      final SolidModel model = MS3DModel.loadFrom(
        path, file, SkinsPreview.class, fileXML, nameXML
      );
      Assets.loadNow(model);
      if (model != null) newModel = model;
    }
    catch(Exception e) {
      I.say("ERROR LOADING "+path+file);
      I.report(e);
      currentPath = "";
    }

    //
    //  If the file loads successfully, then dispose of the old model and
    //  create a sprite based on the new.  Otherwise, return false.
    if (newModel != null) {
      Assets.disposeOf(currentModel);
      currentModel = newModel;
      assetStamps.clear();
      sprites.clear();
      sprites.add(currentModel.makeSprite());
      return true;
    }
    return false;
  }
  
  
  private void checkAssetsChange() {
    if (currentModel == null) return;
    boolean shouldReload = false;
    
    if (! willReload) for (String s : currentModel.importantFiles()) {
      final File asset = new File(s);
      if (! asset.exists()) continue;
      final String stamp = ""+asset.lastModified();
      final String oldVal = assetStamps.get(s);
      if (oldVal != null && ! oldVal.equals(stamp)) shouldReload = true;
      assetStamps.put(s, stamp);
    }
    
    if (shouldReload) {
      willReload = true;
      //  We insert a short time-delay here, so that the file has time to
      //  finish being written to disc.
      new Thread() {
        public void run() {
          try { Thread.sleep(500); }
          catch (Exception e) {}
          reloadNow = true;
        }
      }.start();
    }
    
    if (reloadNow) {
      Assets.disposeOf(currentModel);
      Assets.loadNow(currentModel);
      sprites.clear();
      sprites.add(currentModel.makeSprite());
      willReload = false;
      reloadNow  = false;
    }
  }
}



