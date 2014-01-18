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

// core java
import java.util.Vector;

// spssio


/** 
 * System file.
 *
 */
public class SAVFile {
    
    // MEMBER VARIABLES
    //==================
    
    public SAVHeader header;
    
    public Vector<SAVVariable> variables;
    
    public Vector<SAVValueLabels> valueLabelMaps;
    
    public Vector<SAVExtensionRecord> extensionRecords;
    
    public SAVMatrix dataMatrix;
    
    public Vector<SAVSection> sections;
    
    
    //public Vector<SAVValueLabel> vlabelSets;
    // vlabelSets; ??
    // valueLabelSets
    //ValueLabelMap
    
    // SAVValueLabelSet
    
    // CONSTRUCTORS
    //==============
    
    public SAVFile() {
        header = null;
        variables = new Vector<SAVVariable>();
        dataMatrix = null;
        sections = new Vector<SAVSection>();
        valueLabelMaps = new Vector<SAVValueLabels>();
        extensionRecords = new Vector<SAVExtensionRecord>();
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
    
} // class SAVHeader

