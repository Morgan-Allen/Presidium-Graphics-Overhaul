/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user.mainscreen;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.verse.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.content.abilities.MiscTechniques.*;
import static stratos.game.actors.Backgrounds.*;



//  TODO:  The leader needs to actually have their traits/powers (and portrait)
//         updated when those qualities are toggled on/off.

//  TODO:  Either all state-variables should be derived from the expedition, or
//         it should be wiped when you navigate back (otherwise the local
//         records de-sync.)

//  TODO:  Consider allowing at least *some* customisation of the site, funding
//         and title, etc.  Another pane might be in order.

//  TODO:  Consider listing applicants in a horizontal layout above the
//         sector-display (i.e, instead of the homeworld-selection.)


public class SelectHouseholdPane extends MenuPane {
  
  
  final static int
    MAX_TRAITS = 2,
    MAX_POWERS = 2;
  
  
  private UINode
    traitHeader,
    powerHeader,
    advisHeader,
    colonHeader;
  
  final Expedition expedition;
  
  Background gender;
  List <Trait> traitsPicked = new List();
  List <Technique> powersPicked = new List();
  List <Background> advisorTypes = new List();
  Tally <Background> colonistNumbers = new Tally();
  
  
  
  public SelectHouseholdPane(HUD UI, Expedition expedition) {
    super(UI, MainScreen.MENU_NEW_GAME_CREW);
    this.expedition = expedition;
    initLeader();
  }
  
  
  private void initLeader() {
    if (gender == null) gender = Rand.yes() ? BORN_MALE : BORN_FEMALE;
    
    final VerseLocation home = expedition.origin();
    final Career c = new Career(KNIGHTED, BORN_HIGH, home, gender);
    Human leader = (Human) expedition.leader();
    
    if (leader == null) leader = new Human(c, home.startingOwner);
    else leader.assignCareer(c);
    
    expedition.assignLeader(leader);
    Selection.pushSelectionPane(new ExpeditionPane(UI, expedition), null);
  }
  
  
  protected void fillListing(List <UINode> listing) {
    
    listing.add(traitHeader = createTextItem("Traits", 1.2f, null));
    for (final Trait trait : SELECT_TRAITS) {
      listing.add(new TextButton(UI, "  "+trait.name, 0.75f) {
        protected void whenClicked() { toggleTrait(trait); }
        protected boolean toggled() { return traitsPicked.includes(trait); }
        protected boolean enabled() { return canToggle(trait); }
      });
    }
    
    final UIGroup sexSwitch = new UIGroup(UI);
    float space = 1f / SELECT_GENDERS.length;
    int gI = 0;
    for (final Background g : SELECT_GENDERS) {
      final TextButton option = new TextButton(UI, "  "+g.name, 0.75f) {
        protected void whenClicked() { setGender(g); }
        protected boolean toggled() { return gender == g; }
      };
      option.alignVertical(0, 0);
      option.alignAcross(space * gI, space * ++gI);
      option.attachTo(sexSwitch);
    }
    sexSwitch.alignTop(0, 15);
    listing.add(sexSwitch);
    listing.add(createTextItem(
      "Traits define some of the key strengths and weaknesses of your main "+
      "character.", 0.75f, Colour.LITE_GREY
    ));
    
    listing.add(powerHeader = createTextItem("Powers", 1.2f, null));
    for (final Power power : SELECT_POWERS) {
      listing.add(new TextButton(UI, "  "+power.name, 0.75f) {
        protected void whenClicked() { togglePower(power); }
        protected boolean toggled() { return powersPicked.includes(power); }
        protected boolean enabled() { return canToggle(power); }
      });
    }
    listing.add(createTextItem(
      "Psi Powers allow you to intervene in conflicts remotely, and can be "+
      "learned from Schools.", 0.75f, Colour.LITE_GREY
    ));
    
    listing.add(advisHeader = createTextItem("Advisors", 1.2f, null));
    for (final Background b : Expedition.ADVISOR_BACKGROUNDS) {
      listing.add(new TextButton(UI, "  "+b.name, 0.75f) {
        protected void whenClicked() { toggleAdvisor(b); }
        protected boolean toggled() { return advisorTypes.includes(b); }
        protected boolean enabled() { return canToggleAdvisor(b); }
      });
    }
    listing.add(createTextItem(
      "Advisors are particularly skilled or talented individuals who can be "+
      "sent on crucial missions or perform administrative duties.",
      0.75f, Colour.LITE_GREY
    ));
    
    listing.add(colonHeader = createTextItem("Colonists", 1.2f, null));
    for (final Background b : Expedition.COLONIST_BACKGROUNDS) {
      
      final UIGroup counter = new UIGroup(UI);
      final Text label = new Text(UI, UIConstants.INFO_FONT);
      label.scale = 0.75f;
      label.setText(b.name);
      label.setToLineSize();
      label.alignAcross(0, 0.5f);
      label.attachTo(counter);
      
      TextButton plus = new TextButton(UI, " + ", 1) {
        protected void whenClicked() { incColonists(b, 1); }
        protected boolean enabled() { return canIncColonists(b, 1); }
      };
      plus.alignAcross(0.5f, 0.65f);
      plus.attachTo(counter);
      
      TextButton minus = new TextButton(UI, " - ", 1) {
        protected void whenClicked() { incColonists(b, -1); }
        protected boolean enabled() { return canIncColonists(b, -1); }
      };
      minus.alignAcross(0.65f, 0.8f);
      minus.attachTo(counter);
      
      counter.alignTop(0, 15);
      listing.add(counter);
    }
    listing.add(createTextItem(
      "Colonists provide the backbone of your workforce, giving you a "+
      "headstart in establishing defences or trade.",
      0.75f, Colour.LITE_GREY
    ));
    
    listing.add(new TextButton(UI, "  Continue", 1) {
      protected void whenClicked() { pushNextPane(); }
      protected boolean enabled() { return canProgress(); }
    });
  }
  
  
  protected void updateState() {
    final MainScreen screen = MainScreen.current();
    screen.display.showLabels   = true ;
    screen.display.showWeather  = false;
    screen.worldsDisplay.hidden = true ;

    
    int numT = traitsPicked.size(), maxT = MAX_TRAITS;
    updateTextItem(traitHeader, "Traits ("+numT+"/"+maxT+")", null);
    
    int numP = powersPicked.size(), maxP = maxPowers();
    updateTextItem(powerHeader, "Powers ("+numP+"/"+maxP+")", null);
    
    int numA = advisorTypes.size(), maxA = maxAdvisors();
    updateTextItem(advisHeader, "Advisors ("+numA+"/"+maxA+")", null);
    
    int numC = (int) colonistNumbers.total(), maxC = maxColonists();
    updateTextItem(colonHeader, "Colonists ("+numC+"/"+maxC+")", null);
    
    super.updateState();
  }
  
  
  
  
  /**  Handling trait selection-
    */
  //
  //  TODO:  I need some custom descriptions for the effects of gender and the
  //  traits chosen!
  final static Trait SELECT_TRAITS[] = {
    FEARLESS,
    TRADITIONAL,
    GIFTED
  };
  final static String TRAIT_INFO[] = {
    
    "Grants the "+DUELIST+" technique.  Less likely to panic in battle. "+
    "+5 to "+MARKSMANSHIP+", "+COMMAND+" and "+HAND_TO_HAND+".",
    
    "Grants the "+WORD_OF_HONOUR+" technique.  Improves relations with other "+
    "Noble Houses.  +5 to "+ANCIENT_LORE+" and "+NOBLE_ETIQUETTE+".",
    
    "Grants an additional starting Psi Power.  Improves concentration (which "+
    "reduces Powers' cooldown.)"
  };
  final static Background SELECT_GENDERS[] = {
    BORN_FEMALE,
    BORN_MALE
  };
  
  
  private void toggleTrait(Trait trait) {
    traitsPicked.toggleMember(trait, ! traitsPicked.includes(trait));
    updateHelp(trait);
  }
  
  
  private boolean canToggle(Trait trait) {
    if (powersPicked.size() > MAX_POWERS && trait == GIFTED) return false;
    if (traitsPicked.includes(trait)) return true;
    else return traitsPicked.size() < maxTraits();
  }
  
  
  private int maxTraits() {
    return MAX_TRAITS;
  }
  
  
  private void setGender(Background gender) {
    this.gender = gender;
    updateHelp(gender);
  }
  
  
  
  /**  Handling power selection-
    */
  final static Power SELECT_POWERS[] = {
    Power.REMOTE_VIEWING,
    Power.FORESIGHT,
    Power.SUSPENSION,
    Power.KINESTHESIA
  };
  
  private void togglePower(Power power) {
    powersPicked.toggleMember(power, ! powersPicked.includes(power));
    updateHelp(power);
  }
  
  
  private boolean canToggle(Power power) {
    if (powersPicked.includes(power)) return true;
    else return powersPicked.size() < maxPowers();
  }
  
  
  private int maxPowers() {
    if (traitsPicked.includes(GIFTED)) return MAX_POWERS + 1;
    else return MAX_POWERS;
  }
  
  
  
  /**  Handling advisor selection-
    */
  private void toggleAdvisor(Background b) {
    final boolean is = ! advisorTypes.includes(b);
    advisorTypes.toggleMember(b, is);
    
    if (is) expedition.addAdvisor(b);
    else for (Actor a : expedition.colonists()) if (a.mind.vocation() == b) {
      expedition.removeMigrant(a);
    }
  }
  
  
  private boolean canToggleAdvisor(Background b) {
    if (advisorTypes.includes(b)) return true;
    return advisorTypes.size() < maxAdvisors();
  }
  
  
  private int maxAdvisors() {
    return Expedition.MAX_ADVISORS;
  }
  
  
  
  /**  Handling colonist selection-
    */
  private void incColonists(Background b, int inc) {
    colonistNumbers.add(inc, b);
    
    int numB = 0, allowed = (int) colonistNumbers.valueFor(b);
    for (Actor a : expedition.colonists()) if (a.mind.vocation() == b) {
      if (++numB > allowed) expedition.removeMigrant(a);
    }
    while (allowed > numB++) expedition.addColonist(b);
  }
  
  
  private boolean canIncColonists(Background b, int inc) {
    if (inc > 0) return colonistNumbers.total() < maxColonists();
    else return colonistNumbers.valueFor(b) > 0;
  }
  
  
  private int maxColonists() {
    return Expedition.MAX_COLONISTS;
  }
  
  
  
  /**  Other navigation tasks.
    */
  private boolean canProgress() {
    if (powersPicked.size() != maxPowers()) return false;
    if (traitsPicked.size() != maxTraits()) return false;
    if (advisorTypes.size() != maxAdvisors()) return false;
    if (colonistNumbers.total() != maxColonists()) return false;
    return true;
  }
  
  
  private void updateHelp(Constant c) {
    //helpField.setText("");
    //c.describeHelp(helpField, null);
    //helpField.setToPreferredSize(MainScreen.MENU_PANEL_WIDE);
  }
  
  
  private void pushNextPane() {
    ///navigateForward(new SelectCrewPane(UI), true);
  }
  
  
}





