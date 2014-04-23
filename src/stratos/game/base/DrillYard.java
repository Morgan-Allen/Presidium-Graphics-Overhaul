

package stratos.game.base ;
import stratos.game.common.* ;
import stratos.game.maps.* ;
import stratos.game.actors.* ;
import stratos.game.building.* ;
import stratos.graphics.common.* ;
import stratos.graphics.cutout.* ;
import stratos.graphics.widgets.* ;
import stratos.user.* ;
import stratos.util.* ;

//
//  TODO:  I need some method of ensuring that actors won't just walk through
//  the drill yard unless they have business there.

public class DrillYard extends Venue {
  
  
  
  /**  Constructors, data fields, setup and save/load methods-
    */
  final static String IMG_DIR = "media/Buildings/military/" ;
  final static ModelAsset
    YARD_MODEL = CutoutModel.fromSplatImage(
      DrillYard.class, IMG_DIR+"drill_yard.png", 4.25f
    ),
    YARD_FRONT_MODEL = CutoutModel.fromImage(
      DrillYard.class, IMG_DIR+"drill_yard_front.png", 4.25f, 2.5f
    ),
    MELEE_MODEL = CutoutModel.fromImage(
      DrillYard.class, IMG_DIR+"drill_melee.png", 2, 1
    ),
    RANGED_MODEL = CutoutModel.fromImage(
      DrillYard.class, IMG_DIR+"drill_ranged.png", 2, 1
    ),
    ENDURE_MODEL = CutoutModel.fromImage(
      DrillYard.class, IMG_DIR+"drill_endurance.png", 2, 1
    ),
    AID_MODEL = CutoutModel.fromImage(
      DrillYard.class, IMG_DIR+"drill_aid_table.png", 2, 1
    ),
    BLANK_DRILL_MODEL = CutoutModel.fromImage(
      DrillYard.class, IMG_DIR+"drill_blank.png", 2, 1
    ),
    DRILL_MODELS[] = {
      MELEE_MODEL, RANGED_MODEL, ENDURE_MODEL, AID_MODEL
    } ;
  
  
  final public static int
    NOT_DRILLING    = -1,
    DRILL_MELEE     =  0,
    DRILL_RANGED    =  1,
    DRILL_ENDURANCE =  2,
    DRILL_AID       =  3,
    NUM_DRILLS      =  4,
    
    STATE_RED_ALERT =  4,
    DRILL_STATES[]  = { 0, 1, 2, 3 },
    
    NUM_DUMMIES = 2,
    NUM_OFFSETS = 4,
    DRILL_INTERVAL = World.STANDARD_DAY_LENGTH ;
  
  final static String DRILL_STATE_NAMES[] = {
    "Close Combat",
    "Target Practice",
    "Endurance Course",
    "Aid Table"
  };
  
  
  
  final public Garrison belongs ;
  protected int drill = NOT_DRILLING;
  protected boolean drillOrders[] = new boolean[NUM_DRILLS] ;
  
  
  
  public DrillYard(Garrison belongs) {
    super(4, 1, ENTRANCE_EAST, belongs.base()) ;
    structure.setupStats(50, 10, 25, 0, Structure.TYPE_FIXTURE) ;
    this.belongs = belongs ;
    initSprite();
  }
  

  public DrillYard(Session s) throws Exception {
    super(s) ;
    belongs = (Garrison) s.loadObject() ;
    drill = s.loadInt() ;
    for (int i = 0 ; i < NUM_DRILLS ; i++) drillOrders[i] = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(belongs) ;
    s.saveInt(drill) ;
    for (boolean b : drillOrders) s.saveBool(b) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public boolean enterWorldAt(int x, int y, World world) {
    if (! super.enterWorldAt(x, y, world)) return false ;
    //setupDummies() ;
    return true ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (! structure.intact()) return ;
    
    int numModes = 0 ;
    for (boolean b : drillOrders) if (b) numModes++ ;
    if (numModes > 0) {
      final int mode = (int) (world.currentTime() / DRILL_INTERVAL) ;
      for (int i = 0, d = 0 ; i < drillOrders.length ; i++) {
        if (! drillOrders[i]) continue ;
        if (d == mode % numModes) {
          drill = DRILL_STATES[i] ;
          break ;
        }
        else d++ ;
      }
    }
    
    updateSprite();
  }
  
  
  public Service[] services() { return null ; }
  
  public Background[] careers() { return null ; }
  
  public Behaviour jobFor(Actor actor) { return null ; }
  
  
  public void addServices(Choice choice, Actor forActor) {
    choice.add(new Drilling(forActor, this)) ;
  }
  
  
  protected boolean canTouch(Element e) {
    return (e.owningType() < this.owningType()) || e == belongs ;
  }
  
  
  
  /**  Helping to configure drill actions-
    */
  protected Upgrade bonusFor(int state) {
    switch (state) {
      case (DRILL_MELEE    ) : return Garrison.MELEE_TRAINING     ;
      case (DRILL_RANGED   ) : return Garrison.MARKSMAN_TRAINING  ;
      case (DRILL_ENDURANCE) : return Garrison.ENDURANCE_TRAINING ;
      case (DRILL_AID      ) : return Garrison.AID_TRAINING       ;
    }
    return null ;
  }
  
  
  public int drillType() {
    return drill ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  //*
  final static float
    MELEE_OFFS[] = {
      1, 0,  1, 1,  1, 2,  1, 3,
    },
    RANGED_OFFS[] = {
      3, 0,  3, 1,  3, 2,  3, 3,
    },
    ENDURE_OFFS[] = {
      0, 0,  0, 3,  3, 3,  3, 0,
    },
    AID_OFFS[] = {
      2, 0,  2, 1,  2, 2,  2, 3,
    },
    DUMMY_OFFS[] = {
      0.5f, 2,  0.5f, 3,
    };
  
  
  private void initSprite() {
    final GroupSprite sprite = new GroupSprite();
    sprite.attach(YARD_MODEL, 0, 0, 0);
    sprite.attach(BLANK_DRILL_MODEL, 0, -1.5f, 0);
    sprite.attach(YARD_FRONT_MODEL, 0, 0, 0);
    sprite.setSortMode(GroupSprite.SORT_BY_ADDITION);
    attachSprite(sprite);
  }
  
  
  private void updateSprite() {
    final boolean inUse = world.activities.includes(this, Drilling.class);
    final GroupSprite s = (GroupSprite) buildSprite().baseSprite() ;
    final ModelAsset m =
      (drill == NOT_DRILLING || drill == STATE_RED_ALERT || ! inUse) ?
      BLANK_DRILL_MODEL : DRILL_MODELS[drill] ;
    
    final CutoutSprite old = (CutoutSprite) s.atIndex(1);
    if (old.model() == m) return;
    //  TODO:  Use ephemera to fade in and out.
    old.setModel((CutoutModel) m);
  }
  
  
  protected Target.Dummy dummyFor(Actor visits) {
    Mobile shown = inside().first();
    if (visits != shown) return null;
    
    final Target.Dummy dummy = new Target.Dummy(world);
    this.position(dummy.position);
    dummy.position.y -= 1.5f;
    return dummy;
  }
  
  
  public void renderFor(Rendering rendering, Base base) {
    super.renderFor(rendering, base);
    
    //  TODO:  Try rendering up to 3 at once...
    final Vec3D p = this.position(null);
    Mobile shown = inside().first();
    if (shown != null) shown.renderAt(p, -90, rendering);
  }


  public Composite portrait(BaseUI UI) {
    return Composite.withImage(Garrison.ICON, "drill_yard");
  }
  
  
  public String fullName() {
    return "Drill Yard" ;
  }
  
  
  public String helpInfo() {
    return
      "Soldiers from your garrison and other military structures will gather "+
      "to practice at your drill yard." ;
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_MILITANT ;
  }
  
  
  protected void describeDrills(Description d) {
    d.append("Drill Orders:") ;
    for (final int s : DRILL_STATES) {
      d.append("\n  ") ;
      d.append(new Description.Link(DRILL_STATE_NAMES[s]) {
        public void whenTextClicked() {
          drillOrders[s] = ! drillOrders[s] ;
        }
      }) ;
      if (drillOrders[s]) d.append(" (Scheduled)") ;
    }
    if (drill != NOT_DRILLING) {
      d.append("\n\nCurrent Drill: "+DRILL_STATE_NAMES[drill]) ;
    }
  }
  
  
  public InfoPanel configPanel(InfoPanel panel, BaseUI UI) {
    return VenueDescription.configPanelWith(
      this, panel, UI, CAT_STATUS, CAT_STAFF
    );
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    belongs.renderSelection(rendering, hovered);
  }
}


/*
private void setupDummies() {
  final Tile o = origin() ;
  if (o == null) return ;
  for (int i = NUM_DUMMIES, c = 0 ; i-- > 0 ;) {
    final Tile t = world.tileAt(
      o.x + DUMMY_OFFS[c++],
      o.y + DUMMY_OFFS[c++]
    ) ;
    if (dummies[i] == null) dummies[i] = new Element(t, null) ;
  }
}
//*/



