
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
import spssio.por.PORValue;
import spssio.por.PORVariable;
import spssio.por.PORMissingValue;
import spssio.por.PORValueLabels;

// spssio common
import spssio.common.SPSSFormat;

// spssio utils
import spssio.util.NumberSystem;
import spssio.util.NumberParser;

/*
 *
 */
public class PORReader
{
    
    // CONSTANTS
    //===========
    
    /** 
     * The default row length used 
     * (TODO: This value should be shared with the writer too).
     */
    public static final int SPSS_PORTABLE_ROW_LENGTH = 80;
    
    /**
     * Buffer size used for creating BufferedInputStream, unless specified.
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

    /** Number of the current row. */
    private int row;

    /** 
     * Number of the current column.
     * Can be virtual if the row is internally widened 
     */
    private int col;
    
    /** Current input byte, before translation */
    private int lastc;
    
    /** Decoding table */
    private int[] dectab;
    
    /**
     * NumberSystem object for base-30 numbers
     */
    private NumberSystem numsys;
    
    /**
     * Parser for the base-30 numbers
     */
    private NumberParser numparser;
    
    // TRANSIENT/AUXILIARY
    //=====================
    
    /**
     * The PORFile object that is being built
     */
    private PORFile por;
    
    /**
     * PORVariable currently under construction.
     */
    private PORVariable lastvar;
    
    // CONSTRUCTORS
    //==============
    
    public PORReader() {
        opt_buffer_size = DEFAULT_BUFFER_SIZE;
        opt_row_length = SPSS_PORTABLE_ROW_LENGTH;

        // Allocated decoding table only once.
        dectab = new int[256];
        
        istream = null;
        row = 0;
        col = 0;
        lastc = -1;
        
        // Initialize number system
        numsys = new NumberSystem(30, null);
        numparser = new NumberParser(numsys);
        
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
        
        // Parse the input stream
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
        
        // Clear auxiliary variables
        lastvar = null;
        
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
        int tag;
        do {
            tag = readc();
            System.out.printf("tag: %c\n", tag);
            switch(tag) {
                case '1':
                    parse_software();
                    break;
                
                case '2':
                    parse_author();
                    break;
                
                case '3':
                    parse_title();
                    break;
                
                case '4':
                    parse_varcount();
                    break;
                
                case '5':
                    parse_precision();
                    break;
                
                case '6':
                    parse_weight_variable();
                    break;
                    
                case '7':
                    parse_variable_record();
                    break;
                
                case '8':
                    parse_missing_discrete();
                    break;
            
                case '9':
                    parse_missing_open_lo();
                    break;
                
                case 'A':
                    parse_missing_open_hi();
                    break;
                
                case 'B':
                    parse_missing_closed();
                    break;
                
                case 'C':
                    parse_variable_label();
                    break;
                
                case 'D':
                    parse_value_labels();
                    break;
                
                case 'E':
                    parse_document_records();
                    break;
                
                case 'F':
                    // Will exit
                    break;
                
                default:
                    throw new RuntimeException(String.format(
                        "Unexpected tag code \'%c\'", tag));
            } // switch
        } while (tag != 'F');
    } // parse()
    
    protected void parse_splash_strings() {
        // Allocate a byte array for the splash strings.
        byte[] array = new byte[5*40];
        
        // Populate the array
        read(array, 0, array.length);
        
        // Set the splash strings
        por.header.splash = array;
    } // parse_splash_strings()
    
    protected void parse_charset_map() {
        // TODO:
        // Avoid allocating a new one, and use the one that
        // has been allocated to the PORFile?
        
        byte[] array = new byte[256];
        
        // Read the next 256 bytes (=character map) into "table".
        read(array, 0, array.length);

        // Record the charset table into the header. This is done prior
        // to any kind of validation in order for the table to be available
        // for inspection even if the validation fails.
        por.header.charset = array;
        
        // Compute a decoding table
        PORCharset.computeDecodingTable(dectab, array);
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
        
        // TODO: 
        // Decode?
        
        // TODO:
        // Validate: expect 'A'.
        
        por.header.version = (char) c;
    } // parse_file_version();

    protected void parse_creation_datetime() {
        // Parse creation date
        por.header.date = parse_string();
        
        // Parse creation time of day
        por.header.time = parse_string();
    }

    
    protected void parse_software() {
        por.header.software = parse_string();
    }
    
    protected void parse_author() {
        por.header.author = parse_string();
    }
    
    protected void parse_title() {
        por.header.title = parse_string();
    }
    
    protected void parse_varcount() {
        por.header.nvariables = parse_uint();
    }
    
    protected void parse_precision() {
        por.header.precision = parse_uint();
    }
    
    protected void parse_weight_variable() {
        por.header.weight_var_name = parse_string();
    }
    
    protected void parse_variable_record() {
        // Create a new PORVariable object
        lastvar = new PORVariable();
        
        // Add it immediately to the PORFile object
        por.variables.add(lastvar);
        
        lastvar.width = parse_uint();
        // TODO: Validate value range: 0-255
        
        lastvar.name = parse_string();
        // TODO: Validate name length; 1-8
        
        SPSSFormat fmt = null;
        
        fmt = new SPSSFormat();
        fmt.type = parse_uint();
        fmt.width = parse_uint();
        fmt.decimals = parse_uint();
        // TODO: Validate numeric values
        lastvar.printfmt = fmt;
        
        fmt = new SPSSFormat();
        fmt.type = parse_uint();
        fmt.width = parse_uint();
        fmt.decimals = parse_uint();
        // TODO: Validate numeric values
        lastvar.writefmt = fmt;
        
    } // parse_variable_record()
                
    protected void parse_missing_discrete() {
        if (lastvar == null) {
            error_syntax("Tag '7\' (variable record) should precede tag=\'8' (missing value)");
        }
        
        // Create missing value record
        PORMissingValue miss
            = new PORMissingValue(PORMissingValue.TYPE_DISCRETE_VALUE);
        
        // Append to variable record
        lastvar.missvalues.add(miss);
        
        // Parse the value
        miss.values[0] = parse_value(lastvar);
    } // parse_missing_discrete()
    
            
    protected void parse_missing_open_lo() {
        if (lastvar == null) {
            error_syntax("Tag '7\' (variable record) should precede tag=\'9' (missing open lo)");
        }
        
        // Create missing value record
        PORMissingValue miss
            = new PORMissingValue(PORMissingValue.TYPE_RANGE_OPEN_LO);
        
        // Append to variable record
        lastvar.missvalues.add(miss);
        
        // Parse the value
        miss.values[0] = parse_value(lastvar);
    } // parse_missing_open_lo()
    
    protected void parse_missing_open_hi() {
        if (lastvar == null) {
            error_syntax("Tag '7\' (variable record) should precede tag=\'A' (missing open hi)");
        }
        
        // Create missing value record
        PORMissingValue miss
            = new PORMissingValue(PORMissingValue.TYPE_RANGE_OPEN_HI);
        
        // Append to variable record
        lastvar.missvalues.add(miss);
        
        // Parse the value
        miss.values[0] = parse_value(lastvar);
    } // parse_missing_open_hi()
    
    protected void parse_missing_closed() {
        if (lastvar == null) {
            error_syntax("Tag '7\' (variable record) should precede tag=\'A' (missing range)");
        }
        
        // Create missing value record
        PORMissingValue miss
            = new PORMissingValue(PORMissingValue.TYPE_RANGE_CLOSED);
        
        // Append to variable record
        lastvar.missvalues.add(miss);
        
        // Parse the values
        miss.values[0] = parse_value(lastvar);
        miss.values[1] = parse_value(lastvar);
    } // parse_missing_closed()
    
    protected void parse_variable_label() {
        if (lastvar == null) {
            error_syntax("Tag '7\' (variable record) should precede tag=\'A' (variable label)");
        }
        
        lastvar.label = parse_string();
    } // parse_variable_label()
    
    /**
     * The ability to parse (value, label) pairs requires knowledge whether
     * the values are numerical or textual. This information is not contained
     * within the value labels record itself, so it cannot be deduced.
     * Therefore, the only way of knowing this is to assume and expect
     * that variable records for the variables listed are already specified.
     */
    protected void parse_value_labels() {
        PORValueLabels valuelabels = new PORValueLabels();
        
        int vartype = PORValue.TYPE_UNASSIGNED;

        // Parse the number of variables
        int count;
        
        // Variable names count
        count = parse_uint();
        
        valuelabels.vars.ensureCapacity(count);
        
        for (int i = 0; i < count; i++) {
            String varname = parse_string();
            // Resolve variable
            PORVariable cvar = por.getVariable(varname);
            if (cvar == null) {
                error_parse("Value labels list specify an unknown variable: \"%s\"", varname);
            }
            
            if (vartype == PORValue.TYPE_UNASSIGNED) {
                // Assign the type
                vartype = cvar.width == 0 ? 
                    PORValue.TYPE_NUMERIC : PORValue.TYPE_STRING;
            } else {
                // Verify the type
                if (((vartype == PORValue.TYPE_NUMERIC) 
                    && (cvar.width != 0)) 
                    || ((vartype == PORValue.TYPE_STRING) 
                    && (cvar.width == 0)))
                {
                    error_parse("Value labels list contain contradictory value types");
                }
            } // if-else
            
            // Append to the variables list
            valuelabels.vars.add(cvar);
        } // for: varnames list
        
        // Value labels count
        count = parse_uint();
        
        for (int i = 0; i < count; i++) {
            PORValue value;
            String label;
            
            value = parse_value(vartype);
            label = parse_string();
            valuelabels.mappings.put(value, label);
        } // for: value-label list
    } // parse_value_labels()
                
    protected void parse_document_records() {
        throw new RuntimeException(
            "Document records parsing is unimplemented");
    }
    
    
    // PORTABLE PRIMITIES
    //====================
    
    protected String parse_string() {
        int len = parse_uint();
        
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            int c = readc();
            // TODO: decode
            sb.append((char) c);
        }
        
        return sb.toString();
    } // parse_string()
    
    protected PORValue parse_value(PORVariable variable) {
        PORValue rval;
        
        if (variable.width == 0) {
            // Numeric
            rval = parse_numeric_value();
        } else {
            // String
            rval = parse_string_value();
            // TODO:
            // Validate the string length
        }
        return rval;
    } // parse_value();
    
    protected PORValue parse_value(int value_type) {
        PORValue rval;
        
        if (value_type == PORValue.TYPE_NUMERIC) {
            // Numeric
            rval = parse_numeric_value();
        } else if (value_type == PORValue.TYPE_STRING) {
            // String
            rval = parse_string_value();
            // TODO:
            // Validate the string length
        } else {
            throw new RuntimeException(String.format(
                "Internal error. Invalid value type: %d", value_type));
        }
        
        return rval;
    } // parse_value();
    

    protected PORValue parse_string_value() {
        return new PORValue(PORValue.TYPE_STRING, parse_string());
    }
    
    protected PORValue parse_numeric_value() {
        int c;
        StringBuilder sb = new StringBuilder(128);
        
        // Reset the NumberParser
        numparser.reset();
        
        // Eat up leading whitespaces
        
        c = readc();
        // TODO: decode
        while (c == ' ') {
            sb.append((char) c);
            
            c = readc();
            // TODO: decode
        } // while
        
        if (c == '*') {
            // This is a missing value.
            
            sb.append((char) c);
            
            c = readc(); // consume the succeeding dot.
            // TODO: decode
            
            sb.append((char) c);
        } else {
            // Emit to parser until a slash is found.
            while (c != '/') {
                numparser.consume(c);
                sb.append((char) c);
                c = readc();
                // TODO: decode
            } // while
            
            // Signal end-of-data
            int errno = numparser.consume(-1);

            // Inspect result
            if (errno != NumberParser.E_OK) {
                error_numfmt(numparser.strerror());
            } // if: error
            
        } // if-else
        
        return new PORValue(PORValue.TYPE_NUMERIC, sb.toString());
    } // parse_numeric_value()
    
    
    protected int parse_uint() {
        int c;
        
        // Reset the NumberParser
        numparser.reset();
        
        // Eat up leading whitespaces
        while ((c = readc()) == ' ');
        
        if (c == '*') {
            // This is a missing value.
            readc(); // consume the succeeding dot.
        }
        // Emit to parser until a slash is found.
        while (c != '/') {
            numparser.consume(c);
            c = readc();
        }
        
        // Signal end-of-data
        int errno = numparser.consume(-1);
        
        // Inspect result
        if (errno != NumberParser.E_OK) {
            error_numfmt(numparser.strerror());
        }
        
        if (numparser.lastsign() < 0) {
            error_numfmt("Expected non-negative number");
        }
        
        int rval = (int) numparser.lastvalue();
        
        if (((double) rval) != numparser.lastvalue()) {
            error_numfmt("Expected an integer");
        }
        
        return rval;
    } // parse_uint()
    

    // SUPPORT FUNCTIONS
    //===================

    
    protected void decode(byte[] array) {
        for (int i = 0; i < array.length; i++) {
            int inbyte = ((int) array[i]) & 0xff;
            int outchar = dectab[inbyte];
            
            if (outchar == -1) {
                /*
                throw new RuntimeException(String.format(
                    "No decoding for input byte \'%c\' (%d dec)",
                    inbyte, inbyte));
                */
                System.err.printf(
                    "No decoding for input byte \'%c\' (%d dec)",
                    inbyte, inbyte);
                
                // No decoding; use the byte as-is for the char.
                // TODO: Possibly apply ISO-8859-1 to UTF-8 decoding
                // or something similar.
                continue;
            }
            
            // Replace the element with decoded content
            array[i] = (byte) outchar;
        } // for
    } // decode
    
    
    protected void read(byte[] array, int from, int to) {
        int offset = 0;
        int c = -1;
        
        try {
            for (offset = from; offset < to; offset++) {
                c = read();
                
                if (c == -1) {
                    System.out.printf("eof\n");
                    // eof
                    break;
                }
                array[offset] = (byte) c;
            } // for
            System.out.printf("read %d bytes\n", to-from);
        } catch(IOException ex) {
            error_io(String.format(
                "BufferedInputStream.read(byte[], from=%d, len=%d)",
                from, to-from), ex);
        } // try-catch
        
        // If the read() didn't got as many bytes as required,
        // then the only explanation is that eof was reached.
        if (offset != to) {
            error_eof(String.format(
                "BufferedInputStream.read(byte[], from=%d, len=%d)",
                from, to-from));
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
    
    protected static void error_numfmt(String desc) {
        throw new RuntimeException(String.format(
            "Number format error: %s", desc));
    }
    
    protected static void error_syntax(String desc) {
        throw new RuntimeException(String.format(
            "Syntax error: %s", desc));
    }
    
    protected static void error_parse(String fmt, Object... args) {
        throw new RuntimeException(String.format(
            "Parse error: %s", String.format(fmt, args)));
    }
    
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
                    //System.out.printf("<eol>\n");
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
        //System.out.printf("%c", rval);
        
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








