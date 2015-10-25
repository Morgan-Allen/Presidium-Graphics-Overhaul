/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.economic;
import stratos.content.civic.*;
import stratos.graphics.common.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



public final class Devices {
  
  final static Class BC = Devices.class;
  final public static int
    NONE     = 0,
    //
    //  These are properties of equipped weapons-
    MELEE    = 1 << 0,
    RANGED   = 1 << 1,
    ENERGY   = 1 << 2,
    KINETIC  = 1 << 3,
    STUN     = 1 << 4,
    POISON   = 1 << 5,
    HOMING   = 1 << 6,
    BURNER   = 1 << 7,
    //
    //  These are properties of natural weapons or armour-
    GRAPPLE      = 1 << 8,
    CAUSTIC      = 1 << 9,
    TRANSMORPHIC = 1 << 10,
    ENERGY_DRAIN = 1 << 12;
  
  
  final public static Traded
    AMMO_CLIPS = new Traded(
      BC, "Ammo Clips", null, FORM_SPECIAL, 4,
      "Spare ammunition for weapons."
    );
  
  final public static DeviceType
    STUN_WAND = new DeviceType(
      BC, "Stun Wand",
      "pistol", AnimNames.FIRE,
      6, RANGED | ENERGY | STUN | MELEE, 35,
      EngineerStation.class, 1, PARTS, 10, ASSEMBLY
    ),
    CARBINE = new DeviceType(
      BC, "Carbine",
      "pistol", AnimNames.FIRE,
      8, RANGED | KINETIC, 30,
      EngineerStation.class, 1, PARTS, 10, ASSEMBLY
    ),
    BLASTER = new DeviceType(
      BC, "Blaster",
      "pistol", AnimNames.FIRE,
      10, RANGED | ENERGY | BURNER, 25,
      EngineerStation.class, 1, PARTS, 10, ASSEMBLY
    ),
    HALBERD_GUN = new DeviceType(
      BC, "Halberd Gun",
      "pistol", AnimNames.FIRE,
      13, RANGED | MELEE | KINETIC, 40,
      EngineerStation.class, 1, PARTS, 10, ASSEMBLY
    ),
    
    HUNTING_LANCE = new DeviceType(
      BC, "Hunting Lance",
      "spear", AnimNames.STRIKE,
      10, RANGED | KINETIC, 5,
      null, 5, HANDICRAFTS
    ),
    SIDE_SABRE = new DeviceType(
      BC, "Side Sabre",
      "light blade", AnimNames.STRIKE,
      8, MELEE | KINETIC, 10,
      EngineerStation.class, 1, PARTS, 5, ASSEMBLY
    ),
    ZWEIHANDER = new DeviceType(
      BC, "Zweihander",
      "heavy blade", AnimNames.STRIKE_BIG,
      18, MELEE | KINETIC, 35,
      EngineerStation.class, 1, PARTS, 10, ASSEMBLY
    ),
    ARC_SABRE = new DeviceType(
      BC, "Arc Sabre",
      "sabre", AnimNames.STRIKE,
      24, MELEE | ENERGY, 100,
      EngineerStation.class, 3, PARTS, 15, ASSEMBLY
    ),
    
    LIMB_AND_MAW = new DeviceType(
      BC, "Limb and Maw",
      null, AnimNames.STRIKE,
      0, MELEE | KINETIC, 0,
      null
    ),
    INTRINSIC_BEAM = new DeviceType(
      BC, "Intrinsic Beam",
      null, AnimNames.FIRE,
      0, RANGED | ENERGY, 0,
      null
    ),
    
    MANIPULATOR = new DeviceType(
      BC, "Manipulator",
      "maniples", AnimNames.STRIKE,
      5, MELEE | KINETIC, 10,
      EngineerStation.class, 1, PARTS, 5, ASSEMBLY
    ),
    MODUS_LUTE = new DeviceType(
      BC, "Modus Lute",
      "modus lute", AnimNames.TALK_LONG,
      0, NONE, 40,
      EngineerStation.class, 1, PARTS, 10, ASSEMBLY
    ),
    BIOCORDER = new DeviceType(
      BC, "Biocorder",
      "biocorder", AnimNames.LOOK,
      0, NONE, 55,
      EngineerStation.class, 2, PARTS, 15, ASSEMBLY
    );
  final public static Traded
    ALL_DEVICES[] = Traded.INDEX.soFar(Traded.class);
}




