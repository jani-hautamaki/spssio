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


package spssio.por;

/**
 * Visitor interface for traversing {@link PORMatrix}.
 * For each column in a row, one of the {@code columnXyz} methods is called.
 *
 */
public interface PORMatrixVisitor {

    /**
     * Matrix begins.
     * This is called before any {code rowXxxx} 
     * or {@code columnXxxx} methods.
     *
     * @param xdim      Length of the x dimension, ie. number of columns.
     * @param ydim      Length of the y dimension, ie. number of rows.
     * @param xtypes    Columns' data types as {@link PORValue} types.
     */
    public void matrixBegin(int xdim, int ydim, int[] xtypes);

    /**
     * Matrix ended.
     * This is called after all rows and columns have been traversed.
     */
    public void matrixEnd();

    /**
     * Matrix row begins.
     *
     * @param y         New row's number.
     *
     */
    public void rowBegin(int y);

    /**
     * Matrix row ended.
     *
     * @param y         Finished row's number.
     */
    public void rowEnd(int y);

    /**
     * Data column with NUMERIC type, and value SYSMISS.
     *
     * @param x         Column number.
     * @param data      Column's contents as an array.
     * @param len       Length of valid array.
     */
    public void columnSysmiss(int x, byte[] data, int len);

    /**
     * Data column with NUMERIC type and valid number.
     *
     * @param x         Column number.
     * @param data      Column's contents as an array.
     * @param len       Length of valid array.
     */
    public void columnNumeric(int x, byte[] data, int len, double value);


    /**
     * Data column with STRING type, and valid contents.
     *
     * As a string, the content array begins with the length of the string
     * expressed in Portable's base-30 number system. The actual contents
     * of the string start from the array position {@code offset}.
     *
     * @param x         Column number.
     * @param data      Column's contents as an array.
     * @param len       Length of valid array.
     * @param offset    Offset to the first character in the string.
     */
    public void columnString(int x, byte[] data, int offset, int len);


} // public interface
