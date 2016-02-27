/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.verse;
import stratos.start.*;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.util.*;
import static stratos.game.verse.Faction.*;




public abstract class Verse implements Session.Saveable {
  

  final public static int
    INTENSE_GRAVITY   = -2,
    STRONG_GRAVITY    = -1,
    NORMAL_GRAVITY    =  0,
    MILD_GRAVITY      =  1,
    NOMINAL_GRAVITY   =  2,
    
    SPARSE_POPULATION =  0,
    LIGHT_POPULATION  =  1,
    MEDIUM_POPULATION =  2,
    HIGH_POPULATION   =  3,
    VAST_POPULATION   =  4;
  
  final static String GRAVITY_DESC[] = {
    "Intense", "Strong", "Normal", "Mild", "Nominal"
  };
  final static String POPULATION_DESC[] = {
    "Sparse", "Light", "Medium", "High", "Vast"
  };
  
  
  
  /**  Setup, data fields and save/load methods-
    */
  final   Sector locations[];
  private Stage  currentStage;
  private Sector stageLocation = null;
  private int    startingDate  = Stage.DEFAULT_INIT_TIME;
  final List <SectorScenario> scenarios = new List();
  
  final public VerseJourneys journeys = new VerseJourneys(this);
  final List <SectorBase> bases = new List <SectorBase> ();
  final Table <Relation, Relation> relations = new Table();
  
  
  public Verse(Sector locations[]) {
    this.locations = locations;
    
    this.initSeparations();
    this.initPolitics   ();
    
    for (Sector s : locations) {
      final SectorBase base = new SectorBase(this, s);
      bases.add(base);
      final SectorScenario scenario = s.customScenario(this);
      if (scenario != null) scenarios.add(scenario);
    }
    for (Faction f : CIVIL_FACTIONS) if (f.startSite() != null) {
      final SectorBase base = baseForSector(f.startSite());
      base.assignFaction(f);
    }
  }
  
  
  public Verse(Session s) throws Exception {
    s.cacheInstance(this);
    this.initSeparations();
    
    locations     = (Sector[]) s.loadObjectArray(Sector.class);
    currentStage  = (Stage ) s.loadObject();
    stageLocation = (Sector) s.loadObject();
    startingDate  = s.loadInt();
    s.loadObjects(scenarios);
    
    journeys.loadState(s);
    s.loadObjects(bases);
    
    relations.clear();
    for (int n = s.loadInt(); n-- > 0;) {
      final Relation r = Relation.loadFrom(s);
      relations.put(r, r);
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObjectArray(locations);
    s.saveObject(currentStage );
    s.saveObject(stageLocation);
    s.saveInt   (startingDate );
    s.saveObjects(scenarios);
    
    journeys.saveState(s);
    s.saveObjects(bases);
    
    s.saveInt(relations.size());
    for (Relation r : relations.values()) {
      Relation.saveTo(s, r);
    }
  }
  
  
  
  /**  Political setup and query methods-
    */
  protected void setRelation(
    Faction a, Faction b, float value, boolean symmetric
  ) {
    final Relation key = new Relation(a, b, value, 0);
    Relation match = relations.get(key);
    if (match == null) relations.put(key, match = key);
    match.setValue(value, 0);
    if (symmetric) setRelation(b, a, value, false);
  }
  
  
  protected void setRelations(
    Faction a, boolean symmetric, Object... tableVals
  ) {
    final Table vals = Table.make(tableVals);
    for (Object k : vals.keySet()) {
      final Object v = vals.get(k);
      if (k instanceof Sector) {
        k = ((Sector) k).startingOwner;
      }
      if (k instanceof Faction && v instanceof Float) {
        setRelation(a, (Faction) k, (Float) v, symmetric);
      }
      else I.complain("ILLEGAL RELATION TYPE: "+v+" FOR "+k);
    }
  }
  
  
  protected void setRelations(
    Faction a, float value, boolean symmetric, Faction... others
  ) {
    for (Faction k : others) {
      setRelation(a, k, value, symmetric);
    }
  }
  
  
  protected void setSeparation(
    Sector a, Sector b, int sepType, float tripTime, boolean symmetric
  ) {
    a.setSeparation(b, sepType, tripTime, symmetric);
  }
  
  
  protected void assignAsSiblings(boolean calcSeps, Sector... sectors) {
    for (Sector s : sectors) {
      if (calcSeps) s.calculateRemainingSeparations(sectors);
      s.assignSiblings(sectors);
    }
  }
  
  
  protected abstract void initSeparations();
  protected abstract void initPolitics();
  
  
  
  /**  Helper methods for setup and teardown of the local world-map:
    */
  public void assignStage(Stage stage, Sector location) {
    this.currentStage  = stage   ;
    this.stageLocation = location;
  }
  
  
  public Stage stage() {
    return currentStage;
  }
  
  
  public Sector stageLocation() {
    return stageLocation;
  }
  
  
  public void setStartingDate(int date) {
    startingDate = date;
  }
  
  
  public int startingDate() {
    return startingDate;
  }
  
  
  public void onStageDeletion(Stage world) {
    //
    //  First, take anyone/thing on a journey and remove their references to
    //  the world being deleted-
    for (Journey j : journeys.journeys) {
      for (Mobile m : j.migrants()) {
        m.removeWorldReferences(world);
      }
      if (j.transport != null) {
        j.transport.removeWorldReferences(world);
      }
    }
    //
    //  Then scrub all references to the world for each off-world unit (in
    //  order to do this safely, we need to de- and re-register them before and
    //  after.)
    for (SectorBase b : sectorBases()) {
      final Mobile units[] = b.allUnits().toArray(Mobile.class);
      for (Mobile m : units) {
        b.toggleUnit(m, false);
        m.removeWorldReferences(world);
        b.toggleUnit(m, true);
      }
    }
  }
  
  
  public SectorScenario scenarioFor(Sector match) {
    for (SectorScenario s : scenarios) if (s.location() == match) return s;
    return null;
  }
  
  
  
  /**  Physical demographics and travel methods-
    */
  public Series <SectorBase> sectorBases() {
    return bases;
  }
  
  
  public SectorBase baseForSector(Sector s) {
    //
    //  TODO:  You may want the ability to include multiple bases per sector in
    //  future...
    for (SectorBase b : bases) {
      if (b.location == s) return b;
    }
    return null;
  }
  
  
  public Sector currentSector(Object object) {
    if (object instanceof Sector) {
      return (Sector) object;
    }
    if (object instanceof Mobile) {
      final Mobile mobile = (Mobile) object;
      if (mobile.inWorld() && mobile.base().isResident(mobile)) {
        return stageLocation();
      }
      for (SectorBase base : bases) {
        if (base.isResident(mobile)) return base.location;
      }
      if (mobile instanceof Human) {
        return (Sector) ((Human) mobile).career().homeworld();
      }
    }
    if (object instanceof Target) {
      final Target target = (Target) object;
      if (target.inWorld()) return stageLocation();
    }
    return null;
  }
  
  
  public static boolean isWorldExit(
    Target point, Actor actor, Sector goes
  ) {
    //
    //  Returns whether the given point can be used to escape off-stage to a
    //  given adjacent sector-
    if (! (point instanceof EntryPoints.Portal)) return false;
    final EntryPoints.Portal exit = (EntryPoints.Portal) point;
    if (goes != null && exit.leadsTo() != goes) return false;
    return exit.allowsEntry(actor) && exit.allowsStageExit(actor);
  }
  
  
  public static boolean isWorldExit(Target point, Actor actor) {
    //
    //  Returns whether the given point can be used to escape off-stage at all.
    return isWorldExit(point, actor, null);
  }
  
  
  
  /**  Regular updates-
    */
  public void updateVerse(float time) {
    journeys.updateJourneys((int) time);
    for (SectorScenario hook : scenarios) hook.updateOffstage();
  }
}













