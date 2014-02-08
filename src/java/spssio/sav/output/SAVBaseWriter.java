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
import spssio.util.DataEndianness;

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
    
    /**
     * Controls the size of an internal byte buffer used 
     * for serializing numbers.
     */
    public static final int BYTES_BUFFER_SIZE = 16;
    
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
    private DataEndianness integerEndianness;
    
    /**
     * Endianness of floating point values
     */
    private DataEndianness floatingEndianness;
    
    /**
     *  Character encoding.
     */
    private String stringEncoding;
    
    /**
     * Internal buffer used ONLY for serializing numbers.
     * Strings do not need a separate buffer, because
     * {@code String.getBytes()} creates a new byte array 
     * every time
     */
    private byte[] bytes;
    
    // CONSTRUCTORS
    //==============
    
    public SAVBaseWriter() {
        ostream = null;
        integerEndianness = new DataEndianness();
        floatingEndianness = new DataEndianness();
        stringEncoding = DEFAULT_ENCODING;
        ostreamBufferSize = DEFAULT_BUFFER_SIZE;
        bytes = new byte[BYTES_BUFFER_SIZE];

        // Set default endiannesses
        setEndianness(DataEndianness.LITTLE_ENDIAN);
        
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
    
    public void setEndianness(int endianness) {
        setIntegerEndianness(endianness);
        setFloatingEndianness(endianness);
    }
    
    public void setIntegerEndianness(int endianness) {
        integerEndianness.set(endianness);
    }
    
    public void setFloatingEndianness(int endianness) {
        floatingEndianness.set(endianness);
    }
    
    public int getIntegerEndianness() {
        return integerEndianness.get();
    }

    public int getFloatingEndianness() {
        return floatingEndianness.get();
    }

    // OTHER METHODS
    //===============
    
    /**
     * Used to grant access to the underlying OutputSteram
     */
    protected OutputStream getOutputStream() {
        return ostream;
    }

    public byte[] encodeString(String string) {
        try {
            return string.getBytes(stringEncoding);
        } catch(UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * TODO: This is common to both BaseReader and BaseWriter, 
     * so it should be put into a separate file.
     */
    public static int calculateAlignedLength(
        int length, 
        int alignment, 
        int offset
    ) {
        // Turns the alignment into a consequtive bit seqeuence.
        // For instance if aligment is 2^4, 
        // that is, 000 1000, it will be 0000 0111 after this.
        alignment--;
        
        // Take a bitwise complement of the alignment. 
        // For instance, if the alignment is now 0000 0111, 
        // after this it will be 1111 1000. The bitwise complement is then 
        // used as a bitmask in an AND-operation which essentially performs 
        //an integer division using {@code alignment} as the divider.
        int mask = ~alignment;
        
        // Calculates (ceil((length+offset) / alignment) * alignment) - offset
        int alignedLength = ((length+alignment+offset) & mask) - offset;
        
        return alignedLength;
    }
    
    
    // WRITING METHODS
    //=================
    
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
    
    public void writeBytesRepeat(byte value, int length) {
        if (length > 0) {
            try {
                for (int i = 0; i < length; i++) {
                    ostream.write(value);
                }
            } catch(IOException ex) {
                throw new RuntimeException(ex);
            } // try-catch
        } // if
    }
    
    public void writeInt(int value) {
        // Data
        // Serialize integer into bytes according to endianness
        // write bytes into stream
        integerEndianness.integerToBytes(bytes, 0, value);
        writeBytes(bytes, 0, 4);
    }
    
    public void writeDouble(double value) {
        floatingEndianness.doubleToBytes(bytes, 0, value);
        writeBytes(bytes, 0, 8);
    }
    
    public void writeAlignedString(
        int widthOfLength,
        String string, 
        int alignment, 
        int offset
    ) {
        byte[] encoded = encodeString(string);

        int alignedLength 
            = calculateAlignedLength(encoded.length, alignment, offset);

        // Calculate the required padding
        int paddingLength = alignedLength - encoded.length;

        // Emit encoded length as a BYTE
        if (widthOfLength == 1) {
            write1(encoded.length);
        } else if (widthOfLength == 4) {
            writeInt(encoded.length);
        } else {
            throw new IllegalArgumentException(String.format(
                "widthOfLength must be either 1 or 4"));
        }
        
        // Output the bytes of the actual string
        writeBytes(encoded, 0, encoded.length);
        
        // Output the padding, if any
        writeAlignedStringPadding(string, paddingLength);
    }
    
    public void writeAlignedStringPadding(String string, int paddingLength) {
        // Default behaviour is to output whitespaces for padding 
        writeBytesRepeat((byte) 0x20, paddingLength);
    }
    
    /**
     * @param paddedLength Number of bytes the encoded string should occupy.
     */
    public void writePaddedString(String string, int paddedLength) {
        byte[] encoded = encodeString(string);
        
        int paddingLength = paddedLength - encoded.length;
        
        // Output the bytes of the actual string
        writeBytes(encoded, 0, encoded.length);
        
        // Output the padding, if any
        writeBytesRepeat((byte) 0x20, paddingLength);
    }
} // class



