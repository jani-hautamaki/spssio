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

package spssio.sav.input;

// core java
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

// spssio
import spssio.sav.SAVData;

public class SAVBaseReader {

        
    // CONSTANTS
    //===========

    /**
     * Buffer size used for creating BufferedInputStream, unless specified.
     */
    public static final int DEFAULT_BUFFER_SIZE = 0x4000; // 16 KBs

    /**
     * Default encoding used
     */
    public static final String DEFAULT_ENCODING = "ISO-8859-1";
    
    // TODO: Remove these, since these constants are defined elsewhere
    public static final int BIG_ENDIAN = 1;
    public static final int LITTLE_ENDIAN = -1;
    
    // MEMBER VARIABLES
    //==================
    
    /**
     * Configuration variable which controls the buffer size parameter 
     * of {@code BufferedInputStream}'s constructor. 
     */
    private int bufferSize;
    
    /**
     * Configuration variable which determines the encoding used 
     * for strings.
     */
    private String stringEncoding;

    private int integerEndianness;
    private int floatingEndianness;
    


    /** 
     * The input stream
     */
    private BufferedInputStream istream;
    
    /**
     * The offset of the next byte.
     */
    private int fpos;
    
    /**
     * File size, or -1 if not known.
     */
    private int fsize;
    
    
    // CONSTRUCTORS
    //==============
    
    public SAVBaseReader() {
        bufferSize = DEFAULT_BUFFER_SIZE;
        stringEncoding = DEFAULT_ENCODING;
        istream = null;
        integerEndianness = LITTLE_ENDIAN;
        floatingEndianness = LITTLE_ENDIAN;
        fpos = 0;
        fsize = -1;
    } // ctor
    
    // OTHER METHODS
    //===============
    

    public void bind(InputStream is) {
        istream = new BufferedInputStream(is, bufferSize);
        
        // Assume initial position and unknown file size.
        fpos = 0;
        fsize = -1;
    }
    
    /**
     * Binds the reader to the specified {@code InputStream}.
     *
     * @param is 
     *     The input stream to bind to
     * @param offset 
     *     The offset of the next byte
     * @param size 
     *     The length of the stream if known, or -1 if not known.
     *     Note, the biggest supported file length is 0x7FFFFFFF.
     */
    public void bind(InputStream is, int offset, int size) {
        
        // Validate parameters
        if (offset < 0) {
            throw new IllegalArgumentException("offset < 0");
        }
        if (size < -1) {
            throw new IllegalArgumentException("size < -1");
        }
        if (offset > size) {
            throw new IllegalArgumentException("offset > size");
        }
        
        this.istream = new BufferedInputStream(is, bufferSize);
        this.fpos = offset;
        this.fsize = size;
    }
    
    public void unbind() {
        istream = null;
        // TODO
    }

    /**
     * Returns the offset of the next byte
     */
    public int tell() {
        return fpos;
    }
    
    /**
     * Returns the length of the {@code InputStream} if known,
     * or -1 if not known.
     */
    public int length() {
        return fsize;
    }

    
    public int getIntegerEndianness() {
        return integerEndianness;
    }
    
    public int getFloatingEndianness() {
        return floatingEndianness;
        
    }

    /**
     * Read a single byte, and keeps count of the location.
     * If eof is reached, returns -1.
     */
    protected int read() 
        throws IOException
    {
        int rval = istream.read(); // may throw
        // Increase the offset only if a byte was actually read.
        if (rval != -1) {
            fpos++;
        }
        
        return rval;
    } // read()
    
    /**
     * Reads an array of bytes, and keeps count of the location.
     * Returns the number of bytes read, or -1 if no bytes available.
     */
    protected int read(
        byte[] array,
        int offset,
        int length
    )
        throws IOException
    {
        // May throw
        int rval = istream.read(array, offset, length);
        
        // If not eof, update head's offset
        if (rval != -1) {
            fpos += rval;
        }
        
        return rval;
    } //  read()
    
    public int read1() {
        int rval = -1;
        
        try {
            rval = istream.read();
        } catch(IOException ex) {
            error_io("read1()", ex);
        } // try-catch
        
        if (rval == -1) {
            error_eof("read1()");
        } else {
            fpos++;
        }
        
        return rval;
    }
    
    public int readInt() {
        int rval = 0;
        int cbyte = -1;
        
        try {
            switch(integerEndianness) {
                case BIG_ENDIAN:
                    for (int i = 3; i >= 0; i--) {
                        cbyte = read();
                        rval |= (cbyte & 0xff) << (i << 3);
                    }
                    break;
                case LITTLE_ENDIAN:
                    for (int i = 0; i < 4; i++) {
                        cbyte = read();
                        rval |= (cbyte & 0xff) << (i << 3);
                    }
                    break;
                default:
                    error_endianness(integerEndianness);
                    break;
            }

            if (cbyte == -1) {
                error_eof("readInt()");
            }
            
        } catch(IOException ex) {
            error_io("readInt()", ex);
        } // try-catch
        
        return rval;
    }
    
    public double readDouble() {
        long rawBits = 0;
        int cbyte = -1;
        
        try {
            switch(floatingEndianness) {
                case BIG_ENDIAN:
                    for (int i = 7; i >= 0; i--) {
                        cbyte = read();
                        rawBits |= (long)(cbyte & 0xff) << (i << 3);
                    }
                    break;
                case LITTLE_ENDIAN:
                    for (int i = 0; i < 8; i++) {
                        cbyte = read();
                        rawBits |= (long)(cbyte & 0xff) << (i << 3);
                    }
                    break;
                default:
                    error_endianness(floatingEndianness);
                    break;
            }

            if (cbyte == -1) {
                error_eof("readDouble()");
            }
            
        } catch(IOException ex) {
            error_io("readDouble()", ex);
        } // try-catch

        return Double.longBitsToDouble(rawBits);
    }
    
    /**
     * Reads a number of bytes into an array
     */
    public void readBytes(
        byte[] array,
        int offset,
        int length
    ) {
        
        int bytesRead = -1;
        try {
            /*
            for (int i = 0; i < length; i++) {
                cbyte = read();
                array[i] = (byte) cbyte;
            }
            */
            bytesRead = istream.read(array, offset, length);
        } catch(IOException ex) {
            error_io("readBytes()", ex);
        }
        
        if (bytesRead != -1) {
            fpos += bytesRead;
        }
        
        if (bytesRead != length) {
            error_eof("readBytes()");
        }
    }
    
    /**
     * TODO: Rename into readFixedString() ?
     */
    public String readString(int length) {
        String rval = null;
        
        byte[] encoded = new byte[length];
        readBytes(encoded, 0, length);
        
        try {
            rval = new String(encoded, stringEncoding);
        } catch(UnsupportedEncodingException ex) {
            error_encoding(stringEncoding);
        }
        
        return rval;
    }
    
    /**
     * @param length The padded length in bytes.
     */
    public String readPaddedString(int length) {
        String rval = null;
        
        byte[] encoded = new byte[length];
        readBytes(encoded, 0, length);
        
        // Unpad
        // TODO: Use a whitespace constant instead of char literal
        while ((length > 0) && (encoded[length-1] == ' ')) {
            length--;
        }
        
        try {
            rval = new String(encoded, 0, length, stringEncoding);
        } catch(UnsupportedEncodingException ex) {
            error_encoding(stringEncoding);
        }
        
        return rval;
    }
    
    public String readAlignedString(int encodedLength, int alignment, int offset) {
        String rval = null;
        
        // Calculate aligned length
        alignment--;
        int mask = ~alignment;

        int alignedLength = ((encodedLength+alignment+offset) & mask) - offset;

        byte[] encoded = new byte[alignedLength];
        readBytes(encoded, 0, alignedLength);

        // Unpadding should not be needed, since the unpadded length 
        // is known a priori (ie. encodedLength)
        
        try {
            rval = new String(encoded, 0, encodedLength, stringEncoding);
        } catch(UnsupportedEncodingException ex) {
            error_encoding(stringEncoding);
        }
        
        return rval;
    }
    

    // (truncatedLength, readLength) TODO?
    public String readString(int readLength, int stringLength) {
        String rval = null;
        
        byte[] encoded = new byte[readLength];
        readBytes(encoded, 0, readLength);
        
        try {
            rval = new String(encoded, 0, stringLength, stringEncoding);
        } catch(UnsupportedEncodingException ex) {
            error_encoding(stringEncoding);
        }
        
        return rval;
    }
    
    public String bytesToStringUnpad(byte[] encoded) {
        int length = encoded.length;
        String rval = null;
        // Unpad
        // TODO: Use a whitespace constant instead of char literal
        while ((length > 0) && (encoded[length-1] == ' ')) {
            length--;
        }
        try {
            rval = new String(encoded, 0, length, stringEncoding);
        } catch(UnsupportedEncodingException ex) {
            error_encoding(stringEncoding);
        }
        
        return rval;
    }
    
    
    public double bytesToDouble(byte[] bytes) {
        return SAVData.bytesToDouble(bytes, 0, floatingEndianness);
    }
    
    /*
    public double bytesToDouble(byte[] bytes) {
        long rawBits = 0;
        int cbyte = -1;
        
        // TODO:
        // Validate array length
        
        switch(floatingEndianness) {
            case BIG_ENDIAN:
                for (int i = 0; i < 8; i++) {
                    cbyte = bytes[i];
                    rawBits |= (long)(cbyte & 0xff) << (i << 3);
                }
                break;
            case LITTLE_ENDIAN:
                for (int i = 7; i >= 0; i--) {
                    cbyte = bytes[i];
                    rawBits |= (long)(cbyte & 0xff) << (i << 3);
                }
                break;
            default:
                error_endianness(floatingEndianness);
                break;
        }
        
        return Double.longBitsToDouble(rawBits);
    }
    */
    
    // EXCEPTIONS
    //============
    
    protected static void error_io(String method, IOException ex) {
        String msg = ex.getMessage();
        
        if (msg == null) {
            msg = "message null";
            // TODO: try getCause()
        } // if: no message
            
        throw new RuntimeException(String.format(
            "%s failed: %s", method, msg), ex);
    } // error_io()
    
    protected static void error_eof(String method) {
        throw new RuntimeException(String.format(
            "%s failed: unexpected end-of-file", method));
    } // error_eof()
    
    protected static void error_endianness(int endianness) {
        throw new RuntimeException(String.format(
            "Unhandled endianess: %d (programming error)", endianness));
    }
    
    protected static void error_encoding(String charsetName) {
        throw new RuntimeException(String.format(
            "Unsupported encoding specified for strings: %s", charsetName));
    }
    
    
} // class SAVBaseReader