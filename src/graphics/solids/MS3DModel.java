

package src.graphics.solids;

import src.util.*;
import src.graphics.solids.MS3DFile.*;
import src.graphics.common.*;

import java.lang.reflect.Field;
import java.util.Arrays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.data.*;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial.MaterialType;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;



public class MS3DModel extends SolidModel {
  
  
  private static boolean verbose = false;
  private String filePath, xmlPath, xmlName;
  private boolean loaded = false;
  
  private FileHandle baseDir;
  private MS3DFile file;
  private ModelData data;
  private Model gdxModel;

  private XML config = null;
  private float scale = 1.0f;
  
  
  private MS3DModel(
    String path, String fileName, Class sourceClass,
    String xmlFile, String xmlName
  ) {
    super(path+fileName, sourceClass);
    filePath = path+fileName;
    xmlPath = path+xmlFile;
    this.xmlName = xmlName;
  }
  
  
  public static MS3DModel loadFrom(
    String path, String fileName, Class sourceClass,
    String xmlFile, String xmlName
  ) {
    return new MS3DModel(path, fileName, sourceClass, xmlFile, xmlName);
  }
  
  
  //  TODO:  DISPOSE OF LATER
  public Model gdxModel() {
    return this.gdxModel;
  }
  
  
  protected void loadAsset() {
    try {
      final FileHandle fileHandle = Gdx.files.internal(filePath);
      final DataInput input = new DataInput(fileHandle.read(), true);
      baseDir = fileHandle.parent();
      file = new MS3DFile(input);
      
      if (xmlName != null) {
        XML xml = XML.load(xmlPath);
        config = xml.matchChildValue("name", xmlName);
      }
      else config = null;
    }
    catch (Exception e) {
      I.report(e);
      return;
    }
    
    this.scale = config.getFloat("scale");
    
    data = new ModelData();
    processMaterials();
    processMesh();
    processJoints();
    loadAnimRanges(config.child("animations"));
    //  TODO:  LOAD ATTACH POINTS AS WELL
    
    gdxModel = new Model(data);
    loaded = true;
  }
  
  
  public boolean isLoaded() {
    return loaded;
  }
  
  
  protected void disposeAsset() {
    gdxModel.dispose();
  }
  
  
  public Sprite makeSprite() {
    if (! loaded) I.complain("CANNOT CREATE SPRITES UNTIL LOADED!");
    return new SolidSprite(this, gdxModel);
  }
  
  
  private void processMaterials() {
    for (MS3DMaterial mat : file.materials) {
      final ModelMaterial m = new ModelMaterial();
      m.id = mat.name;
      m.ambient  = color(mat.ambient);
      m.diffuse  = color(mat.diffuse);
      m.emissive = color(mat.emissive);
      m.specular = color(mat.specular);
      m.shininess = mat.shininess;
      m.opacity = mat.transparency;
      m.type = MaterialType.Phong;
      
      if (m.opacity == 0) {
        continue;
      }
      if (!mat.texName.isEmpty()) {
        ModelTexture tex = new ModelTexture();
        if (mat.texName.startsWith(".\\")) {
          mat.texName = mat.texName.substring(2);
        }
        tex.fileName = baseDir + "/" + mat.texName;
        tex.id = mat.texName;
        tex.usage = ModelTexture.USAGE_DIFFUSE;
        m.textures = new Array<ModelTexture>();
        m.textures.add(tex);
      }
      data.materials.add(m);
    }
    if (data.materials.size == 0) {
      final ModelMaterial mat = new ModelMaterial();
      mat.ambient = new Color(0.8f, 0.8f, 0.8f, 1f);
      mat.diffuse = new Color(0.2f, 0.2f, 0.2f, 1f);
      mat.id = "default";
      data.materials.add(mat);
    }
  }
  
  
  private static Color color(float[] col) {
    return new Color(col[0], col[1], col[2], col[3]);
  }
  
  
  private void processMesh() {
    //
    // Initialise the mesh and fill it up with base geometry data-
    final ModelMesh mesh = new ModelMesh();
    mesh.id = "mesh";
    mesh.attributes = new VertexAttribute[] {
      VertexAttribute.Position(),
      VertexAttribute.Normal(),
      VertexAttribute.TexCoords(0),
      VertexAttribute.BoneWeight(0)
    };
    final int numVerts = file.triangles.length * 3, NF = 10;
    final float[] verts = new float[numVerts * NF];
    final short[] indices = new short[numVerts * 3];
    
    int p = 0, index = 0;
    for (MS3DTriangle tri : file.triangles) {
      for (int j = 0; j < 3; j++) {
        MS3DVertex vert = file.vertices[tri.indices[j]];
        
        verts[p++] = vert.vertex[0] * this.scale;
        verts[p++] = vert.vertex[1] * this.scale;
        verts[p++] = vert.vertex[2] * this.scale;
        
        verts[p++] = tri.normals[j][0];
        verts[p++] = tri.normals[j][1];
        verts[p++] = tri.normals[j][2];
        
        verts[p++] = tri.u[j];
        verts[p++] = tri.v[j];
        
        verts[p++] = vert.boneid < 0 ? 0 : vert.boneid;
        verts[p++] = 1;
        
        indices[index] = (short) index++;
      }
    }
    mesh.vertices = verts;
    data.meshes.add(mesh);
    
    
    final ModelNode root = new ModelNode();
    root.id = "root_node";
    root.meshId = mesh.id;
    root.boneId = 0;
    root.scale = new Vector3(1, 1, 1);
    //root.scale = new Vector3(scale, scale, scale);
    
    final ModelMeshPart[] MP = new ModelMeshPart[file.groups.length];
    final ModelNodePart[] NP = new ModelNodePart[file.groups.length];
    
    int k = 0;
    for (MS3DGroup group : file.groups) {
      final ModelMeshPart meshPart = new ModelMeshPart();
      meshPart.id = group.name;
      meshPart.primitiveType = GL20.GL_TRIANGLES;
      meshPart.indices = new short[group.indices.length * 3];
      
      final short[] GI = group.indices;
      for (int i = 0; i < GI.length; i++) {
        meshPart.indices[i * 3 + 0] = file.triangles[GI[i]].indices[0];
        meshPart.indices[i * 3 + 1] = file.triangles[GI[i]].indices[1];
        meshPart.indices[i * 3 + 2] = file.triangles[GI[i]].indices[2];
      }
      
      final ModelNodePart nodePart = new ModelNodePart();
      nodePart.meshPartId = group.name;
      nodePart.materialId = file.materials[group.materialIndex].name;
      nodePart.bones = new ArrayMap <String, Matrix4> ();

      MP[k] = meshPart;
      NP[k] = nodePart;
      k++;
    }
    mesh.parts = MP;
    root.parts = NP;

    data.nodes.add(root);
  }
  
  
  private void processJoints() {
    final ModelNode root = data.nodes.get(0);
    final ModelMesh baseMesh = data.meshes.get(0);
    final ModelAnimation animation = new ModelAnimation();
    animation.id = AnimNames.FULL_RANGE;
    final ArrayMap<String, ModelNode> lookup = new ArrayMap(32);
    
    for (int i = 0; i < file.joints.length; i++) {
      final MS3DJoint fileJoint = file.joints[i];
      for (ModelNodePart part : root.parts) {
        part.bones.put(fileJoint.name, new Matrix4());
      }
      
      final ModelNode node = new ModelNode();
      node.id = fileJoint.name;
      node.meshId = baseMesh.id;
      node.rotation = fileJoint.matrix.getRotation(new Quaternion());
      node.translation = fileJoint.matrix.getTranslation(new Vector3());
      node.translation.scl(this.scale);
      node.scale = new Vector3(1, 1, 1);
      lookup.put(node.id, node);
      
      final ModelNode parent = fileJoint.parent == null ?
        root :
        lookup.get(fileJoint.parent.name);
      addChild(parent, node);

      final ModelNodeAnimation nodeAnim = new ModelNodeAnimation();
      nodeAnim.nodeId = node.id;

      for (int j = 0; j < fileJoint.positions.length; j++) {
        ModelNodeKeyframe frame = new ModelNodeKeyframe();
        frame.keytime = fileJoint.rotations[j].time;
        frame.translation = new Vector3(fileJoint.positions[j].data);
        frame.translation.mul(fileJoint.matrix);
        frame.translation.scl(this.scale);
        final Quaternion
          FE = MS3DFile.fromEuler(fileJoint.rotations[j].data),
          JM = fileJoint.matrix.getRotation(new Quaternion());
        frame.rotation = JM.mul(FE);
        nodeAnim.keyframes.add(frame);
      }
      animation.nodeAnimations.add(nodeAnim);
    }
    
    data.animations.add(animation);
  }
  
  //  TODO:  Dispose of later?
  private static void addChild(ModelNode parent, ModelNode child) {
    if (parent.children == null) {
      parent.children = new ModelNode[] { child };
    }
    else {
      parent.children = Arrays.copyOf(
        parent.children, parent.children.length + 1
      );
      parent.children[parent.children.length - 1] = child;
    }
  }
  
  
  
  /**  Animation processing...
    */
  private void loadAnimRanges(XML anims) {
    if (anims == null || anims.numChildren() < 0) return;
    final ModelAnimation animation = data.animations.get(0);
    
    
    addLoop: for (XML anim : anims.children()) {
      //
      // First, check to ensure that this animation has an approved name:
      final String name = anim.value("name");
      if (! Sprite.isValidAnimName(name)) I.say(
        "WARNING: ANIMATION WITH IRREGULAR NAME: "+name+
        " IN MODEL: "+filePath
      );
      for (ModelAnimation oldAnim : data.animations) {
        if (oldAnim.id.equals(name)) continue addLoop;
      }
      
      // Either way, define the data-
      final float
        animStart  = Float.parseFloat(anim.value("start")),
        animEnd    = Float.parseFloat(anim.value("end")),
        animLength = Float.parseFloat(anim.value("duration"));
      
      final ModelAnimation modelAnim = new ModelAnimation();
      modelAnim.id = name;
      // scaling for exact duration
      final float scale = animLength / (animEnd - animStart);
      
      for (ModelNodeAnimation node : animation.nodeAnimations) {
        final ModelNodeAnimation nodeAnim = new ModelNodeAnimation();
        nodeAnim.nodeId = node.nodeId;
        
        for (ModelNodeKeyframe frame : node.keyframes) {
          if (frame.keytime >= animStart && frame.keytime <= animEnd) {
            final ModelNodeKeyframe copy = copy(frame);
            
            // trimming the beggining and scaling
            copy.keytime -= animStart;
            copy.keytime *= scale;
            nodeAnim.keyframes.add(copy);
          }
        }
        modelAnim.nodeAnimations.add(nodeAnim);
      }
      data.animations.add(modelAnim);
    }
  }
  
  
  private static ModelNodeKeyframe copy(ModelNodeKeyframe frame) {
    final ModelNodeKeyframe copy = new ModelNodeKeyframe();
    copy.keytime = frame.keytime;
    copy.rotation = frame.rotation.cpy();
    copy.translation = frame.translation.cpy();
    return copy;
  }
  
  
  //  TODO:  Consider caching this?  ...Or do I need it at all?
  public String[] groupNames() {
    final String names[] = new String[file.groups.length];
    for (int i = names.length ; i-- > 0;) {
      names[i] = file.groups[i].name;
    }
    return names;
  }
}









//private List <AnimRange> animRanges = new List<AnimRange>();


/*
private static class AnimRange {
  String name;
  float start, end, length;
  private ModelAnimation anim = null;
}
//*/

/*
private void applyToMatchingRanges(ModelNodeKeyframe frame, String boneName) {
  final float time = frame.keytime;
  // I.say("  Looking for match for keyframe at: "+time) ;
  for (AnimRange range : animRanges) {
    // I.say("    Start/end are "+range.start+"/"+range.end) ;
    if (range.start <= time && range.end >= time) {
      final ModelNodeAnimation MNA = getMNA(range, boneName);
      if (MNA != null)
        MNA.keyframes.add(frame);
    }
  }
}
//*/


/*
private ModelNodeAnimation getMNA(AnimRange range, String boneName) {
  if (range.anim == null) {
    range.anim = new ModelAnimation();
    range.anim.id = range.name;
  }
  for (ModelNodeAnimation MNA : range.anim.nodeAnimations) {
    if (MNA.nodeId.equals(boneName))
      return MNA;
  }
  final ModelNodeAnimation MNA = new ModelNodeAnimation();
  MNA.nodeId = boneName;
  range.anim.nodeAnimations.add(MNA);
  return MNA;
}
//*/




/*
private void processXMLConfig() {
  if (config == null) return;
  this.scale = config.getFloat("scale");

  loadAnimRanges(config.child("animations"));
  if (verbose) {
    I.say("\nAnimation ranges are: ");
    for (AnimRange range : animRanges) {
      I.say("  "+range.name+" ("+range.start+" to "+range.end+")");
    }
  }
}
//*/
