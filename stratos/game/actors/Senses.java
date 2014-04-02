

package stratos.game.actors;
import stratos.game.common.*;
import stratos.game.tactical.*;
import stratos.game.building.*;
import stratos.util.*;



public class Senses implements Qualities {
  
  
  final Actor actor;
  final Table <Element, Session.Saveable> seen = new Table() ;
  
  
  protected Senses(Actor actor) {
    this.actor = actor;
  }
  
  
  public void loadState(Session s) throws Exception {
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final Element e = (Element) s.loadObject() ;
      seen.put(e, s.loadObject()) ;
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(seen.size()) ;
    for (Element e : seen.keySet()) {
      s.saveObject(e) ;
      s.saveObject(seen.get(e)) ;
    }
  }
  
  

  
  
  
  /**  Dealing with seen objects and reactions to them-
    */
  private Session.Saveable reactionKey(Element seen) {
    if (seen instanceof Actor) {
      final Actor a = (Actor) seen ;
      if (a.currentAction() == null) return seen ;
      final Behaviour b = a.mind.rootBehaviour() ;
      if (b == null) return seen ;
      else return b ;
    }
    else return seen ;
  }
  
  
  public boolean awareOf(Element e) {
    return seen.get(e) != null ;
  }
  
  
  public boolean hasSeen(Element e) {
    return seen.get(e) != null ;
  }
  
  
  public Batch <Element> awareOf() {
    final Batch <Element> seen = new Batch <Element> () ;
    for (Element e : this.seen.keySet()) seen.add(e) ;
    return seen ;
  }
  
  
  protected boolean notices(Element e, float noticeRange) {
    final ActorMind mind = actor.mind;
    
    if (e == actor || ! e.inWorld()) return false ;
    if (e == mind.home || e == mind.work) return true ;
    final int roll = Rand.index(20) ;
    if (roll == 0 ) noticeRange *= 2 ;
    if (roll == 19) noticeRange /= 2 ;
    
    if (e instanceof Mobile) {
      final Mobile m = (Mobile) e ;
      if (m.indoors()) noticeRange /= 2 ;
    }
    if (e instanceof Actor) {
      final Actor a = (Actor) e ;
      if (a.targetFor(null) == actor) return true;// noticeRange *= 2 ;
    }
    if (e instanceof Fixture) {
      final Fixture f = (Fixture) e ;
      noticeRange += f.size * 2 ;
    }
    
    //  TODO:  Incorporate line-of-sight considerations here.
    noticeRange -= Combat.stealthValue(e, actor) ;
    if (awareOf(e)) noticeRange += World.SECTOR_SIZE / 2f ;
    
    return Spacing.distance(actor, e) < noticeRange ;
  }
  
  
  protected void updateSeen() {
    final World world = actor.world() ;
    final float sightRange = actor.health.sightRange() ;
    final int reactLimit = 3 + (int) (actor.traits.traitLevel(PERCEPT) / 5) ;
    final ActorMind mind = actor.mind;
    
    final Batch <Element>
      couldSee   = new Batch <Element> (),
      justSaw    = new Batch <Element> (),
      outOfSight = new Batch <Element> () ;
    //
    //  Firstly, cull anything you can't see any more-
    for (Element e : seen.keySet()) {
      if (! notices(e, sightRange)) outOfSight.add(e) ;
    }
    for (Element e : outOfSight) seen.remove(e) ;
    //
    //  Then, sample nearby objects you could react to-
    world.presences.sampleFromKeys(
      actor, world, reactLimit, couldSee,
      Mobile.class,
      Venue.class
    ) ;
    for (Behaviour b : world.activities.targeting(actor)) {
      if (b instanceof Action) {
        final Actor a = ((Action) b).actor ;
        //if (Spacing.distance(a, actor) > World.SECTOR_SIZE) continue ;
        couldSee.add(a) ;
      }
    }
    if (mind.home != null) couldSee.include((Element) mind.home) ;
    if (mind.work != null) couldSee.include((Element) mind.work) ;
    //
    //  And check to see if they're anything new.
    for (Element m : couldSee) {
      if (! notices(m, sightRange)) continue ;
      final Session.Saveable after = reactionKey(m), before = seen.get(m) ;
      if (before != after) justSaw.add(m) ;
      seen.put(m, after) ;
    }
    //
    //  Finally, add reactions to anything novel-
    if (justSaw.size() > 0) {
      final Choice choice = new Choice(actor) ;
      for (Element NS : justSaw) mind.addReactions(NS, choice) ;
      final Behaviour reaction = choice.pickMostUrgent() ;
      if (mind.couldSwitchTo(reaction)) mind.assignBehaviour(reaction) ;
    }
  }
  
  
}



