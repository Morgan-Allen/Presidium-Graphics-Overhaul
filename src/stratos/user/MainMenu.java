

package stratos.user;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.civic.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.start.*;
import stratos.util.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.start.StartupScenario.*;



//  TODO:  This needs a proper re-implementation using customised widgets.
//         You also need to include landing-site-selection.

public class MainMenu extends UIGroup {
  
  final static int
    MODE_INIT            = 0,
    MODE_NEW_GAME        = 1,
    MODE_CONTINUE_GAME   = 2,
    MODE_CHANGE_SETTINGS = 3,
    MODE_CONFIRM_QUIT    = 4;
  
  
  final Text text, help;
  int mode = MODE_INIT;
  private StartupScenario.Config config;
  private XML gameCredits = null;
  
  
  public MainMenu(HUD UI) {
    super(UI);
    text = new Text(UI, UIConstants.INFO_FONT);
    text.alignVertical(0, 0);
    text.alignAcross(0, 0.5f);
    text.scale = 1.25f;
    text.attachTo(this);
    
    help = new Text(UI, UIConstants.INFO_FONT);
    help.alignVertical(0, 0);
    help.alignAcross(0.5f, 1);
    help.scale = 0.75f;
    help.attachTo(this);
    
    configMainText(null);
  }
  
  
  public void configMainText(Object args[]) {
    text.setText("");
    help.setText("");
    Call.add("\n  New Game"       , this, "configForNew"     , text);
    Call.add("\n  Quick Tutorial" , this, "configQuickstart" , text);
    Call.add("\n  Continue Game"  , this, "configToContinue" , text);
    Call.add("\n  Info & Credits" , this, "configInfo"       , text);
    Call.add("\n  Quit"           , this, "configToQuit"     , text);
  }
  
  
  public void configInfo(Object args[]) {
    text.setText("");
    Call.add("\n\nBack", this, "configMainText", text);
    
    if (gameCredits == null) gameCredits = XML.load(
      "media/Help/GameCredits.xml"
    ).matchChildValue("name", "Credits").child("content");
    
    help.setText("");
    help.append(gameCredits.content(), Colour.LITE_GREY);
  }
  
  
  public void configForNew(Object args[]) {
    if (this.config == null) this.config = new StartupScenario.Config();
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
  
  
  public void setGender(Object args[]) {
    config.gender = ((Boolean) args[0]) ? BORN_MALE : BORN_FEMALE;
    
    help.setText("");
    final Background g = config.gender;
    for (Skill s : g.skills()) {
      help.append("\n  "+s.name+" +"+g.skillLevel(s)+" ", Colour.LITE_GREY);
    }
    
    configForNew(null);
  }
  
  
  public void setHouse(Object args[]) {
    config.house = (Background) args[0];
    final VerseLocation location = (VerseLocation) config.house;
    final Colour lite = Colour.LITE_GREY;
    
    help.setText(config.house.info);
    
    help.append("\n\nEconomy: ");
    help.append("\n  Needs: "+I.list(location.goodsNeeded), lite);
    help.append("\n  Makes: "+I.list(location.goodsMade  ), lite);
    
    help.append("\n\nBonus Skills:");
    for (Skill s : config.house.skills()) {
      help.append("\n  ("+s.name+" +5)", lite);
    }
    configForNew(null);
  }
  
  
  public void toggleSkill(Object args[]) {
    final Skill s = (Skill) args[0];
    final List <Skill> skills = config.chosenSkills;
    if (skills.includes(s)) {
      skills.remove(s);
    }
    else {
      skills.addLast(s);
      if (skills.size() > MAX_SKILLS) skills.removeFirst();
    }
    help.setText(s.description);
    configForNew(null);
  }
  
  
  public void toggleTrait(Object args[]) {
    final Trait t = (Trait) args[0];
    final List <Trait> traits = config.chosenTraits;
    if (traits.includes(t)) {
      traits.remove(t);
    }
    else {
      traits.addLast(t);
      if (traits.size() > MAX_TRAITS) traits.removeFirst();
    }
    help.setText(t.description);
    configForNew(null);
  }
  
  
  public void togglePower(Object args[]) {
    final Power p = (Power) args[0];
    final List <Technique> powers = config.chosenTechs;
    if (powers.includes(p)) {
      powers.remove(p);
    }
    else {
      powers.addLast(p);
      if (powers.size() > MAX_POWERS) powers.removeFirst();
    }
    help.setText(p.helpInfo);
    configForNew(null);
  }
  
  
  
  /**  Second page of settings-
    */
  public void configForLanding(Object arg[]) {
    if (arg != null && arg.length > 0) help.setText("");
    
    text.setText("");
    text.append("Expedition Settings:\n");
    
    describePerk(0, config.siteLevel, "Site Type"    , SITE_DESC   );
    describePerk(1, config.fundsLevel, "Funding Level", FUNDING_DESC);
    describePerk(
      2, config.titleLevel, "Granted Title",
      (config.gender == BORN_MALE) ? TITLE_MALE : TITLE_FEMALE
    );
    
    text.append("\n  Colonists:");
    int totalColonists = 0;
    for (Background b : COLONIST_BACKGROUNDS) {
      totalColonists += numCrew(b);
    }
    text.append(" ("+totalColonists+"/"+MAX_COLONISTS+")");
    for (Background b : COLONIST_BACKGROUNDS) {
      text.append("\n    "+numCrew(b)+" ");
      Call.add(" More", Colour.CYAN, this, "incColonists", text, b,  1);
      Call.add(" Less", Colour.CYAN, this, "incColonists", text, b, -1);
      text.append(" "+b.name);
    }
    
    text.append("\n  Household Advisors:");
    text.append(" ("+config.advisors.size()+"/"+MAX_ADVISORS+")");
    for (Background b : ADVISOR_BACKGROUNDS) {
      final Colour c = config.advisors.includes(b) ? Colour.CYAN : null;
      Call.add("\n    "+b.name, c, this, "setAdvisor", text, b);
    }
    
    //  TODO:  INTRODUCE CONFIGURATION FOR OTHER STRUCTURES
    config.built.set(ShieldWall.BLUEPRINT, 1);
    
    
    String complaint = null;
    
    if (perkSpent() < MAX_PERKS) {
      complaint = "Please select a higher site type, funding level or title.";
    }
    else if (totalColonists < MAX_COLONISTS) {
      complaint = "Please select "+MAX_COLONISTS+" colonists.";
    }
    else if (config.advisors.size() < MAX_ADVISORS) {
      complaint = "Please select "+MAX_ADVISORS+" advisors.";
    }
    
    if (complaint == null) {
      text.append("\n\n  Sections complete!");
      Call.add("\n    Begin Game", this, "beginNewGame", text);
    }
    else {
      text.append("\n\n  "+complaint, Colour.LITE_GREY);
      text.append("\n    Begin Game", Colour.LITE_GREY);
    }
    Call.add("\n\n  Go Back", this, "configForNew", text);
  }
  
  
  private int perkSpent() {
    return config.fundsLevel + config.siteLevel + config.titleLevel;
  }
  
  
  private void describePerk(int index, int level, String label, String desc[]) {
    text.append("\n  "+label);
    final int perkLeft = MAX_PERKS + level - perkSpent();
    for (int i = 0; i < 3; i++) {
      text.append("\n    ");
      if (i <= perkLeft) {
        final Colour c = level == i ? Colour.CYAN : null;
        Call.add(desc[i], c, this, "setPerkLevel", text, index, i);
      }
      else text.append(desc[i], Colour.GREY);
    }
  }
  
  
  private String perkHelp(int index, int level) {
    if (index == 0) return "More hospitable sites will have fewer enemies.";
    if (index == 1) return "Starting funds will get you off to a good start.";
    if (index == 2) return "A larger estate leaves more room for expansion.";
    return "NO DESCRIPTION YET";
  }
  
  
  public void setPerkLevel(Object args[]) {
    final int index = (Integer) args[0], level = (Integer) args[1];
    if (index == 0) config.siteLevel  = level;
    if (index == 1) config.fundsLevel = level;
    if (index == 2) config.titleLevel = level;
    help.setText(perkHelp(index, level));
    configForLanding(null);
  }
  
  
  public void setAdvisor(Object args[]) {
    final Background b = (Background) args[0];
    if (config.advisors.includes(b)) {
      config.advisors.remove(b);
    }
    else if (config.advisors.size() < MAX_ADVISORS) {
      config.advisors.add(b);
    }
    help.setText(b.info);
    configForLanding(null);
  }
  
  
  private int numCrew(Background b) {
    final Integer num = (int) config.crew.valueFor(b);
    return num == null ? 0 : num;
  }
  
  
  public void incColonists(Object args[]) {
    int totalColonists = 0;
    for (Background b : COLONIST_BACKGROUNDS) {
      totalColonists += numCrew(b);
    }
    final Background b = (Background) args[0];
    final int inc = (Integer) args[1];
    int amount = numCrew(b);
    if (inc < 0 && amount <= 0) return;
    if (inc > 0 && totalColonists >= MAX_COLONISTS) return;
    config.crew.add(inc, b);
    
    help.setText(b.info);
    configForLanding(null);
  }
  
  
  
  /**  Exits the main menu and starts a new game-
    */
  //
  //  TODO:  Give the player a broad summary of the choices made (including the
  //  name of the ruler/subjects,) before committing to the landing choice.
  public void beginNewGame(Object args[]) {
    final VerseLocation sector = (VerseLocation) config.house;
    final String title = SaveUtils.uniqueVariant(sector.houseName);
    PlayLoop.setupAndLoop(new StartupScenario(config, title));
  }
  
  
  
  /**  Beginning a quick-start game-
    */
  public void configQuickstart(Object args[]) {
    final TutorialScenario tutorial = new TutorialScenario("tutorial_quick");
    PlayLoop.setupAndLoop(tutorial);
  }
  
  
  
  /**  Loading games, settings, and quitting-
    */
  public void configToContinue(Object args[]) {
    text.setText("");
    text.append("\nSaved Games:");
    GameOptionsPane.appendLoadOptions(text, null);
    Call.add("\n\nBack", this, "configMainText", text);
  }
  
  
  public void configForSettings(Object args[]) {
    text.setText("\nChange Settings\n");
    Call.add("\n  Back", this, "configMainText", text);
  }
  
  
  public void configToQuit(Object args[]) {
    text.setText("\nAre you sure you want to quit?\n");
    Call.add("\n  Just Quit Already", this, "quitConfirmed", text);
    Call.add("\n  Back", this, "configMainText", text);
  }
  
  
  public void quitConfirmed(Object args[]) {
    PlayLoop.exitLoop();
  }
}





