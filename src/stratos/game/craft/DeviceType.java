/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.craft;
import static stratos.game.craft.Economy.*;

import stratos.game.common.*;
import stratos.game.plans.CombatUtils;
import stratos.graphics.common.*;
import stratos.graphics.sfx.*;
import stratos.graphics.solids.*;
import stratos.util.*;



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
    //  TODO:  Use this method for source-registration.
    //this.addSource(facility);
    setPriceMargin(basePrice, materials);
    
    this.groupName = groupName;
    this.animName  = animName ;
  }
  
  
  public boolean hasProperty(int p) {
    return (properties & p) == p;
  }
  
  
  public float useRating(Actor a) {
    if (! a.gear.canDemand(this)) return 0;
    return baseDamage / CombatUtils.AVG_DAMAGE;
  }
}














