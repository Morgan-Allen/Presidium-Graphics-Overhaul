/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.graphics.widgets.*;
import stratos.util.*;



public class NegotiationPane extends MissionPane {
  
  final static int
    OPTION_INIT     = -1,
    OPTION_RETURN   =  0,
    OPTION_REQUEST  =  1,
    OPTION_OFFER    =  2,
    OPTION_ADVISE   =  3,
    OPTION_BAD_NEWS =  4,
    OPTION_END_TALK =  5,
    BASIC_OPTIONS[] = { 1, 2, 3, 4, 5 },
    
    REQUEST_ANSWER  =  6,
    OFFER_ANSWER    =  7,
    DISMISS_ANSWER  =  8;
  
  final static String OPTION_STRINGS[] = {
    "I have a request.",
    "I'd like to make an offer.",
    "I need some advice.",
    "I have some bad news.",
    "You may go."
  };
  
  
  final MissionContact contact;
  
  int optionID = OPTION_INIT;
  Pledge.Type openedType;
  Pledge offers, sought;
  boolean accepted;
  
  
  public NegotiationPane(HUD UI, MissionContact selected) {
    super(UI, selected);
    this.contact = selected;
  }
  
  
  protected void describeStatus(
    Mission mission, boolean canChange, Description d
  ) {
    if (contact.subjectSummoned()) {
      setupLines();
    }
    else if (contact.isSummons()) {
      Object stays = Summons.summonedTo((Actor) contact.subject());
      d.appendAll("\nSummoning ", contact.subject(), " to ", stays);
    }
    else super.describeStatus(mission, canChange, d);
  }
  
  
  protected void describeOrders(boolean canChange, Description d) {
    if (contact.subjectSummoned()) {
      
    }
    else if (contact.isSummons()) {
      
    }
    else super.describeOrders(canChange, d);
  }
  
  
  protected void listApplicants(
    Mission mission, List <Actor> applied, boolean canConfirm, Description d
  ) {
    if (contact.subjectSummoned()) {
      
    }
    else if (contact.isSummons()) {
      
    }
    else super.listApplicants(mission, applied, canConfirm, d);
  }
  
  
  protected Composite updatePortrait(HUD UI) {
    if (contact.subjectSummoned()) {
      return ((Actor) contact.subject()).portrait(UI);
    }
    else return super.updatePortrait(UI);
  }
  
  
  void setupLines() {
    //
    //  TODO:  Pledges will work strictly off materials and options available
    //         from within the Bastion.
    
    final Description d = detail();
    final Actor ruler = mission.base().ruler();
    final Actor subject = (Actor) mission.subject();
    if (ruler == null || subject == null);
    
    if (optionID == OPTION_INIT || optionID == OPTION_RETURN) {
      if (optionID == OPTION_INIT) d.append("\"Yes my liege?\""             );
      else                         d.append("\"Anything else, your grace?\"");
      
      for (final int o : BASIC_OPTIONS) {
        d.appendAll("\n  ", new Description.Link(OPTION_STRINGS[o - 1]) {
          public void whenClicked(Object context) {
            if (o == OPTION_END_TALK) {
              contact.endMission(true);
            }
            else optionID = o;
          }
        });
      }
    }
    else if (optionID == OPTION_REQUEST) {
      d.append("\"I am here to serve, your grace.\"");
      appendPledgeOptions(subject, ruler, d, true);
      appendReturnOption(d);
    }
    else if (optionID == OPTION_OFFER) {
      d.append("\"What do you have in mind?\"");
      appendPledgeOptions(ruler, subject, d, false);
      appendReturnOption(d);
    }
    else if (optionID == OFFER_ANSWER) {
      d.append("\"That is agreeable.  What would you ask in return?\"");
      appendPledgeOptions(subject, ruler, d, true);
      appendReturnOption(d);
    }
    else if (optionID == OPTION_ADVISE) {
      d.append("\"What troubles you, your grace?\"");
      //  TODO:  Implement this later!
      appendReturnOption(d);
    }
    else if (optionID == OPTION_BAD_NEWS) {
      d.append("\"Could you explain?\"");
      d.appendAll("\n  ", new Description.Link(
        "I no longer require your services."
      ) {
        public void whenClicked(Object context) {
          optionID = DISMISS_ANSWER;
        }
      });
      appendReturnOption(d);
    }
    else if (optionID == REQUEST_ANSWER) {
      if (accepted) {
        if (sought != null && sought.type == Pledge.TYPE_GOOD_WILL) {
          d.append("\"Why... thank you your grace!\"");
        }
        else {
          d.append("\"Very well, your grace.\"");
        }
        d.appendAll("\n  ", new Description.Link("Then it is settled.") {
          public void whenClicked(Object context) {
            confirmDeal(ruler, subject);
          }
        });
      }
      else {
        d.append("\"I... regret I must decline, your grace.\"");
        d.appendAll("\n  ", new Description.Link("A pity.") {
          public void whenClicked(Object context) {
            cancelOption();
          }
        });
      }
    }
    else if (optionID == DISMISS_ANSWER) {
      appendDismissOptions(ruler, subject, d);
    }
  }
  
  
  private void appendReturnOption(Description d) {
    d.appendAll("\n  ", new Description.Link("Nothing.  Pay it no mind.") {
      public void whenClicked(Object context) {
        cancelOption();
      }
    });
  }
  
  
  private void appendPledgeOptions(
    final Actor making, final Actor with, Description d, final boolean asReq
  ) {
    for (final Pledge.Type type : Pledge.TYPE_INDEX) {
      if (! type.canMakePledge(making, with)) continue;
      final Pledge variants[] = type.variantsFor(making, with);
      if (variants == null || variants.length == 0) continue;
      
      boolean showVars = openedType == type || variants.length == 1;
      boolean showType = variants.length > 1;
      
      if (showType) d.appendAll("\n  ", new Description.Link(type.name) {
        public void whenClicked(Object context) {
          if (variants.length == 1) {
            if (asReq) proposeRequest(variants[0], making, with);
            else proposeOffer(variants[0], making, with);
          }
          else if (openedType == type) openedType = null;
          else openedType = type;
        }
      });
      if (showVars) {
        final String indent = showType ? "\n    " : "\n  ";
        for (final Pledge variant : variants) {
          d.appendAll(indent, new Description.Link(variant.description()) {
            public void whenClicked(Object context) {
              if (asReq) proposeRequest(variant, making, with);
              else proposeOffer(variant, making, with);
            }
          });
        }
      }
    }
  }
  
  
  private void proposeRequest(Pledge sought, Actor ruler, Actor subject) {
    this.sought = sought;
    if (offers == null) offers = Pledge.goodWillPledge(ruler, subject);
    
    accepted = Negotiation.tryAcceptance(
      offers, sought, ruler, subject
    );
    openedType = null;
    optionID = REQUEST_ANSWER;
  }
  
  
  private void proposeOffer(Pledge offers, Actor ruler, Actor subject) {
    this.offers = offers;
    openedType = null;
    optionID = OFFER_ANSWER;
  }
  
  
  private void confirmDeal(Actor ruler, Actor subject) {
    Negotiation.setAcceptance(offers, sought, ruler, subject, accepted);
    if (! contact.subjectSummoned()) {
      contact.endMission(true);
      Selection.pushSelection(subject, null);
    }
    else {
      offers = sought = null;
      optionID = OPTION_RETURN;
    }
  }
  
  
  private void appendDismissOptions(
    final Actor ruler, final Actor subject, Description d
  ) {
    //
    //  TODO:  Include this as a negative-value pledge...
    
    d.append("\"Your grace!  I have always been faithful!\"");
    d.appendAll("\n  ", new Description.Link("Nonetheless.  My mind is set.") {
      public void whenClicked(Object context) {
        subject.mind.setWork(null);
      }
    });
    d.appendAll("\n  ", new Description.Link("Perhaps I could reconsider...") {
      public void whenClicked(Object context) {
        subject.relations.incRelation(ruler, -0.5f, 0.2f, 0);
        cancelOption();
      }
    });
  }
  
  
  private void cancelOption() {
    offers = sought = null;
    optionID = OPTION_RETURN;
  }
}


