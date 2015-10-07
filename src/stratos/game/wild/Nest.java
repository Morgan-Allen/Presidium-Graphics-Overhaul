/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.base.BaseDemands;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.SiteUtils;
import stratos.game.maps.Siting;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



//  TODO:  Use the FindHome behaviour here?


public class Nest extends Venue {
  
  /**  Fields, constructors, and save/load methods-
    */
  final Species species;
  private float crowdRatingCache = 1;
  
  
  /**  More typical construction and save/load methods-
    */
  protected Nest(
    Blueprint blueprint, Base base,
    Species species, ModelAsset lairModel
  ) {
    super(blueprint, base);
    this.species = species;
    attachSprite(lairModel.makeSprite());
  }
  
  
  public Nest(Session s) throws Exception {
    super(s);
    species = (Species) s.loadObject();
    crowdRatingCache = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(species);
    s.saveFloat(crowdRatingCache);
  }
  
  
  
  /**  Overrides for standard venue methods-
    */
  public Box2D areaClaimed() {
    return new Box2D(footprint()).expandBy(Fauna.PREDATOR_SEPARATION);
  }
  
  
  public boolean preventsClaimBy(Venue other) {
    final float distance = Spacing.distance(this, other);
    if (other instanceof Nest) {
      return distance <= Fauna.DEFAULT_FORAGE_DIST / 2;
    }
    else return distance <= Fauna.PREDATOR_SEPARATION;
  }
  

  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);

    final int INTERVAL = 10;
    if (numUpdates % INTERVAL == 0 && ! instant) {
      final float inc = INTERVAL * 1f / Fauna.DEFAULT_BREED_INTERVAL;
      crowdRatingCache *= (1 - inc);
      final float crowding = NestUtils.crowding(species, this, world);
      crowdRatingCache += inc * crowding;
    }
  }
  
  
  public float crowdRating(Species s) {
    if (s == species) return crowdRatingCache;
    else return 1;
  }
  
  
  //  Only allow entry to the same species.
  public boolean allowsEntry(Mobile m) {
    if (! structure.intact()) return false;
    return (m instanceof Actor) && ((Actor) m).species() == species;
  }
  
  
  //  Nests have no road connections.
  protected void updatePaving(boolean inWorld) {}
  
  
  
  /**  Rendering and interface methods-
    */
  public SelectionPane configSelectPane(SelectionPane panel, BaseUI UI) {
    panel = VenuePane.configSimplePanel(this, panel, UI, null, null);
    //*
    final Description d = panel.detail(), l = panel.listing();
    l.append("\n  Crowding: "+I.shorten(crowdRatingCache, 2));
    //*/
    return panel;
  }
  

  public void renderSelection(Rendering rendering, boolean hovered) {
    if (destroyed() || origin() == null) return;
    BaseUI.current().selection.renderCircleOnGround(rendering, this, hovered);
  }
}



