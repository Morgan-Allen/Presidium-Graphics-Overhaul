/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.terrain.*;
import stratos.util.*;



public class Habitat extends Index.Entry {
  
  
  final public static Index <Habitat> INDEX = new Index <Habitat> ();
  private static List <Habitat> gradientSort = new List <Habitat> () {
    protected float queuePriority(Habitat r) { return r.layerID; }
  };
  private static Habitat fullGradient[] = null;
  
  
  final static String
    TERRAIN_PATH = "media/Terrain/";
  final public static CutoutModel
    DESERT_FLORA_MODELS[][] = CutoutModel.fromImageGrid(
      Habitat.class, TERRAIN_PATH+"desert_flora_as_copy.png",
      4, 4, 1.2f, 2, false
    ),
    FOREST_FLORA_MODELS[][] = CutoutModel.fromImageGrid(
      Habitat.class, TERRAIN_PATH+"old_flora_resize.png",
      4, 4, 1.2f, 2, false
    ),
    WASTES_FLORA_MODELS[][] = CutoutModel.fromImageGrid(
      Habitat.class, TERRAIN_PATH+"wastes_flora.png",
      4, 4, 1.2f, 2, false
    ),
    TUNDRA_FLORA_MODELS[][] = null,
    PLANKTON_MODELS    [][] = null,
    ANNUALS_MODELS     [][] = null,
    NO_FLORA           [][] = null,
    //
    //  Finally, mineral outcrop models-
    OUTCROP_MODELS[][] = CutoutModel.fromImageGrid(
      Habitat.class, TERRAIN_PATH+"all_deposits.png",
      3, 3, 1.9f, 2, false
    ),
    DUNE_MODELS     [] = OUTCROP_MODELS[2],
    MINERAL_MODELS  [] = OUTCROP_MODELS[1],
    ROCK_LODE_MODELS[] = OUTCROP_MODELS[0],
    SPIRE_MODELS[][] = CutoutModel.fromImageGrid(
      Habitat.class, TERRAIN_PATH+"all_outcrops_old.png",
      3, 3, 1.6f, 1.5f, false
    );
  
  final private static String
    FERTILITY  = "moisture"  ,
    INSOLATION = "insolation",
    MINERALS   = "minerals"  ,
    IS_OCEAN   = "is ocean"  ,
    IS_WASTE   = "is wastes" ,
    IS_SPECKLE = "is speckle";
  
  
  //
  //  TODO:  Each habitat needs to implement it's own routines to handle
  //  painting FX, flora and outcrops setup.  (Growth/erosion can be handled
  //  by the fixtures themselves.)
  
  final public static Habitat
    //
    //  Ocean habitats, which occur at or below current sea levels.
    OCEAN = new Habitat(
      Habitat.class, 0, "Ocean",
      "",
      new String[] {
        "ocean.0.gif",
        "ocean.1.gif",
        "ocean.2.gif"
      }, PLANKTON_MODELS,
      1, false, IS_OCEAN, FERTILITY, 5, INSOLATION, 3, MINERALS, 1
    ),
    SHALLOWS = new Habitat(
      Habitat.class, 1, "Shallows",
      "",
      new String[] {
        "shallows.0.gif",
        "shallows.1.gif",
        "shallows.1.gif"
      }, PLANKTON_MODELS,
      1, false, IS_OCEAN, FERTILITY, 5, INSOLATION, 4, MINERALS, 0
    ),
    SHORELINE = new Habitat(
      Habitat.class, 2, "Shore",
      "",
      "shoreline.png", NO_FLORA,
      0, true, IS_OCEAN, FERTILITY, 5, INSOLATION, 5, MINERALS, 2
    ),
    OCEAN_HABITATS[] = { SHORELINE, SHALLOWS, OCEAN },
    //
    //  Forest habitats, which occur in equatorial regions with adequate rain-
    SWAMPLANDS = new Habitat(
      Habitat.class, 3, "Swamplands",
      "",
      "swamplands_ground.gif", FOREST_FLORA_MODELS,
      3, true, FERTILITY, 9, INSOLATION, 6, MINERALS, 0
    ),
    ESTUARY = new Habitat(
      Habitat.class, 4, "Rain Forest",
      "",
      "estuary_ground.png", FOREST_FLORA_MODELS,
      2, true, IS_SPECKLE, FERTILITY, 7, INSOLATION, 7, MINERALS, 2
    ),
    MEADOW = new Habitat(
      Habitat.class, 5, "Meadow",
      "",
      "meadows_ground.gif", FOREST_FLORA_MODELS,
      2, true, FERTILITY, 6, INSOLATION, 5, MINERALS, 3
    ),
    FOREST_HABITATS[] = { MEADOW, ESTUARY, SWAMPLANDS },
    //
    //  Desert habitats, which occur under hotter conditions-
    SAVANNAH = new Habitat(
      Habitat.class, 6, "Savannah",
      "",
      "savannah_ground.gif", DESERT_FLORA_MODELS,
      1, true, FERTILITY, 5, INSOLATION, 7, MINERALS, 3
    ),
    BARRENS = new Habitat(
      Habitat.class, 7, "Barrens",
      "",
      "barrens_ground.gif", DESERT_FLORA_MODELS,
      0, true, FERTILITY, 3, INSOLATION, 8, MINERALS, 6
    ),
    DUNE = new Habitat(
      Habitat.class, 8, "Desert",
      "",
      "desert_ground.gif", NO_FLORA,
      0, true, FERTILITY, 1, INSOLATION, 9, MINERALS, 5
    ),
    WHITE_MESA = new Habitat(
      Habitat.class, 9, "White Mesa",
      "",
      "mesa_ground.gif", NO_FLORA,
      0, true, FERTILITY, 0, INSOLATION, 5, MINERALS, 5
    ),
    DESERT_HABITATS[] = { DUNE, BARRENS, SAVANNAH },
    
    //
    //  Waste habitats, which have special rules governing their introduction,
    //  related to extreme inhospitability, pollution or volcanism-
    CURSED_EARTH = new Habitat(
      Habitat.class, 10, "Cursed Earth",
      "",
      "cursed_earth.png", NO_FLORA,// WASTES_FLORA_MODELS,
      0, true, FERTILITY, 2, INSOLATION, 3, MINERALS, 7,
      IS_WASTE
    ),
    BLACK_MESA = new Habitat(
      Habitat.class, 11, "Black Mesa",
      "",
      "black_mesa.png", NO_FLORA,
      0, false, FERTILITY, 0, INSOLATION, 9, MINERALS, 6,
      IS_WASTE, IS_SPECKLE
    ),
    TOXIC_RUNOFF = new Habitat(
      Habitat.class, 12, "Toxic Runoff",
      "",
      new String[] {
        "toxic_runoff.0.png",
        "toxic_runoff.1.png",
        "toxic_runoff.2.png"
      }, NO_FLORA,
      0, false, FERTILITY, 0, INSOLATION, 6, MINERALS, 2,
      IS_WASTE
    ),
    STRIP_MINING = new Habitat(
      Habitat.class, 13, "Strip Mining",
      "",
      "strip_mining.png", NO_FLORA,
      0, false, FERTILITY, 0, INSOLATION, 0, MINERALS, 0,
      IS_WASTE
    );
  final public static ImageAsset
    ROAD_TEXTURE = ImageAsset.fromImage(
      Habitat.class, TERRAIN_PATH+"road_map_new.png"
    ),
    STRIP_TEXTURE = ImageAsset.fromImage(
      Habitat.class, TERRAIN_PATH+"strip_mining_alt.png"
    ),
    RESERVE_TEXTURE = ImageAsset.fromImage(
      Habitat.class, TERRAIN_PATH+"reserve_tiles.png"
    );
  
  
  final public int layerID;
  
  final public String name, info;
  final public ImageAsset animTex[], baseTex;
  
  final public Species floraSpecies[];
  final public boolean pathClear;
  
  private int biomass;
  private float moisture, insolation, rockiness;
  private boolean isOcean, isWaste, isSpeckled;
  
  
  Habitat(
    Class baseClass, int layerID,
    String name, String info,
    String texName, CutoutModel fM[][],
    int biomass, boolean pathClear, Object... traits
  ) {
    this(
      baseClass, layerID,
      name, info, new String[] { texName },
      fM, biomass, pathClear, traits
    );
  }
  
  
  Habitat(
    Class baseClass, int layerID,
    String name, String info,
    String groundTex[], CutoutModel fM[][],
    int biomass, boolean pathClear, Object... traits
  ) {
    super(INDEX, "habitat_"+layerID);
    this.layerID = layerID;
    gradientSort.add(this);
    fullGradient = null;
    
    this.name = name;
    this.info = info;
    
    this.animTex = new ImageAsset[groundTex.length];
    for (int i = animTex.length; i-- > 0;) {
      final String path = TERRAIN_PATH+groundTex[i];
      this.animTex[i] = ImageAsset.fromImage(Habitat.class, path);
    }
    this.baseTex = animTex[0];
    
    this.biomass = biomass;
    this.pathClear = pathClear;
    for (int i = 0; i < traits.length; i++) {
      if (traits[i] == FERTILITY ) moisture   = (Integer) traits[i + 1];
      if (traits[i] == MINERALS  ) rockiness  = (Integer) traits[i + 1];
      if (traits[i] == INSOLATION) insolation = (Integer) traits[i + 1];
      if (traits[i] == IS_OCEAN  ) isOcean    = true;
      if (traits[i] == IS_WASTE  ) isWaste    = true;
      if (traits[i] == IS_SPECKLE) isSpeckled = true;
    }
    
    if (fM == null || fM.length == 0) {
      this.floraSpecies = null;
    }
    else {
      this.floraSpecies = new Species[fM.length];
      int fI = 0;
      for (CutoutModel sequence[] : fM) {
        final Species s = new Species(
          baseClass, name+" Flora No. "+fI, Flora.DEFAULT_INFO,
          null, sequence,
          Species.Type.FLORA, 0.5f, moisture / 10f, false
        ) {};
        floraSpecies[fI++] = s;
      }
    }
  }
  
  
  public int   biomass   () { return biomass   ; }
  public float moisture  () { return moisture  ; }
  public float insolation() { return insolation; }
  public float minerals  () { return rockiness ; }
  
  public boolean isOcean() { return isOcean; }
  public boolean isWaste() { return isWaste; }
  
  public boolean isSpeckle() { return isSpeckled; }
  
  
  
  /**  Rendering, interface and debug methods-
    */
  public String toString() {
    return name;
  }
  
  
  public static Habitat[] GRADIENT() {
    if (fullGradient != null) return fullGradient;
    gradientSort.queueSort();
    return fullGradient = gradientSort.toArray(Habitat.class);
  }
}











