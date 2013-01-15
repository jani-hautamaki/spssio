
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

package spssio.util;

// for testing
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class NumberSystemHelper
{

    // DEFAULT CONFIGURATION
    //=======================
    
    // MEMBER VARIABLES
    //==================
    
    /**
     * The character used for plus sign (default '+').
     */
    private char plus_char = '+';
    
    /**
     * The character used for minus sign (default '-').
     */
    private char minus_char = '-';
    
    /**
     * The character used for dot/point (default '.').
     */
    private char point_char = '.';
    
    /**
     * Quick conversion table from ASCII char to integer.
     * The length of the array is expected to be 256. Invalid entries are
     * marked with TODO.
     */
    private int[] toint = null;
    
    // TODO:
    // if lowercase letters are equated with the uppercase letters,
    // then populate those too into toint[] array.
    
    /**
     * Quick conversion table from integer to ASCII char.
     * The length of the array is expected to be equal to {@code opt_base}.<p>
     * 
     * These are the digits of the number system from zero to {@code opt_base-1}.
     */
    private int[] tochar = null;

    /**
     *  The radix, also known as the base, of the number system (default 10).
     */
    private int base;
    
    // CONSTRUCTORS
    //==============
    
    public NumberSystemHelper() {
        // Allocated only once unlike tochar[] table.
        toint = new int[256];
        
        setBase(10);
    } // ctor
    
    // CONFIGURATION METHODS
    //=======================
    
    public void setNumberSystem(int base, String digits) {
        int len = digits.length();
        
        if (base < 2) {
            throw new IllegalArgumentException(String.format(
                "Invalid base; base=%d, but it must be greather than or equal to 2",
                base));
        } // if: invalid base
        
        if (len != base) {
            throw new IllegalArgumentException(String.format(
                "Invalid number of digits; base=%d, digits.length()=%d",
                base, len));
        } // if: invalid number of digits
        
        // Record the base
        this.base = base;
        
        // Clear toint[] table
        for (int i = 0; i< toint.length; i++) {
            toint[i] = -1;
        }
        
        // Allocate new tochar[] table
        tochar = new int[base];
        
        for (int i = 0; i < base; i++) {
            // Get the character at index "i".
            char c = digits.charAt(i);
            
            // Verify that "c" isn't already used
            for (int j = 0; j < i; j++) {
                if (tochar[j] == c) {
                    throw new RuntimeException(String.format(
                        "Invalid digits; digit \'%c\' is specified twice, at positions %d and %d",
                        c, i, j));
                } // if: duplicate digit
            } // for: each digit already specified
            
            // Convert to an integer, and get rid of the sign expansion,
            // which happens for values 0x7F < x <= 0xFF.
            int cval = ((int) c) & 0xff;
            
            // Put the values to the tables.
            tochar[i] = cval;
            toint[cval] = i;
            
            // and possibly the lowercase letter's cval too?
        } // for: each digit
    } // setNumberSystem()
    
    //          1         2
    // 12345678901234567890123456
    // ABCDEFGHIJKLMNOPQRSTUVWXYZ
    
    public void setBase(int base) {
        // Create digits automatically
        if (base > 64) {
            throw new RuntimeException(String.format(
                "Invalid base; cannot automatically generate digits for bases greather than 64"));
        } // if: base too big
        
        StringBuilder sb = new StringBuilder(base);
        for (int i = 0; i < base; i++) {
            int c = -1;
            
            if (i < 10) {
                c = '0';
                c += i;
            }
            else if (i < (10+26)) {
                c = 'A';
                c += (i-10);
            }
            else if (i < (10+26+26)) {
                c = 'a';
                c += (i-10-26);
                
            }
            else if (i == (10+26+26+0)) {
                c = '+';
            }
            else if (i == (10+26+26+1)) {
                c = '/';
            }
            sb.append( (char) c);
            System.out.printf("i=%d, Appending %c\n", i, (char) c);
        } // for
        
        setNumberSystem(base, sb.toString());
    } // setBase()
    
    // OTHER METHODS
    //===============
    
    public final int[] getToIntArray() {
        return toint;
    } // getToIntArray()
    
    public final int[] getToCharArrays() {
        return tochar;
    } // getToCharArray()
    
    public String getDigits() {
        return new String(tochar, 0, tochar.length);
    } // getDigits()
    
    public int getBase() {
        return base;
    } // getBase()
    
    public char getMinusChar() {
        return minus_char;
    } // getMinusChar()
    
    public char getPlusChar() {
        return plus_char;
    }
    
    public char getPointChar() {
        return point_char;
    }
    
    // PARSE FUNCTIONS
    //=================
    

    private static final int S_ERROR                            = -1;
    private static final int S_START                            = 0;
    private static final int S_OPTIONAL_SIGN                    = 1;
    private static final int S_EMPTY_INTEGER                    = 2;
    private static final int S_UNEMPTY_INTEGER                  = 3;
    private static final int S_EMPTY_FRACTION_UNEMPTY_INTEGER   = 4;
    private static final int S_EMPTY_FRACTION                   = 5;
    private static final int S_UNEMPTY_FRACTION                 = 6;
    private static final int S_EXPONENT_SIGN                    = 7;
    private static final int S_EMPTY_EXPONENT                   = 8;
    private static final int S_UNEMPTY_EXPONENT                 = 9;
    private static final int S_ACCEPT                           = 10;
    
    public double parseDouble(String s) {
        
        boolean num_negative = false;
        boolean exp_negative = false;
        
        int index = 0;
        int len = s.length();
        boolean eps = false;
        int state = S_START;
        int c = -1;
        int digit = -1;
        String errmsg = null;
        
        int[] itab = new int[len];
        int ipos = 0;
        int[] etab = new int[len];
        int epos =  0;
        
        int exp = 0;
        
        do {
            // If not a null-transition
            if (eps == false) {
                // Check that there are characters left to consume
                if (index < len) {
                    c = s.charAt(index);
                    digit = toint[c];
                    index++;
                } else {
                    // index==len
                    if (c != -1) {
                        c = -1;
                        digit = -1;
                    } else {
                        // unexpected eof
                    } // if-else
                }
            } // if: consume
            
            // Set null-transition to false
            eps = false;
            
            switch(state) {
                
                // TRIMMING & SIGN
                //=================
                
                // Input "|  +1234.1234+123"
                // Input "|  -1234-123"
                // Input "|.1234"
                case S_START:
                    // Leading spaces are accepted.
                    if (c == ' ') {
                        // ok, ignore leading whitespaces
                    } 
                    else {
                        // expect the first letter of the actual number
                        state = S_OPTIONAL_SIGN;
                        eps = true;
                    }
                    break;
                
                // Input "  |+1234.1234+123"
                // Input "  |-1234-123"
                // Input "|.1234"
                // Input "|123-9"
                // First non-whitespace: sign or digit or point.
                case S_OPTIONAL_SIGN: // Sign is at the front is optional.
                    // No matter what, the next state is digit or dot.
                    state = S_EMPTY_INTEGER;
                    
                    if (c == plus_char) {
                        // Plus sign, no action required
                    }
                    else if (c == minus_char) {
                        // Negative sign
                        num_negative = true;
                    }
                    else {
                        // Not a sign, then it must be either digit or dot.
                        eps = true;
                    }
                    break;
                
                // INTEGER PART
                //==============
                
                // Input "  +|1234.1234+123"
                // Input "  -|1234-123"
                // Input "|.1234"
                case S_EMPTY_INTEGER:
                    if (digit != -1) {
                        // It is a base-N digit.
                        // TODO: Record to array
                        state = S_UNEMPTY_INTEGER;
                        eps = true;
                    }
                    else if (c == point_char) {
                        // Must still have at least a single base-30 digit.
                        state = S_EMPTY_FRACTION;
                    }
                    else {
                        // ERROR: unexpected character
                        throw new RuntimeException(String.format(
                            "Invalid base-30 number; unexpected integer part"));
                    } // if-else
                    break;
                    
                case S_UNEMPTY_INTEGER:
                    if (digit != -1) {
                        // It is a base-N digit.
                        // TODO: Record to array
                        itab[ipos++] = digit;
                    }
                    else if (c == point_char) {
                        state = S_EMPTY_FRACTION_UNEMPTY_INTEGER;
                    }
                    else if ((c == minus_char) || (c == plus_char)) {
                        state = S_EXPONENT_SIGN;
                        eps = true;
                    }
                    else if (c == -1) {
                        // end-of-input, it has only integer part.
                        state = S_ACCEPT;
                    }
                    else {
                        // Error: unrecognized character (can't be eof)
                        throw new RuntimeException();
                    }
                    break;

                // FRACTIONAL PART
                //=================
                
                // The special case "+123." - should this be accepted?
                // The current choice is to accept it. The behaviour
                // can be changed by skippin this state.
                case S_EMPTY_FRACTION_UNEMPTY_INTEGER:
                    if (c == -1) {
                        // eof, accept
                        state = S_ACCEPT;
                    } else {
                        state = S_EMPTY_FRACTION;
                        eps = true;
                    }
                    break;
                
                case S_EMPTY_FRACTION:
                    // Possible input so far: "+."
                    // Possible input so far: "+123.?", where ? is c!=-1.
                    // Don't accept exponent sign before a digit.
                    if (digit != -1) {
                        state = S_UNEMPTY_FRACTION;
                        eps = true;
                    }
                    else {
                        // Either unaccepted character,
                        // or unrecognized character,
                        // or unexpected eof.
                        throw new RuntimeException();
                    }
                    break;

                case S_UNEMPTY_FRACTION:
                    // Possible input so far: 
                    // 1) "1.5"
                    // 2) "+2.5"
                    // 3) "-3.5"
                    // 4) "-.5"
                    if (digit != -1) {
                        // Record digits of the fractional part
                        itab[ipos++] = digit;
                        exp--;
                    }
                    else if ((c == minus_char) || (c == plus_char)) {
                        state = S_EXPONENT_SIGN;
                        eps = true;
                    }
                    else if (c == -1) {
                        // The number has a fractional part,
                        // but it doesn't have an exponent.
                        state = S_ACCEPT;
                    }
                    else {
                        // Unexpected or unrecognized char (can't be eof)
                        throw new RuntimeException();
                    }
                    break;
                    
                // EXPONENT PART
                //===============

                case S_EXPONENT_SIGN:
                    state = S_EMPTY_EXPONENT;
                    if (c == minus_char) {
                        exp_negative = true;
                    }
                    else if (c == plus_char) {
                        // ok
                    }
                    else {
                        // unexpected, unrecognized or eof.
                        throw new RuntimeException();
                    }
                    break;
                    
                case S_EMPTY_EXPONENT:
                    if (digit != -1) {
                        // an exponent value
                        state = S_UNEMPTY_EXPONENT;
                        eps = true;
                    }
                    else {
                        // either unexpected, unrecognized or eof
                        throw new RuntimeException();
                    }
                    break;
                    
                case S_UNEMPTY_EXPONENT:
                    if (digit != -1) {
                        // keep accepting digits to exponent
                        etab[epos++] = digit;
                    }
                    else if (c == -1) {
                        // eof, accept
                        state = S_ACCEPT;
                    }
                    else {
                        // unrecognized or unexpected.
                        throw new RuntimeException();
                    }
                    break;


                    
                default:
                    throw new RuntimeException(String.format(
                        "Unrecognized state=%d (programming error)", state));
            } // switch
        } while ((state != S_ACCEPT) && (state != S_ERROR));
        
        StringBuilder sb = new StringBuilder(100);
        for (int i = 0; i < ipos; i++) {
            if (i == ipos+exp) {
                sb.append(" . ");
            }
            sb.append(String.format("%d ", itab[i]));
        }
        
        if (exp_negative) {
            sb.append(" - ");
        } else {
            sb.append(" + ");
        }
        for (int i = 0; i < epos; i++) {
            sb.append(String.format("%d ", etab[i]));
        }
        
        System.out.printf("parseDouble():\n   %s\n", sb.toString());
        System.out.printf("exp = %d\n", exp);
        
        return 0.;
    } // parseDouble()
    
    /*
    public int parseInt() {
    }
    
    // FORMAT FUNCTIONS
    //==================
    
    public String formatDouble() {
    }
    
    public String formatInt() {
    }
    */
    
    // TESTING
    //=========
    
    public static void main(String[] args) {
        NumberSystemHelper nsh = new NumberSystemHelper();
        nsh.setBase(30);
        
        System.out.printf("Base: %d\n", nsh.getBase());
        System.out.printf("Digits: \"%s\"\n", nsh.getDigits());
        System.out.printf("Plus char:  \'%c\'\n", nsh.getPlusChar());
        System.out.printf("Minus char: \'%c\'\n", nsh.getMinusChar());
        System.out.printf("Point char: \'%c\'\n", nsh.getPointChar());
        try {
            BufferedReader br = new BufferedReader(
                new InputStreamReader(System.in));
            
            String line;
            
            while (true) {
                System.out.printf("tri>> ");
                line = br.readLine();
                if ((line == null) 
                    || line.equals("\\q"))
                {
                    break;
                }
                    
                System.out.printf("Read <%s>\n", line);
                try {
                    nsh.parseDouble(line);
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            } // while
            
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    
} // class NumberSystemHelper



/*
NRadixHelper base30

base30.parse_double()
base30.format_double()


CustomRadix.parse_double()
*/
