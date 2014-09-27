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

// spssio
import spssio.por.input.PORMatrixParser;
import spssio.util.SequentialByteArray;


/**
 * The data matrix of a Portable file.<p>
 *
 * It is not a wise idea to convert the data values of a Portable file into
 * into {@code String} and {@code Double} objects. The additional memory 
 * overhead caused by Java for any {@code Object} is very in terms of both
 * memory (vtable+gc) and time (gc).<p>
 * 
 * For instance, a portable file of 20 MBs would need  more than 1024 MBs
 * of memory before Java would be able to convert it into {@code Double}s
 * and {code String}s. 20 MBs shouldn't be that much of trouble nowaways,
 * when computers have, like, at least 2048 MBs of RAM memory by default.
 * That's Java for you.<p>
 * 
 */
public class PORRawMatrix
    implements PORMatrix
{
    
    // MEMBER VARIABLES
    //==================
    
    /**
     * The backend storage format is a sequential byte array.
     */
    private SequentialByteArray array;
    
    /**
     * The parser which is used to translate the byte stream
     * into a stream of events.
     */
    private PORMatrixParser parser;
    
    // CONSTRUCTORS
    //==============
    
    public PORRawMatrix(
        SequentialByteArray array,
        PORMatrixParser parser
    ) {
        if ((array == null) || (parser == null)) {
            throw new IllegalArgumentException(
                "Neither array nor parser can be null");
        }
        this.array = array;
        this.parser = parser;
    } // ctor
    
    // OTHER METHODS
    //===============
    
    public SequentialByteArray getRawArray() {
        return array;
    }
    
    public int getTextColumn0() {
        return parser.getTextColumn0();
    }
    
    public int getTextRowLength() {
        return parser.getTextRowLength();
    }
    
    // PORMatrix INTERFACE
    //=====================
    
    public int sizeX() {
        return parser.getSizeX();
    }
    
    public int sizeY() {
        return parser.getSizeY();
    }
    
    public int getX() {
        return parser.getX();
    }
    
    public int getY() {
        return parser.getY();
    }
    
    public int sizeBytes() {
        return array.size();
    }
    
    public int[] getColumnLayout() {
        return parser.getDataColumnTypes();
    }
    
    
    /**
     * Visits all data cells in the matrix in row-major order.
     *
     * @param visitor The visitor.
     */
    public void accept(PORMatrixVisitor visitor) {
        // Seek to the beginning of the array
        array.seek(0);
        
        // Restart parser
        parser.restart();
        
        // Set visitor
        parser.setVisitor(visitor);
        
        // Signal beginning of the matrix
        parser.startMatrix();
        
        // Parse the whole matrix
        int c;
        while ((c = array.read()) != -1) {
            parser.consume(c);
        } // while
        
        // Signal ending of the matrix
        parser.endMatrix();
        
        // Unset visitor
        parser.setVisitor(null);
    } // visit()
    
    
} // PORMatrixRaw
