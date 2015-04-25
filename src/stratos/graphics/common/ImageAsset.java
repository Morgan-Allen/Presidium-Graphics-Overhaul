

package stratos.graphics.common;
import stratos.start.Assets;
import stratos.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Texture.TextureFilter;



public class ImageAsset extends Assets.Loadable {
  
  
  private static Texture WHITE_TEX = null;
  private static Object NO_FILE = new Object();
  
  public static Texture WHITE_TEX() {
    //  NOTE:  This method should not be called until the main LibGDX thread
    //  has called create() on the application listener.
    if (WHITE_TEX != null) return WHITE_TEX;
    return WHITE_TEX = withColor(4, Color.WHITE);
  }
  
  public static Texture withColor(int size, Color c) {
    final Texture tex = new Texture(size, size, Pixmap.Format.RGBA8888);
    final Pixmap draw = new Pixmap (size, size, Pixmap.Format.RGBA8888);
    draw.setColor(c);
    draw.fillRectangle(0, 0, size, size);
    tex.draw(draw, 0, 0);
    return tex;
  }
  
  
  
  private String filePath;
  private boolean loaded = false;
  
  private Pixmap pixels;
  private Texture texture;
  private Colour average;
  
  
  private ImageAsset(String keyPath, String filePath, Class sourceClass) {
    super(keyPath, sourceClass, false);
    this.filePath = filePath;
  }
  
  
  public static ImageAsset fromImage(Class sourceClass, String filePath) {
    final String keyPath = "image_asset_"+filePath;
    
    final Object match = Assets.getResource(keyPath);
    if (match == NO_FILE) return null;
    if (match instanceof ImageAsset) return (ImageAsset) match;
    
    if (! Assets.exists(filePath)) {
      I.say("WARNING- NO SUCH IMAGE FILE: "+filePath);
      Assets.cacheResource(NO_FILE, keyPath);
      return null;
    }
    
    final ImageAsset asset = new ImageAsset(keyPath, filePath, sourceClass);
    Assets.cacheResource(asset, keyPath);
    return asset;
  }
  
  
  public static ImageAsset[] fromImages(
    Class sourceClass, String path, String... files
  ) {
    final ImageAsset assets[] = new ImageAsset[files.length];
    for (int i = 0; i < files.length; i++) {
      assets[i] = fromImage(sourceClass, path+files[i]);
    }
    return assets;
  }
  
  
  public Pixmap asPixels() {
    if (! loaded) I.complain("IMAGE ASSET HAS NOT LOADED!- "+filePath);
    return pixels;
  }
  
  
  public Texture asTexture() {
    if (! loaded) I.complain("IMAGE ASSET HAS NOT LOADED!- "+filePath);
    return texture;
  }
  
  
  public Colour average() {
    if (! loaded) I.complain("IMAGE ASSET HAS NOT LOADED!- "+filePath);
    return average;
  }
  
  
  
  /**  Actual loading-
    */
  private static Table <Texture, Pixmap> pixMaps = new Table();
  private static Table <Texture, Colour> averages = new Table();
  
  protected void loadAsset() {
    final Texture cached = (Texture) Assets.getResource(filePath);
    
    if (cached != null) {
      this.texture = cached;
      this.pixels = pixMaps.get(texture);
      this.average = averages.get(texture);
    }
    
    if (average == null) {
      pixels = new Pixmap(Gdx.files.internal(filePath));
      average = new Colour();
      Colour sample = new Colour();
      
      float sumAlphas = 0;
      final int wide = pixels.getWidth(), high = pixels.getHeight();
      for (Coord c : Visit.grid(0, 0, wide, high, 10)) {
        sample.setFromRGBA(pixels.getPixel(c.x, c.y));
        sumAlphas += sample.a;
        average.r += sample.r * sample.a;
        average.g += sample.g * sample.a;
        average.b += sample.b * sample.a;
      }
      
      average.r /= sumAlphas;
      average.g /= sumAlphas;
      average.b /= sumAlphas;
      average.a = 1;
      average.set(average);
      
      if (texture == null) {
        texture = new Texture(pixels);
        texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        Assets.cacheResource(texture, filePath);
      }
      averages.put(texture, average);
      pixMaps.put(texture, pixels);
    }
    loaded = true;
  }
  
  
  public static Texture getTexture(String name) {
    Texture cached = (Texture) Assets.getResource(name);
    if (cached != null) return cached;
    cached = new Texture(Gdx.files.internal(name));
    cached.setFilter(TextureFilter.Linear, TextureFilter.Linear);
    Assets.cacheResource(cached, name);
    return cached;
  }
  
  
  public boolean isLoaded() {
    return loaded;
  }
  
  
  protected void disposeAsset() {
    final Pixmap pixels = pixMaps.get(texture);
    if (pixels != null) {
      pixMaps.remove(texture);
      averages.remove(texture);
      pixels.dispose();
      texture.dispose();
    }
  }
}





