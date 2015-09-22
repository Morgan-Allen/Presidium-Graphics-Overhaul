/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.graphics.common.*;
import stratos.graphics.sfx.*;
import stratos.graphics.solids.*;
import stratos.util.*;
import static stratos.game.economic.Devices.*;



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
      "media/SFX/spear_throw.gif",
      0.1f, 0.2f,
      0.12f, 1.2f,
      false, false
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
    
    if (type == null) {
      //  TODO:  Use a special 'punch' effect here...
      applyMeleeFX(SLASH_FX_MODEL, uses, applied, world);
    }
    
    else if (type == HUNTING_LANCE) {
      applyShotFX(
        SPEAR_FX_MODEL,
        uses, applied, hits, 1 + (distance * 0.1f), world
      );
    }
    
    else if (type.hasProperty(RANGED | KINETIC)) {
      applyShotFX(
        PISTOL_FX_MODEL, PISTOL_BURST_MODEL,
        uses, applied, hits, 1 + (distance * 0.1f), world
      );
    }
    
    else if (type.hasProperty(RANGED | ENERGY)) {
      applyShotFX(
        LASER_FX_MODEL, LASER_BURST_MODEL,
        uses, applied, hits, 0.66f, world
      );
    }
    
    else {
      applyMeleeFX(SLASH_FX_MODEL, uses, applied, world);
    }
  }
  
  
  public static void applyMeleeFX(
    ModelAsset model, Mobile uses, Target applied, Stage world
  ) {
    //
    //  Put in a little 'splash' FX, in the direction of the arc.
    final float r = uses.radius();
    final Sprite slashFX = model.makeSprite();
    uses.viewPosition(slashFX.position);
    slashFX.scale = r * 2;
    slashFX.position.z += uses.height() / 2;
    world.ephemera.addGhost(uses, r, slashFX, 0.33f);
  }
  
  
  public static void applyShotFX(
    ShotFX.Model model, PlaneFX.Model burstModel,
    Mobile uses, Target applied,
    boolean hits, float duration, Stage world
  ) {
    final ShotFX shot = applyShotFX(
      model, uses, applied, hits, duration, world
    );
    applyBurstFX(burstModel, shot.origin, 0.66f, world);
    applyBurstFX(burstModel, shot.target, 0.66f, world);
  }
  
  
  public static ShotFX applyShotFX(
    ShotFX.Model model, Mobile uses, Target applied,
    boolean hits, float duration, Stage world
  ) {
    final ShotFX shot = (ShotFX) model.makeSprite();
    //  TODO:  Consider setting the fire point manually if the animation state
    //  hasn't matured yet?
    
    final Sprite sprite = uses.sprite();
    uses.viewPosition(sprite.position);
    sprite.attachPoint("fire", shot.origin);
    shot.target.setTo(hitPoint(applied, hits));
    
    shot.position.setTo(shot.origin).add(shot.target).scale(0.5f);
    final float size = shot.origin.sub(shot.target, null).length() / 2;
    
    if (false) I.reportVars(
      "\nShot was fired:", "  ",
      "From: "    , uses+" ("+uses.position(null)+")",
      "To: "      , applied+" ("+applied.position(null)+")",
      "Origin: "  , shot.origin,
      "Target: "  , shot.target,
      "Position: ", shot.position,
      "Size: "    , size
    );
    
    world.ephemera.addGhost(null, size + 1, shot, duration);
    return shot;
  }
  
  
  public static void applyBurstFX(
    PlaneFX.Model model, Target point, float heightFraction, float duration
  ) {
    final Vec3D pos;
    if (point instanceof Mobile) pos = ((Mobile) point).viewPosition(null);
    else pos = point.position(null);
    pos.z += point.height() * heightFraction;
    
    final Sprite s = model.makeSprite();
    s.position.setTo(pos);
    point.world().ephemera.addGhost(point, 1, s, duration);
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
      uses.viewPosition(shieldFX.position);
      shieldFX.position.z += uses.height() / 2;
      world.ephemera.addGhost(uses, 1, shieldFX, 2);
    }
    if (attackedBy != null) {
      shieldFX.attachBurstFromPoint(attackedBy.position(null), hits);
    }
    else shieldFX.resetGlow();
  }
}




