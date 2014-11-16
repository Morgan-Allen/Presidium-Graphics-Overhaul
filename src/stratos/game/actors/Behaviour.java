/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.actors;
import stratos.game.common.*;
import stratos.util.*;



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
  
  //  TODO:  Just base these off urgency?  Anything above urgent triggers
  //  running, say?
  final public static int
    MOTION_ANY    = -1,
    MOTION_NORMAL =  0,
    MOTION_FAST   =  1,
    MOTION_SNEAK  =  2;
  
  Behaviour nextStepFor(Actor actor);
  int motionType(Actor actor);
  void abortBehaviour();
  Target subject();
  
  float priorityFor(Actor actor);
  boolean finished();
  boolean valid();
  boolean hasBegun();
  boolean persistent();
  
  void describeBehaviour(Description d);
}





