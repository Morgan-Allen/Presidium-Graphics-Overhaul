/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.economic;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.game.maps.*;
import stratos.util.*;



public class Staff {
  
  /**  Fields, constructors, and save/load methods-
    */
  private static boolean
    verbose = false;
  
  final static int
    REFRESH_INTERVAL = 10,
    AUDIT_INTERVAL   = Stage.STANDARD_DAY_LENGTH / 10;
  
  
  final Property employs;
  final List <Actor>
    workers = new List <Actor> (),
    lodgers = new List <Actor> ();
  final List <FindWork>
    applications = new List <FindWork> ();
  private int shiftType = -1;
  
  
  
  Staff(Property venue) {
    this.employs = venue;
  }
  
  
  void loadState(Session s) throws Exception {
    shiftType = s.loadInt();
    s.loadObjects(applications);
    s.loadObjects(workers);
    s.loadObjects(lodgers);
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveInt(shiftType);
    s.saveObjects(applications);
    s.saveObjects(workers);
    s.saveObjects(lodgers);
  }
  
  
  /**  Queries on workers and lodgers-
    */
  public List <Actor> workers() {
    return workers;
  }
  
  
  public List <Actor> lodgers() {
    return lodgers;
  }
  
  
  public void setShiftType(int type) {
    this.shiftType = type;
  }
  
  
  public boolean unoccupied() {
    return lodgers.size() == 0 && workers.size() == 0;
  }
  
  
  public int population() {
    return lodgers.size();
  }
  
  
  public int workforce() {
    int count = 0;
    for (Actor a : workers) if (a.inWorld()) count++;
    return count;
  }
  
  
  public int workingOrOffworld() {
    return workers.size();
  }
  
  
  public int manning() {
    int count = 0;
    for (Actor a : workers) if (a.aboard() == employs) count++;
    return count;
  }
  
  
  public boolean isLodger(Mobile a) {
    if (a instanceof Actor && ((Actor) a).mind.home() == employs) return true;
    return false;
  }
  
  
  public boolean isWorker(Mobile a) {
    if (a instanceof Actor && ((Actor) a).mind.work() == employs) return true;
    return false;
  }
  
  
  public static boolean doesBelong(Mobile a, Target to) {
    if (! (to instanceof Property)) return false;
    final Property p = (Property) to;
    return p.staff().isWorker(a) || p.staff().isLodger(a);
  }
  
  
  public int numPositions(Background b) {
    if (employs instanceof Venue) {
      return ((Venue) employs).numPositions(b);
    }
    return 0;
  }
  
  
  public int numOpenings(Background b) {
    return numPositions(b) - numHired(b);
  }
  
  
  public int numApplied(Background b) {
    int count = 0;
    for (FindWork a : applications) if (a.position() == b) count++;
    return count;
  }
  
  
  public int numHired(Background match) {
    int num = 0; for (Actor c : workers) {
      if (c.mind.vocation() == match) num++;
    }
    return num;
  }
  
  
  public Batch <Actor> hiredAs(Background match) {
    final Batch <Actor> matches = new Batch <Actor> ();
    for (Actor c : workers) if (c.mind.vocation() == match) matches.add(c);
    return matches;
  }
  
  
  public int numResident(Background match) {
    int num = 0; for (Actor c : lodgers) {
      if (c.mind.vocation() == match) num++;
    }
    return num;
  }
  
  
  
  /**  Handling shifts and being off-duty:
    */
  public int shiftFor(Actor worker) {
    final boolean report = verbose && (
      I.talkAbout == worker || I.talkAbout == employs
    );
    
    if (shiftType == -1) return Venue.OFF_DUTY;
    if (shiftType == Venue.SHIFTS_ALWAYS) {
      return Venue.PRIMARY_SHIFT;
    }
    final Stage world = employs.base().world;
    final int hireIndex = workers.indexOf(worker);
    if (hireIndex == -1) return Venue.NOT_HIRED;
    
    //
    //  Simplified versions in use for the present...
    if (shiftType == Venue.SHIFTS_BY_HOURS) {
      final int day = (int) (world.currentTime() / Stage.STANDARD_DAY_LENGTH);
      final int index = (hireIndex + day) % 3;
      final int hour =
        Planet.isMorning(world) ? 1 :
        (Planet.isEvening(world) ? 2 : 0);
      
      if (index == hour) return Venue.PRIMARY_SHIFT;
      else if (index == (hour + 1) % 3) return Venue.SECONDARY_SHIFT;
      else return Venue.OFF_DUTY;
    }
    
    if (shiftType == Venue.SHIFTS_BY_24_HOUR) {
      final int day = (int) (world.currentTime() / Stage.STANDARD_DAY_LENGTH);
      if (day % 3 ==  hireIndex      % 3) return Venue.PRIMARY_SHIFT  ;
      if (day % 3 == (hireIndex + 1) % 3) return Venue.SECONDARY_SHIFT;
      return Venue.OFF_DUTY;
    }
    
    if (shiftType == Venue.SHIFTS_BY_DAY) {
      final int day = (int) (world.currentTime() / Stage.STANDARD_DAY_LENGTH);
      if (report) {
        I.say("\nGetting day-based shift for "+worker);
        I.say("  Day count: "+day);
        I.say("  Worker ID: "+hireIndex);
        I.say("  At night?  "+Planet.isNight(world));
      }
      
      if (Planet.isNight(world)) return Venue.OFF_DUTY;
      else if ((hireIndex % 3) == (day % 3) || Planet.dayValue(world) < 0.5f) {
        return Venue.SECONDARY_SHIFT;
      }
      else return Venue.PRIMARY_SHIFT;
    }
    
    if (shiftType == Venue.SHIFTS_BY_CALENDAR) {
      I.complain("CALENDAR NOT IMPLEMENTED YET.");
    }
    
    return Venue.OFF_DUTY;
  }
  
  
  public boolean onShift(Actor worker) {
    if (! employs.structure().intact()) return false;
    return shiftFor(worker) == Venue.PRIMARY_SHIFT;
  }
  
  
  public boolean offDuty(Actor worker) {
    if (! employs.structure().intact()) return true;
    return shiftFor(worker) == Venue.OFF_DUTY;
  }
  
  
  public int assignedTo(Class planClass) {
    int num = 0; for (Actor actor : workers) {
      if (actor.isDoing(planClass, null)) num++;
    }
    return num;
  }
  
  
  public int assignedTo(Plan matchPlan) {
    if (matchPlan == null) return 0;
    int count = 0;
    for (Actor actor : workers) {
      for (Behaviour b : actor.mind.agenda()) if (b instanceof Plan) {
        if (((Plan) b).matchesPlan(matchPlan)) {
          count++;
        }
      }
    }
    return count;
  }
  
  
  public int numPresent(Background match) {
    int num = 0;
    for (Mobile m : employs.inside()) if (m instanceof Actor) {
      if (((Actor) m).mind.vocation() == match) num++;
    }
    return num;
  }
  
  
  public int numHomeless() {
    int num = 0;
    for (Actor actor : workers()) if (actor.mind.home() == null) num++;
    return num;
  }
  
  
  public Batch <Actor> visitors() {
    final Batch <Actor> visitors = new Batch <Actor> ();
    for (Mobile m : employs.inside()) if (m instanceof Actor) {
      visitors.add((Actor) m);
    }
    return visitors;
  }
  
  
  public Batch <Actor> visitorsDoing(Class planClass) {
    final Batch <Actor> doing = new Batch <Actor> ();
    for (Mobile m : employs.inside()) if (m instanceof Actor) {
      final Actor a = (Actor) m;
      if (a.isDoing(planClass, null)) doing.add(a);
    }
    return doing;
  }
  
  
  
  /**  Handling applications and recruitment-
    */
  public List <FindWork> applications() {
    return applications;
  }
  
  
  public boolean hasApplication(FindWork app) {
    return applicationMatching(app) != null;
  }
  
  
  public FindWork applicationMatching(FindWork app) {
    for (FindWork a : applications) if (a.matchesPlan(app)) return a;
    return null;
  }
  
  
  public void setApplicant(FindWork app, boolean is) {
    final FindWork match = applicationMatching(app);
    if (is && app.requiresApproval()) {
      if (match != null) return;
      applications.add(app);
    }
    else if (is) confirmApplication(app);
    else if (match != null) applications.remove(match);
  }
  
  
  public void confirmApplication(FindWork a) {
    
    if (I.logEvents()) {
      I.say("\nHIRED: "+a.actor()+" ("+a.position()+")");
      I.say("  At: "+employs+" ("+employs.base()+"");
    }
    
    final Base  base  = employs.base();
    final Stage world = base.world;
    final Actor works = a.actor();
    base.finance.incCredits(
      0 - a.hiringFee(),
      BaseFinance.SOURCE_HIRING
    );
    base.profiles.profileFor(works);
    works.mind.setVocation(a.position());
    works.mind.setWork(employs);
    //
    //  If there are no remaining openings for this background, cull any
    //  existing applications.  Otherwise, refresh signing costs.
    for (FindWork oA : applications) if (oA.position() == a.position()) {
      if (employs.crowdRating(oA.actor(), a.position()) >= 1) {
        applications.remove(oA);
      }
      if (oA == a) applications.remove(a);
    }
    //
    //  If the actor needs transport, arrange it-
    if (! works.inWorld()) {
      world.offworld.journeys.addLocalImmigrant(works, base);
    }
  }
  
  
  
  /**  Life cycle, recruitment and updates-
    */
  protected void updateStaff(int numUpdates) {
    if (numUpdates % REFRESH_INTERVAL == 0) {
      final Base base = employs.base();
      //
      //  Clear out the office for anyone dead or missing-
      for (Actor a : workers) {
        if (a.mind.work() != employs || ! a.health.alive()) {
          if (a.mind.work() == employs) a.mind.setWork(null);
          workers.remove(a);
        }
        
        //  TODO:  This is a temporary hack for a situation that was never
        //  supposed to arise.  Investigate!
        
        final Actor b = a;
        if (VerseJourneys.activityFor(b) != null) continue;
        final Verse verse = base.world.offworld;
        VerseLocation off = Verse.currentLocation(b, verse);
        VerseLocation local = verse.stageLocation();
        
        if (off != local && off != base.commerce.homeworld()) {
          base.world.offworld.journeys.addLocalImmigrant(b, base);
        }
        
        
      }
      for (Actor a : lodgers) {
        if (a.mind.home() != employs || ! a.health.alive()) {
          if (a.mind.home() == employs) a.mind.setHome(null);
          lodgers.remove(a);
        }
      }
      for (FindWork a : applications) {
        if (a.employer() != employs || ! a.actor().health.alive()) {
          setApplicant(a, false);
        }
      }
      //
      //  If there's an unfilled opening, look for someone to fill it.
      if (employs.careers() != null) for (Background v : employs.careers()) {
        final int openings = numOpenings(v);
        if (openings <= 0) continue;
        
        if (GameSettings.hireFree) {
          final Actor citizen = v.sampleFor(base);
          citizen.mind.setWork(employs);
          citizen.enterWorldAt(employs, base.world);
        }
        else {
          base.demands.impingeDemand(v, openings, REFRESH_INTERVAL, employs);
        }
      }
    }
  }
  
  
  protected void onCommission() {
  }
  
  
  protected void onDecommission() {
    for (Actor c : workers()) c.mind.setWork(null);
    for (Actor c : lodgers()) c.mind.setHome(null);
  }
  
  
  public void setWorker(Actor c, boolean is) {
    for (FindWork a : applications) if (a.actor() == c) {
      applications.remove(a);
    }
    if (is) workers.include(c);
    else workers.remove(c);
  }
  
  
  public void setLodger(Actor c, boolean is) {
    if (is) lodgers.include(c);
    else lodgers.remove(c);
  }
}


