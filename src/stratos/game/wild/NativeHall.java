

package stratos.game.wild;
import stratos.game.actors.*;
import static stratos.game.actors.Backgrounds.*;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.game.base.*;
import stratos.game.building.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



//  Note:  I'm going to focus primarily on establishing initial native sites,
//  rather than maintaining them, since human reproduction is so slow and
//  external migration isn't yet implemented.


//  TODO:  All of this crap is gonna be replaced with a more cohesive supply-
//  and-demand system- the ecology system, the stocks-demand-diffusion, the
//  native-huts placement- everything.


public class NativeHall extends NativeHut {
  
  
  final List <NativeHut> children = new List <NativeHut> ();
  private float idealPopEstimate = -1;
  
  
  public NativeHall(Base base) {
    //  NOTE:  Not intended for actual construction purposes.
    this(TYPE_PLACES, base);
  }
  
  
  protected NativeHall(int tribeID, Base base) {
    super(3, 2, TYPE_HALL, tribeID, base);
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
  public Background[] careers() {
    return super.careers();
    //  Cargo Cultist, Marked One, Medicine Man, Chieftain.
  }
  
  
  public Behaviour jobFor(Actor actor) {
    //  Well, firstly determine if any more huts should be placed or repaired.
    return super.jobFor(actor);
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates);
    if (! structure.intact()) return;
    
    if (numUpdates % 10 == 0) updatePopEstimate(world);
    
    for (NativeHut hut : children) {
      if (hut.personnel.unoccupied()) {
        hut.structure.setState(Structure.STATE_SALVAGE,  -1);
      }
      if (hut.destroyed()) children.remove(hut);
    }
  }
  
  
  public void addServices(Choice choice, Actor forActor) {
  }
  
  
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
  
  
  
  /**  Rendering and interface-
    */
  public String fullName() {
    return "Chief's Hall";
  }
  
  
  public Composite portrait(BaseUI UI) {
    return super.portrait(UI);
  }
  
  
  public String helpInfo() {
    return
      "Native settlements will often have a central meeting place where "+
      "the tribe's leadership and elders will gather to make decisions, "+
      "such as arranging marriage, arbitrating dispute or mounting raids.";
  }
}








/*
  /**  Placement of natives-
    */
  
  
  /*
  public void populateWithNatives(int tribeID) {
    float meadowed = (1 + totalFertility) / 2f;
    final int
      numMajorHuts = (int) ((meadowed * numMajor) + 0.5f),
      numMinorHuts = (int) ((meadowed * numMinor) + 0.5f);
    I.say("Major/minor huts: "+numMajorHuts+"/"+numMinorHuts);
    
    final Base base = world.baseWithName("Natives", true, true);
    
    for (int n = numMajorHuts + numMinorHuts; n-- > 0;) {
      final int SS = World.SECTOR_SIZE;
      Coord pos = findBasePosition(null, 1);
      I.say("Huts site at: "+pos);
      final Tile centre = world.tileAt(
        (pos.x + 0.5f) * SS,
        (pos.y + 0.5f) * SS
      );
      final boolean minor = n < numMinorHuts;
      int maxHuts = (minor ? 4 : 2) + Rand.index(3);
      final Batch <Venue> huts = new Batch <Venue> ();
      
      final NativeHall hall = NativeHut.newHall(tribeID, base);
      Placement.establishVenue(hall, centre.x, centre.y, true, world);
      if (hall.inWorld()) huts.add(hall);
      
      while (maxHuts-- > 0) {
        final NativeHut r = NativeHut.newHut(hall);
        Placement.establishVenue(r, centre.x, centre.y, true, world);
        if (r.inWorld()) huts.add(r);
      }
      
      populateNatives(huts, minor);
    }
  }
  
  
  //*/

//*/






