/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.verse;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.util.*;



public class Expedition implements Session.Saveable {
  
  
  VerseLocation origin      = Verse.PLANET_ASRA_NOVI;
  VerseLocation destination = Verse.SECTOR_ELYSIUM  ;
  int funding   ;
  int estateSize;
  int interest  ;
  
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
    interest   = s.loadInt();
    leader = (Actor) s.loadObject();
    s.loadObjects(advisors );
    s.loadObjects(colonists);
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject (origin     );
    s.saveObject (destination);
    s.saveInt    (funding    );
    s.saveInt    (estateSize );
    s.saveInt    (interest   );
    s.saveObject (leader     );
    s.saveObjects(advisors   );
    s.saveObjects(colonists  );
  }
  
  
  public VerseLocation origin     () { return origin     ; }
  public VerseLocation destination() { return destination; }
  public int funding   () { return funding   ; }
  public int estateSize() { return estateSize; }
  public int interest  () { return interest  ; }
  
  public Actor leader() { return leader; }
  public Series <Actor> advisors () { return advisors ; }
  public Series <Actor> colonists() { return colonists; }
  
  
  public Expedition configFrom(
    VerseLocation origin, VerseLocation destination,
    Series <Actor> applicants, int funding, int estateSize
  ) {
    this.origin      = origin     ;
    this.destination = destination;
    
    leader = null;
    advisors.clear();
    colonists.clear();
    
    for (Actor a : applicants) {
      final Background b = a.mind.vocation();
      if (Visit.arrayIncludes(Backgrounds.RULER_CLASSES, b)) {
        leader = a;
      }
      else if (Visit.arrayIncludes(Backgrounds.COURT_CIRCLES, b)) {
        advisors.add(a);
      }
      else colonists.add(a);
    }
    
    this.funding    = funding   ;
    this.estateSize = estateSize;
    return this;
  }
  
  
  public void assignLeader(Actor leader) {
    this.leader = leader;
  }
  
  
  public void addAdvisor(Background b) {
    colonists.add(b.freeSample());
  }
  
  
  public void addColonist(Background b) {
    colonists.add(b.freeSample());
  }
  
}

















