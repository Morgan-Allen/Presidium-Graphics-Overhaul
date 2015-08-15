/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.economic;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.maps.*;
import stratos.user.*;
import stratos.util.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;

import java.lang.reflect.*;



/*
  //  TODO:  Finish moving attributes in here...
  final public Traded materials[];
  final public int shiftType;
  
  //  TODO:  Allow reading from XML?  (Possibly model too...?)
//*/


public class Blueprint extends Constant implements Session.Saveable {
  
  
  final public static Index <Blueprint> INDEX = new Index <Blueprint> ();
  
  final public Class <? extends Venue> baseClass;
  final public String keyID;
  
  final public ImageAsset icon;
  final public String category;
  final public String description;

  final public int size, high;
  final public int
    properties ,
    integrity  ,
    armour     ;
  
  final public int owningTier;
  
  private Upgrade upgradeLevels[];
  private Object services[];
  private Batch <Conversion> producing = new Batch();
  private Batch <Conversion> consuming = new Batch();
  
  private Siting siting = null;
  private Traded tradeServices[] = null;
  private Background careerServices[] = null;
  

  public Blueprint(
    Class <? extends Venue> baseClass, String key,
    String name, String category, ImageAsset icon, String description,
    int size, int high, int properties, int owningTier,
    int integrity, int armour,
    Object... services
  ) {
    super(INDEX, key, name);
    setAsUniqueTo(baseClass);
    
    this.baseClass = baseClass;
    this.keyID     = key;
    
    this.category    = category;
    this.icon        = icon;
    this.description = description;
    
    this.size = size;
    this.high = high;
    
    this.properties  = properties ;
    this.integrity   = integrity  ;
    this.armour      = armour     ;
    this.owningTier  = owningTier ;
    //
    //  And finally, we register with any listed services.
    final Batch <Traded    > TS = new Batch();
    final Batch <Background> CS = new Batch();
    for (Object o : services) {
      if (o instanceof Background) {
        ((Background) o).addHirePoint(this);
        CS.add((Background) o); 
      }
      if (o instanceof Traded) {
        ((Traded) o).addSource(this);
        TS.add((Traded) o);
      }
    }
    this.services = services;
    this.tradeServices  = TS.toArray(Traded    .class);
    this.careerServices = CS.toArray(Background.class);
  }
  
  
  public static Blueprint loadConstant(Session s) throws Exception {
    return INDEX.loadEntry(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    INDEX.saveEntry(this, s.output());
  }
  
  
  public void linkWith(Siting siting) {
    if (this.siting != null && this.siting != siting) {
      I.say("\nWARNING: Siting already assigned to "+this);
    }
    this.siting = siting;
  }
  
  
  public Siting siting() {
    return siting;
  }
  
  
  public Traded[] tradeServices() {
    return tradeServices;
  }
  
  
  public Background[] careerServices() {
    return careerServices;
  }
  
  
  
  /**  Factory methods for direct structure-upgrades.
    */
  public Upgrade[] createVenueLevels(
    int numLevels, Upgrade required, int... buildCosts
  ) {
    if (buildCosts.length != numLevels) {
      I.complain("MUST HAVE BUILD COSTS FOR EACH VENUE LEVEL!");
      return new Upgrade[0];
    }
    this.upgradeLevels = new Upgrade[numLevels];
    
    for (int i = 0; i < numLevels; i++) upgradeLevels[i] = new Upgrade(
      (i == 0) ? this.name :" Level "+(i + 1)+" Upgrade",
      "Upgrade to level "+(i + 1),
      buildCosts[i], 1,
      (i == 0 ? required : upgradeLevels[i - 1]), this,
      Upgrade.Type.VENUE_LEVEL, null
    );
    return upgradeLevels;
  }
  
  
  public Upgrade[] assignVenueLevels(Upgrade... levels) {
    this.upgradeLevels = levels;
    return levels;
  }
  
  
  public Upgrade[] venueLevels() {
    return upgradeLevels;
  }
  
  
  public Upgrade baseUpgrade() {
    if (upgradeLevels == null) return null;
    return upgradeLevels[0];
  }
  
  
  public int numLevels() {
    if (upgradeLevels == null) return 1;
    return upgradeLevels.length;
  }
  
  
  
  /**  Property queries-
    */
  public static boolean hasProperty(Structure s, int property) {
    return (s.properties() & property) == property;
  }
  
  
  public boolean hasProperty(int property) {
    return (properties & property) == property;
  }
  
  
  public boolean isFixture() {
    return hasProperty(Structure.IS_FIXTURE);
  }
  
  
  public boolean isGrouped() {
    return hasProperty(Structure.IS_LINEAR);
  }
  
  
  public boolean isStandard() {
    return properties == Structure.IS_NORMAL;
  }
  
  
  public boolean isUnique() {
    return hasProperty(Structure.IS_UNIQUE);
  }
  
  
  public boolean isWild() {
    return hasProperty(Structure.IS_WILD);
  }
  
  
  
  /**  Conversion queries and registration-
    */
  public Conversion producing(Object t) {
    for (Conversion c : producing) {
      if (c.out != null && c.out.type == t) return c;
    }
    return null;
  }
  
  
  public Conversion consuming(Object t) {
    for (Conversion c : producing) {
      for (Item i : c.raw) if (i.type == t) return c;
    }
    for (Conversion c : consuming) {
      for (Item i : c.raw) if (i.type == t) return c;
    }
    return null;
  }
  
  
  public Series <Conversion> production() {
    return producing;
  }
  
  
  public void addProduction(Conversion p) {
    if (p.out != null) producing.include(p);
    else if (p.raw.length > 0) consuming.include(p);
  }
  
  
  
  /**  Save and load functions for external reference.
    */
  private static Blueprint CIVIC_BP[];
  
  
  public static Blueprint[] allBlueprints() {
    return INDEX.allEntries(Blueprint.class);
  }
  
  
  public static Blueprint[] allCivicBlueprints() {
    if (CIVIC_BP != null) return CIVIC_BP;
    final Batch <Blueprint> matches = new Batch <Blueprint> ();
    for (Blueprint p : allBlueprints()) if (! p.isWild()) matches.add(p);
    return CIVIC_BP = matches.toArray(Blueprint.class);
  }
  
  
  public static Venue[] sampleVenues(
    int owningTier, Blueprint... canPlace
  ) {
    if (canPlace == null) canPlace = allBlueprints();
    final Batch <Venue> typeBatch = new Batch <Venue> ();
    
    for (Blueprint blueprint : canPlace) {
      final Venue sample = blueprint.createVenue(null);
      if (sample == null || sample.owningTier() > owningTier) continue;
      typeBatch.add(sample);
    }
    return typeBatch.toArray(Venue.class);
  }
  
  
  public Venue createVenue(Base base) {
    try {
      if (! Venue.class.isAssignableFrom(baseClass)) return null;
      final Constructor c = baseClass.getConstructor(Base.class);
      return (Venue) c.newInstance(base);
    }
    catch (NoSuchMethodException e) {
      I.say(
        "\n  WARNING: NO BASE CONSTRUCTOR FOR: "+baseClass.getName()+
        "\n  All Venues should implement a public constructor taking a Base "+
        "\n  as the sole argument, or else their blueprint should override "+
        "\n  the createVenue() method.  Thank you.\n"
      );
      return null;
    }
    catch (Exception e) {
      I.say("ERROR INSTANCING "+baseClass.getSimpleName()+": "+e);
      e.printStackTrace();
      return null;
    }
  }
  
  
  public int buildCost(Base base) {
    if (upgradeLevels == null) return -1;
    return baseUpgrade().buildCost(base);
  }
  
  
  
  /**  Interface and debugging-
    */
  public void describeHelp(Description d, Selectable prior) {
    final Base base = BaseUI.currentPlayed();
    
    if (icon != null) {
      Text.insert(icon.asTexture(), 80, 80, false, d);
      d.append("\n\n");
    }
    substituteReferences(description, d);
    
    d.append("\n");
    if (category != null) {
      d.append("\nCategory: "+category+" Structures");
    }
    final int cost = buildCost(base);
    d.append("\nBuild cost: "+cost);
    
    if (baseUpgrade() != null) {
      baseUpgrade().describeResearchStatus(d);
    }
    
    if (consuming.size() > 0) d.append("\n\nConsumption:");
    for (Conversion c : consuming) {
      d.append("\n  ");
      final String name = c.specialName();
      if (name != null) d.append(name+" ");
      d.append("Needs ");
      for (Item i : c.raw) { d.append(i.type); d.append(" "); }
    }
    
    if (producing.size() > 0) d.append("\n\nProduction:");
    for (Conversion c : producing) {
      d.append("\n  ");
      final String name = c.specialName();
      if (name != null) d.append(name);
      
      if (c.raw.length > 0) {
        for (Item i : c.raw) {
          d.append((int) i.amount+" ");
          d.append(i.type);
          d.append(" ");
        }
        d.append("to "+(int) c.out.amount+" ");
        d.append(c.out.type);
      }
      else {
        d.append(c.out.type);
      }
      d.append("\n  Base 2.5x per worker/day");
    }
    
    if (! isGrouped()) {
      final Batch <Venue> built = base.listInstalled(this, false);
      d.append("\n\nCurrently Built:");
      if (built.size() > 0) for (Venue v : built) {
        d.append("\n  ");
        d.append(v);
      }
      else d.append(" None");
    }
  }
}




