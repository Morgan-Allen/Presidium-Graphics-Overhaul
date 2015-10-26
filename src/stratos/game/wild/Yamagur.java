/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.graphics.common.ModelAsset;
import stratos.graphics.cutout.CutoutModel;
import stratos.graphics.solids.MS3DModel;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;



public class Yamagur extends Fauna {
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  final public static ModelAsset
    NEST_MODEL = CutoutModel.fromImage(
      Yamagur.class, LAIR_DIR+"nest_yamagur.png", 3.5f, 2
    );
  
  final public static Species SPECIES = new Species(
    Yamagur.class,
    "Yamagur",
    "Yamagur are immensely powerful quadrapedal browsers which build "+
    "surprisingly elaborate nest structures.  Though not actively aggressive, "+
    "they are often best left undisturbed.",
    FILE_DIR+"YamagurPortrait.png",
    MS3DModel.loadFrom(
      FILE_DIR, "yamagur.ms3d", Yamagur.class,
      XML_FILE, "Yamagur"
    ),
    Species.Type.BROWSER,
    5.00f, //bulk
    0.40f, //speed
    1.10f  //sight
  ) {
    final Blueprint BLUEPRINT = NestUtils.constructBlueprint(
      3, 2, this, NEST_MODEL
    );
    public Actor sampleFor(Base base) { return init(new Yamagur(base)); }
    public Blueprint nestBlueprint() { return BLUEPRINT; }
    public boolean preyedOn() { return false; }
  };
  
  
  public Yamagur(Base base) {
    super(SPECIES, base);
  }
  
  
  public Yamagur(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  protected void initStats() {
    traits.initAtts(25, 8, 7);
    health.initStats(
      25,    //lifespan
      species.baseBulk , //bulk bonus
      species.baseSight, //sight range
      species.speedMult, //speed rate
      ActorHealth.ANIMAL_METABOLISM
    );
    gear.setBaseDamage(20);
    gear.setBaseArmour(10);
    
    traits.setLevel(DEFENSIVE, 0);
    traits.setLevel(FEARLESS , 1);
    
    traits.setLevel(HAND_TO_HAND, 10 + Rand.index(5) - 2);
    traits.setLevel(HANDICRAFTS , 5  + Rand.index(5) - 2);
  }
  
  
  public float radius() {
    return 1.0f * health.ageMultiple();
  }
  
  
  public float height() {
    return 1.5f * super.height();
  }
  
  
  
  /**  Behaviour implementations.
    */
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! isDoingAction("actionHunker", null)) gear.setBaseArmour(15);
  }
  
  
  protected void onTileChange(Tile oldTile, Tile newTile) {
    super.onTileChange(oldTile, newTile);
    if (health.conscious()) {
      float eaten = 1f / Stage.STANDARD_DAY_LENGTH;
      eaten *= newTile.habitat().moisture() / 100f;
      health.takeCalories(eaten, 1);
    }
  }
  

  protected void addChoices(Choice choice) {
    super.addChoices(choice);
  }
  
  

  /**  Rendering and interface methods-
    */
  protected float moveAnimStride() {
    return super.moveAnimStride() * 0.8f;
  }
  
  
  protected float spriteScale() {
    return super.spriteScale();
  }
}








