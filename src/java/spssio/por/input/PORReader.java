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

// spssio portable
import spssio.por.PORFile;
import spssio.por.PORCharset;
import spssio.por.PORValue;
import spssio.por.PORVariable;
import spssio.por.PORMissingValue;
import spssio.por.PORValueLabels;
import spssio.por.PORHeader;
import spssio.por.PORMatrix;
import spssio.por.PORRawMatrix;
import spssio.por.PORConstants;
import spssio.por.PORSection;

// spssio common
import spssio.common.SPSSFormat;

// spssio utils
import spssio.util.NumberSystem;
import spssio.util.NumberParser;
import spssio.util.SequentialByteArray;

/*
 *
 */
public class PORReader
{
    
    // CONSTANTS
    //===========
    
    /**
     * Buffer size used for creating BufferedInputStream, unless specified.
     */
    public static final int DEFAULT_BUFFER_SIZE = 0x4000; // 16 KBs
    
    // MEMBER VARIABLES
    //==================
    
    /**
     * Configuration variable; controls buffer size parameter of
     * {@code BufferedInputStream}'s constructor. Initialized to
     * the default value, {@link #DEFAULT_BUFFER_SIZE}.
     */
    private int buffer_size;

    /** 
     * Configuration variable; determines the length of the rows.
     * If a row is longer than this, it is an error. If a row is shorter,
     * then it is internally widened into this length by spaces. 
     */
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
    

    /** 
     * Number of the current row. 
     */
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
        row_length = PORConstants.ROW_LENGTH;

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
     * Get latest input file row number.
     * @return Latest input row number.
     */
    public int getRow() {
        return row;
    } 
    
    /**
     * Get latest input file column number.
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
        parseSplashStrings();
        
        // Read the 256-byte characte set mapping
        parseCharset();
        
        // Reads the 8-byte signature
        parseFormatSignature();
        
        // reads the 1-byte format identifier
        parseFormatVersion();
        
        // Reads the 8-byte creation date, and the 6-byte creation time.
        parseCreationTimestamp();

        // Add a header section
        por.sections.add(PORSection.newHeader(por.header));
        
        // from this point on, read "tag" and switch until tag='F' is met.
        int tag;
        do {
            // Read tag code
            tag = readc();
            
            // Parse the incoming input according to the tag code
            switch(tag) {
                case PORSection.TAG_SOFTWARE:           // '1'
                    parseSoftware();
                    break;
                
                case PORSection.TAG_AUTHOR:             // '2'
                    parseAuthor();
                    break;
                
                case PORSection.TAG_TITLE:              // '3'
                    parseTitle();
                    break;
                
                case PORSection.TAG_VARIABLE_COUNT:     // '4';
                    parseVariableCount();
                    break;
                
                case PORSection.TAG_PRECISION:          // '5'
                    parseNumericPrecision();
                    break;
                
                case PORSection.TAG_WEIGHT_VARIABLE:    // '6'
                    parseWeightVariable();
                    break;
                    
                case PORSection.TAG_VARIABLE_RECORD:    // '7'
                    parseVariableRecord();
                    break;
                
                case PORSection.TAG_MISSING_DISCRETE:   // '8'
                    parseMissingDiscrete();
                    break;
            
                case PORSection.TAG_MISSING_OPEN_LO:    // '9'
                    parseMissingRangeOpenLo();
                    break;
                
                case PORSection.TAG_MISSING_OPEN_HI:    // 'A'
                    parseMissingRangeOpenHi();
                    break;
                
                case PORSection.TAG_MISSING_RANGE:      // 'B'
                    parseMissingRangeClosed();
                    break;
                
                case PORSection.TAG_VARIABLE_LABEL:     // 'C'
                    parseVariableLabel();
                    break;
                
                case PORSection.TAG_VALUE_LABELS:       // 'D'
                    parseValueLabelsRecord();
                    break;
                
                case PORSection.TAG_DOCUMENTS_RECORD:   // 'E'
                    parseDocumentsRecord();
                    break;
                
                case PORSection.TAG_DATA_MATRIX:        // 'F'
                    // Terminate the loop.
                    // The data itself is always at the end of the file.
                    break;
                
                default:
                    error_parse(String.format(
                        "Unexpected tag code \'%c\'", tag));
                    // never reached
                
            } // switch
        } while (tag != PORSection.TAG_DATA_MATRIX);
        
        // Parse the data matrix
        parseDataMatrixRecord();
    } // parse()
    
    
    // HEADER FIELDS
    //===============
    
    protected void parseSplashStrings() {
        // Allocate a byte array for the splash strings.
        byte[] array = new byte[5*40];
        
        // Populate the array
        read(array, 0, array.length);
        
        // Set the splash strings
        por.header.splash = array;
    } // parse_splash_strings()
    
    protected void parseCharset() {
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
    
    
    protected void parseFormatSignature() {
        
        // Allocate an array for the signature
        byte[] array = new byte[8];
        
        // Read the signature
        read(array, 0, array.length);
        
        // Decode the signature
        decode(array);
        
        // Convert into a string
        String signature = new String(array);
        
        // Finally, assert that the file is SPSS Portable file
        // (Format signature is 'SPSSPORT')
        if (signature.equals(PORConstants.FORMAT_SIGNATURE) == false) {
            throw new RuntimeException(String.format(
                "Unexpected file signature \"%s\"",  signature));
        }

        // Record the signature
        por.header.signature = signature;
    } // parse_signature()

    protected void parseFormatVersion() {
        int c = readc();
        
        // TODO: Decode?
        // TODO: Validate: expect 'A' ?
        
        por.header.version = (char) c;
    } // parse_file_version();

    protected void parseCreationTimestamp() {
        // Parse creation date
        por.header.date = parseString();
        
        // Parse creation time of day
        por.header.time = parseString();
    }

    // TAG RECORDS
    //=============

    /*
    protected void parseHeader() {
        // Read the 200-byte header
        parseSplashStrings();
        
        // Read the 256-byte characte set mapping
        parseCharset();
        
        // Reads the 8-byte signature
        parseFormatSignature();
        
        // reads the 1-byte format identifier
        parseFormatVersion();
        
        // Reads the 8-byte creation date, and the 6-byte creation time.
        parseCreationTimestamp();
        
        por.sections.add(PORSection.newHeader(
            por.heaer));
    }
    */
    
    protected void parseSoftware() {
        por.software = parseString();
        
        por.sections.add(PORSection.newSoftware(
            por.software));
    }
    
    protected void parseAuthor() {
        por.author = parseString();

        por.sections.add(PORSection.newAuthor(
            por.author));
    }
    
    protected void parseTitle() {
        por.title = parseString();

        por.sections.add(PORSection.newTitle(
            por.title));
    }
    
    protected void parseVariableCount() {
        por.variableCount = parseIntU();
        
        por.sections.add(PORSection.newVariableCount(
            por.variableCount));
    }
    
    protected void parseNumericPrecision() {
        por.precision = parseIntU();
        
        por.sections.add(PORSection.newPrecision(
            por.precision));
    }
    
    protected void parseWeightVariable() {
        por.weight_var_name = parseString();

        por.sections.add(PORSection.newWeightVarName(
            por.weight_var_name));
    }
    
    protected void parseVariableRecord() {
        // Create a new PORVariable object
        lastvar = new PORVariable();
        
        // Add it immediately to the PORFile object
        por.variables.add(lastvar);
        
        lastvar.width = parseIntU();
        // TODO: Validate value range: 0-255
        
        lastvar.name = parseString();
        // TODO: Validate name length; 1-8
        
        SPSSFormat fmt = null;
        
        fmt = new SPSSFormat();
        fmt.type = parseIntU();
        fmt.width = parseIntU();
        fmt.decimals = parseIntU();
        // TODO: Validate numeric values
        lastvar.printfmt = fmt;
        
        fmt = new SPSSFormat();
        fmt.type = parseIntU();
        fmt.width = parseIntU();
        fmt.decimals = parseIntU();
        // TODO: Validate numeric values
        lastvar.writefmt = fmt;

        por.sections.add(PORSection.newVariableRecord(
            lastvar));

    } // parse_variable_record()
                
    protected void parseMissingDiscrete() {
        if (lastvar == null) {
            error_syntax("Tag '7\' (variable record) should precede tag=\'8' (missing value)");
        }
        
        // Create missing value record
        PORMissingValue miss
            = new PORMissingValue(PORMissingValue.TYPE_DISCRETE_VALUE);
        
        // Append to variable record
        lastvar.missvalues.add(miss);
        
        // Parse the value
        miss.values[0] = parseValue(lastvar);
        
        por.sections.add(PORSection.newMissingValueRecord(miss));
    } // parse_missing_discrete()
    
            
    protected void parseMissingRangeOpenLo() {
        if (lastvar == null) {
            error_syntax("Tag '7\' (variable record) should precede tag=\'9' (missing open lo)");
        }
        
        // Create missing value record
        PORMissingValue miss
            = new PORMissingValue(PORMissingValue.TYPE_RANGE_OPEN_LO);
        
        // Append to variable record
        lastvar.missvalues.add(miss);
        
        // Parse the value
        miss.values[0] = parseValue(lastvar);
        
        por.sections.add(PORSection.newMissingValueRecord(miss));
    } // parse_missing_open_lo()
    
    protected void parseMissingRangeOpenHi() {
        if (lastvar == null) {
            error_syntax("Tag '7\' (variable record) should precede tag=\'A' (missing open hi)");
        }
        
        // Create missing value record
        PORMissingValue miss
            = new PORMissingValue(PORMissingValue.TYPE_RANGE_OPEN_HI);
        
        // Append to variable record
        lastvar.missvalues.add(miss);
        
        // Parse the value
        miss.values[0] = parseValue(lastvar);
        
        por.sections.add(PORSection.newMissingValueRecord(miss));
    } // parse_missing_open_hi()
    
    protected void parseMissingRangeClosed() {
        if (lastvar == null) {
            error_syntax("Tag '7\' (variable record) should precede tag=\'A' (missing range)");
        }
        
        // Create missing value record
        PORMissingValue miss
            = new PORMissingValue(PORMissingValue.TYPE_RANGE_CLOSED);
        
        // Append to variable record
        lastvar.missvalues.add(miss);
        
        // Parse the values
        miss.values[0] = parseValue(lastvar);
        miss.values[1] = parseValue(lastvar);

        por.sections.add(PORSection.newMissingValueRecord(miss));
    } // parse_missing_closed()
    
    protected void parseVariableLabel() {
        if (lastvar == null) {
            error_syntax("Tag '7\' (variable record) should precede tag=\'A' (variable label)");
        }
        
        lastvar.label = parseString();
        
        por.sections.add(PORSection.newVariableLabel(lastvar.label));
    } // parse_variable_label()
    
    /**
     * The ability to parse (value, label) pairs requires knowledge whether
     * the values are numerical or textual. This information is not contained
     * within the value labels record itself, so it cannot be deduced.
     * Therefore, the only way of knowing this is to assume and expect
     * that variable records for the variables listed are already specified.
     */
    protected void parseValueLabelsRecord() {
        PORValueLabels valuelabels = new PORValueLabels();
        
        int vartype = PORValue.TYPE_UNASSIGNED;

        // Parse the number of variables
        int count;
        
        // Variable names count
        count = parseIntU();
        
        valuelabels.vars.ensureCapacity(count);
        
        for (int i = 0; i < count; i++) {
            String varname = parseString();
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
        count = parseIntU();
        
        for (int i = 0; i < count; i++) {
            PORValue value;
            String label;
            
            value = parseValue(vartype);
            label = parseString();
            valuelabels.mappings.put(value, label);
        } // for: value-label list
        
        // Add to PORFile object
        por.labels.add(valuelabels);
        
        por.sections.add(PORSection.newValueLabelsRecord(valuelabels));
    } // parse_value_labels()
                
    protected void parseDocumentsRecord() {
        throw new RuntimeException(
            "Document records parsing is unimplemented");
    }

    protected void parseDataMatrixRecord() {
        //System.out.printf("File position: %d / %d\n", fpos, fsize);
        
        // Create the backend data container
        SequentialByteArray array = new SequentialByteArray();
        // Create a parser, and reuse the NumberParser of this object.
        PORMatrixParser matrixParser = new PORMatrixParser(numparser);
        
        // Put these into a newly-created matrix
        por.data = new PORRawMatrix(array, matrixParser);

        // Calculate the number of bytes left to read.
        int size = (int) (fsize-fpos);
        
        // Allocate the calculated amount of memory.
        array.allocate(size);
        
        // Setup the column data types
        int[] coltype = new int[por.variables.size()];
        for (int i = 0; i < coltype.length; i++) {
            coltype[i] = por.variables.elementAt(i).width == 0 ?
                PORValue.TYPE_NUMERIC : PORValue.TYPE_STRING;
        } // for
        
        matrixParser.setDataColumnTypes(coltype);
        matrixParser.setTextColumn0(col);
        
        try {
            int c;
            
            // Read next char while not eof
            while ((c = istream.read()) != -1) {

                // Decode the input byte
                c = dectab[c];
                
                // Increase binary position
                fpos++;
                
                // Maintain the text row/col counter of the PORReader.
                col++;
                if (c == '\n') {
                    row++;
                    col = 0;
                } else if (c == '\r') {
                    // Cancel the column increment
                    col--;
                }
                
                // Write to the array
                array.write(c);
                
                // Send to the parser
                matrixParser.consume(c);
                
                // If an error occurred in parsing, throw an exception
                if (matrixParser.errno() == PORMatrixParser.E_REJECTED) {
                    error_cell(matrixParser.getX(), matrixParser.getY(),
                        matrixParser.strerror());
                }
            } // while
        } catch(IOException ex) {
            error_io("BufferedInputStream.read()", ex);
        } // try-catch
        
        por.sections.add(PORSection.newDataMatrix(por.data));
    } // parse_data_matrix()


    
    //=======================================================================
    // PRIMITIVES
    //=======================================================================
    
    protected String parseString() {
        int len = parseIntU();
        
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            int c = readc();
            // TODO: decode
            sb.append((char) c);
        }
        
        return sb.toString();
    } // parseString()
    
    protected PORValue parseValue(PORVariable variable) {
        PORValue rval;
        
        if (variable.width == 0) {
            // Numeric
            rval = parseValueNumeric();
        } else {
            // String
            rval = parseValueString();
            // TODO:
            // Validate the string length
        }
        return rval;
    } // parse_value();
    
    protected PORValue parseValue(int value_type) {
        PORValue rval;
        
        if (value_type == PORValue.TYPE_NUMERIC) {
            // Numeric
            rval = parseValueNumeric();
        } else if (value_type == PORValue.TYPE_STRING) {
            // String
            rval = parseValueString();
            // TODO:
            // Validate the string length
        } else {
            throw new RuntimeException(String.format(
                "Internal error. Invalid value type: %d", value_type));
        }
        
        return rval;
    } // parse_value();
    

    protected PORValue parseValueString() {
        return new PORValue(PORValue.TYPE_STRING, parseString());
    }
    
    protected PORValue parseValueNumeric() {
        int c;
        StringBuilder sb = new StringBuilder(128);
        
        // Reset the NumberParser
        numparser.reset();
        
        // Eat up leading whitespaces
        
        c = readc();
        // TODO: decode
        while (c == PORConstants.WHITESPACE) { // ' '
            sb.append((char) c);
            
            c = readc();
            // TODO: decode
        } // while
        
        if (c == PORConstants.SYSMISS_MARKER) { // '*'
            // This is a missing value.
            
            sb.append((char) c);
            
            c = readc(); // consume the succeeding dot.
            // TODO: decode
            
            sb.append((char) c);
        } else {
            // Emit to parser until a slash is found.
            while (c != PORConstants.NUMBER_SEPARATOR) { // '/'
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
    
    protected int parseIntU() {
        int c;
        
        // Reset the NumberParser
        numparser.reset();
        
        // Eat up leading whitespaces
        while ((c = readc()) == PORConstants.WHITESPACE); 
        
        if (c == PORConstants.SYSMISS_MARKER) { // '*'
            // This is a missing value.
            readc(); // consume the succeeding dot.
        }
        // Emit to parser until a slash is found.
        while (c != PORConstants.NUMBER_SEPARATOR) { // '/'
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
    } // parseIntU()
    

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
                "row is too long (more than %d chars)", row, row_length));
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
    
} // class PORReader
