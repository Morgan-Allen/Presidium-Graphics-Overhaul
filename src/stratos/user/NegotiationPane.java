/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user;
import stratos.game.common.*;
import stratos.game.politic.*;
import stratos.user.*;
import stratos.util.*;



public class NegotiationPane extends MissionPane {
  
  
  final ContactMission contact;
  final PledgeMenu offers, sought;
  
  
  public NegotiationPane(BaseUI UI, ContactMission selected) {
    super(UI, selected);
    this.contact = selected;
    
    this.offers = new PledgeMenu() { void setMade(Pledge p) {
      made = p;
      contact.setTerms(made, contact.pledgeSought());
    }};
    this.sought = new PledgeMenu() { void setMade(Pledge p) {
      made = p;
      contact.setTerms(contact.pledgeOffers(), made);
    }};
  }
  
  
  public SelectionInfoPane configOwningPanel() {
    final Description d = detail(), l = listing();
    
    if (contact.isSummons()) {
      contact.describeMission(d);
      return this;
    }
    
    final boolean canChange = ! mission.hasBegun();
    super.describeStatus(contact, canChange, UI, d);
    super.describeOrders(canChange, d);
    
    
    final Actor ruler = mission.base().ruler();
    final Actor subject = (Actor) mission.subject();
    offers.made = contact.pledgeOffers();
    sought.made = contact.pledgeSought();
    
    offers.listTermsFor(ruler  , subject, "Terms Offered: "   , l);
    sought.listTermsFor(subject, ruler  , "\n\nTerms Sought: ", l);
    
    l.append("\n\n");
    super.listApplicants(contact, contact.applicants(), canChange, UI, l);
    return this;
  }
  
  
  private abstract class PledgeMenu {
    
    Pledge made = null;
    Pledge.Type madeType = null;
    boolean showMenu = false;
    
    private void listTermsFor(
      Actor ruler, Actor subject, String header, Description d
    ) {
      d.append(header);
      final String desc = made == null ? "None" : made.description();
      //
      //  Clicking on the main terms-descriptor opens a menu to allow you to
      //  select other terms (and closes any other menu open.)
      d.append(new Description.Link(desc) {
        public void whenClicked() {
          final boolean willShow = ! showMenu;
          offers.showMenu = sought.showMenu = false;
          showMenu = willShow;
          madeType = made == null ? null : made.type;
        }
      });
      if (showMenu) for (final Pledge.Type type : Pledge.TYPE_INDEX) {
        final Pledge pledges[] = type.variantsFor(ruler, subject);
        if (pledges == null || pledges.length == 0) continue;
        //
        //  having skipped over any non-applicable pledge types, we allow
        //  selection from any variants on the current pledge-type...
        if (madeType == type) {
          if (pledges.length == 1) continue;
          d.append("\n  "+type.name);
          for (final Pledge pledge : pledges) {
            d.append("\n    ");
            d.append(new Description.Link(pledge.description()) {
              public void whenClicked() {
                setMade(pledge);
                showMenu = false;
              }
            });
          }
        }
        //
        //  ...in addition to different types of pledge.  (In the case where
        //  only a single variant of that pledge if available, we select this
        //  directly.)
        else {
          d.append("\n  ");
          final String typeDesc = pledges.length == 1 ?
            pledges[0].description() : type.name
          ;
          d.append(new Description.Link(typeDesc) {
            public void whenClicked() {
              if (pledges.length == 1) {
                setMade(pledges[0]);
                showMenu = false;
              }
              else {
                madeType = type;
              }
            }
          });
        }
      }
    }
    
    abstract void setMade(Pledge made);
  }
  
  
}


