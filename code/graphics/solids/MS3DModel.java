

package code.graphics.solids;
import java.io.IOException;
import java.util.Arrays;
//import src.graphics.kerai_src.MS3DFile0.*;
//import src.graphics.kerai_src.MS3DLoader0.MS3DParameters;






import code.graphics.common.*;
import code.graphics.solids.MS3DFile.*;
import code.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.loaders.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.*;
import com.badlogic.gdx.graphics.g3d.model.data.*;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial.MaterialType;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;


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
  }
  
  
  public static MS3DModel loadFrom(
    String path, String fileName, Class sourceClass,
    String xmlFile, String xmlName
  ) {
    return new MS3DModel(path, fileName, sourceClass, xmlFile, xmlName);
  }
  

  protected void loadAsset() {
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
    }
    catch (Exception e) {
      I.report(e);
      return;
    }
    
    //this.scale = config.getFloat("scale");
    
    data = new ModelData();
    processMaterials();
    processMesh();
    processJoints();
    //  TODO:  LOAD ATTACH POINTS AS WELL
    
    super.compileModel(new Model(data));
    loaded = true;
  }
  
  
  public boolean isLoaded() {
    return loaded;
  }
  
  
  protected void disposeAsset() {
    super.disposeAsset();
  }
  


  private void processMaterials() {
    if (!FORCE_DEFAULT_MATERIAL)
      for (MS3DMaterial mat : ms3d.materials) {
        ModelMaterial m = new ModelMaterial();
        m.id = mat.name;
        m.ambient = color(mat.ambient);
        m.diffuse = color(mat.diffuse);
        m.emissive = color(mat.emissive);
        m.specular = color(mat.specular);
        m.shininess = mat.shininess;
        m.opacity = mat.transparency;
        m.type = MaterialType.Phong;
        
        if (m.opacity == 0) {
          m.opacity = 1;
        }

        if (!mat.texture.isEmpty()) {
          ModelTexture tex = new ModelTexture();
          if (mat.texture.startsWith(".\\") || mat.texture.startsWith("//"))
            mat.texture = mat.texture.substring(2);
          if (verbose) I.say(""+mat.texture);
          tex.fileName = baseDir.child(mat.texture).path();
          // + "/" +
          // mat.texture;
          tex.id = mat.texture;
          tex.usage = ModelTexture.USAGE_DIFFUSE;
          m.textures = new Array<ModelTexture>();
          m.textures.add(tex);
        }
        data.materials.add(m);
      }

    if (data.materials.size == 0) {
      ModelMaterial mat = new ModelMaterial();
      mat.ambient = new Color(0.8f, 0.8f, 0.8f, 1f);
      mat.diffuse = new Color(0.8f, 0.8f, 0.8f, 1f);
      mat.id = "default";
      data.materials.add(mat);
    }
  }
  
  
  private static Color color(float[] col) {
    if (col[0] == 0 && col[1] == 0 && col[2] == 0)
      return null;
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

    mesh.attributes = attrs.toArray();

    final int n = 10;
    float[] verts = new float[ms3d.triangles.length * 3 * n];

    int p = 0;
    {
      for (MS3DTriangle lol : ms3d.triangles) {

        for (int j = 0; j < 3; j++) {
          MS3DVertex vert = ms3d.vertices[lol.indices[j]];

          verts[p * n + 0] = vert.vertex[0];
          verts[p * n + 1] = vert.vertex[1];
          verts[p * n + 2] = vert.vertex[2];

          verts[p * n + 3] = lol.normals[j][0];
          verts[p * n + 4] = lol.normals[j][1];
          verts[p * n + 5] = lol.normals[j][2];

          verts[p * n + 6] = lol.u[j];
          verts[p * n + 7] = lol.v[j];

          verts[p * n + 8] = vert.boneid;
          verts[p * n + 9] = 1;

          lol.indices[j] = (short) p;

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
    root.scale = new Vector3(scale, scale, scale);

    ModelMeshPart[] parts = new ModelMeshPart[ms3d.groups.length];
    ModelNodePart[] nparts = new ModelNodePart[ms3d.groups.length];

    int k = 0;
    for (MS3DGroup group : ms3d.groups) {
      ModelMeshPart part = new ModelMeshPart();
      part.id = group.name;
      part.primitiveType = GL20.GL_TRIANGLES;
      part.indices = new short[group.trindices.length * 3];

      short[] trindices = group.trindices;

      for (int i = 0; i < trindices.length; i++) {
        part.indices[i * 3 + 0] = ms3d.triangles[trindices[i]].indices[0];
        part.indices[i * 3 + 1] = ms3d.triangles[trindices[i]].indices[1];
        part.indices[i * 3 + 2] = ms3d.triangles[trindices[i]].indices[2];
      }

      ModelNodePart npart = new ModelNodePart();
      npart.meshPartId = group.name;
      npart.materialId = ms3d.materials[group.materialIndex].name;
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
        // kf.translation.scl(1);
        kf.rotation = jo.matrix.getRotation(new Quaternion()).mul(
            MS3DFile.fromEuler(jo.rotations[j].data));

        ani.keyframes.add(kf);
      }
      animation.nodeAnimations.add(ani);
    }
    data.animations.add(animation);
    
    final XML animConfig = config.child("animations");
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
        animStart  = Float.parseFloat(animXML.value("start")),
        animEnd    = Float.parseFloat(animXML.value("end")),
        animLength = Float.parseFloat(animXML.value("duration"));
      
      final ModelAnimation anim = new ModelAnimation();
      anim.id = name;

      // scaling for exact duration
      float scale = animLength / (animEnd - animStart);

      for (ModelNodeAnimation node : animation.nodeAnimations) {
        ModelNodeAnimation nd = new ModelNodeAnimation();
        nd.nodeId = node.nodeId;
        for (ModelNodeKeyframe frame : node.keyframes) {
          if (frame.keytime >= animStart && frame.keytime <= animEnd) {
            ModelNodeKeyframe kf = copy(frame);
            
            // trimming the beggining and scaling
            kf.keytime -= animStart;
            kf.keytime *= scale;
            nd.keyframes.add(kf);
          }
        }
        anim.nodeAnimations.add(nd);
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
  
  /*
  public static class MS3DParameters extends AssetLoaderParameters<Model> {
    public Array<MS3DAnimParam> params = new Array();
    public float scale;
    public float stride; // unused, don't know what it does

    public void add(String name, int begin, int end, float dur) {
      params.add(new MS3DAnimParam(name, begin, end, dur));
    }
  }

  public static class MS3DAnimParam {
    public int begin, end;
    public String name;
    public float dur;

    public MS3DAnimParam(String name, int begin, int end, float dur) {
      this.begin = begin;
      this.end = end;
      this.name = name;
      this.dur = dur;
    }
  }
  //*/
}