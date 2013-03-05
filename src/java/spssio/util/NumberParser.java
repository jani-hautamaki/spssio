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
 * Parser for scientifically notated numbers expressed in an arbitrary base.
 *
 */
public class NumberParser
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
    
    /** 
     * Exponent overflow due its length, either positive or negative. 
     * Indicates that the exponent value exceeds numeric limits of {@code int}.
     * NOTE: There is no way of knowing accurately whether this indicates
     * an overflow or underflow.
     */
    public static final int E_EXPONENT_SIZE             = 0x0004;

    /** 
     * Mantissa overflow due its length.
     * Indicates that the mantissa value exceeds numeric limits of {@code double}.
     * NOTE: There is no way of knowing whether this indicates 
     * an overflow or underflow.
     */
    public static final int E_MANTISSA_SIZE             = 0x0006;
    
    /** An internal error, a strong indication of a bug. */
    public static final int E_INTERNAL                  = 0x0007;

    // DEFAULT CONFIGURATION
    //=======================
    
    /**
     * Default value for maximum input string length acceped for parsing.
     */
    static final int DEFAULT_MAX_INPUT_LENGTH = 128;

    // MEMBER VARIABLES
    //==================
    
    /**
     * A reference to the number system that is used.
     */
    private NumberSystem sys;

    /**
     * If not null, the intermediate results are calculated by using the BigDecimal
     * data type. Otherwise, Java's "double" primitive is used.
     */
    private MathContext mctx;
    
    // PARSE PRODUCTS
    //================
    
    /**
     * Buffer for individual digits that are extracted from the input string 
     * during parsing. It is better to have this as a member variable rather
     * than as a local variable, because otherwise the method would need to
     * allocate the array from the heap every time the function is called.
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

    // STATE MACHINE
    //===============

    /**
     * Current state.
     */
    private int state;
    
    /**
     * If true, the next transition is a null-transition (non-consuming).
     */
    private boolean eps;
    
    // (c and digit are unecessary)
    
    /**
     * Current input character, or -1 if <eof>.
     */
    private int c;
    
    /**
     * Current input {@code c} translated into a digit, or -1 if not a digit.
     */
    private int digit;
    
    // INPUT STREAM
    //==============
    
    /**
     * Input string
     */
    private String string;
    
    /**
     * Input string length as a local variable for convenience
     */
    private int stringlen;
    
    /**
     * Input string next read offset
     */
    private int index;
    
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
    
    public NumberParser() {
        sys = null;
        mctx = null;
        dtab = null;
        setMaxInputLength(DEFAULT_MAX_INPUT_LENGTH);
    } // ctor
    
    // CONFIGURATION
    //===============
    
    public void setNumberSystem(NumberSystem numsys) {
        sys = numsys;
    } // setNumberSystem()
    
    public void setMaxInputLength(int len) {
        // Reallocate dtab
        dtab = new int[len];
    }
    
    public void setMathContext(MathContext context) {
        mctx = context;
    }
    
    // ERROR INTERROGATION
    //=====================
    
    /**
     * Returns the error code associated with the last operation.
     * If the last operation was succesful, the value {@link #E_OK} is returned.
     * 
     * @return
     *      The error code, or [@code E_OK} if the last operation succeeded.
     */
    public int errno() {
        return errno;
    }
    
    /**
     * Returns the error message associated with {@link #errno()}.
     * If the error code is {@code E_OK}, the error message is undefined
     * and should not be used.
     *
     * @return
     *      The error message.
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
    
    // PUBLIC METHODS
    //===============
    
    /**
     * Parses a number (mantissa+exponent) into a double.
     * If an error occurs during parsing, the member variables 
     * {link #errno} and {link #strerror} are set accordingly.
     *
     * @param text
     *      The string to parse
     * 
     * @return
     *      The parsed value, if {@link #errno} is set to {@code E_OK}.
     *      Otherwise, the return value is {@link #DBL_ERRVALUE}.
     */
    public double parseDouble(String text) {
        
        if (sys == null) {
            state = S_ERROR;
            errno = E_INTERNAL;
            strerror = String.format("Set a NumberSystem object first");
            return DBL_ERRVALUE;
        }
        
        int c = -1;             // current input char
        int digit = -1;         // c as a digit, or -1 if non-digit
        
        dlen = 0;
        dbi = -1;
        digits_int = 0;
        esign = 0;
        msign = 0;
        
        state = S_START;
        eps = false;
        
        //reset(text);
        
        string = text;
        stringlen = text.length();
        index = 0;
        
        // Verify the input string's length. 
        // Do not process further, if the strng's too long.
        if (stringlen >= dtab.length) {
            errno = E_SYNTAX;
            strerror = String.format("syntax error; input string too long");
            state = S_ERROR;
            return DBL_ERRVALUE;
        } // if: too long

        do {
            // Read the next character,
            // unless this is a null-transition (eps==true).
            if (eps == false) {
                c = readc();
                
                if ((c >= 0) && (c < sys.toint.length)) {
                    digit = sys.toint[c];
                } else {
                    digit = -1;
                }
            } // if: consume
            
            // Null-transition defaults to false
            eps = false;
            
            // take action / make transition
            action(c, digit);
            
        } while ((state != S_ACCEPT) && (state != S_ERROR));
        
        // If the parse ended to an error, exit now
        if (state == S_ERROR) {
            return DBL_ERRVALUE;
        }
        
        int exp = 0;
        
        // If there were digits for an exponent, convert those frist.
        if (dbi != -1) {
            // esign != 0 implies "dbi" is valid
            
            exp = toInteger(dtab, dbi, dlen);
            
            if (state == S_ERROR) {
                errno = E_EXPONENT_SIZE;
                strerror = String.format("exponent overflow; number too long");
                return DBL_ERRVALUE;
            }

            // Take care of the exponent's sign.
            if (esign < 0) {
                exp = -exp;
            }
            
            // Discard the exponent data in dtab.
            dlen = dbi;
        } // if: has an exponent
        
        // Remove trailing zeros from the fraction part, if any.
        // (Doesn't affect the exponent's value).
        while ((digits_int < dlen) && (dtab[dlen-1] == 0)) {
            dlen--;
        } // while

        // Reset base index; It is next used to 
        // find the beginning of the non-zero digits in mantissa.
        dbi = 0;
        
        // Leading zeros in the integer part are stripped by the state machine.
        // Consequently, if there are any leading zeros in dtab[], 
        // they all must belong to the fractional part.
        //System.out.printf("exp (original): %d\n", exp);
        
        // Remove leading zeros of the fractional part.
        while ((dbi < dlen) && (dtab[dbi] == 0)) {
            dbi++;
        }
        
        // The removal of leading zeros after the "decimal point" has to be
        // accounted for in the exponent's value.
        //exp -= dbi;

        //System.out.printf("exp (trimmed): %d\n", exp);

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
            return DBL_ERRVALUE;
        }
        
        // If the number does not have fractional part nor exponent,
        // execute a quick exit here. 
        
        if ((exp == 0) && (digits_int == dlen-dbi)) {
            
            if (mctx != null) {
                dvalue = bvalue.doubleValue();
            } // if-else
            
        } else {
            
            // Now:
            //      result = value * base^(-digits_frac) * base^(exp)
            
            // calculateValue()
            dvalue = calculateDouble(
                dvalue, bvalue,
                digits_int,
                dlen-digits_int,
                exp
            );
            
            // Check for errors
            if (state == S_ERROR) {
                return DBL_ERRVALUE;
            }
            
        } // if-else

        // Account for the mantissa's sign
        if (msign < 0) {
            dvalue = -dvalue;
        } // if
        
        // Clear error indicators
        errno = E_OK;
        strerror = null;
        
        return dvalue;
    } // parsedouble()
    
    
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
     *      Calculated value as a double, if {@link #errno()} returns {@code E_OK}.
     */
    private double calculateDouble(
        double dvalue,
        BigDecimal bvalue,
        int digits_int,
        int digits_frac,
        int exp
    ) {
        
        state = S_ACCEPT;
        
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
            
            // Shift the position of the "decimal point" to left 
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
        
        //System.out.printf("%d.%d   shifting by %d\n", digits_int, digits_frac, pshift);
        
        // Adjust the number of digits in integer and fraction parts
        digits_int -= pshift;
        digits_frac += pshift;

        // Account for the shifts in the exponent's value.
        exp += pshift;
        
        // The following asserts hold:      digits_int == 1
        //                                  digits_int+digits_frac == dlen

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
            // Implement "digits_frac"
            if (mctx == null) {
                dvalue = dvalue * Math.pow(sys.base, -digits_frac);
                
            } else {
                bvalue = bvalue.divide(base_bigdecimal.pow(digits_frac));
                // Use dvalue as a temporarily variable
                dvalue = bvalue.doubleValue();
            } // if-else
            
            if (dvalue > sys.max_mantissa) {
                errno = E_OVERFLOW;
                strerror = String.format("Number overflow; mantissa out of range");
                state = S_ERROR;
            } // if: overflow
        } // if: overflow checking
        else if (exp+digits_int < sys.min_exponent) {
            errno = E_UNDERFLOW;
            strerror = String.format("number underflow; exponent out of range");
            state = S_ERROR;
        } else if (exp-digits_frac <= sys.min_exponent) {
            // Implied: min_exponent <= exp+digits_int
            // Implement "digits_frac+digits_int" (=dlen)
            int digits = digits_int+digits_frac;
            
            // Normalize mantissa for limit checking
            if (mctx == null) {
                // Normalize
                dvalue = dvalue * Math.pow(sys.base, -digits);
            } else {
                // Normalize
                bvalue = bvalue.divide(base_bigdecimal.pow(digits));
                // Use dvalue as a temporarily variable
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
        } // if: underflow checking
        else {
            // The value is just fine. Incorporate the fraction divider
            // into the exponent right away
            exp = exp - digits_frac;
        } // if-else
        
        if (state == S_ERROR) {
            return DBL_ERRVALUE;
        }
        
        /*
        debug_parse_double(
            digits_int, digits_frac, exp, eneg, pshift, 
            ipos, ebase, epos, dtab, 
            mant_dvalue, bbase, mant_bvalue
        ); 
        */
        
        if (mctx == null) {
            dvalue = dvalue * Math.pow(sys.base, exp);
        } else {
            // mctx != null
            if (exp < 0) {
                // This type has no problems with very negative exponents
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
     * Reads the next character from the input string.
     * Increases {@link #index} by one, if it is below {@link #stringlen}. 
     *
     * @return
     *      The next character, or -1 if no more characters left.
     */
    private int readc() {
        int c;
        
        if (index < stringlen) {
            c = string.charAt(index);
            index++;
        } else {
            c = -1;
        }
        
        return c;
    } // readc()
    
    /**
     *
     * The method affects the following member variables:
     * <ul>
     *   <li>{@code eps} - whether next transition is null-transition
     *   <li>{@code state} - what is the next state
     * </ul>
     * <p>
     *
     * And the following as a result of parsing:
     * <li>
     *   <li>{@code msign} - determines the mantissa's sign
     *   <li>{@code esign} - determines the exponent's sign
     *   <li>{@code dtab[dlen]} - current char as a digit at the position
     *   <li>{@code dlen} - writing index of the digit
     *   <li>{@code rshift} - how many digits to shift right afterwards
     *   <li>{@code digits_int} - how many digits before the "decimal point".
     *   <li>{@code exp} - exponent's value
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
                        "syntax error; unexpected character: %s", c > 0 ? c : "<eof>");
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

            default:
                errno = E_INTERNAL;
                strerror = String.format(
                    "internal error; unhandled state: state=%d", state);
                state = S_ERROR;
                break;
        } // switch
    } // action()
    
} // class NumberParser

