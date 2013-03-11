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

package spssio.por;

// spssio utils
import spssio.util.SequentialByteArray;
import spssio.util.NumberParser;
import spssio.util.NumberSystem;


/**
 * The data matrix of a Portable file.<p>
 *
 * It is not a wise idea to convert the data values of a Portable file into
 * into {@code String} and {@code Double} objects. The additional memory 
 * overhead caused by Java for any {@code Object} is very in terms of both
 * memory (vtable+gc) and time (gc).<p>
 * 
 * For instance, a portable file of 20 MBs would need  more than 1024 MBs
 * of memory before Java would be able to convert it into {@code Double}s
 * and {code String}s. 20 MBs shouldn't be that much of trouble nowaways,
 * when computers have, like, at least 2048 MBs of RAM memory by default.
 * That's Java for you.<p>
 * 
 */
public class PORMatrix
    extends SequentialByteArray
{
    // CONSTANTS
    //===========
    
    public static final int DEFAULT_ROW_WIDTH                   = 80;
    public static final int DEFAULT_CELL_BUFFER_SIZE            = 4096;
    
    public static final int CHAR_WHITESPACE                     = ' ';
    public static final int CHAR_ASTERISK                       = '*';
    public static final int CHAR_SLASH                          = '/';
    
    // Status codes
    
    public static final int E_REJECTED                          = -1;
    public static final int E_UNFINISHED                        = 0;
    public static final int E_ACCEPTED                          = 1;
    
    
    // ERROR MANAGEMENT
    //==================
    
    private String strerror;
    
    // STATE MACHINE
    //===============
    
    /** Current state */
    private int state;
    
    /** Null-transition flag */
    private boolean eps;
    
    
    
    // SPSS/PSPP cell-level
    //======================
    
    /** Number of columns */
    private int xdim;

    /** Number of rows */
    private int ydim;

    /** Column data type configuration */
    private int[] xtype;

    /** Cell offsets in row-major order. */
    private int[] yoffset;
    
    /** Current column */
    private int xcur;
    
    /** Current row */
    private int ycur;
    
    
    // PARSING
    //=========
    
    /** Text column (in the file) of the first byte */
    private int startcol;
    
    /** Current text column */
    private int col;
    
    /** Row width */
    private int row_length;


    /**
     * NumberParser is used internally to convert the ASCII base-30 numbers
     * into binary integers and floating points. 
     */
    private NumberParser num_parser;

    /** If string cell, this is the number of bytes left */
    private int stringleft;

    /** Verbatim copy of the current data cell's contents. */
    private byte[] vbuffer;

    /** Valid length of the vbuffer array */
    private int vhead;

    /** Base index for vbuffer[] array. Used for various purposes. */
    private int vbase;
    
    // VISITOR
    //=========
    
    /** 
     * Reference to the Visitor object.
      *
     * This member variable can be assumed to have a valid value only 
     * during the activation of {@link #visit(PORMatrixVisitor)}.
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
    
    public PORMatrix() {
        super();
        
        xdim = 0;
        ydim = 0;
        yoffset = null;
        xcur = 0;
        ycur = 0;
        xtype = null;
        
        state = S_START;
        eps = false;
        
        startcol = 0;
        col = 0;
        row_length = DEFAULT_ROW_WIDTH;
        
        vbuffer = new byte[DEFAULT_CELL_BUFFER_SIZE];
        vhead = 0;
        vbase = 0;
        
        visitor = null;
        num_parser = new NumberParser(new NumberSystem(30, null));
        strerror = null;
    } // ctor
    
    // OVERRIDE
    //==========
    
    
    @Override
    public void allocate(int size_bytes) {
        super.allocate(size_bytes);
        
        // In addition, reset the SPSS/PSPP cell level data
        ydim = 0;
        xdim = 0;
        yoffset = null;
        xtype = null;
        xcur = 0;
        ycur = 0;
        
        // Reset cell buffer state
        vhead = 0;
        vbase = 0;
        
        // Reset parser
        state = S_START;
        eps = false;
        
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
    
    // GET METHODS
    //=============
    
    public int getx() {
        return xcur;
    }
    
    public int gety() {
        return ycur;
    }
    
    public int sizex() {
        return xdim;
    }
    
    public int sizey() {
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
    
    // VISITOR
    //=========
    
    /**
     * Visits all data cells in the matrix in row-major order.
     *
     * @param visitor The visitor.
     */
    public void visit(PORMatrixVisitor visitor) {
        // Seek to the beginning of the array
        seek(0);
        
        // Reset SPSS/PSPP cell address
        xcur = 0;
        ycur = 0;
        
        // Reset current cell buffer head position
        vhead = 0;
        vbase = 0;
        
        // Reset parser state
        // Reset parser
        state = S_START;
        eps = false;
        
        
        // Reset text column position
        col = startcol;
        
        // Set visitor
        this.visitor = visitor;
        
        // Signal the beginning of the matrix
        emit_matrix_begin();
        
        // Parse the whole matrix
        int c;
        while ((c = read()) != -1) {
            consume(c);
        } // while
        
        // Signal the end of the matrix
        emit_matrix_end();
        
        // Unset visitor
        visitor = null;
        
    } // visit()
    
    
    /*
    public void computeOffsets() {
    }
    */
    
    // OTHER METHODS
    //===============
    
    public int consume(int c) {
        if (c == '\n') {
            // LINE FEED.
            
            // Calculate the residual line length
            int skip = row_length-col;
            
            // Fill the end of the current row with whitespaces
            for (int i = 0; i < skip; i++) {
                eat(CHAR_WHITESPACE);
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
        
        
        do {
            
            eps = false;
            switch(state) {
                case S_START:
                    state = S_NEW_ROW;
                    eps = true;
                    break;
                
                case S_NEW_ROW:
                    // allow end-of-data sign
                    eps = true;
                    if (c == 'Z') {
                        // Pick up the data matrix y size
                        ydim = ycur;
                        // Accept; further input is ignored.
                        state = S_ACCEPT;
                    } else {
                        // Signal beginning of a new row
                        emit_row_begin();
                        
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
                    num_parser.reset();
                
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
                    if (c == CHAR_WHITESPACE) { // ' '
                        // Whitespaces are consumed silently.
                        // Reset writing position
                        vhead = 0;
                        // TODO:
                        // Ignorable whitespaces are unfortunate,
                        // since they render the data unreproducible.
                        // This event should be signaled somehow.
                    }
                    else if (c == CHAR_ASTERISK) { // '*'
                        // System missing value. 
                        // This is allowed only for NUMERIC data type.
                        if (xtype[xcur] != PORValue.TYPE_NUMERIC) {
                            // ERROR. Unallowed value.
                            strerror = String.format("Cell has STRING type; SYSMISS is not allowed as string length");
                            state = S_ERROR;
                        } else {
                            state = S_SYSMISS_DUMMY;
                            vbase = vhead-1;
                        }
                    } else if (c == CHAR_SLASH) { // '/'
                        // ERROR. Empty numbers are not allowed.
                        strerror = String.format("A number must have at least one digit");
                        state = S_ERROR;
                    } else {
                        vbase = vhead-1;
                        state = S_NUMERIC_UNEMPTY;
                        eps = true;
                    }
                    break;
                    
                case S_NUMERIC_UNEMPTY:
                    if (c != CHAR_SLASH) { // '/'
                        // Send to parser
                        num_parser.consume(c);
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
                    errno = num_parser.consume(-1);
                    // Examine number parser status
                    if (errno == NumberParser.E_OK) {
                        // Number ok. The finishing slash is not counted,
                        // and therefore vhead is minus one.
                        emit_numeric();
                        
                        // Next column.
                        state = S_NEXT_COLUMN;
                    } else {
                        if (errno == NumberParser.E_OVERFLOW) {
                            // TODO
                            emit_sysmiss();
                        } else if (errno == NumberParser.E_UNDERFLOW) {
                            // TODO
                            emit_sysmiss();
                        } else {
                            // ERROR. Invalid number
                            strerror = String.format("Number parsing failed: %s\n", num_parser.strerror());
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
                    // Signal event
                    emit_sysmiss();
                
                    state = S_NEXT_COLUMN;
                    break;
                    
                case S_STRLEN_READY:
                    // The current char is '/'. Mark the next position
                    // (which is actually current vhead) as vbase.
                
                    //vbase = vhead; // TODO: Make this into an option?
                
                    // Reset writing head position
                    vhead = 0;
                    
                    // Send end-of-data and pick errno.
                    errno = num_parser.consume(-1);
                
                    // This time errors are not tolerated
                    if (errno != NumberParser.E_OK) {
                        // ERROR. String length parsing failed
                        strerror = String.format("String length parsing failed: %s\n", num_parser.strerror());
                        state = S_ERROR;
                    } else {
                        // validate string length
                        stringleft = (int) num_parser.lastvalue();
                        if ((double) stringleft != num_parser.lastvalue()) {
                            // ERROR. String length is not an integer.
                            strerror = String.format("String length has non-integer value: %g", num_parser.lastvalue());
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
                    // emit the string
                    emit_string();
                
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
                    emit_row_end();
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
    protected void emit_sysmiss() {
        if (visitor != null) {
            visitor.columnSysmiss(xcur, vhead, vbuffer);
        }
    } // emit_sysmiss()
    
    protected void emit_numeric() {
        if (visitor != null) {
            visitor.columnNumeric(
                xcur, vhead, vbuffer, num_parser.lastvalue());
        }
    } // emit_numeric()

    protected void emit_string() {
        if (visitor != null) {
            visitor.columnString(xcur, vhead, vbuffer);
        }
    }
    
    protected void emit_row_begin() {
        if (visitor != null) {
            visitor.rowBegin(ycur);
        }
    }
    
    protected void emit_row_end() {
        if (visitor != null) {
            visitor.rowEnd(ycur);
        }
    }
    
    protected void emit_matrix_begin() {
        if (visitor != null) {
            visitor.matrixBegin(xdim, ydim, xtype);
        }
    }
    
    protected void emit_matrix_end() {
        if (visitor != null) {
            visitor.matrixEnd();
        }
    }
} // PORMatrix
