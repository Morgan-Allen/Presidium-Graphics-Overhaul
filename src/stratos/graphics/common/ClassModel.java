

package stratos.graphics.common;
//import stratos.start.ModelAsset;


public abstract class ClassModel extends ModelAsset {
  
  public ClassModel(String modelName, Class sourceClass) {
    super(sourceClass, modelName);
  }

  public Object sortingKey() { return this; }
  
  public boolean stateLoaded  () { return true; }
  public boolean stateDisposed() { return true; }
  
  protected State loadAsset   () { return state = State.LOADED  ; }
  protected State disposeAsset() { return state = State.DISPOSED; }
}