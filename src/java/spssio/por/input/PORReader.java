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
import java.util.Vector;
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
    extends PORBaseReader
{

    // CONSTANTS
    //===========

    // MEMBER VARIABLES
    //==================

    /**
     * File size
     */
    private long fileSize;

    // TRANSIENT/AUXILIARY
    //=====================

    /**
     * The PORFile object that is being built
     */
    private PORFile por;

    /**
     * PORVariable currently under construction.
     */
    private PORVariable lastVariable;

    // CONSTRUCTORS
    //==============

    /**
     * Creates an initialized {@code PORReader} object.
     */
    public PORReader() {
        fileSize = 0;
        por = null;
        lastVariable = null;
    } // ctor

    // OTHER METHODS
    //===============

    /**
     * Parses a Portable file.
     *
     * @param filename The name of the file to parse.
     * @return The {@code PORFile} representing the parsed Portable file.
     */
    public PORFile parse(String filename)
        throws FileNotFoundException, IOException, SecurityException
    {
        File f = new File(filename);

        // Clear auxiliary variables
        lastVariable = null;

        // For recording the return value
        PORFile rval = null;

        // May throw FileNotFound
        InputStream is = new FileInputStream(filename);

        try {

            // Get file length. May throw SecurityException
            fileSize = f.length();

            // Wrap the stream into a BufferedInputStream
            // and bind the underlying PORBaseReader to it.
            is = bind(is, true);

            // Parse the Portable file. May throw.
            parse();

            // Record the return value
            rval = por;

        } finally {
            // Unbinding won't throw.
            unbind();

            // May throw IOException, close quietly.
            try {
                is.close();
            } catch(IOException ignored) {
                // Ignored
            } // try-catch
        } // try-finally

        return rval;
    } // parse()


    /**
     * Parse a Portable file using an {@code InputStream} as source.
     *
     * @param is The input from which the file is parsed.
     * @return The {@code PORFile} reprsenting the parsed Portable file.
     *
     * TODO:
     * This won't work due to the fact that file size is left unknown!
     * Don't forget to fix this!!!
     *
     */
    public PORFile parse(InputStream is) {

        // Clear auxiliary variables
        lastVariable = null;

        // Bind the underlying PORBaseReader to the stream.
        bind(is, false);

        try {
            // May throw
            parse();
        } finally {
            // Release the stream
            unbind();
        } // try-finally


        return por;
    } // parse()

    // RECURSIVE DESCENT PARSER FUNCTIONS
    //====================================

    protected void parse() {

        // Create a new PORFile that will be built from the input stream.
        por = new PORFile();

        // Start by clearing any previous translation
        clearTranslation();

        // Read the 200-byte header
        parseSplash();

        // Read the 256-byte characte set mapping
        parseCharset();

        // Set the new translation in effect.
        setTranslation(por.header.translation);

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
            tag = readChar();

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

    protected void parseSplash() {
        // For convenience
        byte[] splash = por.header.splash;

        // Populate the array
        readBytes(splash, 0, PORConstants.SPLASH_LENGTH);

    } // parseSplash()

    protected void parseCharset() {
        // For convenience
        byte[] translation = por.header.translation;

        // Populate the array
        readBytes(translation, 0, PORConstants.TRANSLATION_LENGTH);

        // TODO: Should the translation be set in effect here?
    } // parseCharset()


    protected void parseFormatSignature() {

        String signature
            = readBytesAsString(PORConstants.SIGNATURE_LENGTH);

        // Do not validate at all?

        /*
        // TODO:
        // Enable the reader to be configured so that the signature
        // is not validated.

        // Finally, assert that the file is SPSS Portable file
        // (Format signature is 'SPSSPORT')
        if (signature.equals(PORConstants.FORMAT_SIGNATURE) == false) {
            throw new RuntimeException(String.format(
                "Unexpected file signature \"%s\"",  signature));
        }
        */

        // Record the signature
        por.header.signature = signature;
    } // parse_signature()

    protected void parseFormatVersion() {
        int c = readChar();

        por.header.version = (char) c;
    } // parse_file_version();

    protected void parseCreationTimestamp() {
        // Parse creation date
        por.header.date = readString();

        // Parse creation time of day
        por.header.time = readString();
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
        por.software = readString();

        por.sections.add(PORSection.newSoftware(
            por.software));
    }

    protected void parseAuthor() {
        por.author = readString();

        por.sections.add(PORSection.newAuthor(
            por.author));
    }

    protected void parseTitle() {
        por.title = readString();

        por.sections.add(PORSection.newTitle(
            por.title));
    }

    protected void parseVariableCount() {
        por.variableCount = readIntU();

        por.sections.add(PORSection.newVariableCount(
            por.variableCount));
    }

    protected void parseNumericPrecision() {
        por.precision = readIntU();

        por.sections.add(PORSection.newPrecision(
            por.precision));
    }

    protected void parseWeightVariable() {
        por.weightVariableName = readString();

        por.sections.add(PORSection.newWeightVariable(
            por.weightVariableName));
    }

    protected void parseVariableRecord() {
        // Create a new PORVariable object
        lastVariable = new PORVariable();

        // Add it immediately to the PORFile object
        por.variables.add(lastVariable);

        lastVariable.width = readIntU();
        // TODO: Validate value range: 0-255

        lastVariable.name = readString();
        // TODO: Validate name length; 1-8

        SPSSFormat fmt = null;

        fmt = new SPSSFormat();
        fmt.type = readIntU();
        fmt.width = readIntU();
        fmt.decimals = readIntU();
        // TODO: Validate numeric values
        lastVariable.printfmt = fmt;

        fmt = new SPSSFormat();
        fmt.type = readIntU();
        fmt.width = readIntU();
        fmt.decimals = readIntU();
        // TODO: Validate numeric values
        lastVariable.writefmt = fmt;

        por.sections.add(PORSection.newVariableRecord(
            lastVariable));

    } // parse_variable_record()

    protected void parseMissingDiscrete() {
        if (lastVariable == null) {
            error_syntax("Tag '7\' (variable record) should precede tag=\'8' (missing value)");
        }

        // Create missing value record
        PORMissingValue miss
            = new PORMissingValue(PORMissingValue.TYPE_DISCRETE_VALUE);

        // Append to variable record
        lastVariable.missvalues.add(miss);

        // Parse the value
        miss.values[0] = parseValue(lastVariable);

        por.sections.add(PORSection.newMissingValueRecord(miss));
    } // parse_missing_discrete()


    protected void parseMissingRangeOpenLo() {
        if (lastVariable == null) {
            error_syntax("Tag '7\' (variable record) should precede tag=\'9' (missing open lo)");
        }

        // Create missing value record
        PORMissingValue miss
            = new PORMissingValue(PORMissingValue.TYPE_RANGE_OPEN_LO);

        // Append to variable record
        lastVariable.missvalues.add(miss);

        // Parse the value
        miss.values[0] = parseValue(lastVariable);

        por.sections.add(PORSection.newMissingValueRecord(miss));
    } // parse_missing_open_lo()

    protected void parseMissingRangeOpenHi() {
        if (lastVariable == null) {
            error_syntax("Tag '7\' (variable record) should precede tag=\'A' (missing open hi)");
        }

        // Create missing value record
        PORMissingValue miss
            = new PORMissingValue(PORMissingValue.TYPE_RANGE_OPEN_HI);

        // Append to variable record
        lastVariable.missvalues.add(miss);

        // Parse the value
        miss.values[0] = parseValue(lastVariable);

        por.sections.add(PORSection.newMissingValueRecord(miss));
    } // parse_missing_open_hi()

    protected void parseMissingRangeClosed() {
        if (lastVariable == null) {
            error_syntax("Tag '7\' (variable record) should precede tag=\'A' (missing range)");
        }

        // Create missing value record
        PORMissingValue miss
            = new PORMissingValue(PORMissingValue.TYPE_RANGE_CLOSED);

        // Append to variable record
        lastVariable.missvalues.add(miss);

        // Parse the values
        miss.values[0] = parseValue(lastVariable);
        miss.values[1] = parseValue(lastVariable);

        por.sections.add(PORSection.newMissingValueRecord(miss));
    } // parse_missing_closed()

    protected void parseVariableLabel() {
        if (lastVariable == null) {
            error_syntax("Tag '7\' (variable record) should precede tag=\'A' (variable label)");
        }

        lastVariable.label = readString();

        por.sections.add(PORSection.newVariableLabel(lastVariable.label));
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
        count = readIntU();

        valuelabels.vars.ensureCapacity(count);

        for (int i = 0; i < count; i++) {
            String varname = readString();
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
        count = readIntU();

        for (int i = 0; i < count; i++) {
            PORValue value;
            String label;

            value = parseValue(vartype);
            label = readString();
            valuelabels.mappings.put(value, label);
        } // for: value-label list

        // Add to PORFile object
        por.labels.add(valuelabels);

        por.sections.add(PORSection.newValueLabelsRecord(valuelabels));
    } // parse_value_labels()

    protected void parseDocumentsRecord() {
        //http://www.gnu.org/software/pspp/pspp-dev/html_node/Portable-File-Document-Record.html#Portable-File-Document-Record

        int lines = readIntU();

        Vector<String> documents = new Vector<String>(lines);

        for (int lineNumber = 0; lineNumber < lines; lineNumber++) {
            String line = readString();
            documents.add(line);
        }

        // TODO: Can there be more than one documents section?
        // If so, it would be safer to inspect whether the variable
        // is already set and perhaps .add() the new vector to it
        por.documents = documents;

        por.sections.add(PORSection.newDocumentsRecord(documents));
    }

    protected void parseDataMatrixRecord() {
        //System.out.printf("File position: %d / %d\n", fpos, fsize);

        // Create the backend data container
        SequentialByteArray array = new SequentialByteArray();
        // Create a parser, and reuse the NumberParser of this object.
        PORMatrixParser matrixParser = new PORMatrixParser(numberParser);

        // Put these into a newly-created matrix
        por.data = new PORRawMatrix(array, matrixParser);

        // Calculate the number of bytes left to read.
        int size = (int) (fileSize-getOffset());

        // Allocate the calculated amount of memory.
        array.allocate(size);

        // Setup the column data types
        int[] coltype = new int[por.variables.size()];
        for (int i = 0; i < coltype.length; i++) {
            coltype[i] = por.variables.elementAt(i).width == 0 ?
                PORValue.TYPE_NUMERIC : PORValue.TYPE_STRING;
        } // for

        matrixParser.setDataColumnTypes(coltype);
        matrixParser.setTextColumn0(getColumn());

        try {
            int c;

            // Read next char while not eof
            while ((c = read()) != -1) {

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

        array.flush();
        array.limitSize(array.pos());

        por.sections.add(PORSection.newDataMatrix(por.data));
    } // parse_data_matrix()


    //=======================================================================
    // PRIMITIVES
    //=======================================================================

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
        return new PORValue(PORValue.TYPE_STRING, readString());
    }

    protected PORValue parseValueNumeric() {
        int c;
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

            c = readChar(); // consume the succeeding dot.

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
                error_numfmt(numberParser.strerror());
            } // if: error

        } // if-else

        return new PORValue(PORValue.TYPE_NUMERIC, sb.toString());
    } // parse_numeric_value()

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
