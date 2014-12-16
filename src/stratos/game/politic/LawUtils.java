


package stratos.game.politic;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.plans.*;
import stratos.util.*;



public class LawUtils {
  
  
  //  TODO:  I'm only going to implement about half of these for now- the rest
  //  are intended for expanded content.
  public static enum Crime {
    CRIME_THEFT       ,
    CRIME_CORRUPTION  ,
    CRIME_ASSAULT     ,
    CRIME_DESERTION   ,
    //  TODO:  Include vice, false evidence, gene-crime and tek-crime.
  };
  public static enum Sentence {
    SENTENCE_CENSURE  ,
    SENTENCE_CAPTIVITY,
    SENTENCE_BEATING  ,
    SENTENCE_DEMOTION ,
    //  TODO:  Include rehab, arena combat, penal labour, and execution.
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
    2, 3, 4, 5
  };
  final static float SENTENCE_SEVERITIES[] = {
    1, 2, 3, 4
  };
  
  
  final static List <Crime> NO_CRIMES = new List <Crime> ();
  
  
  public static float severity(Crime crime) {
    return OFFENCE_SEVERITIES[crime.ordinal()];
  }
  
  
  public static Crime crimeDoneBy(Actor actor, Base base) {
    final Behaviour doing = actor.mind.topBehaviour();
    final Target victim = actor.planFocus(null);
    float harmRating = actor.harmIntended(victim);
    harmRating *= base.relations.relationWith(victim.base());
    //
    //  Theft and assault are relatively easy to spot-
    if (harmRating > 0) {
      if (doing instanceof Combat ) return Crime.CRIME_ASSAULT;
      if (doing instanceof Looting) return Crime.CRIME_THEFT  ;
    }
    //  Corruption means embezzlement, tax evasion or bribery.
    if (doing instanceof Audit && ! ((Audit) doing).honest()) {
      return Crime.CRIME_CORRUPTION;
    }
    //  Desertion means retreating from a mission or defecting to another base.
    final Mission mission = actor.mind.mission();
    if (doing instanceof Retreat && mission != null) {
      return Crime.CRIME_DESERTION ;
    }
    return null;
  }
}








