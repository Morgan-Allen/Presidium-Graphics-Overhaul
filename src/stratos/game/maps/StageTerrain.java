/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.maps;
import stratos.game.common.*;
import stratos.game.wild.Habitat;
import stratos.graphics.common.*;
import stratos.graphics.terrain.*;
import stratos.util.*;




/*
terrain chance (scaled proportionately) = x * (1 - x) * (1 + tx)
x = moisture
t = terraform-progress  (1 as default).
//*/


public class StageTerrain implements TileConstants, Session.Saveable {
  
  
  final public static int
    MAX_INSOLATION  = 10,
    MAX_MOISTURE    = 10,
    MAX_RADIATION   = 10,
    SAMPLE_RESOLUTION = Stage.ZONE_SIZE;
  
  final public static String MINERAL_NAMES[] = {
    "Rubble", "Metal Ore", "Artifacts", "Fuel Isotopes"
  };
  
  final public static byte
    TYPE_METALS   = 1,
    TYPE_RUINS    = 2,
    TYPE_ISOTOPES = 3,
    TYPE_RUBBLE   = 0,
    
    NUM_MINERAL_TYPES  = 4 ,
    MAX_MINERAL_AMOUNT = 10,
    
    ROAD_NONE     = 0,
    ROAD_LIGHT    = 1,
    ROAD_HEAVY    = 2,
    
    TILE_VAR_LIMIT = 4;
  
  final static int
    TIME_INIT = -1,
    TIME_DONE = -2,
    INDEX_FERTILITY  = 0,
    INDEX_INSOLATION = 1,
    INDEX_MINERALS   = 2,
    INDEX_HABITATS   = 3,
    NUM_SAMPLE_MAPS  = INDEX_HABITATS + Habitat.ALL_HABITATS.length;
  
  final public int
    mapSize;
  private byte
    heightVals[][],
    typeIndex[][],
    varsIndex[][];
  
  private Habitat
    habitats[][];
  private byte
    minerals[][],
    paveVals[][],
    reserved[][];
  
  private TerrainSet meshSet;
  private LayerType roadLayer;
  private LayerType reservations;
  
  private static class Sample {
    final int measures[] = new int[NUM_SAMPLE_MAPS];
    int area = 0;
  }
  private Sample sampleGrid[][];
  private Sample globalSample;
  
  
  StageTerrain(
    Habitat[] gradient,
    byte typeIndex[][],
    byte varsIndex[][],
    byte heightVals[][]
  ) {
    this.mapSize = typeIndex.length;
    this.typeIndex = typeIndex;
    this.varsIndex = varsIndex;
    this.heightVals = heightVals;
    
    initHabitatFields();
    initSamples();
  }
  
  
  private void initHabitatFields() {
    habitats = new Habitat[mapSize][mapSize];
    for (Coord c : Visit.grid(0, 0, mapSize, mapSize, 1)) {
      habitats[c.x][c.y] = Habitat.ALL_HABITATS[typeIndex[c.x][c.y]];
    }
    minerals = new byte[mapSize][mapSize];
    paveVals = new byte[mapSize][mapSize];
    reserved = new byte[mapSize][mapSize];
  }
  
  
  public StageTerrain(Session s) throws Exception {
    s.cacheInstance(this);
    mapSize = s.loadInt();
    
    heightVals = new byte[mapSize + 1][mapSize + 1];
    typeIndex  = new byte[mapSize    ][mapSize    ];
    varsIndex  = new byte[mapSize    ][mapSize    ];
    
    initHabitatFields();
    
    s.loadByteArray(heightVals);
    s.loadByteArray(typeIndex);
    s.loadByteArray(varsIndex);
    
    s.loadByteArray(minerals);
    s.loadByteArray(paveVals);
    s.loadByteArray(reserved);
    initSamples();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(mapSize);
    s.saveByteArray(heightVals);
    s.saveByteArray(typeIndex);
    s.saveByteArray(varsIndex);
    
    s.saveByteArray(minerals);
    s.saveByteArray(paveVals);
    s.saveByteArray(reserved);
  }
  
  
  
  /**  Averages and sampling-
    */
  //  TODO:  TRY TO USE THE WILDLIFE-BASE'S DEMAND-MAP TO STORE THIS DATA
  
  private Sample sampleAt(int x, int y) {
    if (x < 0 || y < 0) return globalSample;
    return sampleGrid[x / SAMPLE_RESOLUTION][y / SAMPLE_RESOLUTION];
  }
  
  
  private void initSamples() {
    final int SGS = mapSize / SAMPLE_RESOLUTION;
    this.sampleGrid = new Sample[SGS][SGS];
    for (Coord c : Visit.grid(0, 0, SGS, SGS, 1)) {
      sampleGrid[c.x][c.y] = new Sample();
    }
    globalSample = new Sample();
    for (Coord c : Visit.grid(0, 0, mapSize, mapSize, 1)) {
      incSampleAt(c.x, c.y, habitatAt(c.x, c.y), 1);
    }
  }
  
  
  private void incSampleAt(int x, int y, Habitat h, int inc) {
    if (h == null) return;
    final Sample s = sampleAt(x, y);
    s.measures[INDEX_HABITATS + h.ID] += inc;
    s.measures[INDEX_INSOLATION     ] += h.insolation() * inc;
    s.measures[INDEX_MINERALS       ] += h.minerals  () * inc;
    s.measures[INDEX_FERTILITY      ] += h.moisture  () * inc;
    s.area += inc;
    if (s != globalSample) incSampleAt(-1, -1, h, inc);
  }
  
  
  private float sampleAt(Tile t, int index) {
    final int SR = SAMPLE_RESOLUTION;
    float
      cap = (mapSize / SR) - 1.01f,
      wX = Nums.clamp((t.x * 1f / SR) - 0.5f, 0, cap),
      wY = Nums.clamp((t.y * 1f / SR) - 0.5f, 0, cap);
    final int
      mX = (int) wX,
      mY = (int) wY,
      pX = mX + 1,
      pY = mY + 1;
    final float
      iX = 1 - (wX %= 1),
      iY = 1 - (wY %= 1);
    final Sample
      s1 = sampleGrid[mX][mY],
      s2 = sampleGrid[mX][pY],
      s3 = sampleGrid[pX][mY],
      s4 = sampleGrid[pX][pY];
    
    float sum = 0;
    sum += (s1.measures[index] * iX * iY) / s1.area;
    sum += (s2.measures[index] * iX * wY) / s2.area;
    sum += (s3.measures[index] * wX * iY) / s3.area;
    sum += (s4.measures[index] * wX * wY) / s4.area;
    return sum;
  }
  
  
  public float globalFertility() {
    return globalSample.measures[INDEX_FERTILITY] / 10f;
  }
  
  
  public float fertilitySample(Tile t) {
    return sampleAt(t, INDEX_FERTILITY) / 10;
  }
  
  
  public float insolationSample(Tile t) {
    return sampleAt(t, INDEX_INSOLATION) / 10;
  }
  
  
  public float mineralsSample(Tile t) {
    return sampleAt(t, INDEX_MINERALS) / 10;
  }
  
  
  public float habitatSample(Tile t, Habitat h) {
    return sampleAt(t, INDEX_HABITATS + h.ID);
  }
  
  
  
  /**  Habitats and mineral deposits-
    */
  private static Tile tempV[] = new Tile[9];
  
  
  public void setHabitat(Tile t, Habitat h) {
    final Habitat old = habitats[t.x][t.y];
    if (old == h) return;
    
    habitats [t.x][t.y] = h;
    typeIndex[t.x][t.y] = (byte) h.ID;
    incSampleAt(t.x, t.y, old, -1);
    incSampleAt(t.x, t.y, h  ,  1);
    
    t.refreshHabitat();
    for (Tile n : t.vicinity(tempV)) if (n != null) {
      meshSet.flagUpdateAt(n.x, n.y);
    }
  }
  
  
  public Habitat habitatAt(int x, int y) {
    try { return habitats[x][y]; }
    catch (ArrayIndexOutOfBoundsException e) { return null; }
  }
  
  
  public float mineralsAt(Tile t, byte type) {
    final byte m = minerals[t.x][t.y];
    return m == -1 ? 0 : (1 + (m % MAX_MINERAL_AMOUNT));
  }
  
  
  public byte mineralType(Tile t) {
    final byte value = minerals[t.x][t.y];
    if (value == -1) return TYPE_RUBBLE;
    return (byte) (value / MAX_MINERAL_AMOUNT);
  }
  
  
  public void setMinerals(Tile t, byte type, int amount) {
    byte value = 0;
    if (amount > 0) {
      value += (type * MAX_MINERAL_AMOUNT);
      value += Nums.clamp(amount - 1, MAX_MINERAL_AMOUNT);
    }
    else value = -1;
    minerals[t.x][t.y] = value;
  }
  
  
  public float mineralsAt(Tile t) {
    return mineralsAt(t, mineralType(t));
  }
  
  
  public float trueHeight(float x, float y) {
    return Nums.sampleMap(mapSize, heightVals, x, y) / 4;
  }
  
  
  public int varAt(Tile t) {
    return varsIndex[t.x][t.y];
  }
  
  
  
  /**  Methods for handling road-masking of tiles-
    */
  public boolean isRoad(Tile t) {
    return paveVals[t.x][t.y] > 0;
  }
  
  
  public int roadType(Tile t) {
    return paveVals[t.x][t.y];
  }
  
  
  public void setRoadType(Tile t, byte level) {
    final byte oldLevel = paveVals[t.x][t.y];
    paveVals[t.x][t.y] = level;
    
    if (level != oldLevel) for (Tile n : t.vicinity(tempV)) if (n != null) {
      meshSet.flagUpdateAt(n.x, n.y, roadLayer);
    }
  }
  
  
  
  /**  And finally, reservations-
    */
  public void setReservedAt(Tile t, boolean reserved) {
    final boolean oldRes = this.reserved[t.x][t.y] > 0;
    this.reserved[t.x][t.y] = (byte) (reserved ? 1 : 0);
    
    if (reserved != oldRes) for (Tile n : t.vicinity(tempV)) if (n != null) {
      meshSet.flagUpdateAt(n.x, n.y, reservations);
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void initTerrainMesh(Habitat habitats[]) {
    int lID = -1;
    final LayerType layers[] = new LayerType[habitats.length + 2];
    
    while (++lID < habitats.length) {
      final int layerIndex = lID;
      final Habitat h = habitats[lID];
      layers[lID] = new LayerType(h.animTex, false, lID, h.name) {
        protected boolean maskedAt(int tx, int ty, TerrainSet terrain) {
          return typeIndex[tx][ty] == layerIndex;
        }
        protected int variantAt(int tx, int ty, TerrainSet terrain) {
          return varsIndex[tx][ty];
        }
      };
    }
    
    roadLayer = layers[lID] = new LayerType(
      Habitat.ROAD_TEXTURE, true, lID, "roads"
    ) {
      protected boolean maskedAt(int tx, int ty, TerrainSet terrain) {
        return paveVals[tx][ty] > 0;
      }
      protected int variantAt(int tx, int ty, TerrainSet terrain) {
        return ((tx + ty) % 3 == 0) ? 0 : 1;
      }
    };
    
    reservations = layers[lID + 1] = new LayerType(
      Habitat.RESERVE_TEXTURE, true, lID + 1, "reservations"
    ) {
      protected boolean maskedAt(int tx, int ty, TerrainSet terrain) {
        return reserved[tx][ty] > 0;
      }
      protected int variantAt(int tx, int ty, TerrainSet terrain) {
        return 0;
      }
    };
    
    meshSet = new TerrainSet(mapSize, -1, typeIndex, varsIndex, layers);
  }
  
  
  public TerrainChunk createOverlay(
    final Stage world, Tile tiles[],
    final boolean innerFringe, ImageAsset tex
  ) {
    if (tiles == null || tiles.length < 1) I.complain("No tiles in overlay!");
    final int maxT = tiles.length * 2;
    
    final Table <Tile, Tile> pathTable = new Table <Tile, Tile> (maxT);
    Box2D area = null;
    
    for (Tile t : tiles) if (t != null) {
      if (area == null) area = new Box2D().set(t.x, t.y, 0, 0);
      pathTable.put(t, t);
      area.include(t.x, t.y, 0.5f);
    }
    
    final LayerType layer = new LayerType(tex, innerFringe, -1, "overlay") {
      protected boolean maskedAt(int tx, int ty, TerrainSet terrain) {
        final Tile t = world.tileAt(tx, ty);
        return (t == null) ? false : (pathTable.get(t) != null);
      }
      protected int variantAt(int tx, int ty, TerrainSet terrain) {
        return 0;
      }
    };
    return createOverlay(area, layer);
  }
  
  
  public TerrainChunk createOverlay(
    Box2D b, LayerType layer
  ) {
    final int
      minX = (int) (b.xpos() + 0.5f),
      minY = (int) (b.ypos() + 0.5f),
      dimX = (int) (b.xmax() + 0.5f) - minX,
      dimY = (int) (b.ymax() + 0.5f) - minY;
    final TerrainChunk overlay = new TerrainChunk(
      dimX, dimY, minX, minY,
      layer, meshSet
    );
    overlay.generateMeshData();
    return overlay;
  }
  
  
  public void readyAllMeshes() {
    meshSet.refreshAllMeshes();
  }
  
  
  public void renderFor(Box2D area, Rendering rendering, float time) {
    meshSet.renderWithin(area, rendering);
  }
}











