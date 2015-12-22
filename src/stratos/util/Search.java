/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.util;
import java.lang.reflect.Array;



/**  A genericised search algorithm suitable for A*, Djikstra, or other forms
  *  of pathfinding and graph navigation.
  */
public abstract class Search <T> {
  
  
  /**  Fields and constructors-
    */
  final Sorting <T> agenda = new Sorting <T> () {
    public final int compare(final T a, final T b) {
      if (a == b) return 0;
      final float aT = entryFor(a).total, bT = entryFor(b).total;
      return aT > bT ? 1 : -1;
    }
  };
  
  
  protected class Entry {
    T refers;
    float priorCost, ETA, total;
    Entry prior;
    private Object agendaRef;
  }
  

  final protected T init;
  protected int maxSearched;
  protected Batch <T> flagged = new Batch <T> ();
  
  private float totalCost = -1;
  private boolean success = false;
  private Entry bestEntry = null;
  
  
  final public static int NOT_VERBOSE = 0, VERBOSE = 1, SUPER_VERBOSE = 2;
  public int verbosity = NOT_VERBOSE;
  

  public Search(T init, int maxPathLength) {
    if (init == null) I.complain("INITIAL AGENDA ENTRY CANNOT BE NULL!");
    this.init = init;
    this.maxSearched = (maxPathLength < 0) ? -1 : (maxPathLength * 1);
  }
  
  
  
  /**  Performs the actual search algorithm.
    */
  public Search <T> doSearch() {
    final boolean canSearch = canEnter(init);
    final boolean report = verbosity > NOT_VERBOSE;
    
    if (! canSearch) {
      if (report) I.say("\nCANNOT SEARCH FROM: "+init+"!");
      return this;
    }
    if (report) I.say("\nBEGINNING SEARCH FROM "+init+"!");
    
    tryEntry(init, null, 0);
    while (agenda.size() > 0) if (! stepSearch()) break;
    for (T t : flagged) setEntry(t, null);
    return this;
  }
  
  
  protected boolean stepSearch() {
    final boolean report = verbosity > NOT_VERBOSE;
    
    if (maxSearched > 0 && flagged.size() > maxSearched) {
      if (report) I.say("  Reached maximum search size ("+maxSearched+")");
      return false;
    }
    final Object nextRef = agenda.leastRef();
    final T next = agenda.refValue(nextRef);
    agenda.deleteRef(nextRef);
    
    if (report) I.say("\nBEST ENTRY IS: "+next);
    if (endSearch(next)) {
      success = true;
      bestEntry = entryFor(next);
      totalCost = bestEntry.total;
      if (report) I.say(
        "  Search complete at "+next+", total cost: "+totalCost+
        " all searched: "+flagged.size()
      );
      return false;
    }
    
    final T allNear[] = adjacent(next);
    if (report) I.say("  ALL ADJACENT: "+I.list(allNear));
    for (T near : allNear) if (near != null) {
      tryEntry(near, next, cost(next, near));
    }
    if (report && verbosity > VERBOSE) {
      I.say("\nAgenda afterward: (origin is "+init+")");
      for (T t : agenda) {
        final Entry e = entryFor(t);
        I.say("  ["+t+"]");
        if (e != null) I.add(" "+e.total+" ("+
          I.shorten(e.priorCost, 1)+"+"+I.shorten(e.ETA, 1)+
        ")");
      }
      I.add("");
    }
    return true;
  }
  
  
  protected void tryEntry(T spot, T prior, float cost) {
    if (cost < 0) return;
    final Entry
      oldEntry = entryFor(spot),
      priorEntry = (prior == null) ? null : entryFor(prior);
    //
    //  If a pre-existing entry for this spot already exists and is at least as
    //  efficient, ignore it.  Otherwise replace it.
    final float priorCost = cost + (prior == null ? 0 : priorEntry.priorCost);
    if (oldEntry != null) {
      if (oldEntry.priorCost <= priorCost) return;
      final Object oldRef = oldEntry.agendaRef;
      if (agenda.containsRef(oldRef)) agenda.deleteRef(oldRef);
    }
    else if (! canEnter(spot)) return;
    //
    //  Create the new entry-
    final boolean report = verbosity > VERBOSE;
    final Entry newEntry = new Entry();
    newEntry.priorCost = priorCost;
    newEntry.ETA       = estimate(spot);
    newEntry.total     = newEntry.priorCost + newEntry.ETA;
    newEntry.refers    = spot;
    newEntry.prior     = priorEntry;
    
    if (report) {
      final Entry n = newEntry;
      I.say("\nNew entry is: "+n.refers);
      I.say("  entry cost:        "+cost);
      I.say("  future cost guess: "+n.ETA);
      I.say("  total past cost:   "+n.priorCost);
      I.say("  past + future:     "+n.total);
      I.say("  last step:         "+prior);
    }
    
    //
    //  Finally, flagSprite the tile as assessed-
    setEntry(spot, newEntry);
    newEntry.agendaRef = agenda.insert(spot);
    if (oldEntry == null) flagged.add(spot);
    if (bestEntry == null || bestEntry.ETA > newEntry.ETA) {
      bestEntry = newEntry;
    }
  }
  
  
  protected abstract T[] adjacent(T spot);
  protected boolean canEnter(T spot) { return true; }
  protected abstract boolean endSearch(T best);
  protected abstract float cost(T prior, T spot);
  protected abstract float estimate(T spot);
  
  protected abstract void setEntry(T spot, Entry flag);
  protected abstract Entry entryFor(T spot);
  
  
  
  /**  Public and utility methods for getting the final path, area covered,
    *  total cost, etc. associated with the search.
    */
  protected int pathLength(T t) {
    int length = 0;
    for (Entry entry = entryFor(t); entry != null; entry = entry.prior) {
      length++;
    }
    return length;
  }
  
  
  protected T priorTo(T spot) {
    final Entry e = spot == null ? null : entryFor(spot);
    return (e == null || e.prior == null) ? null : e.prior.refers;
  }
  
  
  protected float fullCostEstimate(T spot) {
    final Entry e = spot == null ? null : entryFor(spot);
    return e == null ? 0 : e.total;
  }
  
  
  protected T[] bestPath(Entry from, Class pathClass, int limit) {
    if (bestEntry == null) return null;
    
    final Stack <T> pathStack = new Stack();
    for (Entry next = from; next != null; next = next.prior) {
      pathStack.addFirst(next.refers);
    }
    
    final int len = limit <= 0 ?
      pathStack.size() :
      Nums.min(limit, pathStack.size());
    
    final T path[] = (T[]) Array.newInstance(pathClass, len);
    int index = 0;
    for (T t : pathStack) { path[index++] = t; if (index >= len) break; }
    
    final boolean report = verbosity > NOT_VERBOSE;
    if (report) {
      I.say("\nGETTING FINAL PATH RESULT");
      I.say("  True Path size: "+pathStack.size()+" (limit "+limit+")");
      if (verbosity > VERBOSE) for (T t : path) I.say("    "+t);
    }
    return path;
  }
  
  
  public T[] bestPath(Class pathClass, int limit) {
    return bestPath(bestEntry, pathClass, limit);
  }
  
  
  public T[] fullPath(Class pathClass, int limit) {
    if (! success) return null;
    return bestPath(pathClass, limit);
  }
  
  
  public T[] fullPath(Class pathClass) {
    return fullPath(pathClass, -1);
  }
  
  
  public T[] allSearched(Class pathClass) {
    int len = flagged.size();
    T searched[] = (T[]) Array.newInstance(pathClass, len);
    for (T t : flagged) searched[--len] = t;
    return searched;
  }
  
  
  public T bestFound() {
    if (bestEntry == null) return null;
    return bestEntry.refers;
  }
  
  
  public float totalCost() {
    return totalCost;
  }
  
  
  public boolean success() {
    return success;
  }
  
  
  public int allSearchedCount() {
    return flagged.size();
  }
}







