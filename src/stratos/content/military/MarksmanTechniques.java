
package stratos.content.military;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.plans.*;

import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Technique.*;




public class MarksmanTechniques {
  
  
  final static String DIR = "media/GUI/Powers/";
  
  //  Steady Aim- bonus as long as you or the target stay in place.
  final public static Technique STEADY_AIM = new Technique(
    "Steady Aim", DIR+"steady_aim.png",
    MarksmanTechniques.class, 00,
    Technique.TYPE_PASSIVE_EFFECT, MARKSMANSHIP, 5,
    TRIGGER_ATTACK, MARKSMANSHIP
  ) {
    public float applyBonus(Actor using, Behaviour b, Action a) {
      if (! (b instanceof Combat)) return -1;

      final Target target = ((Combat) b).subject;
      final Tile spot = target.world().tileAt(target), stood = using.origin();
      
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
      
      //  TODO:  You also need to add a special effect!
      return bonus * chance * 5;
    }
  };
  
  
  //  Suppression Fire- shots can disconcert opponent even when missed.
  final public static Technique SUPPRESSION_FIRE = new Technique(
    "Suppression Fire", DIR+"suppression_fire.png",
    MarksmanTechniques.class, 01,
    Technique.TYPE_COMBINED_ACTION, MARKSMANSHIP, 10,
    TRIGGER_ATTACK, MARKSMANSHIP
  ) {
    public float applyBonus(Actor using, Behaviour b, Action a) {
      //  TODO:  Fill this in.
      return 5;
    }
  };
  
  
  //  Armour Bypass- chance to puncture cover or armour after Take Aim.
  final public static Technique ARMOUR_BYPASS = new Technique(
    "Armour Bypass", DIR+"armour_bypass.png",
    MarksmanTechniques.class, 02,
    Technique.TYPE_PASSIVE_EFFECT, MARKSMANSHIP, 15,
    TRIGGER_ATTACK, MARKSMANSHIP
  ) {
    public float applyBonus(Actor using, Behaviour b, Action a) {
      if (STEADY_AIM.applyBonus(using, b, a) <= 0) return 0;
      
      float bonus = using.traits.traitLevel(MARKSMANSHIP) / 2f;
      bonus *= using.gear.attackDamage() / 10f;
      
      //  TODO:  Find some way to circumvent armour dynamically?
      //CombatUtils.dealDamage(b.subject(), bonus / 2);
      return bonus / 2;
    }
  };

  
  //  Mobile Salvo- chance to fire while dodging or retreating.
  //  TODO:  This basically has to schedule a new action during movement-
  //  actions.  We'll work out the anims later.
  final public static Technique MOBILE_SALVO = new Technique(
    "Mobile Salvo", DIR+"mobile_salvo.png",
    MarksmanTechniques.class, 03,
    Technique.TYPE_PASSIVE_EFFECT, MARKSMANSHIP, 20,
    TRIGGER_ATTACK, TRIGGER_MOTION
  ) {
    //  TODO:  Check if the actor is dodging or retreating, and try scheduling
    //  an extra attack.
    public float applyBonus(Actor using, Behaviour b, Action a) {
      //  TODO:  Fill this in.
      return 5;
    }
  };
}






