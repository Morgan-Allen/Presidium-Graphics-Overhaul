/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;

import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.building.Economy.*;



public class EcologistStation extends Venue {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final static String IMG_DIR = "media/Buildings/ecologist/";
  final static ImageAsset ICON = ImageAsset.fromImage(
    "media/GUI/Buttons/nursery_button.gif", EcologistStation.class
  );
  final static ModelAsset
    STATION_MODEL = CutoutModel.fromImage(
      EcologistStation.class, IMG_DIR+"botanical_station.png", 4, 3
    );
  
  
  final List <Plantation> allotments = new List <Plantation> ();
  //private List <Plantation> nurseries = new List <Plantation> ();
  //private List <Crop[]> allotments = new List <Crop[]> ();
  
  
  
  public EcologistStation(Base belongs) {
    super(4, 3, Venue.ENTRANCE_SOUTH, belongs);
    structure.setupStats(
      150, 3, 250,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    );
    personnel.setShiftType(SHIFTS_BY_DAY);
    attachSprite(STATION_MODEL.makeSprite());
  }
  
  
  public EcologistStation(Session s) throws Exception {
    super(s);
    s.loadObjects(allotments);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObjects(allotments);
  }
  
  
  
  /**  Handling upgrades and economic functions-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    EcologistStation.class, "botanical_upgrades"
  );
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES; }
  final public static Upgrade
    CEREAL_LAB = new Upgrade(
      "Cereal Lab",
      "Improves cereal yields.  Cereals yield more calories than other crop "+
      "species, but lack the full range of nutrients required in a healthy "+
      "diet.",
      100,
      CARBS, 1,
      null, ALL_UPGRADES
    ),
    BROADLEAF_LAB = new Upgrade(
      "Broadleaf Lab",
      "Improves broadleaf yields.  Broadleaves provide a wider range of "+
      "nutrients, and are valued as luxury exports, but their yield is small.",
      150,
      GREENS, 1,
      null, ALL_UPGRADES
    ),
    FIELD_HAND_STATION = new Upgrade(
      "Field Hand Station",
      "Hire additional field hands to plant and reap the harvest more "+
      "quickly, maintain equipment, and bring land under cultivation.",
      50,
      Backgrounds.CULTIVATOR, 1,
      null, ALL_UPGRADES
    ),
    TREE_FARMING = new Upgrade(
      "Tree Farming",
      "Forestry programs assist in terraforming efforts and climate "+
      "moderation, as well as providing carbons for plastic production.",
      100,
      null, 1,
      BROADLEAF_LAB, ALL_UPGRADES
    ),
    INSECTRY_LAB = new Upgrade(
      "Insectry Lab",
      "Many plantations cultivate colonies of social insects or other "+
      "invertebrates, both as a source of protein and pollination, pest "+
      "control, or recycling services.",
      150,
      PROTEIN, 1,
      BROADLEAF_LAB, ALL_UPGRADES
    ),
    ECOLOGIST_STATION = new Upgrade(
      "Ecologist Station",
      "Ecologists are highly-skilled students of plants, animals and gene "+
      "modification, capable of adapting species to local climate conditions.",
      150,
      Backgrounds.ECOLOGIST, 1,
      TREE_FARMING, ALL_UPGRADES
    );
  
  
  public Behaviour jobFor(Actor actor) {
    if (! structure.intact()) return null;
    final Choice choice = new Choice(actor);
    
    //  If you're really short on food, consider foraging in the surrounds-
    final float shortages = (
      stocks.shortagePenalty(CARBS) +
      stocks.shortagePenalty(GREENS)
    ) / 2f;
    if (shortages > 0) {
      final Foraging foraging = new Foraging(actor, this);
      foraging.setMotive(Plan.MOTIVE_EMERGENCY, Plan.PARAMOUNT * shortages);
      choice.add(foraging);
    }
    
    //  If the harvest is really coming in, pitch in regardless-
    if (! Planet.isNight(world)) for (Plantation p : allotments) {
      if (p.needForTending() > 0.5f) choice.add(new Farming(actor, p));
    }
    if (choice.size() > 0) return choice.pickMostUrgent();
    
    //  Otherwise, perform deliveries and more casual work-
    if (! personnel.onShift(actor)) return null;
    final Delivery d = DeliveryUtils.bestBulkDeliveryFrom(
      this, services(), 2, 10, 5
    );
    /*
    final Delivery d = Deliveries.nextDeliveryFor(
      actor, this, services(), 10, world
    );
    //*/
    choice.add(d);
    
    //  Forestry may have to be performed, depending on need for gene samples-
    final boolean needsSeed = stocks.amountOf(GENE_SEED) < 5;
    final Forestry f = new Forestry(actor, this);
    if (needsSeed && actor.vocation() == Backgrounds.ECOLOGIST) {
      f.setMotive(Plan.MOTIVE_DUTY, Plan.ROUTINE);
      f.configureFor(Forestry.STAGE_SAMPLING);
    }
    else {
      f.setMotive(Plan.MOTIVE_DUTY, Plan.ROUTINE);
      f.configureFor(Forestry.STAGE_GET_SEED);
    }
    choice.add(f);
    
    //  And lower-priority tending and upkeep also gets an appearance-
    for (Plantation p : allotments) if (p.type == Plantation.TYPE_NURSERY) {
      for (Species s : Crop.ALL_VARIETIES) {
        if (actor.vocation() == Backgrounds.ECOLOGIST) {
          final SeedTailoring t = new SeedTailoring(actor, p, s);
          if (personnel.assignedTo(t) > 0) continue;
          choice.add(t);
        }
      }
      //  TODO:  Do this for every crop type?
      choice.add(new Farming(actor, p));
    }
    return choice.weightedPick();
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates);
    if (! structure.intact()) return;
    updateAllotments(numUpdates);
    //
    //  Increment demand for gene seed, and decay current stocks-
    stocks.incDemand(GENE_SEED, 5, Stocks.TIER_CONSUMER, 1, this);
    final float decay = 0.1f / World.STANDARD_DAY_LENGTH;
    for (Item seed : stocks.matches(GENE_SEED)) {
      stocks.removeItem(Item.withAmount(seed, decay));
    }
    for (Item seed : stocks.matches(SAMPLES)) {
      stocks.removeItem(Item.withAmount(seed, decay));
    }
    structure.setAmbienceVal(2);
  }
  
  
  public void onDestruction() {
    super.onDestruction();
  }
  
  
  protected void updateAllotments(int numUpdates) {
    //
    //  Then update the current set of allotments-
    if (numUpdates % 10 == 0) {
      //final int STRIP_SIZE = 4;
      //int numCovered = 0;
      //
      //  First of all, remove any missing allotments (and their siblings in
      //  the same strip.)
      for (Plantation p : allotments) {
        if (p.destroyed()) allotments.remove(p);
      }
      //
      //  Then, calculate how many allotments one should have.
      int maxAllots = 1 + personnel.numHired(Backgrounds.CULTIVATOR);
      if (maxAllots > allotments.size()) {
        //
        //  If you have too few, try to find a place for more-
        //final boolean covered = numCovered <= allotments.size() / 3;
        Plantation allots = Plantation.placeAllotmentFor(this);
        if (allots != null) allotments.add(allots);
      }
      if (maxAllots + 1 < allotments.size()) {
        //
        //  And if you have too many, flag the least productive for salvage.
        float minRating = Float.POSITIVE_INFINITY;
        Plantation toRemove = null;
        for (Plantation p : allotments) {
          final float rating = Plantation.rateAllotment(p, world);
          if (rating < minRating) { toRemove = p; minRating = rating; }
        }
        if (toRemove != null) {
          toRemove.structure.setState(Structure.STATE_SALVAGE, -1);
        }
      }
    }
  }
  

  public int numOpenings(Background v) {
    int num = super.numOpenings(v);
    if (v == Backgrounds.CULTIVATOR) return num + 2;
    if (v == Backgrounds.ECOLOGIST ) return num + 1;
    return 0;
  }
  
  
  protected List <Plantation> allotments() {
    return allotments;
  }
  
  
  public TradeType[] services() {
    return new TradeType[] { GREENS, PROTEIN, CARBS };
  }
  
  
  public Background[] careers() {
    return new Background[] { Backgrounds.ECOLOGIST, Backgrounds.CULTIVATOR };
  }
  
  
  
  /**  Rendering and interface methods-
    */
  final static float GOOD_DISPLAY_OFFSETS[] = {
    0.0f, 1.0f,
    0.0f, 0.0f,
    0.5f, 0.0f,
    1.0f, 0.0f,
  };
  
  
  protected float[] goodDisplayOffsets() {
    return GOOD_DISPLAY_OFFSETS;
  }
  
  
  protected TradeType[] goodsToShow() {
    return new TradeType[] { GENE_SEED, CARBS, GREENS, PROTEIN };
  }
  
  
  protected float goodDisplayAmount(TradeType good) {
    if (good == GENE_SEED) return stocks.amountOf(good) > 0 ? 5 : 0;
    return super.goodDisplayAmount(good);
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "botanical_station");
  }
  
  
  public String fullName() { return "Ecologist Station"; }
  
  
  public String helpInfo() {
    return
      "Ecologist Stations are responsible for agriculture and forestry, "+
      "helping to secure food supplies and advance terraforming efforts.";
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_ECOLOGIST;
  }
}







