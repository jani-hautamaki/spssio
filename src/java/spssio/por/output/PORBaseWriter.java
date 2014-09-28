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

package spssio.por.output;

// core java
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

// spssio
import spssio.por.PORConstants;
import spssio.por.PORCharset;
import spssio.util.NumberFormatter;
import spssio.util.NumberSystem;


public class PORBaseWriter {

    // CONSTANTS
    //===========

    /**
     * Use Unix-style end-of-lines: LF ({@code '\n'}) only.
     */
    public static final int
        EOL_LF                                  = 0x0A;

    /**
     * Use Windows-style end-of-lines: CR+LF ({@code "\r\n"}.
     */
    public static final int
        EOL_CRLF                                = 0x0D0A;

    // DEFAULTS
    //==========

    /**
     * Default value used for {@see #ostreamBufferSize}.
     */
    public static final int
        DEFAULT_OSTREAM_BUFFER_SIZE             = 0x4000; // 16 KBs

    /**
     * Default value used for {@see #lineLength}.
     */
    public static final int
        DEFAULT_LINE_LENGTH                     = PORConstants.ROW_LENGTH;

    /**
     * Default value used to set {@see #charset}.
     */
    public static final String
        DEFAULT_ENCODING                        = "Cp1252";

    /**
     * Default value used to set {@see #eolMarker}.
     */
    public static final int
        DEFAULT_EOL_MARKER                      = EOL_CRLF;

    /**
     * TODO: Use something from PORConstants
     */
    public static final int
        DEFAULT_MAX_STRING_LENGTH               = PORConstants.MAX_STRING_LENGTH;

    /**
     * Default value used for {@see #truncateLongerStrings}.
     */
    public static final boolean
        DEFAULT_TRUNCATE_LONGER_STRINGS         = false;


    // MEMBER VARIABLES: CONFIG
    //==========================

    /**
     * Configuration variable which controls the buffer size parameter
     * of {@code BufferedOutputStream}'s constructor.
     */
    private int ostreamBufferSize;

    /**
     * Forced line length for the output stream.
     */
    private int lineLength;

    /**
     * Maximum length for an individiual string
     */
    private int maxStringLength;

    /**
     * Whether to truncate strings longer than maxStringLength.
     * If not truncated, an exception is thrown.
     */
    private boolean truncateLongerStrings;

    /**
     * The charset used for encoding the byte sequences into strings
     */
    private Charset charset;

    /**
     * The file's translation map as an encoding table
     */
    private int[] encodingTable;

    /**
     * End-of-line type, either LF (unix) or CRLF (windows).
     */
    private int eolMarker;



    // MEMBER VARIABLES
    //==================

    /**
     * Current output stream
     */
    private OutputStream ostream;

    /**
     * Zero-based row number for the next output line.
     */
    private int row;

    /**
     * Zero-based column number for the next output byte.
     */
    private int column;

    /**
     * NumberSystem object for base-30 numbers.
     * TODO: This reference is not really needed anywhere.
     */
    protected NumberSystem numberSystem;

    /**
     * Formatter for the base-30 numbers
     */
    protected NumberFormatter numberFormatter;

    /**
     * NumberFormatter's internal buffer for quicker access.
     */
    private int[] numberBuffer;


    // CONSTRUCTORS
    //==============

    public PORBaseWriter() {
        // Set the buffer size to the default value
        ostreamBufferSize = DEFAULT_OSTREAM_BUFFER_SIZE;

        // No output stream associated yet.
        ostream = null;

        // Set the line length to the default value
        lineLength = DEFAULT_LINE_LENGTH;

        // Set the maximum string length to the default value
        maxStringLength = DEFAULT_MAX_STRING_LENGTH;

        // Set the longer strings truncation flag to the default value
        truncateLongerStrings = DEFAULT_TRUNCATE_LONGER_STRINGS;

        // Initialize default encoding,
        // TODO: Get the system default?
        charset = null;
        setEncoding(DEFAULT_ENCODING);

        // Set the end-of-line marker to the default value
        // TODO: Get the system default?
        eolMarker = DEFAULT_EOL_MARKER;

        // Reset the next output byte's location
        row = 0;
        column = 0;

        // Create a NumberSystem with default Portable parameters.
        numberSystem = new NumberSystem(30, null);

        // Create the formatter, and associate it to the newly-created
        // number system. Use the default precision specified
        // by the underlying implementation of the NumberFormatter.
        numberFormatter = new NumberFormatter(numberSystem);

        // Get a reference to the formatter's internal buffer
        // for faster access.
        numberBuffer = numberFormatter.getBuffer();

        // Initialize decoding table
        encodingTable = new int[256]; // TBC: use a constant instead?

        // Clear translation; sets the encoding table
        // to identity table, ie. no encoding.
        setTranslation(null);
    }

    // CONFIGURATION METHODS
    //=======================

    public void setEncoding(String charsetName) {
        try {
            charset = Charset.forName(charsetName);
        } catch(UnsupportedCharsetException ex) {
            throw new RuntimeException(String.format(
                "The character set is not supported in this Java VM: %s",
                charsetName));
        } catch(IllegalCharsetNameException ex) {
            throw new RuntimeException(String.format(
                "The charset name contains illegal characters: %s",
                charsetName));
        }
    }

    /**
     * Set byte output translation according to the table.
     *
     * @param translation The output byte translation table
     * to use in encoding, or {@code null} to disable translation.
     *
     */
    public void setTranslation(byte[] translation) {
        if (translation != null) {
            PORCharset.computeEncodingTable(encodingTable, translation);
        } else {
            PORCharset.computeIdentityCodec(encodingTable);
        }
    }

    public void setEolMarker(int eolMarker) {
        // Validate; accept only Unix or Windows style.
        if ((eolMarker != EOL_CRLF) && (eolMarker != EOL_LF)) {
            throw new IllegalArgumentException(String.format(
                "Illegal end-of-line marker specified: 0x%04x",
                eolMarker & 0xffff));
        }
        // Update
        this.eolMarker = eolMarker;
    }

    public int getEolMarker() {
        return eolMarker;
    }

    public void setLineLength(int lineLength) {
        // Validate
        if (lineLength < 1) {
            throw new IllegalArgumentException(String.format(
                "Illegal line length specified: %d", lineLength));
        }
        // Update
        this.lineLength = lineLength;
    }

    public int getLineLength() {
        return lineLength;
    }

    public void setNumberFormatter(NumberFormatter numberFormatter) {
        // Validate
        if (numberFormatter == null) {
            throw new IllegalArgumentException(
                "Illegal NumberFormatter specified: null");
        }
        // Update
        this.numberFormatter = numberFormatter;
    }

    /*
     * TBC: Direct access to the underlying NumberFormatter allowed?
     */
    public NumberFormatter getNumberFormatter() {
        return numberFormatter;
    }

    /*
    public void setPrecision(int precision) {
        numberFormatter.setPrecision(precision);
    }

    public int getPrecision() {
        return numberFormatter.getPrecision();
    }
    */

    // OTHER METHODS
    //===============

    public void bind(OutputStream ostream) {
        if (ostream == null) {
            throw new IllegalArgumentException(
                "Illegal OutputStream specified: null");
        }

        // Bind the output stream
        // throws if ostreamBufferSize <= 0
        this.ostream = new BufferedOutputStream(ostream, ostreamBufferSize);

        // Reset textual location
        row = 0;
        column = 0;
        //offset = 0; //??

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

    /**
     * Return the row of next char
     *
     * @return The row of the next char
     */
    public int getRow() {
        return row;
    }

    /**
     * Return the column of the next char
     *
     * @return The column of the next char
     */
    public int getColumn() {
        return column;
    }

    /**
     * Encode a string into byte array using the configured encoding.
     * @param string The string to encode
     * @return The bytes representing the string in the current encoding.
     */
    public byte[] encodeString(String string) {
        return string.getBytes(charset);
    }

    //=======================================================================
    // LOW-LEVEL OUTPUT METHODS
    //=======================================================================

    public void writeEol()
        throws IOException
    {
        // Write end-of-line sequence.
        // If Windows-style, then precede with CR.
        if (eolMarker == EOL_CRLF) {
            ostream.write('\r'); // CR
        }
        ostream.write('\n'); // LF

        // Reset column and move to next row.
        column = 0;
        row++;
    }

    /**
     * Write a byte to output stream.
     *
     * The current translation table is used. An end-of-line marker is
     * emitted after outputting the byte if the current line's length
     * reaches the configured {#see lineLength}.
     *
     * @param c The byte to write
     */
    public void write(int c)
        throws IOException
    {
        // Apparently SPSS writes an end-of-line sequence
        // after the 'Z' sequence used as eof marker.
        // The following implementation replicates this behaviour.

        // Encode the byte according to the current translation table
        c = encodingTable[c & 0xff];

        // Write to the output stream, may throw.
        ostream.write(c);

        // Move to the next column. If the specified lineLength
        // is reached, finish the current line by writing eol marker.
        column++;

        if (column == lineLength) {
            writeEol();
        }
    } // write(int)

    public void writeBytes(byte[] data, int offset, int len)
        throws IOException
    {
        for (int i = offset; i < len; i++) {
            write(data[i]);
        }
    }


    // SPSS/PSPP PRIMITIVES
    //======================

    /**
     * Write an integer number in base-30 digits.
     *
     * @param value An {@code int}-valued integer number.
     */
    public void writeInt(int value)
        throws IOException
    {

        // Serialize and get the length of the serialization
        int len = numberFormatter.formatInt(value);

        // Output the serialization
        for (int i = 0; i < len; i++) {
            write(numberBuffer[i]);
        }

        // Write number separator
        write(PORConstants.NUMBER_SEPARATOR);
    }

    /**
     * Write a decimal number in base-30 digits.
     * Formatting is done according to the current precision settings.
     *
     * @param value The number to be written.
     */
    public void writeDouble(double value)
        throws IOException
    {
        // Serialize and store the data length
        int len = numberFormatter.formatDouble(value);

        // Write data
        for (int i = 0; i < len; i++) {
            write(numberBuffer[i]);
        } // for

        // Write number separator
        write(PORConstants.NUMBER_SEPARATOR);
    }


    /**
     * Write a string.
     *
     * @param string The string to be written.
     */
    public void writeString(String string)
        throws IOException
    {
        // Encode the string according to "charset"
        byte encodedString[] = encodeString(string);

        // encoded length
        int encodedLength = encodedString.length;

        if ((maxStringLength != 0) &&
            (encodedLength > maxStringLength))
        {
            if (truncateLongerStrings == true) {
                // Truncate to max length.
                // This is DANGEROUS; it is possible that the string
                // is truncated within a codepoint.
                // TODO: Smart truncation.
                encodedLength = maxStringLength;
            } else {
                errorStringTooLong(encodedLength, maxStringLength);
            }
        }

        // Write string length followed with the string
        writeInt(encodedLength);

        // Write data (the string itself)
        for (int i = 0; i < encodedLength; i++) {
            write(encodedString[i]);
        } // for
    }

    /**
     * Write a base-30 number after reformatting it according to
     * to the current precision settings.
     *
     * @param number The base-30 number pre-formatted as a {@code String}
     * that in the current NumberSystem that is to be serialized.
     *
     */
    public void writeReformattedNumber(String number)
        throws IOException
    {
        // Serialize the string into the buffer
        int len = number.length();

        // Truncate the length if necessary
        if (len > numberBuffer.length) {
            len = numberBuffer.length;
        }

        // Serialize the number into the number buffer.
        for (int i = 0; i < len; i++) {
            numberBuffer[i] = number.charAt(i);
        }

        // Reformat and store the length of serialization
        len = numberFormatter.reformat(
            numberBuffer, len, numberFormatter.getPrecision());

        // Write out the value
        for (int i = 0; i < len; i++) {
            write(numberBuffer[i]);
        }

        // Write number separator
        write(PORConstants.NUMBER_SEPARATOR);
    }

    /**
     * Write a SYSMISS numeric value with the default value separator.
     *
     * @see PORConstants#SYSMISS_SEPARATOR
     */
    public void outputSysmiss()
        throws IOException
    {
        write(PORConstants.SYSMISS_MARKER);
        write(PORConstants.SYSMISS_SEPARATOR);
    }

    /**
     * Write a SYSMISS numeric value with a specified value separator.
     *
     * @param sep The character to be used as a value separator
     *      after SYSMISS value.
     */
    public void outputSysmiss(int sep)
        throws IOException
    {
        write(PORConstants.SYSMISS_MARKER);
        write(sep);
    }

    /**
     * Write a tag code
     *
     * @param c The tag code
     */
    public void outputTag(int c)
        throws IOException
    {
        write(c);
    }

    /**
     * Write end-of-file markers ('Z') to complete the current line.
     *
     */
    public void outputEofMarkers()
        throws IOException
    {
        // Write end-marker
        write(PORConstants.EOF_MARKER); // 'Z'

        if (column > 0) {
            // Complete the last line with end-markers.
            int len = lineLength-column;
            for (int i = 0; i < len; i++) {
                write(PORConstants.EOF_MARKER); // 'Z'
            }
        }

    }

    // EXCEPTION THROWERS
    //====================

    protected static void errorStringTooLong(
        int encodedLength,
        int maxLength
    ) {
        throw new RuntimeException(String.format(
            "String too long: %d (max allowed length: %d)",
            encodedLength, maxLength));
    }


} // class PORBaseWriter

