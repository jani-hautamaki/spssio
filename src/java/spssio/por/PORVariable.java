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

package spssio.por;

// core java
import java.util.Vector;
// spssio common
import spssio.common.SPSSFormat;

/**
 * Tag code '7', variable record.<p>
 *
 * Relevant documentation:
 * <ul>
 *    <li>
 *    <a href="http://www.gnu.org/software/pspp/pspp-dev/html_node/Variable-Records.html#Variable-Records">
 *       A.8 Variable Records
 *    </a>
 * </ul>
 *
 */
public class PORVariable {
    
    // MEMBER VARIABLES
    //==================
    
    /**
     * Width of the variable. This is 0 for a numeric variable, 
     * and a number between 1 and 255 for a string variable. 
     */
    public int width;
    
    /**
     * Short name of the variable. 1-8 characters long. Must be in all capitals. 
     */
    public String name;
    
    /** 
     * Print format. Same as in SAVFile.
     */
    public SPSSFormat printfmt;
    
    /**
     * Write format. Same as in SAVFile.
     */
    public SPSSFormat writefmt;
    
    /**
     * Tag codes '8', '9', 'A' and 'B', missing value specifications (optional).
     * If there aren't any missing values, the vector length is zero.
     */
    public Vector<PORMissingValue> missvalues;
    
    /**
     * Tag code 'C', variable label (optional).
     * Portable files max 255 chars while System files max 120 chars.
     * If the field is not present, this is set to {@code null}.
     */
    public String label;
    
    // CONSTRUCTORS
    //==============
    
    /** Create an uninitialized variable */
    public PORVariable() {
        width = -1;
        name = null;
        printfmt = new SPSSFormat(); 
        writefmt = new SPSSFormat(); 
        missvalues = new Vector<PORMissingValue>(3);
        label = null;
    } // ctor
    
} // class PORVariable