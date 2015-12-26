/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.verse;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.util.*;



//  TODO:  Consider saving any details of the original founding Expedition (or
//         just the expedition itself!)



public class SectorBase implements Session.Saveable {
  

  final public Verse universe;
  final public Sector location;
  
  private Faction claimant;
  private Expedition founding;
  final private Tally <Object> presences = new Tally();
  final private List  <Mobile> expats    = new List ();
  private float popLevel, powerLevel;
  
  
  protected SectorBase(Verse universe, Sector location) {
    this.universe = universe;
    this.location = location;
    this.popLevel = location.population;
  }
  
  
  public SectorBase(Session s) throws Exception {
    s.cacheInstance(this);
    this.universe = s.world().offworld;
    this.location = (Sector    ) s.loadObject();
    this.claimant = (Faction   ) s.loadObject();
    this.founding = (Expedition) s.loadObject();
    s.loadTally(presences);
    
    for (int n = s.loadInt(); n-- > 0;) {
      Mobile m = (Mobile) s.loadObject();
      m.setWorldEntry(expats.addLast(m));
    }
    popLevel   = s.loadFloat();
    powerLevel = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(location);
    s.saveObject(claimant);
    s.saveObject(founding);
    s.saveTally(presences);
    
    s.saveInt(expats.size());
    for (Mobile m : expats) s.saveObject(m);
    s.saveFloat(popLevel  );
    s.saveFloat(powerLevel);
  }
  
  
  public void setClaimant(Faction claimant, Expedition founding) {
    this.claimant = claimant;
    this.founding = founding;
  }

  
  
  /**  Basic query/access methods-
    */
  protected Series <Mobile> expats() {
    return expats;
  }
  
  
  protected boolean isResident(Mobile m) {
    final ListEntry <Mobile> e = m.worldEntry();
    return e != null && e.list() == expats;
  }
  
  
  public Faction faction() {
    return claimant;
  }
  
  
  public float population() {
    return popLevel;
  }
  
  
  public float powerLevel(Faction faction) {
    if (faction != claimant) return 0;
    return powerLevel;
  }
  
  
  
  /**  Update and modification methods-
    */
  protected void updateBase() {
    for (Mobile m : expats) {
      final Journey.Purpose a = Journey.activityFor(m);
      if (a != null) a.whileOffworld();
    }
    //
    //  TODO:  Allow all residents a chance to apply for work elsewhere- use
    //  that to replace candidate generation in BaseCommerce.
  }
  
  
  protected void toggleExpat(Mobile m, boolean is) {
    final ListEntry <Mobile> e = m.worldEntry();
    final boolean belongs = e != null && e.list() == expats;
    
    if (is) {
      if (belongs) return;
      m.setWorldEntry(expats.addLast(m));
    }
    else {
      if (! belongs) return;
      e.delete();
      m.setWorldEntry(null);
    }
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







