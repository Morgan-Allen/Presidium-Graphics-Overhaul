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



