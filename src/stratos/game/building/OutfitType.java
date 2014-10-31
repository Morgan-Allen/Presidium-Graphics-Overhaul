

package stratos.game.building;

import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.sfx.ShieldFX;
import stratos.util.*;

import static stratos.game.building.Economy.*;

import java.io.*;



public class OutfitType extends Traded {
  
  
  final public float
    defence,
    shieldBonus;
  final public Conversion materials;
  
  final public ImageAsset skin;
  
  
  public OutfitType(
    Class baseClass, String name, int defence, int shieldBonus, int basePrice,
    Class facility, Object... conversionArgs
  ) {
    super(baseClass, FORM_OUTFIT, name, basePrice);
    
    this.defence = defence;
    this.shieldBonus = shieldBonus;
    this.materials = new Conversion(facility, Visit.compose(
      Object.class, conversionArgs, new Object[] { TO, this })
    );
    
    final String imagePath = ITEM_PATH+name+"_skin.gif";
    if (new File(imagePath).exists()) {
      this.skin = ImageAsset.fromImage(baseClass, imagePath);
    }
    else this.skin = null;
  }
  
  
  public Conversion materials() {
    return materials;
  }
  
  
  public static void applyFX(
    OutfitType type, Mobile uses, Target attackedBy, boolean hits
  ) {
    final Stage world = uses.world();
    final Stage.Visible visible = world.ephemera.matchGhost(
      uses, ShieldFX.SHIELD_MODEL
    );
    if (visible != null) {
      final ShieldFX shieldFX = (ShieldFX) visible.sprite();
      shieldFX.attachBurstFromPoint(attackedBy.position(null), hits);
      world.ephemera.updateGhost(uses, 1, ShieldFX.SHIELD_MODEL, 2);
    }
    else {
      final ShieldFX shieldFX = new ShieldFX();
      shieldFX.scale = 0.5f * uses.height();
      world.ephemera.addGhost(uses, 1, shieldFX, 2);
    }
  }
}







