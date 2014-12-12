/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.common;
import stratos.start.*;
import stratos.util.*;
import stratos.util.Sorting.*;



/**  A Schedule is used to sort events in order of timing, and dispatch them as
  *  they occur.  The schedule using the last recorded current time from
  *  schedule advancement for calculating the proper time for new events.
  *  Consequently, the shedule should be advanced at regular intervals.
  *  
  *  (The purpose of this class is, essentially, to allow potentially processor-
  *  intensive tasks to be either time-sliced or deferred so as not to disrupt
  *  normal graphical framerate or simulation updates.)
  */
public class Schedule {
  
  
  private static boolean
    verbose       = false,
    updateVerbose = false,
    verboseDelays = verbose && true;
  
  private long initTime = -1, maxInterval = -1;
  private boolean lastUpdateOkay = true;


  public static interface Updates extends Session.Saveable {
    float scheduledInterval();
    void updateAsScheduled(int numUpdates, boolean instant);
  }
  
  private static class Event {
    private float time;
    private int initTime, lastUpdateCount;
    private Updates updates;
    
    public String toString() {
      return updates+" Update for: "+time;
    }
  }
  
  final Sorting <Event> events = new Sorting <Event> () {
    public int compare(Event a, Event b) {
      if (a.updates == b.updates) return 0;
      return a.time > b.time ? 1 : -1;
    }
  };
  
  final Table <Updates, Object>
    allUpdates = new Table <Updates, Object> (1000);
  
  private float currentTime = 0;
  
  
  Schedule(float initTime) {
    this.currentTime = initTime;
  }
  
  
  protected void saveTo(Session s) throws Exception {
    s.saveFloat(currentTime);
    s.saveInt(allUpdates.size());
    for (Object node : allUpdates.values()) {
      final Event event = events.refValue(node);
      s.saveFloat(event.time);
      s.saveInt(event.initTime);
      s.saveInt(event.lastUpdateCount);
      s.saveObject(event.updates);
    }
  }
  
  
  protected void loadFrom(Session s) throws Exception {
    currentTime = s.loadFloat();
    for (int n = s.loadInt(); n-- > 0;) {
      final Event event = new Event();
      event.time = s.loadFloat();
      event.initTime = s.loadInt();
      event.lastUpdateCount = s.loadInt();
      event.updates = (Updates) s.loadObject();
      allUpdates.put(event.updates, events.insert(event));
    }
  }
  
  
  
  /**  Registration and deregistration methods for updatable objects-
    */
  public void scheduleForUpdates(Updates updates) {
    if (allUpdates.get(updates) != null) {
      I.say("\nWARNING: "+updates+" ALREADY REGISTERED FOR UPDATES!");
      I.reportStackTrace();
      return;
    }
    final Event event = new Event();
    if (verbose) I.say("Scheduling "+updates+" for updates...");
    
    //  We 'fudge' the incept and scheduling times a little here to help ensure
    //  that client updates are staggered evenly over the schedule, rather
    //  than clustering in one or two processor-intensive updates.
    event.lastUpdateCount = Rand.index(10);
    event.initTime = ((int) currentTime) - event.lastUpdateCount;
    event.time = currentTime + (Rand.num() * updates.scheduledInterval());
    event.updates = updates;
    allUpdates.put(updates, events.insert(event));
  }
  
  
  public void unschedule(Updates updates) {
    final Object node = allUpdates.get(updates);
    if (node == null) {
      I.say("\nWARNING: "+updates+" NEVER REGISTERED FOR UPDATES!");
      I.reportStackTrace();
      return;
    }
    if (verbose) I.say("...Unscheduling: "+updates);
    events.deleteRef(node);
    allUpdates.remove(updates);
  }
  
  
  public void scheduleNow(Updates updates) {
    final Object ref = allUpdates.get(updates);
    if (ref == null) {
      I.say("\nWARNING: "+updates+" NEVER REGISTERED FOR UPDATES!");
      I.reportStackTrace();
      return;
    }
    if (verbose) I.say("...Scheduling for instant update: "+updates);
    final Event event = events.refValue(ref);
    event.time = currentTime;
    event.lastUpdateCount = -1; //  flag to pass 'instant' argument (below.)
    events.deleteRef(ref);
    allUpdates.put(event.updates, events.insert(event));
  }
  
  
  
  /**  Advances the schedule of events in accordance with the current time in
    *  the host world.
    */
  public boolean timeUp() {
    if (maxInterval == -1) return false;
    final long taken = System.currentTimeMillis() - initTime;
    return taken > maxInterval;
  }
  
  
  protected void advanceSchedule(final float currentTime) {
    this.currentTime = currentTime;
    
    final int NU = PlayLoop.UPDATES_PER_SECOND * 2;
    maxInterval = (int) (1000 / (PlayLoop.gameSpeed() * NU)) - 1;
    
    final long oldInit = initTime;
    initTime = System.currentTimeMillis();
    int totalUpdated = 0;
    float lastEventTime = -1;
    boolean finishedOK = true;
    
    Updates longestUpdate = null;
    long longestTime = 0;
    
    if (verbose) I.say(
      "\nUPDATING SCHEDULE, MS SINCE LAST UPDATE: "+(initTime - oldInit)+
      "\nCurrent time: "+currentTime+"\n"
    );
    
    while (true) {
      if (timeUp()) {
        finishedOK = false;
        break;
      }
      
      final Object leastRef = events.leastRef();
      if (leastRef == null) {
        finishedOK = true;
        break;
      }
      
      final Event event = events.refValue(leastRef);
      lastEventTime = event.time;
      if (event.time > currentTime) {
        finishedOK = true;
        break;
      }

      final float interval = event.updates.scheduledInterval();
      events.deleteRef(leastRef);
      
      //  TODO:  Stretch this out further based on the proportion of clients
      //  that updated successfully last cycle?
      if (lastUpdateOkay) event.time += interval + ((Rand.num() - 0.5f) / 5);
      else event.time += (interval * 2) - Rand.num();
      allUpdates.put(event.updates, events.insert(event));
      
      //  TODO:  You need a better system here- particularly in the case of
      //  instant scheduling.
      final int updateCount = (int) (currentTime - event.initTime);
      final boolean instant = event.lastUpdateCount == -1;
      
      if (instant || updateCount > event.lastUpdateCount) {
        long startTime = System.nanoTime();
        if (updateVerbose) I.say("  Updating: "+event.updates);
        event.updates.updateAsScheduled(updateCount, instant);
        event.lastUpdateCount = updateCount;
        
        long updateTime = System.nanoTime() - startTime;
        if (updateTime > longestTime) {
          longestTime = updateTime;
          longestUpdate = event.updates;
        }
        totalUpdated++;
      }
    }
    
    this.lastUpdateOkay = finishedOK;
    final long taken = System.currentTimeMillis() - initTime;
    if (verbose) {
      if (taken >= maxInterval * 2) {
        I.say("___PATHOLOGICALLY DELAYED___");
      }
      if (finishedOK) {
        I.say("  SCHEDULE UPDATED OKAY, OBJECTS UPDATED: "+totalUpdated);
      }
      else {
        I.say("  SCHEDULE IS OUT OF TIME, TOTAL UPDATED: "+totalUpdated);
      }
      I.say("  Last event time: "+lastEventTime+"\n");
      I.say("  TOTAL OBJECTS NEEDING UPDATE: "+events.size());
      I.say("  TIME SPENT: "+taken);
      I.say("  Longest client to update: "+longestUpdate);
      I.say("  Time taken "+(longestTime / 1000000.0)+"\n");
      I.say("\n");
    }
  }
}









