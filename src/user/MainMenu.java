

package src.user ;
import src.start.*;
import src.game.base.*;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.*;
import src.game.campaign.* ;
import src.game.campaign.System ;
import src.game.planet.* ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;
import src.util.* ;

import org.lwjgl.opengl.Display ;

import java.io.* ;


//  TODO:  Much of this needs to be moved out to a dedicated Scenario subclass.


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
  
  
  final Text text ;
  int mode = MODE_INIT ;
  
  
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
  
  
  final static int
    MAX_SKILLS    = 3,
    MAX_TRAITS    = 2,
    MAX_ADVISORS  = 2,
    MAX_COLONISTS = 9,
    MAX_PERKS     = 3 ;
  final static Background ADVISOR_BACKGROUNDS[] = {
    Background.FIRST_CONSORT,
    Background.MINISTER_FOR_ACCOUNTS,
    Background.WAR_MASTER
  } ;
  final static Background COLONIST_BACKGROUNDS[] = {
    Background.VOLUNTEER,
    Background.SUPPLY_CORPS,
    Background.FABRICATOR,
    Background.TECHNICIAN,
    Background.CULTIVATOR,
    Background.MEDIC
  } ;
  final static String
    SITE_DESC[] = {
      "Wasteland",
      "Wilderness",
      "Settled"
    },
    FUNDING_DESC[] = {
      "Minimal  (7500  Credits, 3% interest)",
      "Standard (10000 Credits, 2% interest)",
      "Generous (12500 Credits, 1% interest)"
    },
    TITLE_MALE[] = {
      "Knighted Lord (Small Estate)",
      "Count (Typical Estate)",
      "Baron (Large Estate)"
    },
    TITLE_FEMALE[] = {
      "Knighted Lady (Small Estate)",
      "Countess (Typical Estate)",
      "Baroness (Large Estate)"
    } ;
  final static int
    MAP_SIZES[] = { 64, 128, 256 } ;
  
  
  private int gender = 1 ;
  private Background house = Background.PLANET_ASRA_NOVI ;
  private List <Trait> chosenTraits = new List <Trait> () ;
  private List <Skill> chosenSkills = new List <Skill> () ;
  
  private List <Background> advisors = new List <Background> () ;
  private int colonists[] = new int[COLONIST_BACKGROUNDS.length] ;
  
  int perkSpent = 0 ;
  int landPerks[] = new int[3] ;
  
  
  public void configForNew(Object args[]) {
    text.setText("") ;
    
    text.append("\nRuler Settings:\n") ;
    
    text.append("\n  Gender:") ;
    Call.add(
      "\n    Male", gender == 1 ? Colour.CYAN : null,
      this, "setGender", text, true
    ) ;
    Call.add(
      "\n    Female", gender == 0 ? Colour.CYAN : null,
      this, "setGender", text, false
    ) ;
    text.append("\n      ") ;
    final Background g = gender == 1 ?
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
      final Colour c = house == s ? Colour.CYAN : null ;
      Call.add("\n    "+s.houseName, c, this, "setHouse", text, s) ;
    }
    for (Skill s : house.skills()) {
      text.append("\n      ("+s.name+" +5)", Colour.LIGHT_GREY) ;
    }
    
    text.append("\n  Favoured skills: ") ;
    text.append("("+chosenSkills.size()+"/"+MAX_SKILLS+")") ;
    for (Skill s : Background.KNIGHTED.skills()) {
      if (Background.KNIGHTED.skillLevel(s) <= 5) continue ;
      final Colour c = chosenSkills.includes(s) ? Colour.CYAN : null ;
      Call.add("\n    "+s.name, c, this, "toggleSkill", text, s) ;
    }
    
    text.append("\n  Favoured traits: ") ;
    text.append("("+chosenTraits.size()+"/"+MAX_TRAITS+")") ;
    for (Trait t : Background.KNIGHTED.traits()) {
      final float l = Background.KNIGHTED.traitChance(t) > 0 ? 2 : -2 ;
      final String name = Trait.descriptionFor(t, l) ;
      final Colour c = chosenTraits.includes(t) ? Colour.CYAN : null ;
      Call.add("\n    "+name, c, this, "toggleTrait", text, t) ;
    }
    
    //
    //  Only allow continuance once all sections are filled-
    boolean canProceed = true ;
    if (chosenSkills.size() < MAX_SKILLS) canProceed = false ;
    if (chosenTraits.size() < MAX_TRAITS) canProceed = false ;
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
    gender = ((Boolean) args[0]) ? 1 : 0 ;
    I.say("Gender is now: "+gender) ;
    configForNew(null) ;
  }
  
  
  public void setHouse(Object args[]) {
    house = (Background) args[0] ;
    I.say("House is now: "+house) ;
    configForNew(null) ;
  }
  
  
  public void toggleSkill(Object args[]) {
    final Skill s = (Skill) args[0] ;
    if (chosenSkills.includes(s)) {
      chosenSkills.remove(s) ;
    }
    else {
      chosenSkills.addLast(s) ;
      if (chosenSkills.size() > MAX_SKILLS) chosenSkills.removeFirst() ;
    }
    configForNew(null) ;
  }
  
  
  public void toggleTrait(Object args[]) {
    final Trait t = (Trait) args[0] ;
    if (chosenTraits.includes(t)) {
      chosenTraits.remove(t) ;
    }
    else {
      chosenTraits.addLast(t) ;
      if (chosenTraits.size() > MAX_TRAITS) chosenTraits.removeFirst() ;
    }
    configForNew(null) ;
  }
  
  
  /**  Second page of settings-
    */
  public void configForLanding(Object arg[]) {
    text.setText("") ;
    text.append("\nExpedition Settings:\n") ;
    
    describePerk(0, "Site Type"    , SITE_DESC   ) ;
    describePerk(1, "Funding Level", FUNDING_DESC) ;
    describePerk(2, "Granted Title", gender == 1 ? TITLE_MALE : TITLE_FEMALE) ;
    
    text.append("\n  Colonists:") ;
    int totalColonists = 0 ; for (int i : colonists) totalColonists += i ;
    text.append(" ("+totalColonists+"/"+MAX_COLONISTS+")") ;
    int i = 0 ; for (Background b : COLONIST_BACKGROUNDS) {
      text.append("\n    "+colonists[i++]+" ") ;
      Call.add(" More", Colour.CYAN, this, "incColonists", text, b,  1) ;
      Call.add(" Less", Colour.CYAN, this, "incColonists", text, b, -1) ;
      text.append(" "+b.name) ;
    }
    
    text.append("\n  Accompanying Household:") ;
    text.append(" ("+advisors.size()+"/"+MAX_ADVISORS+")") ;
    for (Background b : ADVISOR_BACKGROUNDS) {
      final Colour c = advisors.includes(b) ? Colour.CYAN : null ;
      Call.add("\n    "+b.name, c, this, "setAdvisor", text, b) ;
    }
    
    boolean canBegin = true ;
    if (perkSpent < MAX_PERKS) canBegin = false ;
    if (totalColonists < MAX_COLONISTS) canBegin = false ;
    if (advisors.size() < MAX_ADVISORS) canBegin = false ;
    
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
  
  
  private void describePerk(int index, String label, String desc[]) {
    text.append("\n  "+label) ;
    final int perkLeft = MAX_PERKS + landPerks[index] - perkSpent ;
    for (int i = 0 ; i < 3 ; i++) {
      text.append("\n    ") ;
      if (i <= perkLeft) {
        final Colour c = landPerks[index] == i ? Colour.CYAN : null ;
        Call.add(desc[i], c, this, "setPerkLevel", text, index, i) ;
      }
      else text.append(desc[i], Colour.GREY) ;
    }
  }
  
  
  public void setAdvisor(Object args[]) {
    final Background b = (Background) args[0] ;
    if (advisors.includes(b)) {
      advisors.remove(b) ;
    }
    else if (advisors.size() < MAX_ADVISORS) {
      advisors.add(b) ;
    }
    configForLanding(null) ;
  }
  
  
  public void incColonists(Object args[]) {
    final Background b = (Background) args[0] ;
    final int
      inc = (Integer) args[1],
      index = Visit.indexOf(b, COLONIST_BACKGROUNDS) ;
    int amount = colonists[index], total = 0 ;
    for (int i : colonists) total += i ;
    if (inc < 0 && amount <= 0) return ;
    if (inc > 0 && total >= MAX_COLONISTS) return ;
    colonists[index] += inc ;
    configForLanding(null) ;
  }
  
  
  public void setPerkLevel(Object args[]) {
    final int index = (Integer) args[0], level = (Integer) args[1] ;
    landPerks[index] = level ;
    perkSpent = 0 ;
    for (int i : landPerks) perkSpent += i ;
    configForLanding(null) ;
  }
  
  
  
  /**  Exits the main menu and starts a new game-
    */
  //
  //  TODO:  Give the player a broad summary of the choices made (including the
  //  name of the ruler/subjects,) before committing to the landing choice.
  public void beginNewGame(Object args[]) {
    PlayLoop.setupAndLoop(new Scenario() {
      protected World createWorld() {
        return generateWorld();
      }
      
      protected Base createBase(World world) {
        return generateBase(world);
      }
      
      protected void configureScenario(World world, Base base, BaseUI UI) {
        generateScenario(world, base, UI);
      }
      
      protected String saveFilePrefix(World world, Base base) {
        String title = base.ruler().fullName() ;
        while (true) {
          File match = new File(Scenario.fullSavePath(title, null)) ;
          if (! match.exists()) break ;
          title = title+"I" ;
        }
        return title;
      }
      
      protected void afterCreation() {
        saveProgress(false);
      }
    });
  }
  
  
  private World generateWorld() {
    final int station = landPerks[2] ;
    float water = 2 ;
    float forest = 0, meadow = 0, barrens = 0, desert = 0, wastes = 0 ;
    
    switch (landPerks[0]) {
      case(0) : wastes = 3 ; desert  = 2 ; barrens = 4 ; water = 0 ; break ;
      case(1) : meadow = 4 ; barrens = 2 ; desert  = 2 ; water = 3 ; break ;
      case(2) : forest = 2 ; meadow  = 3 ; barrens = 2 ; water = 1 ; break ;
    }
    
    final TerrainGen TG = new TerrainGen(
      MAP_SIZES[station], 0.2f,
      Habitat.OCEAN       , water  ,
      Habitat.ESTUARY     , forest ,
      Habitat.MEADOW      , meadow ,
      Habitat.BARRENS     , barrens,
      Habitat.DUNE      , desert ,
      Habitat.CURSED_EARTH, wastes
    ) ;
    final World world = new World(TG.generateTerrain()) ;
    TG.setupMinerals(world, 0, 0, 0) ;
    TG.setupOutcrops(world) ;
    
    //
    //  TODO:  Generate ecology as well!
    return world ;
  }
  
  
  private Base generateBase(World world) {
    final Base base = Base.createFor(world, false) ;
    
    int funding = -1, interest = -1 ;
    switch (landPerks[1]) {
      case(0) : funding = 7500  ; interest = 3 ; break ;
      case(1) : funding = 10000 ; interest = 2 ; break ;
      case(2) : funding = 12500 ; interest = 1 ; break ;
    }
    base.incCredits(funding) ;
    base.setInterestPaid(interest) ;
    base.commerce.assignHomeworld((System) house) ;
    return base ;
  }
  
  
  protected void generateScenario(World world, Base base, BaseUI UI) {
    //
    //  Determine relevant attributes for the ruler-
    final int station = landPerks[2] ;
    final float promoteChance = (25 - (station * 10)) / 100f ;
    final Background vocation = Background.RULING_POSITIONS[station] ;
    final Background birth ;
    if (Rand.num() < promoteChance) {
      if (Rand.num() < promoteChance) birth = Background.FREE_BIRTH ;
      else birth = Background.GELDER_BIRTH ;
    }
    else birth = Background.HIGH_BIRTH ;
    
    final boolean male = gender == 1 ;
    final Career rulerCareer = new Career(male, vocation, birth, house) ;
    final Human ruler = new Human(rulerCareer, base) ;
    for (Skill s : house.skills()) ruler.traits.incLevel(s, 5) ;
    
    //
    //  Try to pick out some complementary advisors-
    final List <Human> advisors = new List <Human> () ;
    final int numTries = 5 ;
    for (Background b : this.advisors) {
      Human picked = null ;
      float bestRating = Float.NEGATIVE_INFINITY ;
      
      for (int i = numTries ; i-- > 0 ;) {
        final Human candidate = new Human(b, base) ;
        float rating = Career.ratePromotion(b, candidate) ;
        
        if (b == Background.FIRST_CONSORT) {
          rating +=
            ruler.mind.attraction(candidate) +
            (candidate.mind.attraction(ruler) / 2) ;
        }
        if (rating > bestRating) { picked = candidate ; bestRating = rating ; }
      }
      if (picked != null) advisors.add(picked) ;
    }
    //
    //  Pick out some random colonists-
    final List <Human> colonists = new List <Human> () ;
    for (int i = COLONIST_BACKGROUNDS.length ; i-- > 0 ;) {
      for (int n = this.colonists[i] ; n-- > 0 ;) {
        final Human c = new Human(COLONIST_BACKGROUNDS[i], base) ;
        for (Skill s : house.skills()) if (c.traits.traitLevel(s) > 0) {
          c.traits.incLevel(s, 5) ;
        }
        colonists.add(c) ;
      }
    }
    
    final Bastion bastion = establishBastion(
      world, base, ruler, advisors, colonists
    ) ;
    UI.assignBaseSetup(base, bastion.position(null)) ;
  }
  
  
  
  /**  Beginning a quick-start game-
    */
  public void configQuickstart(Object args[]) {
    //  TODO:  This will need to be replaced with a dedicated external
    //  TutorialScenario class.
    
    PlayLoop.setupAndLoop(new Scenario() {
      
      protected World createWorld() {
        final TerrainGen TG = new TerrainGen(
          64, 0.0f,
          Habitat.ESTUARY, 0.15f,
          Habitat.MEADOW , 0.35f,
          Habitat.BARRENS, 0.35f,
          Habitat.DUNE , 0.15f
        ) ;
        final World world = new World(TG.generateTerrain()) ;
        TG.setupMinerals(world, 0, 0, 0) ;
        TG.setupOutcrops(world) ;
        return world;
      }
      
      
      protected Base createBase(World world) {
        final Base base = Base.createFor(world, false) ;
        return base;
      }
      
      
      protected void configureScenario(World world, Base base, BaseUI UI) {
        final Human ruler = new Human(new Career(
          Rand.yes(), Background.KNIGHTED, Background.HIGH_BIRTH,
          (Background) Rand.pickFrom(Background.ALL_PLANETS)
        ), base) ;
        final Human consort = new Human(new Career(
          ruler.traits.female(), Background.FIRST_CONSORT,
          Background.HIGH_BIRTH, ruler.career().homeworld()
        ), base) ;
        
        final List <Human> advisors = new List <Human> () ;
        advisors.add(ruler) ;
        advisors.add(consort) ;
        final List <Human> colonists = new List <Human> () ;
        for (int n = 2 ; n-- > 0 ;) {
          colonists.add(new Human(Background.VOLUNTEER  , base)) ;
          colonists.add(new Human(Background.SUPPLY_CORPS, base)) ;
          colonists.add(new Human(Background.TECHNICIAN  , base)) ;
        }
        for (int n = 3 ; n-- > 0 ;) {
          final Background b = (Background) Rand.pickFrom(COLONIST_BACKGROUNDS) ;
          colonists.add(new Human(b, base)) ;
        }
        for (Human c : colonists) {
          for (Skill s : house.skills()) if (c.traits.traitLevel(s) > 0) {
            c.traits.incLevel(s, 5) ;
          }
        }
        
        base.incCredits(10000) ;
        base.commerce.assignHomeworld((System) house) ;
        base.setInterestPaid(2) ;
        base.commerce.assignHomeworld((System) ruler.career().homeworld()) ;
        
        final Bastion bastion = establishBastion(
          world, base, ruler, advisors, colonists
        ) ;
        UI.assignBaseSetup(base, bastion.position(null)) ;
      }
      
      
      protected String saveFilePrefix(World world, Base base) {
        String title = base.ruler().fullName() ;
        while (true) {
          File match = new File(Scenario.fullSavePath(title, null)) ;
          if (! match.exists()) break ;
          title = title+"I" ;
        }
        return title;
      }
      
      
      protected void afterCreation() {
        //saveProgress(false);
      }
    });
  }
  
  
  
  private Bastion establishBastion(
    World world, Base base,
    Human ruler, List <Human> advisors, List <Human> colonists
  ) {
    //
    //  And finally, initiate the settlement within the world-
    final Bastion bastion = new Bastion(base) ;
    advisors.add(ruler) ;
    base.assignRuler(ruler) ;
    Human AA[] = advisors.toArray(Human.class) ;
    
    //
    //  TODO:  Place lairs away from the bastion.
    
    Placement.establishVenue(bastion, 12, 12, true, world, AA) ;
    bastion.clearSurrounds() ;
    for (Actor a : advisors) {
      a.mind.setHome(bastion) ;
    }
    for (Actor a : colonists) {
      a.assignBase(base) ;
      a.enterWorldAt(bastion, world) ;
      a.goAboard(bastion, world) ;
    }
    return bastion ;
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





