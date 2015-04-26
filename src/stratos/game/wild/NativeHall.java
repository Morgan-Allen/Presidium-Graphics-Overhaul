

package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.civic.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Backgrounds.*;



//  Note:  I'm going to focus primarily on establishing initial native sites,
//  rather than maintaining them, since human reproduction is so slow and
//  external migration isn't yet implemented.


//  TODO:  All of this crap is gonna be replaced with a more cohesive supply-
//  and-demand system- the ecology system, the stocks-demand-diffusion, the
//  native-huts placement- everything.


public class NativeHall extends NativeHut implements Performance.Theatre {
  
  
  final List <NativeHut> children = new List <NativeHut> ();
  
  
  protected NativeHall(int tribeID, Base base) {
    super(VENUE_BLUEPRINTS[tribeID][1], TYPE_HALL, tribeID, base);
  }


  public NativeHall(Session s) throws Exception {
    super(s);
    s.loadObjects(children);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObjects(children);
  }
  
  
  
  
  /**  Updates and behavioural functions-
    */
  final static String CHANT_NAMES[] = {
    "Tribal Chant"
  };
  
  
  public Background[] careers() {
    return super.careers();
    //  Cargo Cultist, Marked One, Medicine Man, Chieftain.
  }
  
  
  public String[] namesForPerformance(int type) {
    if (type != Performance.TYPE_SONG) return null;
    return CHANT_NAMES;
  }
  
  
  public Behaviour jobFor(Actor actor, boolean onShift) {
    final Behaviour job = super.jobFor(actor, onShift);
    if (job != null) return job;
    
    if (staff.shiftFor(actor) == SECONDARY_SHIFT) {
      final Performance chant = new Performance(
        actor, this, Performance.TYPE_SONG, null, 0
      );
      return chant;
    }
    return null;
  }
  
  
  public void addServices(Choice choice, Actor actor) {
    choice.add(new Recreation(actor, this, Performance.TYPE_SONG, 0));
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    
    /*
    if (numUpdates % 10 == 0) {
      updatePopEstimate(world);
    }
    //*/
    for (NativeHut hut : children) {
      if (hut.staff.unoccupied()) {
        hut.structure.setState(Structure.STATE_SALVAGE,  -1);
      }
      if (hut.destroyed()) children.remove(hut);
    }
  }
  
  /*
  protected void updatePopEstimate(Stage world) {
    //
    //  TODO:  Have the population-estimate routines for Nests take an argument
    //  to specify search range/minimum separation?
    float estimate = Nest.idealPopulation(this, Species.HUMAN, world, false);
    if (idealPopEstimate == -1) idealPopEstimate = estimate;
    else {
      final float inc = 10f / Stage.STANDARD_DAY_LENGTH;
      idealPopEstimate *= 1 - inc;
      idealPopEstimate += estimate * inc;
    }
  }
  
  
  protected int idealNumHuts() {
    return (int) (idealPopEstimate / HUT_OCCUPANCY);
  }
  //*/
  
  
  
  /**  Rendering and interface-
    */
  public Composite portrait(BaseUI UI) {
    return super.portrait(UI);
  }
  
  
  public String helpInfo() {
    return
      "Native settlements will often have a central meeting place where "+
      "the tribe's leadership and elders will gather to make decisions.";
  }
}


