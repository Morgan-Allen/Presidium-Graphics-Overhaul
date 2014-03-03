/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.planet ;
import src.game.common.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.cutout.CutoutModel ;
import src.util.* ;



public class Flora extends Element implements TileConstants {
  
  
  /**  Field definitions and constructors-
    */
  final public static int
    MAX_GROWTH = 4 ;
  final public static float
    GROWTH_PER_UPDATE = 0.25f ;
  
  final Habitat habitat ;
  final int varID ;
  float growth = 0 ;
  
  
  
  Flora(Habitat h) {
    this.habitat = h ;
    this.varID = Rand.index(4) ;
  }
  
  
  public Flora(Session s) throws Exception {
    super(s) ;
    habitat = Habitat.ALL_HABITATS[s.loadInt()] ;
    varID = s.loadInt() ;
    growth = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(habitat.ID) ;
    s.saveInt(varID) ;
    s.saveFloat(growth) ;
  }
  
  
  
  /**  Attempts to seed or grow new flora at the given coordinates.
    */
  public static void tryGrowthAt(int x, int y, World world, boolean init) {
    
    final Ecology ecology = world.ecology() ;
    final Tile t = world.tileAt(x, y) ;
    final Habitat h = world.terrain().habitatAt(x, y) ;
    if (h.floraModels == null) return ;
    final float growChance = h.moisture / (10f * 4) ;
    
    //
    //  Check to see how many neighbours this flora would have-
    int numBlocked = 0 ;
    for (int i : N_INDEX) {
      final Tile n = world.tileAt(t.x + N_X[i], t.y + N_Y[i]) ;
      if (n == null || n.blocked()) numBlocked++ ;
      if (n != null) {
        if (n.owningType() > Element.ELEMENT_OWNS) {
          numBlocked = 8 ;
          break ;
        }
        if (n.owner() instanceof Boardable) {
          if (t.isEntrance((Boardable) n.owner())) {
            numBlocked = 8 ;
            break ;
          }
        }
      }
    }
    
    if (t.owner() instanceof Flora) {
      final Flora f = (Flora) t.owner() ;
      if (Rand.num() < (growChance * 4 * GROWTH_PER_UPDATE)) {
        f.incGrowth(1, world, false) ;
        ecology.impingeBiomass(f, f.growth, true) ;
      }
    }
    else if ((! t.blocked()) && Rand.num() < growChance) {
      //
      //  If the place isn't too crowded, introduce a new specimen-
      if (numBlocked < 2) {
        final Flora f = new Flora(h) ;
        if (init) {
          f.enterWorldAt(t.x, t.y, world) ;
          float stage = 0.5f ;
          for (int n = MAX_GROWTH ; n-- > 0 ;) {
            if (Rand.num() < growChance * 4) stage++ ;
          }
          stage = Visit.clamp(stage, 0, MAX_GROWTH - 0.5f) ;
          f.incGrowth(stage, world, true) ;
          f.setAsEstablished(true) ;
          ecology.impingeBiomass(f, f.growth, false) ;
        }
        else if (Rand.num() < GROWTH_PER_UPDATE) {
          f.enterWorldAt(t.x, t.y, world) ;
          f.incGrowth(1, world, false) ;
          ecology.impingeBiomass(f, f.growth, true) ;
        }
      }
    }
  }
  
  
  public void incGrowth(
    float inc, World world, boolean init
  ) {
    final int oldGrowth = (int) growth ;
    growth += inc ;
    if (growth <= 0) { setAsDestroyed() ; return ; }
    final int newGrowth = (int) growth ;
    if (oldGrowth == newGrowth && ! init) return ;
    
    if (inc > 0 && ! init) {
      final float moisture = origin().habitat().moisture / 10f ;
      final int minGrowth = (int) ((moisture * moisture * MAX_GROWTH) + 1f) ;
      final float dieChance = 1 - moisture ;
      if (
        (growth < 0) || (growth >= MAX_GROWTH * 2) ||
        (growth > minGrowth && Rand.num() < dieChance)
      ) {
        setAsDestroyed() ;
        return ;
      }
    }
    final int tier = Visit.clamp((int) growth, MAX_GROWTH) ;
    final CutoutModel model = habitat.floraModels[varID][tier] ;
    final Sprite oldSprite = this.sprite() ;
    attachSprite(model.makeSprite()) ;
    setAsEstablished(false) ;
    if (oldSprite != null) world.ephemera.addGhost(this, 1, oldSprite, 2.0f) ;
  }
  
  
  public int growStage() {
    return Visit.clamp((int) growth, MAX_GROWTH) ;
  }
  
  
	public boolean enterWorldAt(int x, int y, World world) {
    if (! super.enterWorldAt(x, y, world)) return false ;
		world.presences.togglePresence(this, true ) ;
		return true ;
	}
	
	
	public void exitWorld() {
		world.presences.togglePresence(this, false) ;
		super.exitWorld() ;
	}
	
	
	public String toString() {
	  return "Flora" ;
	}
}














