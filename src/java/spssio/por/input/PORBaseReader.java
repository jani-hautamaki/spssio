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


package spssio.por.input;


// core java
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

// spssio
import spssio.por.PORConstants;
import spssio.por.PORCharset;
import spssio.util.NumberParser;
import spssio.util.NumberSystem;

public class PORBaseReader {

    // CONSTANTS
    //===========

    /**
     * Default value used for {@see #istreamBufferSize}.
     */
    public static final int 
        DEFAULT_ISTREAM_BUFFER_SIZE             = 0x4000; // 16 KBs
    
    /**
     * Default value used for {@see #lineLength}.
     */
    public static final int 
        DEFAULT_LINE_LENGTH                     = PORConstants.ROW_LENGTH;
    
    /**
     * Default value used for {@see #allowLongerLines}.
     */
    public static final boolean 
        DEFAULT_ALLOW_LONGER_LINES              = false;
    
    /**
     * Default value used to set {@see #charset}.
     */
    public static final String 
        DEFAULT_ENCODING                        = "Cp1252";
    
    /**
     * TODO: Use something from PORConstants
     */
    public static final int 
        DEFAULT_MAX_STRING_LENGTH               = 255;
        
    /**
     * Default value used for {@see #allowLongerStrings}.
     */
    public static final boolean
        DEFAULT_ALLOW_LONGER_STRINGS            = false;
    
    // MEMBER VARIABLES: CONFIG
    //==========================
    
    /**
     * Configuration variable which controls the buffer size parameter 
     * of {@code BufferedInputStream}'s constructor. 
     */
    private int istreamBufferSize;
    
    /**
     * Nominal length of a single text line in the source stream
     */
    private int lineLength;
    
    /**
     * Whether to allow lines longer than the nominal length.
     */
    private boolean allowLongerLines;
    
    /**
     * Maximum length for an individiual string
     */
    private int maxStringLength;
    
    /**
     * Whether to allow strings with longer length than
     * maxStringLength. If allowed, longer strings are
     * truncated to maxStringLength.
     */
    private boolean allowLongerStrings;
    
    /**
     * The charset used for decoding the byte sequences into strings
     */
    private Charset charset;
    
    /**
     * The file's translation map as a decoding table
     */
    private int[] decodingTable;
    
    // MEMBER VARIABLES
    //==================
    
    /**
     * Current input stream
     */
    private InputStream istream;
    
    /**
     * Last byte read
     */
    private int lastByte;
    
    /**
     * Zero-based row number for the next input line.
     */
    private int row;
    
    /**
     * Zero-based column number for the next input byte.
     */
    private int column;
    
    /**
     * Offset of the next input byte
     */
    private int offset;
    
    /**
     * NumberSystem object for base-30 numbers
     */
    protected NumberSystem numberSystem;
    
    /**
     * Parser for the base-30 numbers
     */
    protected NumberParser numberParser;
    
    /**
     * Internal buffer used for reading strings
     */
    private byte[] stringBuffer;
    
    // CONSTRUCTORS
    //==============
    
    public PORBaseReader() {
        istreamBufferSize = DEFAULT_ISTREAM_BUFFER_SIZE;
        istream = null;
        lineLength = DEFAULT_LINE_LENGTH;
        allowLongerLines = DEFAULT_ALLOW_LONGER_LINES;
        setEncoding(DEFAULT_ENCODING);
        maxStringLength = DEFAULT_MAX_STRING_LENGTH; 
        allowLongerStrings = DEFAULT_ALLOW_LONGER_STRINGS;
        
        row = 0;
        column = 0;
        offset = 0;

        lastByte = -1;
        
        // Initialize number system
        numberSystem = new NumberSystem(30, null);
        numberParser = new NumberParser(numberSystem);
        
        // Initialize string buffer
        stringBuffer = new byte[maxStringLength];
        
        // Initialize decoding table
        decodingTable = new int[256]; // TBC: use a constant instead?
        
        // Clear translation
        clearTranslation();
        
    }
    
    // OTHER METHODS
    //===============
    
    public InputStream bind(InputStream in) {
        return bind(in, true);
    }
    
    public InputStream bind(InputStream in, boolean wrap) {
        
        // Set this to null before attempting to construct the stream.
        // Should the constructor fail, it will be safe to call unbind()
        istream = null;
        
        if (wrap == true) {
            // May throw.
            istream = new BufferedInputStream(in, istreamBufferSize);
        } else {
            istream = in;
        }
        
        setLocation(0, 0);
        setOffset(0);
        
        return istream;
    }
    
    public void close() 
        throws IOException
    {
        if (istream == null) {
            throw new RuntimeException("Not bound to an InputStream");
        }
        
        InputStream in = unbind();
        
        // May throw
        in.close();
    }
    
    public InputStream unbind() {
        InputStream rval = istream;
        istream = null;
        return rval;
    }
    

    public void setLocation(int row, int column) {
        this.row = row;
        this.column = column;
    }
    
    public void setOffset(int offset) {
        this.offset = 0;
    }
    
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
    
    public void setMaxStringLength(int maxStringLength) {
        if (maxStringLength < 1) {
            throw new IllegalArgumentException();
        }
        
        // Assign and reallocate string buffer
        this.maxStringLength = maxStringLength;
        stringBuffer = new byte[maxStringLength];
    }
    
    public void clearTranslation() {
        PORCharset.computeIdentityTable(decodingTable);
    }
    
    public void setTranslation(byte[] translation) {
        PORCharset.computeDecodingTable(decodingTable, translation);
    }
    
    // ERROR MANAGEMENT
    //==================
    
    
    public int getRow() {
        return row;
    }
    
    public int getColumn() {
        return column;
    }
    
    public int getOffset() {
        return offset;
    }
    
    // READ METHODS
    //==============
    
    /**
     * Read a single byte and keep count of the current row and column.
     * This method also appends the lines to row_length if newline is met
     * earlier than expected. If eof is reached, returns -1.
     */
    public int read() 
        throws IOException
    {
        int rval = -1;
        
        do {
            if (lastByte == '\n') {
                // Interpreted as the end-of-line marker 
                // for both Windows and Unix. 
                
                if (column < lineLength) {
                    // Keep filling with spaces until 
                    // the nominal line length has been reached.
                    // The value fictiously read has to be whitespace (0x20)
                    // BEFORE the decoding takes place.
                    rval = PORConstants.WHITESPACE;
                    break; // exit loop
                } else {
                    // Otherwise, begin a new line
                    row++;
                    column = 0;
                }
            } // if-else
            
            // Read next byte from the stream. May throw!
            lastByte = istream.read();
            
            if (lastByte == -1) {
                // End-of-file.
                // Return immediately without increasing 
                // the column or the file offset.
                return lastByte;
            }
            else if (lastByte == '\r') {
                // Ignored. Read next.
            }
            else if (lastByte == '\n') {
                // Already handled. Read next.
            } 
            else {
                // Otherwise the byte read is valid for returning.
                rval = lastByte;
            } // if-else
            
            // Increase the file offset as the eof was not reached.
            offset++;
            
            // Repeat until end-of-file or valid byte.
        } while (rval == -1);
        
        // Increase column
        column++;
        if ((column > lineLength) && (allowLongerLines == false)) {
            errorLineTooLong(lineLength);
        }
        
        // Apply decoding
        rval = decodingTable[rval];
        
        return rval;
    } // read()
    
    public void readBytes(
        byte[] array,
        int offset,
        int length
    ) {
        /*
        if (offset+length > array.length) {
            errorArrayTooShort();
        }
        */
        
        try {
            // The bytes must be read one by one.
            for (int i = offset; i < length; i++) {
                int b = read();
                if (b == -1) {
                    // Premature eof
                    errorEof();
                }
                array[i] = (byte) b;
            } // for
        } catch(IOException ex) {
            errorIoException(ex);
        } // try-catch
    }
    
    public int readChar() {
        int rval = -1;
        
        try {
            rval = read();
        } catch(IOException ex) {
            errorIoException(ex);
        }
        
        if (rval == 1) {
            errorEof();
        }
        
        return rval;
    }
    
    public int readIntU() {
        int c;
        
        // Reset the NumberParser
        numberParser.reset();
        
        /*
        do {
            c = read();
            if (c == -1) {
                errorEof();
            }
        } while (c == PORConstants.WHITESPACE);
        */

        // Eat up leading whitespaces
        while ((c = readChar()) == PORConstants.WHITESPACE);
        
        if (c == PORConstants.SYSMISS_MARKER) { // '*'
            // The SYSMISS should never be encountered 
            // when an unsigned integer is expected.
            errorUnexpectedSysmiss();
            
            // TODO: This needs reconsideration.
            // This is a missing value.
            //readChar(); // consume the succeeding dot.
        }
        
        // Emit to parser until a slash is found.
        while (c != PORConstants.NUMBER_SEPARATOR) { // '/'
            numberParser.consume(c);
            c = readChar();
        }
        
        // Signal end-of-data
        int errno = numberParser.consume(-1);
        
        // Inspect result
        if (errno != NumberParser.E_OK) {
            errorInvalidNumber(numberParser.strerror());
        }
        
        if (numberParser.lastsign() < 0) {
            errorInvalidNumber("Expected non-negative number");
        }
        
        // Pull out the parsed number,
        // and convert it to an integer
        double dvalue = numberParser.lastvalue();
        int rval = (int) dvalue;
        
        // Verify that the number is an integer
        if (((double) rval) != dvalue) {
            errorInvalidNumber("Expected an integer");
        }
        
        return rval;
    } // readIntU()
    
    public String readString() {
        
        // String's length in number of bytes
        int encodedLength = readIntU();
        int skipLength = 0;
        
        // Verify that the string's length is in sensible limits.
        if (encodedLength > maxStringLength) {
            if (allowLongerStrings == true) {
                // Truncate to max length
                skipLength = encodedLength - maxStringLength;
                encodedLength = maxStringLength;
            } else {
                errorStringTooLong(encodedLength, maxStringLength);
            }
        }
        
        // Fill the array. 
        for (int i = 0; i < encodedLength; i++) {
            stringBuffer[i] = (byte) readChar();
        }
        
        // Skip bytes if the string is too long
        for (int i = 0; i < skipLength; i++) {
            readChar();
        }
        
        return new String(stringBuffer, 0, encodedLength, charset);
    } // readString()
    
    
    public String readBytesAsString(int encodedLength) {
        // Fill the array. 
        for (int i = 0; i < encodedLength; i++) {
            stringBuffer[i] = (byte) readChar();
        }
        
        return new String(stringBuffer, 0, encodedLength, charset);
    } // readBytesAsString()
    
    // TODO:
    // readNumberAsString()

    /*
    public String readNumber() 
        throws IOException
    {
        int c;
        
        // TODO: Use an internal buffer instead to gather the bytes...
        // ... with a constant
        StringBuilder sb = new StringBuilder(128);
        
        // Reset the NumberParser
        numberParser.reset();
        
        // Eat up leading whitespaces
        c = readChar();
        while (c == PORConstants.WHITESPACE) { // ' '
            sb.append((char) c);
            c = readChar();
        } // while
        
        if (c == PORConstants.SYSMISS_MARKER) { // '*'
            // This is a missing value.
            sb.append((char) c);
            // Consume the sysmiss separator
            c = readChar();
            sb.append((char) c);
        } else {
            // Emit to parser until a slash is found.
            while (c != PORConstants.NUMBER_SEPARATOR) { // '/'
                numberParser.consume(c);
                sb.append((char) c);
                c = readChar();
            } // while
            
            // Signal end-of-data
            int errno = numberParser.consume(-1);

            // Inspect result
            if (errno != NumberParser.E_OK) {
                errorInvalidNumber(numberParser.strerror());
            } // if: error
            
        } // if-else
        
        return sb.toString();
    } // readNumber()
    */


    protected static void errorInvalidNumber(String desc) {
        throw new RuntimeException(String.format(
            "Invalid number: %s", desc));
    }
    
    protected static void errorLineTooLong(int lineLength) {
        throw new RuntimeException(String.format(
            "Nominal line length (%d) exceeded", lineLength));
    }
    
    protected static void errorStringTooLong(int encodedLength, int max) {
        throw new RuntimeException(String.format(
            "String too long: %d (max allowed length: %d)",
            encodedLength, max));
    }
    
    protected static void errorUnexpectedSysmiss() {
        throw new RuntimeException(String.format(
            "Unexpected SYSMISS while reading a non-negative integer"));
    }
    
    protected static void errorEof() {
        throw new RuntimeException(String.format(
            "Unexpected end-of-file"));
    }
    
    protected static void errorIoException(IOException ex) {
        String msg = ex.getMessage();
        if (msg == null) {
            msg = "(error message not available)";
        }
        
        msg = String.format("An I/O error occurred: %s", msg);
        
        throw new RuntimeException(msg, ex);
    }
    
    
    
}