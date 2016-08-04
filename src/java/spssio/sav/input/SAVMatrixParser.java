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

    // ERROR CODES
    //=============

    /** The parsing is unfinished, more input is expected. */
    public static final int E_UNFINISHED = -1;

    /** Parse was succesfully finished, no errors occurred. */
    public static final int E_OK = 0;

    /** Unexpeced end-of-file. */
    public static final int E_EOF = 1;

    /** Column has an invalid width (< -1). */
    public static final int E_WIDTH = 2;

    // INTERNAL STATES
    //=================

    private static final int S_ERROR = -1;
    private static final int S_ACCEPT = 0;
    private static final int S_START = 1;
    private static final int S_BEGIN_MATRIX = 2;
    private static final int S_BEGIN_ROW = 3;
    private static final int S_CELL = 4;
    private static final int S_CELL_NUMERIC = 5;
    private static final int S_CELL_STRING = 6;
    private static final int S_NEXT = 7;
    private static final int S_END_ROW = 8;
    private static final int S_NEXT_ROW_OR_EOF = 9;
    private static final int S_END_MATRIX = 10;

    // MEMBER VARIABLES FOR DFA
    //==========================

    /**
     * Incidates whether the current cycle is a null-transition,
     * that is, non-consuming.
     */
    private boolean eps;

    /**
     * Current state
     */
    private int state;

    /**
     * Errro code
     */
    private int errno;

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
        state = S_START;
        errno = E_UNFINISHED;

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
        state = S_START;
        errno = E_UNFINISHED;
    }

    public int errno() {
        return errno;
    }

    public boolean hasError() {
        return errno > E_OK;
    }

    // Expects a 8-byte array each time.
    //
    public int consume(byte[] data) {
        do {
            eps = false;
            cycle(data);
        } while (eps == true);
        return errno;
    }

    private void cycle(byte[] data) {
        int colWidth = columnWidths[index];
        switch(state) {
            case S_START:
                if (data != null) {
                    state = S_BEGIN_MATRIX;
                    eps = true;
                } else {
                    state = S_ERROR;
                    errno = E_EOF;
                } // if-else
                break;

            case S_BEGIN_MATRIX:
                emitMatrixBegin();
                state = S_BEGIN_ROW;
                eps = true;
                break;

            case S_BEGIN_ROW:
                emitRowBegin();
                state = S_CELL;
                eps = true;
                break;

            case S_CELL:
                if (data == null) {
                    // Unexpected end-of-file.
                    state = S_ERROR;
                    errno = E_EOF;
                } else if (colWidth == 0) {
                    // Numeric variable, single cell
                    state = S_CELL_NUMERIC;
                    eps = true;
                } else if (colWidth > 0) {
                    // String variable, single cell or leading cell
                    state = S_CELL_STRING;
                    eps = true;
                } else if (colWidth == -1) {
                    // String variable, continuation cell
                    state = S_CELL_STRING;
                    eps = true;
                } else {
                    //throw new RuntimeException("Impossible");
                    state = S_ERROR;
                    errno = E_WIDTH;
                } // if-else
                break;

            case S_CELL_NUMERIC:
                // This is a numeric column.
                // Decode data into a dobule and emit
                // either a number or a sysmiss.
                consumeNumberData(data);

                state = S_NEXT;
                eps = true;
                break;

            case S_CELL_STRING:
                // Single-cell, leading cell or continuation cell.

                // In any case, copy data into the string variable buffer.
                appendStringBuffer(data);

                // In case of the leading or single cell, this can be
                // (i) short string, at most 8 bytes wide,
                // (ii) long string, at most 255 bytes wide, or
                // (iii) very long string, up to at least 1 MB.

                // Examine whether this was the last cell
                // of the string variable. If so, then emit the string.
                int nextColWidth = 0; // Default to a non-continuation cell
                if (index+1 < columnWidths.length) {
                   // Not the last, so take the next cell's actual width.
                    nextColWidth = columnWidths[index+1];
                }

                if (nextColWidth >= 0) {
                   // Either at the end of the row,
                   // or the next cell is non-continuation cell.

                   // Emit the string, unless the next cell is still
                   // part of a very long string, in which case
                   // the current string buffer is unpadded only.
                   consumeStringBuffer();
                } else {
                   // Next cell is a continuation cell for a string.
                   // Keep accumulating
                }
                state = S_NEXT;
                eps = true;
                break;

            case S_NEXT:
                // Increase the index
                index++;
                if (index == columnWidths.length) {
                    // At the end of a row. Roll-over
                    index = 0;
                    // Emit also an row ending event.
                    state = S_END_ROW;
                    eps = true;
                } else {
                    // Not the last cell, so expect another.
                    state = S_CELL;
                }
                break;

            case S_END_ROW:
                emitRowEnd();
                state = S_NEXT_ROW_OR_EOF;
                break;

            case S_NEXT_ROW_OR_EOF:
                if (data != null) {
                    state = S_BEGIN_ROW;
                    eps = true;
                } else {
                    // EOF, end matrix and accept.
                    state = S_END_MATRIX;
                    eps = true;
                }
                break;

            case S_END_MATRIX:
                // End matrix and accept.
                emitMatrixEnd();
                state = S_ACCEPT;
                errno = E_OK;
                break;

            case S_ACCEPT:
                // Stay here.
                break;

            case S_ERROR:
                // Stay here.
                break;

            default:
                throw new RuntimeException(String.format(
                    "Unhandled state: %d (programming error)", state));
        } // switch
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

