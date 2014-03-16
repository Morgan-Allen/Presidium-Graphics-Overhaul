
package src.graphics.kerai_src;

import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;



/**
 * A Node2 is part of a hierarchy of Node2s in a {@link Model}. A Node2 encodes
 * a transform relative to its parents. A Node2 can have child Node2s. Optionally
 * a Node2 can specify a {@link MeshPart} and a {@link Material} to be applied to the mesh part.
 * @author badlogic
 *
 */
public class Node2 {
    
  
  
  
    /** the id, may be null, FIXME is this unique? **/
    public String id;
    /** parent Node2, may be null **/
    public Node2 parent;
    /** child Node2s **/
    public final Array<Node2> children = new Array<Node2>(2);
    /** Whether this Node2 is currently being animated, if so the translation, rotation and scale values are not used. */
    public boolean isAnimated;
    /** the translation, relative to the parent, not modified by animations **/
    public final Vector3 translation = new Vector3();
    /** the rotation, relative to the parent, not modified by animations **/
    public final Quaternion rotation = new Quaternion(0, 0, 0, 1);
    /** the scale, relative to the parent, not modified by animations **/
    public final Vector3 scale = new Vector3(1, 1, 1);
    /** the local transform, based on translation/rotation/scale ({@link #calculateLocalTransform()}) or any applied animation **/
    public final Matrix4 localTransform = new Matrix4();
    /** the global transform, product of local transform and transform of the parent Node2, calculated via {@link #calculateWorldTransform()}**/
    public final Matrix4 globalTransform = new Matrix4();
    public Array<NodePart2> parts = new Array<NodePart2>(2);
    

    /**
     * Calculates the local transform based on the translation, scale and rotation
     * @return the local transform
     */
    public Matrix4 calculateLocalTransform() {
        if (!isAnimated)
            localTransform.set(translation, rotation, scale);
        return localTransform;
    }

    /**
     * Calculates the world transform; the product of local transform and the
     * parent's world transform. 
     * @return the world transform
     */
    public Matrix4 calculateWorldTransform() {
        if (parent == null) 
            globalTransform.set(localTransform);
        else
            globalTransform.set(parent.globalTransform).mul(localTransform);
        return globalTransform;
    }

    /**
     * Calculates the local and world transform of this Node2 and optionally all
     * its children.
     * 
     * @param recursive whether to calculate the local/world transforms for children.
     */
    public void calculateTransforms(boolean recursive) {
        calculateLocalTransform();
        calculateWorldTransform();

        if(recursive) {
            for(Node2 child: children) {
                child.calculateTransforms(true);
            }
        }
    }

    public void calculateBoneTransforms(boolean recursive) {
        for (final NodePart2 part : parts) {
            if (part.invBoneBindTransforms == null || part.bones == null || part.invBoneBindTransforms.size != part.bones.length)
                continue;
            final int n = part.invBoneBindTransforms.size;
            for (int i = 0; i < n; i++)
                part.bones[i].set(part.invBoneBindTransforms.keys[i].globalTransform).mul(part.invBoneBindTransforms.values[i]);
        }
        if(recursive) {
            for(Node2 child: children) {
                child.calculateBoneTransforms(true);
            }
        }
    }

    /** Calculate the bounding box of this Node2.
     * This is a potential slow operation, it is advised to cache the result. */
    public BoundingBox calculateBoundingBox(final BoundingBox out) {
        out.inf();
        return extendBoundingBox(out);
    }

    /** Calculate the bounding box of this Node2.
     * This is a potential slow operation, it is advised to cache the result. */
    public BoundingBox calculateBoundingBox(final BoundingBox out, boolean transform) {
        out.inf();
        return extendBoundingBox(out, transform);
    }

    /** Extends the bounding box with the bounds of this Node2.
     * This is a potential slow operation, it is advised to cache the result. */
    public BoundingBox extendBoundingBox(final BoundingBox out) {
        return extendBoundingBox(out, true);
    }

    /** Extends the bounding box with the bounds of this Node2.
     * This is a potential slow operation, it is advised to cache the result. */
    public BoundingBox extendBoundingBox(final BoundingBox out, boolean transform) {
        final int partCount = parts.size;
        for (int i = 0; i < partCount; i++) {
            final NodePart2 part = parts.get(i);
            if(part.enabled) {
                final MeshPart meshPart = part.meshPart;
                if (transform)
                    meshPart.mesh.extendBoundingBox(out, meshPart.indexOffset, meshPart.numVertices, globalTransform);
                else
                    meshPart.mesh.extendBoundingBox(out, meshPart.indexOffset, meshPart.numVertices);
            }
        }
        final int childCount = children.size;
        for (int i = 0; i < childCount; i++)
            children.get(i).extendBoundingBox(out);
        return out;
    }

    /** @param recursive false to fetch a root child only, true to search the entire Node2 tree for the specified Node2.
     * @return The Node2 with the specified id, or null if not found. */
    public Node2 getChild(final String id, boolean recursive, boolean ignoreCase) {
        return getNode(children, id, recursive, ignoreCase);
    }
    
    
    /** Helper method to recursive fetch a Node2 from an array
     * @param recursive false to fetch a root Node2 only, true to search the entire Node2 tree for the specified Node2.
     * @return The Node2 with the specified id, or null if not found. */
    public static Node2 getNode(final Array<Node2> Node2s, final String id, boolean recursive, boolean ignoreCase) {
        final int n = Node2s.size;
        Node2 Node2;
        if (ignoreCase) {
            for (int i = 0; i < n; i++)
                if ((Node2 = Node2s.get(i)).id.equalsIgnoreCase(id))
                    return Node2;
        } else {
            for (int i = 0; i < n; i++)
                if ((Node2 = Node2s.get(i)).id.equals(id))
                    return Node2;
        }
        if (recursive) {
            for (int i = 0; i < n; i++)
                if ((Node2 = getNode(Node2s.get(i).children, id, true, ignoreCase)) != null)
                    return Node2;
        }
        return null;
    }
}



