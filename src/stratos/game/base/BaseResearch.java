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
  
  
  private static class Research {
    Upgrade upgrade;
    int policyLevel;
    int actualLevel;
  }
  
  
  final Base base;
  final Table <Upgrade, Research> allResearch = new Table(100);
  
  
  public BaseResearch(Base base) {
    this.base = base;
  }
  
  
  public void loadState(Session s) throws Exception {
    for (int n = s.loadInt(); n-- > 0;) {
      final Upgrade key = (Upgrade) s.loadObject();
      final Research r = new Research();
      r.upgrade     = key        ;
      r.policyLevel = s.loadInt();
      r.actualLevel = s.loadInt();
      allResearch.put(key, r);
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(allResearch.size());
    for (Research r : allResearch.values()) {
      s.saveObject(r.upgrade    );
      s.saveInt   (r.policyLevel);
      s.saveInt   (r.actualLevel);
    }
  }
  
  
  
  
  /**  Setting-queries and modifications-
    */
  private Research researchFor(Upgrade u) {
    Research match = allResearch.get(u);
    if (match != null) return match;
    match = new Research();
    match.upgrade = u;
    return match;
  }
  
  
  public void setPolicyLevel(Upgrade u, int level) {
    final Research r = researchFor(u);
    r.policyLevel = level;
  }
  
  
  public void setResearchLevel(Upgrade u, int level) {
    final Research r = researchFor(u);
    r.actualLevel = level;
  }
  
  
  public int getResearchLevel(Upgrade u) {
    final Research match = allResearch.get(u);
    if (match == null) return LEVEL_BANNED;
    else return match.actualLevel;
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








