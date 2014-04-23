/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild ;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.maps.Flora;
import stratos.game.maps.Habitat;
import stratos.game.maps.Species;
import stratos.game.maps.Species.Type;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



public class Nest extends Venue {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static int
    BROWSER_SEPARATION = World.SECTOR_SIZE / 2,
    SPECIES_SEPARATION = World.SECTOR_SIZE / 2,
    PREDATOR_SEPARATION = BROWSER_SEPARATION * 2,
    MAX_SEPARATION  = World.SECTOR_SIZE * 2,
    
    BROWSING_SAMPLE = 8 ,
    BROWSER_RATIO   = 12,
    PREDATOR_RATIO  = 8,
    OMNIVORE_BONUS  = 2,
    
    MAX_CROWDING    = 10,
    NEW_SITE_SAMPLE = 2 ,
    DEFAULT_BREED_INTERVAL = World.STANDARD_DAY_LENGTH ;
  
  private static boolean verbose = false ;
  
  
  final Species species ;
  private float idealPopEstimate = -1 ;
  
  
  public Nest(
    int size, int high, int entranceFace,
    Species species, ModelAsset lairModel
  ) {
    super(size, high, entranceFace, null) ;
    this.species = species ;
    attachSprite(lairModel.makeSprite()) ;
  }
  
  
  public Nest(Session s) throws Exception {
    super(s) ;
    species = Species.ALL_SPECIES[s.loadInt()] ;
    idealPopEstimate = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(species.ID) ;
    s.saveFloat(idealPopEstimate) ;
  }
  
  
  
  /**  Behavioural assignments (all null in this case.)
    */
  public Behaviour jobFor(Actor actor) { return null ; }
  public Background[] careers() { return null ; }
  public Service[] services() { return null ; }
  public int owningType() { return Element.ELEMENT_OWNS ; }
  
  
  public boolean allowsEntry(Mobile m) {
    return (m instanceof Actor) && ((Actor) m).species() == species ;
  }
  
  
  
  /**  Methods for determining crowding and site placement-
    */
  static int minSpacing(Venue nest, Venue other, Species species) {
    final Species OS = (other instanceof Nest) ?
      ((Nest) other).species : null ;
    int spacing = (species.browser() && OS != null && OS.browser()) ?
      BROWSER_SEPARATION : PREDATOR_SEPARATION ;
    if (species == OS) spacing += SPECIES_SEPARATION ;
    return spacing ;
  }
  
  
  //  TODO:  Use the fertility sampling methods in the terrain class!
  
  private static float sampleFertility(Tile point) {
    final int range = BROWSER_SEPARATION * 2 ;// + (SPECIES_SEPARATION / 2) ;
    float fertility = 0 ;
    int numSamples = BROWSING_SAMPLE ; while (numSamples-- > 0) {
      final Tile t = point.world.tileAt(
        point.x + Rand.range(-range, range),
        point.y + Rand.range(-range, range)
      ) ;
      if (t == null) continue ;
      final Habitat h = t.habitat() ;
      if (h.isOcean() || h.isWaste()) continue ;
      else {
        final float moisture = Visit.clamp((h.moisture() - 2) / 10f, 0, 1) ;
        fertility += moisture / 4f ;
        if (t.owner() instanceof Flora) {
          final Flora f = (Flora) t.owner() ;
          fertility += (f.growStage() + 0.5f) * moisture / 1.5f ;
        }
      }
    }
    return fertility * (range * range) / BROWSING_SAMPLE ;
  }
  
  
  private static int idealPopulation(
    Venue nest, Species species, World world
  ) {
    final boolean reports = verbose && I.talkAbout == nest ;
    //
    //  Firstly, ensure there is no other lair within lair placement range.
    //final int range = minSpacing(null, spot, species) * 2 ;
    final int range = MAX_SEPARATION ;
    int numLairsNear = 0 ;
    float alikeLairs = 0 ;
    float preySupply = 0 ;
    for (Object o : world.presences.matchesNear(Venue.class, nest, range)) {
      final Venue v = (Venue) o ;
      final List <Actor> resident = v.personnel.residents() ;
      if (v == nest || resident.size() == 0) continue ;
      
      final int spacing = minSpacing(nest, v, species) ;
      if (reports) I.say("Minimum spacing from "+v+" is "+spacing) ;
      if (Spacing.distance(nest, v) < spacing) return -1 ;
      
      float alike = 0 ;
      for (Actor a : resident) {
        final Species s = a.species();
        if (s != null && s == species) alike++ ;
        if (s != null && s.browser()) preySupply += a.species().metabolism() ;
      }
      numLairsNear++ ;
      alikeLairs += alike / resident.size() ;
    }
    //
    //  Secondly, sample the fertility & biomass of nearby terrain (within half
    //  of lair placement range.)  (We average with presumed default growth
    //  levels for local flora.)
    final float fertility = sampleFertility(world.tileAt(nest)) ;
    if (reports) I.say("\nFinding ideal browser population at "+nest) ;
    if (reports) I.add("  Total fertility at "+nest+" is "+fertility) ;
    //
    //  Then return the correct number of inhabitants for the location-
    float foodSupply, ratio ;
    if (species.predator()) {
      foodSupply = preySupply ;
      ratio = PREDATOR_RATIO ;
    }
    else {
      foodSupply = fertility ;
      ratio = BROWSER_RATIO ;
      if (! species.browser()) foodSupply *= OMNIVORE_BONUS ;
    }
    
    final float
      metabolism = species.metabolism(),
      rarity = 1f - (alikeLairs / (numLairsNear + 1)),
      idealPop = (foodSupply * rarity) / (metabolism * ratio) ;
    if (reports) I.say("  Ideal population is: "+idealPop) ;
    
    return (int) (idealPop - 0.5f) ;
  }
  
  
  public static float idealNestPop(
    Species species, Venue site, World world, boolean cached
  ) {
    final Nest nest = (cached && site instanceof Nest) ?
      (Nest) site : null ;
    if (nest != null && nest.idealPopEstimate != -1) {
      return nest.idealPopEstimate ;
    }
    //  TODO:  Repeating the sample has no particular benefit in the case of
    //  predator nests, and is still unreliable in the case of browser nests.
    //  Consider doing a brute-force flood-fill check instead?
    float estimate = 0 ; for (int n = NEW_SITE_SAMPLE ; n-- > 0 ;) {
      estimate += idealPopulation(site, species, world) * 1f / NEW_SITE_SAMPLE ;
    }
    if (nest != null && nest.idealPopEstimate == -1) {
      nest.idealPopEstimate = estimate ;
    }
    return estimate ;
  }
  
  
  public static float crowdingFor(Actor actor) {
    return crowdingFor(actor.mind.home(), actor.species(), actor.world()) ;
  }
  
  
  public static float crowdingFor(Boardable home, Object species, World world) {
    if (! (home instanceof Venue)) return MAX_CROWDING ;
    if (! (species instanceof Species)) return MAX_CROWDING ;
    final Venue venue = (Venue) home ;
    float actualPop = 0 ; for (Actor a : venue.personnel.residents()) {
      if (a.health.alive()) actualPop++ ;
    }
    final float idealPop = idealNestPop(
      (Species) species, venue, world, true
    ) ;
    if (idealPop <= 0) return MAX_CROWDING ;
    if (verbose && I.talkAbout == venue) {
      I.say("Actual/ideal population: "+actualPop+"/"+idealPop) ;
    }
    return Visit.clamp(actualPop / (1 + idealPop), 0, MAX_CROWDING) ;
  }
  
  
  public static int forageRange(Species s) {
    return (s.type == Species.Type.PREDATOR) ?
      PREDATOR_SEPARATION :
      BROWSER_SEPARATION  ;
  }
  
  
  protected static Nest findNestFor(Fauna fauna) {
    //
    //  If you're homeless, or if home is overcrowded, consider moving into a
    //  vacant lair, or building a new one.
    final World world = fauna.world() ;
    final float range = forageRange(fauna.species) ;
    if (crowdingFor(fauna) > 0.5f) {
      Nest bestNear = null ;
      float bestRating = 0 ;
      for (Object o : world.presences.sampleFromMap(
        fauna, world, 5, null, Nest.class
      )) {
        final Nest l = (Nest) o ;
        if (l.species != fauna.species) continue ;
        final float dist = Spacing.distance(l, fauna) ;
        if (dist > range) continue ;
        final float crowding = crowdingFor(l, l.species, world) ;
        if (crowding > 0.5f) continue ;
        float rating = (1 - crowding) * range / (range + dist) ;
        rating *= Rand.avgNums(2) * 4 ;
        if (rating > bestRating) { bestRating = rating ; bestNear = l ; }
      }
      if (bestNear != null) return bestNear ;
    }
    //
    //  If no existing lair is suitable, try establishing a new one-
    Tile toTry = Spacing.pickRandomTile(fauna, range * 2, world) ;
    toTry = Spacing.nearestOpenTile(toTry, fauna) ;
    if (toTry == null) return null ;
    return siteNewLair(fauna.species, toTry, world) ;
  }
  
  
  protected static Nest siteNewLair(
    Species species, final Target client, final World world
  ) {
    final float range = forageRange(species) ;
    final Nest newLair = species.createNest() ;
    final TileSpread spread = new TileSpread(world.tileAt(client)) {
      
      protected boolean canAccess(Tile t) {
        return Spacing.distance(client, t) < range ;
      }
      
      protected boolean canPlaceAt(Tile t) {
        newLair.setPosition(t.x, t.y, world) ;
        return newLair.canPlace() ;
      }
    } ;
    spread.doSearch() ;
    if (spread.success()) {
      final float idealPop = idealNestPop(species, newLair, world, false) ;
      if (idealPop <= 0) return null ;
      return newLair ;
    }
    return null ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (numUpdates % 10 != 0) return ;
    
    final float idealPop = idealNestPop(species, this, world, false) ;
    final float inc = 10f / World.STANDARD_DAY_LENGTH ;
    if (idealPopEstimate == -1) {
      idealPopEstimate = idealPop ;
    }
    else {
      idealPopEstimate *= 1 - inc ;
      idealPopEstimate += idealPop * inc ;
    }
    if (verbose && I.talkAbout == this) {
      I.say("Estimate increment is: "+inc+", value: "+idealPop) ;
      I.say("Ideal population estimate: "+idealPopEstimate) ;
    }
  }
  
  
  protected void updatePaving(boolean inWorld) {}
  
  
  
  
  /**  Placing the site-
    */
  public static void placeNests(
    final World world, final Species... species
  ) {
    //  TODO:  Use the SitingPass code for this?
    
    //  Use fertility samples from terrain type, together with biomass summaries
    //  from the ecology() class.
    
    
    
    final int SS = World.SECTOR_SIZE ;
    int numAttempts = (world.size * world.size * 4) / (SS * SS) ;
    ///I.say("No. of attempts: "+numAttempts) ;
    final Base wildlife = Base.baseWithName(world, Base.KEY_WILDLIFE, true) ;
    
    while (numAttempts-- > 0) {
      Tile tried = world.tileAt(
        Rand.index(world.size),
        Rand.index(world.size)
      ) ;
      tried = Spacing.nearestOpenTile(tried, tried) ;
      if (tried == null) continue ;
      Nest toPlace = null ;
      float bestRating = 0 ;
      
      for (Species s : species) {
        final Nest nest = Nest.siteNewLair(s, tried, world) ;
        if (nest == null) continue ;
        final float
          idealPop = Nest.idealNestPop(s, nest, world, false),
          adultMass = s.baseBulk * s.baseSpeed,
          rating = (idealPop * adultMass) + 0.5f ;
        if (rating > bestRating) { toPlace = nest ; bestRating = rating ; }
      }
      
      if (toPlace != null) {
        I.say("New lair for "+toPlace.species+" at "+toPlace.origin()) ;
        toPlace.doPlace(toPlace.origin(), null) ;
        toPlace.assignBase(wildlife) ;
        toPlace.structure.setState(Structure.STATE_INTACT, 1) ;
        final Species s = toPlace.species ;
        final float adultMass = s.baseBulk * s.baseSpeed ;
        float bestPop = bestRating / adultMass ;
        
        while (bestPop-- > 0) {
          final Fauna f = (Fauna) toPlace.species.newSpecimen(wildlife) ;
          f.health.setupHealth(Rand.num(), 0.9f, 0.1f) ;
          f.mind.setHome(toPlace) ;
          if (Rand.num() < 0.1f) {
            f.enterWorldAt(toPlace, world) ;
            f.goAboard(toPlace, world) ;
          }
          else {
            final Tile t = Spacing.pickRandomTile(
              toPlace, BROWSER_SEPARATION, world
            );
            f.enterWorldAt(t, world);
          }
        }
      }
    }
  }
  
  

  /**  Rendering and interface methods-
    */
  public String fullName() {
    return species.name+" Nest" ;
  }
  
  
  public Composite portrait(BaseUI UI) {
    return null ;
  }
  
  
  public void writeInformation(Description d, int categoryID, HUD UI) {
    d.append("\nCondition: ") ;
    d.append((int) (structure.repairLevel() * 100)+"%") ;
    
    int idealPop = 1 + (int) idealNestPop(species, this, world, true) ;
    int actualPop = personnel.residents().size() ;
    d.append("\n  Population: "+actualPop+"/"+idealPop) ;
    
    d.append("\nNesting: ") ;
    if (personnel.residents().size() == 0) d.append("Unoccupied") ;
    else for (Actor actor : personnel.residents()) {
      d.append("\n  ") ;
      d.append(actor) ;
    }
    
    d.append("\n\n") ;
    d.append(species.info) ;
  }
  
  
  public String helpInfo() {
    return null ;
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_HIDDEN ;
  }
  

  public void renderSelection(Rendering rendering, boolean hovered) {
    if (destroyed() || ! inWorld()) return ;
    Selection.renderPlane(
      rendering, position(null), (xdim() / 2f) + 1,
      hovered ? Colour.transparency(0.5f) : Colour.WHITE,
      Selection.SELECT_CIRCLE
    ) ;
  }
}



