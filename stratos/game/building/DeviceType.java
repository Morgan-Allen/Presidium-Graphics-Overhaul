

package stratos.game.building ;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.sfx.*;
import stratos.graphics.solids.*;
import stratos.util.*;



public class DeviceType extends Service implements Economy {
  
  
  
  /**  Data fields, property accessors-
    */
  final static ShotFX.Model
    LASER_FX_MODEL = new ShotFX.Model(
      "laser_beam_fx", DeviceType.class,
      "media/SFX/blast_beam.gif", 0.05f, 0, 0.05f, 3, true, true
    ),
    PISTOL_FX_MODEL = new ShotFX.Model(
      "pistol_shot_fx", DeviceType.class,
      "media/SFX/pistol_shot.gif", 0.075f, 0, 0.03f, 1.5f, true, true
    ),
    SPEAR_FX_MODEL = new ShotFX.Model(
      "spear_fx", DeviceType.class,
      "media/SFX/spear_throw.gif", 0.1f, 0.0f, 0.12f, 1.2f, false, false
    ) ;
  final static PlaneFX.Model
    SLASH_FX_MODEL = new PlaneFX.Model(
      "slash_fx", DeviceType.class,
      "media/SFX/melee_slash.png", 0.5f, 0, 0, false, false
    ),
    LASER_BURST_MODEL = new PlaneFX.Model(
      "laser_burst_fx", DeviceType.class,
      "media/SFX/laser_burst.png", 0.75f, 0, 0, true, true
    ) ;
  
  
  final public float baseDamage ;
  final public int properties ;
  final Conversion materials ;
  
  final public String groupName, animName ;
  
  
  DeviceType(
    Class baseClass, String name,
    float baseDamage, int properties,
    int basePrice, Conversion materials,
    String groupName, String animName
  ) {
    super(
      baseClass, FORM_DEVICE, name,
      basePrice + (materials == null ? 0 : materials.rawPriceValue())
    ) ;
    this.baseDamage = baseDamage ;
    this.properties = properties ;
    this.materials = materials ;
    this.groupName = groupName ;
    this.animName = animName ;
  }
  
  
  public Conversion materials() {
    return materials ;
  }
  
  
  public boolean hasProperty(int p) {
    return (properties & p) == p ;
  }
  
  
  
  /**  Rendering and interface-
    */
  static Vec3D hitPoint(Target applied, boolean hits) {
    final Vec3D HP = applied.position(null) ;
    final float r = applied.radius(), h = applied.height() / 2 ;
    HP.z += h ;
    if (hits) return HP ;
    HP.x += Rand.range(-r, r) ;
    HP.y += Rand.range(-r, r) ;
    HP.z += Rand.range(-h, h) ;
    return HP ;
  }
  
  
  //
  //  TODO:  Move all weapon/armour types to a dedicated interface listing and
  //  customise their SFX there.
  
  public static void applyFX(
    DeviceType type, Mobile uses, Target applied, boolean hits
  ) {
    final float distance = Spacing.distance(uses, applied);
    final World world = uses.world() ;
    if (type == null || type.hasProperty(MELEE)) {
      //
      //  Put in a little 'splash' FX, in the direction of the arc.
      final float r = uses.radius() ;
      final Sprite slashFX = SLASH_FX_MODEL.makeSprite() ;
      slashFX.scale = r * 2 ;
      world.ephemera.addGhost(uses, r, slashFX, 0.33f) ;
    }
    //  TODO:  Have the device types themselves specify their preferred SFX.
    //else if (type.hasProperty(RANGED | THROWN)) {
    //}
    
    //  TODO:  Consider setting the fire point manually- at least if the
    //  animation state hasn't matured yet?
    
    else if (type.hasProperty(RANGED | PHYSICAL)) {
      
      //  You'll have to create a missile effect, with similar parameters.
      final ShotFX shot = (ShotFX) PISTOL_FX_MODEL.makeSprite() ;
      
      final SolidSprite sprite = (SolidSprite) uses.sprite() ;
      uses.viewPosition(sprite.position) ;
      sprite.attachPoint("fire", shot.origin);
      shot.target.setTo(hitPoint(applied, hits)) ;
      
      shot.position.setTo(shot.origin).add(shot.target).scale(0.5f) ;
      final float size = shot.origin.sub(shot.target, null).length() / 2 ;
      world.ephemera.addGhost(null, size + 1, shot, 1 + (distance * 0.1f)) ;
    }
    else if (type.hasProperty(RANGED | ENERGY)) {
      
      //  Otherwise, create an appropriate 'beam' FX-
      final ShotFX shot = (ShotFX) LASER_FX_MODEL.makeSprite() ;
      
      final SolidSprite sprite = (SolidSprite) uses.sprite() ;
      uses.viewPosition(sprite.position) ;
      sprite.attachPoint("fire", shot.origin);
      shot.target.setTo(hitPoint(applied, hits)) ;
      
      shot.position.setTo(shot.origin).add(shot.target).scale(0.5f) ;
      final float size = shot.origin.sub(shot.target, null).length() / 2 ;
      world.ephemera.addGhost(null, size + 1, shot, 0.66f) ;
      
      final Sprite
        BO = LASER_BURST_MODEL.makeSprite(),
        BT = LASER_BURST_MODEL.makeSprite() ;
      BO.position.setTo(shot.origin) ;
      BT.position.setTo(shot.target) ;
      //world.ephemera.addGhost(null, 1, BO, 0.66f) ;
      world.ephemera.addGhost(null, 1, BT, 0.66f) ;
    }
  }
}














