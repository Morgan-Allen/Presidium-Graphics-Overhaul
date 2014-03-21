

package stratos.game.base ;
import stratos.game.common.* ;
import stratos.game.planet.* ;
import stratos.game.actors.* ;
import stratos.game.building.* ;
import stratos.graphics.common.* ;
import stratos.graphics.cutout.* ;
import stratos.graphics.widgets.* ;
import stratos.user.* ;
import stratos.util.* ;




/*
 Crops and Flora include:
   Durwheat                     (primary carbs on land)
   Oni Rice                     (primary carbs in water)
   Broadfruits                  (secondary greens on land)
   Tuber Lily                   (secondary greens in water)
   Ant/termite/bee/worm cells   (protein on land)
   Fish/mussel/clam farming     (protein in water)
   
   Vapok Canopy/Broadleaves  (tropical)   //...Consider merging with desert?
   Mixtaob Tree/Glass Cacti  (desert)
   Redwood/Cushion Plants    (tundra)
   Strain XV97/Mycon Burst   (wastes)
   Lichens/Algal Bloom       (pioneer species)
   Coral Beds/Kelp Forest    (seas/oceans)
   
   ...You'll also need to include flora sets for the changelings and silicates.
//*/



public class Plantation extends Venue implements
  TileConstants, Economy
{
  
  
  final static String IMG_DIR = "media/Buildings/ecologist/" ;
  final static ImageAsset ICON = ImageAsset.fromImage(
    "media/GUI/Buttons/nursery_button.gif", Plantation.class
  );
  final static ModelAsset
    NURSERY_MODEL = CutoutModel.fromImage(
      Plantation.class, IMG_DIR+"curing_shed.png", 2, 2
    ),
    COVERING_LEFT = CutoutModel.fromImage(
      Plantation.class, IMG_DIR+"covering_left.png", 1, 1
    ),
    COVERING_RIGHT = CutoutModel.fromImage(
      Plantation.class, IMG_DIR+"covering_right.png", 1, 1
    ),
    CROP_MODELS[][] = CutoutModel.fromImageGrid(
      Plantation.class, IMG_DIR+"all_crops.png",
      4, 4, 1, 1
    ),
    GRUB_BOX_MODEL = CutoutModel.fromImage(
      Plantation.class, IMG_DIR+"grub_box.png", 1, 1
    ) ;
  final public static Species ALL_VARIETIES[] = {
    Species.ONI_RICE,
    Species.DURWHEAT,
    Species.TUBER_LILY,
    Species.BROADFRUITS,
    Species.HIVE_GRUBS
  } ;
  
  //
  //  TODO:  Move most or all of this out to the Species class, where it
  //         belongs.
  final static Object CROP_SPECIES[][] = {
    new Object[] { Species.ONI_RICE, CARBS   , CROP_MODELS[0] },
    new Object[] { Species.DURWHEAT, CARBS   , CROP_MODELS[1] },
    new Object[] { Species.TUBER_LILY, GREENS  , CROP_MODELS[3] },
    new Object[] { Species.BROADFRUITS, GREENS  , CROP_MODELS[2] },
    new Object[] { Species.HIVE_GRUBS, PROTEIN , new ModelAsset[] { GRUB_BOX_MODEL }},
    null,
    null,
    new Object[] { Species.TIMBER, GREENS, null },
  } ;
  
  
  public static Service speciesYield(Species s) {
    final int varID = Visit.indexOf(s, ALL_VARIETIES) ;
    return (Service) CROP_SPECIES[varID][1] ;
    //
    //  TODO:  Use this instead...
    //return s.nutrients[0].type ;
  }
  
  
  public static ModelAsset speciesModel(Species s, int growStage) {
    final int varID = Visit.indexOf(s, ALL_VARIETIES) ;
    final ModelAsset seq[] = (ModelAsset[]) CROP_SPECIES[varID][2] ;
    return seq[Visit.clamp(growStage, seq.length)] ;
  }
  
  
  public static Species pickSpecies(Tile t, BotanicalStation parent) {
    final Float chances[] = new Float[5] ;
    int i = 0 ; for (Species s : ALL_VARIETIES) {
      final float stocked = 50 + parent.stocks.amountOf(speciesYield(s)) ;
      chances[i++] = parent.growBonus(t, s, false) / stocked ;
    }
    return (Species) Rand.pickFrom(ALL_VARIETIES, chances) ;
  }
  
  
  
  
  /**  Constructors, data fields, setup and save/load methods-
    */
  final static int
    TYPE_NURSERY = 0,
    TYPE_BED     = 1,
    TYPE_COVERED = 2 ;
  
  
  final BotanicalStation belongs ;
  final int type ;
  final int facing ;
  final Plantation strip[] ;
  final Crop planted[] = new Crop[4] ;
  
  private float needsTending = 0 ;
  
  
  
  public Plantation(
    BotanicalStation belongs, int type, int facing, Plantation strip[]
  ) {
    super(2, 2, (ENTRANCE_WEST + (facing / 2)) % 4, belongs.base()) ;
    final boolean IN = type == TYPE_NURSERY ;
    structure.setupStats(
      IN ? 50 : 20,  //integrity
      3,  //armour
      IN ? 30 : 10,  //build cost
      0,  //max upgrades
      Structure.TYPE_FIXTURE
    ) ;
    this.belongs = belongs ;
    this.type = type ;
    this.facing = facing ;
    this.strip = strip ;
  }
  

  public Plantation(Session s) throws Exception {
    super(s) ;
    belongs = (BotanicalStation) s.loadObject() ;
    type = s.loadInt() ;
    facing = s.loadInt() ;
    
    final int SS = s.loadInt() ;
    strip = new Plantation[SS] ;
    for (int p = SS ; p-- > 0 ;) strip[p] = (Plantation) s.loadObject() ;
    for (int i = 0 ; i < 4 ; i++) {
      planted[i] = (Crop) s.loadObject() ;
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(belongs) ;
    s.saveInt(type) ;
    s.saveInt(facing) ;
    
    s.saveInt(strip.length) ;
    for (Plantation p : strip) s.saveObject(p) ;
    for (Crop c : planted) s.saveObject(c) ;
  }
  
  
  public int owningType() {
    return FIXTURE_OWNS ;
  }
  
  
  public boolean privateProperty() {
    return true ;
  }
  
  
  public int pathType() {
    if (type == TYPE_NURSERY) return Tile.PATH_BLOCKS ;
    if (type == TYPE_COVERED) return Tile.PATH_BLOCKS ;
    return Tile.PATH_HINDERS ;
  }
  
  
  protected void updatePaving(boolean inWorld) {
    if (type == TYPE_NURSERY) super.updatePaving(inWorld) ;
    else {
      final Paving paving = base().paving ;
      paving.updatePerimeter(this, inWorld) ;
    }
  }
  
  
  
  /**  Establishing crop areas-
    */
  final static int
    STRIP_DIRS[]  = { N, E, S, W },
    CROPS_POS[] = { 0, 0, 0, 1, 1, 0, 1, 1 } ;
  
  
  public boolean enterWorldAt(int x, int y, World world) {
    if (! super.enterWorldAt(x, y, world)) return false ;
    if (type == TYPE_NURSERY) {
      attachModel(NURSERY_MODEL) ;
    }
    else {
      for (int c = 0, i = 0 ; c < 4 ; c++) {
        final Tile t = world.tileAt(x + CROPS_POS[i++], y + CROPS_POS[i++]) ;
        ///I.say("Creating new bed, species: "+Species.CROP_SPECIES[0]) ;
        planted[c] = new Crop(
          this, Species.CROP_SPECIES[0], t
        ) ;
      }
      refreshCropSprites() ;
    }
    return true ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    
    if (! structure.intact()) return ;
    structure.setAmbienceVal(2) ;
    
    if (type == TYPE_NURSERY && numUpdates % 10 == 0) {
      final float
        decay  = 10 * 0.1f / World.STANDARD_DAY_LENGTH,
        growth = 10 * Planet.dayValue(world) / World.GROWTH_INTERVAL ;
      for (Item seed : stocks.matches(SAMPLES)) {
        stocks.removeItem(Item.withAmount(seed, decay)) ;
        final Species s = (Species) seed.refers ;
        if (s != null) {
          final Service yield = Plantation.speciesYield(s) ;
          stocks.bumpItem(yield, growth * seed.quality, 10) ;
        }
      }
    }
  }
  
  
  public void onGrowth(Tile t) {
    if (! structure.intact()) return ;
    //
    //  Here, we average fertility over the plantation as a whole-
    //  TODO:  Possibly combine with irrigation effects from water supply or
    //  life support?
    float avgMoisture = 0, count = 0 ;
    for (Plantation p : strip) {
      final Tile o = p.origin() ;
      for (int c = 0, i = 0 ; c < 4 ; c++) {
        final Tile u = world.tileAt(
          o.x + CROPS_POS[i++],
          o.y + CROPS_POS[i++]
        ) ;
        avgMoisture += u.habitat().moisture() ;
        count++ ;
      }
    }
    avgMoisture /= count * 10 ;
    if (type == TYPE_COVERED) avgMoisture = (avgMoisture + 1) / 2f ;
    //
    //  Then apply growth to each crop-
    boolean anyChange = false ;
    for (Crop c : planted) if (c != null) {
      final int oldGrowth = (int) c.growStage ;
      c.doGrowth(avgMoisture, 0.25f) ;
      final int newGrowth = (int) c.growStage ;
      if (oldGrowth != newGrowth) anyChange = true ;
      world.ecology().impingeBiomass(this, c.health * c.growStage / 2f, true) ;
    }
    checkCropStates() ;
    if (anyChange) refreshCropSprites() ;
  }
  
  
  protected void checkCropStates() {
    needsTending = 0 ;
    for (Crop c : planted) if (c != null && c.needsTending()) needsTending++ ;
    needsTending /= planted.length ;
  }
  
  
  protected void refreshCropSprites() {
    if (sprite() != null) {
      final GroupSprite oldSprite = (GroupSprite) buildSprite().baseSprite() ;
      world.ephemera.addGhost(this, 2, oldSprite, 2.0f) ;
    }
    
    final GroupSprite s = new GroupSprite() ;
    final Tile o = origin() ;
    for (int i = 4 ; i-- > 0 ;) {
      final Crop c = planted[i] ;
      if (c == null) continue ;
      //
      //  Update the sprite-
      ModelAsset m = null ; if (type == TYPE_COVERED) {
        if ((facing == N || facing == S) && c.tile.x == o.x + 1) {
          m = COVERING_RIGHT ;
        }
        if ((facing == E || facing == W) && c.tile.y == o.y + 0) {
          m = COVERING_LEFT ;
        }
      }
      if (m == null) {
        m = Plantation.speciesModel(c.species, (int) c.growStage);
      }
      s.attach(m,
        CROPS_POS[i * 2] - 0.5f,
        CROPS_POS[(i * 2) + 1] - 0.5f,
        0
      ) ;
    }
    attachSprite(s) ;
    setAsEstablished(false) ;
  }
  
  
  Crop[] planted() {
    return planted ;
  }
  
  
  protected Crop plantedAt(Tile t) {
    for (Crop c : planted) if (c != null && c.tile == t) return c ;
    return null ;
  }
  
  
  protected float needForTending() {
    if (type != TYPE_NURSERY || ! structure.intact()) return 0 ;
    float sum = 0 ;
    for (Plantation p : strip) sum += p.needsTending ;
    return sum / (strip.length - 1) ;
  }
  
  
  public Service[] services() { return null ; }
  
  public Background[] careers() { return null ; }
  
  public Behaviour jobFor(Actor actor) { return null ; }
  
  
  
  /**  Finding space.
    */
  static Plantation[] placeAllotment(
    final BotanicalStation parent, final int minSize, boolean covered
  ) {
    final World world = parent.world() ;
    
    Plantation bestSite[] = null ;
    float bestRating = 0 ;
    
    for (int m = 10 ; m-- > 0 ;) {
      final Tile t = Spacing.pickRandomTile(parent, 12, world) ;
      if (t == null) continue ;
      final int off = Rand.index(4) ;
      for (int n = 4 ; n-- > 0 ;) {
        final Plantation allots[] = new Plantation[minSize] ;
        final int i = (n + off) % 4 ;
        if (tryPlacementAt(t, parent, allots, STRIP_DIRS[i], covered)) {
          final float rating = rateArea(allots, world) ;
          if (rating > bestRating) { bestSite = allots ; bestRating = rating ; }
        }
      }
    }
    if (bestSite != null) {
      for (Plantation p : bestSite) {
        final Tile o = p.origin() ;
        p.doPlace(o, o) ;
      }
      return bestSite ;
    }
    return null ;
  }
  
  
  static float rateArea(Plantation allots[], World world) {
    //
    //  Favour fertile, unpaved areas close to the parent botanical station but
    //  farther from other structures-
    float
      fertility = 0, num = 0,
      minDist = World.SECTOR_SIZE, parentDist = 0 ;
    for (Plantation p : allots) {
      parentDist += Spacing.distance(p, p.belongs) ;
      Target close = world.presences.nearestMatch(Venue.class, p, minDist) ;
      if (
        close != null && close != p.belongs &&
        ! (close instanceof Plantation)
      ) {
        minDist = Spacing.distance(p, close) ;
      }
      for (Tile t : world.tilesIn(p.area(), false)) {
        fertility += t.habitat().moisture() ;
        if (t.pathType() == Tile.PATH_ROAD) fertility /= 2 ;
        num++ ;
      }
    }
    float rating = fertility / num ;
    rating *= 1 - (parentDist / (allots.length * World.SECTOR_SIZE)) ;
    rating *= minDist / World.SECTOR_SIZE ;
    return rating ;
  }
  
  
  private static boolean tryPlacementAt(
    Tile t, BotanicalStation parent, Plantation allots[],
    int dir, boolean covered
  ) {
    for (int i = 0 ; i < allots.length ; i++) try {
      final Plantation p = allots[i] = new Plantation(
        parent, i == 0 ? TYPE_NURSERY : (covered ? TYPE_COVERED : TYPE_BED),
        dir, allots
      ) ;
      p.setPosition(
        t.x + (N_X[dir] * 2 * i),
        t.y + (N_Y[dir] * 2 * i),
        t.world
      ) ;
      if (! p.canPlace()) return false ;
    } catch (Exception e) { return false ; }
    return true ;
  }
  

  protected boolean canTouch(Element e) {
    return e.owningType() < this.owningType() ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(BaseUI UI) {
    final Composite cached = Composite.fromCache("plantation");
    if (cached != null) return cached;
    return Composite.withImage(ICON, "plantation");
  }
  
  
  public String fullName() {
    return type == TYPE_NURSERY ? "Nursery" : "Plantation" ;
  }
  
  
  public String helpInfo() {
    if (type == TYPE_NURSERY) return
      "Nurseries allow young plants to be cultivated in a secure environment "+
      "prior to outdoor planting, and provide a small but steady food yield "+
      "regardless of outside conditions." ;
    return
      "Plantations of managed, mixed-culture cropland secure a high-quality "+
      "food source for your base, but require space and constant attention." ;
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_ECOLOGIST ;
  }
  
  
  public InfoPanel configPanel(InfoPanel panel, BaseUI UI) {
    if (panel == null) panel = new InfoPanel(
      UI, this, 0
    );
    super.configPanel(panel, UI);
    final Description d = panel.detail();
    
    if (type == TYPE_NURSERY) {
      d.append("\n") ;
      boolean any = false ;
      for (Item seed : stocks.matches(SAMPLES)) {
        final Species s = (Species) seed.refers ;
        d.append("\n  Seed for "+s+" (") ;
        d.append(Crop.HEALTH_NAMES[(int) seed.quality]+" quality)") ;
        any = true ;
      }
      if (! any) d.append(
        "\nThis nursery needs gene-tailored seed stock before it can maximise "+
        "crop yield."
      ) ;
    }
    else {
      d.append("\n") ;
      for (Crop c : planted) {
        d.append("\n  "+c) ;
      }
    }
    return panel;
  }
}
















