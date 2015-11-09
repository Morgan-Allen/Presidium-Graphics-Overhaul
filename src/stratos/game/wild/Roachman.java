/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.graphics.solids.MS3DModel;
import stratos.util.*;
import stratos.user.*;
import static stratos.game.actors.Qualities.*;



//  Use size variants- Roach, Giant Roach and Titan Roach.

public class Roachman extends Vermin {
  
  
  final public static Species SPECIES = new Species(
    Roachman.class,
    "Roachman",
    "Roachmen are larger, bipedal cousins of their insectile brethren, "+
    "rumoured to be the biproduct of a tragic medical accident.  They have "+
    "an odd fixation for shiny objects.",
    null,
    MS3DModel.loadFrom(
      FILE_DIR, "Roachman2.ms3d", Roachman.class,
      XML_FILE, "Roachman"
    ),
    Species.Type.VERMIN, 1.5f, 1.5f, 1.8f
  ) {
    public Actor sampleFor(Base base) { return init(new Roachman(base)); }
  };
  
  
  public Roachman(Base base) {
    super(SPECIES, base);
  }
  
  
  protected void initStats() {
    traits.initAtts(12, 15, 9);
    health.initStats(
      1,                 //lifespan
      species.baseBulk , //bulk bonus
      species.baseSight, //sight range
      species.speedMult, //speed rate
      ActorHealth.ANIMAL_METABOLISM
    );
    gear.setBaseDamage(8);
    gear.setBaseArmour(8);
    
    traits.setLevel(FEARLESS  , -1);
    traits.setLevel(ACQUISITIVE, 1);
    traits.setLevel(STEALTH_AND_COVER, 15 + Rand.index(5) - 3);
    traits.setLevel(HAND_TO_HAND     , 5  + Rand.index(5) - 3);
    
    skills.addTechnique(Fauna.MAUL        );
    skills.addTechnique(Fauna.NIGHT_VISION);
    skills.addTechnique(Fauna.INFECTION   );
  }
  
  
  public Roachman(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  public float radius() {
    return 0.33f;
  }
  
  
  /**  Rendering and interface methods-
    */
  protected float moveAnimStride() {
    //  TODO:  Base this off the default duration for the 'move' animation!
    return 2.0f;
  }
}













