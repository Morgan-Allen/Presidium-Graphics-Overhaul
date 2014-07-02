/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.campaign ;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.civilian.*;
import stratos.game.common.*;
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


//  TODO:  Also, houses & factions will have to be made into separate entities,
//         so that their degree of ownership/influence can change over time.



public class Sector extends Background {

  
  final public static Object
    MAKES = new Object(),
    NEEDS = new Object();
  
  
  final public String houseName ;
  final public String description ;
  final String imagePath ;
  final Vec2D starCoords ;
  
  final Service goodsMade[], goodsNeeded[] ;
  final public Trait climate ;
  final public int gravity ;
  
  
  
  public Sector(
    String name, String houseName, String description,
    String imgName, float starX, float starY,
    Trait climate, int gravity, Object... args
  ) {
    super(name, null, null, -1, Backgrounds.NOT_A_GUILD, args) ;
    this.houseName = houseName ;
    this.description = description ;
    this.imagePath = imgName;
    this.starCoords = new Vec2D(starX, starY) ;
    
    this.climate = climate ;
    this.gravity = gravity ;
    
    final Batch <Service> madeB = new Batch(), needB = new Batch() ;
    Object tag = null ;
    for (Object arg : args) {
      if (arg == MAKES || arg == NEEDS) tag = arg ;
      if (arg instanceof Service) {
        if (tag == MAKES) madeB.add((Service) arg) ;
        if (tag == NEEDS) needB.add((Service) arg) ;
      }
    }
    
    goodsMade   = madeB.toArray(Service.class) ;
    goodsNeeded = needB.toArray(Service.class) ;
  }
}










