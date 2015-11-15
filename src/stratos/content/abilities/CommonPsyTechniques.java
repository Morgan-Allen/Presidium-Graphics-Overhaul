/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.abilities;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Technique.*;
import stratos.content.civic.TrooperLodge;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.Devices;
import stratos.game.plans.Combat;
import stratos.graphics.sfx.PlaneFX;
import stratos.util.Rand;



public class CommonPsyTechniques {
  

  final static String
    FX_DIR = "media/SFX/",
    UI_DIR = "media/GUI/Powers/";
  final static Class BASE_CLASS = CommonPsyTechniques.class;
  
  
  final public static PlaneFX.Model
    ZAP_FX = PlaneFX.imageModel(
      "zap_fx", BASE_CLASS,
      FX_DIR+"electro_zap.png",
      0.75f, 0, 0.1f, true, false
    ),
    HARMONICS_FX = PlaneFX.imageModel(
      "harmonics_fx", BASE_CLASS,
      FX_DIR+"shield_harmonics.png",
      0.65f, 0, 0.2f, true, false
    );
  
  final static float
    HARMONICS_RANGE   = 2.0f,
    HARMONICS_BOOST   = 1.0f;
  

  
  //  Shield Bypass- chance to puncture cover or armour after Take Aim.
  //  Special FX:  Attach 'puncture' fx at target...
  
  final public static Technique SHIELD_BYPASS = new Technique(
    "Shield Bypass", UI_DIR+"armour_bypass.png",
    "Allows shields' damage-reduction to be ignored.",
    BASE_CLASS, "shield_bypass",
    MINOR_POWER         ,
    REAL_HARM           ,
    NO_FATIGUE          ,
    MEDIUM_CONCENTRATION,
    IS_PASSIVE_SKILL_FX | IS_TRAINED_ONLY, HAND_TO_HAND, 15, Action.FIRE,
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
        /*
        ActionFX.applyBurstFX(
          PIERCE_FX_MODEL, subject.position(null), 0.5f, using.world()
        );
        //*/
        ((Actor) subject).health.takeInjury(Rand.index(2) + 0.5f, true);
      }
    }
  };
  
  
  final public static Technique BULLET_PARRY = null;//  TODO:  Fill in!
  
  
  
  final public static Technique ELECTROCUTE = new Technique(
    "Electrocute", UI_DIR+"electrocute.png",
    "Deals bonus shock damage during close combat, even in defence.",
    BASE_CLASS, "electrocute",
    MINOR_POWER        ,
    REAL_HARM          ,
    NO_FATIGUE         ,
    MINOR_CONCENTRATION,
    IS_PASSIVE_SKILL_FX | IS_TRAINED_ONLY, HAND_TO_HAND, 5,
    HAND_TO_HAND
  ) {
    
    public boolean canBeLearnt(Actor learns, boolean trained) {
      return
        hasGear(learns, Devices.HALBERD_GUN) &&
        hasUpgrade(learns.mind.work(), TrooperLodge.SPARRING_GYM, 1) &&
        super.canBeLearnt(learns, trained);
    }
    
    
    public float passiveBonus(Actor using, Skill skill, Target subject) {
      return Rand.num() * 5;
    }
    
    
    public void applyEffect(
      Actor using, boolean success, Target subject, boolean passive
    ) {
      super.applyEffect(using, success, subject, passive);
      if (success && subject instanceof Actor) {
        ActionFX.applyBurstFX(ZAP_FX, subject, 0.5f, 1);
        
        final float damage = using.gear.totalDamage() * Rand.num() / 2;
        final Actor struck = (Actor) subject;
        struck.health.takeFatigue(damage);
        struck.health.takeInjury(damage, false);
      }
    }
  };
  
  
  final public static Technique SHIELD_HARMONICS = new Technique(
    "Shield Harmonics", UI_DIR+"shield_harmonics.png",
    "Provides additional shield regeneration to nearby allies.",
    BASE_CLASS, "shield_harmonics",
    MEDIUM_POWER       ,
    REAL_HELP          ,
    NO_FATIGUE         ,
    MINOR_CONCENTRATION,
    IS_ANY_TARGETING | IS_PSY_ABILITY | IS_TRAINED_ONLY, null, -1,
    Action.FIRE, Action.QUICK
  ) {
    
    public boolean triggersAction(
      Actor actor, Plan current, Target subject
    ) {
      if (! (current instanceof Combat)) return false;
      if (actor.traits.hasTrait(asCondition)) return false;
      return true;
    }


    public void applyEffect(
      Actor using, boolean success, Target subject, boolean passive
    ) {
      using.traits.setLevel(asCondition, 1);
    }
    
    
    protected void applyAsCondition(Actor affected) {
      super.applyAsCondition(affected);
      ActionFX.applyBurstFX(HARMONICS_FX, affected, 0.5f, 1);
      affected.gear.boostShields(HARMONICS_BOOST, true);
      
      for (Actor ally : PlanUtils.subjectsInRange(affected, HARMONICS_RANGE)) {
        if (affected.base() != ally.base() || affected == ally) continue;
        ally.gear.boostShields(HARMONICS_BOOST / 2, true);
        ActionFX.applyBurstFX(HARMONICS_FX, ally, 0.5f, 1);
      }
    }
  };
}
