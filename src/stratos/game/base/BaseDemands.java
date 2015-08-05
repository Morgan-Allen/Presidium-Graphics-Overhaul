/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import static stratos.game.economic.Economy.PLASTICS;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.util.*;
import stratos.user.BaseUI;

import java.util.Map.Entry;



public class BaseDemands {
  
  
  /**  Data fields, construction, setup and save/load methods-
    */
  private static boolean
    verbose = false;
  private static Object
    reportKey = Economy.SERVICE_HOUSING;
  
  final static int
    DEFAULT_PATCH_SIZE  = Stage.ZONE_SIZE * 2,
    DEFAULT_TIME_PERIOD = Stage.STANDARD_DAY_LENGTH;
  
  final Stage world;
  final Base base;
  final int size, patchSize, timePeriod;
  
  final Table <Object, BlurMap>
    supply = new Table <Object, BlurMap> (),
    demand = new Table <Object, BlurMap> ();
  
  private Table allTables[] = { supply, demand };
  private Vec3D temp = new Vec3D();
  
  
  
  public BaseDemands(Base base, Stage world) {
    this.world      = world;
    this.base       = base;
    this.size       = world.size;
    this.patchSize  = DEFAULT_PATCH_SIZE;
    this.timePeriod = DEFAULT_TIME_PERIOD;
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
  }
  
  
  public void saveState(Session s) throws Exception {
    
    for (Table <Object, BlurMap> table : allTables) {
      s.saveInt(table.size());
      for (Entry <Object, BlurMap> entry : table.entrySet()) {
        s.saveKey(entry.getKey());
        entry.getValue().saveState(s);
      }
    }
  }
  
  
  private BlurMap mapForKeyFrom(
    Table <Object, BlurMap> table, Object key
  ) {
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
  
  
  
  /**  Updates and modifications-
    */
  public void updateAllMaps(int period) {
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



