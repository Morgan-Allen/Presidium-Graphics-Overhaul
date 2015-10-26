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
    ratingVerbose = true ,
    updateVerbose = false;
  
  
  public static float nestCrowding(Actor fauna) {
    if (! (fauna.mind.home() instanceof Nest)) return 1.0f;
    final Nest nest = (Nest) fauna.mind.home();
    return nest.crowdRating(fauna.species());
  }
  
  
  public static float crowding(Actor fauna) {
    
    //  TODO:  This raises the problem of whether and how non-nesting species
    //  know when to reproduce.
    
    return nestCrowding(fauna);
  }
  
  
  public static float crowding(Species s, Nest nest, Stage world) {
    int limit = nestLimit(s);
    return nest.staff.lodgers().size() / limit;
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
  
  
  //
  //  TODO:  This is working well for purposes of initial setup.  But I need
  //  to translate it back into a SitingPass, and I need to handle migratory
  //  species.
  
  //  In principle, all you have to do is take global demand for a species and
  //  divide it by local crowding.
  

  public static void populateFauna(Stage world, Species... speciesOrder) {
    final boolean report = true;
    
    final Base base = Base.wildlife(world);
    final Tally <Species> idealNumbers  = new Tally <Species> ();
    final Tally <Species> actualNumbers = new Tally <Species> ();
    
    int numBrowseS = 0, numOtherS = 0;
    
    for (Species s : speciesOrder) {
      if (! s.predator()) numBrowseS++;
      else numOtherS++;
    }
    
    float sumFlora = world.terrain().globalFertility();
    float sumPreyedOn = 0, sumPredators = 0;
    
    for (Species s : speciesOrder) if (! s.predator()) {
      float supports = sumFlora;
      supports /= s.metabolism() * numBrowseS * BROWSER_TO_FLORA_RATIO;
      supports = Nums.round(supports, 1, false);
      idealNumbers.add(supports, s);
      if (s.preyedOn()) sumPreyedOn += supports * s.metabolism();
    }
    
    for (Species s : speciesOrder) if (! s.browser()) {
      float supports = sumPreyedOn;
      supports /= s.metabolism() * numOtherS * PREDATOR_TO_PREY_RATIO;
      supports = Nums.round(supports, 1, false);
      idealNumbers.add(supports, s);
      if (s.predator()) sumPredators += supports * s.metabolism();
    }
    
    if (report) {
      I.say("\nPopulating fauna within world...");
      
      for (Species s : speciesOrder) {
        I.say("  Ideal population of "+s+": "+idealNumbers.valueFor(s));
      }
    }
    
    while (true) {
      
      final Pick <Species> pick = new Pick(0);
      for (Species s : speciesOrder) {
        float gap = idealNumbers.valueFor(s) - actualNumbers.valueFor(s);
        pick.compare(s, gap);
      }
      if (pick.empty()) break;
      
      final Species species   = pick.result();
      final int     popGap    = Nums.round(pick.bestRating(), 1, false);
      final int     nestLimit = nestLimit(species);
      final float   range     = forageRange(species) * 2;
      
      Pick <StageRegion> placing = new Pick(0);
      for (StageRegion r : world.regions.allGroundRegions()) {
        
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
        if (entry == null) continue;
        
        final Actor f = species.sampleFor(base);
        actualNumbers.add(1, species);
        
        if (nests) {
          f.mind.setHome(nest);
          if (Rand.yes() || entry == null) f.enterWorldAt(nest, world);
        }
        if (! f.inWorld()) f.enterWorldAt(entry, world);
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



  /*
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
    final float mult = Nums.clamp(s.metabolism(), 1, 1.5f);
    return (int) (mult * (s.predator() ?
      PREDATOR_SEPARATION :
      DEFAULT_FORAGE_DIST
    ));
  }
  //*/
  
    
    /*
    //  TODO:  Just have a fixed population-size per nest?
    
    //  TODO:  Consider moving the Sitings out to the individual Species'
    //  class-implementations.  (Blueprints too.)
    
    final Siting siting = new Siting(blueprint) {
      
      public float rateSettlementDemand(Base base) {
        final float totalPop = idealPopulation(s, null, base.world);
        if (ratingVerbose || true) {
          I.say("\n\nIDEAL TOTAL POPULATION FOR "+s+": "+totalPop);
        }
        return totalPop;
      }
      
      public float ratePointDemand(Base base, Target point, boolean exact) {
        
        final Stage world = point.world();
        Target other = world.presences.nearestMatch(Venue.class, point, -1);
        final float distance;
        if (other == null) distance = world.size;
        else distance = Spacing.distance(point, other);
        
        //  TODO:  It's not enough to just check the nearest venue.  There
        //  might be another venue just past that which actually has a larger
        //  exclusion-radius.
        
        float distMod = 1;
        if (other instanceof Nest) {
          final Nest near = (Nest) other;
          final float spacing = Nums.max(
            forageRange(s), forageRange(near.species)
          );
          if (distance < Nums.max(2, spacing / 2)) return -1;
          if (distance < spacing) distMod *= 1 - (distance / spacing);
        }
        else if (distance <= PREDATOR_SEPARATION) return -1;

        final float
          idealPop  = (int) idealPopulation(s, point, world),
          actualPop = population(s, point),
          crowding  = Nums.clamp(actualPop / idealPop, 0, 1),
          mass      = s.metabolism(),
          rating    = idealPop * mass * (1 - crowding);
        
        if (ratingVerbose && ! exact) {
          I.say("\nGetting demand for "+s+" at: "+point);
          I.say("  Population:     "+actualPop+"/"+idealPop);
          I.say("  Crowding/mass:  "+crowding+"/"+mass);
          I.say("  Rating/distMod: "+rating+"/"+distMod);
        }
        
        return rating * distMod;
      }
    };
    blueprint.linkWith(siting);
    //*/
  
  
  /*
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
  //*/
  
  
  /*
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
  //*/
