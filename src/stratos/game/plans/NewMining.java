/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.game.wild.Habitat;
import stratos.game.actors.*;
import stratos.game.economic.*;
import stratos.util.Rand;
import stratos.content.civic.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



public class NewMining extends ResourceTending {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final public static int
    TYPE_MINING  = 0,
    TYPE_FORMING = 1,
    TYPE_DUMPING = 2,
    TYPE_BORING  = 3;
  
  final public static float
    MAX_SAMPLE_STORE      = 50,
    DEFAULT_TILE_DIG_TIME = Stage.STANDARD_HOUR_LENGTH,
    HARVEST_MULT          = 1.0f,
    SLAG_RATIO            = 2.5f;
  
  final static Trait
    MINE_TRAITS[] = { PATIENT, METICULOUS };

  final public static Traded
    MINED_TYPES[] = { FOSSILS, POLYMER, METALS, FUEL_RODS };
  
  
  final int type;
  
  
  private NewMining(
    Actor actor, HarvestVenue depot, int type, Traded extracts[]
  ) {
    super(actor, depot, extracts);
    this.type = type;
  }
  
  
  public NewMining(Session s) throws Exception {
    super(s);
    this.type = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt(type);
  }
  
  
  public Plan copyFor(Actor other) {
    return new NewMining(actor, (HarvestVenue) depot, type, harvestTypes);
  }
  
  
  public static NewMining asMining(Actor actor, ExcavationSite site) {
    final NewMining mining = new NewMining(
      actor, site, TYPE_MINING, MINED_TYPES
    );
    mining.coop = true;
    return mining;
  }
  
  
  
  
  
  protected Trait[] enjoyTraits() {
    return MINE_TRAITS;
  }
  
  
  protected Conversion tendProcess() {
    return ExcavationSite.LAND_TO_METALS;
  }
  
  
  
  protected float rateTarget(Target t) {
    //
    //  TODO:  Include surface rocks!
    
    final Tile at = (Tile) t;
    final StageTerrain terrain = at.world.terrain();
    
    if (type == TYPE_MINING) {
      return terrain.mineralsAt(at) > 0 ? 1 : 0;
    }
    if (type == TYPE_FORMING) {
      
    }
    if (type == TYPE_DUMPING) {
      
    }
    if (type == TYPE_BORING) {
      
    }
    return 0;
  }
  
  

  public boolean actionCollectTools(Actor actor, Venue depot) {
    if (! super.actionCollectTools(actor, depot)) return false;
    
    //
    //  TODO:  You may have to collect and dispose of slag here too.

    if (type == TYPE_MINING) {
      
    }
    if (type == TYPE_FORMING) {
      
    }
    if (type == TYPE_DUMPING) {
      
    }
    if (type == TYPE_BORING) {
      
    }
    return true;
  }
  
  
  protected Item[] afterHarvest(Target t) {
    
    ///if (Rand.num() > 1f / DEFAULT_TILE_DIG_TIME) return null;
    
    final Tile at = (Tile) t;
    final StageTerrain terrain = at.world.terrain();
    
    //  Remember the sequence?  First barrens, then strip-mining.  Then the
    //  strip-fx once minerals are exhausted.
    
    //
    //  TODO:  USE THE UTILITY METHODS IN OUTCROP FOR THIS!
    //  final Item ore = Outcrop.mineralsAt(at);
    
    if (type == TYPE_MINING) {
      terrain.setHabitat(at, Habitat.STRIP_MINING);
      at.clearUnlessOwned();
      at.refreshAdjacent();
      
      final int amount = (int) terrain.mineralsAt(at);
      final byte typeID = terrain.mineralType(at);
      terrain.setMinerals(at, typeID, amount - 1);
      
      return new Item[] {
        Item.withAmount(MINED_TYPES[typeID], HARVEST_MULT),
        Item.withAmount(SLAG               , SLAG_RATIO  )
      };
    }
    if (type == TYPE_FORMING) {
      
    }
    if (type == TYPE_DUMPING) {
      
    }
    if (type == TYPE_BORING) {
      
    }
    return null;
  }
  
  
  protected void afterDepotDisposal() {
    if (type == TYPE_MINING) {
      actor.gear.transfer(SLAG, depot);
    }
    if (type == TYPE_FORMING) {
      
    }
    if (type == TYPE_DUMPING) {
      
    }
    if (type == TYPE_BORING) {
      
    }
  }
}












