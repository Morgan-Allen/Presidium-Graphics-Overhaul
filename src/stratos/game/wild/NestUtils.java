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
  
  
  public static Nest createNestFor(Actor a) {
    final Species s = a.species();
    if (s == null || s.nestBlueprint() == null) return null;
    return (Nest) s.nestBlueprint().createVenue(a.base());
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
    
    float numBrowseS = 0, numOtherS = 0;
    for (Species s : speciesOrder) {
      if (! s.predator()) {
        if (s.preyedOn()) numBrowseS++;
        else numBrowseS += 0.5f;
      }
      else numOtherS++;
    }
    
    float sumFlora = world.terrain().globalFertility();
    float sumPreyedOn = 0, sumPredators = 0;
    
    for (Species s : speciesOrder) if (! s.predator()) {
      float supports = sumFlora;
      supports /= s.metabolism() * numBrowseS * BROWSER_TO_FLORA_RATIO;
      if (! s.preyedOn()) supports /= 2;
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
      I.say("  Sum preyed on: "+sumPreyedOn );
      I.say("  Sum predators: "+sumPredators);
      
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
      
      Pick <StagePatch> placing = new Pick(0);
      for (StagePatch r : world.regions.allGridRegions()) {
        
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



