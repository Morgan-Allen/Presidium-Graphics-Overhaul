

package stratos.game.politic;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.economic.Venue;
import stratos.game.plans.Audit;
import stratos.user.BaseUI;
import stratos.util.*;



//  TODO:  These should mediate trade & defence dynamics.
//  Vassal.  Liege.  Alliance.
//  Vendetta.  Rebel.  Uprising.
//  Closed.  Neutral.  Trading.

//  TODO:  Can this be applied to abstract bases as well?  Yeah, it would have
//         to.
//  TODO:  Merge this with the Reputation class!

//  TODO:  Modify relations between bases depending on the average relations
//         of their members?  Or have the ruler set that explicitly?


public class BaseRelations {
  
  
  
  private static boolean
    verbose = true;
  
  final Base base;
  final Table <Accountable, Relation>
    baseRelations = new Table();
  private int
    population;
  private float
    communitySpirit   = 1.0f,
    alertLevel        = 0.5f,
    crimeLevel        = 0.0f,
    averageMood       = 0.5f,
    propertyValues    = 0.0f,
    creditCirculation = 0.0f;
  
  
  public BaseRelations(Base base) {
    this.base = base;
  }
  
  
  public void loadState(Session s) throws Exception {
    for (int n = s.loadInt(); n-- > 0;) {
      final Relation r = Relation.loadFrom(s);
      baseRelations.put(r.subject, r);
    }
    population        = s.loadInt  ();
    communitySpirit   = s.loadFloat();
    alertLevel        = s.loadFloat();
    crimeLevel        = s.loadFloat();
    averageMood       = s.loadFloat();
    propertyValues    = s.loadFloat();
    creditCirculation = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(baseRelations.size());
    for (Relation r : baseRelations.values()) {
      Relation.saveTo(s, r);
    }
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
  
  
  
  /**  Setting up relationships between bases and subjects-
    */
  public void setRelation(Base other, float attitude, boolean symmetric) {    
    final Relation r = new Relation(base, other, attitude, -1);
    baseRelations.put(other, r);
    if (symmetric) other.relations.setRelation(base, attitude, false);
  }
  
  
  public float relationWith(Base other) {
    if (other == null) return 0;
    if (other == base) return 1;
    final Relation r = baseRelations.get(other);
    
    if (r == null) {
      final float initR = Setting.defaultRelations(base, other);
      setRelation(other, initR, false);
      final Relation n = baseRelations.get(other);
      return n.value();
    }
    return r.value();
  }
  
  
  public boolean isEnemy(Mobile citizen) {
    final Relation r = baseRelations.get(citizen.base());
    return r != null && r.value() < 0;
  }
  
  
  //  TODO:  Implement this (merge with BaseProfiles class?)
  /*
  public boolean setRelation(Mobile citizen, float value) {
    baseRelations.put(arg0, arg1)
  }
  //*/
  
  
  
  /**  Performing regular updates-
    */
  public void updateRelations() {
    
    //I.say("\nUPDATING BASE RELATIONS");
    
    final Tile t = base.world.tileAt(0, 0);
    int numResidents  = 0;
    averageMood       = 0.5f;
    propertyValues    = 0;
    creditCirculation = base.finance.credits();
    
    //  Compute overall credits in circulation, so that adjustments to money
    //  supply can be made by your auditors.
    for (Object o : base.world.presences.matchesNear(this, t, -1)) {
      if (! (o instanceof Venue)) continue;
      final Venue v = (Venue) o;
      propertyValues += Audit.propertyValue(v);
      creditCirculation += v.stocks.credits();
      
      for (Actor resident : v.staff.residents()) {
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




