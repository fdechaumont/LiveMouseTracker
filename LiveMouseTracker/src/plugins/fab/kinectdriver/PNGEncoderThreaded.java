package plugins.fab.kinectdriver;


import icy.file.FileUtil;
import icy.image.IcyBufferedImage;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class PNGEncoderThreaded extends Thread //implements Runnable
{
	
	IcyBufferedImage image ;
	File file ;

  /** Image must be ushort grayscale 1 channel */
  public PNGEncoderThreaded(IcyBufferedImage image, File f) {

	  this.file = f;
	  this.image = image;

	}

  @Override
	public void run() {

//	  Chronometer c = new Chronometer("save png file");		

	  FileUtil.ensureParentDirExist( file );
	  
	  short[] data = (short[]) image.getDataXY( 0 );

	  try {
		  
		  ByteArrayOutputStream png = PNGEncoderThreaded.toPNG( image.getWidth(), image.getHeight(), data );
		  FileOutputStream fos = new FileOutputStream ( file ); 
		  png.writeTo(fos);
		  fos.close();

	  } catch (IOException e) {
		  e.printStackTrace();
	  }

//	  c.displayMs();
	  	  
	}
  
public static ByteArrayOutputStream toPNG(int width, int height, 
		  short[] imageData) throws IOException
  {
    byte[] signature = new byte[] {(byte) 137, (byte) 80, (byte) 78, (byte) 71, (byte) 13, (byte) 10, (byte) 26, (byte) 10};
    byte[] header = createHeaderChunk(width, height);
    byte[] data = createDataChunk(width, height, imageData );
    byte[] trailer = createTrailerChunk();
    
    ByteArrayOutputStream png = new ByteArrayOutputStream(
    		signature.length + header.length + data.length + trailer.length);
    png.write(signature);
    png.write(header);
    png.write(data);
    png.write(trailer);
    return png;
  }

  public static byte[] createHeaderChunk(int width, int height) throws IOException
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream(13);
    DataOutputStream chunk = new DataOutputStream(baos);
    chunk.writeInt(width);
    chunk.writeInt(height);
    chunk.writeByte(16); // Bitdepth
    chunk.writeByte(0); // Each pixel is a grayscale sample
    chunk.writeByte(0); // Compression
    chunk.writeByte(0); // Filter
    chunk.writeByte(0); // Interlace    
    return toChunk("IHDR", baos.toByteArray());
  }
  
  public static byte[] createDataChunk(int width, int height, short[] data) throws IOException
  {   
		byte[] dataBufferAsByte = new byte[data.length *2 + height];

		int dest = 0;
		int source = 0;
		for ( int yy = 0 ; yy < height ; yy++ )
		{
			dataBufferAsByte[dest]= (byte)0; // say there is no filter for this scanline.
			dest++;
			for ( int xx = 0 ; xx < width ; xx++ )
			{
				dataBufferAsByte[dest] = (byte) ( (data[source] & 0xFF00) >> 8 );								
				dataBufferAsByte[dest+1] = (byte) (data[source] & 0x00FF);
				dest+=2;
				source++;
			}
		}
	  
    //int source = 0;
    //int dest = 0;
    //byte[] raw = new byte[4*(width*height) + height];
    /*
    byte[] raw = new byte[4*(width*height) + height];
    for (int y = 0; y < height; y++)
    {
      raw[dest++] = 0; // No filter
      for (int x = 0; x < width; x++)
      {
        raw[dest++] = red[source];
        raw[dest++] = green[source];
        raw[dest++] = blue[source];
        raw[dest++] = alpha[source++];
      }
    }*/
    return toChunk("IDAT", toCompression( dataBufferAsByte ));
  }

  public static byte[] createTrailerChunk() throws IOException
  {
    return toChunk("IEND", new byte[] {});
  }
  
  public static byte[] toChunk(String id, byte[] raw) throws IOException
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream(raw.length + 12);
    DataOutputStream chunk = new DataOutputStream(baos);
    
    chunk.writeInt(raw.length);
    
    byte[] bid = new byte[4];
    for (int i = 0; i < 4; i++)
    {
      bid[i] = (byte) id.charAt(i);
    }
    
    chunk.write(bid);
        
    chunk.write(raw);
    
    int crc = 0xFFFFFFFF;
    crc = updateCRC(crc, bid);  
    crc = updateCRC(crc, raw);    
    chunk.writeInt(~crc);
    
    return baos.toByteArray();
  }

  static int[] crcTable = null;
  
  public static void createCRCTable()
  {
    crcTable = new int[256];
    
    for (int i = 0; i < 256; i++)
    {
      int c = i;
      for (int k = 0; k < 8; k++)
      {
        c = ((c & 1) > 0) ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
      }
      crcTable[i] = c;
    }
  }
  
  public static int updateCRC(int crc, byte[] raw)
  {
    if (crcTable == null)
    {
      createCRCTable();
    }
    
    for (int i = 0; i < raw.length; i++)
    {
      crc = crcTable[(crc ^ raw[i]) & 0xFF] ^ (crc >>> 8);      
    }
    
    return crc;
  }

  /* This method is called to encode the image data as a zlib
     block as required by the PNG specification. This file comes
     with a minimal ZLIB encoder which uses uncompressed deflate
     blocks (fast, short, easy, but no compression). If you want
     compression, call another encoder (such as JZLib?) here. */
  public static byte[] toCompression(byte[] raw) throws IOException
  {
	  return icy.util.ZipUtil.pack( raw );
    //return ZLIB.toZLIB(raw);
	  //return raw; // 20% gain CPU if no compression
  }
}



