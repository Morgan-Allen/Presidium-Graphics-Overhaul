/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.common;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.campaign.*;
import stratos.game.civilian.*;
import stratos.game.tactical.*;
import stratos.graphics.common.*;
import stratos.user.*;
import stratos.util.*;



//  TODO:  Primal bases shouldn't employ commerce transactions.  (I think?)

public class Base implements
  Session.Saveable, Schedule.Updates, Accountable
{
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static String
    KEY_ARTILECTS = "Artilects",
    KEY_WILDLIFE  = "Wildlife" ,
    KEY_NATIVES   = "Natives"  ,
    KEY_FREEHOLD  = "Freehold";
  
  
  final public World   world ;
  final public boolean primal;
  
  final public BaseSetup setup   ;
  final public Commerce  commerce;
  final public Paving    paving  ;
  float credits = 0, interest = 0;
  
  final public BaseProfiles profiles ;
  final public DangerMap    dangerMap;
  final public IntelMap     intelMap ;
  
  Actor ruler;
  Venue commandPost;
  final List <Mission> missions = new List <Mission> ();
  final public BaseRelations relations;
  
  public String title  = "Player Base";
  public Colour colour = new Colour().set(Colour.BLUE);  //TODO:  Make private.
  
  
  
  public static Base baseWithName(
    World world, String title, boolean primal
  ) {
    for (Base base : world.bases()) if (
      base.title != null &&
      base.title.equals(title) &&
      base.primal == primal
    ) {
      return base;
    }
    
    final Base base = new Base(world, primal);
    world.registerBase(base, true);

    base.title = title;
    if (primal) base.colour.set(Colour.LIGHT_GREY);
    
    return base;
  }
  
  
  private Base(World world, boolean primal) {
    this.world = world;
    this.primal = primal;
    
    setup     = new BaseSetup(this, world);
    commerce  = new Commerce(this)        ;
    paving    = new Paving(world)         ;
    
    profiles  = new BaseProfiles(this)    ;
    dangerMap = new DangerMap(world, this);
    intelMap  = new IntelMap(this)        ;
    intelMap.initFog(world);
    
    relations = new BaseRelations(this)   ;
  }
  
  
  public Base(Session s) throws Exception {
    this(s.world(), s.loadBool());
    s.cacheInstance(this);
    
    commerce.loadState(s);
    paving  .loadState(s);
    credits  = s.loadFloat();
    interest = s.loadFloat();
    
    profiles .loadState(s);
    dangerMap.loadState(s);
    intelMap .loadState(s);

    ruler       = (Actor) s.loadObject();
    commandPost = (Venue) s.loadObject();
    s.loadObjects(missions);
    relations.loadState(s);
    
    title = s.loadString();
    colour.loadFrom(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveBool(primal);
    
    commerce.saveState(s);
    paving  .saveState(s);
    s.saveFloat(credits );
    s.saveFloat(interest);
    
    profiles .saveState(s);
    dangerMap.saveState(s);
    intelMap .saveState(s);
    
    s.saveObject(ruler      );
    s.saveObject(commandPost);
    s.saveObjects(missions);
    relations.saveState(s);
    
    s.saveString(title);
    colour.saveTo(s.output());
  }
  
  
  
  /**  Dealing with missions amd personnel-
    */
  public List <Mission> allMissions() {
    return missions;
  }
  
  
  public void addMission(Mission t) {
    missions.include(t);
  }
  
  
  public void removeMission(Mission t) {
    missions.remove(t);
  }
  
  
  public Actor ruler() {
    return ruler;
  }
  
  
  public void assignRuler(Actor rules) {
    this.ruler = rules;
  }
  
  
  
  /**  Dealing with finances, trade and taxation-
    */
  public int credits() {
    return (int) credits;
  }
  
  
  public void incCredits(float inc) {
    credits += inc;
  }
  
  
  public boolean hasCredits(float sum) {
    return credits >= sum;
  }
  
  
  public void setInterestPaid(float paid) {
    this.interest = paid;
  }
  
  
  public Base base() { return this; }
  
  
  
  /**  Regular updates-
    */
  public float scheduledInterval() {
    return 1;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    
    setup.updatePlacements();
    
    commerce.updateCommerce(numUpdates);
    
    paving.distribute(Economy.ALL_PROVISIONS, this);
    
    dangerMap.update();
    
    for (Mission mission : missions) mission.updateMission();
    
    if (numUpdates % (World.STANDARD_DAY_LENGTH / 3) == 0) {
      relations.updateRelations();
      
      final float repaid = credits * interest / 100f;
      if (repaid > 0) incCredits(0 - repaid);
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void renderFor(Rendering rendering) {
    for (Mission mission : missions) {
      if (! rendering.view.intersects(mission.flagSelectionPos(), 2)) continue;
      mission.flagSprite().readyFor(rendering);
    }
  }
  
  
  public Mission pickedMission(BaseUI UI, Viewport view) {
    Mission closest = null;
    float minDist = Float.POSITIVE_INFINITY;
    for (Mission mission : missions) {
      final Vec3D selPos = mission.flagSelectionPos();
      if (! view.mouseIntersects(selPos, 0.5f, UI)) continue;
      final float dist = view.translateToScreen(selPos).z;
      if (dist < minDist) { minDist = dist; closest = mission; }
    }
    return closest;
  }
  
  
  public String toString() {
    return title;
  }
}





















