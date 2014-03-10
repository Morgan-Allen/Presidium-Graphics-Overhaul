

package src.graphics.common;
import src.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.*;




public class ImageAsset extends Assets.Loadable {
  
  
  final public static Texture WHITE_TEX = new Texture(
    4, 4, Pixmap.Format.RGBA8888
  );
  static {
    final Pixmap draw = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
    draw.setColor(Color.WHITE);
    draw.fillRectangle(0, 0, 4, 4);
    WHITE_TEX.draw(draw, 0, 0);
  }
  
  
  private String filePath;
  private boolean loaded = false;
  
  private Texture texture;
  private Colour average;
  
  
  private ImageAsset(String filePath, Class sourceClass) {
    super("image_asset_"+filePath, sourceClass);
    this.filePath = filePath;
    if (! Assets.exists(filePath)) I.complain("NO SUCH FILE: "+filePath) ;
    Assets.registerForLoading(this);
  }
  
  
  public static ImageAsset fromImage(String filePath, Class sourceClass) {
    return new ImageAsset(filePath, sourceClass);
  }
  
  
  public static ImageAsset[] fromImages(
    Class sourceClass, String path, String... files
  ) {
    final ImageAsset assets[] = new ImageAsset[files.length];
    for (int i = 0 ; i < files.length ; i++) {
      assets[i] = new ImageAsset(path+files[i], sourceClass);
    }
    return assets;
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
  private static Table <Texture, Colour> averages = new Table();
  
  protected void loadAsset() {
    final Texture cached = (Texture) Assets.getResource(filePath);
    if (cached != null) {
      this.texture = cached;
      this.average = averages.get(texture);
    }
    else texture = cached;
    
    if (average == null) {
      final Pixmap imgData = new Pixmap(Gdx.files.internal(filePath));
      average = new Colour();
      Colour sample = new Colour();
      
      float sumAlphas = 0;
      final int wide = imgData.getWidth(), high = imgData.getHeight();
      for (Coord c : Visit.grid(0, 0, wide, high, 1)) {
        sample.setFromRGBA(imgData.getPixel(c.x, c.y));
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
        texture = new Texture(imgData);
        texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        Assets.cacheResource(texture, filePath);
      }
      averages.put(texture, average);
      imgData.dispose();
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
    texture.dispose();
  }
}







