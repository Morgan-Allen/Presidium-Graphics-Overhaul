/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.campaign;
import stratos.game.actors.*;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.graphics.common.*;
import stratos.util.*;



//  TODO:  Replace with SectorState- so that you can trace the development of a
//  given sector over time.  (This then becomes the Background, not the sector
//  itself.)

//  Sector contains constants such as name, gravity, orbital period, 
//  relative distance from other Sectors, and default levels of moisture,
//  minerals and insolation (which might be modified by terraforming.)

//  SectorState measures biomass/terraform progress, population, wealth,
//  danger, squalor, economic output, political interests and culture.  You use
//  that to track development and knock-on effects over time.

//  Housing.     (population)
//  Resources.   (climate)
//  Philosophy.  (interests)
//  Time & Space.
//  Danger & Squalor.
//  Equality & Autonomy.

//  (Also, houses & factions will have to be made into separate entities, so
//  that their degree of ownership/influence can change over time.)



public class Sector extends Background {

  
  final public static Object
    MAKES = new Object(),
    NEEDS = new Object();
  
  
  final public String houseName;
  final Traded goodsMade[], goodsNeeded[];
  final public Trait climate;
  final public int gravity;
  
  //  TODO:  Include information about distances here, loaded from XML.
  
  
  public Sector(
    Class baseClass,
    String name, String houseName, String description,
    Trait climate, int gravity, Object... args
  ) {
    super(
      baseClass, name, description, null, null,
      -1, Backgrounds.NOT_A_GUILD, args
    );
    this.houseName = houseName;
    
    this.climate = climate;
    this.gravity = gravity;
    
    final Batch <Traded> madeB = new Batch(), needB = new Batch();
    Object tag = null;
    for (Object arg : args) {
      if (arg == MAKES || arg == NEEDS) tag = arg;
      if (arg instanceof Traded) {
        if (tag == MAKES) madeB.add((Traded) arg);
        if (tag == NEEDS) needB.add((Traded) arg);
      }
    }
    
    goodsMade   = madeB.toArray(Traded.class);
    goodsNeeded = needB.toArray(Traded.class);
  }
  
  
  
  public static Sector sectorNamed(String name) {
    for (Sector s : Sectors.ALL_SECTORS) if (s.name.equals(name)) return s;
    return null;
  }
}


