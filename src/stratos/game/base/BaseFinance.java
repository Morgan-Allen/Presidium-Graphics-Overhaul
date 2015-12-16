/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.user.*;
import stratos.util.*;



public class BaseFinance {
  
  
  private static boolean
    verboseSummary = false;
  
  final static int
    EVAL_PERIOD   = Stage.STANDARD_DAY_LENGTH,
    MAX_RECORDS   = Stage.DAYS_PER_WEEK,
    TOTALS_PERIOD = -1,
    INIT_PERIOD   =  0;
  
  
  final private static Index <Source> SOURCES = new Index <Source> ();
  
  final public static class Source extends Index.Entry {
    
    final public String name;
    final private int index;
    
    private Source (int index, String name) {
      super(SOURCES, ""+index);
      this.name  = name ;
      this.index = index;
    }
    
    public String toString() { return name; }
  }
  
  final public static Source
    SOURCE_IMPORTS  = new Source(0 , "Imports"          ),
    SOURCE_EXPORTS  = new Source(1 , "Exports"          ),
    SOURCE_BIZ_OUT  = new Source(2 , "Business Expenses"),
    SOURCE_BIZ_IN   = new Source(3 , "Business Income"  ),
    SOURCE_CORRUPT  = new Source(4 , "Corruption"       ),
    SOURCE_REWARDS  = new Source(5 , "Rewards"          ),
    
    SOURCE_HIRING   = new Source(6 , "Hiring"           ),
    SOURCE_WAGES    = new Source(7 , "Wages"            ),
    SOURCE_TAXES    = new Source(8 , "Taxation"         ),
    
    SOURCE_INSTALL  = new Source(9 , "Installation"     ),
    SOURCE_REPAIRS  = new Source(10, "Repairs"          ),
    SOURCE_UPGRADE  = new Source(11, "Upgrades"         ),
    SOURCE_SALVAGE  = new Source(12, "Salvage"          ),
    
    SOURCE_LENDING  = new Source(13, "Lending"          ),
    SOURCE_INTEREST = new Source(14, "Interest"         ),
    
    ALL_SOURCES[] = SOURCES.allEntries(Source.class);
  
  
  final Base base;
  float credits = 0, interest = 0;
  private float lastUpdate = -1;
  
  static class CashRecord {
    private int period;
    float income[] = new float[ALL_SOURCES.length];
    float outlay[] = new float[ALL_SOURCES.length];
    Tally <Property> perVenue = new Tally();
    int balanceCents = 0;  //100x actual balance, (int for precision.)
  }
  
  final List <CashRecord> records = new List <CashRecord> ();
  private CashRecord recent, totals;
  
  
  public BaseFinance(Base base) {
    this.base = base;
  }
  
  
  public void loadState(Session s) throws Exception {
    credits    = s.loadFloat();
    interest   = s.loadFloat();
    lastUpdate = s.loadFloat();
    
    for (int n = s.loadInt(); n-- > 0;) {
      final CashRecord record = new CashRecord();
      record.period = s.loadInt();
      s.loadFloatArray(record.income);
      s.loadFloatArray(record.outlay);
      record.balanceCents = s.loadInt();
      records.add(record);
    }
    totals = records.first();
    recent = records.last ();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveFloat(credits   );
    s.saveFloat(interest  );
    s.saveFloat(lastUpdate);
    
    s.saveInt(records.size());
    for (CashRecord record : records) {
      s.saveInt(record.period);
      s.saveFloatArray(record.income);
      s.saveFloatArray(record.outlay);
      s.saveInt(record.balanceCents);
    }
  }
  
  
  private void initRecords() {
    if (records.size() == 0) {
      
      totals = new CashRecord();
      totals.period = TOTALS_PERIOD;
      records.addFirst(totals);
      
      recent = new CashRecord();
      recent.period = INIT_PERIOD;
      records.addLast(recent);
    }
  }
  
  
  
  /**  Basic queries and access-
    */
  public void setInitialFunding(int credits, float interest) {
    this.credits  = credits ;
    this.interest = interest;
  }
  

  public int credits() {
    return (int) credits;
  }
  
  
  public boolean hasCredits(float sum) {
    return credits >= sum;
  }
  
  
  public float totalOutlay(Source key) {
    initRecords();
    return totals.outlay[key.index];
  }
  
  
  public float totalIncome(Source key) {
    initRecords();
    return totals.income[key.index];
  }
  
  
  public float recentOutlay(Source key) {
    initRecords();
    return recent.outlay[key.index];
  }
  
  
  public float recentIncome(Source key) {
    initRecords();
    return recent.income[key.index];
  }
  
  
  public float recentBalance() {
    initRecords();
    return periodBalance(recent.period);
  }
  
  
  public float totalBalance() {
    initRecords();
    return totals.balanceCents / 100f;
  }
  
  
  public float periodBalance(int ID) {
    final CashRecord record = forPeriod(ID);
    if (record == null) return 0;
    return record.balanceCents / 100f;
  }
  
  
  public boolean hasPeriodRecord(int ID) {
    return forPeriod(ID) != null;
  }
  
  
  private CashRecord forPeriod(int ID) {
    initRecords();
    for (CashRecord r : records) if (r.period != TOTALS_PERIOD) {
      if (r.period == ID) return r;
    }
    return totals;
  }
  
  
  public float periodOutlay(Source key, int ID) {
    final CashRecord record = forPeriod(ID);
    return record == null ? 0 : record.outlay[key.index];
  }
  
  
  public float periodIncome(Source key, int ID) {
    final CashRecord record = forPeriod(ID);
    return record == null ? 0 : record.income[key.index];
  }
  
  
  public Batch <Integer> periodIDs() {
    final Batch <Integer> all = new Batch <Integer> ();
    for (CashRecord record : records) if (record.period != TOTALS_PERIOD) {
      all.add(record.period);
    }
    return all;
  }
  
  
  public Tally <Property> venueBalances(int periodID) {
    CashRecord record = forPeriod(periodID);
    return record.perVenue;
  }
  
  
  
  /**  Updates and modifications-
    */
  public void incCredits(float inc, Source source) {
    initRecords();
    credits += inc;
    if (source == null) return;
    
    final int cents = (int) (inc * 100);
    recent.balanceCents += cents;
    totals.balanceCents += cents;
    if (inc >= 0) {
      recent.income[source.index] += inc;
      totals.income[source.index] += inc;
    }
    else {
      recent.outlay[source.index] += inc;
      totals.outlay[source.index] += inc;
    }
  }
  
  
  public void recordVenueBalances(Tally <Property> balances) {
    initRecords();
    for (Property venue : balances.keys()) {
      final float balance = balances.valueFor(venue);
      recent.perVenue.add(balance, venue);
      totals.perVenue.add(balance, venue);
    }
  }
  
  
  public void updateFinances() {
    //
    //  A few basic setup checks...
    if (GameSettings.cashFree && credits < 5000) {
      credits += 1000;
    }
    initRecords();
    //
    //  Check to see whether a new record-period has begun, and exit if not.
    final float time = base.world.currentTime();
    if (lastUpdate == -1) lastUpdate = time;
    final int
      newPeriod = (int) (time       / EVAL_PERIOD),
      oldPeriod = (int) (lastUpdate / EVAL_PERIOD);
    if (newPeriod == oldPeriod) return;
    //
    //  If so, create a new cash record, and add it to the top of the stack.
    final CashRecord newRecord = new CashRecord();
    newRecord.period = newPeriod;
    records.addLast(recent = newRecord);
    lastUpdate = time;
    //
    //  If we've exceeded the maximum record size (bearing in mind the first is
    //  always used for storing totals,) then delete the oldest kept.
    if (records.size() > MAX_RECORDS + 1) {
      final CashRecord oldest = records.atIndex(1);
      records.remove(oldest);
    }
  }
}











