/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;



public class NegotiationPane extends MissionPane {
  
  
  final MissionContact contact;
  final PledgeMenu offers, sought;
  
  
  public NegotiationPane(HUD UI, MissionContact selected) {
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
  
  
  public SelectionPane configOwningPanel() {
    final Description d = detail();
    final Actor ruler = mission.base().ruler();
    final Actor subject = (Actor) mission.subject();
    if (ruler == null || subject == null) return null;
    
    if (contact.isSummons()) {
      d.append(subject);
      d.append(" is being summoned to ");
      d.append(ruler.aboard());
      d.append(".");
      return this;
    }
    
    final boolean canChange = ! mission.hasBegun();
    super.describeStatus(contact, canChange, d);
    
    offers.made = contact.pledgeOffers();
    sought.made = contact.pledgeSought();
    
    offers.listTermsFor(ruler  , subject, "\n\nTerms Offered: ", d);
    sought.listTermsFor(subject, ruler  , "\n\nTerms Sought: " , d);
    
    d.append("\n\n");
    super.listApplicants(contact, contact.applicants(), canChange, d);
    return this;
  }
  
  
  private abstract class PledgeMenu {
    
    Pledge made = null;
    Pledge.Type shownType = null;
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
        public void whenClicked(Object context) {
          final boolean willShow = ! showMenu;
          offers.showMenu = sought.showMenu = false;
          showMenu = willShow;
          shownType = made == null ? null : made.type;
        }
      });
      if (showMenu) for (final Pledge.Type type : Pledge.TYPE_INDEX) {
        if (! type.canMakePledge(ruler, subject)) continue;
        final Pledge variants[] = type.variantsFor(ruler, subject);
        if (variants == null || variants.length == 0) continue;
        if (made != null && type == made.type) continue;
        //
        //  having skipped over any non-applicable pledge types, we allow
        //  selection from any variants on the current pledge-type...
        if (shownType == type) {
          if (variants.length == 1) continue;
          d.append("\n  "+type.name);
          for (final Pledge variant : variants) {
            d.append("\n    ");
            d.append(new Description.Link(variant.description()) {
              public void whenClicked(Object context) {
                setMade(variant);
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
          final String typeDesc = variants.length == 1 ?
            variants[0].description() : type.name
          ;
          d.append(new Description.Link(typeDesc) {
            public void whenClicked(Object context) {
              if (variants.length == 1) {
                setMade(variants[0]);
                showMenu = false;
              }
              else {
                shownType = type;
              }
            }
          });
        }
      }
    }
    
    abstract void setMade(Pledge made);
  }
  
  
}


