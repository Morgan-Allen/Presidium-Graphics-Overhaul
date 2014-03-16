package src.graphics.kerai_src;

/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

//package com.badlogic.gdx.graphics.g3d.model;

import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.model.*;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;

/**
 * A combination of {@link MeshPart} and {@link Material}, used to represent a
 * {@link Node2}'s graphical properties. A NodePart is the smallest visible part
 * of a {@link Model}, each NodePart implies a render call.
 * 
 * @author badlogic, Xoppa
 */



public class NodePart2 {

  public MeshPart meshPart;
  public Material material;

  /**
   * Mapping to each bone (node) and the inverse transform of the bind pose.
   * Will be used to fill the {@link #bones} array. May be null.
   */
  public ArrayMap<Node2, Matrix4> invBoneBindTransforms;
  /**
   * The current transformation (relative to the bind pose) of each bone, may be
   * null. When the part is skinned, this will be updated by a call to
   * {@link ModelInstance#calculateTransforms()}. Do not set or change this
   * value manually.
   */
  public Matrix4[] bones;

  /**
   * true by default. If set to false, this part will not participate in
   * rendering and bounding box calculation.
   */
  public boolean enabled = true;

  public NodePart2() {
  }

  public NodePart2(final MeshPart meshPart, final Material material) {
    this.meshPart = meshPart;
    this.material = material;
  }
  
  
  //  TODO: This is the part of interest.  Odds are, the vast majority of the
  //  rest can be dispensed with.
  
  public Renderable setRenderable(final Renderable out) {
    out.material = material;
    out.mesh = meshPart.mesh;
    out.meshPartOffset = meshPart.indexOffset;
    out.meshPartSize = meshPart.numVertices;
    out.primitiveType = meshPart.primitiveType;
    out.bones = bones;
    return out;
  }
}




