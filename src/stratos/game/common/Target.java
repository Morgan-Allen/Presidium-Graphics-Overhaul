/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.common;
import stratos.util.*;



public interface Target extends Accountable, Session.Saveable {
  
  
  final public static String
    
    TYPE_SECURITY  = "Security & Defence"    ,
    TYPE_PHYSICIAN = "Health & Education"    ,
    TYPE_COMMERCE  = "Trade & Commerce"      ,
    TYPE_ENGINEER  = "Engineering & Industry",
    TYPE_ECOLOGIST = "Ecology & Environment" ,
    TYPE_AESTHETIC = "Arts & Entertainment"  ,
    
    TYPE_MISC      = "Miscellaneous",
    
    TYPE_UNPLACED  = "Unplaced",
    TYPE_SCHOOL    = "School",
    TYPE_WIP       = "<WORK IN PROGRESS>",
    
    CIVIC_CATEGORIES[] = {
      TYPE_SECURITY, TYPE_PHYSICIAN, TYPE_COMMERCE,
      TYPE_ENGINEER, TYPE_ECOLOGIST, TYPE_AESTHETIC,
      TYPE_UNPLACED, TYPE_SCHOOL
    },
    
    TYPE_NATIVE    = "Native",
    TYPE_WILD      = "Wild"  ,
    TYPE_RUINS     = "Ruins" ,
    
    TYPE_ACTOR     = "<actor>"  ,
    TYPE_VEHICLE   = "<vehicle>",
    TYPE_TERRAIN   = "<terrain>",
    TYPE_MISSION   = "<mission>";
  
  
  boolean inWorld();
  Stage world();
  Base base();
  boolean destroyed();
  
  boolean indoors();
  boolean isMobile();
  
  Vec3D position(Vec3D v);
  float height();
  float radius();
  
  void flagWith(Object f);
  Object flaggedWith();
}


