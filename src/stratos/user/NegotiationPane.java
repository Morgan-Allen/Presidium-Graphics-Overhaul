

package stratos.user;
import stratos.game.common.*;
import stratos.game.politic.*;
import stratos.user.*;
import stratos.util.*;



public class NegotiationPane extends MissionPane {
  
  
  public NegotiationPane(BaseUI UI, ContactMission selected) {
    super(UI, selected);
  }
  
  
  public SelectionInfoPane configOwningPanel() {
    //return super.configOwningPanel();
    final Description d = detail(), l = listing();
    final ContactMission contact = (ContactMission) mission;
    
    if (contact.isSummons()) {
      contact.describeMission(d);
      return this;
    }
    
    
    listTerms(l);
    return this;
  }
  
  
  //  Okay.  A pane for terms asked, a pane for terms given, and a pane for
  //  applicants to deliver the message.
  
  
  //  Just... do the listing bits first.  And see how they look.
  
  private void listTerms(Description d) {
    
    final Actor ruler = mission.base().ruler();
    final Actor subject = (Actor) mission.subject();
    
    d.append("\nTerms Given:");
    
    for (Pledge.Type type : Pledge.TYPE_INDEX) {
      final Pledge pledges[] = type.variantsFor(ruler, subject);
      if (pledges == null || pledges.length == 0) continue;
      
      //  TODO:  If there's only a single option, use that as the title-key...
      
      d.append("\n"+type.name);
      for (Pledge pledge : pledges) {
        d.append("\n  ");
        //  TODO:  Just get a string and use that as the link.
        pledge.describeTo(d);
      }
    }
    
    d.append("\nTerms asked:");
    
    for (Pledge.Type type : Pledge.TYPE_INDEX) {
      final Pledge pledges[] = type.variantsFor(subject, ruler);
      if (pledges == null || pledges.length == 0) continue;
      
      //  TODO:  If there's only a single option, use that as the title-key...
      
      d.append("\n"+type.name);
      for (Pledge pledge : pledges) {
        d.append("\n  ");
        //  TODO:  Just get a string and use that as the link.
        pledge.describeTo(d);
      }
    }
  }
}





