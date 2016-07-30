//*******************************{begin:header}******************************//
//             spssio - https://github.com/jani-hautamaki/spssio             //
//***************************************************************************//
//
//      Java classes for reading and writing
//      SPSS/PSPP Portable and System files
//
//      Copyright (C) 2013-2016 Jani Hautamaki <jani.hautamaki@hotmail.com>
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
import java.util.Map;
import java.util.LinkedHashMap; // for preserving the order of key-value pairs
import java.util.Vector;

/**
 * Tag code 'D', value label record. In the Portable file these records have
 * the following structure:</p>
 *
 * <ul>
 *    <li>integer {@code var_count}: variable count.
 *    <li>string {@code vars[]}: list of variables ({@code var_count} elements).
 *    <li>integer {@code labl_count}: label count.
 *    <li>keyvalue {@code mapping[]}: list of (value, label) pairs
 *        ({@code labl_count} elements). Value is either numeric or string,
 *        and label is a string.
 * </ul>
 * <p>
 *
 * Relevant PSPP documentation:
 * <ul>
 *   <li>
 *   <a href="http://www.gnu.org/software/pspp/pspp-dev/html_node/Value-Label-Records.html#Value-Label-Records">
 *      A.9 Value Label Records
 *   </a>
 * </ul>
 */
public class PORValueLabels
{

    // MEMBER VARIABLES
    //==================

    /**
     * Determines the type of the values.
     * Uses same constants are PORValue
     */
    public int type;

    /**
     * List of the variables.
     * TODO: rename into variables
     */
    public Vector<PORVariable> vars;

    /**
     * List of the (value, label) pairs. The {@code Map} is an order-preserving map.
     * TODO: rename into map
     */
    public Map<PORValue, String> mappings;

    // CONSTRUCTORS
    //==============

    /**
     * Creates an empty value labels record.
     */
    public PORValueLabels() {
        type = PORValue.TYPE_UNASSIGNED;
        vars = new Vector<PORVariable>();
        mappings = new LinkedHashMap<PORValue, String>();
    } // ctor

    // OTHER METHODS
    //===============


} // class PORValueLabels
