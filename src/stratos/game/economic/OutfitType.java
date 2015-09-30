

package stratos.game.economic;

import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.sfx.ShieldFX;
import stratos.util.*;
import static stratos.game.economic.Economy.*;

import java.io.*;



public class OutfitType extends Traded {
  
  
  final public float
    defence,
    shieldBonus;
  final public Conversion materials;
  
  final public ImageAsset skin;
  
  
  public OutfitType(
    Class baseClass, String name,
    int defence, int shieldBonus, int basePrice,
    Class <? extends Venue> facility, Object... conversionArgs
  ) {
    super(baseClass, FORM_OUTFIT, name, basePrice, null);
    this.defence     = defence    ;
    this.shieldBonus = shieldBonus;
    
    if (Visit.empty(conversionArgs)) {
      this.materials = NATURAL_MATERIALS;
    }
    else this.materials = new Conversion(
      facility, name+"_manufacture",
      Visit.compose(Object.class, conversionArgs, new Object[] { TO, 1, this })
    );
    setPrice(basePrice, materials);
    
    final String imagePath = ITEM_PATH+name+"_skin.gif";
    if (new File(imagePath).exists()) {
      this.skin = ImageAsset.fromImage(baseClass, imagePath);
    }
    else this.skin = null;
  }
}







