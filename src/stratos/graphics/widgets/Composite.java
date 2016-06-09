/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.widgets;
import stratos.start.*;
import stratos.graphics.common.*;
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
    verbose = false;
  
  final static int MAX_CACHED = 40;
  static Table <String, Composite> recentTable = new Table();
  static Stack <Composite> recent = new Stack <Composite> ();
  
  
  final static Assets.Loadable DISPOSAL = new Assets.Loadable(
    "COMPOSITE_DISPOSAL", Composite.class, true
  ) {
    protected State loadAsset() { return state = State.LOADED; }
    public boolean stateLoaded() { return true; }
    
    protected State disposeAsset() {
      if (verbose) I.say("DISPOSING OF COMPOSITES");
      for (Composite c : recent) c.dispose();
      recent.clear();
      recentTable.clear();
      
      Assets.registerForLoading(this);
      return state = State.LOADED;
    }
  };
  
  
  private String tableKey;
  private Pixmap drawn;
  private Texture composed;
  private boolean disposed = false;
  
  
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
  

  public void layerFromGrid(
    ImageAsset image, int offX, int offY, int gridW, int gridH
  ) {
    layerInBoundsRoot(
      image, offX, offY, gridW, gridH, 0, 0, 1, 1
    );
  }
  

  public void layerInBounds(
    Composite image, float x, float y, float w, float h
  ) {
    layerInBoundsRoot(image, 0, 0, 1, 1, x, y, w, h);
  }
  
  
  private void layerInBoundsRoot(
    Object image,
    int offX, int offY, int gridW, int gridH,  //  Source grid coords
    float x, float y, float w, float h         //  Destination relative coords
  ) {
    if (image == null || disposed) return;
    if (composed != null) {
      I.complain("Cannot add layers once texture is compiled!");
    }
    Pixmap source = null;
    if (image instanceof Composite ) source = ((Composite ) image).drawn;
    if (image instanceof ImageAsset) source = ((ImageAsset) image).asPixels();
    
    final float
      sX = (source.getWidth () * 1f) / gridW,
      sY = (source.getHeight() * 1f) / gridH,
      dW = drawn.getWidth(),
      dH = drawn.getHeight();
    
    Pixmap.setBlending(Blending.SourceOver);
    Pixmap.setFilter  (Filter  .BiLinear  );
    drawn.drawPixmap(
      source,
      //  Source coordinates (x/y/w/h)
      (int) (offX * sX), (int) (offY * sY), (int) sX, (int) sY,
      //  Destination coordinates (x/y/w/h)
      1 + (int) (dW * x), 1 + (int) (dH * y), (int) (dW * w), (int) (dH * h)
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
  
  
  
  /**  Various utility methods for caching and production purposes-
    */
  public static Composite fromCache(String key) {
    final Composite cached = recentTable.get(key);
    if (cached != null) return cached;
    return cached;
  }
  
  
  public static void wipeCache(String key) {
    final Composite cached = recentTable.get(key);
    if (cached == null) return;
    recent.remove(cached);
    recentTable.remove(key);
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
  
  
  public Image delayedImage(HUD UI, final String widgetID) {
    final Image image = new Image(UI, Image.SOLID_WHITE) {
      protected void render(WidgetsPass pass) {
        this.texture = texture();
        super.render(pass);
      }
    };
    image.setWidgetID(widgetID);
    return image;
  }
  
  
  public static Composite imageWithCornerInset(
    ImageAsset back, Composite corner, int size, String key
  ) {
    final Composite c = Composite.withSize(size, size, key);
    c.layerFromGrid(back  , 0   , 0   , 1   , 1   );
    c.layerInBounds(corner, 0.1f, 0.1f, 0.4f, 0.4f);
    return c;
  }
}












