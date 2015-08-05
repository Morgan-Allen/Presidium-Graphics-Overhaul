/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.military;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.game.wild.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Technique.*;



public class CloseCombatTechniques {
  

  //Shield Bypass.
  //Disarming Stroke.
  //Impact Connect.
  //Cleaving Arc.

  final static String DIR = "media/GUI/Powers/";
  
  
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
}











