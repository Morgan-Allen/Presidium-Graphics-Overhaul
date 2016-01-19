/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.verse;
import stratos.start.*;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
import stratos.game.economic.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.user.mainscreen.MainScreen;
import stratos.util.*;



public class Sector extends Constant {
  
  final public static Index <Sector> INDEX = new Index <Sector> ();
  
  //  TODO:  For the sake of clarity, consider including tags for all the data-
  //         types recorded in the arguments-list (see below)
  final public static Object
    MAKES = new Object(),
    NEEDS = new Object();
  final public static int
    SEP_NONE    = 0,  //  Is the same or parent sector.
    SEP_BORDERS = 1,  //  Has a shared surface border.
    SEP_PLANET  = 2,  //  Is on the same planet...
    SEP_STELLAR = 3;  //  ...or is on a different world entirely.
  
  final static class Separation {
    Sector other;
    int sepType;
    float tripTime;
  }
  
  final public Faction startingOwner;
  final public Background asBackground;
  final public int population;
  final public Traded goodsMade[], goodsNeeded[];
  
  final Table <Background, Float> circles = new Table();
  final List <Upgrade> knowledge = new List();
  
  final public Sector belongs;
  final public Trait climate;
  final public int gravity;
  
  final Batch <Sector> siblings = new Batch();
  final Batch <Sector> borders  = new Batch();
  final Table <Sector, Separation> separations = new Table();
  
  final Habitat habitats      [];
  final Float   habitatWeights[];
  final Species nativeSpecies [];
  
  final public String description;
  final public ImageAsset planetImage;
  
  
  public Sector(
    Class baseClass, String name, String imagePath, Faction owner,
    String description,
    Trait climate, int gravity, Sector belongs,
    int population, Object... args
  ) {
    super(INDEX, name, name);
    this.description = description;
    this.planetImage = imagePath == null ?
      Image.SOLID_WHITE : ImageAsset.fromImage(baseClass, imagePath)
    ;
    //
    //  Populate basic fields first-
    this.startingOwner = owner;
    if (owner != null) owner.bindToStartSite(this);
    this.asBackground = new Background(
      baseClass, name, description, null, null, -1, -1, args
    );
    this.belongs    = belongs   ;
    this.climate    = climate   ;
    this.gravity    = gravity   ;
    this.population = population;
    //
    //  Then extract any information on variable-length attributes from the
    //  list of final arguments:
    final Batch <Traded > madeB = new Batch();
    final Batch <Traded > needB = new Batch();
    final Batch <Habitat> habB  = new Batch();
    final Batch <Float  > habWB = new Batch();
    final Batch <Species> specB = new Batch();
    Object tag = null;
    float rating = -1;
    //
    //  TODO:  Consider requiring tags for attributes other than needs/makes
    for (Object arg : args) {
      if (arg == MAKES || arg == NEEDS) {
        tag = arg;
      }
      if (arg instanceof Traded) {
        final Traded t = (Traded) arg;
        if (t.form != Economy.FORM_MATERIAL) continue;
        if (tag == MAKES) madeB.add(t);
        if (tag == NEEDS) needB.add(t);
      }
      if (arg instanceof Float) {
        rating = (Float) arg;
      }
      if (arg instanceof Background[]) {
        for (Background b : (Background[]) arg) circles.put(b, rating);
      }
      if (arg instanceof Blueprint) {
        arg = ((Blueprint) arg).baseUpgrade();
      }
      if (arg instanceof Upgrade) {
        final Upgrade upgrade = (Upgrade) arg;
        knowledge.include(upgrade);
        for (Upgrade u : upgrade.leadsTo()) {
          if (u.isBlueprintUpgrade()) continue;
          knowledge.include(u);
        }
      }
      if (arg instanceof Habitat) {
        habB.add((Habitat) arg);
        habWB.add(rating);
      }
      if (arg instanceof Species) {
        specB.add((Species) arg);
      }
    }
    //
    //  If any of our key attribute-lists is empty, populate from the parent.
    if (belongs != null) {
      if (madeB.empty()) Visit.appendTo(madeB, belongs.goodsMade     );
      if (needB.empty()) Visit.appendTo(needB, belongs.goodsNeeded   );
      if (habB .empty()) Visit.appendTo(habB , belongs.habitats      );
      if (habWB.empty()) Visit.appendTo(habWB, belongs.habitatWeights);
      if (specB.empty()) Visit.appendTo(specB, belongs.nativeSpecies );
    }
    //
    //  Otherwise, store them in a more compact form.
    goodsMade      = madeB.toArray(Traded .class);
    goodsNeeded    = needB.toArray(Traded .class);
    habitats       = habB .toArray(Habitat.class);
    habitatWeights = habWB.toArray(Float  .class);
    nativeSpecies  = specB.toArray(Species.class);
  }
  
  
  public static Sector loadConstant(Session s) throws Exception {
    return INDEX.loadEntry(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    INDEX.saveEntry(this, s.output());
  }
  
  
  
  /**  Supplementary setup methods for recording inter-relations between
    *  Sectors-
    */
  protected void setSeparation(
    Sector other, int sepType, float tripTime, boolean symmetric
  ) {
    final Separation s = new Separation();
    s.other    = other   ;
    s.sepType  = sepType ;
    s.tripTime = tripTime;
    separations.put(other, s);
    
    if (sepType == SEP_BORDERS) borders.include(other);
    if (symmetric) other.setSeparation(this, sepType, tripTime, false);
  }
  
  
  protected void calculateRemainingSeparations(Sector... sectors) {
    setSeparation(this, SEP_NONE, 0, false);
    if (belongs != null) setSeparation(belongs, SEP_NONE, 0, true);
    //
    //  We intentionally set no destination here to force the search to explore
    //  all available sectors.  Paths can be constructed afterward.
    JourneyPathSearch search = new JourneyPathSearch(this, null);
    search.doSearch();
    //
    //  
    for (Sector s : sectors) if (separations.get(s) == null) {
      final Sector path[] = search.pathTo(s);
      if (path == null) continue;
      
      int sepType = SEP_NONE;
      float totalTripTime = 0;
      Sector last = this;
      
      for (Sector p : path) {
        Separation sep = last.separations.get(p);
        totalTripTime += sep.tripTime;
        if (sep.sepType > sepType) sepType = sep.sepType;
        last = p;
      }
      
      setSeparation(s, sepType, totalTripTime, true);
    }
    
    /*
    if (false) {
      I.say("SEPARATIONS FOR "+this+" ARE: ");
      for (Sector s : separations.keySet()) {
        final Separation sep = separations.get(s);
        I.say("  "+s+": "+sep.tripTime);
      }
      I.say("\n...");
    }
    //*/
  }
  
  
  protected void assignSiblings(Sector... siblings) {
    this.siblings.clear();
    Visit.appendTo(this.siblings, siblings);
  }
  
  
  
  /**  Social and political access-methods-
    */
  public Background[] circles() {
    final Background[] result = new Background[circles.size()];
    return circles.keySet().toArray(result);
  }
  
  
  public float weightFor(Background circle) {
    final Float weight = circles.get(circle);
    if (weight == null) return 0;
    return weight;
  }
  
  
  public Series <Upgrade> knowledge() {
    return knowledge;
  }
  
  
  
  /**  Location and adjacency access-methods-
    */
  public boolean borders(Sector other) {
    final Separation s = separations.get(other);
    return s != null && s.sepType == SEP_BORDERS;
  }
  
  
  public float tripTimeUnits(Sector other) {
    final Separation s = separations.get(other);
    return s == null ? -1 : s.tripTime;
  }
  
  
  public float standardTripTime(Sector other, int maxSepType) {
    final Separation s = separations.get(other);
    final float timeUnit = GameSettings.fastTrips ?
      Stage.STANDARD_HOUR_LENGTH / 2 :
      Stage.STANDARD_DAY_LENGTH
    ;
    if (s == null || s.sepType > maxSepType) return -1;
    else return s.tripTime * timeUnit;
  }
  
  
  public Series <Sector> siblings() {
    return siblings;
  }
  
  
  
  /**  Helper methods for terrain and scenario setup-
    */
  public TerrainGen initialiseTerrain(int mapSize) {
    return new TerrainGen(mapSize, 0.2f, habitats, habitatWeights);
  }
  
  
  public Species[] nativeSpecies() {
    return nativeSpecies;
  }
  
  
  public SectorScenario customScenario(Verse verse) {
    return null;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeHelp(Description d, Selectable prior) {
    
    final Verse verse = currentVerse();
    if (verse == null) return;
    
    final Base           p    = BaseUI.currentPlayed();
    final boolean        seen = p == null || p.intelMap.fogAt(this) > 0;
    final SectorBase     base = verse.baseForSector(this);
    final SectorScenario hook = verse.scenarioFor  (this);
    
    if (! seen) {
      d.append(
        "\nIntel on this Sector is not available.  Send a Recon Mission to "+
        "scout this territory."
      );
      return;
    }
    if (hook != null) hook.describeHook(d);
    else substituteReferences(description, d);
    
    //d.append("\n");
    //d.append("\nGravity: "   +Verse.GRAVITY_DESC   [gravity + 2]);
    //d.append("\nPopulation: "+Verse.POPULATION_DESC[population ]);
    d.appendList("\nHabitats: "     , (Object[]) habitats     );
    d.appendList("\nNative Species:", (Object[]) nativeSpecies);
    
    if (base.faction() == null || base.ruler() == null) {
      d.append("\n");
      d.appendList("\nProduces: ", (Object[]) goodsMade  );
      d.appendList("\nShort of: ", (Object[]) goodsNeeded);
    }
    else {
      d.append("\n");
      d.appendAll("\nClaimed by: ", base.faction());
      d.appendAll("\nGoverned by: ", base.ruler());
      
      d.append("\n");
      d.appendList("\nProduces: ", (Object[]) base.made  ());
      d.appendList("\nShort of: ", (Object[]) base.needed());
    }
    if (startingOwner != null && startingOwner.startSite() == this) {
      d.append("\n\n");
      d.append(startingOwner.startInfo);
      
      for (Skill s : asBackground.skills()) {
        final float bonus = asBackground.skillLevel(s);
        if (bonus < 0) continue;
        d.append("\n  +5 to "+s);
      }
    }
    
    /*
    d.append("\n\nCommon backgrounds: ");
    for (Background b : circles.keySet()) {
      d.appendAll("\n  ", b);
    }
    //*/
    
    /*
    d.append("\n\nKnown Technologies:");
    for (Upgrade u : knowledge) if (u.isBlueprintUpgrade()) {
      d.appendAll("\n  ", u);
    }
    //*/
  }
  
  
  private static Verse currentVerse() {
    final Base played = BaseUI.currentPlayed();
    if (played != null) return played.world.offworld;
    else return MainScreen.currentVerse();
  }
  

  public static Sector sectorNamed(String name, Sector sectors[]) {
    for (Sector s : sectors) if (s.name.equals(name)) {
      return s;
    }
    return null;
  }
}



















