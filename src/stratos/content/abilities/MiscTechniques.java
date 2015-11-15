/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.abilities;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Technique.*;
import stratos.graphics.sfx.*;
import stratos.util.*;



public class MiscTechniques {
  
  
  final static String
    UI_DIR = "media/GUI/Powers/",
    FX_DIR = "media/SFX/";
  final static Class BASE_CLASS = MiscTechniques.class;
  
  
  
  final public static Technique AIR_OF_COMMAND = null;
  
  final public static Technique WORD_OF_HONOUR = null;

  //  TODO:  Tie this into upgrades at the Bastion!
  final public static Technique DUELIST = null;
  //  (Extra defence+damage in single combat, melee or ranged.)
  
  
  
  //  And finally, a smattering of abilities for vendors, performers, auditors
  //  and bureaucrats?
  
  //  Seduction.  Lucky Draw.
  //  Total Recall.  Strong Back.
}




  
  //  These are special techniques that boost damage against specific enemy-
  //  types...
  
  /*
  private static Technique killTechnique(
    String name, String uniqueID, String icon, String FX,
    final Class <? extends Actor> victimClass
  ) {
    return new Technique(
      name, DIR+icon, Action.STRIKE_BIG,
      CloseCombatTechniques.class, uniqueID,
      MINOR_POWER        ,
      REAL_HARM          ,
      NO_FATIGUE         ,
      MINOR_CONCENTRATION,
      Technique.TYPE_SKILL_USE_BASED, HAND_TO_HAND, 5,
      KommandoRedoubt.VENDETTA_ARTILECT, Technique.TRIGGER_ATTACK
    ) {
      
      public float bonusFor(Actor using, Skill skill, Target subject) {
        if (victimClass.isAssignableFrom(subject.getClass())) return 5;
        else return -1;
      }
      
      public void applyEffect(Actor using, boolean success, Target subject) {
        super.applyEffect(using, success, subject);
        //
        //  TODO:  Include a special FX here!
        CombatFX.applyFX(using.gear.deviceType(), using, subject, true);
      }
    };
  }
  
  final public static Technique ARTILECT_KILLER = killTechnique(
    "Artilect Killer", "artilect_killer", null, null, Artilect.class
  );
  
  final public static Technique VERMIN_KILLER = killTechnique(
    "Vermin Killer", "vermin_killer", null, null, Vermin.class
  );
  
  final public static Technique MAN_KILLER = killTechnique(
    "Man Killer", "man_killer", null, null, Human.class
  );
  //*/











