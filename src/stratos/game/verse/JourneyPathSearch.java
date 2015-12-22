/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.verse;
import stratos.util.*;



//  TODO:  Allow filtering of routes by transport type- (e.g, so you can't
//  voyage offworld in a cargo barge...)


public class JourneyPathSearch extends Search <Sector> {
  
  
  final Sector destination;
  final Table <Sector, Entry> entries = new Table();
  
  
  JourneyPathSearch(Sector orig, Sector dest) {
    super(orig, -1);
    this.destination = dest;
  }
  
  
  protected Sector[] adjacent(Sector spot) {
    return spot.borders.toArray(Sector.class);
  }
  
  
  protected boolean endSearch(Sector best) {
    return best == destination;
  }
  
  
  protected float cost(Sector prior, Sector spot) {
    final Sector.Separation s = prior.separations.get(spot);
    return s == null ? -1 : s.tripTime;
  }
  
  
  protected float estimate(Sector spot) {
    return 0;
  }
  
  
  protected void setEntry(Sector spot, Entry flag) {
    entries.put(spot, flag);
  }
  
  
  protected Entry entryFor(Sector spot) {
    return entries.get(spot);
  }

  
  
  /**  Auxiliary query method for 'blind' searches:
    */
  protected Sector[] pathTo(Sector s) {
    final Entry from = entries.get(s);
    return from == null ? null :bestPath(from, Sector.class, -1);
  }
}





