/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.wip;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.game.wild.*;
import stratos.game.actors.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;

import static stratos.game.actors.Backgrounds.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.craft.Economy.*;

import stratos.content.civic.BotanicalStation;
import stratos.content.civic.EngineerStation;



public class FormerBay extends HarvestVenue implements TileConstants {
  
  
  /**  Constructors, data fields, setup and save/load methods-
    */
  protected static boolean verbose = false;
  
  final static String IMG_DIR = "media/Buildings/ecologist/";
  final static ImageAsset ICON = ImageAsset.fromImage(
    FormerBay.class, "former_bay_icon",
    "media/GUI/Buttons/former_plant_button.gif"
  );
  final static ModelAsset
    MODEL = CutoutModel.fromImage(
      FormerBay.class, "former_bay_model",
      IMG_DIR+"former_plant.png", 3, 1
    );
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    FormerBay.class, "former_bay",
    "Former Bay", Target.TYPE_WIP, ICON,
    "The Former Bay extracts light minerals from the soil and atmosphere "+
    "while speeding terraforming programs.",
    4, 1, Structure.IS_NORMAL | Structure.IS_ZONED,
    Owner.TIER_FACILITY, 25, 5,
    POLYMER//, FORMER_ENGINEER
  );
  
  
  public FormerBay(Base base) {
    super(BLUEPRINT, base, 4, 12);
    staff.setShiftType(SHIFTS_BY_DAY);
    attachModel(MODEL);
  }
  
  
  public FormerBay(Session s) throws Exception {
    super(s);
    //areaClaimed.loadFrom(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    //areaClaimed.saveTo(s.output());
  }
  
  
  
  /**  Economic functions-
    */
  //  TODO:  Integrate these upgrades to improve efficiency here.
  //  TODO:  Include options for either desertification or 'greening'!
  
  final public static Upgrade
    LEVELS[] = BLUEPRINT.createVenueLevels(
      Upgrade.SINGLE_LEVEL,
      new Upgrade[] { BotanicalStation.LEVELS[0], EngineerStation.LEVELS[0] },
      new Object[] { 15, ASSEMBLY, 15, CHEMISTRY },
      450//, 650
    );
  
  /*
  final public static Upgrade
    
    AIR_PROCESSING = new Upgrade(
      "Carbons Cycling",
      "Improves output of life support, speeds terraforming and reduces "+
      "pollution.",
      200,
      Upgrade.THREE_LEVELS, null, 1, 
      null, FormerPlant.class
    ),
    RESINS_PRESS = null,
    
    BIOMASS_REACTOR = null,
    
    SPICE_REDUCTION = new Upgrade(
      "Spice Reduction",
      "Employs microbial culture to capture minute quantities of spice from "+
      "the surrounding environment.",
      300,
      Upgrade.THREE_LEVELS, null, 1,
      BIOMASS_REACTOR, FormerPlant.class
    );
  //*/
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    structure.setAmbienceVal(Ambience.MILD_SQUALOR);
    stocks.setConsumption(POLYMER, 1);
    stocks.updateStockDemands(1, services());
  }
  
  
  protected Behaviour jobFor(Actor actor) {
    if (staff.offDuty(actor)) return null;
    
    final Bringing d = BringUtils.bestBulkDeliveryFrom(
      this, services(), 1, 5, 5, true
    );
    if (d != null) return d;
    final Choice choice = new Choice(actor);

    Venue source = (BotanicalStation) world.presences.nearestMatch(
      BotanicalStation.class, this, Stage.ZONE_SIZE
    );
    if (source == null) source = this;
    
    choice.add(Gathering.asForestPlanting(actor, source));
    choice.add(Gathering.asForestCutting (actor, this  ));
    
    return choice.weightedPick();
  }
  
  
  protected int numPositions(Background b) {
    final int level = structure.mainUpgradeLevel();
    //if (b == FORMER_ENGINEER) return level + 1;
    return 0;
  }
  
  
  
  /**  Division claims and tending methods.
    */
  protected ClaimDivision updateDivision() {
    return ClaimDivision.forEmptyArea(this, areaClaimed());
  }
  
  
  public float needForTending(ResourceTending tending) {
    float abundance = world.ecology().forestRating(world.tileAt(this));
    final float shortage = stocks.relativeShortage(POLYMER, true);
    
    final Gathering g = (Gathering) tending;
    if (g.type == Gathering.TYPE_FORESTING) {
      return 0.5f;
    }
    if (g.type == Gathering.TYPE_LOGGING) {
      return abundance + shortage - 0.5f;
    }
    return 0;
  }
  
  
  public Tile[] getHarvestTiles(ResourceTending tending) {
    final Gathering g = (Gathering) tending;
    if (g.type == Gathering.TYPE_FORESTING) {
      return Gathering.sampleSeedingPoints(this, Stage.ZONE_SIZE);
    }
    if (g.type == Gathering.TYPE_LOGGING) {
      return Gathering.sampleFloraPoints(this, Stage.ZONE_SIZE);
    }
    return new Tile[0];
  }
  
  
  protected void checkTendStates() {
    //  We override the needForTending method, so this isn't needed here.
  }
  
  
  public boolean needsTending(Tile t) {
    //  And again, individual tiles aren't evaluated for this purpose.
    return true;
  }
  
  
  public float harvestMultiple(Target tended, Object type) {
    return 1;
  }
  
  
  private Item[] estimateDailyOutput() {
    
    //  TODO:  Harvest and processing should take considerably longer.  Typical
    //  polymer-per-day is 10, and they're gathering that in maybe 4-8 hours?
    
    float sumP = 0;
    for (Tile t : world.tilesIn(areaClaimed(), true)) {
      final Flora f = Flora.foundAt(t);
      if (f != null) {
        final float yield = f.dailyYieldEstimate(t);
        for (Item i : f.materials()) if (i.type == POLYMER) {
          sumP += i.amount * yield;
        }
      }
    }
    return new Item[] { Item.withAmount(POLYMER, sumP) };
  }
  
  
  


  /**  Rendering and interface methods-
    */
  public SelectionPane configSelectPane(SelectionPane panel, HUD UI) {
    return VenuePane.configStandardPanel(this, panel, UI, null);
  }
  
  
  private String compileOutputReport() {
    final StringBuffer report = new StringBuffer();
    report.append(super.helpInfo());
    
    final Item out[] = estimateDailyOutput();
    for (Item i : out) {
      final String amount = I.shorten(i.amount, 1);
      report.append("\n  Estimated "+i.type+" per day: "+amount);
    }
    return report.toString();
  }
  
  
  public String helpInfo() {
    if (inWorld() && structure.intact()) return compileOutputReport();
    else return super.helpInfo();
  }
}



//  SAVED FOR LATER REFERENCE-
/*
  final public static Upgrade
    
    CARBONS_CYCLING = new Upgrade(
      "Carbons Cycling",
      "Improves output of life support, speeds terraforming and reduces "+
      "pollution.",
      200,
      Upgrade.THREE_LEVELS, null, 1, 
      null, FormerPlant.class
    ),
    
    EVAPORATION_CYCLING = new Upgrade(
      "Evaporation Cycling",
      "Increases efficiency around desert and oceans terrain, and increases "+
      "water output.",
      200,
      Upgrade.THREE_LEVELS, null, 1,
      null, FormerPlant.class
    ),
    
    DUST_PANNING = new Upgrade(
      "Dust Panning",
      "Permits modest output of metal ore and fuel cores, and installs "+
      "automated crawlers to gather soil samples.",
      150,
      Upgrade.THREE_LEVELS, null, 1,
      CARBONS_CYCLING, FormerPlant.class
    ),
    
    SPICE_REDUCTION = new Upgrade(
      "Spice Reduction",
      "Employs microbial cultures to capture minute quantities of spice from "+
      "the surrounding environment.",
      300,
      Upgrade.THREE_LEVELS, null, 1,
      EVAPORATION_CYCLING, FormerPlant.class
    );
//*/
