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


package spssio.sav;

// core java
import java.util.Vector;
import java.util.Map;
import java.util.LinkedHashMap;

// spssio
import spssio.sav.SAVValue;

public class SAVValueLabels {

    // VARIABLES
    //===========

    /**
     * Determines the type of the values,
     * See SAVValue.
     */
    public int type;

    /**
     * Actual mapping of values to labels.
     * The labels have (8, 1) alignment in the System file format.
     */
    public Map<SAVValue, String> map;

    /**
     * List of associated variables
     */
    public Vector<SAVVariable> variables;


    // CONSTRUCTORS
    //==============

    public SAVValueLabels() {
        type = SAVValue.TYPE_UNASSIGNED;
        map = new LinkedHashMap<SAVValue, String>();
        variables = new Vector<SAVVariable>();
    }

    // OTHER METHODS
    //===============

}