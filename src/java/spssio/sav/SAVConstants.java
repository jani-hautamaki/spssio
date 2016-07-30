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



package spssio.sav;


import spssio.util.DataEndianness;

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

    public static final String
        FORMAT_SIGNATURE                                = "$FL2";

    public static final String
        SOFTWARE_PREFIX                                 = "@(#) SPSS DATA FILE";

    // DEFAULTS
    //==========

    public static final long
        DEFAULT_HIGHEST_VALUE_RAW                       = 0x7fefffffffffffffL;

    public static final long
        DEFAULT_LOWEST_VALUE_RAW                        = 0xffeffffffffffffeL;

    public static final long
        DEFAULT_SYSMISS_VALUE_RAW                       = 0xffefffffffffffffL;


    public static final double
        DEFAULT_HIGHEST_VALUE                           = Double.longBitsToDouble(DEFAULT_HIGHEST_VALUE_RAW);

    public static final double
        DEFAULT_LOWEST_VALUE                            = Double.longBitsToDouble(DEFAULT_LOWEST_VALUE_RAW);

    public static final double
        DEFAULT_SYSMISS_VALUE                           = Double.longBitsToDouble(DEFAULT_SYSMISS_VALUE_RAW);

    public static final String
        DEFAULT_STRING_ENCODING                         = "ISO-8859-15";

    public static final int
        DEFAULT_ENDIANNESS                              = DataEndianness.LITTLE_ENDIAN;



    public static final double
        DEFAULT_COMPRESSION_BIAS                        = 100.0;

    // For populating SAVHeader

    // TODO: Retrieve package revision from somewhere
    public static final String
        DEFAULT_SOFTWARE                                = SOFTWARE_PREFIX + " spssio/Java";

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
     * Intentionally disabled.
     *
     */
    private SAVConstants() {
    }

    // OTHER METHODS
    //===============

} // class SAVConstants