

package stratos.graphics.common;
//import stratos.start.ModelAsset;


public abstract class ClassModel extends ModelAsset {
  
  public ClassModel(String modelName, Class sourceClass) {
    super(modelName, sourceClass);
  }
  
  public boolean stateLoaded  () { return true; }
  public boolean stateDisposed() { return true; }
  
  protected State loadAsset   () { return State.LOADED  ; }
  protected State disposeAsset() { return State.DISPOSED; }
}