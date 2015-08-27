/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.content.civic.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.*;
import stratos.game.wild.Flora;
import stratos.game.wild.Species;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



public class Farming extends ResourceTending {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final static Trait BASE_TRAITS[] = { ENERGETIC, NATURALIST };
  
  Crop toPlant = null;
  
  
  public Farming(Actor actor, Nursery depot) {
    super(actor, depot, CARBS, GREENS, PROTEIN);
  }
  
  
  public Farming(Session s) throws Exception {
    super(s);
    this.toPlant = (Crop) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(toPlant);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Farming(other, (Nursery) depot);
  }
  
  
  
  /**  Priority and step evaluation-
    */
  protected Behaviour getNextStep() {
    final Behaviour step = super.getNextStep();
    //
    //  In the event that we've decided to plant a fresh tile, determine what
    //  species it should be...
    if (stage() == STAGE_TEND && (toPlant = cropAt(tended())) == null) {
      final Tile    t = (Tile) tended();
      final Nursery n = (Nursery) depot;
      final Species s = pickSpecies(t);
      if (s == null || ! n.couldPlant(t)) return null;
      toPlant = new Crop(n, s);
      toPlant.setPosition(t.x, t.y, t.world);
    }
    return step;
  }
  
  
  private Species pickSpecies(Tile t) {
    final Pick <Species> pick = new Pick <Species> ();
    
    for (Species s : Crop.ALL_VARIETIES) {
      final Item seed = actor.gear.bestSample(GENE_SEED, s, 1);
      final float shortage = depot.stocks.relativeShortage(Crop.yieldType(s));
      
      float chance = Crop.habitatBonus(t, s, seed) * (1 + shortage);
      chance *= (1 + Crop.yieldMultiple(s)) / 2;
      pick.compare(s, chance + Rand.num());
    }
    return pick.result();
  }
  
  
  private Crop cropAt(Target t) {
    final Element e = ((Tile) t).above();
    return (e instanceof Crop) ? (Crop) e : null;
  }
  
  
  protected float rateTarget(Target t) {
    final Crop c = cropAt(t);
    if (c == null       ) return 1.0f;
    if (c.needsTending()) return 0.5f;
    else                  return 0.0f;
  }
  
  
  protected Trait[] enjoyTraits() {
    return BASE_TRAITS;
  }
  
  
  protected Conversion tendProcess() {
    return Nursery.LAND_TO_CARBS;
  }
  
  
  
  /**  Action-implementations-
    */
  protected Item[] afterHarvest(Target t) {
    final Crop c = cropAt(t);
    if (c == null) {
      seedTile((Tile) t);
      return null;
    }
    else if (c.blighted()) {
      c.disinfest();
      return null;
    }
    else {
      c.setAsDestroyed();
      return new Item[] { c.yieldCrop() };
    }
  }
  
  
  private void seedTile(Tile t) {
    if (toPlant == null) return;
    final Species s = toPlant.species();
    //
    //  TODO:  Just base seed quality off upgrades at the source depot!
    //
    //  Assuming that's possible, we then determine the health of the seedling
    //  based on stock quality and planting skill-
    final Item seed = actor.gear.bestSample(Item.asMatch(GENE_SEED, s), 0.1f);
    float health = 0;
    if (seed != null) health += seed.quality * 1f / Item.MAX_QUALITY;
    health += tendProcess().performTest(actor, 0, 1);
    //
    //  Then put the thing in the dirt-
    if (t.above() != null) t.above().setAsDestroyed();
    toPlant.enterWorldAt(t.x, t.y, t.world, true);
    toPlant.seedWith(s, health);
  }
  
  
  public boolean actionCollectTools(Actor actor, Venue depot) {
    if (! super.actionCollectTools(actor, depot)) return false;
    
    for (Species s : Crop.ALL_VARIETIES) {
      final Item seed = depot.stocks.bestSample(GENE_SEED, s, 1);
      if (seed == null || actor.gear.amountOf(seed) > 0) continue;
      actor.gear.addItem(seed);
    }
    return true;
  }
  
  
  protected void afterDepotDisposal() {
    actor.gear.removeAllMatches(GENE_SEED);
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (stage() == STAGE_TEND && toPlant != null) {
      final Crop c = cropAt(tended());
      if      (c == null   ) d.append("Planting "  );
      else if (c.blighted()) d.append("Weeding "   );
      else if (c.ripe    ()) d.append("Harvesting ");
      d.append(toPlant.species());
    }
    else super.describeBehaviour(d); 
  }
}








