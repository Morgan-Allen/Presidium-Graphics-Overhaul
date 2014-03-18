

package src.graphics.solids;
import src.util.*;
import java.io.*;
import java.util.*;
import com.badlogic.gdx.math.*;



@SuppressWarnings("unused")
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

    // ignoring rest of the file
    // all weights are 1 in my test model anyway

    inverse();
  }

  public static class MS3DVertex {
    public float[] vertex;
    public byte boneid;
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

    // public Quaternion rotation = new Quaternion();
    // public Vector3 position = new Vector3();

    public Matrix4 matrix = new Matrix4();
    public Matrix4 inverse = new Matrix4();

    public Keyframe[] rotations;
    public Keyframe[] positions;
  }

  public MS3DJoint[] joints;

  private void parseJoints(DataInput0 in) throws IOException {
    fAnimationFPS = in.readFloat();
    fCurrentTime = in.readFloat();
    iTotalFrames = in.readInt();

    int nNumJoints = in.readUShort();

    joints = new MS3DJoint[nNumJoints];

    for (int i = 0; i < nNumJoints; i++) {
      MS3DJoint joint = (joints[i] = new MS3DJoint());

      byte flags = in.readByte(); // useless

      joint.name = in.readUTF(32);
      joint.parentName = in.readUTF(32);

      if (verbose) I.say("Joint: " + joint.name);

      Quaternion rot = fromEuler(in.readFloats(new float[3]));
      Vector3 pos = in.read3D(new Vector3());

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

      joint.matrix.set(rot);
      joint.matrix.setTranslation(pos);

      joint.inverse.set(joint.matrix);
      joint.inverse.inv();
    }
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
      if (!j.parentName.isEmpty())
        j.inverse.mul(map.get(j.parentName).inverse);
    }

    Vector3 tmp = new Vector3();

    for (int i = 0; i < vertices.length; i++) {

      MS3DVertex vert = vertices[i];
      int bone = vert.boneid;

      if (bone == -1)
        continue;

      tmp.x = vert.vertex[0];
      tmp.y = vert.vertex[1];
      tmp.z = vert.vertex[2];

      tmp.mul(joints[bone].inverse);

      vert.vertex[0] = tmp.x;
      vert.vertex[1] = tmp.y;
      vert.vertex[2] = tmp.z;

    }

    for (int i = 0; i < triangles.length; i++) {
      MS3DTriangle tri = triangles[i];
      for (int j = 0; j < tri.normals.length; j++) {
        int bone = vertices[tri.indices[j]].boneid;

        if (bone == -1)
          continue;

        float[] norm = tri.normals[j];

        tmp.x = norm[0];
        tmp.y = norm[1];
        tmp.z = norm[2];

        tmp.mul(joints[bone].inverse);

        norm[0] = tmp.x;
        norm[1] = tmp.y;
        norm[2] = tmp.z;
      }
    }

    // TODO I should also inverse normals here, but I'll deal with it later
  }

  public static Quaternion fromEuler(float[] angles) {
    float angle;
    float sr, sp, sy, cr, cp, cy;
    angle = (angles[2]) * 0.5f;
    sy = (float) Math.sin(angle);
    cy = (float) Math.cos(angle);
    angle = angles[1] * 0.5f;
    sp = (float) Math.sin(angle);
    cp = (float) Math.cos(angle);
    angle = angles[0] * 0.5f;
    sr = (float) Math.sin(angle);
    cr = (float) Math.cos(angle);

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
  // float roll = (float) Math.atan2(2*y*w - 2*x*z, 1 - 2*y*y - 2*z*z);
  // float pitch = (float) Math.atan2(2*x*w - 2*y*z, 1 - 2*x*x - 2*z*z);
  // float yaw = (float) Math.asin(2*x*y + 2*z*w);
  //
  // return new Vector3(roll, pitch, yaw);
  // }
}
