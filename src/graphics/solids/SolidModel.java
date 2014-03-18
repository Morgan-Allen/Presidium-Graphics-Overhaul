


package src.graphics.solids;
import src.graphics.common.*;
import src.util.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.model.*;
import com.badlogic.gdx.utils.ObjectMap;



public abstract class SolidModel extends ModelAsset {
  
  
  private static boolean verbose = false;
  
  
  protected boolean compiled = false;
  protected Model gdxModel;
  protected AnimControl animControl;
  
  protected Node     allNodes[];
  protected NodePart allParts[];
  protected Material allMaterials[];
  
  private ObjectMap <Object, Integer>
    indices = new ObjectMap <Object, Integer> ();
  private String
    partNames[];
  
  
  protected SolidModel(String modelName, Class sourceClass) {
    super(modelName, sourceClass);
  }
  
  
  protected void disposeAsset() {
    if (gdxModel != null) gdxModel.dispose();
  }
  
  
  public Sprite makeSprite() {
    if (gdxModel == null) I.complain("MODEL MUST BE COMPILED FIRST!");
    return new SolidSprite(this);
  }
  
  
  
  protected void compileModel(Model model) {
    this.gdxModel = model;
    
    if (verbose) I.say("\nCompiling model structure...");
    final Batch <Node> nodeB = new Batch <Node> ();
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
  
  
  
  /**  Convenience methods for iteration and component reference-
    */
  protected int indexFor(Object o) {
    return indices.get(o);
  }
  
  
  public String[] partNames() {
    if (gdxModel == null) I.complain("MODEL MUST BE COMPILED FIRST!");
    return partNames;
  }
  
  
  protected NodePart partWithName(String name) {
    for (NodePart p : allParts) if (p.meshPart.id.equals(name)) return p;
    return null;
  }
}





