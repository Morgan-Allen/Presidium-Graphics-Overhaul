/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.actors.*;
import stratos.user.*;
import stratos.util.*;
import stratos.graphics.widgets.*;
import stratos.graphics.common.*;
import static stratos.game.base.Mission.*;



//  TODO:  I'm taking out the variations in objective for the moment- I'll
//  probably add extra buttons at the target-options level for that.  Do that.


public class MissionPane extends SelectionPane {
  
  
  final protected Base viewing;
  final protected Mission mission;
  private boolean confirmAbort = false;
  
  
  public MissionPane(HUD UI, Mission selected) {
    super(UI, selected, selected.portrait(UI), true);
    this.viewing = BaseUI.currentPlayed();
    this.mission = selected;
  }
  
  
  public SelectionPane configOwningPanel() {
    //
    //  Obtain some basic facts about the mission and shorthand variables
    //  first-
    final Description d = detail(), l = listing();
    final List <Actor> applied = mission.applicants();
    boolean canChange = viewing == mission.base() && ! mission.hasBegun();
    //
    //  Then, we fill up the left-hand pane with broad mission parameters and
    //  commands:
    describeStatus(mission, canChange, d);
    listApplicants(mission, applied, canChange, l);
    return this;
  }
  
  
  public SelectionPane configPublicPanel() {
    final Description d = detail(), l = listing();
    
    describeStatus(mission, false, d);
    listApplicants(mission, mission.applicants(), false, l);
    return this;
  }
  
  
  public SelectionPane configScreenedPanel() {
    return this;
  }
  
  
  protected void describeStatus(
    final Mission mission, boolean canChange, Description d
  ) {
    
    //  Firstly, declare the mission's patron and current status:
    final Base declares = mission.base();
    d.append("Declared by "+declares);
    d.append("\n  Subject: ");
    if (mission.visibleTo(mission.base())) d.append(mission.subject());
    else d.append(""+mission.subject(), Colour.GREY);
    d.append("\n  Status:  "+mission.progressDescriptor());
    
    if (declares == viewing) describeOrders(canChange, d);
    if (confirmAbort) return;
    d.append("\n");
    
    //  Secondly, describe the mission type:
    final int type     = mission.missionType();
    final int priority = mission.assignedPriority();
    if (type == TYPE_BASE_AI) return;
    int payAmount = mission.rewardAmount();
    
    d.append("\nType and Reward: ");
    for (final int newType : Mission.ALL_MISSION_TYPES) {
      if (! mission.allowsMissionType(newType)) continue;
      final boolean active = type == newType;
      d.append("\n  ");
      
      if (canChange) d.append(new Description.Link(TYPE_DESC[newType]) {
        public void whenClicked(Object context) {
          mission.setMissionType(newType);
        }
      }, (active ? Colour.GREEN : Text.LINK_COLOUR));
      
      else d.append(
        TYPE_DESC[newType], active ? Colour.GREEN : Colour.LITE_GREY
      );
      
      if (active) {
        d.append(" ("+payAmount+" credits) ");
        
        final float nextAmount = mission.rewardForPriority(priority + 1);
        final boolean canPay = mission.base().finance.credits() > nextAmount;
        final boolean canAdjust = canPay && (canChange || (
          type == TYPE_PUBLIC && priority < PRIORITY_PARAMOUNT
        ));
        
        if (canAdjust) d.append(new Description.Link("(More)") {
          public void whenClicked(Object context) {
            mission.assignPriority((priority + 1) % LIMIT_PRIORITY);
          }
        });
      }
    }
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
        public void whenClicked(Object context) {
          mission.endMission(true);
        }
      });
      d.append(new Description.Link(" (NO)") {
        public void whenClicked(Object context) {
          confirmAbort = false;
        }
      });
    }
    else {
      d.append("\n ");
      
      final boolean begun = mission.hasBegun(), canBegin =
        mission.rolesApproved() > 0 && canChange && type != TYPE_PUBLIC
      ;
      if (canBegin) d.append(new Description.Link(" (BEGIN)") {
        public void whenClicked(Object context) {
          mission.beginMission();
        }
      });
      else d.append(" (BEGIN)", Colour.GREY);
      
      d.append(new Description.Link(" (ABORT)") {
        public void whenClicked(Object context) {
          if (begun) confirmAbort = true;
          else mission.endMission(false);
        }
      }, Colour.RED);
    }
  }
  
  
  protected void listApplicants(
    final Mission mission, List <Actor> applied, boolean canConfirm,
    Description d
  ) {
    if (mission.missionType() == TYPE_MILITARY && canConfirm) {

      final Stage world = mission.base().world;
      final Batch <Conscription> barracks = new Batch();
      
      for (Target b : world.presences.allMatches(Economy.SERVICE_SECURITY)) {
        if (b.base() == viewing && b instanceof Conscription) {
          barracks.add((Conscription) b);
        }
      }
      d.append("Standing Forces:");
      
      for (final Conscription b : barracks) {
        final Batch <Actor>
          soldiers  = new Batch(),
          available = new Batch(),
          onMission = new Batch(),
          downtime  = new Batch();
        
        for (Actor s : b.staff().workers()) {
          if (! b.canConscript(s, false)) continue;
          soldiers.add(s);
          final Mission m = s.mind.mission();
          if (m != null) onMission.add(s);
          else if (! b.canConscript(s, true )) downtime.add(s);
          else available.add(s);
        }
        
        final int
          numA = available.size(),
          numD = downtime .size(),
          numM = onMission.size();
        if (soldiers.empty()) continue;
        
        d.append("\n  ");
        final Composite portrait = b.portrait(UI);
        if (portrait != null) Text.insert(portrait.texture(), 40, 40, true, d);
        d.append(" "+b);
        
        d.append("\n  ");
        for (Actor s : soldiers) {
          final Composite PS = s.portrait(UI);
          final boolean okay = available.includes(s);
          if (PS != null) {
            final Button mini = Text.insert(PS.texture(), 20, 20, s, false, d);
            if (! okay) {
              mini.setDisabledOverlay(Image.TRANSLUCENT_BLACK);
              mini.enabled = false;
            }
          }
        }
        
        if (numA > 0) {
          d.append("\n  ");
          d.append(new Description.Link("(DEPLOY "+numA+"X)") {
            public void whenClicked(Object context) {
              for (Actor s : available) {
                b.beginConscription(s, mission);
              }
            }
          });
        }
        if (numD > 0) {
          d.append("\n  ("+numD+"x on Downtime)", Colour.LITE_GREY);
        }
        if (numM > 0) {
          d.append("\n  ("+numM+"x on Mission)", Colour.LITE_GREY);
        }
      }
      Text.cancelBullet(d);
    }
    
    if (applied.size() == 0) {
      d.append("\n");
      d.append(mission.helpInfo(), Colour.LITE_GREY);
    }
    else {
      d.append("\nApplied:");
      
      for (final Actor a : applied) {
        final boolean approved = mission.isApproved(a);
        d.append("\n  ");
        final Composite portrait = a.portrait(UI);
        if (portrait != null) Text.insert(portrait.texture(), 40, 40, true, d);
        d.append(" ");
        d.append(a);
        d.append("\n ("+a.mind.vocation()+")", Colour.LITE_GREY);
        
        if (canConfirm) {
          d.append(" ");
          final String option = approved ? "(DISMISS)" : "(APPROVE)";
          d.append(new Description.Link(option) {
            public void whenClicked(Object context) {
              mission.setApprovalFor(a, ! approved);
              if (approved) a.mind.assignMission(null);
            }
          });
        }
        else {
          d.append("\n ");
          a.describeStatus(d, mission);
        }
      }
    }
  }
}

