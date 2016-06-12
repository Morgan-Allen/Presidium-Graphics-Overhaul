

package stratos.graphics.solids;
import java.io.*;
import java.util.*;
import stratos.util.*;

import com.badlogic.gdx.math.*;



public class MS3DFile {
  
  private static boolean verbose = false;
  
  public static final int MAX_VERTICES = 65535;
  public static final int MAX_TRIANGLES = 65535;
  public static final int MAX_GROUPS = 255;
  public static final int MAX_MATERIALS = 128;
  public static final int MAX_JOINTS = 128;

  public static final int SELECTED = 1;
  public static final int HIDDEN = 2;
  public static final int SELECTED2 = 4;
  public static final int DIRTY = 8;

  public String id;
  public int version;
  
  

  public MS3DFile(DataInput0 in) throws IOException {

    id = in.readUTF(10);
    version = in.readInt();
    if (verbose) I.say(id + " v" + version);

    parseVertices(in);
    parseIndices(in);
    parseGroups(in);
    parseMaterials(in);
    parseJoints(in);
    //if(in.available() > 4) {
    parseSubVersions(in);
    //}

    inverse();
  }
  
  
  public static class MS3DVertex {
    public float[] vertex;
    public byte boneid;
    
    // bone0 = boneid , weights[0]
    // bone1 = boneIds[0] , weights[1]
    // bone2 = boneIds[1] , weights[2]
    // bone3 = boneIds[2] , 100 - weights[0] - weights[1] - weights[2]
    // this format is stupid...
    
    public byte[] boneIds;
    public byte[] weights;
    public int extra;
  }
  
  
  public MS3DVertex[] vertices;

  private void parseVertices(DataInput0 in) throws IOException {
    int nNumVertices = in.readUShort();

    vertices = new MS3DVertex[nNumVertices];

    for (int i = 0; i < nNumVertices; i++) {
      MS3DVertex vert = vertices[i] = new MS3DVertex();

      int flags = in.readByte(); // useless

      vert.vertex = new float[3];
      vert.vertex[0] = in.readFloat();
      vert.vertex[1] = in.readFloat();
      vert.vertex[2] = in.readFloat();

      vert.boneid = in.readByte();
      if (vert.boneid == -1) vert.boneid = 0;

      int referenceCount = in.readByte(); // useless

    }
  }
  
  
  public static class MS3DTriangle {
    public short[] indices;
    public float[][] normals = new float[3][];
    public float[] u;
    public float[] v;
    public byte smoothingGroup;
    public byte groupIndex;
  }

  public MS3DTriangle[] triangles;

  private void parseIndices(DataInput0 in) throws IOException {
    int nNumTriangles = in.readUShort();

    triangles = new MS3DTriangle[nNumTriangles];

    for (int i = 0; i < nNumTriangles; i++) {
      MS3DTriangle tri = triangles[i] = new MS3DTriangle();

      int flags = in.readUShort(); // useless
      tri.indices = in.readShorts(new short[3]);

      tri.normals[0] = in.readFloats(new float[3]);
      tri.normals[1] = in.readFloats(new float[3]);
      tri.normals[2] = in.readFloats(new float[3]);

      tri.u = in.readFloats(new float[3]);
      tri.v = in.readFloats(new float[3]);

      tri.smoothingGroup = in.readByte();
      tri.groupIndex = in.readByte();

    }
  }

  public static class MS3DGroup {
    public String name;
    public short[] trindices;
    public byte materialIndex;
  }

  public MS3DGroup[] groups;

  private void parseGroups(DataInput0 in) throws IOException {
    int nNumGroups = in.readUShort();

    groups = new MS3DGroup[nNumGroups];

    for (int i = 0; i < nNumGroups; i++) {
      MS3DGroup group = groups[i] = new MS3DGroup();

      byte flags = in.readByte(); // useless

      group.name = in.readUTF(32);

      if (verbose) I.say("Group: " + group.name);

      int numTriangles = in.readUShort();
      group.trindices = in.readShorts(new short[numTriangles]);

      group.materialIndex = in.readByte();
    }
  }

  public static class MS3DMaterial {
    public String name;
    public float[] ambient;
    public float[] diffuse;
    public float[] specular;
    public float[] emissive;
    public float shininess;
    public float transparency;
    public byte mode;
    public String texture;
    public String alphamap;
  }

  public MS3DMaterial[] materials;

  private void parseMaterials(DataInput0 in) throws IOException {
    int nNumMaterials = in.readUShort();

    materials = new MS3DMaterial[nNumMaterials];

    for (int i = 0; i < nNumMaterials; i++) {
      MS3DMaterial mat = materials[i] = new MS3DMaterial();

      mat.name = in.readUTF(32);
      mat.ambient = in.readFloats(new float[4]);
      mat.diffuse = in.readFloats(new float[4]);
      mat.specular = in.readFloats(new float[4]);
      mat.emissive = in.readFloats(new float[4]);
      mat.shininess = in.readFloat();
      mat.transparency = in.readFloat();
      
      mat.mode = in.readByte();
      mat.texture = in.readUTF(128);
      mat.alphamap = in.readUTF(128);
    }
  }
  
  
  public float fAnimationFPS;
  public float fCurrentTime;
  public int iTotalFrames;
  
  
  public static class Keyframe {
    public float time;
    public float[] data;
  }
  
  
  public static class MS3DJoint {
    public String name;
    public String parentName;

    public Matrix4 lmatrix = new Matrix4();
    public Matrix4 cmatrix = new Matrix4();

    public Keyframe[] rotations;
    public Keyframe[] positions;
  }

  public MS3DJoint[] joints;

  private void parseJoints(DataInput0 in) throws IOException {
    fAnimationFPS = in.readFloat();
    fCurrentTime = in.readFloat();
    iTotalFrames = in.readInt();

    int nNumJoints = in.readUShort();

    joints = new MS3DJoint[Nums.max(nNumJoints, 1)];

    for (int i = 0; i < nNumJoints; i++) {
      MS3DJoint joint = (joints[i] = new MS3DJoint());

      byte flags = in.readByte(); // useless

      joint.name = in.readUTF(32);
      joint.parentName = in.readUTF(32);

      if (verbose) I.say("Joint: " + joint.name);

      Quaternion rot = fromEuler(in.readFloats(new float[3]));
      Vector3 pos = in.read3D(new Vector3());
      
      joint.lmatrix.set(pos, rot, new Vector3(1,1,1));
      joint.cmatrix.set(joint.lmatrix);

      int rots = in.readUShort();
      int poss = in.readUShort();

      joint.rotations = new Keyframe[rots];
      joint.positions = new Keyframe[poss];

      for (int j = 0; j < rots; j++) {
        Keyframe kf = joint.rotations[j] = new Keyframe();
        kf.time = in.readFloat();
        kf.data = in.readFloats(new float[3]);
      }

      for (int j = 0; j < poss; j++) {
        Keyframe kf = joint.positions[j] = new Keyframe();
        kf.time = in.readFloat();
        kf.data = in.readFloats(new float[3]);
      }
    }
    
    if (nNumJoints == 0) {
      final MS3DJoint root = joints[0] = new MS3DJoint();
      root.name = "root";
      root.parentName = "";
      root.rotations = new Keyframe[0];
      root.positions = new Keyframe[0];
      root.lmatrix.idt();
      root.cmatrix.idt();
    }
  }
  
  
  private void skipComments(DataInput0 in) throws IOException {
    int nNumComments = in.readInt();
    
    for(int i=0; i<nNumComments; i++) {
      int index = in.readInt();
      int len = in.readInt();
      in.skip(len);
    }
  }

  private void parseSubVersions(DataInput0 in) throws IOException {
    
    if(in.available() < 4) {
      if(verbose) I.say("Model doesn't have weights");
      return;
    }
    int subv1 = in.readInt(); // ignore
    
    skipComments(in); // skip group comments
    skipComments(in); // skip material comments
    skipComments(in); // skip joint comments
    skipComments(in); // skip model comments

    if(in.available() < 4) {
      if(verbose) I.say("Model doesn't have weights");
      return;
    }
    
    int subv2 = in.readInt();
    int extraw = 0;
    
    
    if (verbose) I.say("\nProcessing after sub-version...");
    for(int i=0; i<vertices.length; i++) {
      //  TODO:  ...This seems to work, but I don't know why bone-information
      //         wouldn't be included...
      if (in.available() < 14) break;
      in.read(vertices[i].boneIds = new byte[3]);
      in.read(vertices[i].weights = new byte[3]);
      if(verbose && vertices[i].weights[1] > 0) extraw++;
      if(subv2 > 1)
        vertices[i].extra = in.readInt();
      if(subv2 > 2)
    	  in.readInt(); // another extra, ignore
    }
    if(verbose) I.say("Loaded " + extraw + " extra weights");
    // f... the rest
  }
  

  /**
   * Post-processing of joints data (lifted from earlier codebase.)
   */
  final static Matrix4 ROT_X = new Matrix4(), INV_R = new Matrix4();
  static {
    ROT_X.setToRotation(new Vector3(1, 0, 0), -90);
    INV_R.set(ROT_X).inv();
  }

  /**
   * Big bunch of utility methods-
   */
  private void inverse() {
    Map<String, MS3DJoint> map = new HashMap<String, MS3DFile.MS3DJoint>();

    for (MS3DJoint j : joints) {
      map.put(j.name, j);
    }

    for (MS3DJoint j : joints) {
      if (!j.parentName.isEmpty()) {
        MS3DJoint parent = map.get(j.parentName);
        j.cmatrix.set(parent.cmatrix).mul(j.lmatrix);
      }
    }
  }

  public static Quaternion fromEuler(float[] angles) {
    float angle;
    float sr, sp, sy, cr, cp, cy;
    angle = (angles[2]) * 0.5f;
    sy = (float) Nums.sin(angle);
    cy = (float) Nums.cos(angle);
    angle = angles[1] * 0.5f;
    sp = (float) Nums.sin(angle);
    cp = (float) Nums.cos(angle);
    angle = angles[0] * 0.5f;
    sr = (float) Nums.sin(angle);
    cr = (float) Nums.cos(angle);

    float crcp = cr * cp;
    float srsp = sr * sp;

    float x = (sr * cp * cy - cr * sp * sy);
    float y = (cr * sp * cy + sr * cp * sy);
    float z = (crcp * sy - srsp * cy);
    float w = (crcp * cy + srsp * sy);

    return new Quaternion(x, y, z, w);
  }

  // public static Vector3 getRPY(Quaternion q) {
  // float x = q.x;
  // float y = q.y;
  // float z = q.z;
  // float w = q.w;
  //
  // float roll = (float) Nums.atan2(2*y*w - 2*x*z, 1 - 2*y*y - 2*z*z);
  // float pitch = (float) Nums.atan2(2*x*w - 2*y*z, 1 - 2*x*x - 2*z*z);
  // float yaw = (float) Nums.asin(2*x*y + 2*z*w);
  //
  // return new Vector3(roll, pitch, yaw);
  // }
}
