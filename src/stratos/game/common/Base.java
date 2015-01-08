/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.common;
import stratos.game.actors.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.politic.*;
import stratos.game.wild.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.KeyInput;
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
    KEY_FREEHOLD  = "Freehold" ;
  
  
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
  final public BaseTactics   tactics  ;
  
  private String title  = "Player Base";
  private Colour colour = new Colour();
  
  
  
  private static Base baseWithName(
    String title, Colour colour,
    Stage world, boolean primal, VenueProfile... canPlace
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
    base.colour.set(colour);
    base.setup.setAvailableVenues(canPlace);
    return base;
  }
  
  
  public static Base withName(Stage world, String title, Colour colour) {
    return baseWithName(title, colour, world, false);
  }
  
  
  public static Base wildlife(Stage world) {
    return baseWithName(
      KEY_WILDLIFE, Colour.LITE_GREEN, world, true, Nest.VENUE_PROFILES
    );
  }
  
  
  public static Base artilects(Stage world) {
    return baseWithName(
      KEY_ARTILECTS, Colour.LITE_RED, world, true, Ruins.VENUE_PROFILES
    );
  }
  
  
  public static Base natives(Stage world, int tribeID) {
    return baseWithName(
      KEY_NATIVES+" ("+NativeHut.TRIBE_NAMES[tribeID]+")", Colour.LITE_YELLOW,
      world, true, NativeHut.VENUE_PROFILES[tribeID]
    );
  }
  
  
  public boolean isNative() {
    return title.startsWith(KEY_NATIVES);
  }
  
  
  public boolean isPrimal() {
    return primal;
  }
  
  
  private Base(Stage world, boolean primal) {
    this.world = world;
    this.primal = primal;
    
    setup      = new BaseSetup(this, world);
    commerce   = new Commerce(this);
    paveRoutes = new PavingRoutes(world);
    
    finance    = new BaseFinance(this);
    profiles   = new BaseProfiles(this);
    
    dangerMap  = new DangerMap(world, this);
    intelMap   = new IntelMap(this);
    intelMap.initFog(world);
    
    relations  = new BaseRelations(this);
    tactics    = new BaseTactics(this);
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
    tactics  .loadState(s);
    
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
    tactics  .saveState(s);
    
    s.saveString(title);
    colour.saveTo(s.output());
  }
  
  
  public Colour colour() {
    return colour;
  }
  
  
  public String title() {
    return title;
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
  
  
  public Property HQ() {
    return ruler == null ? null : ruler.mind.home();
  }
  
  
  public Base base() { return this; }
  
  
  
  /**  Regular updates-
    */
  public float scheduledInterval() {
    return 1;
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    ///I.say("UPDATING BASE, NUM UPDATES: "+numUpdates);
    
    setup.updatePlacements();
    commerce.updateCommerce(numUpdates);
    paveRoutes.distribute(Economy.ALL_PROVISIONS, this);
    dangerMap.update();
    
    for (Mission mission : missions) {
      mission.updateMission();
    }
    tactics.updateMissionAssessment(numUpdates);
    
    final int interval = Stage.STANDARD_DAY_LENGTH / 3;
    if (numUpdates % interval == 0) {
      relations.updateRelations();
      finance.updateFinances(interval);
    }
  }
  
  
  
  /**  Gets missions already assigned to a given target:
    */
  public Mission matchingMission(Target t, Class typeClass) {
    for (Mission match : allMissions()) {
      if (typeClass != null && match.getClass() != typeClass) continue;
      if (match.subject() != t) continue;
      return match;
    }
    return null;
  }
  
  
  public Mission matchingMission(Mission m) {
    return matchingMission(m.subject(), m.getClass());
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public static void renderMissions(
    Stage world, Rendering rendering, Base player
  ) {
    for (Mission mission : layoutMissions(world, rendering.view, player)) {
      mission.flagSprite().readyFor(rendering);
    }
  }
  
  
  public static Mission pickedMission(
    Stage world, BaseUI UI, Viewport view, Base player
  ) {
    final Pick <Mission> pick = new Pick <Mission> ();
    final Vec3D centre = new Vec3D();
    
    for (Mission mission : layoutMissions(world, view, player)) {
      centre.setTo(mission.flagSprite().position);
      centre.z += 0.5f;
      if (! view.mouseIntersects(centre, 0.5f, UI)) continue;
      final float dist = view.translateToScreen(centre).z;
      pick.compare(mission, 0 - dist);
    }
    return pick.result();
  }
  
  
  private static Batch <Mission> layoutMissions(
    Stage world, Viewport view, Base player
  ) {
    final Table <Target, List <Mission>> seenTable = new Table();
    
    for (Base base : world.bases()) for (Mission m : base.allMissions()) {
      if (! m.visibleTo(player)) continue;
      final Target t = m.subject();
      if (! view.intersects(t.position(null), t.radius())) continue;
      
      List <Mission> onT = seenTable.get(t);
      if (onT == null) seenTable.put(t, onT = new List());
      onT.add(m);
    }
    
    final Batch <Mission> visible = new Batch();
    for (Target t : seenTable.keySet()) {
      final List <Mission> onT = seenTable.get(t);
      layoutMissions(onT, t, view);
      Visit.appendTo(visible, onT);
    }
    return visible;
  }
  
  
  private static void layoutMissions(
    Series <Mission> missions, Target above, Viewport view
  ) {
    final Vec3D horiz = view.screenHorizontal(), adds = new Vec3D();
    
    float offset = (missions.size() - 1) / 2f, index = 0;
    for (Mission m : missions) {
      Sprite s = m.flagSprite();
      s.position.setTo(above.position(null));
      s.position.z += above.height() + 0.5f;
      adds.setTo(horiz).normalise().scale(index - offset);
      s.position.add(adds);
      index++;
    }
  }
  
  
  
  public String toString() {
    return title;
  }
}



