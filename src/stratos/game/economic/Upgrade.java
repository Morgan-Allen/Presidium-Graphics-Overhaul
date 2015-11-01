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

public class Upgrade extends Constant implements
  MessagePane.MessageSource, UIConstants
{
  
  
  final static Index <Upgrade> INDEX = new Index <Upgrade> ();
  
  public static enum Type {
    VENUE_LEVEL,
    TECH_MODULE,
    SOC_POLICY ,
    MISC_CHANGE
  };
  
  final public static int
    SINGLE_LEVEL = 1,
    TWO_LEVELS   = 2,
    THREE_LEVELS = 3;
  final public static int
    COST_LOW    =  200,
    COST_MEDIUM =  350,
    COST_HIGH   =  500,
    SIDE_EFFECT = -100;
  
  
  final public String baseName;
  final public String description;
  private ImageAsset icon = null;
  
  final public Type type;
  final public Object refers;
  final public int tier;
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
    //
    //  Finally, compile a proper listing of pre-requisite upgrades (if any),
    //  and compute your tier in the hierarchy.
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
    
    int maxTier = -1;
    for (Upgrade u : this.required) maxTier = Nums.max(maxTier, u.tier);
    this.tier = maxTier + 1;
    
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
  }
  
  /*
  final public static String
    REASON_NO_KNOWLEDGE    = "Lacks theoretical knowledge.",
    REASON_NO_REQUIREMENTS = "Lacks pre-requisite upgrades",
    REASON_MAX_UP_LEVEL    = "At maximum level",
    REASON_SLOTS_FILLED    = "Upgrade slots filled!",
    REASON_NO_FUNDS        = "Lacks sufficient funds!";
  
  public boolean possibleAt(Object client, Base base, Account reasons) {
    
    if (client instanceof Placeable) {
      final Structure s = ((Placeable) client).structure();
      
      final int numType = s.upgradeLevel(this, Structure.STATE_NONE);
      if (numType >= maxLevel) return reasons.setFailure(REASON_MAX_UP_LEVEL);
      
      final boolean noSlots = s.slotsFree() <= 0;
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
  //*/
  
  
  public boolean hasRequirements(Structure structure) {
    for (Upgrade r : required) {
      if (! structure.hasUpgradeOrQueued(r)) return false;
    }
    return true;
  }
  
  
  public boolean hasRequirements(Base base) {
    for (Upgrade r : required) {
      if (! base.research.hasTheory(r)) return false;
    }
    return true;
  }
  
  
  public static Series <Upgrade> upgradesAvailableFor(Venue venue) {
    final Batch <Upgrade> available = new Batch();
    final Upgrade levels[] = venue.blueprint.venueLevels();
    if (levels != null) for (Upgrade u : levels) {
      if (! venue.structure.hasUpgrade(u)) continue;
      for (Upgrade l : u.leadsTo()) if (l.origin == venue.blueprint) {
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
    describeTechChain(d);
  }
  
  
  public void describeTechChain(Description d) {
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
    final Conversion c = researchProcess;
    if (c.skills.length > 0) {
      d.append("Research obstacles:");
      for (int i = 0; i < c.skills.length; i++) {
        d.append("\n  ");
        d.append(c.skills[i]);
        d.append(": "+(int) c.skillDCs[i]);
      }
      d.append("\n");
    }
  }
  
  
  public void appendBaseOrders(
    Description d, final Base base
  ) {
    appendOrders(d, null, base);
  }
  
  
  public void appendVenueOrders(
    Description d, final Venue v, final Base base
  ) {
    appendOrders(d, v, base);
  }
  
  
  private void appendOrders(
    Description d, final Property v, final Base base
  ) {
    final Upgrade upgrade = this;
    final String name = v == null ? baseName : upgrade.nameAt(v, -1, null);
    
    final boolean canBuild =
      v == null &&
      base.research.hasTheory(upgrade)
    ;
    final boolean canUpgrade =
      base.research.hasTheory(upgrade) &&
      v != null && upgrade.hasRequirements(v.structure())
    ;
    final boolean canResearch = (! base.research.banned(upgrade)) && (
      upgrade.hasRequirements(base) ||
      (v != null && upgrade.hasRequirements(v.structure()))
    );
    
    Text.Clickable linksTo    = null;
    String         progReport = null;
    Colour         progColour = Colour.LITE_GREY;
    
    if (canBuild) {
      int cost = upgrade.buildCost(base);
      if (base.finance.hasCredits(cost)) {
        linksTo = new Description.Link("") {
          public void whenClicked() {
            PlacingTask.performPlacingTask(upgrade.origin);
          }
        };
        progReport = (base.research.hasPractice(upgrade) ?
          "(Build for " : "(Prototype for ")+cost+")"
        ;
      }
      else {
        progReport = (base.research.hasPractice(upgrade) ?
          "(Build for " : "(Prototype for ")+cost+")"
        ;
        progColour = Colour.RED;
      }
    }
    
    else if (canUpgrade) {
      int
        level    = v.structure().upgradeLevel(upgrade),
        maxLevel = upgrade.maxLevel,
        cost     = upgrade.buildCost(base);
      boolean doingUpgrade = v.structure().hasUpgradeOrQueued(upgrade);
      float progress = v.structure().upgradeProgress(upgrade);
      
      if (level == maxLevel) {
        progReport = "At max. level";
      }
      else if (v.structure().slotsFree() == 0) {
        progReport = "No free slots";
      }
      else if (doingUpgrade) {
        progReport = "Progress: "+(int) (progress * 100)+"%";
      }
      else if (base.finance.hasCredits(cost)) {
        linksTo = new Description.Link("") {
          public void whenClicked() {
            v.structure().beginUpgrade(upgrade, false);
          }
        };
        progReport = (base.research.hasPractice(upgrade) ?
          "(Build for " : "(Prototype for ")+cost+")"
        ;
      }
      else {
        progReport = (base.research.hasPractice(upgrade) ?
          "(Build for " : "(Prototype for ")+cost+")"
        ;
        progColour = Colour.RED;
      }
    }
    
    else if (canResearch) {
      int resLevel = base.research.getResearchLevel(upgrade) + 1;
      final Mission resDone = upgrade.researchDone(base);
      float progress = base.research.researchProgress(upgrade, resLevel);
      
      if (resDone != null) {
        linksTo = resDone;
        progReport = "Research: "+(int) (progress * 100)+"%";
      }
      else {
        linksTo = new Description.Link(" "+name) {
          public void whenClicked() {
            upgrade.beginResearch(base);
          }
        };
        progReport = "Begin Research";
      }
    }
    
    else {
      progReport = "Banned";
      progColour = Colour.RED;
    }
    

    Text.insert(
      SelectionPane.WIDGET_INFO.asTexture(),
      15, 15, upgrade, false, d
    );
    
    d.append(" "+name, Colour.LITE_GREY);
    if (progReport != null) {
      d.append("\n    ");
      if (linksTo != null) d.append(progReport, linksTo);
      else d.append(progReport, progColour);
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
    BREAKTHROUGH_KEY = "Breakthrough: "     ,
    SETBACK_KEY      = "Setback: "          ;
  
  
  public void sendCompletionMessage(Base base) {
    sendMessageWithKey(base, COMPLETE_KEY+name);
  }
  
  
  public void sendBreakthroughMessage(Base base) {
    sendMessageWithKey(base, BREAKTHROUGH_KEY+name);
  }
  
  
  public void sendSetbackMessage(Base base) {
    sendMessageWithKey(base, SETBACK_KEY+name);
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
    final Upgrade upgrade = this;
    
    if (titleKey.equals(COMPLETE_KEY+name)) message.assignContent(
      upgrade+" is now available in prototype form.  Prototypes are "+
      "expensive, but their cost will decline as your engineers become "+
      "familiar with the technology.",
      
      new Description.Link("View Upgrade") {
        public void whenClicked() {
          upgrade.whenClicked();
          UI.clearMessagePane();
        }
      },
      upgrade.isBlueprintUpgrade() ? new Description.Link("Place Prototype") {
        public void whenClicked() {
          PlacingTask.performPlacingTask(upgrade.origin);
          UI.clearMessagePane();
        }
      } : null
    );
    
    if (titleKey.equals(BREAKTHROUGH_KEY+name)) message.assignContent(
      "One of our researchers had a flash of sudden insight, leading to a "+
      "breakthrough in our research into "+upgrade+"!",
      new Description.Link("View Project") {
        public void whenClicked() {
          final Mission match = upgrade.researchDone(UI.played());
          if (match != null) match.whenClicked();
          UI.clearMessagePane();
        }
      }
    );
    
    if (titleKey.equals(SETBACK_KEY+name)) message.assignContent(
      "There has been a setback in our research into "+upgrade+".  An avenue "+
      "of investigation proved to be unfruitful.",
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
    if (icon != null) return icon;
    
    if (origin == null) return icon = DEFAULT_UPGRADE_ICON;
    if (this == origin.baseUpgrade()) return icon = origin.icon;
    
    final String cat = origin.category;
    final int index = Visit.indexOf(cat, MAIN_INSTALL_CATEGORIES);
    if (index <= -1) return icon = DEFAULT_UPGRADE_ICON;
    
    return icon = UIConstants.GUILD_IMAGE_ASSETS[index];
  }
}













