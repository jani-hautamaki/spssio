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


public interface SAVMatrixHandler {

    public void onMatrixBegin(int xsize, int ysize, int[] columnWidths);

    public void onMatrixEnd();

    public void onRowBegin(int y);

    public void onRowEnd(int y);

    public void onCellSysmiss(int x);

    public void onCellNumber(int x, double value);

    public void onCellInvalid(int x);

    public void onCellString(int x, String value);

} // class

