/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.solids;
import stratos.graphics.common.*;
import stratos.graphics.solids.MS3DFile.*;
import stratos.util.*;

import java.util.Arrays;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.model.data.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial.MaterialType;




public class MS3DModel extends SolidModel {
  
  
  final static boolean FORCE_DEFAULT_MATERIAL = false;
  private static boolean verbose = false;
  
  private String filePath, xmlPath, xmlName;
  private FileHandle baseDir;
  private XML config;
  private boolean loaded = false;
  
  private ModelData data;
  private ModelMesh mesh;
  private ModelNode root;
  private MS3DFile ms3d;
  

  private MS3DModel(
    String path, String fileName, Class sourceClass,
    String xmlFile, String xmlName
  ) {
    super(path+fileName, sourceClass);
    filePath = path+fileName;
    xmlPath = path+xmlFile;
    this.xmlName = xmlName;
    this.setKeyFile(filePath);
    this.setKeyFile(xmlPath );
  }
  
  
  public static MS3DModel loadFrom(
    String path, String fileName, Class sourceClass,
    String xmlFile, String xmlName
  ) {
    return new MS3DModel(path, fileName, sourceClass, xmlFile, xmlName);
  }
  
  
  protected State loadAsset() {
    try {
      final FileHandle fileHandle = Gdx.files.internal(filePath);
      final DataInput0 input = new DataInput0(fileHandle.read(), true);
      baseDir = fileHandle.parent();
      ms3d = new MS3DFile(input);
      
      if (xmlName != null) {
        XML xml = XML.load(xmlPath);
        config = xml.matchChildValue("name", xmlName);
      }
      else config = null;
      input.close();
    }
    catch (Exception e) {
      I.report(e);
      return state = State.ERROR;
    }
    
    data = new ModelData();
    processMaterials();
    processMesh();
    processJoints();
    
    super.compileModel(new Model(data));
    if (config != null) loadAttachPoints(config.child("attachPoints"));
    return super.loadAsset();
  }
  
  
  protected State disposeAsset() {
    return super.disposeAsset();
  }
  


  private void processMaterials() {
    if (! FORCE_DEFAULT_MATERIAL) for (MS3DMaterial mat : ms3d.materials) {
      ModelMaterial m = new ModelMaterial();
      m.id = mat.name;
      m.ambient   = color(mat.ambient);
      m.diffuse   = color(mat.diffuse);
      m.emissive  = color(mat.emissive);
      m.specular  = color(mat.specular);
      m.shininess = mat.shininess;
      m.opacity   = mat.transparency;
      m.type      = MaterialType.Phong;
      
      if (m.opacity == 0) {
        m.opacity = 1;
      }
      if (!mat.texture.isEmpty()) {
        ModelTexture tex = new ModelTexture();
        if (mat.texture.startsWith(".\\") || mat.texture.startsWith("//")) {
          mat.texture = mat.texture.substring(2);
        }
        if (verbose) I.say(""+mat.texture);
        tex.fileName = baseDir.child(mat.texture).path();
        this.setKeyFile(tex.fileName);
        // + "/" +
        // mat.texture;
        tex.id = mat.texture;
        tex.usage = ModelTexture.USAGE_DIFFUSE;
        m.textures = new Array <ModelTexture> ();
        m.textures.add(tex);
      }
      data.materials.add(m);
    }

    ModelMaterial mat = new ModelMaterial();
    mat.ambient = new Color(0.8f, 0.8f, 0.8f, 1f);
    mat.diffuse = new Color(0.8f, 0.8f, 0.8f, 1f);
    mat.id = "default";
    data.materials.add(mat);
    /*
    if (data.materials.size == 0) {
    }
    //*/
  }
  
  
  private static Color color(float[] col) {
    if (col[0] == 0 && col[1] == 0 && col[2] == 0) return null;
    return new Color(col[0], col[1], col[2], col[3]);
  }
  
  
  private void processMesh() {
    mesh = new ModelMesh();
    mesh.id = "mesh";

    data.meshes.add(mesh);

    Array<VertexAttribute> attrs = new Array<VertexAttribute>(
        VertexAttribute.class);
    attrs.add(VertexAttribute.Position());
    attrs.add(VertexAttribute.Normal());
    attrs.add(VertexAttribute.TexCoords(0));
    attrs.add(VertexAttribute.BoneWeight(0));
    attrs.add(VertexAttribute.BoneWeight(1));
    attrs.add(VertexAttribute.BoneWeight(2));

    mesh.attributes = attrs.toArray();

    final int n = 14;
    float[] verts = new float[ms3d.triangles.length * 3 * n];

    int p = 0;
    {
      for (MS3DTriangle tri : ms3d.triangles) {

        for (int j = 0; j < 3; j++) {
          MS3DVertex vert = ms3d.vertices[tri.indices[j]];
          
          verts[p * n + 0] = vert.vertex[0];
          verts[p * n + 1] = vert.vertex[1];
          verts[p * n + 2] = vert.vertex[2];
          
          verts[p * n + 3] = tri.normals[j][0];
          verts[p * n + 4] = tri.normals[j][1];
          verts[p * n + 5] = tri.normals[j][2];
          
          verts[p * n + 6] = tri.u[j];
          verts[p * n + 7] = tri.v[j];
          
          // there are actually 4 bone weights, but we use only 3

          verts[p * n + 8] = vert.boneid;
          if(vert.boneIds == null || vert.boneIds[0] == -1) {
            verts[p * n + 9] = 1f;
          }
          else {
            verts[p * n + 9] = vert.weights[0] / 100f;
            
            verts[p * n + 10] = vert.boneIds[0];
            verts[p * n + 11] = vert.weights[1] / 100f;
            
            verts[p * n + 12] = vert.boneIds[1];
            verts[p * n + 13] = vert.weights[2] / 100f;
          }            
          
          tri.indices[j] = (short) p;
          p++;
        }
      }
    }

    mesh.vertices = verts;

    root = new ModelNode();
    root.id = "node";
    root.meshId = "mesh";
    root.boneId = 0;
    final float scale = config == null ? 1 : config.getFloat("scale");
    //I.say("Scale for "+this.filePath+" is "+scale);
    root.scale = new Vector3(scale, scale, scale);

    ModelMeshPart[] parts = new ModelMeshPart[ms3d.groups.length];
    ModelNodePart[] nparts = new ModelNodePart[ms3d.groups.length];
    
    int k = 0;
    for (MS3DGroup group : ms3d.groups) {
      final ModelMeshPart part = new ModelMeshPart();
      part.id = group.name;
      part.primitiveType = GL20.GL_TRIANGLES;
      part.indices = new short[group.trindices.length * 3];
      
      final short[] trindices = group.trindices;

      for (int i = 0; i < trindices.length; i++) {
        part.indices[i * 3 + 0] = ms3d.triangles[trindices[i]].indices[0];
        part.indices[i * 3 + 1] = ms3d.triangles[trindices[i]].indices[1];
        part.indices[i * 3 + 2] = ms3d.triangles[trindices[i]].indices[2];
      }

      final ModelNodePart npart = new ModelNodePart();
      final int matID = group.materialIndex;
      npart.meshPartId = group.name;
      npart.materialId = (matID == -1) ? "default" : ms3d.materials[matID].name;
      npart.bones = new ArrayMap();
      
      parts[k] = part;
      nparts[k] = npart;
      k++;
      // nparts[]
    }
    mesh.parts = parts;
    root.parts = nparts;

    data.nodes.add(root);
  }
  
  
  private void processJoints() {

    final ModelAnimation animation = new ModelAnimation();
    animation.id = AnimNames.FULL_RANGE;

    ArrayMap<String, ModelNode> lookup = new ArrayMap<String, ModelNode>(32);
    if (verbose) I.say("FPS: " + ms3d.fAnimationFPS); // whatever that is...

    for (int i = 0; i < ms3d.joints.length; i++) {
      MS3DJoint jo = ms3d.joints[i];
      for (ModelNodePart part : root.parts) {
        part.bones.put(jo.name, new Matrix4());
      }

      ModelNode mn = new ModelNode();
      
      mn.id = jo.name;
      mn.meshId = "mesh";
      mn.boneId = i;
      mn.rotation = jo.matrix.getRotation(new Quaternion());
      mn.translation = jo.matrix.getTranslation(new Vector3());
      mn.scale = new Vector3(1, 1, 1);

      ModelNode parent = jo.parentName.isEmpty() ? root : lookup
          .get(jo.parentName);
      
      addChild(parent, mn);
      lookup.put(mn.id, mn);

      ModelNodeAnimation ani = new ModelNodeAnimation();
      ani.nodeId = mn.id;

      for (int j = 0; j < jo.positions.length; j++) {
        ModelNodeKeyframe kf = new ModelNodeKeyframe();
        
        kf.keytime = jo.rotations[j].time;
        kf.translation = new Vector3(jo.positions[j].data);
        kf.translation.mul(jo.matrix);
        kf.rotation = jo.matrix.getRotation(new Quaternion()).mul(
          MS3DFile.fromEuler(jo.rotations[j].data)
        );
        ani.keyframes.add(kf);
      }
      animation.nodeAnimations.add(ani);
    }
    
    data.animations.add(animation);
    loadKeyframes(animation);
    
    if (verbose) {
      I.say("MODEL IS: "+filePath);
      I.say("  TOTAL ANIMATIONS LOADED: "+data.animations.size);
    }
  }
  
  
  private void loadKeyframes(ModelAnimation animation) {
    if (animation == null || config == null) return;
    
    if (verbose) I.say("\nLoading animations for model: "+filePath);
    
    final XML animConfig = config.child("animations");
    float FPS = animConfig.getFloat("fps");
    if (FPS == 0 || FPS == 1) FPS = 1.0f;
    this.rotateOffset = animConfig.getFloat("rotate");
    
    if (verbose) for (ModelNodeAnimation node : animation.nodeAnimations) {
      I.add("\n  Total animations in "+node.nodeId+": "+node.keyframes.size);
      I.add(" (");
      for (ModelNodeKeyframe frame : node.keyframes) {
        I.add(" "+frame.keytime);
      }
      I.add(")");
    }
    
    addLoop: for (XML animXML : animConfig.children()) {
      //
      // First, check to ensure that this animation has an approved name:
      final String name = animXML.value("name");
      if (! Sprite.isValidAnimName(name)) I.say(
        "WARNING: ANIMATION WITH IRREGULAR NAME: "+name+
        " IN MODEL: "+filePath
      );
      for (ModelAnimation oldAnim : data.animations) {
        if (oldAnim.id.equals(name)) continue addLoop;
      }
      
      // Either way, define the data-
      final float
        animStart  = Float.parseFloat(animXML.value("start"   )) / FPS,
        animEnd    = Float.parseFloat(animXML.value("end"     )) / FPS,
        animLength = Float.parseFloat(animXML.value("duration"));
      
      final ModelAnimation anim = new ModelAnimation();
      anim.id = name;

      // scaling for exact duration
      float scale = animLength / (animEnd - animStart);
      int maxFrames = 0;
      
      for (ModelNodeAnimation node : animation.nodeAnimations) {
        final ModelNodeAnimation nd = new ModelNodeAnimation();
        nd.nodeId = node.nodeId;
        int numFrames = 0;
        
        for (ModelNodeKeyframe frame : node.keyframes) {
          if (frame.keytime >= animStart && frame.keytime <= animEnd) {
            final ModelNodeKeyframe kf = copy(frame);
            
            // trimming the beggining and scaling
            kf.keytime -= animStart;
            kf.keytime *= scale;
            nd.keyframes.add(kf);
            numFrames++;
          }
        }
        anim.nodeAnimations.add(nd);
        maxFrames = Nums.max(maxFrames, numFrames);
      }
      
      if (verbose) {
        I.say("  Adding animation with name: "+name);
        I.say("  Start/end:                  "+animStart+"/"+animEnd);
        I.say("  Total frames:               "+maxFrames);
      }
      data.animations.add(anim);
    }
  }
  
  
  private static void addChild(ModelNode parent, ModelNode child) {
    if (parent.children == null) {
      parent.children = new ModelNode[] { child };
    } else {
      parent.children = Arrays.copyOf(parent.children,
          parent.children.length + 1);
      parent.children[parent.children.length - 1] = child;
    }
  }
  
  
  private static ModelNodeKeyframe copy(ModelNodeKeyframe frame) {
    ModelNodeKeyframe kf = new ModelNodeKeyframe();
    kf.keytime = frame.keytime;
    kf.rotation = frame.rotation.cpy();
    // kf.scale = frame.scale.cpy();
    kf.translation = frame.translation.cpy();
    return kf;
  }
}


