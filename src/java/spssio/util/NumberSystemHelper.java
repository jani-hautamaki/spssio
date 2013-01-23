
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

/**
 *
 * Just to make clear the terminology regarding underflow
 * <code><pre>
 *  
 *          -MAX_VALUE   -MIN_VALUE     ZERO      MIN_VALUE   +MAX_VALUE
 *                 |        |             |             |        |                 
 *       Negative  |        |  Negative   |  Positive   |        |  Positive       
 *       Overflow  |        |  Underflow  |  Underflow  |        |  Overflow       
 *                 |        |             |             |        |                 
 * </code></pre>
 *
 */
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
    
    /**
     * Maximum positive double value which can be multiplied by  the base 
     * without overflow.
     */
    private double max_double;
    
    /**
     * Smallest POSITIVE double value which can be divided by the base without 
     * underflow.
     */
    private double min_double;
    
    /**
     * Maximum long value which can be multiplied by the base without overflow.
     */
    private long max_long;

    /**
     * The highest exponent in the current base within the numeric limits
     * of {@code Double} data type.
     */
    private int max_exponent;

    /**
     * The smallest exponent in the current base within the numeric limits
     * of {@code Double} data type.
     */
    private int min_exponent;

    /**
     * Powers of the base up to the numeric limit of double, 
     * and {@code pow.length-1} is the maximum exponent a number may 
     * have in the current base.
     */
    private double[] pow;
    
    
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
        
        if (base < 2) {
            throw new IllegalArgumentException(String.format(
                "Invalid base; base=%d, but it must be greather than or equal to 2",
                base));
        } // if: invalid base
        
        // Get the length of digits string
        int len = digits.length();
        
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
        
        // Calculate maximum and minimum double values feasible for multiplication
        // and division by the base.
        
        max_double = Double.MAX_VALUE / (double) base;
        min_double = Double.MIN_VALUE * (double) base;
        
        // This needs to be rounded down in order to work.
        max_long = Long.MAX_VALUE / base;
        
        // Type cast rounds this towards zero.
        max_exponent = (int) (Math.log(Double.MAX_VALUE) / Math.log(base));
        // (when base=30, this results npow = 208 = 0xD0)
        
        // Type cast rounds this towards zero.
        min_exponent = (int) (Math.log(Double.MIN_VALUE) / Math.log(base));

        // Allocate new array for powers, garbaging the old.
        // The +1 is there so that max_exponent will be the last valid position.
        pow = new double[max_exponent+1];
        
        // Precalc the powers.
        for (int cexp = 0; cexp < pow.length; cexp++) {
            pow[cexp] = Math.pow(base, (double) cexp);
        } // for
        
    } // setNumberSystem()
    
    //          1         2
    // 12345678901234567890123456
    // ABCDEFGHIJKLMNOPQRSTUVWXYZ
    
    /**
     * Convenience method for setting the number system. The method will
     * automatically calculate the digits up to base-64.
     */
    public void setBase(int base) {
        // Create digits automatically
        if (base > 64) {
            throw new RuntimeException(String.format(
                "Invalid base; cannot automatically generate digits for bases greater than 64"));
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
    
    public double getMinDouble() {
        return min_double;
    } // getMinDouble()
    
    public double getMaxDouble() {
        return max_double;
    } // getMaxDouble()
    
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
        
        int digits_frac = 0;   // digits in the fraction part (no trailing zeros)
        int digits_int = 0;    // digits in the integer part (no leading zeros)
        
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
                    if (digit == 0) {
                        // Skip leading zeros
                    }
                    else if (digit != -1) {
                        // It is a base-N digit.
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
                        // Record to array
                        itab[ipos++] = digit;
                        // Increase the number of digits in the integer part.
                        digits_int++;
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
                        
                        // Increase the number of digits in the fraction part
                        digits_frac++;
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
                    if (digit == 0) {
                        // Skip leading zeros
                    }
                    else if (digit != -1) {
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
        
        
        // Remove trailing zeros from the fraction part, if any
        while ((ipos > 0) && (digits_frac > 0) && (itab[ipos-1] == 0)) {
            ipos--;
            digits_frac--;
        } // while
        
        // the exponent of a double fits to a long.
        long exp = 0;
        for (int i = 0; i < epos; i++) {
            if (exp > max_long) {
                // TODO:
                // exponent magnitude overflow.
            }
            
            exp *= base;
            exp += etab[i];
        }
        
        // Set exponent's sign.
        if (exp_negative == true) {
            exp = -exp;
        }
        
        long expfull = exp + digits_int - 1;
        if (expfull > max_exponent) {
            throw new RuntimeException(String.format(
                "number overflow, normalized exponent = %d", expfull));
        }
        if (expfull < min_exponent) {
            throw new RuntimeException(String.format(
                "number underflow, normalized exponent = %d", expfull));
        }
        
        // Consider, for instance, 1.4ACBDFHGA0+6S
        // This is "1.4ACBDFHGA0+e208"
        // The parser reads it as
        // 14ACBDFHGA0+e(208-10)
        
        
        
        StringBuilder sb = new StringBuilder(100);
        
        for (int i = 0; i < ipos; i++) {
            if (i == digits_int) {
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
        System.out.printf("digits %d.%d\n", digits_int, digits_frac);
        
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
    
    public static void dump_double_info(double d) {
        long bits = Double.doubleToLongBits(d);
        
        long neg =      bits & 0x8000000000000000L;
        long exponent = bits & 0x7ff0000000000000L;
        long mantissa = (bits & 0x000fffffffffffffL) | 0x0010000000000000L; // normalized

        exponent = (exponent >> 52) - 1023; // shift and unbiasing
        double frac = ((double) mantissa) * Math.pow(2.0, -52);

        System.out.printf("   double:     %.18g\n", d);
        System.out.printf("   toLongBits: %s\n", Long.toHexString(bits));
        System.out.printf("   toLongBits: %s\n", Long.toHexString(Double.doubleToLongBits(d)));
        System.out.printf("   sign:       %s\n", neg != 0 ? "-" : "+");
        System.out.printf("   exponent:   2**%d\n", exponent);
        System.out.printf("   mantissa:   %s\n", Long.toHexString(mantissa));
        System.out.printf("   frac:       %.16g\n", frac);
    }
    
    private static void modeDecimal(String line) {
        double d = Double.valueOf(line);
        dump_double_info(d);
    }
    
    private static void modeTrigesimal(String line, NumberSystemHelper nsh) {
        nsh.parseDouble(line);
    }
    
    public static void main(String[] args) {
        final int MODE_TRIGESIMAL = 0;
        final int MODE_DECIMAL    = 1;
        
        NumberSystemHelper nsh = new NumberSystemHelper();
        nsh.setBase(30);
        
        System.out.printf("Base: %d\n", nsh.getBase());
        System.out.printf("Digits: \"%s\"\n", nsh.getDigits());
        System.out.printf("Plus char:  \'%c\'\n", nsh.getPlusChar());
        System.out.printf("Minus char: \'%c\'\n", nsh.getMinusChar());
        System.out.printf("Point char: \'%c\'\n", nsh.getPointChar());
        System.out.printf("Max double: %.18g\n", nsh.getMaxDouble());
        System.out.printf("Min double: %.18g\n", nsh.getMinDouble());
        try {
            BufferedReader br = new BufferedReader(
                new InputStreamReader(System.in));
            
            String line;
            int mode = MODE_TRIGESIMAL;
            
            while (true) {
                if (mode == MODE_TRIGESIMAL) {
                    System.out.printf("tri>> ");
                } 
                else if (mode == MODE_DECIMAL) {
                    System.out.printf("dec>> ");
                } 
                else {
                    System.out.printf("?>> ");
                }
                
                
                line = br.readLine();
                if ((line == null) 
                    || line.equals("\\q"))
                {
                    break;
                }
                else if (line.equals("\\dec")) {
                    mode = MODE_DECIMAL;
                    continue;
                }
                else if (line.equals("\\tri")) {
                    mode = MODE_TRIGESIMAL;
                    continue;
                }

                System.out.printf("Read <%s>\n", line);
                
                try {
                    if (mode == MODE_TRIGESIMAL) {
                        modeTrigesimal(line, nsh);
                    } 
                    else if (mode == MODE_DECIMAL) {
                        modeDecimal(line);
                    }
                    else {
                        System.out.printf("Error: in an unknown mode\n");
                    }
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
