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

package spssio.sav.output;

// core java
import java.util.Vector;
import java.util.Map;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
    
    // OTHER METHODS
    //===============
    
    public void output(SAVFile sav, String filename) 
        throws FileNotFoundException, IOException, SecurityException
    {
        // TODO:
        // Convert sav to sections
        
        // Output the sections
        outputSections(sav, filename);
    }
    
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
        
        writeInt(header.compressed);
        
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

            byte[] encoded = encodeString(v.label);
            writeInt(encoded.length);
            writeAlignedString(encoded, 4, 0);

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

            // Write the label, aligned to 8 bytes with offset=1
            byte[] encoded = encodeString(vlabel);
            write1(encoded.length);
            writeAlignedString(encoded, 8, 1);
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
    
    public void outputSAVExtensionRecord(SAVExtensionRecord ext) {
        writeInt(SAVSection.TAG_EXTENSION_RECORD);
        
        writeInt(ext.subtag);
        
        writeInt(ext.elementSize);

        writeInt(ext.numberOfElements);
        
        int sizeBytes = ext.elementSize * ext.numberOfElements;
        
        writeBytes(ext.data, 0, sizeBytes);
    }
    
    public void outputSAVMatrix(SAVMatrix dataMatrix) {
        writeInt(SAVSection.TAG_DATA_MATRIX);

        // TODO: filler
        writeInt(0); // filler
        
        // NOTE! A parent structure not provided by the arguments is referenced
        // if compressed...
        
        SAVMatrixWriter matrixWriter = new SAVMatrixWriter();
        
        // Set up...
        
        if (sav.header.compressed != 0) {
            // set writer outputstream as compressor
        } else {
        }
    }
    
    
}

