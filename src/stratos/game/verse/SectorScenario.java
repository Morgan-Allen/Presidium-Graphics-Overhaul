/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.verse;
import stratos.content.civic.*;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.verse.*;
import stratos.game.wild.*;
import stratos.user.*;
import stratos.util.*;
import stratos.graphics.common.*;
import stratos.start.PlayLoop;
import stratos.start.Scenario;



public class SectorScenario extends Scenario {
  

  final public static int
    MAP_SIZES [] = { 128, 128, 128 },
    WALL_SIZES[] = { 16, 20, 24 };
  
  final public Sector location;
  private Verse verse;
  private Expedition expedition;
  
  
  
  public SectorScenario(Expedition config, Verse verse, String prefix) {
    super(prefix, false);
    this.location   = config.destination();
    this.verse      = verse ;
    this.expedition = config;
  }
  
  
  protected SectorScenario(Sector location, Verse verse) {
    super(false);
    this.location = location;
    this.verse = verse;
  }
  
  
  public SectorScenario(Session s) throws Exception {
    super(s);
    this.location   = (Sector    ) s.loadObject();
    this.verse      = (Verse     ) s.loadObject();
    this.expedition = (Expedition) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(location  );
    s.saveObject(verse     );
    s.saveObject(expedition);
  }
  
  
  public void beginScenario(Expedition config, String savesPrefix) {
    this.expedition = config;
    setSavesPrefix(savesPrefix);
    skipLoading();
    PlayLoop.setupAndLoop(this);
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
    world.readyAfterPopulation();
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
    base.setup.fillVacancies(bastion, true);
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
        ShieldWall.BLUEPRINT, enclosed, base, true
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
    
    final Species nesting[] = expedition.origin().nativeSpecies();

    int maxRuins = expedition.titleGranted() - 2;
    for (Species s : nesting) if (s.type == Species.Type.ARTILECT) maxRuins++;
    
    final Batch <Venue> ruins = Base.artilects(world).setup.doPlacementsFor(
      Ruins.VENUE_BLUEPRINTS[0], maxRuins
    );
    Base.artilects(world).setup.fillVacancies(ruins, true);
    
    NestUtils.populateFauna(world, nesting);
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














