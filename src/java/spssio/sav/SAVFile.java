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



package spssio.sav;

// core java
import java.util.Vector;
import java.util.List;




/**
 * System file.
 *
 *
 * TODO:
 * SAVVariableList would be a ArrayList sub-class containing a reference
 * to the SAVFile it is part of.
 *
 */
public class SAVFile {

    // MEMBER VARIABLES
    //==================

    /**
     * Header record.
     */
    public SAVHeader header;

    /**
     * Variable definitions, including filler variables.
     * NOTE: SPSS v20.0 ignores dummy variables completely.
     *
     */
    public Vector<SAVVariable> variables;

    /**
     * Value-Label mappings.
     */
    public Vector<SAVValueLabels> valueLabelMaps;

    /**
     * Documents record; a list of lines.
     */
    public List<String> documents;

    /**
     * Extension record: system configuration / info
     */
    public SAVExtSystemConfig systemConfig;

    /**
     * Extension record: pre-defined floating-point constants.
     */
    public SAVExtNumberConfig numberConfig;


    /**
     * Unhandled extension records.
     */
    public Vector<SAVExtensionRecord> extensionRecords;


    /**
     * The data matrix
     */
    public SAVMatrix dataMatrix;

    /**
     * All the parsed sections in the order of appearance.
     */
    public Vector<SAVSection> sections;

    // CONSTRUCTORS
    //==============

    public SAVFile() {
        header = null;
        variables = new Vector<SAVVariable>();
        dataMatrix = null;
        documents = null;
        sections = new Vector<SAVSection>();
        valueLabelMaps = new Vector<SAVValueLabels>();
        extensionRecords = new Vector<SAVExtensionRecord>();
        numberConfig = null;
        systemConfig = null;
    }

    public static SAVFile createNew() {
        SAVFile sav = new SAVFile();

        sav.header = SAVHeader.createNew();

        // Default values for special symbols
        sav.numberConfig = new SAVExtNumberConfig();

        // TODO
        //sav.systemConfig = new SAVExtSystemConfig();

        return sav;
    }

    // OTHER METHODS
    //===============

    public int calculateNumberOfVariables() {
        int count = 0;
        for (SAVVariable v : variables) {
            if (v.width >= 0) {
                count++;
            }
        }
        return count;
    }

    public int[] getColumnWidths() {
        // Number of variables (including padding variables)
        int size = variables.size();

        // Create the return variable array
        int[] array = new int[size];

        // Populate the array
        for (int i = 0; i < size; i++) {
            SAVVariable v = variables.elementAt(i);
            array[i] = v.getWidth();
        }

        return array;
    }

    // VARIABLES
    //===========

    public void addVariable(SAVVariable v) {
        addVariable(variables.size(), v);
        updateVariableCount();
    }

    public void addVariable(int index, SAVVariable v) {
        // TODO: Sanity checks (name is unique, v is not duplicate, etc?)
        variables.add(index, v);
        updateVariableCount();
    }

    public int indexOfVariable(SAVVariable v) {
        return variables.indexOf(v);
    }

    public boolean removeVariable(SAVVariable v) {
        int index = variables.indexOf(v);
        if (index == -1) {
            // No such variable
            return false;
        }
        removeVariable(index);
        return true;
    }

    public SAVVariable removeVariable(int index) {
        // Remove the variable from the list
        SAVVariable v = variables.remove(index);

        // TODO: If the width > 0, remove all dummies too, if any.

        // TODO: remove also from the value labels


        updateVariableCount();

        return v;
    }

    public void rebuildVariables() {
        int len = variables.size();

        // Calculate new length
        int newLength = 0;
        for (int i = 0; i < len; i++) {
            SAVVariable v = variables.get(i);
            int width = v.getWidth();

            int pieces = 0;

            if (width == 0) {
                pieces = 1;
            } else if (width > 0) {
                // ceil(width/8)*8
                pieces = ((width+7)/8)*8;
            } // if-else

            newLength += pieces;
        } // for

        // Create new variables vector

        Vector<SAVVariable> vector = new Vector<SAVVariable>(newLength);

        for (int i = 0; i < len; i++) {
            SAVVariable v = variables.get(i);
            int width = v.getWidth();

            if (width == 0) {
                vector.add(v);
            } else if (width > 0) {
                vector.add(v);
                int dummiesNeeded = (((width+7)/8)*8) - 1;
                for (int j = 0; j < dummiesNeeded; j++) {
                    vector.add(v.createDummy());
                }
            } else {
                // width < 0.
                // Ignore
            } // if-else
        } // for
    }

    public int getNumberOfVariables() {
        return variables.size();
    }

    public void updateVariableCount() {
        // Push actual variable count to the header
        header.variableCount = variables.size();
    }


    // VALUE CONFIG
    //==============

    public double getHighestValue() {
        if (numberConfig != null) {
            return numberConfig.getHighestValue();
        }
        return SAVConstants.DEFAULT_HIGHEST_VALUE;
    }

    public double getLowestValue() {
        if (numberConfig != null) {
            return numberConfig.getLowestValue();
        }
        return SAVConstants.DEFAULT_LOWEST_VALUE;
    }

    public double getSysmissValue() {
        if (numberConfig != null) {
            return numberConfig.getSysmissValue();
        }
        return SAVConstants.DEFAULT_SYSMISS_VALUE;
    }

    // ENCODING CONFIG
    //=================

    public String getStringEncoding() {
        return SAVConstants.DEFAULT_STRING_ENCODING;
    }


    // HEADER ACCESS
    //===============

    public SAVHeader getHeader() {
        return header;
    }

    // TODO


} // class SAVHeader

