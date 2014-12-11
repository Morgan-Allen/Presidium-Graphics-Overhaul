/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.game.actors.*;
import stratos.game.wild.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.economic.Economy.*;



//  TODO:  Demand reagents to perform gene tailoring?

public class EcologistStation extends Venue {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final static String IMG_DIR = "media/Buildings/ecologist/";
  final static ImageAsset ICON = ImageAsset.fromImage(
    EcologistStation.class, "media/GUI/Buttons/nursery_button.gif"
  );
  final static ModelAsset
    STATION_MODEL = CutoutModel.fromImage(
      EcologistStation.class, IMG_DIR+"botanical_station.png", 4, 3
    );
  
  final static int EXTRA_CLAIM_SIZE = 4;
  
  
  
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
    //s.loadObjects(allotments);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    //s.saveObjects(allotments);
  }
  
  
  protected Box2D areaClaimed() {
    return new Box2D().setTo(footprint()).expandBy(EXTRA_CLAIM_SIZE);
  }
  
  
  public boolean preventsClaimBy(Venue other) {
    if (other.privateProperty()) return false;
    return super.preventsClaimBy(other);
  }
  
  
  
  /**  Handling upgrades and economic functions-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
  );
  
  //  Tree Farming.  Durwheat.  Oni Rice.  Tuber Lily.  Hive Grubs.
  
  //  TODO:  Re-evaluate these.  Name actual species.  Make it exciting!  And
  //  allow for direct venue upgrades.
  
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES; }
  final public static Upgrade
    CEREAL_LAB = new Upgrade(
      "Cereal Lab",
      "Improves cereal yields.  Cereals yield more calories than other crop "+
      "species, but lack the full range of nutrients required in a healthy "+
      "diet.",
      100,
      CARBS, 1,
      null,
      EcologistStation.class, ALL_UPGRADES
    ),
    BROADLEAF_LAB = new Upgrade(
      "Broadleaf Lab",
      "Improves broadleaf yields.  Broadleaves provide a wider range of "+
      "nutrients, and are valued as luxury exports, but their yield is small.",
      150,
      GREENS, 1,
      null,
      EcologistStation.class, ALL_UPGRADES
    ),
    FIELD_HAND_STATION = new Upgrade(
      "Field Hand Station",
      "Hire additional field hands to plant and reap the harvest more "+
      "quickly, maintain equipment, and bring land under cultivation.",
      50,
      Backgrounds.CULTIVATOR, 1,
      null,
      EcologistStation.class, ALL_UPGRADES
    ),
    TREE_FARMING = new Upgrade(
      "Tree Farming",
      "Forestry programs assist in terraforming efforts and climate "+
      "moderation, as well as providing carbons for plastic production.",
      100,
      null, 1,
      BROADLEAF_LAB,
      EcologistStation.class, ALL_UPGRADES
    ),
    INSECTRY_LAB = new Upgrade(
      "Insectry Lab",
      "Many plantations cultivate colonies of social insects or other "+
      "invertebrates, both as a source of protein and pollination, pest "+
      "control, or recycling services.",
      150,
      PROTEIN, 1,
      BROADLEAF_LAB,
      EcologistStation.class, ALL_UPGRADES
    ),
    ECOLOGIST_STATION = new Upgrade(
      "Ecologist Station",
      "Ecologists are highly-skilled students of plants, animals and gene "+
      "modification, capable of adapting species to local climate conditions.",
      150,
      Backgrounds.ECOLOGIST, 1,
      TREE_FARMING,
      EcologistStation.class, ALL_UPGRADES
    );
  
  
  public Behaviour jobFor(Actor actor) {
    if (! structure.intact()) return null;
    final Choice choice = new Choice(actor);
    
    //  Forestry may have to be performed, depending on need for gene samples-
    final boolean needsSeed = stocks.amountOf(GENE_SEED) < 5;
    if (needsSeed) choice.add(Forestry.nextSampling(actor, this));
    else choice.add(Forestry.nextPlanting(actor, this));
    
    //  Tailor seed varieties
    for (Species s : Crop.ALL_VARIETIES) {
      final SeedTailoring t = new SeedTailoring(actor, this, s);
      if (personnel.assignedTo(t) > 0) continue;
      choice.add(t);
    }
    
    final Exploring x = Exploring.nextExploration(actor);
    if (x != null) choice.add(x.setMotive(Plan.MOTIVE_DUTY, Plan.ROUTINE));
    
    for (Target e : actor.senses.awareOf()) if (e instanceof Fauna) {
      choice.add(Hunting.asSample(actor, (Fauna) e, this));
    }
    
    if (choice.empty()) choice.add(new Supervision(actor, this));
    return choice.weightedPick();
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates);
    if (! structure.intact()) return;
    //
    //  Increment demand for gene seed, and decay current stocks-
    stocks.incDemand(GENE_SEED, 5, TIER_CONSUMER, 1, this);
    final float decay = 0.1f / Stage.STANDARD_DAY_LENGTH;
    
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
  
  
  public int numOpenings(Background v) {
    int num = super.numOpenings(v);
    if (v == ECOLOGIST) return num + 1;
    return 0;
  }
  
  
  public Traded[] services() {
    return new Traded[] { GENE_SEED };
  }
  
  
  public Background[] careers() {
    return new Background[] { ECOLOGIST };
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
  
  
  protected Traded[] goodsToShow() {
    return new Traded[] { GENE_SEED, CARBS, GREENS, PROTEIN };
  }
  
  
  protected float goodDisplayAmount(Traded good) {
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
  
  
  public String objectCategory() {
    return InstallTab.TYPE_ECOLOGIST;
  }
}



