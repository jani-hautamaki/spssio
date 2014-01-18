//*******************************{begin:header}******************************//
//                 spssio - http://code.google.com/p/spssio/                 //
//***************************************************************************//
//
//      Java classes for reading and writing 
//      SPSS/PSPP Portable and System files
//
//      Copyright (C) 2013-2014 Jani Hautamaki <jani.hautamaki@hotmail.com>
//
//      Licensed under the terms of GNU General Public License v3.
//
//      You should have received a copy of the GNU General Public License v3
//      along with this program as the file LICENSE.txt; if not, please see
//      http://www.gnu.org/licenses/gpl-3.0.html
//
//********************************{end:header}*******************************//

package spssio.sav.output;

// core java
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.UnsupportedEncodingException;
import java.io.IOException;

// spssio
import spssio.sav.SAVEndianness;
import spssio.sav.SAVData;

public class SAVBaseWriter
{
    
    // CONSTANTS
    //===========
    
    /**
     * Default encoding used.<p>
     *
     * TODO: This value and the one in SAVBaseReader should be moved
     * into a common constants file.<p>
     *
     */
    public static final String DEFAULT_ENCODING = "ISO-8859-1";
    
    /**
     * Default size used for the BufferedOutputStream encapsulation
     */
    public static final int DEFAULT_BUFFER_SIZE = 0x10000; // 64 kbs
    
    // MEMBER VARIABLES
    //==================

    /**
     * Config variable; is used as the buffer size parameter for
     * {@code BufferedOutputStream}'s constructor. 
     * Initialized to the default value, {@link DEFAULT_BUFFER_SIZE}
     */
    private int ostreamBufferSize;

    /**
     * Output byte stream to which the serialization is sent
     */
    private BufferedOutputStream ostream;
    
    /**
     * Endianness of integer values
     */
    private int integerEndianness;
    
    /**
     * Endianness of floating point values
     */
    private int floatingEndianness;
    
    /**
     *  Character encoding.
     */
    private String stringEncoding;
    
    /**
     * Internal buffer used for serializing numbers.
     * Strings do not need a separate buffer, because
     * {@code String.getBytes()} creates a new byte array every time
     */
    private byte[] bytes;
    
    /**
     * Reference to last encoded string written,
     * This is used to generate similar paddings to SPSS
     */
    private byte[] lastEncoded;
    
    // CONSTRUCTORS
    //==============
    
    public SAVBaseWriter() {
        ostream = null;
        integerEndianness = SAVEndianness.LITTLE_ENDIAN;
        floatingEndianness = SAVEndianness.LITTLE_ENDIAN;
        stringEncoding = DEFAULT_ENCODING;
        ostreamBufferSize = DEFAULT_BUFFER_SIZE;

        lastEncoded = new byte[256];
        /*
        for (int i = 0; i < lastEncoded.length; i++) {
            lastEncoded[i] = 0x20; // init with padding
        }
        */
        
        bytes = new byte[16];
    }
    
    // CONFIGURATION METHODS
    //=======================
    
    public void bind(OutputStream ostream) {
        // throws if ostreamBufferSize <= 0
        this.ostream = new BufferedOutputStream(ostream, ostreamBufferSize);
    }
    
    public void flush() 
        throws IOException
    {
        ostream.flush();
    }
    
    public void close() 
        throws IOException
    {
        ostream.close();
    }
    
    public void unbind() {
        if (this.ostream != null) {
            try {
                this.ostream.flush();
            } catch(IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        this.ostream = null;
    }
    
    
    // OTHER METHODS
    //===============
    
    public void write1(int byteValue) {
        try {
            ostream.write(byteValue);
        } catch(IOException ex) {
            throw new RuntimeException(ex);
        } // try-catch
    }
    
    
    public void writeBytes(byte[] data, int offset, int len) {
        try {
            ostream.write(data, offset, len);
        } catch(IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public void writePaddingBytes(byte paddingValue, int paddingLength) {
        if (paddingLength > 0) {
            try {
                for (int i = 0; i < paddingLength; i++) {
                    ostream.write(paddingValue);
                }
            } catch(IOException ex) {
                throw new RuntimeException(ex);
            } // try-catch
        } // if
    }
    
    public void writePadding(int encodedLength, int paddedLength) {
        try {
            for (int i = encodedLength; i < paddedLength; i++) {
                ostream.write((int) lastEncoded[i]);
            }
        } catch(IOException ex) {
            throw new RuntimeException(ex);
        } // try-catch
    }
    
    
    public void writeInt(int value) {
        // Data
        // Serialize integer into bytes according to endianness
        // write bytes into stream
        SAVData.integerToBytes(bytes, 0, value, integerEndianness);
        writeBytes(bytes, 0, 4);
    }
    
    public void writeDouble(double value) {
        SAVData.doubleToBytes(bytes, 0, value, floatingEndianness);
        writeBytes(bytes, 0, 8);
    }
    
    
    public void writeString(String value) {
    }
    
    public byte[] encodeString(String string) {
        try {
            return string.getBytes(stringEncoding);
        } catch(UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * NOTE! The {@code alignment} must be a power of 2. 
     * That is, the legal values are 2, 4, 8, 16, etc..
     * 
     * @param string The string to serialize
     * @param alignment The aligment, must be a power of 2
     * @param offset The offset with respect to alignment
     */
    public void writeAlignedString(byte[] encoded, int alignment, int offset) {
        // Calculate the aligned length of the array according to the params
        alignment--;
        int mask = ~alignment;
        int alignedLength = ((encoded.length+alignment+offset) & mask) - offset;
        int paddingLength = alignedLength - encoded.length;
        
        // Output the bytes of the actual string
        writeBytes(encoded, 0, encoded.length);
        
        // Output the padding, if any
        writePaddingBytes((byte) 0x20, paddingLength);
        //writePadding(encoded.length, alignedLength);
        
        // Remember this
        //System.arraycopy(encoded, 0, lastEncoded, 0, encoded.length);
    }
    
    /**
     * Provided for convenience
     */
    public void writePaddedString(String string, int len) {
        byte[] encoded = encodeString(string);
        writePaddedString(encoded, len);
    }
    
    
    /**
     * @param paddedLength Number of bytes the encoded string should occupy.
     */
    public void writePaddedString(byte[] encoded, int paddedLength) {
        
        int paddingLength = paddedLength - encoded.length;
        
        // Output the bytes of the actual string
        writeBytes(encoded, 0, encoded.length);
        
        // Output the padding, if any
        writePaddingBytes((byte) 0x20, paddingLength);
        //writePadding(encoded.length, paddedLength);
        
        
        // Remember this
        //System.arraycopy(encoded, 0, lastEncoded, 0, encoded.length);
    }
    
}

