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



public class Qudu extends Fauna {
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  final public static ModelAsset
    MODEL_NEST_QUUD = CutoutModel.fromImage(
      Qudu.class, LAIR_DIR+"nest_quud.png", 2.5f, 2
    );
  
  final public static Species SPECIES = new Species(
    Qudu.class,
    "Qudu",
    "Qudu are placid, slow-moving, vegetarian browsers that rely on their "+
    "dense, leathery hides and intractable grip on the ground to protect "+
    "themselves from most predators.",
    FILE_DIR+"QuduPortrait.png",
    MS3DModel.loadFrom(
      FILE_DIR, "Qudu.ms3d", Qudu.class,
      XML_FILE, "Qudu"
    ),
    Species.Type.BROWSER,
    1.50f, //bulk
    0.50f, //speed
    0.75f  //sight
  ) {
    final VenueProfile PROFILE = Nest.constructProfile(
      2, 2, this, MODEL_NEST_QUUD
    );
    public Actor sampleFor(Base base) { return init(new Qudu(base)); }
    public VenueProfile nestProfile() { return PROFILE; }
  };
  
  
  public Qudu(Base base) {
    super(SPECIES, base);
  }
  
  
  public Qudu(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  protected void initStats() {
    traits.initAtts(7, 3, 2);
    health.initStats(
      1,    //lifespan
      species.baseBulk , //bulk bonus
      species.baseSight, //sight range
      species.speedMult, //speed rate
      ActorHealth.ANIMAL_METABOLISM
    );
    gear.setBaseDamage(8);
    gear.setBaseArmour(15);
    
    traits.setLevel(DEFENSIVE, -1);
    traits.setLevel(FEARLESS , -2);
  }
  
  
  public float radius() {
    return 0.33f;
  }
  
  
  public float height() {
    return 0.33f * super.height();
  }
  
  
  
  /**  Behaviour implementations.
    */
  /*
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
  
  
  protected void putEmergencyResponse(Choice choice) {
    final Batch <Action> hunkering = new Batch <Action> ();
    
    for (int n = Stage.STANDARD_HOUR_LENGTH; n-- > 0;) {
      final Action hunker = new Action(
        this, this,
        this, "actionHunker",
        Action.FALL, "Hunkering Down"
      );
      hunker.setProperties(Action.QUICK | Action.NO_LOOP);
      hunkering.add(hunker);
    }
    
    final Steps steps = new Steps(
      this, this,
      Action.PARAMOUNT + (senses.fearLevel() * Plan.ROUTINE),
      Plan.MOTIVE_EMERGENCY, Plan.NO_HARM,
      "Hunkering",
      hunkering.toArray(Action.class)
    );
    choice.add(steps);
  }
  
  
  public boolean actionHunker(Qudu actor, Qudu doing) {
    if (actor != this || doing != this) I.complain("No outside access.");
    doing.gear.setBaseArmour(25);
    return true;
  }
  //*/
  
  

  /**  Rendering and interface methods-
    */
  protected float moveAnimStride() { return super.moveAnimStride() * 0.8f; }
  protected float spriteScale() { return super.spriteScale() * 0.8f; }
}



