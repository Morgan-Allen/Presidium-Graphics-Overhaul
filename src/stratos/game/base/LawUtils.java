/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.plans.*;
import stratos.game.verse.Faction;
import stratos.util.*;



public class LawUtils {
  
  
  //  TODO:  I'm only going to implement about half of these for now- the rest
  //  are intended for expanded content.
  public static enum Crime {
    CRIME_THEFT       ,
    CRIME_CORRUPTION  ,
    CRIME_ASSAULT     ,
    CRIME_DESERTION   ;
    //  TODO:  Include vice, false evidence, gene-crime and tek-crime.
    public String description() { return OFFENCE_DESCRIPTIONS[ordinal()]; }
    public float  severity   () { return OFFENCE_SEVERITIES  [ordinal()]; }
  };
  public static enum Sentence {
    SENTENCE_CENSURE  ,
    SENTENCE_CAPTIVITY,
    SENTENCE_BEATING  ,
    SENTENCE_DEMOTION ;
    //  TODO:  Include rehab, arena combat, penal labour, and execution.
    public String description() { return SENTENCE_DESCRIPTIONS[ordinal()]; }
    public float  severity   () { return SENTENCE_SEVERITIES  [ordinal()]; }
  }
  final static String OFFENCE_DESCRIPTIONS[] = {
    "Theft and Smuggling",
    "Corruption"         ,
    "Assault"            ,
    "Desertion of Duty"  ,
  };
  final static String SENTENCE_DESCRIPTIONS[] = {
    "Censure"            ,
    "Captivity"          ,
    "Beating"            ,
    "Demotion"           ,
  };
  //  TODO:  Unify these into single object declarations.
  final static float OFFENCE_SEVERITIES[] = {
    2.5f, 5, 7.5f, 10
  };
  final static float SENTENCE_SEVERITIES[] = {
    1, 2, 3, 4
  };
  final static List <Crime> NO_CRIMES = new List <Crime> ();
  
  
  public static Crime crimeDoneBy(Actor actor, Actor arrests) {
    
    if (actor.mind.work() == arrests.mind.work()) return null;
    
    final Behaviour
      root = actor.mind.rootBehaviour(),
      top  = actor.mind.topBehaviour ();
    //
    //  For now, we prevent any mission-activities from being registered as
    //  'crimes'...
    if (Plan.hasMotive(root, Plan.MOTIVE_MISSION)) return null;
    final Target victim = actor.planFocus(null, true);
    
    float harmRating = 0;
    final Action a = arrests.currentAction();
    
    if (victim != null) {
      harmRating += Plan.harmIntended(actor, victim) * 2;
      harmRating *= Faction.factionRelation(arrests, victim);
    }
    //
    //  Theft and assault are relatively easy to spot-
    if (harmRating > 0.5f) {
      if (root instanceof Arrest ) return null;
      if (top  instanceof Combat ) return Crime.CRIME_ASSAULT;
      if (top  instanceof Looting) return Crime.CRIME_THEFT  ;
    }
    //  Corruption means embezzlement, tax evasion or bribery.
    if (Audit.checkForEmbezzlement(root, arrests, true, a)) {
      return Crime.CRIME_CORRUPTION;
    }
    //  Desertion means retreating from a mission or defecting to another base.
    
    //  TODO:  Reserve this only for 
    /*
    final Mission mission = actor.mind.mission();
    if (root instanceof Retreat && mission != null) {
      return Crime.CRIME_DESERTION ;
    }
    //*/
    return null;
  }
}



