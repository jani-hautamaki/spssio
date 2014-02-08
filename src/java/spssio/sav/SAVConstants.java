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

package spssio.sav;


/**
 * Compression codes, see
 * {@url http://www.gnu.org/software/pspp/pspp-dev/html_node/Data-Record.html#Data-Record}
 *
 *
 * Why SAVEndianness constants are in their own file 
 * while the compression codes are not? This is illogical.
 *
 */
public class SAVConstants {
    
    // CONSTANTS
    //===========
    
    public static final long VALUE_HIGHEST_RAW          = 0x7fefffffffffffffL;
    public static final long VALUE_LOWEST_RAW            = 0xffeffffffffffffeL;
    public static final long VALUE_SYSMISS_RAW          = 0xffefffffffffffffL;
    
    public static final double VALUE_HIGHEST
        = Double.longBitsToDouble(VALUE_HIGHEST_RAW);

    public static final double VALUE_LOWEST
        = Double.longBitsToDouble(VALUE_LOWEST_RAW);

    public static final double VALUE_SYSMISS
        = Double.longBitsToDouble(VALUE_SYSMISS_RAW);
    
    // CONSTANTS: COMPRESSION
    //========================

    /** No-operation. This is simply ignored. */
    public static final int CBYTE_NOP                  = 0;
    
    // Compressed number. Expand to an 8-byte floating-point number
    // with value {@code code - bias}.
    //                                             VALUES 1..251
    
    /** End-of-file. */
    public static final int CBYTE_EOF                  = 252;
    /** Verbatim raw data. Read an 8-byte segment of raw data. */
    public static final int CBYTE_RAW_DATA             = 253;
    /** Compressed whitespaces. Expand to an 8-byte segment of whitespaces. */
    public static final int CBYTE_WHITESPACES          = 254;
    /** Compressed sysmiss value. Expand to an 8-byte segment of SYSMISS value. */
    public static final int CBYTE_SYSMISS              = 255;
   
    // CONSTRUCTORS
    //==============
    
    /**
     * Intentionally disabled
     */
    private SAVConstants() {
    }
    
    // OTHER METHODS
    //===============
    
} // class SAVConstants