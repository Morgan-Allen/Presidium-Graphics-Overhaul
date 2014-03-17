


package src.graphics.cutout;
import src.graphics.common.*;
import src.util.*;

import java.io.*;
import java.util.Iterator;



public class GroupSprite extends Sprite {
  
  
  final static ModelAsset GROUP_MODEL = new ModelAsset(
    "GROUP-MODEL", GroupSprite.class
  ) {
    public Sprite makeSprite() { return new GroupSprite(); }
    public boolean isLoaded() { return true; }
    protected void loadAsset() {}
    protected void disposeAsset() {}
  };
  private static boolean verbose = false;
  
  
  protected Stack <Sprite> modules = new Stack <Sprite> () ;
  protected Stack <Vec3D>  offsets = new Stack <Vec3D>  () ;
  private Vec3D lastPosition = null;
  
  
  public GroupSprite() {}
  
  
  public void loadFrom(DataInputStream in) throws Exception {
    super.loadFrom(in) ;
    final int numMods = in.readInt() ;
    for (int i = numMods ; i-- > 0 ;) {
      final Sprite sprite = ModelAsset.loadSprite(in) ;
      final Vec3D off = new Vec3D().loadFrom(in) ;
      modules.addLast(sprite) ;
      offsets.addLast(off) ;
    }
  }
  
  
  public void saveTo(DataOutputStream out) throws Exception {
    super.saveTo(out) ;
    out.writeInt(modules.size()) ;
    final Iterator <Sprite> overMods = modules.iterator() ;
    final Iterator <Vec3D> overOffs = offsets.iterator() ;
    while (overMods.hasNext()) {
      ModelAsset.saveSprite(overMods.next(), out) ;
      overOffs.next().saveTo(out) ;
    }
  }
  
  
  public ModelAsset model() { return GROUP_MODEL; }
  
  
  
  /**  Actual content modification-
    */
  public void attach(Sprite sprite, float xoff, float yoff, float zoff) {
    if (!
      ((sprite instanceof GroupSprite) ||
      (sprite instanceof CutoutSprite))
    ) I.complain("Can only attach cutouts or other group-sprites!");
    modules.addLast(sprite);
    offsets.addLast(new Vec3D(xoff, yoff, zoff));
    lastPosition = null;
  }
  
  
  public void clearAllAttachments() {
    modules.clear() ;
    offsets.clear() ;
    lastPosition = null;
  }
  
  
  public void detach(Sprite sprite) {
    final int index = modules.indexOf(sprite) ;
    if (index == -1) return ;
    modules.removeIndex(index) ;
    offsets.removeIndex(index) ;
    lastPosition = null;
  }
  
  
  public void attach(ModelAsset model, float xoff, float yoff, float zoff) {
    attach(model.makeSprite(), xoff, yoff, zoff);
  }
  
  
  public int indexOf(Sprite sprite) {
    if (sprite == null) return -1 ;
    int i = 0 ;
    for (Sprite h : modules) {
      if (h == sprite) return i ; else i++ ;
    }
    return -1 ;
  }
  
  
  public Sprite atIndex(int n) {
    if (n == -1) return null ;
    return modules.atIndex(n) ;
  }
  
  
  
  /**  Rendering and updates-
    */
  public void setAnimation(String animName, float progress) {
    for (Sprite module : modules) module.setAnimation(animName, progress) ;
  }
  

  public void update() {
    //  TODO:  Move the update-check here...
  }
  
  
  public void registerFor(Rendering rendering) {
    if (checkNeedsUpdate()) {
      final Batch <Sprite> kids = new Batch <Sprite> ();
      setOffsets(rendering, kids);
      if (verbose) I.say("Refreshing z order, kids:"+kids.size());
      compressBefore(rendering, kids, 0.1f * kids.size() / 2);
    }
    renderKids(rendering);
  }
  
  
  private void renderKids(Rendering rendering) {
    for (Sprite module : modules) {
      if (module.model() == GROUP_MODEL) {
        final GroupSprite GS = (GroupSprite) module;
        GS.renderKids(rendering);
        continue;
      }
      module.colour = colour;
      module.fog = fog;
      module.registerFor(rendering);
    }
    if (lastPosition == null) lastPosition = new Vec3D();
    lastPosition.setTo(position);
  }
  
  
  private boolean checkNeedsUpdate() {
    for (Sprite module : modules) if (module.model() == GROUP_MODEL) {
      if (((GroupSprite) module).checkNeedsUpdate()) return true;
    }
    if (lastPosition == null) return true;
    if (position.x != lastPosition.x) return true;
    if (position.y != lastPosition.y) return true;
    if (position.z != lastPosition.z) return true;
    return false;
  }
  
  
  private void setOffsets(Rendering rendering, Series <Sprite> kids) {
    final Iterator <Vec3D> offs = offsets.iterator();
    for (Sprite module : modules) {
      final Vec3D off = offs.next();
      module.position.setTo(off).add(this.position);
      kids.add(module);
      
      if (module.model() == GROUP_MODEL) {
        ((GroupSprite) module).setOffsets(rendering, kids);
      }
    }
  }
  
  
  private void compressBefore(
    Rendering rendering, Series <Sprite> overlaid, float depthRange
  ) {
    if (overlaid.size() == 0) return;
    final float baseDepth = rendering.view.screenDepth(this.position);
    if (verbose) I.say("\nCompressing Z order, base depth: "+baseDepth);
    
    //*
    float
      minDepth = Float.POSITIVE_INFINITY,
      maxDepth = Float.NEGATIVE_INFINITY;
    for (Sprite s : overlaid) {
      rendering.view.translateToScreen(s.position);
      s.depth = s.position.z;
      minDepth = Math.min(minDepth, s.depth);
      maxDepth = Math.max(maxDepth, s.depth);
    }
    if (verbose) I.say("  True depth range: "+(maxDepth - minDepth));
    final float margin = depthRange / overlaid.size();
    //*/
    
    int i = 0 ;for (Sprite s : overlaid) {
      final float relDepth = (s.depth - minDepth) / (maxDepth - minDepth);
      //final float relDepth = ++i * depthRange / overlaid.size();
      s.position.z = baseDepth - (margin + (depthRange * (1 - relDepth)));
      rendering.view.translateFromScreen(s.position);
    }
  }
  
  
}






