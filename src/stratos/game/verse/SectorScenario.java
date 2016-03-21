/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.verse;
import stratos.content.civic.*;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
import stratos.user.*;
import stratos.user.notify.*;
import stratos.util.*;
import stratos.start.*;



public class SectorScenario extends Scenario {
  

  final public static int
    MAP_SIZES [] = { 128, 128, 128 },
    WALL_SIZES[] = { 16, 20, 24 };
  
  private Sector location;
  private Verse verse;
  private Expedition expedition;
  
  private MessageScript script;
  
  
  
  public SectorScenario() {
    this((Sector) null, null);
  }
  
  
  public SectorScenario(Sector location, String scriptPath) {
    super(false);
    this.location = location;
    if (! Assets.exists(scriptPath)) script = null;
    else script = new MessageScript(this, scriptPath);
  }
  
  
  protected SectorScenario(String scriptPath, String prefix) {
    super(prefix, false);
    if (! Assets.exists(scriptPath)) script = null;
    else script = new MessageScript(this, scriptPath);
  }
  
  
  public SectorScenario(Session s) throws Exception {
    super(s);
    this.location   = (Sector    ) s.loadObject();
    this.verse      = (Verse     ) s.loadObject();
    this.expedition = (Expedition) s.loadObject();
    this.script  = (MessageScript) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(location  );
    s.saveObject(verse     );
    s.saveObject(expedition);
    s.saveObject(script    );
  }
  
  
  public void beginScenario(Expedition config, Verse verse, String savesPrefix) {
    setupScenario(config, verse, savesPrefix);
    setSkipLoading(true);
    PlayLoop.setupAndLoop(this);
  }
  
  
  public void setupScenario(Expedition config, Verse verse, String savesPrefix) {
    this.location   = config.destination();
    this.verse      = verse ;
    this.expedition = config;
    setSavesPrefix(savesPrefix);
  }
  
  
  public Sector location() {
    return location;
  }
  
  
  public Verse verse() {
    return verse;
  }
  
  
  public Expedition expedition() {
    return expedition;
  }
  
  
  public MessageScript script() {
    return script;
  }
  

  protected void resetScenario() {
    //
    //  TODO:  I will eventually need a more sophisticated solution here-
    //         at the moment we simply 'yank' the expedition members out of
    //         the world before re-inserting them, when properly speaking they
    //         should be restored to their state from before the scenario...
    for (Actor a : expedition.allMembers()) {
      if (a.inWorld()) a.exitWorld();
      a.removeWorldReferences(world());
    }
    if (script != null) script.clearScript();
    super.resetScenario();
  }
  
  
  public void updateGameState() {
    super.updateGameState();
    if (script != null) script.checkForEvents();
  }
  
  
  
  /**  Required setup methods-
    */
  protected Stage createWorld() {
    if (verse.stage() != null) verse.onStageDeletion(verse.stage());
    
    final int          mapSize = MAP_SIZES[expedition.titleGranted()];
    final Sector       locale  = expedition.destination();
    final TerrainGen   TG      = locale.initialiseTerrain(mapSize);
    final StageTerrain terrain = TG.generateTerrain();
    final Stage        world   = Stage.createNewWorld(verse, locale, terrain);
    
    TG.setupOutcrops(world);
    Flora.populateFlora(world);
    return world;
  }
  
  
  protected Base createBase(Stage world) {
    final Sector origin = expedition.origin();
    final Base base = Base.settlement(
      world, "Player Base", expedition.backing()
    );
    base.finance.setInitialFunding(expedition.funding(), expedition.tribute());
    base.research.initKnowledgeFrom(origin);
    base.visits  .assignHomeworld  (origin);
    return base;
  }
  
  
  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    //
    //  Determine relevant attributes for the ruler-
    final Human ruler = ruler(base);
    //
    //  Try to pick out some complementary advisors-
    final List <Human> advisors = advisors(base, ruler);
    //
    //  Pick out some random colonists-
    final List <Human> colonists = colonists(base);
    //
    //  Establish the position of the base site-
    final Bastion bastion = establishBastion(
      world, base, ruler, advisors, colonists
    );
    UI.assignBaseSetup(base, bastion.position(null));
    //
    //  Establish starting positions for the locals.  (We actually do this last
    //  largely for balance reasons, though strictly speaking the locals would
    //  of course be there first.)
    establishLocals(world);
    //
    //  And finally, establish any other faction-specific special FX...
    expedition.backing().applyEffectsOnLanding(expedition, base, world);
  }
  
  
  protected void afterCreation() {
    //saveProgress(false);
    world().readyAfterPopulation();
  }
  


  /**  Private helper methods-
    */
  protected Human ruler(Base base) {
    return (Human) expedition.leader();
  }
  
  
  protected List <Human> advisors(Base base, Actor ruler) {
    return (List) expedition.advisors();
  }
  
  
  protected List <Human> colonists(Base base) {
    return (List) expedition.colonists();
  }
  
  
  protected Bastion establishBastion(
    final Stage world, Base base,
    Human ruler, List <Human> advisors, List <Human> colonists
  ) {
    //
    //  First of all, we attempt to establish the Bastion itself and some
    //  peripheral defences-
    final Bastion bastion = new Bastion(base);
    if (ruler != null) advisors.add(ruler);
    base.assignRuler(ruler);
    base.setup.doPlacementsFor(bastion);
    if (! bastion.inWorld()) I.complain("BASTION COULD NOT ENTER WORLD!");
    //
    //  We clear away any structures that might have conflicted with the
    //  bastion, along with their inhabitants-
    for (Venue v : world.claims.venuesConflicting(bastion)) {
      for (Actor a : v.staff.lodgers()) a.exitWorld();
      for (Actor a : v.staff.workers()) a.exitWorld();
      v.exitWorld();
    }
    //
    //  Once that's done, we can draw a curtain wall:
    
    //  TODO:  INTRODUCE ESTABLISHMENT FOR OTHER STRUCTURES.  ...But walls
    //  should probably still go first.
    
    final int estateSize = expedition.titleGranted();
    if (estateSize >= 0) {
      final int wallSize = WALL_SIZES[estateSize] - bastion.blueprint.size;
      final Box2D enclosed = new Box2D(bastion.footprint());
      enclosed.incWide(wallSize);
      enclosed.incHigh(wallSize);
      //enclosed.incX(0 - wallSize / 2);
      //enclosed.incY(0 - 2           );
      final Venue wall[] = SiteUtils.placeAroundPerimeter(
        ShieldWall.BLUEPRINT, bastion, enclosed, base, true
      );
      for (Venue v : wall) ((ShieldWall) v).updateFacing(true);
      final float fogBound = bastion.areaClaimed().maxSide() * Nums.ROOT2 / 2;
      base.intelMap.liftFogAround(bastion, fogBound);
    }
    
    //
    //  Then introduce personnel-
    for (Actor a : advisors) {
      a.assignBase(base);
      a.mind.setHome(bastion);
      a.mind.setWork(bastion);
      a.enterWorldAt(bastion, world);
    }
    for (Actor a : colonists) {
      a.assignBase(base);
      a.enterWorldAt(bastion, world);
      a.goAboard(bastion, world);
    }
    base.setup.establishRelations(bastion.staff.lodgers());
    //
    //  TODO:  Vary this based on starting House!
    bastion.updateAsScheduled(0, false);
    for (Item i : bastion.stocks.shortages()) {
      bastion.stocks.addItem(i);
    }
    return bastion;
  }
  
  
  protected void establishLocals(Stage world) {
    
    //  TODO:  Allow for natives as well?
    final Species nesting[] = location.nativeSpecies();
    
    int maxRuins = expedition.titleGranted() - 2;
    for (Species s : nesting) if (s.type == Species.Type.ARTILECT) maxRuins++;
    
    final Batch <Venue> ruins = Base.artilects(world).setup.doPlacementsFor(
      Ruins.VENUE_BLUEPRINTS[0], maxRuins
    );
    Base.artilects(world).setup.fillVacancies(ruins, true);
    
    NestUtils.populateFauna(world, 1, nesting);
  }
  
  
  
  /**  Update methods for when off-stage:
    */
  public void updateOffstage() {
  }
  
  
  
  /**  Rendering, debug and interface methods-
    */
  public void describeHook(Description d) {
  }
}














