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



package spssio.por;

// spssio utils

public interface PORMatrix {

    public int getX();

    public int getY();

    public int sizeX();

    public int sizeY();

    public int sizeBytes();

    public int[] getColumnLayout();

    /**
     * Visits all data cells in the matrix in row-major order.
     *
     * @param visitor The visitor.
     */
    public void accept(PORMatrixVisitor visitor);

} // PORMatrix
