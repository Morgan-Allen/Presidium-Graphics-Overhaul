


package stratos.user;
import stratos.game.base.Mission;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.user.*;
import stratos.util.*;
import stratos.graphics.widgets.*;
import stratos.graphics.common.*;
import static stratos.game.base.Mission.*;



public class MissionPane extends SelectionPane {
  
  
  final BaseUI UI;
  final protected Mission mission;
  private boolean confirmAbort = false;
  
  
  public MissionPane(BaseUI UI, Mission selected) {
    super(UI, selected, selected.portrait(UI), true);
    this.UI = UI;
    this.mission = selected;
  }


  public SelectionPane configOwningPanel() {
    //
    //  Obtain some basic facts about the mission and shorthand variables
    //  first-
    final Description d = detail(), l = listing();
    final List <Actor> applied = mission.applicants();
    final boolean emptyList = applied.size() == 0;
    final boolean canChange = ! mission.hasBegun();
    //
    //  Then, we fill up the left-hand pane with broad mission parameters and
    //  commands:
    describeStatus(mission, canChange, UI, d);
    describeOrders(canChange, d);
    //
    //  And lastly, we fill up the right-hand pane with the list of
    //  applications, and options to confirm or deny them:
    if (emptyList) {
      l.append("Applications: None");
    }
    else listApplicants(mission, applied, canChange, UI, l);
    return this;
  }
  
  
  protected void describeOrders(boolean canChange, Description d) {
    final int type = mission.missionType();
    
    if (confirmAbort) {
      d.append(
        "\n\nNOTE:  If you abort this mission, any reward specified will "+
        "still be paid out to applicants.  Do you still want to abort?",
        Colour.LITE_GREY
      );
      d.append(new Description.Link(" (YES)") {
        public void whenClicked() {
          mission.endMission(true);
        }
      });
      d.append(new Description.Link(" (NO)") {
        public void whenClicked() {
          confirmAbort = false;
        }
      });
      
    }
    else {
      d.append("\nOrders:");
      //  TODO:  CONSIDER REQUIRING CONFIRMATION EVEN FOR PUBLIC MISSIONS?
      final boolean begun = mission.hasBegun(), canBegin =
        mission.rolesApproved() > 0 && canChange && type != TYPE_PUBLIC
      ;
      if (canBegin) d.append(new Description.Link(" (BEGIN)") {
        public void whenClicked() {
          mission.beginMission();
        }
      });
      else d.append(" (BEGIN)", Colour.LITE_GREY);
      d.append(new Description.Link(begun ? " (ABORT)" : " (DISMISS)") {
        public void whenClicked() {
          if (begun) confirmAbort = true;
          else mission.endMission(false);
        }
      });
      
      if (type == TYPE_PUBLIC  ) d.append(
        "\n\nThis is a public contract, open to all comers.",
        Colour.LITE_GREY
      );
      if (type == TYPE_SCREENED) d.append(
        "\n\nThis is a screened mission.  Applicants will be subject to your "+
        "approval before they can embark.",
        Colour.LITE_GREY
      );
      if (type == TYPE_COVERT  ) d.append(
        "\n\nThis is a covert mission.  No agents or citizens will apply "+
        "unless recruited by interview.",
        Colour.LITE_GREY
      );
    }
  }
  
  
  public SelectionPane configPublicPanel() {
    final Description d = detail(), l = listing();
    
    describeStatus(mission, false, UI, d);
    listApplicants(mission, mission.applicants(), false, UI, l);
    return this;
  }
  
  
  public SelectionPane configScreenedPanel() {
    final Description d = detail(), l = listing();
    return this;
  }
  
  
  protected void describeStatus(
    final Mission mission, boolean canChange,
    BaseUI UI, Description d
  ) {
    final Colour FIXED = Colour.LITE_GREY;
    final Base declares = mission.base();
    //
    //  Firstly, declare the mission's patron and current status:
    d.append("Declared by ");
    if (declares.ruler() == null) d.append(declares);
    else d.append(declares.ruler());
    
    d.append("\nStatus:  ");
    if (mission.hasBegun()) d.append("In Progress");
    else d.append("Recruiting");
    //
    //  Secondly, describe the mission type:
    final int type = mission.missionType();
    final String typeDesc = type == TYPE_BASE_AI ? "BASE AI" :
      TYPE_DESC[type]
    ;
    d.append("\nMission Type:  ");
    if (canChange) d.append(new Description.Link(typeDesc) {
      public void whenClicked() {
        mission.setMissionType((type + 1) % LIMIT_TYPE);
      }
    });
    else d.append(typeDesc, FIXED);
    //
    //  Then describe the mission's priority and/or payment.  (We allow payment
    //  increases for public missions at any time, but otherwise only before
    //  being confirmed.)
    d.append("\nReward offered:  ");
    final int priority = mission.assignedPriority();
    String payDesc = (priority == 0 || type == TYPE_BASE_AI) ?  "None" :
      REWARD_AMOUNTS[priority]+" credits"
    ;
    
    final boolean canAdjust = canChange || (
      type == TYPE_PUBLIC && priority < PRIORITY_PARAMOUNT
    );
    if (canAdjust) d.append(new Description.Link(payDesc) {
      public void whenClicked() {
        if (priority == PRIORITY_PARAMOUNT) mission.assignPriority(0);
        mission.assignPriority(priority + 1);
      }
    });
    else d.append(payDesc, FIXED);
    //
    //  And finally, describe the mission objective-
    describeObjective(d, canChange);
  }
  
  
  protected void describeObjective(Description d, boolean canChange) {
    final String allDesc[] = mission.objectiveDescriptions();
    if (allDesc == null || allDesc.length == 0) return;
    canChange &= allDesc.length > 1;
    
    final int object = mission.objective();
    final Colour FIXED = Colour.LITE_GREY;
    final String objectDesc = mission.describeObjective(object);
    
    d.append("\nObjective:  ");
    if (canChange) d.append(new Description.Link(objectDesc) {
      public void whenClicked() {
        mission.setObjective((object + 1) % allDesc.length);
      }
    });
    else d.append(objectDesc, FIXED);
    d.append(mission.subject());
  }
  
  
  protected void listApplicants(
    final Mission mission, List <Actor> applied, boolean canConfirm,
    BaseUI UI, Description d
  ) {
    d.append("Applications:");
    for (final Actor a : applied) {
      d.append("\n  ");
      final Composite portrait = a.portrait(UI);
      if (portrait != null) Text.insert(portrait.texture(), 40, 40, true, d);
      d.append(a);
      d.append(" ("+a.mind.vocation()+")");

      final boolean approved = mission.isApproved(a);
      if (canConfirm) {
        d.append("\n");
        final String option = approved ? "(DISMISS)" : "(APPROVE)";
        d.append(new Description.Link(option) {
          public void whenClicked() {
            mission.setApprovalFor(a, ! approved);
          }
        });
      }
      else d.append(approved ? "  (approved)" : "");
    }
  }
}






