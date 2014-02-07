

package graphics.jointed;
import java.io.IOException;
//import java.util.* ;

import util.*;
import graphics.jointed.MS3DFile.* ;
import java.lang.reflect.Field;

import com.badlogic.gdx.assets.* ;
import com.badlogic.gdx.assets.loaders.* ;
import com.badlogic.gdx.files.FileHandle ;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Model ;
import com.badlogic.gdx.graphics.g3d.model.data.* ;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial.MaterialType;
import com.badlogic.gdx.math.* ;
import com.badlogic.gdx.utils.* ;






public class MS3DLoader extends ModelLoader<AssetLoaderParameters<Model>> {
	
	
	private FileHandle handle, baseDir ;
	private MS3DFile file ;
	private ModelData data ;
	
	private XML config = null;
	private float scale = 1.0f;
  private List <AnimRange> animRanges = new List <AnimRange> ();
  private static Table <String, String> validAnimNames = null ;
	
	
	private static class MS3DParams extends AssetLoaderParameters <Model> {
	  String xmlFile, xmlName;
	}
  
  public MS3DLoader(FileHandleResolver resolver) {
    super(resolver) ;
  }
	
	private static class AnimRange {
	  String name ;
	  float start, end, length ;
	  private ModelAnimation anim = null;
	}
	
	
  public static void beginLoading(
    String path, String file,
    String xmlFile, String xmlName,
    AssetManager manager
  ) {
    final MS3DParams params = new MS3DParams();
    params.xmlFile = path+""+xmlFile;
    params.xmlName = xmlName;
    manager.load(path+""+file, Model.class, params);
  }
  
	
	public ModelData loadModelData(
		FileHandle fileHandle,
		AssetLoaderParameters <Model> assetParams
	) {
		try {
			final DataInput input = new DataInput(fileHandle.read(), true);
			handle = fileHandle;
			baseDir = fileHandle.parent();
			file = new MS3DFile(input);
			
			final MS3DParams params = (MS3DParams) assetParams ;
			if (params != null && params.xmlName != null) {
	      XML xml = XML.load(params.xmlFile);
	      config = xml.matchChildValue("name", params.xmlName);
			}
			else config = null;
		}
		catch (Exception e) { I.report(e) ; return null ; }
		
    animRanges.clear();
    final AnimRange defaultRange = new AnimRange();
    defaultRange.name = "default";
    defaultRange.start = 0;
    defaultRange.end = file.iTotalFrames;
    defaultRange.length = defaultRange.end;
    animRanges.add(defaultRange) ;
    processXMLConfig();
    
		data = new ModelData() ;
		processMaterials();
		processMesh();
		processJoints();
		
		//I.say("Processing complete! "+data) ;
		return data ;
	}
	
	
	private void processXMLConfig() {
    if (config == null) return ;
    this.scale = config.getFloat("scale") ;
    
    loadAnimRanges(config.child("animations"));
    
    I.say("\nAnimation ranges are: ") ;
    for (AnimRange range : animRanges) {
      I.say("  "+range.name+" ("+range.start+" to "+range.end+")") ;
    }
  }
	
	
	private void loadAnimRanges(XML anims) {
    //
    //  If neccesary, initialise the table of valid animation names-
	  //  TODO:  try moving this to the JointModel class.
    if (validAnimNames == null) {
      validAnimNames = new Table <String, String> (100) ;
      for (Field field : AnimNames.class.getFields()) try {
        if (field.getType() != String.class) continue ;
        final String value = (String) field.get(null) ;
        validAnimNames.put(value, value) ;
      }
      catch (Exception e) {}
    }
    //
    //  Quit if there are no animations to load.  Otherwise, check each entry-
    if (anims == null || anims.numChildren() < 0) return ;
    addLoop: for (XML anim : anims.children()) {
      //
      //  First, check to ensure that this animation has an approved name:
      String name = anim.value("name") ;
      if (validAnimNames.get(name) == null) I.say(
        "WARNING: ANIMATION WITH IRREGULAR NAME: "+name+
        " IN MODEL: "+handle
      ) ;
      else name = (String) validAnimNames.get(name) ;
      for (AnimRange oldAnim : animRanges) {
        if (oldAnim.name.equals(name)) continue addLoop ;
      }
      //
      //  Either way, define the data-
      final float
        animStart  = Float.parseFloat(anim.value("start")),
        animEnd    = Float.parseFloat(anim.value("end")),
        animLength = Float.parseFloat(anim.value("duration")) ;
      final AnimRange newAnim = new AnimRange() ;
      newAnim.name = name ;
      newAnim.start = animStart;
      newAnim.end = animEnd;
      newAnim.length = animLength;
      animRanges.addFirst(newAnim) ;
    }
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
				
				verts[p++] = vert.vertex[0] * this.scale;
				verts[p++] = vert.vertex[1] * this.scale;
				verts[p++] = vert.vertex[2] * this.scale;
				
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
		
		//  ...Will *this* combine easily with multiple groups..?  ...No.
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
		//  Set up reference tables/items for lookup purposes-
    //final float fpsmod = 1 / (25 / file.fAnimationFPS);
		ModelNode rootBone = data.nodes.get(0);
		final ArrayMap <String, ModelNode>
		  jointTable = new ArrayMap <String, ModelNode> ();
		final ArrayMap <String, Matrix4>
		  boneMatrices = new ArrayMap <String, Matrix4> ();
		rootBone.parts[0].bones = boneMatrices;
		
		//
		//  Then establish proper animation sequences for every joint-
		for (MS3DJoint fileJoint : file.joints) {
			
      final String boneName = fileJoint.name ;
			final ModelNode childBone = new ModelNode();
			jointTable.put(boneName, childBone) ;
      boneMatrices.put(boneName, new Matrix4());
      
			childBone.id = boneName ;
			childBone.meshId = "mesh" ;
			childBone.rotation = fileJoint.matrix.getRotation(new Quaternion());
			childBone.translation = fileJoint.matrix.getTranslation(new Vector3());
			childBone.scale = new Vector3(1,1,1) ;
			childBone.translation.scl(this.scale);

	    //
	    //  Then, set up the correct sequence of animation keyframes for
	    //  this node-
	    for (int j = 0 ; j < fileJoint.positions.length ; j++) {
	      final ModelNodeKeyframe frame = new ModelNodeKeyframe();
	      frame.keytime = fileJoint.rotations[j].time ;// * fpsmod;
	      frame.translation = new Vector3(fileJoint.positions[j].data);
	      frame.translation.mul(fileJoint.matrix);
	      frame.translation.scl(this.scale);
	      final Quaternion
	        FE = MS3DFile.fromEuler(fileJoint.rotations[j].data),
	        JM = fileJoint.matrix.getRotation(new Quaternion());
	      frame.rotation = JM.mul(FE);
	      applyToMatchingRanges(frame, boneName);
	    }
		}
    
		//
		//  Assign each child node to it's parent-
    final Batch <ModelNode> orphans = new Batch <ModelNode> ();
    for (MS3DJoint fileJoint : file.joints) {
      final ModelNode childBone = jointTable.get(fileJoint.name);
      childBone.children = new ModelNode[fileJoint.children.size()];
      int i = 0;
      for (MS3DJoint j : fileJoint.children) {
        childBone.children[i++] = jointTable.get(j.name);
      }
      if (fileJoint.parent == null) orphans.add(childBone);
    }
    rootBone.children = orphans.toArray(ModelNode.class);
    
    //
    //  Then compile the total set of animations (and clear for later)-
    I.say("\nCompiled ranges are:") ;
    for (AnimRange range : this.animRanges) if (range.anim != null) {
      data.animations.add(range.anim);
      I.say("  "+range.anim.id) ;
      range.anim = null;
    }
    I.say("\n\n") ;
	}
	
	
	private void applyToMatchingRanges(ModelNodeKeyframe frame, String boneName) {
    final float time = frame.keytime;
    //I.say("  Looking for match for keyframe at: "+time) ;
    for (AnimRange range : animRanges) {
      //I.say("    Start/end are "+range.start+"/"+range.end) ;
      if (range.start <= time && range.end >= time) {
        final ModelNodeAnimation MNA = getMNA(range, boneName);
        if (MNA != null) MNA.keyframes.add(frame);
      }
    }
	}
	
	
	private ModelNodeAnimation getMNA(AnimRange range, String boneName) {
	  if (range.anim == null) {
	    range.anim = new ModelAnimation();
	    range.anim.id = range.name;
	  }
	  for (ModelNodeAnimation MNA : range.anim.nodeAnimations) {
	    if (MNA.nodeId.equals(boneName)) return MNA;
	  }
    final ModelNodeAnimation MNA = new ModelNodeAnimation();
    MNA.nodeId = boneName;
    range.anim.nodeAnimations.add(MNA);
    return MNA;
	}
}



