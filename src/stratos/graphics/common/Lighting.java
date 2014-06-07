


package stratos.graphics.common ;
import stratos.util.*;



public class Lighting {
  
  
  private static boolean verbose = false;
  
  final Rendering rendering;
  final public Colour ambient = new Colour();
  final public Colour diffuse = new Colour();
  final public Vec3D direction = new Vec3D();
  final public float lightSum[] = new float[4];
  
  
  Lighting(Rendering rendering) {
    this.rendering = rendering;
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
    
    diffuse.set(r, g, b, 0.8f);
    diffuse.complement(ambient);
    ambient.a = 1 - diffuse.a;
    direction.set(1, -1, -1).normalise();
    
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
}







