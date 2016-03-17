/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.maps.*;
import stratos.game.verse.*;
import stratos.util.*;
import stratos.user.*;

import static stratos.game.craft.Economy.*;

import java.util.Map.*;



public class BaseDemands {
  
  
  /**  Data fields, construction, setup and save/load methods-
    */
  private static boolean
    verbose        = false,
    demandsVerbose = false,
    extraVerbose   = false;
  private static Object
    reportKey = Economy.SERVICE_HOUSING;
  
  final static int
    DEFAULT_PATCH_SIZE     = Stage.ZONE_SIZE * 2,
    DEFAULT_TIME_PERIOD    = Stage.STANDARD_DAY_LENGTH,
    DEMAND_UPDATE_INTERVAL = 10;
  final public static float
    BASE_SALE_MARGIN = 1.25f,
    NO_DOCK_MARGIN   = 1.25f,
    BASE_EXPORT_DIV  = 1.50f,
    BASE_IMPORT_MUL  = 1.75f,
    SMUGGLE_MARGIN   = 2.00f;
  
  final Base base;
  private int size = -1, patchSize, timePeriod;
  
  final Table <Object, BlurMap>
    supply = new Table <Object, BlurMap> (),
    demand = new Table <Object, BlurMap> ();
  
  private Table allTables[] = { supply, demand };
  private Vec3D temp = new Vec3D();
  
  private static class Demands {
    Traded type;
    float supply, demand;
    float dailyCons, dailyProd;
    float importDemand, exportSupply;
    boolean allowImport = false, allowExport = true;
  }
  private Table <Traded, Demands> allDemands = new Table();
  
  
  
  public BaseDemands(Base base) {
    this.base = base;
    clearDemands();
  }
  
  
  public void loadState(Session s) throws Exception {
    
    for (Table <Object, BlurMap> table : allTables) {
      for (int n = s.loadInt(); n-- > 0;) {
        final Object key = s.loadkey();
        final BlurMap map = mapForKeyFrom(table, key);
        map.loadState(s);
        table.put(key, map);
      }
    }
    
    for (Traded type : ALL_MATERIALS) {
      Demands        d = new Demands();
      d.type         = type;
      d.demand       = s.loadFloat();
      d.supply       = s.loadFloat();
      d.dailyCons    = s.loadFloat();
      d.dailyProd    = s.loadFloat();
      d.importDemand = s.loadFloat();
      d.exportSupply = s.loadFloat();
      d.allowImport  = s.loadBool ();
      d.allowExport  = s.loadBool ();
      allDemands.put(type, d);
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    
    for (Table <Object, BlurMap> table : allTables) {
      s.saveInt(table.size());
      for (Entry <Object, BlurMap> entry : table.entrySet()) {
        s.saveKey(entry.getKey());
        entry.getValue().saveState(s);
      }
    }
    
    for (Traded type : ALL_MATERIALS) {
      Demands d = allDemands.get(type);
      s.saveFloat(d.demand      );
      s.saveFloat(d.supply      );
      s.saveFloat(d.dailyCons   );
      s.saveFloat(d.dailyProd   );
      s.saveFloat(d.importDemand);
      s.saveFloat(d.exportSupply);
      s.saveBool (d.allowImport );
      s.saveBool (d.allowExport );
    }
  }
  
  
  private void clearDemands() {
    for (Traded type : ALL_MATERIALS) {
      Demands d = allDemands.get(type);
      if (d == null) d = new Demands();
      d.type = type;
      d.supply       = d.demand       = 0;
      d.exportSupply = d.importDemand = 0;
      d.dailyCons    = d.dailyProd    = 0;
      allDemands.put(type, d);
    }
  }
  
  
  private BlurMap mapForKeyFrom(
    Table <Object, BlurMap> table, Object key
  ) {
    //
    //  If needed, we extract size information from the world.
    if (size == -1) {
      this.size = base.world.size;
      this.patchSize  = DEFAULT_PATCH_SIZE;
      this.timePeriod = DEFAULT_TIME_PERIOD;
    }
    //
    //  For the sake of rapid access, we permit direct access to any maps that
    //  have been manufactured earlier by this demand-set:
    if (key instanceof BlurMap) {
      final BlurMap map = (BlurMap) key;
      if (map.parent == table) return map;
      else return mapForKeyFrom(table, map.key);
    }
    //
    //  Otherwise, we check to see if the map has been cached, and initialise a
    //  fresh map if absent:
    BlurMap map = table.get(key);
    if (map != null) return map;
    
    if (! Session.isValidKey(key)) {
      I.complain("INVALID MAP KEY: "+key);
      return null;
    }
    
    table.put(key, map = new BlurMap(size, patchSize, timePeriod, table, key));
    return map;
  }
  
  
  public BlurMap mapForSupply(Object key) {
    return mapForKeyFrom(supply, key);
  }
  
  
  public BlurMap mapForDemand(Object key) {
    return mapForKeyFrom(demand, key);
  }
  
  
  /**  Assessing supply and demand associated with goods-
    */
  private void summariseDemandAndPrices(int numUpdates) {
    if ((numUpdates % DEMAND_UPDATE_INTERVAL) != 0) return;
    final boolean report = base == BaseUI.currentPlayed() && demandsVerbose;
    if (report) I.say("\nSUMMARISING DEMAND FOR "+base);
    //
    //  Firstly, we summarise domestic supply and demand for all the major
    //  commodities-
    clearDemands();
    
    for (Object o : base.world.presences.matchesNear(base, null, -1)) {
      final Venue venue = (Venue) o;
      if (venue.blueprint.isFixture()) continue;
      final int tier = venue.owningTier();
      
      final boolean trader = Visit.arrayIncludes(
        venue.services(), SERVICE_COMMERCE
      );
      for (Traded type : venue.stocks.demanded()) {
        final Demands d = allDemands.get(type);
        if (d == null) continue;
        final float
          amount      = venue.stocks.amountOf        (type),
          consumption = venue.stocks.consumption     (type),
          production  = venue.stocks.production      (type),
          consPD      = venue.stocks.dailyConsumption(type),
          prodPD      = venue.stocks.dailyProduction (type)
        ;
        if (report) {
          I.say("  "+venue+" "+type+" (tier: "+tier+")");
          I.say("    Amount:          "+amount+", trader: "+trader);
          I.say("    Produce/Consume: "+production+"/"+consumption);
          I.say("    Per day:         "+prodPD    +"/"+consPD     );
        }
        if (trader) {
          d.exportSupply += Nums.min(production, amount);
          d.importDemand += Nums.max(0, consumption    );
        }
        else {
          d.supply += Nums.max(0, amount     );
          d.demand += Nums.max(0, consumption);
          d.dailyCons += consPD;
          d.dailyProd += prodPD;
        }
      }
    }
    
    /*
    final Series <SectorBase> partners = base.visits.partners();
    final Sector homeworld = base.visits.homeworld();
    //
    //  Then, we tally up average supply and demand for goods offworld.
    for (Traded type : ALL_MATERIALS) {
      float
        basePrice = type.defaultPrice(),
        importMul = 2, exportDiv = 2,
        avgDemand = 0, homeBonus = 0;
      for (SectorBase partner : partners) {
        if (Visit.arrayIncludes(partner.made()  , type)) {
          avgDemand -= partner.supplyLevel(type);
        }
        if (Visit.arrayIncludes(partner.needed(), type)) {
          avgDemand += partner.demandLevel(type);
        }
        if (partner.location == homeworld) homeBonus++;
      }
      //
      //  Goods that are in demand offworld are more expensive to import but
      //  more profitable to export, and vice versa for goods in abundance.
      //  Small settlements dependent on their homeworld also get price
      //  subsidies.
      if (partners.empty()) {
        importMul += 1;
        exportDiv += 1;
      }
      else {
        avgDemand /= partners.size();
        homeBonus *= base.ratings.communitySpirit() / partners.size();
        importMul += (avgDemand - homeBonus) / 2;
        exportDiv -= (avgDemand + homeBonus) / 2;
      }
      importMul *= BASE_IMPORT_MUL / 2;
      exportDiv *= BASE_EXPORT_DIV / 2;
      
      final Demands d = allDemands.get(type);
      importPrices.put(type, basePrice * importMul);
      exportPrices.put(type, basePrice / exportDiv);
    }
    //
    //  Report as necessary and return...
    if (report) {
      I.say("\nTotal export-supply: ");
      for (Traded t : exportSupply.keys()) {
        I.say("  "+t+" "+exportSupply.valueFor(t));
      }
      I.say("\nTotal import-demand: ");
      for (Traded t : importDemand.keys()) {
        I.say("  "+t+" "+importDemand.valueFor(t));
      }
    }
    //*/
  }
  
  
  public float primarySupply(Traded type) {
    final Demands d = allDemands.get(type);
    return d == null ? 0 : d.supply;
  }
  
  
  public float primaryDemand(Traded type) {
    final Demands d = allDemands.get(type);
    return d == null ? 0 : d.demand;
  }
  
  
  public float primaryShortage(Traded type) {
    float demand = primaryDemand(type), supply = primarySupply(type);
    if (demand == 0) return 0;
    return (demand - supply) / demand;
  }
  
  
  public float dailyConsumption(Traded type) {
    final Demands d = allDemands.get(type);
    return d == null ? 0 : d.dailyCons;
  }
  
  
  public float dailyProduction(Traded type) {
    final Demands d = allDemands.get(type);
    return d == null ? 0 : d.dailyProd;
  }
  
  
  public float importDemand(Traded type, boolean allowed) {
    final Demands d = allDemands.get(type);
    if (d == null || (allowed && ! d.allowImport)) return 0;
    return d.importDemand;
  }
  
  
  public float exportSupply(Traded type, boolean allowed) {
    final Demands d = allDemands.get(type);
    if (d == null || (allowed && ! d.allowExport)) return 0;
    return d.exportSupply;
  }
  
  
  public float tradingShortage(Traded type, boolean allowed) {
    final Demands d = allDemands.get(type);
    if (d == null) return 0;
    float demand = d.importDemand, supply = d.exportSupply;
    if (allowed && ! d.allowImport) demand = 0;
    if (allowed && ! d.allowExport) supply = 0;
    if (demand == 0) return 0;
    return (demand - supply) / demand;
  }
  
  
  public float importPrice(Traded type) {
    final Float price = null;// importPrices.get(type);
    if (price == null) return type.defaultPrice() * BASE_SALE_MARGIN;
    return price;
  }
  
  
  public float exportPrice(Traded type) {
    final Float price = null;// exportPrices.get(type);
    if (price == null) return type.defaultPrice() / BASE_SALE_MARGIN;
    return price;
  }
  
  
  public Series <Traded> exportsAvailable() {
    final Batch <Traded> all = new Batch();
    for (Traded t : ALL_MATERIALS) {
      final Demands d = allDemands.get(t);
      if (d.exportSupply > 0 && d.allowExport) all.add(t);
    }
    return all;
  }
  
  
  public Series <Traded> importsAvailable() {
    final Batch <Traded> all = new Batch();
    for (Traded t : ALL_MATERIALS) {
      final Demands d = allDemands.get(t);
      if (d.importDemand > 0 && d.allowImport) all.add(t);
    }
    return all;
  }
  
  
  public boolean allowsImport(Traded type) {
    final Demands d = allDemands.get(type);
    return d != null && d.allowImport;
  }
  
  
  public boolean allowsExport(Traded type) {
    final Demands d = allDemands.get(type);
    return d != null && d.allowExport;
  }
  
  
  
  /**  Updates and modifications-
    */
  public void updateAllMaps(int period, int numUpdates) {
    final boolean report = verbose && BaseUI.currentPlayed() == base;
    if (report) {
      I.say("\nUPDATING BASE DEMANDS FOR "+I.tagHash(base));
    }
    
    for (BlurMap map : supply.values()) {
      map.updateAllValues(period);
      if (report) {
        I.say("  Global supply for "+map.key+" is: "+map.globalValue());
      }
    }
    for (BlurMap map : demand.values()) {
      map.updateAllValues(period);
      if (report) {
        I.say("  Global demand for "+map.key+" is: "+map.globalValue());
      }
    }
    
    if (report) for (Object o : supply.keySet()) {
      final float shortage = globalShortage(o, true);
      I.say("  Relative shortage for "+o+" is "+shortage);
    }

    summariseDemandAndPrices(numUpdates);
  }
  
  
  public void setImportsAllowed(Traded type, boolean allow) {
    final Demands d = allDemands.get(type);
    d.allowImport = allow;
  }
  
  
  public void setExportsAllowed(Traded type, boolean allow) {
    final Demands d = allDemands.get(type);
    d.allowExport = allow;
  }
  
  
  
  /**  Imposes a supply-signal on the map.  (The period is used to 'dilute' the
    *  signal based on how frequently or slowly updates occur, and thus allow
    *  for gradual introduction.  Use a -1 value to bypass this.)
    */
  public void impingeSupply(
    Object key, float amount, float period, Target at
  ) {
    if (at == null || amount <= 0) return;
    final boolean report = verbose && key == reportKey;
    if (report) {
      I.say("\nImpinging supply for "+amount+" "+key);
      I.say("  At "+at+" over "+period+" seconds ");
    }
    
    final BlurMap map = mapForSupply(key);
    at.position(temp);
    if (period > 0) amount *= period / timePeriod;
    map.impingeValue(amount, (int) temp.x, (int) temp.y);
    
    if (report) {
      I.say("  Patch value:   "+map.patchValue(temp.x, temp.y));
      I.say("  Global supply: "+map.globalValue());
    }
  }
  
  
  /**  Imposes a demand-signal on the map.  (The period is used to 'dilute' the
    *  signal based on how frequently or slowly updates occur, and thus allow
    *  for gradual introduction.  Use a -1 value to bypass this.)
    */
  public void impingeDemand(
    Object key, float amount, float period, Target at
  ) {
    if (at == null || amount <= 0) return;
    final boolean report = verbose && key == reportKey;
    if (report) {
      I.say("\nImpinging demand for "+amount+" "+key);
      I.say("  At "+at+" over "+period+" seconds ");
    }

    final BlurMap map = mapForDemand(key);
    at.position(temp);
    if (period > 0) amount *= period / timePeriod;
    map.impingeValue(amount, (int) temp.x, (int) temp.y);
    
    if (report) {
      I.say("  Patch value:   "+map.patchValue(temp.x, temp.y));
      I.say("  Global supply: "+map.globalValue());
    }
  }
  
  
  
  /**  Global and localised supply-and-demand queries-
    */
  public float globalDemand(Object key) {
    return mapForDemand(key).globalValue();
  }
  
  
  public float globalSupply(Object key) {
    return mapForSupply(key).globalValue();
  }
  
  
  public float globalShortage(Object key, boolean relative) {
    final float GD = globalDemand(key), GS = globalSupply(key);
    if (relative) {
      if (GD <= 0) return 0;
      return 1 - (GS / GD);
    }
    else {
      return GD - GS;
    }
  }
  
  
  public float demandSampleFor(Target point, Object key, int period) {
    final BlurMap map = mapForDemand(key);
    point.position(temp);
    return map.sampleAsFraction(temp.x, temp.y, period);
  }
  
  
  public float demandAround(Target point, Object key, float radius) {
    final BlurMap map = mapForDemand(key);
    point.position(temp);
    float value = map.sampleAsFraction(temp.x, temp.y, -1);
    if (radius > 0) value *= (radius * radius * 4) / (patchSize * patchSize);
    return value;
  }
  
  
  public float supplySampleFor(Target point, Object key, int period) {
    final BlurMap map = mapForSupply(key);
    point.position(temp);
    return map.sampleAsFraction(temp.x, temp.y, period);
  }
  
  
  public float supplyAround(Target point, Object key, float radius) {
    final BlurMap map = mapForSupply(key);
    point.position(temp);
    float value = map.sampleAsFraction(temp.x, temp.y, -1);
    if (radius > 0) value *= (radius * radius * 4) / (patchSize * patchSize);
    return value;
  }
  
  
  public float demandSampling(Object key) {
    return mapForDemand(key).sumSampling();
  }
  
  
  public float supplySampling(Object key) {
    return mapForSupply(key).sumSampling();
  }
}



