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

/**
 * The data matrix of a Portable file in compact format. The data of a portable
 * file cannot be converted into a {@code Vector} of {@code Double} and 
 * {@code String} objects. This is because the overhead of Java for any {@Object}
 * is very high in terms of both the space (vtable) and time (gc).<p>
 * 
 * For instance, a portable file of 20 MBs would need much more than 1024 MBs
 * of memory before Java would be able to convert it into {@code Double}s
 * and {code String}s. 20 MBs shouldn't be that much of trouble nowaways,
 * when computers have, like, at least 2048 MBs of RAM memory by default.
 * But that's Java for you.<p>
 * 
 */
public class PORDataMatrix
{
    // MEMBER VARIABLES
    //==================
    
    /*
     * (TODO) 
     * Beneath the hood, it is a compact byte array.
     *
     * The byte array has the following structure:
     *    offset+0     uint8        the type of the data
     *    offset+1     N x uint8    the data
     *
     * The type is recorded, so that...?
     *
     * The size of the data, N, is determined by looking at the succeeding
     * cell offset or at the next write index (windex).
     *
     * It is apparent, that the data put into this format uses at most as many
     * bytes as its serialization in the portable file format.
     *     
     */
    //private CompactByteArray array;
    
    /**
     * Cell offsets in row-major order.
     */
    private int[] offset;
    
    /** Number of columns */
    private int xdim;
    
    /** Number of rows */
    private int ydim;
    
    /** Current  columns */
    private int xcur;
    
    /** Current row */
    private int ycur;
    
    /** Next write index to offset[] array (ycur*ydim)+xcur */
    private int windex;
    
    // CONSTRUCTORS
    //==============
    
    public PORDataMatrix() {
        offset = null;
        xdim = -1;
        ydim = -1;
        xcur = -1;
        ycur = -1;
        windex = -1;
    } // ctor
    
    // OTHER METHODS
    //===============
    
    
    
} // PORDataMatrix
