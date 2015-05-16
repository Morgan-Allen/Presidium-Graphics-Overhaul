

package stratos.user.notify;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;


//  TODO:  List a red-green-amber indicator for approval status.


public class MissionReminder extends ReminderListing.Entry {
  
  final Mission m;
  final List <Image> appImgs = new List <Image> ();
  private Batch <Actor> applied = new Batch <Actor> ();
  
  
  MissionReminder(final BaseUI BUI, final Mission m) {
    super(BUI, m, 40, 40);
    this.m = m;
    
    final Composite portrait = m.portrait(BUI);
    final Button button = new Button(
      BUI, m.fullName(),
      portrait.texture(), Button.CIRCLE_LIT.asTexture(), m.fullName()
    ) {
      protected void whenClicked() {
        BUI.selection.pushSelection(m);
      }
    };
    button.alignVertical  (0, 0);
    button.alignHorizontal(0, 0);
    button.attachTo(this);
    
    final BorderedLabel label = new BorderedLabel(BUI);
    label.alignLeft  ( 0 , 0);
    label.alignBottom(-DEFAULT_MARGIN, 0);
    label.text.scale = SMALL_FONT_SIZE;
    label.setMessage(m.fullName(), false, 0);
    label.attachTo(this);
  }
  
  
  private void updateApplicantsShown() {
    final BaseUI BUI = (BaseUI) UI;
    
    for (Image i : appImgs) i.detach();
    appImgs.clear();
    
    int n = 0; for (final Actor a : applied) {
      final Composite AP = a.portrait(BUI);
      if (AP == null) continue;
      final Image AI = new Image(BUI, AP.texture()) {
        protected String info() { return a.fullName(); }
      };
      AI.blocksSelect = true;
      final int size = MIN_WIDGET_SIZE;
      AI.alignTop(0, size);
      AI.alignRight(++n * -size, size);
      appImgs.add(AI);
      AI.attachTo(this);
    }
  }
  
  
  protected void updateState() {
    if (! m.applicants().contentsMatch(applied)) {
      applied.clear();
      Visit.appendTo(applied, m.applicants());
      updateApplicantsShown();
    }
    super.updateState();
  }
}







