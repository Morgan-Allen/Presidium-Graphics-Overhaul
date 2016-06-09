/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.solids;
import stratos.graphics.common.*;
import stratos.util.*;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.*;
import com.badlogic.gdx.math.*;
import java.io.*;



public class SolidSprite extends Sprite {
  
  
  final static float
    ANIM_INTRO_TIME    = 0.20f,
    ANIM_TIME_ENDPOINT = 0.99f;
  
  private static boolean verbose = false;
  
  
  final public SolidModel model;
  final Matrix4 transform = new Matrix4();
  final Matrix4 boneTransforms[];
  final Material materials[];
  private int hideMask = 0;
  
  private static class AnimState {
    Animation anim;
    float time, incept;
  }
  final Stack <AnimState> animStates = new Stack <AnimState> ();
  
  private static Vector3 tempV = new Vector3();
  private static Matrix4 tempM = new Matrix4();
  
  
  
  protected SolidSprite(final SolidModel model) {
    this.model = model;
    if (! model.compiled) I.complain("MODEL MUST BE COMPILED FIRST!");
    
    this.boneTransforms = new Matrix4[model.allNodes.length];
    for (int i = boneTransforms.length; i-- > 0;) {
      boneTransforms[i] = new Matrix4();
    }
    
    this.materials = new Material[model.allMaterials.length];
    for (int i = materials.length; i-- > 0;) {
      materials[i] = model.allMaterials[i];
    }
    
    this.setAnimation(AnimNames.FULL_RANGE, 0, true);
  }
  
  
  public ModelAsset model() {
    return model;
  }
  
  
  protected void saveTo(DataOutputStream out) throws Exception {
    super.saveTo(out);
    
    final float AT = Rendering.activeTime();
    out.write(animStates.size());
    for (AnimState state : animStates) {
      out.writeInt  (model.indexFor(state.anim));
      out.writeFloat(state.time  );
      out.writeFloat(AT - state.incept);
    }
  }
  
  
  protected void loadFrom(DataInputStream in) throws Exception {
    super.loadFrom(in);
    
    final float AT = Rendering.activeTime();
    for (int n = in.read(); n-- > 0;) {
      final AnimState state = new AnimState();
      state.anim    = model.gdxModel.animations.get(in.readInt());
      state.time    = in.readFloat();
      state.incept  = AT - in.readFloat();
      animStates.add(state);
    }
  }
  
  
  public void readyFor(Rendering rendering) {
    
    //  Set up the translation matrix based on game-world position and facing-
    Viewport.worldToGL(position, tempV);
    transform.setToTranslation(tempV);
    transform.scl(tempV.set(scale, scale, scale));
    
    final float radians = Nums.toRadians(model.rotateOffset - rotation);
    transform.rotate(Vector3.Y, radians);
    
    model.animControl.begin(this);
    if (animStates.size() > 0) {
      //  If we're currently being animated, then we need to loop over each
      //  animation state and blend them together, while culling any that have
      //  expired-
      final float time = Rendering.activeTime();
      AnimState validFrom = animStates.first();
      for (AnimState state : animStates) {
        float alpha = (time - state.incept) / ANIM_INTRO_TIME;
        if (alpha >= 1) { validFrom = state; alpha = 1; }
        model.animControl.apply(state.anim, state.time, alpha);
      }
      while (animStates.first() != validFrom) animStates.removeFirst();
    }
    model.animControl.end();
    
    //  The nodes here are ordered so as to guarantee that parents are always
    //  visited before children, allowing a single pass-
    for (int i = 0; i < model.allNodes.length; i++) {
      final Node node = model.allNodes[i];
      if (! node.hasParent()) {
        boneTransforms[i].setToTranslation(node.translation);
        boneTransforms[i].scl(node.scale);
        continue;
      }
      final Matrix4 parentTransform = boneFor(node.getParent());
      tempM.set(parentTransform).mul(boneTransforms[i]);
      boneTransforms[i].set(tempM);
    }
    
    rendering.solidsPass.register(this);
  }
  
  
  public void setAnimation(String id, float progress, boolean loop) {
    Animation match = model.gdxModel.getAnimation(id);
    if (match == null) return;
    
    AnimState topState = animStates.last();
    if (topState == null || match != topState.anim) {
      topState        = new AnimState();
      topState.anim   = match;
      topState.incept = Rendering.activeTime();
      animStates.addLast(topState);
    }
    
    if (loop) {
      progress = Nums.clamp(progress, 0, ANIM_TIME_ENDPOINT);
      topState.time = progress * match.duration;
    }
    else {
      final float minTime = progress * match.duration;
      if (minTime > topState.time) topState.time = minTime;
    }
  }
  
  
  
  /**  Rendering and animation-
   */
  static class Part {
    SolidSprite belongs;
    
    Texture texture, overlays[];
    Colour colour;
    
    Mesh mesh;
    Matrix4 meshBones[];
    int meshType, meshIndex, meshVerts;
  }
  
  
  protected void addPartsTo(Series <Part> allParts) {
    
    final Colour c = new Colour();
    if (this.colour == null) c.set(Colour.WHITE);
    else c.set(this.colour);
    c.r *= fog;
    c.g *= fog;
    c.b *= fog;
    
    for (int i = 0; i < model.allParts.length; i++) {
      final NodePart part = model.allParts[i];
      if ((hideMask & (1 << i)) != 0) continue;
      
      final int numBones = part.invBoneBindTransforms.size;
      //  TODO:  Use an object pool for these, if possible?
      final Matrix4 boneSet[] = new Matrix4[numBones];
      for (int b = 0; b < numBones; b++) {
        final Node node = part.invBoneBindTransforms.keys[b];
        final Matrix4 offset = part.invBoneBindTransforms.values[b];
        boneSet[b] = new Matrix4(boneFor(node)).mul(offset);
      }
      
      final int matIndex = model.indexFor(part.material);
      final Material material = materials[matIndex];

      final TextureAttribute t;
      t = (TextureAttribute) material.get(TextureAttribute.Diffuse);
      final OverlayAttribute a;
      a = (OverlayAttribute) material.get(OverlayAttribute.Overlay);
      
      final Part p = new Part();
      p.belongs = this;
      p.texture = t == null ? null : t.textureDescription.texture;
      p.overlays = a == null ? null : a.textures;
      p.colour = c;
      
      p.mesh = part.meshPart.mesh;
      p.meshBones = boneSet;
      p.meshType  = part.meshPart.primitiveType;
      p.meshIndex = part.meshPart.offset;// .indexOffset;
      p.meshVerts = part.meshPart.size;// .numVertices;
      allParts.add(p);
    }
  }
  
  
  protected Matrix4 boneFor(Node node) {
    final int index = model.indexFor(node);
    return boneTransforms[index];
  }
  
  
  
  /**  Customising appearance (toggling parts, adding skins)-
    */
  public void setOverlaySkins(String partName, Texture... skins) {
    final NodePart match = model.partWithName(partName);
    if (match == null) return;
    final Material base = match.material;
    final Material overlay = new Material(base);
    overlay.set(new OverlayAttribute(skins));
    this.materials[model.indexFor(base)] = overlay;
  }
  
  
  private Material currentOverlay(String partName) {
    final String ID = model.materialID(partName);
    if (ID == null) return null;
    
    final Material base = model.gdxModel.getMaterial(ID);
    if (base == null) return null;
    
    final int index = model.indexFor(base);
    if (materials[index] == base) return null;
    
    return materials[index];
  }
  

  public boolean hasOverlay(String partName) {
    return currentOverlay(partName) != null;
  }
  

  public void clearOverlays(String partName) {
    final Material overlay = currentOverlay(partName);
    if (overlay == null) return;
    final int index = Visit.indexOf(overlay, materials);
    if (index == -1) return;
    this.materials[index] = model.allMaterials[index];
  }
  
  
  public Vec3D attachPoint(String function, Vec3D v) {
    if (v == null) v = new Vec3D();
    if (animStates.size() == 0) return v.setTo(position);
    
    final Integer nodeIndex = model.indexFor(function);
    if (nodeIndex == null) return super.attachPoint(function, v);
    
    tempV.set(0, 0, 0);
    tempV.mul(boneTransforms[nodeIndex]);
    tempV.mul(transform);
    return Viewport.GLToWorld(tempV, v);
  }
  
  
  
  /**  Showing and hiding model parts-
    */
  private void hideMask(NodePart p, boolean is) {
    final int index = model.indexFor(p);
    if (is) hideMask |= 1 << index;
    else hideMask &= ~ (1 << index);
  }
  
  
  public void hideParts(String... partIDs) {
    for (String id : partIDs) {
      togglePart(id, false);
    }
  }
  
  
  public void showOnly(String partID) {
    final Node root = model.allNodes[0];
    boolean match = false;
    for (NodePart np : root.parts) {
      if (np.meshPart.id.equals(partID)) {
        hideMask(np, false);
        match = true;
      }
      else hideMask(np, true);
    }
    if (verbose && ! match) {
      I.say("  WARNING:  No matching model part: "+partID);
    }
  }
  
  
  public void togglePart(String partID, boolean visible) {
    final Node root = model.allNodes[0];
    for (NodePart np : root.parts) {
      if (np.meshPart.id.equals(partID)) {
        hideMask(np, ! visible);
        return;
      }
    }
    if (verbose) I.say("  WARNING:  No matching model part: "+partID);
  }
}



