

package src.graphics.common;
import src.util.*;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;




public class ImageAsset extends Assets.Loadable {
  
  
  private String filePath;
  private boolean loaded = false;
  
  private Texture texture;
  
  
  private ImageAsset(String filePath, Class sourceClass) {
    super("image_asset_"+filePath, sourceClass);
    this.filePath = filePath;
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
    if (! loaded) I.complain("IMAGE ASSET HAS NOT LOADED!");
    return texture;
  }
  
  
  
  protected void loadAsset() {
    texture = Assets.getTexture(filePath);
    loaded = true;
  }
  
  
  public boolean isLoaded() {
    return loaded;
  }
  
  
  protected void disposeAsset() {
    texture.dispose();
  }
}




