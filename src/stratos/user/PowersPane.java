


package stratos.user;
import stratos.game.common.*;
import stratos.game.politic.Power;  //  TODO:  Not the best package?
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;
import com.badlogic.gdx.Input.Keys;



public class PowersPane extends SelectionInfoPane {
  
  

  private OptionList optionList;
  
  
  public PowersPane(BaseUI UI) {
    super(UI, null, false, false);
  }
  
  
  protected void updateText(
    BaseUI UI, Text headerText, Text detailText, Text listingText
  ) {
    headerText.setText("Powers");
    
    for (final Power power : Power.BASIC_POWERS) {
      final PowerButton button = new PowerButton(UI, power, this);
      detailText.insert(button, BAR_BUTTON_SIZE, true);
    }
  }
  
  
  private void dismissOptions() {
    if (optionList == null) return;
    optionList.detach();
    optionList = null;
  }
  
  
  protected void updateState() {
    if (KeyInput.wasTyped(Keys.ESCAPE)) {
      dismissOptions();
    }
    super.updateState();
  }
  

  
  
  /**
    */
  private class PowerButton extends Button {
    
    final Power  power;
    final UINode bar  ;
    final BaseUI UI   ;
    
    
    PowerButton(BaseUI UI, Power p, UINode b) {
      super(
        UI, p.buttonImage,
        p.name.toUpperCase()+"\n  "+p.helpInfo
      );
      this.UI = UI;
      this.power = p;
      this.bar   = b;
    }
    
    
    protected void whenClicked() {
      if (! enabled) return;
      final BaseUI UI = BaseUI.current();
      //
      //  If another such task was ongoing, dismiss it.  (If this was the same
      //  task, you can quit.)
      if (optionList != null) {
        final Power belongs = optionList.power;
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
        optionList = new OptionList(UI, bar, power, options);
        optionList.alignToMatch(this);
        optionList.alignBottom(BAR_BUTTON_SIZE + 10, 0);
        optionList.attachTo(bar);
        return;
      }
      else if (power.finishedWith(caster, null, null, true)) {
        return;
      }
      else {
        final PowerTask task = new PowerTask(bar, power, null, caster);
        UI.beginTask(task);
      }
    }
    
    
    protected void updateState() {
      this.enabled = true;
      //  TODO:  Restore this dependancy (except for remembrance and
      //  foresight?)
      ///this.enabled = BaseUI.currentPlayed().ruler() != null;
      super.updateState();
    }
    
    
    protected String disableInfo() {
      return "  (Unavailable: No governor)";
    }
  }
  
  
  /**
    * 
    */
  private class OptionList extends UIGroup {
    
    final Power power;
    
    OptionList(
      final BaseUI UI, final UINode bar, final Power power, String options[]
    ) {
      super(UI);
      this.power = power;
      
      float maxWide = 0;
      
      final Batch <Text> links = new Batch <Text> ();
      for (final String option : options) {
        final Text text = new Text(UI, UIConstants.INFO_FONT);
        text.append(new Description.Link(option) {
          public void whenClicked() {
            final Actor caster = UI.played().ruler();
            final PowerTask task = new PowerTask(bar, power, option, caster);
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
  class PowerTask implements UITask {
    
    final Power power;
    final String option;
    final Actor caster;
    final Image preview;
    
    final float PS = BAR_BUTTON_SIZE * 0.75f, HPS = PS / 2;
    
    
    PowerTask(UINode bar, Power p, String o, Actor c) {
      power = p;
      option = o;
      caster = c;
      //  TODO:  CREATE A DEDICATED CURSOR CLASS.
      preview = new Image(UI, power.buttonImage);
      preview.blocksSelect = false;
      preview.attachTo(UI);
      preview.relAlpha = 0.5f;
    }
    
    
    public void doTask() {
      final boolean clicked = UI.mouseClicked();
      Object hovered = UI.selection.hovered();
      if (hovered == null) hovered = UI.selection.pickedTile();
      
      preview.alignToArea(
        (int) (UI.mouseX() - HPS),
        (int) (UI.mouseY() - HPS),
        (int) PS, (int) PS
      );

      if (! (hovered instanceof Target)) hovered = null;
      final Target picked = (Target) hovered;
      if (power.finishedWith(caster, option, picked, clicked)) {
        cancelTask();
      }
    }
    
    
    public void cancelTask() {
      UI.endCurrentTask();
      preview.detach();
    }
    
    //  TODO:  RESTORE THIS
    public ImageAsset cursorImage() { return null; }// power.buttonImage; }
  }
  
  
}