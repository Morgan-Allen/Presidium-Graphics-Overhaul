/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.craft;
import stratos.game.common.*;

import static stratos.game.craft.Economy.*;

import stratos.game.actors.*;
import stratos.util.Visit;



public abstract class UsedItemType extends Traded {
  
  
  final Technique whenUsed[];
  final Conversion materials;
  
  
  public UsedItemType(
    Class typeClass,
    String name, String imgName,
    int basePrice, String description,
    Technique whenUsed,
    Class <? extends Venue> facility, Object... conversionArgs
  ) {
    super(typeClass, name, imgName, FORM_USED_ITEM, basePrice, description);
    this.whenUsed = new Technique[] { whenUsed };
    
    if (Visit.empty(conversionArgs)) {
      this.materials = NATURAL_MATERIALS;
    }
    else this.materials = new Conversion(
      facility, name+"_manufacture", Visit.compose(
        Object.class, conversionArgs, new Object[] { TO, 1, this }
      )
    );
    //  TODO:  Use this method for source-registration.
    //this.addSource(facility);
    setPriceMargin(basePrice, materials);
  }
  
  
  public abstract int normalCarry(Actor actor);
  public abstract float useRating(Actor uses);
  public Technique[] techniques() { return whenUsed; }
}






