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
     * Element at {@code (k*2+0)}: the translation table index number,<br>
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
    public static final int[] DEFAULT_MAPPING = {
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
        151,    '#', // 0x23
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
        156,    -1,
        157,    -1,
        158,    -1,
        159,    -1,
        160,    -1,
        161,    -1,
        162,    '~', // 0x7E
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
        184,    '{',
        185,    '}',
        186,    '\\',
        
        // 187     Cents symbol. 
        // 188     Centered dot, or bullet. 
        187,    -1,
        188,    -1,
        
        // 189-255 Reserved. 
    }; // TABLE
    
    // Some important constants
    //==========================
    
    /** Full stop ('.'), at index 127. */
    public static final int FULL_STOP                            = 127;
    
    /** Plus sign ('+'), at index 130. */
    public static final int PLUS_SIGN                            = 130;
    
    /** Minus sign ('-'), at index 141. */
    public static final int MINUS_SIGN                           = 141;
    
    /** Slash ('/'), at index 142. */
    public static final int SLASH                                = 142;
    
    /** Asterisk ('*'), at index 137. */
    public static final int ASTERISK                             = 137;
    
    /** Space bar, at index 126. */
    public static final int SPACE                                = 126;
    
    /** Digit zero ('0'), at index 64. */
    public static final int DIGIT_0                              = 64;
    
    /** Uppercase letter 'A', at index 74. */
    public static final int LETTER_UPPERCASE_A                   = 74;
    
    /** Uppercase letter 'Z', at index 99. */
    public static final int LETTER_UPPERCASE_Z                   = 99;

    // CLASS VARIABLES
    //=================

    
    
    private static int[] g_default_charset = null;
    private static int[] g_identity_table = null;
    
    
    // CONSTRUCTORS
    //==============
    
    /** Constructor intentionally disabled. */
    private PORCharset() {
    } // ctor

    
    // CLASS METHODS
    //===============

    public static int indexToChar(int index) {
        // For convenience
        final int[] table = DEFAULT_MAPPING;
        
        int c = -1;
        
        // TODO: The following loop could improved with a bisect search.
        
        // Number of entries (each entry occupies two array elements)
        int entries = table.length / 2;
        
        for (int i = 0; i < entries; i++) {
            if (table[(i*2)+0] == index) {
                // The table entry number "i" contains
                // specifies the character for character index "index".
                // Get the Java char value
                c = table[(i*2)+1];
                break; // Stop search immediately
            }
        }
        
        if (c == -1) {
            // Re-interpret the index as a char
            c = index;
        }
        
        return c;
    }
    
    public static void assignDefaultTranslation(byte[] translation) {
        System.arraycopy(
            PORConstants.DEFAULT_TRANSLATION, 0, 
            translation, 0,
            PORConstants.TRANSLATION_LENGTH
        );
    }
    
    /**
     * Computes an identity table (can be used either as
     * a decoding or an encoding table).
     */
    public static void computeIdentityTable(int[] table) {
        if (table == null) {
            throw new IllegalArgumentException();
        }
        if (table.length != 256) {
            throw new RuntimeException("table.length != 256");
        }
        
        for (int i = 0; i < table.length; i++) {
            table[i] = i;
        }
    }
    
    /**
     * Computes a decoding table for the given translation
     *
     */
    public static void computeDecodingTable(
        int[] decodingTable, 
        byte[] translation
    ) {
        if ((decodingTable == null) || (translation == null)) {
            throw new IllegalArgumentException();
        }
        
        if (translation.length != 256) {
            throw new RuntimeException("translation.length != 256");
        }
        if (decodingTable.length != 256) {
            throw new RuntimeException("decodingTable.length != 256");
        }
        
        // Clear decoding table
        for (int inByte = 0; inByte < decodingTable.length; inByte++) {
            decodingTable[inByte] = -1;
        }
        
        // In the translation table the unused entries are 
        // marked with zero. Therefore, the byte value used to 
        // indiciate zero must be picked up.
        int zero = translation[DIGIT_0];
        
        // The entry for zero in the decoding table gets skipped
        // in the following loop. Hence, it must be set manually.
        decodingTable[zero] = '0';
        
        for (int index = 0; index < translation.length; index++) {
            // The input byte is character index
            int inByte = ((int) translation[index]) & 0xff;
            // The output byte is the character in Java's internal encoding.
            int outByte = indexToChar(index);
            
            // Zero as the input indicates that the character index
            // should be decoded using the default value.
            if (inByte == zero) {
                continue;
            }
            
            // Make "inByte" to be decoded as "outByte"
            decodingTable[inByte] = outByte;
            
        } // for
        
        
        // Unspecified entries assume their default output value
        for (int inByte = 0; inByte < decodingTable.length; inByte++) {
            int c = decodingTable[inByte];
            if (c == -1) {
                // Make it an identity translation
                int outByte = inByte;
                decodingTable[inByte] = outByte;
            }
        }
    }
    


    
    public static final int[] getDefaultCharset() {
        if (g_default_charset == null) {
            g_default_charset = new int[256];
            computeDefaultCharset(g_default_charset);
        }
        return g_default_charset;
    } // getDefaultCharset()
    
    public static final int[] getIdentityTable() {
        if (g_identity_table == null) {
            g_identity_table = new int[256];
            for (int i = 0; i < 256; i++) {
                g_identity_table[i] = i;
            }
        }
        return g_identity_table;
    } // getIdentityTable()
    
    /**
     * Computes the default charset into the specified array.
     * In this terminology, charset means a 256-element array, whose
     * elements are the codes corresponding to the pre-defined characters.
     *
     * @param table 
     *      [out] The array to populate, must contain exactly 256 elements.
     */
    public static void computeDefaultCharset(int[] table) {
        if (table == null) {
            throw new IllegalArgumentException(
                "Input array is null");
        }
        
        if (table.length != 256) {
            throw new IllegalArgumentException(
                "Input array\'s length differs from 256 elements");
        }
        
        // Set all entries to an invalid value
        for (int i = 0; i < table.length; i++) {
            table[i] = -1;
        } // for
        
        // Calc the length of TRANS table
        int len = DEFAULT_MAPPING.length / 2;
        
        for (int i = 0; i < len; i++) {
            int offset = i*2;
            int index = DEFAULT_MAPPING[offset+0];
            int value = DEFAULT_MAPPING[offset+1];
            
            // if the value is -1, then skip this
            if (value == -1) {
                continue;
            } // if: skip
            
            // Otherwise, assert that the entry hasn't been mapped already.
            if (table[index] >= 0) {
                throw new RuntimeException(String.format(
                    "PORCharset.computeDefaultCharset(): table[%d] >= 0 (internal error). ",
                    index));
            }
            
            // Set mapping to the entry
            table[index] = value;
        } // for
        
    } // computeDefaultCharset()

    /**
     * Computes a decoding table for the given charset.
     *
     * @param dectab
     *      [out] The decoding table to populate.
     *      The array must have a size of 256 elements.
     * @param charset
     *      [in] The charset for which the decoding table is computed.
     */
    public static void computeDecodingTable(int[] dectab, int[] charset) {
        if ((dectab.length != 256) || (charset.length != 256)) {
            throw new IllegalArgumentException(
                "ComputeDecodingTable(): incorrect array length (internal error)");
        } // if
        

        // Get the default charset. This is used as reference.
        final int[] default_charset = getDefaultCharset();
        
        // Set all entries to invalid value
        for (int i = 0; i < dectab.length; i++) {
            dectab[i] = -1;
        }

        // In portable files, unused entries are marked with the zero.
        // Therefore, the code for zero must be picked
        int inzero = charset[DIGIT_0];
        
        // The entry for zero in the decoding table gets skipped
        // in the following loop. It mut be set manually instead:
        dectab[inzero] = '0';

        for (int code = 0; code < charset.length; code++) {
            // When a byte charset[code] is read from a portable file,
            // it should be interpreted as a character default_charset[code]
            // (which is the UTF-8 character for code SPSS symbol code "code").
            
            int inbyte = charset[code];
            int outchar = default_charset[code];
            
            if (inbyte == inzero) {
                // the input table entry has been marked with zero,
                // so skip this entry.
                // Make it passthrough:
                dectab[code] = code;
                continue;
            }
            
            // TODO: outchar can be -1. It is probably okay?
            
            // Otherwise, set the corresponding output char
            dectab[inbyte] = outchar;
        } // for
    } // computeDecodingTable()


    /**
     * Computes an encoding table for the given charset.
     *
     * @param enctab
     *      [out] The encoding table to populate.
     *      The array must have a size of 256 elements.
     * @param charset
     *      [in] The charset for which the decoding table is computed.
     */
    public static void computeEncodingTable(int[] enctab, int[] charset) {
        if ((enctab.length != 256) || (charset.length != 256)) {
            throw new IllegalArgumentException(
                "ComputeEncodingTable(): incorrect array length (internal error)");
        } // if
        

        // Get the default charset. This is used as reference.
        final int[] default_charset = getDefaultCharset();
        
        // Set all entries to invalid value
        for (int i = 0; i < enctab.length; i++) {
            enctab[i] = -1;
        }

        // In portable files, unused entries are marked with the zero.
        // Therefore, the code for zero must be picked
        int outzero = charset[DIGIT_0];
        
        // The entry for zero in the decoding table gets skipped
        // in the following loop. It mut be set manually instead:
        enctab[outzero] = '0';

        for (int code = 0; code < charset.length; code++) {
            // When a byte charset[code] is read from a portable file,
            // it should be interpreted as a character default_charset[code]
            // (which is the UTF-8 character for code SPSS symbol code "code").
            
            int outbyte = charset[code];
            int inchar = default_charset[code];
            
            // TODO:
            // Validate the outbyte value (by verifying it is < 0x100)
            
            if (outbyte == outzero) {
                // the input table entry has been marked with zero,
                // so skip this entry.
                // Make it passthrough:
                // TODO: 
                // This should be: enctab[inchar] = inchar?
                //enctab[code] = code;
                enctab[inchar] = inchar;
            } else {
                // Otherwise, set the corresponding output char
                enctab[inchar] = outbyte;
            }
        } // for
    } // computeEncodingTable()
} // class PORCharset

