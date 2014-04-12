


package stratos.game.civilian ;
import stratos.game.actors.*;
import stratos.game.actors.ActorMind.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.user.*;
import stratos.util.*;



//
//  TODO:  Generalise this into a class for seeking alternative employment,
//  including at different venues in the same world.

//
//  ...Also, what about looking for homes?




public class Migration extends Plan implements Economy {
  
  
  /**  Data fields, setup and save/load functions-
    */
  final static int
    BOARD_PRICE = 100 ;
  
  private static boolean verbose = false ;
  
  
  float initTime = -1 ;
  Dropship ship ;
  
  
  public Migration(Actor actor) {
    super(actor, actor) ;
  }
  
  
  public Migration(Session s) throws Exception {
    super(s) ;
    initTime = s.loadFloat() ;
    ship = (Dropship) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveFloat(initTime) ;
    s.saveObject(ship) ;
  }
  
  
  
  /**  Evaluating targets and priority-
    */
  public float priorityFor(Actor actor) {
    //
    //  DISABLING FOR NOW  TODO:  Restore once job-applications are sorted out.
    //if (true) return -1 ;
    
    if (actor.mind.work() != null) return 0 ;
    if (initTime == -1) return ROUTINE ;
    final float timeSpent = actor.world().currentTime() + 10 - initTime ;
    float impetus = ROUTINE * timeSpent / World.STANDARD_DAY_LENGTH ;
    impetus *= ((1 - actor.health.moraleLevel()) + 1) / 2 ;
    return Visit.clamp(impetus, 0, URGENT) ;
  }
  
  
  public boolean finished() {
    return actor != null && priorityFor(actor) <= 0 ;
  }
  
  
  public static Migration migrationFor(Actor actor) {
    if (actor.mind.hasToDo(Migration.class)) return null ;
    return new Migration(actor) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    if (actor.mind.work() != null) {
      abortBehaviour() ;
      return null ;
    }
    if (actor.gear.credits() < BOARD_PRICE) return null ;
    if (initTime == -1) {
      final Action thinks = new Action(
        actor, actor.aboard(),
        this, "actionConsider",
        Action.TALK_LONG, "Thinking about migrating"
      ) ;
      return thinks ;
    }
    if (ship == null || ! ship.inWorld()) {
      final Visit <Dropship> picks = new Visit <Dropship> () {
        public float rate(Dropship t) {
          return 0 - Spacing.distance(actor, t) ;
        }
      } ;
      ship = picks.pickBest(actor.base().commerce.allVessels()) ;
    }
    if (ship == null || ! ship.inWorld()) return null ;
    final Action boards = new Action(
      actor, ship,
      this, "actionBoardVessel",
      Action.TALK, "Leaving on "+ship
    ) ;
    return boards ;
  }
  
  
  public boolean actionConsider(Actor actor, Target t) {
    if (initTime == -1) {
      initTime = actor.world().currentTime() ;
      I.sayAbout(actor, "Setting initial time "+this.hashCode()) ;
      return true ;
    }
    return false ;
  }
  
  
  public boolean actionBoardVessel(Actor actor, Dropship leaves) {
    actor.mind.setHome(null) ;
    if (actor.aboard() == leaves) return true ;
    final int price = BOARD_PRICE ;
    if (actor.gear.credits() < price) return false ;
    actor.gear.incCredits(0 - price) ;
    leaves.cargo.incCredits(price) ;
    actor.goAboard(leaves, actor.world()) ;
    return true ;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("Migrating off-planet") ;
  }
}














