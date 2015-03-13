/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.politic;
import stratos.game.common.*;
import stratos.user.BaseUI;
import stratos.util.*;



//  TODO:  Consider building up finance reports that go back week after week
//  for up to a year.


public class BaseFinance {
  
  
  private static boolean
    verboseSummary = false;
  
  final static int
    EVAL_PERIOD = Stage.STANDARD_WEEK_LENGTH;
  
  final public static String
    
    SOURCE_IMPORTS  = "Imports"     ,
    SOURCE_EXPORTS  = "Exports"     ,
    
    SOURCE_HIRING   = "Hiring"      ,
    SOURCE_WAGES    = "Wages"       ,
    SOURCE_TAXES    = "Taxation"    ,
    
    SOURCE_CORRUPT  = "Embezzlement"     ,
    SOURCE_BIZ_OUT  = "Business Expenses",
    SOURCE_BIZ_IN   = "Business Income"  ,
    
    SOURCE_INSTALL  = "Installation",
    SOURCE_REPAIRS  = "Repairs"     ,
    SOURCE_SALVAGE  = "Salvage"     ,
    
    SOURCE_REWARDS  = "Rewards"     ,
    SOURCE_CHARITY  = "Largesse"    ,
    SOURCE_INTEREST = "Interest"    ;
  
  
  final Base base;
  float credits = 0, interest = 0;
  
  private float lastUpdate = -1;
  final Tally <String>
    weekOutlay  = new Tally <String> (),
    totalOutlay = new Tally <String> (),
    weekIncome  = new Tally <String> (),
    totalIncome = new Tally <String> ();
  
  
  public BaseFinance(Base base) {
    this.base = base;
  }
  
  
  public void loadState(Session s) throws Exception {
    credits  = s.loadFloat();
    interest = s.loadFloat();
    
    lastUpdate = s.loadFloat();
    
    for (int n = s.loadInt(); n-- > 0;) {
      final String key = s.loadString();
      weekIncome .set(key, s.loadFloat());
      totalIncome.set(key, s.loadFloat());
    }
    for (int n = s.loadInt(); n-- > 0;) {
      final String key = s.loadString();
      weekOutlay .set(key, s.loadFloat());
      totalOutlay.set(key, s.loadFloat());
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveFloat(credits );
    s.saveFloat(interest);
    
    s.saveFloat(lastUpdate);
    
    s.saveInt(weekIncome.size());
    for (String key : weekIncome.keys()) {
      s.saveString(key);
      s.saveFloat(weekIncome .valueFor(key));
      s.saveFloat(totalIncome.valueFor(key));
    }
    s.saveInt(weekOutlay.size());
    for (String key : weekOutlay.keys()) {
      s.saveString(key);
      s.saveFloat(weekOutlay .valueFor(key));
      s.saveFloat(totalOutlay.valueFor(key));
    }
  }
  
  
  public void incCredits(float inc, String source) {
    credits += inc;
    if (inc >= 0) {
      weekIncome .add(inc, source);
      totalIncome.add(inc, source);
    }
    else {
      weekOutlay .add(inc, source);
      totalOutlay.add(inc, source);
    }
  }
  

  public int credits() {
    return (int) credits;
  }
  
  
  public boolean hasCredits(float sum) {
    return credits >= sum;
  }
  
  
  public void setInterestPaid(float paid) {
    this.interest = paid;
  }
  
  
  public void updateFinances(int interval) {
    final float time = base.world.currentTime();
    if (lastUpdate == -1) lastUpdate = time;
    final int
      newWeek = (int) (time       / EVAL_PERIOD),
      oldWeek = (int) (lastUpdate / EVAL_PERIOD);
    
    if (newWeek == oldWeek) return;
    else lastUpdate = time;
    weekIncome.clear();
    weekOutlay.clear();
  }
  
  
  public void describeTo(Description d) {
    d.append("FINANCE REPORT FOR "+base);
    int sumTI = 0, sumWI = 0, sumTO = 0, sumWO = 0, sumTB = 0, sumWB = 0;
    
    d.append("\n\n  Income sources: (week/total)");
    for (String key : totalIncome.keys()) {
      final int
        week  = (int) weekIncome .valueFor(key),
        total = (int) totalIncome.valueFor(key);
      d.append("\n    "+key+" "+week+"/"+total);
      sumWI += week ;
      sumTI += total;
    }
    d.append("\n    Total income: "+sumWI+"/"+sumTI);
    
    d.append("\n\n  Outlay sources: (week/total)");
    for (String key : totalOutlay.keys()) {
      final int
        week  = 0 - (int) weekOutlay .valueFor(key),
        total = 0 - (int) totalOutlay.valueFor(key);
      d.append("\n    "+key+" "+week+"/"+total);
      sumWO += week ;
      sumTO += total;
    }
    d.append("\n    Total outlays: "+sumWO+"/"+sumTO);
    
    sumTB = sumTI - sumTO;
    sumWB = sumWI - sumWO;
    d.append("\n\n  Balance: "+sumWB+"/"+sumTB);
    d.append("\n  Current credit: "+(int) credits);
  }
}










