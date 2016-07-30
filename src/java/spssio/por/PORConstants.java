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



package spssio.por;

/**
 * Parameters affecting the deserialization and serialization
 * of SPSS/PSPP Portable files.
 *
 */
public class PORConstants
{

    // CONSTANTS
    //===========

    /**
     * Text row width for Portable files (bytes).
     * The row length is used during both reading and writing of
     * a Portable file. During reading the lines having length less
     * than this value are expanded with whitespaces to the specified
     * length. During writing the lines wrapped at the specified length.
     */
    public static final int ROW_LENGTH              = 80;


    /**
     * Maximum length of a data element (bytes).
     * A data element is either a single number field or string contents.
     * TODO: Where is this value used? The description doesn't reveal.
     */
    public static final int MAX_DATA_ELEMENT_LENGTH = 1024;

    /**
     * Maximum length of a string (bytes).
     */
    public static final int MAX_STRING_LENGTH       = 255;



    // THESE ARE TODO (Note: they dont have "final" qualifier)

    /**
     * Software maximum length.
     */
    public static int MAX_SOFTWARE_LENGTH           = 255;


    /**
     * Author maximum length.
     */
    public static int MAX_AUTHOR_LENGTH             = 255;

    /**
     * Title maximum length.
     */
    public static int MAX_TITLE_LENGTH              = 255;

    /**
     * Variable name maximum length.
     */
    public static int MAX_VARNAME_LENGTH            = 8;

    /**
     * Variable label maximum length.
     */
    public static int MAX_VARLABEL_LENGTH           = 255;


    /**
     * The total length of splash strings.
     */
    public static final int SPLASH_LENGTH           = 200;

    /**
     * The character-to-byte translation table length.
     */
    public static final int TRANSLATION_LENGTH      = 256;

    /**
     * The signature string length.
     */
    public static final int SIGNATURE_LENGTH        = 8;

    /**
     * The creation date string length.
     */
    public static final int DATE_LENGTH             = 8;

    /**
     * The creation time string length.
     */
    public static final int TIME_LENGTH             = 6;



    /**
     * File format signature
     */
    public static final String FORMAT_SIGNATURE     = "SPSSPORT";

    /**
     * File format version
     */
    public static final int FORMAT_VERSION          = 'A';

    /**
     * Whitespace character.<p>
     *
     * This character is used for two different purposes:
     * a) to fill incomplete lines to the required length; and
     * b) to prepend numbers with ignorable content.
     *
     */
    public static final int WHITESPACE              = ' ';

    /**
     * Number separator.
     */
    public static final int NUMBER_SEPARATOR        = '/';

    /**
     * SYSMISS missing value marker.
     */
    public static final int SYSMISS_MARKER          = '*';

    /**
     * Separator after SYSMISS missing value.
     */
    public static final int SYSMISS_SEPARATOR       = '.';

    /**
     * End-of-File marker.<p>
     *
     * This character must be different than any base-30 digit
     * or any tag code.
     */
    public static final int EOF_MARKER              = 'Z';

    // DEFAULT SPLASH STRINGS
    //========================

    // i=0; "-+++@@+@++@@@@@@@@@@@@@@@@@@@@          " (EBCDIC)
    public static final int[] SPLASH1_EBCDIC = {
        0xc1, 0xe2, 0xc3, 0xc9, 0xc9, 0x40, 0xe2, 0xd7, 0xe2, 0xe2,
        0x40, 0xd7, 0xd6, 0xd9, 0xe3, 0x40, 0xc6, 0xc9, 0xd3, 0xc5,
        0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40,
        0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40
    };

    // i=1; "ASCII SPSS PORT FILE                    " (7-bit ASCII)
    public static final int[] SPLASH2_ASCII_7BIT = {
        0x41, 0x53, 0x43, 0x49, 0x49, 0x20, 0x53, 0x50, 0x53, 0x53,
        0x20, 0x50, 0x4f, 0x52, 0x54, 0x20, 0x46, 0x49, 0x4c, 0x45,
        0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20,
        0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20
    };

    // i=2; "00000-0000-0000-0000--------------------" (CDC 6-bit ASCII)
    public static final int[] SPLASH3_CDC_ASCII_6BIT = {
        0x30, 0x30, 0x30, 0x30, 0x30, 0x2d, 0x30, 0x30, 0x30, 0x30,
        0x2d, 0x30, 0x30, 0x30, 0x30, 0x2d, 0x30, 0x30, 0x30, 0x30,
        0x2d, 0x2d, 0x2d, 0x2d, 0x2d, 0x2d, 0x2d, 0x2d, 0x2d, 0x2d,
        0x2d, 0x2d, 0x2d, 0x2d, 0x2d, 0x2d, 0x2d, 0x2d, 0x2d, 0x2d
    };

    // i=3; "!3#))0303300/240&),%00000000000000000000" (6-bit ASCII)
    public static final int[] SPLASH4_ASCII_6BIT = {
        0x21, 0x33, 0x23, 0x29, 0x29, 0x30, 0x33, 0x30, 0x33, 0x33,
        0x30, 0x30, 0x2f, 0x32, 0x34, 0x30, 0x26, 0x29, 0x2c, 0x25,
        0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30,
        0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30
    };

    // i=4; "0200002'220'&)3000#000000000000000000000" (Honeywell 6-bit ASCII)
    public static final int[] SPLASH5_HONEYWELL_ASCII_6BIT = {
        0x30, 0x32, 0x30, 0x30, 0x30, 0x30, 0x32, 0x27, 0x32, 0x32,
        0x30, 0x27, 0x26, 0x29, 0x33, 0x30, 0x30, 0x30, 0x23, 0x30,
        0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30,
        0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30
    };

    public static String[] DEFAULT_SPLASHES = {
        new String(SPLASH1_EBCDIC, 0, 40),
        new String(SPLASH2_ASCII_7BIT, 0, 40),
        new String(SPLASH3_CDC_ASCII_6BIT, 0, 40),
        new String(SPLASH4_ASCII_6BIT, 0, 40),
        new String(SPLASH5_HONEYWELL_ASCII_6BIT, 0, 40),
    };


    public static final byte[] DEFAULT_TRANSLATION = {
        '0', '0', '0', '0', '0', '0', '0', '0',
        '0', '0', '0', '0', '0', '0', '0', '0',
        '0', '0', '0', '0', '0', '0', '0', '0',
        '0', '0', '0', '0', '0', '0', '0', '0',
        '0', '0', '0', '0', '0', '0', '0', '0',
        '0', '0', '0', '0', '0', '0', '0', '0',
        '0', '0', '0', '0', '0', '0', '0', '0',
        '0', '0', '0', '0', '0', '0', '0', '0',
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F',
        'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
        'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V',
        'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd',
        'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
        'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
        'u', 'v', 'w', 'x', 'y', 'z', ' ', '.',
        '<', '(', '+', '0', '&', '[', ']', '!',
        '$', '*', ')', ';', '^', '-', '/', '|',
        ',', '%', '_', '>', '?', '`', ':', '#',
        '@', '\'','=', '\"','0', '0', '0', '0',
        '0', '0', '~', '0', '0', '0', '0', '0',
        '0', '0', '0', '0', '0', '0', '0', '0',
        '0', '0', '0', '0', '0', '0', '0', '0',
        '{', '}', '\\','0', '0', '0', '0', '0',
        '0', '0', '0', '0', '0', '0', '0', '0',
        '0', '0', '0', '0', '0', '0', '0', '0',
        '0', '0', '0', '0', '0', '0', '0', '0',
        '0', '0', '0', '0', '0', '0', '0', '0',
        '0', '0', '0', '0', '0', '0', '0', '0',
        '0', '0', '0', '0', '0', '0', '0', '0',
        '0', '0', '0', '0', '0', '0', '0', '0',
        '0', '0', '0', '0', '0', '0', '0', '0'
    };

    // CONSTRUCTORS
    //==============

    /**
     * Constructor is intentionally disabled.
     */
    private PORConstants() {
    } // ctor

} // class PORConstants