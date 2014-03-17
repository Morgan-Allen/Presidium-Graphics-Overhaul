


package src.graphics.solids;
import src.graphics.common.*;
import src.util.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.model.*;
import com.badlogic.gdx.utils.ObjectMap;



public abstract class SolidModel extends ModelAsset {
  
  
  private static boolean verbose = true;
  
  
  protected boolean compiled = false;
  protected Model gdxModel;
  protected Node modelNodes[];
  protected NodePart modelParts[];
  protected Material modelMaterials[];
  
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
  
  
  public String[] partNames() {
    if (gdxModel == null) I.complain("MODEL MUST BE COMPILED FIRST!");
    return partNames;
  }
  
  
  
  protected void compileModel(Model model) {
    this.gdxModel = model;
    
    if (verbose) I.say("\nCompiling model structure...");
    final Batch <Node> nodeB = new Batch <Node> ();
    final Batch <NodePart> partB = new Batch <NodePart> ();
    final Batch <Material> matsB = new Batch <Material> ();
    for (Node n : model.nodes) compileFrom(n, nodeB, partB, matsB);
    if (verbose) I.say("\n\n");
    
    modelNodes = nodeB.toArray(Node.class);
    int i = 0; for (Node n : modelNodes) {
      indices.put(n, i++);
    }
    modelParts = partB.toArray(NodePart.class);
    partNames = new String[modelParts.length];
    int j = 0; for (NodePart p : modelParts) {
      partNames[j] = p.meshPart.id;
      indices.put(p, j++);
    }
    modelMaterials = matsB.toArray(Material.class);
    int k = 0; for (Material m : modelMaterials) {
      indices.put(m, k++);
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
  
  
  protected int indexFor(Object o) {
    return indices.get(o);
  }
}





