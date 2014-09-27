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


public class PORValue
{
    // CONSTANTS
    //===========

    /** The cell has unassigned type. */
    public static final int TYPE_UNASSIGNED     = -1;

    /** The cell has numeric type. */
    public static final int TYPE_NUMERIC        =  0;

    /** The cell has textual type. */
    public static final int TYPE_STRING         =  1;

    // MEMBER VARIABLES
    //==================

    /**
     * Determines the type of the cell:
     */
    public int type;

    /**
     * The literal contents of the cell, or {@code null} if the cell is empty.
     * If the type is numeric, then this is the numeric value represented as
     * a string, and the string is identical to the one that was found from or will
     * be written to the Portable file.
     */
    public String value;

    // CONSTRUCTORS
    //==============

    /**
     * Creates an empty cell with unassigned type.
     */
    public PORValue() {
        type = TYPE_UNASSIGNED;
        value = null;
    } // ctor

    /**
     * Creates a cell with specified type and value.
     */
    public PORValue(int type, String value) {
        this.type = type;
        this.value = value;
    } // ctor

    // OTHER METHODS
    //===============


} // class PORValue
