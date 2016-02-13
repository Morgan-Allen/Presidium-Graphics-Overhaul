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



public class StageTerrain implements TileConstants, Session.Saveable {
  
  
  final public static int
    MAX_INSOLATION  = 10,
    MAX_MOISTURE    = 10,
    MAX_RADIATION   = 10,
    SAMPLE_RESOLUTION = Stage.ZONE_SIZE;
  
  final public static byte
    ROAD_NONE     =  0,
    ROAD_LIGHT    =  1,
    ROAD_STRIP    = -1;
  
  final static int
    TIME_INIT = -1,
    TIME_DONE = -2,
    INDEX_FERTILITY  = 0,
    INDEX_INSOLATION = 1,
    INDEX_MINERALS   = 2,
    INDEX_HABITATS   = 3;
  
  final Habitat gradient[] = Habitat.GRADIENT();
  final public int
    mapSize;
  private byte
    heightVals[][],
    typeIndex [][],
    varsIndex [][];
  
  private Habitat
    habitats[][];
  private byte
    paveVals[][],
    reserved[][],
    digLevel[][];
  
  private TerrainSet meshSet;
  private LayerType roadLayer;
  private LayerType stripLayer;
  private LayerType reservations;
  
  private class Sample {
    final int measures[] = new int[gradient.length + INDEX_HABITATS];
    int area = 0;
  }
  private Sample sampleGrid[][];
  private Sample globalSample;
  
  
  StageTerrain(
    byte typeIndex[][],
    byte varsIndex[][],
    byte heightVals[][]
  ) {
    this.mapSize    = typeIndex.length;
    this.typeIndex  = typeIndex;
    this.varsIndex  = varsIndex;
    this.heightVals = heightVals;
    
    initHabitatFields();
    initSamples();
  }
  
  
  private void initHabitatFields() {
    habitats = new Habitat[mapSize][mapSize];
    for (Coord c : Visit.grid(0, 0, mapSize, mapSize, 1)) {
      habitats[c.x][c.y] = gradient[typeIndex[c.x][c.y]];
    }
    paveVals = new byte[mapSize][mapSize];
    reserved = new byte[mapSize][mapSize];
    digLevel = new byte[mapSize][mapSize];
  }
  
  
  public StageTerrain(Session s) throws Exception {
    s.cacheInstance(this);
    
    mapSize = s.loadInt();
    heightVals = new byte[mapSize * 2][mapSize * 2];
    typeIndex  = new byte[mapSize    ][mapSize    ];
    varsIndex  = new byte[mapSize    ][mapSize    ];
    
    s.loadByteArray(heightVals);
    s.loadByteArray(typeIndex);
    s.loadByteArray(varsIndex);
    
    initHabitatFields();
    
    s.loadByteArray(paveVals);
    s.loadByteArray(reserved);
    s.loadByteArray(digLevel);
    initSamples();
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveInt(mapSize);
    s.saveByteArray(heightVals);
    s.saveByteArray(typeIndex);
    s.saveByteArray(varsIndex);
    
    s.saveByteArray(paveVals);
    s.saveByteArray(reserved);
    s.saveByteArray(digLevel);
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
    final int hID = h.layerID;
    s.measures[INDEX_HABITATS + hID] += inc;
    s.measures[INDEX_INSOLATION    ] += h.insolation() * inc;
    s.measures[INDEX_MINERALS      ] += h.minerals  () * inc;
    s.measures[INDEX_FERTILITY     ] += h.moisture  () * inc;
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
  
  
  public float habitatSample(Tile t, int habID) {
    return sampleAt(t, INDEX_HABITATS + habID);
  }
  
  
  public float habitatSample(Tile t, Habitat h) {
    return sampleAt(t, h.layerID);
  }
  
  
  
  /**  Habitats and mineral deposits-
    */
  private static Tile tempV[] = new Tile[9];
  
  
  public void setHabitat(Tile t, Habitat h) {
    final Habitat old = habitats[t.x][t.y];
    if (old == h) return;
    
    habitats [t.x][t.y] = gradient[h.layerID];
    typeIndex[t.x][t.y] = (byte) h.layerID;
    incSampleAt(t.x, t.y, old, -1);
    incSampleAt(t.x, t.y, h  ,  1);
    
    for (Tile n : t.vicinity(tempV)) if (n != null) {
      meshSet.flagUpdateAt(n.x, n.y);
    }
  }
  
  
  public Habitat habitatAt(int x, int y) {
    try { return habitats[x][y]; }
    catch (ArrayIndexOutOfBoundsException e) { return null; }
  }
  
  
  public int varAt(Tile t) {
    return varsIndex[t.x][t.y];
  }
  
  
  public Habitat[] gradient() {
    return gradient;
  }
  
  
  
  /**  Methods for handling elevation:
    */
  public float trueHeight(float x, float y) {
    return Nums.sampleMap(mapSize, heightVals, x + 0.5f, y + 0.5f) / 4;
  }
  
  
  public int digLevel(Tile at) {
    return digLevel[at.x][at.y];
  }
  
  
  public void setDigLevel(Tile at, int level) {
    final int oldLevel = digLevel(at);
    if (level == oldLevel) return;
    
    digLevel[at.x][at.y] = (byte) level;
    meshSet.flagUpdateAt(at.x, at.y);
    at.refreshAdjacent();
    
    calcCornerHeight(at,  1,  1);
    calcCornerHeight(at, -1,  1);
    calcCornerHeight(at,  1, -1);
    calcCornerHeight(at, -1, -1);
  }
  
  
  private void calcCornerHeight(Tile at, int offX, int offY) {
    float maxVal = Float.NEGATIVE_INFINITY;
    
    for (int x = 2; x-- > 0;) for (int y = 2; y-- > 0;) {
      final Tile n = at.world.tileAt(at.x + (x * offX), at.y + (y * offY));
      if (n != null) maxVal = Nums.max(maxVal, digLevel(n));
    }

    for (int x = 2; x-- > 0;) for (int y = 2; y-- > 0;) try {
      int cx = (at.x * 2) + (x * offX), cy = (at.y * 2) + (y * offY);
      heightVals[cx][cy] = (byte) maxVal;
    }
    catch (ArrayIndexOutOfBoundsException e) {}
  }
  
  
  
  /**  Methods for handling road-masking of tiles-
    */
  public boolean isRoad(Tile t) {
    return paveVals[t.x][t.y] >= ROAD_LIGHT;
  }
  
  
  public boolean isStripped(Tile t) {
    return paveVals[t.x][t.y] == ROAD_STRIP;
  }
  
  
  public int roadType(Tile t) {
    return paveVals[t.x][t.y];
  }
  
  
  public void setRoadType(Tile t, byte level) {
    final byte oldLevel = paveVals[t.x][t.y];
    paveVals[t.x][t.y] = level;
    
    if (level != oldLevel) for (Tile n : t.vicinity(tempV)) if (n != null) {
      meshSet.flagUpdateAt(n.x, n.y, roadLayer );
      meshSet.flagUpdateAt(n.x, n.y, stripLayer);
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
  public void initTerrainMesh() {
    final Batch <LayerType> layers = new Batch();
    
    for (Habitat h : gradient) {
      final int layerIndex = layers.size();
      layers.add(new LayerType(h.animTex, false, layerIndex, h.name) {
        protected boolean maskedAt(int tx, int ty, TerrainSet terrain) {
          return typeIndex[tx][ty] == layerIndex;
        }
        protected int variantAt(int tx, int ty, TerrainSet terrain) {
          final int var = varsIndex[tx][ty];
          return var <= 4 ? 0 : var;
        }
      });
    }
    
    layers.add(roadLayer = new LayerType(
      Habitat.ROAD_TEXTURE, true, layers.size(), "roads"
    ) {
      protected boolean maskedAt(int tx, int ty, TerrainSet terrain) {
        return paveVals[tx][ty] > 0;
      }
      protected int variantAt(int tx, int ty, TerrainSet terrain) {
        return ((tx + ty) % 3 == 0) ? 0 : 1;
      }
    });
    
    layers.add(stripLayer = new LayerType(
      Habitat.STRIP_TEXTURE, true, layers.size(), "stripping"
    ) {
      protected boolean maskedAt(int tx, int ty, TerrainSet terrain) {
        return paveVals[tx][ty] < 0;
      }
      protected int variantAt(int tx, int ty, TerrainSet terrain) {
        return 0;
      }
      protected int levelAt(int tx, int ty, TerrainSet terrain) {
        return 0 * heightVals[tx * 2][ty * 2];
      }
    });
    
    layers.add(reservations = new LayerType(
      Habitat.RESERVE_TEXTURE, true, layers.size(), "reservations"
    ) {
      protected boolean maskedAt(int tx, int ty, TerrainSet terrain) {
        return reserved[tx][ty] > 0;
      }
      protected int variantAt(int tx, int ty, TerrainSet terrain) {
        return 0;
      }
    });
    
    meshSet = new TerrainSet(
      mapSize, -1,
      typeIndex, varsIndex, heightVals,
      layers.toArray(LayerType.class)
    );
  }
  
  
  public TerrainChunk createOverlay(
    final Stage world, Tile tiles[],
    final boolean innerFringe, ImageAsset tex,
    boolean throwaway
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
    return createOverlay(area, layer, throwaway);
  }
  
  
  public TerrainChunk createOverlay(
    Box2D b, LayerType layer, boolean throwaway
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
    overlay.throwAway = throwaway;
    return overlay;
  }
  
  
  public void readyAllMeshes() {
    meshSet.refreshAllMeshes();
  }
  
  
  public void renderFor(Box2D area, Rendering rendering, float time) {
    meshSet.renderWithin(area, rendering);
  }
}











