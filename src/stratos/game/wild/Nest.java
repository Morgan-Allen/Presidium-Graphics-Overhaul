/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



public class Nest extends Venue {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  private static boolean
    crowdingVerbose = false,
    idealVerbose    = false,
    updateVerbose   = false;
  
  final public static int
    BROWSER_SEPARATION  = Stage.SECTOR_SIZE  / 2,
    SPECIES_SEPARATION  = Stage.SECTOR_SIZE  / 2,
    PREDATOR_SEPARATION = BROWSER_SEPARATION * 2,
    MAX_SEPARATION      = Stage.SECTOR_SIZE  * 2,
    
    BROWSER_RATIO   = 12,
    PREDATOR_RATIO  = 8 ,
    MAX_CROWDING    = 10,
    DEFAULT_BREED_INTERVAL = Stage.STANDARD_DAY_LENGTH;
  
  final static float
    SAMPLE_AREA  = Nums.square(BROWSER_SEPARATION),
    SAMPLE_RATIO = SAMPLE_AREA / Nums.square(StageTerrain.SAMPLE_RESOLUTION);
  
  
  final Species species;
  private float idealPopEstimate = -1;
  
  
  final public static VenueProfile VENUE_PROFILES[];
  static {
    final Species nesting[] = Species.ANIMAL_SPECIES;
    VENUE_PROFILES = new VenueProfile[nesting.length];
    for (int n = nesting.length ; n-- > 0;) {
      VENUE_PROFILES[n] = nesting[n].nestProfile();
    }
  }
  
  
  /**  More typical construction and save/load methods-
    */
  protected Nest(
    VenueProfile profile, Base base,
    Species species, ModelAsset lairModel
  ) {
    super(profile, base);
    this.species = species;
    attachSprite(lairModel.makeSprite());
  }
  
  
  public Nest(Session s) throws Exception {
    super(s);
    species = Species.ALL_SPECIES[s.loadInt()];
    idealPopEstimate = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt(species.ID);
    s.saveFloat(idealPopEstimate);
  }
  
  
  
  /**  Behavioural assignments (all null in this case.)
    */
  protected Behaviour jobFor(Actor actor, boolean onShift) { return null; }
  public Traded[] services() { return null; }
  
  
  public Background[] careers() {
    return new Background[] { species };
  }
  
  public int numOpenings(Background b) {
    final int nO = super.numOpenings(b);
    if (b == species) return nO + MAX_CROWDING;
    return 0;
  }
  
  public float crowdRating(Actor a, Background b) {
    if (b == species) return super.crowdRating(a, b);
    if (b == Backgrounds.AS_RESIDENT) return 0;
    return 1;
  }
  
  
  
  //  Only allow entry to the same species.
  public boolean allowsEntry(Mobile m) {
    return (m instanceof Actor) && ((Actor) m).species() == species;
  }
  
  //  Nests have no road connections.
  protected void updatePaving(boolean inWorld) {}
  
  
  
  /**  Methods for determining crowding and site placement-
    */
  private static float idealPopulation(
    Target point, Species species, Stage world
  ) {
    final Base base = Base.wildlife(world);
    float foodSupply = 0;
    
    if (species.browser()) {
      foodSupply = world.terrain().fertilitySample(world.tileAt(point));
      foodSupply *= SAMPLE_AREA / (BROWSER_RATIO * SAMPLE_RATIO);
    }
    else {
      foodSupply = base.demands.supplyAround(point, Species.KEY_BROWSER, -1);
      foodSupply /= PREDATOR_RATIO;
    }
    return foodSupply / species.metabolism();
  }
  

  public static float crowdingFor(Target point, Species species, Stage world) {
    if (point == null || species == null) return 1;
    
    final float idealPop = idealPopulation(point, species, world);
    if (idealPop == 0) return 1;
    
    final Base   base     = Base.wildlife(world);
    final float  mass     = species.metabolism();
    final String category = species.type.name() ;
    final float
      allType     = base.demands.supplyAround(point, category, -1),
      allSpecies  = base.demands.supplyAround(point, species , -1),
      rarity      = Nums.clamp(1 - (allSpecies / allType), 0, 1),
      competition = allType / ((1 + rarity) * mass);
    
    if (true) I.reportVars(
      "\nGetting crowding at "+point+" for "+species, "  ",
      "all of type", allType,
      "all of species", allSpecies,
      "rarity", rarity,
      "competition", competition,
      "metabolic mass", mass,
      "ideal population", idealPop
    );
    return competition / idealPop;
  }
  
  
  public static float crowdingFor(Actor fauna) {
    return crowdingFor(fauna.mind.home(), fauna.species(), fauna.world());
  }
  
  
  public float ratePlacing(Target point, boolean exact) {
    final float
      idealPop = idealPopulation(point, species, point.world()),
      crowding = crowdingFor    (point, species, point.world());
    
    //  TODO:  Cache the crowd-rating/ideal-pop if possible?
    return idealPop * (1 - crowding);
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    final int INTERVAL = 10;
    
    //  TODO:  Impose these supply quotients on world-entry as well.
    if (numUpdates % INTERVAL == 0 && ! instant) {
      impingeDemands(base.demands, INTERVAL);
    }
  }
  
  
  public boolean enterWorldAt(int x, int y, Stage world) {
    if (! super.enterWorldAt(x, y, world)) return false;
    impingeDemands(base.demands, -1);
    return true;
  }
  
  
  protected void impingeDemands(BaseDemands demands, int period) {
    final float  idealPop = idealPopulation(this, species, world);
    final float  mass     = species.metabolism() * idealPop;
    final String category = species.type.name();
    demands.impingeSupply(species , mass, period, this);
    demands.impingeSupply(category, mass, period, this);
  }
  
  
  public static Nest findNestFor(Fauna fauna) {
    //  TODO:  Work this out.
    return null;
  }
  
  
  public static int forageRange(Species s) {
    return s.predator() ? PREDATOR_SEPARATION : BROWSER_SEPARATION;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(BaseUI UI) {
    return null;
  }


  public String helpInfo() {
    return species.info;
  }
  
  
  public SelectionPane configPanel(SelectionPane panel, BaseUI UI) {
    panel = VenuePane.configSimplePanel(this, panel, UI, null);
    
    /*
    final Description d = panel.detail(), l = panel.listing();

    int idealPop = 1 + (int) idealPopulation(this, species, world, true);
    int actualPop = staff.residents().size();
    
    l.append("Nesting: ("+actualPop+"/"+idealPop+")");
    if (staff.residents().size() == 0) {
      l.append("Unoccupied");
    }
    else for (Actor actor : staff.residents()) {
      final Composite portrait = actor.portrait(UI);
      ((Text) l).insert(portrait.texture(), 40, true);
      l.append(" ");
      l.append(actor);
    }
    //*/
    return panel;
  }
  
  
  public String objectCategory() {
    return InstallationPane.TYPE_HIDDEN;
  }
  

  public void renderSelection(Rendering rendering, boolean hovered) {
    if (destroyed() || origin() == null) return;
    BaseUI.current().selection.renderCircleOnGround(rendering, this, hovered);
  }
}



