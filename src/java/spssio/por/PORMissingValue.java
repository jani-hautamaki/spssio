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
 * The class represents both the discrete missing values and ranges.
 *
 * Possible types (the tag code) and the corresponding definitions for values:
 * <ul>
 *    <li> 
 *    Tag code '8'. Discrete missing value. the value is in {@code value[0]}.
 *    There can up to three of these.
 *
 *    <li>
 *    Tag code '9'. Represents {@code LO THRU y}, where the value of {@code y}
 *    is in {@code value[0]}. May be followed by a discrete missing value.
 * 
 *    <li>
 *    Tag code 'A'. Represents [@code x THRU HI}, where the value of {@code x}
 *    is in {@code value[0]}. May be followed by a discrete missing value.
 *
 *    <li>
 *    Tag code 'B'. Represetns {@code x THRU y}, where the values of {@code x}
 *    and {@code y} are given in {@code value[0]} and in {@code value[1]},
 *    respectively. May be followed by a discrete missing value.
 *
 * </ul>
 */
public class PORMissingValue
{
    // CONSTANTS
    //===========

    /** An unassigned type */
    public static final int TYPE_UNASSIGNED             = -1;

    /** A discrete value. */
    public static final int TYPE_DISCRETE_VALUE         = (int) '8';

    /** An open interval unclosed from the high end, {@code [x, +inf]}. */
    public static final int TYPE_RANGE_OPEN_LO          = (int) '9';

    /** An open interval unclosed from the low end, {@code [-inf, y]}. */
    public static final int TYPE_RANGE_OPEN_HI          = (int) 'A';

    /** A closed interval, {@code [x, y]}. */
    public static final int TYPE_RANGE_CLOSED           = (int) 'B';

    // MEMBER VARIABLES
    //==================

    /**
     * The type of the missing value specification.
     */
    public int type;

    /**
     * The values related to the missing value specification
     */
    public PORValue[] values;
    // TBC: use singular instead of plural?

    // CONSTRUCTORS
    //==============

    /** 
     * Creates an uninitialized missing value specification
     */
    public PORMissingValue() {
        type = TYPE_UNASSIGNED;
        values = null;
    } // ctor

    /** 
     * Creates a missing value specification with a specified type,
     * and with empty array.
     */
    public PORMissingValue(int type) {

        int size = 0;

        switch(type) {
            case TYPE_DISCRETE_VALUE:
                size = 1;
                break;

            case TYPE_RANGE_OPEN_LO:
                size = 1;
                break;

            case TYPE_RANGE_OPEN_HI:
                size = 1;
                break;

            case TYPE_RANGE_CLOSED:
                size = 2;
                break;

            default:
                throw new IllegalArgumentException(String.format(
                    "invalid type: \'%c\' (%d)", (char) type, type));
        } // switch

        // Create an array for PORValue object references. 
        // These are initialized to null I think..
        this.values = new PORValue[size];

        // Assign the type
        this.type = type;
    } // PORMissingValue()


} // class PORMissingValue
