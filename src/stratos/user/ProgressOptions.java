

package stratos.user;
import stratos.start.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;



//
//  An options pane for pause, fast-forward, slow-motion, save and load options.

public class ProgressOptions extends UIGroup implements UIConstants {
  
  
  
  final static String IMG_DIR = "media/GUI/Powers/";
  final static ImageAsset
    PROG_IMAGES[] = ImageAsset.fromImages(
      ProgressOptions.class, "game_progress_buttons", IMG_DIR,
      "progress_save.png"  ,
      "progress_load.png"  ,
      "progress_pause.png" ,
      "progress_slow.png"  ,
      "progress_normal.png",
      "progress_fast.png"
    ),
    IMG_SAVE   = PROG_IMAGES[0],
    IMG_LOAD   = PROG_IMAGES[1],
    IMG_PAUSE  = PROG_IMAGES[2],
    IMG_SLOW   = PROG_IMAGES[3],
    IMG_NORMAL = PROG_IMAGES[4],
    IMG_FAST   = PROG_IMAGES[5];
  
  
  final BaseUI BUI;
  Button saves, loads, pauses, slows, norms, fasts;
  Button lastSpeed = null;
  Table <Button, Float> speedOptions = new Table();
  
  
  ProgressOptions(BaseUI UI) {
    super(UI);
    this.BUI = UI;
    setup();
  }
  

  private void setup() {
    final Batch <UINode> options = new Batch();
    
    this.saves = new Button(
      UI, "button_save", IMG_SAVE, "Save Progress"
    ) {
      protected void whenClicked() {
        Scenario.current().scheduleSave();
      }
    };
    options.add(saves);
    
    this.loads = new Button(
      UI, "button_load", IMG_LOAD, "Revert Progress"
    ) {
      protected void whenClicked() {
        final Scenario current = Scenario.current();
        if (current == null) return;
        if (SaveUtils.latestSave(current.savesPrefix()) == null) {
          current.scheduleReset();
        }
        else {
          current.scheduleReload();
        }
      }
    };
    options.add(loads);
    
    this.pauses = new Button(
      UI, "button_pause", IMG_PAUSE, "Pause Game (F)"
    ) {
      protected void whenClicked() {
        PlayLoop.setPaused(true);
        toggleSpeedOption(this);
      }
    };
    options.add(pauses);
    speedOptions.put(pauses, 0f);

    this.slows = new Button(UI, "button_slow", IMG_SLOW, "Slow Time") {
      protected void whenClicked() {
        PlayLoop.setGameSpeed(0.33f);
        PlayLoop.setPaused(false);
        toggleSpeedOption(this);
      }
    };
    options.add(slows);
    speedOptions.put(slows, 0.33f);
    
    this.norms = new Button(UI, "button_norm", IMG_NORMAL, "Normal Time") {
      protected void whenClicked() {
        PlayLoop.setGameSpeed(1);
        PlayLoop.setPaused(false);
        toggleSpeedOption(this);
      }
    };
    options.add(norms);
    speedOptions.put(norms, 1.0f);
    
    this.fasts = new Button(UI, "button_fast", IMG_FAST, "Fast Time") {
      protected void whenClicked() {
        PlayLoop.setGameSpeed(3.0f);
        PlayLoop.setPaused(false);
        toggleSpeedOption(this);
      }
    };
    options.add(fasts);
    speedOptions.put(fasts, 3.0f);
    
    final int sizeB = OPT_BUTTON_SIZE - OPT_MARGIN;
    int across = PANEL_TAB_SIZE;
    for (UINode option : options) {
      option.setToPreferredSize();
      int imgH = (int) option.absBound().ydim();
      int imgW = (int) option.absBound().xdim();
      int optW = (int) (imgW * (sizeB * 1f / imgH));
      
      option.alignLeft(across, optW);
      option.alignBottom(OPT_MARGIN, sizeB);
      option.attachTo(this);
      across += optW;
    }
  }

  
  protected void updateState() {
    super.updateState();
    
    if (KeyInput.wasTyped('p') || KeyInput.wasTyped('P')) {
      if (! PlayLoop.paused()) pauses.performAction();
      else if (lastSpeed != null) lastSpeed.performAction();
      else PlayLoop.setPaused(false);
    }
    
    final Button speed = closestSpeed();
    toggleSpeedOption(speed);
    if (speed != pauses) lastSpeed = speed;
    
    if (BUI.currentTask() == null && PlayLoop.paused()) {
      BaseUI.setPopupMessage("Game Paused- Hit P to unpause");
    }
  }
  
  
  private Button closestSpeed() {
    float speed = PlayLoop.paused() ? 0 : PlayLoop.gameSpeed();
    final Pick <Button> pick = new Pick();
    for (Button b : speedOptions.keySet()) {
      float val = speedOptions.get(b);
      pick.compare(b, 0 - Nums.abs(val - speed));
    }
    return pick.result();
  }
  
  
  private void toggleSpeedOption(Button picked) {
    if (picked != pauses) {
      lastSpeed = picked;
    }
    for (Button b : speedOptions.keySet()) {
      if (b == picked) b.toggled = true;
      else b.toggled = false;
    }
  }
  
  
}










