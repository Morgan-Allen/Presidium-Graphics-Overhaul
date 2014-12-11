


package stratos.game.plans;

import stratos.game.common.*;
import stratos.game.economic.DeviceType;
import stratos.game.economic.OutfitType;
import stratos.graphics.common.*;
import stratos.graphics.sfx.*;
import stratos.graphics.solids.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;



public class CombatFX {
  
  
  /**  Data fields, property accessors-
    */
  final static ShotFX.Model
    LASER_FX_MODEL = new ShotFX.Model(
      "laser_beam_fx", CombatFX.class,
      "media/SFX/blast_beam.gif", 0.05f, 0, 0.05f, 3, true, true
    ),
    PISTOL_FX_MODEL = new ShotFX.Model(
      "pistol_shot_fx", CombatFX.class,
      "media/SFX/pistol_shot.gif", 0.02f, 0, 0.03f, 1.5f, true, true
    ),
    SPEAR_FX_MODEL = new ShotFX.Model(
      "spear_fx", CombatFX.class,
      "media/SFX/spear_throw.gif", 0.1f, 0.0f, 0.12f, 1.2f, false, false
    );
  final static PlaneFX.Model
    SLASH_FX_MODEL = new PlaneFX.Model(
      "slash_fx", CombatFX.class,
      "media/SFX/melee_slash.png", 0.5f, 0, 0, false, false
    ),
    LASER_BURST_MODEL = new PlaneFX.Model(
      "laser_burst_fx", CombatFX.class,
      "media/SFX/laser_burst.png", 0.75f, 0, 0, true, true
    ),
    PISTOL_BURST_MODEL = new PlaneFX.Model(
      "pistol_burst_fx", CombatFX.class,
      "media/SFX/pistol_burst.png", 0.2f, 180, 0, true, true
    );
  
  
  
  /**  Rendering and interface-
    */
  static Vec3D hitPoint(Target applied, boolean hits) {
    final Vec3D HP = applied.position(null);
    final float r = applied.radius() * 2, h = applied.height() / 2;
    HP.z += h;
    if (hits) return HP;
    HP.x += Rand.range(-r, r);
    HP.y += Rand.range(-r, r);
    HP.z += Rand.range(-h, h);
    return HP;
  }
  
  
  //
  //  TODO:  Move all weapon/armour types to a dedicated interface listing and
  //  customise their SFX there.
  //  TODO:  Have the device types themselves specify their preferred SFX.
  //else if (type.hasProperty(RANGED | THROWN)) {
  //}
  
  public static void applyFX(
    DeviceType type, Mobile uses, Target applied, boolean hits
  ) {
    final float distance = Spacing.distance(uses, applied);
    final Stage world = uses.world();
    
    if (type == null || type.hasProperty(MELEE)) {
      //
      //  Put in a little 'splash' FX, in the direction of the arc.
      final float r = uses.radius();
      final Sprite slashFX = SLASH_FX_MODEL.makeSprite();
      slashFX.scale = r * 2;
      world.ephemera.addGhost(uses, r, slashFX, 0.33f);
    }
    
    else if (type.hasProperty(RANGED | KINETIC)) {
      final ShotFX shot = applyShotFX(
        PISTOL_FX_MODEL, uses, applied, hits, 1 + (distance * 0.1f), world
      );
      applyBurstFX(PISTOL_BURST_MODEL, shot.origin, 0.66f, world);
      applyBurstFX(PISTOL_BURST_MODEL, shot.target, 0.66f, world);
    }
    
    else if (type.hasProperty(RANGED | ENERGY)) {
      final ShotFX shot = applyShotFX(
        LASER_FX_MODEL, uses, applied, hits, 0.66f, world
      );
      applyBurstFX(LASER_BURST_MODEL, shot.origin, 0.66f, world);
      applyBurstFX(LASER_BURST_MODEL, shot.target, 0.66f, world);
    }
  }
  
  
  public static ShotFX applyShotFX(
    ShotFX.Model model, Mobile uses, Target applied,
    boolean hits, float duration, Stage world
  ) {
    final ShotFX shot = (ShotFX) model.makeSprite();
    //  TODO:  Consider setting the fire point manually if the animation state
    //  hasn't matured yet.
    
    final SolidSprite sprite = (SolidSprite) uses.sprite();
    uses.viewPosition(sprite.position);
    sprite.attachPoint("fire", shot.origin);
    shot.target.setTo(hitPoint(applied, hits));
    
    shot.position.setTo(shot.origin).add(shot.target).scale(0.5f);
    final float size = shot.origin.sub(shot.target, null).length() / 2;
    world.ephemera.addGhost(null, size + 1, shot, duration);
    
    return shot;
  }
  
  
  public static void applyBurstFX(
    PlaneFX.Model model, Vec3D point, float duration, Stage world
  ) {
    final Sprite s = model.makeSprite();
    s.position.setTo(point);
    world.ephemera.addGhost(null, 1, s, duration);
  }
  
  
  public static void applyShieldFX(
    OutfitType type, Mobile uses, Target attackedBy, boolean hits
  ) {
    final Stage world = uses.world();
    final Stage.Visible visible = world.ephemera.matchGhost(
      uses, ShieldFX.SHIELD_MODEL
    );
    final ShieldFX shieldFX;
    if (visible != null) {
      shieldFX = (ShieldFX) visible.sprite();
      world.ephemera.updateGhost(uses, 1, ShieldFX.SHIELD_MODEL, 2);
    }
    else {
      shieldFX = new ShieldFX();
      shieldFX.scale = 0.5f * uses.height();
      world.ephemera.addGhost(uses, 1, shieldFX, 2);
    }
    if (attackedBy != null) {
      shieldFX.attachBurstFromPoint(attackedBy.position(null), hits);
    }
    else shieldFX.resetGlow();
  }
}




