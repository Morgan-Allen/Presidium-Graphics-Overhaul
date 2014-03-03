package src.graphics.solids;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;



public class DataInput extends InputStream {
	
	private final boolean endian; // little endian
	private final InputStream in;
	
	
	public DataInput(InputStream in, boolean littleEndian)
	{
		this.in = in;
		this.endian = littleEndian;
	}
	
	public int read() throws IOException {
		int r = in.read();
		if(r==-1)
			throw new EOFException();
		return r;
	}
	
	
	public int readInt() throws IOException {
		if(endian) {
			return
				(read() & 0xFF) | (read() & 0xFF) << 8 |
				(read() & 0xFF) << 16 | (read() & 0xFF) << 24;
		} else {
			return
				(read() & 0xFF) <<24 | (read() & 0xFF) << 16 |
				(read() & 0xFF) << 8 | (read() & 0xFF);
		}
	}
	
	public short readShort() throws IOException {
		if(endian) {
			return (short) ((read() & 0xFF) | (read() & 0xFF) << 8);
		} else {
			return (short) ((read() & 0xFF) <<8 | (read() & 0xFF)) ;
		}
	}
	
	public char readUShort() throws IOException {
		if(endian) {
			return (char) ((read() & 0xFF) | (read() & 0xFF) << 8);
		} else {
			return (char) ((read() & 0xFF) <<8 | (read() & 0xFF)) ;
		}
	}
	
	public float readFloat() throws IOException {
		return Float.intBitsToFloat(readInt());
	}
	
	public byte readByte() throws IOException {
		return (byte) (read() & 0xFF);
	}
	
	public String readUTF(int len) throws IOException {
		byte[] bytes = new byte[len];
		in.read(bytes);
		
		// null terminated string length
		len = strlen(bytes);
		return new String(bytes, 0, len, "UTF8");
	}
	
	// --------
	// read floats
		
	public float[] readFloats(float[] arr, int off, int len) throws IOException {
		for(int i=0; i<len; i++) {
			arr[off+i] = readFloat();
		}
		return arr;
	}
	public float[] readFloatsR(float[] arr, int off, int len) throws IOException {
		for(int i=len-1; i>=0; i--) {
			arr[off+i] = readFloat();
		}
		return arr;
	}

	public float[] readFloats(float[] arr) throws IOException {
		return readFloats(arr, 0, arr.length);
	}

	public float[] readFloatsR(float[] arr) throws IOException {
		return readFloatsR(arr, 0, arr.length);
	}
	
	public Vector3 read3D(Vector3 v) throws IOException {
		return v.set(readFloat(), readFloat(), readFloat());
	}
	
	// -------
	// read shorts

	public short[] readShorts(short[] arr) throws IOException {
		for(int i=0;i<arr.length; i++) {
			arr[i] = readShort();
		}
		return arr;
	}
	
	// -----
	// other stuff
	
	@Override
	public void close() throws IOException {
		in.close();
	}
	
	@Override
	public long skip(long n) throws IOException {
		return in.skip(n);
	}
	
	@Override
	public int available() throws IOException {
		return in.available();
	}
	
	
	
	
	
	
	
	
	private int strlen(byte[] array) {
		for(int i=0; i< array.length; i++) {
			if(array[i]==0)
				return i;
		}
		return array.length;
	}

}




