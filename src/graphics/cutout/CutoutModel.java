

package graphics.cutout ;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.* ;
import com.badlogic.gdx.graphics.g2d.* ;
import com.badlogic.gdx.math.* ;
import com.badlogic.gdx.utils.* ;



public class CutoutModel {
  
  
  Texture texture ;
  TextureRegion region ;
  Vector2 offset, dimension ;
  
  
  //  TODO:  You might want to cache this here as well.  At least prior to
  //  transformation.
  //  float vertices[] ;
  
  
  public static CutoutModel fromImage(String fileName, int size, int height) {
    final CutoutModel model = new CutoutModel() ;
    
    model.texture = new Texture(Gdx.files.internal(fileName)) ;
    model.region = new TextureRegion(model.texture, 0, 0, 1.0f, 1.0f) ;
    model.offset = new Vector2(0, 0) ;
    model.dimension = new Vector2(size, height) ;
    
    return model ;
  }
}





