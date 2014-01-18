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

package spssio.sav;

// spssio
import spssio.sav.SAVValue;


/**
 * System File Variable has:
 *
 *      rec_type (implied)
 *
 *      width (int, 0 for numeric, 1-255 for a short string variable or 
 *          for a first part of a long string variable this is set to
 *          the width of the numeric variable. Subsequent parts are set to -1)
 *
 *      has_var_label (implied)
 *
 *      n_missing_values (implied)
 *
 *      print fmt (int32)
 *          The least-significant byte of the int32 represents the number of 
 *          decimal places, and the next two bytes in order of increasing 
 *          significance represent field width and format type, respectively. 
 *          The most-significant byte is not used and should be set to zero. 
 *
 *      write fmt (int32)
 *          See above
 *
 *      name (char[8], padded with spaces)
 *          The variable name must begin with a capital letter or the at-sign (`@').
 *          Subsequent characters may also be digits, octothorpes (`#'), 
 *          dollar signs (`$'), underscores (`_'), or full stops (`.'). 
 *
 *      label_len (int32) (implied, between 0-120)
 *          This field is present only if has_var_label is set to 1. 
 *          It is set to the length, in characters, of the variable label, 
 *          which must be a number between 0 and 120. 
 *
 *      label (varchar, aligned to nearest 4 bytes, only first label_len used)
 *
 *      missing_values[] (flt64, repeated 0..3 times)
 *
 * Related data elements:
 *
 *      Value Labels
 * 
 *      Display Parameters
 *          measure/scale (int32: nominal, ordinal, continuous)
 *          width (int32: column width in chars)
 *          alignment (int32, left, right centre)
 *  
 *      Long name
 *          "The value field is at most 64 bytes long"
 * 
 * "There must be one variable record for each numeric variable and each string 
 *  variable with width 8 bytes or less. String variables wider than 8 bytes have 
 *  one variable record for each 8 bytes, rounding up. The first variable record 
 *  for a long string specifies the variable's correct dictionary information. 
 *  Subsequent variable records for a long string are filled with dummy information:
 *  a type of -1, no variable label or missing values, print and write formats that
 *  are ignored, and an empty string as name. "
 */
public class SAVVariable {

    // MEMBER VARIABLES
    //==================
    
    /**
     * 0 for numeric,
     * 0-255 width of a short string
     * -1 string, non-first entry
     */
    public int width;
    
    /**
     * Set to 1 if the variable has a label
     */
    public int hasLabel;
    
    /**
     * Number of missing values (0-3).
     */
    public int numberOfMissingValues;

    /**
     * The format expected when parsing the value
     */
    public SAVValueFormat inputFormat;
    
    /**
     * The format used when printing the value
     */
    public SAVValueFormat outputFormat;
    
    /**
     * Variable name, max 8 chars.
     */
    public String name;

    //=================================
    // The following fiels are present 
    // only if hasLabel is non-zero.
    //=================================
    
    /**
     * Field is present only if hasLabel is non-zero 
     * The label has alignment (4, 0) in the file.
     */
    public String label;

    /**
     * Field is present only if numberofMissingValues is non-zero 
     */
    public double[] missingValues;

    // CONSTRUCTORS
    //==============

    public SAVVariable() {
    }
    
    // OTHER METHODS
    //===============

    
    public int getType() {
        if (width == 0) {
            return SAVValue.TYPE_NUMERIC;
        } else if (width > 0) {
            return SAVValue.TYPE_STRING;
        }
        
        return SAVValue.TYPE_UNASSIGNED;
    }
    
    public int getWidth() {
        return width;
    }
    
    public void setWidth(int width) {
        this.width = width;
    }
    
    public int getHasLabel() {
        return hasLabel;
    }
    
    public void setHasLabel(int hasLabel) {
        this.hasLabel = hasLabel;
    }
    
    public String getLabel() {
        return label;
    }
    
    public void setLabel(String label) {
        if (label != null) {
            hasLabel = 1;
        } else {
            hasLabel = 0;
        }
        
        this.label = label;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String newName) {
        name = newName;
    }
    
    public int getNumberOfMissingValues() {
        return numberOfMissingValues;
    }
    
    public void setNumberOfMissingValues(int numberOfMissingValues) {
        this.numberOfMissingValues = numberOfMissingValues;
    }
    
    public double[] getMissingValues() {
        return missingValues;
    }
    
    public void setMissingValues(double[] missingValues) {
        // TODO: Should clone
        this.missingValues = missingValues;
    }
    
} // class SAVVariable








