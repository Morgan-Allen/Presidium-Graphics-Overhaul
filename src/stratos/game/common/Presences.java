

package stratos.game.common;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.wild.Flora;
import stratos.game.wild.Outcrop;
import stratos.util.*;



//  TODO:  As an added safety feature, presence-maps will need to be
//  periodically checked for dead objects.

public class Presences {
	
	
	
  /**  fields, constructors and save/load functionality-
    */
	final Stage world;
  final Table <Object, PresenceMap> allMaps;
  
  private PresenceMap floraMap;
  private PresenceMap outcropMap;
  private PresenceMap mobilesMap;
  
  final private PresenceMap nullMap;
  final private Stack nullStack;
  
  //  TODO:  All elements need to register their presence with their base of
  //  origin (or the world if that fails.)
  
  
  
  Presences(Stage world) {
  	this.world = world;
  	allMaps = new Table <Object, PresenceMap> ();
  	floraMap   = new PresenceMap(world, Flora  .class);
  	outcropMap = new PresenceMap(world, Outcrop.class);
  	mobilesMap = new PresenceMap(world, Mobile .class);
  	allMaps.put(Flora.class  , floraMap  );
  	allMaps.put(Outcrop.class, outcropMap);
  	allMaps.put(Mobile.class , mobilesMap);
  	
    nullMap = new PresenceMap(world, "nothing") {
      public void toggleMember(Target t, Tile at, boolean is) {
        I.complain("Cannot modify null-presence map!");
      }
  	};
  	nullStack = new Stack();
  }
  
  
  protected void loadState(Session s) throws Exception {
    for (int n = s.loadInt(); n-- > 0;) {
      final PresenceMap f = (PresenceMap) s.loadObject();
      if (f != null) allMaps.put(f.key, f);
    }
    floraMap = allMaps.get(Flora.class);
    outcropMap = allMaps.get(Outcrop.class);
    mobilesMap = allMaps.get(Mobile.class);
  }
  
  
  protected void saveState(Session s) throws Exception {
    s.saveInt(allMaps.size());
    for (PresenceMap f : allMaps.values()) {
      if (f.population() > 0) s.saveObject(f);
      else s.saveObject(null);
    }
  }
  
  
  

  /**  Modifying presences-
    */
  public void togglePresence(Target t, Tile at, boolean is, Object key) {
    ///if (! is) I.say("De-registering "+t+" for key "+key);
    final PresenceMap map = mapFor(key);
    if (is) map.toggleMember(t, at, true);
    else map.toggleMember(t, at, false);
  }
  
  
  public void togglePresence(Flora f, boolean is) {
  	floraMap.toggleMember(f, f.origin(), is);
  }
  
  //
  //  TODO:  Include species/species-type registration.
  public void togglePresence(Mobile m, Tile at, boolean is) {
  	mobilesMap.toggleMember(m, at, is);
  }
  
  
  //  TODO:  Move this to the venue/venue-stocks classes?
  public void togglePresence(
    Venue venue, boolean is
  ) {
  	final Tile origin = venue.origin();
  	
  	togglePresence(venue, origin, is, Venue.class);
    togglePresence(venue, origin, is, venue.getClass());
    if (venue.base() != null) {
      togglePresence(venue, origin, is, venue.base());
    }
    
    final Object services[] = venue.services();
    if (services != null) for (Object service : services) {
      togglePresence(venue, origin, is, service);
    }
    
    final Object careers[] = venue.careers();
    if (careers != null) for (Object career : careers) {
      togglePresence(venue, origin, is, career);
    }
    
    /*
    I.say("\n\nCHECKING FOR RESIDUE...");
    for (PresenceMap map : allMaps.values()) {
      map.printSectionFor(venue, null);
    }
    //*/
  }
  
  
  
  /**  Querying presences-
    */
  public PresenceMap mapFor(Object key) {
  	PresenceMap map = allMaps.get(key);
  	if (map == null) allMaps.put(key, map = new PresenceMap(world, key));
    return map;
  }
  
  
  public Iterable <Target> allMatches(Object service) {
    return matchesNear(service, null, -1);
  }
  
  
  public Iterable <Target> matchesNear(
    Object service, Target client, float range
  ) {
    final PresenceMap map = allMaps.get(service);
    if (map == null) return nullStack;
    return map.visitNear(client, range, null);
  }
  
  
  public Iterable <Target> matchesNear(
    Object service, Target client, Box2D area
  ) {
    if (service == null) return nullStack;
    final PresenceMap map = allMaps.get(service);
    if (map == null) return nullStack;
    return map.visitNear(world.tileAt(client), -1, area);
  }
  
  
  public Target nearestMatch(Object service, Target client, Box2D limit) {
    for (Object o : matchesNear(service, client, limit)) return (Target) o;
    return null;
  }
  
  
  public Target nearestMatch(Object service, Target client, float range) {
    for (Object o : matchesNear(service, client, range)) return (Target) o;
    return null;
  }
  
  
  public Target randomMatchNear(Object service, Target client, Box2D limit) {
    final PresenceMap map = allMaps.get(service);
    if (map == null) return null;
    return map.pickRandomAround(client, -1, limit);
  }
  
  
  public Target randomMatchNear(Object service, Target client, float range) {
    final PresenceMap map = allMaps.get(service);
    if (map == null) return null;
    return map.pickRandomAround(client, range, null);
  }
  
  
  public Series <Target> sampleFromMaps(
    Target t, Stage world, int limit,
    Series <? extends Target> sampled, Object... keys
  ) {
    if (sampled == null) sampled = new Batch();
    else if (sampled.size() > 0) {
      for (Target o : sampled) shouldFlag(o, sampled);
    }
    for (Object key : keys) {
      sampleTargets(key, t, world, limit, sampled);
    }
    for (Target o : sampled) {
      shouldFlag(o, null);
    }
    return (Series) sampled;
  }
  
  
  public Series <Target> sampleFromMap(
    Target t, Stage world, int limit,
    Series <? extends Target> sampled, Object key
  ) {
    if (sampled == null) sampled = new Batch();
    else if (sampled.size() > 0) for (Target o : sampled) {
      shouldFlag(o, sampled);
    }
    sampleTargets(key, t, world, limit, sampled);
    for (Target o : sampled) {
      shouldFlag(o, null);
    }
    return (Series) sampled;
  }
  
  
  private Series <Target> sampleTargets(
    Object key, Target t, Stage world, int limit, Series sampled
  ) {
    for (int n = limit / 2; n-- > 0;) {
      final Target v = randomMatchNear(key, t, -1);
      if (v != t && shouldFlag(v, sampled)) sampled.add(v);
    }
    for (Object o : matchesNear(key, t, -1)) {
      if (o != t && shouldFlag((Target) o, sampled)) sampled.add(o);
      if (sampled.size() >= limit) break;
    }
    return sampled;
  }
  
  
  private static boolean shouldFlag(Target item, Series sampled) {
    if (item == null) return false;
    if (item.flaggedWith() == sampled) return false;
    item.flagWith(sampled);
    return true;
  }
}






  
  