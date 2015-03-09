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
    BROWSER_SEPARATION = Stage.SECTOR_SIZE / 2,
    SPECIES_SEPARATION = Stage.SECTOR_SIZE / 2,
    PREDATOR_SEPARATION = BROWSER_SEPARATION * 2,
    MAX_SEPARATION  = Stage.SECTOR_SIZE * 2,
    
    BROWSING_SAMPLE = 8 ,
    BROWSER_RATIO   = 12,
    PREDATOR_RATIO  = 8,
    OMNIVORE_BONUS  = 2,
    
    MAX_CROWDING    = 10,
    NEW_SITE_SAMPLE = 2 ,
    DEFAULT_BREED_INTERVAL = Stage.STANDARD_DAY_LENGTH;
  
  
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
  public Behaviour jobFor(Actor actor, boolean onShift) { return null; }
  public Background[] careers() { return null; }
  public Traded[] services() { return null; }
  
  
  public boolean allowsEntry(Mobile m) {
    return (m instanceof Actor) && ((Actor) m).species() == species;
  }
  
  
  
  /**  Methods for determining crowding and site placement-
    */
  //  TODO:  Restore the full range of functions here.
  
  public static float crowdingFor(Boarding home, Object species, Stage world) {
    return 1;
    /*
    if (home == null || world == null) return 0;
    if (! (species instanceof Species)) return 0;
    final boolean report = crowdingVerbose && I.talkAbout == home;
    
    final float idealPop = idealPopulation(
      home, (Species) species, world, true
    );
    if (idealPop <= 0) return MAX_CROWDING;
    
    float actualPop = 0;
    if (home instanceof Venue) {
      final Venue venue = (Venue) home;
      for (Actor a : venue.staff.residents()) {
        if (a.health.alive() && a.species() == species) actualPop++;
      }
    }
    
    if (report) {
      I.say("    Actual/ideal population: "+actualPop+"/"+idealPop);
    }
    return Nums.clamp(actualPop / (1 + idealPop), 0, MAX_CROWDING);
    //*/
  }
  
  
  public static float crowdingFor(Actor actor) {
    return crowdingFor(actor.mind.home(), actor.species(), actor.world());
  }
  
  
  public static int forageRange(Species s) {
    return (s.type == Species.Type.PREDATOR) ?
      PREDATOR_SEPARATION :
      BROWSER_SEPARATION ;
  }
  

  protected static Nest findNestFor(Fauna fauna) {
    return null;
  }
  
  
  /*
  private static int minSpacing(Target nest, Venue other, Species species) {
    final Species OS = (other instanceof Nest) ?
      ((Nest) other).species : null;
    int spacing = (species.browser() && OS != null && OS.browser()) ?
      BROWSER_SEPARATION : PREDATOR_SEPARATION;
    if (species == OS) spacing += SPECIES_SEPARATION;
    return spacing;
  }
  
  
  public static int idealPopulation(
    Target site, Species species, Stage world, boolean cached
  ) {
    final boolean report = idealVerbose && I.talkAbout == site;
    if (report) {
      I.say("\nGetting ideal population for "+species.name+" at "+site);
      I.say("  Metabolism: "+species.metabolism());
    }
    
    final Nest nest = (cached && site instanceof Nest) ? (Nest) site : null;
    if (nest != null && nest.idealPopEstimate != -1) {
      return (int) (nest.idealPopEstimate - 0.5f);
    }
    
    final int range = MAX_SEPARATION;
    int numLairsNear = 0;
    float alikeLairs = 0;
    float preySupply = 0;
    
    for (Object o : world.presences.matchesNear(Venue.class, site, range)) {
      final Venue v = (Venue) o;
      final List <Actor> resident = v.staff.residents();
      if (v == site || resident.size() == 0) continue;
      
      final int spacing = minSpacing(site, v, species);
      final float distance = Spacing.distance(site, v);
      if (distance < spacing) {
        if (report) {
          I.say("Too close to "+v+"!");
          I.say("Spacing: "+spacing+", distance: "+distance);
        }
        return -1;
      }
      
      float alike = 0;
      for (Actor a : resident) {
        final Species s = a.species();
        if (s != null && s == species) alike++;
        if (s != null && s.browser()) preySupply += a.species().metabolism();
      }
      numLairsNear++;
      alikeLairs += alike / resident.size();
    }

    final int browseRange = BROWSER_SEPARATION * 2;
    float fertility = 0;
    
    final Tile at = world.tileAt(site);
    float moisture = world.terrain().fertilitySample(at);
    float biomass = world.ecology().biomassRating(at) * 2;
    fertility += biomass * moisture;
    moisture = moisture - 0.2f;
    fertility += moisture / 4f;
    fertility *= browseRange * browseRange;
    
    if (report) {
      I.say("  Total fertility at "+site+" is "+fertility);
      I.say("  Moisture/Biomass: "+moisture+"/"+biomass);
    }
    
    //
    //  Then return the correct number of inhabitants for the location-
    float foodSupply, ratio;
    if (species.predator()) {
      foodSupply = preySupply;
      ratio = PREDATOR_RATIO;
    }
    else {
      foodSupply = fertility;
      ratio = BROWSER_RATIO;
      if (! species.browser()) foodSupply *= OMNIVORE_BONUS;
    }
    
    final float
      metabolism = species.metabolism(),
      rarity = 1f - (alikeLairs / (numLairsNear + 1)),
      idealPop = (foodSupply * rarity) / (metabolism * ratio);
    if (report) I.say("  Ideal population is: "+idealPop);
    
    if (nest != null && nest.idealPopEstimate == -1) {
      nest.idealPopEstimate = idealPop;
    }
    return (int) (idealPop - 0.5f);
  }
  
  
  public static float crowdingFor(Boarding home, Object species, Stage world) {
    if (home == null || world == null) return 0;
    if (! (species instanceof Species)) return 0;
    final boolean report = crowdingVerbose && I.talkAbout == home;
    
    final float idealPop = idealPopulation(
      home, (Species) species, world, true
    );
    if (idealPop <= 0) return MAX_CROWDING;
    
    float actualPop = 0;
    if (home instanceof Venue) {
      final Venue venue = (Venue) home;
      for (Actor a : venue.staff.residents()) {
        if (a.health.alive() && a.species() == species) actualPop++;
      }
    }
    
    if (report) {
      I.say("    Actual/ideal population: "+actualPop+"/"+idealPop);
    }
    return Nums.clamp(actualPop / (1 + idealPop), 0, MAX_CROWDING);
  }
  
  
  public static float crowdingFor(Actor actor) {
    return crowdingFor(actor.mind.home(), actor.species(), actor.world());
  }
  
  
  public static int forageRange(Species s) {
    return (s.type == Species.Type.PREDATOR) ?
      PREDATOR_SEPARATION :
      BROWSER_SEPARATION ;
  }
  
  
  protected static Nest findNestFor(Fauna fauna) {
    final boolean report = crowdingVerbose && I.talkAbout == fauna;
    
    //  If you're homeless, or if home is overcrowded, consider moving into a
    //  vacant lair, or building a new one.
    final Target home = fauna.mind.home();
    final Stage world = fauna.world();
    final float range = forageRange(fauna.species);
    if (home == null || crowdingFor(fauna) > 0.5f) {
      Nest bestNear = null;
      float bestRating = 0;
      for (Object o : world.presences.sampleFromMap(
        fauna, world, 5, null, Nest.class
      )) {
        final Nest l = (Nest) o;
        if (l.species != fauna.species) continue;
        final float dist = Spacing.distance(l, fauna);
        if (dist > range) continue;
        final float crowding = crowdingFor(l, l.species, world);
        if (crowding > 0.5f) continue;
        float rating = (1 - crowding) * range / (range + dist);
        rating *= Rand.avgNums(2) * 4;
        if (rating > bestRating) { bestRating = rating; bestNear = l; }
      }
      if (bestNear != null) return bestNear;
    }
    
    //  If no existing lair is suitable, try establishing a new one-
    if (report) {
      I.say("  LOOKING FOR NEW NEST, ORIGIN: "+fauna.origin());
      I.say("  Range is: "+range);
    }
    
    final Species species = fauna.species;
    Tile pick = null;
    float leastCrowd = 1, crowd;
    
    final Tile o = fauna.origin();
    crowd = Nest.crowdingFor(o, species, world);
    if (crowd < leastCrowd) { pick = o; leastCrowd = crowd; }
    
    for (int n : T_ADJACENT) {
      final Tile toTry = world.tileAt(
        o.x + (T_X[n] * Stage.SECTOR_SIZE) + (Rand.num() * range),
        o.y + (T_Y[n] * Stage.SECTOR_SIZE) + (Rand.num() * range)
      );
      crowd = Nest.crowdingFor(toTry, species, world);
      if (crowd < leastCrowd) { pick = toTry; leastCrowd = crowd; }
    }
    if (report) I.say("  Least crowding: "+leastCrowd+" at "+pick);
    
    final Nest newNest = siteNewNest(fauna.species, pick, world, report);
    if (newNest != null) {
      if (report) I.say("  NEW NEST FOUND AT "+pick);
      return newNest;
    }
    else return null;
  }
  
  
  protected static Nest siteNewNest(
    Species species, final Target client, final Stage world, boolean report
  ) {
    if (client == null || species == null) return null;
    final float range = forageRange(species);
    final Nest newLair = species.createNest();
    if (report) I.say("  Searching from "+client+"...");
    
    final TileSpread spread = new TileSpread(world.tileAt(client)) {
      
      protected boolean canAccess(Tile t) {
        return Spacing.distance(client, t) < range;
      }
      
      protected boolean canPlaceAt(Tile t) {
        newLair.setPosition(t.x, t.y, world);
        return newLair.canPlace();
      }
    };
    spread.doSearch();
    if (spread.success()) {
      final float idealPop = idealPopulation(newLair, species, world, false);
      if (report) {
        I.say("  Space at: "+newLair.origin());
        I.say("  Ideal population: "+idealPop);
      }
      if (idealPop <= 0) return null;
      return newLair;
    }
    return null;
  }
  //*/
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    /*
    if (numUpdates % 10 != 0) return;
    
    final float idealPop = idealPopulation(this, species, world, false);
    final float inc = 10f / Stage.STANDARD_DAY_LENGTH;
    if (idealPopEstimate == -1) {
      idealPopEstimate = idealPop;
    }
    else {
      idealPopEstimate *= 1 - inc;
      idealPopEstimate += idealPop * inc;
    }
    if (updateVerbose && I.talkAbout == this) {
      I.say("Estimate increment is: "+inc+", value: "+idealPop);
      I.say("Ideal population estimate: "+idealPopEstimate);
    }
    //*/
  }
  
  
  protected void updatePaving(boolean inWorld) {}
  
  
  /**  Placing the site-
    */
  /*
  //  TODO:  YOU'LL HAVE TO GET RID OF THIS SOON!
  public static void placeNests(
    final Stage world, final Species... species
  ) {
    final boolean report = idealVerbose;
    //  TODO:  Use the SitingPass code for this
    
    final int SS = Stage.SECTOR_SIZE;
    int numAttempts = (world.size * world.size * 4) / (SS * SS);
    final Base wildlife = Base.wildlife(world);
    
    while (numAttempts-- > 0) {
      Tile tried = world.tileAt(
        Rand.index(world.size),
        Rand.index(world.size)
      );
      tried = Spacing.nearestOpenTile(tried, tried);
      if (tried == null) continue;
      Nest toPlace = null;
      float bestRating = 0;
      
      for (Species s : species) {
        final Nest nest = Nest.siteNewNest(s, tried, world, report);
        if (nest == null) continue;
        final float
          idealPop = idealPopulation(nest, s, world, false),
          adultMass = s.baseBulk * s.baseSpeed,
          rating = (idealPop * adultMass) + 0.5f;
        if (rating > bestRating) { toPlace = nest; bestRating = rating; }
      }
      
      if (toPlace != null) {
        I.say("New lair for "+toPlace.species+" at "+toPlace.origin());
        toPlace.doPlacement();
        toPlace.assignBase(wildlife);
        toPlace.structure.setState(Structure.STATE_INTACT, 1);
        final Species s = toPlace.species;
        final float adultMass = s.baseBulk * s.baseSpeed;
        float bestPop = bestRating / adultMass;
        
        while (bestPop-- > 0) {
          final Fauna f = (Fauna) toPlace.species.sampleFor(wildlife);
          f.health.setupHealth(Rand.num(), 0.9f, 0.1f);
          f.mind.setHome(toPlace);
          if (Rand.num() < 0.1f) {
            f.enterWorldAt(toPlace, world);
            f.goAboard(toPlace, world);
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
  //*/
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(BaseUI UI) {
    return null;
  }


  public String helpInfo() {
    return species.info;
  }
  
  
  public SelectionInfoPane configPanel(SelectionInfoPane panel, BaseUI UI) {
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
    return InstallTab.TYPE_HIDDEN;
  }
  

  public void renderSelection(Rendering rendering, boolean hovered) {
    if (destroyed() || origin() == null) return;
    BaseUI.current().selection.renderCircleOnGround(rendering, this, hovered);
  }
}





