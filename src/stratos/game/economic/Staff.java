/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.economic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.game.politic.*;
import stratos.game.wild.Species;
import stratos.game.maps.*;
import stratos.util.*;





//
//  An actor's willingness to apply for an opening should be based on the
//  number of current applicants.  You don't want to overwhelm the player
//  with choices.
//
//  You'd need to assess an actor's fitness for a given Vocation, in terms of
//  both skills and personality.  (And they'd have to be given gear.)


public class Staff {
  
  /**  Fields, constructors, and save/load methods-
    */
  final static int
    REFRESH_INTERVAL = 10,
    AUDIT_INTERVAL   = Stage.STANDARD_DAY_LENGTH / 10;
  
  private static boolean verbose = false;
  
  
  final Property employs;
  final List <FindWork>
    applications = new List <FindWork> ();
  final List <Actor>
    workers   = new List <Actor> (),
    residents = new List <Actor> ();
  private int
    shiftType = -1;
  
  
  
  Staff(Property venue) {
    this.employs = venue;
  }
  
  
  void loadState(Session s) throws Exception {
    shiftType = s.loadInt();
    s.loadObjects(applications);
    s.loadObjects(workers);
    s.loadObjects(residents);
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveInt(shiftType);
    s.saveObjects(applications);
    s.saveObjects(workers);
    s.saveObjects(residents);
  }
  
  
  public List <Actor> workers() {
    return workers;
  }
  
  
  public List <Actor> residents() {
    return residents;
  }
  
  
  public void setShiftType(int type) {
    this.shiftType = type;
  }
  
  
  public boolean unoccupied() {
    return residents.size() == 0;
  }
  
  
  public int population() {
    return residents.size();
  }
  
  
  public int workforce() {
    return workers.size();
  }
  
  
  public boolean isResident(Mobile a) {
    for (Actor r : residents) if (r == a) return true;
    return false;
  }
  
  
  public boolean isWorker(Mobile a) {
    for (Actor w : workers) if (w == a) return true;
    return false;
  }
  
  
  public boolean doesBelong(Mobile a) {
    return isResident(a) || isWorker(a);
  }
  
  
  
  /**  Handling shifts and being off-duty:
    */
  public int shiftFor(Actor worker) {
    if (shiftType == -1) return Venue.OFF_DUTY;
    if (shiftType == Venue.SHIFTS_ALWAYS) {
      return Venue.PRIMARY_SHIFT;
    }
    final Stage world = employs.base().world;
    
    //
    //  Simplified versions in use for the present...
    if (shiftType == Venue.SHIFTS_BY_HOURS) {
      final int day = (int) (world.currentTime() / Stage.STANDARD_DAY_LENGTH);
      final int index = (workers.indexOf(worker) + day) % 3;
      final int hour =
        Planet.isMorning(world) ? 1 :
        (Planet.isEvening(world) ? 2 : 0);
      
      if (index == hour) return Venue.PRIMARY_SHIFT;
      else if (index == (hour + 1 % 3)) return Venue.SECONDARY_SHIFT;
      else return Venue.OFF_DUTY;
    }
    
    if (shiftType == Venue.SHIFTS_BY_24_HOUR) {
      final int day = (int) (world.currentTime() / Stage.STANDARD_DAY_LENGTH);
      final int index = workers.indexOf(worker);
      if (day % 3 == index % 3) return Venue.PRIMARY_SHIFT;
      if (day % 3 == (index + 1) % 3) return Venue.SECONDARY_SHIFT;
      return Venue.OFF_DUTY;
    }
    
    if (shiftType == Venue.SHIFTS_BY_DAY) {
      final int day = (int) (world.currentTime() / Stage.STANDARD_DAY_LENGTH);
      final int index = workers.indexOf(worker);
      
      if (Planet.isNight(world)) return Venue.OFF_DUTY;
      else if ((index % 3) == (day % 3) || Planet.dayValue(world) < 0.5f) {
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
      if (((Actor) m).vocation() == match) num++;
    }
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
  
  
  public void setApplicant(FindWork app, boolean is) {
    if (is) {
      for (FindWork a : applications) if (a.matchesPlan(app)) return;
      applications.add(app);
    }
    else for (FindWork a : applications) if (a.matchesPlan(app)) {
      applications.remove(a);
    }
  }
  
  
  public void confirmApplication(FindWork a) {
    final Base base = employs.base();
    final Stage world = base.world;
    
    base.finance.incCredits(
      0 - a.hiringFee(), BaseFinance.SOURCE_HIRING
    );
    final Actor works = a.actor();
    //
    //  TODO:  Once you have incentives worked out, restore this-
    //works.gear.incCredits(app.salary / 2);
    //works.gear.taxDone();
    works.setVocation(a.position());
    works.mind.setWork(employs);
    //
    //  If there are no remaining openings for this background, cull any
    //  existing applications.  Otherwise, refresh signing costs.
    for (FindWork oA : applications) if (oA.position() == a.position()) {
      if (employs.numOpenings(oA.position()) == 0) {
        applications.remove(oA);
      }
    }
    //
    //  If the actor needs transport, arrange it-
    if (! works.inWorld()) {
      world.offworld.addMigrant(works, world);
    }
  }
  
  
  
  /**  Life cycle, recruitment and updates-
    */
  protected void updatePersonnel(int numUpdates) {
    if (numUpdates % REFRESH_INTERVAL == 0) {
      final Base base = employs.base();
      
      //  Clear out the office for anyone dead-
      for (Actor a : workers) if (a.destroyed() || a.base() != base) {
        setWorker(a, false);
      }
      for (Actor a : residents) if (a.destroyed() || a.base() != base) {
        setResident(a, false);
      }
      for (FindWork a : applications) if (a.employer() != employs) {
        setApplicant(a, false);
      }
      
      //  If there's an unfilled opening, look for someone to fill it.
      //  TODO:  This should really be handled more from the Commerce class?
      if (employs.careers() == null) return;
      
      if (employs.privateProperty()) for (FindWork a : applications) {
        confirmApplication(a);
      }
      
      for (Background v : employs.careers()) {
        final int numOpenings = employs.numOpenings(v);
        if (numOpenings > 0) {
          if (GameSettings.hireFree) {
            final Human citizen = new Human(v, employs.base());
            citizen.mind.setWork(employs);
            final Boarding t = employs.canBoard()[0];
            citizen.enterWorldAt(t, base.world);
          }
          else {
            base.commerce.incDemand(v, numOpenings, REFRESH_INTERVAL);
          }
        }
      }
    }
  }
  
  
  protected void onCommission() {
  }
  
  
  protected void onDecommission() {
    for (Actor c : workers()) c.mind.setWork(null);
    for (Actor c : residents()) c.mind.setHome(null);
  }
  
  
  public void setWorker(Actor c, boolean is) {
    for (FindWork a : applications) if (a.actor() == c) {
      applications.remove(a);
    }
    if (is) workers.include(c);
    else workers.remove(c);
  }
  
  
  public void setResident(Actor c, boolean is) {
    if (is) residents.include(c);
    else residents.remove(c);
  }
  
  
  public int numHired(Background match) {
    int num = 0; for (Actor c : workers) {
      if (c.vocation() == match) num++;
    }
    return num;
  }
  
  
  public int numResident(Species match) {
    int num = 0; for (Actor c : residents) {
      if (c.species() == match) num++;
    }
    return num;
  }
}


