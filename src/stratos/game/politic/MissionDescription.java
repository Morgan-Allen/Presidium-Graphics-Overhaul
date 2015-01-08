


package stratos.game.politic;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.user.*;
import stratos.util.*;
import stratos.graphics.widgets.*;
import stratos.graphics.common.*;
import static stratos.game.politic.Mission.*;



public class MissionDescription {
  
  
  protected static SelectionInfoPane configOwningPanel(
    final Mission mission, SelectionInfoPane panel, BaseUI UI
  ) {
    //
    //  Obtain some basic facts about the mission and shorthand variables
    //  first-
    final Description d = panel.detail(), l = panel.listing();
    final int type = mission.missionType();
    final List <Actor> applied = mission.applicants();
    final boolean emptyList = applied.size() == 0;
    final boolean canChange = ! mission.hasBegun();
    //
    //  Then, we fill up the left-hand pane with broad mission parameters and
    //  commands:
    describeStatus(mission, canChange, UI, d);
    //  TODO:  CONSIDER REQUIRING CONFIRMATION EVEN FOR PUBLIC MISSIONS.
    if (mission.rolesApproved() > 0 && canChange && type != TYPE_PUBLIC) {
      d.append(" ");
      d.append(new Description.Link(" (CONFIRM)") {
        public void whenClicked() {
          mission.beginMission();
        }
      });
    }
    d.append(new Description.Link(" (ABORT)") {
      public void whenClicked() {
        mission.endMission(true);
      }
    });
    //
    //  And lastly, we fill up the right-hand pane with the list of
    //  applications, and options to confirm or deny them:
    if (emptyList) {
      l.append("Applications: None");
      if (type == TYPE_PUBLIC  ) l.append(
        "\n\nThis is a public contract, open to all comers."
      );
      if (type == TYPE_SCREENED) l.append(
        "\n\nThis is a screened mission.  Applicants will be subject to your "+
        "approval before they can embark."
      );
      if (type == TYPE_COVERT  ) l.append(
        "\n\nThis is a covert mission.  No agents or citizens will apply "+
        "unless recruited by interview."
      );
    }
    else listApplicants(mission, applied, canChange, UI, l);
    return panel;
  }
  
  
  protected static SelectionInfoPane configPublicPanel(
    final Mission mission, SelectionInfoPane panel, BaseUI UI
  ) {
    final Description d = panel.detail(), l = panel.listing();
    
    describeStatus(mission, false, UI, d);
    listApplicants(mission, mission.applicants(), false, UI, l);
    return panel;
  }
  
  
  protected static SelectionInfoPane configScreenedPanel(
    final Mission mission, SelectionInfoPane panel, BaseUI UI
  ) {
    final Description d = panel.detail(), l = panel.listing();
    
    return panel;
  }
  
  
  private static void describeStatus(
    final Mission mission, boolean canChange,
    BaseUI UI, Description d
  ) {
    final Colour FIXED = Colour.LITE_GREY;
    final Base declares = mission.base;
    d.append("Declared by ");
    if (declares.ruler() == null) d.append(declares);
    else d.append(declares.ruler());
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
    //  Then, describe the mission objective-
    final int object = mission.objectIndex();
    final String
      allDesc[]  = mission.objectiveDescriptions(),
      objectDesc = mission.describeObjective(object);

    d.append("\nObjective:  ");
    if (canChange) d.append(new Description.Link(objectDesc) {
      public void whenClicked() {
        mission.setObjective((object + 1) % allDesc.length);
      }
    });
    else d.append(objectDesc, FIXED);
    d.append(mission.subject());
    
    //
    //  And finally, describe the mission's priority and/or payment:
    final int priority = mission.assignedPriority();
    final String payDesc = (priority == 0 || type == TYPE_BASE_AI) ?  "None" :
      REWARD_AMOUNTS[priority]+" credits"
    ;
    
    d.append("\nPayment:  ");
    if (canChange) d.append(new Description.Link(payDesc) {
      public void whenClicked() {
        if (priority == PRIORITY_PARAMOUNT) return;
        mission.assignPriority(priority + 1);
      }
    });
    else d.append(payDesc, FIXED);
  }
  
  
  private static void listApplicants(
    final Mission mission, List <Actor> applied, boolean canConfirm,
    BaseUI UI, Description d
  ) {
    d.append("Applications:");
    for (final Actor a : applied) {
      d.append("\n  ");
      final Composite portrait = a.portrait(UI);
      if (portrait != null) ((Text) d).insert(portrait.texture(), 40, true);
      d.append(a);
      d.append(" ("+a.vocation()+")");
      
      if (canConfirm) {
        d.append("\n");
        final boolean approved = mission.isApproved(a);
        final String option = approved ? "(DISMISS)" : "(APPROVE)";
        d.append(new Description.Link(option) {
          public void whenClicked() {
            mission.setApprovalFor(a, ! approved);
          }
        });
      }
    }
  }
}




