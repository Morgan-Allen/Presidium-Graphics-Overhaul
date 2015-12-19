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
import stratos.user.Selectable;
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



public class Sector extends Background {

  
  final public static Object
    MAKES = new Object(),
    NEEDS = new Object();
  
  
  final public Sector belongs;
  final public Trait climate;
  final public int gravity;
  
  final public Faction startingOwner;
  final public int population;
  final public Traded goodsMade[], goodsNeeded[];
  
  final Table <Background, Float> circles = new Table();
  final List <Upgrade> knowledge = new List();
  
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
  
  
  public static Sector sectorNamed(String name) {
    for (Sector s : Verse.ALL_SECTORS) if (s.name.equals(name)) {
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
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeHelp(Description d, Selectable prior) {
    substituteReferences(info, d);
    
    if (startingOwner != null) {
      d.append("\n\n");
      d.append(startingOwner.startInfo);
    }
  }
}



















