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

// spssio
import spssio.por.PORMatrixVisitor;
import spssio.por.PORValue;
import spssio.por.PORConstants;
import spssio.util.NumberParser;
import spssio.util.NumberSystem;

/**
 * Parser for Portable file data matrix record.<p>
 * 
 * TODO:
 *  - Renaming of the MatrixVisitor into MatrixHandler
 *  - The called methods; include "onInvalidNumber" to 
 *    indicate invalid numeric value.
 * 
 */
public class PORMatrixParser
{
    // CONSTANTS
    //===========
    
    public static final int DEFAULT_ROW_WIDTH                   = 80;
    public static final int DEFAULT_CELL_BUFFER_SIZE            = 4096;
    
    // Status codes
    
    public static final int E_REJECTED                          = -1;
    public static final int E_UNFINISHED                        = 0;
    public static final int E_ACCEPTED                          = 1;
    
    
    // ERROR MANAGEMENT
    //==================
    
    /**
     * Error message
     */
    private String strerror;

    // TODO: private int errno;

    // STATE MACHINE
    //===============
    
    /**
     * Current state 
     */
    private int state;
    
    /** 
     * Null-transition flag 
     */
    private boolean eps;
    
    
    // SPSS/PSPP cell-level
    //======================

    /** 
     * Column data type configuration indicating which columns
     * are numbers and which are strings.
     */
    private int[] xtype;

    /**
     * Current matrix column 
     */
    private int xcur;
    
    /**
     * Current matrix row 
     */
    private int ycur;

    /** 
     * Number of columns in the matrix.
     * This is set to zero initially, and discovered during 
     * the first pass, which is the parsing.
     */
    private int xdim;

    /** 
     * Number of rows in the matrix.
     * This is set to zero initially, and discovered during
     * the first pass, which is the parsing.
     */
    private int ydim;

    /**
     * Cell offsets in row-major order.<p>
     *
     * TODO: Discard this attribute? It cannot be allocated accurtely
     * before the first pass has been ran.
     */
    private int[] yoffset;
    
    // PARSING
    //=========
    
    /** 
     * Starting text column. This is the text column value 
     * of the first byte. The knowledge of the starting text column
     * is required in the case the first row is not full-lengthed.
     */
    private int startcol;
    
    /**
     * Current text column. The knowledge of the current text column
     * is requiredin the case the row is not full-lengthed, and it
     * needs to expanded with whitespaces to the full length,
     * as required by the PSPP's Portable file format specification.
     */
    private int col;
    
    /**
     * Row width 
     */
    private int row_length;

    /**
     * NumberParser is used internally to convert the ASCII base-30 
     * numbers into binary integers and floating points. 
     */
    private NumberParser numberParser;

    /**
     * If parsing a string cell, this is the number of bytes left 
     */
    private int stringleft;

    /** 
     * Verbatim copy of the current data cell's contents. 
     */
    private byte[] vbuffer;

    /**
     * Valid length of the vbuffer array 
     */
    private int vhead;

    /** 
     * Base index for vbuffer[] array. Used for various purposes. 
     */
    private int vbase;
    
    // VISITOR
    //=========
    
    /** 
     * Reference to the Visitor object.
     *
     * This member variable can be assumed to have a valid value only 
     * during the activation of 
     * {@link spssio.por.PORMatrix#accept(PORMatrixVisitor)}.
     */
    private PORMatrixVisitor visitor;
    
    // STATES
    //========
    
    private static final int S_ERROR                            = -1;
    private static final int S_START                            = 0;
    private static final int S_NEW_ROW                          = 1;
    private static final int S_NEW_COLUMN                       = 2;
    private static final int S_NUMERIC_EMPTY                    = 3;
    private static final int S_NUMERIC_UNEMPTY                  = 4;
    private static final int S_NUMERIC_READY                    = 5;
    private static final int S_STRLEN_READY                     = 6;
    private static final int S_SYSMISS_DUMMY                    = 7;
    private static final int S_SYSMISS_READY                    = 8;
    private static final int S_STRING_CONTENTS                  = 9;
    private static final int S_STRING_READY                     = 10;
    private static final int S_NEXT_COLUMN                      = 11;
    private static final int S_NEXT_ROW                         = 12;
    private static final int S_ACCEPT                           = 99;
    
    // CONSTRUCTORS
    //==============
    
    public PORMatrixParser(NumberParser numParser) {
        // Allocate a buffer to use as temporary space for the cell data.
        vbuffer = new byte[DEFAULT_CELL_BUFFER_SIZE];
        // Reset buffer pointers
        vhead = 0;
        vbase = 0;
        
        if (numParser == null) {
            // Create a default parser for base-30 numbers.
            numParser = new NumberParser(new NumberSystem(30, null));
        }
        
        numberParser = numParser;
        
        xdim = 0;
        ydim = 0;
        yoffset = null;
        xcur = 0;
        ycur = 0;
        xtype = null;
        
        // Reset parser state
        strerror = null;
        state = S_START;
        eps = false;
        
        startcol = 0;
        col = 0;
        row_length = DEFAULT_ROW_WIDTH;
        
        visitor = null;
    }
    
    public PORMatrixParser() {
        this(null); // Redirect the creation..
    } // ctor
    
    // OVERRIDE
    //==========
    
    /*
     * TODO: What should this method really do?
     */
    public void clear() {
        // Reset the SPSS/PSPP cell level data
        ydim = 0;
        xdim = 0;
        yoffset = null;
        xtype = null;
        
        // Remove the visitor
        visitor = null;
        
        restart();
    }
    
    public void restart() {
        // Reset cell buffer state
        vhead = 0;
        vbase = 0;
        
        // Reset parser state
        strerror = null;
        state = S_START;
        eps = false;
        
        // Reset current text column to the starting position.
        col = startcol;
        
        // Reset matrix data position
        xcur = 0;
        ycur = 0;
    }
    
    // CONFIGURATION
    //===============
    
    public void setTextColumn0(int cur_column) {
        startcol = cur_column;
        col = cur_column;
    }
    
    public void setTextRowLength(int len) {
        row_length = len;
    }
    
    public void setDataColumnTypes(int[] column_types) {
        // Set the x-coordinate types
        xtype = column_types;
        
        // Set the matrices x-dimension
        xdim = xtype.length;
    }
    
    public void setVisitor(PORMatrixVisitor visitor) {
        this.visitor = visitor;
    }
    
    // GET METHODS
    //=============
    
    public int getX() {
        return xcur;
    }
    
    public int getY() {
        return ycur;
    }
    
    public int getSizeX() {
        return xdim;
    }
    
    public int getSizeY() {
        return ydim;
    }
    
    public int[] getDataColumnTypes() {
        return xtype;
    }
    
    public int errno() {
        if (state == S_ACCEPT) {
            return E_ACCEPTED;
        }
        else if (state == S_ERROR) {
            return E_REJECTED;
        }
        // Unfinished
        return E_UNFINISHED;
    }
    
    public String strerror() {
        return strerror;
    }
    
    // OTHER METHODS
    //===============
    
    public void startMatrix() {
        emitStartMatrix();
        // Reset parser's state
    }
    
    public void endMatrix() {
        emitEndMatrix();
    }
    
    public int consume(int c) {
        if (c == '\n') {
            // LINE FEED.
            
            // Calculate the residual line length
            int skip = row_length-col;
            
            // Fill the end of the current row with whitespaces
            for (int i = 0; i < skip; i++) {
                eat(PORConstants.WHITESPACE);
            } // for: skip
            
            // Reset column number
            col = 0;
        } 
        else if (c == '\r') {
            // CARRIAGE RETURN.
            // This is just simply ignored
        }
        else {
            // In any other case, the character is forwarded
            // without modifications.
            eat(c);
            
            // Increase column number
            col++;
        }
        
        return errno();
    } // consume()
    
    private void eat(int c) {
        int errno;
        // TODO, expand newlines automatically to row length
        
        // Write cell data
        if (vhead < vbuffer.length) {
            vbuffer[vhead++] = (byte) c;
        } else {
            // TODO: Error. Cell buffer overflow.
            System.out.printf("buffer overflow vhead=%d\n", vhead);
        }
        
        // Loop while null-transitions are being carried out
        do {
            
            // Reset null-transition flag
            eps = false;
            
            switch(state) {
                case S_START:
                    state = S_NEW_ROW;
                    eps = true;
                    break;
                
                case S_NEW_ROW:
                    // allow end-of-data sign
                    eps = true;
                    if (c == PORConstants.EOF_MARKER) { // 'Z'
                        // Pick up the data matrix y size
                        ydim = ycur;
                        // Accept; further input is ignored.
                        state = S_ACCEPT;
                    } else {
                        // Signal beginning of a new row
                        emitStartRow();
                        
                        /*
                        // Record offset
                        if (yoffset != null) {
                            yoffset[ycur] = pos();
                        }
                        */
                        
                        // No 'Z' found. The c has to be data.
                        state = S_NEW_COLUMN;
                    }
                    break;

                case S_NEW_COLUMN:
                    // Reset number parser
                    numberParser.reset();
                
                    // Reset vbuffer writing position and base index
                    vhead = 0;
                    // Reset practically destroyed the vbuffer contents,
                    // so the current char needs to be appended again.
                    // However, this time there's space left for sure
                    vbuffer[vhead++] = (byte) c;
                
                    // Regardless of the cell type, 
                    // it should begin with a numeric.
                    state = S_NUMERIC_EMPTY;
                    eps = true;
                    break;
                
                case S_NUMERIC_EMPTY:
                    // Skip leading spaces
                    if (c == PORConstants.WHITESPACE) { // ' '
                        // Whitespaces are consumed silently.
                        // Reset writing position
                        vhead = 0;
                        // TODO:
                        // Ignorable whitespaces are unfortunate,
                        // since they render the data unreproducible.
                        // This event should be signaled somehow.
                    }
                    else if (c == PORConstants.SYSMISS_MARKER) { // '*'
                        // System missing value. 
                        // This is allowed only for NUMERIC data type.
                        if (xtype[xcur] != PORValue.TYPE_NUMERIC) {
                            // ERROR. Unallowed value.
                            strerror = String.format("Cell has STRING type; SYSMISS is not allowed as string length");
                            state = S_ERROR;
                        } else {
                            state = S_SYSMISS_DUMMY;
                            vbase = 0;
                        }
                    } else if (c == PORConstants.NUMBER_SEPARATOR) { // '/'
                        // ERROR. Empty numbers are not allowed.
                        strerror = String.format("A number must have at least one digit");
                        state = S_ERROR;
                    } else {
                        state = S_NUMERIC_UNEMPTY;
                        eps = true;
                    }
                    break;
                    
                case S_NUMERIC_UNEMPTY:
                    if (c != PORConstants.NUMBER_SEPARATOR) { // '/'
                        // Send to parser
                        numberParser.consume(c);
                    }
                    else {
                        // Switch immediately
                        eps = true;
                        // Pick the date type to a local var for convenience.
                        int ctype = xtype[xcur];
                        // Examine the data type
                        if (ctype == PORValue.TYPE_NUMERIC) {
                            state = S_NUMERIC_READY;
                        } else if (ctype == PORValue.TYPE_STRING) {
                            state = S_STRLEN_READY;
                        } else {
                            // ERROR. Unknown data type
                            strerror = String.format("Column has unknown data type (likely a programming error)");
                            state = S_ERROR;
                        } // if-else
                    } // if-else
                    break;
                    
                case S_NUMERIC_READY:
                    // Discard the ending slash
                    vhead--;
                    // send end-of-data
                    errno = numberParser.consume(-1);
                    // Examine number parser status
                    if (errno == NumberParser.E_OK) {
                        // Number ok. The finishing slash is not counted,
                        // and therefore vhead is minus one.
                        emitNumberValue();
                        
                        // Next column.
                        state = S_NEXT_COLUMN;
                    } else {
                        if (errno == NumberParser.E_OVERFLOW) {
                            // These are emitted to the handler,
                            // who can decide how to deal with these
                            emitNumberError();
                        } else if (errno == NumberParser.E_UNDERFLOW) {
                            // These are emitted to the handler,
                            // who can decide how to deal with these
                            emitNumberError();
                        } else {
                            // ERROR. Invalid number
                            strerror = String.format("Number parsing failed: %s\n", numberParser.strerror());
                            state = S_ERROR;
                        }
                    } // if-else
                    break;
                    
                case S_SYSMISS_DUMMY:
                    // Accept any character
                    state = S_SYSMISS_READY;
                    eps = true;
                    break;

                case S_SYSMISS_READY:
                    // Any character is accepted after the asterisk.
                    // Signal SYSMISS number
                    emitNumberSysmiss();
                
                    state = S_NEXT_COLUMN;
                    break;
                    
                case S_STRLEN_READY:
                    // The current char is '/'. Mark the next position
                    // (which is actually current vhead) as vbase.
                
                    // vhead points now to the array location where 
                    // the first actual content character will come.
                    // The position is recorded as the base offset
                    vbase = vhead;
                
                    // Reset writing head position
                    //vhead = 0;
                    
                    // Send end-of-data and pick errno.
                    errno = numberParser.consume(-1);
                
                    // This time errors are not tolerated
                    if (errno != NumberParser.E_OK) {
                        // ERROR. String length parsing failed
                        strerror = String.format("String length parsing failed: %s\n", numberParser.strerror());
                        state = S_ERROR;
                    } else {
                        // validate string length
                        stringleft = (int) numberParser.lastvalue();
                        if ((double) stringleft != numberParser.lastvalue()) {
                            // ERROR. String length is not an integer.
                            strerror = String.format("String length has non-integer value: %g", numberParser.lastvalue());
                            state = S_ERROR;
                        } else if (stringleft <= 0) {
                            // ERROR. String length is non-positive.
                            strerror = String.format("String length has non-positive value: %d", stringleft);
                            state = S_ERROR;
                        } else if (stringleft >= 256) {
                            // ERROR. String length is too long.
                            strerror = String.format("String length is too long: %d", stringleft);
                            state = S_ERROR;
                        } else {
                            // String length validated.
                            state = S_STRING_CONTENTS;
                        } // if-else: number valid for string's length?
                    } // if-else: valid number?
                    break;
                    
                case S_STRING_CONTENTS:
                    // Decrease the number of chars left to read
                    stringleft--;
                    if (stringleft == 0) {
                        // String finished
                        state = S_STRING_READY;
                        eps = true;
                    }
                    break;

                    
                case S_STRING_READY:
                    // Emit the string
                    emitStringValue();
                
                    // To the next column
                    state = S_NEXT_COLUMN;
                    break;
                    
                case S_NEXT_COLUMN:
                    // Switch immediately to the subsequent state
                    eps = true;
                    // Increase x-coordinate
                    xcur++;
                    // If the row became full, switch to next row.
                    if (xcur == xdim) {
                        state = S_NEXT_ROW;
                    } else {
                        state = S_NEW_COLUMN;
                    }
                    break;
                    
                case S_NEXT_ROW:
                    // Emit ending of the row
                    emitEndRow();
                    // Increase the y-coordinate
                    ycur++;
                    // Reset the x-coordinate
                    xcur = 0;
                    // Switch to the next state; this allows end-of-data tag.
                    state = S_NEW_ROW;
                    eps = true;
                    break;
                
                case S_ERROR:
                    // Loop quits immediately
                    break;
                
                case S_ACCEPT:
                    // Loop quits immediately
                    break;
                
            } // switch state
        } while ((eps == true) && (state != S_ACCEPT) && (state != S_ERROR));
    } // consume()
    
    
    public void debug() {
        /*
        System.out.printf("Ending state: %d\n", state);
        if (state == S_ACCEPT) {
            System.out.printf("Input is ACCEPTED.\n");
        } else if (state == S_ERROR) {
            System.out.printf("Input is REJECTED.\n");
            System.out.printf("error: %s\n", strerror);
        } else {
            System.out.printf("Input is UNFINISHED.\n");
        }
        
        System.out.printf("xdim: %d\n", xdim);
        System.out.printf("xcur: %d, ycur: %d\n", xcur, ycur);
        */
    }
    
    /**
     * Emits (ycur, xcur, vbuffer, vbase, vhead, null)
     */
    protected void emitNumberSysmiss() {
        if (visitor != null) {
            visitor.columnSysmiss(xcur, vbuffer, vhead);
        }
    }
    
    protected void emitNumberValue() {
        if (visitor != null) {
            visitor.columnNumeric(
                xcur, vbuffer, vhead, numberParser.lastvalue());
        }
    }
    
    protected void emitNumberError() {
        if (visitor != null) {
            // TODO
        }
    }

    protected void emitStringValue() {
        if (visitor != null) {
            visitor.columnString(xcur, vbuffer, vbase, vhead);
        }
    }
    
    protected void emitStartRow() {
        if (visitor != null) {
            visitor.rowBegin(ycur);
        }
    }
    
    protected void emitEndRow() {
        if (visitor != null) {
            visitor.rowEnd(ycur);
        }
    }
    
    protected void emitStartMatrix() {
        if (visitor != null) {
            visitor.matrixBegin(xdim, ydim, xtype);
        }
    }
    
    protected void emitEndMatrix() {
        if (visitor != null) {
            visitor.matrixEnd();
        }
    }
} // PORMatrixParser
