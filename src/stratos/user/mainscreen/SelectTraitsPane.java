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



//  TODO:  Use a shared Expedition reference to maintain persistent data.  And
//  create a UI for that which is displayed by default!


public class SelectTraitsPane extends MenuPane {
  
  
  final static int
    MAX_TRAITS = 2,
    MAX_POWERS = 2;
  
  
  UINode traitHeader, powerHeader;
  
  Stage landing;
  Base base;
  
  Human leader;
  Background gender;
  List <Trait> traitsPicked = new List();
  List <Technique> powersPicked = new List();
  
  
  
  public SelectTraitsPane(HUD UI) {
    super(UI, MainScreen.MENU_NEW_GAME_CREW);
  }
  
  
  private void initLeader() {
    SelectSitePane oldPane = (SelectSitePane) before();
    if (oldPane == null) return;
    
    if (gender == null) gender = Rand.yes() ? BORN_MALE : BORN_FEMALE;
    final Faction f = oldPane.homeworld.startingOwner;
    final Career c = new Career(KNIGHTED, BORN_HIGH, oldPane.homeworld, gender);
    
    if (leader == null) leader = new Human(c, f);
    else leader.assignCareer(c);
  }
  
  
  protected void fillListing(List <UINode> listing) {
    if (leader == null) initLeader();

    listing.add(traitHeader = createTextItem("Traits", 1.2f, null));
    for (final Trait trait : SELECT_TRAITS) {
      listing.add(new TextButton(UI, "  "+trait.name, 1) {
        protected void whenClicked() { toggleTrait(trait); }
        protected boolean toggled() { return traitsPicked.includes(trait); }
      });
    }
    
    final UIGroup sexSwitch = new UIGroup(UI);
    float space = 1f / SELECT_GENDERS.length;
    int gI = 0;
    for (final Background g : SELECT_GENDERS) {
      final TextButton option = new TextButton(UI, "  "+g.name, 1) {
        protected void whenClicked() { setGender(g); }
        protected boolean toggled() { return gender == g; }
      };
      option.alignVertical(0, 0);
      option.alignAcross(space * gI, space * ++gI);
      option.attachTo(sexSwitch);
    }
    sexSwitch.alignTop(0, 20);
    listing.add(sexSwitch);
    
    listing.add(createTextItem(
      "Traits define some of the key strengths and weaknesses of your main "+
      "character.", 0.75f, Colour.LITE_GREY
    ));
    
    listing.add(powerHeader = createTextItem("Powers", 1.2f, null));
    final Power powers[] = {
      Power.REMOTE_VIEWING,
      Power.FORESIGHT,
      Power.SUSPENSION,
      Power.KINESTHESIA
    };
    for (final Power power : powers) {
      listing.add(new TextButton(UI, "  "+power.name, 1) {
        protected void whenClicked() { togglePower(power); }
        protected boolean toggled() { return powersPicked.includes(power); }
      });
    }

    listing.add(createTextItem(
      "Psi Powers allow you to intervene in conflicts remotely, and can be "+
      "learned from Schools.", 0.75f, Colour.LITE_GREY
    ));
  }
  
  
  protected void updateState() {
    final MainScreen screen = MainScreen.current();
    screen.display.showLabels   = true ;
    screen.display.showWeather  = false;
    screen.worldsDisplay.hidden = true ;
    
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
    "noble houses.  +5 to "+ANCIENT_LORE+" and "+NOBLE_ETIQUETTE+".",
    
    "Grants an additional starting Psi Power.  Improves concentration (which "+
    "reduces Powers' cooldown.)"
  };
  final static Background SELECT_GENDERS[] = {
    BORN_FEMALE,
    BORN_MALE
  };
  
  private void toggleTrait(Trait trait) {
    traitsPicked.toggleMember(trait, traitsPicked.includes(trait));
    //trait.whenClicked(null);
    
    int num = traitsPicked.size(), max = MAX_TRAITS;
    updateTextItem(traitHeader, "Traits ("+num+"/"+max+")", null);
  }
  
  
  private void setGender(Background gender) {
    this.gender = gender;
    //gender.whenClicked(null);
  }
  
  
  private void updateActorTraits() {
    //leader.traits.clearAll();
  }
  
  
  
  /**  Handling power selection-
    */
  private void togglePower(Power power) {
    powersPicked.toggleMember(power, ! powersPicked.includes(power));
    //power.whenClicked(null);
    
    int num = powersPicked.size(), max = MAX_POWERS;
    if (traitsPicked.includes(GIFTED)) max += 1;
    updateTextItem(powerHeader, "Powers ("+num+"/"+max+")", null);
  }
  
  
  
  
}



  /*
  public void configForNew(Object args[]) {
    if (this.config == null) this.config = new Expedition();
    text.setText("");
    text.append("Ruler Settings:\n");
    //
    //  TODO:  Give an accompanying description of the House in question, using
    //  a preview image and side-text.
    text.append("\n  House:");
    I.say("  HOUSE IS: "+config.house);
    
    final VerseLocation START_HOUSES[] = {
      Verse.PLANET_ASRA_NOVI,
      Verse.PLANET_AXIS_NOVENA,
      Verse.PLANET_HALIBAN,
      Verse.PLANET_PAREM_V
    };
    if (config.house == null) {
      config.house = (VerseLocation) Rand.pickFrom(START_HOUSES);
    }
    
    for (Background b : START_HOUSES) {
      final VerseLocation s = (VerseLocation) b;
      final Colour c = config.house == s ? Colour.CYAN : null;
      Call.add("\n    "+s.houseName, c, this, "setHouse", text, s);
    }
    
    text.append("\n  Starting Skills: ");
    text.append("("+config.chosenSkills.size()+"/"+MAX_SKILLS+")");
    for (Skill s : Backgrounds.KNIGHTED.skills()) {
      if (Backgrounds.KNIGHTED.skillLevel(s) <= 5) continue;
      final Colour c = config.chosenSkills.includes(s) ? Colour.CYAN : null;
      Call.add("\n    "+s.name, c, this, "toggleSkill", text, s);
    }
    
    text.append("\n  Starting Powers: ");
    text.append("("+config.chosenTechs.size()+"/"+MAX_POWERS+")");
    for (Power p : Power.BASIC_POWERS) {
      final Colour c = config.chosenTechs.includes(p) ? Colour.CYAN : null;
      Call.add("\n    "+p.name, c, this, "togglePower", text, p);
    }
    
    text.append("\n  Favoured traits: ");
    text.append("("+config.chosenTraits.size()+"/"+MAX_TRAITS+")");
    for (Trait t : Backgrounds.KNIGHTED.traits()) {
      final float l = Backgrounds.KNIGHTED.traitChance(t);
      final String name = Trait.descriptionFor(t, l);
      final Colour c = config.chosenTraits.includes(t) ? Colour.CYAN : null;
      Call.add("\n    "+name, c, this, "toggleTrait", text, t);
    }
    
    text.append("\n    ");
    if (config.gender == null) config.gender = BORN_FEMALE;
    Call.add(
      "Male", (config.gender == BORN_MALE) ? Colour.CYAN : null,
      this, "setGender", text, true
    );
    text.append(" | ");
    Call.add(
      "Female", (config.gender == BORN_FEMALE) ? Colour.CYAN : null,
      this, "setGender", text, false
    );
    //
    //  Only allow continuation once all sections are filled-
    String complaint = null;
    if      (config.chosenSkills.size() < MAX_SKILLS) {
      complaint = "Please select "+MAX_SKILLS+" skills";
    }
    else if (config.chosenTraits.size() < MAX_TRAITS) {
      complaint = "Please select "+MAX_TRAITS+" traits";
    }
    else if (config.chosenTechs.size() < MAX_POWERS) {
      complaint = "Please select "+MAX_POWERS+" powers";
    }
    if (complaint == null) {
      text.append("\n\n  Sections complete!");
      Call.add("\n    Continue", this, "configForLanding", text, true);
    }
    else {
      text.append("\n\n  "+complaint, Colour.LITE_GREY);
      text.append("\n    Continue", Colour.LITE_GREY);
    }
    Call.add("\n\n  Go Back", this, "configMainText", text);
  }

//*/




