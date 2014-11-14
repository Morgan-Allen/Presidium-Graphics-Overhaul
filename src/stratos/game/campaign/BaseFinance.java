

package stratos.game.campaign;
import stratos.game.common.*;
import stratos.user.BaseUI;
import stratos.util.*;



public class BaseFinance {
  
  
  private static boolean
    verboseSummary = true ;
  
  final public static String
    
    SOURCE_IMPORTS  = "Imports"     ,
    SOURCE_EXPORTS  = "Exports"     ,
    SOURCE_INJECT   = "Injection"   ,
    
    SOURCE_HIRING   = "Hiring"      ,
    SOURCE_WAGES    = "Wages"       ,
    SOURCE_TAXES    = "Taxation"    ,
    
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
    outlay = new Tally <String> (),
    income = new Tally <String> ();
  
  
  public BaseFinance(Base base) {
    this.base = base;
  }
  
  
  public void loadState(Session s) throws Exception {
    credits  = s.loadFloat();
    interest = s.loadFloat();
    
    lastUpdate = s.loadFloat();
    
    for (int n = s.loadInt(); n-- > 0;) {
      final String key = s.loadString();
      final float value = s.loadFloat();
      income.set(key, value);
    }
    for (int n = s.loadInt(); n-- > 0;) {
      final String key = s.loadString();
      final float value = s.loadFloat();
      outlay.set(key, value);
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveFloat(credits );
    s.saveFloat(interest);
    
    s.saveFloat(lastUpdate);
    
    s.saveInt(income.size());
    for (String key : income.keys()) {
      s.saveString(key);
      s.saveFloat(outlay.valueFor(key));
    }
    s.saveInt(outlay.size());
    for (String key : outlay.keys()) {
      s.saveString(key);
      s.saveFloat(outlay.valueFor(key));
    }
  }
  
  
  
  public void incCredits(float inc, String source) {
    credits += inc;
    if (inc >= 0) income.add(inc, source);
    else outlay.add(inc, source);
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
    final boolean report = verboseSummary && BaseUI.current().played() == base;
    
    //final float repaid = credits * interest / 100f;
    //if (repaid > 0) incCredits(0 - repaid);
    //  TODO:  This needs to have some form of visual interface!
    
    if (report) {
      I.say("\nFINANCE REPORT FOR "+base);
      
      I.say("  Income sources:");
      for (String key : income.keys()) {
        I.say("    "+key+": "+income.valueFor(key));
      }
      
      I.say("  Outlay sources:");
      for (String key : outlay.keys()) {
        I.say("    "+key+": "+outlay.valueFor(key));
      }
    }
    
    income.clear();
    outlay.clear();
  }
}














