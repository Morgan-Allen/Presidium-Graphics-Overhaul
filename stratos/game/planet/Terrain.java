/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.planet ;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.terrain.*;
import stratos.util.*;


/*
Forest, Grassland, Barrens, Spice Desert.
Ocean, Shallows, Albedan, Swamp.
Mesa, Deep Desert, Cursed Earth, Strip Mining.

terrain chance (scaled proportionately) = x * (1 - x) * (1 + tx)
x = moisture
t = terraform-progress  (1 as default).
//*/


public class Terrain implements TileConstants, Session.Saveable {
  
  
  final public static int
    MAX_INSOLATION  = 10,
    MAX_MOISTURE    = 10,
    MAX_RADIATION   = 10 ;
  final public static String MINERAL_NAMES[] = {
    "None", "Metal Ore", "Artifacts", "Fuel Isotopes"
  } ;
  
  final public static byte
    TYPE_METALS   = 1,
    TYPE_RUINS    = 2,
    TYPE_ISOTOPES = 3,
    TYPE_NOTHING  = 0,
    
    DEGREE_TRACE  = 1,
    DEGREE_COMMON = 2,
    DEGREE_HEAVY  = 3,
    DEGREE_TAKEN  = 0,
    
    AMOUNT_TRACE  = 1,
    AMOUNT_COMMON = 3,
    AMOUNT_HEAVY  = 9,
    
    NUM_TYPES = 4,
    NUM_DEGREES = 4,
    MAX_MINERAL_AMOUNT = 10 ;
  
  final static int
    TIME_INIT = -1,
    TIME_DONE = -2 ;
  
  final public int
    mapSize ;
  private byte
    heightVals[][],
    typeIndex[][],
    varsIndex[][] ;
  
  final Habitat
    habitats[][] ;
  private byte
    minerals[][],
    roadCounter[][],
    dirtVals[][] ;
  
  private TerrainSet meshSet ;
  private LayerType dirtLayer, roadLayer;
  
  
  
  Terrain(
    Habitat[] gradient,
    byte typeIndex[][],
    byte varsIndex[][],
    byte heightVals[][]
  ) {
    this.mapSize = typeIndex.length ;
    this.typeIndex = typeIndex ;
    this.varsIndex = varsIndex ;
    this.heightVals = heightVals ;
    this.roadCounter = new byte[mapSize][mapSize] ;
    this.habitats = new Habitat[mapSize][mapSize] ;
    for (Coord c : Visit.grid(0, 0, mapSize, mapSize, 1)) {
      habitats[c.x][c.y] = Habitat.ALL_HABITATS[typeIndex[c.x][c.y]] ;
    }
    this.minerals = new byte[mapSize][mapSize] ;
    this.dirtVals = new byte[mapSize][mapSize] ;
  }
  
  
  public Terrain(Session s) throws Exception {
    s.cacheInstance(this) ;
    mapSize = s.loadInt() ;
    
    heightVals = new byte[mapSize + 1][mapSize + 1] ;
    typeIndex = new byte[mapSize][mapSize] ;
    varsIndex = new byte[mapSize][mapSize] ;
    s.loadByteArray(heightVals) ;
    s.loadByteArray(typeIndex) ;
    s.loadByteArray(varsIndex) ;
    
    roadCounter = new byte[mapSize][mapSize] ;
    s.loadByteArray(roadCounter) ;
    
    habitats = new Habitat[mapSize][mapSize] ;
    for (Coord c : Visit.grid(0, 0, mapSize, mapSize, 1)) {
      habitats[c.x][c.y] = Habitat.ALL_HABITATS[typeIndex[c.x][c.y]] ;
    }
    minerals = new byte[mapSize][mapSize] ;
    dirtVals = new byte[mapSize][mapSize] ;
    s.loadByteArray(minerals) ;
    s.loadByteArray(dirtVals) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(mapSize) ;
    s.saveByteArray(heightVals) ;
    s.saveByteArray(typeIndex) ;
    s.saveByteArray(varsIndex) ;
    
    s.saveByteArray(roadCounter) ;
    s.saveByteArray(minerals) ;
    s.saveByteArray(dirtVals) ;
  }
  
  
  
  /**  Habitats and mineral deposits-
    */
  private static Tile tempV[] = new Tile[9] ;
  
  
  public void setHabitat(Tile t, Habitat h) {
    habitats[t.x][t.y] = h ;
    typeIndex[t.x][t.y] = (byte) h.ID ;
    t.refreshHabitat() ;
    for (Tile n : t.vicinity(tempV)) if (n != null) {
      meshSet.flagUpdateAt(n.x, n.y);
    }
  }
  
  
  public Habitat habitatAt(int x, int y) {
    try { return habitats[x][y] ; }
    catch (ArrayIndexOutOfBoundsException e) { return null ; }
  }
  
  
  public float mineralsAt(Tile t, byte type) {
    byte m = minerals[t.x][t.y] ;
    if (m == 0) return 0 ;
    if (m / NUM_TYPES != type) return 0 ;
    switch (m % NUM_TYPES) {
      case (DEGREE_TRACE)  : return AMOUNT_TRACE  ;
      case (DEGREE_COMMON) : return AMOUNT_COMMON ;
      case (DEGREE_HEAVY ) : return AMOUNT_HEAVY  ;
    }
    return 0 ;
  }
  
  
  public byte mineralType(Tile t) {
    return (byte) (minerals[t.x][t.y] / NUM_TYPES) ;
  }
  
  
  public byte mineralDegree(Tile t) {
    return (byte) (minerals[t.x][t.y] % NUM_TYPES) ;
  }
  
  
  public float extractMineralAt(Tile t, byte type) {
    final float amount = mineralsAt(t, type) ;
    if (amount <= 0) I.complain("Can't extract that mineral type!") ;
    minerals[t.x][t.y] = (byte) ((type * NUM_DEGREES) + DEGREE_TAKEN) ;
    return amount ;
  }
  
  
  public void setMinerals(Tile t, byte type, byte degree) {
    minerals[t.x][t.y] = (byte) ((type * NUM_DEGREES) + degree) ;
  }
  
  
  public void incMineralDegree(Tile t, byte type, int inc) {
    int degree = Visit.clamp(mineralDegree(t) + inc, DEGREE_HEAVY + 1) ;
    setMinerals(t, type, (byte) degree) ;
  }
  
  
  public void setSqualor(Tile t, byte newVal) {
    final byte oldVal = dirtVals[t.x][t.y] ;
    dirtVals[t.x][t.y] = newVal ;
    if (oldVal != newVal) for (Tile n : t.vicinity(tempV)) if (n != null) {
      meshSet.flagUpdateAt(n.x, n.y, dirtLayer);
      //final MeshPatch patch = patches[t.x / patchSize][t.y / patchSize] ;
      //patch.updateDirt = true ;
    }
  }
  
  
  public float trueHeight(float x, float y) {
    return Visit.sampleMap(mapSize, heightVals, x, y) / 4 ;
  }
  
  
  public int varAt(Tile t) {
    return varsIndex[t.x][t.y] ;
  }
  
  
  
  /**  Methods for handling road-masking of tiles-
    */
  public boolean isRoad(Tile t) {
    return roadCounter[t.x][t.y] > 0 ;
  }
  
  
  public int roadMask(Tile t) {
    return roadCounter[t.x][t.y] ;
  }
  
  
  public void maskAsPaved(Tile tiles[], boolean is) {
    if (tiles == null || tiles.length == 0) return ;
    //Box2D bounds = null ;
    for (Tile t : tiles) if (t != null) {
      final boolean wasRoad = roadCounter[t.x][t.y] > 0 ;
      final byte c = (roadCounter[t.x][t.y] += is ? 1 : -1) ;
      if (c < 0) I.complain("CANNOT HAVE NEGATIVE ROAD COUNTER: "+t) ;
      if (wasRoad == roadCounter[t.x][t.y] > 0) continue ;
      //if (bounds == null) bounds = new Box2D().set(t.x, t.y, 0, 0) ;
      //bounds.include(t.x, t.y, 0.5f) ;
      
      for (Tile n : t.vicinity(tempV)) if (n != null) {
        meshSet.flagUpdateAt(n.x, n.y, roadLayer);
      }
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void initTerrainMesh(Habitat habitats[]) {
    int lID = -1;
    final LayerType layers[] = new LayerType[habitats.length + 2];
    
    while (++lID < habitats.length) {
      final int layerIndex = lID;
      layers[lID] = new LayerType(habitats[lID].animTex, false, lID) {
        protected boolean maskedAt(int tx, int ty, TerrainSet terrain) {
          return typeIndex[tx][ty] == layerIndex;
        }
        protected int variantAt(int tx, int ty, TerrainSet terrain) {
          return varsIndex[tx][ty];
        }
      };
    }
    
    dirtLayer = layers[lID] = new LayerType(
      Habitat.SQUALOR_TEXTURE, true, lID
    ) {
      protected boolean maskedAt(int tx, int ty, TerrainSet terrain) {
        final byte ID = varsIndex[tx][ty] ;
        final byte squalor = dirtVals[tx][ty] ;
        return ID * squalor > 10 ;
      }
      protected int variantAt(int tx, int ty, TerrainSet terrain) {
        return (tx + ty) % 4 ;
      }
    };
    lID++;
    roadLayer = layers[lID] = new LayerType(
      Habitat.ROAD_TEXTURE, true, lID
    ) {
      protected boolean maskedAt(int tx, int ty, TerrainSet terrain) {
        return roadCounter[tx][ty] > 0 ;
      }
      protected int variantAt(int tx, int ty, TerrainSet terrain) {
        return (tx + ty) % 4 ;
      }
    };
    
    meshSet = new TerrainSet(mapSize, -1, typeIndex, layers);
    meshSet.refreshAllMeshes();
  }
  
  
  public TerrainChunk createOverlay(
    final World world, Tile tiles[], final boolean nullsCount, String tex
  ) {
    if (tiles == null || tiles.length < 1) I.complain("No tiles in overlay!");
    final Table <Tile, Tile> pathTable = new Table(tiles.length * 2) ;
    Box2D area = null ;
    for (Tile t : tiles) if (t != null) {
      if (area == null) area = new Box2D().set(t.x, t.y, 0, 0) ;
      pathTable.put(t, t) ;
      area.include(t.x, t.y, 0.5f) ;
    }
    final LayerType layer = new LayerType(tex, false, -1) {
      protected boolean maskedAt(int tx, int ty, TerrainSet terrain) {
        final Tile t = world.tileAt(tx, ty) ;
        return (t == null) ? false : (pathTable.get(t) != null) ;
      }
      protected int variantAt(int tx, int ty, TerrainSet terrain) {
        return 0;
      }
    };
    return createOverlay(area, layer);
  }
  
  
  public TerrainChunk createOverlay(
    Box2D area, LayerType layer
  ) {
    final int
      minX = (int) Math.ceil(area.xpos()),
      minY = (int) Math.ceil(area.ypos()),
      dimX = (int) area.xdim(),
      dimY = (int) area.ydim() ;
    final TerrainChunk overlay = new TerrainChunk(
      minX, minY, minX + dimX, minY + dimY,
      layer, meshSet
    ) ;
    overlay.generateMesh();
    return overlay ;
  }
  
  
  public void renderFor(Box2D area, Rendering rendering, float time) {
    meshSet.renderWithin(area, rendering);
  }
  
  
  public TerrainSet meshSet() {
    return meshSet;
  }
}





