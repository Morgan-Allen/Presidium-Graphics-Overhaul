


package stratos.graphics.common ;

import stratos.util.I;
import stratos.util.Visit;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;



public class Lighting {
  
  
  private static boolean verbose = false;
  
  final Rendering rendering;
  final Environment environment;
  final float lightSum[] = new float[4];
  
  
  Lighting(Rendering rendering) {
    this.rendering = rendering;
    environment = new Environment();
    setup(1, 1, 1);
  }
  
  
  /**  Initialises this light based on expected rgb values, ambience ratio,
    *  and whether ambient light should complement diffuse shading (to create
    *  the appearance of naturalistic shadows.)
    */
  public void setup(
    float r,
    float g, 
    float b
  ) {
    if (verbose) I.add("\n\nRGB are: "+r+" "+g+" "+b);
    
    final Colour c = new Colour().set(r, g, b, 0.8f);
    final Colour s = c.complement(null);
    s.a = 0.2f;
    
    if (verbose) I.add("\nLight is: "+c);
    if (verbose) I.add("\nComplement: "+s);
    
    environment.directionalLights.clear();
    environment.add(new DirectionalLight().set(
      c.r * c.a, c.g * c.a, c.b * c.a, 1, 1, 1
    ));
    environment.set(new ColorAttribute(
      ColorAttribute.AmbientLight,
      s.r, s.g, s.b, s.a
    ));
    
    lightSum[0] = Visit.clamp((c.r * c.a) + (s.r * s.a), 0, 1);
    lightSum[1] = Visit.clamp((c.g * c.a) + (s.g * s.a), 0, 1);
    lightSum[2] = Visit.clamp((c.b * c.a) + (s.b * s.a), 0, 1);
    lightSum[3] = 1;
    
    if (verbose) I.add("\nLight sum is: ");
    if (verbose) for (float f : lightSum) I.add(f+" ");
  }
  
  
  public float[] lightSum() {
    return lightSum;
  }
}


