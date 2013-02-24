
//*******************************{begin:header}******************************//
//                 spssio - http://code.google.com/p/spssio/                 //
//***************************************************************************//
//
//      Java classes for reading and writing 
//      SPSS/PSPP Portable and System files
//
//      Copyright (C) 2013 Jani Hautamaki <jani.hautamaki@hotmail.com>
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
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

// spssio portable
import spssio.por.PORFile;
import spssio.por.PORCharset;

/*

*/
public class PORReader
{
    
    // CONSTANTS
    //===========
    
    /** 
     * The default row length used (TODO, this value should be shared
     * with the writer too).
     */
    public static final int SPSS_PORTABLE_ROW_LENGTH = 80;
    
    /**
     * Buffer size used for creating BufferedInputStream, unless
     * specified
     */
    public static final int DEFAULT_BUFFER_SIZE = 16384;
    
    // MEMBER VARIABLES
    //==================
    
    /**
     * Configuration variable; the size of the buffer to create.
     * Initialized to the default value.
     */
    private int opt_buffer_size;

    /** 
     * Configuration variable; determines the length of the rows.
     * If a row is longer than this, it is an error. If a row is shorter,
     * then it is internally widened into this length by spaces. */
    private int opt_row_length;


    /** 
     * The input stream
     */
    private BufferedInputStream istream;

    /** Current row */
    private int row;

    /** Current column, may be virtual if the row is internally widened */
    private int col;
    
    /** current input byte, before translation */
    private int lastc;
    
    /** Decoding table */
    private int[] dtable;
    
    // TRANSIENT/AUXILIARY
    //=====================
    
    /**
     * The PORFile object that is being built
     */
    private PORFile por;
    
    // CONSTRUCTORS
    //==============
    
    public PORReader() {
        opt_buffer_size = DEFAULT_BUFFER_SIZE;
        opt_row_length = SPSS_PORTABLE_ROW_LENGTH;

        // Allocated once
        dtable = new int[256];
        
        istream = null;
        row = 0;
        col = 0;
        lastc = -1;
    } // ctor
    
    // OTHER METHODS
    //===============
    
    /*
    public void open(String fname) {
    } // open()
    
    public void open(File file) {
    } // open()
    
    public void close() {
    } // close()
    */
    
    public PORFile parse(String fname) 
        throws FileNotFoundException
    {
        // May throw FileNotFound
        FileInputStream fis = new FileInputStream(fname);
        
        PORFile rval = parse(fis);
        
        // May throw IOException, close quietly.
        try {
            fis.close();
        } catch(IOException ex) {
            System.err.printf("%s: FileInputStream.close() failed. Ignoring.\n", fname);
        } // try-catch
        
        return rval;
    } // parse()
    
    public PORFile parse(InputStream is) {
        // Initialize
        istream = new BufferedInputStream(is, opt_buffer_size);
        // Reset location
        col = 0;
        row = 1;
        lastc = -1;
        
        // TODO: clear dtable?
        
        try {
            parse();
        } catch(Exception ex) {
            System.out.printf("Row: %d, column: %d\n", row, col);
            ex.printStackTrace();
        } // try-catch
        
        return por;
    } // parse()
    
    
    
    // RECURSIVE DESCENT PARSER FUNCTIONS
    //====================================
    
    protected void parse() 
    {
        
        // Create a new PORFile that will be built from the input stream.
        por = new PORFile();
        
        // Read the 200-byte header
        parse_splash_strings();
        
        // Read the 256-byte characte set mapping
        parse_charset_map();
        
        // Reads the 8-byte signature
        parse_signature();
        
        // reads the 1-byte format identifier
        parse_file_version();
        
        // Reads the 8-byte creation date, and the 6-byte creation time.
        parse_creation_datetime();
        
        // from this point on, read "tag" and switch until tag='F' is met.
        
        
        
        
    } // parse()
    
    protected void parse_splash_strings() {
        byte[] array = new byte[5*40];
        
        // Read to array
        read(array, 0, array.length);
        
        // Set the splash strings
        por.header.splash = array;
    } // parse_splash_strings()
    
    protected void parse_charset_map() {
        // TODO:
        // Avoid allocating a new one, and use the one that
        // has been allocated to the PORFile?
        
        byte[] table = new byte[256];
        
        // Read the character map
        read(table, 0, table.length);

        // Record the charset table into the header. This is done prior
        // to any kind of validation in order for the table to be available
        // for inspection even if the validation fails.
        por.header.charset = table;
        
        
        // Calculate a decoding table
        PORCharset.initDecoderTable(dtable, table);
        
    } // parse_charset_map()
    
    
    protected void parse_signature() {
        
        // Allocate an array for the signature
        byte[] array = new byte[8];
        
        // Read the signature
        read(array, 0, array.length);
        
        // Decode the signature
        decode(array);
        
        // Convert into a string
        String signature = new String(array);
        
        // Finally, assert that the file is SPSS Portable file
        if (signature.equals("SPSSPORT") == false) {
            throw new RuntimeException(String.format(
                "Unexpected file signature \"%s\"",  signature));
        }

        // Record the signature
        por.header.signature = signature;
    } // parse_signature()

    protected void parse_file_version() {
        int c = readc();
        // TODO: decode?
        
        // TODO:
        // Validate: expect 'A'.
        
        por.header.version = (char) c;
    } // parse_file_version();

    protected void parse_creation_datetime() {
        
        String date = parse_string();
        String time = parse_string();
        
    } // parse_creation_timestamp();

    
    // PORTABLE PRIMITIES
    //====================
    
    protected String parse_string() {
        String rval = null;
        
        // Read an integer field into "ccount".
        // The integer could, in theory, be expressed in a scientific notation,
        // eg. "0.008+3" for 8 chars.
        
        
        
        
        // Read "ccount" chars.
        
        return rval;
    } // parse_string()
    
    // No sign allowed. May have an exponent?
    // String length could be something like 0.008+3, or just 8, or 8000-3.
    // TODO: Should try tweaking POR files manually to see how SPSS does it.
    
    // states
    private static final int S_START                    = 0;
    private static final int S_SIGN_OR_DIGIT_OR_DOT     = 1;
    private static final int S_DIGIT_OR_DOT             = 2;
    private static final int S_DIGIT_OR_DOT_OR_SLASH    = 3;
    private static final int S_DIGIT_AFTER_DOT          = 4;
    private static final int S_DIGIT_OR_SIGN_OR_SLASH   = 5;
    private static final int S_DIGIT_OR_SLASH           = 6;
    private static final int S_ACCEPT                   = 99;
    
    
    protected String parse_double() {
        StringBuilder sb = new StringBuilder();
        
        int c = -1;
        int state = S_START;
        boolean eps = false;
        boolean num_negative = false;
        boolean exp_negative = false;
        
        try {
            do {
                if (eps == false) {
                    c = read();
                    
                    // If eof was reached, throw
                    if (c == -1) {
                        error_eof("parse_double()");
                    }
                } // if: consume
                
                // Set null-transition to false
                eps = false;
                
                switch(state) {
                    case S_START:
                        // Leading spaces are accepted.
                        if (c == ' ') {
                            // ok
                        } 
                        else {
                            // expect the first letter of the actual number
                            state = S_SIGN_OR_DIGIT_OR_DOT;
                            eps = true;
                        }
                        break;
                    
                    case S_SIGN_OR_DIGIT_OR_DOT:
                        // No matter what, the next state is digit or dot.
                        state = S_DIGIT_OR_DOT;
                        if (c == '+') {
                            // Plus sign, no action required
                        }
                        else if (c == '-') {
                            // Negative sign
                            num_negative = true;
                        }
                        else {
                            // Not a sign, then it must be either digit or dot.
                            eps = true;
                        }
                        break;
                        
                    case S_DIGIT_OR_DOT:
                        if (isBase30(c)) {
                            // It is a base-30 digit
                            state = S_DIGIT_OR_DOT_OR_SLASH;
                        }
                        else if (c == '.') {
                            // Must still have at least a single base-30 digit.
                            state = S_DIGIT_AFTER_DOT;
                        }
                        else {
                            // ERROR: unexpected character
                            throw new RuntimeException(String.format(
                                "Invalid base-30 number; unexpected integer part"));
                        } // if-else
                        break;

                    case S_DIGIT_AFTER_DOT:
                        if (isBase30(c)) {
                            // ok, got the required digit.
                            state = S_DIGIT_OR_SIGN_OR_SLASH;
                        }
                        else {
                            throw new RuntimeException(String.format(
                                "Invalid base-30 number; unexpected decimal part"));
                        }
                        break;
                        
                    case S_DIGIT_OR_DOT_OR_SLASH:
                        if (isBase30(c)) {
                            // ok
                        }
                        else if (c == '.') {
                            // trigesimal point, expect digits_sign_slash
                            state = S_DIGIT_OR_SIGN_OR_SLASH;
                        }
                        else if (c == '/') {
                            // ACCEPT
                            state = S_ACCEPT;
                        }
                        break;
                        
                    case S_DIGIT_OR_SIGN_OR_SLASH:
                        if (isBase30(c)) {
                            // ok
                        }
                        else if (c == '+') {
                            // positive exponent, no action required
                            state = S_DIGIT_OR_SLASH;
                        }
                        else if (c == '-') {
                            // negative exponent
                            exp_negative = true;
                            state = S_DIGIT_OR_SLASH;
                        }
                        else if (c == '/') {
                            // accept
                            state = S_ACCEPT;
                        }
                        break;
                        
                    case S_DIGIT_OR_SLASH:
                        if (isBase30(c)) {
                            // ok
                        }
                        else if (c == '/') {
                            // accept
                            state = S_ACCEPT;
                        }
                        else {
                            // ERROR
                            throw new RuntimeException(String.format(
                                "Invalid base-30 digit; unexpected exponent"));
                        }
                        break;
                        
                    default:
                        throw new RuntimeException(String.format(
                            "Unrecognized state=%d (programming error)", state));
                } // switch
            } while (state != S_ACCEPT);
        } catch(IOException ex) {
        } // try-catch
        
        return sb.toString();
    } // parse_double()
    
    protected static boolean isBase30(int c) {
        return ((('0' <= c) && (c <= '9')) || (('A' <= c) && (c <= 'T')));
    } // isBase30
    
    protected int parse_uint() {
        StringBuilder sb = new StringBuilder();
        int c;
        int rval = 0;
        int len = 0;
        
        do {
            c = readc();
            if (c == -1) {
                // eof
                throw new RuntimeException(String.format(
                    "unexpected end-of-file"));
            }
            
            // TODO:
            // decode
            
            // TODO:
            // validate? (no trigesimal point allowed!)
            if ((len != 0) && ((c == '-') || (c == '+'))) {
                // switch to reading an exponent
            }
            
            sb.append(c);
            len++;
            
        } while (c != '/');
        
        // TODO
        // atoi
        
        return rval;
    } // parse_uint()
    

    // SUPPORT FUNCTIONS
    //===================

    
    protected void decode(byte[] array) {
        /*
        for (int i = 0; i < array.length; i++) {
            int inc = ((int) array[i]) & 0xff;
            int outc = dtable[inc];
            
            if (outc == -1) {
                System.err.printf("No translation for character \'%c\' (%d)\n", 
                    inc, inc);
                continue;
            }
            // Otherwise translate
            array[i] = (byte) outc;
        } // for: each item in the array
        */
    } // decode
    
    protected void read(byte[] array, int offset, int len) {
        int result = 0;
        int c = -1;
        
        try {
            for (result = 0; result < len; result++) {
                c = read();
                
                if (c == -1) {
                    System.out.printf("eof\n");
                    // eof
                    break;
                }
                array[result] = (byte) c;
            } // for
            System.out.printf("read %d bytes\n", result);
        } catch(IOException ex) {
            error_io(String.format(
                "BufferedInputStream.read(byte[], %d, %d)",
                offset, len), ex);
        } // try-catch
        
        // If the read() didn't got as many bytes as required,
        // then the only explanation is that eof was reached.
        if (result != len) {
            error_eof(String.format(
                "BufferedInputStream.read(byte[], %d, %d)",
                offset, len));
        } // if: 
    } // read()
    
    protected int readc() {
        int rval = -1;
        try {
            // May throw
            rval = read();
        } catch(IOException ex) {
            error_io("BufferedInputStream.read()", ex);
        } // try-catch
        
        // End-of-file is not allowed. If eof detected, throw an exception
        if (rval == -1) {
            error_eof("BufferedInputStream.read()");
        } // if: eof
        
        return rval;
    } //  readc()
    
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
            "%s failed: unexpected end-of-file"));
    } // error_eof()
    
    protected int read() 
        throws IOException
    {
        int rval = -1;
        
        do {
            if (lastc == '\n') {
                if (col < opt_row_length) {
                    // Keep filling with spaces until the required row length
                    // has been reached.
                    // It has to be space (0x20) BEFORE the decoding.
                    
                    rval = ' ';
                    break; // exit loop
                } else {
                    // Otherwise, begin a new line
                    row++;
                    col = 0;
                    System.out.printf("<eol>\n");
                }
            } // if-else
            
            // This call shoudn't be slow, because the input stream has been
            // wrapped into BufferedInputStream.
            lastc = istream.read();
            
            if (lastc == -1) {
                // end-of-file. Return immediately, and do not increase column.
                return -1;
            }
            else if (lastc == '\r') {
                // skip this, and read next.
            }
            else if (lastc == '\n') {
                // skip this here, and read next.
            } 
            else {
                // acceptable character.
                rval = lastc;
            } // if-else
        } while (rval == -1);
        
        // Increase column
        col++;
        if (col > opt_row_length) {
            throw new RuntimeException(String.format(
                "row %d is too long (more than %d chars)", row, opt_row_length));
        }
        System.out.printf("%c", rval);
        
        // TODO:
        // Apply decoding?
        
        return rval;
    } // read()

    
    // TODO:
    // How to account for SYSMIS values? (asterisk followed by period "*.")
    // They are accounted only in the casereader, which attempts to match
    // the current input.
    
    public String parseNumeric() {
        StringBuilder sb = new StringBuilder();
        return sb.toString();
    } // parseNumeric()
    
    public String parseInteger() {
        String rval = null;
        return rval;
    } // parseInteger()
    
    // TODO: Do not translate the string???
    
    public String parseString() {
        /*
        int len = parseInteger();
        */ 
        int len = 1;
        StringBuilder sb = new StringBuilder(len);
        
        
        return sb.toString();
    }
    
    // Translated
    public char parseChar() {
        char rval = '\0';
        return rval;
    }
    
    /*
    public String parseNumericData(boolean allow_sysmis) {
    }
    public String parseStringData(boolean allow_sysmis) {
    }
    */
    
    
    
    public static void main(String[] args) {
        try {
            PORReader preader = new PORReader();
            
            for (int i = 0; i < args.length; i++) {
                System.out.printf("%s\n", args[i]);
                preader.parse(args[i]);
            }
        } catch(Exception ex) {
        } // try-catch
    } // main()
    
} // class PORReader








