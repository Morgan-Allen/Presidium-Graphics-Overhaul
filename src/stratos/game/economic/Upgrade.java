

package stratos.game.economic;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.util.*;
import stratos.user.*;
import stratos.user.notify.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;


//
//  Upgrades tend to either expand employment, give a bonus to production
//  of a particular item type, or enhance a particular kind of service.

public class Upgrade extends Constant {
  
  
  final static Index <Upgrade> INDEX = new Index <Upgrade> ();
  private static Table <Blueprint, Batch <Upgrade>> byVenue = new Table();
  
  public static enum Type {
    VENUE_LEVEL,
    TECH_MODULE,
    SOC_POLICY
  };
  
  final public static int
    SINGLE_LEVEL = 1,
    TWO_LEVELS   = 2,
    THREE_LEVELS = 3;
  
  
  final public String baseName;
  final public String description;
  
  final public Type type;
  final public int buildCost;
  final public int maxLevel;
  
  final public Blueprint origin;
  final public Upgrade required[];
  final public Object refers;
  final public int bonus;
  private Batch <Upgrade> leadsTo = new Batch <Upgrade> ();
  
  
  public Upgrade(
    String name, String desc,
    int buildCost, int maxLevel
  ) {
    this(name, desc, buildCost, maxLevel, null, 1, null, null);
  }
  
  
  public Upgrade(
    String name, String desc,
    int buildCost, int maxLevel,
    Object refers, int bonus,
    Object required, Blueprint origin
  ) {
    super(INDEX, name+"_"+origin, name);
    
    this.baseName    = name;
    this.description = desc;
    this.type        = Type.TECH_MODULE;
    this.buildCost   = buildCost;
    this.origin      = origin;
    this.refers      = refers;
    this.bonus       = bonus;
    this.maxLevel    = maxLevel;
    
    if (required instanceof Upgrade) {
      this.required = new Upgrade[] { (Upgrade) required };
    }
    else if (required instanceof Upgrade[]) {
      this.required = (Upgrade[]) required;
    }
    else {
      if (required != null) I.say(
        "\nWARNING: "+required+" is not an upgrade or upgrade array!"
      );
      this.required = new Upgrade[0];
    }
    for (Upgrade u : this.required) u.leadsTo.add(this);
    
    Batch <Upgrade> VU = byVenue.get(origin);
    if (VU == null) byVenue.put(origin, VU = new Batch());
    VU.add(this);
  }
  
  
  public static Upgrade loadConstant(Session s) throws Exception {
    return INDEX.loadEntry(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    INDEX.saveEntry(this, s.output());
  }
  
  
  public static Upgrade[] upgradesFor(Blueprint venueType) {
    final Batch <Upgrade> VU = byVenue.get(venueType);
    return VU == null ? new Upgrade[0] : VU.toArray(Upgrade.class);
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String nameAt(Structure.Basis b, int index, Upgrade queued[]) {
    //  TODO:  THIS IS AN UGLY HACK SOLUTION WHICH YOU SHOULD REPLACE ASAP.
    int level = -1;
    if (index >= 0 && queued != null) {
      while (index >= 0) { if (queued[index] == this) level++; index--; }
    }
    else {
      level = b.structure().upgradeLevel(this, Structure.STATE_NONE);
    }
    if (level == 0) return baseName;
    if (level == 1) return "Improved "+baseName;
    else            return "Advanced "+baseName;
  }
  
  
  public void describeHelp(Description d, Selectable prior) {
    if (refers instanceof Background) {
      ((Background) refers).describeHelp(d, prior);
      d.append("\n");
    }
    else {
      d.append("\n");
      substituteReferences(description, d);
      d.append("\n");
      
      if (origin != null) {
        d.append("\n  Installed at: ");
        d.append(origin);
      }
    }
    
    if (required.length > 0) d.append("\nRequires:");
    for (Upgrade u : required) { d.append("\n  "); d.append(u); }
    if (leadsTo.size() > 0) d.append("\nLeads to:");
    for (Upgrade u : leadsTo ) { d.append("\n  "); d.append(u); }
    
    if (prior instanceof Venue) {
      final String error = ((Venue) prior).structure().upgradeError(this);
      if (error != null) d.append("\n\n  Cannot Install: "+error);
    }
  }
}











