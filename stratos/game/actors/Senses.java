

package stratos.game.actors;
import stratos.game.common.*;
import stratos.game.tactical.*;
import stratos.game.building.*;
import stratos.util.*;



public class Senses implements Qualities {
  
  
  private static boolean verbose = false;
  
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
  
  
  public static boolean hasLineOfSight(
    Target origin, Target target, float maxRange
  ) {
    if (origin == null || target == null) return false ;
    if (maxRange > 0 && Spacing.distance(origin, target) > maxRange) {
      return false ;
    }
    final boolean reports = verbose && I.talkAbout == origin ;
    
    //  Firstly, we determine the start and end points for the line segment,
    //  and the vector connecting the two-
    final World world = origin.world() ;
    final Vec2D
      orig = new Vec2D().setTo(origin.position(null)),
      dest = new Vec2D().setTo(target.position(null)),
      line = new Vec2D().setTo(dest).sub(orig) ;
    final float fullLength = line.length() ;
    final Batch <Tile> considered = new Batch <Tile> () ;
    final Vec2D toCheck = new Vec2D() ;
    
    //  Next, we assemble a list of each tile that might plausibly intersect
    //  the line segment-
    for (int i = 0 ; i < fullLength ; i++) {
      toCheck.setTo(line).normalise().scale(i).add(orig) ;
      final Tile t = world.tileAt(toCheck.x, toCheck.y) ;
      if (t == null || t.flaggedWith() != null) continue ;
      considered.add(t) ;
      t.flagWith(considered) ;
      for (Tile n : t.edgeAdjacent(Spacing.tempT4)) {
        if (n == null || n.flaggedWith() != null) continue ;
        considered.add(n) ;
        n.flagWith(considered) ;
      }
    }
    for (Tile t : considered) t.flagWith(null) ;
    
    //  Then, we check to see if any such tiles are actually blocked, and
    //  perform a more exacting intersection test-
    if (reports) {
      I.say("\nCHECKING LINE OF SIGHT TO: "+target) ;
      I.say("  Mobile origin: "+orig) ;
      I.say("  Target position: "+dest) ;
    }
    boolean blocked = false ;
    boolean onRight, onLeft ;
    for (Tile t : considered) if (t.blocked()) {
      if (t == target || t.owner() == target) continue ;
      
      //  We first check whether the centre of the tile in question falls
      //  between the start and end points of the line segment-
      toCheck.set(t.x, t.y).sub(orig);
      final float dot = line.dot(toCheck);
      if (dot < 0 || dot > (fullLength * fullLength)) continue;
      onRight = onLeft = false;
      
      //  Then, we check to see if corners of this tile lie to both the left
      //  and right of the line segment-
      for (int d : Tile.N_DIAGONAL) {
        toCheck.set(
          t.x - (Tile.N_X[d] / 2f),
          t.y - (Tile.N_Y[d] / 2f)
        ).sub(orig) ;
        final float side = line.side(toCheck) ;
        if (side < 0) onLeft = true ;
        if (side > 0) onRight  = true ;
      }
      
      //  If both checks are positive, we consider the tile blocked, and return
      //  the result-
      if (reports) {
        I.say("  Might be blocked at: "+t) ;
        I.say("  On right/left? "+onRight+"/"+onLeft) ;
      }
      if (onRight && onLeft) { blocked = true ; break ; }
    }
    if (reports) I.say(blocked ? "L_O_S BLOCKED!" : "L_O_S OKAY...") ;
    return ! blocked ;
  }
}



