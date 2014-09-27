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
import java.util.List;

public class SAVSection {
    /*
     * Quick summary:
     *
     * Tag      Name                Dynamic type of "obj"
     *  0       Header              SAVHeader
     *  1       ?                   ?
     *  2       Variable            SAVVariable
     *  3       ValuLabelMap        SAVValueLabels
     *  4       VariableList        Vector<SAVVariable>
     *  5       ?                   ?
     *  6       Documents           List<String>
     *  7       ExtensionRecord     SAVExtensionRecord, see subtag.
     *  999     DataMatrix          SAVMatrix
     * 
     */

    // CONSTANTS
    //===========

    /**
     * obj is SAVHeader.
     * Thsi is a pseudo-tag used only by the program.
     */
    public static final int TAG_HEADER                          = 0;

    /**
     * obj is SAVVariable.
     */
    public static final int TAG_VARIABLE                        = 2;

    /**
     * obj is SAVValueLabels.
     */
    public static final int TAG_VALUE_LABELS                    = 3;

    /**
     * obj is SAVVariableSet.
     */
    public static final int TAG_VARIABLE_LIST                   = 4;

    /**
     * obj is List<String>
     */
    public static final int TAG_DOCUMENTS                       = 6;

    /**
     * obj is SAVExtensionRecord, see subtag.
     */
    public static final int TAG_EXTENSION_RECORD                = 7;

    /**
     * obj is SAVMatrix.
     */
    public static final int TAG_DATA_MATRIX                     = 999;

    // MEMBER VARIABLES
    //==================

    /**
     * Tag code for the section.
     */
    public int tag;

    /**
     * Object related to the section.
     * The dynamic type of the object is determined by the tag code.
     */
    public Object obj;

    // CONSTRUCTORS
    //==============

    // OTHER METHODS
    //===============

    public SAVSection(int tag, Object obj) {
        // Validate the input first.
        validate(tag, obj);

        // Assign
        this.tag = tag;
        this.obj = obj;
    }


    public static void validate(int tag, Object obj) {
        switch(tag) {
            case TAG_HEADER:
                expect(obj, SAVHeader.class);
                break;

            case TAG_VARIABLE:
                expect(obj, SAVVariable.class);
                break;

            case TAG_VALUE_LABELS:
                expect(obj, SAVValueLabels.class);
                break;

            case TAG_VARIABLE_LIST:
                expect(obj, Vector.class);
                break;

            case TAG_DOCUMENTS:
                expect(obj, List.class);
                break;

            case TAG_EXTENSION_RECORD:
                expect(obj, SAVExtensionRecord.class);
                break;

            case TAG_DATA_MATRIX:
                expect(obj, SAVMatrix.class);
                break;

            default:
                throw new RuntimeException(String.format(
                    "Unexpected tag: %d", tag));
        } // switch()
    }

    protected static void expect(Object obj, Class<?> classType) {
        if (obj == null) {
            throw new RuntimeException(String.format(
                "Expected an instance of class %s, but found null",
                classType.getName()));
        }

        if (classType.isInstance(obj) == false) {
            throw new RuntimeException(String.format(
                "Expected an instance of class %s, but found: %s",
                classType.getName(), obj.getClass().getName()));
        }
    }

} // class SAVSection