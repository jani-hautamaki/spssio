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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Vector;
import java.util.List;
import java.util.LinkedList;
import java.util.LinkedHashMap;
import java.util.Map;

// spssio
import spssio.sav.SAVFile;
import spssio.sav.SAVHeader;
import spssio.sav.SAVValueLabels;
import spssio.sav.SAVVariable;
import spssio.sav.SAVValueFormat;
import spssio.sav.SAVMatrix;
import spssio.sav.SAVRawMatrix;
import spssio.sav.SAVSection;
import spssio.sav.SAVValue;
import spssio.sav.SAVExtensionRecord;
import spssio.sav.SAVExtNumberConfig;
import spssio.sav.SAVExtSystemConfig;
import spssio.util.DynamicByteArray;
import spssio.util.ByteCursor;
import spssio.util.DataEndianness;


/**
 * SYSMIS is -DBL_MAX in PSPP, see
 * http://www.gnu.org/software/pspp/pspp-dev/html_node/Numeric-Values.html
 */
public class SAVReader
    extends SAVBaseReader
{

    // MEMBER VARIABLES
    //==================

    /**
     * The object that is being built
     */
    private SAVFile sav;

    /**
     * Latest SAVValueLabels object parsed
     */
    private SAVValueLabels lastVLMap;

    /**
     * Latest extension record.
     * This is used back and forth between extension record discriminator
     * and the actual parsing method.
     */
    private SAVExtensionRecord lastExt;

    // CONSTRUCTORS
    //==============

    public SAVReader() {
        sav = null;
        lastVLMap = null;
        lastExt = null;
    }

    // OTHER METHODS
    //===============

    public SAVFile getLatestSAVFile() {
        return sav;
    }

    // TODO:
    // Should use try-finally-catch
    public SAVFile parse(String filename)
        throws FileNotFoundException, IOException, SecurityException
    {
        // Create a fname object
        File f = new File(filename);

        // May throw FileNotFound
        FileInputStream fis = new FileInputStream(filename);

        bind(fis, 0, (int) f.length());


        parseSAVFile();

        // May throw IOException, close quietly.
        try {
            fis.close();
        } catch(IOException ex) {
            System.err.printf("%s: FileInputStream.close() failed. Ignoring.\n", filename);
        } // try-catch

        return sav;
    }


    private void parseSAVFile() {

        sav = new SAVFile();
        lastVLMap = null;
        lastExt = null;

        parseSAVHeader();

        boolean quit = false;

        do {
            int tag = readInt();
            //System.out.printf("Bytes left: %x hex\n", length()-tell());
            //System.out.printf("tag: %x\n", tag);

            switch(tag) {
                case SAVSection.TAG_VARIABLE:
                    parseSAVVariable();
                    break;

                case SAVSection.TAG_VALUE_LABELS:
                    parseSAVValueLabels();
                    break;

                case SAVSection.TAG_VARIABLE_LIST:
                    parseSAVVariableList();
                    break;

                case SAVSection.TAG_DOCUMENTS:
                    parseSAVDocuments();
                    break;

                case SAVSection.TAG_EXTENSION_RECORD:
                    parseSAVExtensionRecord();
                    break;

                case SAVSection.TAG_DATA_MATRIX:
                    // Data record begins
                    quit = true;
                    break;

                default:
                    // Unrecoverable error.
                    throw new RuntimeException(String.format(
                        "%d: unexpected tag code %x",
                        tell(), tag));
                    // Error; unexpected tag code
            }
        } while (quit == false);

        int filler;

        filler = readInt();

        // TODO: Record filler somewhere!
        //System.out.printf("filler1: %d\n", filler);

        parseSAVDataMatrix2();

        // parse data record
    }

    private void addSection(int tag, Object obj) {
        // Create the new section (includes validation of the object type)
        SAVSection section = new SAVSection(tag, obj);

        // Append it
        sav.sections.add(section);
    }

    // OVERRIDES
    //===========

    public void readAlignedStringPadding(
        String string,
        int paddingLength
    ) {
        // Use a constant byte array into which the padding is read

        skip(paddingLength);
    }



    private SAVHeader parseSAVHeader() {
        // Create a new SAVHeader
        sav.header = new SAVHeader();

        // For convenience
        SAVHeader header = sav.header;

        // Parse format signature
        String signature = readPaddedString(4);

        /*
        // Validate signature
        if (signature.equals(SAVConstants.FORMAT_SIGNATURE) == false) {
            throw new RuntimeException(String.format(
                "Unexpected file signature: \"%s\"", signature));
        }
        */

        // Once validated, assign
        header.signature = signature;

        // Parse software
        header.software = readPaddedString(60);

        // Parse layout code
        header.layout = readInt();

        // Examine layout to determine the endianness
        if ((header.layout == 2) || (header.layout == 1) || (header.layout == 3)) {
            // ok
        } else {
            // Swap
            int layout = header.layout;
            // Assume Big-Endian, flip layout
            setEndianness(DataEndianness.BIG_ENDIAN);
            layout = 0
                | (layout & 0xff) << 24
                | (layout & 0xff00) << 8
                | (layout & 0xff0000) >> 8
                | (layout & 0xff000000) >> 24;
            header.layout = layout;
        }


        // Parse variable count
        header.variableCount = readInt();

        // Parse compression flag
        header.compression = readInt();

        // Parse index of the weight variable, if any
        header.weightVariableIndex = readInt();

        // Parse the number of cases (number of rows in the data matrix)
        header.numberOfCases = readInt();

        // Parse the compression bias
        header.bias = readDouble();

        // Parse the creation date
        header.date = readPaddedString(9);

        // Parse the creation time
        header.time = readPaddedString(8);

        // Parse the title of the file
        header.title = readPaddedString(64);

        // Header padding
        //header.padding = readString(3);
        readBytes(header.padding, 0, 3);


        // Section for the header
        addSection(SAVSection.TAG_HEADER, header);

        return header;
    }

    private SAVVariable parseSAVVariable() {
        int rawFormat;

        SAVVariable v = new SAVVariable();

        // Parse variable width
        v.width = readInt();

        // Parse label flag
        v.hasLabel = readInt();

        // Parse number of missing values
        v.numberOfMissingValues = readInt();

        // Parse input format specification
        rawFormat = readInt();
        v.inputFormat = new SAVValueFormat(rawFormat);

        // Parse output format specification
        rawFormat = readInt();
        v.outputFormat = new SAVValueFormat(rawFormat);

        // Parse variable name
        v.name = readPaddedString(8);

        if (v.hasLabel != 0) {

            // This field is present only if has_var_label is set to 1.
            // It has length label_len, rounded up to the nearest multiple
            // of 32 bits. The first label_len characters are
            // the variable's variable label.

            v.label = readAlignedString(4, 4, 0);
        }

        // For convenience and brevity
        int nvalues = v.numberOfMissingValues;

        // Q: What if the variable is a STRING variable?
        // A: String variables cannot have missing values or SYSMISS.

        // If the variable has a range for missing variables, set to -2;
        // if the variable has a range for missing variables
        // plus a single discrete value, set to -3.

        if (nvalues < 0) {
            // Has a range
            nvalues = -nvalues;
        }

        v.missingValues = null;
        if (nvalues > 0) {
            v.missingValues = new double[nvalues];

            for (int i = 0; i < nvalues; i++) {
                v.missingValues[i] = readDouble();
            }
        }

        if (v.width == -1) {
            // This is a virtual variable, an column in the data matrix
        } else {
            // This is a real variable
        }

        // Include the variable in the variables list
        sav.variables.add(v);

        // Section for the variable
        addSection(SAVSection.TAG_VARIABLE, v);

        return v;
    }

    private SAVValueLabels parseSAVValueLabels() {
        SAVValueLabels vls = new SAVValueLabels();

        // Number of labels
        int arrayLength = readInt();

        for (int i = 0; i < arrayLength; i++) {

            // Allocate an array for raw 8-byte value
            byte[] data = new byte[8];

            // Read the value itself
            // NOTE: These eight bytes can represent
            // either a string or a number.
            readBytes(data, 0, 8);

            String vlabel = readAlignedString(1, 8, 1);

            // Add a mapping
            SAVValue value = new SAVValue(data);
            vls.map.put(value, vlabel);
        }

        // Save the newly built Value-Label Map into the SAVFile
        sav.valueLabelMaps.add(vls);

        // S ection for the value label map
        addSection(SAVSection.TAG_VALUE_LABELS, vls);

        // Remember this as the latest
        lastVLMap = vls;

        return vls;
    }

    private Vector<SAVVariable> parseSAVVariableList() {

        // Parse array's length
        int arrayLength = readInt();

        // Allocate with guaranteed capacity
        Vector<SAVVariable> varsArray
            = new Vector<SAVVariable>(arrayLength);

        // Cache to a local the number of variables
        int numVariables = sav.variables.size();

        // Type of the variables
        int type = SAVValue.TYPE_UNASSIGNED;

        for (int i = 0; i < arrayLength; i++) {
            // Parse the variable index number
            int indexNumber = readInt();

            // Sanity check for the index number
            if ((indexNumber < 1) || (indexNumber > numVariables)) {
                throw new RuntimeException(String.format(
                    "Unexpected variable index: %d (should be between 1..%d)",
                    indexNumber, numVariables));
            }

            // Resolve the index number into a SAVVariable object
            SAVVariable v = sav.variables.elementAt(indexNumber-1);

            // Sanity check on the resolved variable.
            // The variable should be non-virtual,
            // that is, it should have width >= 0.

            if (v.width < 0) {
                throw new RuntimeException(String.format(
                    "Unexpected variable width while associating the variable with Value-Label map: %d (index=%d)",
                    v.width, indexNumber));
            }

            // Get the variable type
            int varType = v.getType();
            //System.out.printf("Var#%d width: %d, name: %s   %s\n", i, v.width, v.name, v.label);

            // Decide type or type check
            if (i == 0) {
                // Decide type
                type = varType;
                // The variable must have a valid type,
                // because at this point the width >= 0.
            } else if (type != varType) {

                throw new RuntimeException(String.format(
                    "Mismatching variable type: %d (expected %d)",
                    type, varType));

            } // if-else

            // Add the resolved variable into the variable list
            varsArray.add(v);
        } // for

        if (lastVLMap == null) {
            // The Value-Label map must be provided prior
            // to the variable list.
            throw new RuntimeException("Variable list must be preceded by a Value-Label map");
        }

        // Reinterpret the map keys
        lastVLMap.map = reinterpretVLMap(lastVLMap.map, type);

        // Set the VL map's type
        lastVLMap.type = type;

        // Bind variable list to the latest VL map.
        // NOTE: If there was an earlier variable list,
        // this overrides it.
        lastVLMap.variables = varsArray;

        // Section for the variable set
        addSection(SAVSection.TAG_VARIABLE_LIST, varsArray);

        return varsArray;
    }

    private Map<SAVValue, String> reinterpretVLMap(
        Map<SAVValue, String> rawMap,
        int type
    ) {
        // This will be the reinterpreted map
        Map<SAVValue, String> map = new LinkedHashMap<SAVValue, String>();

        // Convert the map according to the determined type
        for (Map.Entry<SAVValue, String> entry : rawMap.entrySet()) {

            SAVValue valueOrig = entry.getKey();
            String vlabel = entry.getValue();

            byte[] bytes = valueOrig.getRaw();

            SAVValue valueNew = new SAVValue();

            switch(type) {
                case SAVValue.TYPE_NUMERIC:
                    // Utilizes floatingEndianness in the base-class
                    valueNew.setDouble(bytesToDouble(bytes, 0));
                    //System.out.printf("%f    %s\n", valueNew.getDouble(), vlabel);
                    break;
                case SAVValue.TYPE_STRING:
                    // Utilizes the configured encoding
                    valueNew.setString(bytesToStringUnpad(bytes));
                    //System.out.printf("%8s    %s\n", valueNew.getString(), vlabel);
                    break;
                default:
                    throw new RuntimeException(String.format(
                        "Variables in the list have an unexpected type: %d", type));
            } // switch

            map.put(valueNew, vlabel);

        } // for

        return map;
    }

    private List<String> parseSAVDocuments() {
        List<String> list = new LinkedList<String>();

        int lines = readInt();
        byte[] lineBuffer = new byte[80];
        for (int i = 0; i < lines; i++) {
            readBytes(lineBuffer, 0, 80);
            list.add(bytesToStringUnpad(lineBuffer));
        }

        // S ection for the value label map
        addSection(SAVSection.TAG_DOCUMENTS, list);

        sav.documents = list;

        return list;
    }

    private void parseSAVExtensionRecord() {
        /*
        int subtag;
        int elementSize;
        int numberOfElements;

        // Prior to creating the extension record itself,
        // read all the known fields into local variables,
        // and from there, pass to the actual object.
        */


        SAVExtensionRecord ext = new SAVExtensionRecord();

        ext.subtag = readInt();
        ext.elementSize = readInt();
        ext.numberOfElements = readInt();
        ext.data = null;

        // This is used to communicate the current extension record
        // down to the individual parsing methods
        lastExt = ext;

        boolean handled = false;
        switch(ext.subtag) {
            case 3: // Source platform integer info record
                parseExtSystemConfig();
                handled = true;
                break;
            case 4: // Source platform floating-point info record
                parseExtNumberConfig();
                handled = true;
                break;
            case 7: // Variable sets
                break;
            case 11: // Level, Width, Aligment
                break;
            case 13: // Long variable names record
                break;
            case 14: // Very long string record
                break;
            case 16: // int64 version of numberOfCases
                break;
            case 17: // Attributes
                break;
            case 18: // Attributes
                break;
            case 19: // Variable sets
                break;
            case 20: // Encoding
                break;
            case 21: // Long value label string map
                break;
            case 22: // Long missing value map
                break;

            default:
                // Unhandled extension record
                break;
        } // switch

        if (handled == false) {
            int sizeBytes = ext.elementSize * ext.numberOfElements;
            ext.data = new byte[sizeBytes];
            readBytes(ext.data, 0, sizeBytes);

            lastExt = ext;
        }

        // Add to extension records
        sav.extensionRecords.add(lastExt);

        // Section for the variable set
        addSection(SAVSection.TAG_EXTENSION_RECORD, lastExt);

    }

    private void expectExtensionSize(int elementSize, int numberOfElements) {
        if ((lastExt.elementSize != elementSize)
            || (lastExt.numberOfElements != numberOfElements))
        {
            throw new RuntimeException(String.format(
                "Extension record subtag=%d was expected to have elemsize=%d and nelem=%d, but found %d and %d",
                lastExt.subtag,
                elementSize, numberOfElements,
                lastExt.elementSize, lastExt.numberOfElements)
            );
        }
    }


    private void parseExtSystemConfig() {
        expectExtensionSize(4, 8);

        // Create a new extension record object
        sav.systemConfig = new SAVExtSystemConfig();

        // For convenience
        SAVExtSystemConfig systemConfig = sav.systemConfig;

        // Swap lastExt with the newly created systemConfig
        systemConfig.copy(lastExt);
        lastExt = systemConfig;

        // Parse the extension record
        systemConfig.versionMajor = readInt();
        systemConfig.versionMinor = readInt();
        systemConfig.versionRevision = readInt();
        systemConfig.machineCode = readInt();
        systemConfig.fpFormat = readInt();
        systemConfig.compression =  readInt();
        systemConfig.systemEndianness = readInt();
        systemConfig.stringCodepage = readInt();

    }

    private void parseExtNumberConfig() {
        expectExtensionSize(8, 3);

        // Create a new extension record object
        sav.numberConfig = new SAVExtNumberConfig();

        // For convenience
        SAVExtNumberConfig numberConfig = sav.numberConfig;

        // Swap lastExt with the newly created systemConfig
        numberConfig.copy(lastExt);
        lastExt = numberConfig;

        // Parse the extension record
        numberConfig.sysmissValue = readDouble();
        numberConfig.highestValue = readDouble();
        numberConfig.lowestValue = readDouble();
    }

    protected void parseSAVDataMatrix2() {
        // Compute the matrix data size in bytes
        int matrixSize = length() - tell();

        // Sanity check for the data size.
        if ((matrixSize % 8) != 0) {
            throw new RuntimeException(String.format(
                "The data matrix size mod 8 is non-zero (%d)", matrixSize % 8));
        }

        // Create a fixed-size byte array
        DynamicByteArray array = new DynamicByteArray(matrixSize);
        // Also create a cursor at the beginning for writing
        ByteCursor cursor = new ByteCursor(array, 0);

        // Create and configure the parser, which is needed in any case.
        SAVMatrixParser matrixParser = new SAVMatrixParser();

        // Use the configuration values are the reader.
        matrixParser.setEncoding(getEncoding());
        matrixParser.setEndianness(getFloatingEndianness());

        // Returns the SYSMISS value from SAV extension record,
        // or the default value, if no extension record available.
        matrixParser.setSysmiss(sav.getSysmissValue());

        // Allocate the string buffer, and at the same time,
        // set the limit for the maximum string length
        matrixParser.reallocStringBuffer(32*1024); // 32 kilobytes

        // Set column widths
        int nvars = sav.variables.size();
        int[] widths = new int[nvars];
        for (int i = 0; i < nvars; i++) {
            widths[i] = sav.variables.elementAt(i).width;
        }
        matrixParser.setColumnWidths(widths);

        // Create and configure decompressor

        SAVMatrixDecompressor decompressor = new SAVMatrixDecompressor();
        // Configure according to parser configuration
        decompressor.setEndianness(matrixParser.getEndianness());
        decompressor.setSysmiss(matrixParser.getSysmiss());
        // Configure bias according to the SAV header record.
        decompressor.setBias(sav.header.getBias());
        // Set matrix parser as the data receiver
        decompressor.setDataReceiver(matrixParser);

        int compression = sav.header.getCompression(); // For convenience
        if (compression == 0) {
            // Data is uncompressed. Enable passthrough
            decompressor.setPassthrough(true);
        } else if (compression == 1) {
            // Using compression.
        } else {
            throw new RuntimeException(String.format(
                "Unsupported compression code: %d", compression));
        }

        byte[] data = new byte[8];
        int bytesRead = -1;
        int errno;

        while (true) {

            // may throw
            try {
                bytesRead = read(data, 0, 8);
            } catch(IOException ex) {
                error_io("read(data, 0, 8)", ex);
            }

            if (bytesRead == -1) {
                // At the end-of-file.
                // Send the EOF marker to decompressor, and quit.
                errno = decompressor.consume(null);
                break;
            }

            // Otherwise verify that 8 bytes was read
            if (bytesRead != 8) {
                throw new RuntimeException(String.format(
                    "Unaligned number of data bytes in the matrix. "
                    +"Read %d bytes while expected 8",
                    bytesRead));
            }

            // Write the data into the array immediately
            int bytesWritten;
            bytesWritten = cursor.write(data, 0, bytesRead);
            if (bytesWritten != bytesRead) {
                throw new RuntimeException(String.format(
                    "Buffer size miscalculated: was able to "
                    +"write only %d bytes out of %d",
                    bytesWritten, bytesRead));
            }

            // The array got 8 bytes. Send them to the decompressor.
            errno = decompressor.consume(data);

            // Check the decompressor's status
            if (errno > SAVMatrixDecompressor.E_OK) {
                // Decompressor has finished with an error.
                break;
            }
        } // while

        // flush array
        cursor.flush();

        if (errno > SAVMatrixDecompressor.E_OK) {
            if (errno == SAVMatrixDecompressor.E_OTHER) {
                throw new RuntimeException(String.format(
                    "Data matrix parsing finished with an error code: %d",
                    matrixParser.errno()));
            } else {
                throw new RuntimeException(String.format(
                    "Decompressor finished with an error code: %d (%s)",
                    decompressor.errno(),
                    decompressor.strerror()));
            }
        }

        // Create RawMatrix
        SAVMatrix dataMatrix
            = new SAVRawMatrix(array, matrixParser, decompressor);

        sav.dataMatrix = dataMatrix;

        // Section for the data matrix
        addSection(SAVSection.TAG_DATA_MATRIX, dataMatrix);

        // Test out the traversal functionality
        //SAVMatrixDebugHandler debugHandler = new SAVMatrixDebugHandler();
        //dataMatrix.traverse(debugHandler);
    }


    public static class SAVMatrixDebugHandler
        implements spssio.sav.SAVMatrixHandler
    {

        public SAVMatrixDebugHandler() {
            System.out.printf("MatrixDebugHandler: ctor\n");
        }

        public void onMatrixBegin(int xsize, int ysize, int[] columnWidths) {
            System.out.printf("matrix begin: xsize=%d ysize=%d, ...\n",
                xsize, ysize);
        }

        public void onMatrixEnd() {
            System.out.printf("matrix end\n");
        }

        public void onRowBegin(int y) {
            System.out.printf("row begin: y=%d\n", y);
        }

        public void onRowEnd(int y) {
            System.out.printf("row end: y=%d\n", y);
        }

        public void onCellSysmiss(int x) {
            System.out.printf("cell sysmiss: x=%d\n", x);
        }

        public void onCellNumber(int x, double value) {
            System.out.printf("cell number: x=%d value=%f\n", x, value);
        }

        public void onCellInvalid(int x) {
            System.out.printf("cell invalid: x=%d\n", x);
        }

        public void onCellString(int x, String value) {
            System.out.printf("cell string: x=%d value=<%s>\n", x, value);
        }
    }

    // TEST CODE
    //===========

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.printf("No file specified\n");
            return;
        }

        String filename = args[0];
        System.out.printf("%s\n", filename);

        SAVReader savReader = new SAVReader();

        try {
            savReader.parse(filename);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}

