/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.actors;
import stratos.game.common.*;
import stratos.util.*;



//  TODO:  Get rid of most of this, or restrict to Actions and Plans.  Just
//  have a single 'update' method and read priority, step, success chance etc.
//  passively afterward.


public interface Behaviour extends Session.Saveable {
  
  final public static float
    IDLE        =  1,
    CASUAL      =  2.5f,
    ROUTINE     =  5,
    URGENT      =  7.5f,
    PARAMOUNT   =  10,
    
    DEFAULT_SWITCH_THRESHOLD = 2.5f;
  
  final public static String PRIORITY_DESCRIPTIONS[] = {
    "Idle", "Casual", "Routine", "Urgent", "Paramount"
  };
  
  final public static int
    MOTION_ANY    = -1,
    MOTION_NORMAL =  0,
    MOTION_FAST   =  1,
    MOTION_SNEAK  =  2;
  
  final public static String
    INTERRUPT_CANCEL     = "Cancelled",
    INTERRUPT_NOT_VALID  = "Plan Is Impossible",
    INTERRUPT_NO_PREREQ  = "Lack Of Prerequisites",
    INTERRUPT_NO_TARGET  = "Target Out Of World",
    INTERRUPT_LOSE_SIGHT = "Target Is Hidden",
    INTERRUPT_LOSE_PATH  = "No Valid Path";
  
  boolean matchesPlan(Behaviour b);
  
  Behaviour nextStepFor(Actor actor);
  int motionType(Actor actor);
  void toggleActive(boolean is);  //TODO:  REPLACE WITH BEGIN/END CALLS
  void interrupt(String cause);
  Target subject();
  
  float priorityFor(Actor actor);
  boolean finished();
  boolean valid();
  boolean hasBegun();
  boolean persistent();
  boolean isEmergency();
  
  void describeBehaviour(Description d);
}





