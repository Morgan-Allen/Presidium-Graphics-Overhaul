/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.plans.Hunting;
import stratos.game.wild.Species.Type;
import stratos.graphics.common.ModelAsset;
import stratos.graphics.cutout.CutoutModel;
import stratos.graphics.solids.MS3DModel;
import stratos.util.*;



public class Hareen extends Fauna {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static ModelAsset
    MODEL_NEST_VAREEN = CutoutModel.fromImage(
      Hareen.class, LAIR_DIR+"nest_vareen.png", 2.5f, 3
    );
  
  final public static Species SPECIES = new Species(
    Hareen.class,
    "Hareen",
    "Hareen are sharp-eyed aerial omnivores active by day, with a twinned "+
    "pair of wings that makes them highly maneuverable flyers.  Their "+
    "diet includes fruit, nuts, insects and carrion, but symbiotic algae "+
    "in their skin also allow them to subsist partially on sunlight.",
    FILE_DIR+"VareenPortrait.png",
    MS3DModel.loadFrom(
      FILE_DIR, "Vareen.ms3d", Hareen.class,
      XML_FILE, "Vareen"
    ),
    Type.BROWSER,
    0.50f, //bulk
    2.60f, //speed
    1.00f  //sight
  ) {
    final Blueprint BLUEPRINT = Nest.constructBlueprint(
      2, 2, this, MODEL_NEST_VAREEN
    );
    public Actor sampleFor(Base base) { return init(new Hareen(base)); }
    public Blueprint nestBlueprint() { return BLUEPRINT; }
  };
  
  
  final static float DEFAULT_FLY_HEIGHT = 2.5f;
  final static int FLY_PATH_LIMIT = 16;
  
  private float flyHeight = DEFAULT_FLY_HEIGHT;
  private Nest nest = null;
  
  
  
  public Hareen(Base base) {
    super(SPECIES, base);
  }
  
  
  public Hareen(Session s) throws Exception {
    super(s);
    flyHeight = s.loadFloat();
    nest = (Nest) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveFloat(flyHeight);
    s.saveObject(nest);
  }
  
  
  protected void initStats() {
    //
    //  TODO:  PUT ALL THESE ATTRIBUTES IN THE SPECIES FIELDS
    traits.initAtts(10, 20, 3);
    health.initStats(
      5,    //lifespan
      species.baseBulk , //bulk bonus
      species.baseSight, //sight range
      species.speedMult, //speed rate
      ActorHealth.ANIMAL_METABOLISM
    );
    gear.setBaseDamage(4);
    gear.setBaseArmour(2);
  }
  
  
  public float radius() {
    return 0.5f;
  }
  
  
  
  /**  Behaviour modifications/implementation-
    */
  protected void updateAsMobile() {
    final Target target = this.planFocus(null, true);
    float idealHeight = DEFAULT_FLY_HEIGHT;
    
    if (indoors() || ! health.conscious()) {
      idealHeight = 0;
    }
    else if (
      (target != null && target != this) &&
      (Spacing.distance(this, target) < 2)
    ) {
      idealHeight = target.position(null).z + (target.height() / 2f) - 0.5f;
    }
    
    final float min = flyHeight - 0.1f, max = flyHeight + 0.1f;
    flyHeight = Nums.clamp(Nums.max(0, idealHeight), min, max);
    
    super.updateAsMobile();
  }
  

  public void updateAsScheduled(int numUpdates, boolean instant) {
    if (! indoors()) {
      final float value = Planet.dayValue(world) / Stage.STANDARD_DAY_LENGTH;
      health.takeCalories(value, 1);
    }
    super.updateAsScheduled(numUpdates, instant);
  }
  
  
  protected float aboveGroundHeight() {
    return flyHeight;
  }
  
  
  protected Behaviour nextBrowsing() {
    final Choice c = new Choice(this);
    for (Target e : senses.awareOf()) {
      if (Hunting.validPrey(e, this)) {
        final Actor prey = (Actor) e;
        if (! prey.health.alive()) c.add(Hunting.asFeeding(this, prey));
      }
    }
    final Behaviour p = c.pickMostUrgent();
    if (p != null) return p;
    return super.nextBrowsing();
  }
  


  /**  Rendering and interface methods-
    */
  protected float moveAnimStride() { return 1.0f; }
}




//
//  TODO:  Try to restore flight-pathing later on.
/*
protected MobilePathing initPathing() {
  final Vareen actor = this;
  //
  //  We use a modified form of pathing search that can bypass most
  //  tiles.
  return new MobilePathing(actor) {
    protected Boardable[] refreshPath(Boardable initB, Boardable destB) {
      final Lair lair = (Lair) actor.AI.home();
      
      final PathingSearch flightPS = new PathingSearch(
        initB, destB, FLY_PATH_LIMIT
      ) {
        final Boardable tileB[] = new Boardable[8];
        
        protected Boardable[] adjacent(Boardable spot) {
          //
          //  TODO:  There has got to be some way to factor this out into the
          //  Tile and PathingSearch classes.  This is recapitulating a lot
          //  of functionality AND IT'S DAMNED UGLY
          //  ...Also, it can't make use of the pathingCache class, though
          //  that's less vital here.
          if (spot instanceof Tile) {
            final Tile tile = ((Tile) spot);
            for (int i : Tile.N_DIAGONAL) {
              final Tile near = world.tileAt(
                tile.x + Tile.N_X[i],
                tile.y + Tile.N_Y[i]
              );
              tileB[i] = blocksMotion(near) ? null : near;
            }
            for (int i : Tile.N_ADJACENT) {
              final Tile near = world.tileAt(
                tile.x + Tile.N_X[i],
                tile.y + Tile.N_Y[i]
              );
              if (
                near != null && near.owner() == lair &&
                lair != null && tile == lair.mainEntrance()
              ) tileB[i] = lair;
              else tileB[i] = blocksMotion(near) ? null : near;
            }
            Spacing.cullDiagonals(tileB);
            return tileB;
          }
          return super.adjacent(spot);
        }
        
        protected boolean canEnter(Boardable spot) {
          return ! blocksMotion(spot);
        }
      };
      flightPS.doSearch();
      return flightPS.bestPath(Boardable.class);
    }
  };
}


public boolean blocksMotion(Boardable t) {
  if (t instanceof Tile) {
    final Element owner = ((Tile) t).owner();
    return owner != null && owner.height() > 2.5f;
  }
  return false;
}
//*/




/*
protected Behaviour nextFeeding() {
  //
  //  If you can't find other food, just bask in the sun to gain energy-
  if (pick == null && ! origin().blocked()) {
    final Action basking = new Action(
      this, origin(),
      this, "actionBask",
      Action.MOVE, "Basking"
    );
    basking.setProperties(Action.CAREFUL);
    ///basking.setDuration(2.0f);
    return basking;
  }
  //
  //  Otherwise, go pluck some fruit or whatever-
  if (pick == null) return null;
  final Action foraging = new Action(
    this, pick,
    this, "actionForage",
    Action.STRIKE, "Foraging"
  );
  foraging.setMoveTarget(Spacing.nearestOpenTile(pick, this));
  return foraging;
}


public boolean actionBask(Vareen actor, Tile location) {
  //
  //  Adjust this based on night/day values?
  health.takeSustenance(location.habitat().insolation() / 100f, 1.0f);
  return true;
}
//*/




