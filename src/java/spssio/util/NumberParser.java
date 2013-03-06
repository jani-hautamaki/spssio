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

import java.math.MathContext;
import java.math.BigDecimal;

/**
 * Parser for numbers expressed in an arbitrary base.
 * The numbers may be fractional and have an exponent.
 *
 */
public class NumberParser
{

    // ERROR CODES
    //=============
    
    /** Parsing is unfinished, more input is expected. */
    public static final int E_UNFINISHED                = 0xffff;
    
    /** No error; the operation succeeded. */
    public static final int E_OK                        = 0x0000;
    
    /** Syntax error. */
    public static final int E_SYNTAX                    = 0x0001;
    
    /** Number overflow, consult {link #lastsign()} for the sign. */
    public static final int E_OVERFLOW                  = 0x0002;
    
    /** Number underflow, consult {link #lastsign()} for the sign. */
    public static final int E_UNDERFLOW                 = 0x0003;
    
    /** Exponent overflow, not necessarily number overflow or underflow. */
    public static final int E_EXPONENT_SIZE             = 0x0004;

    /** Mantissa overflow, not necessarily number overflow or underflow. */
    public static final int E_MANTISSA_SIZE             = 0x0006;
    
    /** Internal input buffer overflow; input data is too long. */
    public static final int E_BUFFER                    = 0x0007;
    
    /** An internal error, a strong indication of a bug. */
    public static final int E_INTERNAL                  = 0x0008;

    // RETURN VALUE IN CASE OF AN ERROR
    //==================================

    /** In case of an error, {link #parseDouble(String)} returns this. */
    public static final double DBL_ERRVALUE             = 0.0;

    // DEFAULT CONFIGURATION
    //=======================
    
    /**
     * Default value for maximum input string length acceped for parsing.
     */
    static final int DEFAULT_MAX_INPUT_LENGTH = 128;


    // MEMBER VARIABLES BEGIN
    //========================
    
    /**
     * A reference to the number system that is used.
     */
    private NumberSystem sys;

    /**
     * If not null, the intermediate results are calculated by using 
     * the BigDecimal data type. Otherwise, Java's "double" primitive is used.
     */
    private MathContext mctx;
    
    // PARSE PRODUCTS
    //================
    
    /**
     * Buffer for individual digits extracted from the input string.
     * It is better to have this as a member variable rather than as a local 
     * variable, because otherwise the method would need to allocate the array 
     * from the heap every time the function is called.
     */
    private int dtab[];

    /**
     * Write index for {@code dtab} array, and also the length.
     */
    private int dlen;
    
    /**
     * Base index for {@code dtab} array. 
     * Designates the starting offset of the exponent's or mantissa's digits.
     */
    private int dbi;

    /**
     * Mantissa's sign as parsed in the last operation. 
     * Set to -1 if negative sign, and 1 if positive sign.
     * Otherwise, set to 0 (eg. unsigned or unparsed).
     */
    private int msign;
    
    /**
     * Exponent's sign as parsed in the last operation.
     * Set to -1 if negative sign, and 1 if positive sign.
     * Otherwise, set to 0 (eg. unsigned, unparsed or no exponent).
     */
    private int esign;
    
    /**
     * Number of digits in the integer part (before the "decimal point").
     */
    private int digits_int;
    
    /**
     * The parsed number as double; the product of the parse.
     */
    private double thedouble;

    // STATE MACHINE
    //===============

    /**
     * Current state.
     */
    private int state;
    
    /**
     * Null-transition flag. 
     * If true, the next transition is a null-transition (non-consuming).
     */
    private boolean eps;
    
    // ERROR MANAGEMENT
    //==================
    
    /**
     * Error number code for the last operation.
     */
    private int errno;
    
    /**
     * A human-redable error message related to the error code.
     * If no errror, this is set to null.
     */
    private String strerror;

    // STATES
    //========
    
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

    // CONSTRUCTORS
    //==============
    
    /**
     * Constructs an uninitialized parser. A {@code NumberSystem} must
     * be associated to the parser before use.
     */
    public NumberParser() {
        sys = null;
        mctx = null;
        dtab = null;
        setMaxInputLength(DEFAULT_MAX_INPUT_LENGTH);
    } // ctor
    
    /**
     * Constructs an initialized parser by associating a {@code NumberSystem}.
     *
     * @param numsys The number system (radix, digits and limits) to use.
     */
    public NumberParser(NumberSystem numsys) {
        this();
        setNumberSystem(numsys);
    } // ctor
    
    // CONFIGURATION
    //===============
    
    /**
     * Associates the parser with a {@code NumberSystem}.
     *
     * @param numsys The number system to associate this parser with.
     */
    public void setNumberSystem(NumberSystem numsys) {
        sys = numsys;
    } // setNumberSystem()
    
    /**
     * Sets the maximum input string length.
     *
     * @param len The maximum input string length.
     */
    public void setMaxInputLength(int len) {
        // Reallocate dtab
        dtab = new int[len];
    }
    
    /**
     * Controls whether {@code BigDecimal}s or {@code double}s are used
     * for intermediate results during parsing. This is a decision between
     * accuracy and speed. Using {@code BigDecimal}s give accuracy while
     * sacrificing speed.<p>
    
     * To use {@code BigDecimal}s, pass non-{@code null} context with desired 
     * precision and rounding mode. To use {@code double}s, pass {@code null}.
    
     * @param context the context to use, or {@code null}.
     */
    public void setMathContext(MathContext context) {
        mctx = context;
    }
    
    // ERROR INTERROGATION
    //=====================
    
    /**
     * Returns the error code associated with the last operation.
     * If the last operation was succesful, the value {@link #E_OK} 
     * is returned.
     * 
     * @return The error code, or [@code E_OK} if the last operation succeeded.
     */
    public int errno() {
        return errno;
    }
    
    /**
     * Returns the error message associated with {@link #errno()}.
     * If the error code is {@code E_OK}, the error message is undefined
     * and should not be used.
     *
     * @return The human-readable error message.
     */
    public String strerror() {
        return strerror;
    }
    
    /**
     * Returns the sign of the last mantissa encountered.
     * In the case of an error, the sign of the last mantissa is not always
     * defined. Consult the error codes for details regarding in which cases
     * the sign is defined.
     *
     * @return
     *      If negatively signed, -1 is returned.
     *      If positively signed or unsigned, 1 is returned.
     *
     */
    public int lastsign() {
        return msign;
    }
    
    /**
     * Returns the result of parsing as a double.
     *
     * @return
     *      The double value of the parsed number.
     */
    public double lastvalue() {
        return thedouble;
    }
    
    // PARSE DOUBLE - THE MAIN METHOD
    //================================
    
    /**
     * Parses a number into a double.<p>
     * 
     * After the call, use {@link #errno()} to determine whether the operation 
     * succeeded or not. If there's an error, it is possible to get 
     * a human-readable error message with {@link #strerror()}. 
     *
     * @param text The string to parse
     * 
     * @return The number's value as a double. Return value is valid
     *      only if {@code errno()} returns {@code E_OK}. Otherwise,
     *      the return value is {@link #DBL_ERRVALUE} which can be 
     *      considered to be undefined.
     */
    public double parseDouble(String text) {
        
        String string = text;
        int stringlen = text.length();
        int index = 0;

        reset();
        
        // Verify the input string's length. 
        // Do not process further, if the strng's too long.
        do {
            int c;

            if (index < stringlen) {
                c = string.charAt(index);
                index++;
            } else {
                c = -1;
            }
            
            consume(c);
            
        } while (errno == E_UNFINISHED);
        
        return thedouble;
    } // parseDouble()

    // PARSER DRIVER
    //===============
    
    /**
     * Reset the parser to initial state.
     * This method needs to be called always before starting to 
     * parse a new input. 
     */
    public void reset() {
        
        dlen = 0;
        dbi = -1;
        digits_int = 0;
        
        esign = 0;
        msign = 0;
        
        state = S_START;
        eps = false;
        
        errno = E_UNFINISHED;
        strerror = "Parsing is unfinished; more input is expected";
        
        
        // Check that there's a non-null number system reference.
        
        if (sys == null) {
            state = S_ERROR;
            errno = E_INTERNAL;
            strerror = String.format("Set a NumberSystem object first");
        }
    } // reset()
    
    /**
     * Consumes the input character.
     * This method can be used to drive the parser one character at a time.
     * <p>
     *
     * The return values of interest are:
     * <ul>
     *    <li>{@code E_UNFINISHED} - The parsing hasn't finished yet. 
     *        More input is expected
     *    <li>{@code E_OK} - The parsing has finished succesfully, 
     *        The result is available with {@link #lastvalue()}.
     *    <li>Other - The parsing has finished unsuccesfully.
     *        Error code and details are available with {@link #errno()}
     *        and {@link #strerror()}.
     * </ul>
     * <p>
     *
     * Once the parser has reached an ending state and the return value
     * is different than {@code E_UNFINISHED}, the parser has to be reset
     * before it can be used again. The resetting is done with {@link reset()}.
     * <p>
     * 
     * Emitting {@code -1} will always cause the parser to finish with 
     * either accepting or rejecting the input.<p>
     *
     * @return
     *      The current {@code errno()} value. See the description.
     */
    public int consume(int c) {
        
        // Fast exit, if already finished
        if ((state == S_ACCEPT) || (state == S_ERROR)) {
            return errno;
        }
        
        // Translate input char into a digit
        int digit;
        
        if ((c >= 0) && (c < sys.toint.length)) {
            digit = sys.toint[c];
        } else {
            digit = -1;
        }
        
        // Check for buffer overflow
        if ((digit != -1) && (dlen == dtab.length)) {
            state = S_ERROR;
            errno = E_BUFFER;
            strerror = String.format("Internal buffer overflow; input too big");
        }
        
        // Do all transitions up to the next consuming one.
        
        do {
            
            // reset null-transition flag.
            eps = false;
            
            // State transition
            action(c, digit);
            
        } while ((eps == true) && (state != S_ACCEPT) && (state != S_ERROR));
        
        // Inspect the exit state; if accept, do postprocessing
        if (state == S_ACCEPT) {
            postprocess();
        }
        
        return errno;
    } // consume()
    
    private void postprocess() {
        // Variable for exponent's value.
        int exp = 0;
        
        // If there was an exponent, convert it.
        if (dbi != -1) {
            // esign != 0 implies "dbi" is valid
            
            exp = toInteger(dtab, dbi, dlen);
            
            if (state == S_ERROR) {
                errno = E_EXPONENT_SIZE;
                strerror = String.format("exponent overflow; number too long");
                return;
            }

            // Set the exponent's sign
            if (esign < 0) {
                exp = -exp;
            }
            
            // Discard the exponent data in dtab.
            dlen = dbi;
        } // if: has an exponent
        
        // Remove trailing zeros from the fraction part, if any.
        while ((digits_int < dlen) && (dtab[dlen-1] == 0)) {
            dlen--;
        }

        // Reset base index; It is next used to find 
        // the starting point of non-zero digits in mantissa.
        dbi = 0;
        
        // Leading zeros in the integer part were stripped by 
        // the state machine. Consequently, if there are any leading 
        // zeros in dtab[], they all must belong to the fractional part.
        
        // Remove leading zeros of the fractional part, if any.
        while ((dbi < dlen) && (dtab[dbi] == 0)) {
            dbi++;
        }

        // Convert the mantissa to a double or BigDecimal.
        
        BigDecimal bvalue = null; 
        double dvalue = 0.0;
        
        if (mctx == null) {
            dvalue = toDouble(dtab, dbi, dlen);
        } else {
            bvalue = toBigDecimal(dtab, dbi, dlen);
        } // if-else
        
        // If an error occurred during conversion, exit here.
        if (state == S_ERROR) {
            errno = E_MANTISSA_SIZE;
            strerror = String.format("mantissa overflow; number too long");
            return;
        }
        
        // If the number does not have a fractional part nor exponent,
        // do not bother to examine the numeric limits. 
        
        if ((exp == 0) && (digits_int == dlen-dbi)) {
            // No fractional part nor exponent. Faster exit.
            
            if (mctx != null) {
                dvalue = bvalue.doubleValue();
            } // if-else
            
        } else {
            // Final value can now be calculated as:
            //     dvalue*base^(-digits_frac) * base(exp)
            // However, it is possible that this expression yields 
            // a number which exceeds the numeric limits of double.
            // Therefore, a more detailed calculation is required.
            
            dvalue = calculateDouble(
                dvalue, bvalue,
                digits_int,
                dlen-digits_int,
                exp
            );
            
            // Check for errors
            if (state == S_ERROR) {
                return;
            }
            
        } // if-else

        // Set mantissa's sign
        if (msign < 0) {
            dvalue = -dvalue;
        } // if
        
        // Clear error indicators;
        // This signals the succesful completion of parsing.
        errno = E_OK;
        strerror = null;
        
        // Assign result
        thedouble = dvalue;
        
    } // postprocess()
    
    
    
    private int toInteger(int[] tab, int from, int to) {
        int rval = 0;
        
        state = S_ACCEPT;
        for (int i = from; i < to; i++) {
            if (rval > sys.max_int) {
                state = S_ERROR;
                break;
            } // if: overflow
            rval *= sys.base;
            rval += tab[i];
        } // for
        
        return rval;
    } // toInteger()
    
    private double toDouble(int[] tab, int from, int to) {
        double rval = 0.0;
        
        state = S_ACCEPT;
        for (int i = from; i < to; i++) {
            if (rval > sys.max_double) {
                state = S_ERROR;
                break;
            } // if: overflow
            rval *= sys.base;
            rval += tab[i];
        } // for
        
        return rval;
    } // toDouble()
    
    
    private BigDecimal toBigDecimal(int[] tab, int from, int to) {
        BigDecimal rval = BigDecimal.ZERO;
        BigDecimal base_bigdecimal = new BigDecimal(sys.base, mctx);
        
        state = S_ACCEPT;
        
        for (int i = from; i < to; i++) {
            if (rval.doubleValue() > sys.max_double) {
                state = S_ERROR;
                break;
            } // if: overflow
            rval = rval.multiply(base_bigdecimal);
            // The following add() uses mctx for rounding
            rval = rval.add(new BigDecimal(tab[i], mctx));
        } // for
        
        return rval;
    } // toBigDecimal()
    
    /**
     * Calculate the {@code double} value of a floating-point number.
     * The mantissa's value has to be in given as an integer even though
     * it is stored into a {@code double} or {@code BigDecimal}. 
     * The method checks the numerical limits, and sets {@link #errno}
     * correspondingly if an error occurs.
     *
     * @param dvalue 
     *      Mantissa's integer value as a double.
     * @param bvalue 
     *      Mantissa's integer value as a BigDecimal.
     *      The parameter is used only if {@link #mctx} is not {@code null}.
     * @param digits_int 
     *      Number of digits in the mantissa's integer part.
     * @param digits_frac 
     *      Number of digits in the mantissa's fractional part.
     * @param exp 
     *      Exponent's value.
    
     * @return
     *      Calculated value as a double. The return value is valid
     *      only if {@link #errno()} returns {@code E_OK}.
     */
    private double calculateDouble(
        double dvalue,
        BigDecimal bvalue,
        int digits_int,
        int digits_frac,
        int exp
    ) {
        
        state = S_ACCEPT;
        
        // Mantissa is normalized into form "a.bcdef", 
        // if not already in that form.
        //
        // If there's only 1 integer digit in the mantissa, 
        // then it is already in the required form. 
        //
        // If there are >1 integer digits in the mantissa (abcd.ef),
        // then the "decimal point" is shifted RIGHT by digits_int-1.
        //
        // If there are no integer digits in the mantissa (.abcdef),
        // then the "decimal point" is shifted LEFT by 1.
        
        int pshift = 0; // how many digits the point is shifted to the left
        
        if (digits_int > 1) {
            // Mantissa has two or more digits in the integer part.
            // Shift the "decimal point" LEFT by (digits_int-1).
            pshift = digits_int-1;
        }
        else if (digits_int == 0) {
            // Mantissa has no digits in the integer part.
            // Shift the "decimal point" RIGHT by 1
            pshift = -1;
        } // if-else
        
        // Adjust the number of digits in integer and fraction parts
        digits_int -= pshift;
        digits_frac += pshift;

        // Account for the shifts in the exponent's value too.
        exp += pshift;
        
        // Now the following holds: digits_int == 1

        // If BigDecimals are used, convert the NumberSystem's base.
        BigDecimal base_bigdecimal = null;
        if (mctx != null) {
            base_bigdecimal = new BigDecimal(sys.base, mctx);
        }
        
        // Check for overflow
        if (exp > sys.max_exponent) {
            errno = E_OVERFLOW;
            strerror = String.format("number overflow; exponent out of range");
            state = S_ERROR;
        } else if (exp == sys.max_exponent) {
            // Get normalized mantissa value into dvalue for limit checking.
            if (mctx == null) {
                dvalue = dvalue * Math.pow(sys.base, -digits_frac);
            } else {
                bvalue = bvalue.divide(base_bigdecimal.pow(digits_frac));
                dvalue = bvalue.doubleValue();
            } // if-else
            
            if (dvalue > sys.max_mantissa) {
                errno = E_OVERFLOW;
                strerror = String.format("Number overflow; mantissa out of range");
                state = S_ERROR;
            } // if: overflow
        } else if (exp+digits_int < sys.min_exponent) {
            errno = E_UNDERFLOW;
            strerror = String.format("number underflow; exponent out of range");
            state = S_ERROR;
        } else if (exp-digits_frac <= sys.min_exponent) {
            // Implied: min_exponent <= exp+digits_int
            // Implement "digits_frac+digits_int" (=dlen)
            int digits = digits_int+digits_frac;
            
            // Get normalized mantissa value into dvalue for limit checking.
            if (mctx == null) {
                dvalue = dvalue * Math.pow(sys.base, -digits);
            } else {
                bvalue = bvalue.divide(base_bigdecimal.pow(digits));
                dvalue = bvalue.doubleValue();
            } // if-else

            // The effect of these variables has now been accounted for
            exp += digits_int;
            
            // Check overflow with the normalized mantissa
            if ((exp == sys.min_exponent) && (dvalue < sys.min_mantissa)) {
                errno = E_UNDERFLOW;
                strerror = String.format("number underflow; mantissa out of range");
                state = S_ERROR;
            } // if: underflow
        } else {
            // The value is within numeric limits.
            // Incorporate mantissa's divider into the exponent.
            exp = exp - digits_frac;
        } // if-else
        
        if (state == S_ERROR) {
            return DBL_ERRVALUE;
        }
        
        if (mctx == null) {
            dvalue = dvalue * Math.pow(sys.base, exp);
        } else {
            // mctx != null. BigDecimals don't have problems
            // with negative exponents less than Double.MIN_EXPNENT
            if (exp < 0) {
                bvalue = bvalue.divide(base_bigdecimal.pow(-exp));
            } else if (exp > 0) {
                bvalue = bvalue.multiply(base_bigdecimal.pow(exp));
            } else {
                // exp == 0; no actions required
            }
            // Round the BigDecimal to a double
            dvalue = bvalue.doubleValue();
        } // if-else
        
        return dvalue;
    } // calculateDouble()
    
    // INTERNAL METHODS
    //==================
    
    /**
     *
     * The method affects the following member variables:
     * <ul>
     *   <li>{@code eps} - whether next transition is null-transition
     *   <li>{@code state} - what is the next state
     * </ul>
     * <p>
     *
     * Following member variables are being populated:
     * <li>
     *   <li>{@code msign} - determines the mantissa's sign
     *   <li>{@code esign} - determines the exponent's sign
     *   <li>{@code dtab[dlen]} - current char as a digit at the position
     *   <li>{@code dlen} - writing index of the digit
     *   <li>{@code digits_int} - how many digits before the "decimal point".
     * </ul>
     * <p>
     *
     * @param c 
     *      The current input character
     * @param digit 
     *      The current character as a digit, or -1 if non-digit.
     *
     */
    private void action(int c, int digit) {
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
                
                if (c == sys.plus_char) {
                    // Significand/mantissa is positive
                    msign = 1;
                }
                else if (c == sys.minus_char) {
                    // Significand/mantissa is negative
                    msign = -1;
                }
                else {
                    // Not a sign, then it must be either digit or dot.
                    msign = 1;
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
                else if (c == sys.point_char) {
                    // No digits in the integer part, even though there might
                    // have been a sign. The input must have a digit in
                    // fractional part. Otherwise, the input is rejected.
                    state = S_EMPTY_FRACTION_EMPTY_INTEGER;
                }
                else {
                    // ERROR: unexpeced or eof
                    errno = E_SYNTAX;
                    strerror = String.format(
                        "syntax error; unexpected character: %s", 
                        c > 0 ? c : "<eof>");
                    state = S_ERROR;
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
                else if (c == sys.point_char) {
                    state = S_EMPTY_FRACTION_UNEMPTY_INTEGER;
                }
                else if ((c == sys.minus_char) || (c == sys.plus_char)) {
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
                    state = S_ERROR;
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
                    state = S_ERROR;
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
                    state = S_ERROR;
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
                    state = S_ERROR;
                }
                break;

            case S_UNEMPTY_FRACTION:
                // Allow: eof, digit, exponent sign
                // Possible input so far: 
                // 1) "1.5"
                // 2) "+2.5"
                // 3) "-3.5"
                // 4) "-.5"
                if (digit != -1) {
                    // Record digits of the fractional part
                    dtab[dlen++] = digit;
                }
                else if ((c == sys.minus_char) || (c == sys.plus_char)) {
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
                    state = S_ERROR;
                }
                break;
                
            // EXPONENT PART
            //===============

            case S_EXPONENT_SIGN:
                state = S_EMPTY_EXPONENT;
                if (c == sys.plus_char) {
                    // exponent is positive
                    esign = 1;
                }
                else if (c == sys.minus_char) {
                    // exponent is negative
                    esign = -1;
                }
                else {
                    // unexpected, unrecognized or eof.
                    errno = E_SYNTAX;
                    strerror = String.format(
                        "syntax error; unexpected character or end-of-data");
                    state = S_ERROR;
                }
                break;
                
            case S_EMPTY_EXPONENT:
                if (digit != -1) {
                    // the required digit, switch to a new state.
                    state = S_UNEMPTY_EXPONENT;
                    eps = true;
                    // mark the starting offset for the exponent's digits
                    dbi = dlen;
                }
                else {
                    // either unexpected, unrecognized or eof
                    errno = E_SYNTAX;
                    strerror = String.format(
                        "syntax error; unexpected character or end-of-data");
                    state = S_ERROR;
                }
                break;
                
            case S_UNEMPTY_EXPONENT:
                if (digit != -1) {
                    dtab[dlen++] = digit;
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
                    state = S_ERROR;
                }
                break;
                
            case S_ERROR:
                // Already in the error state; will quit on the next round.
                break;
            
            case S_ACCEPT:
                // Already in the accept state; will quit on the next round.
                break;

            default:
                errno = E_INTERNAL;
                strerror = String.format(
                    "internal error; unhandled state: state=%d", state);
                state = S_ERROR;
                break;
        } // switch
    } // action()
} // class NumberParser

