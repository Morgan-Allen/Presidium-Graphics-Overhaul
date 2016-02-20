/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.abilities;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.plans.*;
import stratos.game.base.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Technique.*;
import stratos.graphics.sfx.*;
import stratos.util.*;



public class MiscTechniques {
  
  
  final static String
    UI_DIR = "media/GUI/Powers/",
    FX_DIR = "media/SFX/";
  final static Class BASE_CLASS = MiscTechniques.class;
  
  final static int
    HONOUR_SWAY_BONUS  = 5,
    DUEL_DEFENCE_BONUS = 5,
    DUEL_DAMAGE_MAX    = 6;
  
  //  TODO:  Tie these into upgrades at the Bastion?
  
  
  final public static Technique WORD_OF_HONOUR = new Technique(
    "Word of Honour", UI_DIR+"word_of_honour.png",
    "Improves any persuasion attempts involving pledges without deceit.",
    BASE_CLASS, "word_of_honour",
    MINOR_POWER          ,
    HARM_UNRATED         ,
    NO_FATIGUE           ,
    MINOR_CONCENTRATION  ,
    IS_PASSIVE_SKILL_FX | IS_TRAINED_ONLY, null, -1, null
  ) {
    
    public boolean triggersPassive(
      Actor actor, Plan current, Skill used, Target subject, boolean reactive
    ) {
      if (! (current instanceof Proposal)) return false;
      if (current.subject() != subject) return false;
      final Proposal prop = (Proposal) current;
      final Pledge made = prop.termsOffered();
      if (made == null || made.isDeceit()) return false;
      return used == COMMAND || used == SUASION;
    }
    
    
    public float passiveBonus(Actor using, Skill skill, Target subject) {
      return HONOUR_SWAY_BONUS;
    }
  };
  
  
  //  TODO:  This doesn't *quite* do exactly what it says on the tin.  You'll
  //  need to work out a Volley/Result system for that.
  
  final public static Technique DUELIST = new Technique(
    "Duelist", UI_DIR+"duelist.png",
    "Improves both defence and damage while engaged in single combat (both "+
    "parties targeting the other.)",
    BASE_CLASS, "duelist",
    MINOR_POWER          ,
    HARM_UNRATED         ,
    NO_FATIGUE           ,
    MINOR_CONCENTRATION  ,
    IS_PASSIVE_SKILL_FX | IS_TRAINED_ONLY, null, -1, null
  ) {
    
    public boolean triggersPassive(
      Actor actor, Plan current, Skill used, Target subject, boolean reactive
    ) {
      if (used != EVASION && used != HAND_TO_HAND) return false;
      if (! (subject instanceof Actor )) return false;
      if (! (current instanceof Combat)) return false;
      final Actor struck = (Actor) subject;
      return
        struck.actionFocus() == actor &&
        struck.isDoing(Combat.class, actor);
    }
    
    
    public float passiveBonus(Actor using, Skill skill, Target subject) {
      return DUEL_DEFENCE_BONUS;
    }
    
    
    public void applyEffect(
      Actor using, Target subject, boolean success, boolean passive
    ) {
      super.applyEffect(using, subject, success, passive);
      if (success && Spacing.distance(using, subject) < 1) {
        final Actor struck = (Actor) subject;
        float damage = roll(0, DUEL_DAMAGE_MAX);
        struck.health.takeInjury(damage, false);
      }
    }
  };
  
  
  final public static Technique NOBLE_TECHNIQUES[] = {
    WORD_OF_HONOUR, DUELIST
  };
  
  
}





//  And finally, a smattering of abilities for vendors, performers, auditors
//  and bureaucrats?

//  Seduction.  Lucky Draw.
//  Total Recall.  Strong Back.
  
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











