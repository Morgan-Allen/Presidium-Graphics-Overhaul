


package stratos.user;
import stratos.game.common.*;
import stratos.game.politic.Power;  //  TODO:  Not the best package?
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;

import com.badlogic.gdx.Input.Keys;



public class PowersPane extends SelectionInfoPane {
  
  
  private static OptionList optionList;
  
  
  public PowersPane(BaseUI UI) {
    super(UI, null, false, false);
  }
  
  
  protected void updateText(
    BaseUI UI, Text headerText, Text detailText, Text listingText
  ) {
    headerText.setText("Powers");
    //  TODO:  You need to be able to avoid specifying a focus from the start
    //         in order to use this pane.
    
    /*
    for (final Power power : Power.BASIC_POWERS) {
      final PowerButton button = new PowerButton(UI, power, this);
      detailText.insert(button, BAR_BUTTON_SIZE, true);
    }
    //*/
  }
  
  
  private static void dismissOptions() {
    if (optionList == null) return;
    optionList.detach();
    optionList = null;
  }
  
  
  
  /**
    */
  public static class PowerButton extends Button {
    
    final Power  power;
    final Target focus;
    final UINode bar  ;
    final BaseUI UI   ;
    
    
    public PowerButton(BaseUI UI, Power p, Target focus, UINode parent) {
      super(
        UI, p.buttonImage, CIRCLE_LIT,
        p.name.toUpperCase()+"\n  "+p.helpInfo
      );
      this.power = p;
      this.focus = focus;
      this.UI    = UI;
      this.bar   = parent;
      
      if (focus == null) I.complain("NO SUBJECT!");
    }
    
    
    protected void whenClicked() {
      if (! enabled) return;
      final BaseUI UI = BaseUI.current();
      //
      //  If another such task was ongoing, dismiss it.  (If this was the same
      //  task, you can quit.)
      if (optionList != null) {
        final Power belongs = optionList.parent.power;
        dismissOptions();
        if (belongs == power) return;
      }
      if (UI.currentTask() instanceof PowerTask) {
        final PowerTask task = (PowerTask) UI.currentTask();
        task.cancelTask();
        if (task.power == power) return;
      }
      //
      //  If there are options, display them instead.
      final Actor caster = UI.played().ruler();
      final String options[] = power.options();
      if (options != null) {
        optionList = new OptionList(this, options);
        optionList.alignToMatch(this);
        optionList.alignBottom(BAR_BUTTON_SIZE + 10, 0);
        optionList.attachTo(bar);
        return;
      }
      else {
        final PowerTask task = new PowerTask(UI, power, null, focus, caster);
        UI.beginTask(task);
      }
    }
    
    
    protected void updateState() {
      this.enabled = true;
      super.updateState();
    }
    
    
    protected String disableInfo() {
      return "  (Unavailable: No governor)";
    }
  }
  
  
  /**
    * 
    */
  private static class OptionList extends UIGroup {
    
    final PowerButton parent;
    
    OptionList(
      final PowerButton parent, String options[]
    ) {
      super(parent.UI);
      this.parent = parent;
      final BaseUI UI = parent.UI;
      
      float maxWide = 0;
      final Batch <Text> links = new Batch <Text> ();
      
      for (final String option : options) {
        final Text text = new Text(UI, UIConstants.INFO_FONT);
        text.append(new Description.Link(option) {
          public void whenClicked() {
            final Actor caster = UI.played().ruler();
            final PowerTask task = new PowerTask(
              UI, parent.power, option, parent.focus, caster
            );
            UI.beginTask(task);
            dismissOptions();
          }
        }, Colour.GREY);

        text.setToPreferredSize(1000);
        maxWide = Nums.max(maxWide, text.preferredSize().xdim());
        links.add(text);
      }
      
      int i = 0; for (Text text : links) {
        final Bordering bordering = new Bordering(UI, MessagePopup.BLACK_BAR);
        bordering.setInsets(20, 20, 10, 10);
        bordering.setUV(0.33f, 0.33f, 0.5f, 0.5f);
        bordering.attachTo(this);
        
        text.alignBottom(i++ * 20, 16           );
        text.alignLeft  (10      , (int) maxWide);
        text.attachTo(this);
        bordering.alignToMatch(text, 10, 2);
      }
    }
  }
  
  
  /**  
    */
  private static class PowerTask implements UITask {
    
    final BaseUI UI;
    final Power  power;
    final String option;
    final Actor  caster;
    final Target focus;
    
    final Image preview;
    final static float PS = BAR_BUTTON_SIZE * 0.75f, HPS = PS / 2;
    
    
    PowerTask(BaseUI UI, Power p, String o, Target focus, Actor casts) {
      this.UI     = UI;
      this.power  = p;
      this.option = o;
      this.caster = casts;
      this.focus  = focus;
      
      if (focus == null) I.complain("NO SUBJECT!");
      
      //  TODO:  CREATE A DEDICATED CURSOR CLASS.
      preview = new Image(UI, power.buttonImage);
      preview.blocksSelect = false;
      preview.attachTo(UI);
      preview.relAlpha = 0.5f;
    }
    
    
    public void doTask() {
      Object hovered = UI.selection.hovered();
      if (hovered == null) hovered = UI.selection.pickedTile();
      
      preview.alignToArea(
        (int) (UI.mouseX() - HPS),
        (int) (UI.mouseY() - HPS),
        (int) PS, (int) PS
      );
      
      if (! (hovered instanceof Target)) hovered = null;
      final Target picked = (Target) hovered;
      if (power.finishedWith(caster, option, focus, picked)) {
        cancelTask();
      }
    }
    
    
    public void cancelTask() {
      UI.endCurrentTask();
      preview.detach();
    }
    
    
    public ImageAsset cursorImage() {
      return null;
      // power.buttonImage;
    }
    
    
    public String toString() {
      return "Casting "+power.name;
    }
  }
}





