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

/**
 * Represents SPSS/PSPP's Portable file.
 */
public class PORFile {
    
    // MEMBER VARIABLES
    //==================
    
    /**
     * Portable file header. 
     * The header contains fields for the following tag codes:
     * <ul>
     *    <li>'1' (product)
     *    <li>'2' (author)
     *    <li>'3' (file label)
     *    <li>'4' (variable count)
     *    <li>'5' (precision)
     *    <li>'6' (case weight variable)
     * </ul>
     */
    public PORHeader header;
    
    /**
     * Sequence of variable records (tag code '7', struct).
     * The variable records contains the following tag codes:
     * <ul>
     *    <li>'7' (the variable record itself)
     *    <li>'8', '9', 'A' and 'B' (missing value specifications)
     *    <li>'C' (variable label)
     * </ul>
     */
    public Vector<PORVariable> variables;
    
    /**
     * Sequence of value labels records (tag code 'D').
     */
    public Vector<PORValueLabels> labels;
    
    /**
     * Document record (tag 'E', vector of strings)
     */
    // TODO
    
    /**
     * Data record (tag 'F', sequence of floating-point and string fields)
     */
    //public PORDataMatrix data;
    
    
    // CONSTRUCTORS
    //==============
    
    // OTHER METHODS
    //===============
    
} // class PORFile