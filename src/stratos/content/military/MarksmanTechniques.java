

package stratos.content.military;

import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.game.tactical.*;
import stratos.graphics.sfx.*;
import stratos.util.*;
import stratos.game.building.DeviceType;
import stratos.game.building.OutfitType;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Technique.*;



//
//  TODO:  These should mostly be implemented as independent, attack-triggered
//         actions, rather than passive skill bonuses.  (This should allow them
//         to be chosen at the start of an action, rather than at the end, and
//         thus allow for more appropriate SFX.)


public class MarksmanTechniques {
  
  
  final static ShotFX.Model
    TRACE_FX_MODEL = new ShotFX.Model(
      "tracer_beam_fx", MarksmanTechniques.class,
      "media/SFX/tracer_beam.png", 0.05f, 0, 0.05f, 3, true, true
    );
  final public static PlaneFX.Model
    AIM_MODEL = new PlaneFX.Model(
      "aim_model", MarksmanTechniques.class,
      "media/SFX/aiming_anim.png", 4, 4, 16,
      (8 / 25f), 0.25f
    );
  final static PlaneFX.Model
    PIERCE_FX_MODEL = new PlaneFX.Model(
      "pierce_fx", MarksmanTechniques.class,
      "media/SFX/penetrating_shot.png", 0.5f, 0, 0, false, false
    );
  final static String DIR = "media/GUI/Powers/";
  
  
  
  //  Steady Aim- bonus as long as you or the target stay in place.
  //  Special FX- little clock-timer above head.
  //            - direct line from barrel to target.
  
  final public static Technique STEADY_AIM = new Technique(
    "Steady Aim", DIR+"steady_aim.png", Action.FIRE,
    MarksmanTechniques.class, 00,
    MINOR_POWER        ,
    REAL_HARM          ,
    NO_FATIGUE         ,
    MINOR_CONCENTRATION,
    Technique.TYPE_SKILL_USE_BASED, MARKSMANSHIP, 5
  ) {
    
    public float bonusFor(Actor using, Skill skill, Target subject) {
      if (! (subject instanceof Mobile)) return 5;
      if (((Mobile) subject).isMoving()) return -1;
      return 5;
    }
    
    public void applyEffect(Actor using, boolean success, Target subject) {
      super.applyEffect(using, success, subject);
      
      //  Last but not least, include special FX-
      final Vec3D posFX = using.position(null);
      posFX.z += using.height() + 0.25f;
      CombatFX.applyBurstFX(AIM_MODEL, posFX, 1, using.world());
    }
  };
  
  
  //  Suppression Fire- expend extra ammo and reduce damage/accuracy, but with
  //                    chance to stun enemies in AoE.
  
  final public static Technique SUPPRESSION_FIRE = new Technique(
    "Suppression Fire", DIR+"suppression_fire.png", Action.FIRE,
    MarksmanTechniques.class, 01,
    MEDIUM_POWER       ,
    REAL_HARM          ,
    NO_FATIGUE         ,
    MAJOR_CONCENTRATION,
    Technique.TYPE_SKILL_USE_BASED, MARKSMANSHIP, 10
  ) {
    
    public float bonusFor(Actor using, Skill skill, Target subject) {
      return -5;
    }
    
    public void applyEffect(Actor using, boolean success, Target subject) {
      super.applyEffect(using, success, subject);
      I.say("Applying suppression: "+using+" to "+subject);

      final Tile o = using.world().tileAt(subject);
      for (Tile t : o.allAdjacent(null)) {
        if (Rand.num() > 0.33f) continue;
        //  TODO:  Introduced fancy SFX here!
        //Combat.performStrike(actor, target, offence, defence, false);
      }
    }
    
    public float priorityFor(Actor actor, Target subject, float harmLevel) {
      final float appeal = super.priorityFor(actor, subject, harmLevel);
      if (appeal <= 0) return 0;
      return appeal * actor.gear.ammoLevel();
    }
    
    protected void applyAsCondition(Actor affected) {
      super.applyAsCondition(affected);
      //  TODO:  Just a simple stun effect instead...
      affected.traits.incBonus(ATHLETICS   , -20);
      affected.traits.incBonus(SURVEILLANCE, -10);
      affected.traits.incBonus(MOTOR       , -5 );
    }
  };
  
  
  //  Armour Bypass- chance to puncture cover or armour after Take Aim.
  //  Special FX:  Attach 'puncture' fx at target...
  
  final public static Technique ARMOUR_BYPASS = new Technique(
    "Armour Bypass", DIR+"armour_bypass.png", Action.FIRE,
    MarksmanTechniques.class, 02,
    MINOR_POWER         ,
    REAL_HARM           ,
    NO_FATIGUE          ,
    MEDIUM_CONCENTRATION,
    Technique.TYPE_SKILL_USE_BASED, MARKSMANSHIP, 15
  ) {
    
    public float bonusFor(Actor using, Skill skill, Target subject) {
      return 0;
    }
    
    public void applyEffect(Actor using, boolean success, Target subject) {
      super.applyEffect(using, success, subject);
      if (success) {
        CombatFX.applyBurstFX(
          PIERCE_FX_MODEL, subject.position(null), 0.5f, using.world()
        );
        ((Actor) subject).health.takeInjury(Rand.index(2) + 0.5f);
      }
    }
  };

  
  /*
  //  Mobile Salvo- chance to fire while dodging or retreating.
  //  TODO:  This basically has to schedule a new action during movement-
  //  actions.  We'll work out the anims later.
  //  Special FX- special back-or-side shot animations.
  final public static Technique MOBILE_SALVO = new Technique(
    "Mobile Salvo", DIR+"mobile_salvo.png", Action.FIRE,
    MarksmanTechniques.class, 03,
    MINOR_POWER        ,
    REAL_HARM          ,
    NO_FATIGUE         ,
    MINOR_CONCENTRATION,
    Technique.TYPE_COMBINED_ACTION, MARKSMANSHIP, 20,
    TRIGGER_ATTACK, TRIGGER_MOTION
  ) {
    //  TODO:  Check if the actor is dodging or retreating, and try scheduling
    //  an extra attack.
    public float applyBonus(Actor using, Behaviour b, Action a) {
      //  TODO:  Fill this in.
      return 5;
    }
  };
  //*/
}









