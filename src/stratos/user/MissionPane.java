/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.economic.*;
import stratos.user.*;
import stratos.util.*;
import stratos.graphics.widgets.*;
import stratos.graphics.common.*;
import static stratos.game.base.Mission.*;



//  TODO:  I'm taking out the variations in objective for the moment- I'll
//  probably add extra buttons at the target-options level for that.  Do that.


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
    boolean canChange = UI.played() == mission.base() && ! mission.hasBegun();
    //
    //  Then, we fill up the left-hand pane with broad mission parameters and
    //  commands:
    describeStatus(mission, canChange, UI, d);
    describeOrders(canChange, d);
    listApplicants(mission, applied, canChange, UI, l);
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
      d.append("\n ");
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
      
      d.append(new Description.Link(" (ABORT)") {
        public void whenClicked() {
          if (begun) confirmAbort = true;
          else mission.endMission(false);
        }
      }, Colour.RED);
    }
  }
  
  
  public SelectionPane configPublicPanel() {
    final Description d = detail(), l = listing();
    
    describeStatus(mission, false, UI, d);
    listApplicants(mission, mission.applicants(), false, UI, l);
    return this;
  }
  
  
  public SelectionPane configScreenedPanel() {
    return this;
  }
  
  
  protected void describeStatus(
    final Mission mission, boolean canChange,
    BaseUI UI, Description d
  ) {
    //final Colour FIXED = Colour.LITE_GREY;
    //
    //  Firstly, declare the mission's patron and current status:
    final Base declares = mission.base();
    d.append("Declared by "+declares);
    d.append("\n  Subject: ");
    if (mission.visibleTo(mission.base())) d.append(mission.subject());
    else d.append(""+mission.subject(), Colour.GREY);
    d.append("\n  Status:  "+mission.progressDescriptor());
    d.append("\n");
    
    //
    //  Secondly, describe the mission type:
    final int type     = mission.missionType();
    final int priority = mission.assignedPriority();
    if (type == TYPE_BASE_AI) return;
    int payAmount = REWARD_AMOUNTS[priority];
    
    d.append("\nType and Reward: ");
    for (int i = 0; i < LIMIT_TYPE; i++) {
      final int newType = i;
      final boolean active = type == newType;
      d.append("\n  ");
      
      if (canChange) d.append(new Description.Link(TYPE_DESC[newType]) {
        public void whenClicked() {
          mission.setMissionType(newType);
        }
      }, (active ? Colour.GREEN : Text.LINK_COLOUR));
      
      else d.append(
        TYPE_DESC[newType], active ? Colour.GREEN : Colour.LITE_GREY
      );
      
      if (active) {
        d.append(" ("+payAmount+" credits) ");
        
        final float nextAmount = Mission.rewardFor(priority + 1);
        final boolean canPay = mission.base().finance.credits() > nextAmount;
        final boolean canAdjust = canPay && (canChange || (
          type == TYPE_PUBLIC && priority < PRIORITY_PARAMOUNT
        ));
        
        if (canAdjust) d.append(new Description.Link("(More)") {
          public void whenClicked() {
            mission.assignPriority((priority + 1) % LIMIT_PRIORITY);
          }
        });
      }
    }
    d.append("\n");
  }
  
  
  protected void listApplicants(
    final Mission mission, List <Actor> applied, boolean canConfirm,
    BaseUI UI, Description d
  ) {
    
    if (mission.missionType() == TYPE_MILITARY && canConfirm) {
      
      final Stage world = UI.played().world;
      final Batch <Venue> barracks = new Batch();
      final Background MILITARY[] = Backgrounds.MILITARY_CIRCLES;
      
      for (Object o : world.presences.allMatches(Economy.SERVICE_SECURITY)) {
        final Venue v = (Venue) o;
        if (v.base() == UI.played()) barracks.add(v);
      }
      d.append("Standing Forces:");
      
      for (Venue v : barracks) {
        
        final Batch <Actor> soldiers = new Batch();
        for (Actor s : v.staff.workers()) {
          if (! Visit.arrayIncludes(MILITARY, s.mind.vocation())) {
            continue;
          }
          soldiers.add(s);
        }
        if (soldiers.empty()) continue;
        
        d.append("\n  ");
        final Composite portrait = v.portrait(UI);
        if (portrait != null) Text.insert(portrait.texture(), 40, 40, true, d);
        d.append(" "+v);
        
        d.append("\n  ");
        for (Actor s : soldiers) {
          final Composite PS = s.portrait(UI);
          if (PS != null) Text.insert(PS.texture(), 20, 20, s, false, d);
        }
        
        d.append("\n  ");
        d.append(new Description.Link("(DEPLOY "+soldiers.size()+"X)") {
          public void whenClicked() {
            for (Actor s : soldiers) {
              s.mind.assignMission(mission);
              mission.setApprovalFor(s, true);
              mission.setSpecialRewardFor(s, Pledge.militaryDutyPledge(s));
            }
          }
        });
      }
      Text.cancelBullet(d);
    }
    
    if (applied.size() == 0) {
      d.append("\n");
      d.append(mission.helpInfo(), Colour.LITE_GREY);
    }
    else {
      d.append("Team Members:");
      
      for (final Actor a : applied) {
        d.append("\n  ");
        final Composite portrait = a.portrait(UI);
        if (portrait != null) Text.insert(portrait.texture(), 40, 40, true, d);
        d.append(" ");
        d.append(a);
        ///d.append(" ("+a.mind.vocation()+")");
        
        final boolean approved = mission.isApproved(a);
        if (canConfirm) {
          d.append("\n ");
          final String option = approved ? "(DISMISS)" : "(APPROVE)";
          d.append(new Description.Link(option) {
            public void whenClicked() {
              mission.setApprovalFor(a, ! approved);
            }
          });
        }
        else {
          d.append("\n ");
          d.append(approved ? "(approved)" : "", Colour.LITE_GREY);
          d.append("\n ");
          a.describeStatus(d, mission);
        }
      }
    }
  }
}





    
    
    /*
    final String typeDesc = type == TYPE_BASE_AI ? "BASE AI" :
      TYPE_DESC[type]
    ;
    d.append("\n  Mission Type:  ");
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
    d.append("\n  Reward offered:  ");
    final int priority = mission.assignedPriority();
    final float nextAmount = Mission.rewardFor(priority + 1);
    final boolean canPay = mission.base().finance.credits() > nextAmount;
    
    String payDesc = (priority == 0 || type == TYPE_BASE_AI) ?  "None" :
      REWARD_AMOUNTS[priority]+" credits"
    ;
    if (! canPay) payDesc+=" (Can't afford increase!)";
    
    final boolean canAdjust = canPay && (canChange || (
      type == TYPE_PUBLIC && priority < PRIORITY_PARAMOUNT
    ));
    if (canAdjust) d.append(new Description.Link(payDesc) {
      public void whenClicked() {
        if (priority == PRIORITY_PARAMOUNT) mission.assignPriority(0);
        mission.assignPriority(priority + 1);
      }
    });
    else d.append(payDesc, FIXED);
    //*/

