/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package code.game.wild ;
import code.game.actors.*;
import code.game.common.*;
import code.game.planet.*;
import code.game.tactical.*;
import code.util.*;



public class Quud extends Fauna {
  
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  public Quud() {
    super(Species.QUD) ;
  }
  
  
  public Quud(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  protected void initStats() {
    traits.initAtts(5, 2, 1) ;
    health.initStats(
      1,    //lifespan
      species.baseBulk , //bulk bonus
      species.baseSight, //sight range
      species.baseSpeed, //speed rate
      ActorHealth.ANIMAL_METABOLISM
    ) ;
    gear.setDamage(2) ;
    gear.setArmour(15) ;
  }
  
  
  public float radius() {
    return 0.33f ;
  }
  
  
  public float height() {
    return 0.33f ;
  }
  
  
  
  /**  Behaviour implementations.
    */
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (! isDoing("actionHunker", null)) gear.setArmour(15) ;
  }
  
  
  protected void onTileChange(Tile oldTile, Tile newTile) {
    super.onTileChange(oldTile, newTile) ;
    if (health.conscious()) {
      float eaten = 1f / World.STANDARD_DAY_LENGTH ;
      eaten *= newTile.habitat().moisture() / 100f ;
      health.takeSustenance(eaten, 1) ;
    }
  }
  

  protected void addChoices(Choice choice) {
    /*
    final Behaviour defence = nextDefence(null) ;
    if (defence != null) {
      if (! isDoing("actionHunker", null)) choice.add(defence) ;
      return ;
    }
    //*/
    super.addChoices(choice) ;
  }
  
  
  protected Behaviour nextDefence(Actor near) {
    if (near.isDoing(Combat.class, this)) {
      final Action hunker = new Action(
        this, this,
        this, "actionHunker",
        Action.FALL, "Hunkering Down"
      ) ;
      hunker.setProperties(Action.QUICK) ;
      hunker.setPriority(Action.CRITICAL) ;
      return hunker ;
    }
    return null ;
  }
  
  
  public boolean actionHunker(Quud actor, Quud doing) {
    if (actor != this || doing != this) I.complain("No outside access.") ;
    doing.gear.setArmour(25) ;
    return true ;
  }
  
  

  /**  Rendering and interface methods-
    */
  protected float moveAnimStride() { return 4.0f ; }
  protected float spriteScale() { return super.spriteScale() * 0.8f ; }
}






