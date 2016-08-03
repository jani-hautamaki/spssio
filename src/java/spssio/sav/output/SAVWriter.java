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



package spssio.sav.output;

// core java
import java.util.Vector;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileNotFoundException;

// spssio
import spssio.sav.output.SAVBaseWriter;
import spssio.sav.SAVFile;
import spssio.sav.SAVSection;
import spssio.sav.SAVHeader;
import spssio.sav.SAVVariable;
import spssio.sav.SAVValueLabels;
import spssio.sav.SAVValue;
import spssio.sav.SAVExtensionRecord;
import spssio.sav.SAVExtSystemConfig;
import spssio.sav.SAVExtNumberConfig;
import spssio.sav.SAVMatrix;

public class SAVWriter
    extends SAVBaseWriter
{
    // MEMBER VARIABLES
    //==================

    private SAVFile sav;

    // CONSTRUCTORS
    //==============

    public SAVWriter() {
        sav = null;
    }

    // HELPERS
    //=========

    protected void addSection(int tag, Object obj) {
        sav.sections.add(new SAVSection(tag, obj));
    }

    // OTHER METHODS
    //===============

    public void output(SAVFile sav, String filename)
        throws FileNotFoundException, IOException, SecurityException
    {
        // Remember the SAVFile object being outputted
        this.sav = sav;

        // Remove previous sectionization
        sav.sections.clear();

        // Sections:
        // 1.   Header (pseudo-section)
        // 2.   Variables
        // 3a.  Value-Label map
        // 3b.  Variable list
        // 4.   Extension records
        // 5.   Data matrix

        addSection(SAVSection.TAG_HEADER, sav.header);

        int len = sav.variables.size();
        for (int index = 0; index < len; index++) {
            SAVVariable v = sav.variables.get(index);
            addSection(SAVSection.TAG_VARIABLE, v);
        }

        len = sav.valueLabelMaps.size();
        for (int index = 0; index < len; index++) {
            SAVValueLabels vlMap = sav.valueLabelMaps.get(index);
            addSection(SAVSection.TAG_VALUE_LABELS, vlMap);
            addSection(SAVSection.TAG_VARIABLE_LIST, vlMap.variables);

        }

        // Documents, if any
        if (sav.documents != null) {
            addSection(SAVSection.TAG_DOCUMENTS, sav.documents);
        }

        // Extension records in the order of their appearance.
        len = sav.extensionRecords.size();
        for (int index = 0; index < len; index++) {
            SAVExtensionRecord ext = sav.extensionRecords.get(index);
            addSection(SAVSection.TAG_EXTENSION_RECORD, ext);
        }

        // Data matrix
        addSection(SAVSection.TAG_DATA_MATRIX, sav.dataMatrix);

        // Output the sections
        outputSections(sav, filename);
    }

    /*
    // TODO: Should these actually be in SAVFile ?
    public void rebuildSections() {
    }
    public Vector<SAVSection> sectionize() {
}
    */

    public void outputSections(SAVFile sav, String filename)
        throws FileNotFoundException, IOException, SecurityException
    {

        // Create a fname object
        File f = new File(filename);

        // May throw FileNotFound
        FileOutputStream fis = new FileOutputStream(filename);

        // Remember the SAVFile object being outputted
        // TODO: This should probably be a parameter of bind()
        this.sav = sav;

        bind(fis);

        // Open file, setup base writer
        for (SAVSection section : sav.sections) {
            outputSAVSection(section);
        }


        // May throw IOException, close quietly.
        try {
            unbind(); // Does flushing
            fis.close();
        } catch(IOException ex) {
            System.err.printf("%s: FileInputStream.close() failed. Ignoring.\n", filename);
        } // try-catch

        // Forget the SAVFile
        this.sav = null;
    }

    @SuppressWarnings("unchecked")
    public void outputSAVSection(SAVSection section) {
        // For convenience and brevity
        Object obj = section.obj;

        switch(section.tag) {
            case SAVSection.TAG_HEADER:
                outputSAVHeader((SAVHeader) obj);
                break;

            case SAVSection.TAG_VARIABLE:
                outputSAVVariable((SAVVariable) obj);
                break;

            case SAVSection.TAG_VALUE_LABELS:
                outputSAVValueLabels((SAVValueLabels) obj);
                break;

            case SAVSection.TAG_VARIABLE_LIST:
                // NOTE: Unchecked cast
                outputSAVVariableList((Vector<SAVVariable>) obj);
                break;

            case SAVSection.TAG_DOCUMENTS:
                // NOTE: Unchecked cast
                outputSAVDocuments((List<String>) obj);
                break;

            case SAVSection.TAG_EXTENSION_RECORD:
                outputSAVExtensionRecord((SAVExtensionRecord) obj);
                break;

            case SAVSection.TAG_DATA_MATRIX:
                outputSAVMatrix((SAVMatrix) obj);
                // Data record begins
                break;

            default:
                // Unrecoverable error.
                throw new RuntimeException(String.format(
                    "unexpected tag code %x", section.tag));
        } // switch
    }

    public void outputSAVHeader(SAVHeader header) {
        //writeInt(SAVSection.TAG_HEADER); // this is a pseudo-tag...

        writePaddedString(header.signature, 4);

        writePaddedString(header.software, 60);

        writeInt(header.layout);

        writeInt(header.variableCount);

        writeInt(header.compression);

        writeInt(header.weightVariableIndex);

        writeInt(header.numberOfCases);

        writeDouble(header.bias);

        writePaddedString(header.date, 9);

        writePaddedString(header.time, 8);

        writePaddedString(header.title, 64);

        // TODO header padding as bytes!
        writeBytes(header.padding, 0, 3);
    }

    public void outputSAVVariable(SAVVariable v) {
        writeInt(SAVSection.TAG_VARIABLE);

        writeInt(v.width);

        writeInt(v.hasLabel);

        writeInt(v.numberOfMissingValues);

        writeInt(v.inputFormat.raw);

        writeInt(v.outputFormat.raw);

        writePaddedString(v.name, 8);

        if (v.hasLabel != 0) {
            // Write the label, aligned to 4 bytes with offset=0,
            // String length as DWORD (INTEGER)
            writeAlignedString(4, v.label, 4, 0);

            // For convenience and brevity
            int nvalues = v.numberOfMissingValues;

            if (nvalues < 0) {
                // Has a range
                nvalues = -nvalues;
            }

            if (nvalues > 0) {
                for (int i = 0; i < nvalues; i++) {
                    writeDouble(v.missingValues[i]);
                }
            }
        }
    }

    public void outputSAVValueLabels(SAVValueLabels vls) {
        writeInt(SAVSection.TAG_VALUE_LABELS);

        writeInt(vls.map.size());

        for (Map.Entry<SAVValue, String> e : vls.map.entrySet()) {
            SAVValue value = e.getKey();
            String vlabel = e.getValue();

            // Write 8 bytes; the value
            outputSAVValue(value);

            // Write the label, aligned to 8 bytes with offset=1,
            // String length as BYTE.
            writeAlignedString(1, vlabel, 8, 1);
        }
    }

    public void outputSAVValue(SAVValue value) {
        switch(value.type) {
            case SAVValue.TYPE_UNASSIGNED:
                throw new RuntimeException();
            case SAVValue.TYPE_NUMERIC:
                writeDouble(value.getDouble());
                break;
            case SAVValue.TYPE_STRING:
                writePaddedString(value.getString(), 8);
                break;
            case SAVValue.TYPE_RAW:
                // TODO
                break;
            default:
                throw new RuntimeException();
        }
    }

    public void outputSAVVariableList(Vector<SAVVariable> list) {
        writeInt(SAVSection.TAG_VARIABLE_LIST);

        writeInt(list.size());

        for (SAVVariable v : list) {
            // Get index number.
            // NOTE! A parent structure not provided by the arguments is referenced
            int indexNumber = sav.variables.indexOf(v);

            // TODO: Validate index
            if (indexNumber == -1) {
                throw new RuntimeException();
            }

            // The list has 1-based values
            writeInt(indexNumber+1);
        }
    }

    public void outputSAVDocuments(List<String> list) {
        writeInt(SAVSection.TAG_DOCUMENTS);

        writeInt(list.size());

        for (String line : list) {
            byte[] encoded = encodeString(line);

            // If the length is more than 80 chars,
            // the extra characters are ignored silently.
            // Another option would be to wrap to the next line.
            // However, that would require pre-calculations.

            if (encoded.length > 80) {
                writeBytes(encoded, 0, 80);
            } else {
                writeBytes(encoded, 0, encoded.length);
                writeBytesRepeat((byte) 0x20, 80-encoded.length);
            }
        } // for

    }


    public void outputSAVExtensionRecord(SAVExtensionRecord ext) {
        writeInt(SAVSection.TAG_EXTENSION_RECORD);

        writeInt(ext.subtag);

        writeInt(ext.elementSize);

        writeInt(ext.numberOfElements);

        boolean handled = false;
        switch(ext.subtag) {
            case 3: // Source platform integer info record
                outputSAVExtSystemConfig((SAVExtSystemConfig) ext);
                handled = true;
                break;
            case 4: // Source platform floating-point info record
                outputSAVExtNumberConfig((SAVExtNumberConfig) ext);
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
        }

        if (handled == false) {
            if (ext.data == null) {
                throw new RuntimeException(String.format(
                    "Extension record subtag=%d has data==null", ext.subtag));
            }
            int sizeBytes = ext.elementSize * ext.numberOfElements;
            writeBytes(ext.data, 0, sizeBytes);
        }
    }

    public void outputSAVExtSystemConfig(SAVExtSystemConfig ext) {
        writeInt(ext.versionMajor);
        writeInt(ext.versionMinor);
        writeInt(ext.versionRevision);
        writeInt(ext.machineCode);
        writeInt(ext.fpFormat);
        writeInt(ext.compression);
        writeInt(ext.systemEndianness);
        writeInt(ext.stringCodepage);
    }
    public void outputSAVExtNumberConfig(SAVExtNumberConfig ext) {
        writeDouble(ext.sysmissValue);
        writeDouble(ext.highestValue);
        writeDouble(ext.lowestValue);
    }


    public void outputSAVMatrix(SAVMatrix dataMatrix) {
        writeInt(SAVSection.TAG_DATA_MATRIX);

        // TODO: filler
        writeInt(0); // filler

        // NOTE! A parent structure, which is not provided by the arguments,
        // is referenced here, ie. "sav".

        SAVMatrixWriter matrixWriter = new SAVMatrixWriter();

        // Get the underlying output stream to which the matrix is serialized.
        OutputStream ostream = getOutputStream();

        // Column widths according to the serialized variable records.
        // NOTE: The column widths cannot be taken from the data matrix.
        // It may contain information different from the variable records.
        int[] columnWidths = sav.getColumnWidths();


        if (sav.header.compression != 0) {
            // Insert Compressor to the serialization chain
            SAVMatrixCompressor compressor = new SAVMatrixCompressor();

            // Configure the compressor
            compressor.setOutputStream(ostream);
            compressor.setColumnWidths(columnWidths);
            compressor.setBias(sav.header.bias);

            // Set sysmiss value; either default or the configured value
            compressor.setSysmissValue(sav.getSysmissValue());

            // Wrap
            ostream = compressor;

        } else {
            // not compressed; needed routines are ...
            // writeAlignedString(s, width, noLength)
            // writeDouble()

        }

        // Set the output stream to which the matrix is serialized to.
        matrixWriter.setOutputStream(ostream);

        // Set the column widths; this is needed to determine how
        // many bytes should an encoded string variable value occupy.
        matrixWriter.setColumnWidths(columnWidths);

        // Set sysmiss value; this must be the same as for the compressor
        matrixWriter.setSysmissValue(sav.getSysmissValue());

        // Set string encoding
        matrixWriter.setStringEncoding(sav.getStringEncoding());

        // Serialize the matrix
        matrixWriter.outputSAVMatrix(sav.dataMatrix);
    }
}


