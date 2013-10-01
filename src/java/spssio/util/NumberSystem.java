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

/**
 *
 * The following diagram is meant to clarify the terminology,
 * especially the meaning of the terms <i>underflow</i> and <i>overflow</i>
 * <code><pre>
 *  
 *          -MAX_VALUE   -MIN_VALUE     ZERO      MIN_VALUE   +MAX_VALUE
 *                 |        |             |             |        |                 
 *       Negative  |        |  Negative   |  Positive   |        |  Positive       
 *       Overflow  |        |  Underflow  |  Underflow  |        |  Overflow       
 *                 |        |             |             |        |                 
 * </code></pre>
 * <p>
 * 
 * <h4>TODO</h4>
 * <ul>
 *   <li>The numeric limits are properties that emerge from the relationship
 *     between a NumberSystem and a data type (eg. {@code double}, 
 *     {@code float}, or {@code BigDecimal}). Therefore, the limits should be
 *     separated from the {@code NumberSystem} classinto their own class (eg. 
 *     {@code SystemLimits}, which associates to a {@code NumberSystem} and 
 *     to a {@code Class<?>} corresponding to the underlying data type.</li>
 * </ul>
 *   
 */
public class NumberSystem
{
    
    // CONSTANTS
    //===========
    
    /**
     * The initial value for the base indicating that the base hasn't
     * been set yet.
     */
    public static final int BASE_UNSET = -1;
    
    // MEMBER VARIABLES
    //==================
    
    /**
     * The character used for plus sign (default '+').
     */
    char plus_char = '+';
    
    /**
     * The character used for minus sign (default '-').
     */
    char minus_char = '-';
    
    /**
     * The character used for dot/point (default '.').
     */
    char point_char = '.';
    
    /**
     * Quick conversion table from ASCII char to integer.
     * The length of the array is expected to be 256. Invalid entries are
     * marked with -1.
     */
    int[] toint = null;
    
    // TODO:
    // if lowercase letters are equated with the uppercase letters,
    // then populate those too into toint[] array.
    
    /**
     * Quick conversion table from integer to ASCII char.
     * The length of the array is expected to be equal to {@code opt_base}.<p>
     * 
     * These are the digits of the number system from zero to {@code opt_base-1}.
     */
    int[] tochar = null;

    /**
     *  The radix, also known as the base, of the number system.
     *  If unset, the base is set to -1.
     */
    int base;
    
    /**
     * Maximum positive double value which can be multiplied by  the base 
     * without overflow.
     */
    double max_double;
    
    /**
     * Smallest POSITIVE double value which can be divided by the base without 
     * underflow.
     */
    double min_double;
    
    /**
     * Maximum long value which can be multiplied by the base without overflow.
     */
    long max_long;
    
    /**
     * Maximum int value which can be multiplied by the base without overflow.
     */
    int max_int;

    /**
     * The highest exponent in the current base within the numeric limits
     * of {@code Double} data type. Otherwise the data type overflows.
     */
    int max_exponent;

    /**
     * The smallest exponent in the current base within the numeric limits
     * of {@code Double} data type. Otherwise the data type underflows.
     */
    int min_exponent;
    
    /**
     * The highest possible mantissa value when exponent==max_exponent within
     * the numeric limits of {@code Double} data type. 
     * Otherwise the data type overflows.
     */
    double max_mantissa;
    
    /**
     * The smallest possible mantissa value when exponent==min_exponent within
     * the numeric limits of {@code Double} data type.
     * Otherwise the data type underflows.
     */
    double min_mantissa;

    /**
     * Powers of the base up to the numeric limit of double, 
     * and {@code pow.length-1} is the maximum exponent a number may 
     * have in the current base.
     */
    double[] pow;
    
    // CONSTRUCTORS
    //==============
    
    /** 
     * Default constructor; leaves the base (and digits) unset. 
     * The base (and digits) must be set before usage.
     */
    public NumberSystem() {
        // toint[] array is allocated only once,
        // (whereas tochar[] table is reallocated when the radix changes)
        toint = new int[256];
        
        // Keep the base unset. The number system is not initialized 
        // until user explicitly requests so.
        base = BASE_UNSET;
    } // ctor
    
    /**
     * Construct with the given radix and digits.
     *
     * @param base 
     *      The radix to use
     * @param digits 
     *      The digits to use, or {@code null} if automatically 
     *      generated digits shall be used instead.
     *
     */
    public NumberSystem(int base, String digits) {
        this();
        
        if (digits != null) {
            setNumberSystem(base, digits);
        } else {
            setBase(base);
        }
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
    } // getPlusChar()
    
    public char getPointChar() {
        return point_char;
    } // getPointChar()
    
    
} // class NumberSystemHelper
