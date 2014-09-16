

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
}














