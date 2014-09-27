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
import java.util.Arrays; // copyOf
import java.io.OutputStream;
import java.io.IOException;

// spssio
import spssio.sav.SAVConstants;
import spssio.util.DataEndianness;

/**
 *
 */
public class SAVMatrixCompressor
    extends OutputStream
{

    // MEMBER VARIABLES: SETTINGS
    //============================

    private DataEndianness floatingEndianness;

    /**
     * Configured compression bias
     */
    private double bias;

    /**
     * Configured SYSMISS value
     */
    private double sysmiss;

    /**
     * Column widths
     */
    private int[] columnWidths;

    // MEMBER VARIABLES
    //==================

    /**
     * Next output stream to which the compressed stream is sent to
     */
    private OutputStream ostream;

    /**
     * Current column
     */
    private int column;

    /**
     * Buffer for single data packet, which consists of the control data,
     * and optionally 8*8 bytes of payload data.
     */
    private byte[] buffer;

    /**
     * Current control byte index
     */
    private int cbyteIndex;

    /**
     * Number of bytes in the buffer. This is always at least 8.
     */
    private int bytes;

    // CONSTRUCTORS
    //==============

    public SAVMatrixCompressor() {
        ostream = null;
        columnWidths = null;
        column = 0;
        cbyteIndex = 0;
        bytes = 8;

        buffer = new byte[8+(8*8)];


        floatingEndianness = new DataEndianness();

        // Set default floating endianness
        setFloatingEndianness(SAVConstants.DEFAULT_ENDIANNESS);

        // Set default sysmiss
        setSysmissValue(SAVConstants.DEFAULT_SYSMISS_VALUE);
    }

    // CONFIGURATION METHODS
    //=======================

    public void setFloatingEndianness(int endianness) {
        floatingEndianness.set(endianness);
    }

    public int getFloatingEndianness() {
        return floatingEndianness.get();
    }

    public void setColumnWidths(int[] columnWidths) {
        this.columnWidths = Arrays.copyOf(columnWidths, columnWidths.length);
    }

    public int[] getColumnWidths() {
        return Arrays.copyOf(columnWidths, columnWidths.length);
    }

    public void setSysmissValue(double sysmiss) {
        this.sysmiss = sysmiss;
        //updateSysmissBytes();
    }

    public double getSysmissVaue() {
        return sysmiss;
    }

    public void setBias(double bias) {
        this.bias = bias;
    }

    public double getBias() {
        return bias;
    }

    public void setOutputStream(OutputStream ostream) {
        this.ostream = ostream;
    }

    // OTHER METHODS
    //===============

    public void reset() {
        column = 0;
        cbyteIndex = 0;
        bytes = 8;
    }


    @Override
    public void write(int b)
        throws IOException
    {
        throw new IOException("Do not call this method");
    }

    @Override
    public void write(byte[] b)
        throws IOException
    {
        throw new IOException("Do not call this method");
    }

    @Override
    public void write(byte[] b, int off, int len)
        throws IOException
    {
        if (len != 8) {
            throw new IllegalArgumentException("len != 8");
        }

        // The width of the current column
        int width = columnWidths[column];
        boolean isCompressible = false;

        // 0: numeric, >0 first of a string, -1, non-first of a string
        if (width == 0) {
            // compress numeric
            isCompressible = compressNumeric(b, off);
        } else {
            // compress string
            isCompressible = compressString(b, off);
        }

        if (isCompressible == false) {
            // Append buffer
            //appendBuffer(b, off);
            System.arraycopy(b, off, buffer, bytes, 8);
            bytes += 8;
        }

        if (cbyteIndex == 8) {
            // Time to flush the buffer to the next output stream
            flushBuffer();
        }

        column++;
        if (column == columnWidths.length) {
            column = 0;
        }
    }

    @Override
    public void flush()
        throws IOException
    {
        if (cbyteIndex > 0) {
            // Finish up the current segment
            for (; cbyteIndex < 8; cbyteIndex++) {
                buffer[cbyteIndex] = SAVConstants.CBYTE_NOP;
            }
            flushBuffer();
        }
        ostream.flush();
    }

    private void appendBuffer(byte[] b, int off) {
        System.arraycopy(b, off, buffer, bytes, 8);
        bytes += 8;
    }

    private void flushBuffer()
        throws IOException
    {
        // TODO
        ostream.write(buffer, 0, bytes);

        // Reset counters
        cbyteIndex = 0;
        bytes = 8;
    }

    private boolean compressNumeric(byte[] b, int off) {
        boolean rval = false;
        int cbyte = -1;

        // The value of the number data must be examined.
        double value = floatingEndianness.bytesToDouble(b, off);

        //System.out.printf("value: %f (%16x)\n", value, Double.doubleToLongBits(value));

        if (value == sysmiss) {
            // SYSMISS is compressible
            cbyte = SAVConstants.CBYTE_SYSMISS;
            rval = true;
        } else {
            // Biased value is compressible if it is
            //    -an integer
            //    -between 1..251

            // Calculate the code as a double
            double dcode = value + bias;

            // Convert the double into an integer
            int icode = (int) dcode;

            // Test whether the code is an integer
            // and whether it is between 1..251
            if (((double)(icode) == dcode)
                && ((1 <= icode) && (icode <= 251)))
            {
                // Compressible
                cbyte = icode;
                rval = true;
            } else {
                // Incompressible numeric value
                cbyte = SAVConstants.CBYTE_RAW_DATA;
            }
        } // if-else

        // Deserialize b[] into a double according to the endianness
        // and floating-point format.

        // in any case:
        buffer[cbyteIndex] = (byte) cbyte;
        cbyteIndex++;

        return rval;
    }

    private boolean compressString(byte[] b, int off) {
        boolean allWhitespaces = true;
        boolean rval = false;

        // Test whether the string segment is all whitespaces
        for (int i = 0; i < 8; i++) {
            byte c = b[off+i];
            if (c != 0x20) {
                allWhitespaces = false;
                break;
            }
        }

        int cbyte = -1;
        if (allWhitespaces == true) {
            // Compressible
            cbyte = SAVConstants.CBYTE_WHITESPACES;
            rval = true;
        } else {
            // Incompressible
            cbyte = SAVConstants.CBYTE_RAW_DATA;
        }

        // in any case:
        buffer[cbyteIndex] = (byte) cbyte;
        cbyteIndex++;

        return rval;
    }
}
