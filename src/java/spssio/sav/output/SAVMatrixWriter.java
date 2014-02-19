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
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;

// spssio
import spssio.sav.SAVMatrixHandler;
import spssio.sav.SAVMatrix;
import spssio.sav.SAVConstants; // Default sysmiss
import spssio.util.DataEndianness;

/*
 * TBC: The renaming of the class?
 */
public class SAVMatrixWriter 
    implements SAVMatrixHandler
{
    
    // MEMBER VARIABLES
    //==================

    /**
     * The index of the variable that is being read next
     */
    private int index;
    
    /**
     * Configured column widths
     */
    private int[] columnWidths;

    /**
     * Encoding of the raw string bytes
     */
    private Charset encoding;

    /**
     * Configured SYSMISS value of the input data
     */
    private double sysmiss;
    
    /**
     * Configured endianness of the input data
     */
    private DataEndianness floatingEndianness;
    
    /**
     * Memory area for 8-byte segments
     */
    private byte[] byteBuffer;

    /**
     * Data matrix compressor
     */
    private OutputStream ostream;

    // CONSTRUCTORS
    //==============
    
    public SAVMatrixWriter() {
        encoding = null;
        ostream = null;
        byteBuffer = new byte[8];
        floatingEndianness = new DataEndianness();
        
        // Set default sysmiss value
        setSysmissValue(SAVConstants.DEFAULT_SYSMISS_VALUE);
        
        // Set default endianness
        floatingEndianness.set(SAVConstants.DEFAULT_ENDIANNESS);
        
        // Set default encoding
        setStringEncoding(SAVConstants.DEFAULT_STRING_ENCODING);
    }
    
    // CONFIGURATIN METHODS
    //======================

    public void setEndianness(int endianness) {
        floatingEndianness.set(endianness);
    }
    
    public int getEndianness() {
        return floatingEndianness.get();
    }
    
    public void setSysmissValue(double sysmiss) {
        this.sysmiss = sysmiss;
    }
    
    public double getSysmissValue() {
        return sysmiss;
    }
    
    public void setStringEncoding(String charsetName) {
        try {
            encoding = Charset.forName(charsetName);
        } catch(UnsupportedCharsetException ex) {
            throw new IllegalArgumentException(ex);
        } // try-catch
    }
    
    public String getStringEncoding() {
        return encoding.name();
    }
    
    public void setOutputStream(OutputStream ostream) {
        this.ostream = ostream;
    }
    
    public void setColumnWidths(int[] columnWidths) {
        this.columnWidths = Arrays.copyOf(columnWidths, columnWidths.length);
    }

    // OTHER METHODS
    //===============
    
    public void outputSAVMatrix(SAVMatrix dataMatrix) {

        // Reset index prior to traversal
        index = -1; // increasing by one will get to the first
        
        // Create a matrix handler
        
        // Select and configure handler:
        // Either direct or compressor
        
        dataMatrix.traverse(this);
        
        try {
            ostream.flush();
        } catch(IOException ex) {
            // Pass up
            throw new RuntimeException(ex);
        }
    }
    
    // PRIVATE HELPERS
    //=================

    /**
     * TODO: This is common to both BaseReader and BaseWriter, 
     * so it should be put into a separate file.
     */
    private static int calculateAlignedLength(
        int length, 
        int alignment, 
        int offset
    ) {
        alignment--;
        int mask = ~alignment;
        // Calculates (ceil((length+offset) / alignment) * alignment) - offset
        return ((length+alignment+offset) & mask) - offset;
    }
    
    private void serializeString(String string, int width) {
        if (encoding == null) {
            throw new RuntimeException("Encoding is unconfigured");
        }
        
        byte[] encoded = string.getBytes(encoding);
        
        int alignedLength = calculateAlignedLength(width, 8, 0);
        int paddingLength = alignedLength - width;
        
        // Number of valid bytes in the byteBuffer
        int bytes = 0;
        
        for (int i = 0; i < alignedLength; i++) {
            byte cbyte = 0x20;
            
            if (i < encoded.length) {
                cbyte = encoded[i];
            }
            
            byteBuffer[bytes] = cbyte;
            bytes++;
            
            if (bytes == byteBuffer.length) {
                // emit accumulated buffer contents
                consumeByteBuffer();
                bytes = 0;
            }
        }
        
        // Verify that bytes == 0
        if (bytes != 0) {
            throw new RuntimeException("bytes != 0 (this should not happen)");
        }
    }
    
    private void serializeDouble(double value) {
        // TODO: convert to bits according to encoding
        long rawBits = Double.doubleToLongBits(value);
        // TODO: serialize into buffer according to endianness
        
        for (int i = 0; i < 8; i++) {
            byteBuffer[i] = (byte)(rawBits & 0xff);
            rawBits = rawBits >>> 8;
        }
        consumeByteBuffer();
    }
    
    /*
    private void serializeRawDouble(long raw) {
    }
    */
    
    private void consumeByteBuffer() {
        try {
            ostream.write(byteBuffer, 0, 8);
        } catch(IOException ex) {
            // Pass upwards
            throw new RuntimeException(ex);
        }

    }
    
    private int getNextWidth() {
        
        do {
            index++;
            if (index >= columnWidths.length) {
                index = 0;
            }
            
            if (index < columnWidths.length) {
                int width = columnWidths[index];
                if (width >= 0) {
                    // use this
                    return width;
                }
                // otherwise continue search
            }
        } while (true);
    }
    
    // SAVMatrixHandler interface
    //============================
    
    public void onMatrixBegin(int xsize, int ysize, int[] columnWidths) {
        // NOP
    }
    
    public void onMatrixEnd() {
        // NOP
    }
    
    public void onRowBegin(int y) {
        // NOP
    }
    
    public void onRowEnd(int y) {
        // NOP
    }
    
    public void onCellSysmiss(int x) {
        int width = getNextWidth();
        // Verify width? 
        serializeDouble(sysmiss);
    }
    
    public void onCellNumber(int x, double value) {
        int width = getNextWidth();
        // Verify width? 
        serializeDouble(value);
    }
    
    public void onCellInvalid(int x) {
        int width = getNextWidth();
        // Verify width? 
        // TODO: raise an error?
        serializeDouble(sysmiss);
    }
    
    public void onCellString(int x, String value) {
        int width = getNextWidth();
        //int width = 8;
        //System.out.printf("Serializng \"%s\", width: %d\n", value, width);
        // Verify width? 
        
        // Serialize string to that length, and padd to align
        serializeString(value, width);
    }
    
    protected void emitBytes() {
    }
    
    
}
