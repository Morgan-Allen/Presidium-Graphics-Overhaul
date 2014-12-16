


package stratos.game.politic;
import stratos.graphics.common.*;
import stratos.user.*;
import stratos.util.*;




/**  Holds a representation of the game's 'tech tree'.  As most forms of
  *  advanced technology are viewed with suspicion in this setting, focusing on
  *  a particular technology represents not so much a research effort- as the
  *  technology is, in fact, already known- so much as the time and difficulty
  *  involved in persuading your citizens and superiors to accept the social,
  *  political, economic or philosophical side-effects of the technology, or
  *  re-discovering the principles behind it's use (as opposed to dogmatic
  *  cargo-cultism.)
  *  
  *  Consequently, there's no limit on the number of technologies you can
  *  invest in simultaneously- but if you piss off particular interest groups
  *  in the process, you might have poor morale, a damaged reputation, or even
  *  outright rebellion to contend with.
  *  
  *  All techs have their place in a web of association with other technology,
  *  and depending on just how democratic your polity is, some might be
  *  spontaneously enacted or revoked.
  *  
  *  Protocols can be be either Banned, Permitted, or Mandatory.  In the first
  *  and last case, an appropriate severity of punishment should be allowed.
  *  
  *  Note that I'm using 'technology' quite broadly here, to refer to political
  *  or cultural innovations or bans, as well as the material sciences.
  */
public class Laws {
  
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
  
  
  /**  Data fields, constructors and setup methods-
    */
  private static Object
    REQUIRES  = new Object(),
    CONFLICTS = new Object(),
    APPEALS   = new Object();
  private static List <Laws>
    soFar = new List <Laws> (),
    all   = new List <Laws> ();
  
  final public String name;
  private Laws requires[], conflicts[];
  private String description;
  
  
  private Laws(String name) {
    this.name = name;
  }
  
  
  void setup(Object associations[], String description) {
    Batch appB = new Batch(), reqB = new Batch(), conB = new Batch();
    Batch toAdd = null;
    for (Object o : associations) {
      if      (o == APPEALS  ) toAdd = appB;
      else if (o == REQUIRES ) toAdd = reqB;
      else if (o == CONFLICTS) toAdd = conB;
      else toAdd.add(o);
    }
    //appeals   = (Trait   []) appB.toArray(Trait   .class);
    requires  = (Laws[]) reqB.toArray(Laws.class);
    conflicts = (Laws[]) conB.toArray(Laws.class);
    this.description = description;
    soFar.add(this);
    all.add(this);
  }
  
  public boolean conflicts(Laws p) {
    return Visit.arrayIncludes(conflicts, p);
  }
  
  public boolean requires(Laws p) {
    return Visit.arrayIncludes(requires, p);
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void writeInformation(Description desc) {
    desc.append(name, Colour.WHITE);
    desc.append("\n");
    desc.append(description, Colour.GREY);
    desc.appendList("Requires: ", (Object[]) requires);
    desc.appendList("Conflicts: ", (Object[]) conflicts);
    //desc.appendList("Appeals to the: ", (Object[]) appeals);
  }
  
  public String fullName() {
    return name;
  }

  public void whenClicked() {
    //
    //  ...If the policies panel isn't already open, open it.  Then select and
    //  highlight this policy.
  }
}



/*
  public static Legislation[] protocolsSoFar() {
    final Object result = soFar.toArray(Legislation.class);
    soFar.clear();
    return (Legislation[]) result;
  }
  
  public static Legislation[] allProtocols() {
    return (Legislation[]) all.toArray(Legislation.class);
  }
//*/
/*
//  TODO:  Find some method of imposing these.
//  Rate penalties as: None, Mild, Moderate or Severe.
public static enum Penalty {
  LEGAL, LIGHT_FINES, HEAVY_FINES,
  LIGHT_SENTENCE, HEAVY_SENTENCE,
  DEMOTION_AND_EXILE, EXECUTION,
  TRIAL_BY_COMBAT, REHABILITATION, TORTURE
}
//*/
//private Trait appeals[];
/*
public boolean appeals(Trait t) {
  return Visit.arrayIncludes(appeals, t);
}
//*/




