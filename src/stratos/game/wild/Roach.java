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

public class Roach extends Vermin {
  
  
  final public static Species SPECIES = new Species(
    Roach.class,
    "Roach",
    "Roaches are typically shy, retiring creatures, but have been known to "+
    "spread disease and steal rations.",
    null,
    MS3DModel.loadFrom(
      FILE_DIR, "GiantRoach.ms3d", Roach.class,
      XML_FILE, "GiantRoach"
    ),
    Species.Type.VERMIN,
    0.8f, 2.0f, 1.1f
  ) {
    public Actor sampleFor(Base base) { return init(new Roach(base)); }
  };
  
  
  public Roach(Base base) {
    super(SPECIES, base);
  }
  
  
  protected void initStats() {
    traits.initAtts(8, 10, 3);
    health.initStats(
      1,                 //lifespan
      species.baseBulk , //bulk bonus
      species.baseSight, //sight range
      species.speedMult, //speed rate
      ActorHealth.ANIMAL_METABOLISM
    );
    gear.setBaseDamage(5);
    gear.setBaseArmour(8);
    
    traits.setLevel(DEFENSIVE, -1);
    traits.setLevel(FEARLESS , -2);
    traits.setLevel(STEALTH_AND_COVER, 5 + Rand.index(5) - 3);
  }
  
  
  public Roach(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected float moveAnimStride() {
    //  TODO:  Base this off the default duration for the 'move' animation!
    return 2.5f;
  }
}













