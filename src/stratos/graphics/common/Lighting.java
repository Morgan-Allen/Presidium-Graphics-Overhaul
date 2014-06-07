


package stratos.graphics.common ;
import stratos.util.*;
import com.badlogic.gdx.graphics.g3d.Environment;
//import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
//import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;



public class Lighting {

  
  /*
  private static boolean verbose = false;
  
  final Rendering rendering;
  final Environment environment;
  final public float lightSum[] = new float[4];
  
  
  Lighting(Rendering rendering) {
    this.rendering = rendering;
    environment = new Environment();
    setup(1, 1, 1);
  }
  
  
  /**  Initialises this light based on expected rgb values, ambience ratio,
    *  and whether ambient light should complement diffuse shading (to create
    *  the appearance of naturalistic shadows.)
    */
  /*
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
      c.r * c.a, c.g * c.a, c.b * c.a, 1, -1, -1
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
  //*/
  
  
  
  
  
  //*
  private static boolean verbose = false;
  
  final Rendering rendering;
  final Environment environment;
  
  final public Colour ambient = new Colour();
  final public Colour diffuse = new Colour();
  final public Vec3D direction = new Vec3D();
  final public float lightSum[] = new float[4];
  
  
  Lighting(Rendering rendering) {
    this.rendering = rendering;
    environment = new Environment();
    setup(1, 1, 1);
  }
  //*/
  
  
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
    
    diffuse.set(r, g, b, 0.8f);
    diffuse.complement(ambient);
    ambient.a = 0.2f;
    direction.set(1, -1, -1);
    
    final Colour d = diffuse, a = ambient;
    lightSum[0] = Visit.clamp((d.r * d.a) + (a.r * a.a), 0, 1);
    lightSum[1] = Visit.clamp((d.g * d.a) + (a.g * a.a), 0, 1);
    lightSum[2] = Visit.clamp((d.b * d.a) + (a.b * a.a), 0, 1);
    lightSum[3] = 1;
    
    if (verbose) {
      I.add("\nLight sum is: ");
      for (float f : lightSum) I.add(f+" ");
    }
  }
  //*/
}







