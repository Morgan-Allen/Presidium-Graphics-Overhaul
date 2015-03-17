


package stratos.graphics.widgets;
import stratos.graphics.common.*;
import stratos.start.Assets;
import stratos.util.*;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Pixmap.*;

import static com.badlogic.gdx.graphics.Texture.TextureFilter.*;



//  TODO:  There's a potential fail-condition here in situations where more
//  than the MAX_CACHED number of composites are supposed to be displayed on-
//  screen at once.  Not likely, but possible.  Find a more graceful way to
//  handle this.

public class Composite {
  
  
  private static boolean
    verbose = true ;
  
  final static int MAX_CACHED = 40;
  static Table <String, Composite> recentTable = new Table();
  static Stack <Composite> recent = new Stack <Composite> ();
  
  
  final static Assets.Loadable DISPOSAL = new Assets.Loadable(
    "COMPOSITE_DISPOSAL", Composite.class, true
  ) {
    protected void loadAsset() {}
    public boolean isLoaded() { return true; }
    
    protected void disposeAsset() {
      I.say("DISPOSING OF COMPOSITES");
      for (Composite c : recent) c.dispose();
      recent.clear();
      recentTable.clear();
      
      Assets.registerForLoading(this);
    }
  };
  
  
  private String tableKey;
  private Pixmap drawn;
  private Texture composed;
  private boolean disposed = false;
  
  
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
      oldest.dispose();
    }
    
    recentTable.put(c.tableKey, c);
    recent.addLast(c);
    return c;
  }
  
  
  public static Composite withImage(ImageAsset image, String key) {
    final Composite cached = recentTable.get(key);
    if (cached != null) return cached;
    final Pixmap p = image.asPixels();
    final Composite c = withSize(p.getWidth(), p.getHeight(), key);
    c.layer(image);
    return c;
  }
  
  
  private void dispose() {
    if (disposed) return;
    if (verbose) I.say("\nDISPOSING OF COMPOSITE: "+tableKey);
    
    drawn.dispose();
    if (composed != null) composed.dispose();
    disposed = true;
  }
  
  
  
  public void layer(ImageAsset image) {
    if (image == null || disposed) return;
    if (composed != null) {
      I.complain("Cannot add layers once texture is compiled!");
    }
    Pixmap.setBlending(Blending.SourceOver);
    Pixmap.setFilter(Filter.BiLinear);
    drawn.drawPixmap(image.asPixels(), 0, 0);
  }
  
  
  public void layerInBounds(
    Composite image, float x, float y, float w, float h
  ) {
    if (image == null || disposed) return;
    if (composed != null) {
      I.complain("Cannot add layers once texture is compiled!");
    }
    final Pixmap source = image.drawn;
    final float dw = drawn.getWidth(), dh = drawn.getHeight();
    
    drawn.drawPixmap(
      source,
      1, 1, source.getWidth(), source.getHeight(),
      1 + (int) (dw * x), 1 + (int) (dh * y), (int) (dw * w), (int) (dh * h)
    );
  }
  
  
  public void layerFromGrid(
    ImageAsset image, int offX, int offY, int gridW, int gridH
  ) {
    if (image == null || disposed) return;
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
    if (disposed) return null;
    if (composed == null) {
      composed = new Texture(drawn);
      composed.setFilter(Linear, Linear);
    }
    return composed;
  }
  
  
  public Image delayedImage(HUD UI) {
    final Image image = new Image(UI, ImageAsset.WHITE_TEX()) {
      protected void renderTex(Texture tex, float alpha, WidgetsPass pass) {
        tex = texture();
        super.renderTex(tex, alpha, pass);
      }
    };
    return image;
  }
  
  
  public void drawTo(WidgetsPass pass, Box2D bounds, float alpha) {
    if (disposed) return;
    texture();
    pass.draw(
      composed, Colour.transparency(alpha),
      bounds.xpos(), bounds.ypos(),
      bounds.xdim(), bounds.ydim(),
      0, 1, 1, 0
    );
  }
}






