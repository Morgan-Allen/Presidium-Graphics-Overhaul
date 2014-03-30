/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */



package stratos.game.planet ;
import stratos.game.common.*;
import stratos.util.*;



public abstract class SitingPass {
  
  
  int resolution = World.SECTOR_SIZE;
  
  
  private static class Site {
    Tile centre = null;
    float rating = -1;
    boolean placed = false;
  }
  
  
  //  TODO:  Return a list of the sites generated...
  public void applyPassTo(final World world, final int numSites) {
    
    final List <Site> allSites = new List <Site> () {
      protected float queuePriority(Site site) {
        return site.rating;
      }
    };
    
    final int scanSize = (int) Math.ceil(world.size * 1f / resolution);
    final RandomScan scan = new RandomScan(scanSize) {
      protected void scanAt(int x, int y) {
        final Site site = new Site();
        site.centre = world.tileAt(
          (x + 0.5f) * resolution,
          (y + 0.5f) * resolution
        );
        site.rating = rateSite(site.centre);
        allSites.add(site);
      }
    };
    scan.doFullScan();
    allSites.queueSort();
    
    int sited = 0;
    for (Site site : allSites) {
      if (createSite(site.centre)) sited++;
      if (sited >= numSites) break;
    }
  }
  
  
  protected abstract float rateSite(Tile centre);
  protected abstract boolean createSite(Tile centre);
}




  
  
  /**  Field definitions, constructors and setup methods.
    */
  /*
  final static int
    WORLD_SIZE_CATS[] = { 32, 64, 128, 256 },
    MAJOR_LAIR_COUNTS[] = { 0, 0, 1, 4 },
    MINOR_LAIR_COUNTS[] = { 0, 1, 4, 8 },
    VARIANCE = 4 ;
  
  
  final World world ;
  final TerrainGen terGen ;
  
  private float fertilityLevels[][], totalFertility ;
  private int numMinor, numMajor ;
  private List <Vec2D> allSites = new List <Vec2D> () ;
  
  
  public EcologyGen(World world, TerrainGen terGen) {
    this.world = world ;
    this.terGen = terGen ;
    summariseFertilities() ;
    calcNumSites() ;
  }
  
  
  
  
  /**  General helper methods-
    */
  /*
  private void summariseFertilities() {
    final int SS = World.SECTOR_SIZE, SR = world.size / World.SECTOR_SIZE ;
    fertilityLevels = new float[SR][SR] ;
    for (Coord c : Visit.grid(0, 0, world.size, world.size, 1)) {
      final Tile t = world.tileAt(c.x, c.y) ;
      final Habitat h = t.habitat() ;
      float f = h.moisture() ;
      if (! h.pathClear) f = -5 ;
      if (h == Habitat.CURSED_EARTH) f = -10 ;
      fertilityLevels[c.x / SS][c.y / SS] += f ;
      totalFertility += f ;
    }
    for (Coord c : Visit.grid(0, 0, SR, SR, 1)) {
      fertilityLevels[c.x][c.y] /= SS * SS * 10 ;
    }
    totalFertility /= world.size * world.size * 10 ;
  }
  
  
  private void calcNumSites() {
    int i = WORLD_SIZE_CATS.length ;
    for (; i-- > 0 ;) {
      if (WORLD_SIZE_CATS[i] == world.size) break ;
    }
    numMajor = MAJOR_LAIR_COUNTS[i] ;
    numMinor = MINOR_LAIR_COUNTS[i] ;
    final int majorVar = numMajor / VARIANCE, minorVar = numMinor / VARIANCE ;
    numMajor += Rand.index((majorVar * 2) + 1) - majorVar ;
    numMinor += Rand.index((minorVar * 2) + 1) - minorVar ;
  }
  
  
  private Coord findBasePosition(
    Vec2D preferred, float fertilityMult
  ) {
    final int SR = fertilityLevels.length ;
    Coord best = null ;
    float bestRating = Float.NEGATIVE_INFINITY ;
    
    for (Coord c : Visit.grid(0, 0, SR, SR, 1)) {
      final Habitat base = terGen.baseHabitat(c, World.SECTOR_SIZE) ;
      if (! base.pathClear) continue ;
      
      float rating = fertilityLevels[c.x][c.y], distPenalty = 0 ;
      if (fertilityMult > 0 && rating < (1 - fertilityMult)) continue ;
      if (fertilityMult < 0 && base.moisture() > 5) continue ;
      
      for (Vec2D pos : allSites) {
        final float dist = pos.pointDist(c.x, c.y) ;
        distPenalty += 1f / (1 + dist) ;
      }
      if (allSites.size() > 1) distPenalty /= allSites.size() ;
      
      if (preferred != null) {
        final float dist = preferred.pointDist(c.x, c.y) ;
        distPenalty += dist / SR ;
      }
      rating = (rating * fertilityMult) - distPenalty ;
      if (rating > bestRating) { best = new Coord(c) ; bestRating = rating ; }
    }
    
    if (best == null) return null ;
    allSites.add(new Vec2D(best.x, best.y)) ;
    return best ;
  }
  
  
  
  
  /**  Placement of natural flora and animal dens/populations-
    */
  /*
  public void populateFlora() {
    //
    //  Migrate the population code for the Flora class over to here?
    for (Coord c : Visit.grid(0, 0, world.size, world.size, 1)) {
      Flora.tryGrowthAt(c.x, c.y, world, true) ;
    }
  }
  
//}
//*/




