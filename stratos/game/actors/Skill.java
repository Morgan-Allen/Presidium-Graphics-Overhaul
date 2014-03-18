/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.actors ;
import stratos.graphics.common.Colour;
import stratos.util.*;



//
//  Consider folding these into the Trait class.
public class Skill extends Trait {
  
  
  final static String DESC_LEVELS[] = new String[] {
    "(Learning)", "(Novice)", "(Practiced)",
    "(Seasoned)", "(Expert)", "(Master)",
  } ;
  final static String ATT_LEVELS[] = new String[] {
    "(Feeble)", "(Meagre)", "(Developed)",
    "(Outstanding)", "(Prodigious)", "(Phenomenal)",
  } ;
  final static Colour DESC_TONES[] = {
    Colour.YELLOW, Colour.GREEN, Colour.CYAN,
    Colour.BLUE, Colour.MAGENTA, Colour.RED,
  } ;
  
  
  private static String[] descLevelsFor(String skillName) {
    final String desc[] = new String[31] ;
    for (int n = 0 ; n < 30 ; n++) {
      desc[n] = DESC_LEVELS[(30 - (n + 1)) / 5]+" "+skillName ;
    }
    desc[30] = null ;
    return desc ;
  }
  
  
  public static String skillDesc(int level) {
    return DESC_LEVELS[Visit.clamp(level / 5, 6)] ;
  }

  public static String attDesc(int level) {
    return ATT_LEVELS[Visit.clamp(level / 5, 6)] ;
  }
  
  public static Colour skillTone(int level) {
    return DESC_TONES[Visit.clamp(level / 5, 6)] ;
  }
  
  
  
  final public String name ;
  final public int form ;
  final public Skill parent ;
  
  
  Skill(String name, int form, Skill parent) {
    super(SKILL, descLevelsFor(name)) ;
    this.name = name ;
    this.form = form ;
    this.parent = parent ;
  }
  

  public String toString() {
    return name ;
  }
}










