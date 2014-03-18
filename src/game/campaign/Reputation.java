


package src.game.campaign ;
import src.game.common.* ;



public class Reputation {
  
  //
  //  Overall ratings for your current settlement and policies-
  float
    knowledge, tradition,
    wealth, diversions,
    planet, security ;
  //
  //  Overall ratings for your past dealings, history and conduct-
  float
    integrity, autonomy,
    compassion, interest,
    courage, patience ;
  //
  //  Taken together, these impact the attitude of the various schools toward
  //  you, how likely they are to support or attack you, ease of recruitment,
  //  etc...
  //    The Logicians value Knowledge and Integrity.
  //    The Collective value Security and Compassion.
  //    The Symbiont value Planet and Courage.
  //    The Shapers value Diversions and Autonomy.
  //    The Spacers value Wealth and (Self) Interest.
  //    The Initiates value Tradition and Patience.
  //
  //  Schools which are on good terms with eachother will also assign some
  //  value to the metrics of their allies (e.g, the Logicians share the values
  //  of the Initiates and Collective, just to a lesser degree.  Same with the
  //  Shapers with respect to the Spacers and Symbiont, and so on.  The above
  //  listing lists each School next to their allies, in a cyclic order.)
  //  
  //  Similar priorities most likely apply to general social strata-
  //    The aristocracy value Tradition and Diversion.
  //    The merchant and artisan classes value Wealth and Knowledge.
  //    The underclasses value Planet and Security.
  //
  //  (Although these can't be directly recruited, the alien Preserves also have
  //  certain policy preferences.)
  //    The Changelings value Diversions and Planet.
  //    The Krech value Security and Wealth.
  //    The Jovians value Knowledge and Tradition.
  
  
  float rateKnowledge(Base base) {
    float rating = 0 ;
    //  Bonus for area of map uncovered.
    //  Bonus for citizens' average level of intellect-based skills.
    //  Bonus for access to: Senate Chamber, Archives, and Minder's Ward.
    //  Bonus for prevalence of Inquisitive, Impassive and Supercognitive
    //  traits.
    return rating ;
  }
  
  
  float rateSecurity(Base base) {
    float rating = 0 ;
    //  Bonus for low levels of crime or external raiding (i.e, danger levels
    //  on map.)
    //  Bonus for low levels of disease, malnutrition or dissaffection.
    //  Bonus for access to:  Detention Bloc, Sickbay and Town Vault.
    //  Bonus for prevalance of Empathic, Pacifist and Melded traits.
    return rating ;
  }
  
  
  float ratePlanet(Base base) {
    float rating = 0 ;
    //  Bonus for area of map left totally unbuilt on (i.e, wild, with a
    //  partial bonus around Flesh Stills, Plantations and Arcology.)
    //  Bonus for low levels of squalor and strong access to Life Support.
    //  Bonus for low reliance on foreign trade (especially offworld.)
    //  Bonus for diversity of animal species.
    return rating ;
  }
  
  
  float rateDiversions(Base base) {
    float rating = 0 ;
    //  Bonus for high levels of aesthetic ambience and citizen morale.
    //  Bonus for access to Arena, Cantina and Training House.
    return rating ;
  }
  
  
  float rateWealth(Base base) {
    float rating = 0 ;
    //  Bonus for average value of citizens' property assets.
    //  Bonus for total revenue based on commerce and taxation.
    return rating ;
  }
  
  
  float rateTradition(Base base) {
    float rating = 0 ;
    //  Bonus for policies that favour minimal technology and class system.
    //  Bonus for loyalty to House Procyon and The Empress.
    return rating ;
  }
  
  
  //
  //  How do I measure personal values?
  //  Courage-
  //    Undertake missions in dangerous areas, especially in person.
  //    Be willing to take risks and act on impulse.
  //  Patience-
  //    Defer short-term gratification for the sake of long-term rewards.
  //  Compassion-
  //    Employ powers, missions and facilities that aid or comfort others.
  //    Avoid the use of violence or coercion.
  //  Interest-
  //    Extract favours from allies and superiors, tribute from foes.
  //  Integrity-
  //    Follow through on promised services, don't break treaties.
  //    Honour requests by superiors and formal allies.
  //  Autonomy-
  //    Disavow old commitments or explore the novel and unknown.
}












