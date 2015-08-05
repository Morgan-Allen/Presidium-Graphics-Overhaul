/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user.notify;
import stratos.graphics.widgets.*;
import stratos.graphics.common.*;
import stratos.user.*;
import stratos.util.*;



public abstract class LabelledReminder extends ReminderListing.Entry {
  
  final BaseUI UI;
  final String widgetKey;
  final BorderedLabel label;
  
  private String helpInfo;
  private boolean urgent;
  
  
  LabelledReminder(
    BaseUI UI, Object refers, String widgetKey,
    ImageAsset buttonIcon, ImageAsset buttonLit, String initLabel
  ) {
    super(UI, refers, 60, 40);
    this.UI = UI;
    this.widgetKey = widgetKey;
    this.helpInfo  = initLabel;
    
    final Button button = new Button(UI, widgetKey, buttonIcon, buttonLit, "") {
      
      protected void whenClicked() {
        whenButtonClicked();
      }
      
      protected void render(WidgetsPass pass) {
        super.render(pass);
        if (! urgent) return;
        float flashRate = (Rendering.activeTime() % 2) / 2;
        flashRate *= (1 - flashRate) * 2;
        super.renderTex(highlit, flashRate * absAlpha, pass);
      }
      
      protected String info() {
        return helpInfo;
      }
    };
    button.stretch = false;
    button.alignToFill();
    button.attachTo(this);
    
    label = new BorderedLabel(UI);
    label.alignLeft(0, 0);
    label.alignBottom(-DEFAULT_MARGIN, 0);
    label.text.scale = SMALL_FONT_SIZE;
    label.setMessage(initLabel, false, 0);
    label.attachTo(this);
  }
  
  
  protected abstract void whenButtonClicked();
  
  
  protected void setLabel(String message) {
    label.setMessage(message, false, 0);
  }
  
  
  protected void setHelp(String help) {
    this.helpInfo = help;
  }
  
  
  protected void setUrgent(boolean urgent) {
    this.urgent = urgent;
  }
  
  
  protected boolean urgent() {
    return urgent;
  }
}






