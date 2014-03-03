/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.util ;
import java.lang.reflect.Array ;



/**  A genericised search algorithm suitable for A*, Djikstra, or other forms
  *  of pathfinding and graph navigation.
  */
public abstract class Search <T> {
  
  
  /**  Fields and constructors-
    */
  final Sorting <T> agenda = new Sorting <T> () {
    public final int compare(final T a, final T b) {
      if (a == b) return 0 ;
      final float aT = entryFor(a).total, bT = entryFor(b).total ;
      return aT > bT ? 1 : -1 ;
    }
  } ;
  
  
  protected class Entry {
    T refers ;
    float priorCost, ETA, total ;
    Entry prior ;
    private Object agendaRef ;
  }
  

  final protected T init ;
  final protected int maxSearched ;
  protected Batch <T> flagged = new Batch <T> () ;
  
  private float totalCost = -1 ;
  private boolean success = false ;
  private Entry bestEntry = null ;
  
  public boolean verbose = false ;
  

  public Search(T init, int maxPathLength) {
    if (init == null) I.complain("INITIAL AGENDA ENTRY CANNOT BE NULL!") ;
    this.init = init ;
    this.maxSearched = (maxPathLength < 0) ? -1 : (maxPathLength * 1) ;
  }
  
  
  
  /**  Performs the actual search algorithm.
    */
  public Search <T> doSearch() {
    final boolean canSearch = canEnter(init) ;
    if (verbose) I.say("   ...searching "+canSearch) ;
    if (! canSearch) {
      if (verbose) I.say("Cannot enter "+init) ;
      return this ;
    }
    tryEntry(init, null, 0) ;
    while (agenda.size() > 0) if (! stepSearch()) break ;
    for (T t : flagged) setEntry(t, null) ;
    return this ;
  }
  
  
  protected boolean stepSearch() {
    if (maxSearched > 0 && flagged.size() > maxSearched) {
      if (verbose) I.say("Reached maximum search size ("+maxSearched+")") ;
      return false ;
    }
    final Object nextRef = agenda.leastRef() ;
    final T next = agenda.refValue(nextRef) ;
    agenda.deleteRef(nextRef) ;
    if (endSearch(next)) {
      success = true ;
      bestEntry = entryFor(next) ;
      totalCost = bestEntry.total ;
      if (verbose) I.say(
        "  ...search complete at "+next+", total cost: "+totalCost+
        " all searched: "+flagged.size()
      ) ;
      return false ;
    }
    for (T near : adjacent(next)) if (near != null) {
      tryEntry(near, next, cost(next, near)) ;
    }
    return true ;
  }
  
  
  protected void tryEntry(T spot, T prior, float cost) {
    if (cost < 0) return ;
    final Entry
      oldEntry = entryFor(spot),
      priorEntry = (prior == null) ? null : entryFor(prior) ;
    //
    //  If a pre-existing entry for this spot already exists and is at least as
    //  efficient, ignore it.  Otherwise replace it.
    final float priorCost = cost + (prior == null ? 0 : priorEntry.priorCost) ;
    if (oldEntry != null) {
      if (oldEntry.priorCost <= priorCost) return ;
      final Object oldRef = oldEntry.agendaRef ;
      if (agenda.containsRef(oldRef)) agenda.deleteRef(oldRef) ;
    }
    else if (! canEnter(spot)) return ;
    //
    //  Create the new entry-
    final Entry newEntry = new Entry() ;
    newEntry.priorCost = priorCost ;
    newEntry.ETA = estimate(spot) ;
    newEntry.total = newEntry.priorCost + newEntry.ETA ;
    newEntry.refers = spot ;
    newEntry.prior = priorEntry ;
    //
    //  Finally, flagSprite the tile as assessed-
    setEntry(spot, newEntry) ;
    newEntry.agendaRef = agenda.insert(spot) ;
    if (oldEntry == null) flagged.add(spot) ;
    if (bestEntry == null || bestEntry.ETA > newEntry.ETA) {
      bestEntry = newEntry ;
    }
    if (verbose) I.add("|") ;
  }
  
  
  protected abstract T[] adjacent(T spot) ;
  protected boolean canEnter(T spot) { return true ; }
  protected abstract boolean endSearch(T best) ;
  protected abstract float cost(T prior, T spot) ;
  protected abstract float estimate(T spot) ;
  
  protected abstract void setEntry(T spot, Entry flag) ;
  protected abstract Entry entryFor(T spot) ;
  
  
  
  /**  Public and utility methods for getting the final path, area covered,
    *  total cost, etc. associated with the search.
    */
  protected int pathLength(T t) {
    int length = 0 ;
    for (Entry entry = entryFor(t) ; entry != null ; entry = entry.prior) {
      length++ ;
    }
    return length ;
  }
  
  
  public T[] bestPath(Class pathClass) {
    if (bestEntry == null) return null ;
    final Batch <T> pathTiles = new Batch <T> () ;
    for (Entry next = bestEntry ; next != null ; next = next.prior) {
      pathTiles.add(next.refers) ;
    }
    if (verbose) I.say("Path size: "+pathTiles.size()) ;
    int len = pathTiles.size() ;
    T path[] = (T[]) Array.newInstance(pathClass, len) ;
    for (T t : pathTiles) path[--len] = t ;
    return path ;
  }
  
  
  public T[] fullPath(Class pathClass) {
    if (! success) return null ;
    return bestPath(pathClass) ;
  }
  
  
  public T[] allSearched(Class pathClass) {
    int len = flagged.size() ;
    T searched[] = (T[]) Array.newInstance(pathClass, len) ;
    for (T t : flagged) searched[--len] = t ;
    return searched ;
  }
  
  
  public T bestFound() {
    if (bestEntry == null) return null ;
    return bestEntry.refers ;
  }
  
  
  public float totalCost() {
    return totalCost ;
  }
  
  
  public boolean success() {
    return success ;
  }
}







