/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.maps;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.util.*;



public class SiteDivision {
  
  
  /**  Data fields and save/load methods-
    */
  final public static SiteDivision NONE = new SiteDivision();
  static {
    NONE.reserved = new Tile[0];
    NONE.toPave   = new Tile[0];
    NONE.useMap   = new byte[0][0];
  }
  
  
  public Tile reserved[];
  public Tile toPave[];
  public byte useMap[][];
  
  
  public static void saveTo(Session s, SiteDivision d) throws Exception {
    if (d == null || d == NONE) { s.saveBool(false); return; }
    else s.saveBool(true);
    
    s.saveObjectArray(d.reserved);
    s.saveObjectArray(d.toPave  );
    s.saveInt(d.useMap.length   );
    s.saveInt(d.useMap[0].length);
    s.saveByteArray(d.useMap);
  }
  
  
  public static SiteDivision loadFrom(Session s) throws Exception {
    if (! s.loadBool()) return NONE;
    
    final SiteDivision d = new SiteDivision();
    d.reserved = (Tile[]) s.loadObjectArray(Tile.class);
    d.toPave   = (Tile[]) s.loadObjectArray(Tile.class);
    d.useMap   = new byte[s.loadInt()][s.loadInt()];
    s.loadByteArray(d.useMap);
    return d;
  }
  
  
  /**  Utility construction methods-
    */
  public static SiteDivision forArea(
    Venue v, Box2D area, int face, int spacing,
    Fixture... excluded
  ) {
    if (v.origin() == null) return null;
    final Stage world = v.origin().world;
    
    area = new Box2D(area).cropBy(world.area());
    final Tile o = world.tileAt(area.xpos(), area.ypos());
    
    final Vec2D central = area.centre();
    final boolean across = true;// face == Venue.FACE_EAST || face == Venue.FACE_WEST;
    final int halfDim = (int) ((across ? area.ydim() : area.xdim()) / 2);
    
    I.say("\nCentral point is: "+central+" area: "+area);
    
    Batch <Tile> paved = new Batch();
    Batch <Tile> claim = new Batch();
    final Tile batch[] = new Tile[8];
    final byte useMap[][] = new byte[(int) area.xdim()][(int) area.ydim()];
    
    for (Tile t : world.tilesIn(area, false)) {
      //final int x = t.x - o.x, y = t.y - o.y;
      byte use = 1;
      
      //
      //  Any tiles under current or projected structures should be ignored.
      if (use == 1) if (t.owningTier() >= Owner.TIER_PRIVATE) {
        use = -1;
      }
      if (use == 1) for (Fixture f : excluded) {
        if (! f.footprint().contains(t.x, t.y, 0)) continue;
        use = -1; break;
      }
      //
      //  Tiles adjacent to current or projected structures should be paved.
      if (use == 1) for (Tile n : t.allAdjacent(batch)) {
        if (n != null && n.owningTier() >= Owner.TIER_PRIVATE) {
          use = 0;
          break;
        }
      }
      if (use == 1) for (Fixture f : excluded) {
        if (! f.footprint().contains(t.x, t.y, -1)) continue;
        use = 0; break;
      }
      
      //
      //  TODO:  Explain this properly...
      final boolean midSplit = ((halfDim - 1) % spacing) != 0;
      if (use == 1) {
        final int
          dist = across ?
            halfDim - (int) Nums.abs(central.y - t.y) :
            halfDim - (int) Nums.abs(central.x - t.x),
          perpDist = across ? (t.x - o.x) : (t.y - o.y);
        
        use = 1;
        if ((dist % spacing == 0) || (dist == halfDim && midSplit)) {
          use = 0;
        }
        else if (perpDist == halfDim) {
          use = 0;
        }
        else if (dist % (spacing * 2) == 1) {
          use = 2;
        }
      }
      
      final int x = t.x - o.x, y = t.y - o.y;
      useMap[x][y] = use;
      if (use == 0) paved.add(t);
      if (use >= 1) claim.add(t);
    }
    
    //  TODO:  You may want to do some basic screening here too?
    for (Tile t : Spacing.perimeter(area, world)) paved.add(t);
    
    final SiteDivision d = new SiteDivision();
    d.reserved = claim.toArray(Tile.class);
    d.toPave   = paved.toArray(Tile.class);
    d.useMap   = useMap;
    return d;
  }
  
  
  
  /**  Utility methods for queries-
    */
  public byte useType(Tile t, Box2D areaClaimed) {
    final Tile o = t.world.tileAt(areaClaimed.xpos(), areaClaimed.ypos());
    if (o == null) return -1;
    try { return useMap[t.x - o.x][t.y - o.y]; }
    catch (ArrayIndexOutOfBoundsException e) { return -1; }
  }
  
  
}











