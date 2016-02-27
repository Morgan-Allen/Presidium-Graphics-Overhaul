/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;




public class Ruins extends Venue {
  
  
  
  /**  Construction and save/load methods-
    */
  private static boolean
    placeVerbose  = false,
    updateVerbose = false;
  
  final static ModelAsset MODEL_RUINS[] = CutoutModel.fromImages(
    Ruins.class, "ruins_models",
    "media/Buildings/lairs and ruins/", 4, 2, false,
    "ruins_a.png",
    "ruins_b.png",
    "ruins_c.png"
  );
  final static int
    MIN_RUINS_SPACING = (int) (Stage.ZONE_SIZE * 0.75f),
    MAX_RUINS_POP     = 6;
  
  
  private static int NI = (int) (Rand.unseededNum() * 3);
  
  final byte speciesCaps[] = new byte[SPECIES.length];
  
  
  public Ruins(Base base) {
    super(VENUE_BLUEPRINTS[0], base);
    staff.setShiftType(SHIFTS_BY_24_HOUR);
    final int index = (NI++ + Rand.index(1)) % 3;
    attachSprite(MODEL_RUINS[index].makeSprite());
  }
  
  
  public Ruins(Session s) throws Exception {
    super(s);
    s.loadByteArray(speciesCaps);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveByteArray(speciesCaps);
  }
  
  
  
  /**  Situation and claims-management-
    */
  final public static Blueprint VENUE_BLUEPRINTS[];
  static {
    VENUE_BLUEPRINTS = new Blueprint[1];
    VENUE_BLUEPRINTS[0] = new Blueprint(
      Ruins.class, "ruins",
      "Ancient Ruins", Target.TYPE_RUINS, null,
      "Ancient ruins cover the landscape of many worlds in regions irradiated "+
      "by nuclear fire or blighted by biological warfare.  Strange and "+
      "dangerous beings often haunt such forsaken places.",
      4, 2, Structure.IS_ANCIENT, Owner.TIER_FACILITY, 500, 25
    ) {
      public Venue createVenue(Base base) {
        final Venue sample = new Ruins(base);
        
        float initRepair = (0.5f + Rand.num()) / 2;
        sample.structure.setState(Structure.STATE_INTACT, initRepair);
        return sample;
      }
    };
    
    final Siting siting = new Siting(VENUE_BLUEPRINTS[0]) {
      
      public float rateSettlementDemand(Base base) {
        return 0;
      }
      
      public float ratePointDemand(
        Base base, Target point, boolean exact, int claimRadius
      ) {
        final boolean report = placeVerbose && (point instanceof StagePatch);
        
        final Stage world = point.world();
        final Tile under = world.tileAt(point);
        float rating = 2;
        rating -= world.terrain().fertilitySample(under);
        rating += world.terrain().habitatSample(under, Habitat.CURSED_EARTH);
        rating *= SiteUtils.worldOverlap(point, world, MIN_RUINS_SPACING);
        
        if (report) {
          I.say("\nRating ruins: "+this);
          I.say("  Point evaluated: "+point);
          I.say("  Rating is:       "+rating);
        }
        return rating;
      }
    };
    VENUE_BLUEPRINTS[0].linkWith(siting);
  }
  
  
  public Box2D areaClaimed() {
    return new Box2D(footprint()).expandBy(MIN_RUINS_SPACING);
  }
  
  
  public boolean preventsClaimBy(Venue other) {
    if (other instanceof Ruins) return false;
    return true;
  }
  
  
  protected void updatePaving(boolean inWorld) {
    //
    //  Ruins aren't, well... maintained, so neat paving isn't appropriate.
  }
  
  
  public static void populateRuins(
    Stage world, int numRuins, Species... inhabit
  ) {
    final Base artilects = Base.artilects(world);
    final Batch <Venue> placed = artilects.setup.doPlacementsFor(
      Ruins.VENUE_BLUEPRINTS[0], numRuins
    );
    artilects.setup.fillVacancies(placed, true);
  }
  
  
  
  /**  Behavioural routines-
    */
  //
  //  This is the number of days ahead of 'schedule' that a given species can
  //  be expected to spawn within the ruins, relative to the 'wakeup' time for
  //  the base as a whole (typically 30 days.)
  final static Species SPECIES[] = {
    Drone  .SPECIES,
    Tripod .SPECIES,
    Cranial.SPECIES
  };
  final static Table <Species, Float> SPECIES_SCHEDULE = Table.make(
    Drone  .SPECIES,  1.0f,
    Tripod .SPECIES,  0.0f,
    Cranial.SPECIES, -0.5f
  );
  
  
  //
  //  TODO:  Allow for mutants and other squatters as well- as long as no
  //  artilects are present!
  public float crowdRating(Actor forActor, Background background) {
    final int index = Visit.indexOf(forActor.species(), SPECIES);
    
    if (index >= 0 && speciesCaps.length > 0) {
      final int cap = speciesCaps[index];
      final int pop = staff.numResident(SPECIES[index]);
      return pop * 1f / cap;
    }
    else return 0;
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    
    if (numUpdates % 10 == 0 && base() instanceof ArtilectBase && ! instant) {
      if (GameSettings.noSpawn) return;
      
      final ArtilectBase AB = (ArtilectBase) base();
      final int numSpecies = SPECIES.length;
      final int popLimit = 1 + (int) (MAX_RUINS_POP * structure.repairLevel());
      
      float fillLevel = AB.onlineLevel();
      fillLevel += structure.repairLevel() - 0.5f;
      int maxAll = 0;
      //
      //  The maximum number of species that can be housed grows by 1 for
      //  every X/2 days (where X is the wake-up period for the base- typically
      //  15 days or so.)  This is offset by the schedule-delay for each
      //  species, plus the repair of the structure.
      for (int n = 0; n < numSpecies; n++) {
        final float schedule = SPECIES_SCHEDULE.get(SPECIES[n]);
        int maxSpecies = Nums.max(0, (int) ((fillLevel + schedule) * 2));
        maxAll         += maxSpecies;
        speciesCaps[n] = (byte) maxSpecies;
      }
      //
      //  If the total allowance is higher than the maximum population for a
      //  given ruin, we discard the weakest species first-
      while (maxAll > popLimit) {
        for (int n = 0; n < numSpecies; n++) if (speciesCaps[n] > 0) {
          speciesCaps[n]--;
          maxAll--;
          break;
        }
      }
      //
      //  As a final touch, we outsource spawning to the base itself (as this
      //  ties in with larger tactical considerations.)
      AB.updateSpawning(this, 10);
    }
  }
  


  /**  Rendering and interface methods-
    */
  public SelectionPane configSelectPane(SelectionPane panel, HUD UI) {
    return VenuePane.configStandardPanel(this, panel, UI, null);
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    if (destroyed() || origin() == null) return;
    BaseUI.current().selection.renderCircleOnGround(rendering, this, hovered);
  }
}







