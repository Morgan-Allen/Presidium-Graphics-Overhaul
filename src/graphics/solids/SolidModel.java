


package src.graphics.solids;
import src.graphics.common.*;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.math.*;



public abstract class SolidModel extends ModelAsset {
  
  
  
  
  public SolidModel(String modelName, Class sourceClass) {
    super(modelName, sourceClass);
  }
  
  
  
  static class Group {
    Mesh vertices;
    Texture skin;
  }
  
  
  static class Joint {
    Joint parent;
    Matrix4 transform;
    Keyframe posFrames[], rotFrames[];
  }
  
  
  static class Keyframe {
    float time;
    Matrix4 transform;
  }
  
  
  
  public abstract String[] groupNames();
}




