

package stratos.graphics.solids;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Attribute;




public class OverlayAttribute extends Attribute {
  
  
  public final static String alias = "overlay";
  public static final long Overlay = register(alias);
  
  public Texture[] textures;
  
  
  public OverlayAttribute(Texture[] texts) {
    super(Overlay);
    textures = texts;
  }
  
  
  public Attribute copy() {
    return new OverlayAttribute(textures);
  }
  
  
  protected boolean equals(Attribute that) {
    return this == that;
  }
  
  
  public int compareTo(Attribute that) {
    final int hA = this.hashCode(), hB = that.hashCode();
    return (hA > hB) ? 1 : (hA == hB ? 0 : -1);
  }

}





