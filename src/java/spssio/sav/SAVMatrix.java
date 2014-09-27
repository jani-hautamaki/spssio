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


public interface SAVMatrix {
    
    /**
     * Return the number of columns
     */
    public int sizeX();
    
    /**
     * Returns the number of rows (cases)
     */
    public int sizeY();
    
    /**
     * Returns the number of bytes used to store the matrix.
     */
    public int sizeBytes();

    /**
     * Return an integer array containing the width of each column.
     *
     */
    public int[] getColumnWidths();
    
    /**
     * Visits all data cells in the matrix in row-major order.
     *
     * @param contentHandler The handler.
     */
    public void traverse(SAVMatrixHandler contentHandler);
    
    /*
     * For sequential access.
     */
    //public SAVMatrixReader getReader();
    
} // SAVMatrix
