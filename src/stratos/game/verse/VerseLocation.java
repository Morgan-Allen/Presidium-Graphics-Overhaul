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
import stratos.util.*;



//  TODO:  Replace with SectorState- so that you can trace the development of a
//  given sector over time.  (This then becomes the Background, not the sector
//  itself.)

//  Sector contains constants such as name, gravity, orbital period,
//  relative distance from other Sectors, and default levels of moisture,
//  minerals and insolation (which might be modified by terraforming.)

//  SectorState measures biomass/terraform progress, population, wealth,
//  danger, squalor, economic output, political interests and culture.  You use
//  that to track development and knock-on effects over time.

//  Housing.     (population)
//  Resources.   (climate)
//  Philosophy.  (interests)
//  Time & Space.
//  Danger & Squalor.
//  Equality & Autonomy.

//  (Also, houses & factions will have to be made into separate entities, so
//  that their degree of ownership/influence can change over time.)

//  TODO:  Include information about distances here, loaded from XML.



public class VerseLocation extends Background {

  
  final public static Object
    MAKES = new Object(),
    NEEDS = new Object();

  
  final public ImageAsset planetImage, houseImage;
  
  final public VerseLocation belongs;
  final public Trait climate;
  final public int gravity;
  final public String houseName;
  final public Traded goodsMade[], goodsNeeded[];
  final public int population;
  
  final Table <Background, Float> circles = new Table();
  final List <Upgrade> knowledge = new List();
  
  final Habitat habitats      [];
  final Float   habitatWeights[];
  final Species nativeSpecies [];
  
  
  public VerseLocation(
    Class baseClass, String name, String houseName,
    ImageAsset planetImage, ImageAsset houseImage,
    String description,
    Trait climate, int gravity, VerseLocation belongs,
    int population, Object... args
  ) {
    super(
      baseClass, name, description, null, null,
      -1, Backgrounds.NOT_A_GUILD, args
    );
    this.houseName   = houseName  ;
    this.planetImage = planetImage == null ? Image.SOLID_WHITE : planetImage;
    this.houseImage  = houseImage  == null ? Image.SOLID_WHITE : houseImage ;
    
    this.belongs = belongs;
    this.climate = climate;
    this.gravity = gravity;
    
    final Batch <Traded > madeB = new Batch();
    final Batch <Traded > needB = new Batch();
    final Batch <Habitat> habB  = new Batch();
    final Batch <Float  > habWB = new Batch();
    final Batch <Species> specB = new Batch();
    Object tag = null;
    float rating = -1;
    
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
    
    goodsMade      = madeB.toArray(Traded .class);
    goodsNeeded    = needB.toArray(Traded .class);
    habitats       = habB .toArray(Habitat.class);
    habitatWeights = habWB.toArray(Float  .class);
    nativeSpecies  = specB.toArray(Species.class);
    this.population = population;
  }
  
  
  public Background[] circles() {
    final Background[] result = new Background[circles.size()];
    return circles.values().toArray(result);
  }
  
  
  public Series <Upgrade> knowledge() {
    return knowledge;
  }
  
  
  public float weightFor(Background circle) {
    final Float weight = circles.get(circle);
    if (weight == null) return 0;
    return weight;
  }
  
  
  public static VerseLocation sectorNamed(String name) {
    for (VerseLocation s : Verse.ALL_SECTORS) if (s.name.equals(name)) {
      return s;
    }
    return null;
  }
  
  
  
  public TerrainGen initialiseTerrain(int mapSize) {
    return new TerrainGen(mapSize, 0.2f, habitats, habitatWeights);
  }
  
  
  public Species[] nativeSpecies() {
    return nativeSpecies;
  }
}


















