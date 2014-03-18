

package src.game.common ;
import src.game.building.* ;
import src.game.planet.* ;
import src.util.* ;



public class Presences {
	
	
	
  /**  fields, constructors and save/load functionality-
    */
	final World world ;
  final Table <Object, PresenceMap> allMaps ;
  
  private PresenceMap floraMap ;
  private PresenceMap outcropMap ;
  private PresenceMap mobilesMap ;
  
  final private PresenceMap nullMap ;
  final private Stack nullStack ;
  
  
  
  Presences(World world) {
  	this.world = world ;
  	allMaps = new Table <Object, PresenceMap> () ;
  	floraMap = new PresenceMap(world, Flora.class) ;
  	outcropMap = new PresenceMap(world, Outcrop.class) ;
  	mobilesMap = new PresenceMap(world, Mobile.class) ;
  	allMaps.put(Flora.class , floraMap  ) ;
  	allMaps.put(Outcrop.class, outcropMap) ;
  	allMaps.put(Mobile.class, mobilesMap) ;
  	
  	nullMap = new PresenceMap(world, "nothing") {
		  public void toggleMember(Target t, Tile at, boolean is) {
		  	I.complain("Cannot modify null-presence map!") ;
		  }
  	} ;
  	nullStack = new Stack() ;
  }
  
  
  protected void loadState(Session s) throws Exception {
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final PresenceMap f = (PresenceMap) s.loadObject() ;
      if (f != null) allMaps.put(f.key, f) ;
    }
    floraMap = allMaps.get(Flora.class) ;
    outcropMap = allMaps.get(Outcrop.class) ;
    mobilesMap = allMaps.get(Mobile.class) ;
  }
  
  
  protected void saveState(Session s) throws Exception {
    s.saveInt(allMaps.size()) ;
    for (PresenceMap f : allMaps.values()) {
      if (f.population() > 0) s.saveObject(f) ;
      else s.saveObject(null) ;
    }
  }
  
  
  

  /**  Modifying presences-
    */
  public void togglePresence(Target t, Tile at, boolean is, Object key) {
    ///if (! is) I.say("De-registering "+t+" for key "+key) ;
    final PresenceMap map = mapFor(key) ;
    if (is) map.toggleMember(t, at, true) ;
    else map.toggleMember(t, at, false) ;
  }
  
  
  public void togglePresence(Flora f, boolean is) {
  	floraMap.toggleMember(f, f.origin(), is) ;
  }
  
  //
  //  TODO:  Include species/species-type registration.
  public void togglePresence(Mobile m, Tile at, boolean is) {
  	mobilesMap.toggleMember(m, at, is) ;
  }
  
  
  public void togglePresence(
    Venue venue, boolean is//, Object services[]
  ) {
  	final Tile origin = venue.origin() ;
  	
  	togglePresence(venue, origin, is, Venue.class) ;
    togglePresence(venue, origin, is, venue.getClass()) ;
    if (venue.base() != null) {
      togglePresence(venue, origin, is, venue.base()) ;
    }
    
    final Object services[] = venue.services() ;
    if (services != null) for (Object service : services) {
      togglePresence(venue, origin, is, service) ;
    }
    
    final Object careers[] = venue.careers() ;
    if (careers != null) for (Object career : careers) {
      togglePresence(venue, origin, is, career) ;
    }
    
    /*
    I.say("\n\nCHECKING FOR RESIDUE...") ;
    for (PresenceMap map : allMaps.values()) {
      map.printSectionFor(venue, null) ;
    }
    //*/
  }
  
  
  
  /**  Querying presences-
    */
  public PresenceMap mapFor(Object key) {
  	PresenceMap map = allMaps.get(key) ;
  	if (map == null) allMaps.put(key, map = new PresenceMap(world, key)) ;
    return map ;
  }
  
  
  public Iterable matchesNear(Object service, Target client, float range) {
    final PresenceMap map = allMaps.get(service) ;
    if (map == null) return nullStack ;
    return map.visitNear(world.tileAt(client), range, null) ;
  }
  
  
  public Iterable matchesNear(Object service, Target client, Box2D area) {
    final PresenceMap map = allMaps.get(service) ;
    if (map == null) return nullStack ;
    return map.visitNear(world.tileAt(client), -1, area) ;
  }
  
  
  public Target nearestMatch(Object service, Target client, float range) {
    for (Object o : matchesNear(service, client, range)) return (Target) o ;
    return null ;
  }
  
  
  public Target randomMatchNear(Object service, Target client, float range) {
    final PresenceMap map = allMaps.get(service) ;
    if (map == null) return null ;
    return map.pickRandomAround(client, range) ;
  }
  
  
  public Series <Target> sampleFromKeys(
    Target t, World world, int limit, Series sampled, Object... keys
  ) {
    if (sampled == null) sampled = new Batch() ;
    for (Object key : keys) {
      sampleTargets(key, t, world, limit, sampled) ;
    }
    for (Object o : sampled) ((Target) o).flagWith(null) ;
    return sampled ;
  }
  

  public Series <Target> sampleFromKey(
    Target t, World world, int limit, Series sampled, Object key
  ) {
    if (sampled == null) sampled = new Batch() ;
    sampleTargets(key, t, world, limit, sampled) ;
    for (Object o : sampled) ((Target) o).flagWith(null) ;
    return sampled ;
  }
  
  
  private Series <Target> sampleTargets(
    Object key, Target t, World world, int limit, Series sampled
  ) {
    for (int n = limit / 2 ; n-- > 0 ;) {
      final Target v = randomMatchNear(key, t, -1) ;
      if (v == t || v == null || v.flaggedWith() != null) continue ;
      sampled.add(v) ;
      v.flagWith(sampled) ;
    }
    for (Object o : matchesNear(key, t, -1)) {
      if (o == t) continue ; final Target v = (Target) o ;
      if (v.flaggedWith() != null) continue ;
      sampled.add(v) ;
      if (sampled.size() >= limit) break ;
    }
    return sampled ;
  }
}






  
  