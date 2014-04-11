/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.base ;
import stratos.game.common.* ;
import stratos.game.building.* ;
import stratos.game.planet.* ;
import stratos.game.actors.* ;
import stratos.user.* ;
import stratos.util.* ;


//  TODO:  Only allow mining within/underneath tailings?  Huh.  Maybe.
//  ...Yeah.  That's probably the best way.


public class Mining extends Plan implements Economy {
  
  
  
  /**  Fields, constructors and save/load methods-
    */
  final static int
    STAGE_INIT   = -1,
    STAGE_MINE   =  0,
    STAGE_RETURN =  1,
    STAGE_DONE   =  2 ;
  final static int MAX_SAMPLE_STORE = 50 ;
  
  private static boolean verbose = false, picksVerbose = false ;
  
  final ExcavationSite site ;
  final Target face ;
  private int stage = STAGE_INIT ;
  
  
  Mining(Actor actor, Target face, ExcavationSite site) {
    super(actor, site) ;
    this.site = site ;
    this.face = face ;
  }
  
  
  public Mining(Session s) throws Exception {
    super(s) ;
    site = (ExcavationSite) s.loadObject() ;
    face = s.loadTarget() ;
    stage = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(site) ;
    s.saveTarget(face) ;
    s.saveInt(stage) ;
  }
  
  
  public boolean matchesPlan(Plan p) {
    if (! super.matchesPlan(p)) return false ;
    final Mining m = (Mining) p ;
    return m.face == this.face && m.site == this.site ;
  }
  
  
  
  /**  Static location methods and priority evaluation-
    */
  protected static Tile[] getTilesUnder(final ExcavationSite site) {
    final Sorting <Tile> sorting = new Sorting <Tile> () {
      public int compare(Tile a, Tile b) {
        final float
          rA = (Float) a.flaggedWith(),
          rB = (Float) b.flaggedWith() ;
        if (rA > rB) return  1 ;
        if (rB > rA) return -1 ;
        return 0 ;
      }
    } ;
    final World world = site.world() ;
    final int range = site.digLimit(), SS = World.SECTOR_SIZE ;
    final Box2D area = new Box2D().setTo(site.area()) ;
    //
    //  Firstly, we spread out from beneath the excavation site and claim all
    //  tiles which are either mined out, or directly under the structure, up
    //  to a certain distance limit.
    final TileSpread spread = new TileSpread(world.tileAt(site)) {
      
      protected boolean canAccess(Tile t) {
        if (world.terrain().mineralDegree(t) != WorldTerrain.DEGREE_TAKEN) {
          if (site.area().contains(t.x, t.y)) return true ;
          return false ;
        }
        if (area.distance(t.x, t.y) > range) return false ;
        return true ;
      }
      
      protected boolean canPlaceAt(Tile t) { return false ; }
    } ;
    spread.doSearch() ;
    //
    //  We then get all adjacent tiles which are *not* mined out, sort them
    //  based on distance and promise, and return-
    final Tile open[] = spread.allSearched(Tile.class) ;
    final Batch <Tile> touched = new Batch <Tile> () ;
    for (Tile o : open) for (Tile n : o.edgeAdjacent(Spacing.tempT4)) {
      if (n == null || n.flaggedWith() != null) continue ;
      if (world.terrain().mineralDegree(n) == WorldTerrain.DEGREE_TAKEN) {
        continue ;
      }
      float rating = 10 ;
      rating *= SS / (SS + Spacing.distance(site, n)) ;
      final Item left = mineralsLeft(n) ;
      rating *= left.amount * site.extractionBonus(left.type) ;
      n.flagWith((Float) rating) ;
      touched.add(n) ;
      if (rating > 0) sorting.add(n) ;
    }
    if (picksVerbose) I.say("Tiles touched are: ") ;
    for (Tile t : touched) {
      if (picksVerbose) I.say("  "+t) ;
      t.flagWith(null) ;
    }
    if (sorting.size() == 0) return null ;
    return sorting.toArray(Tile.class) ;
  }
  
  
  protected static Target nextMineFace(ExcavationSite site, Tile under[]) {
    //
    //  Firstly, we set up some initial reference variables, and sample the
    //  tiles and outcrops that can harvest from.
    final Presences presences = site.world().presences ;
    final int SS = World.SECTOR_SIZE ;
    final Batch <Target> sampled = new Batch <Target> () ;
    Target picked = null ;
    float bestRating = Float.NEGATIVE_INFINITY ;
    presences.sampleFromKey(site, site.world(), 5, sampled, Outcrop.class) ;
    if (under != null) for (int n = 5 ; n-- > 0 ;) {
      sampled.add((Tile) Rand.pickFrom(under)) ;
    }
    //
    //  Then, we assess the promise associated with each prospective face, pick
    //  the best, and return-
    for (Target face : sampled) {
      final float dist = Spacing.distance(face, site) ;
      if (dist > site.digLimit() + (SS / 2)) continue ;
      final Item left = mineralsLeft(face) ;
      if (left == null) continue ;
      float rating = 1 + site.extractionBonus(left.type) ;
      if (face instanceof Outcrop) rating *= 10 ;
      rating *= SS / (SS + dist) ;
      if (rating > bestRating) { bestRating = rating ; picked = face ; }
    }
    return picked ;
  }
  
  
  public float priorityFor(Actor actor) {
    return ROUTINE ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    //
    //  If you've successfully extracted enough ore, or the target is exhausted,
    //  then deliver to the smelters near the site.
    if (verbose) I.sayAbout(actor, "  Getting new mine action.") ;
    boolean
      shouldQuit   = stage == STAGE_DONE,
      shouldReturn = stage == STAGE_RETURN ;
    final float carried = oresCarried(actor) ;
    
    if (mineralsLeft(face) == null || ! site.personnel.onShift(actor)) {
      if (verbose && I.talkAbout == actor) {
        I.say("QUITTING MINING") ;
        I.say("Minerals left: "+mineralsLeft(face)) ;
        I.say("On shift: "+site.personnel.onShift(actor)) ;
      }
      shouldQuit = true ;
      if (carried > 0) shouldReturn = true ;
    }
    else if (carried >= 5) shouldReturn = true ;
    
    if (shouldReturn) {
      stage = STAGE_RETURN ;
      Service mineral = null ; for (Item sample : Smelter.SAMPLE_TYPES) {
        if (actor.gear.matchFor(sample) == null) continue ;
        mineral = (Service) sample.refers ; break ;
      }
      final Venue smelter = site.smeltingSite(mineral) ;
      return new Action(
        actor, smelter == null ? site : smelter,
        this, "actionDeliverOres",
        Action.REACH_DOWN, "returning ores"
      ) ;
    }
    
    
    if (shouldQuit) return null ;
    //
    //  If the target is a tile, then target it from underneath, via the
    //  excavation site.
    stage = STAGE_MINE ;
    if (face instanceof Tile) {
      if (actor.aboard() == site) {
        final Action mines = new Action(
          actor, face,
          this, "actionMineTile",
          Action.STRIKE_BIG, "Mining"
        ) ;
        mines.setMoveTarget(site) ;
        return mines ;
      }
      else {
        final Action entry = new Action(
          actor, site,
          this, "actionEnterShaft",
          Action.STAND, "Entering shaft"
        ) ;
        return entry ;
      }
    }
    //
    //  If the target is an outcrop, then target it from above.
    if (face instanceof Outcrop) {
      final Outcrop o = (Outcrop) face ;
      boolean shouldMove = Rand.index(10) == 0 ;
      if (! Spacing.adjacent(actor.origin(), o)) shouldMove = true ;
      
      final Action mines = new Action(
        actor, face,
        this, "actionMineOutcrop",
        Rand.yes() ? Action.STRIKE_BIG : Action.BUILD, "Mining"
      ) ;
      if (shouldMove) {
        mines.setMoveTarget(Spacing.pickFreeTileAround(face, actor)) ;
      }
      else mines.setMoveTarget(actor.origin()) ;
      return mines ;
    }
    I.complain("Target type not supported: "+face) ;
    return null ;
  }
  
  
  public boolean actionEnterShaft(Actor actor, ExcavationSite site) {
    actor.goAboard(site, site.world()) ;
    //
    //  TODO:  Consider introducing security/safety measures here?
    return true ;
  }
  
  
  public boolean actionDeliverOres(Actor actor, Venue venue) {
    if (verbose) I.sayAbout(actor, "Returning to "+venue) ;
    
    for (Service type : Smelter.MINED_TYPES) {
      if (venue instanceof Smelter) {
        final Service output = ((Smelter) venue).output ;
        if (type != output) continue ;
      }
      actor.gear.transfer(type, venue) ;
      for (Item match : actor.gear.matches(Item.asMatch(SAMPLES, type))) {
        actor.gear.removeItem(match) ;
        if (venue.stocks.amountOf(match) < MAX_SAMPLE_STORE) {
          venue.stocks.addItem(match) ;
        }
      }
    }
    if (oresCarried(actor) == 0) stage = STAGE_DONE ;
    return true ;
  }
  
  
  public boolean actionMineOutcrop(Actor actor, Outcrop face) {
    final float oldAmount = face.mineralAmount() ;
    float progress = successCheck(actor, Habitat.MESA) / (10f * face.bulk()) ;
    final Upgrade SPU = ExcavationSite.SAFETY_PROTOCOL ;
    progress *= 1 + (site.structure.upgradeLevel(SPU) / 3f) ;
    
    face.incCondition(0 - progress) ;
    if (face.condition() == 0) face.setAsDestroyed() ;
    final float taken = oldAmount - face.mineralAmount() ;
    final Item left = mineralsLeft(face) ;
    if (taken == 0 || left == null) return false ;
    
    final Item mined = Item.with(SAMPLES, left.type, taken, 0) ;
    actor.gear.addItem(mined) ;
    return true ;
  }
  
  
  public boolean actionMineTile(Actor actor, Tile face) {
    float progressChance = successCheck(actor, face.habitat()) / 10f ;
    final Item left = mineralsLeft(face) ;
    if (left == null) return false ;
    final float bonus = site.extractionBonus(left.type) ;
    progressChance *= 1 + (bonus / 2f) ;
    if (Rand.num() > progressChance) return false ;
    
    final WorldTerrain terrain = face.world.terrain() ;
    final byte typeID = terrain.mineralType(face) ;
    final float oldAmount = terrain.mineralsAt(face, typeID) ;
    terrain.incMineralDegree(face, typeID, -1) ;
    final float taken = oldAmount - terrain.mineralsAt(face, typeID) ;
    
    if (taken == 0 || left == null) return false ;
    final Item mined = Item.with(SAMPLES, left.type, taken * bonus / 2f, 0) ;
    actor.gear.addItem(mined) ;
    
    return true ;
  }
  
  
  private static float oresCarried(Actor actor) {
    float total = 0 ;
    for (Service type : Smelter.MINED_TYPES) {
      total += actor.gear.amountOf(type) ;
    }
    for (Item sample : Smelter.SAMPLE_TYPES) {
      final Item match = actor.gear.matchFor(sample) ;
      if (match != null) total += match.amount ;
    }
    return total ;
  }
  
  
  private static Item mineralsLeft(Target face) {
    byte type = -1 ;
    float amount = -1 ;
    if (face instanceof Tile) {
      final Tile t = (Tile) face ;
      type = t.world.terrain().mineralType(t) ;
      amount = t.world.terrain().mineralsAt(t, type) ;
      if (type == WorldTerrain.TYPE_NOTHING) {
        type = Rand.yes() ? WorldTerrain.TYPE_METALS : WorldTerrain.TYPE_ISOTOPES ;
        amount = 1 ;
      }
    }
    else if (face instanceof Outcrop) {
      final Outcrop o = (Outcrop) face ;
      type = o.mineralType() ;
      amount = o.mineralAmount() ;
      if (type == WorldTerrain.TYPE_NOTHING) {
        type = Rand.yes() ? WorldTerrain.TYPE_METALS : WorldTerrain.TYPE_ISOTOPES ;
        amount = 1 * o.bulk() * o.condition() ;
      }
    }
    else return null ;
    Service mineral = null ; switch (type) {
      case (WorldTerrain.TYPE_RUINS   ) : mineral = ARTIFACTS  ; break ;
      case (WorldTerrain.TYPE_METALS  ) : mineral = METALS  ; break ;
      case (WorldTerrain.TYPE_ISOTOPES) : mineral = FUEL_RODS ; break ;
    }
    if (mineral == null || amount <= 0) {
      I.say("Type/amount: "+type+"/"+amount) ;
      return null ;
    }
    return Item.withAmount(mineral, amount) ;
  }
  
  
  private static float successCheck(Actor actor, Habitat h) {
    float success = 1 ;
    success += actor.traits.test(GEOPHYSICS , 5 , 1) ? 1 : 0 ;
    success *= actor.traits.test(HARD_LABOUR, 15, 1) ? 2 : 1 ;
    if (h != null) {
      success *= (0.5f + 1 - (h.minerals() / 10f)) / 2 ;
    }
    return success ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (stage <= STAGE_MINE) {
      d.append("Mining ") ;
      if (face instanceof Tile) d.append(((Tile) face).habitat().name) ;
      else d.append(face) ;
    }
    if (stage == STAGE_RETURN) {
      d.append("Returning ores to "+actor.focusFor(Mining.class)) ;
    }
  }
}







