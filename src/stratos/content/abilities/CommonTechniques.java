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


//
//  TODO:  These should mostly be implemented as independent, attack-triggered
//         actions, rather than passive skill bonuses?


public class CommonTechniques {
  
  
  
  final static String DIR = "media/GUI/Powers/";
  final static Class BASE_CLASS = CommonTechniques.class;
  
  final static ShotFX.Model
    TRACE_FX_MODEL = new ShotFX.Model(
      "tracer_beam_fx", BASE_CLASS,
      "media/SFX/tracer_beam.png", 0.05f, 0, 0.05f, 3, true, true
    );
  final public static PlaneFX.Model
    AIM_FX_MODEL = PlaneFX.animatedModel(
      "aim_model", BASE_CLASS,
      "media/SFX/aiming_anim.png",
      4, 4, 16, (16 / 25f), 0.25f
    ),
    PIERCE_FX_MODEL = PlaneFX.imageModel(
      "pierce_fx", BASE_CLASS,
      "media/SFX/penetrating_shot.png",
      0.5f, 0, 0, false, false
    );
  
  
  final public static Technique FOCUS_FIRE = new Technique(
    "Focus Fire", DIR+"steady_aim.png",
    "Increases effective "+MARKSMANSHIP+" against stationary targets.",
    BASE_CLASS, "focus_fire",
    MINOR_POWER         ,
    REAL_HARM           ,
    NO_FATIGUE          ,
    MINOR_CONCENTRATION ,
    IS_PASSIVE_SKILL_FX, MARKSMANSHIP, 5,
    MARKSMANSHIP
  ) {
    
    public float passiveBonus(Actor using, Skill skill, Target subject) {
      if (! (subject instanceof Mobile)) return  5;
      if (((Mobile) subject).isMoving()) return -1;
      return 5;
    }
    
    
    public void applyEffect(
      Actor using, boolean success, Target subject, boolean passive
    ) {
      super.applyEffect(using, success, subject, passive);
      ActionFX.applyBurstFX(AIM_FX_MODEL, using, 1.5f, 1);
    }
    
    
    public boolean canBeLearnt(Actor learns, boolean trained) {
      final DeviceType DT = learns.gear.deviceType();
      if (DT == null || DT.natural()) return false;
      return super.canBeLearnt(learns, trained);
    }
  };
  
  
  //  Suppression Fire- expend extra ammo and reduce damage/accuracy, but with
  //                    chance to stun enemies in AoE.
  
  final public static Technique SUPPRESSION = new Technique(
    "Suppression", DIR+"suppression_fire.png",
    "Temporarily lowers the target's mobility and accuracy.",
    BASE_CLASS, "suppression_fire",
    MEDIUM_POWER        ,
    REAL_HARM           ,
    NO_FATIGUE          ,
    MAJOR_CONCENTRATION ,
    IS_PASSIVE_SKILL_FX, MARKSMANSHIP, 10,
    MARKSMANSHIP
  ) {
    
    public float passiveBonus(Actor using, Skill skill, Target subject) {
      return -5;
    }
    
    
    public void applyEffect(
      Actor using, boolean success, Target subject, boolean passive
    ) {
      super.applyEffect(using, success, subject, passive);
      ///I.say("Applying suppression: "+using+" to "+subject);
      
      //  TODO:  This is an AoE effect, and should be evaluated & applied as
      //  such.
      
      final Tile o = using.world().tileAt(subject);
      for (Tile t : o.allAdjacent(null)) {
        if (Rand.num() > 0.33f) continue;
        for (Mobile m : t.inside()) if (m instanceof Actor) {
          
        }
        //  TODO:  Introduced fancy SFX here!
        //Combat.performStrike(actor, target, offence, defence, false);
      }
    }
    
    
    public boolean canBeLearnt(Actor learns, boolean trained) {
      final DeviceType DT = learns.gear.deviceType();
      if (DT == null || DT.natural()) return false;
      return super.canBeLearnt(learns, trained);
    }
    
    
    public float basePriority(Actor actor, Plan current, Target subject) {
      final float appeal = super.basePriority(actor, current, subject);
      if (appeal <= 0) return 0;
      return appeal * actor.gear.ammoLevel();
    }
    
    
    protected void applyAsCondition(Actor affected) {
      super.applyAsCondition(affected);
      //
      //  TODO:  Just use a simple stun effect instead?
      affected.traits.incBonus(ATHLETICS   , -20);
      affected.traits.incBonus(SURVEILLANCE, -10);
      affected.traits.incBonus(MOTOR       , -5 );
    }
  };
  
  
  //  Shield Bypass- chance to puncture cover or armour after Take Aim.
  //  Special FX:  Attach 'puncture' fx at target...
  
  final public static Technique SHIELD_BYPASS = new Technique(
    "Shield Bypass", DIR+"armour_bypass.png",
    "Allows shields' damage-reduction to be ignored.",
    BASE_CLASS, "shield_bypass",
    MINOR_POWER         ,
    REAL_HARM           ,
    NO_FATIGUE          ,
    MEDIUM_CONCENTRATION,
    Technique.IS_PASSIVE_SKILL_FX, HAND_TO_HAND, 15, Action.FIRE,
    Action.QUICK
  ) {
    
    public float passiveBonus(Actor using, Skill skill, Target subject) {
      return 0;
    }
    
    public void applyEffect(
      Actor using, boolean success, Target subject, boolean passive
    ) {
      super.applyEffect(using, success, subject, passive);
      //  TODO:  Use a 'volley' class to ignore armour values.
      
      if (success) {
        ActionFX.applyBurstFX(
          PIERCE_FX_MODEL, subject.position(null), 0.5f, using.world()
        );
        ((Actor) subject).health.takeInjury(Rand.index(2) + 0.5f, true);
      }
    }
  };
  
  
  final public static Technique BULLET_PARRY = null;//  TODO:  Fill in!
  
  
  final public static Technique FIRST_SHOT = null;
  
  
  final public static Technique SOLO_RUN = null;
  
  
  final public static Technique AIR_OF_AUTHORITY = null;
  
  
  final public static Technique WORD_OF_HONOUR = null;
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











