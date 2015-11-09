/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.economic;
import stratos.content.civic.*;
import stratos.content.wip.*;
import stratos.graphics.common.AnimNames;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



public final class Outfits {
  
  final static Class BC = Outfits.class;
  
  final public static Traded
    POWER_CELLS = new Traded(
      BC, "Power Cells", null, FORM_MATERIAL, 4,
      "Spare power to maintain shields."
    );
  
  //  TODO:  You should have skins associated with some of these...
  final public static OutfitType
    
    INTRINSIC_ARMOUR = new OutfitType(
      BC, "Intrinsic Armour", 0, 0, 0,
      null
    ),
    INTRINSIC_SHIELDING = new OutfitType(
      BC, "Intrinsic Shielding", 0, 10, 0,
      null
    ),
    
    SCRAP_GEAR = new OutfitType(
      BC, "Scrap Gear", 3, 0, 5,
      null, 0, HANDICRAFTS
    ),
    OVERALLS       = new OutfitType(
      BC, "Overalls"      , 1, 0, 50,
      EngineerStation.class, 1, PLASTICS, 5, ASSEMBLY
    ),
    FINERY         = new OutfitType(
      BC, "Finery"        , 1, 0 , 400,
      Fabricator.class, 2, PLASTICS, 15, GRAPHIC_DESIGN
    ),
    
    SEALSUIT       = new OutfitType(
      BC, "Sealsuit"      , 4, 1, 150,
      EngineerStation.class, 1, PLASTICS, 1, PARTS, 10, HANDICRAFTS
    ),
    STEALTH_SUIT   = new OutfitType(
      BC, "Stealth Suit"  , 8, 5, 250,
      EngineerStation.class, 1, PLASTICS, 2, PARTS, 15, HANDICRAFTS
    ),
    
    LIFTER_FRAME    = new OutfitType(
      BC, "Lifter Frame", 7, 0, 35,
      EngineerStation.class, 2, PARTS, 10, ASSEMBLY
    ),
    POWER_LIFTER    = new OutfitType(
      BC, "Power Lifter", 12, 5, 75,
      EngineerStation.class, 3, PARTS, 20, ASSEMBLY
    ),
    
    BELT_AND_BRACER = new OutfitType(
      BC, "Belt and Bracer"   , 5, 10, 50,
      EngineerStation.class, 1, PARTS, 5, ASSEMBLY
    ),
    BODY_ARMOUR    = new OutfitType(
      BC, "Body Armour"   , 15, 10, 150,
      EngineerStation.class, 2, PARTS, 15, ASSEMBLY
    ),
    POWER_ARMOUR   = new OutfitType(
      BC, "Power Armour"  , 20, 10, 275,
      EngineerStation.class, 4, PARTS, 20, ASSEMBLY
    );
  final public static Traded
    ALL_OUTFITS[] = Traded.INDEX.soFar(Traded.class);
  
}




