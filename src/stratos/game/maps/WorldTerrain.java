/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.maps;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.terrain.*;
import stratos.util.*;

import org.apache.commons.math3.util.FastMath;


/*
Forest, Grassland, Barrens, Spice Desert.
Ocean, Shallows, Albedan, Swamp.
Mesa, Dune Sea, Cursed Earth, Strip Mining.

terrain chance (scaled proportionately) = x * (1 - x) * (1 + tx)
x = moisture
t = terraform-progress  (1 as default).
//  ...Organics.  That's the name for carbons.
//*/



public class WorldTerrain implements TileConstants, Session.Saveable {
  
  
  final public static int
    MAX_INSOLATION  = 10,
    MAX_MOISTURE    = 10,
    MAX_RADIATION   = 10;
  final public static String MINERAL_NAMES[] = {
    "Rubble", "Metal Ore", "Artifacts", "Fuel Isotopes"
  };
  
  final public static byte
    TYPE_METALS   = 1,
    TYPE_RUINS    = 2,
    TYPE_ISOTOPES = 3,
    TYPE_RUBBLE   = 0,
    
    NUM_MINERAL_TYPES  = 4 ,
    MAX_MINERAL_COUNT  = 32,
    MAX_MINERAL_AMOUNT = 10,
    
    ROAD_NONE     = 0,
    ROAD_LIGHT    = 1,
    ROAD_HEAVY    = 2,
    
    TILE_VAR_LIMIT = 4;
  
  final static int
    TIME_INIT = -1,
    TIME_DONE = -2,
    SAMPLE_RESOLUTION = Stage.SECTOR_SIZE,
    SAMPLE_AREA = SAMPLE_RESOLUTION * SAMPLE_RESOLUTION;
  
  final public int
    mapSize;
  private byte
    heightVals[][],
    typeIndex[][],
    varsIndex[][];
  
  final Habitat
    habitats[][];
  private byte
    minerals[][],
    paveVals[][],
    //roadCounter[][],
    dirtVals[][];
  
  private TerrainSet meshSet;
  private LayerType dirtLayer, roadLayer;
  
  private static class Sample {
    int fertility, insolation, minerals;
    final int habitat[] = new int[Habitat.ALL_HABITATS.length];
  }
  
  private Sample sampleGrid[][];
  
  
  
  
  WorldTerrain(
    Habitat[] gradient,
    byte typeIndex[][],
    byte varsIndex[][],
    byte heightVals[][]
  ) {
    this.mapSize = typeIndex.length;
    this.typeIndex = typeIndex;
    this.varsIndex = varsIndex;
    this.heightVals = heightVals;
    
    this.habitats = new Habitat[mapSize][mapSize];
    for (Coord c : Visit.grid(0, 0, mapSize, mapSize, 1)) {
      habitats[c.x][c.y] = Habitat.ALL_HABITATS[typeIndex[c.x][c.y]];
    }
    this.minerals = new byte[mapSize][mapSize];
    this.paveVals = new byte[mapSize][mapSize];
    this.dirtVals = new byte[mapSize][mapSize];
    
    initSamples();
  }
  
  
  public WorldTerrain(Session s) throws Exception {
    s.cacheInstance(this);
    mapSize = s.loadInt();
    
    heightVals = new byte[mapSize + 1][mapSize + 1];
    typeIndex = new byte[mapSize][mapSize];
    varsIndex = new byte[mapSize][mapSize];
    s.loadByteArray(heightVals);
    s.loadByteArray(typeIndex);
    s.loadByteArray(varsIndex);
    
    habitats = new Habitat[mapSize][mapSize];
    for (Coord c : Visit.grid(0, 0, mapSize, mapSize, 1)) {
      habitats[c.x][c.y] = Habitat.ALL_HABITATS[typeIndex[c.x][c.y]];
    }
    minerals = new byte[mapSize][mapSize];
    paveVals = new byte[mapSize][mapSize];
    dirtVals = new byte[mapSize][mapSize];
    s.loadByteArray(minerals);
    s.loadByteArray(paveVals);
    s.loadByteArray(dirtVals);
    
    initSamples();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(mapSize);
    s.saveByteArray(heightVals);
    s.saveByteArray(typeIndex);
    s.saveByteArray(varsIndex);
    
    s.saveByteArray(minerals);
    s.saveByteArray(paveVals);
    s.saveByteArray(dirtVals);
  }
  
  
  
  /**  Averages and sampling-
    */
  private Sample sampleAt(int x, int y) {
    return sampleGrid[x / SAMPLE_RESOLUTION][y / SAMPLE_RESOLUTION];
  }
  
  
  private void initSamples() {
    final int SGS = mapSize / SAMPLE_RESOLUTION;
    this.sampleGrid = new Sample[SGS][SGS];
    for (Coord c : Visit.grid(0, 0, SGS, SGS, 1)) {
      sampleGrid[c.x][c.y] = new Sample();
    }
    for (Coord c : Visit.grid(0, 0, mapSize, mapSize, 1)) {
      incSampleAt(c.x, c.y, habitatAt(c.x, c.y), 1);
    }
  }
  
  
  private void incSampleAt(int x, int y, Habitat h, int inc) {
    if (h == null) return;
    final Sample s = sampleAt(x, y);
    s.habitat[h.ID] += inc;
    s.insolation += h.insolation * inc;
    s.minerals += h.rockiness * inc;
    s.fertility += h.moisture * inc;
  }
  
  
  public float fertilitySample(Tile t) {
    return sampleAt(t.x, t.y).fertility / SAMPLE_AREA;
  }
  
  
  public float insolationSample(Tile t) {
    return sampleAt(t.x, t.y).insolation / SAMPLE_AREA;
  }
  
  
  public float mineralsSample(Tile t) {
    return sampleAt(t.x, t.y).minerals / SAMPLE_AREA;
  }
  
  
  public float habitatSample(Tile t, Habitat h) {
    return sampleAt(t.x, t.y).habitat[h.ID] / SAMPLE_AREA;
  }
  
  
  
  /**  Habitats and mineral deposits-
    */
  private static Tile tempV[] = new Tile[9];
  
  
  public void setHabitat(Tile t, Habitat h) {
    final Habitat old = habitats[t.x][t.y];
    incSampleAt(t.x, t.y, old, -1);
    habitats[t.x][t.y] = h;
    typeIndex[t.x][t.y] = (byte) h.ID;
    incSampleAt(t.x, t.y, h, 1);
    
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
    byte m = minerals[t.x][t.y];
    if (m == 0) return 0;
    
    if (m / MAX_MINERAL_COUNT != type) return 0;
    final int count = m % MAX_MINERAL_COUNT;
    return count * MAX_MINERAL_AMOUNT * 1f / MAX_MINERAL_COUNT;
  }
  
  
  public byte mineralType(Tile t) {
    return (byte) (minerals[t.x][t.y] / MAX_MINERAL_COUNT);
  }
  
  
  public float mineralsAt(Tile t) {
    return mineralsAt(t, mineralType(t));
  }
  
  
  public void setMinerals(Tile t, byte type, float amount) {
    amount = amount * MAX_MINERAL_COUNT * 1f / MAX_MINERAL_AMOUNT;
    final int count = Visit.clamp((int) amount, MAX_MINERAL_COUNT);
    minerals[t.x][t.y] = (byte) ((type * MAX_MINERAL_COUNT) + count);
  }
  
  
  //  TODO:  Revisit this
  public void setSqualor(Tile t, byte newVal) {
    final byte oldVal = dirtVals[t.x][t.y];
    dirtVals[t.x][t.y] = newVal;
    if (oldVal != newVal) for (Tile n : t.vicinity(tempV)) if (n != null) {
      meshSet.flagUpdateAt(n.x, n.y, dirtLayer);
      //final MeshPatch patch = patches[t.x / patchSize][t.y / patchSize];
      //patch.updateDirt = true;
    }
  }
  
  
  public float trueHeight(float x, float y) {
    return Visit.sampleMap(mapSize, heightVals, x, y) / 4;
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
    
    for (Base b : t.world.bases()) b.paveRoutes.map.updateFlags(t);
    
    if (level != oldLevel) for (Tile n : t.vicinity(tempV)) if (n != null) {
      meshSet.flagUpdateAt(n.x, n.y, roadLayer);
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
        final byte ID = varsIndex[tx][ty];
        final byte squalor = dirtVals[tx][ty];
        return ID * squalor > 10;
      }
      protected int variantAt(int tx, int ty, TerrainSet terrain) {
        return (tx + ty) % 4;
      }
    };
    lID++;
    
    roadLayer = layers[lID] = new LayerType(
      Habitat.ROAD_TEXTURE, true, lID
    ) {
      protected boolean maskedAt(int tx, int ty, TerrainSet terrain) {
        return paveVals[tx][ty] > 0;
      }
      protected int variantAt(int tx, int ty, TerrainSet terrain) {
        return ((tx + ty) % 3 == 0) ? 0 : 1;
      }
    };
    
    meshSet = new TerrainSet(mapSize, -1, typeIndex, varsIndex, layers);
  }
  
  
  public TerrainChunk createOverlay(
    final Stage world, Tile tiles[],
    final boolean nullsCount, ImageAsset tex
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
    
    final LayerType layer = new LayerType(tex, false, -1) {
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




/*
final int
  minX = (int) FastMath.ceil(area.xpos()),
  minY = (int) FastMath.ceil(area.ypos()),
  dimX = (int) FastMath.ceil(area.xmax()) - minX,
  dimY = (int) FastMath.ceil(area.ymax()) - minY;
//*/

