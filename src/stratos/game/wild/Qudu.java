/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.util.*;

import static stratos.game.actors.Qualities.*;



public class Qudu extends Fauna {
  
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  public Qudu(Base base) {
    super(Species.QUDU, base);
  }
  
  
  public Qudu(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  protected void initStats() {
    traits.initAtts(5, 2, 1);
    health.initStats(
      1,    //lifespan
      species.baseBulk , //bulk bonus
      species.baseSight, //sight range
      species.baseSpeed, //speed rate
      ActorHealth.ANIMAL_METABOLISM
    );
    gear.setDamage(2);
    gear.setArmour(15);
    
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
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates);
    if (! isDoingAction("actionHunker", null)) gear.setArmour(15);
  }
  
  
  protected void onTileChange(Tile oldTile, Tile newTile) {
    super.onTileChange(oldTile, newTile);
    if (health.conscious()) {
      float eaten = 1f / World.STANDARD_DAY_LENGTH;
      eaten *= newTile.habitat().moisture() / 100f;
      health.takeCalories(eaten, 1);
    }
  }
  

  protected void addChoices(Choice choice) {
    super.addChoices(choice);
  }
  
  
  protected void putEmergencyResponse(Choice choice) {
    final Batch <Action> hunkering = new Batch <Action> ();
    
    for (int n = World.STANDARD_HOUR_LENGTH; n-- > 0;) {
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
      Action.PARAMOUNT + (senses.fearLevel() * Plan.ROUTINE), false,
      "Hunkering",
      hunkering.toArray(Action.class)
    );
    choice.add(steps);
  }
  
  
  public boolean actionHunker(Qudu actor, Qudu doing) {
    if (actor != this || doing != this) I.complain("No outside access.");
    doing.gear.setArmour(25);
    return true;
  }
  
  

  /**  Rendering and interface methods-
    */
  protected float moveAnimStride() { return super.moveAnimStride() * 0.8f; }
  protected float spriteScale() { return super.spriteScale() * 0.8f; }
}



