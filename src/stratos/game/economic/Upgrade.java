/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.economic;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.util.*;
import stratos.user.*;
import stratos.user.notify.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;


//
//  Upgrades tend to either expand employment, give a bonus to production
//  of a particular item type, or enhance a particular kind of service.

public class Upgrade extends Constant implements MessagePane.MessageSource {
  
  
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
  final public Conversion researchProcess;
  
  final public int defaultCost;
  final public int maxLevel;
  
  final public Blueprint origin;
  private Batch <Upgrade> required = new Batch();
  private Batch <Upgrade> leadsTo  = new Batch();
  
  
  public Upgrade(
    String name, String desc,
    int defaultCost, int maxLevel,
    Object required, Blueprint origin,
    Type type, Object refers,
    Object... researchConversionArgs
  ) {
    super(INDEX, name+"_"+origin, name);
    
    this.baseName    = name;
    this.description = desc;
    this.type        = type;
    this.refers      = refers;
    this.defaultCost = defaultCost;
    this.origin      = origin;
    this.maxLevel    = maxLevel;
    
    this.researchProcess = new Conversion(
      origin,
      entryKey()+"_research",
      researchConversionArgs
    );
    
    if (required instanceof Upgrade) {
      this.required.add((Upgrade) required);
    }
    else if (required instanceof Upgrade[]) {
      Visit.appendTo(this.required, (Upgrade[]) required);
    }
    else if (required != null) I.say(
      "\nWARNING: "+required+" is not an upgrade or upgrade array!"
    );
    for (Upgrade u : this.required) u.leadsTo.add(this);
    
    final Upgrade frame = origin == null ? null : origin.baseUpgrade();
    if (frame != null && frame != this) this.required.include(frame);
  }
  
  
  public static Upgrade loadConstant(Session s) throws Exception {
    return INDEX.loadEntry(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    INDEX.saveEntry(this, s.output());
  }
  
  
  public Batch <Upgrade> leadsTo() {
    return leadsTo;
  }
  
  
  
  /**  Support for research projects and checking pre-requisites-
    */
  public void beginResearch(Base base) {
    base.research.setPolicyLevel(this, BaseResearch.LEVEL_PRAXIS);
    
    final Mission research = new MissionResearch(base, this);
    research.assignPriority(Mission.PRIORITY_NOMINAL);
    base.tactics.addMission(research);
    
    if (base == BaseUI.currentPlayed()) research.whenClicked();
  }
  
  
  final public static String
    REASON_NO_KNOWLEDGE    = "Lacks theoretical knowledge.",
    REASON_NO_REQUIREMENTS = "Lacks pre-requisite upgrades",
    REASON_SLOTS_FILLED    = "Upgrade slots filled!",
    REASON_NO_FUNDS        = "Lacks sufficient funds!";
  
  public boolean possibleAt(Object client, Base base, Account reasons) {
    
    if (client instanceof Placeable) {
      final Structure s = ((Placeable) client).structure();
      
      final int numType = s.upgradeLevel(this, Structure.STATE_NONE);
      final boolean noSlots = s.slotsFree() <= 0 || numType >= maxLevel;
      if (noSlots) return reasons.setFailure(REASON_SLOTS_FILLED);
      
      final boolean hasReq = hasRequirements(s);
      if (! hasReq) return reasons.setFailure(REASON_NO_REQUIREMENTS);
    }
    
    final boolean unknown = ! base.research.hasTheory(this);
    if (unknown) return reasons.setFailure(REASON_NO_KNOWLEDGE);
    
    final boolean noFund = buildCost(base) > base.finance.credits();
    if (noFund) return reasons.setFailure(REASON_NO_FUNDS);
    
    return reasons.setSuccess();
  }
  
  
  public boolean hasRequirements(Structure structure) {
    for (Upgrade r : required) {
      if (! structure.hasUpgradeOrQueued(r)) return false;
    }
    return true;
  }
  
  
  public boolean hasRequirements(Base base) {
    for (Upgrade r : required) {
      if (! base.research.hasPractice(r)) return false;
    }
    return true;
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
  
  
  public int buildCost(Base base) {
    final BaseResearch BR = base.research;
    if (BR.hasPractice(this)) return defaultCost;
    
    final float resLeft = BR.researchRemaining(this, BaseResearch.LEVEL_PRAXIS);
    final float mult = BaseResearch.PROTOTYPE_COST_MULT * resLeft;
    return (int) (defaultCost * (1 + mult));
  }
  
  
  public boolean isBlueprintUpgrade() {
    return origin.baseUpgrade() == this;
  }
  
  
  public Mission researchDone(Base base) {
    return base.matchingMission(this, MissionResearch.class);
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
    }
    
    describeResearchStatus(d, this);
  }
  
  
  public void describeResearchStatus(Description d, final Object client) {
    
    final Base base = BaseUI.currentPlayed();
    final Upgrade upgrade = this;
    if (base == null) return;
    
    if (client == origin || client == this) {
      Text.cancelBullet(d);
      
      if (required.size() > 0) {
        d.append("Requires:");
        for (Upgrade u : required) { d.append("\n  "); d.append(u); }
        d.append("\n");
      }
      if (leadsTo.size() > 0) {
        d.append("Leads to:");
        for (Upgrade u : leadsTo ) { d.append("\n  "); d.append(u); }
        d.append("\n");
      }
      return;
    }
    
    final Account reasons = new Account();
    final Mission researchDone = researchDone(base);
    final boolean
      possible = possibleAt(client, base, reasons),
      unknown  = reasons.hadReason(REASON_NO_KNOWLEDGE);
    
    //
    //  You can either research, prototype or install the upgrade, assuming
    //  knowledge is the problem.  If it isn't, just allow for normal
    //  installation (or prototyping.)
    
    final int knowledge = base.research.getResearchLevel(this);
    final String progDesc = base.research.progressDescriptor(this);
    final float progLeft = base.research.researchRemaining(this, knowledge + 1);
    final boolean underResearch = unknown && researchDone != null;
    
    d.append("\n Research Status: "+progDesc);
    if (progLeft < 1) {
      d.append(" ("+(int) ((1 - progLeft) * 100)+"%)");
    }
    d.append("\n");
    
    String desc = "BEGIN RESEARCH";
    if (underResearch) desc = "Research in progress";
    if (knowledge == BaseResearch.LEVEL_THEORY) desc = "PROTOTYPE";
    if (knowledge == BaseResearch.LEVEL_PRAXIS) desc = "INSTALL";
    
    d.append(" ");
    if (possible || unknown) d.append(new Description.Link(desc) {
      public void whenClicked() {
        if (underResearch) {
          researchDone.whenClicked();
        }
        else if (unknown) {
          if (I.logEvents()) I.say("\nBEGAN RESEARCH: "+upgrade+" FOR "+base);
          upgrade.beginResearch(base);
        }
        else {
          if (I.logEvents()) I.say("\nBEGAN UPGRADE: "+upgrade+" AT "+client);
          if (upgrade.isBlueprintUpgrade()) {
            PlacingTask.performPlacingTask(upgrade.origin);
          }
          else if (client instanceof Placeable) {
            ((Placeable) client).structure().beginUpgrade(upgrade, false);
          }
        }
      }
    });
    else d.append(desc, Colour.GREY);
    //
    //  If knowledge isn't the problem, either cite the reason or list the
    //  funds required (in red if not available.)
    if (! unknown) {
      final int cost = upgrade.buildCost(base);
      if (reasons.hadReason(REASON_NO_FUNDS)) {
        d.append(" ("+cost+" Credits)", Colour.GREY);
      }
      else if (! possible) {
        d.append(" "+reasons.failReasons(), Colour.RED);
      }
      else d.append(" ("+cost+" Credits)");
    }
  }
  
  
  public void whenClicked() {
    if (this == origin.baseUpgrade()) origin.whenClicked();
    else super.whenClicked();
  }
  
  
  
  /**  Messages related to message-status...
    */
  final static String
    COMPLETE_KEY     = "Research Complete: ",
    BREAKTHROUGH_KEY = "Breakthrough!";
  
  
  public void sendCompletionMessage(Base base) {
    sendMessageWithKey(base, COMPLETE_KEY+name);
  }
  
  
  public void sendBreakThroughMessage(Base base) {
    sendMessageWithKey(base, BREAKTHROUGH_KEY);
  }
  
  
  private void sendMessageWithKey(Base base, String titleKey) {
    final BaseUI current = BaseUI.current();
    if (current == null) return;
    final float date = base.world.currentTime();
    final MessagePane message = configMessage(titleKey, current);
    current.reminders().addMessageEntry(message, true, date);
  }
  
  
  public MessagePane configMessage(final String titleKey, final BaseUI UI) {
    final MessagePane message = new MessagePane(UI, titleKey, this);
    final BaseResearch BR = UI.played().research;
    final String desc = BR.progressDescriptor(this);
    final Upgrade upgrade = this;
    
    if (titleKey.equals(COMPLETE_KEY+name)) message.assignContent(
      "A new upgrade is now available in "+desc+" stage.",
      new Description.Link("View "+name) {
        public void whenClicked() {
          upgrade.whenClicked();
          UI.clearMessagePane();
        }
      }
    );
    
    if (titleKey.equals(BREAKTHROUGH_KEY)) message.assignContent(
      "There has been a breakthrough in our research into "+upgrade+"!",
      new Description.Link("View Project") {
        public void whenClicked() {
          final Mission match = upgrade.researchDone(UI.played());
          if (match != null) match.whenClicked();
          UI.clearMessagePane();
        }
      }
    );
    
    return message;
  }
  
  
  public void messageWasOpened(String titleKey, BaseUI UI) {
    UI.reminders().retireMessage(titleKey);
  }
  
  
  public ImageAsset portraitImage() {
    if (origin == null) return null;
    if (this == origin.baseUpgrade()) return origin.icon;
    return InstallPane.upgradeIcon(origin.category);
  }
}













