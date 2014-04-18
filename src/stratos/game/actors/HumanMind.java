/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.actors ;
import stratos.game.building.*;
import stratos.game.campaign.*;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.game.planet.*;
import stratos.game.tactical.*;
import stratos.user.*;
import stratos.util.*;






//OUTLINE FOR DECISION-MAKING:
/*


Structures volunteer their services to nearby actors who care to sample them.
It's simple, it's efficient, and it's compact.

Then, there are instinctive reactions to local actors, structures & items,
terrain and conditions-
  *  Flee/surrender
  *  Salvage/repairs/relocation
  *  Feeding/resting
  *  Finding home/finding work/migrating
  *  Pick up/First aid (Recover)
  *  Attack/destroy/capture  (Strike)
  *  Converse/carnal/assist  (Contact/Insertion)
  *  Wandering/exploring (Security/Recon)

Then there are tasks derived from home and work (if distinct.)

Finally, there are missions formally declared by the player or scenario, which
issue certain tasks and vet others, and can extend to foreign kingdoms.


*  Is there a pressing, life-threatening emergency?
 Running from an enemy, or raising an alarm.
 Treating/sheltering/defending someone injured or attacked.
 Getting food and sleep.

*  Have you been assigned or embarked on a mission?
 (Embarking on said missions, or accepting the rewards involved.)
 (Might be specified by player, or started spontaneously.)
 Strike Mission.
 Security Mission.
 Recovery Mission.
 Recon Mission.
 Contact Mission.
 Covert Mission.
 Accepting a promotion/ceremonial honours/license to marry.

*  Do you have work assigned by your employer?
 (Derived from home or work venues.)
 Seeding & Harvest.
 Excavation or Drilling.
 Hunting.
 Transport.
 Manufacture.
 Construction & Salvage.
 Patrolling/Enforcement.
 Treatment & Sick Leave.
 Entertainment.

*  Do you have spare time?
 (Procreation, Relaxation, Self-improvement, Helping out.)
 Relaxation/conversation/sex in public, at home, or at the Cantina.
 Matches/debates/spectation at the Arena or Senate Chamber.
 Learning new skills through apprenticeship or research at the Archives.
 Purchasing new equipment and home items.
//*/

//
//  TODO:  Have a 'fear' metric built in to determine retreat behaviour, that
//  can build up gradually over a few seconds?




public class HumanMind extends ActorMind implements Qualities {
  
  
  
  /**  Constructor and save/load functions-
    */
  private static boolean verbose = false ;
  
  //  private Background careerInterest ;
  //  private float lifeSatisfaction ;
  
  
  
  protected HumanMind(Actor actor) {
    super(actor) ;
  }
  
  
  protected void loadState(Session s) throws Exception {
    super.loadState(s) ;
  }
  
  
  protected void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  /*
  APPLY TO LOCAL ACTORS:
    Combat, Defence, Game Hunting           (Aggression)
    Dialogue & Accompaniment, Gifts & Aid   (Pro-Social)
    
  APPLY TO LOCAL OBJECTS & TERRAIN:
    Picking up & Foraging/Gathering          (Resources)
    Exploring/Patrolling/Wandering/Disguise  (Discovery)
  
  APPLY AT ALL TIMES:
    Dining, Rest & Sex/Child Play  (Enjoyment when Home)
    Retreat & Surrender            (Surviving when Away)
  
  APPLY TO LOCAL VENUES:
    <> Getting Tasks from Work/Home, Payment/Taxation
    <> Getting Services, Repair & Salvage
    <> Mission Application, Missions, Base & Scenario
    <> Finding Home/Work, Migration
  //*/
  
  protected Behaviour createBehaviour() {
    final Choice choice = new Choice(actor) ;
    
    addActorResponses(choice) ;
    addConstantResponses(choice) ;
    addVenueResponses(choice) ;
    addBaseResponses(choice) ;
    
    ///choice.isVerbose = I.talkAbout == actor;
    return choice.weightedPick() ;
  }
  
  
  protected void addReactions(Target seen, Choice choice) {
    if (seen instanceof Actor) if (seen != actor) {
      final Actor nearby = (Actor) seen ;
      ///I.say(actor+" adding reaction to: "+nearby);
      
      choice.add(Hunting.asHarvest(actor, nearby, home, true));
      
      choice.add(new Combat(actor, nearby)) ;
      choice.add(new Retreat(actor)) ;  //TODO:  MAKE MORE ECONOMICAL
      choice.add(new Dialogue(actor, nearby, Dialogue.TYPE_CASUAL)) ;
      choice.add(new FirstAid(actor, nearby)) ;
    }
  }
  
  
  private void addActorResponses(Choice choice) {
    for (Target e : actor.senses.awareOf()) addReactions(e, choice) ;
  }
  
  
  private void addConstantResponses(Choice choice) {
    
    choice.add(Exploring.nextExplorationFor(actor)) ;
    choice.add(Patrolling.wandering(actor)) ;
    choice.add(new Foraging(actor, null)) ;
    choice.add(new Retreat(actor)) ;

    final boolean timeoff = work == null || ! work.personnel().onShift(actor) ;
    if (work != null) {
      choice.add(work.jobFor(actor)) ;
      if (timeoff && work != home) work.addServices(choice, actor) ;
      choice.add(new Payday(actor, work)) ;
    }
    if (home != null) {
      if (home != work) choice.add(home.jobFor(actor)) ;
      if (timeoff) home.addServices(choice, actor) ;
      choice.add(new Resting(actor, home));
    }
    else {
      final Target haven = Retreat.nearestHaven(actor, null);
      choice.add(new Resting(actor, haven));
    }
  }
  
  
  //  TODO:  Move this to the reactions method?
  private void addVenueResponses(Choice choice) {
    final World world = actor.world() ;
    final Batch <Employer> around = new Batch <Employer> () ;
    float numSampled = 5 + (actor.traits.traitLevel(COGNITION) / 4) ;
    
    world.presences.sampleFromMap(
      actor, world, (int) numSampled, around, Venue.class
    ) ;
    final boolean timeoff = work == null || ! work.personnel().onShift(actor) ;
    
    for (Employer employs : around) {
      if (timeoff && employs.structure().intact()) {
        employs.addServices(choice, actor) ;
      }
      if (! (employs instanceof Venue)) continue ;
      choice.add(new Repairs(actor, (Venue) employs)) ;
    }
  }
  
  
  private void addBaseResponses(Choice choice) {
    //
    //  Derive tasks from missions or the scenario.
    choice.add(mission);
    choice.add(Scenario.current().taskFor(actor));
    //
    // Apply for missions, migration, work and home.
    choice.add(FindWork.attemptFor(actor));
    choice.add(FindHome.attemptFor(actor));
    //
    //  Finally, free-born actors may apply for missions.
    final int standing = actor.vocation().standing;
    if (standing >= Background.CLASS_FREEMEN) {
      choice.add(FindMission.attemptFor(actor));
      //choice.add(new Migration(actor)) ;
    }
  }
}



