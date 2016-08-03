//*******************************{begin:header}******************************//
//             spssio - https://github.com/jani-hautamaki/spssio             //
//***************************************************************************//
//
//      Java classes for reading and writing
//      SPSS/PSPP Portable and System files
//
//      Copyright (C) 2013-2016 Jani Hautamaki <jani.hautamaki@hotmail.com>
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
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

// spssio
import spssio.sav.SAVConstants;
import spssio.util.DataEndianness;
import spssio.util.Utf8Helper;

public class SAVBaseReader {


    // CONSTANTS
    //===========

    /**
     * Buffer size used for creating BufferedInputStream, unless specified.
     */
    public static final int DEFAULT_BUFFER_SIZE = 0x4000; // 16 KBs

    /**
     * Canonical name for UTF8 encoding in java.nio.charset.
     */
    public static final String UTF8_CANONICAL_NAME = "UTF-8";

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
    private Charset encoding;

    /**
     * Indicates whether the curent encoding is utf-8.
     */
    private boolean encodingIsUtf8;

    /**
     * Endianness for integers
     */
    private DataEndianness integerEndianness;

    /**
     * Endianness for floating-point numbers
     */
    private DataEndianness floatingEndianness;

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

    /**
     * Internal buffer used for reading numbers.
     * The size is fixed to 8 bytes, which is
     * the number of bytes needed for a double.
     */
    private byte[] numberBuffer;

    /**
     * Whether to allow smart utf-8 encoding promotion.
     */
    private boolean enableSmartUtf8;

    /**
     * Set to true, when the smart utf-8 detection has been
     * enabled, and the scanner finds a byte sequence, that
     * cannot occur in valid utf-8.
     */
    private boolean containsInvalidUtf8;

    // CONSTRUCTORS
    //==============

    public SAVBaseReader() {
        bufferSize = DEFAULT_BUFFER_SIZE;
        encoding = null;
        istream = null;
        integerEndianness = new DataEndianness();
        floatingEndianness = new DataEndianness();
        fpos = 0;
        fsize = -1;
        enableSmartUtf8 = true;
        encodingIsUtf8 = false;
        containsInvalidUtf8 = false;

        numberBuffer = new byte[8];

        // Set default endianness
        setEndianness(SAVConstants.DEFAULT_ENDIANNESS);

        // Set default encoding
        setEncoding(SAVConstants.DEFAULT_STRING_ENCODING);

    } // ctor

    // CONFIGURATION METHODS
    //=======================

    public void bind(InputStream is) {
        istream = new BufferedInputStream(is, bufferSize);

        // Assume initial position and unknown file size.
        fpos = 0;
        fsize = -1;
        // Reset state variables
        containsInvalidUtf8 = false;
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
        // Reset other state variables
        containsInvalidUtf8 = false;
    }

    public void unbind() {
        istream = null;
        // TODO
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

    public void setEncoding(String charsetName) {
        try {
            encoding = Charset.forName(charsetName);
        } catch(UnsupportedCharsetException ex) {
            throw new IllegalArgumentException(ex);
        } // try-catch

        // "UTF-8" is the canonical name for UTF-8 encoding
        // in the package "java.nio.charset".
        encodingIsUtf8 = encoding.name().equals(UTF8_CANONICAL_NAME);
    }

    /**
     * Returns the canonical name of the encoding charset.
     */
    public String getEncoding() {
        if (encoding == null) {
            return null;
        }
        return encoding.name();
    }

    public void setSmartUtf8(boolean enableSmartUtf8) {
        this.enableSmartUtf8 = enableSmartUtf8;
    }

    public boolean getSmartUtf8() {
        return enableSmartUtf8;
    }

    public boolean foundInvalidUtf8() {
        return containsInvalidUtf8;
    }

    // OTHER METHODS
    //===============

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

    public String decodeString(byte[] encoded, int offset, int length) {
        if (enableSmartUtf8 && !encodingIsUtf8 && !containsInvalidUtf8) {
            int mbChars = Utf8Helper
                .countMultibyteChars(encoded, offset, length);
            // There is one or more valid multi-byte characters present.
            // Switch to UTF-8 automatically.
            if (mbChars > 0) {
                setEncoding(UTF8_CANONICAL_NAME);
            } else if (mbChars < 0) {
                // Disable further attempts.
                containsInvalidUtf8 = true;
            }
        }
        return new String(encoded, offset, length, encoding);
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

    public String bytesToStringUnpad(byte[] encoded) {
        int length = encoded.length;
        String rval = null;

        // Unpad
        // TODO: Use a whitespace constant instead of char literal
        while ((length > 0) && (encoded[length-1] == ' ')) {
            length--;
        }

        return decodeString(encoded, 0, length);
    }

    /**
     * Converts byte sequence into an integer according to
     * the integer endianness.
     * Used to lift access to integerEndianness to sub-classes.
     */
    public int bytesToInteger(byte[] buffer, int offset) {
        return integerEndianness.bytesToInteger(buffer, offset);
    }

    /**
     * Converts byte sequence into a double according to
     * the floating-point endianness.
     * Used to lift access to floatingEndianness to sub-classes.
     */
    public double bytesToDouble(byte[] buffer, int offset) {
        return floatingEndianness.bytesToDouble(buffer, offset);
    }

    // READ METHODS
    //==============

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

    /**
     * Skips over and discards bytes from the input stream.
     * This does not forward the call to {@code InputStream.skip()},
     * instead, it uses a for loop. This is to avoid creating a buffer
     * every time {@code skip()} is called.
     */
    protected void skip(int n) {
        int rval = 0;

        try {
            for (int i = 0; i < n; i++) {
                rval = read();
            }
        } catch(IOException ex) {
            error_io("skip()", ex);
        } // try-catch

        if (rval == -1) {
            error_eof("skip()");
        }
    }

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

    public int readInt() {
        readBytes(numberBuffer, 0, 4);
        return integerEndianness.bytesToInteger(numberBuffer, 0);
    }

    public double readDouble() {
        readBytes(numberBuffer, 0, 8);
        return floatingEndianness.bytesToDouble(numberBuffer, 0);
    }


    /**
     * @param length The padded length in bytes.
     */

    public String readPaddedString(int length) {
        String rval = null;

        byte[] encoded = new byte[length];
        readBytes(encoded, 0, length);

        // Unpad and return
        return bytesToStringUnpad(encoded);
    }

    public String readAlignedString(
        int widthOfLength,
        int alignment,
        int offset
    ) {
        String rval = null;

        int encodedLength = 0;

        // Read encoded length according to widthOfLength parameter

        if (widthOfLength == 1) {
            encodedLength = read1();
        } else if (widthOfLength == 4) {
            encodedLength = readInt();
        } else {
            throw new IllegalArgumentException(String.format(
                "widthOfLength must be either 1 or 4"));
        }

        int alignedLength
            = calculateAlignedLength(encodedLength, alignment, offset);

        // Calculate the required padding
        int paddingLength = alignedLength - encodedLength;

        // Read the whole aligned length into
        // a newly created byte buffer
        byte[] encoded = new byte[alignedLength];
        //readBytes(encoded, 0, alignedLength);
        readBytes(encoded, 0, encodedLength);

        // Decode the string according to the current charset.
        // Unpadding is not needed, since the unpadded
        // encoded length is already known.
        rval = decodeString(encoded, 0, encodedLength);

        // Read padding
        readAlignedStringPadding(rval, paddingLength);

        return rval;
    }

    public void readAlignedStringPadding(String string, int paddingLength) {
        skip(paddingLength);
    }

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

} // class SAVBaseReader
