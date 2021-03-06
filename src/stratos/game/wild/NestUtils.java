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
    if (s.preyedOn()) return 4;
    else return 2;
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
    
    int numPatches = world.size * world.size;
    numPatches /= Stage.ZONE_SIZE * Stage.ZONE_SIZE;
    
    float crowding = 1.0f;
    crowding *= localPop * numPatches / globalPop;
    if (globalFert > 0) crowding *= localFert / globalFert;
    return crowding;
  }
  
  
  public static void populateFauna(
    Stage world, float popScale, Species... speciesOrder
  ) {
    final Base base = Base.wildlife(world);
    final Tally <Species> actualNumbers = new Tally <Species> ();
    
    world.ecology().includeSpecies(speciesOrder);
    world.ecology().updateAnimalCrowdEstimates();
    
    while (true) {
      final Pick <Species> pick = new Pick(0);
      for (Species s : speciesOrder) if (s.nestBlueprint() != null) {
        final float total = world.ecology().idealPopulation(s) * popScale;
        pick.compare(s, total - actualNumbers.valueFor(s));
      }
      if (pick.empty()) break;
      
      final Species species = pick.result();
      final int     popGap  = Nums.round(pick.bestRating(), 1, false);
      int pop = Nums.min(nestLimit(species), popGap);
      if (pop <= 0) break;
      
      Nest nest = (Nest) species.nestBlueprint().createVenue(base);
      final boolean nests = species.fixedNesting();
      SiteUtils.establishVenue(nest, null, -1, true, world);
      
      if (! nest.inWorld()) {
        I.say("COULD NOT FIND SPACE FOR NEST: "+species);
        actualNumbers.add(pop, species);
        continue;
      }
      if (! nests) {
        nest.exitWorld();
      }
      while (pop-- > 0) {
        Tile entry = Spacing.pickRandomTile (nest, 0, world);
        entry      = Spacing.nearestOpenTile(entry, nest   );
        
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
    
    final float range = forageRange(s) * 2;
    final int nestLimit = (int) (nestLimit(s) * 0.75f);
    
    new Siting(blueprint) {
      public float ratePointDemand(
        Base base, Target point, boolean exact, int claimRadius
      ) {
        final Stage world = point.world();
        final Tile  under = world.tileAt(point);
        float rating = world.terrain().fertilitySample(under);
        float crowding = base.demands.supplyAround(point, Fauna.class, range);
        rating *= nestLimit / (nestLimit + crowding);
        return rating;
      }
    };
    
    return blueprint;
  }
}











