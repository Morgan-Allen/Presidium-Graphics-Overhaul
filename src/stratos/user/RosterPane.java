

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


//  TODO:  PROBLEM- Actors that have already been hired still appear as being
//  available for hire here.



public class RosterPane extends SelectionPane {
  
  
  final static ImageAsset
    ROSTER_ICON = ImageAsset.fromImage(
      RosterPane.class, "media/GUI/Panels/roster_tab.png"
    ),
    ROSTER_ICON_LIT = Button.CROSSHAIRS_LIT;
  
  final static String
    CAT_APPLIES = "APPLICANTS",
    CAT_CURRENT = "CURRENT"   ,
    ALL_CATS[] = { CAT_APPLIES, CAT_CURRENT };
  
  
  public RosterPane(BaseUI UI) {
    super(UI, null, null, false, false, 0, ALL_CATS);
    setWidgetID(ROSTER_PANE_ID);
  }
  
  
  static UINode createButton(final BaseUI baseUI) {
    
    final RosterPane rosterPane = new RosterPane(baseUI);
    final UIGroup tab = new UIGroup(baseUI);
    final BorderedLabel appsLabel = new BorderedLabel(baseUI);
    
    final Button button = new PaneButton(
      rosterPane, baseUI,
      ROSTER_BUTTON_ID, ROSTER_ICON, ROSTER_ICON_LIT, "Base Roster"
    ) {
      protected void updateState() {
        int numApps = rosterPane.listApplied(baseUI.played()).size();
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
    final boolean report = I.used60Frames && false;
    if (report) I.say("\nListing applicants...");
    
    final Batch <Actor> applied = new Batch <Actor> ();
    for (Actor a : base.commerce.allCandidates()) {
      final FindWork finds = (FindWork) a.matchFor(FindWork.class, false);
      
      if (report) {
        I.say("\n  "+a+" ("+a.mind.vocation()+")");
        I.say("  Application:   "+finds);
        I.say("  Was hired?     "+(finds != null && finds.wasHired()));
        I.say("  Can/did apply? "+(finds != null && finds.canOrDidApply()));
      }
      
      if (finds == null || finds.wasHired() || ! finds.canOrDidApply()) {
        continue;
      }
      applied.add(a);
    }
    return applied;
  }
  
  
  protected void updateText(
    Text headerText, Text detailText, Text listingText
  ) {
    super.updateText(headerText, detailText, listingText);
    final Base base = BaseUI.currentPlayed();
    final Description d = detailText;
    
    if (category() == CAT_APPLIES) {
      detailText.append("Offworld Applicants:");
      for (Actor a : listApplied(base)) {
        final FindWork findWork = (FindWork) a.matchFor(FindWork.class, false);
        VenuePane.descApplicant(a, findWork, detailText, UI);
        d.append("\n  Applying at: ");
        d.append(findWork.employer());
        d.append("\n  ");
      }
    }
    if (category() == CAT_CURRENT) {
      detailText.append("Current roster:");
      float allSalaries = 0;
      
      for (Object m : base.world.presences.matchesNear(base, null, -1)) {
        final Venue v = (Venue) m;
        float venueSalaries = 0;
        if (v.staff.workingOrOffworld() == 0) continue;
        
        detailText.append("\n\n");
        detailText.append(v);
        for (Actor a : v.staff.workers()) {
          detailText.append("\n  ");
          detailText.append(a);
          detailText.append(" ("+a.mind.vocation()+")");
          venueSalaries += base.profiles.salaryPerDay(a);
        }
        
        allSalaries += venueSalaries;
        final String salaryLabel = I.shorten(venueSalaries, 1);
        detailText.append("\n  Salaries per day: "+salaryLabel);
      }
      
      final String salaryLabel = I.shorten(allSalaries, 1);
      detailText.append("\n\nTotal Salaries per day: "+salaryLabel);
    }
  }
}










