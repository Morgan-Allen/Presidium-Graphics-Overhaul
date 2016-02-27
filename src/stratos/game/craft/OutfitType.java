/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.craft;
import stratos.game.common.*;
import stratos.game.plans.CombatUtils;
import stratos.graphics.common.*;
import stratos.util.*;

import static stratos.game.craft.Economy.*;

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

    //  TODO:  Use this method for source-registration.
    //this.addSource(facility);
    setPriceMargin(basePrice, materials);
    
    final String imagePath = ITEM_PATH+name+"_skin.gif";
    if (new File(imagePath).exists()) {
      this.skin = ImageAsset.fromImage(baseClass, name+"_skin", imagePath);
    }
    else this.skin = null;
  }
  
  
  public float useRating(Actor a) {
    if (! a.gear.canDemand(this)) return 0;
    return defence / CombatUtils.AVG_DAMAGE;
  }
}








