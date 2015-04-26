/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.civic;
import stratos.game.actors.Choice;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.util.*;



public abstract class School extends Venue {
  
  
  
  protected School(Blueprint profile, Base base) {
    super(profile, base);
  }
  
  
  public School(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  //
  //  All Schools have a few basic functions in common.  They are, first of all,
  //  places of basic education and spiritual instruction, in a manner similar
  //  to the monasteries of the orient and medieval period.
  
  //  Secondly, they are places of psyonic training for advanced adepts, and as
  //  such can be called upon to supplement military or diplomatic missions.
  
  
  protected void addServices(Choice choice, Actor forActor) {
    
    //
    //  TODO:  Offer training under supervision in the skill and Techniques
    //  appropriate to the school of thought in question.
    
    //  Basic training is available to the public, but advanced training only
    //  to members of the school OR to the Sovereign.
    
    //  Premonition-boosts for Logicians, plus general science research.
    //  Artilect taming and construction for Tek Priestesses.
    //  Charity and thought-sync sessions for the Collective.
    //  Ship-construction and accompaniment for Navigators.
    //  Animal-taming and body-modification for Jil Baru.
    //  Body-armour upgrades and device-assimilation for KOTSF.
    
    super.addServices(choice, forActor);
  }
  
  
  
}










