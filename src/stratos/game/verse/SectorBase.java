/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.verse;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.plans.CombatUtils;
import stratos.user.BaseUI;
import stratos.game.actors.Backgrounds;
import stratos.game.base.*;
import stratos.util.*;



//  TODO:  Consider saving any details of the original founding Expedition (or
//         just the expedition itself!)



public class SectorBase implements Session.Saveable, Schedule.Updates {
  
  
  final public Verse universe;
  final public Sector location;

  private Faction faction;
  private Actor ruler;
  private List <Mobile> allUnits = new List();
  private float popLevel, powerLevel;
  
  private Tally <Traded> demandLevels = new Tally();
  private Traded[] needed = Economy.NO_GOODS, made = Economy.NO_GOODS;
  
  
  protected SectorBase(Verse universe, Sector init) {
    this.universe = universe;
    this.location = init;
    this.popLevel = init.population;
  }
  
  
  public SectorBase(Session s) throws Exception {
    s.cacheInstance(this);
    
    this.universe = (Verse  ) s.loadObject();
    this.location = (Sector ) s.loadObject();
    this.faction  = (Faction) s.loadObject();
    this.ruler    = (Actor  ) s.loadObject();

    for (int n = s.loadInt(); n-- > 0;) {
      Mobile m = (Mobile) s.loadObject();
      m.setBaseEntry(allUnits.addLast(m));
    }
    
    popLevel   = s.loadFloat();
    powerLevel = s.loadFloat();
    s.loadTally(demandLevels);
    needed = (Traded[]) s.loadObjectArray(Traded.class);
    made   = (Traded[]) s.loadObjectArray(Traded.class);
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(universe);
    s.saveObject(location);
    s.saveObject(faction );
    s.saveObject(ruler   );
    
    s.saveInt(allUnits.size());
    for (Mobile m : allUnits) s.saveObject(m);
    
    s.saveFloat(popLevel  );
    s.saveFloat(powerLevel);
    s.saveTally(demandLevels);
    s.saveObjectArray(needed);
    s.saveObjectArray(made  );
  }
  
  
  
  /**  Basic query/access methods-
    */
  public Actor ruler() {
    return ruler;
  }
  
  
  public void assignRuler(Actor rules) {
    this.ruler = rules;
  }
  
  
  public Faction faction() {
    return faction;
  }
  
  
  public void assignFaction(Faction f) {
    this.faction = f;
  }
  
  
  protected boolean isResident(Mobile m) {
    final ListEntry <Mobile> e = m.baseEntry();
    return e != null && e.list() == allUnits;
  }
  
  
  public float population() {
    return popLevel;
  }
  
  
  public void setPopulation(float popLevel) {
    this.popLevel = Nums.max(0, popLevel);
  }
  
  
  public float powerLevel(Faction faction) {
    if (faction != this.faction) return 0;
    return powerLevel;
  }
  
  
  public void setPowerLevel(float level) {
    this.powerLevel = Nums.max(0, level);
  }
  
  
  public boolean isPrimal() {
    return faction().primal();
  }
  
  
  public boolean isRealPlayer() {
    return BaseUI.currentPlayed() == this;
  }
  
  
  public boolean isBaseAI() {
    return ! isRealPlayer();
  }
  
  
  public boolean isWorldBase() {
    return location == universe.stageLocation();
  }
  
  
  public Base baseInWorld() {
    if (isWorldBase()) return (Base) this;
    return Base.findBase(universe.stage(), null, faction);
  }
  
  
  
  /**  Update and modification methods-
    */
  public float scheduledInterval() {
    return 1;
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    final boolean onStage = location == universe.stageLocation();
    if (instant || onStage || faction == null) return;
    
    powerLevel = popLevel / 2f;
    powerLevel *= CombatUtils.AVG_POWER * Mission.AVG_PARTY_LIMIT;
    //  TODO:  Scale this further by the preponderance of military backgrounds.
    
    for (Mobile m : allUnits) {
      final Journey.Purpose a = Journey.activityFor(m);
      if (a != null) a.whileOffworld();
    }
    
    updatePopulation();
    updateEconomy();
    
    //  NOTE:  This has to be done to ensure that all factions have a base-
    //  presence within the world (even if it goes entirely unused), because
    //  the world-bases are what trigger raids & visits.
    Stage played = universe.stage();
    if (played != null) Base.settlement(played, null, faction);
    
    //
    //  TODO:  Allow all residents a chance to apply for work elsewhere, or
    //  the faction's own missions- use that to replace candidate generation in
    //  BaseCommerce.
  }
  
  
  
  /**  Modifying internal population and modelling their rough behaviours-
    */
  public void toggleUnit(Mobile m, boolean is) {
    final Stage world = universe.stage();
    final boolean onStage = location == universe.stageLocation();
    
    final ListEntry <Mobile> e = m.baseEntry();
    final boolean belongs = e != null && e.list() == allUnits;
    
    if (e != null && e.list() != allUnits) {
      final Sector oldLoc = universe.currentSector(m);
      if (I.logEvents()) {
        I.say("\nUNIT BELONGS TO A DIFFERENT BASE: "+m+", IS AT: "+oldLoc);
      }
      if (oldLoc != null && oldLoc.base() != null) {
        oldLoc.base().toggleUnit(m, false);
      }
      toggleUnit(m, is);
      return;
    }
    
    if (is) {
      if (belongs) return;
      if (I.logEvents()) I.say("ADDING UNIT: "+m+" TO: "+location);
      if (onStage) world.presences.togglePresence(m, m.origin(), true);
      m.setBaseEntry(allUnits.addLast(m));
    }
    else {
      if (! belongs) return;
      if (I.logEvents()) I.say("REMOVING UNIT: "+m+" FROM: "+location);
      if (onStage) world.presences.togglePresence(m, m.origin(), false);
      allUnits.removeEntry(e);
      m.setBaseEntry(null);
    }
  }
  
  
  public Series <Mobile> allUnits() {
    return allUnits;
  }
  
  
  
  /**  Population-specific update methods-
    */
  protected void updatePopulation() {
    if (faction == null || faction.primal()) return;
    final Sector home = faction.startSite();
    if (home == null) return;
    
    if (ruler == null) {
      ruler = Backgrounds.KNIGHTED.sampleFor(faction);
      ruler.assignBase(baseInWorld());
      toggleUnit(ruler, true);
    }
  }
  
  
  
  protected void generateApplicants(Mission mission) {
    
  }
  
  
  
  /**  Economy-specific update methods-
    */
  //  TODO:  Ideally, this could be moved out to the BaseDemands class, and
  //         then that could be queried for the demands of both SectorBases and
  //         bases in the world.
  
  protected void updateEconomy() {
    if (faction == null || faction.primal()) return;
    final Sector home = faction.startSite();
    if (home == null) return;
    //
    //  Base goods in demand or supplied on the average of homeworld with
    //  local resources, but increase demand for finished goods based on
    //  population levels.
    demandLevels.clear();
    for (Traded t : home.goodsMade) {
      demandLevels.add(-0.5f * home.population, t);
    }
    for (Traded t : home.goodsNeeded) {
      demandLevels.add( 0.5f * home.population, t);
    }
    //
    //  (We skip local-averaging for the homeworlds themselves...)
    if (home != location) {
      for (Traded t : location.goodsMade) {
        demandLevels.add(-0.5f * (this.popLevel + 1), t);
      }
      for (Traded t : Economy.ALL_FINISHED_GOODS) {
        demandLevels.add(0.5f * this.popLevel / 2f, t);
      }
    }
    //
    //  ...Then compile and store the result.
    final Batch <Traded> needB = new Batch(), makeB = new Batch();
    for (Traded t : demandLevels.keys()) {
      final float level = demandLevels.valueFor(t);
      if (level <= -0.5f) makeB.add(t);
      if (level >   0.5f) needB.add(t);
    }
    this.needed = needB.toArray(Traded.class);
    this.made   = makeB.toArray(Traded.class);
  }
  
  
  public Traded[] needed() {
    return needed;
  }
  
  
  public Traded[] made() {
    return made;
  }
  
  
  public float demandLevel(Traded t) {
    return demandLevels.valueFor(t);
  }
  
  
  public float supplyLevel(Traded t) {
    return 0 - demandLevels.valueFor(t);
  }
  
  
  
  /**  Rendering, debug and interface methods-
    */
  public String toString() {
    //  TODO:  This needs to be more customised!
    return location.name;
  }
}





/*
public class BaseDemands {
  
  
  /**  Data fields, constants, constructors and save/load methods-
    */
  /*
  private static boolean verbose = true;
  
  final Location location;
  
  float population = 0;
  final Tally <Object>
    demand = new Tally <Object> (),
    supply = new Tally <Object> ();
  Table <Object, Venue>
    samples = new Table <Object, Venue> ();
  
  
  
  public BaseDemands(Location l) {
    this.location = l;
  }
  
  
  
  /**  Setup and query methods-
    */
/*
  public void initWithSupply(Object... args) {
    if (args.length % 2 != 0) I.say("WARNING:  Irregular argument size...");
    
    for (int i = 0; (i / 2) < (args.length / 2);) {
      final Object key = args[i++], val = args[i++];
      if      (val instanceof Float  ) supply.add((Float)   val, key);
      else if (val instanceof Integer) supply.add((Integer) val, key);
      else I.say("WARNING:  Irregular argument value: "+val);
    }
  }
  
  
  private float shortage(Object key) {
    return demand.valueFor(key) - supply.valueFor(key);
  }
  
  
  
  /**  Regular update methods-
    */
  /*
  public void update(float timeInterval) {
    if (verbose) I.say("Updating base demands...");
    updateDemands();
    updateConstruction(timeInterval);
    updateRecruitment (timeInterval);
  }
  
  
  private void updateDemands() {
    demand.clear();
    
    for (Background b : Background.INDEX) {
      final int incomeBracket = b.standing;
      final int upgradeLevel = (incomeBracket - 1) * 2;
      
      final Upgrade upgrade = HoldingUpgrades.upgradeFor(upgradeLevel);
      final Conversion conv = HoldingUpgrades.materials (upgradeLevel);
      if (upgrade == null || conv == null) continue;
      
      final float population = supply.valueFor(b);
      for (Item i : conv.raw) {
        demand.add(population * i.amount, i.type);
      }
      demand.add(population, upgrade);
    }
    
    //  TODO:  Multiple passes may be needed here to cover multiple steps in
    //  the supply chain, unless you order the conversions carefully.
    
    //
    //  Now, for each good being demanded, find conversions which produce it,
    //  and increment demand for (A) their raw materials, (B) their production
    //  facilities, and (C) their associated skills.
    for (Conversion c : Conversion.INDEX) {
      if (c.out == null || c.facility == null) continue;
      float needed = demand.valueFor(c.out.type);
      needed /= c.out.amount;
      if (needed == 0) continue;
      
      if (verbose) I.say("    Demand for "+c+" is: "+needed);
      demand.add(needed, c.facility);
      for (Item i : c.raw) demand.add(i.amount * needed, i.type);
      //for (Skill s : c.skills) demand.add(needed, s);
    }
    
    //
    //  TODO:  Two other major subsystems need to be incorporated here- trade
    //  and defence policy.
    //
    //  Finally, increment demand for particular vocations, based on the skills
    //  they afford.
    for (Background b : Background.INDEX) {
      float sumNeed = 0;
      for (Skill s : b.skills()) sumNeed += demand.valueFor(s);
      demand.add(sumNeed, b);
    }
    
    //
    //  TODO:  Weight toward certain facilities and policies based on
    //  availability of resources and personality of the base's ruling body.
    
    //
    //  TODO:  Internal demands:  Maintenance & Administration
    //
    //  TODO:  External demands:  Trade & Defence
  }
  
  
  
  /**  Dealing with construction and salvage-
    */
  /*
  private void updateConstruction(float timeInterval) {
    //
    //  First, estimate the total available labour pool and how much work needs
    //  to be done.
    float sumWork = 0;
    final VenueProfile profiles[] = VenueProfile.facilityProfiles();
    
    for (VenueProfile facility : profiles) {
      sumWork += Nums.abs(workRemaining(facility));
    }
    float totalLabour = 0;
    for (Background b : Backgrounds.ARTIFICER_CIRCLES) {
      totalLabour += supply.valueFor(b);
    }
    totalLabour += supply.valueFor(ASSEMBLY) / 10f;
    if (verbose) {
      I.say("Total facility types: "+profiles.length);
      I.say("    Construction Needed: "+sumWork);
      I.say("    Total Labour:        "+totalLabour);
    }
    
    //  TODO:  Order work on different facilities based on urgency or stage in
    //  the supply chain?
    for (VenueProfile  profile : profiles) {
      final float need = workRemaining(profile);
      float progress = need * totalLabour * timeInterval / sumWork;
      
      if (need > 0) progress = Nums.min(need, progress);
      else progress = Nums.max(need, progress);
      supply.add(progress, profile);
    }
  }
  
  
  private float workRemaining(VenueProfile profile) {
    float work = shortage(profile.baseClass);
    work *= profile.maxIntegrity / 25f;
    work *= Repairs.TIME_PER_25_HP / Stage.STANDARD_DAY_LENGTH;
    return work;
  }
  
  
  
  /**  Dealing with personnel recruitment.
    */
  /*
  private void updateRecruitment(float timeInterval) {
    
    float sumDemand = 0;
    for (Background b : Background.INDEX) {
      sumDemand += shortage(b);
    }
    float totalPool = 1; //TODO:  FIX!
    //float totalPool = location.area() * 10 * (population + 1);
    //population /= (location.area() * 10);
    
    //
    //  
    for (Background b : Background.INDEX) {
      float demand = shortage(b);
      float progress = demand * totalPool * timeInterval / sumDemand;

      if (demand > 0) progress = Nums.min(demand, progress);
      else progress = Nums.max(demand, progress);
      supply.add(progress, b);
    }
  }
  
  
  
  /**  Debugging and interface methods-
    */
  /*
  public void reportState() {
    
    I.say("\nTOTAL DEMAND: ");
    for (Object o : demand.keys()) {
      I.say("  "+o+": "+demand.valueFor(o));
    }
    I.say("\nTOTAL SUPPLY: ");
    for (Object o : supply.keys()) {
      I.say("  "+o+": "+supply.valueFor(o));
    }
    I.say("\n");
  }
  
}
//*/







