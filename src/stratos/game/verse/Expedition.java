/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.verse;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.util.*;
import static stratos.game.actors.Backgrounds.*;



public class Expedition implements Session.Saveable {
  

  /**  Data fields, constants, constructors and save/load methods-
    */
  final public static int
    MAX_ADVISORS  = 2,
    MAX_COLONISTS = 8;
  final public static Background ADVISOR_BACKGROUNDS[] = {
    FIRST_CONSORT,
    MINISTER_FOR_ACCOUNTS,
    WAR_MASTER,  //  TODO:  Include Herald!
  };
  final public static Background COLONIST_BACKGROUNDS[] = {
    VOLUNTEER,
    SUPPLY_CORPS,
    FABRICATOR,
    TECHNICIAN,
    CULTIVATOR,
    MINDER
  };
  final public static String
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
    };
  final public static int
    FUNDING_MINIMAL  = 0,
    FUNDING_STANDARD = 1,
    FUNDING_GENEROUS = 2,
    TITLE_KNIGHTED   = 0,
    TITLE_COUNT      = 1,
    TITLE_BARON      = 2;
  
  
  Faction backing           = Verse.DEFAULT_HOMEWORLD.startingOwner;
  VerseLocation origin      = Verse.DEFAULT_HOMEWORLD;
  VerseLocation destination = Verse.DEFAULT_START_LOCATION;
  
  int funding   ;
  int estateSize;
  int tribute  ;
  
  Table <Background, Integer> positions = new Table();
  List <Actor> applicants = new List();
  Actor leader = null;
  List <Actor> advisors  = new List();
  List <Actor> colonists = new List();
  
  
  
  public Expedition() {
    return;
  }
  
  
  public Expedition(Session s) throws Exception {
    s.cacheInstance(this);
    origin      = (VerseLocation) s.loadObject();
    destination = (VerseLocation) s.loadObject();
    funding    = s.loadInt();
    estateSize = s.loadInt();
    tribute   = s.loadInt();
    leader = (Actor) s.loadObject();
    s.loadObjects(advisors );
    s.loadObjects(colonists);
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject (origin     );
    s.saveObject (destination);
    s.saveInt    (funding    );
    s.saveInt    (estateSize );
    s.saveInt    (tribute    );
    s.saveObject (leader     );
    s.saveObjects(advisors   );
    s.saveObjects(colonists  );
  }
  
  
  
  /**  Basic no-brainer access methods-
    */
  public VerseLocation origin     () { return origin     ; }
  public VerseLocation destination() { return destination; }
  public Faction       backing    () { return backing    ; }
  public int funding   () { return funding   ; }
  public int estateSize() { return estateSize; }
  public int tribute   () { return tribute   ; }
  
  public Actor leader() { return leader; }
  public Series <Actor> advisors () { return advisors ; }
  public Series <Actor> colonists() { return colonists; }
  
  
  
  /**  Configuration utilities for use by the new-game flow-
    */
  public void setOrigin(VerseLocation l, Faction b) {
    this.backing = b;
    this.origin  = l;
  }
  
  
  public void setDestination(VerseLocation l) {
    this.destination = l;
  }
  
  
  public void setFunding(int funding) {
    this.funding = funding;
  }
  
  
  public void setTribute(int tribute) {
    this.tribute = tribute;
  }
  
  
  public void setEstateSize(int size) {
    this.estateSize = size;
  }
  
  
  public void assignLeader(Actor leader) {
    this.leader = leader;
  }
  
  
  public void addAdvisor(Background b) {
    //
    //  We make several attempts to find the 'best' candidate possible for
    //  the job.
    
    I.say("\nAdding advisor: "+b);
    
    final Pick <Actor> pick = new Pick();
    for (int i = 5; i-- > 0;) {
      final Actor candidate = b.sampleFor(backing);
      float rating = 0;
      //
      //  TODO:  Consider including this under an 'evalPromotion' method for
      //  backgrounds, which you could then customise per-job!
      
      if (b == Backgrounds.FIRST_CONSORT && leader != null) {
        rating += leader.motives.attraction(candidate) * 1.0f;
        rating += candidate.motives.attraction(leader) * 0.5f;
        rating += Career.ratePromotion(b, candidate, false)  ;
      }
      else rating += Career.ratePromotion(b, candidate, false);
      
      I.say("  Rating for "+candidate+": "+rating);
      
      pick.compare(candidate, rating);
    }
    if (! pick.empty()) advisors.add(pick.result());
  }
  
  
  public void addColonist(Background b) {
    colonists.add(b.sampleFor(backing));
  }
  
  
  public void removeMigrant(Actor a) {
    if (leader == a) leader = null;
    advisors.remove(a);
    colonists.remove(a);
  }
  
  
  public Actor firstMigrant(Background b) {
    if (leader != null && leader.mind.vocation() == b) return leader;
    for (Actor a : advisors ) if (a.mind.vocation() == b) return a;
    for (Actor a : colonists) if (a.mind.vocation() == b) return a;
    return null;
  }
  
  
  public int numMigrants(Background b) {
    int num = 0;
    if (leader != null && leader.mind.vocation() == b) num++;
    for (Actor a : advisors ) if (a.mind.vocation() == b) num++;
    for (Actor a : colonists) if (a.mind.vocation() == b) num++;
    return num;
  }

  
  
  /**  Configuration utilities for use by in-world Missions-
    */
  public Expedition configFrom(
    VerseLocation origin, VerseLocation destination,
    Faction backing, int funding, int estateSize,
    Series <Actor> applicants
  ) {
    this.backing     = backing    ;
    this.origin      = origin     ;
    this.destination = destination;
    
    leader = null;
    advisors.clear();
    colonists.clear();
    
    for (Actor a : applicants) {
      final Background b = a.mind.vocation();
      if (Visit.arrayIncludes(RULER_CLASSES, b)) {
        leader = a;
      }
      else if (Visit.arrayIncludes(COURT_CIRCLES, b)) {
        advisors.add(a);
      }
      else colonists.add(a);
    }
    
    this.funding    = funding   ;
    this.estateSize = estateSize;
    return this;
  }
  
  
  
  /**  Rendering and interface utilities-
    */
  public String titleDesc() {
    boolean male = leader == null ? true : leader.traits.male();
    if (male) return TITLE_MALE  [estateSize];
    else      return TITLE_FEMALE[estateSize];
  }
}






