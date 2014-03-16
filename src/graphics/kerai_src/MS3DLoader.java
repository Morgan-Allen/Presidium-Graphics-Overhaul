package src.graphics.kerai_src;

import java.io.IOException;
import java.util.Arrays;

import src.graphics.kerai_src.MS3DFile.*;
import src.graphics.kerai_src.MS3DLoader.MS3DParameters;
//import sf.gdx.ms3d.MS3DFile.MS3DVertex;
//import sf.gdx.ms3d.MS3DLoader.MS3DParameters;
//import sf.io.DataInput;

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



public class MS3DLoader extends ModelLoader<MS3DParameters> {
	
	
	public static boolean FORCE_DEFAULT_MATERIAL = false;
	
	private static Color color(float[] col) {
		if(col[0] ==0 && col[1]==0 && col[2]==0)
			return null;
		return new Color(col[0], col[1], col[2], col[3]);
	}
	
	/** Add child to modelnode, because it has an array[] instead of Array type */
	private static void addChild(ModelNode parent, ModelNode child) {
		if(parent.children == null) {
			parent.children = new ModelNode[]{child};
		} else {
			parent.children = Arrays.copyOf(parent.children, parent.children.length+1);
			parent.children[parent.children.length-1] = child;
		}
	}
	
	public MS3DLoader(FileHandleResolver resolver) {
		super(resolver);
	}
	
	private MS3DFile parseFile(ModelData data, FileHandle file) throws IOException {
		// class used to read little endian data
		DataInput in = new DataInput(file.read(), true);
		try {
			return new MS3DFile(in);
		} finally {
			in.close();
		}
	}
	
	private static final float SCALE = 0.25f;
	
	@Override
	public ModelData loadModelData(FileHandle file, MS3DParameters parameters) {
		data = new ModelData();
		params = parameters;
		try {
			ms3d = parseFile(data, file);
		} catch(IOException e) {
			throw new GdxRuntimeException("Failed to parse " + file.name(), e);
		}
		
		processMaterials(file);
		processMesh();
		processJoints();
		return data;
	}
	
	// temporary variables
	private MS3DParameters params;
	private ModelData data;
	private ModelMesh mesh;
	private ModelNode root;
	private MS3DFile ms3d;
	
	private void processMaterials(FileHandle file) {
		if(!FORCE_DEFAULT_MATERIAL)
			for(MS3DMaterial mat : ms3d.materials) {
				ModelMaterial m = new ModelMaterial();
				m.id = mat.name;
				m.ambient = color(mat.ambient);
				m.diffuse = color(mat.diffuse);
				m.emissive = color(mat.emissive);
				m.specular = color(mat.specular);
				m.shininess = mat.shininess;
				m.opacity = mat.transparency;
				m.type = MaterialType.Lambert;
				
				if(m.opacity == 0) {
					m.opacity = 1;
				}
				
				if(!mat.texture.isEmpty()) {
					ModelTexture tex = new ModelTexture();
					if(mat.texture.startsWith(".\\") || mat.texture.startsWith("//"))
						mat.texture = mat.texture.substring(2);
					System.out.println(mat.texture);
					tex.fileName = file.parent().child(mat.texture).path();// + "/" + mat.texture;
					tex.id = mat.texture;
					tex.usage = ModelTexture.USAGE_DIFFUSE;
					m.textures = new Array<ModelTexture>();
					m.textures.add(tex);
				}
				data.materials.add(m);
			}

		if(data.materials.size==0) {
			ModelMaterial mat = new ModelMaterial();
			mat.ambient = new Color(0.8f, 0.8f, 0.8f, 1f);
			mat.diffuse = new Color(0.8f, 0.8f, 0.8f, 1f);
			mat.id = "default";
			data.materials.add(mat);
		}
	}
	
	private void processMesh() {
		mesh = new ModelMesh();
		mesh.id = "mesh";
		
		data.meshes.add(mesh);
		
		Array<VertexAttribute> attrs = new Array<VertexAttribute>(VertexAttribute.class);
		attrs.add(VertexAttribute.Position());
		attrs.add(VertexAttribute.Normal());
		attrs.add(VertexAttribute.TexCoords(0));
		attrs.add(VertexAttribute.BoneWeight(0));
		
		mesh.attributes = attrs.toArray();
		
		final int n = 10;
		float[] verts = new float[ms3d.triangles.length * 3 * n];
		
//		Vector3 vec1 = new Vector3();
//		Vector3 vec2 = new Vector3();
//		Vector3 tmp = new Vector3();
		
		int p = 0;
		{
			for(MS3DTriangle lol : ms3d.triangles) {
//				MS3DVertex v1 = ms3d.vertices[lol.indices[0]];
//				MS3DVertex v2 = ms3d.vertices[lol.indices[1]];
//				MS3DVertex v3 = ms3d.vertices[lol.indices[2]];
				
//				calculating normals myself, not needed
//				tmp.set(v1.vertex);
//				vec1.set(v2.vertex).sub(tmp);
//				vec2.set(v3.vertex).sub(tmp);
//				vec1.crs(vec2);
				
				
				for(int j = 0; j < 3; j++) {
					MS3DVertex vert = ms3d.vertices[lol.indices[j]];
					
					verts[p*n + 0] = vert.vertex[0];
					verts[p*n + 1] = vert.vertex[1];
					verts[p*n + 2] = vert.vertex[2];
					
					//verts[p*n + 3] = vec1.x;
					//verts[p*n + 4] = vec1.y;
					//verts[p*n + 5] = vec1.z;
					
					verts[p*n + 3] = lol.normals[j][0];
					verts[p*n + 4] = lol.normals[j][1];
					verts[p*n + 5] = lol.normals[j][2];
					
					verts[p*n + 6] = lol.u[j];
					verts[p*n + 7] = lol.v[j];
					
					verts[p*n + 8] = vert.boneid;
					verts[p*n + 9] = 1;
					
					lol.indices[j] = (short) p;
					
					p++;
				}
			}
		}
		
		
//		float[] verts = new float[ms3d.vertices.length * n];
//		
//		{
//			int i=0;
//			for(MS3DVertex vert : ms3d.vertices) {
//				verts[i+0] = vert.vertex[0];
//				verts[i+1] = vert.vertex[1];
//				verts[i+2] = vert.vertex[2];
//				
//				verts[i+8] = vert.boneid;
//				verts[i+9] = 1;
//				
//				i+=n;
//			}
//			
//			i=0;
//			for(MS3DTriangle lol : ms3d.triangles) {
//				for(int j = 0; j < 3; j++) {
//					int p = lol.indices[j];
//	
//					verts[p*n + 3] = lol.u[j];
//					verts[p*n + 4] = lol.v[j];
//					
//					verts[p*n + 5] += lol.normals[j][0];
//					verts[p*n + 6] += lol.normals[j][1];
//					verts[p*n + 7] += lol.normals[j][2];
//				}
//			}
//			
//			for(int k=0; k< ms3d.vertices.length; k++) {
//				int p = k*n;
//				
//				Vector3 tmp = new Vector3();
//				tmp.x = verts[p+5];
//				tmp.y = verts[p+6];
//				tmp.z = verts[p+7];
//				
//				tmp.nor();
//				
//				verts[p+5] = tmp.x;
//				verts[p+6] = tmp.y;
//				verts[p+7] = tmp.z;
//			}
//		}
		
		mesh.vertices = verts;
		

		root = new ModelNode();
		root.id = "node";
		root.meshId = "mesh";
		root.boneId = 0;
		root.scale = new Vector3(params.scale, params.scale, params.scale);
		
		ModelMeshPart[] parts = new ModelMeshPart[ms3d.groups.length];
		ModelNodePart[] nparts = new ModelNodePart[ms3d.groups.length];
		
		int k =0;
		for(MS3DGroup group : ms3d.groups) {
			ModelMeshPart part = new ModelMeshPart();
			part.id = group.name;
			part.primitiveType = GL20.GL_TRIANGLES;
			part.indices = new short[group.trindices.length * 3];

			short[] trindices = group.trindices;
			
			for(int i=0; i< trindices.length; i++) {
				part.indices[i*3+0] = ms3d.triangles[trindices[i]].indices[0];
				part.indices[i*3+1] = ms3d.triangles[trindices[i]].indices[1];
				part.indices[i*3+2] = ms3d.triangles[trindices[i]].indices[2];
			}
			
			ModelNodePart npart = new ModelNodePart();
			npart.meshPartId = group.name;
			npart.materialId = ms3d.materials[group.materialIndex].name;
			npart.bones = new ArrayMap();
			
			parts[k] = part;
			nparts[k] = npart;
			k++;
			//nparts[]
		}
		mesh.parts = parts;
		root.parts = nparts;
		
		data.nodes.add(root);
			
//		screw the groups now
//		{
//			int j = 0;
//			for(MS3DGroup group : ms3d.groups) {
//				ModelMeshPart mpart = new ModelMeshPart();
//				mpart.id = group.name;
//				mpart.primitiveType = GL20.GL_TRIANGLES;
//				
//				short[] indices = new short[group.indices.length * 3];
//				
//				for(int i=0; i< group.indices.length; i++) {
//					indices[i*3+0] = ms3d.triangles[i].indices[0];
//					indices[i*3+1] = ms3d.triangles[i].indices[1];
//					indices[i*3+2] = ms3d.triangles[i].indices[2];
//				}
//				mpart.indices = indices;
//				
//				parts[j] = mpart;
//				
//				ModelNodePart npart = new ModelNodePart();
//				npart.materialId = ms3d.materials[group.materialIndex].name;
//				npart.meshPartId = group.name;
//				
//				nparts[j] = npart;
//				
//				
//				j++;
//			}
//		}
		
	}
	
	private void processJoints() {
		
		//ModelNodePart np = root.parts[0];
		
		// for now just one animation, dont split it yet
		
		ModelAnimation animation = new ModelAnimation();
		animation.id = "default";
		
		
		ArrayMap<String, ModelNode> lookup = new ArrayMap<String, ModelNode>(32);
		
		//np.bones = new ArrayMap<String, Matrix4>(13);
		
		
		System.out.println("FPS: " + ms3d.fAnimationFPS); // whatever that is...
		//float fpsmod = 1 / (25 / ms3d.fAnimationFPS); // whatever, random, just to make it work somehow
		
		
		for(int i=0; i < ms3d.joints.length; i++) {
			MS3DJoint jo = ms3d.joints[i];
			for(ModelNodePart part : root.parts ) {
				part.bones.put(jo.name, new Matrix4());
			}
			
			ModelNode mn= new ModelNode();
			
			mn.id = jo.name;
			mn.meshId = "mesh";
			mn.rotation = jo.matrix.getRotation(new Quaternion());
			mn.translation = jo.matrix.getTranslation(new Vector3());
			mn.scale = new Vector3(1,1,1);
			
			ModelNode parent = jo.parentName.isEmpty() ? root : lookup.get(jo.parentName);
			
			addChild(parent, mn);
			lookup.put(mn.id, mn);
			
			ModelNodeAnimation ani = new ModelNodeAnimation();
			ani.nodeId = mn.id;
			
			for(int j=0; j< jo.positions.length; j++) {
				ModelNodeKeyframe kf = new ModelNodeKeyframe();
				
				kf.keytime = jo.rotations[j].time ;
				kf.translation = new Vector3(jo.positions[j].data);
				kf.translation.mul(jo.matrix);
				//kf.translation.scl(1);
				kf.rotation = jo.matrix.getRotation(new Quaternion()).mul(MS3DFile.fromEuler(jo.rotations[j].data));
				
				ani.keyframes.add(kf);
			}
			animation.nodeAnimations.add(ani);
		}
		if(params == null ) {
			data.animations.add(animation);
			return;
		}
		
		// params are present, split animations
		
		for(MS3DAnimParam param : params.params) {
			ModelAnimation anim = new ModelAnimation();
			anim.id = param.name;
			
			// scaling for exact duration
			float scale = param.dur / (param.end-param.begin);
			
			for(ModelNodeAnimation node : animation.nodeAnimations) {
				ModelNodeAnimation nd = new ModelNodeAnimation();
				nd.nodeId = node.nodeId;
				for(ModelNodeKeyframe frame : node.keyframes) {
					if(frame.keytime >= param.begin && frame.keytime <= param.end) {
						ModelNodeKeyframe kf = copy(frame);
						
						// trimming the beggining and scaling
						kf.keytime -= param.begin;
						kf.keytime *= scale;
						
						nd.keyframes.add(kf);
					}
				}
				anim.nodeAnimations.add(nd);
			}
			data.animations.add(anim);
		}
	}
	
	private static ModelNodeKeyframe copy(ModelNodeKeyframe frame) {
		ModelNodeKeyframe kf = new ModelNodeKeyframe();
		kf.keytime = frame.keytime;
		kf.rotation = frame.rotation.cpy();
		//kf.scale = frame.scale.cpy();
		kf.translation = frame.translation.cpy();
		return kf;
	}
	
	

	
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
		public MS3DAnimParam( String name, int begin, int end, float dur) {
	        this.begin = begin;
	        this.end = end;
	        this.name = name;
	        this.dur = dur;
        }
	}
}