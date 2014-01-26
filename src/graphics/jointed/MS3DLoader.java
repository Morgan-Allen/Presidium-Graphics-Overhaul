

package graphics.jointed;
import java.io.IOException;
import java.util.* ;

import util.I;
import graphics.jointed.MS3DFile.* ;

import com.badlogic.gdx.assets.* ;
import com.badlogic.gdx.assets.loaders.* ;
import com.badlogic.gdx.files.FileHandle ;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Model ;
import com.badlogic.gdx.graphics.g3d.model.data.* ;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial.MaterialType;
import com.badlogic.gdx.math.* ;
import com.badlogic.gdx.utils.* ;






//  TODO:  I'll need to pass in the XML document as well to get full info.



public class MS3DLoader extends ModelLoader<AssetLoaderParameters<Model>> {
	
	
	private FileHandle baseDir ;
	private MS3DFile file ;
	private ModelData data ;
	
	
	public MS3DLoader(FileHandleResolver resolver) {
		super(resolver) ;
	}
	
	
	public ModelData loadModelData(
		FileHandle fileHandle,
		AssetLoaderParameters<Model> parameters
	) {
		try {
			final DataInput input = new DataInput(fileHandle.read(), true) ;
			baseDir = fileHandle.parent() ;
			file = new MS3DFile(input) ;
		}
		catch (Exception e) { I.report(e) ; return null ; }
		data = new ModelData() ;
		
		processMaterials() ;
		processMesh() ;
		processJoints() ;
		return data ;
	}
	
	
	private void processMaterials() {
		for (MS3DMaterial mat : file.materials) {
			ModelMaterial m = new ModelMaterial() ;
			m.id = mat.name;
			m.ambient  = color(mat.ambient);
			m.diffuse  = color(mat.diffuse);
			m.emissive = color(mat.emissive);
			m.specular = color(mat.specular);
			m.shininess = mat.shininess;
			m.opacity = mat.transparency;
			m.type = MaterialType.Lambert;
			
			if (m.opacity == 0) {
				continue ;
			}
			if (! mat.texName.isEmpty()) {
				ModelTexture tex = new ModelTexture() ;
				if (mat.texName.startsWith(".\\")) {
					mat.texName = mat.texName.substring(2) ;
				}
				tex.fileName = baseDir+"/"+mat.texName ;
				tex.id = mat.texName ;
				tex.usage = ModelTexture.USAGE_DIFFUSE ;
				m.textures = new Array<ModelTexture>() ;
				m.textures.add(tex) ;
			}
			data.materials.add(m) ;
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
		return new Color(col[0], col[1], col[2], col[3]);
	}
	
	
	
	private void processMesh() {
		//
		//  Initialise the mesh and fill it up with base geometry data-
		final ModelMesh mesh = new ModelMesh();
		mesh.id = "mesh";
		mesh.attributes = new VertexAttribute[] {
			VertexAttribute.Position(),
			VertexAttribute.TexCoords(0),
			VertexAttribute.Normal(),
			VertexAttribute.BoneWeight(0)
		} ;
		final int numVerts = file.triangles.length * 3, NF = 10 ;
		final float[] verts = new float[numVerts * NF] ;
		final short[] indices = new short[numVerts * 3] ;
		int p = 0, index = 0 ;
		for (MS3DTriangle tri : file.triangles) {
			for (int j = 0; j < 3; j++) {
				MS3DVertex vert = file.vertices[tri.indices[j]];
				
				verts[p++] = vert.vertex[0];
				verts[p++] = vert.vertex[1];
				verts[p++] = vert.vertex[2];
				
				verts[p++] = tri.u[j];
				verts[p++] = tri.v[j];
				
				verts[p++] = tri.normals[j][0];
				verts[p++] = tri.normals[j][1];
				verts[p++] = tri.normals[j][2];
				
				verts[p++] = vert.boneid < 0 ? 0 : vert.boneid ;
				verts[p++] = 1;
				
				indices[index] = (short) index++ ;
			}
		}
		mesh.vertices = verts;
		
		//
		//  TODO:  Construct all the groups needed correctly.
		ModelMeshPart[] parts = new ModelMeshPart[1];//[ms3d.groups.length];
		ModelMeshPart part = new ModelMeshPart();
		part.id = "part";
		part.primitiveType = GL20.GL_TRIANGLES;
		part.indices = indices ;  //TODO:  Won't work for multiple groups.
		parts[0] = part;
		
		//  ...Will *this* combine easily with multiple groups..?
		final ModelNode modelNode = new ModelNode();
		modelNode.id = "node";
		modelNode.meshId = "mesh";
		modelNode.boneId = 0;
		ModelNodePart root = new ModelNodePart();
		root.materialId = data.materials.get(0).id;
		root.meshPartId = "part";
		modelNode.parts = new ModelNodePart[]{ root };
		
		data.nodes.add(modelNode);
		mesh.parts = parts;
		data.meshes.add(mesh);
	}
	
	

	private void processJoints() {
		//
		//  For now just one animation, don't split it yet-
		final ModelAnimation modelAnim = new ModelAnimation() ;
		modelAnim.id = "default";
		final float fpsmod = 1 / (25 / file.fAnimationFPS);
		
		//
		//  Set up reference tables for lookup purposes-
		final ModelNode modelNode = data.nodes.get(0) ;
		final ModelNodePart rootPart = modelNode.parts[0] ;
		final ArrayMap <String, ModelNode> 
			jointTable = new ArrayMap <String, ModelNode> (32) ;
		final ArrayMap <String, Matrix4>
			boneTable = new ArrayMap <String, Matrix4> (13) ;
		rootPart.bones = boneTable ;
		
		//
		//  Then establish proper animation sequences for every joint-
		for(int i=0; i < file.joints.length; i++) {
			final MS3DJoint
				fileJoint = file.joints[i],
				fileParent = fileJoint.parent ;
			final String childID = fileJoint.name ;
			
			//
			//  Attach the child node to the appropriate parent-
			ModelNode childNode = new ModelNode();
			final ModelNode parentNode = fileParent == null ?
				modelNode :
				jointTable.get(fileParent.name) ;
			
			addChild(parentNode, childNode) ;
			jointTable.put(childID, childNode) ;
			childNode.id = childID ;
			childNode.meshId = "mesh" ;
			childNode.rotation = fileJoint.matrix.getRotation(new Quaternion()) ;
			childNode.translation = fileJoint.matrix.getTranslation(new Vector3()) ;
			childNode.scale = new Vector3(1,1,1) ;
			//System.out.println(childID +" <= " + parentNode.id);
			
			//
			//  Then, set up the correct sequence of animation keyframes for
			//  this node-
			final ModelNodeAnimation childAnim = new ModelNodeAnimation();
			childAnim.nodeId = childID ;
			boneTable.put(childID, new Matrix4()) ;
			
			for (int j = 0 ; j < fileJoint.positions.length ; j++) {
				final ModelNodeKeyframe frame = new ModelNodeKeyframe();
				childAnim.keyframes.add(frame);
				//
				//  Set up translation correctly-
				frame.keytime = fileJoint.rotations[j].time * fpsmod;
				frame.translation = new Vector3(fileJoint.positions[j].data);
				frame.translation.mul(fileJoint.matrix);
				//
				//  Then set up rotation-
				final Quaternion
				  FE = MS3DFile.fromEuler(fileJoint.rotations[j].data),
				  JM = fileJoint.matrix.getRotation(new Quaternion()) ;
				frame.rotation = JM.mul(FE) ;
			}
			modelAnim.nodeAnimations.add(childAnim) ;
		}
		data.animations.add(modelAnim) ;
	}
	
	
	private static void addChild(ModelNode parent, ModelNode child) {
		if (parent.children == null) {
			parent.children = new ModelNode[] {child};
		}
		else {
			parent.children = Arrays.copyOf(
				parent.children, parent.children.length + 1
			);
			parent.children[parent.children.length - 1] = child;
		}
	}
}



