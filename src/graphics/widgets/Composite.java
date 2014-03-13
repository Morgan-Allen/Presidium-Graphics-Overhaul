


package src.graphics.widgets;
import src.graphics.common.*;
import src.util.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.Pixmap.*;
import static com.badlogic.gdx.graphics.Texture.TextureFilter.*;




public class Composite {
  
  
  final static int MAX_CACHED = 40;
  static Table <String, Composite> recentTable = new Table();
  static Stack <Composite> recent = new Stack <Composite> ();

  
  private String tableKey;
  private Pixmap drawn;
  private Texture composed;
  
  
  public static Composite fromCache(String key) {
    final Composite cached = recentTable.get(key);
    if (cached != null) return cached;
    return cached;
  }
  
  
  public static Composite withSize(int wide, int high, String key) {
    final Composite c = new Composite();
    c.tableKey = key;
    c.drawn = new Pixmap(wide, high, Format.RGBA8888);
    
    while (recent.size() >= MAX_CACHED) {
      Composite oldest = recent.removeFirst();
      recentTable.remove(oldest.tableKey);
      oldest.drawn.dispose();
      oldest.composed.dispose();
    }
    
    recentTable.put(c.tableKey, c);
    recent.addLast(c);
    return c;
  }
  
  
  public static Composite withImage(ImageAsset image, String key) {
    final Pixmap p = image.asPixels();
    final Composite c = withSize(p.getWidth(), p.getHeight(), key);
    c.layer(image);
    return c;
  }
  
  
  public static void wipeCache() {
    for (Composite c : recent) {
      c.drawn.dispose();
      c.composed.dispose();
    }
    recent.clear();
    recentTable.clear();
  }
  
  
  
  public void layer(ImageAsset image) {
    if (image == null) return;
    if (composed != null) {
      I.complain("Cannot add layers once texture is compiled!");
    }
    Pixmap.setBlending(Blending.SourceOver);
    Pixmap.setFilter(Filter.BiLinear);
    drawn.drawPixmap(image.asPixels(), 0, 0);
  }
  
  
  public void layerFromGrid(
    ImageAsset image, int offX, int offY, int gridW, int gridH
  ) {
    if (image == null) return;
    if (composed != null) {
      I.complain("Cannot add layers once texture is compiled!");
    }
    final Pixmap source = image.asPixels();
    final float
      gX = (source.getWidth()  * 1f) / gridW,
      gY = (source.getHeight() * 1f) / gridH;
    Pixmap.setBlending(Blending.SourceOver);
    Pixmap.setFilter(Filter.BiLinear);
    drawn.drawPixmap(
      source,
      (int) (offX * gX), (int) (offY * gY),
      (int) gX, (int) gY,
      0, 0, drawn.getWidth(), drawn.getHeight()
    );
  }
  
  
  public Texture texture() {
    if (composed == null) {
      composed = new Texture(drawn);
      composed.setFilter(Linear, Linear);
    }
    return composed;
  }
  
  
  public void drawTo(SpriteBatch batch2D, Box2D bounds) {
    texture();
    batch2D.draw(
      composed,
      bounds.xpos(), bounds.ypos(),
      bounds.xdim(), bounds.ydim(),
      0, 1, 1, 0
    );
  }
}



