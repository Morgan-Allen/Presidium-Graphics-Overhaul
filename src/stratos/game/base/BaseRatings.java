

package stratos.game.base;
import stratos.game.common.*;
import stratos.game.craft.Venue;
import stratos.game.actors.*;
import stratos.game.plans.Audit;
import stratos.util.*;




//  TODO:  Merge this with the Reputation class...


public class BaseRatings {
  
  
  final static int UPDATE_INTERVAL = Stage.STANDARD_DAY_LENGTH / 3;
  
  private static boolean
    verbose = true;
  
  final Base base;
  private int
    population;
  private float
    communitySpirit   = 1.0f,
    alertLevel        = 0.5f,
    crimeLevel        = 0.0f,
    averageMood       = 0.5f,
    propertyValues    = 0.0f,
    creditCirculation = 0.0f;
  
  
  public BaseRatings(Base base) {
    this.base = base;
  }
  
  
  public void loadState(Session s) throws Exception {
    population        = s.loadInt  ();
    communitySpirit   = s.loadFloat();
    alertLevel        = s.loadFloat();
    crimeLevel        = s.loadFloat();
    averageMood       = s.loadFloat();
    propertyValues    = s.loadFloat();
    creditCirculation = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt  (population       );
    s.saveFloat(communitySpirit  );
    s.saveFloat(alertLevel       );
    s.saveFloat(crimeLevel       );
    s.saveFloat(averageMood      );
    s.saveFloat(propertyValues   );
    s.saveFloat(creditCirculation);
  }
  
  
  
  /**  Basic no-brainer accessor methods-
    */
  //  TODO:  Move much of these to the BaseDemands class, I suspect.
  public float propertyValues() {
    return propertyValues;
  }
  
  
  public float creditCirculation() {
    return creditCirculation;
  }
  
  
  public float communitySpirit() {
    return communitySpirit;
  }
  
  
  public float crimeLevel() {
    return crimeLevel;
  }
  
  
  public float alertLevel() {
    return alertLevel;
  }
  
  
  public int population() {
    return population;
  }
  
  
  
  /**  Performing regular updates-
    */
  public void updateRelations(int numUpdates) {
    //  TODO:  MERGE THIS WITH THE BASE-ADVICE CLASS!
    
    if (numUpdates % UPDATE_INTERVAL != 0) return;
    
    //I.say("\nUPDATING BASE RELATIONS");
    int numResidents  = 0;
    averageMood       = 0.5f;
    propertyValues    = 0;
    creditCirculation = base.finance.credits();
    
    //  TODO:  BaseCommerce should handle this?
    
    //  Compute overall credits in circulation, so that adjustments to money
    //  supply can be made by your auditors.
    for (Object o : base.world.presences.allMatches(this)) {
      final Venue v = (Venue) o;
      propertyValues += Audit.propertyValue(v);
      creditCirculation += v.stocks.allCredits();
      
      for (Actor resident : v.staff.lodgers()) {
        numResidents++;
        averageMood += resident.health.moraleLevel();
      }
    }
    
    //  Once per day, iterate across all personnel to get a sense of citizen
    //  mood, and compute community spirit.  (This declines as your settlement
    //  gets bigger and citizens become less happy.)
    averageMood /= (numResidents + 1);
    final float s = Nums.log(10, 1 + numResidents);
    final float m = averageMood / 2f;
    communitySpirit = Nums.clamp(1 + m - (s / 4), 0, 1);
    population = numResidents;
  }
}




