/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.util.*;
import static stratos.game.wild.Fauna.*;




//  TODO:  Translate this back into a SitingPass?


public class NestUtils {
  
  
  private static boolean
    ratingVerbose = true ,
    updateVerbose = false;
  
  
  public static float crowding(Actor fauna) {
    if (fauna.species().fixedNesting()) {
      return nestCrowding(fauna);
    }
    else {
      return localCrowding(fauna.species(), fauna);
    }
  }
  
  
  public static float nestCrowding(Actor fauna) {
    if (! (fauna.mind.home() instanceof Nest)) return 1.5f;
    final Nest nest = (Nest) fauna.mind.home();
    return nest.crowdRating(fauna.species());
  }
  
  
  public static float crowding(Species s, Target nest, Stage world) {
    if (! (nest instanceof Nest)) return 100;
    final Nest home = (Nest) nest;
    if (home.species != s) return 100;
    int limit = nestLimit(s);
    return home.staff.lodgers().size() / limit;
  }
  
  
  public static int forageRange(Species s) {
    return (int) (1 * (s.predator() ?
      PREDATOR_SEPARATION :
      DEFAULT_FORAGE_DIST
    ));
  }
  
  
  public static int nestLimit(Species s) {
    return Nums.round(Nums.clamp(5f / s.metabolism(), 2, 6), 1 , true);
  }
  
  
  public static Nest createNestFor(Actor a) {
    final Species s = a.species();
    if (s == null || s.nestBlueprint() == null) return null;
    return (Nest) s.nestBlueprint().createVenue(a.base());
  }
  
  
  public static float localCrowding(Species s, Target around) {
    if (s == null || around == null || around.world() == null) return 100;
    
    final Stage   world   = around.world();
    final Base    fauna   = Base.wildlife(world);
    final Ecology ecology = world.ecology();
    
    if (ecology.globalCrowding(s) > 1) return 1;
    final float
      globalPop  = ecology.idealPopulation(s),
      localPop   = fauna.demands.supplyAround(around, s, Stage.ZONE_SIZE),
      localFert  = ecology.biomassRating(around),
      globalFert = ecology.globalBiomass();
    
    if (globalPop <= 0 || globalFert <= 0 || localFert <= 0) return 1;
    int numPatches = world.size * world.size;
    numPatches /= Stage.ZONE_SIZE * Stage.ZONE_SIZE;

    float crowding = 1.0f;
    crowding *= localPop * numPatches / globalPop;
    crowding *= localFert / globalFert;
    return crowding;
  }
  

  public static void populateFauna(Stage world, Species... speciesOrder) {
    final Base base = Base.wildlife(world);
    final Tally <Species> actualNumbers = new Tally <Species> ();
    
    world.ecology().includeSpecies(speciesOrder);
    world.ecology().updateAnimalCrowdEstimates();
    
    while (true) {
      
      final Pick <Species> pick = new Pick(0);
      for (Species s : speciesOrder) {
        final float total = world.ecology().idealPopulation(s);
        pick.compare(s, total - actualNumbers.valueFor(s));
      }
      if (pick.empty()) break;
      
      final Species species   = pick.result();
      final int     popGap    = Nums.round(pick.bestRating(), 1, false);
      final int     nestLimit = nestLimit(species);
      final float   range     = forageRange(species) * 2;
      
      Pick <StagePatch> placing = new Pick(0);
      for (StagePatch r : world.patches.allGridPatches()) {
        
        final Tile point = Spacing.pickRandomTile(r, 0, world);
        float rating = world.terrain().fertilitySample(point);
        float crowding = base.demands.supplyAround(point, Fauna.class, range);
        rating *= nestLimit / (nestLimit + crowding);
        placing.compare(r, rating);
      }
      if (placing.empty()) break;
      
      
      final Target  point = placing.result();
      final boolean nests = species.fixedNesting();
      int           pop   = Nums.min(nestLimit(species), popGap);
      if (pop <= 0) continue;
      
      final Nest nest = (Nest) species.nestBlueprint().createVenue(base);
      if (nests) {
        SiteUtils.establishVenue(nest, placing.result(), true, world);
        if (! nest.inWorld()) continue;
      }
      while (pop-- > 0) {
        Tile entry = Spacing.pickRandomTile(point, 0, world);
        entry      = Spacing.nearestOpenTile(entry, point);
        
        final Actor f = species.sampleFor(base);
        actualNumbers.add(1, species);
        
        if (nests) {
          f.mind.setHome(nest);
          if (Rand.yes() || entry == null) f.enterWorldAt(nest, world);
        }
        if (entry != null && ! f.inWorld()) f.enterWorldAt(entry, world);
      }
    }
  }
  
  
  
  /**  Utility methods for Nest-establishment.
    */
  protected static Blueprint constructBlueprint(
    int size, int high, final Species s, final ModelAsset model
  ) {
    final Blueprint blueprint = new Blueprint(
      Nest.class, s.name+"_nest",
      s.name+" Nest", Target.TYPE_WILD, null, s.info,
      size, high, Structure.IS_CRAFTED,
      Owner.TIER_PRIVATE, 100,
      5
    ) {
      public Venue createVenue(Base base) {
        return new Nest(this, base, s, model);
      }
    };
    
    return blueprint;
  }
}



