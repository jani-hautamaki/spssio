
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


package spssio.common;

/** 
 * This is the format specification used for specying the print and the write
 * format by both Portable and System files.<p>
 *
 * Relevant documentation:
 * <ul>
 *   <li>
 *      <a href="http://www.gnu.org/software/pspp/pspp-dev/html_node/Variable-Record.html#Variable-Record">
 *         B.2 Variable Record
 *      </a> (see the bottom of the page)
 * </ul>
 */
public class SPSSFormat {

    // CONSTANTS
    //===========

    /**
     * Used as an invalid value for {@link #type}.
     */
    public static final int INVALID_TYPE = -1;

    /**
     * Used as an invalid value for {@link #width}.
     */
    public static final int INVALID_WIDTH = -1;

    /**
     * Used as an invalid value for {@link #decimals}.
     */
    public static final int INVALID_DECIMALS = -1;

    // MEMBER VARIABLES
    //==================

    /**
     * Format type.
     */
    public int type;

    /**
     * Format width, 1-40.
     */
    public int width;

    /**
     * Number of decimal places 1-40.
     */
    public int decimals;

    // CONSTRUCTORS
    //==============

    /**
     * Initialize all member variables to their invalid values.
     */
    public SPSSFormat() {
        type = INVALID_TYPE;
        width = INVALID_WIDTH;
        decimals = INVALID_DECIMALS;
    } // ctor

    /**
     * Initialize member variables to specified values.
     */
    public SPSSFormat(int t, int w, int d) {
        type = t;
        width = w;
        decimals = d;
    } // ctor

    // OTHER METHODS
    //===============

} // class FmtSpec


