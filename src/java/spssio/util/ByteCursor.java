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



package spssio.util;

import java.util.Vector;

/**
 * Fixed-size int array pool for bytes.
 */
public class ByteCursor {
    
    // MEMBER VARIABLES
    //==================
    
    /**
     * Reference to the dynamic byte array
     */
    DynamicByteArray array;
    
    /**
     * Cursor location: absolute offset (in bytes)
     */
    int offset;

    /**
     * Cursor location: current block number.
     * Equal to: offset / array.bytesPerBlock.
     */
    int blockNumber;
    
    /**
     * Cursor location: relative offset (in elements)
     * Equal to: (offset % array.bytesPerBlock) / BYTES_IN_ELEMENT.
     */
    int elementNumber;
    
    /**
     * Cursor location: relative offset within the element (in bytes)
     * Equal to: offset % BYTES_IN_ELEMENT.
     */
    int byteNumber;
    
    /**
     * Reference to the current block for convenience
     */
    int[] data;
    
    /**
     * Current data element.
     */
    int element;
    
    
    // CONSTRUCTORS
    //==============
    
    public ByteCursor(DynamicByteArray array, int offset) {
        if (array == null) {
            throw new IllegalArgumentException("array is null");
        }
        if ((offset < 0) || (offset > array.size)) {
            throw new IllegalArgumentException(String.format(
                "offset is out of bounds: %d",  offset));
        }
        this.array = array;
        
        // Reset location
        this.offset = offset;
        
        // Compute derived values
        blockNumber = offset / array.bytesPerBlock;
        elementNumber = (offset % array.bytesPerBlock) 
            / DynamicByteArray.BYTES_IN_ELEMENT;
        byteNumber = offset % DynamicByteArray.BYTES_IN_ELEMENT;

        // Once the location has been computed, retrieve the block
        // and cache the current element.
        data = null;
        element = 0;
        
        if (array.capacity > 0) {
            data = array.blockAt(blockNumber);
            element = data[elementNumber];
        }
    }
    
    // OTHER METHODS
    //===============
    
    public void setOffset(int offset) {
        if ((offset < 0) || (offset > array.size)) {
            throw new IllegalArgumentException(String.format(
                "offset is out of bounds: %d",  offset));
        }
        
        // Reset location
        this.offset = offset;
        
        // Compute derived values
        blockNumber = offset / array.bytesPerBlock;
        elementNumber = (offset % array.bytesPerBlock) 
            / DynamicByteArray.BYTES_IN_ELEMENT;
        byteNumber = offset % DynamicByteArray.BYTES_IN_ELEMENT;

        System.out.printf("byteNumber: %d - elementNumber: %d - blockNumber: %d\n",
            byteNumber, elementNumber, blockNumber);
        
        // Once the location has been computed, retrieve the block
        // and cache the current element.
        if (offset < array.size) {
            data = array.blockAt(blockNumber);
            element = data[elementNumber];
        } else {
            data = null;
            element = 0;
        }
    }
    
    /**
     * Sensible only when reading
     */
    public boolean eof() {
        return false;
    }
    
    public int getOffset() {
        return offset;
    }
    
    // READ AND WRITE METHODS
    //========================
    public void flush() {
        data[elementNumber] = element;
    }
    
    public void write(int b) {
        if ((elementNumber == 0) && (byteNumber == 0)) {
            // At the beginning of a new block.
            // At the beginning of a new element.
            
            if (blockNumber == array.getNumberOfBlocks()) {
                // Increase capacity
                data = array.allocateBlocks(1);
                System.out.printf("New block allocated. Capacity: %d\n", array.capacity);
            } else {
                data = array.blockAt(blockNumber);
            }
            
            // For the random access to work...
            if (offset < array.size) {
                element = data[elementNumber]; // Essential for reading
            } else {
                element = 0; // Practical for writing...
            }
        }
        
        // Clear out the previous byte in the data element.
        element &= ~((int)0xff << (byteNumber << 3));

        // Use bitwise OR to include the input byte to the data element.
        element |= (b & 0xff) << (byteNumber << 3);

        // Increase the array size
        array.size++;
        
        // Move the absolute offset forward
        offset++;
        
        // Move the head foward.
        byteNumber++;
        
        // Is there an element cross-over due to byteNumber overflow?
        if (byteNumber == DynamicByteArray.BYTES_IN_ELEMENT) {
            // End of the data element reached. 
            // Write it back to the array, and increase elementNumber.
            data[elementNumber] = element;
            
            // Roll-over byte number, and move on to the next element.
            byteNumber = 0;
            elementNumber++;
            
            // See if the element number reached the end of the block
            if (elementNumber == array.elementsPerBlock) {
                // Roll-over element number, and move on to the next block.
                elementNumber = 0;
                blockNumber++;
            } else {
                // No overflow happened. Read in the next data element.
                // Read in the next data element
                element = data[elementNumber];
            }
        }
    }
    
    public int read() {
        if (offset >= array.size) {
            return -1;
        }
        
        if ((elementNumber == 0) && (byteNumber == 0)) {
            // At the beginning of a new block.
            // At the beginning of a new element.
            if (blockNumber == array.getNumberOfBlocks()) {
                // At the end of the dynamic array.
                //return -1;
                throw new RuntimeException("Should never happen");
            } else {
                data = array.blockAt(blockNumber);
            }
            element = data[elementNumber];
        }
        
        // Read the byte at the current position in the data element
        int b = (element >>> (byteNumber << 3)) & 0xff;
        
        // Move the absolute offset forward
        offset++;
        
        // Move the head forward
        byteNumber++;
        
        // Is there an element cross-over due to byteNumber overflow?
        if (byteNumber == DynamicByteArray.BYTES_IN_ELEMENT) {
            // End of the data element reached. 
            
            // Roll-over byte number, and move on to the next element.
            byteNumber = 0;
            elementNumber++;
            
            // See if the element number reached the end of the block
            if (elementNumber == array.elementsPerBlock) {
                // Roll-over element number, and move on to the next block.
                elementNumber = 0;
                blockNumber++;
            } else {
                // No overflow happened. Read in the next data element.
                // Read in the next data element
                element = data[elementNumber];
            }
        }
        
        return b;
    }
    
    public static void main(String[] args) {
        DynamicByteArray array  = new DynamicByteArray(4, 8, false);
        
        ByteCursor cursor = new ByteCursor(array, 0);
        
        cursor.setOffset(0);
        for (int i = 0; i < 4*8+2; i++) {
            System.out.printf("Writing at offset %d\n", i);
            cursor.write(i);
        }
        cursor.flush();
        
        cursor.setOffset(0);
        int b;
        int offset = cursor.getOffset();
        
        while ((b = cursor.read()) != -1) {
            System.out.printf("Offset %d has byte %d. Next offset: %d\n",
                offset, b, cursor.getOffset());
            offset++;
        }
        
    }
}
