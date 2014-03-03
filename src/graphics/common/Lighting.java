


package src.graphics.common ;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;



public class Lighting {
  
  
  final Rendering rendering;
  final Environment environment;
  
  
  Lighting(Rendering rendering) {
    this.rendering = rendering;
    
    environment = new Environment();
    environment.add(new DirectionalLight().set(
      0.8f, 0.8f, 0.8f,
      -1f, -0.8f, -0.2f
    ));
    environment.set(new ColorAttribute(
      ColorAttribute.AmbientLight,
      0.4f, 0.4f, 0.4f, 0.1f
    ));
  }
  
  
  //TODO:  Make sure this modifies the environment class.
  
  
  /**  Initialises this light based on expected rgb values, ambience ratio,
    *  and whether ambient light should complement diffuse shading (to create
    *  the appearance of naturalistic shadows.)
    */
  //TODO:  Create variant method that deals with Colours directly.
  public void setup(
    float r,
    float g, 
    float b,
    //float brightness,
    //float ambience,
    boolean shadow,
    boolean global
  ) {
    
    /*
    float weigh = 0.8f ;//brightness * (1 - ambience) ;
    diffused[0] = this.r = r * weigh ;
    diffused[1] = this.g = g * weigh ;
    diffused[2] = this.b = b * weigh ;
    weigh = 0.1f ;//brightness * ambience ;
    if (shadow) {
      ambience[0] = weigh * (g + b) / 2 ;
      ambience[1] = weigh * (r + b) / 2 ;
      ambience[2] = weigh * (r + g) / 2 ;  //set to complementary colour.
    }
    else {
      ambience[0] = r * weigh ;
      ambience[1] = g * weigh ;
      ambience[2] = b * weigh ;
    }
    ambience[3] = diffused[3] = (global) ? 0 : 1 ;
    //*/
  }
}



