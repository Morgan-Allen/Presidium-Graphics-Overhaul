

package stratos.start;
import stratos.util.*;



public abstract class Disposal {
  
  
  final static List <Disposal> toDispose = new List <Disposal> ();
  final boolean removeWithSession;
  
  
  protected Disposal(
    boolean removeWithSession
  ) {
    this.removeWithSession = removeWithSession;
    toDispose.include(this);
  }
  
  
  protected abstract void performAssetSetup();
  protected abstract void performAssetDisposal();
  
  
  protected static void performSetup() {
    if (! PlayLoop.onRenderThread()) I.complain("ONLY ON RENDER THREAD!");
    for (Disposal d : toDispose) {
      d.performAssetSetup();
    }
  }
  
  
  protected static void performDisposal() {
    for (Disposal d : toDispose) {
      d.performAssetDisposal();
      if (d.removeWithSession) toDispose.remove(d);
    }
  }
}


