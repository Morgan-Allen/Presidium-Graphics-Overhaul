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
    delaysOnly    = true ;
  
  private long initTime = -1, maxInterval = -1;
  private boolean lastUpdateOkay = true;


  public static interface Updates extends Session.Saveable {
    float scheduledInterval();
    void updateAsScheduled(int numUpdates, boolean instant);
  }
  
  private static class Event {
    private float updateTime, callTime = -1;
    private int updatesCount;
    private Updates updates;
    
    public String toString() {
      return updates+" Update for: "+updateTime+"/"+callTime;
    }
  }
  
  final Sorting <Event> events = new Sorting <Event> () {
    public int compare(Event a, Event b) {
      if (a.updates == b.updates) return 0;
      return a.callTime > b.callTime ? 1 : -1;
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
      s.saveFloat (event.updateTime  );
      s.saveFloat (event.callTime    );
      s.saveInt   (event.updatesCount);
      s.saveObject(event.updates     );
    }
  }
  
  
  protected void loadFrom(Session s) throws Exception {
    currentTime = s.loadFloat();
    for (int n = s.loadInt(); n-- > 0;) {
      final Event event = new Event();
      event.updateTime   = s.loadFloat();
      event.callTime     = s.loadFloat();
      event.updatesCount = s.loadInt();
      event.updates      = (Updates) s.loadObject();
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
    //
    //  We 'fudge' the incept and scheduling times a little here to help ensure
    //  that client updates are staggered evenly over the schedule, rather
    //  than clustering in one or two processor-intensive updates.
    final float interval = updates.scheduledInterval();
    event.updateTime   = currentTime + (Rand.num() * interval);
    event.updatesCount = Rand.index(10);
    event.callTime     = event.updateTime;
    event.updates      = updates;
    allUpdates.put(updates, events.insert(event));
    
    if (verbose) {
      I.say("Scheduling "+updates+" for updates...");
      I.say("  Update time: "+event.updateTime);
      I.say("  Call time:   "+event.callTime  );
    }
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
    
    final Event event = events.refValue(ref);
    if (event.updatesCount == 0) {
      event.updateTime = event.callTime = currentTime;
    }
    else {
      event.callTime = Nums.min(currentTime, event.updateTime) - 5;
    }
    events.deleteRef(ref);
    allUpdates.put(event.updates, events.insert(event));
    
    if (verbose && updateVerbose) {
      I.say("...Scheduled for instant update: "+updates);
      I.say("  Call time:    "+event.callTime  );
      I.say("  Current time: "+currentTime     );
      I.say("  Update time:  "+event.updateTime);
      
      I.reportStackTrace();
    }
  }
  
  
  
  /**  Returns whether event-updates within the current schedule-call have
    *  taken up more time than allowed.
    */
  public boolean timeUp() {
    if (maxInterval == -1) return false;
    final long taken = System.currentTimeMillis() - initTime;
    return taken > maxInterval;
  }
  
  

  /**  Advances the schedule of events in accordance up to the current time in
    *  the host world.
    */
  protected void advanceSchedule(final float currentTime) {
    
    final boolean report = updateVerbose;
    if (report) {
      I.say("\nUpdating schedule- time: "+currentTime);
    }
    
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
      lastEventTime = event.callTime;
      if (event.callTime > currentTime) {
        finishedOK = true;
        break;
      }
      
      final float interval = event.updates.scheduledInterval();
      final boolean instant = event.callTime < event.updateTime - 1;
      events.deleteRef(leastRef);
      
      if (report) {
        I.say("  Updating: "+event.updates);
        I.say("    Scheduled time:    "+event.updateTime  );
        I.say("    Called time:       "+event.callTime    );
        I.say("    Total updates:     "+event.updatesCount);
        I.say("    Update-interval:   "+interval);
        I.say("    Instant?           "+instant );
      }
      
      //  TODO:  Stretch this out further based on the proportion of clients
      //  that updated successfully last cycle?
      if (! instant) {
        event.updatesCount++;
        event.updateTime += lastUpdateOkay ?
          (interval + ((Rand.num() - 0.5f) / 5)) :
          ((interval * 2) - Rand.num());
      }
      event.callTime = event.updateTime;
      allUpdates.put(event.updates, events.insert(event));

      long startTime = System.nanoTime();
      final int updates = instant ? -1 : event.updatesCount;
      event.updates.updateAsScheduled(updates, instant);
      
      long updateTime = System.nanoTime() - startTime;
      if (updateTime > longestTime) {
        longestTime = updateTime;
        longestUpdate = event.updates;
      }
      totalUpdated++;
    }
    
    this.lastUpdateOkay = finishedOK;
    final long taken = System.currentTimeMillis() - initTime;
    final boolean tooLong = taken >= maxInterval;
    
    if (verbose && (tooLong || ! delaysOnly)) {
      I.say("\nUPDATING SCHEDULE");
      I.say("  Milliseconds since last update: "+(initTime - oldInit));
      I.say("  Current time: "+currentTime);
      I.say("  Maximum time allowance: "+maxInterval);
      I.say("");
      
      if (tooLong) {
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









