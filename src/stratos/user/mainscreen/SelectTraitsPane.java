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



public class SelectTraitsPane extends MenuPane {
  
  
  final static int
    MAX_TRAITS = 2,
    MAX_POWERS = 2;
  
  final Expedition expedition;
  
  private UINode
    traitHeader,
    traitFooter,
    powerHeader,
    powerFooter;
  private Text helpField;
  
  
  public SelectTraitsPane(HUD UI, Expedition expedition) {
    super(UI, MainScreen.MENU_NEW_GAME_CREW);
    this.expedition = expedition;
    initLeader();
  }
  
  
  protected void fillListing(List <UINode> listing) {
    
    listing.add(traitHeader = createTextItem("Traits", 1.2f, null, 1));
    for (final Trait trait : SELECT_TRAITS) {
      listing.add(new TextButton(UI, "  "+trait.name, 0.75f) {
        protected void whenClicked() { toggleTrait(trait); }
        protected boolean toggled() { return hasTrait(trait); }
        protected boolean enabled() { return canToggle(trait); }
      });
    }
    
    final UIGroup sexSwitch = new UIGroup(UI);
    float space = 1f / SELECT_GENDERS.length;
    int gI = 0;
    for (final Background g : SELECT_GENDERS) {
      final TextButton option = new TextButton(UI, "  "+g.name, 0.75f) {
        protected void whenClicked() { setGender(g); }
        protected boolean toggled() { return hasGender(g); }
      };
      option.alignVertical(0, 0);
      option.alignAcross(space * gI, space * ++gI);
      option.attachTo(sexSwitch);
    }
    sexSwitch.alignTop(0, 15);
    listing.add(sexSwitch);
    listing.add(traitFooter = createTextItem(
      "Traits define some of the key strengths and weaknesses of your main "+
      "character.", 0.75f, Colour.LITE_GREY, 3
    ));
    
    listing.add(powerHeader = createTextItem("Powers", 1.2f, null, 1));
    for (final Power power : SELECT_POWERS) {
      listing.add(new TextButton(UI, "  "+power.name, 0.75f) {
        protected void whenClicked() { togglePower(power); }
        protected boolean toggled() { return hasPower(power); }
        protected boolean enabled() { return canToggle(power); }
      });
    }
    listing.add(powerFooter = createTextItem(
      "Psi Powers allow you to intervene in conflicts remotely, and can be "+
      "learned from Schools.", 0.75f, Colour.LITE_GREY, 3
    ));
    
    listing.add(new TextButton(UI, "  Continue", 1) {
      protected void whenClicked() { pushNextPane(); }
      protected boolean enabled() { return canProgress(); }
    });
    
    helpField = new Text(UI, UIConstants.INFO_FONT);
    helpField.alignAcross(0, 1);
    listing.add(helpField);
  }
  
  
  protected void updateState() {
    final MainScreen screen = MainScreen.current();
    screen.display.showLabels   = true ;
    screen.display.showWeather  = false;
    screen.worldsDisplay.hidden = true ;
    screen.crewDisplay.hidden   = false;
    
    int numT = numTraits(), maxT = MAX_TRAITS;
    updateTextItem(traitHeader, "Traits ("+numT+"/"+maxT+")", null);
    
    int numP = numPowers(), maxP = maxPowers();
    updateTextItem(powerHeader, "Powers ("+numP+"/"+maxP+")", null);
    
    screen.crewDisplay.setupFrom(expedition);
    
    super.updateState();
  }
  
  
  private void updateHelpFor(UINode footer, String helpText) {
    updateTextItem(footer, helpText, Colour.LITE_GREY);
  }
  
  
  private void updateLeaderMedia() {
    Human.refreshMedia((Human) leader());
  }
  

  
  /**  Leader creation and updates-
    */
  private void initLeader() {
    if (expedition.leader() != null) return;
    
    final Sector  home    = expedition.origin ();
    final Faction faction = expedition.backing();
    
    Background gender = Rand.yes() ? BORN_MALE : BORN_FEMALE;
    final Career c     = new Career(KNIGHTED, BORN_HIGH, home, gender);
    final Human leader = new Human(c, faction);
    
    for (Trait t : SELECT_TRAITS) leader.traits.setLevel(t, 0);
    leader.skills.wipeTechniques();
    expedition.assignLeader(leader);
  }
  
  
  private Actor leader() {
    return expedition.leader();
  }
  
  
  private boolean hasTrait(Trait t) {
    return leader().traits.hasTrait(t);
  }
  
  
  private boolean hasGender(Background b) {
    if (b == BORN_MALE) return leader().traits.male  ();
    else                return leader().traits.female();
  }
  
  
  private boolean hasPower(Technique t) {
    return leader().skills.hasTechnique(t);
  }
  
  
  
  
  /**  Handling trait selection-
    */
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
  final static String GENDER_INFO[] = {
    "Women gain a slight bonus to longevity and social skills.",
    "Men gain a slight bonus to athletics and close combat."
  };
  
  
  private void toggleTrait(Trait trait) {
    final boolean has = hasTrait(trait);
    leader().traits.setLevel(trait, has ? 0 : trait.maxVal / 2f);
    
    if (trait == FEARLESS) {
      togglePower(DUELIST);
      leader().traits.incLevel(MARKSMANSHIP, has ? -5 : 5);
      leader().traits.incLevel(COMMAND     , has ? -5 : 5);
      leader().traits.incLevel(HAND_TO_HAND, has ? -5 : 5);
    }
    if (trait == TRADITIONAL) {
      togglePower(WORD_OF_HONOUR);
      leader().traits.incLevel(ANCIENT_LORE   , has ? -5 : 5);
      leader().traits.incLevel(NOBLE_ETIQUETTE, has ? -5 : 5);
      //  TODO:  Including effects on political alliance.
    }
    
    String helpText = TRAIT_INFO[Visit.indexOf(trait, SELECT_TRAITS)];
    updateHelpFor(traitFooter, helpText);
    updateLeaderMedia();
  }
  
  
  private boolean canToggle(Trait trait) {
    if (numPowers() > MAX_POWERS && trait == GIFTED) return false;
    if (hasTrait(trait)) return true;
    else return numTraits() < maxTraits();
  }
  
  
  private int numTraits() {
    int n = 0; for (Trait t : SELECT_TRAITS) if (hasTrait(t)) n++;
    return n;
  }
  
  
  private int maxTraits() {
    return MAX_TRAITS;
  }
  
  
  private void setGender(Background gender) {
    final float genVal = FEMININE.maxVal / 2f;
    final boolean male = gender == BORN_MALE;
    
    leader().traits.setLevel(FEMININE     , genVal * (male ? -1 : 1));
    leader().traits.setLevel(GENDER_MALE  , male ? 1 : 0            );
    leader().traits.setLevel(GENDER_FEMALE, male ? 0 : 1            );
    
    //  TODO:  Include secondary skill/trait FX.
    
    String helpInfo = GENDER_INFO[Visit.indexOf(gender, SELECT_GENDERS)];
    updateHelpFor(traitFooter, helpInfo);
    updateLeaderMedia();
  }
  
  
  /**  Handling power selection-
    */
  final static Power SELECT_POWERS[] = Power.BASIC_POWERS;
  
  
  private void togglePower(Technique power) {
    final boolean has = hasPower(power);
    if (has) leader().skills.removeTechnique(power);
    else     leader().skills.addTechnique   (power);
    
    updateHelpFor(powerFooter, power.description);
    updateLeaderMedia();
  }
  
  
  private boolean canToggle(Technique power) {
    if (hasPower(power)) return true;
    else return numPowers() < maxPowers();
  }
  
  
  private int numPowers() {
    int n = 0; for (Power p : SELECT_POWERS) if (hasPower(p)) n++;
    return n;
  }
  
  
  private int maxPowers() {
    if (hasTrait(GIFTED)) return MAX_POWERS + 1;
    else return MAX_POWERS;
  }
  
  
  
  /**  Other navigation tasks.
    */
  private boolean canProgress() {
    if (numPowers   () != maxPowers   ()) return false;
    if (numTraits   () != maxTraits   ()) return false;
    return true;
  }
  
  
  private void pushNextPane() {
    //MainScreen.current().clearInfoPane();
    navigateForward(new SelectCrewPane(UI, expedition), true);
  }
  
  
  protected void navigateBack() {
    MainScreen.current().clearInfoPane();
    super.navigateBack();
  }
}












