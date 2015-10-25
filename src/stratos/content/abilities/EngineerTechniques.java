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
import stratos.game.wild.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Technique.*;
import stratos.graphics.common.*;
import stratos.graphics.sfx.*;
import stratos.util.Rand;



public class EngineerTechniques {
  
  
  final static String
    FX_DIR = "media/SFX/",
    UI_DIR = "media/GUI/Powers/";
  final static Class BASE_CLASS = EngineerTechniques.class;
  
  final static int
    DEMO_DAMAGE      = 5 ,
    WELD_REPAIR_MIN  = 2 ,
    WELD_REPAIR_MAX  = 7 ,
    RAM_DAMAGE_MIN   = 5 ,
    RAM_DAMAGE_MAX   = 10,
    RAM_BASE_HP_STUN = 10;
  
  
  final public static Technique DEMOLITION = new Technique(
    "Demolition", UI_DIR+"demolition.png",
    "Grants bonus damage against buildings, vehicles or robot opponents.",
    BASE_CLASS, "demolition",
    MINOR_POWER         ,
    REAL_HARM           ,
    NO_FATIGUE          ,
    MEDIUM_CONCENTRATION,
    IS_PASSIVE_SKILL_FX, ASSEMBLY, 5,
    HAND_TO_HAND
  ) {
    
    public boolean triggersPassive(
      Actor actor, Plan current, Skill used, Target subject
    ) {
      if (used != HAND_TO_HAND && used != MARKSMANSHIP) {
        return false;
      }
      if (subject instanceof Artilect) {
        return true;
      }
      if (subject instanceof Placeable) {
        return ((Placeable) subject).structure().isMechanical();
      }
      return false;
    }
    
    
    public void applyEffect(
      Actor using, boolean success, Target subject, boolean passive
    ) {
      super.applyEffect(using, success, subject, passive);
      if (success && subject instanceof Artilect) {
        final Artilect struck = (Artilect) subject;
        struck.health.takeInjury(DEMO_DAMAGE, false);
      }
      if (success && subject instanceof Placeable) {
        final Placeable struck = (Placeable) subject;
        struck.structure().takeDamage(DEMO_DAMAGE);
      }
    }
  };
  
  
  final public static Technique THERMAL_WELD = new Technique(
    "Thermal Weld", UI_DIR+"thermal_weld.png",
    "Speeds initial repairs of buildings and vehicles.  (Requires a "+
    Devices.MANIPULATOR+".)",
    BASE_CLASS, "thermal_weld",
    MEDIUM_POWER,
    REAL_HELP,
    MINOR_FATIGUE,
    MAJOR_CONCENTRATION,
    IS_PASSIVE_SKILL_FX, ASSEMBLY, 10,
    ASSEMBLY
  ) {
    
    public boolean triggersPassive(
      Actor actor, Plan current, Skill used, Target subject
    ) {
      if (actor.gear.deviceType() != Devices.MANIPULATOR) return false;
      if (! (current instanceof Repairs)) return false;
      final Structure s = ((Placeable) current.subject()).structure();
      return s.isMechanical() && s.repairLevel() < 0.5f;
    }
    
    
    public void applyEffect(
      Actor using, boolean success, Target subject, boolean passive
    ) {
      super.applyEffect(using, success, subject, passive);
      if (success) {
        final Placeable built = (Placeable) subject;
        float repair = roll(WELD_REPAIR_MIN, WELD_REPAIR_MAX);
        built.structure().repairBy(repair);
      }
    }
  };
  
  
  final public static Technique PNEUMATIC_RAM = new Technique(
    "Pneumatic Ram", UI_DIR+"pneumatic_ram.png",
    "Deals concussive damage to melee opponents.  Less effective against "+
    "large opponents, requires "+Devices.MANIPULATOR+", chance to stun.",
    BASE_CLASS, "pneumatic_ram",
    MEDIUM_POWER        ,
    MILD_HARM           ,
    NO_FATIGUE          ,
    MEDIUM_CONCENTRATION,
    IS_PASSIVE_SKILL_FX, HAND_TO_HAND, 5,
    HAND_TO_HAND
  ) {
    
    public boolean triggersPassive(
      Actor actor, Plan current, Skill used, Target subject
    ) {
      if (actor.gear.deviceType() != Devices.MANIPULATOR) return false;
      return (current instanceof Combat);
    }
    
    
    public void applyEffect(
      Actor using, boolean success, Target subject, boolean passive
    ) {
      super.applyEffect(using, success, subject, passive);
      float damage = roll(RAM_DAMAGE_MIN, RAM_DAMAGE_MAX);
      
      if (success && subject instanceof Actor) {
        final Actor struck = (Actor) subject;
        float stunChance = RAM_BASE_HP_STUN / struck.health.maxHealth();
        
        if (struck.health.organic()) {
          struck.health.takeInjury (damage / 2, false);
          struck.health.takeFatigue(damage / 2       );
        }
        else {
          struck.health.takeInjury(damage, false);
        }
        if (Rand.num() < stunChance) {
          struck.enterStateKO(Action.STAND);
        }
      }
      else if (success && subject instanceof Placeable) {
        final Placeable struck = (Placeable) subject;
        struck.structure().takeDamage(damage);
      }
    }
  };
  
  
  final public static Technique POWER_LIFTER_USE = new Technique(
    "Power Lifter Use", UI_DIR+"power_lifter_use.png",
    "Permits certain tamed animals to be used as personal mounts.",
    BASE_CLASS      , "mount_training",
    MAJOR_POWER     ,
    NO_HARM         ,
    NO_FATIGUE      ,
    NO_CONCENTRATION,
    IS_GEAR_PROFICIENCY | IS_TRAINED_ONLY, HAND_TO_HAND, 10,
    Outfits.POWER_LIFTER
  ) {};
  
  
  final public static Technique ENGINEER_TECHNIQUES[] = new Technique[] {
    DEMOLITION, THERMAL_WELD, PNEUMATIC_RAM, POWER_LIFTER_USE
  };
}













