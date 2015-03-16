

package stratos.user;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.game.actors.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;


//  TODO:  Make this a general Household/Advisors pane?  Hmm.  Maybe.  Or save
//  this for the Migrations info-tab of the Planet/Sectors pane?  Yeah.  I
//  reckon so.

//  Advisors:  Ruler.  Household.  Demand and Finances.
//  Sectors:   Migrations.  Relations.  Homeworld.


public class RosterPane extends SelectionInfoPane {
  
  
  final static ImageAsset
    ROSTER_ICON = ImageAsset.fromImage(
      RosterPane.class, "media/GUI/Panels/roster_tab.png"
    ),
    ROSTER_ICON_LIT = Button.CIRCLE_LIT;
  
  final static String
    CAT_APPLIES = "APPLICANTS",
    CAT_CURRENT = "CURRENT"   ,
    ALL_CATS[] = { CAT_APPLIES, CAT_CURRENT };
  
  
  public RosterPane(BaseUI UI) {
    super(UI, null, false, false, ALL_CATS);
  }
  
  
  static UINode createButton(final BaseUI baseUI) {
    
    final RosterPane pane = new RosterPane(baseUI);
    final UIGroup tab = new UIGroup(baseUI);
    final BorderedLabel appsLabel = new BorderedLabel(baseUI);
    
    final Button button = new Button(
      baseUI, ROSTER_ICON, ROSTER_ICON_LIT, "Base Roster"
    ) {
      protected void whenClicked() {
        if (baseUI.currentPane() == pane) {
          baseUI.setInfoPanels(null, null);
        }
        else {
          baseUI.setInfoPanels(pane, null);
        }
      }
      
      protected void updateState() {
        int numApps = pane.listApplied(baseUI.played()).size();
        String message = ""+numApps;
        appsLabel.setMessage(message, false, 0);
        appsLabel.hidden = numApps == 0;
      }
    };
    button.stretch = false;
    button.alignAcross(0, 1);
    button.alignDown  (0, 1);
    button.attachTo(tab);
    
    appsLabel.alignLeft(DEFAULT_MARGIN , 0);
    appsLabel.alignTop (MIN_WIDGET_SIZE, 0);
    appsLabel.attachTo(tab);
    
    return tab;
  }
  
  
  
  private Batch <Actor> listApplied(Base base) {
    final Batch <Actor> applied = new Batch <Actor> ();
    for (Actor a : base.commerce.allCandidates()) {
      final FindWork findWork = (FindWork) a.matchFor(FindWork.class, false);
      if (findWork == null || ! findWork.canOrDidApply()) continue;
      applied.add(a);
    }
    return applied;
  }
  
  
  protected void updateText(
    BaseUI UI, Text headerText, Text detailText, Text listingText
  ) {
    super.updateText(UI, headerText, detailText, listingText);
    final Base base = UI.played();
    final Description d = detailText;
    
    if (category() == CAT_APPLIES) {
      detailText.append("\nOffworld Applicants:");
      for (Actor a : listApplied(base)) {
        
        final FindWork findWork = (FindWork) a.matchFor(FindWork.class, false);
        final Background sought = findWork.position();
        VenuePane.descApplicant(a, sought, detailText, UI);
        
        d.append("\n  Applying at: ");
        d.append(findWork.employer());
        d.append("\n  ");
        final String hireDesc = "Hire for "+findWork.hiringFee()+" credits";
        d.append(new Description.Link(hireDesc) {
          public void whenClicked() {
            findWork.employer().staff().confirmApplication(findWork);
          }
        });
      }
    }
    if (category() == CAT_CURRENT) {
      detailText.append("\nCurrent roster:");
      
      for (Object m : base.world.presences.matchesNear(base, null, -1)) {
        final Venue v = (Venue) m;
        if (v.staff.workforce() == 0) continue;
        detailText.append("\n\n  ");
        detailText.append(v);
        for (Actor a : v.staff.workers()) {
          detailText.append("\n    ");
          detailText.append(a);
          detailText.append(" ("+a.vocation()+")");
        }
      }
    }
  }
}













