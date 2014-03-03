

package src.game.building ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.sfx.* ;
import src.graphics.solids.* ;
import src.util.* ;



public class DeviceType extends Service implements Economy {
  
  
  
  /**  Data fields, property accessors-
    */
  final static ShotFX.Model
    LASER_FX_MODEL = new ShotFX.Model(
      "laser_beam_fx", DeviceType.class,
      "media/SFX/blast_beam.gif", 0.05f, 0, 0.05f, 3, true
    ),
    SPEAR_FX_MODEL = new ShotFX.Model(
      "spear_fx", DeviceType.class,
      "media/SFX/spear_throw.gif", 0.1f, 0.33f, 0.06f, 1.2f, false
    ) ;
  final static PlaneFX.Model
    SLASH_FX_MODEL = new PlaneFX.Model(
      "slash_fx", DeviceType.class,
      "media/SFX/melee_slash.png", 0.5f, 0, 0, false
    ),
    LASER_BURST_MODEL = new PlaneFX.Model(
      "laser_burst_fx", DeviceType.class,
      "media/SFX/laser_burst.png", 0.75f, 0, 0, true
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
    
    final World world = uses.world() ;
    if (type == null || type.hasProperty(MELEE)) {
      //
      //  Put in a little 'splash' FX, in the direction of the arc.
      final float r = uses.radius() ;
      final Sprite slashFX = SLASH_FX_MODEL.makeSprite() ;
      slashFX.scale = r * 2 ;
      world.ephemera.addGhost(uses, r, slashFX, 0.33f) ;
    }
    else if (type.hasProperty(RANGED | PHYSICAL)) {
      
      //  You'll have to create a missile effect, with similar parameters.
      final ShotFX shot = (ShotFX) SPEAR_FX_MODEL.makeSprite() ;
      
      final SolidSprite sprite = (SolidSprite) uses.sprite() ;
      uses.viewPosition(sprite.position, null) ;
      shot.origin.setTo(sprite.attachPoint("fire")) ;
      shot.target.setTo(hitPoint(applied, hits)) ;
      
      shot.position.setTo(shot.origin).add(shot.target).scale(0.5f) ;
      final float size = shot.origin.sub(shot.target, null).length() / 2 ;
      world.ephemera.addGhost(null, size + 1, shot, 1) ;
    }
    else if (type.hasProperty(RANGED | ENERGY)) {
      
      //  Otherwise, create an appropriate 'beam' FX-
      final ShotFX shot = (ShotFX) LASER_FX_MODEL.makeSprite() ;
      
      final SolidSprite sprite = (SolidSprite) uses.sprite() ;
      uses.viewPosition(sprite.position, null) ;
      shot.origin.setTo(sprite.attachPoint("fire")) ;
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














