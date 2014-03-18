

package src.game.building ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.sfx.ShieldFX;
import src.util.I;

import java.io.* ;



public class OutfitType extends Service implements Economy {
  
  
  final public float
    defence,
    shieldBonus ;
  final public Conversion materials ;
  final public ImageAsset skin ;
  
  
  public OutfitType(
    Class baseClass, String name, int defence, int shieldBonus, int basePrice,
    Conversion materials
  ) {
    super(
      baseClass, FORM_OUTFIT, name,
      basePrice + (materials == null ? 0 : materials.rawPriceValue())
    ) ;
    this.defence = defence ;
    this.shieldBonus = shieldBonus ;
    this.materials = materials ;
    final String imagePath = ITEM_PATH+name+"_skin.gif" ;
    if (new File(imagePath).exists())
      this.skin = ImageAsset.fromImage(imagePath, baseClass) ;
    else
      this.skin = null ;
  }
  
  
  public Conversion materials() {
    return materials ;
  }
  
  
  public static void applyFX(
    OutfitType type, Mobile uses, Target attackedBy, boolean hits
  ) {
    final World world = uses.world() ;
    final World.Visible visible = world.ephemera.matchGhost(
      uses, ShieldFX.SHIELD_MODEL
    ) ;
    if (visible != null) {
      final ShieldFX shieldFX = (ShieldFX) visible.sprite() ;
      shieldFX.attachBurstFromPoint(attackedBy.position(null), hits) ;
      world.ephemera.updateGhost(uses, 1, ShieldFX.SHIELD_MODEL, 2) ;
    }
    else {
      final ShieldFX shieldFX = new ShieldFX() ;
      shieldFX.scale = 0.5f * uses.height() ;
      world.ephemera.addGhost(uses, 1, shieldFX, 2) ;
    }
  }
}







