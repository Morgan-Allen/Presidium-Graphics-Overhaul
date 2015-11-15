/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.abilities;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.game.wild.Wreckage;
import stratos.game.actors.*;
import stratos.graphics.sfx.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Technique.*;
import static stratos.game.economic.Economy.*;




public class TrooperTechniques {

  final static String
    FX_DIR = "media/SFX/",
    UI_DIR = "media/GUI/Powers/";
  final static Class BASE_CLASS = TrooperTechniques.class;
  
  
  final public static PlaneFX.Model
    FRAG_BURST_FX = PlaneFX.imageModel(
      "frag_burst_fx", BASE_CLASS,
      FX_DIR+"frag_burst.png",
      1.0f, 0, 1.0f, true, true
    ),
    FRAG_FRINGE_FX = PlaneFX.imageModel(
      "frag_fringe_fx", BASE_CLASS,
      FX_DIR+"detonate_burst.png",
      1.0f, 0.5f, 1.0f, false, false
    );
  final public static ShotFX.Model
    SUPPRESS_FIRE_FX = new ShotFX.Model(
      "suppress_fire_fx", BASE_CLASS,
      FX_DIR+"suppress_fire.png",
      -1, 0, 0.25f, 1, false, true
    ),
    FRAG_MISSILE_FX = new ShotFX.Model(
      "frag_missile_fx", BASE_CLASS,
      FX_DIR+"frag_missile.png",
      0.05f, 0.1f, 0.15f, 0.75f, false, false
    );
  
  final static int
    FENDING_SKILL_BONUS  =   5,
    FENDING_DAMAGE_EXTRA =   3,
    SUPPRESS_RADIUS      =   1,
    SUPPRESS_AMMO_DRAIN  = ActorGear.MAX_AMMO_COUNT / 4,
    SUPPRESS_DEBUFF      = -10,
    SUPPRESS_DURATION    =   5,
    FRAG_BURST_RANGE     =   3,
    FRAG_DAMAGE_MIN      =   4,
    FRAG_DAMAGE_BONUS    =   8,
    ARMOUR_POWER_BASE    =   5;
  
  
  
  final public static Technique FENDING_BLOW = new Technique(
    "Fending Blow", UI_DIR+"fend.png",
    "Improves defensive skill in melee while granting a chance to deal bonus "+
    "nonlethal damage.",
    BASE_CLASS, "fending_blow",
    MINOR_POWER         ,
    MILD_HARM           ,
    MINOR_FATIGUE       ,
    MINOR_CONCENTRATION ,
    IS_FOCUS_TARGETING | IS_PASSIVE_SKILL_FX | IS_TRAINED_ONLY, HAND_TO_HAND, 5,
    Action.STRIKE_BIG, Action.QUICK
  ) {
    
    public boolean triggersAction(Actor actor, Plan current, Target subject) {
      if (actor.gear.deviceType() != Devices.HALBERD_GUN) return false;
      if (Spacing.distance(actor, subject) >= 1) return false;
      return current instanceof Combat && subject instanceof Actor;
    }
    
    
    public boolean triggersPassive(
      Actor actor, Plan current, Skill used, Target subject, boolean reactive
    ) {
      if (actor.gear.deviceType() != Devices.HALBERD_GUN) return false;
      if (used != HAND_TO_HAND) return false;
      return reactive || Technique.isDoingAction(actor, this);
    }
    
    
    public float passiveBonus(Actor using, Skill skill, Target subject) {
      return FENDING_SKILL_BONUS;
    }
    
    
    protected boolean checkActionSuccess(Actor actor, Target subject) {
      if (! Combat.performStrike(
        actor, (Actor) subject, HAND_TO_HAND, HAND_TO_HAND,
        Combat.OBJECT_EITHER, actor.currentAction()
      )) return false;
      return super.checkActionSuccess(actor, subject);
    }
    
    
    public void applyEffect(
      Actor using, boolean success, Target subject, boolean passive
    ) {
      //  (We want the passive skill-bonus, but we don't want to apply the
      //  technique twice-)
      if (passive && Technique.isDoingAction(using, this)) return;
      super.applyEffect(using, success, subject, passive);
      if (success) {
        final Actor struck = (Actor) subject;
        struck.health.takeFatigue(FENDING_DAMAGE_EXTRA);
      }
    }
  };
  
  
  final public static Technique SUPPRESSION = new Technique(
    "Suppression", UI_DIR+"suppression.png",
    "Deals scattershot damage to temporarily lower a target's mobility and "+
    "accuracy.",
    BASE_CLASS, "suppression_fire",
    MEDIUM_POWER        ,
    MILD_HARM           ,
    NO_FATIGUE          ,
    MAJOR_CONCENTRATION ,
    IS_FOCUS_TARGETING | IS_TRAINED_ONLY, MARKSMANSHIP, 5,
    Action.FIRE, Action.RANGED
  ) {
    
    public boolean triggersAction(Actor actor, Plan current, Target subject) {
      if (actor.gear.deviceType() != Devices.HALBERD_GUN) return false;
      return current instanceof Combat && subject instanceof Actor;
    }
    
    
    public float basePriority(Actor actor, Plan current, Target subject) {
      final float appeal = super.basePriority(actor, current, subject);
      if (appeal <= 0) return 0;
      float ammoCost = SUPPRESS_AMMO_DRAIN * 1f / actor.gear.ammoCount();
      return appeal * Nums.clamp(1 - ammoCost, 0, 1);
    }
    
    
    protected float effectRadius() {
      return SUPPRESS_RADIUS;
    }
    
    
    protected boolean effectDescriminates() {
      return false;
    }
    
    
    protected boolean checkActionSuccess(Actor actor, Target subject) {
      return Combat.performGeneralStrike(
        actor, subject, Combat.OBJECT_EITHER, actor.currentAction()
      );
    }
    
    
    protected void applySelfEffects(Actor using) {
      super.applySelfEffects(using);
      using.gear.incAmmo(0 - SUPPRESS_AMMO_DRAIN);
      ActionFX.applyShotFX(
        SUPPRESS_FIRE_FX, using, using.actionFocus(), true, 1, using.world()
      );
    }
    
    
    public void applyEffect(
      Actor using, boolean success, Target subject, boolean passive
    ) {
      super.applyEffect(using, success, subject, passive);
      if (! (subject instanceof Actor)) return;
      
      final Actor hits = (Actor) subject;
      final float oldLevel = hits.traits.traitLevel(asCondition);
      if (success) {
        hits.traits.setLevel(asCondition, 1);
        hits.forceReflex(Action.MOVE_SNEAK, false);
      }
      else if (oldLevel < 0.5f) {
        hits.traits.setLevel(asCondition, 0.5f);
      }
    }
    
    
    protected float conditionDuration() {
      return SUPPRESS_DURATION;
    }
    
    
    protected void applyAsCondition(Actor affected) {
      super.applyAsCondition(affected);
      final float level = affected.traits.traitLevel(asCondition);
      
      final float penalty = SUPPRESS_DEBUFF * level;
      affected.traits.incBonus(MARKSMANSHIP, penalty);
      affected.traits.incBonus(SURVEILLANCE, penalty);
      affected.traits.incBonus(ATHLETICS   , penalty);
    }
  };
  
  
  
  final public static Traded FRAG_LAUNCHER_AMMO = new Traded(
    BASE_CLASS, "Frag Launcher", null, FORM_MATERIAL, 0,
    "Ammunition for Frag Launchers."
  ) {
    
  };
  
  
  final public static Technique FRAG_LAUNCHER = new Technique(
    "Frag Launcher", UI_DIR+"frag_launcher.png",
    "Launches a devastating rocket attack at long-to-medium range.  Base "+
    "damage is proportionate to weapon quality, with a +"+FRAG_DAMAGE_BONUS+
    "bonus.  50% damage within "+FRAG_BURST_RANGE+" tiles, +50% vs. "+
    "buildings and vehicles.",
    BASE_CLASS, "frag_launcher",
    MAJOR_POWER        ,
    EXTREME_HARM       ,
    MINOR_FATIGUE      ,
    MAJOR_CONCENTRATION,
    IS_FOCUS_TARGETING | IS_TRAINED_ONLY, MARKSMANSHIP, 10,
    Action.FIRE, Action.QUICK | Action.RANGED
  ) {
    
    public boolean triggersAction(
      Actor actor, Plan current, Target subject
    ) {
      if (actor.gear.deviceType() != Devices.HALBERD_GUN) return false;
      if (actor.gear.amountOf(FRAG_LAUNCHER_AMMO) == 0) return false;
      return current instanceof Combat && subject instanceof Actor;
    }
    
    
    public Traded itemNeeded() {
      return FRAG_LAUNCHER_AMMO;
    }
    
    
    protected float effectRadius() {
      return FRAG_BURST_RANGE;
    }
    
    
    protected boolean effectDescriminates() {
      return false;
    }
    
    
    protected void applySelfEffects(Actor using) {
      super.applySelfEffects(using);
      final Stage  world = using.world();
      final Target focus = using.actionFocus();
      final Vec3D  point = focus.position(null);
      
      using.gear.bumpItem(FRAG_LAUNCHER_AMMO, -1);
      
      ActionFX.applyShotFX(
        FRAG_MISSILE_FX, using, focus, true, 1.5f, world
      );
      ActionFX.applyBurstFX(FRAG_BURST_FX , point, 1, world);
      point.z += 0.1f;
      ActionFX.applyBurstFX(FRAG_FRINGE_FX, point, 1, world);
      Wreckage.plantCraterAround(world.tileAt(focus), 0.5f);
    }


    public void applyEffect(
      Actor using, boolean success, Target subject, boolean passive
    ) {
      super.applyEffect(using, success, subject, passive);
      
      float damage = (Rand.num() + 0.5f) * using.gear.totalDamage() / 1.5f;
      damage += FRAG_DAMAGE_BONUS;
      if (subject != using.actionFocus()) damage /= 2;
      
      if (subject instanceof Actor) {
        final Actor a = (Actor) subject;
        
        damage = Nums.max(0, a.gear.afterShields(damage, true));
        damage -= a.gear.totalArmour() * Rand.num();
        damage = Nums.max(damage, FRAG_DAMAGE_MIN);
        
        a.health.takeInjury(damage, true);
        a.forceReflex(Action.STAND, true);
        float damageLevel = damage / a.gear.baseArmour();
        Item.checkForBreakdown(a, a.gear.outfitEquipped(), damageLevel, 1);
      }
      
      if (subject instanceof Placeable) {
        final Placeable p = (Placeable) subject;
        p.structure().takeDamage(damage * 1.5f);
      }
    }
  };
  
  
  final public static Technique POWER_ARMOUR_USE = new Technique(
    "Power Armour Use", UI_DIR+"power_armour_use.png",
    "Permits the use of "+Outfits.POWER_ARMOUR+", granting additional "+
    "strength and protection at higher maintenance cost.",
    BASE_CLASS      , "power_armour_use",
    MAJOR_POWER     ,
    NO_HARM         ,
    NO_FATIGUE      ,
    NO_CONCENTRATION,
    IS_GEAR_PROFICIENCY | IS_PASSIVE_ALWAYS | IS_TRAINED_ONLY, HAND_TO_HAND, 10,
    Outfits.POWER_ARMOUR
  ) {
    public void applyEffect(
      Actor using, boolean success, Target subject, boolean passive
    ) {
      if (using.gear.outfitType() != Outfits.POWER_ARMOUR) return;
      super.applyEffect(using, success, subject, passive);
      
      final Item armour = using.gear.outfitEquipped();
      float bonus = ARMOUR_POWER_BASE;
      bonus *= (1 + (armour.quality / Item.AVG_QUALITY));
      using.traits.incBonus(MUSCULAR    , bonus    );
      using.traits.incBonus(ATHLETICS   , bonus / 2);
      using.traits.incBonus(HAND_TO_HAND, bonus / 2);
    }
  };
  
  
  final public static Technique TROOPER_TECHNIQUES[] = {
    FENDING_BLOW, SUPPRESSION, FRAG_LAUNCHER, POWER_ARMOUR_USE
  };
}













