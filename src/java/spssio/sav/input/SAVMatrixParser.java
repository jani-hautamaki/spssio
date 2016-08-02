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
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;

// spssio
import spssio.sav.SAVMatrixHandler;
import spssio.util.DataEndianness;

public class SAVMatrixParser {

    // MEMBER VARIABLES
    //==================

    /**
     * The index of the variable that is being read next
     */
    private int index;

    /**
     * The current column having non-negative width
     * that is being built.
     */
    private int currentColumn;

    /**
     * Buffer used for accumulating string data
     */
    private byte[] stringBuffer;

    /**
     * Number of valid bytes in the stringBuffer
     */
    private int stringBytes;

    /**
     * Configured column widths
     */
    private int[] columnWidths;

    /**
     * Encoding of the raw string bytes
     */
    private Charset encoding;

    /**
     * Configured endianness of the input data
     */
    private int endianness;

    /**
     * Configured SYSMISS value of the input data
     */
    private double sysmiss;

    /**
     * Configured SYSMISS value as long bits
     */
    private long sysmissRaw;

    /**
     * Receiver of the parsing events.
     */
    private SAVMatrixHandler contentHandler;

    // CONSTRUCTORS
    //==============

    public SAVMatrixParser() {
        index = 0;
        currentColumn = 0;
        columnWidths = null;

        // Buffer uninitialized
        stringBuffer = null;
        stringBytes = 0;

        // User content handler
        contentHandler = null;

        // TODO: a good default
        sysmiss = 0.0;
        sysmissRaw = 0;
        endianness = DataEndianness.LITTLE_ENDIAN;
    }

    /**
     * Copy constructor
     */
    public SAVMatrixParser(SAVMatrixParser other) {
        this();

        // Copy endianness settings
        setEndianness(other.getEndianness());

        // Copy Sysmiss settings
        setSysmissRaw(other.getSysmissRaw());

        // Create a string buffer with equal length
        if (other.stringBuffer != null) {
            stringBuffer = new byte[other.stringBuffer.length];
        }

        // Copy encoding settings
        setEncoding(other.getEncoding());

        // Copy column widths
        setColumnWidths(other.getColumnWidths());
    }

    // CONFIGURATION METHODS
    //=======================

    public void setEndianness(int endianness) {
        // Validate argument
        if (DataEndianness.isValid(endianness) == false) {
            throw new IllegalArgumentException(String.format(
                "Illegal endianness: %d", endianness));
        }

        this.endianness = endianness;
    }

    public int getEndianness() {
        return this.endianness;
    }

    public void setSysmiss(double sysmiss) {
        this.sysmiss = sysmiss;
        this.sysmissRaw = Double.doubleToLongBits(this.sysmiss);
    }

    public void setSysmissRaw(long sysmissRaw) {
        this.sysmissRaw = sysmissRaw;
        this.sysmiss = Double.longBitsToDouble(this.sysmissRaw);
    }

    public double getSysmiss() {
        return sysmiss;
    }

    public long getSysmissRaw() {
        return sysmissRaw;
    }

    public void reallocStringBuffer(int size) {

        // Validate size
        if (size <= 0) {
            throw new IllegalArgumentException("size <= 0");
        }
        if ((size & 0xfffffff8) != size) {
            throw new IllegalArgumentException("size is not a multiple of eight");
        }

        // Allocate a new buffer
        byte[] newBuffer = new byte[size];

        // Copy the contents of the previous buffer, if any
        if (stringBuffer != null) {
            // minLength = min(strBuffer.length, newBuffer.length);
            int minLength = stringBuffer.length < newBuffer.length ?
                stringBuffer.length : newBuffer.length;
            // Copy array
            System.arraycopy(newBuffer, 0, stringBuffer, 0, minLength);

            // Saturate the number of valid bytes
            if (stringBytes > minLength) {
                stringBytes = minLength;
            }
        }

        stringBuffer = newBuffer;
    }

    public int getStringBufferLength() {
        if (stringBuffer != null) {
            return stringBuffer.length;
        }
        return -1;
    }

    public void freeStringBuffer() {
        stringBuffer = null;
        stringBytes = 0;
    }

    public void setEncoding(String charsetName) {
        try {
            encoding = Charset.forName(charsetName);
        } catch(UnsupportedCharsetException ex) {
            throw new IllegalArgumentException(ex);
        } // try-catch
    }

    public String getEncoding() {
        return encoding.name();
    }

    public void setColumnWidths(int[] widths) {
        columnWidths = widths;
    }

    /**
     * Returns a read-only copy
     */
    public int[] getColumnWidths() {
        return Arrays.copyOf(columnWidths, columnWidths.length);
    }

    /**
     * Sets the content handler for receiving the parsing events.
     */
    public void setContentHandler(SAVMatrixHandler contentHandler) {
        this.contentHandler = contentHandler;
    }

    // OTHER METHODS
    //===============


    // Note: The name "restart" was considered first, but it implies
    // that the parser should *start* which it does not do here.
    // Therefore, "reset" is a better word for the method.
    public void reset() {
        index = 0;
        currentColumn = 0;
        stringBytes = 0;
    }


    // Expects a 8-byte array each time.
    //
    public int consume(byte[] data) {

        if (data == null) {

            // EOF received

            if (index == 0) {
                // Accept
                return 1;
            } else {
                // EOF at an unexpected position
                throw new RuntimeException(String.format(
                    "EOF encountered when at column %d / %d",
                    index, columnWidths.length));
            }
        }

        // If index == 0, begin a new row
        if (index == 0) {
            emitRowBegin();
        }

        int curWidth = columnWidths[index];
        if (curWidth >= 0) {
            // Reset the column
            currentColumn = index;
        }

        // Next column
        int nextIndex = index+1;

        if (nextIndex == columnWidths.length) {
            // This data packet was last for the current row.
            // Reset index to the beginning of the next row.
            nextIndex = 0;
        }

        int nextWidth = columnWidths[nextIndex];

        if (curWidth == 0) {
            // Numeric variable.
            consumeNumberData(data);
            // emitNumber()
        } else if (curWidth > 0) {
            // Copy data into the string variable buffer

            appendStringBuffer(data);

            // Determine whether this was the last column
            // of a string variable.
            // If that is the case, then consume the string buffer
            // and emit the variable.

            // String variable, non-virtual
            if (nextWidth >= 0) {
                // Short string variable
                // These cannot be sent as a pass-through,
                // because they need unpadding.
                // consumeStringData()

                consumeStringBuffer();

            } else { // nextVar.width < 0
                // Long string variable BEGINS.
                // Start accumulation
            }
        } else { // curVar.width < 0
            // Copy data into the string variable buffer

            appendStringBuffer(data);

            // String variable, virtual
            if (nextWidth >= 0) {
                // Long string variable ENDS
                // Finish accumulation and emit data

                consumeStringBuffer();

            } else { // nextVar.width >= 0
                // Long string variable MIDDLE
                // Continue accumulation
            }
        } // if-else: curVar.width

        // If at the last column, emit row end
        if (nextIndex == 0) {
            emitRowEnd();
        }

        index = nextIndex;

        return 0;
    }

    private long deserializeRawDouble(byte[] data) {
        long raw = 0;
        long cbyte = 0;

        if (endianness == DataEndianness.LITTLE_ENDIAN) {
            for (int i = 7; i >= 0; i--) {
                cbyte = ((long)data[i]) & 0xff;
                raw = raw << 8;
                raw = raw | cbyte;
            }
        } else if (endianness == DataEndianness.BIG_ENDIAN) {
            for (int i = 0; i < 8; i++) {
                cbyte = ((long)data[i]) & 0xff;
                raw = raw << 8;
                raw = raw | cbyte;
            }
        } else {
            throw new RuntimeException("Unconfigured endianness");
        } // if-else

        return raw;
    }

    private void consumeNumberData(byte[] data) {
        // Convert the byte array into a "long" suitable
        // according to the endianness setting
        long raw = deserializeRawDouble(data);

        // Compare to raw sysmiss value
        if (raw == sysmissRaw) {
            emitSysmiss();
        } else {
            // Convert to a double, and emit the number
            double value = Double.longBitsToDouble(raw);
            emitNumber(value);
        }
    }

    private void consumeStringBuffer() {
        // Get the buffer contents as a String
        String value = deserializeStringBuffer();
        // Consume the buffer
        stringBytes = 0;
        // Emit the value
        emitString(value);
    }



    private void appendStringBuffer(byte[] data) {
        if (stringBuffer == null) {
            throw new RuntimeException("String buffer unallocated");
        }

        // Number of bytes left in the string buffer
        int bytesLeft = stringBuffer.length - stringBytes;

        // Number of bytes to write.
        // This is limited by the available buffer size
        int dataLength = data.length;

        if (bytesLeft < dataLength) {
            // Not enough free space in the buffer.
            // Limit the number of bytes written
            dataLength = bytesLeft;
        }

        if (dataLength > 0) {
            // Copy
            System.arraycopy(
                data, 0,
                stringBuffer, stringBytes, data.length);
            // Move write head
            stringBytes += data.length;
        } else {
            // No space left at all
        }
    } // appendStringBuffer()

    private String deserializeRawString(byte[] data, int len) {

        // Unpad
        while ((len > 0) && (data[len-1] == ' ')) {
            len--;
        }

        if (encoding == null) {
            throw new RuntimeException("Encoding is unconfigured");
        }

        // Convert the raw bytes into a string
        String rval = new String(data, 0, len, encoding);

        return rval;
    }

    private String deserializeStringBuffer() {
        String value = deserializeRawString(stringBuffer, stringBytes);
        return value;
    }

    // EMIT METHODS
    //==============

    private void emitNumber(double value) {
        if (contentHandler != null) {
            contentHandler.onCellNumber(currentColumn, value);
        }
    }

    private void emitSysmiss() {
        if (contentHandler != null) {
            contentHandler.onCellSysmiss(currentColumn);
        }
    }

    private void emitString(String value) {
        if (contentHandler != null) {
            contentHandler.onCellString(currentColumn, value);
        }
    }

    private void emitRowBegin() {
        if (contentHandler != null) {
            contentHandler.onRowBegin(-1);
        }
    }

    private void emitRowEnd() {
        if (contentHandler != null) {
            contentHandler.onRowEnd(-1);
        }
    }



    private void emitMatrixBegin() {
        if (contentHandler != null) {
        }
    }

    private void emitMatrixEnd() {
        if (contentHandler != null) {
        }
    }

} // class

