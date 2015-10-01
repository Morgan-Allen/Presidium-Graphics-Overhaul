/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.util.*;
import static stratos.game.wild.Fauna.*;


public class NestUtils {
  
  
  private static boolean
    ratingVerbose = false,
    updateVerbose = false;
  
  
  public static float idealPopulation(
    Species species, Target point, Stage world
  ) {
    //
    //  Here, we either use local fertility for browsers, or the abundance of
    //  browsers themselves for predators.
    final Base  base        = Base.wildlife(world);
    final float forageRange = forageRange(species);
    
    float foodSupply = 0;
    if (species.predator()) {
      if (point == null) {
        foodSupply = base.demands.globalSupply(Species.KEY_BROWSER);
        foodSupply /= DEFAULT_PREDATOR_SPECIES;
      }
      else foodSupply = base.demands.supplyAround(
        point, Species.KEY_BROWSER, forageRange
      );
      foodSupply /= PREDATOR_TO_PREY_RATIO;
    }
    else {
      if (point == null) {
        foodSupply = world.terrain().globalFertility();
        foodSupply /= BROWSER_TO_FLORA_RATIO * DEFAULT_BROWSER_SPECIES;
      }
      else {
        foodSupply = world.terrain().fertilitySample(world.tileAt(point));
        foodSupply *= (forageRange * forageRange * 4) / BROWSER_TO_FLORA_RATIO;
      }
    }
    //
    //  Include the effects of border-positioning, and then return an estimate
    //  based on food-supply and requirements:
    if (point != null) {
      foodSupply *= SiteUtils.worldOverlap(point, world, (int) forageRange);
    }
    final float estimate = (foodSupply / species.metabolism());
    return estimate;
  }
  
  
  public static int population(Species s, Target point) {
    final float range = forageRange(s);
    int pop = 0;
    for (Actor a : PlanUtils.subjectsInRange(point, range)) {
      if (a.species() == s) pop++;
    }
    return pop;
  }
  
  
  public static float crowding(Species s, Target point, Stage world) {
    Fauna fauna = null;
    Nest  nests = null;
    if (point instanceof Fauna) {
      fauna = (Fauna) point;
    }
    else if (point instanceof Nest) {
      nests = (Nest) point;
    }
    if (fauna != null && fauna.mind.home() instanceof Nest) {
      nests = (Nest) fauna.mind.home();
    }
    if (nests != null) {
      return nests.crowdRating(s);
    }
    
    final float idealPop = idealPopulation(s, point, world);
    if (idealPop <= 0) return 1;
    final int actualPop = population(s, point);
    return Nums.clamp(actualPop / idealPop, 0, 1);
  }
  
  
  public static float crowding(Actor fauna) {
    if (! (fauna instanceof Fauna)) return 0.5f;
    return crowding(fauna.species(), fauna, fauna.world());
  }
  
  
  public static float nestCrowding(Actor fauna) {
    if (! (fauna.mind.home() instanceof Nest)) return 1.0f;
    final Nest nest = (Nest) fauna.mind.home();
    return nest.crowdRating(fauna.species());
  }
  
  
  public static int forageRange(Species s) {
    return s.predator() ?
      PREDATOR_SEPARATION :
      DEFAULT_FORAGE_DIST ;
  }
  
  

  /*
  public static float crowdingFor(Target point, Species species, Stage world) {
    if (point == null || species == null) return 1;
    final boolean report = ratingVerbose && I.talkAbout == point;
    
    final float idealPop = idealPopulation(point, species, world);
    if ((int) idealPop <= 0) return 1;
    
    final Base   base     = Base.wildlife(world);
    final float  mass     = species.metabolism();
    final String category = species.type.name() ;
    final float
      allType     = base.demands.supplyAround(point, category, -1),
      allSpecies  = base.demands.supplyAround(point, species , -1),
      rarity      = Nums.clamp(1 - (allSpecies / allType), 0, 1),
      competition = allType / ((1 + rarity) * mass);
    
    if (report) I.reportVars(
      "\nGetting crowding at "+point+" for "+species, "  ",
      "all of type"     , allType    ,
      "all of species"  , allSpecies ,
      "rarity"          , rarity     ,
      "competition"     , competition,
      "metabolic mass"  , mass       ,
      "ideal population", idealPop   
    );
    return competition / (int) idealPop;
  }
  
  
  public static float crowdingFor(Actor fauna) {
    final Target home = fauna.mind.home();
    if (! (home instanceof Nest)) return 0.49f;
    final Nest nest = (Nest) home;
    if (nest.cachedIdealPop == -1) return 0;
    if (nest.cachedIdealPop ==  0) return 1;
    return nest.staff.lodgers().size() * 1f / nest.cachedIdealPop;
  }
  
  
  protected void impingeDemands(BaseDemands demands, int period) {
    final float  idealPop = idealPopulation(this, species, world);
    final float  mass     = species.metabolism() * idealPop;
    final String category = species.type.name();
    demands.impingeSupply(species , mass, period, this);
    demands.impingeSupply(category, mass, period, this);
  }
  //*/
  
  

  
  
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
    
    //  TODO:  Just have a fixed population-size per nest?
    
    //  TODO:  Consider moving the Sitings out to the individual Species'
    //  class-implementations.  (Blueprints too.)
    
    final Siting siting = new Siting(blueprint) {
      
      public float rateSettlementDemand(Base base) {
        return idealPopulation(s, null, base.world);
      }
      
      
      public float ratePointDemand(Base base, Target point, boolean exact) {
        
        if (! exact) {
          I.say("\nPoint is: "+point);
        }
        
        final Stage world = point.world();
        Target other = world.presences.nearestMatch(Venue.class, point, -1);
        final float distance;
        if (other == null) distance = world.size;
        else distance = Spacing.distance(point, other);
        
        if (distance <= MIN_SEPARATION) return -1;
        float distMod = 1;
        if (other instanceof Nest) {
          final Nest near = (Nest) other;
          final float spacing = Nums.max(
            forageRange(s),
            forageRange(near.species)
          );
          if (distance < spacing) distMod *= 1 - (distance / spacing);
        }
        else if (distance <= PREDATOR_SEPARATION) return -1;

        final float
          idealPop  = (int) idealPopulation(s, point, world),
          actualPop = population(s, point),
          crowding  = Nums.clamp(actualPop / idealPop, 0, 1),
          mass      = s.metabolism(),
          rating    = idealPop * mass * (1 - crowding);
        
        if (! exact) {
          I.say("  Rating/distMod: "+rating+"/"+distMod);
        }
        
        return rating * distMod;
      }
    };
    blueprint.linkWith(siting);
    return blueprint;
  }
  
  
  private static void populate(Stage world, Species with[], Species.Type type) {
    final Base wildlife = Base.wildlife(world);
    
    final Batch <Blueprint> blueprints = new Batch();
    for (Species s : with) if (s.type == type) {
      final Blueprint nestType = s.nestBlueprint();
      if (nestType != null) blueprints.add(nestType);
    }
    final Batch <Venue> placed = wildlife.setup.doFullPlacements(
      blueprints.toArray(Blueprint.class)
    );
    //
    //  Once placement is complete, we insert the actors into the world at a
    //  semi-random location near their lairs (deleting them in the process for
    //  species that don't nest.)
    for (Venue v : placed) if (v instanceof Nest) {
      final Species s = ((Nest) v).species;
      final boolean nests = s.fixedNesting();
      final float range = NestUtils.forageRange(s) / 2;
      
      if (! nests) v.exitWorld();
      
      int pop = Nums.round(idealPopulation(s, v, world), 1, false);
      while (pop-- > 0) {
        final Actor f = s.sampleFor(wildlife);
        Tile entry = Spacing.pickRandomTile(v, range, world);
        entry = Spacing.nearestOpenTile(entry, v);
        
        if (nests) {
          f.mind.setHome(v);
          if (Rand.yes() || entry == null) f.enterWorldAt(v, world);
        }
        if (! f.inWorld()) f.enterWorldAt(entry, world);
      }
    }
  }
  
  
  public static void populateFauna(Stage world, Species... available) {
    populate(world, available, Species.Type.BROWSER );
    populate(world, available, Species.Type.VERMIN  );
    populate(world, available, Species.Type.PREDATOR);
  }
  
  
  public static Nest findNestFor(Fauna fauna) {
    final Pick <Nest> pick = new Pick <Nest> (null, 0);
    
    final Stage      world   = fauna.world();
    final Species    species = fauna.species;
    final Batch <Nest> nests = new Batch <Nest> ();
    world.presences.sampleFromMap(fauna, world, 5, nests, Nest.class);
    //
    //  First, we check the rating for our current home-
    if (fauna.mind.home() instanceof Nest) {
      final Nest home = (Nest) fauna.mind.home();
      final float rating = 1 - home.crowdRating(species);
      pick.compare(home, rating * 1.1f);
    }
    //
    //  Then compare against any suitable nests nearby.
    for (Nest v : nests) if (v.species == species) {
      pick.compare(v, 1 - v.crowdRating(species));
    }
    //
    //  And finally, we consider the attraction of establishing an entirely new
    //  nest.
    final Base  wild  = Base.wildlife(world);
    final float range = NestUtils.forageRange(species) * 2;
    final Tile  at    = Spacing.pickRandomTile(fauna, range, world);
    final Nest  built = (Nest) fauna.species.nestBlueprint().createVenue(wild);
    
    built.setPosition(at.x, at.y, world);
    if (built.canPlace()) {
      pick.compare(built, 1 - crowding(species, at, world));
    }
    return pick.result();
  }
}




