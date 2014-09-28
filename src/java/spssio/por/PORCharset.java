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


package spssio.por;


/**
 * Character translation tables for portable files.<p>
 *
 * According to PSPP's documentation
 * <a href="http://www.gnu.org/software/pspp/pspp-dev/html_node/Portable-File-Characters.html#Portable-File-Characters">
 *   A.1 Portable File Characters
 * </a>
 * and
 * <a href="http://www.gnu.org/software/pspp/pspp-dev/html_node/Portable-File-Header.html#Portable-File-Header">
 *   A.3 Portable File Header
 * </a>:<p>
 *
 * "The file contents may be in any character set; the file contains
 *  a description of its own character set [...] Therefore, the `Z' character
 * is not necessarily an ASCII `Z'."<p>
 *
 * "Symbols that are not defined in a particular character set are set to
 * the same value as symbol 64; i.e., to `0'."<p>
 *
 * The constant array {@link #DEFAULT_MAPPING} contains the following table
 * which has been pulled out from the PSPP's documentation:<p>
 *
 * <code><pre>
 *  0-60     Control characters. Not important enough to describe in full here.
 *  61-63    Reserved
 *  64-73    Digits `0` through `9`
 *  74-99    Capital letters `A` through `Z`
 *  100-125  Lowercase letters `a` through `z`
 *  126      Space
 *  127-130  Symbols .<(+
 *  131      Solid vertical pipe
 *  132-142  Symbols &[]!$*);^-/
 *  143      Broken vertical pipe
 *  144-150  Symbols ,%_>?`:
 *  151      British pound symbol
 *  152-155  Symbols @'="
 *  156      Less than or equal symbol
 *  157      Empty box
 *  158      Plus or minus
 *  159      Filled box
 *  160      Degree symbol
 *  161      Dagger
 *  162      Symbol `~`
 *  163      En dash
 *  164      Lower left corner box draw
 *  165      Upper left corner box draw
 *  166      Greater than or equal symbol
 *  167-176  Superscript `0` through `9`
 *  177      Lower right corner box draw
 *  178      Upper right corner box draw
 *  179      Not equal symbol
 *  180      Em dash
 *  181      Superscript `(`
 *  182      Superscript `)`
 *  183      Horizontal dagger (?)
 *  184-186  Symbols {}\
 *  187      Cents symbol
 *  188      Centered dot, or bullet
 *  189-255  Reserved
 * </pre></code>
 *
 */
public class PORCharset
{

    /**
     * Translation table for the character set defined in a portable file.
     * The table has the following format:<br>
     * index {@code (k*2+0)} holds the symbol number, and <br>
     * index {@code (k*2+1)} holds the character meant by the symbol number.<br><p>
     *
     * When reading a portable file, the translation can be done as follows:<br>
     * 1) Read symbol x from the portable file.<br>
     * 2) Find the index for the symbol x in the character set defined in the
     *    portable file.<br>
     * 3) Find the corresponding index number from the translation table.<br>
     * 4) Translate the input symbol x to the character found from the translation
     *    table having the same index number.<br><p>
     *
     * Writing a portable file can be done in the same manner.<p>
     *
     */

    /**
     * "translation array index"-to-"Java char" table.<p>
     *
     * This table has the following format:<br>
     * Element at {@code (k*2+0)}: spss index number,<br>
     * Element at {@code (k*2+1)}: the corresponding Java char.<br><p>
     *
     * The value of -1 indicates that the translation table index number
     * is re-interpreted as Java char directly.<p>
     *
     * If a translation table index number is not present,
     * the situation is interpreted as there would be an entry
     * for it with the value of -1. Put concisely, if the translation table
     * index is missing, the index number is re-intepreted as Java char directly.
     *
     */
    public static final int[] TRANSLATION_INDEX_MAP = {
        // 0-60    Control characters. Not important enough to describe in full here.
        // 61-63   Reserved.

        // 64-73   Digits `0' through `9'.
        64,     '0',
        65,     '1',
        66,     '2',
        67,     '3',
        68,     '4',
        69,     '5',
        70,     '6',
        71,     '7',
        72,     '8',
        73,     '9',

        // 74-99   Capital letters `A' through `Z'.
        74,     'A',
        75,     'B',
        76,     'C',
        77,     'D',
        78,     'E',
        79,     'F',
        80,     'G',
        81,     'H',
        82,     'I',
        83,     'J',
        84,     'K',
        85,     'L',
        86,     'M',
        87,     'N',
        88,     'O',
        89,     'P',
        90,     'Q',
        91,     'R',
        92,     'S',
        93,     'T',
        94,     'U',
        95,     'V',
        96,     'W',
        97,     'X',
        98,     'Y',
        99,     'Z',

        // 100-125 Lowercase letters `a' through `z'.
        100,    'a',
        101,    'b',
        102,    'c',
        103,    'd',
        104,    'e',
        105,    'f',
        106,    'g',
        107,    'h',
        108,    'i',
        109,    'j',
        110,    'k',
        111,    'l',
        112,    'm',
        113,    'n',
        114,    'o',
        115,    'p',
        116,    'q',
        117,    'r',
        118,    's',
        119,    't',
        120,    'u',
        121,    'v',
        122,    'w',
        123,    'x',
        124,    'y',
        125,    'z',

        // 126     Space.
        126,    ' ',

        // 127-130 Symbols .<(+
        127,    '.',
        128,    '<',
        129,    '(',
        130,    '+',

        // 131     Solid vertical pipe.
        131,    -1,

        // 132-142 Symbols &[]!$*);^-/
        132,    '&',
        133,    '[',
        134,    ']',
        135,    '!',
        136,    '$',
        137,    '*',
        138,    ')',
        139,    ';',
        140,    '^',
        141,    '-',
        142,    '/',

        // 143     Broken vertical pipe.
        // 144-150 Symbols ,%_>?`:
        // 151     British pound symbol.
        // 152-155 Symbols @'=".
        143,    '|', // ???
        144,    ',',
        145,    '%',
        146,    '_',
        147,    '>',
        148,    '?',
        149,    '`',
        150,    ':',
        151,    '#', // (char=0x23)
        152,    '@',
        153,    '\'',
        154,    '=',
        155,    '\"',

        // 156     Less than or equal symbol.
        // 157     Empty box.
        // 158     Plus or minus.
        // 159     Filled box.
        // 160     Degree symbol.
        // 161     Dagger.
        // 162     Symbol `~'.
        // 163     En dash.
        // 164     Lower left corner box draw.
        // 165     Upper left corner box draw.
        // 166     Greater than or equal symbol.
        156,    -1, // (index=0x9C)
        157,    -1,
        158,    -1,
        159,    -1,
        160,    -1,
        161,    -1,
        162,    '~', // (char=0x7E, index=0xA2, ISO8859-1: cent symbol)
        163,    -1,
        164,    -1,
        165,    -1,
        166,    -1,

        // 167-176 Superscript `0' through `9'.
        167,    -1,
        168,    -1,
        169,    -1,
        170,    -1,
        171,    -1,
        172,    -1,
        173,    -1,
        174,    -1,
        175,    -1,
        176,    -1,

        // 177     Lower right corner box draw.
        // 178     Upper right corner box draw.
        // 179     Not equal symbol.
        // 180     Em dash.
        // 181     Superscript `('.
        // 182     Superscript `)'.
        // 183     Horizontal dagger (?).
        177,    -1,
        178,    -1,
        179,    -1,
        180,    -1,
        181,    -1,
        182,    -1,
        183,    -1,

        // 184-186 Symbols `{}\'.
        184,    '{', // (index=0xB8, ISO8859-1: latin small-z with caron)
        185,    '}', // (index=0xB9, ISO8859-1: superscript 1)
        186,    '\\', // (index=0xBA, ISO8859-1: ordinal indicator)

        // 187     Cents symbol.
        // 188     Centered dot, or bullet.
        187,    -1,
        188,    -1,

        // 189-255 Reserved.
    }; // TABLE

    // Some important constants
    //==========================

    /** Full stop ('.'), at index 127. */
    public static final int FULL_STOP                           = 127;

    /** Plus sign ('+'), at index 130. */
    public static final int PLUS_SIGN                           = 130;

    /** Minus sign ('-'), at index 141. */
    public static final int MINUS_SIGN                          = 141;

    /** Slash ('/'), at index 142. */
    public static final int SLASH                               = 142;

    /** Asterisk ('*'), at index 137. */
    public static final int ASTERISK                            = 137;

    /** Space bar, at index 126. */
    public static final int SPACE                               = 126;

    /** Digit zero ('0'), at index 64. */
    public static final int DIGIT_0                             = 64;

    /** Uppercase letter 'A', at index 74. */
    public static final int LETTER_UPPERCASE_A                  = 74;

    /** Uppercase letter 'Z', at index 99. */
    public static final int LETTER_UPPERCASE_Z                  = 99;

    // Other important constants
    //===========================

    /** ASCII/ISO8859/CP1252/UNICODE value for digit 0. */
    public static final int ASCII_DIGIT_0                       = 0x30;

    /** Length for encoding/decoding table. */
    public static final int CODEC_ARRAY_LENGTH                  = 256;


    // CONSTRUCTORS
    //==============

    /** Constructor intentionally disabled. */
    private PORCharset() {
    } // ctor


    // CLASS METHODS
    //===============

    public static int indexToChar(int index) {
        // For convenience
        final int[] table = TRANSLATION_INDEX_MAP;

        // Return value; if no such index, return char -1
        int c = -1;

        int entries = table.length / 2;

        for (int i = 0; i < entries; i++) {
            if (table[(i*2)+0] == index) {
                // The table entry number "i" contains
                // specifies the character for character index "index".
                // Get the Java char value
                c = table[(i*2)+1];
                break; // Stop looping immediately
            }
        }

        return c;
    }

    public static int charToIndex(int c) {
        // For convenience
        final int[] table = TRANSLATION_INDEX_MAP;

        // Return value; if no such char, return index -1
        int index = -1;

        int entries = table.length / 2;

        for (int i = 0; i < entries; i++) {
            if (table[(i*2)+1] == c) {
                // The table entry "i" contains the specified character.
                // Get the index number
                index = table[(i*2)+0];
                break; // Stop looping immediately
            }
        }

        return index;
    }

    public static int byteToIndex(int diskbyte, byte[] translation) {
        if ((diskbyte < 0) || (diskbyte > 0xFF)) {
            throw new IllegalArgumentException();
        }
        if ((translation == null) || (translation.length != 256)) {
            throw new IllegalArgumentException();
        }

        if (diskbyte == translation[DIGIT_0]) {
            throw new IllegalArgumentException();
            //return DIGIT_0;
        }

        // Return value; if no such char, return index -1
        int index = -1;

        for (int i = 0; i < translation.length; i++) {
            // Unsigned conversion byte->int.
            int trbyte = ((int) translation[i]) & 0xff;

            if (trbyte == diskbyte) {
                /*
                // TODO: This check should be done when a translation
                // is set up for the first time.
                if (index != -1) {
                    // Multiple matches for the diskbyte.
                    throw new RuntimeException(
                        "Multiple matches for a diskbyte");
                }
                */
                index = i;
                break; // Stop looping immediately
            }
        }

        return index;
    }

    public static int indexToByte(int index, byte[] translation) {
        if (index == DIGIT_0) {
            throw new IllegalArgumentException();
        }
        if ((index < 0) || (index > 0xFF)) {
            throw new IllegalArgumentException();
        }
        if ((translation == null) || (translation.length != 256)) {
            throw new IllegalArgumentException();
        }

        // Fetch the diskbyte at the specified index;
        // unsigned conversion byte->int.
        int diskbyte = ((int) translation[index]) & 0xff;

        // If the requested index is not zero, but the diskbyte
        // is the same as for zero, then the character under
        // this index is unspecified. In that case, return -1.
        /*
        if ((index != DIGIT_0)
            && (diskbyte == translation[DIGIT_0]))
        {
            diskbyte = -1;
        }
        */

        return diskbyte;
    }

    /**
     * Reset the given decoding/encoding table to identity transformation.
     *
     * @param codecTable [out] The encoding/decoding table to reset.
     */
    public static void computeIdentityCodec(int[] codecTable) {
        if ((codecTable == null) || (codecTable.length != 256)) {
            throw new IllegalArgumentException();
        }

        for (int i = 0; i < codecTable.length; i++) {
            codecTable[i] = i;
        }
    }

    /**
     * Compute the default translation table.
     * This corresponds to identity transformation.
     *
     * @param translation [out] The translation table to set.
     */
    public static void computeDefaultTranslation(byte[] translation) {
        final int[] table = TRANSLATION_INDEX_MAP;

        if ((translation == null) || (translation.length != 256)) {
            throw new IllegalArgumentException();
        }

        // Pick the character for digit 0 manually,
        // since this will be used to mark unspecified indices.
        int zero_membyte = indexToChar(DIGIT_0);

        for (int index = 0; index < translation.length; index++) {
            // Convert the index to membyte
            int membyte = indexToChar(index);

            // Use the membyte directly as the diskbyte,
            // or mark the index as unspecified.
            int diskbyte = membyte;

            if (diskbyte == -1) {
                diskbyte = zero_membyte;
            }

            // Narrowing conversion int->byte
            translation[index] = (byte) diskbyte;
        }
    }

    /*
    private static byte[] s_defaultTranslation = null;
    public byte[] getDefaultTranslation() {

        if (s_defaultTranslation == null) {
            s_defaultTranslation = new byte[256];
            computeDefaultTranslation(s_defaultTranslation);
        }
        return s_defaultTranslation;
    }
    */

    /**
     * Computes a decoding table for the given translation
     * Both arrays must have exactly 256 elements prior to the call.
     *
     * @param decodingTable [out] The computed decoding table.
     * @param translation [in] The translation table.
     */
    public static void computeDecodingTable(
        int[] decodingTable,
        byte[] translation
    ) {
        if ((decodingTable == null) || (translation == null)) {
            throw new IllegalArgumentException();
        }
        if ((translation.length != 256) || (decodingTable.length != 256)) {
            throw new IllegalArgumentException();
        }

        // Clear decoding table
        for (int inByte = 0; inByte < decodingTable.length; inByte++) {
            decodingTable[inByte] = -1;
        }

        // In the translation table the unused entries are
        // marked with zero. Therefore, the byte value used to
        // indiciate zero must be picked up.
        int zero = translation[DIGIT_0];

        // Set the decoding for the zero manually.
        decodingTable[zero] = ASCII_DIGIT_0; // '0'

        for (int diskbyte = 0; diskbyte < decodingTable.length; diskbyte++) {
            // Skip zero
            if (diskbyte == zero) {
                continue;
            }

            // By default, membyte equals to diskbyte
            int membyte = -1;

            // See if there's such a diskbyte in the translation array
            int index = byteToIndex(diskbyte, translation);

            // If the diskbyte has an index, see to which character
            // that index corresponds to.
            if (index != -1) {
                membyte = indexToChar(index);
                // If the character under the index is unspecified,
                // value -1 is returned.
            }

            // If no such index or the index position is unspecified,
            // diskbyte is interpreted as membyte directly.
            if (membyte == -1) {
                membyte = diskbyte;
            }

            // Update decoding table
            decodingTable[diskbyte] = membyte;
        }
    }


    /**
     * Computes an encoding table for the given translation.
     * Both arrays must have exactly 256 elements prior to the call.
     *
     * @param encodingTable [out] The computed encoding table.
     * @param translation [in] The translation table.
     */
    public static void computeEncodingTable(
        int[] encodingTable,
        byte[] translation
    ) {
        if ((encodingTable == null) || (translation == null)) {
            throw new IllegalArgumentException();
        }
        if ((translation.length != 256) || (encodingTable.length != 256)) {
            throw new IllegalArgumentException();
        }

        // Clear encodingg table
        for (int inByte = 0; inByte < encodingTable.length; inByte++) {
            encodingTable[inByte] = -1;
        }

        // In portable files, unused entries are marked with the zero.
        // Therefore, the code for zero must be picked by hand.
        int zero = (int) translation[DIGIT_0]; // this is a diskbyte

        // Set the encoding for the zero manually.
        encodingTable[ASCII_DIGIT_0] = zero;

        // use membyte/diskbyte

        for (int membyte = 0; membyte < encodingTable.length; membyte++) {

            // Skip zero.
            if (membyte == ASCII_DIGIT_0) { // '0'
                continue;
            }

            // By default, diskbyte equals to membyte
            int diskbyte = zero;

            // Interpret "membyte" as a 8-bit character,
            // and find translation table index for it.
            int index = charToIndex(membyte);
            // (return value is guaranteed to be always within byte range)

            if (index != -1) {
                // The membyte matched to an index,
                // Get the diskbyte from the translation table.
                diskbyte = indexToByte(index, translation);
                // If the diskbyte for the index position is unspecified,
                // the value "zero" is returned.
            }

            // Either no index for the char, or the translation table
            // left the index unspecified. Use membyte as the diskbyte
            if (diskbyte == zero) {
                diskbyte = membyte;
            }

            // Write an entry to the destination table
            encodingTable[membyte] = diskbyte;
        }
    }

    /*
    public static int[] createEncodingTable() {
        return new int[256];
    }

    public static int[] createDecodingTable() {
        return new int[256];
    }
    */
} // class PORCharset

