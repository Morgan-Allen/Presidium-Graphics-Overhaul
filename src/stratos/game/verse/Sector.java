/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.verse;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
import stratos.game.economic.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



public class Sector extends Background {

  
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
  final public int population;
  final public Traded goodsMade[], goodsNeeded[];
  
  final Table <Background, Float> circles = new Table();
  final List <Upgrade> knowledge = new List();
  
  final public Sector belongs;
  final public Trait climate;
  final public int gravity;
  
  final Batch <Sector> borders = new Batch();
  final Table <Sector, Separation> separations = new Table();
  
  final Habitat habitats      [];
  final Float   habitatWeights[];
  final Species nativeSpecies [];
  
  final public ImageAsset planetImage;
  
  
  public Sector(
    Class baseClass, String name, String imagePath, Faction owner,
    String description,
    Trait climate, int gravity, Sector belongs,
    int population, Object... args
  ) {
    super(
      baseClass, name, description, null, null,
      -1, Backgrounds.NOT_A_GUILD, args
    );
    this.planetImage = imagePath == null ?
      Image.SOLID_WHITE : ImageAsset.fromImage(baseClass, imagePath)
    ;
    //
    //  Populate basic fields first-
    this.startingOwner = owner;
    if (owner != null) owner.bindToStartSite(this);
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
    
    if (false) {
      I.say("SEPARATIONS FOR "+this+" ARE: ");
      for (Sector s : separations.keySet()) {
        final Separation sep = separations.get(s);
        I.say("  "+s+": "+sep.tripTime);
      }
      I.say("\n...");
    }
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
  public static Sector sectorNamed(String name) {
    for (Sector s : Verse.ALL_SECTORS) if (s.name.equals(name)) {
      return s;
    }
    return null;
  }
  
  
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
  
  
  
  /**  Helper methods for terrain and scenario setup-
    */
  public TerrainGen initialiseTerrain(int mapSize) {
    return new TerrainGen(mapSize, 0.2f, habitats, habitatWeights);
  }
  
  
  public Species[] nativeSpecies() {
    return nativeSpecies;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeHelp(Description d, Selectable prior) {
    substituteReferences(info, d);
    
    //  TODO:  You need to display information for whatever Scenario is
    //  currently applied within a given sector.
    
    d.append("\n");
    d.append("\nGravity: "   +Verse.GRAVITY_DESC   [gravity + 2]);
    d.append("\nPopulation: "+Verse.POPULATION_DESC[population ]);
    d.appendList("\nNative Species:", (Object[]) nativeSpecies);
    d.appendList("\nHabitats: "     , (Object[]) habitats     );
    
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
    
    final Base played = BaseUI.currentPlayed();
    if (played != null) {
      final Verse verse = played.world.offworld;
      final SectorBase base = verse.baseForSector(this);
      
      if (base.faction() == null) {
        d.append("\n");
        d.appendList("\nProduces: ", (Object[]) goodsMade  );
        d.appendList("\nShort of: ", (Object[]) goodsNeeded);
        
        //Scenario active = verse.scenarioFor(this);
      }
      else {
        d.append("\n");
        d.appendAll("\nClaimed by: ", base.faction());
        d.appendAll("\nGoverned by: ", base.ruler());
        
        d.append("\n");
        d.appendList("\nGoods made: "  , (Object[]) base.made  ());
        d.appendList("\nGoods needed: ", (Object[]) base.needed());
      }
      if (! base.allUnits().empty()) {
        d.append("\nResidents: ");
        for (Mobile m : base.allUnits()) {
          d.appendAll("\n  ", m);
        }
      }
    }
    
    else if (startingOwner != null) {
      d.append("\n");
      d.appendList("\nGoods made: "  , (Object[]) goodsMade  );
      d.appendList("\nGoods needed: ", (Object[]) goodsNeeded);
      
      d.append("\n\n");
      d.appendAll("Ruled by ", startingOwner, "\n");
      d.append(startingOwner.startInfo);
    }
  }
  
  
  
}



















