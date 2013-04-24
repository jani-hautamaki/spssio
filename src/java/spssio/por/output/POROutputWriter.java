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

package spssio.por.output;


// core java
import java.io.IOException;
import java.io.OutputStream;

// spssio
import spssio.por.PORCharset;
import spssio.por.PORConstants;
import spssio.util.NumberFormatter;
import spssio.util.NumberSystem;

/**
 * Provides serialization operations for the Portable file primitives,
 * and takes care of line-wrapping and character encoding.
 */
public class POROutputWriter {

    // CONSTANTS
    //===========
    
    /**
     * Use Unix-style end-of-lines: LF ({@code '\n'}) only.
     */
    public static final int EOL_LF                  = 0x0A;
    
    /**
     * Use Windows-style end-of-lines: CR+LF ({@code "\r\n"}.
     */
    public static final int EOL_CRLF                = 0x0D0A;

    // MEMBER VARIABLES
    //==================

    /**
     * Output byte stream to which the serialization is sent.
     */
    private OutputStream ostream;

    /*
     * Number of bytes written.
     * TODO: This is unused. Does it have any use?
     */
    //private int bytesWritten;

    /**
     * Encoding table
     */
    private int[] enctab;
    
    /**
     * Column number of the next byte, 0-based.
     */
    private int col;
    
    /**
     * Row number of the next byte, 0-based.
     */
    private int row;

    /**
     * Row length (number of columns in a row).
     */
    private int rowLength;
    
    /**
     * End-of-line type, either LF (unix) or CRLF (windows).
     */
    private int eolMarker;
    
    /**
     * NumberSystem object for base-30 numbers;
     * TODO: This reference is really not required.
     */
    private NumberSystem numberSystem;
    
    /**
     * Formatter for the base-30 numbers
     */
    private NumberFormatter numberFormatter;
    
    /**
     * NumberFormatter's internal buffer for quicker access.
     */
    private int[] buffer;
    
    // CONSTRUCTOR
    //=============
    
    public POROutputWriter() {
        // No output stream associated yet.
        ostream = null;
        
        // Reset the number of bytes written
        //bytesWritten = 0;
        
        // Reset encoding table
        enctab = null;
        
        // Reset next byte's textual location
        col = 0;
        row = 0;
        
        // Use system default value for row length
        rowLength = PORConstants.ROW_LENGTH;
        
        // End-of-line marker defaults to Windows style CRLF.
        // TODO: Get system default?
        eolMarker = EOL_CRLF;
        
        // Create a NumberSystem with default Portable parameters.
        NumberSystem numberSystem = new NumberSystem(30, null);
        
        // Create the formatter, and associate it to the newly-created
        // number system. Use the default precision specified
        // by the underlying implementation of the NumberFormatter.
        numberFormatter = new NumberFormatter(numberSystem);
        /*
        numberFormatter = new NumberFormatter(
            numberSystem, 
            NumberFormatter.getDefaultPrecision(numberSystem)
        );
        */
        
        // Get a reference to the formatter's internal 
        // buffer for faster access.
        buffer = numberFormatter.getBuffer();
        
        // Finally, initialize the encoding to identity transformation
        // (ie. no encoding).
        setEncoding(null);
    }
    
    // CONFIGURATION METHODS
    //=======================
    
    public void setEolMarker(int eolMarker) {
        // Validate; accept only Unix or Windows style.
        if ((eolMarker != EOL_CRLF) && (eolMarker != EOL_LF)) {
            throw new IllegalArgumentException(String.format(
                "Illegal end-of-line marker specified: 0x%04x", 
                eolMarker & 0xffff));
        }
        // Update
        this.eolMarker = eolMarker;
    }
    
    public int getEolMarker() {
        return eolMarker;
    }
    
    public void setRowLength(int rowLength) {
        // Validate
        if (rowLength < 1) {
            throw new IllegalArgumentException(String.format(
                "Illegal row length specified: %d",
                rowLength));
        }
        // Update value
        this.rowLength = rowLength;
    }
    
    public int getRowLength() {
        return rowLength;
    }
    
    public void setNumberFormatter(NumberFormatter numberFormatter) {
        // Validate
        if (numberFormatter == null) {
            throw new IllegalArgumentException(
                "Illegal NumberFormatter specified: null");
        }
        // Update
        this.numberFormatter = numberFormatter;
    }
    
    public NumberFormatter getNumberFormatter() {
        return numberFormatter;
    }

    /*
    public NumberSystem getNumberSystem() {
        return null;
        //return numberFormatter.get
    }

    public void setPrecision(int precision) {
        numberFormatter.setPrecision(precision);
    }
    
    public int getPrecision() {
        return numberFormatter.getPrecision();
    }
    */
    
    // OTHER METHODS
    //===============
    
    public void bind(OutputStream os) {
        if (os == null) {
            throw new IllegalArgumentException(
                "Illegal OutputStream specified: null");
        }
        
        // Bind the output stream
        ostream = os;
        
        // Reset textual location
        row = 0;
        col = 0;
        
        // TODO: Should the encoding be reset?
    }
    
    public void unbind() {
        ostream = null;
    }
    
    /*
    public int getBytesWritten() {
    }
    */
    
    /**
     * Return the row of next char
     *
     * @return The row of the next char
     */
    public int getRow() {
        return row;
    }
    
    /**
     * Return the column of the next char
     * 
     * @return The column of the next char
     */
    public int getColumn() {
        return col;
    }

    /**
     * Set byte encoding according to charset.
     * 
     * @param charset The character set to use in encoding,
     *      or {@code null} to disable encoding.
     * 
     */
    public void setEncoding(int[] charset) {
        // TODO: calculate encoding table
        if (charset != null) {
            enctab = PORCharset.getIdentityTable();
        } else {
            // If encoding unset, use identity transformation.
            enctab = PORCharset.getIdentityTable();
        }
    }
    
    
    // OTHER METHODS
    //===============
    
    //=======================================================================
    // SPSS/PSPP PRIMITIVES' OUTPUT METHODS
    //=======================================================================
    
    /**
     * Write a string.
     *
     * <b>TODO:</b> Should the 255 character string length 
     * limit be obeyed here?
     *
     * @param string The string to be written.
     */
    public void outputString(String string) 
        throws IOException
    {
        // UNROLLED outputInt()
        
        // Serialize and store the length of serialization
        int len = numberFormatter.formatInt(string.length());
        
        // Write out the value
        for (int i = 0; i < len; i++) {
            write(buffer[i]);
        }
        
        // Write number separator
        write(PORConstants.NUMBER_SEPARATOR);
        
        // Finally, serialize the string itself.
        write(string);
    }
    
    /**
     * Write a decimal number in base-30 digits. Formatting is done 
     * according to the current precision settings.<p>
     *
     * <b>TODO:</b> The method should be renamed into outputDouble()
     *
     * @param value The number to be written.
     *
     */
    public void outputDouble(double value)
        throws IOException
    {
        // Serialize and store the data length
        int len = numberFormatter.formatDouble(value);
        
        // Write data
        for (int i = 0; i < len; i++) {
            write(buffer[i]);
        } // for
        
        // Write number separator
        write(PORConstants.NUMBER_SEPARATOR);
    }
    
    /**
     * Write a SYSMISS numeric value with the default value separator.
     *
     * @see PORConstants#SYSMISS_SEPARATOR
     */
    public void outputSysmiss()
        throws IOException
    {
        write(PORConstants.SYSMISS_MARKER);
        write(PORConstants.SYSMISS_SEPARATOR);
    }
    
    /** 
     * Write a SYSMISS numeric value with a specified value separator.
     *
     * @param sep The character to be used as a value separator 
     *      after SYSMISS value.
     */
    public void outputSysmiss(int sep)
        throws IOException
    {
        write(PORConstants.SYSMISS_MARKER);
        write(sep);
    }

    /**
     * Write an integer number in base-30 digits.
     *
     * @param value An {@code int}-valued integer number.
     * 
     */
    public void outputInt(int value)
        throws IOException
    {
        
        // Serialize and get the length of the serialization
        int len = numberFormatter.formatInt(value);
        
        // Output the serialization
        for (int i = 0; i < len; i++) {
            write(buffer[i]);
        }
        
        // Write number separator
        write(PORConstants.NUMBER_SEPARATOR);
    }
    
    /**
     * Write a tag code
     *
     * @param c The tag code
     */
    public void outputTag(int c)
        throws IOException
    {
        // TODO!!!!!!!!!! 
        // Study whether the tag codes are subject to decoding/encoding
        write(c);
    }

    /**
     * Write end-of-file markers ('Z') to complete the current line.
     *
     */
    public void outputEofMarkers() 
        throws IOException
    {
        // Write end-marker
        write(PORConstants.EOF_MARKER); // 'Z'
        
        // Complete the last line with end-markers.
        int len = rowLength-col;
        for (int i = 0; i < len; i++) {
            write(PORConstants.EOF_MARKER); // 'Z'
        }
    }
    
    //=======================================================================
    // LOW-LEVEL OUTPUT METHODS
    //=======================================================================
    
    
    public void write(String string) 
        throws IOException
    {
        int len = string.length();
        int c;
        
        for (int i = 0; i < len; i++) {
            // Pick the current char
            c = string.charAt(i);
            
            // UNROLLED write(int)
            //=====================

            // Truncate and encode
            c = enctab[c & 0xff];
            
            // Write
            ostream.write(c);
            
            // Next column
            col++;
            
            // If line is full, write an end-of-line sequence,
            // reset column, and move to next row.
            if (col == rowLength) {
                // Write end-of-line sequence.
                // If Windows-style, then precede LF by CR.
                if (eolMarker == EOL_CRLF) {
                    ostream.write('\r'); // CR
                }
                ostream.write('\n'); // LF
                
                // Reset column and move to next row.
                col = 0;
                row++;
            } // if: row full
        } // for: each char
        
    } // write(String)
    

    /**
     * Write a sequence to output stream with the current encoding. 
     * A new line sequence is emitted if the line exceeds the current
     * row length setting.
     *
     * @param array The data array.
     * @param from First array position to write.
     * @param to First array position not to write.
     */
    public void write(int[] array, int from, int to) 
        throws IOException
    {
        int c;
        
        for (int i = from; i < to; i++) {
            // Pick the next byte
            c = array[i];
 
            // Finish with update sequence
            
            // UNROLLED write(int)
            //=====================

            // Truncate and encode
            c = enctab[c & 0xff];
            
            // Write
            ostream.write(c);
            
            // Next column
            col++;
            
            // If line is full, write an end-of-line sequence,
            // reset column, and move to next row.
            if (col == rowLength) {
                // Write end-of-line sequence.
                // If Windows-style, then precede LF by CR.
                if (eolMarker == EOL_CRLF) {
                    ostream.write('\r'); // CR
                }
                ostream.write('\n'); // LF
                
                // Reset column and move to next row.
                col = 0;
                row++;
            } // if: row full
        } // for
        
    } // write(array)
    
    /**
     * Write a byte to output stream with the current encoding. 
     * A new line sequence is emitted if the line exceeds the current
     * row length setting.
     *
     * @param c The byte to write
     */
    public void write(int c) 
        throws IOException
    {
        // Apparently SPSS writes an end-of-line sequence 
        // after the 'Z' sequence used as eof marker.
        // The following implementation replicates this behaviour.
        
        // Encode
        c = enctab[c & 0xff];
        
        // Write to the output stream
        ostream.write(c);
        
        // Next column
        col++;
        
        // If line is full, write an end-of-line sequence,
        // reset column, and move to next row.
        if (col == rowLength) {
            // Write end-of-line sequence.
            // If Windows-style, then precede with CR.
            if (eolMarker == EOL_CRLF) {
                ostream.write('\r'); // CR
            }
            ostream.write('\n'); // LF
            
            // Reset column and move to next row.
            col = 0;
            row++;
        } // if: row full
    } // write(int)
    
}