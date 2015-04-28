

package stratos.graphics.common;
//import stratos.start.ModelAsset;


public abstract class ClassModel extends ModelAsset {
  
  public ClassModel(String modelName, Class sourceClass) {
    super(modelName, sourceClass);
  }
  public boolean isLoaded() { return true; }
  public boolean isDisposed() { return true; }
  protected void loadAsset() {}
  protected void disposeAsset() {}
  
}