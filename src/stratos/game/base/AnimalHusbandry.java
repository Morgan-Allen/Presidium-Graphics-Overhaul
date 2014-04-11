/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */



package stratos.game.base ;
import stratos.game.building.* ;
import stratos.game.common.* ;
import stratos.game.actors.* ;
import stratos.game.planet.* ;
import stratos.graphics.common.* ;
import stratos.user.* ;
import stratos.util.* ;


//  TODO:  Only insist upon specimen imports in Hard Core mode.  (You're
//  skipping over a lot of other 'manufacturing essentials' at the moment,
//  so you might as well...)



public class AnimalHusbandry extends Plan implements Economy {
  
  
  private static boolean verbose = false ;
  
  final SurveyStation station ;
  private Fauna handled ;
  
  
  
  AnimalHusbandry(Actor actor, SurveyStation station, Fauna handled) {
    super(actor, station) ;
    this.station = station ;
    this.handled = handled ;
  }
  
  
  public AnimalHusbandry(Session s) throws Exception {
    super(s) ;
    station = (SurveyStation) s.loadObject() ;
    handled = (Fauna) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(station) ;
    s.saveObject(handled) ;
  }
  
  
  
  /**  Priority and target evaluation-
    */
  public float priorityFor(Actor actor) {
    return ROUTINE ;
  }
  
  
  protected static Fauna nextHandled(SurveyStation station) {
    for (Item match : station.stocks.matches(REPLICANTS)) {
      if (match.refers instanceof Fauna) return (Fauna) match.refers ;
    }
    
    final World world = station.world() ;
    final Tile e = world.tileAt(station) ;
    //final Ecology ecology = world.ecology() ;
    Fauna picked = null ;
    float bestRating = 0 ;
    
    for (Species species : Species.ANIMAL_SPECIES) {
      final float crowding = Nest.crowdingFor(station, species, world) ;
      I.say("Abundance of "+species+" is "+crowding) ;
      if (crowding >= 1) continue ;
      final Fauna specimen = species.newSpecimen(null) ;
      if (specimen == null) continue ;
      float rating = 10f / (1 + crowding) ;
      if (rating > bestRating) {
        I.say("Best is: "+species) ;
        picked = specimen ;
        bestRating = rating ;
      }
    }
    
    if (picked == null) return null ;
    picked.setPosition(e.x, e.y, world) ;
    I.say("Next fauna to breed is: "+picked) ;
    return picked ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public Behaviour getNextStep() {
    if (! handled.inWorld()) {
      final Action tends = new Action(
        actor, handled,
        this, "actionTendSpecimen",
        Action.BUILD, "Tending "+handled
      ) ;
      tends.setMoveTarget(station) ;
      return tends ;
    }
    return null ;
  }
  
  
  //
  //  TODO:  If you're raising a predator, then they need to be fed on meat,
  //  and possibly take up residence at the flesh still.
  
  public boolean actionTendSpecimen(Actor actor, Fauna fauna) {
    final float upgrade = station.structure.upgradeLevel(
      SurveyStation.CAPTIVE_BREEDING
    ) ;
    float success = 1 + (upgrade / 2f) ;
    if (actor.traits.test(XENOZOOLOGY, ROUTINE_DC, 1)) success++ ;
    if (actor.traits.test(DOMESTICS  , SIMPLE_DC , 1)) success++ ;
    float inc = success * 10f / World.STANDARD_DAY_LENGTH ;
    
    Item basis = Item.withReference(REPLICANTS, fauna) ;
    basis = station.stocks.matchFor(basis) ;
    if (basis == null) {
      basis = Item.with(REPLICANTS, fauna, inc / 5, 0) ;
      fauna.health.setupHealth(0, 1, 0) ;
      station.stocks.addItem(basis) ;
    }
    else if (basis.amount < 1) {
      ///I.say("Increment is: "+inc) ;
      station.stocks.addItem(Item.withAmount(basis, inc / 5)) ;
    }
    else {
      final Item eaten = Item.withAmount(CARBS, inc * 2) ;
      if (station.stocks.hasItem(eaten)) {
        station.stocks.removeItem(eaten) ;
      }
      else inc /= 5 ;
      ///I.say("Increment is: "+inc) ;
      float oldAge = fauna.health.ageLevel() ;
      fauna.health.setMaturity(oldAge + inc) ;
    }
    ///I.say("Age stage/upgrade: "+fauna.health.agingStage()+"/"+upgrade) ;
    
    if (fauna.health.agingStage() >= upgrade) {
      station.stocks.removeItem(basis) ;
      final World world = actor.world() ;
      fauna.assignBase(actor.base()) ;
      fauna.enterWorldAt(station, world) ;
      fauna.goAboard(station, world) ;
    }
    return true ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (! describedByStep(d)) ;
  }
}

