/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.util.*;



public class BaseResearch {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final public static int
    LEVEL_BANNED = -1,
    LEVEL_ALLOWS =  0,
    LEVEL_THEORY =  1,
    LEVEL_PRAXIS =  2;
  
  final public static float
    DEFAULT_RESEARCH_TIME = Stage.STANDARD_SHIFT_LENGTH * 10;
  
  
  private static class Research {
    Upgrade upgrade;
    int policyLevel;
    float actualLevel;
  }
  
  
  final Base base;
  final Table <Upgrade, Research> allResearch = new Table(100);
  final List <Upgrade> underResearch = new List();
  
  
  public BaseResearch(Base base) {
    this.base = base;
  }
  
  
  public void loadState(Session s) throws Exception {
    for (int n = s.loadInt(); n-- > 0;) {
      final Upgrade key = (Upgrade) s.loadObject();
      final Research r = new Research();
      r.upgrade     = key          ;
      r.policyLevel = s.loadInt  ();
      r.actualLevel = s.loadFloat();
      allResearch.put(key, r);
    }
    s.loadObjects(underResearch);
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(allResearch.size());
    for (Research r : allResearch.values()) {
      s.saveObject(r.upgrade    );
      s.saveInt   (r.policyLevel);
      s.saveFloat (r.actualLevel);
    }
    s.saveObjects(underResearch);
  }
  
  
  
  
  /**  Setting-queries and modifications-
    */
  private Research researchFor(Upgrade u) {
    Research match = allResearch.get(u);
    if (match != null) return match;
    match = new Research();
    match.upgrade = u;
    allResearch.put(u, match);
    return match;
  }
  
  
  private void checkUnderResearch(Research r) {
    if (r.policyLevel >= LEVEL_ALLOWS && r.actualLevel < LEVEL_THEORY) {
      underResearch.include(r.upgrade);
    }
    else {
      underResearch.remove(r.upgrade);
    }
  }
  
  
  public Series <Upgrade> underResearch() {
    return underResearch;
  }
  
  
  public void setPolicyLevel(Upgrade u, int level) {
    final Research r = researchFor(u);
    r.policyLevel = level;
    checkUnderResearch(r);
  }
  
  
  public void incResearchFor(Upgrade u, float inc) {
    final Research r = researchFor(u);
    final int oldCat = (int) r.actualLevel;
    r.actualLevel += inc;
    final int newCat = (int) r.actualLevel;
    if (oldCat != newCat) r.actualLevel = newCat;
    checkUnderResearch(r);
  }
  
  
  public void setResearchLevel(Upgrade u, int level) {
    final Research r = researchFor(u);
    r.actualLevel = level;
    r.policyLevel = level;
    checkUnderResearch(r);
  }
  
  
  public void setResearchLevel(Blueprint b, int level) {
    setResearchLevel(b.baseUpgrade(), level);
  }
  
  
  public void setBanned(Upgrade u) {
    setResearchLevel(u, LEVEL_BANNED);
  }
  
  
  public void setAllowed(Upgrade u) {
    setResearchLevel(u, LEVEL_ALLOWS);
  }
  
  
  public void setToTheory(Upgrade u) {
    setResearchLevel(u, LEVEL_THEORY);
  }
  
  
  public void setToPractice(Upgrade u) {
    setResearchLevel(u, LEVEL_PRAXIS);
  }
  
  
  public float getResearchLevel(Upgrade u) {
    final Research match = u == null ? null : allResearch.get(u);
    if (match == null) return LEVEL_BANNED;
    else return match.actualLevel;
  }
  
  
  public int getPolicyLevel(Upgrade u) {
    final Research match = u == null ? null : allResearch.get(u);
    if (match == null) return LEVEL_BANNED;
    else return match.policyLevel;
  }
  
  
  public float researchRemaining(Upgrade u) {
    final Research match = u == null ? null : allResearch.get(u);
    if (match == null) return 0;
    if (match.policyLevel <= match.actualLevel) return 0;
    return 1 - (match.actualLevel % 1);
  }
  
  
  public boolean banned(Upgrade u) {
    return getResearchLevel(u) == LEVEL_BANNED;
  }
  
  
  public boolean allows(Upgrade u) {
    return getResearchLevel(u) >= LEVEL_ALLOWS;
  }
  
  
  public boolean hasTheory(Upgrade u) {
    return getResearchLevel(u) >= LEVEL_THEORY;
  }
  
  
  public boolean hasPractice(Upgrade u) {
    return getResearchLevel(u) >= LEVEL_PRAXIS;
  }
  
  
  
  /**  Utility methods for feedback and interface-
    */
  final public static String PROGRESS_LABELS[] = {
    "Unknown", "Theoretical", "Prototype", "Practical"
  };
  
  public String progressDescriptor(Upgrade u) {
    final int level = (int) getResearchLevel(u);
    return PROGRESS_LABELS[Nums.clamp(level + 1, 4)];
  }
}



//  TODO:  Consider integrating these at some point.

/*
  //
  //  Expand this a little once these are all implemented and running...
  final public static Laws
    
    ALMS_AND_BENISON       = new Laws("Alms and Benison"),
    FREEDOM_OF_LEAVE       = new Laws("Freedom of Leave"),
    ELECTED_COUNSEL        = new Laws("Elected Counsel"),
    
    RECLAMATION_DISCIPLINE = new Laws("Reclamation Discipline"),
    NATIVE_PRESERVES       = new Laws("Native Preserves"),
    FORMER_INITIATIVE      = new Laws("Former Initiative"),
    
    ASSEMBLY_LINE          = new Laws("Assembly Line"),
    SOFTWARE_SCRIPTING     = new Laws("Software Scripting"),
    CYBERNETICS            = new Laws("Cybernetics"),
    
    GENE_THERAPIES         = new Laws("Gene Therapies"),
    REHABILITATION_PROGRAM = new Laws("Rehabilitation Program"),
    CLONE_ENGINEERING      = new Laws("Clone Engineering"),
    
    TRIAL_BY_COMBAT        = new Laws("Trial by Combat"),
    CASTE_CODE             = new Laws("Caste Code"),
    PSYON_BREEDING_SCHEME  = new Laws("Psyon Breeding Scheme"),
    
    MILITIA_SERVICE        = new Laws("Militia Service"),
    NONLETHAL_METHODS      = new Laws("Nonlethal Methods"),
    PROPAGANDA_OFFICE      = new Laws("Propaganda Office"),

    FREE_MARKET            = new Laws("Free Market"),
    PRIVATE_PROPERTY       = new Laws("Private Property"),
    CAPTIVES_TRADE         = new Laws("Captives Trade"),
    
    GAMES_AND_TOURNEYS     = new Laws("Games and Tourneys"),
    POLYAMORY              = new Laws("Polyamory"),
    SOMA_DISPENSATION      = new Laws("Soma Dispensation")
 ;
//*/








