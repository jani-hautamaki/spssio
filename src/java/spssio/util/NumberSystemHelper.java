
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

// for accuracy
import java.math.BigDecimal;
import java.math.MathContext;

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
    
    // CONSTANTS
    //===========
    
    /** Value returned when an error occurred. */
    public static final double DBL_ERRVALUE             = 0.0;
    
    /** No error; the operation succeeded. */
    public static final int E_OK                        = 0x0000;
    
    /** Syntax error. */
    public static final int E_SYNTAX                    = 0x0001;
    
    /** Number overflow, either positive or negative. */
    public static final int E_OVERFLOW                  = 0x0002;
    
    /** Number underflow, either positive or negative. */
    public static final int E_UNDERFLOW                 = 0x0003;
    
    /** Exponent overflow due length, either positive, or negative. */
    public static final int E_EXPONENT_SIZE             = 0x0004;
    
    /** Mantissa overflow due length, either positive or negative. */
    public static final int E_MANTISSA_SIZE             = 0x0005;
    
    /** An internal error, a strong indication of a bug. */
    public static final int E_INTERNAL                  = 0x0006;
    

    // DEFAULT CONFIGURATION
    //=======================
    
    /**
     * Default value for maximum input string length acceped for parsing.
     */
    private static final int DEFAULT_MAX_INPUT_LENGTH = 128;
    
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
     * marked with -1.
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
     *  The radix, also known as the base, of the number system.
     *  If unset, the base is set to -1.
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
     * Maximum int value which can be multiplied by the base without overflow.
     */
    private int max_int;

    /**
     * The highest exponent in the current base within the numeric limits
     * of {@code Double} data type. Otherwise the data type overflows.
     */
    private int max_exponent;

    /**
     * The smallest exponent in the current base within the numeric limits
     * of {@code Double} data type. Otherwise the data type underflows.
     */
    private int min_exponent;
    
    /**
     * The highest possible mantissa value when exponent==max_exponent within
     * the numeric limits of {@code Double} data type. 
     * Otherwise the data type overflows.
     */
    private double max_mantissa;
    
    /**
     * The smallest possible mantissa value when exponent==min_exponent within
     * the numeric limits of {@code Double} data type.
     * Otherwise the data type underflows.
     */
    private double min_mantissa;

    /**
     * Powers of the base up to the numeric limit of double, 
     * and {@code pow.length-1} is the maximum exponent a number may 
     * have in the current base.
     */
    private double[] pow;
    
    
    // VARIABLES FOR METHOD: parseDouble()
    //=====================================
    
    /**
     * Buffer for individual digits that are extracted from the input string 
     * during parsing. It is better to have this as a member variable rather
     * than as a local variable, because otherwise the method would need to
     * allocate the array from the heap every time the function is called.
     */
    private int dtab[];
    
    /**
     * Error number code for the previous operation. If the value is non-zero,
     * then an error has occurred. Otherwise, the previous operation finished
     * normally.
     */
    private int errno;
    
    /**
     * Indicates the sign of the value related to an error. If the value is
     * negative, this is set to -1. Otherwise, this is set to 1 (signed positive,
     * or unsigned).
     */
    //private int errsign;
    
    /**
     * A human-redable error message. If no errror, this is set to null..
     */
    private String strerror;
    
    
    /**
     * If not null, the intermediate results are calculated by using the BigDecimal
     * data type. Otherwise, Java's "double" primitive is used.
     */
    private MathContext mctx;
    
    // CONSTRUCTORS
    //==============
    
    /** 
     * Default constructor; leaves the base (and digits) unset. 
     * The base (and digits) must be set before usage.
     */
    public NumberSystemHelper() {
        // toint[] array is allocated only once,
        // (whereas tochar[] table is reallocated when the radix changes)
        toint = new int[256];
        
        // Allocate the dtab with default maximum input string length
        dtab = new int[DEFAULT_MAX_INPUT_LENGTH];
        
        // Keep the base unset. The number system is not initialized 
        // until user explicitly requests so.
        base = -1;
        
        // Use "double" for intermediate results by default
        mctx = null;
    } // ctor
    
    // CONFIGURATION METHODS
    //=======================
    
    /**
     * Sets the base and digits of the number system.
     *
     * @param base The base, or radix, of the number system
     * @param digits The digits for the number system. String's length
     *      must be equal to the base.
     */
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
        
        // These need to be rounded down in order to work.
        max_long = Long.MAX_VALUE / base;
        max_int = Integer.MAX_VALUE / base;
        
        // Type cast rounds this towards zero.
        max_exponent = (int) (Math.log(Double.MAX_VALUE) / Math.log(base));
        // (when base=30, this results npow = 208 = 0xD0)
        
        // Type cast rounds this towards zero.
        min_exponent = (int) (Math.log(Double.MIN_VALUE) / Math.log(base));

        // Calcualte maximum and minimum allowed mantissa values when
        // exponent is set to maximum/minimum value
        
        max_mantissa = Double.MAX_VALUE / Math.pow(base, max_exponent);
        
        min_mantissa = Double.MIN_VALUE / Math.pow(base, min_exponent);

        // Minimum mantissa has to be corrected in this way, because Math.pow()
        // cant take min_exponent-1 in without breaking.
        //min_mantissa *= base;
        //min_exponent -= 1;

        // Allocate new array for powers, garbaging the old.
        // The +1 is there so that max_exponent will be the last valid position.
        pow = new double[max_exponent+1];
        
        // Precalc the powers.
        for (int cexp = 0; cexp < pow.length; cexp++) {
            pow[cexp] = Math.pow(base, (double) cexp);
        } // for

        
        System.out.printf("max_mantissa: %.18g\n", max_mantissa);
        System.out.printf("min_mantissa: %.18g\n", min_mantissa);
        System.out.printf("max_exponent = %d\n", max_exponent);
        System.out.printf("min_exponent = %d\n", min_exponent);
        System.out.printf("base**max_exponent: %.18g\n", Math.pow(base, max_exponent));
        System.out.printf("adjusting min mantissa, log_base: %.18g\n",
            Math.log(min_mantissa) / Math.log(base));

        // adjust
        
    } // setNumberSystem()
    
    //          1         2
    // 12345678901234567890123456
    // ABCDEFGHIJKLMNOPQRSTUVWXYZ
    
    /**
     * Convenience method for setting the number system. The method will
     * automatically calculate the digits up to base-64.
     *
     * @param base the base, or radix, of the number system
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
    
    /**
     * Sets the maximum input string length allowed. 
     *
     * @param len new value for maximum input string length.
     */
    public void setMaxInputLength(int len) {
        // Less than one is unreasonable length
        if (len < 1) throw new IllegalArgumentException();
        
        // No upper limit.
        
        
    }
    
    /**
     * Enable or disable the use of {@code BigDecimal}s in the intermediate
     * calculations. If the parameter is not null, then that math context
     * is used for instantiating the {@code BigDecimal}s. If null,
     * then Java's primitive type {@code double} is used instead.
     * 
     * @param mctx the accuracy settings to be used for {@code BigDecimal}s,
     *      or null if {@code double}s are to be used.
     * 
     */
    public void setMathContext(MathContext mctx) {
        this.mctx = mctx;
    } // setMathContext()
    
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
    } // getPlusChar()
    
    public char getPointChar() {
        return point_char;
    } // getPointChar()
    
    // PARSE FUNCTIONS
    //=================
    

    private static final int S_ERROR                            = -1;
    private static final int S_START                            = 0;
    private static final int S_OPTIONAL_SIGN                    = 1;
    private static final int S_EMPTY_INTEGER                    = 2;
    private static final int S_UNEMPTY_INTEGER_ZERO             = 3;
    private static final int S_UNEMPTY_INTEGER                  = 4;
    private static final int S_EMPTY_FRACTION_EMPTY_INTEGER     = 5;
    private static final int S_EMPTY_FRACTION_UNEMPTY_INTEGER   = 6;
    private static final int S_EMPTY_FRACTION                   = 7;
    private static final int S_UNEMPTY_FRACTION                 = 8;
    private static final int S_EXPONENT_SIGN                    = 9;
    private static final int S_EMPTY_EXPONENT                   = 10;
    private static final int S_UNEMPTY_EXPONENT                 = 11;
    private static final int S_ACCEPT                           = 99;
    
    /**
     * Returns the error code of the last operation.
     * If no error occurred, {@link #E_OK} is returned.
     */
    public int errno() {
        return errno;
    }
    
    /**
     * Returns a human-redable error message of the last operation.
     * The message is valid only, if {link #errno()} indicates an error.
     * Otherwise, when the last operation was successful, {@code null} is returned.
     */
    public String strerror() {
        return strerror;
    }
    
    /**
     * Should the method return Double.NaN on error?
     */
    public double parseDouble(String s) {
        
        int index = 0;          // current read offset
        int len = s.length();   // length of the string
        boolean eps = false;    // null-transition flag
        int state = S_START;    // current state, initialized to starting state
        int c = -1;             // current input char
        int digit = -1;         // c translated to a digit, or -1 if non-digit.

        boolean ineg = false;   // true if significand/mantissa is negative
        boolean eneg = false;   // true if exponent is negatives
        
        int dlen = 0;           // write index for significand/mantissa digits
        int exp = 0;            // exponent value
        
        int digits_int = 0;     // digits in the integer part (no leading zeros)
      //int digits_frac = 0;    // digits in the fraction part (no trailing zeros)
        
        int rshift = 0;         // how many digits the point is shifted RIGHT
                                // used only if input has form "0.000abcd"

        // Verify the input string's length. 
        // Do not process further, if the strng's too long.
        if (len >= dtab.length) {
            errno = E_SYNTAX;
            strerror = String.format("syntax error; input string too long");
            return DBL_ERRVALUE;
        } // if: too long

        do {
            // Read the next character,
            // unless this is a null-transition (eps==true).
            if (eps == false) {
                // Check that there are characters left to consume
                if (index < len) {
                    // Read the next character
                    c = s.charAt(index);
                    // Increase the reading offset
                    index++;
                    
                    // Convert the character into a digit, if possible
                    if (c < toint.length) {
                        digit = toint[c];
                    } else {
                        // Unexpected high code point.
                        // Explicit translation unavailable; assume non-digit.
                        digit = -1;
                    } // if-else
                } else {
                    // No characters left in the input string, this is "eof".
                    if (c != -1) {
                        // First time in eof branch
                        c = -1;
                        digit = -1;
                    } else {
                        // Second time in eof branch
                        // unexpected eof
                    } // if-else
                } // if-else: chars left in the input?
            } // if: consume
            
            // Null-transition defaults to false
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
                        // Significand/mantissa is positive
                    }
                    else if (c == minus_char) {
                        // Significand/mantissa is negative
                        ineg = true;
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
                        // This has become an unempty integer.
                        state = S_UNEMPTY_INTEGER;
                        eps = true;
                    }
                    else if (c == point_char) {
                        // No digits in the integer part, even though there might
                        // have been a sign. The input must have a digit in
                        // fractional part. Otherwise, the input is rejected.
                        state = S_EMPTY_FRACTION_EMPTY_INTEGER;
                    }
                    else {
                        // ERROR: unexpected character
                        errno = E_SYNTAX;
                        strerror = String.format(
                            "syntax error; unexpected character: %c", c);
                        return DBL_ERRVALUE;
                    } // if-else
                    break;
                    
                case S_UNEMPTY_INTEGER:
                    if ((digit == 0) & (dlen == 0)) {
                        // Eat leading zeros
                    }
                    else if (digit != -1) {
                        // It is a base-N digit.
                        // Record to array, the array is guaranteed to have space.
                        dtab[dlen++] = digit;
                        // Increase the number of digits in the integer part.
                        digits_int++;
                    }
                    else if (c == point_char) {
                        state = S_EMPTY_FRACTION_UNEMPTY_INTEGER;
                    }
                    else if ((c == minus_char) || (c == plus_char)) {
                        // This char begins the exponent; there is no "decimal
                        // "point" and consequently no fraction part.
                        state = S_EXPONENT_SIGN;
                        eps = true;
                    }
                    else if (c == -1) {
                        // end-of-input, it has only integer part.
                        state = S_ACCEPT;
                    }
                    else {
                        // Error: unrecognized character (can't be eof)
                        errno = E_SYNTAX;
                        strerror = String.format(
                            "syntax error; unrecognized character: %c", c);
                        return DBL_ERRVALUE;
                    }
                    break;

                // FRACTIONAL PART
                //=================

                // Possible inputs:
                //      "+."
                //      "."
                case S_EMPTY_FRACTION_EMPTY_INTEGER:
                    // Unallowed here: eof, exponent
                    if (c == -1) {
                        // unallowed
                        errno = E_SYNTAX;
                        strerror = String.format(
                            "syntax error; unexpected end-of-data");
                        return DBL_ERRVALUE;
                    }
                    else if (digit != -1) {
                        // The required digit. Now the flow can continue normally.
                        state = S_EMPTY_FRACTION;
                        eps = true;
                    }
                    else {
                        // Unexpected char or unexpected exponent
                        errno = E_SYNTAX;
                        strerror = String.format(
                            "syntax error; no digits before the exponent");
                        return DBL_ERRVALUE;
                    }
                    break;
                    
                // The special case "+123." or "+000." - should this be accepted?
                // The current choice is to accept it. The behaviour
                // can be changed by skippin this state.
                case S_EMPTY_FRACTION_UNEMPTY_INTEGER:
                    // allowed here: eof (the number ends to a point), 
                    // allowed here: exponent "+123.+" ???
                    if (c == -1) {
                        // eof, accept
                        state = S_ACCEPT;
                    } else {
                        // TODO:
                        // Exponent allowed after the point char.
                        // Allows "+1.", but unallows: "+1.+3". TODO?
                        state = S_EMPTY_FRACTION;
                        eps = true;
                    }
                    break;
                
                case S_EMPTY_FRACTION:
                    // Possible input so far: "+."
                    // Possible input so far: "+123.?", where ? is non-eof.
                    // Don't accept exponent sign before a digit.
                    if (digit != -1) {
                        state = S_UNEMPTY_FRACTION;
                        eps = true;
                    }
                    else {
                        // Either unaccepted character,
                        // or unrecognized character,
                        // or unexpected eof.
                        errno = E_SYNTAX;
                        strerror = String.format(
                            "syntax error; unexpected character or end-of-data");
                        return DBL_ERRVALUE;
                    }
                    break;

                case S_UNEMPTY_FRACTION:
                    // Allow: eof, digit, exponent sign
                    // Possible input so far: 
                    // 1) "1.5"
                    // 2) "+2.5"
                    // 3) "-3.5"
                    // 4) "-.5"
                    if ((digit == 0) && (dlen == 0)) {
                        // Eat leading zeros.
                        
                        // However, the number of digits in the fraction part
                        // must be recorded in order to know the exponent of
                        // the divider.
                        rshift++;
                        //digits_frac++;
                    }
                    else if (digit != -1) {
                        // Record digits of the fractional part
                        dtab[dlen++] = digit;
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
                        errno = E_SYNTAX;
                        strerror = String.format(
                            "syntax error; unexpected character: %c", c);
                        return DBL_ERRVALUE;
                    }
                    break;
                    
                // EXPONENT PART
                //===============

                case S_EXPONENT_SIGN:
                    state = S_EMPTY_EXPONENT;
                    if (c == plus_char) {
                        // exponent is positive
                    }
                    else if (c == minus_char) {
                        // exponent is negative
                        eneg = true;
                    }
                    else {
                        // unexpected, unrecognized or eof.
                        errno = E_SYNTAX;
                        strerror = String.format(
                            "syntax error; unexpected character or end-of-data");
                        return DBL_ERRVALUE;
                    }
                    break;
                    
                case S_EMPTY_EXPONENT:
                    if (digit != -1) {
                        // the required digit, switch to a new state.
                        state = S_UNEMPTY_EXPONENT;
                        eps = true;
                    }
                    else {
                        // either unexpected, unrecognized or eof
                        errno = E_SYNTAX;
                        strerror = String.format(
                            "syntax error; unexpected character or end-of-data");
                        return DBL_ERRVALUE;
                    }
                    break;
                    
                case S_UNEMPTY_EXPONENT:
                    if (digit != -1) {
                        // Otherise, keep accepting digits to the exponent.
                        if (exp > max_int) {
                            errno = E_EXPONENT_SIZE;
                            strerror = String.format(
                                "exponent overflow;s exponent too big");
                            return DBL_ERRVALUE;
                        } // if: overflow of "int"
            
                        exp *= base;
                        exp += digit;
                    }
                    else if (c == -1) {
                        // eof, accept
                        state = S_ACCEPT;
                    }
                    else {
                        // unrecognized or unexpected.
                        errno = E_SYNTAX;
                        strerror = String.format(
                            "syntax error; unexpected characer: %c", c);
                        return DBL_ERRVALUE;
                    }
                    break;

                default:
                    errno = E_INTERNAL;
                    strerror =String.format(
                        "internal error; unhandled state: state=%d", state);
                    return DBL_ERRVALUE;
                    
            } // switch
        } while ((state != S_ACCEPT) && (state != S_ERROR));
        
        // At this point asserted:
        //      1) digits_frac <= ipos 
        //      2) digits_int <= ipos
        //      3) digits_int+digits_frac == ipos
        // 
        
        // Manage exponent's sign
        if (eneg == true) {
            exp = -exp;
        } // if: signed exp
        
        // Remove trailing zeros from the fraction part, if any.
        // This does affect the exponent's value.
        while ((digits_int < dlen) && (dtab[dlen-1] == 0)) {
            dlen--;
        } // while

        // Note: it is possible, if so desired, to continue removing trailing 
        // zeros from the integer part too; that would just have to be 
        // accounted for in the exponent's value. 
        
        // THE FOLLOWING VARIABLES ARE THE INPUT FOR THE NEXT PART:
        //    Member variables:
        //          -base
        //          -dtab
        //          -max_double
        //          -max_int
        //          -max_mantissa, min_mantissa
        //          -max_exponent, min_exponent
        //    Local variables:
        //          -ipos, ineg
        //          -epos, ebase, eneg
        //          -digits_int, digits_frac
        //
        //    There are some redundant variables there...
        //    For instance, 
        //
        //          digits_int+digits_frac = ipos
        //
        //    is just the initial place of decimal point, which will be then
        //    shifted by the routined according to its preferences. In fact,
        //    digits_int IS the index position of the point, and 
        //
        //          digits_frac = ipos - digits_int
        //  
        //    also, the exponent can be multiplied IN PLACE, and then
        //    there will not be any need for variables related to the exponent,
        //    expect the value of the exponent itself.
        //
        //    Thus, final set of requierd variables looks probably like
        //      
        //          -ipos, ineg,
        //          -digits_int,
        //          -exp
        //
        
        // Convert the significand/mantissa to an integer.
        
        BigDecimal bvalue = null; 
        BigDecimal bbase = null;
        
        double dvalue = 0.0;       // if mctx == null
        
        if (mctx == null) {
            // Use "double"
            for (int i = 0; i < dlen; i++) {
                if (dvalue > max_double) {
                    errno = E_MANTISSA_SIZE;
                    strerror = String.format(
                        "mantissa overflow; mantissa too big");
                    return DBL_ERRVALUE;
                } // if: overflow
                dvalue *= base;
                dvalue += dtab[i];
            } // for
        } else {
            // Use "BigDecimal"
            
            // Initialize the variables
            bvalue = BigDecimal.ZERO;
            bbase = new BigDecimal(base, mctx);
            
            // Introduce a helper variable
            BigDecimal bdigit = null;
            
            for (int i = 0; i < dlen; i++) {
                if (bvalue.doubleValue() > max_double) {
                    errno = E_MANTISSA_SIZE;
                    strerror = String.format(
                        "mantissa overflow; mantissa too big");
                    return DBL_ERRVALUE;
                } // if: overflow
                bdigit = new BigDecimal(dtab[i], mctx);
                bvalue = bvalue.multiply(bbase);
                bvalue = bvalue.add(bdigit); // uses mctx for rounding
            } // for
        } // if-else
        

        // Introduce an auxiliary variable
        int digits_frac = dlen - digits_int;
        
        // If the number does not have fractional part nor exponent,
        // execute a quick exit here.
        // the fractional part nor the exponent
        if ((exp == 0) && (digits_frac == 0)) {
            double rval;
            
            if (mctx == null) {
                rval = dvalue;
            } else {
                rval = bvalue.doubleValue();
            } // if-else
            
            if (ineg == true) {
                rval = -rval;
            } // if: signed
            
            return rval;
            //return rval;
        } // if: no fractional part nor exponent
        
        // Now:
        //      result = value * base^(-digits_frac) * base^(exp)
        
        
        // The length of the significand is "ipos". The significand needs to be
        // normalized to the form "a.bcdef" if not already in that form. 
        //
        // If significand's length, "ipos" is 0 or 1, it is already in
        // the required form. However, if the length >= 2 then there might 
        // be need for normalization depending on how many integer digits
        // there are (there might be 0, 1 or more).
        
        int pshift = 0; // how many digits the point is shifted to the left
        
        if (digits_int > 1) {
            // Integer part of significand/mantissa has two or more digits.
            
            // Shift the position of the "decimal" point to left 
            // by the amount of (digits_int-1)
            
            // Example:
            // The mantissa could be, "abc.def"; three digits in the integer part.
            // then the point needs to be shifted to left by 2 digits.
            
            // Calcualte the amount to shift
            pshift = digits_int-1;
        }
        else if (digits_int == 0) {
            // This time the point is shifted RIGHT by one
            // to extract the first non-zero digit into the integer part.
            pshift = -1;
            
        } // if-else
        
        // Account for the shifts in the exponent's value.
        // rshift is visible ONLY here.
        exp -= rshift;
        exp += pshift;
        
        // Adjust the number of digits in integer and fraction parts
        digits_int -= pshift;
        digits_frac += pshift;
        
        // The following asserts hold:      digits_int == 1
        //                                  digits_int+digits_frac == dlen
        
        // Check for overflow
        if (exp > max_exponent) {
            errno = E_OVERFLOW;
            strerror = String.format("number overflow; exponent out of range");
            return DBL_ERRVALUE;
        } else if (exp == max_exponent) {
            
            // Implement "digits_frac"
            
            if (mctx == null) {
                dvalue = dvalue * Math.pow(base, -digits_frac);
                
            } else {
                bvalue = bvalue.divide(bbase.pow(digits_frac));
                // Use dvalue as a temporarily variable
                dvalue = bvalue.doubleValue();
            } // if-else
            
            if (dvalue > max_mantissa) {
                errno = E_OVERFLOW;
                strerror = String.format("Number overflow; mantissa out of range");
                return DBL_ERRVALUE;
            } // if: overflow
        } // if: overflow checking
        else if (exp+digits_int < min_exponent) {
            errno = NumberSystemHelper.E_UNDERFLOW;
            strerror = String.format("number underflow; exponent out of range");
            return DBL_ERRVALUE;
        } else if (exp-digits_frac <= min_exponent) {
            // Implied: min_exponent <= exp+digits_int
            
            // Implement "digits_frac+digits_int" (=dlen)
            
            // Normalize mantissa for limit checking
            if (mctx == null) {
                // Normalize
                dvalue = dvalue * Math.pow(base, -dlen);
                // Check overflow with the normalized mantissa
            } else {
                // Normalize
                bvalue = bvalue.divide(bbase.pow(dlen));
                // Use dvalue as a temporarily variable
                dvalue = bvalue.doubleValue();
            } // if-else

            // The effect of these variables has now been accounted for
            exp += digits_int;
            
            if ((exp == min_exponent) && (dvalue < min_mantissa)) {
                errno = E_UNDERFLOW;
                strerror = String.format("number underflow; mantissa out of range");
                return DBL_ERRVALUE;
            } // if: underflow
        } // if: underflow checking
        else {
            // The value is just fine. Incorporate the fraction divider
            // into the exponent right away
            exp = exp - digits_frac;
        } // if-else
        
        /*
        debug_parse_double(
            digits_int, digits_frac, exp, eneg, pshift, 
            ipos, ebase, epos, dtab, 
            mant_dvalue, bbase, mant_bvalue
        ); 
        */
        
        if (mctx == null) {
            dvalue = dvalue * Math.pow(base, exp);
        } else {
            // mctx != null
            if (exp < 0) {
                // This type has no problems with very negative exponents
                bvalue = bvalue.divide(bbase.pow(-exp));
            } else if (exp > 0) {
                bvalue = bvalue.multiply(bbase.pow(exp));
            } else {
                // exp == 0; no actions required
            }
            // Round the BigDecimal to a double
            dvalue = bvalue.doubleValue();
        } // if-else

        // Account for the significand's sign
        if (ineg == true) {
            dvalue = -dvalue;
        } // if
        
        errno = E_OK;
        strerror = null;
        
        return dvalue;
    } // parseDouble()
    
    /*
    private void debug_parse_double(
        int digits_int, 
        int digits_frac,
        int exp,
        boolean eneg,
        int pshift,
        int ipos,
        int ebase,
        int epos,
        int[] dtab,
        double dvalue,
        BigDecimal bbase,
        BigDecimal bvalue
    ) {
        StringBuilder sb = new StringBuilder(100);
        
        for (int i = 0; i < ipos; i++) {
            if (i == digits_int) {
                sb.append(" . ");
            }
            sb.append(String.format("%d ", dtab[i]));
        }
        
        if (eneg) {
            sb.append(" - ");
        } else {
            sb.append(" + ");
        }
        for (int i = 0; i < epos; i++) {
            sb.append(String.format("%d ", dtab[ebase+i]));
        }

        System.out.printf("Input string:            <%s>\n", sb.toString());
        System.out.printf("Digit arrangement:       %d.%d\n", digits_int+pshift, digits_frac-pshift);
        System.out.printf("Digit arrangement ADJ:   %d.%d\n", digits_int, digits_frac);
        System.out.printf("Exponent value:          %d\n", exp-pshift);
        System.out.printf("Exponent value ADJ:      %d\n", exp);
        System.out.printf("Exponent value EFF:      %d\n", exp-digits_frac);
        
        if (mctx == null) {
        System.out.printf("Mant raw value (double): %.18g\n", dvalue);
        System.out.printf("Mant ADJ value (double): %.18g\n", dvalue * Math.pow(base, -digits_frac));
        System.out.printf("Value (double):          %.18g\n", dvalue * Math.pow(base, exp-digits_frac));
        } else {
        System.out.printf("Mant raw value (BigStr): %s\n", bvalue.toString());
        System.out.printf("Mant raw value (BigDbl): %.18g\n", bvalue.doubleValue());
        exp = exp - digits_frac; // finalize exp
        if (exp < 0) {
            bvalue = bvalue.divide(bbase.pow(-exp));
        } else if (exp > 0) {
            bvalue = bvalue.multiply(bbase.pow(exp));
        } else {
            // do nothing to bvalue.
        } // if-else: exp<0?
        System.out.printf("Mant ADJ value (BigStr): (not implemented)\n");
        System.out.printf("Mant ADJ value (BigDbl): (not implemented)\n");
        System.out.printf("value2 (BigStr):         %s\n", bvalue.toString());
        System.out.printf("value2 (BigDbl):         %.18g\n", bvalue.doubleValue());
        } // if-else: mctx == null?
    } // debug_parse_double()
    */
    
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

    // Very close to the Double.MAX_VALUE
    //      "A.9E17IR6IFL+6S"
    // Should result in the same as the values
    //      ".0A9E17IR6IFL+70"
    //      ".000A9E17IR6IFL+72"
    
    // These should overflow:
    //      ".000A9E17IR6IFL+80"        (exponent too big)
    //      ".000A9E17IR6JFL+72"        (mantissa too big)
    
    // Double.MAX_VALUE         = 1.79769313486231570e+308
    // Double.MIN_VALUE         = 4.90000000000000000e-324
    
    // max_mantissa = 10.3156020126482610
    // max_exponent = 208                 == 6S
    // compare to     10.3156020126482600 == A.9E17IR6IFL+6S
    
    // min_mantissa = 1.5
    // min_exponent = -219                == 79
    // compare to   = 
    
    // Very close to the Double.MIN_VALUE
    //      "1.F-79"                      == 1.0e-323 (double)
    
    // These should underflow:
    //      "1.F-7A"                    (exponent too small)
    //      "1.E-79"                    (mantissa too small)
    
    // Also look at
    //      http://steve.hollasch.net/cgindex/coding/ieeefloat.html
    // Approx decimal:
    //  double limits: ~10**(-323.3) to ~10**(308.3)
    
    // N-7A gives with BigDecimal:
    // 4.90000000000000000e-324 which seems to be the smallest non-zero
    // Minimum exponent is 218 (dec), ie. 78 (trig)
    // Minimum mantissa is then 0.05
    
    // Numeric limits in base-10:
    // Max double: 5.99231044954105300e+306
    // Min double: 1.50000000000000000e-322

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
        double rval;
        rval = nsh.parseDouble(line);
        if (nsh.errno() != NumberSystemHelper.E_OK) {
            System.out.printf("ERROR: %s\n", nsh.strerror());
        } else {
            System.out.printf("Result: %.18g\n", rval);
        }
    }
    
    public static void main(String[] args) {
        final int MODE_TRIGESIMAL = 0;
        final int MODE_DECIMAL    = 1;
        
        NumberSystemHelper nsh = new NumberSystemHelper();
        //nsh.setBase(30);
        nsh.setBase(10);
        //nsh.setMathContext(MathContext.DECIMAL128);
        
        System.out.printf("Base: %d\n", nsh.getBase());
        System.out.printf("Digits: \"%s\"\n", nsh.getDigits());
        System.out.printf("Plus char:  \'%c\'\n", nsh.getPlusChar());
        System.out.printf("Minus char: \'%c\'\n", nsh.getMinusChar());
        System.out.printf("Point char: \'%c\'\n", nsh.getPointChar());
        System.out.printf("Max double / base: %.18g\n", nsh.getMaxDouble());
        System.out.printf("Min double * base: %.18g\n", nsh.getMinDouble());
        System.out.printf("Double.MAX:        %.18g\n", Double.MAX_VALUE);
        System.out.printf("Double.MIN:        %.18g\n", Double.MIN_VALUE);
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
        } // try-catch
    } // main()
    
    
} // class NumberSystemHelper
