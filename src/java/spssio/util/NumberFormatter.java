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


package spssio.util;

// for arbitrary precision
import java.math.BigDecimal;
import java.math.MathContext;

// for testing
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;


public class NumberFormatter {
    
    // CONSTANTS
    //===========
    
    /**
     * Default buffer size for 256 characters.
     */
    public static final int DEFAULT_BUFFER_SIZE             = 256;
    
    /**
     * Initial value of the precision.
     * Indicates that the precision hasn't been initialized yet.
     */
    public static final int PRECISION_UNSET                 = -1;
    
    // MEMBER VARIABLES
    //==================
    
    /**
     * Serialization precision, that is, the maximum number of digits
     * serialized for the mantissa. the sign, the "decimal" point,
     * and exponent are not counted.
     */
    private int precision;
 
    /**
     * A reference to the number system that is used.
     */
    private NumberSystem numberSystem;
 
    /**
     * Indicates the {@code MathContext} to be used with {@code BigDecimal}
     * data type for calculating the intermediate results.
     * If null, then Java's {@code double}s are used instead 
     * for the intermediate results.
     */
    private MathContext mctx;

    /**
     * Reference to a buffer which is used to for storing
     * the serializations.
     */
    private int[] buffer;
    
    /** 
     * Writing head's position. The head also indicates the length
     * of valid data in the buffer.
     */
    private int head;
    
    /**
     * Internal buffer used for store the digits before serializing
     * them into the actual buffer.
     */
    private int[] dtab;
    
    /**
     * The remainder of the last serialization operation.
     * This is stored only for inspection and debugging purposes.
     */
    private double remainder;

    // CONSTRUCTORS
    //==============
    
    /** 
     * Create an uninitialized {@code NumberFormatter}.
     * The number system must be set before usage.
     */
    public NumberFormatter() {
        
        // Allocate buffer with default size.
        buffer = new int[DEFAULT_BUFFER_SIZE];
        dtab = new int[DEFAULT_BUFFER_SIZE];
        
        // Reset head
        head = 0;
        
        // Mark the precision as invalid
        precision = PRECISION_UNSET;
        
        // Unset the number system
        numberSystem = null;
        
        // Unset the math context (ie. use doubles)
        mctx = null;
    } // ctor
    
    /**
     * Create an initialized {@code NumberFormatter} with
     * default precision for the given number system.
     *
     * @param numsys The number system to be used
     */
    public NumberFormatter(NumberSystem numsys) {
        this(); // call base constructor
        
        // Validate the parameter
        if (numsys == null) throw new IllegalArgumentException();
        
        // Set the number system with the default precision
        setNumberSystem(numsys);
    } // ctor

    /**
     * Create an initialized {@code NumberFormatter} with
     * the specified precision
     * 
     * @param numsys The number system to be used
     * @param precision The precision to be used
     */
    public NumberFormatter(NumberSystem numsys, int precision) {
        this(); // call base constructor
        
        // Validate the parameter
        if (numsys == null) throw new IllegalArgumentException();
        
        // Set the number system with the given precision
        setNumberSystem(numsys, precision);
    } // ctor
    
    // OTHER METHODS
    //===============
    
    /** 
     * Set the number system, and use the default precision for it,
     * if the number system is initialized.
     * 
     * @param numsys The number system to be used
     */
    public void setNumberSystem(NumberSystem numsys) {
        // Validate the parameter
        if (numsys == null) throw new IllegalArgumentException();
        
        // Set the number system
        this.numberSystem = numsys;
        
        // If the base has been set in the number system,
        // use default precision. Otherwise, reset the precision to
        // uninitialized state.
        if (numsys.base != NumberSystem.BASE_UNSET) {
            setDefaultPrecision();
        } else {
            precision = PRECISION_UNSET;
        }
    } // setNumberSystem()
    
    /**
     * Set the number system and the precision.
     * 
     * @param numsys The number system to be used.
     * @param precision The precision to be used.
     */
    public void setNumberSystem(NumberSystem numsys, int precision) {
        // Validate the parameters
        if (numsys == null) throw new IllegalArgumentException();
        if (precision  < 1) throw new IllegalArgumentException();
        if (precision >= buffer.length-2) {
            throw new IllegalArgumentException("precision too big");
        }
        
        // Set the parameters
        this.numberSystem = numsys;
        this.precision = precision;
    }
    
    /**
     * Get the underlying {@code NumberSystem}.
     * 
     * @return The underlying {@code NumberSystem} object.
     */
    public NumberSystem getNumberSystem() {
        return numberSystem;
    }
    
    /**
     * Set the precision used by the formatter.
     * 
     * @param newPrecision The precision to be used
     */
    public void setPrecision(int newPrecision) {
        if (newPrecision < 1) throw new IllegalArgumentException();
        this.precision = newPrecision;
    }
    
    /**
     * Set the precision used by the formatter to a default value.
     * The default value is calculated from the number system's base.
     * Therefore, the number system must be initialized prior to this.
     */
    public void setDefaultPrecision() {
        if (numberSystem == null) {
            throw new RuntimeException(
                "Unable to set default precision: NumberSystem is null");
        }
        precision = getDefaultPrecision(numberSystem);
    }
    
    /**
     * Calculate default precision for the specified {@code NumberSystem}.<p>
     *
     * The default precision is defined as the number of digits required
     * in the specified {@code NumberSystem} to express the biggest value
     * that can be stored into the mantissa/significand of Java's 
     * {@code double}s. They are IEEE double precision floating points.<p<
     *
     * The IEEE double precision floating point is defined to have 53 bits
     * in the mantissa, with 52 bits explicitly stored. If the radix
     * of the number system is N, the default precision is then given
     * by:<p>
     * 
     *      logN(2^53)<p>
     *
     * which gives<p>
     * 
     *      defaultPrecision = ln(2^53) / ln(N)<p>
     * 
     * To emphasize: the default precision is the number of base-N digits 
     * needed to express the biggest mantissa value.
     *
     * @param numberSystem The number system for which to calculate
     *      the default precision
     *
     * @return The default precision for the specified {@code NumberSystem}.
     */
    public static int getDefaultPrecision(NumberSystem numberSystem) {
        // Validate parameters
        if (numberSystem == null) {
            throw new IllegalArgumentException(
                "Illegal NumberSystem specified: null");
        }
        if (numberSystem.base == NumberSystem.BASE_UNSET) {
            throw new RuntimeException(
                "Illegal NumberSystem specified: the base is unset");
        }
        
        // Calculate the default precision.
        // Java's doubles are IEEE double precision floating points,
        // so they have 53 bit mantissa's (with 52 bits explicitly stored).
        // Therefore, it takes
        //
        //      53*ln(2) / ln(N) 
        //
        // base-N digits to express the mantissa completely.
        
        int defaultPrecision = (int) Math.ceil(
            (53.0*Math.log(2.0)) / Math.log(numberSystem.base)
        ); // ceil()
        
        return defaultPrecision;
    }
    
    /**
     * Get the precision used by the formatter. 
     * 
     * @return The precision used by the formatter.
     */
    public int getPrecision() {
        return precision;
    }
    
    /** 
     * Set or unset the math context for {@code BigDecimal}s.
     * Setting the math context will enable the use of {@code BigDecimal}s
     * for intermediate calculations. They are more accurate than
     * Java's {@code double}s, but the calculations on them are slower.
     * So it is a trade-off between accuracy and speed.
     * 
     * @param context The settings to be used with {@code BigDecimal}s.
     *      Pass {@code null} to unset the math context, and to use
     *      {@code double}s instead.
     */
    public void setMathContext(MathContext context) {
        mctx = context;
    }
    
    /**
     * Get the currently used settings for {@code BigDecimal}s.
     *
     * @return The currently used {@code} MathContext object, 
     *      or {@code null} if none.
     */
    public MathContext getMathContext() {
        return mctx;
    }
    
    
    /**
     * Return serialized number from the buffer as a {@code String}.
     * 
     * @return The last serialization
     */
    public String getString() {
        return new String(buffer, 0, head);
    }
    
    /**
     * Get the serialization buffer used by the formatter.
     * An application requiring high-throughput serialization should
     * read the serialized number directly from this buffer, thus
     * avoiding creation of unnecessary {@code String} objects.<p>
     *
     * After a formatting call, the buffer contains a null-terminated
     * serialization.<p>
     * 
     * @return Reference to the serialization buffer used.
     */
    public int[] getBuffer() {
        return buffer;
    }
    
    /**
     * Set the serialization buffer used by the formatter.
     * An application can set the serialization buffer explicitly.
     * This might be preferable if a same buffer in an object is used 
     * for various serializations.<p>
     * 
     * Setting the buffer causes a reallocation of an internal array
     * {@link #dtab}, so that their sizes match.<p>
     * 
     * @param newBuffer Reference to the buffer to be used for
     * serializations. {@code null} is not allowed.
     */
    public void setBuffer(int[] newBuffer) {
        // Validate
        if (newBuffer == null) throw new IllegalArgumentException();
        
        // Set the buffer
        buffer = newBuffer;
        // reallocate internal buffer
        dtab = new int[buffer.length];
    }
  
    /**
     * Format a double value into the serialization buffer.
     * The return value can be used to determine the end of the string
     * in the serialization buffer.
     * 
     * @param value The value to be serialized
     * 
     * @return Number of characters written to the serialization buffer.
     * 
     */
    public int formatDouble(
        double value
    ) {
        // Reset head position
        head = 0;
        
        // If a minus sign is needed, add it and take the complement
        // of the value for further processing.
        //if ((Double.doubleToRawLongBits(value) & 0x8000000000000000L) != 0) {
        if (value < 0.0) {
            // Put minus sign in the front.
            buffer[head++] = numberSystem.minus_char;
            // Take complement
            value = -value;
        }
        
        // Head pointer for the dtab[] array
        int dlen = 0;
        
        // Calculate the amount of shifting needed
        // TODO: Use precalculated tables for these.
        // TODO: Determine the numeric limits too.
        int exp = 0;
        
        // If value is zero, don't do this.
        if (value != 0.0) {
            exp = (int) Math.floor(
                Math.log(value) / Math.log(numberSystem.base)
            ); // floor()
        }

        // Number of digits to serialize.
        // The value is determined below.
        int ndigits;
        
        // Is the value an integer?
        boolean isInteger = ((double) ((long) value)) == value;
        
        // Determine the number of digits to print.
        //
        // If the input value is an integer, then the number of digits
        // is limited to mitigate the effects caused by the inaccuracies
        // caused by doubles. An example: by default,
        // value "500.0" (dec) produces "GK.000000001".
        
        if (isInteger) {
            ndigits = exp+1;
            if (ndigits > precision) {
                ndigits = precision;
            }
        } else {
            ndigits = precision;
        } // if-else
        
        // Serialize using doubles or BigDecimals
        if (mctx == null) {
            // Use doubles.
            
            // Convert the base into a double only once.
            double base = numberSystem.base;
            
            // Divide the value so that the it is normalized between
            // 0 <= value < base
            value = value / Math.pow(base, exp);
            
            // NOTE 1:: There is a slight possibility that Math.log() 
            // returned a bigger value than it should have. 
            // For example, for double=656099999999.9994 the Math.Log() 
            // for base=30 gives exactly 8.0, which will cause the value 
            // to become less than 1 in the division above.
            // NOTE 2: Math.log() may also return a smaller value than it
            // should. For example, with base=10, double=1000.0 returns
            // 2.99999999999999960 which is then floored to 2.0
            //
            // Therefore, this is a dirty hack xxxx for asserting
            // that the first digit is never zero or >= base
            //
            // TODO: Searching the log() value from a lookup table with 
            // comparision would avoid this.
            if (value < 1.0) {
                value *= base;
                exp--;
            } else if (value >= base) {
                value /= base;
                exp++;
            }
            
            
            // This serialization assumes that in the most cases
            // there is no need to prepend the serialized number.
            
            // Serialize a digit x "ndigits"
            for (int i = 0; i < ndigits; i++) {

                // value > 0, so (int) gives floor()
                int d = (int) value;
                
                // Write the digit into the buffer
                dtab[dlen++] = d;
                
                // Update the value
                value = (value - d) * base;
                // TODO: Check if the value became zero.
            } // for
            
        } else {
            // Use BigDecimals and MathContext
            
            BigDecimal bbase = new BigDecimal(numberSystem.base, mctx);
            BigDecimal bvalue = new BigDecimal(value, mctx);
            
            bvalue = bvalue.divide(bbase.pow(exp, mctx), mctx);

            // XXX: dirty hack, see above for explanation
            if (bvalue.compareTo(BigDecimal.ONE) < 0) {
                bvalue = bvalue.multiply(bbase, mctx);
                exp--;
            } else if (bvalue.compareTo(bbase) >= 0) {
                bvalue = bvalue.divide(bbase, mctx);
                exp++;
            }
            
            for (int i = 0; i < ndigits; i++) {
                // Discard fractional part 
                int d = bvalue.intValue();
                
                dtab[dlen++] = d;
                bvalue = bvalue.subtract(new BigDecimal(d, mctx), mctx);
                bvalue = bvalue.multiply(bbase, mctx);
            } // for
            
            value = bvalue.doubleValue();
        } // if-else
        
        // Save the remainder into a member variable for debugging
        remainder = value;
        
        // Step the head backwards by one char. 
        // Then it points to the last valid digit in the array.
        dlen--;
        
        // TODO: Include an option for rounding towards nearest odd/even
        // Inspect the remainder to see is the last digit rounded upwards?
        if (value >= numberSystem.base/2) {
            // Round upwards.
            
            // Inspect digits right-to-left to find 
            // the first digit which doesn't cause overflow.
            while ((dlen >= 0) && (dtab[dlen]+1 == numberSystem.base)) {
                dlen--;
            }
            
            if (dlen >= 0) {
                // Implement the increment (won't overflow)
                dtab[dlen]++;
            } else {
                // Append the number by one; this resets the whole
                // serialization to a single digit plus an exponent.
                dtab[0] = 1;
                dlen = 0;
                exp++;
            }
        } // if: round upwards
        
        // Remove trailing zeros, if any. There can be trailing zeros, 
        // if the rounding up didn't take place.
        while ((dlen > 0) && (dtab[dlen] == 0)) {
            dlen--;
        }
        
        // Examine the serialization. If it has only one digit which
        // is zero, then a shortcut exit can be taken to return the value.
        if ((dlen == 0) && (dtab[0] == 0)) {
            // Reset head (discard the sign, if any)
            head = 0;
            // Write zero into the buffer
            buffer[head++] = numberSystem.tochar[0];
            // Finish the buffer
            buffer[head] = 0;
            // And exit
            return head;
        } // if: serialization is zero

        // Examine the exponent which is to be outputted
        
        // Value to be used for exponent
        int pow = 0;
        // Location of the "decimal point"
        int pointpos = -1;
        
        //System.out.printf("exp: %d, dlen: %d\n", exp, dlen);
        // Determine the "decimal point" location and exponent value.
        if ((exp >= -1) && (exp < dlen)) {
            // Insert a point, and no exponent.
            pointpos = exp+1;
            pow = 0;
        } else {
            // No point, but an exponent
            pointpos = -1;
            pow  = exp - dlen;
        } // if-else
        
        // Translate the digits into characters
        for (int i = 0; i <= dlen; i++) {
            if (i == pointpos) {
                buffer[head++] = numberSystem.point_char;
            }
            
            int digit = dtab[i];
            buffer[head++] = numberSystem.tochar[digit];
        } // for
        
        // Serialize the exponent if non-zero.
        if (pow != 0) {
            head = appendInt(pow, true, buffer, head);
        }
        
        // Finish the buffer
        buffer[head] = 0;
        
        return head;
    } // formatDouble()
    
    /**
     * Format an integer into the serialization buffer.
     * 
     * @param value The integer to be serialized
     * 
     * @return Number of characters written into the buffer.
     */
    public int formatInt(int value) {
        // Serialize the integer value
        head = appendInt(value, false, buffer, 0);
        
        return head;
    }
    
    /**
     * Append the buffer by serializing an integer.
     * 
     * @param value The integer to be serialized
     * @param signAlways Whether the sign should always be emitted.
     * @param buffer The buffer where the serialization is written to
     * @param head Starting offset for the serialization
     * @return The new head value
     */
    protected int appendInt(
        int value, 
        boolean signAlways,
        int[] buffer,
        int head
    ) {
        if (value >= 0) {
            if (signAlways == true) {
                // Write positive sign.
                buffer[head++] = numberSystem.plus_char;
            }
        } else {
            // value < 0
            
            // Write negative sign
            buffer[head++] = numberSystem.minus_char;
            // Take complement
            value = -value;
        }
        
        // Serialize the value into dtab[] buffer in reverse order.
        int dlen = 0;
        
        // Note: if the number is zero, it is written once.
        
        do {
            // Take the remainder, ie. extract the digit
            int d = value % numberSystem.base;
            
            // Subtract it from the value, now value is modulo base
            value -= d;
            
            // Divide by the modulo. The division is exact.
            value = value / numberSystem.base;
            
            // Record the extracted digit.
            dtab[dlen++] = d;
            
            // Loop until the value is zero
        } while (value != 0);
            
        // Serialize the digits into the buffer in reverse order
        for (dlen--; dlen >= 0; dlen--) {
            buffer[head++] = numberSystem.tochar[dtab[dlen]];
        }
        
        return head;
    } // appendInt()
    
    
    /**
     * Reformats a number to given precision. If the number is already
     * within the precision, the buffer is left untouched. Otherwise,
     * the number is reformatted.
     * 
     * @param buffer The buffer containing the number to reformat
     * @param len Length of the buffer in number of characters.
     * @param newPrecision The new precision
     * @return The updated length of the buffer
     *
     * TODO: Should the method be renamed into reformatNumber() ?
     */
    public int reformat(int[] buffer, int len, int newPrecision) {
        // Offset for dtab[]
        int dlen = 0;
        // Offset for data[] array
        int pos = 0;
        // Current char
        int c; 
        // Determines the number of digits the decimal point
        // has been shifted left.
        int lshift = -1;
        // exponent sign: 0 none, 1 positive, -1 negative.
        int esign = 0;

        // Reset head
        int head = 0;
        
        // Number of leading zeros skipped
        int skipped = 0;
        
        // read the first char
        c = buffer[pos];
        
        // If the first character is a sign, it is emitted right away
        if ((c == numberSystem.minus_char)
            || (c == numberSystem.plus_char))
        {
            head++;
        }
        
        // Translate the data into digits
        for (; pos < len; pos++) {
            c = buffer[pos];
            
            if (c == numberSystem.point_char) {
                lshift = pos;
            }
            else if (c == numberSystem.plus_char) {
                // Positive exponent, quit loop
                esign = 1;
                break;
            }
            else if (c == numberSystem.minus_char) {
                // Negative exponent, quit loop
                esign = -1;
                break;
            } else {
                // Assume it is a digit
                int d = numberSystem.toint[c];
            
                // Validate
                if (d == -1) {
                    throw new RuntimeException(String.format(
                        "Not a digit: %c\n", c));
                }
                if ((dlen > 0) || (d != 0)) {
                    // Write to the dtab
                    dtab[dlen++] = d;
                } else {
                    // Skipping a leading zero
                    skipped++;
                }
            } // if-else
        } // for
        
        // Time to check is reformatting really necessary
        if (dlen+skipped <= newPrecision) {
            // Reformatting is NOT needed
            return len;
        }
        
        // Reaching this point implies that reformatting IS needed.

        // If the decimal point was met, calculate the actual shift
        if (lshift != -1) {
            lshift = pos-1 - lshift;
        } else {
            lshift = 0;
        }
        
        // Parse the exponent if it is present.
        int exp = 0; // exponent value
        
        if (esign != 0) {
            pos++;
            for (; pos < len; pos++) {
                c = buffer[pos];
                exp *= numberSystem.base;
                exp += numberSystem.toint[c];
            }
            // Apply sign
            if (esign < 0) {
                exp = -exp;
            }
        }
        
        // Since we are going to reuse the code from formatDouble(),
        // the exponent needs to be "inversed" so that it matches
        // to value which would divide mantissa into range [0,base[.
        
        //exp = -exp;
        // To that value the number of integer digits is added
        exp += (dlen-1-lshift);
        
        // Set dlen to the last valid digit
        if (dlen > newPrecision) {
            // The number had excess digits; last can be used 
            // as a remainder for rounding.
            dlen = newPrecision-1;
        } else {
            // No excess digits; no rounding. The dlen is currently
            // pointing past the last valid digit - that is going to
            // be used as a remainder, so it is set to zero,
            // and then the dlen is decreased by one.
            dtab[dlen] = 0;
            dlen--;
        } 
        
        // See if the number has to be rounded upwards
        if (dtab[dlen+1] >= numberSystem.base/2) {

        // FROM THIS POINT ON THIS IS A COPY-PASTE FROM formatDouble()
        //=============================================================
            
            // Inspect digits right-to-left to find 
            // the first digit which doesn't cause overflow.
            while ((dlen >= 0) && (dtab[dlen]+1 == numberSystem.base)) {
                dlen--;
            }
            
            if (dlen >= 0) {
                // Implement the increment (won't overflow)
                dtab[dlen]++;
            } else {
                // Append the number by one; this resets the whole
                // serialization to a single digit plus an exponent.
                dtab[0] = 1;
                dlen = 0;
                exp++;
            }
        } // if: round upwards
        
        // Remove trailing zeros, if any. There can be trailing zeros, 
        // if the rounding up didn't take place.
        while ((dlen > 0) && (dtab[dlen] == 0)) {
            dlen--;
        }
        
        // Examine the serialization. If it has only one digit which
        // is zero, then a shortcut exit can be taken to return the value.
        if ((dlen == 0) && (dtab[0] == 0)) {
            // Reset head (discard the sign, if any)
            head = 0;
            // Write zero into the buffer
            buffer[head++] = numberSystem.tochar[0];
            // Finish the buffer
            buffer[head] = 0;
            // And exit
            return head;
        } // if: serialization is zero

        // Examine the exponent which is to be outputted
        
        // Value to be used for exponent
        int pow = 0;
        // Location of the "decimal point"
        int pointpos = -1;
        
        // Determine the "decimal point" location and exponent value.
        if ((exp >= -1) && (exp < dlen)) {
            // Insert a point, and no exponent.
            pointpos = exp+1;
            pow = 0;
        } else {
            // No point, but an exponent
            pointpos = -1;
            pow  = exp - dlen;
        } // if-else
        
        // Translate the digits into characters
        for (int i = 0; i <= dlen; i++) {
            if (i == pointpos) {
                buffer[head++] = numberSystem.point_char;
            }
            
            int digit = dtab[i];
            buffer[head++] = numberSystem.tochar[digit];
        } // for
        
        // Serialize the exponent if non-zero.
        if (pow != 0) {
            head = appendInt(pow, true, buffer, head);
        }
        
        // Finish the buffer
        buffer[head] = 0;
        
        return head;
    } // reformat()

} // class NumberFormatter