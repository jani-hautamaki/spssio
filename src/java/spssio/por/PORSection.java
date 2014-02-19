
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

// spssio
import spssio.por.PORHeader;
import spssio.por.PORVariable;
import spssio.por.PORMissingValue;
import spssio.por.PORValueLabels;

/**
 *
 */
public class PORSection
{
    // Quick summary:
    
    /*
    0: header:          STRUCT
    1: software:        String
    2: author:          String
    3: title:           String
    4: varcount:        Integer
    5: precision:       Integer
    6: weight_var:      String
    7: var_record       PORVariable
    8: missing_val      PORMissingValue
    9: missing_val      PORMissingValue
    A: missing_val      PORMissingValue
    B: missing_val      PORMissingValue
    C: var_label        String
    D: value_labels     PORValueLabels
    E: doc_records      Vector<String>
    F: data             PORMatrix
    */

    // CONSTANTS
    //===========

    /**
     * obj is PORHeader. 
     * This is a pseudo-tag code used only by this program.
     */
    public static final int TAG_HEADER                  = '0';
     
    /**
     * obj is String.
     */
    public static final int TAG_SOFTWARE                = '1';
    
    /**
     * obj is String.
     */
    public static final int TAG_AUTHOR                  = '2';
    
    /**
     * obj is String.
     */
    public static final int TAG_TITLE                   = '3';
    
    /**
     * obj is Integer.
     */
    public static final int TAG_VARIABLE_COUNT          = '4';
    
    /**
     * obj is Integer.
     */
    public static final int TAG_PRECISION               = '5';

    /**
     * obj is Integer.
     */
    public static final int TAG_WEIGHT_VARIABLE         = '6';
    
    /**
     * obj is PORVariable.
     */
    public static final int TAG_VARIABLE_RECORD         = '7';
    
    /**
     * obj is PORMissingValue.
     */
    public static final int TAG_MISSING_DISCRETE        = '8';
    
    /**
     * obj is PORMissingValue.
     */
    public static final int TAG_MISSING_OPEN_LO         = '9';
    
    /**
     * obj is PORMissingValue.
     */
    public static final int TAG_MISSING_OPEN_HI         = 'A';
    
    /**
     * obj is PORMissingValue.
     */
    public static final int TAG_MISSING_RANGE           = 'B';
    
    /**
     * obj is String.
     */
    public static final int TAG_VARIABLE_LABEL          = 'C';
    
    /**
     * obj is PORValueLabels
     * TODO: Rename into       TAG_VALUE_LABELS_RECORD
     */
    public static final int TAG_VALUE_LABELS            = 'D';
    
    /**
     * Not implemented.
     */
    public static final int TAG_DOCUMENTS_RECORD        = 'E';
    
    /**
     * obj is PORMatrix.
     */
    public static final int TAG_DATA_MATRIX             = 'F';
    
    
    // MEMBER VARIABLES
    //==================
    
    /**
     * Tag code for the section.
     * 
     */
    public int tag;
    
    /**
     * Object related to the section, and type determined by the tag code.
     */
    public Object obj;
    
    // CONSTRUCTORS
    //==============
    
    public PORSection(int tag, Object obj) {
        this.tag = tag;
        this.obj = obj;
    }
    
    // OTHER METHODS
    //===============

    /*
    public int getTag() {
        return tag;
    }
    
    public Object getObject() {
        return obj;
    }
    */
    
    // FACTORY METHODS
    //=================

    public static PORSection newHeader(PORHeader header) {
        return new PORSection(TAG_HEADER, header);
    }
    
    public static PORSection newSoftware(String software) {
        return new PORSection(TAG_SOFTWARE, software);
    }
    
    public static PORSection newAuthor(String author) {
        return new PORSection(TAG_AUTHOR, author);
    }
    
    public static PORSection newTitle(String title) {
        return new PORSection(TAG_TITLE, title);
    }
    
    public static PORSection newVariableCount(int count) {
        return new PORSection(TAG_VARIABLE_COUNT, new Integer(count));
    }
    
    public static PORSection newPrecision(int precision) {
        return new PORSection(TAG_PRECISION, precision);
    }
    
    public static PORSection newWeightVariable(String variableName) {
        return new PORSection(TAG_WEIGHT_VARIABLE, variableName);
    }
    
    public static PORSection newVariableRecord(PORVariable v) {
        return new PORSection(TAG_VARIABLE_RECORD, v);
    }
    
    public static PORSection newMissingValueRecord(
        PORMissingValue miss
    ) {
        // This is a bit more complicated. 
        // The actual tag code depends on the sub-type of the parameter.
        switch(miss.type) {
            case PORMissingValue.TYPE_DISCRETE_VALUE:
                return new PORSection(TAG_MISSING_DISCRETE, miss);
            case PORMissingValue.TYPE_RANGE_OPEN_LO:
                return new PORSection(TAG_MISSING_OPEN_LO, miss);
            case PORMissingValue.TYPE_RANGE_OPEN_HI:
                return new PORSection(TAG_MISSING_OPEN_HI, miss);
            case PORMissingValue.TYPE_RANGE_CLOSED:
                return new PORSection(TAG_MISSING_RANGE, miss);
            default:
                throw new IllegalArgumentException();
        } // switch
        // never reached
    }
    
    public static PORSection newVariableLabel(String label) {
        return new PORSection(TAG_VARIABLE_LABEL, label);
    }

    public static PORSection newValueLabelsRecord(
        PORValueLabels vallabels
    ) {
        return new PORSection(TAG_VALUE_LABELS, vallabels);
    }
    
    public static PORSection newDocumentsRecord(Vector<String> documents) {
        return new PORSection(TAG_DOCUMENTS_RECORD, documents);
    }
    
    public static PORSection newDataMatrix(PORMatrix matrix) {
        return new PORSection(TAG_DATA_MATRIX, matrix);
    }
    
} // class PORWriter
