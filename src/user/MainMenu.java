

package src.user ;
import src.start.*;
import src.game.base.*;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.*;
import src.game.planet.* ;

import src.game.campaign.* ;
import src.game.campaign.System ;
import static src.game.campaign.StartupScenario.*;

import src.graphics.common.* ;
import src.graphics.widgets.* ;
import src.util.*;

//import org.lwjgl.opengl.Display;
import java.io.*;



//  TODO:  Much of this needs to be moved out to a dedicated Scenario subclass.
/*
private int gender = 1 ;
private Background house = Background.PLANET_ASRA_NOVI ;
private List <Trait> chosenTraits = new List <Trait> () ;
private List <Skill> chosenSkills = new List <Skill> () ;

private List <Background> advisors = new List <Background> () ;
private int colonists[] = new int[COLONIST_BACKGROUNDS.length] ;

int perkSpent = 0 ;
int landPerks[] = new int[3] ;
//*/



public class MainMenu extends UIGroup {
  
  //
  //  Okay.  I need options for:
  //    Starting a new game  (Ideally, with a basic char & world-gen system.)
  //    Resuming an existing game
  //    Modifying settings
  //    Quitting
  
  final static int
    MODE_INIT            = 0,
    MODE_NEW_GAME        = 1,
    MODE_CONTINUE_GAME   = 2,
    MODE_CHANGE_SETTINGS = 3,
    MODE_CONFIRM_QUIT    = 4 ;
  
  
  final Text text;
  int mode = MODE_INIT;
  private StartupScenario.Config config;
  //private StartupScenario scenario;
  
  
  public MainMenu(HUD UI) {
    super(UI) ;
    text = new Text(UI, UIConstants.INFO_FONT) ;
    text.relBound.set(0, 0, 1, 1) ;
    text.scale = 1.25f ;
    text.attachTo(this) ;
    configMainText(null) ;
  }
  
  
  public void configMainText(Object args[]) {
    text.setText("") ;
    //
    //  TODO:  Add a Tutorial/Quickstart option?
    Call.add("\n  New Game"       , this, "configForNew"     , text) ;
    Call.add("\n  Quick Tutorial" , this, "configQuickstart" , text) ;
    Call.add("\n  Continue Game"  , this, "configToContinue" , text) ;
    //Call.add("\n  Change Settings", this, "configForSettings", text) ;
    Call.add("\n  Quit"           , this, "configToQuit"     , text) ;
  }
  
  
  public void configForNew(Object args[]) {
    
    config = new StartupScenario.Config();
    text.setText("");
    text.append("\nRuler Settings:\n");
    
    text.append("\n  Gender:");
    Call.add(
      "\n    Male", config.male ? Colour.CYAN : null,
      this, "setGender", text, true
    ) ;
    Call.add(
      "\n    Female", (! config.male) ? Colour.CYAN : null,
      this, "setGender", text, false
    ) ;
    text.append("\n      ") ;
    final Background g = config.male ?
        Background.MALE_BIRTH :
        Background.FEMALE_BIRTH ;
    for (Skill s : g.skills()) {
      text.append(s.name+" +"+g.skillLevel(s)+" ", Colour.LIGHT_GREY) ;
    }
    
    //
    //  TODO:  Give an accompanying description of the House in question, using
    //  a preview image and side-text.
    
    text.append("\n  House:") ;
    for (Background b : Background.ALL_PLANETS) {
      final System s = (System) b ;
      final Colour c = config.house == s ? Colour.CYAN : null ;
      Call.add("\n    "+s.houseName, c, this, "setHouse", text, s) ;
    }
    for (Skill s : config.house.skills()) {
      text.append("\n      ("+s.name+" +5)", Colour.LIGHT_GREY) ;
    }
    
    text.append("\n  Favoured skills: ") ;
    text.append("("+config.chosenSkills.size()+"/"+MAX_SKILLS+")") ;
    for (Skill s : Background.KNIGHTED.skills()) {
      if (Background.KNIGHTED.skillLevel(s) <= 5) continue ;
      final Colour c = config.chosenSkills.includes(s) ? Colour.CYAN : null ;
      Call.add("\n    "+s.name, c, this, "toggleSkill", text, s) ;
    }
    
    text.append("\n  Favoured traits: ") ;
    text.append("("+config.chosenTraits.size()+"/"+MAX_TRAITS+")") ;
    for (Trait t : Background.KNIGHTED.traits()) {
      final float l = Background.KNIGHTED.traitChance(t) > 0 ? 2 : -2 ;
      final String name = Trait.descriptionFor(t, l) ;
      final Colour c = config.chosenTraits.includes(t) ? Colour.CYAN : null ;
      Call.add("\n    "+name, c, this, "toggleTrait", text, t) ;
    }
    
    //
    //  Only allow continuation once all sections are filled-
    boolean canProceed = true ;
    if (config.chosenSkills.size() < MAX_SKILLS) canProceed = false ;
    if (config.chosenTraits.size() < MAX_TRAITS) canProceed = false ;
    if (canProceed) {
      text.append("\n\n  (Sections complete)", Colour.LIGHT_GREY) ;
      Call.add("\n    Expedition Settings", this, "configForLanding", text) ;
    }
    else {
      text.append("\n\n  (Please fill all sections)", Colour.LIGHT_GREY) ;
      text.append("\n    Expedition Settings", Colour.LIGHT_GREY) ;
    }
    Call.add("\n  Go Back", this, "configMainText", text) ;
  }
  
  
  public void setGender(Object args[]) {
    config.male = (Boolean) args[0] ;
    configForNew(null) ;
  }
  
  
  public void setHouse(Object args[]) {
    config.house = (Background) args[0] ;
    configForNew(null) ;
  }
  
  
  public void toggleSkill(Object args[]) {
    final Skill s = (Skill) args[0] ;
    final List <Skill> skills = config.chosenSkills;
    if (skills.includes(s)) {
      skills.remove(s);
    }
    else {
      skills.addLast(s);
      if (skills.size() > MAX_SKILLS) skills.removeFirst() ;
    }
    configForNew(null) ;
  }
  
  
  public void toggleTrait(Object args[]) {
    final Trait t = (Trait) args[0] ;
    final List <Trait> traits = config.chosenTraits;
    if (traits.includes(t)) {
      traits.remove(t) ;
    }
    else {
      traits.addLast(t) ;
      if (traits.size() > MAX_TRAITS) traits.removeFirst() ;
    }
    configForNew(null) ;
  }
  
  
  
  /**  Second page of settings-
    */
  private int perkSpent() {
    return config.fundsLevel + config.siteLevel + config.titleLevel;
  }
  
  
  private void describePerk(int index, int level, String label, String desc[]) {
    text.append("\n  "+label) ;
    final int perkLeft = MAX_PERKS + level - perkSpent() ;
    for (int i = 0 ; i < 3 ; i++) {
      text.append("\n    ") ;
      if (i <= perkLeft) {
        final Colour c = level == i ? Colour.CYAN : null ;
        Call.add(desc[i], c, this, "setPerkLevel", text, index, i) ;
      }
      else text.append(desc[i], Colour.GREY) ;
    }
  }
  
  
  public void setPerkLevel(Object args[]) {
    final int index = (Integer) args[0], level = (Integer) args[1] ;
    if (index == 0) config.siteLevel  = level;
    if (index == 1) config.fundsLevel = level;
    if (index == 2) config.titleLevel = level;
    configForLanding(null) ;
  }
  
  
  public void configForLanding(Object arg[]) {
    text.setText("") ;
    text.append("\nExpedition Settings:\n") ;
    
    describePerk(0, config.siteLevel, "Site Type"    , SITE_DESC   ) ;
    describePerk(1, config.fundsLevel, "Funding Level", FUNDING_DESC) ;
    describePerk(
      2, config.titleLevel, "Granted Title",
      config.male ? TITLE_MALE : TITLE_FEMALE
    ) ;
    
    text.append("\n  Colonists:") ;
    int totalColonists = 0 ;
    for (Background b : COLONIST_BACKGROUNDS) {
      totalColonists += config.numCrew(b);
    }
    text.append(" ("+totalColonists+"/"+MAX_COLONISTS+")") ;
    int i = 0 ;
    for (Background b : COLONIST_BACKGROUNDS) {
      text.append("\n    "+config.numCrew(b)+" ") ;
      Call.add(" More", Colour.CYAN, this, "incColonists", text, b,  1) ;
      Call.add(" Less", Colour.CYAN, this, "incColonists", text, b, -1) ;
      text.append(" "+b.name) ;
    }
    
    text.append("\n  Accompanying Household:") ;
    text.append(" ("+config.advisors.size()+"/"+MAX_ADVISORS+")") ;
    for (Background b : ADVISOR_BACKGROUNDS) {
      final Colour c = config.advisors.includes(b) ? Colour.CYAN : null ;
      Call.add("\n    "+b.name, c, this, "setAdvisor", text, b) ;
    }
    
    boolean canBegin = true ;
    if (perkSpent() < MAX_PERKS) canBegin = false ;
    if (totalColonists < MAX_COLONISTS) canBegin = false ;
    if (config.advisors.size() < MAX_ADVISORS) canBegin = false ;
    
    if (canBegin) {
      text.append("\n\n  (Sections complete)", Colour.LIGHT_GREY) ;
      Call.add("\n    Begin Game", this, "beginNewGame", text) ;
    }
    else {
      text.append("\n\n  (Please fill all sections)", Colour.LIGHT_GREY) ;
      text.append("\n    Begin Game", Colour.LIGHT_GREY) ;
    }
    Call.add("\n  Go Back", this, "configForNew", text) ;
  }
  
  
  public void setAdvisor(Object args[]) {
    final Background b = (Background) args[0] ;
    if (config.advisors.includes(b)) {
      config.advisors.remove(b) ;
    }
    else if (config.advisors.size() < MAX_ADVISORS) {
      config.advisors.add(b) ;
    }
    configForLanding(null) ;
  }
  
  
  public void incColonists(Object args[]) {
    int totalColonists = 0;
    for (Background b : COLONIST_BACKGROUNDS) {
      totalColonists += config.numCrew(b);
    }
    final Background b = (Background) args[0];
    final int inc = (Integer) args[1];
    int amount = config.numCrew(b);
    if (inc < 0 && amount <= 0) return;
    if (inc > 0 && totalColonists >= MAX_COLONISTS) return;
    config.numCrew.put(b, amount + inc);
    configForLanding(null);
  }
  
  
  
  /**  Exits the main menu and starts a new game-
    */
  //
  //  TODO:  Give the player a broad summary of the choices made (including the
  //  name of the ruler/subjects,) before committing to the landing choice.
  public void beginNewGame(Object args[]) {
    PlayLoop.setupAndLoop(new StartupScenario(config));
  }
  
  
  
  /**  Beginning a quick-start game-
    */
  public void configQuickstart(Object args[]) {
    config = new StartupScenario.Config();
    config.house = (Background) Rand.pickFrom(Background.ALL_PLANETS);
    config.male = Rand.yes();
    
    config.numCrew.put(Background.VOLUNTEER, 2);
    config.numCrew.put(Background.SUPPLY_CORPS, 2);
    config.numCrew.put(Background.TECHNICIAN, 2);
    for (int n = 3 ; n-- > 0 ;) {
      final Background b = (Background) Rand.pickFrom(COLONIST_BACKGROUNDS);
      config.numCrew.put(b, config.numCrew(b) + 1);
    }
    config.numCrew.put(Background.FIRST_CONSORT, 1);
    
    config.siteLevel  = 1;
    config.titleLevel = 1;
    config.fundsLevel = 1;
    
    PlayLoop.setupAndLoop(new StartupScenario(config));
  }
  
  
  
  /**  Loading games, settings, and quitting-
    */
  public void configToContinue(Object args[]) {
    text.setText("") ;
    
    text.append("\n  Saved Games:") ;
    for (String fileName : Scenario.savedFiles(null)) {
      text.append("\n    ") ;
      int cutoff = (Scenario.CURRENT_SAVE+".rep").length() ;
      String playName = fileName.substring(0, fileName.length() - cutoff) ;
      Call.add(playName, this, "loadSavedGame", text, fileName) ;
    }
    
    Call.add("\n\n  Back", this, "configMainText", text) ;
  }
  
  
  public void loadSavedGame(Object args[]) {
    final String fullPath = "saves/"+(String) args[0] ;
    I.say("Loading game: "+fullPath) ;
    Scenario.loadGame(fullPath, true);
    /*
    try {
      final Session s = Session.loadSession(fullPath) ;
      final Scenario scenario = s.scenario() ;
      PlayLoop.setupAndLoop(scenario.UI(), scenario) ;
    }
    catch (Exception e) { I.report(e) ; }
    //*/
  }
  
  
  //
  //  TODO:  Provide a couple of settings to tweak:  Difficulty, hardness, and
  //  drama rating...  Ha!
  
  public void configForSettings(Object args[]) {
    text.setText("\nChange Settings\n") ;
    Call.add("\n  Back", this, "configMainText", text) ;
  }
  
  
  public void configToQuit(Object args[]) {
    text.setText("\nAre you sure you want to quit?\n") ;
    Call.add("\n  Just Quit Already", this, "quitConfirmed", text) ;
    Call.add("\n  Back", this, "configMainText", text) ;
  }
  
  
  public void quitConfirmed(Object args[]) {
    PlayLoop.exitLoop() ;
  }
}





