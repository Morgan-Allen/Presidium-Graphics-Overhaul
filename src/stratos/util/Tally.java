


package stratos.util;
import java.util.Map;



public class Tally <K> {
  
  
  final Table <K, Float> store = new Table <K, Float> ();
  
  
  public float valueFor(K key) {
    final Float val = store.get(key);
    return val == null ? 0 : val;
  }
  
  
  public float add(float value, K key) {
    final float oldVal = valueFor(key), newVal = oldVal + value;
    if (newVal == 0) store.remove(key);
    else store.put(key, newVal);
    return newVal;
  }
  
  
  public void clear() {
    store.clear();
  }
  
  
  public Iterable <K> keys() {
    return store.keySet();
  }
  
  
  public K highestValued() {
    K highest = null;
    float bestVal = Float.NEGATIVE_INFINITY;
    
    for (Map.Entry <K, Float> e : store.entrySet()) {
      final float val = e.getValue();
      if (val > bestVal) { bestVal = val; highest = e.getKey(); }
    }
    return highest;
  }
  
  
  public int size() {
    return store.size();
  }
}

