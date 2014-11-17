/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.actors;
import stratos.game.building.*;
import stratos.game.campaign.*;
import stratos.game.common.*;
import stratos.game.plans.*;
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
  
  
  protected void loadState(Session s) throws Exception {
    super.loadState(s);
  }
  
  
  protected void saveState(Session s) throws Exception {
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
    addBaseResponses(choice);
    return choice;
  }
  
  
  protected void addReactions(Target seen, Choice choice) {
    final boolean report = verbose && I.talkAbout == actor;
    if (seen instanceof Actor) {
      if (report) I.say("  Have seen other actor: "+seen);
      
      final Actor nearby = (Actor) seen;
      choice.add(new Combat(actor, nearby));
      choice.add(new FirstAid(actor, nearby));

      //  TODO:  Add pleas & objections- specifically if the other actor is
      //  doing something objectionable, and you want them to stop.
      
      if (CombatUtils.isActiveHostile(actor, nearby)) {
        if (report) I.say("  Pleading with them to stop!");
        choice.add(new Dialogue(actor, nearby, Dialogue.TYPE_PLEA));
      }
      /*
      if (report) I.say("  ADDING DIALOGUE WITH "+seen);
      if (CombatUtils.hostileRating(actor, nearby) > 0) {
        choice.add(new Dialogue(actor, nearby, Dialogue.TYPE_PLEA));
      }
      else
      //*/
      else choice.add(new Dialogue(actor, nearby, Dialogue.TYPE_CASUAL));
    }
  }
  
  
  protected void putEmergencyResponse(Choice choice) {
    choice.add(new Retreat(actor));
  }
  
  
  private void addConstantResponses(Choice choice) {
    final boolean report = verbose && I.talkAbout == actor;
    if (report) I.say("\nGetting constant responses.");
    
    //  TODO:  You need to respond to more distant actors here.
    for (Target e : actor.senses.awareOf()) {
      if (e instanceof Actor) {
        if (report) I.say("  Responding to actor: "+e);
        final Actor nearby = (Actor) e;
        addReactions(nearby, choice);
        choice.add(Hunting.asHarvest(actor, nearby, home, true));
        choice.add(Gifting.nextGifting(null, actor, nearby));
      }
    }
    
    choice.add(Exploring.nextExploration(actor));
    choice.add(Exploring.nextWandering  (actor));
    choice.add(new Foraging(actor, null));
    choice.add(new Retreat(actor));
    
    final boolean timeoff = work == null || ! work.personnel().onShift(actor);
    if (work != null) {
      choice.add(work.jobFor(actor));
      if (timeoff && work != home) work.addServices(choice, actor);
      choice.add(new Payday(actor, work));
    }
    if (home != null) {
      if (home != work) choice.add(home.jobFor(actor));
      if (timeoff) home.addServices(choice, actor);
      choice.add(new Resting(actor, home));
    }
    else {
      final Target restsAt = actor.senses.haven();
      if (restsAt != null) choice.add(new Resting(actor, restsAt));
    }
  }
  
  
  private void addVenueResponses(Choice choice) {
    final Stage world = actor.world();
    final Batch <Venue> around = new Batch <Venue> ();
    float numSampled = 5 + (actor.traits.traitLevel(COGNITION) / 4);
    
    world.presences.sampleFromMaps(
      actor, world, (int) numSampled, around, Venue.class
    );
    
    final boolean timeoff = work == null || ! work.personnel().onShift(actor);
    for (Venue venue : around) {
      if (timeoff && venue.structure().intact()) {
        venue.addServices(choice, actor);
        choice.add(FindWork.attemptFor(actor, venue));
        choice.add(FindHome.attemptFor(actor, venue));
      }
    }
    if (timeoff) {
      choice.add(Repairs.getNextRepairFor(actor, false));
      choice.add(new ItemDisposal(actor));
    }
  }
  
  
  private void addBaseResponses(Choice choice) {
    //
    //  Derive tasks from missions or the scenario.
    choice.add(mission);
    choice.add(Scenario.current().taskFor(actor));
    choice.add(FindMission.attemptFor(actor));
  }
  
  
  public void updateAI(int numUpdates) {
    super.updateAI(numUpdates);
  }
}










