/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.maps;
import stratos.game.common.*;
import stratos.game.wild.*;
import stratos.util.*;



public class TerrainGen implements TileConstants {
  
  
  /**  Constructors and field definitions-
    */
  final static int
    DETAIL_RESOLUTION = 8;
  final static float
    MAX_MINERAL_DENSITY = 1.0f;
  final static boolean
    USE_HEIGHT_VALS = false;
  
  final int mapSize, sectorGridSize;
  final float typeNoise;
  
  final Habitat habitats[];
  final Float habitatAmounts[];
  
  static class Sector {
    int coreX, coreY;
    int gradientID;
    Object resource;
  }

  private Sector sectors[][];
  private float blendsX[][], blendsY[][];
  private byte sectorVal[][];
  private byte typeIndex[][];
  private byte heightMap[][];
  private byte varsIndex[][];
  
  
  
  /**  NOTE:  This constructor expects a gradient argument consisting of paired
    *  habitats and float-specified proportions for each.  Do not fuck with it.
    */
  public TerrainGen(int minSize, float typeNoise, Object... gradient) {
    this.mapSize        = checkMapSize(minSize);
    this.typeNoise      = Nums.clamp(typeNoise, 0, 1);
    this.sectorGridSize = mapSize / Stage.ZONE_SIZE;
    //
    //  Here, we verify amd compile the gradient of habitat proportions.
    final Batch <Habitat> habB  = new Batch <Habitat> ();
    final Batch <Float> amountB = new Batch <Float> ()  ;
    boolean habNext = true;
    for (Object o : gradient) {
      if (habNext) {
        if (! (o instanceof Habitat)) I.complain("Expected habitat...");
        habB.add((Habitat) o);
        habNext = false;
      }
      else {
        if (! (o instanceof Float)) I.complain("Expected amount as float...");
        amountB.add((Float) o);
        habNext = true;
      }
    }
    if (gradient.length % 2 != 0) I.complain("Missing argument...");
    habitats       = habB   .toArray(Habitat.class);
    habitatAmounts = amountB.toArray(Float  .class);
  }
  
  
  public TerrainGen(
    int minSize, float typeNoise, Habitat habitats[], Float habitatWeights[]
  ) {
    this.mapSize        = checkMapSize(minSize);
    this.typeNoise      = Nums.clamp(typeNoise, 0, 1);
    this.sectorGridSize = mapSize / Stage.ZONE_SIZE;
    if (habitats.length != habitatWeights.length) {
      I.complain("Length mismatch...");
    }
    this.habitats       = habitats      ;
    this.habitatAmounts = habitatWeights;
  }
  
  
  public StageTerrain generateTerrain() {
    setupSectors();
    setupTileHabitats();
    final StageTerrain t = new StageTerrain(typeIndex, varsIndex, heightMap);
    return t;
  }
  
  
  protected float baseAmount(Habitat h) {
    float sum = 0; for (Float f : habitatAmounts) sum += f;
    final int index = Visit.indexOf(h, habitats);
    if (index == -1) return 0;
    return habitatAmounts[index] / sum;
  }
  
  
  protected Habitat baseHabitat(Coord c, int resolution) {
    final int grid = Stage.ZONE_SIZE / resolution;
    final int ID = sectors[c.x / grid][c.y / grid].gradientID;
    return habitats[ID];
  }
  
  
  
  /**  Generating the overall region layout:
    */
  private int checkMapSize(int minSize) {
    int mapSize = Stage.ZONE_SIZE * 2;
    while (mapSize < minSize) mapSize *= 2;
    if (mapSize == minSize) return mapSize;
    I.complain("MAP SIZE MUST BE A POWER OF 2 MULTIPLE OF SECTOR SIZE.");
    return -1;
  }
  
  
  private byte[][] genSectorMap(int scale) {
    final int seedSize = (mapSize / DETAIL_RESOLUTION) + 1;
    final HeightMap sectorMap = new HeightMap(seedSize);
    return sectorMap.asScaledBytes(scale);
  }
  
  
  private void setupSectors() {
    final int GS = sectorGridSize;
    initSectorVals(GS);
    initSectorBlends(GS);
    sectors = new Sector[GS][GS];
    for (Coord c : Visit.grid(0, 0, GS, GS, 1)) {
      final Sector s = new Sector();
      s.coreX = (int) ((c.x + 0.5f) * Stage.ZONE_SIZE);
      s.coreY = (int) ((c.y + 0.5f) * Stage.ZONE_SIZE);
      s.gradientID = sectorVal[c.x][c.y];
      ///I.say("Type ID: "+s.gradientID+", core: "+s.coreX+"|"+s.coreY);
      sectors[c.x][c.y] = s;
    }
  }
  
  
  
  //
  //  TODO:  Allow these 'seed values' to be provided as a starting argument.
  //         Also, use static factory methods, not an object.
  
  
  private void initSectorVals(int GS) {
    //
    //  Set up the requisite data stores first-
    final Vec3D seedVals[][] = new Vec3D[GS][GS];
    final int seedSize = (mapSize / DETAIL_RESOLUTION) + 1;
    final HeightMap sectorMap = new HeightMap(seedSize);
    final float heightVals[][] = sectorMap.value();
    sectorVal = new byte[GS][GS];
    //
    //  We then generate seed values for each sector, and sort by height.
    final Sorting <Vec3D> sorting = new Sorting <Vec3D> () {
      public int compare(Vec3D a, Vec3D b) {
        if (a == b) return 0;
        return a.z > b.z ? 1 : -1;
      }
    };
    for (Coord c : Visit.grid(0, 0, GS, GS, 1)) {
      final Vec3D v = seedVals[c.x][c.y] = new Vec3D();
      final float val = (Rand.num() < typeNoise) ?
        ((Rand.num() < typeNoise) ? Rand.num() : heightVals[c.x][c.y]) :
        ((Rand.num() * typeNoise) + (heightVals[c.x][c.y] * (1 - typeNoise)));
      v.set(c.x, c.y, val);
      sorting.add(v);
    }
    //
    //  We then determine how many sectors of each habitat are required,
    //  compile the IDs sequentially by height, and assign to their sectors.
    float sumAmounts = 0, sumToNext = 0;
    for (int i = habitats.length; i-- > 0;) sumAmounts += habitatAmounts[i];
    final byte typeAssigned[] = new byte[sorting.size()];
    byte currentTypeID = -1;
    for (int i = 0; i < typeAssigned.length; i++) {
      final float indexInSum = i * sumAmounts / typeAssigned.length;
      if (indexInSum >= sumToNext) {
        currentTypeID++;
        sumToNext += habitatAmounts[currentTypeID];
      }
      typeAssigned[i] = currentTypeID;
    }
    int count = 0; for (Vec3D v : sorting) {
      sectorVal[(int) v.x][(int) v.y] = typeAssigned[count++];
    }
  }
  
  
  private void initSectorBlends(int GS) {
    blendsX = new float[GS - 1][];
    blendsY = new float[GS - 1][];
    final int SS = Stage.ZONE_SIZE, DR = DETAIL_RESOLUTION;
    for (int n = GS - 1; n-- > 0;) {
      blendsX[n] = staggeredLine(mapSize + 1, DR, SS / 2, true);
      blendsY[n] = staggeredLine(mapSize + 1, DR, SS / 2, true);
    }
  }
  
  
  
  /**  Generating fine-scale details.
    */
  private void setupTileHabitats() {
    //
    //  Firstly, establish a broad picture of the terrain using seed values.
    final int seedSize = (mapSize / DETAIL_RESOLUTION) + 1;
    final HeightMap heightDetail = new HeightMap(
      mapSize + 1, new float[seedSize][seedSize], 1, 0.5f
    );
    final byte detailGrid[][] = heightDetail.asScaledBytes(10);
    //
    //  Then, fill in the details at higher resolution using random sampling on
    //  one side or the other.
    typeIndex = new byte[mapSize    ][mapSize    ];
    varsIndex = new byte[mapSize    ][mapSize    ];
    heightMap = new byte[mapSize * 2][mapSize * 2];
    
    for (Coord c : Visit.grid(0, 0, mapSize, mapSize, 1)) {
      
      final int
        XBI = (int) ((c.x * 1f / mapSize) * blendsX.length),
        YBI = (int) ((c.y * 1f / mapSize) * blendsY.length);
      final float
        sampleX = Nums.clamp(c.x + blendsX[XBI][c.y], 0, mapSize - 1),
        sampleY = Nums.clamp(c.y + blendsY[YBI][c.x], 0, mapSize - 1);
      float sum = Nums.sampleMap(mapSize, mapSize, sectorVal, sampleX, sampleY);
      
      Habitat under = habitats[Nums.clamp((int) sum, habitats.length)];
      if (! under.pathClear) {
        typeIndex[c.x][c.y] = (byte) under.layerID;
      }
      else {
        float detail = Nums.sampleMap(
          mapSize, mapSize, detailGrid, c.x, c.y
        ) / 10f;
        sum += detail * detail * 2;
        under = habitats[Nums.clamp((int) sum, habitats.length)];
        typeIndex[c.x][c.y] = (byte) under.layerID;
      }
    }
    //
    //  Then, establish terrain-variants (used to help fix mineral content and
    //  growth of flora.)
    this.setupTerrainMarks();
    //
    //  Then paint the interiors of any ocean tiles-
    final int
      oceanID = Habitat.OCEAN    .layerID,
      shoreID = Habitat.SHORELINE.layerID,
      shallID = Habitat.SHALLOWS .layerID;
    
    paintEdge(oceanID, shoreID);
    paintEdge(oceanID, shallID);
    for (Coord c : Visit.grid(0, 0, mapSize, mapSize, 1)) {
      final byte type = typeIndex[c.x][c.y];
      if (type >= shallID) continue;
      float detail = Nums.sampleMap(
        mapSize, mapSize, detailGrid, c.x, c.y
      ) / 10f;
      detail *= detail * 1.5f;
      typeIndex[c.x][c.y] = (byte) (((detail * detail) > 0.25f) ?
        shallID : oceanID
      );
    }
    //
    //  And finally, establish actual height-values:
    if (USE_HEIGHT_VALS) for (Coord c : Visit.grid(0, 0, mapSize, mapSize, 1)) {
      final byte high = typeIndex[c.x][c.y];
      //
      //  The height-map has twice the resolution of the tile-map, and we want
      //  the edges of tiles to align (by default), so we have to visit 16
      //  points both within and around the tile itself...
      for (Coord p : Visit.grid(-1, -1, 4, 4, 1)) {
        raisePoint(c, p.x, p.y, high);
      }
    }
  }
  
  
  
  
  //  TODO:  Create a FloraUtils class for this sort of thing???
  
  final public static int
    FILL_NONE  = 0,
    FILL_SCRUB = 1,
    FILL_TOTAL = 2,
    MARK_VARS  = 4;
  
  private void setupTerrainMarks() {
    final RandomScan fullScan = new RandomScan(mapSize) {
      protected void scanAt(int x, int y) {
        int var = Rand.index(MARK_VARS);
        if      (canFillAt (x, y)) var += FILL_TOTAL * MARK_VARS;
        else if (canScrubAt(x, y)) var += FILL_SCRUB * MARK_VARS;
        varsIndex[x][y] = (byte) var;
      }
    };
    fullScan.doFullScan();
  }
  
  
  private int markedFill(int x, int y) {
    try { return varsIndex[x][y] / MARK_VARS; }
    catch (ArrayIndexOutOfBoundsException e) { return -1; }
  }
  
  
  private boolean canScrubAt(int mX, int mY) {
    //
    //  This is for low-lying vegetation that doesn't interfere with pathing!
    int count = 0, countFull = 0;
    for (int i : T_ADJACENT) {
      final int fill = markedFill(mX + T_X[i], mY + T_Y[i]);
      if (fill == FILL_SCRUB) count++;
      if (fill == FILL_TOTAL) countFull++;
    }
    return count < 1 && countFull > 0;
  }
  
  
  private boolean canFillAt(int mX, int mY) {
    //
    //  Rule No. 1:  Do not mark a tile if it would deprive any marked
    //  neighbour of a 'free exit'- i.e, enclose it completely.
    for (int i : T_ADJACENT) {
      int x = mX + T_X[i], y = mY + T_Y[i];
      boolean anyExit = false;
      
      for (int n : T_ADJACENT) {
        int nX = x + T_X[n], nY = y + T_Y[n];
        //
        //  If a neighbour is either blocked (or would be- i.e, is the
        //  coordinate being checked) then there's no 'exit' on this side.
        if (nX == mX && nY == mY) continue;
        if (markedFill(nX, nY) == FILL_TOTAL) continue;
        anyExit = true;
        break;
      }
      if (! anyExit) return false;
    }
    
    //
    //  Rule No. 2:  Do not mark a tile within two tiles of another, unless you
    //  have an existing contiguous neighbour marked.
    for (int i = 16; i-- > 0;) {
      int x = mX + PERIM_2_OFF_X[i], y = mY + PERIM_2_OFF_Y[i];
      if (markedFill(x, y) != FILL_TOTAL) continue;
      //
      //  Only allow marking this tile if the tile between is already marked.
      int bX = (x + mX) / 2, bY = (y + mY) / 2;
      if (markedFill(bX, bY) != FILL_TOTAL) return false;
    }
    return true;
  }
  
  
  
  private void raisePoint(Coord c, int x, int y, byte high) {
    x = Nums.clamp(x + (c.x * 2), mapSize * 2);
    y = Nums.clamp(y + (c.y * 2), mapSize * 2);
    byte val = heightMap[x][y];
    if (val == 0) val = high;
    else if (high < val) val = high;
    heightMap[x][y] = val;
  }
  
  
  private void paintEdge(int edgeID, int replaceID) {
    final Batch <Coord> toPaint = new Batch <Coord> ();
    for (Coord c : Visit.grid(0, 0, mapSize, mapSize, 1)) {
      final int h = typeIndex[c.x][c.y];
      if (h != edgeID) continue;
      boolean inside = true;
      for (int i : T_INDEX) {
        try {
          final int n = typeIndex[c.x + T_X[i]][c.y + T_Y[i]];
          if (n != edgeID && n != replaceID) { inside = false; break; }
        }
        catch (Exception e) { continue; }
      }
      if (! inside) toPaint.add(new Coord(c));
    }
    for (Coord c : toPaint) {
      typeIndex[c.x][c.y] = (byte) replaceID;
    }
  }
  
  
  
  /**  Methods for determining boundaries between sectors-
    */
  private float[] staggeredLine(
    int length, int initStep, float variation, boolean sub
  ) {
    //  NOTE:  Length must be an exact power of 2, plus 1.
    float line[] = new float[length];
    int step = (initStep > 0) ? initStep : (length - 1);
    while (step > 1) {
      for (int i = 0; i < length - 1; i += step) {
        final float value = (line[i] + line[i + step]) / 2;
        final float rand = Rand.num() - (sub ? 0.5f : 0);
        line[i + (step / 2)] = value + (rand * variation);
      }
      step /= 2;
      variation /= 2;
    }
    
    return line;
  }
  

  /**  Generating mineral deposits-
    */
  //
  //  Put the various tiles for processing in different batches and treat 'em
  //  that way?
  public void setupOutcrops(final Stage world) {
    final StageTerrain worldTerrain = world.terrain();
    final int seedSize = (mapSize / DETAIL_RESOLUTION) + 1;
    final HeightMap heightDetail = new HeightMap(
      mapSize + 1, new float[seedSize][seedSize], 1, 0.5f
    );
    final byte detailGrid[][] = heightDetail.asScaledBytes(10);
    final Batch <Tile> desertTiles = new Batch <Tile> ();
    
    final RandomScan scan = new RandomScan(mapSize) {
      protected void scanAt(int x, int y) {
        //
        //  First, determine the outcrop type.  (In the case of desert tiles,
        //  we insert dunes wherever possible.)
        final Habitat habitat = worldTerrain.habitatAt(x, y);
        final Tile location = world.tileAt(x, y);
        float rockAmount = detailGrid[x][y] / 10f;
        rockAmount *= rockAmount * Rand.num() * 1.25f;
        
        if ((rockAmount * 10) > (10 - habitat.minerals())) {
          //
          //  If placement was successful, 'paint' the perimeter with suitable
          //  habitat types-
          final Outcrop o = tryOutcrop(
            Outcrop.TYPE_MESA, location, 3, 1
          );
          if (o != null) for (Tile t : world.tilesIn(o.footprint(), false)) {
            if (t.habitat() == Habitat.SHORELINE) continue;
            if (Rand.index(4) > 0) worldTerrain.setHabitat(t, Habitat.BARRENS);
            else worldTerrain.setHabitat(t, Habitat.WHITE_MESA);
          }
        }
        
        if (habitat == Habitat.DUNE) {
          desertTiles.add(location);
        }
        else if (habitat == Habitat.BARRENS && Rand.index(10) == 0) {
          tryOutcrop(Outcrop.TYPE_DUNE, location, 1, 1);
        }
      }
    };
    scan.doFullScan();
    //
    //  Desert tiles get special treatment-
    for (Tile t : desertTiles) if (Rand.num() < 0.1f) {
      tryOutcrop(Outcrop.TYPE_DUNE, t, 3, 3);
    }
    for (Tile t : desertTiles) tryOutcrop(Outcrop.TYPE_DUNE, t, 2, 2);
    for (Tile t : desertTiles) tryOutcrop(Outcrop.TYPE_DUNE, t, 1, 1);
  }
  
  
  private Outcrop tryOutcrop(
    int type, Tile t, int maxSize, int minSize
  ) {
    for (int size = maxSize; size >= minSize; size--) {
      final Outcrop o = new Outcrop(size, 1, type);
      o.setPosition(t.x, t.y, t.world);
      if (o.oreType() == null) return null;
      
      if (SiteUtils.pathingOkayAround(o, t.world) && o.canPlace()) {
        o.enterWorld();
        return o;
      }
    }
    return null;
  }
}





