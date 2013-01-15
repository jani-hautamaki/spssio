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
 * The constant array {@link #TRANS} contains the following table which has 
 * been pulled out from the PSPP's documentation:<p>
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
    public static final int[] TRANS = {
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
    
    /** Full stop ('.'), symbol 127. */
    public static final int FULL_STOP                            = 127;
    
    /** Plus sign ('+'), symbol 130. */
    public static final int PLUS_SIGN                            = 130;
    
    /** Minus sign ('-'), symbol 141. */
    public static final int MINUS_SIGN                           = 141;
    
    /** Slash ('/'), symbol 142. */
    public static final int SLASH                                = 142;
    
    /** Asterisk ('*'), symbol 137. */
    public static final int ASTERISK                             = 137;
    
    /** Space bar, symbol 126. */
    public static final int SPACE                                = 126;
    
    /** Digit zero ('0'), symbol 64. */
    public static final int DIGIT_0                              = 64;
    
    /** Uppercase letter 'A', symbol 74. */
    public static final int LETTER_UPPERCASE_A                   = 74;
    
    /** Uppercase letter 'Z', symbol 99. */
    public static final int LETTER_UPPERCASE_Z                   = 99;

    // CONSTRUCTORS
    //==============
    
    /** Constructor intentionally disabled. */
    private PORCharset() {
    } // ctor

    
    private static int[] g_table = null;
    
    public static final int[] getDefaultTable() {
        if (g_table == null) {
            // TODO: initDefaultTable()
            initialize();
        }
        return g_table;
    } // getTable();
    
    public static void initDecoderTable(int[] dtable, byte[] intable) {
        // get default table which is used as the codex
        final int[] outtable = getDefaultTable();
        
        for (int i = 0; i < dtable.length; i++) {
            dtable[i] = -1;
        } // for
        
        // In portable files, unused entries are marked with the zero
        int inzero = intable[DIGIT_0];
        
        // This gets skipped in the following loop, so it must be set
        // manually here
        dtable[inzero] = '0';
        
        for (int i = 0; i < intable.length; i++) {
            // when a character intable[i] is read from a portable file,
            // it should be interpreted as character outtable[i]
            int inc = ((int) intable[i]) & 0xff;
            int outc = outtable[i];
            
            if (inc == inzero) {
                // the input table entry has been marked with zero,
                // so skip this entry
                continue;
            }
            
            // Otherwise, set the corresponding output char
            dtable[inc] = outc;
            
        } // for
        
    } // initDecodeTable()
    
    private static void initialize() {
        g_table = new int[256];
        
        // Clear the table to an invalid value
        for (int i = 0; i < g_table.length; i++) {
            g_table[i] = -1;
        } // for
        
        // Calc the length of TRANS table
        int len = TRANS.length / 2;
        
        for (int i = 0; i < len; i++) {
            // Calculate offset
            int offset = i*2;
            // Get the index
            int index = TRANS[offset+0];
            // Get the value
            int value = TRANS[offset+1];
            
            // if the value is -1, then skip this
            if (value == -1) {
                continue;
            } // if: skip
            
            // Otherwise, see if there is already uninvalid value
            if (g_table[index] >= 0) {
                // Error, already specified
                throw new RuntimeException(String.format(
                    "PORCharset.initialize() failed: index=%d. This is a programming error",
                    index));
            }
            
            // If no previous value, then initialize the value
            g_table[index] = value;
        } // for
        
    } // initialize()
    
    
    
    /*
    public static final int[] getFullTable() {
        if (FULL_TABLE == null) {
            // Construct it
            FULL_TABLE = new int[256];
            for (int i = 0; i < FULL_TABLE.length; i++) {
                int c = getIndex(i);
                if (c == -1) {
                    c = '0';
                }
                FULL_TABLE[i] = c;
            } // for
        } // if
        return FULL_TABLE;
    }
    */
    
    /**
     * Dumb search; fix with bisect
     */
    public static int getIndex(int index) {
        int len = TRANS.length / 2;
        for (int i = 0; i < len; i++) {
            if (TRANS[i*2] == index) {
                return TRANS[(i*2)+1];
            }
        } // for: each entry
        return -1;
    } // getIndex()
    
    /*
    TODO:
    input: byte from portable file.
    output: java utf-8 char in the correct character set
    
    input: translation table from a portable file
    
    */
    
} // class PORCharset


