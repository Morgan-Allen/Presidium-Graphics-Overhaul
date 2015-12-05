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
import stratos.start.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.content.abilities.MiscTechniques.*;
import static stratos.game.actors.Backgrounds.*;



//  TODO:  Consider allowing at least *some* customisation of the site, funding
//         and title (possibly from homeworld or traits).  Another pane might
//         be in order.

//  TODO:  Consider listing applicants in a horizontal layout above the
//         sector-display (i.e, instead of the homeworld-selection.)  The
//         info-pane could then give data on individual colonists, etc.

//  TODO:  There definitely needs to be help-text for the traits & powers- and
//         the info-pane should disappear once you navigate back to the main
//         screen.

//  TODO:  Include the extra Techniques/Skills related to traits!


//  TODO:  Consider some further refinements to leader-generation...?
//  Traditional or Upstart
//  Fearless or Diplomatic
//  Gifted
//  Male or Female + Orientation

/*
-    //
-    //  Firstly, we determine the ruler's current rank in the feudal hierarchy
-    //  and their class of origin.
-    final int station = config.titleLevel;
-    final Background vocation = Backgrounds.RULING_POSITIONS[station];
-    
-    final float promoteChance = (25 - (station * 10)) / 100f;
-    Background birth = Backgrounds.BORN_HIGH;
-    
-    while (Rand.num() < promoteChance) {
-      int index = Visit.indexOf(birth, Backgrounds.RULER_CLASSES);
-      if (index <= 0) break;
-      else birth = Backgrounds.RULER_CLASSES[index - 1];
-    }
-    //
-    //  Then we generate the ruler themselves along with any modifications
-    //  chosen by the player.
-    final Background house = config.house;
-    final Career rulerCareer = new Career(
-      vocation, birth, house, config.gender
-    );
-    final Human ruler = new Human(rulerCareer, base);
-    for (Skill s : house.skills()) {
-      ruler.traits.incLevel(s, 5 + Rand.index(5) - 2);
-    }
-    for (Trait t : config.chosenTraits) {
-      ruler.traits.setLevel(t, t.maxVal * (Rand.num() + 1) / 1);
-    }
-    for (Skill s : config.chosenSkills) {
-      ruler.traits.incLevel(s, 10 + Rand.index(5) - 2);
-    }
-    for (Technique t : config.chosenTechs) {
-      ruler.skills.addTechnique(t);
-    }
-    
-    return ruler;
+    return (Human) expedition.leader();
//*/




public class SelectHouseholdPane extends MenuPane {
  
  
  final static int
    MAX_TRAITS = 2,
    MAX_POWERS = 2;
  
  
  private UINode
    traitHeader,
    powerHeader,
    advisHeader,
    colonHeader;
  private Text
    colonLabels[];
  
  final Expedition expedition;
  
  
  public SelectHouseholdPane(HUD UI, Expedition expedition) {
    super(UI, MainScreen.MENU_NEW_GAME_CREW);
    this.expedition = expedition;
    initLeader();
  }
  
  
  protected void fillListing(List <UINode> listing) {
    
    listing.add(traitHeader = createTextItem("Traits", 1.2f, null));
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
    listing.add(createTextItem(
      "Traits define some of the key strengths and weaknesses of your main "+
      "character.", 0.75f, Colour.LITE_GREY
    ));
    
    listing.add(powerHeader = createTextItem("Powers", 1.2f, null));
    for (final Power power : SELECT_POWERS) {
      listing.add(new TextButton(UI, "  "+power.name, 0.75f) {
        protected void whenClicked() { togglePower(power); }
        protected boolean toggled() { return hasPower(power); }
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
        protected boolean toggled() { return hasAdvisor(b); }
        protected boolean enabled() { return canToggleAdvisor(b); }
      });
    }
    listing.add(createTextItem(
      "Advisors are particularly skilled or talented individuals who can be "+
      "sent on crucial missions or perform administrative duties.",
      0.75f, Colour.LITE_GREY
    ));
    
    listing.add(colonHeader = createTextItem("Colonists", 1.2f, null));
    colonLabels = new Text[Expedition.COLONIST_BACKGROUNDS.length];
    int labelIndex = 0;
    
    for (final Background b : Expedition.COLONIST_BACKGROUNDS) {
      
      final UIGroup counter = new UIGroup(UI);
      final Text label = new Text(UI, UIConstants.INFO_FONT);
      colonLabels[labelIndex++] = label;
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
    
    int numT = numTraits(), maxT = MAX_TRAITS;
    updateTextItem(traitHeader, "Traits ("+numT+"/"+maxT+")", null);
    
    int numP = numPowers(), maxP = maxPowers();
    updateTextItem(powerHeader, "Powers ("+numP+"/"+maxP+")", null);
    
    int numA = numAdvisors(), maxA = maxAdvisors();
    updateTextItem(advisHeader, "Advisors ("+numA+"/"+maxA+")", null);
    
    int numC = numColonists(), maxC = maxColonists();
    updateTextItem(colonHeader, "Colonists ("+numC+"/"+maxC+")", null);
    
    if (colonLabels != null) for (int i = colonLabels.length; i-- > 0;) {
      final Background b = Expedition.COLONIST_BACKGROUNDS[i];
      final Text t = colonLabels[i];
      final int numM = expedition.numMigrants(b);
      final Colour tint = numM > 0 ? Text.LINK_COLOUR : Colour.LITE_GREY;
      t.setText("");
      t.append("  "+b.name+" ("+numM+")", tint);
    }
    
    super.updateState();
  }
  
  
  private void updateHelp(Constant c) {
    //helpField.setText("");
    //c.describeHelp(helpField, null);
    //helpField.setToPreferredSize(MainScreen.MENU_PANEL_WIDE);
  }
  
  
  private void updateLeaderMedia() {
    Human.refreshMedia((Human) leader());
  }
  

  
  /**  Leader creation and updates-
    */
  private void initLeader() {
    
    final VerseLocation home    = expedition.origin ();
    final Faction       faction = expedition.backing();
    
    Background gender = Rand.yes() ? BORN_MALE : BORN_FEMALE;
    final Career c = new Career(KNIGHTED, BORN_HIGH, home, gender);
    Human leader = (Human) expedition.leader();
    
    if (leader == null) leader = new Human(c, faction);
    else                leader.applyCareer(c, faction);
    
    for (Trait t : SELECT_TRAITS) leader.traits.setLevel(t, 0);
    leader.skills.wipeTechniques();
    
    expedition.assignLeader(leader);
    Selection.pushSelectionPane(new ExpeditionPane(UI, expedition), null);
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
    final boolean has = hasTrait(trait);
    leader().traits.setLevel(trait, has ? 0 : trait.maxVal / 2f);
    
    //  TODO:  Include extra techniques related to trait!
    
    updateHelp(trait);
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
    
    updateHelp(gender);
    updateLeaderMedia();
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
    final boolean has = hasPower(power);
    if (has) leader().skills.removeTechnique(power);
    else     leader().skills.addTechnique   (power);
    updateHelp(power);
    updateLeaderMedia();
  }
  
  
  private boolean canToggle(Power power) {
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
  
  
  
  /**  Handling advisor selection-
    */
  private void toggleAdvisor(Background b) {
    final Actor a = expedition.firstMigrant(b);
    if (a != null) expedition.removeMigrant(a);
    else expedition.addAdvisor(b);
  }
  
  
  private boolean hasAdvisor(Background b) {
    return expedition.firstMigrant(b) != null;
  }
  
  
  private boolean canToggleAdvisor(Background b) {
    if (hasAdvisor(b)) return true;
    return numAdvisors() < maxAdvisors();
  }
  
  
  private int numAdvisors() {
    return expedition.advisors().size();
  }
  
  
  private int maxAdvisors() {
    return Expedition.MAX_ADVISORS;
  }
  
  
  
  /**  Handling colonist selection-
    */
  private void incColonists(Background b, int inc) {
    if (inc > 0) expedition.addColonist(b);
    else expedition.removeMigrant(expedition.firstMigrant(b));
  }
  
  
  private boolean canIncColonists(Background b, int inc) {
    if (inc > 0) return numColonists() < maxColonists();
    else return expedition.numMigrants(b) > 0;
  }
  
  
  private int numColonists() {
    return expedition.colonists().size();
  }
  
  
  private int maxColonists() {
    return Expedition.MAX_COLONISTS;
  }
  
  
  
  /**  Other navigation tasks.
    */
  private boolean canProgress() {
    if (numPowers   () != maxPowers   ()) return false;
    if (numTraits   () != maxTraits   ()) return false;
    if (numAdvisors () != maxAdvisors ()) return false;
    if (numColonists() != maxColonists()) return false;
    return true;
  }
  
  
  private void pushNextPane() {
    String prefix = SaveUtils.uniqueVariant(expedition.leader().fullName());
    final StartupScenario newGame = new StartupScenario(expedition, prefix);
    PlayLoop.setupAndLoop(newGame);
  }
}












