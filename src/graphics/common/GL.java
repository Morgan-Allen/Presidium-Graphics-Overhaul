

package src.graphics.common;
import java.nio.*;
import org.lwjgl.opengl.*;
import com.badlogic.gdx.*;



/**
 * Class for static OpenGL functions access
 * @author Kerai
 */
public class GL {
	// ------------------
	// GLCommon
	
	public static final int GL_GENERATE_MIPMAP = 0x8191;
	public static final int GL_TEXTURE_MAX_ANISOTROPY_EXT = 0x84FE;
	public static final int GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT = 0x84FF;

	public static final void glActiveTexture (int texture) {
		Gdx.gl20.glActiveTexture(texture);
	}

	public static final void glBindTexture (int target, int texture) {
		Gdx.gl20.glBindTexture(target, texture);
	}

	public static final void glBlendFunc (int sfactor, int dfactor) {
		Gdx.gl20.glBlendFunc(sfactor, dfactor);
	}

	public static final void glClear (int mask) {
		Gdx.gl20.glClear(mask);
	}

	public static final void glClearColor (float red, float green, float blue, float alpha) {
		Gdx.gl20.glClearColor(red, green, blue, alpha);
	}

	public static final void glClearDepthf (float depth) {
		Gdx.gl20.glClearDepthf(depth);
	}

	public static final void glClearStencil (int s) {
		Gdx.gl20.glClearStencil(s);
	}

	public static final void glColorMask (boolean red, boolean green, boolean blue, boolean alpha) {
		Gdx.gl20.glColorMask(red, green, blue, alpha);
	}

	public static final void glCompressedTexImage2D (int target, int level, int internalformat, int width, int height, int border, int imageSize, Buffer data) {
		Gdx.gl20.glCompressedTexImage2D(target, level, internalformat, width, height, border, imageSize, data);
	}

	public static final void glCompressedTexSubImage2D (int target, int level, int xoffset, int yoffset, int width, int height, int format, int imageSize, Buffer data) {
		Gdx.gl20.glCompressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, imageSize, data);
	}

	public static final void glCopyTexImage2D (int target, int level, int internalformat, int x, int y, int width, int height, int border) {
		Gdx.gl20.glCopyTexImage2D(target, level, internalformat, x, y, width, height, border);
	}

	public static final void glCopyTexSubImage2D (int target, int level, int xoffset, int yoffset, int x, int y, int width, int height) {
		Gdx.gl20.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);
	}

	public static final void glCullFace (int mode) {
		Gdx.gl20.glCullFace(mode);
	}

	public static final void glDeleteTextures (int n, IntBuffer textures) {
		Gdx.gl20.glDeleteTextures(n, textures);
	}

	public static final void glDepthFunc (int func) {
		Gdx.gl20.glDepthFunc(func);
	}

	public static final void glDepthMask (boolean flag) {
		Gdx.gl20.glDepthMask(flag);
	}

	public static final void glDepthRangef (float zNear, float zFar) {
		Gdx.gl20.glDepthRangef(zNear, zFar);
	}

	public static final void glDisable (int cap) {
		Gdx.gl20.glDisable(cap);
	}

	public static final void glDrawArrays (int mode, int first, int count) {
		Gdx.gl20.glDrawArrays(mode, first, count);
	}

	public static final void glDrawElements (int mode, int count, int type, Buffer indices) {
		Gdx.gl20.glDrawElements(mode, count, type, indices);
	}

	public static final void glEnable (int cap) {
		Gdx.gl20.glEnable(cap);
	}

	public static final void glFinish () {
		Gdx.gl20.glFinish();
	}

	public static final void glFlush () {
		Gdx.gl20.glFlush();
	}

	public static final void glFrontFace (int mode) {
		Gdx.gl20.glFrontFace(mode);
	}

	public static final void glGenTextures (int n, IntBuffer textures) {
		Gdx.gl20.glGenTextures(n, textures);
	}

	public static final int glGetError () {
		return Gdx.gl20.glGetError();
	}

	public static final void glGetIntegerv (int pname, IntBuffer params) {
		Gdx.gl20.glGetIntegerv(pname, params);
	}

	public static final String glGetString (int name) {
		return Gdx.gl20.glGetString(name);
	}

	public static final void glHint (int target, int mode) {
		Gdx.gl20.glHint(target, mode);
	}

	public static final void glLineWidth (float width) {
		Gdx.gl20.glLineWidth(width);
	}

	public static final void glPixelStorei (int pname, int param) {
		Gdx.gl20.glPixelStorei(pname, param);
	}

	public static final void glPolygonOffset (float factor, float units) {
		Gdx.gl20.glPolygonOffset(factor, units);
	}

	public static final void glReadPixels (int x, int y, int width, int height, int format, int type, Buffer pixels) {
		Gdx.gl20.glReadPixels(x, y, width, height, format, type, pixels);
	}

	public static final void glScissor (int x, int y, int width, int height) {
		Gdx.gl20.glScissor(x, y, width, height);
	}

	public static final void glStencilFunc (int func, int ref, int mask) {
		Gdx.gl20.glStencilFunc(func, ref, mask);
	}

	public static final void glStencilMask (int mask) {
		Gdx.gl20.glStencilMask(mask);
	}

	public static final void glStencilOp (int fail, int zfail, int zpass) {
		Gdx.gl20.glStencilOp(fail, zfail, zpass);
	}

	public static final void glTexImage2D (int target, int level, int internalformat, int width, int height, int border, int format, int type, Buffer pixels) {
		Gdx.gl20.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
	}

	public static final void glTexParameterf (int target, int pname, float param) {
		Gdx.gl20.glTexParameterf(target, pname, param);
	}

	public static final void glTexSubImage2D (int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, Buffer pixels) {
		Gdx.gl20.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels);
	}

	public static final void glViewport (int x, int y, int width, int height) {
		Gdx.gl20.glViewport(x, y, width, height);
	}
	
	
	// ------------------
	// GL20
	
	
	public static final int GL_ES_VERSION_2_0 = 1;
	public static final int GL_DEPTH_BUFFER_BIT = 0x00000100;
	public static final int GL_STENCIL_BUFFER_BIT = 0x00000400;
	public static final int GL_COLOR_BUFFER_BIT = 0x00004000;
	public static final int GL_FALSE = 0;
	public static final int GL_TRUE = 1;
	public static final int GL_POINTS = 0x0000;
	public static final int GL_LINES = 0x0001;
	public static final int GL_LINE_LOOP = 0x0002;
	public static final int GL_LINE_STRIP = 0x0003;
	public static final int GL_TRIANGLES = 0x0004;
	public static final int GL_TRIANGLE_STRIP = 0x0005;
	public static final int GL_TRIANGLE_FAN = 0x0006;
	public static final int GL_ZERO = 0;
	public static final int GL_ONE = 1;
	public static final int GL_SRC_COLOR = 0x0300;
	public static final int GL_ONE_MINUS_SRC_COLOR = 0x0301;
	public static final int GL_SRC_ALPHA = 0x0302;
	public static final int GL_ONE_MINUS_SRC_ALPHA = 0x0303;
	public static final int GL_DST_ALPHA = 0x0304;
	public static final int GL_ONE_MINUS_DST_ALPHA = 0x0305;
	public static final int GL_DST_COLOR = 0x0306;
	public static final int GL_ONE_MINUS_DST_COLOR = 0x0307;
	public static final int GL_SRC_ALPHA_SATURATE = 0x0308;
	public static final int GL_FUNC_ADD = 0x8006;
	public static final int GL_BLEND_EQUATION = 0x8009;
	public static final int GL_BLEND_EQUATION_RGB = 0x8009;
	public static final int GL_BLEND_EQUATION_ALPHA = 0x883D;
	public static final int GL_FUNC_SUBTRACT = 0x800A;
	public static final int GL_FUNC_REVERSE_SUBTRACT = 0x800B;
	public static final int GL_BLEND_DST_RGB = 0x80C8;
	public static final int GL_BLEND_SRC_RGB = 0x80C9;
	public static final int GL_BLEND_DST_ALPHA = 0x80CA;
	public static final int GL_BLEND_SRC_ALPHA = 0x80CB;
	public static final int GL_CONSTANT_COLOR = 0x8001;
	public static final int GL_ONE_MINUS_CONSTANT_COLOR = 0x8002;
	public static final int GL_CONSTANT_ALPHA = 0x8003;
	public static final int GL_ONE_MINUS_CONSTANT_ALPHA = 0x8004;
	public static final int GL_BLEND_COLOR = 0x8005;
	public static final int GL_ARRAY_BUFFER = 0x8892;
	public static final int GL_ELEMENT_ARRAY_BUFFER = 0x8893;
	public static final int GL_ARRAY_BUFFER_BINDING = 0x8894;
	public static final int GL_ELEMENT_ARRAY_BUFFER_BINDING = 0x8895;
	public static final int GL_STREAM_DRAW = 0x88E0;
	public static final int GL_STATIC_DRAW = 0x88E4;
	public static final int GL_DYNAMIC_DRAW = 0x88E8;
	public static final int GL_BUFFER_SIZE = 0x8764;
	public static final int GL_BUFFER_USAGE = 0x8765;
	public static final int GL_CURRENT_VERTEX_ATTRIB = 0x8626;
	public static final int GL_FRONT = 0x0404;
	public static final int GL_BACK = 0x0405;
	public static final int GL_FRONT_AND_BACK = 0x0408;
	public static final int GL_TEXTURE_2D = 0x0DE1;
	public static final int GL_CULL_FACE = 0x0B44;
	public static final int GL_BLEND = 0x0BE2;
	public static final int GL_DITHER = 0x0BD0;
	public static final int GL_STENCIL_TEST = 0x0B90;
	public static final int GL_DEPTH_TEST = 0x0B71;
	public static final int GL_SCISSOR_TEST = 0x0C11;
	public static final int GL_POLYGON_OFFSET_FILL = 0x8037;
	public static final int GL_SAMPLE_ALPHA_TO_COVERAGE = 0x809E;
	public static final int GL_SAMPLE_COVERAGE = 0x80A0;
	public static final int GL_NO_ERROR = 0;
	public static final int GL_INVALID_ENUM = 0x0500;
	public static final int GL_INVALID_VALUE = 0x0501;
	public static final int GL_INVALID_OPERATION = 0x0502;
	public static final int GL_OUT_OF_MEMORY = 0x0505;
	public static final int GL_CW = 0x0900;
	public static final int GL_CCW = 0x0901;
	public static final int GL_LINE_WIDTH = 0x0B21;
	public static final int GL_ALIASED_POINT_SIZE_RANGE = 0x846D;
	public static final int GL_ALIASED_LINE_WIDTH_RANGE = 0x846E;
	public static final int GL_CULL_FACE_MODE = 0x0B45;
	public static final int GL_FRONT_FACE = 0x0B46;
	public static final int GL_DEPTH_RANGE = 0x0B70;
	public static final int GL_DEPTH_WRITEMASK = 0x0B72;
	public static final int GL_DEPTH_CLEAR_VALUE = 0x0B73;
	public static final int GL_DEPTH_FUNC = 0x0B74;
	public static final int GL_STENCIL_CLEAR_VALUE = 0x0B91;
	public static final int GL_STENCIL_FUNC = 0x0B92;
	public static final int GL_STENCIL_FAIL = 0x0B94;
	public static final int GL_STENCIL_PASS_DEPTH_FAIL = 0x0B95;
	public static final int GL_STENCIL_PASS_DEPTH_PASS = 0x0B96;
	public static final int GL_STENCIL_REF = 0x0B97;
	public static final int GL_STENCIL_VALUE_MASK = 0x0B93;
	public static final int GL_STENCIL_WRITEMASK = 0x0B98;
	public static final int GL_STENCIL_BACK_FUNC = 0x8800;
	public static final int GL_STENCIL_BACK_FAIL = 0x8801;
	public static final int GL_STENCIL_BACK_PASS_DEPTH_FAIL = 0x8802;
	public static final int GL_STENCIL_BACK_PASS_DEPTH_PASS = 0x8803;
	public static final int GL_STENCIL_BACK_REF = 0x8CA3;
	public static final int GL_STENCIL_BACK_VALUE_MASK = 0x8CA4;
	public static final int GL_STENCIL_BACK_WRITEMASK = 0x8CA5;
	public static final int GL_VIEWPORT = 0x0BA2;
	public static final int GL_SCISSOR_BOX = 0x0C10;
	public static final int GL_COLOR_CLEAR_VALUE = 0x0C22;
	public static final int GL_COLOR_WRITEMASK = 0x0C23;
	public static final int GL_UNPACK_ALIGNMENT = 0x0CF5;
	public static final int GL_PACK_ALIGNMENT = 0x0D05;
	public static final int GL_MAX_TEXTURE_SIZE = 0x0D33;
	public static final int GL_MAX_TEXTURE_UNITS = 0x84E2;
	public static final int GL_MAX_VIEWPORT_DIMS = 0x0D3A;
	public static final int GL_SUBPIXEL_BITS = 0x0D50;
	public static final int GL_RED_BITS = 0x0D52;
	public static final int GL_GREEN_BITS = 0x0D53;
	public static final int GL_BLUE_BITS = 0x0D54;
	public static final int GL_ALPHA_BITS = 0x0D55;
	public static final int GL_DEPTH_BITS = 0x0D56;
	public static final int GL_STENCIL_BITS = 0x0D57;
	public static final int GL_POLYGON_OFFSET_UNITS = 0x2A00;
	public static final int GL_POLYGON_OFFSET_FACTOR = 0x8038;
	public static final int GL_TEXTURE_BINDING_2D = 0x8069;
	public static final int GL_SAMPLE_BUFFERS = 0x80A8;
	public static final int GL_SAMPLES = 0x80A9;
	public static final int GL_SAMPLE_COVERAGE_VALUE = 0x80AA;
	public static final int GL_SAMPLE_COVERAGE_INVERT = 0x80AB;
	public static final int GL_NUM_COMPRESSED_TEXTURE_FORMATS = 0x86A2;
	public static final int GL_COMPRESSED_TEXTURE_FORMATS = 0x86A3;
	public static final int GL_DONT_CARE = 0x1100;
	public static final int GL_FASTEST = 0x1101;
	public static final int GL_NICEST = 0x1102;
	public static final int GL_GENERATE_MIPMAP_HINT = 0x8192;
	public static final int GL_BYTE = 0x1400;
	public static final int GL_UNSIGNED_BYTE = 0x1401;
	public static final int GL_SHORT = 0x1402;
	public static final int GL_UNSIGNED_SHORT = 0x1403;
	public static final int GL_INT = 0x1404;
	public static final int GL_UNSIGNED_INT = 0x1405;
	public static final int GL_FLOAT = 0x1406;
	public static final int GL_FIXED = 0x140C;
	public static final int GL_DEPTH_COMPONENT = 0x1902;
	public static final int GL_ALPHA = 0x1906;
	public static final int GL_RGB = 0x1907;
	public static final int GL_RGBA = 0x1908;
	public static final int GL_LUMINANCE = 0x1909;
	public static final int GL_LUMINANCE_ALPHA = 0x190A;
	public static final int GL_UNSIGNED_SHORT_4_4_4_4 = 0x8033;
	public static final int GL_UNSIGNED_SHORT_5_5_5_1 = 0x8034;
	public static final int GL_UNSIGNED_SHORT_5_6_5 = 0x8363;
	public static final int GL_FRAGMENT_SHADER = 0x8B30;
	public static final int GL_VERTEX_SHADER = 0x8B31;
	public static final int GL_MAX_VERTEX_ATTRIBS = 0x8869;
	public static final int GL_MAX_VERTEX_UNIFORM_VECTORS = 0x8DFB;
	public static final int GL_MAX_VARYING_VECTORS = 0x8DFC;
	public static final int GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS = 0x8B4D;
	public static final int GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS = 0x8B4C;
	public static final int GL_MAX_TEXTURE_IMAGE_UNITS = 0x8872;
	public static final int GL_MAX_FRAGMENT_UNIFORM_VECTORS = 0x8DFD;
	public static final int GL_SHADER_TYPE = 0x8B4F;
	public static final int GL_DELETE_STATUS = 0x8B80;
	public static final int GL_LINK_STATUS = 0x8B82;
	public static final int GL_VALIDATE_STATUS = 0x8B83;
	public static final int GL_ATTACHED_SHADERS = 0x8B85;
	public static final int GL_ACTIVE_UNIFORMS = 0x8B86;
	public static final int GL_ACTIVE_UNIFORM_MAX_LENGTH = 0x8B87;
	public static final int GL_ACTIVE_ATTRIBUTES = 0x8B89;
	public static final int GL_ACTIVE_ATTRIBUTE_MAX_LENGTH = 0x8B8A;
	public static final int GL_SHADING_LANGUAGE_VERSION = 0x8B8C;
	public static final int GL_CURRENT_PROGRAM = 0x8B8D;
	public static final int GL_NEVER = 0x0200;
	public static final int GL_LESS = 0x0201;
	public static final int GL_EQUAL = 0x0202;
	public static final int GL_LEQUAL = 0x0203;
	public static final int GL_GREATER = 0x0204;
	public static final int GL_NOTEQUAL = 0x0205;
	public static final int GL_GEQUAL = 0x0206;
	public static final int GL_ALWAYS = 0x0207;
	public static final int GL_KEEP = 0x1E00;
	public static final int GL_REPLACE = 0x1E01;
	public static final int GL_INCR = 0x1E02;
	public static final int GL_DECR = 0x1E03;
	public static final int GL_INVERT = 0x150A;
	public static final int GL_INCR_WRAP = 0x8507;
	public static final int GL_DECR_WRAP = 0x8508;
	public static final int GL_VENDOR = 0x1F00;
	public static final int GL_RENDERER = 0x1F01;
	public static final int GL_VERSION = 0x1F02;
	public static final int GL_EXTENSIONS = 0x1F03;
	public static final int GL_NEAREST = 0x2600;
	public static final int GL_LINEAR = 0x2601;
	public static final int GL_NEAREST_MIPMAP_NEAREST = 0x2700;
	public static final int GL_LINEAR_MIPMAP_NEAREST = 0x2701;
	public static final int GL_NEAREST_MIPMAP_LINEAR = 0x2702;
	public static final int GL_LINEAR_MIPMAP_LINEAR = 0x2703;
	public static final int GL_TEXTURE_MAG_FILTER = 0x2800;
	public static final int GL_TEXTURE_MIN_FILTER = 0x2801;
	public static final int GL_TEXTURE_WRAP_S = 0x2802;
	public static final int GL_TEXTURE_WRAP_T = 0x2803;
	public static final int GL_TEXTURE = 0x1702;
	public static final int GL_TEXTURE_CUBE_MAP = 0x8513;
	public static final int GL_TEXTURE_BINDING_CUBE_MAP = 0x8514;
	public static final int GL_TEXTURE_CUBE_MAP_POSITIVE_X = 0x8515;
	public static final int GL_TEXTURE_CUBE_MAP_NEGATIVE_X = 0x8516;
	public static final int GL_TEXTURE_CUBE_MAP_POSITIVE_Y = 0x8517;
	public static final int GL_TEXTURE_CUBE_MAP_NEGATIVE_Y = 0x8518;
	public static final int GL_TEXTURE_CUBE_MAP_POSITIVE_Z = 0x8519;
	public static final int GL_TEXTURE_CUBE_MAP_NEGATIVE_Z = 0x851A;
	public static final int GL_MAX_CUBE_MAP_TEXTURE_SIZE = 0x851C;
	public static final int GL_TEXTURE0 = 0x84C0;
	public static final int GL_TEXTURE1 = 0x84C1;
	public static final int GL_TEXTURE2 = 0x84C2;
	public static final int GL_TEXTURE3 = 0x84C3;
	public static final int GL_TEXTURE4 = 0x84C4;
	public static final int GL_TEXTURE5 = 0x84C5;
	public static final int GL_TEXTURE6 = 0x84C6;
	public static final int GL_TEXTURE7 = 0x84C7;
	public static final int GL_TEXTURE8 = 0x84C8;
	public static final int GL_TEXTURE9 = 0x84C9;
	public static final int GL_TEXTURE10 = 0x84CA;
	public static final int GL_TEXTURE11 = 0x84CB;
	public static final int GL_TEXTURE12 = 0x84CC;
	public static final int GL_TEXTURE13 = 0x84CD;
	public static final int GL_TEXTURE14 = 0x84CE;
	public static final int GL_TEXTURE15 = 0x84CF;
	public static final int GL_TEXTURE16 = 0x84D0;
	public static final int GL_TEXTURE17 = 0x84D1;
	public static final int GL_TEXTURE18 = 0x84D2;
	public static final int GL_TEXTURE19 = 0x84D3;
	public static final int GL_TEXTURE20 = 0x84D4;
	public static final int GL_TEXTURE21 = 0x84D5;
	public static final int GL_TEXTURE22 = 0x84D6;
	public static final int GL_TEXTURE23 = 0x84D7;
	public static final int GL_TEXTURE24 = 0x84D8;
	public static final int GL_TEXTURE25 = 0x84D9;
	public static final int GL_TEXTURE26 = 0x84DA;
	public static final int GL_TEXTURE27 = 0x84DB;
	public static final int GL_TEXTURE28 = 0x84DC;
	public static final int GL_TEXTURE29 = 0x84DD;
	public static final int GL_TEXTURE30 = 0x84DE;
	public static final int GL_TEXTURE31 = 0x84DF;
	public static final int GL_ACTIVE_TEXTURE = 0x84E0;
	public static final int GL_REPEAT = 0x2901;
	public static final int GL_CLAMP_TO_EDGE = 0x812F;
	public static final int GL_MIRRORED_REPEAT = 0x8370;
	public static final int GL_FLOAT_VEC2 = 0x8B50;
	public static final int GL_FLOAT_VEC3 = 0x8B51;
	public static final int GL_FLOAT_VEC4 = 0x8B52;
	public static final int GL_INT_VEC2 = 0x8B53;
	public static final int GL_INT_VEC3 = 0x8B54;
	public static final int GL_INT_VEC4 = 0x8B55;
	public static final int GL_BOOL = 0x8B56;
	public static final int GL_BOOL_VEC2 = 0x8B57;
	public static final int GL_BOOL_VEC3 = 0x8B58;
	public static final int GL_BOOL_VEC4 = 0x8B59;
	public static final int GL_FLOAT_MAT2 = 0x8B5A;
	public static final int GL_FLOAT_MAT3 = 0x8B5B;
	public static final int GL_FLOAT_MAT4 = 0x8B5C;
	public static final int GL_SAMPLER_2D = 0x8B5E;
	public static final int GL_SAMPLER_CUBE = 0x8B60;
	public static final int GL_VERTEX_ATTRIB_ARRAY_ENABLED = 0x8622;
	public static final int GL_VERTEX_ATTRIB_ARRAY_SIZE = 0x8623;
	public static final int GL_VERTEX_ATTRIB_ARRAY_STRIDE = 0x8624;
	public static final int GL_VERTEX_ATTRIB_ARRAY_TYPE = 0x8625;
	public static final int GL_VERTEX_ATTRIB_ARRAY_NORMALIZED = 0x886A;
	public static final int GL_VERTEX_ATTRIB_ARRAY_POINTER = 0x8645;
	public static final int GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING = 0x889F;
	public static final int GL_IMPLEMENTATION_COLOR_READ_TYPE = 0x8B9A;
	public static final int GL_IMPLEMENTATION_COLOR_READ_FORMAT = 0x8B9B;
	public static final int GL_COMPILE_STATUS = 0x8B81;
	public static final int GL_INFO_LOG_LENGTH = 0x8B84;
	public static final int GL_SHADER_SOURCE_LENGTH = 0x8B88;
	public static final int GL_SHADER_COMPILER = 0x8DFA;
	public static final int GL_SHADER_BINARY_FORMATS = 0x8DF8;
	public static final int GL_NUM_SHADER_BINARY_FORMATS = 0x8DF9;
	public static final int GL_LOW_FLOAT = 0x8DF0;
	public static final int GL_MEDIUM_FLOAT = 0x8DF1;
	public static final int GL_HIGH_FLOAT = 0x8DF2;
	public static final int GL_LOW_INT = 0x8DF3;
	public static final int GL_MEDIUM_INT = 0x8DF4;
	public static final int GL_HIGH_INT = 0x8DF5;
	public static final int GL_FRAMEBUFFER = 0x8D40;
	public static final int GL_RENDERBUFFER = 0x8D41;
	public static final int GL_RGBA4 = 0x8056;
	public static final int GL_RGB5_A1 = 0x8057;
	public static final int GL_RGB565 = 0x8D62;
	public static final int GL_DEPTH_COMPONENT16 = 0x81A5;
	public static final int GL_STENCIL_INDEX = 0x1901;
	public static final int GL_STENCIL_INDEX8 = 0x8D48;
	public static final int GL_RENDERBUFFER_WIDTH = 0x8D42;
	public static final int GL_RENDERBUFFER_HEIGHT = 0x8D43;
	public static final int GL_RENDERBUFFER_INTERNAL_FORMAT = 0x8D44;
	public static final int GL_RENDERBUFFER_RED_SIZE = 0x8D50;
	public static final int GL_RENDERBUFFER_GREEN_SIZE = 0x8D51;
	public static final int GL_RENDERBUFFER_BLUE_SIZE = 0x8D52;
	public static final int GL_RENDERBUFFER_ALPHA_SIZE = 0x8D53;
	public static final int GL_RENDERBUFFER_DEPTH_SIZE = 0x8D54;
	public static final int GL_RENDERBUFFER_STENCIL_SIZE = 0x8D55;
	public static final int GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE = 0x8CD0;
	public static final int GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME = 0x8CD1;
	public static final int GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL = 0x8CD2;
	public static final int GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE = 0x8CD3;
	public static final int GL_COLOR_ATTACHMENT0 = 0x8CE0;
	public static final int GL_DEPTH_ATTACHMENT = 0x8D00;
	public static final int GL_STENCIL_ATTACHMENT = 0x8D20;
	public static final int GL_NONE = 0;
	public static final int GL_FRAMEBUFFER_COMPLETE = 0x8CD5;
	public static final int GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT = 0x8CD6;
	public static final int GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT = 0x8CD7;
	public static final int GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS = 0x8CD9;
	public static final int GL_FRAMEBUFFER_UNSUPPORTED = 0x8CDD;
	public static final int GL_FRAMEBUFFER_BINDING = 0x8CA6;
	public static final int GL_RENDERBUFFER_BINDING = 0x8CA7;
	public static final int GL_MAX_RENDERBUFFER_SIZE = 0x84E8;
	public static final int GL_INVALID_FRAMEBUFFER_OPERATION = 0x0506;
	public static final int GL_VERTEX_PROGRAM_POINT_SIZE = 0x8642;

	// Extensions
	public static final int GL_COVERAGE_BUFFER_BIT_NV = 0x8000;

	public static final void glAttachShader (int program, int shader) {
		Gdx.gl20.glAttachShader(program, shader);
	}

	public static final void glBindAttribLocation (int program, int index, String name) {
		Gdx.gl20.glBindAttribLocation(program, index, name);
	}

	public static final void glBindBuffer (int target, int buffer) {
		Gdx.gl20.glBindBuffer(target, buffer);
	}

	public static final void glBindFramebuffer (int target, int framebuffer) {
		Gdx.gl20.glBindFramebuffer(target, framebuffer);
	}

	public static final void glBindRenderbuffer (int target, int renderbuffer) {
		Gdx.gl20.glBindRenderbuffer(target, renderbuffer);
	}

	public static final void glBlendColor (float red, float green, float blue, float alpha) {
		Gdx.gl20.glBlendColor(red, green, blue, alpha);
	}

	public static final void glBlendEquation (int mode) {
		Gdx.gl20.glBlendEquation(mode);
	}

	public static final void glBlendEquationSeparate (int modeRGB, int modeAlpha) {
		Gdx.gl20.glBlendEquationSeparate(modeRGB, modeAlpha);
	}

	public static final void glBlendFuncSeparate (int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
		Gdx.gl20.glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
	}

	public static final void glBufferData (int target, int size, Buffer data, int usage) {
		Gdx.gl20.glBufferData(target, size, data, usage);
	}

	public static final void glBufferSubData (int target, int offset, int size, Buffer data) {
		Gdx.gl20.glBufferSubData(target, offset, size, data);
	}

	public static final int glCheckFramebufferStatus (int target) {
		return Gdx.gl20.glCheckFramebufferStatus(target);
	}

	public static final void glCompileShader (int shader) {
		Gdx.gl20.glCompileShader(shader);
	}

	public static final int glCreateProgram () {
		return Gdx.gl20.glCreateProgram();
	}

	public static final int glCreateShader (int type) {
		return Gdx.gl20.glCreateShader(type);
	}

	public static final void glDeleteBuffers (int n, IntBuffer buffers) {
		Gdx.gl20.glDeleteBuffers(n, buffers);
	}

	public static final void glDeleteFramebuffers (int n, IntBuffer framebuffers) {
		Gdx.gl20.glDeleteFramebuffers(n, framebuffers);
	}

	public static final void glDeleteProgram (int program) {
		Gdx.gl20.glDeleteProgram(program);
	}

	public static final void glDeleteRenderbuffers (int n, IntBuffer renderbuffers) {
		Gdx.gl20.glDeleteRenderbuffers(n, renderbuffers);
	}

	public static final void glDeleteShader (int shader) {
		Gdx.gl20.glDeleteShader(shader);
	}

	public static final void glDetachShader (int program, int shader) {
		Gdx.gl20.glDetachShader(program, shader);
	}

	public static final void glDisableVertexAttribArray (int index) {
		Gdx.gl20.glDisableVertexAttribArray(index);
	}

	public static final void glDrawElements (int mode, int count, int type, int indices) {
		Gdx.gl20.glDrawElements(mode, count, type, indices);
	}

	public static final void glEnableVertexAttribArray (int index) {
		Gdx.gl20.glEnableVertexAttribArray(index);
	}

	public static final void glFramebufferRenderbuffer (int target, int attachment, int renderbuffertarget, int renderbuffer) {
		Gdx.gl20.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
	}

	public static final void glFramebufferTexture2D (int target, int attachment, int textarget, int texture, int level) {
		Gdx.gl20.glFramebufferTexture2D(target, attachment, textarget, texture, level);
	}

	public static final void glGenBuffers (int n, IntBuffer buffers) {
		Gdx.gl20.glGenBuffers(n, buffers);
	}

	public static final void glGenerateMipmap (int target) {
		Gdx.gl20.glGenerateMipmap(target);
	}

	public static final void glGenFramebuffers (int n, IntBuffer framebuffers) {
		Gdx.gl20.glGenFramebuffers(n, framebuffers);
	}

	public static final void glGenRenderbuffers (int n, IntBuffer renderbuffers) {
		Gdx.gl20.glGenRenderbuffers(n, renderbuffers);
	}

	// deviates
	public static final String glGetActiveAttrib (int program, int index, IntBuffer size, Buffer type) {
		return Gdx.gl20.glGetActiveAttrib(program, index, size, type);
	}

	// deviates
	public static final String glGetActiveUniform (int program, int index, IntBuffer size, Buffer type) {
		return Gdx.gl20.glGetActiveUniform(program, index, size, type);
	}

	public static final void glGetAttachedShaders (int program, int maxcount, Buffer count, IntBuffer shaders) {
		Gdx.gl20.glGetAttachedShaders(program, maxcount, count, shaders);
	}

	public static final int glGetAttribLocation (int program, String name) {
		return Gdx.gl20.glGetAttribLocation(program, name);
	}

	public static final void glGetBooleanv (int pname, Buffer params) {
		Gdx.gl20.glGetBooleanv(pname, params);
	}

	public static final void glGetBufferParameteriv (int target, int pname, IntBuffer params) {
		Gdx.gl20.glGetBufferParameteriv(target, pname, params);
	}

	public static final void glGetFloatv (int pname, FloatBuffer params) {
		Gdx.gl20.glGetFloatv(pname, params);
	}

	public static final void glGetFramebufferAttachmentParameteriv (int target, int attachment, int pname, IntBuffer params) {
		Gdx.gl20.glGetFramebufferAttachmentParameteriv(target, attachment, pname, params);
	}

	public static final void glGetProgramiv (int program, int pname, IntBuffer params) {
		Gdx.gl20.glGetProgramiv(program, pname, params);
	}

	// deviates
	public static final String glGetProgramInfoLog (int program) {
		return Gdx.gl20.glGetProgramInfoLog(program);
	}

	public static final void glGetRenderbufferParameteriv (int target, int pname, IntBuffer params) {
		Gdx.gl20.glGetRenderbufferParameteriv(target, pname, params);
	}

	public static final void glGetShaderiv (int shader, int pname, IntBuffer params) {
		Gdx.gl20.glGetShaderiv(shader, pname, params);
	}

	// deviates
	public static final String glGetShaderInfoLog (int shader) {
		return Gdx.gl20.glGetShaderInfoLog(shader);
	}

	public static final void glGetShaderPrecisionFormat (int shadertype, int precisiontype, IntBuffer range, IntBuffer precision) {
		Gdx.gl20.glGetShaderPrecisionFormat(shadertype, precisiontype, range, precision);
	}

	public static final void glGetTexParameterfv (int target, int pname, FloatBuffer params) {
		Gdx.gl20.glGetTexParameterfv(target, pname, params);
	}

	public static final void glGetTexParameteriv (int target, int pname, IntBuffer params) {
		Gdx.gl20.glGetTexParameteriv(target, pname, params);
	}

	public static final void glGetUniformfv (int program, int location, FloatBuffer params) {
		Gdx.gl20.glGetUniformfv(program, location, params);
	}

	public static final void glGetUniformiv (int program, int location, IntBuffer params) {
		Gdx.gl20.glGetUniformiv(program, location, params);
	}

	public static final int glGetUniformLocation (int program, String name) {
		return Gdx.gl20.glGetUniformLocation(program, name);
	}

	public static final void glGetVertexAttribfv (int index, int pname, FloatBuffer params) {
		Gdx.gl20.glGetVertexAttribfv(index, pname, params);
	}

	public static final void glGetVertexAttribiv (int index, int pname, IntBuffer params) {
		Gdx.gl20.glGetVertexAttribiv(index, pname, params);
	}

	public static final void glGetVertexAttribPointerv (int index, int pname, Buffer pointer) {
		Gdx.gl20.glGetVertexAttribPointerv(index, pname, pointer);
	}

	public static final boolean glIsBuffer (int buffer) {
		return Gdx.gl20.glIsBuffer(buffer);
	}

	public static final boolean glIsEnabled (int cap) {
		return Gdx.gl20.glIsEnabled(cap);
	}

	public static final boolean glIsFramebuffer (int framebuffer) {
		return Gdx.gl20.glIsFramebuffer(framebuffer);
	}

	public static final boolean glIsProgram (int program) {
		return Gdx.gl20.glIsProgram(program);
	}

	public static final boolean glIsRenderbuffer (int renderbuffer) {
		return Gdx.gl20.glIsRenderbuffer(renderbuffer);
	}

	public static final boolean glIsShader (int shader) {
		return Gdx.gl20.glIsShader(shader);
	}

	public static final boolean glIsTexture (int texture) {
		return Gdx.gl20.glIsTexture(texture);
	}

	public static final void glLinkProgram (int program) {
		Gdx.gl20.glLinkProgram(program);
	}

	public static final void glReleaseShaderCompiler () {
		Gdx.gl20.glReleaseShaderCompiler();
	}

	public static final void glRenderbufferStorage (int target, int internalformat, int width, int height) {
		Gdx.gl20.glRenderbufferStorage(target, internalformat, width, height);
	}

	public static final void glSampleCoverage (float value, boolean invert) {
		Gdx.gl20.glSampleCoverage(value, invert);
	}

	public static final void glShaderBinary (int n, IntBuffer shaders, int binaryformat, Buffer binary, int length) {
		Gdx.gl20.glShaderBinary(n, shaders, binaryformat, binary, length);
	}

	// Deviates
	public static final void glShaderSource (int shader, String string) {
		Gdx.gl20.glShaderSource(shader, string);
	}

	public static final void glStencilFuncSeparate (int face, int func, int ref, int mask) {
		Gdx.gl20.glStencilFuncSeparate(face, func, ref, mask);
	}

	public static final void glStencilMaskSeparate (int face, int mask) {
		Gdx.gl20.glStencilMaskSeparate(face, mask);
	}

	public static final void glStencilOpSeparate (int face, int fail, int zfail, int zpass) {
		Gdx.gl20.glStencilOpSeparate(face, fail, zfail, zpass);
	}

	public static final void glTexParameterfv (int target, int pname, FloatBuffer params) {
		Gdx.gl20.glTexParameterfv(target, pname, params);
	}

	public static final void glTexParameteri (int target, int pname, int param) {
		Gdx.gl20.glTexParameteri(target, pname, param);
	}

	public static final void glTexParameteriv (int target, int pname, IntBuffer params) {
		Gdx.gl20.glTexParameteriv(target, pname, params);
	}

	public static final void glUniform1f (int location, float x) {
		Gdx.gl20.glUniform1f(location, x);
	}

	public static final void glUniform1fv (int location, int count, FloatBuffer v) {
		Gdx.gl20.glUniform1fv(location, count, v);
	}

	public static final void glUniform1i (int location, int x) {
		Gdx.gl20.glUniform1i(location, x);
	}

	public static final void glUniform1iv (int location, int count, IntBuffer v) {
		Gdx.gl20.glUniform1iv(location, count, v);
	}

	public static final void glUniform2f (int location, float x, float y) {
		Gdx.gl20.glUniform2f(location, x, y);
	}

	public static final void glUniform2fv (int location, int count, FloatBuffer v) {
		Gdx.gl20.glUniform2fv(location, count, v);
	}

	public static final void glUniform2i (int location, int x, int y) {
		Gdx.gl20.glUniform2i(location, x, y);
	}

	public static final void glUniform2iv (int location, int count, IntBuffer v) {
		Gdx.gl20.glUniform2iv(location, count, v);
	}

	public static final void glUniform3f (int location, float x, float y, float z) {
		Gdx.gl20.glUniform3f(location, x, y, z);
	}

	public static final void glUniform3fv (int location, int count, FloatBuffer v) {
		Gdx.gl20.glUniform3fv(location, count, v);
	}

	public static final void glUniform3i (int location, int x, int y, int z) {
		Gdx.gl20.glUniform3i(location, x, y, z);
	}

	public static final void glUniform3iv (int location, int count, IntBuffer v) {
		Gdx.gl20.glUniform3iv(location, count, v);
	}

	public static final void glUniform4f (int location, float x, float y, float z, float w) {
		Gdx.gl20.glUniform4f(location, x, y, z, w);
	}

	public static final void glUniform4fv (int location, int count, FloatBuffer v) {
		Gdx.gl20.glUniform4fv(location, count, v);
	}

	public static final void glUniform4i (int location, int x, int y, int z, int w) {
		Gdx.gl20.glUniform4i(location, x, y, z, w);
	}

	public static final void glUniform4iv (int location, int count, IntBuffer v) {
		Gdx.gl20.glUniform4iv(location, count, v);
	}

	public static final void glUniformMatrix2fv (int location, int count, boolean transpose, FloatBuffer value) {
		Gdx.gl20.glUniformMatrix2fv(location, count, transpose, value);
	}

	public static final void glUniformMatrix3fv (int location, int count, boolean transpose, FloatBuffer value) {
		Gdx.gl20.glUniformMatrix3fv(location, count, transpose, value);
	}

	public static final void glUniformMatrix4fv (int location, int count, boolean transpose, FloatBuffer value) {
		Gdx.gl20.glUniformMatrix4fv(location, count, transpose, value);
	}

	public static final void glUseProgram (int program) {
		Gdx.gl20.glUseProgram(program);
	}

	public static final void glValidateProgram (int program) {
		Gdx.gl20.glValidateProgram(program);
	}

	public static final void glVertexAttrib1f (int indx, float x) {
		Gdx.gl20.glVertexAttrib1f(indx, x);
	}

	public static final void glVertexAttrib1fv (int indx, FloatBuffer values) {
		Gdx.gl20.glVertexAttrib1fv(indx, values);
	}

	public static final void glVertexAttrib2f (int indx, float x, float y) {
		Gdx.gl20.glVertexAttrib2f(indx, x, y);
	}

	public static final void glVertexAttrib2fv (int indx, FloatBuffer values) {
		Gdx.gl20.glVertexAttrib2fv(indx, values);
	}

	public static final void glVertexAttrib3f (int indx, float x, float y, float z) {
		Gdx.gl20.glVertexAttrib3f(indx, x, y, z);
	}

	public static final void glVertexAttrib3fv (int indx, FloatBuffer values) {
		Gdx.gl20.glVertexAttrib3fv(indx, values);
	}

	public static final void glVertexAttrib4f (int indx, float x, float y, float z, float w) {
		Gdx.gl20.glVertexAttrib4f(indx, x, y, z, w);
	}

	public static final void glVertexAttrib4fv (int indx, FloatBuffer values) {
		Gdx.gl20.glVertexAttrib4fv(indx, values);
	}

	public static final void glVertexAttribPointer (int indx, int size, int type, boolean normalized, int stride, Buffer ptr) {
		Gdx.gl20.glVertexAttribPointer(indx, size, type, normalized, stride, ptr);
	}

	public static final void glVertexAttribPointer (int indx, int size, int type, boolean normalized, int stride, int ptr) {
		Gdx.gl20.glVertexAttribPointer(indx, size, type, normalized, stride, ptr);
	}
	
	// LWJGL
	// 
	
	public static final int GL_LINE = GL11.GL_LINE;
	public static final int GL_FILL = GL11.GL_FILL;
	public static final int GL_POINT = GL11.GL_POINT;
	
	public static final void glPolygonMode(int face, int mode) {
		GL11.glPolygonMode(face, mode);
	}
	
	public static final void glPointSize(float size) {
		GL11.glPointSize(size);
	}
}
