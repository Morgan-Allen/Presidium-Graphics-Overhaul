/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
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
  
  public static enum Type {
    VENUE_LEVEL,
    TECH_MODULE,
    SOC_POLICY,
    MISC_CHANGE
  };
  
  final public static int
    SINGLE_LEVEL = 1,
    TWO_LEVELS   = 2,
    THREE_LEVELS = 3;
  
  
  final public String baseName;
  final public String description;
  
  final public Type type;
  final public Object refers;
  final public int buildCost;
  final public int maxLevel;
  
  final public Blueprint origin;
  final public Upgrade required[];
  private Batch <Upgrade> leadsTo = new Batch <Upgrade> ();
  
  
  public Upgrade(
    String name, String desc,
    int buildCost, int maxLevel,
    Object required, Blueprint origin,
    Type type, Object refers
  ) {
    super(INDEX, name+"_"+origin, name);
    
    this.baseName    = name;
    this.description = desc;
    this.type        = type;
    this.refers      = refers;
    this.buildCost   = buildCost;
    this.origin      = origin;
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
  }
  
  
  public static Upgrade loadConstant(Session s) throws Exception {
    return INDEX.loadEntry(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    INDEX.saveEntry(this, s.output());
  }
  
  
  public static Series <Upgrade> upgradesAvailableFor(Venue venue) {
    final Batch <Upgrade> available = new Batch();
    for (Upgrade u : venue.blueprint.venueLevels()) {
      if (! venue.structure.hasUpgrade(u)) continue;
      for (Upgrade l : u.leadsTo()) if (u.origin == venue.blueprint) {
        available.add(l);
      }
    }
    return available;
  }
  
  
  public Batch <Upgrade> leadsTo() {
    return leadsTo;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String nameAt(Placeable b, int index, Upgrade queued[]) {
    //  TODO:  THIS IS AN UGLY HACK SOLUTION WHICH YOU SHOULD REPLACE ASAP.
    int level = -1;
    if (index >= 0 && queued != null) {
      while (index >= 0) { if (queued[index] == this) level++; index--; }
    }
    else {
      level = b.structure().upgradeLevel(this, Structure.STATE_NONE);
    }
    if (level >= maxLevel) level = maxLevel - 1;
    
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
    
    describeResearchStatus(d);
    
    if (prior instanceof Venue) {
      final Venue at = (Venue) prior;
      if (! at.structure().upgradePossible(this)) {
        final String error = at.structure().upgradeError(this);
        d.append("\n\n  Cannot Install: "+error);
      }
    }
  }
  
  
  public void describeResearchStatus(Description d) {
    if (required.length > 0) d.append("\nRequires:");
    for (Upgrade u : required) { d.append("\n  "); d.append(u); }
    if (leadsTo.size() > 0) d.append("\nLeads to:");
    for (Upgrade u : leadsTo ) { d.append("\n  "); d.append(u); }
    
    final Base base = BaseUI.currentPlayed();
    if (base != null) {
      String progDesc = base.research.progressDescriptor(this);
      float  progLeft = base.research.researchRemaining (this);
      d.append("\nResearch Status: "+progDesc);
      if (progLeft > 0) {
        d.append("\nResearch Progress: ");
        d.append((int) ((1 - progLeft) * 100)+"%");
      }
    }
  }
  
  
  public void whenClicked() {
    if (this == origin.baseUpgrade()) origin.whenClicked();
    else super.whenClicked();
  }
}











