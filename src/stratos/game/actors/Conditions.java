

package stratos.game.actors;
import stratos.util.*;

import static stratos.game.actors.Qualities.*;



public final class Conditions {
  
  private Conditions() {}
  
  
  final public static int
    
    //  Number of days required for full development.
    NO_LATENCY     = 0,
    SHORT_LATENCY  = 1,
    MEDIUM_LATENCY = 10,
    LONG_LATENCY   = 100,
    
    //  Average number of other people infected.
    NO_SPREAD    = 0,
    SLOW_SPREAD  = 2,
    RAPID_SPREAD = 5,
    
    //  Intrinsic difficulty to treat or recover from.
    NO_VIRULENCE        = 0,
    MINIMAL_VIRULENCE   = 5,
    LOW_VIRULENCE       = 10,
    AVERAGE_VIRULENCE   = 15,
    HIGH_VIRULENCE      = 20,
    EXTREME_VIRULENCE   = 25;
  
  final public static Condition
    //
    //  Finally, listings for various conditions that might beset the actor-
    INJURY = new Condition(
      "Injury", true,
      "Critical Injury",
      "Serious Injury",
      "Modest Injury",
      "Slight Injury",
      null
    ),
    FATIGUE = new Condition(
      "Fatigue", true,
      "Extreme Fatigue",
      "Heavy Fatigue",
      "Modest Fatigue",
      "Mild Fatigue",
      null
    ),
    POOR_MORALE = new Condition(
      "Morale", true,
      "Broken Morale",
      "Poor Morale",
      null,
      "Fair Morale",
      "Superb Morale"
    ),
    
    HUNGER = new Condition(
      "Hunger", true,
      "Near Starvation",
      "Hungry",
      "Peckish",
      null,
      "Full"
    ),
    MALNOURISHMENT = new Condition(
      "Malnourishment", true,
      "Malnourished",
      null
    ),
    POISONING = new Condition(
      "Poisoning",
      SHORT_LATENCY, LOW_VIRULENCE, NO_SPREAD, Table.make(
        IMMUNE, -15, MUSCULAR, -15
      ),
      "Severe Poisoning",
      "Bad Poisoning",
      "Mild Poisoning",
      null,
      "Poison Immune"
    ),
    
    SOMA_HAZE = new Condition(
      "Soma Haze",
      NO_LATENCY, MINIMAL_VIRULENCE, NO_SPREAD, Table.make(
        MOTOR, -3, COGNITION, -2, PERCEPT, 1
      ),
      "Soma Haze",
      "Soma Haze",
      "Soma Haze",
      null,
      "Haze Immune"
    ),
    ILLNESS = new Condition(
      "Illness",
      SHORT_LATENCY, LOW_VIRULENCE, RAPID_SPREAD, Table.make(
        IMMUNE, -5, MUSCULAR, -5
      ),
      "Debilitating Illness",
      "Serious Illness",
      "Mild Illness",
       null,
       "Illness Immune"
    ),
    SPICE_ADDICTION = new Condition(
      "Spice Addiction",
      LONG_LATENCY, AVERAGE_VIRULENCE, NO_SPREAD, Table.make(
        IMMUNE, -10, PERCEPT, -5, NERVE, -5, COGNITION, -5
      ),
      "Complete Spice Addiction",
      "Heavy Spice Addiction",
      "Mild Spice Addiction",
      null,
      "Addiction Immune"
    ),
    CANCER = new Condition(
      "Cancer",
      LONG_LATENCY, AVERAGE_VIRULENCE, NO_SPREAD, Table.make(
        IMMUNE, -20, MUSCULAR, -10
      ),
      "Terminal Cancer",
      "Advanced Cancer",
      "Early Cancer",
      null,
      "Cancer Immune"
    ),
    RAGE_INFECTION = new Condition(
      "Rage Infection",
      SHORT_LATENCY, HIGH_VIRULENCE, RAPID_SPREAD, Table.make(
        IMMUNE, 5, MUSCULAR, 5, DEFENSIVE, 5, COGNITION, -15
      ),
      "Rage Frenzy",
      "Rage Fever",
      "Rage Onset",
      null,
      "Rage Immune"
    ),
    HIREX_PARASITE = new Condition(
      "Hirex Parasite",
      MEDIUM_LATENCY, HIGH_VIRULENCE, SLOW_SPREAD, Table.make(
        COGNITION, -5, MOTOR, -5, PERCEPT, -5, MUSCULAR, -5, HANDSOME, -5
      ),
      "Hirex Consumption",
      "Hirex Infestation",
      "Hirex Gestation",
      null,
      "Hirex Immune"
    ),
    ALBEDAN_STRAIN = new Condition(
      "Albedan Strain",
      MEDIUM_LATENCY, EXTREME_VIRULENCE, SLOW_SPREAD, Table.make(
        INDULGENT, 2, IMMUNE, 5, PERCEPT, 5, MOTOR, -5
      ),
      "Albedan Strain",
      "Albedan Strain",
      "Albedan Strain",
      null,
      "Strain Immune"
    ),
    SILVERQUICK = new Condition(
      "Silverquick",
      SHORT_LATENCY, EXTREME_VIRULENCE, RAPID_SPREAD, Table.make(
        IMPASSIVE, 5, IMMUNE, -20, MUSCULAR, -20
      ),
      "Silverquick Rictus",
      "Silverquick Scale",
      "Silverquick Taint",
      null,
      "Silverquick Immune"
    ),
    MOEBIUS_PLAGUE = new Condition(
      "Moebius Plague",
      LONG_LATENCY, EXTREME_VIRULENCE, NO_SPREAD, Table.make(
        MOTOR, -20, MUSCULAR, -20
      ),
      "Moebius Sublimation",
      "Moebius Displacement",
      "Moebius Plague",
      null,
      "Plague Immune"
    ),
    
    SPONTANEOUS_DISEASE[] = {
      ILLNESS, CANCER, HIREX_PARASITE
    },
    ALL_CONDITIONS[] = {
      POISONING, ILLNESS, SPICE_ADDICTION,
      CANCER, RAGE_INFECTION, HIREX_PARASITE,
      ALBEDAN_STRAIN, SILVERQUICK, MOEBIUS_PLAGUE
    };
}




