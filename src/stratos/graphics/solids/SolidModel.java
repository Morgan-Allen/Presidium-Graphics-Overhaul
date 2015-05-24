/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.solids;
import stratos.graphics.common.*;
import stratos.util.*;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.*;
import com.badlogic.gdx.utils.ObjectMap;



public abstract class SolidModel extends ModelAsset {
  
  
  private static boolean verbose = false;
  
  
  protected boolean compiled = false, disposed = false;
  protected Model gdxModel;
  protected AnimControl animControl;
  
  protected Node     allNodes[];
  protected NodePart allParts[];
  protected Material allMaterials[];
  protected float    rotateOffset;
  
  private ObjectMap <Object, Integer> indices = new ObjectMap();
  private String partNames[];
  
  
  
  protected SolidModel(String modelName, Class sourceClass) {
    super(modelName, sourceClass);
  }
  
  
  protected State loadAsset() {
    return state = State.LOADED;
  }
  
  
  protected State disposeAsset() {
    if (gdxModel != null) gdxModel.dispose();
    gdxModel = null;
    return state = State.DISPOSED;
  }
  
  
  public Sprite makeSprite() {
    if (gdxModel == null) I.complain("MODEL MUST BE COMPILED FIRST!");
    return new SolidSprite(this);
  }
  
  
  protected void compileModel(Model model) {
    this.gdxModel = model;
    
    if (verbose) I.say("\nCompiling model structure...");
    final Batch <Node>     nodeB = new Batch <Node>     ();
    final Batch <NodePart> partB = new Batch <NodePart> ();
    final Batch <Material> matsB = new Batch <Material> ();
    for (Node n : model.nodes) compileFrom(n, nodeB, partB, matsB);
    if (verbose) I.say("\n\n");
    
    allNodes = nodeB.toArray(Node.class);
    int i = 0; for (Node n : allNodes) {
      indices.put(n, i++);
    }
    
    allParts = partB.toArray(NodePart.class);
    partNames = new String[allParts.length];
    int j = 0; for (NodePart p : allParts) {
      partNames[j] = p.meshPart.id;
      indices.put(p, j++);
    }
    
    allMaterials = matsB.toArray(Material.class);
    int k = 0; for (Material m : allMaterials) {
      indices.put(m, k++);
    }
    
    animControl = new AnimControl(this);
    int a = 0; for (Animation anim : gdxModel.animations) {
      indices.put(anim, a++);
    }
    
    compiled = true;
  }
  
  
  private void compileFrom(
    Node node,
    Batch <Node> nodeB, Batch <NodePart> partB, Batch <Material> matsB
  ) {
    nodeB.add(node);
    if (verbose) I.say("Node is: "+node.id);
    if (verbose && node.parent != null) I.say("  Parent is: "+node.parent.id);
    for (NodePart p : node.parts) {
      partB.add(p);
      matsB.include(p.material);
      if (verbose) I.say("  Part is: "+p.meshPart.id);
      if (verbose) I.say("  Material is: "+p.material.id);
    }
    for (Node n : node.children) compileFrom(n, nodeB, partB, matsB);
  }
  
  
  protected void loadAttachPoints(XML points) {
    if (points == null) return;
    
    for (XML point : points.children()) {
      final String
        function = point.value("function"),
        jointName = point.value("joint");
      final Node match = gdxModel.getNode(jointName);
      if (match == null) {
        I.say("WARNING: NO MATCH FOR ATTACH POINT: "+function);
        continue;
      }
      indices.put(function, indexFor(match));
    }
  }
  
  
  
  /**  Convenience methods for iteration and component reference-
    */
  protected Integer indexFor(Object o) {
    return indices.get(o);
  }
  
  
  public String[] partNames() {
    if (gdxModel == null) I.complain("MODEL MUST BE COMPILED FIRST!");
    return partNames;
  }
  
  
  public NodePart partWithName(String name) {
    for (NodePart p : allParts) if (p.meshPart.id.equals(name)) return p;
    return null;
  }
  
  
  public String materialID(String partName) {
    final NodePart match = partWithName(partName);
    if (match == null) return null;
    return match.material.id;
  }
  
  
  public Mesh meshForPart(int index) {
    return allParts[index].meshPart.mesh;
  }
  
  
  public String[] animNames() {
    if (gdxModel == null) I.complain("MODEL MUST BE COMPILED FIRST!");
    final String names[] = new String[gdxModel.animations.size];
    int i = 0;
    for (Animation anim : gdxModel.animations) names[i++] = anim.id;
    return names;
  }
  
  
  public float defaultAnimDuration(String name) {
    final Animation a = gdxModel.getAnimation(name);
    return a != null ? a.duration : 0;
  }
}







