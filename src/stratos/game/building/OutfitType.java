

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
    super(baseClass, FORM_OUTFIT, name, basePrice, null);
    
    this.defence = defence;
    this.shieldBonus = shieldBonus;
    this.materials = new Conversion(facility, Visit.compose(
      Object.class, conversionArgs, new Object[] { TO, 1, this })
    );
    setPrice(basePrice, materials);
    
    final String imagePath = ITEM_PATH+name+"_skin.gif";
    if (new File(imagePath).exists()) {
      this.skin = ImageAsset.fromImage(baseClass, imagePath);
    }
    else this.skin = null;
  }
}







