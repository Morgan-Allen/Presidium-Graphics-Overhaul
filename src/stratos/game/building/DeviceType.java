

package stratos.game.building;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.sfx.*;
import stratos.graphics.solids.*;
import stratos.util.*;

import static stratos.game.building.Economy.*;



//  TODO:  Replace most of these with a shorter set of categories.

//  Trooper-    Halberd Gun & Power Armour
//  Noble-      Dirk & Body Armour
//  Enforcer-   Stun Wand & Body Armour
//  Kommando-   Zweihander & Stealth Suit
//  Runner-     Blaster & Stealth Suit
//  Ace-        Blaster & Seal Suit

//  Pseer-      Psy Staff
//  Palatine-   Arc Sabre & Shield Bracer
//  Xenopath-   Inhibitor
//  Physician-  Biocorder
//  Artificer-  Maniples & Golem Frame
//  Ecologist-  Stun Wand & Seal Suit

//  Collective- Gestalt Psy
//  Archon-     Zero Point Energy
//  Jil Baru-   Pets & Microbes
//  Logician-   Unarmed
//  Navigator-  Psy Projection
//  Tek Priest- Drone Minions



public class DeviceType extends TradeType {
  
  
  /**  Data fields, property accessors-
    */
  final static ShotFX.Model
    LASER_FX_MODEL = new ShotFX.Model(
      "laser_beam_fx", DeviceType.class,
      "media/SFX/blast_beam.gif", 0.05f, 0, 0.05f, 3, true, true
    ),
    PISTOL_FX_MODEL = new ShotFX.Model(
      "pistol_shot_fx", DeviceType.class,
      "media/SFX/pistol_shot.gif", 0.02f, 0, 0.03f, 1.5f, true, true
    ),
    SPEAR_FX_MODEL = new ShotFX.Model(
      "spear_fx", DeviceType.class,
      "media/SFX/spear_throw.gif", 0.1f, 0.0f, 0.12f, 1.2f, false, false
    );
  final static PlaneFX.Model
    SLASH_FX_MODEL = new PlaneFX.Model(
      "slash_fx", DeviceType.class,
      "media/SFX/melee_slash.png", 0.5f, 0, 0, false, false
    ),
    LASER_BURST_MODEL = new PlaneFX.Model(
      "laser_burst_fx", DeviceType.class,
      "media/SFX/laser_burst.png", 0.75f, 0, 0, true, true
    ),
    PISTOL_BURST_MODEL = new PlaneFX.Model(
      "pistol_burst_fx", DeviceType.class,
      "media/SFX/pistol_burst.png", 0.2f, 180, 0, true, true
    );
  
  
  final public float baseDamage;
  final public int properties;
  final Conversion materials;
  
  final public String groupName, animName;
  
  
  DeviceType(
    Class baseClass, String name,
    String groupName, String animName,
    float baseDamage, int properties, int basePrice,
    Class facility, Object... conversionArgs
  ) {
    super(baseClass, Economy.FORM_DEVICE, name, basePrice);
    
    this.baseDamage = baseDamage;
    this.properties = properties;
    this.materials = new Conversion(facility, Visit.compose(
      Object.class, conversionArgs, new Object[] { TO, this })
    );
    
    this.groupName = groupName;
    this.animName = animName;
  }
  
  
  public Conversion materials() {
    return materials;
  }
  
  
  public boolean hasProperty(int p) {
    return (properties & p) == p;
  }
  
  
  
  /**  Rendering and interface-
    */
  static Vec3D hitPoint(Target applied, boolean hits) {
    final Vec3D HP = applied.position(null);
    final float r = applied.radius(), h = applied.height() / 2;
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
    final World world = uses.world();
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
      applyBurstFX(PISTOL_BURST_MODEL, shot, 0.66f, world);
    }
    else if (type.hasProperty(RANGED | ENERGY)) {
      final ShotFX shot = applyShotFX(
        LASER_FX_MODEL, uses, applied, hits, 0.66f, world
      );
      applyBurstFX(LASER_BURST_MODEL, shot, 0.66f, world);
    }
  }
  
  
  private static ShotFX applyShotFX(
    ShotFX.Model model, Mobile uses, Target applied,
    boolean hits, float duration, World world
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
  
  
  //  TODO:  Unify the burst FX with ShotFX models?
  private static void applyBurstFX(
    PlaneFX.Model model, ShotFX shot, float duration, World world
  ) {
    final Sprite
      BO = model.makeSprite(),
      BT = model.makeSprite();
    BO.position.setTo(shot.origin);
    BT.position.setTo(shot.target);
    world.ephemera.addGhost(null, 1, BO, duration);
    world.ephemera.addGhost(null, 1, BT, duration);
  }
}














