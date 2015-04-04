/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.maps;
import stratos.game.common.*;
import stratos.game.wild.Habitat;
import stratos.game.wild.Outcrop;
import stratos.util.*;



public class TerrainGen implements TileConstants {
  
  
  /**  Constructors and field definitions-
    */
  final static int
    DETAIL_RESOLUTION = 8;
  final static float
    MAX_MINERAL_DENSITY = 1.0f;
  
  final int mapSize, sectorGridSize;
  final float typeNoise;
  final Habitat habitats[];
  final Float habitatAmounts[];
  
  static class Sector {
    int coreX, coreY;
    int gradientID;
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
    this.mapSize = checkMapSize(minSize);
    this.typeNoise = Nums.clamp(typeNoise, 0, 1);
    this.sectorGridSize = mapSize / Stage.SECTOR_SIZE;
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
    habitats = habB.toArray(Habitat.class);
    habitatAmounts = amountB.toArray(Float.class);
  }
  
  
  public StageTerrain generateTerrain() {
    setupSectors();
    setupTileHabitats();
    final StageTerrain t = new StageTerrain(habitats, typeIndex, varsIndex, heightMap);
    return t;
  }
  
  
  protected float baseAmount(Habitat h) {
    float sum = 0; for (Float f : habitatAmounts) sum += f;
    final int index = Visit.indexOf(h, habitats);
    if (index == -1) return 0;
    return habitatAmounts[index] / sum;
  }
  
  
  protected Habitat baseHabitat(Coord c, int resolution) {
    final int grid = Stage.SECTOR_SIZE / resolution;
    final int ID = sectors[c.x / grid][c.y / grid].gradientID;
    return habitats[ID];
  }
  
  
  
  /**  Generating the overall region layout:
    */
  private int checkMapSize(int minSize) {
    int mapSize = Stage.SECTOR_SIZE * 2;
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
      s.coreX = (int) ((c.x + 0.5f) * Stage.SECTOR_SIZE);
      s.coreY = (int) ((c.y + 0.5f) * Stage.SECTOR_SIZE);
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
    final int SS = Stage.SECTOR_SIZE, DR = DETAIL_RESOLUTION;
    for (int n = GS - 1; n-- > 0;) {
      blendsX[n] = staggeredLine(mapSize + 1, DR, SS / 2, true);
      blendsY[n] = staggeredLine(mapSize + 1, DR, SS / 2, true);
    }
  }
  
  
  
  /**  Generating fine-scale details.
    */
  private void setupTileHabitats() {
    final int seedSize = (mapSize / DETAIL_RESOLUTION) + 1;
    final HeightMap heightDetail = new HeightMap(
      mapSize + 1, new float[seedSize][seedSize], 1, 0.5f
    );
    final byte detailGrid[][] = heightDetail.asScaledBytes(10);
    typeIndex = new byte[mapSize][mapSize];
    varsIndex = new byte[mapSize][mapSize];
    heightMap = new byte[mapSize + 1][mapSize + 1];
    
    for (Coord c : Visit.grid(0, 0, mapSize, mapSize, 1)) {
      varsIndex[c.x][c.y] = terrainVarsAt(c.x, c.y);
      
      final int
        XBI = (int) ((c.x * 1f / mapSize) * blendsX.length),
        YBI = (int) ((c.y * 1f / mapSize) * blendsY.length);
      final float
        sampleX = Nums.clamp(c.x + blendsX[XBI][c.y], 0, mapSize - 1),
        sampleY = Nums.clamp(c.y + blendsY[YBI][c.x], 0, mapSize - 1);
      float sum = Nums.sampleMap(mapSize, sectorVal, sampleX, sampleY);
      
      Habitat under = habitats[Nums.clamp((int) sum, habitats.length)];
      
      if (! under.isOcean()) {
        float detail = Nums.sampleMap(mapSize, detailGrid, c.x, c.y) / 10f;
        sum += detail * detail * 2;
        under = habitats[Nums.clamp((int) sum, habitats.length)];
        typeIndex[c.x][c.y] = (byte) under.ID;
        
        if (under == Habitat.ESTUARY && Rand.index(4) == 0) {
          typeIndex[c.x][c.y] = (byte) Habitat.MEADOW.ID;
        }
        if (under == Habitat.CURSED_EARTH) {
          boolean paint = (detail * detail) > 0.25f;
          if (paint) { if (Rand.index(4) == 0) paint = false; }
          else if (Rand.index(10) == 0) paint = true;
          if (paint) typeIndex[c.x][c.y] = (byte) Habitat.STRIP_MINING.ID;
        }
      }
    }
    //
    //  Finally, pain the interiors of any ocean tiles-
    //paintEdge(Habitat.STRIP_MINING.ID, Habitat.CURSED_EARTH.ID);
    paintEdge(Habitat.OCEAN.ID, Habitat.SHORELINE.ID);
    paintEdge(Habitat.OCEAN.ID, Habitat.SHALLOWS .ID);
    for (Coord c : Visit.grid(0, 0, mapSize, mapSize, 1)) {
      final byte type = typeIndex[c.x][c.y];
      //if (! Habitat.ALL_HABITATS[type].isOcean) continue;
      if (type >= Habitat.SHALLOWS.ID) continue;
      float detail = Nums.sampleMap(mapSize, detailGrid, c.x, c.y) / 10f;
      detail *= detail * 1.5f;
      typeIndex[c.x][c.y] = (byte) (((detail * detail) > 0.25f) ?
        Habitat.SHALLOWS.ID : Habitat.OCEAN.ID
      );
    }
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
  

  private byte terrainVarsAt(int x, int y) {
    final int dir = Rand.index(T_INDEX.length);
    byte sampleVar;
    try { sampleVar = varsIndex[x + T_X[dir]][y + T_Y[dir]]; }
    catch (ArrayIndexOutOfBoundsException e) { sampleVar = 0; }
    
    final int MV = StageTerrain.TILE_VAR_LIMIT;
    if (sampleVar == 0) sampleVar = (byte) (Rand.index(MV + 1) % MV);
    varsIndex[x][y] = sampleVar;
    return sampleVar;
  }
  
  
  private void raiseHeight(int x, int y, float val) {
    heightMap[x    ][y    ] = (byte) Nums.max(heightMap[x    ][y    ], val);
    heightMap[x + 1][y    ] = (byte) Nums.max(heightMap[x + 1][y    ], val);
    heightMap[x    ][y + 1] = (byte) Nums.max(heightMap[x    ][y + 1], val);
    heightMap[x + 1][y + 1] = (byte) Nums.max(heightMap[x + 1][y + 1], val);
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
  //  TODO:  Surface deposits are now much fairer indicators of sub-surface
  //  minerals, but at the cost of much less influence over actual sub-surface
  //  abundance (since the abundance maps now affect larger discrete areas.)
  //  While it's not critical, it would be nice to have some method of fine-
  //  tuning this.  Perhaps the dither-map?
  public void setupMinerals(
    final Stage world,
    float chanceMetals, float chanceArtifacts, float chanceIsotopes
  ) {
    final StageTerrain terrain = world.terrain();
    if (terrain == null) I.complain("No terrain assigned to world!");
    final byte
      artifactsMap[][] = genSectorMap(10),
      metalsMap   [][] = genSectorMap(10),
      isotopesMap [][] = genSectorMap(10),
      allMaps[][][] = { null, metalsMap, artifactsMap, isotopesMap };
    final float
      abundances[] = { 0, chanceMetals, chanceArtifacts, chanceIsotopes },
      totals[] = new float[4],
      chances[] = new float[4];
    //
    //  
    for (Coord c : Visit.grid(0, 0, mapSize, mapSize, 1)) {
      //
      //  Obtain the relative strength of each of the mineral maps at this tile-
      final boolean pickHighest = Rand.num() < 0.33f;
      float sumChances = chances[0] = 0.5f;
      for (int i = 4; i-- > 1;) {
        float sample = Nums.sampleMap(mapSize, allMaps[i], c.x, c.y) / 10f;
        sample *= (1 * sample) + ((1 - sample) * abundances[i]);
        chances[i] = sample;
        sumChances += sample;
      }
      //
      //  Pick one of the mineral types on this basis-
      float chance = 0;
      int var = 0;
      if (pickHighest) {
        for (int i = 4; i-- > 1;) {
          if (chances[i] > chance) { chance = chances[i]; var = i; }
        }
      }
      else {
        float pick = Rand.num() * sumChances;
        for (int i = 4; i-- > 1;) {
          if (pick < chances[i]) { chance = chances[i]; var = i; break; }
          pick -= chances[i];
        }
        if (var == 0) continue;
      }
      //
      //  Adjust abundance based on local terrain and global variables, and
      //  find the degree for the local deposit-
      if (pickHighest) chance *= abundances[var];
      final float minChance = terrain.habitatAt(c.x, c.y).minerals() / 10f;
      chance *= minChance;
      
      float minAmount = minChance * (1.5f - Rand.num());
      if (Rand.num() < minChance) minAmount += 0.5f;
      minAmount *= StageTerrain.MAX_MINERAL_AMOUNT / 2f;
      if (minAmount <= 0) continue;
      //
      //  Store and summarise-
      final Tile location = world.tileAt(c.x, c.y);
      terrain.setMinerals(location, (byte) var, minAmount);
      totals[var] += terrain.mineralsAt(location, (byte) var);
    }
    
    final boolean report = false;
    if (report) {
      I.say(
        "Total metals/carbons/isotopes: "+
        totals[1]+"/"+totals[2]+"/"+totals[3]
      );
      presentMineralMap(world, terrain);
    }
  }
  
  
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
            else worldTerrain.setHabitat(t, Habitat.MESA);
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
      if (Placement.perimeterFits(o, t.world) && o.canPlace()) {
        o.enterWorld();
        return o;
      }
    }
    return null;
  }
  

  
  /**  Utility methods for debugging-
    */
  public void presentMineralMap(Stage world, StageTerrain worldTerrain) {
    final int colourKey[][] = new int[mapSize][mapSize];
    final int typeColours[] = {
      0xff000000,
      0xffff0000,
      0xff0000ff,
      0xff00ff00
    };
    final int degreeMasks[] = {
      0xff000000,
      0xff3f3f3f,
      0xff7f7f7f,
      0xffbfbfbf,
      0xffffffff
    };
    for (Coord c : Visit.grid(0, 0, mapSize, mapSize, 1)) {
      final Tile t = world.tileAt(c.x, c.y);
      final byte type = worldTerrain.mineralType(t);
      final float amount = worldTerrain.mineralsAt(t, type);
      int degree = (int) (amount * 3.99f / StageTerrain.MAX_MINERAL_AMOUNT);
      colourKey[c.x][c.y] = typeColours[type] & degreeMasks[degree];
    }
    I.present(colourKey, "minerals map", 256, 256);
  }
}





