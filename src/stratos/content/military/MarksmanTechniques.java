

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



public class MarksmanTechniques {
  
  
  final static ShotFX.Model
    TRACE_FX_MODEL = new ShotFX.Model(
      "tracer_beam_fx", MarksmanTechniques.class,
      "media/SFX/tracer_beam.gif", 0.05f, 0, 0.05f, 3, true, true
    );
  final public static PlaneFX.Model
    AIM_MODEL = new PlaneFX.Model(
      "aim_model", MarksmanTechniques.class,
      "media/SFX/aiming_anim.gif", 4, 4, 25, (25 / 25f)
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
    Technique.TYPE_PASSIVE_EFFECT, MARKSMANSHIP, 5,
    TRIGGER_ATTACK, MARKSMANSHIP
  ) {
    public float applyEffect(Actor using, Behaviour b, Action a) {
      if (! (b instanceof Combat)) return -1;
      
      final Target target = ((Combat) b).subject;
      final Tile spot = target.world().tileAt(target), stood = using.origin();
      
      //  TODO:  Try and get rid of these?  Are they really needed?
      //  Just check if the target is moving, and confer a bonus otherwise.
      
      final Target lastTarget = (Target) refBeforeStore(using, target, "LT");
      final Tile   lastSpot   = (Tile  ) refBeforeStore(using, spot  , "LS");
      final Tile   lastStood  = (Tile  ) refBeforeStore(using, stood , "LO");
      
      final float lastBonus = valStored(using, "LB");
      float bonus = 0;
      if (target == lastTarget && spot  == lastSpot ) bonus += 0.5f;
      if (target == lastTarget && stood == lastStood) bonus += 0.5f;
      if (bonus > 0) bonus += lastBonus;
      if (bonus > 2) bonus = 2;
      
      float chance = using.skills.chance(MARKSMANSHIP, 10);
      storeVal(using, bonus, "LB");
      
      //
      //  Last but not least, include special FX-
      final Vec3D posFX = using.position(null);
      posFX.z += using.height();
      CombatFX.applyBurstFX(AIM_MODEL, posFX, 1, using.world());
      
      return bonus * chance * 5;
    }
  };
  
  
  //  Suppression Fire- expend extra ammo- missed shots can still prevent
  //                    target from moving.
  //  Special FX- make target stunned/stealthed for a second.
  //            - hail of secondary shots in arc.
  
  final public static Technique SUPPRESSION_FIRE = new Technique(
    "Suppression Fire", DIR+"suppression_fire.png", Action.FIRE,
    MarksmanTechniques.class, 01,
    MINOR_POWER        ,
    REAL_HARM          ,
    NO_FATIGUE         ,
    MAJOR_CONCENTRATION,
    Technique.TYPE_COMBINED_ACTION, MARKSMANSHIP, 10,
    TRIGGER_ATTACK, MARKSMANSHIP
  ) {
    public float applyEffect(Actor using, Behaviour b, Action a) {
      
      final DeviceType type = using.gear.deviceType();
      final Target target = a.subject();
      final Tile o = using.world().tileAt(target);
      
      for (Tile t : o.allAdjacent(null)) {
        if (Rand.yes()) continue;
        CombatFX.applyFX(type, using, t, false);
      }
      
      using.gear.incAmmo(-5);
      
      if (target instanceof Actor && Rand.yes()) {
        applyAsCondition((Actor) target);
      }
      return 5;
    }
    
    
    protected float appealFor(Actor actor, Target subject, float harmLevel) {
      final float appeal = super.appealFor(actor, subject, harmLevel);
      if (appeal <= 0) return 0;
      return appeal * actor.gear.ammoLevel();
    }
    
    
    protected void applyAsCondition(Actor affected) {
      super.applyAsCondition(affected);
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
    Technique.TYPE_PASSIVE_EFFECT, MARKSMANSHIP, 15,
    TRIGGER_ATTACK, MARKSMANSHIP
  ) {
    public float applyEffect(Actor using, Behaviour b, Action a) {
      if (STEADY_AIM.applyEffect(using, b, a) <= 0) return 0;
      
      float bonus = using.traits.traitLevel(MARKSMANSHIP) / 2f;
      bonus *= using.gear.attackDamage() / 10f;
      //  TODO:  Find some way to circumvent armour dynamically?
      //CombatUtils.dealDamage(b.subject(), bonus / 2);
      return bonus / 2;
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









