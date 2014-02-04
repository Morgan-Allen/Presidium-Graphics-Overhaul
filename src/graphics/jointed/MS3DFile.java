

package graphics.jointed;

import java.io.IOException;

import com.badlogic.gdx.math.* ;

import java.util.Hashtable ;

import util.I;




@SuppressWarnings("unused")
public class MS3DFile {
	public static final int MAX_VERTICES  = 65535;
	public static final int MAX_TRIANGLES = 65535;
	public static final int MAX_GROUPS    = 255;
	public static final int MAX_MATERIALS = 128;
	public static final int MAX_JOINTS    = 128;
	
	public static final int SELECTED  = 1;
	public static final int HIDDEN    = 2;
	public static final int SELECTED2 = 4;
	public static final int DIRTY     = 8;

	public String id;
	public int version;

	
	
	public MS3DFile(DataInput in) throws IOException {

		id = in.readUTF(10);
		version = in.readInt();
		//System.out.println(id + " v" + version);
		
		parseVertices(in);
		parseIndices(in);
		parseGroups(in);
		parseMaterials(in);
		parseJoints(in);
		
		// ignoring rest of the file
		// all weights are 1 in my test model anyway
		
		postProcessJoints() ;
	}
	

	public static class MS3DVertex {
		public float[] vertex;
		public byte boneid;
	}
	
	
	public MS3DVertex[] vertices;
	
	
	private void parseVertices(DataInput in) throws IOException {
		int nNumVertices = in.readUShort();
		
		vertices = new MS3DVertex[nNumVertices];
		
		for(int i=0; i< nNumVertices; i++) {
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
		public short[]   indices;
		public float[][] normals = new float[3][];
		public float[]   u;
		public float[]   v;
		public byte      smoothingGroup;
		public byte      groupIndex;
	}
	
	public MS3DTriangle[] triangles;
	
	
	private void parseIndices(DataInput in) throws IOException {
		int nNumTriangles = in.readUShort();
		
		triangles = new MS3DTriangle[nNumTriangles];
		
		
		for(int i=0; i< nNumTriangles; i++) {
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
		public short[] indices;
		public byte materialIndex;
	}
	
	public MS3DGroup[] groups;
	
	private void parseGroups(DataInput in) throws IOException {
		int nNumGroups = in.readUShort();
		
		groups = new MS3DGroup[nNumGroups];

		for(int i=0; i< nNumGroups; i++) {
			MS3DGroup group = groups[i] = new MS3DGroup();
			
			byte flags = in.readByte(); // useless
			
			group.name = in.readUTF(32);
			
			int numTriangles = in.readUShort();
			group.indices = in.readShorts(new short[numTriangles]);
			
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
		public String texName;
		public String alphamap;
	}
	
	public MS3DMaterial[] materials;
	
	
	private void parseMaterials(DataInput in) throws IOException {
		int nNumMaterials = in.readUShort();

		materials = new MS3DMaterial[nNumMaterials];
		
		for(int i=0; i< nNumMaterials; i++) {
			MS3DMaterial mat = materials[i] = new MS3DMaterial();
			
			mat.name = in.readUTF(32);
			mat.ambient = in.readFloats(new float[4]);
			mat.diffuse = in.readFloats(new float[4]);
			mat.specular = in.readFloats(new float[4]);
			mat.emissive = in.readFloats(new float[4]);
			mat.shininess = in.readFloat();
			mat.transparency = in.readFloat();
			mat.mode = in.readByte();
			mat.texName = in.readUTF(128);
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
		public int index ;
		public String name;
		public MS3DJoint parent = null ;
		private String parentName ;
		//public String parentName;
		
		//public Quaternion rotation = new Quaternion();
		//public Vector3 position = new Vector3();
		
		public Matrix4 matrix = new Matrix4();
		public Matrix4 inverse = new Matrix4();
		
		public Keyframe[] rotations;
		public Keyframe[] positions;
	}
	
	
	public MS3DJoint[] joints;
	
	
	private void parseJoints(DataInput in) throws IOException {
		fAnimationFPS = in.readFloat();
		fCurrentTime = in.readFloat();
		iTotalFrames = in.readInt();
		
		int nNumJoints = in.readUShort();
		
		joints = new MS3DJoint[nNumJoints];

		for(int i=0; i< nNumJoints; i++) {
			MS3DJoint joint = ( joints[i] = new MS3DJoint() );
			
			byte flags = in.readByte(); // useless
			
			joint.name = in.readUTF(32);
			joint.parentName = in.readUTF(32);
			
			//System.out.println("\nJOINT NAME: "+joint.name) ;
			Quaternion rot = fromEuler(in.readFloats(new float[3]));
			Vector3 pos = in.read3D(new Vector3());
			///swap(pos);
			
			int rots = in.readUShort();
			int poss = in.readUShort();
			
			joint.rotations = new Keyframe[rots];
			joint.positions = new Keyframe[poss];
			float v[] ;
			
			//System.out.println() ;
			for(int j=0; j < rots; j++) {
				Keyframe kf = joint.rotations[j] = new Keyframe();
				kf.time = in.readFloat();
				kf.data = in.readFloats(v = new float[3]);
				
				Quaternion q = fromEuler(v) ;
				//System.out.println(" Rotation: ("+q.x+", "+q.y+", "+q.z+","+q.w+")") ;
			}
			
			//System.out.println() ;
			for(int j=0; j < poss; j++) {
				Keyframe kf = joint.positions[j] = new Keyframe();
				kf.time = in.readFloat();
				kf.data = in.readFloats(v = new float[3]);
				//System.out.println(" Position: ("+v[0]+", "+v[1]+", "+v[2]+")") ;
			}
			
			joint.matrix.set(rot);
			joint.matrix.setTranslation(pos);
			joint.inverse.set(joint.matrix);
			joint.inverse.inv();
		}
	}
	
	
	
	
	
	/**
	 *   Post-processing of joints data (lifted from earlier codebase.)
	 */
	final static Matrix4 ROT_X = new Matrix4(), INV_R = new Matrix4() ;
	static {
		ROT_X.setToRotation(new Vector3(1, 0, 0), -90) ;
		INV_R.set(ROT_X).inv() ;
	}
	
	
	private void postProcessJoints() {
		Hashtable <String, MS3DJoint> jointTable = new Hashtable <String, MS3DJoint> () ;
		///System.out.println("JOINTS ARE NULL? "+(joints == null));
		for (MS3DJoint j : joints) jointTable.put(j.name, j) ;
		for (MS3DJoint j : joints) {
			j.parent = jointTable.get(j.parentName) ;
		}
		
		for (MS3DJoint j : joints) {
			if (j.parent != null) j.inverse.mul(j.parent.inverse) ;
		}
		
		Vector3 tmp = new Vector3();
		for (int i = 0 ; i < vertices.length ; i++) {
			MS3DVertex vert = vertices[i];
			
			tmp.x = vert.vertex[0];
			tmp.y = vert.vertex[1];
			tmp.z = vert.vertex[2];
			
			if (vert.boneid >= 0) {
				tmp.mul(joints[vert.boneid].inverse);
				///I.say("  VERT["+i+"] is: "+tmp) ;
			}
			else {
				///I.say("NO BONE ASSIGNED AT: "+i) ;
			}
			
			vert.vertex[0] = tmp.x;
			vert.vertex[1] = tmp.y;
			vert.vertex[2] = tmp.z;
		}
	}
	
	
	public static Quaternion fromEuler(float vals[]) {  //roll, pitch, and yaw.
		final float er = vals[0] / 2, ep = vals[1] / 2, ey = vals[2] / 2 ;
		final float
			sr = (float) (Math.sin(er)),
			cr = (float) (Math.cos(er)),
			sp = (float) (Math.sin(ep)),
			cp = (float) (Math.cos(ep)),
			sy = (float) (Math.sin(ey)),
			cy = (float) (Math.cos(ey)),
			w = (cr * cp * cy) + (sr * sp * sy),
			x = (sr * cp * cy) - (cr * sp * sy),
			y = (cr * sp * cy) + (sr * cp * sy),
			z = (cr * cp * sy) - (sr * sp * cy) ;
		return new Quaternion(x, y, z, w) ;
	}
}

