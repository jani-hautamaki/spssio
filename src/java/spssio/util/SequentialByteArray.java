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

package spssio.util;

/**
 * Sequentially writable and readable byte array.
 *
 * The data matrix of a Portable file in compact format. The data of a portable
 * file cannot be converted into a {@code Vector} of {@code Double} and 
 * {@code String} objects. This is because the overhead of Java for any 
 * {@code Object} is very high in terms of both the space (vtable) 
 * and time (gc).<p>
 * 
 * For instance, a portable file of 20 MBs would need much more than 1024 MBs
 * of memory before Java would be able to convert it into {@code Double}s
 * and {code String}s. 20 MBs shouldn't be that much of trouble nowaways,
 * when computers have, like, at least 2048 MBs of RAM memory by default.
 * But that's Java for you.<p>
 * 
 */
public class SequentialByteArray
{
    // CONSTANTS
    //===========
    
    /**
     * Number of bytes in an array element. For int[] array this needs
     * to be 4, and foor long[] array this needs to be 8.
     */
    private static final int BYTES_IN_ELEMENT               = 4;
    
    // MEMBER VARIABLES
    //==================
    
    /** The data array */
    private int[] data;

    /** Size in bytes */
    private int size;
    
    // Reading/Writing at byte-level
    //===============================
    
    /** Current array element number */
    private int offset;
    
    /** Current total byte offset. */
    private int cbyte;

    /** Current byte number within the array element */
    private int bytenum;
    
    /** Current array element value */
    private int elem;
    
    // CONSTRUCTORS
    //==============
    
    /**
     * Creates an uninitialized {@code SequentialByteArray}.
     * The byte array needs to be allocated with {@link #allocate(int)}
     * prior to any use.
     */
    public SequentialByteArray() {
        data = null;
        size = 0;
        
        offset = 0;
        cbyte = 0;
        bytenum = 0;
        elem = 0;
    } // ctor
    
    // OTHER METHODS
    //===============
    
    /**
     * Allocates memory for the given number of bytes.
     * The allocation loses any previously allocated data.
     *
     * @param size_bytes The amount of memory to allocate in bytes
     */
    public void allocate(int size_bytes) {
        size = size_bytes;
        int elems = (size + BYTES_IN_ELEMENT - 1) / BYTES_IN_ELEMENT;
        System.out.printf("Reserving %d elements for %d bytes\n", elems, size_bytes);
        data = new int[elems];
        
        // Reset head position
        offset = 0;
        cbyte = 0;
        bytenum = 0;
        elem = 0;
    } // allocate
    
    /**
     * Writes the specified byte into the array. 
     * The internal head is moved forward. The first byte of 
     * the element is written to the most signicant end. 
     * This enables easier sequential read, which is desirable
     * since it is expected that the data is written once, and
     * read multiple times.
     * 
     * @param c The byte to write. 
     *          Only the least-significant 8 bits are written.
     *
     */
    public final void write(int c) {
        if (cbyte == size) {
            throw new ArrayIndexOutOfBoundsException(String.format("%d", cbyte));
        }
        
        // Append the input byte to the data element.
        elem |= (c & 0xff) << (bytenum << 3);
        
        bytenum++;
        cbyte++;
        if (bytenum == BYTES_IN_ELEMENT) {
            // Write the data
            data[offset] = elem;
            // Increase offset
            offset++;
            // Reset current element
            elem = 0;
            // Reset bytenum
            bytenum = 0;
        }
    } // write()
    
    /**
     * Flushes the current element to the array
     */
    public void flush() {
        // Flushing is required only if there are written bytes in
        // the current data element.
        if (bytenum != 0) {
            data[offset] = elem;
        }
    }

    /**
     * Seeks to read/write head to the specified byte offset.
     * @param tobyte The byte offset to seek to.
     */
    public void seek(int tobyte) {
        
        // Record the total byte offset
        cbyte = tobyte;
        // Calculate offset
        offset = tobyte / BYTES_IN_ELEMENT;
        // Pick the corresponding data element, and move head
        elem = data[offset];
        // Set in-element offset
        bytenum = tobyte % BYTES_IN_ELEMENT;
        // The element has to be shifted properly.
        // Using unsigned shift right:
        elem = elem >>> (bytenum << 3);
    } // seek()
    
    /**
     * Reads a byte currently under the read/write head.
     * The read/write head is moved forward by one byte.
     *
     * @return
     *      The byte read, or -1 if end-of-data.
     */
    public final int read() {
        
        if (cbyte == size) {
            return -1;
        }
        
        // Otherwise, the byte under the head at "cbyte" can be read.
        
        if (bytenum == BYTES_IN_ELEMENT) {
            // Read next byte
            offset++;
            elem = data[offset];
            bytenum = 0;
        }
        
        int rval = 0;
        rval = elem & 0xff;
        
        // increase byte position
        elem = elem >>> 8; // unsigned SHR
        bytenum++;
        cbyte++;
        
        return rval;
    } // read()
    
    /**
     * Get the size of the array in bytes.
     * @return The array size in bytes.
     */
    public int size() {
        return size;
    }
    
    // for testing
    public static void main(String[] args) {
        SequentialByteArray matrix = new SequentialByteArray();
        int len = 12;
        matrix.allocate(len);
        
        for (int i = 0; i < len; i++) {
            matrix.write(i*3);
        }
        matrix.flush();
        
        int pos = 5;
        matrix.seek(pos);
        for (int i = pos; i < len+6; i++) {
            int c = matrix.read();
            System.out.printf("data[%d] = %d\n", i, c);
        }
    }
    
} // PORDataMatrix
