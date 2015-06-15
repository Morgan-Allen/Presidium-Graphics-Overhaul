/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.economic;
import stratos.game.civic.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;


public final class Outfits {
  
  
  //  TODO:  You should have skins associated with some of these...
  final public static OutfitType
    
    OVERALLS       = new OutfitType(
      BC, "Overalls"      , 1, 0, 50,
      EngineerStation.class, 1, PLASTICS, 5, ASSEMBLY
    ),
    FINERY         = new OutfitType(
      BC, "Finery"        , 1, 0 , 400,
      Fabricator.class, 2, PLASTICS, 15, GRAPHIC_DESIGN
    ),
    SCRAP_GEAR = new OutfitType(
      BC, "Scrap Gear", 3, 0, 5,
      null, 0, HANDICRAFTS
    ),
    INTRINSIC_ARMOUR = new OutfitType(
      BC, "Natural Armour", 0, 0, 0,
      null
    ),
    
    SEALSUIT       = new OutfitType(
      BC, "Sealsuit"      , 4, 1, 150,
      EngineerStation.class, 1, PLASTICS, 1, PARTS, 10, HANDICRAFTS
    ),
    STEALTH_SUIT   = new OutfitType(
      BC, "Stealth Suit"  , 8, 5, 250,
      EngineerStation.class, 1, PLASTICS, 2, PARTS, 15, HANDICRAFTS
    ),
    BELT_AND_BRACER = new OutfitType(
      BC, "Belt and Bracer"   , 5, 10, 50,
      EngineerStation.class, 1, PARTS, 5, ASSEMBLY
    ),
    BODY_ARMOUR    = new OutfitType(
      BC, "Body Armour"   , 15, 10, 150,
      EngineerStation.class, 5, PARTS, 15, ASSEMBLY
    ),
    POWER_ARMOUR   = new OutfitType(
      BC, "Power Armour"  , 20, 10, 275,
      EngineerStation.class, 8, PARTS, 20, ASSEMBLY
    ),
    GOLEM_FRAME = new OutfitType(
      BC, "Golem Frame"  , 25, 10, 500,
      EngineerStation.class, 12, PARTS, 25, ASSEMBLY
    );
  final public static Traded
    ALL_OUTFITS[] = Traded.INDEX.soFar(Traded.class);
  
}
