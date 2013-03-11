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
    
    /** Number of rows */
    private int ydim;

    /** Number of columns */
    private int xdim;
    
    /** Cell offsets in row-major order. */
    private int[] yoffset;

    /** Column data type configuration */
    private int[] xtype;
    
    /** Current column */
    private int xcur;
    
    /** Current row */
    private int ycur;
    
    
    // PARSING
    //=========
    
    /** Text column (in the file) of the byte */
    private int col0;
    
    /** Current text column */
    private int col;
    
    /** Row width */
    private int row_length;


    /**
     * NumberParser is used internally to convert the ASCII base-30 number
     * into binary integers and floating points. 
     */
    private NumberParser num_parser;

    /** If string cell, this is the number of bytes left */
    private int strend;


    /** Verbatim copy of the current data cell's contents. */
    private byte[] ccell;

    /** Valid length of the ccell array */
    private int clen;

    /** 
     * Base index for ccell[] array. This is used to mark the beginning
     * the beginning of the actual contents. For numeric cells, this marks
     * the position where leading whitespaces end. For textual cells, this
     * marks the position of the slash.
     */
    private int cbi;
    
    
    
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
        
        col0 = 0;
        col = 0;
        row_length = DEFAULT_ROW_WIDTH;
        
        ccell = new byte[DEFAULT_CELL_BUFFER_SIZE];
        clen = 0;
        cbi = 0;
        
        num_parser = new NumberParser(new NumberSystem(30, null));
        strerror = null;
    } // ctor
    
    // CONFIGURATION
    //===============
    
    public void setTextColumn0(int cur_column) {
        col0 = cur_column;
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
        if (clen < ccell.length) {
            ccell[clen++] = (byte) c;
        } else {
            // TODO: Error. Cell buffer overflow.
            System.out.printf("buffer overflow\n");
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
                        /*
                        // Record offset
                        if (yoffset != null) {
                            yoffset[ycur] = pos();
                        }
                        */
                        // Otherwise, it has to be data.
                        state = S_NEW_COLUMN;
                    }
                    break;

                case S_NEW_COLUMN:
                    // Reset number parser
                    num_parser.reset();
                
                    // Reset ccell writing position and base index
                    clen = 0;
                    // Reset practically destroyed the ccell contents,
                    // so the current char needs to be appended again.
                    // However, this time there's space left for sure
                    ccell[clen++] = (byte) c;
                
                    // Regardless of the cell type, 
                    // it should begin with a numeric.
                    state = S_NUMERIC_EMPTY;
                    eps = true;
                    break;
                
                case S_NUMERIC_EMPTY:
                    // Skip leading spaces
                    if (c == CHAR_WHITESPACE) { // ' '
                        // Consumed
                        //System.out.printf("Surprising whitespace met\n");
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
                            cbi = clen-1;
                        }
                    } else if (c == CHAR_SLASH) { // '/'
                        // ERROR. Empty numbers are not allowed.
                        strerror = String.format("A number must have at least one digit");
                        state = S_ERROR;
                    } else {
                        cbi = clen-1;
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
                    clen--;
                    // send end-of-data
                    errno = num_parser.consume(-1);
                    // Examine number parser status
                    if (errno == NumberParser.E_OK) {
                        // Number ok. The finishing slash is not counted,
                        // and therefore clen is minus one.
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
                    // (which is actually current clen) as cbi.
                    cbi = clen;
                    
                    // Send end-of-data and pick errno.
                    errno = num_parser.consume(-1);
                
                    // This time errors are not tolerated
                    if (errno != NumberParser.E_OK) {
                        // ERROR. String length parsing failed
                        strerror = String.format("String length parsing failed: %s\n", num_parser.strerror());
                        state = S_ERROR;
                    } else {
                        // validate string length
                        strend = (int) num_parser.lastvalue();
                        if ((double) strend != num_parser.lastvalue()) {
                            // ERROR. String length is not an integer.
                            strerror = String.format("String length has non-integer value: %g", num_parser.lastvalue());
                            state = S_ERROR;
                        } else if (strend <= 0) {
                            // ERROR. String length is non-positive.
                            strerror = String.format("String length has non-positive value: %d", strend);
                            state = S_ERROR;
                        } else if (strend >= 256) {
                            // ERROR. String length is too long.
                            strerror = String.format("String length is too long: %d", strend);
                            state = S_ERROR;
                        } else {
                            // String length validated.
                            state = S_STRING_CONTENTS;
                        } // if-else: number valid for string's length?
                    } // if-else: valid number?
                    break;
                    
                case S_STRING_CONTENTS:
                    // Decrease the number of chars left to read
                    strend--;
                    if (strend == 0) {
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
                    // Increase the y-coordinate
                    ycur++;
                    // Reset the x-coordinate
                    xcur = 0;
                    // emit new row
                    emit_row();
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
    }
    
    
    /**
     * Emits (ycur, xcur, ccell, cbi, clen, null)
     */
    protected void emit_sysmiss() {
        /*
        if (xcur > 0) {
            System.out.printf(",");
        }
        */
    }
    
    /**
     * Emits (ycur, xcur, ccell, cbi, clen, num_parser.lastvalue()).
     * if value is null, this is "sysmiss"
     *
     */
    protected void emit_numeric() {
        /*
        if (xcur > 0) {
            System.out.printf(",");
        }
        System.out.printf("%s", new String(ccell, cbi, clen-cbi));
        //System.out.printf("%f", num_parser.lastvalue());
        */
    } // emit_numeric()

    /**
     * Emits (ycur, xcur, ccell, cbi, clen)
     */
    protected void emit_string() {
        /*
        if (xcur > 0) {
            System.out.printf(",");
        }
        System.out.printf("\"%s\"", new String(ccell, cbi, clen-cbi));
        */
    } // emit_string()
    
    protected void emit_row() {
        //System.out.printf("\n");
    }
    
} // PORMatrix
