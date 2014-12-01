/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.common;
import stratos.game.actors.*;
import stratos.game.campaign.*;
import stratos.game.civilian.*;
import stratos.game.economic.*;
import stratos.game.tactical.*;
import stratos.graphics.common.*;
import stratos.user.*;
import stratos.util.*;



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
  
  
  final public Stage   world ;
  final public boolean primal;
  
  final public BaseSetup    setup     ;
  final public Commerce     commerce  ;
  final public PavingRoutes paveRoutes;
  
  final public BaseFinance  finance  ;
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
    Stage world, String title, boolean primal
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
  
  
  private Base(Stage world, boolean primal) {
    this.world = world;
    this.primal = primal;
    
    setup      = new BaseSetup(this, world);
    commerce   = new Commerce(this)        ;
    paveRoutes = new PavingRoutes(world)   ;
    
    finance   = new BaseFinance(this);
    profiles  = new BaseProfiles(this)    ;
    
    dangerMap = new DangerMap(world, this);
    intelMap  = new IntelMap(this)        ;
    intelMap.initFog(world);
    
    relations = new BaseRelations(this)   ;
  }
  
  
  public Base(Session s) throws Exception {
    this(s.world(), s.loadBool());
    s.cacheInstance(this);
    
    commerce  .loadState(s);
    paveRoutes.loadState(s);
    
    finance  .loadState(s);
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
    paveRoutes  .saveState(s);
    
    finance  .saveState(s);
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
  
  
  public Base base() { return this; }
  
  
  
  /**  Regular updates-
    */
  public float scheduledInterval() {
    return 1;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    ///I.say("UPDATING BASE, NUM UPDATES: "+numUpdates);
    
    setup.updatePlacements();
    
    commerce.updateCommerce(numUpdates);
    
    paveRoutes.distribute(Economy.ALL_PROVISIONS, this);
    
    dangerMap.update();
    
    for (Mission mission : missions) mission.updateMission();
    
    final int interval = Stage.STANDARD_DAY_LENGTH / 3;
    if (numUpdates % interval == 0) {
      relations.updateRelations();
      finance.updateFinances(interval);
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





















