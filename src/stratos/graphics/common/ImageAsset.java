/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.common;
import stratos.start.Assets;
import stratos.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Texture.TextureFilter;



public class ImageAsset extends Assets.Loadable {
  
  
  private static Object NO_FILE = new Object();
  
  private String  filePath;
  private Pixmap  pixels  ;
  private Texture texture ;
  private Colour  average ;
  
  
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
    asset.setKeyFile(filePath);
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
    if (! stateLoaded()) Assets.loadNow(this);
    if (! stateLoaded()) I.complain("IMAGE ASSET HAS NOT LOADED!- "+filePath);
    return pixels;
  }
  
  
  public Texture asTexture() {
    if (! stateLoaded()) Assets.loadNow(this);
    if (! stateLoaded()) I.complain("IMAGE ASSET HAS NOT LOADED!- "+filePath);
    return texture;
  }
  
  
  public Colour average() {
    if (! stateLoaded()) Assets.loadNow(this);
    if (! stateLoaded()) I.complain("IMAGE ASSET HAS NOT LOADED!- "+filePath);
    return average;
  }
  
  
  
  /**  Actual loading-
    */
  final static String
    TEX_PREFIX = "texture_for_",
    PIX_PREFIX = "pixels_for_" ,
    AVG_PREFIX = "average_for_";
  
  
  protected State loadAsset() {
    Texture texture = (Texture) Assets.getResource(TEX_PREFIX+filePath);
    Pixmap pixels   = (Pixmap ) Assets.getResource(PIX_PREFIX+filePath);
    Colour average  = (Colour ) Assets.getResource(AVG_PREFIX+filePath);
    return loadAsset(texture, pixels, average);
  }
  
  
  protected State loadAsset(
    Texture withTexture, Pixmap withPixels, Colour withAverage
  ) {
    
    if (withPixels != null) {
      this.pixels = withPixels;
    }
    else {
      pixels = new Pixmap(Gdx.files.internal(filePath));
      Assets.cacheResource(pixels, PIX_PREFIX+filePath);
    }
    
    if (withAverage != null) {
      this.average = withAverage;
    }
    else {
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
      Assets.cacheResource(average, AVG_PREFIX+filePath);
    }
    
    if (withTexture != null) {
      this.texture = withTexture;
    }
    else {
      texture = new Texture(pixels);
      texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
      Assets.cacheResource(texture, TEX_PREFIX+filePath);
    }
    
    return state = State.LOADED;
  }
  
  
  public static Texture getTexture(String fileName) {
    Texture cached = (Texture) Assets.getResource(TEX_PREFIX+fileName);
    if (cached != null) return cached;
    cached = new Texture(Gdx.files.internal(fileName));
    cached.setFilter(TextureFilter.Linear, TextureFilter.Linear);
    Assets.cacheResource(cached, TEX_PREFIX+fileName);
    return cached;
  }
  
  
  protected State disposeAsset() {
    Texture texture = (Texture) Assets.getResource(TEX_PREFIX+filePath);
    Pixmap pixels   = (Pixmap ) Assets.getResource(PIX_PREFIX+filePath);
    Colour average  = (Colour ) Assets.getResource(AVG_PREFIX+filePath);
    
    if (pixels != null) {
      Assets.clearCachedResource(PIX_PREFIX+filePath);
      pixels.dispose();
    }
    if (average != null) {
      Assets.clearCachedResource(AVG_PREFIX+filePath);
    }
    if (texture != null) {
      Assets.clearCachedResource(TEX_PREFIX+filePath);
      texture.dispose();
    }
    return state = State.DISPOSED;
  }
  
  
  
  /**  Utility method for creating static constants-
    */
  public static ImageAsset withColor(final int size, Colour c, Class source) {
    final Color gdxColor = new Color(c.r, c.g, c.b, c.a);
    final ImageAsset asset = new ImageAsset("IMAGE_ASSET_", c+"", source) {
      protected State loadAsset() {
        final Texture tex = new Texture(size, size, Pixmap.Format.RGBA8888);
        final Pixmap draw = new Pixmap (size, size, Pixmap.Format.RGBA8888);
        draw.setColor(gdxColor);
        draw.fillRectangle(0, 0, size, size);
        tex.draw(draw, 0, 0);
        return loadAsset(tex, draw, null);
      }
    };
    return asset;
  }
}




