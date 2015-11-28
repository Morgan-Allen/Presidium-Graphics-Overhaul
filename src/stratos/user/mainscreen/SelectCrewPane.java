/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user.mainscreen;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.verse.*;
import stratos.graphics.widgets.*;
import stratos.user.Selection;
import stratos.user.mainscreen.MenuPane.TextButton;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;



//  TODO:  Use a shared Expedition reference to maintain persistent data.  And
//  create a UI for that which is displayed by default!

//  TODO:  I will have to use a Faction index instead of a Base for this.  No
//         choice.


public class SelectCrewPane extends MenuPane {
  
  
  
  Stage landing;
  Base base;
  
  Human leader;
  Background gender;
  List <Trait> traitsPicked = new List();
  List <Power> powersPicked = new List();
  
  
  
  public SelectCrewPane(HUD UI) {
    super(UI, MainScreen.MENU_NEW_GAME_CREW);
  }
  
  
  private void initLeader() {
    SelectSitePane oldPane = (SelectSitePane) before();
    if (oldPane == null) return;
    
    if (gender == null) gender = Rand.yes() ?
      Backgrounds.BORN_MALE : Backgrounds.BORN_FEMALE
    ;
    
    final Faction f = oldPane.homeworld.startingOwner;
    final Career c = new Career(
      Backgrounds.KNIGHTED, Backgrounds.BORN_HIGH,
      oldPane.homeworld, gender
    );
    
    if (leader == null) leader = new Human(c, f);
    else leader.assignCareer(c);
    
    Selection.pushSelection(leader, null);
  }
  
  
  protected void fillListing(List <UINode> listing) {
    if (leader == null) initLeader();
    
    listing.add(createTextItem("Traits", 1.2f, null));
    final Trait traits[] = {
      FEARLESS,
      TRADITIONAL,
      PSYONIC
    };
    for (final Trait trait : traits) {
      listing.add(new TextButton(UI, "  "+trait.name, 1) {
        protected void whenClicked() { selectTrait(trait); }
        protected boolean toggled() { return traitsPicked.includes(trait); }
      });
    }
    
    listing.add(createTextItem("Powers", 1.2f, null));
    final Power powers[] = {
      Power.REMOTE_VIEWING,
      Power.FORESIGHT,
      Power.SUSPENSION,
      Power.KINESTHESIA
    };
    for (final Power power : powers) {
      listing.add(new TextButton(UI, "  "+power.name, 1) {
        protected void whenClicked() { selectPower(power); }
        protected boolean toggled() { return powersPicked.includes(power); }
      });
    }
    
    //  List gender & techniques.
    
    //    Male or Female.
    //    Duelist (fearless, bonus combat.)
    //    Word of Honour (traditional, bonus suasion.)
    //    Gifted (may choose extra Power.)
    
    //    Remote Viewing. (Reveals map.)
    //    Time Dilation.  (Slows down time & target.)
    //    Foresight.      (Extra save/s.)
    //    Suspension.     (Heals over time.)
    //    Absorption.     (Better defence/stealth.)
    //    Pyrolysis.      (Deals minor DoT.)
    //    Suggestion.     (Open a dialogue.)
    //    Kinesthesia.    (Boost reflex/speed.)
  }
  
  
  private void selectTrait(Trait trait) {
    traitsPicked.include(trait);
  }
  
  
  private void selectPower(Power power) {
    powersPicked.include(power);
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




