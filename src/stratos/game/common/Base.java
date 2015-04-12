/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.common;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
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
    KEY_NATIVES   = "Natives"  ,
    KEY_VERMIN    = "Vermin"   ,
    KEY_WILDLIFE  = "Wildlife" ,
    KEY_FREEHOLD  = "Freehold" ,
    KEY_SETTLED   = "Settled"  ;
  
  
  final public Stage   world ;
  final public boolean primal;
  
  final public BaseSetup     setup    ;
  final public BaseDemands   demands  ;
  final public BaseCommerce  commerce ;
  final public BaseTransport transport;
  
  final public BaseFinance  finance  ;
  final public BaseProfiles profiles ;
  final public DangerMap    dangerMap;
  final public IntelMap     intelMap ;
  
  private Actor ruler;
  private Venue commandPost;
  private boolean isNative;
  final public BaseRelations relations = initRelations();
  final public BaseTactics   tactics   = initTactics  ();
  
  private String title  = "Player Base";
  private Colour colour = new Colour();
  
  
  private static Base namedBase(Stage world, String title) {
    for (Base base : world.bases()) {
      if (base.title != null && base.title.equals(title)) return base;
    }
    return null;
  }
  
  
  private static Base registerBase(
    Base base, Stage world, String title, Colour colour,
    VenueProfile... canBuild
  ) {
    base.title = title;
    base.colour.set(colour);
    base.setup.setAvailableVenues(canBuild);
    world.registerBase(base, true);
    if (I.logEvents()) I.say("\nREGISTERING NEW BASE: "+base);
    return base;
  }
  
  
  public static Base settlement(Stage world, String title, Colour colour) {
    final Base base = namedBase(world, title);
    if (base != null) return base;

    final VenueProfile canBuild[] = VenueProfile.allCivicProfiles();
    return registerBase(new Base(world, false), world, title, colour, canBuild);
  }
  
  
  public static Base wildlife(Stage world) {
    Base base = namedBase(world, KEY_WILDLIFE);
    if (base != null) return base;
    else base = new Base(world, true);
    
    final VenueProfile canBuild[] = new VenueProfile[0];
    return registerBase(base, world, KEY_WILDLIFE, Colour.LITE_GREEN, canBuild);
  }
  
  
  public static VerminBase vermin(Stage world) {
    VerminBase base = (VerminBase) namedBase(world, KEY_ARTILECTS);
    if (base != null) return base;
    else base = new VerminBase(world);
    
    final VenueProfile canBuild[] = new VenueProfile[0];
    registerBase(base, world, KEY_VERMIN, Colour.LITE_GREY, canBuild);
    return base;
  }
  
  
  public static ArtilectBase artilects(Stage world) {
    ArtilectBase base = (ArtilectBase) namedBase(world, KEY_ARTILECTS);
    if (base != null) return base;
    else base = new ArtilectBase(world);
    
    final VenueProfile canBuild[] = new VenueProfile[0];
    registerBase(base, world, KEY_ARTILECTS, Colour.LITE_RED, canBuild);
    return base;
  }
  
  
  public static Base natives(Stage world, int tribeID) {
    final String title = NativeHut.TRIBE_NAMES[tribeID];
    Base base = namedBase(world, title);
    if (base != null) return base;
    else base = new Base(world, true);
    
    base.isNative = true;
    final VenueProfile canBuild[] = NativeHut.VENUE_PROFILES[tribeID];
    return registerBase(base, world, title, Colour.LITE_YELLOW, canBuild);
  }
  
  
  public boolean isNative() {
    return isNative;
  }
  
  
  public boolean isPrimal() {
    return primal;
  }
  
  
  public boolean isRealPlayer() {
    return BaseUI.currentPlayed() == this;
  }
  
  
  public boolean isBaseAI() {
    return ! isRealPlayer();
  }
  
  
  protected Base(Stage world, boolean primal) {
    this.world = world;
    this.primal = primal;
    
    setup     = new BaseSetup(this, world);
    demands   = new BaseDemands(this, world);
    commerce  = new BaseCommerce(this);
    transport = new BaseTransport(world);
    
    finance    = new BaseFinance(this);
    profiles   = new BaseProfiles(this);
    
    dangerMap  = new DangerMap(world, this);
    intelMap   = new IntelMap(this);
    intelMap.initFog(world);
  }
  
  
  public Base(Session s) throws Exception {
    this(s.world(), s.loadBool());
    s.cacheInstance(this);
    
    setup    .loadState(s);
    demands  .loadState(s);
    commerce .loadState(s);
    transport.loadState(s);
    finance  .loadState(s);
    profiles .loadState(s);
    dangerMap.loadState(s);
    intelMap .loadState(s);

    ruler       = (Actor) s.loadObject();
    commandPost = (Venue) s.loadObject();
    relations.loadState(s);
    tactics  .loadState(s);
    
    title = s.loadString();
    colour.loadFrom(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveBool(primal);
    
    setup    .saveState(s);
    demands  .saveState(s);
    commerce .saveState(s);
    transport.saveState(s);
    finance  .saveState(s);
    profiles .saveState(s);
    dangerMap.saveState(s);
    intelMap .saveState(s);
    
    s.saveObject(ruler      );
    s.saveObject(commandPost);
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
  
  
  protected BaseTactics   initTactics  () { return new BaseTactics  (this); }
  protected BaseRelations initRelations() { return new BaseRelations(this); }
  
  
  
  /**  Dealing with missions amd personnel-
    */
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
    
    setup.updatePlacements();
    demands.updateAllMaps(1);
    commerce.updateCommerce(numUpdates);
    transport.distributeProvisions(this);
    dangerMap.update();
    tactics.updateTactics(numUpdates);
    
    final int interval = Stage.STANDARD_DAY_LENGTH / 3;
    if (numUpdates % interval == 0) {
      relations.updateRelations();
      finance.updateFinances(interval);
    }
  }
  
  
  
  /**  Gets missions already assigned to a given target:
    */
  //  TODO:  Move to the tactics class?
  
  public Mission matchingMission(Target t, Class typeClass) {
    for (Mission match : tactics.allMissions()) {
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
      mission.renderFlag(rendering);
    }
  }
  
  
  public static Mission pickedMission(
    Stage world, BaseUI UI, Viewport view, Base player
  ) {
    final Pick <Mission> pick = new Pick <Mission> ();
    final Vec3D centre = new Vec3D();
    final float scale = Mission.FLAG_SCALE;
    
    for (Mission mission : layoutMissions(world, view, player)) {
      centre.setTo(mission.flagSprite().position);
      centre.z += 1 * scale;
      if (! view.mouseIntersects(centre, scale, UI)) continue;
      final float dist = view.translateToScreen(centre).z;
      pick.compare(mission, 0 - dist);
    }
    return pick.result();
  }
  
  
  private static Batch <Mission> layoutMissions(
    Stage world, Viewport view, Base player
  ) {
    final Table <Target, List <Mission>> seenTable = new Table();
    
    for (Base base : world.bases()) {
      for (Mission m : base.tactics.allMissions()) {
        if (! m.visibleTo(player)) continue;
        final Target t = m.subject();
        if (! view.intersects(t.position(null), t.radius())) continue;
        
        List <Mission> onT = seenTable.get(t);
        if (onT == null) seenTable.put(t, onT = new List());
        onT.add(m);
      }
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
    final float scale = Mission.FLAG_SCALE * 1.5f;
    
    float offset = (missions.size() - 1) / 2f, index = 0;
    for (Mission m : missions) {
      final Sprite s = m.flagSprite();
      if (above instanceof Element) {
        ((Element) above).viewPosition(s.position);
      }
      else {
        above.position(s.position);
      }
      s.position.z += above.height() + (Mission.FLAG_SCALE / 3);
      adds.setTo(horiz).normalise().scale((index - offset) * scale);
      s.position.add(adds);
      index++;
    }
  }
  
  
  
  /**  Venue enumeration methods-
    */
  public Batch <Venue> listInstalled(
    VenueProfile type, boolean intact
  ) {
    final Batch <Venue> installed = new Batch <Venue> ();
    for (Object o : world.presences.matchesNear(type.baseClass, null, -1)) {
      final Venue v = (Venue) o;
      if (intact && ! v.structure.intact()) continue;
      if (v.base() == this) installed.add(v);
    }
    return installed;
  }
  
  
  public boolean checkPrerequisites(VenueProfile profile, Account reasons) {
    if (profile.isUnique()) {
      if (listInstalled(profile, false).size() > 0) {
        return reasons.asFailure("You cannot have more than one "+profile.name);
      }
    }
    for (VenueProfile req : profile.required) {
      if (listInstalled(req, true).size() <= 0) {
        return reasons.asFailure("Requires "+req);
      }
    }
    return reasons.asSuccess();
  }
  
  
  
  /**  
    */
  public String toString() {
    return title;
  }
}








