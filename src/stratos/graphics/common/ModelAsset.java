


package stratos.graphics.common;
import java.io.*;

import stratos.start.Assets;
import stratos.util.*;


//
//  This is intended for use by external simulation classes, so that they can
//  maintain a static/constant reference to a graphical resource (i.e, a sprite
//  model) while it's still loading on another thread, and without blocking
//  any main-loop code that refers to the class.
//
//  This should also provide some convenience methods for saving/loading
//  sprites of a given model type, and caching the model in case of multiple
//  references.



public abstract class ModelAsset extends Assets.Loadable {
  
  
  protected ModelAsset(String modelName, Class sourceClass) {
    super(modelName, sourceClass, false);
  }
  
  
  public abstract Sprite makeSprite();
  
  
  public static void saveSprite(
      Sprite sprite, DataOutputStream out
  ) throws Exception {
    if (sprite == null) { Assets.saveReference(null, out); return; }
    final ModelAsset model = sprite.model();
    if (model == null) I.complain("Sprite must have model!");
    Assets.saveReference(model, out);
    sprite.saveTo(out);
  }
  
  
  public static Sprite loadSprite(
      DataInputStream in
  ) throws Exception {
    final ModelAsset model = (ModelAsset) Assets.loadReference(in);
    if (model == null) return null;
    final Sprite sprite = model.makeSprite();
    sprite.loadFrom(in);
    return sprite;
  }
}





