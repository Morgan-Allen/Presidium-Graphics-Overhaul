/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.common;
import stratos.game.base.*;
import stratos.game.verse.*;



public interface StageExit extends Boarding {
  
  
  final static int
    TYPE_BORDER    = 0,
    TYPE_BOLT_HOLE = 1,
    TYPE_WAY_GATE  = 2;
  
  
  int exitType();
  VerseLocation leadsTo();
  boolean allowsStageExit(Mobile m);
}
