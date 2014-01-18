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

package spssio.sav.output;

// core java
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;

// spssio
import spssio.sav.SAVMatrixHandler;
import spssio.sav.SAVMatrix;

/*
 * TBC: The renaming of the class?
 */
public class SAVMatrixWriter 
    implements SAVMatrixHandler
{
    
    // MEMBER VARIABLES
    //==================
    
    // CONSTRUCTORS
    //==============
    
    public SAVMatrixWriter() {
    }
    
    // OTHER METHODS
    //===============
    
    public void outputSAVMatrix(SAVMatrix dataMatrix) {
        
        // Create a matrix handler
        
    }
    
    // SAVMatrixHandler interface
    //============================
    
    public void onMatrixBegin(int xsize, int ysize, int[] columnWidths) {
    }
    
    public void onMatrixEnd() {
    }
    
    public void onRowBegin(int y) {
    }
    
    public void onRowEnd(int y) {
    }
    
    public void onCellSysmiss(int x) {
    }
    
    public void onCellNumber(int x, double value) {
        
    }
    
    public void onCellInvalid(int x) {
    }
    
    public void onCellString(int x, String value) {
        // get variable width, and padd the string to that length
        
    }
    
}
