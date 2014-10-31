

package stratos.util;


public class Pick <T> {
  
  T picked;
  float bestRating;
  
  
  public Pick() {
    this(null, Float.NEGATIVE_INFINITY);
  }
  
  
  public Pick(T firstPick, float initRating) {
    this.picked     = firstPick ;
    this.bestRating = initRating;
  }
  
  
  public void compare(T next, float rating) {
    if (next == null || rating <= bestRating) return;
    bestRating = rating;
    picked     = next  ;
  }
  
  
  public boolean empty() {
    return picked == null;
  }
  
  
  public T result() {
    return picked;
  }
  
  
  public float bestRating() {
    return bestRating;
  }
}
