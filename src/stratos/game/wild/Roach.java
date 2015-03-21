/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.util.*;
import stratos.user.*;
import static stratos.game.actors.Qualities.*;



public class Roach extends Vermin {
  
  
  final static Species SPECIES = new Species(
    Roach.class,
    "Roach",
    "Roaches are typically shy, retiring creatures, but have been known to "+
    "spread disease and steal rations.",
    null,
    null,
    Species.Type.VERMIN, 1.5f, 2.0f, 1.1f
  ) {
    public Actor sampleFor(Base base) { return new Roach(base); }
  };
  
  
  public Roach(Base base) {
    super(SPECIES, base);
  }
  
  
  protected void initStats() {
    traits.initAtts(6, 5, 2);
    health.initStats(
      1,                 //lifespan
      species.baseBulk , //bulk bonus
      species.baseSight, //sight range
      species.baseSpeed, //speed rate
      ActorHealth.ANIMAL_METABOLISM
    );
    gear.setBaseDamage(5);
    gear.setBaseArmour(8);
    
    traits.setLevel(DEFENSIVE, -1);
    traits.setLevel(FEARLESS , -2);
  }
  
  
  public Roach(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
}






