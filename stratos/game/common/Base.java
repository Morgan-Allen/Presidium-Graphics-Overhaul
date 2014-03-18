/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.common ;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.campaign.*;
import stratos.game.civilian.*;
import stratos.game.tactical.*;
import stratos.graphics.common.*;
import stratos.user.*;
import stratos.util.*;



//  TODO:  Primal bases shouldn't employ fog of war, commerce transactions, and
//  the like.  They are intended for use primarily by artilects, wildlife,
//  natives, and the like.

//  TODO:  Modify relations between bases depending on the average relations
//  of their members.


public class Base implements
  Session.Saveable, Schedule.Updates, Accountable
{
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static String
    KEY_ARTILECTS = "Artilects",
    KEY_WILDLIFE  = "Wildlife" ,
    KEY_NATIVES   = "Natives"  ;
  
  
  final public World world ;
  final boolean primal ;
  
  final public Commerce commerce = new Commerce(this) ;
  final public Paving paving ;
  float credits = 0, interest = 0 ;
  
  Actor ruler ;
  Venue commandPost ;
  final List <Mission> missions = new List <Mission> () ;
  
  //
  //  TODO:  Create a Reputation/Ratings class for these, and move 'em there.
  private float
    communitySpirit,
    alertLevel,
    crimeLevel,
    averageMood,
    propertyValues,
    creditCirculation ;
  final Table <Accountable, Relation> baseRelations = new Table() ;
  
  final public BaseProfiles profiles = new BaseProfiles(this) ;
  final public DangerMap dangerMap ;
  final public IntelMap intelMap = new IntelMap(this) ;
  
  
  public String title  = "Player Base" ;
  public Colour colour = Colour.BLUE ;
  
  
  
  public static Base createFor(World world, boolean primal) {
    final Base base = new Base(world, primal) ;
    world.registerBase(base, true) ;
    if (primal) base.colour = Colour.LIGHT_GREY ;
    return base ;
  }
  
  /*
  public static Base withName(String name, World world, boolean primal) {
    return null ;
  }
  //*/
  
  
  private Base(World world, boolean primal) {
    this.world = world ;
    this.primal = primal ;
    paving = new Paving(world) ;
    //maintenance = new PresenceMap(world, "damaged") ;
    dangerMap = new DangerMap(world, this) ;
    intelMap.initFog(world) ;
    
  }
  
  
  public Base(Session s) throws Exception {
    s.cacheInstance(this) ;
    this.world = s.world() ;
    this.primal = s.loadBool() ;
    commerce.loadState(s) ;
    paving = new Paving(world) ;
    paving.loadState(s) ;
    credits = s.loadFloat() ;
    interest = s.loadFloat() ;

    ruler = (Actor) s.loadObject() ;
    s.loadObjects(missions) ;
    
    communitySpirit = s.loadFloat() ;
    alertLevel = s.loadFloat() ;
    crimeLevel = s.loadFloat() ;
    averageMood = s.loadFloat() ;
    propertyValues = s.loadFloat() ;
    creditCirculation = s.loadFloat() ;
    
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final Relation r = Relation.loadFrom(s) ;
      baseRelations.put(r.subject, r) ;
    }
    
    profiles.loadState(s) ;
    dangerMap = new DangerMap(world, this) ;
    dangerMap.loadState(s) ;
    intelMap.loadState(s) ;
    
    title = s.loadString() ;
    colour.loadFrom(s.input()) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveBool(primal) ;
    commerce.saveState(s) ;
    paving.saveState(s) ;
    s.saveFloat(credits) ;
    s.saveFloat(interest) ;
    
    s.saveObject(ruler) ;
    s.saveObjects(missions) ;
    
    //
    //  TODO:  Move these to the Reputation/Ratings class!
    s.saveFloat(communitySpirit) ;
    s.saveFloat(alertLevel) ;
    s.saveFloat(crimeLevel) ;
    s.saveFloat(averageMood) ;
    s.saveFloat(propertyValues) ;
    s.saveFloat(creditCirculation) ;
    
    s.saveInt(baseRelations.size()) ;
    for (Relation r : baseRelations.values()) Relation.saveTo(s, r) ;
    
    profiles.saveState(s) ;
    dangerMap.saveState(s) ;
    intelMap.saveState(s) ;
    
    s.saveString(title) ;
    colour.saveTo(s.output()) ;
  }
  
  
  
  /**  Dealing with missions amd personnel-
    */
  public List <Mission> allMissions() {
    return missions ;
  }
  
  
  public void addMission(Mission t) {
    missions.include(t) ;
  }
  
  
  public void removeMission(Mission t) {
    missions.remove(t) ;
  }
  
  
  public Actor ruler() {
    return ruler ;
  }
  
  
  public void assignRuler(Actor rules) {
    this.ruler = rules ;
  }
  
  
  
  /**  Dealing with finances, trade and taxation-
    */
  public int credits() {
    return (int) credits ;
  }
  
  
  public void incCredits(float inc) {
    credits += inc ;
  }
  
  
  public boolean hasCredits(float sum) {
    return credits >= sum ;
  }
  
  
  public void setInterestPaid(float paid) {
    this.interest = paid ;
  }
  
  
  public float propertyValues() {
    return propertyValues ;
  }
  
  
  public float creditCirculation() {
    return creditCirculation ;
  }
  
  
  
  /**  Dealing with admin functions-
    */
  public void setRelation(Base base, float attitude) {
    baseRelations.put(base, new Relation(this, base, attitude, -1)) ;
  }
  
  
  public float relationWith(Base base) {
    final Relation r = baseRelations.get(base) ;
    if (r == null) return 0 ;
    return r.value() ;
  }
  
  
  public Base base() { return this ; }
  
  
  public float communitySpirit() {
    return communitySpirit ;
  }
  
  
  public float crimeLevel() {
    return crimeLevel ;
  }
  
  
  public float alertLevel() {
    return alertLevel ;
  }
  
  
  
  /**  Regular updates-
    */
  public float scheduledInterval() {
    return 1 ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    commerce.updateCommerce(numUpdates) ;
    paving.distribute(Economy.ALL_PROVISIONS) ;
    dangerMap.updateVals() ;
    for (Mission mission : missions) mission.updateMission() ;
    //
    //  Once per day, iterate across all personnel to get a sense of citizen
    //  mood, and compute community spirit.  (This declines as your settlement
    //  gets bigger.)
    if (numUpdates % World.STANDARD_DAY_LENGTH == 0) {
      final Tile t = world.tileAt(0, 0) ;
      int numResidents = 0 ;
      averageMood = 0.5f ;
      propertyValues = 0 ;
      creditCirculation = credits ;
      
      for (Object o : world.presences.matchesNear(this, t, -1)) {
        if (! (o instanceof Venue)) continue ;
        final Venue v = (Venue) o ;
        propertyValues += Audit.propertyValue(v) ;
        credits += v.stocks.credits() ;
        
        for (Actor resident : v.personnel.residents()) {
          numResidents++ ;
          averageMood += resident.health.moraleLevel() ;
        }
      }
      
      averageMood /= (numResidents + 1) ;
      communitySpirit = 1f / (1 + (numResidents / 100f)) ;
      communitySpirit = (communitySpirit + averageMood) / 2f ;
      
      final float repaid = credits * interest / 100f ;
      if (repaid > 0) incCredits(0 - repaid) ;
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void renderFor(Rendering rendering) {
    final Viewport port = rendering.view ;
    
    for (Mission mission : missions) {
      final Sprite flag = mission.flagSprite() ;
      if (! port.intersects(flag.position, 2)) continue ;
      flag.readyFor(rendering);
      //rendering.addClient(flag) ;
    }
  }
  
  
  public Mission pickedMission(BaseUI UI, Viewport view) {
    Mission closest = null ;
    float minDist = Float.POSITIVE_INFINITY ;
    for (Mission mission : missions) {
      final Sprite flag = mission.flagSprite() ;
      float dist = view.translateToScreen(new Vec3D(flag.position)).z;
      if (view.mouseIntersects(flag.position, 0.5f, UI)) {
        if (dist < minDist) { minDist = dist ; closest = mission ; }
      }
    }
    return closest ;
  }
  
  
  public String toString() {
    return "Base: "+title ;
  }
}





















