

package stratos.user.notify;
import stratos.game.common.*;
import stratos.game.politic.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;


//  TODO:  List a red-green-amber indicator for approval status.


public class MissionEntry extends UIGroup {
  
  final Mission m;
  final List <Image> appImgs = new List <Image> ();
  private Batch <Actor> applied = new Batch <Actor> ();
  
  
  MissionEntry(final BaseUI BUI, final Mission m) {
    super(BUI);
    this.m = m;

    final Composite portrait = m.portrait(BUI);
    final Button button = new Button(
      BUI, portrait.texture(), Button.CIRCLE_LIT.asTexture(), m.fullName()
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
    label.alignBottom(-10, 0);
    label.text.scale = 0.75f;
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
      AI.alignTop(0, 20);
      AI.alignRight(++n * -20, 20);
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







