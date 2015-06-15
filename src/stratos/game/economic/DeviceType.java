

package stratos.game.economic;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.sfx.*;
import stratos.graphics.solids.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;



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



public class DeviceType extends Traded {
  
  
  final public float baseDamage;
  final public int properties;
  final Conversion materials;
  
  final public String groupName, animName;
  
  
  DeviceType(
    Class baseClass, String name,
    String groupName, String animName,
    float baseDamage, int properties, int basePrice,
    Class <? extends Venue> facility, Object... conversionArgs
  ) {
    super(baseClass, Economy.FORM_DEVICE, name, basePrice, null);
    
    this.baseDamage = baseDamage;
    this.properties = properties;
    
    if (Visit.empty(conversionArgs)) {
      this.materials = NATURAL_MATERIALS;
    }
    else this.materials = new Conversion(
      facility, name+"_manufacture", Visit.compose(
        Object.class, conversionArgs, new Object[] { TO, 1, this }
      )
    );
    setPrice(basePrice, materials);
    
    this.groupName = groupName;
    this.animName = animName;
  }
  
  
  public boolean hasProperty(int p) {
    return (properties & p) == p;
  }
}














