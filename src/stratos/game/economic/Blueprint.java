/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.economic;
import stratos.game.common.*;
import stratos.user.*;
import stratos.util.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;

import java.lang.reflect.*;


/*
  //  TODO:  Finish moving attributes in here.
  
  final public Traded materials[];
  final public Conversion services[];
  
  final public Background careers[];
  final public int shiftType;
  
  //  TODO:  Allow reading from XML?  (Possibly model too...?)
//*/


public class Blueprint extends Constant implements Session.Saveable {
  
  
  final public static Index <Blueprint> INDEX = new Index <Blueprint> ();
  
  final public Class <? extends Venue> baseClass;
  final public String keyID;
  
  final public String name;
  final public ImageAsset icon;
  final public String category;
  final public String description;

  final public int size, high;
  final public int
    properties ,
    integrity  ,
    armour     ,
    buildCost  ,
    maxUpgrades;
  
  final public Blueprint required[];
  final public int owningTier;
  final public Conversion processed[];
  
  final public int maxIntegrity = Structure.DEFAULT_INTEGRITY;
  
  private Batch <Blueprint> allows = new Batch <Blueprint> ();
  private Batch <Blueprint> denies = new Batch <Blueprint> ();
  
  
  public Blueprint(
    Class <? extends Venue> baseClass, String key,
    String name, String category, ImageAsset icon, String description,
    int size, int high, int properties,
    Blueprint required, int owningTier,
    int integrity, int armour, int buildCost, int maxUpgrades,
    Conversion... processed
  ) {
    this(
      baseClass, key,
      name, category, icon, description,
      size, high,
      properties, required == null ? null : new Blueprint[] { required },
      owningTier, integrity, armour, buildCost, maxUpgrades,
      processed
    );
  }
  

  public Blueprint(
    Class <? extends Venue> baseClass, String key,
    String name, String category, ImageAsset icon, String description,
    int size, int high, int properties,
    Blueprint required[], int owningTier,
    int integrity, int armour, int buildCost, int maxUpgrades,
    Conversion... processed
  ) {
    super(INDEX, key, name);
    this.baseClass = baseClass;
    this.keyID     = key;
    
    this.name        = name;
    this.category    = category;
    this.icon        = icon;
    this.description = description;
    
    this.size = size;
    this.high = high;
    
    this.properties  = properties ;
    this.integrity   = integrity  ;
    this.armour      = armour     ;
    this.buildCost   = buildCost  ;
    this.maxUpgrades = maxUpgrades;
    
    this.required   = required == null ? Venue.NO_REQUIREMENTS : required;
    this.owningTier = owningTier;
    this.processed  = processed ;
    
    for (Blueprint p : required) p.allows.include(this);
  }
  
  
  public static Blueprint loadConstant(Session s) throws Exception {
    return INDEX.loadEntry(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    INDEX.saveEntry(this, s.output());
  }
  
  
  
  /**  Property queries-
    */
  public Series <Blueprint> allows() {
    return allows;
  }
  
  
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
  
  
  public Conversion producing(Object t) {
    for (Conversion c : processed) if (c.out.type == t) return c;
    return null;
  }
  
  
  public Conversion consuming(Object t) {
    for (Conversion c : processed) {
      for (Item i : c.raw) if (i.type == t) return c;
    }
    return null;
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
  
  
  
  /**  External utility methods-
    */
  
  
  
  /**  Interface and debugging-
    */
  protected void describeHelp(Description d, Selectable prior) {
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
    final int cost = buildCost;
    d.append("\nBuild cost: "+cost);
    
    for (Blueprint req : required) {
      d.append("\n  Requires: ");
      d.append(req);
    }
    for (Blueprint all : allows) {
      d.append("\n  Allows: ");
      d.append(all);
    }
    
    if (processed.length > 0) d.append("\n\nAllows Production:");
    for (Conversion c : processed) {
      d.append("\n  ");
      if (c.raw.length > 0) {
        for (Item i : c.raw) { d.append(i.type); d.append(" "); }
        d.append("to ");
        d.append(c.out.type);
      }
      else {
        d.append(c.out.type);
      }
    }
    
    final Upgrade upgrades[] = Upgrade.upgradesFor(this);
    if (upgrades.length > 0) d.append("\n\nUpgrades:");
    for (Upgrade u : upgrades) {
      d.append("\n  ");
      d.append(u);
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

    //  TODO:  Include backgrounds too?  (Or is that covered under upgrades?)
  }
}




