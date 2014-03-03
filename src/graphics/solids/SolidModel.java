


package src.graphics.solids;
import src.graphics.common.*;



public abstract class SolidModel extends ModelAsset {
  
  
  public SolidModel(String modelName, Class sourceClass) {
    super(modelName, sourceClass);
  }
  
  
  public abstract String[] groupNames();
}
