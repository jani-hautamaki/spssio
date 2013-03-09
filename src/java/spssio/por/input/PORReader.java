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
import java.io.File; // for size
//  for iterating value labels (TODO: remove after initial dev)
import java.util.Map;


// spssio portable
import spssio.por.PORFile;
import spssio.por.PORCharset;
import spssio.por.PORValue;
import spssio.por.PORVariable;
import spssio.por.PORMissingValue;
import spssio.por.PORValueLabels;
import spssio.por.PORHeader;
import spssio.por.PORDataMatrix;

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
    public static final int DEFAULT_BUFFER_SIZE = 0x4000; // 16 KBs
    
    // MEMBER VARIABLES
    //==================
    
    /**
     * Configuration variable; the size of the buffer to create.
     * Initialized to the default value.
     */
    private int buffer_size;

    /** 
     * Configuration variable; determines the length of the rows.
     * If a row is longer than this, it is an error. If a row is shorter,
     * then it is internally widened into this length by spaces. */
    private int row_length;


    /** 
     * The input stream
     */
    private BufferedInputStream istream;

    /**
     * File size
     */
    private long fsize;
    
    /**
     * Bytes read
     */
    private long fpos;
    

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
    
    /**
     * Creates an initialized {@code PORReader} object.
     */
    public PORReader() {
        buffer_size = DEFAULT_BUFFER_SIZE;
        row_length = SPSS_PORTABLE_ROW_LENGTH;

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
    
    // ERROR MANAGEMENT
    //==================
    
    /**
     * Get latest input row number.
     * @return Latest input row number.
     */
    public int getRow() {
        return row;
    } 
    
    /**
     * Get latest input column number.
     * @return Latest input column number.
     */
    public int getColumn() {
        return col;
    } // getColumn()
    
    
    // OTHER METHODS
    //===============
    
    /**
     * Parses a Portable file.
     *
     * @param fname The name of the file to parse.
     * @return The {@code PORFile} representing the parsed Portable file.
     */
    public PORFile parse(String fname) 
        throws FileNotFoundException, IOException, SecurityException
    {
        // Create a fname object
        File f = new File(fname);
        
        // May throw FileNotFound
        FileInputStream fis = new FileInputStream(fname);
        
        // Reset file position
        fpos = 0;
        
        // Get file length. May throw SecurityException 
        fsize = f.length();
        
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
    
    /**
     * Parse a Portable file using an {@code InputStream} as source.
     *
     * @param is The input from which the file is parsed.
     * @return The {@code PORFile} reprsenting the parsed Portable file.
     */
    public PORFile parse(InputStream is) {
        
        // Initialize
        istream = new BufferedInputStream(is, buffer_size);
        
        // Reset location
        col = 0;
        row = 1;
        lastc = -1;
        
        // Clear auxiliary variables
        lastvar = null;
        
        // May throw
        parse();
        
        return por;
    } // parse()
    
    // RECURSIVE DESCENT PARSER FUNCTIONS
    //====================================
    
    protected void parse() {
        
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
            // Read tag code
            tag = readc();
            
            // Parse the incoming input according to the tag code
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
                    // Terminate the loop.
                    // The data itself is always at the end of the file.
                    break;
                
                default:
                    error_parse(String.format(
                        "Unexpected tag code \'%c\'", tag));
                    // never reached
                
            } // switch
        } while (tag != 'F');
        
        // Parse the data matrix
        parse_data_matrix();
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
        
        // TODO: Decode?
        // TODO: Validate: expect 'A' ?
        
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
        
        // Add to PORFile object
        por.labels.add(valuelabels);
    } // parse_value_labels()
                
    protected void parse_document_records() {
        throw new RuntimeException(
            "Document records parsing is unimplemented");
    }

    protected void parse_data_matrix() {
        //System.out.printf("File position: %d / %d\n", fpos, fsize);
        
        // Pick to a local variable for convenience
        PORDataMatrix dm = por.data;
        
        // Calculate the number of bytes left to read.
        int size = (int) (fsize-fpos);
        
        // Allocate the calculated amount of memory.
        dm.allocate(size);
        
        // Setup the column data types
        int[] coltype = new int[por.variables.size()];
        for (int i = 0; i < coltype.length; i++) {
            coltype[i] = por.variables.elementAt(i).width == 0 ?
                PORValue.TYPE_NUMERIC : PORValue.TYPE_STRING;
        } // for
        
        dm.setDataColumnTypes(coltype);
        dm.setTextColumn0(col);
        
        try {
            int c;
            
            // Read the next character
            while ((c = istream.read()) != -1) {

                // decode the input byte
                c = dectab[c];
                
                // Increase binary position
                fpos++;
                // Increase column number
                col++;
                
                // Track column and row
                if (c == '\n') {
                    row++;
                    col = 0;
                } else if (c == '\r') {
                    // Cancel the column increment
                    col--;
                }
                
                // Write
                dm.write(c);
                
                // Send to parser
                dm.consume(c);
                
                if (dm.errno() == PORDataMatrix.E_REJECTED) {
                    error_cell(dm.getx(), dm.gety(), dm.strerror());
                }
            } // while
            
            // TODO:
            // send end-of-file marker to dm
            dm.debug();
            
        } catch(IOException ex) {
            error_io("BufferedInputStream.read()", ex);
        } // try-catch
    } // parse_data_matrix()


    
    //=======================================================================
    // PRIMITIVES
    //=======================================================================
    
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

    /**
     * Decodes an array of bytes
     */
    protected void decode(byte[] array) {
        for (int i = 0; i < array.length; i++) {
            int inbyte = ((int) array[i]) & 0xff;
            int outchar = dectab[inbyte];
            
            // Replace the element with decoded content
            array[i] = (byte) outchar;
        } // for
    } // decode
    
    /**
     * Read an array of bytes. Throws an exception, if eof is reached.
     */
    protected void read(byte[] array, int from, int to) {
        int offset = 0;
        int c = -1;
        
        try {
            for (offset = from; offset < to; offset++) {
                c = read();
                
                if (c == -1) {
                    // eof
                    break;
                }
                array[offset] = (byte) c;
            } // for
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
    
    /**
     * Read a single byte. Throws an exception, if eof is reached.
     */
    protected int readc() {
        int rval = -1;
        try {
            // May throw
            rval = read();
        } catch(IOException ex) {
            error_io("BufferedInputStream.readc()", ex);
        } // try-catch
        
        // End-of-file is not allowed. If eof detected, throw an exception
        if (rval == -1) {
            error_eof("BufferedInputStream.readc()");
        } // if: eof
        
        return rval;
    } //  readc()

    /**
     * Read a single byte and keep count of the current row and column.
     * This method also appends the lines to row_length if newline is met
     * earlier than expected. If eof is reached, returns -1.
     */
    protected int read() 
        throws IOException
    {
        int rval = -1;
        
        do {
            if (lastc == '\n') {
                if (col < row_length) {
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
            fpos++;
            
            if (lastc == -1) {
                fpos--;
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
        if (col > row_length) {
            throw new RuntimeException(String.format(
                "row %d is too long (more than %d chars)", row, row_length));
        }
        //System.out.printf("%c", rval);
        
        // TODO:
        // Apply decoding?
        
        return rval;
    } // read()
    
    
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
    
    protected static void error_cell(
        int x, 
        int y, 
        String fmt, 
        Object... args
    ) {
        throw new RuntimeException(String.format(
            "Cell (x=%d,y=%d): %s", x, y, String.format(fmt, args)));
    }
    
    
    public static void printPortable(PORFile por) {
        PORHeader header = por.header;
        System.out.printf("Portable file contents\n");
        System.out.printf("  Header:\n");
        System.out.printf("     Signature:          \"%s\"\n", header.signature);
        System.out.printf("     Version:            \'%c\'\n", header.version);
        System.out.printf("     Creation date:      \"%s\"\n", header.date);
        System.out.printf("     Creation time:      \"%s\"\n", header.time);
        
        if (header.software != null) {
        System.out.printf("     Software:           \"%s\"\n", header.software);
        } else {
        System.out.printf("     Software:           <unset>\n");
        } // if-else
        
        if (header.author != null) {
        System.out.printf("     Author:             \"%s\"\n", header.author);
        } else {
        System.out.printf("     Author:             <unset>\n");
        } // if-else

        if (header.title != null) {
        System.out.printf("     Title:              \"%s\"\n", header.title);
        } else {
        System.out.printf("     Title:              <unset>\n");
        } // if-else

        System.out.printf("     # of variables:     %d\n", header.nvariables);
        
        System.out.printf("     Precision:          %d base-30 digits\n", header.precision);
        
        if (header.weight_var_name != null) {
        System.out.printf("     Weight variable:    \"%s\"\n", header.weight_var_name);
        } else {
        System.out.printf("     Weight variable:    <unset>\n");
        } // if-else
        
        System.out.printf("     Data matrix:        %d x %d\n", por.data.sizey(), por.data.sizex());
        System.out.printf("     Data size:          %d bytes\n", por.data.size());
        int[] types = por.data.getDataColumnTypes();
        int numeric_columns = 0;
        int string_columns = 0;
        for (int i = 0; i < types.length; i++) {
            if (types[i] == PORValue.TYPE_STRING) {
                string_columns++;
            } else if (types[i] == PORValue.TYPE_NUMERIC) {
                numeric_columns++;
            } else {
                // error
            }
        } // for
        System.out.printf("     Numeric variables:  %d\n", numeric_columns);
        System.out.printf("     String variables:   %d\n", string_columns);
        
        int size = por.variables.size();
        System.out.printf("  Variables (%d):\n", size);
        
        for (int i = 0; i < size; i++) {
        PORVariable cvar = por.variables.elementAt(i);
        System.out.printf("     Name:               \"%s\"\n", cvar.name);
        System.out.printf("     Width:              %d\n", cvar.width);
            
        if (cvar.label != null) {
        System.out.printf("     Label:              \"%s\"\n", cvar.label);
        } else {
        System.out.printf("     Label:              <unsert>\n");
        } // if-else
        
        System.out.printf("     Print fmt:          %d / %d / %d\n",
            cvar.printfmt.type, cvar.printfmt.width, cvar.printfmt.decimals);
        System.out.printf("     Wrint fmt:          %d / %d / %d\n",
            cvar.writefmt.type, cvar.writefmt.width, cvar.writefmt.decimals);
        
        System.out.printf("     Missing values (%d)\n", cvar.missvalues.size());
        for (int j = 0; j < cvar.missvalues.size(); j++) {
            PORMissingValue miss = cvar.missvalues.elementAt(j);
            String s = null;
            switch(miss.type) {
                case PORMissingValue.TYPE_DISCRETE_VALUE:
                    s = String.format(
                        "discrete: %s", 
                        miss.values[0].value);
                    break;
                case PORMissingValue.TYPE_RANGE_OPEN_LO:
                    s = String.format(
                        "range:    --%s",
                        miss.values[0].value);
                    break;
                case PORMissingValue.TYPE_RANGE_OPEN_HI:
                    s = String.format(
                        "range:    %s--",
                        miss.values[0].value);
                    break;
                case PORMissingValue.TYPE_RANGE_CLOSED:
                    s = String.format(
                        "range:    %s--%s",
                        miss.values[0].value,
                        miss.values[1].value);
                    break;
                
                default:
                    s = "????? error";
                    break;
                
            } // switch
            System.out.printf("         %s\n", s);
        } // for: missing values
        
        } // for: variables
        
        size = por.labels.size();
        
        System.out.printf("  Value labels lists (%d):\n", size);
        for (int i = 0; i < size; i++) {
            PORValueLabels labels = por.labels.elementAt(i);
            
        System.out.printf("     Variables:   %d\n", labels.vars.size());
        for (int j = 0; j < labels.vars.size(); j++) {
        System.out.printf("         \"%s\"\n", labels.vars.elementAt(j).name);
        } // for: variables
        System.out.printf("     Pairs:       %d\n", labels.mappings.size());
        for (Map.Entry<PORValue, String> entry : labels.mappings.entrySet()) {
        System.out.printf("          \"%s\" : \"%s\"\n",
            entry.getKey().value, entry.getValue());
        } // for: each mapping
        
        } // for: value labels lists
    }
    
    
    
    public static void main(String[] args) {
        String curFilename = null; 
        PORReader preader = new PORReader();
        try {
            
            for (int i = 0; i < args.length; i++) {
                curFilename = args[i];
                System.out.printf("%s\n", curFilename);
                PORFile por = null;
                por = preader.parse(args[i]);
                
                printPortable(por);
            }
        } catch(Exception ex) {
            System.out.printf("%s:%d:%d: %s", 
                curFilename, preader.getRow(), preader.getColumn(),
                ex.getMessage());
            ex.printStackTrace();
        } // try-catch
    } // main()
    
} // class PORReader








