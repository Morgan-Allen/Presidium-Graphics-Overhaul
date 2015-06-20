/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.actors;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.start.Scenario;
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
  private static boolean
    verbose = false;
  
  
  protected HumanMind(Actor actor) {
    super(actor);
  }
  
  
  public void loadState(Session s) throws Exception {
    super.loadState(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
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
  
  protected Choice createNewBehaviours(Choice choice) {
    if (choice == null) choice = new Choice(actor);
    addConstantResponses(choice);
    addVenueResponses(choice);
    return choice;
  }
  
  
  protected void addReactions(Target seen, Choice choice) {
    final boolean report = verbose && I.talkAbout == actor;
    if (seen instanceof Actor) {
      if (report) I.say("  Have seen other actor: "+seen);
      
      final Actor nearby = (Actor) seen;
      choice.add(new Combat  (actor, nearby));
      choice.add(new FirstAid(actor, nearby));
      choice.add(new Arrest  (actor, nearby));
      choice.add(new Dialogue(actor, nearby));
    }
    if (seen instanceof Item.Dropped) {
      choice.add(new Looting(actor, (Item.Dropped) seen));
    }
  }
  
  
  protected void putEmergencyResponse(Choice choice) {
    choice.add(new Retreat(actor));
  }
  
  
  private void addConstantResponses(Choice choice) {
    final boolean report = verbose && I.talkAbout == actor;
    if (report) I.say("\nGetting constant responses.");
    choice.isVerbose = report;
    
    //  TODO:  You need to respond to more distant actors/venues/items here?
    for (Target e : actor.senses.awareOf()) {
      addReactions(e, choice);
      if (e instanceof Actor) {
        if (report) I.say("  Responding to actor: "+e);
        final Actor nearby = (Actor) e;
        choice.add(Hunting.asHarvest(actor, nearby, home, true));
        
        //  TODO:  Reserve this for people you know.
        //choice.add(Gifting.nextGifting(null, actor, nearby));
      }
    }
    
    choice.add(Exploring.nextExploration(actor));
    choice.add(Exploring.nextWandering  (actor));
    choice.add(new Foraging(actor, null));
    choice.add(new Retreat(actor));
    
    if (work != null) {
      if (report) I.say("  Work is "+work);
      work.addTasks(choice, actor, actor.mind.vocation());
      choice.add(new Payday(actor, work));
    }
    if (home != null) {
      if (report) I.say("  Home is "+home);
      if (home != null) home.addTasks(choice, actor, Backgrounds.AS_RESIDENT);
      choice.add(new Resting(actor, home));
    }
    else {
      final Target restsAt = actor.senses.haven();
      if (restsAt != null) choice.add(new Resting(actor, restsAt));
    }

    choice.add(Scenario.current().taskFor(actor));
    choice.add(JoinMission.attemptFor(actor));
  }
  
  
  private void addVenueResponses(Choice choice) {
    final boolean report = I.talkAbout == actor && verbose;
    if (report) I.say("\nAdding venue responses...");
    
    final Stage world = actor.world();
    final Batch <Venue> around = new Batch <Venue> ();
    float numSampled = 5 + (actor.traits.traitLevel(COGNITION) / 4);
    
    world.presences.sampleFromMaps(
      actor, world, (int) numSampled, around, Venue.class
    );
    
    final boolean timeoff = work == null || ! work.staff().onShift(actor);
    for (Venue venue : around) {
      if (venue.structure().intact()) {
        if (report) I.say("  Getting services from "+venue);
        
        venue.addTasks(choice, actor, Backgrounds.AS_VISITOR);
        choice.add(FindWork.attemptFor(actor, venue));
        choice.add(FindHome.attemptFor(actor, venue));
      }
    }
    if (timeoff) {
      choice.add(Repairs.getNextRepairFor(actor, false, 0));
      choice.add(BringUtils.nextDisposalFor(actor));
    }
  }
  
  
  public void updateAI(int numUpdates) {
    super.updateAI(numUpdates);
  }
}










