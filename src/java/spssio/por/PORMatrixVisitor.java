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


public interface PORMatrixVisitor {
    
    public void matrixBegin(int xdim, int ydim, int[] xtypes);
    public void matrixEnd();
    
    public void rowBegin(int y);
    public void rowEnd(int y);
    
    public void columnSysmiss(int x, int len, byte[] data);
    public void columnNumeric(int x, int len, byte[] data, double value);
    public void columnString(int x, int len, byte[] data);
    
    
} // public interface
    