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
public class DynamicByteArray {

    // CONSTANTS
    //===========

    /**
     * Number of the bytes in the primitive data type
     * chosen for the fixed-size arrays
     */
    public static final int BYTES_IN_ELEMENT = 4;

    /**
     * Default size for the fixed-size arrays in bytes, 1 megabyte.
     */
    public static final int DEFAULT_BLOCK_SIZE = 1024*1024;

    // MEMBER VARIABLES
    //==================

    /**
     * Dynamic array for the fixed-size arrays.
     */
    Vector<int[]> blocks;

    /**
     * Reference to the last block in the vector. Provided for convenience.
     */
    int[] lastBlock;

    /**
     * Number of elements that can be fitted to a single fixed-size array.
     * Configured at the instantation, and remains immutable afterwards.
     */
    int elementsPerBlock;

    /**
     * Number of bytes that can be fitted to a single fixed-size array.
     * This is pre-computed at instantation, and remains immutable afterwards.
     */
    int bytesPerBlock;

    /**
     * Current logical size (in bytes).
     * Due to the integer data type the maximum size is 2^31 bytes (2 GB).
     */
    int size;

    /**
     * Current capacity, that is, physical size (in bytes).
     * Due to the integer data type the maximum capacity is 2^31 bytes (2 GB).
     */
    int capacity;

    /**
     * If capacity is fixed, adding a new block causes an exception.
     */
    boolean capacityLocked;

    // HELPERS
    //=========

    private static int roundUp(int bytes) {
        // Round-up to nearest 4-byte size
        int elements = (bytes + (BYTES_IN_ELEMENT-1))
            / BYTES_IN_ELEMENT;
        return elements * BYTES_IN_ELEMENT;
    }

    // CONSTRUCTORS
    //==============

    public DynamicByteArray(
        int blockSize,
        int initialCapacity,
        boolean locked
    ) {
        blockSize = roundUp(blockSize);
        initialCapacity = roundUp(initialCapacity);

        if ((blockSize % BYTES_IN_ELEMENT) != 0) {
            throw new IllegalArgumentException(String.format(
                "The configured blockSize (%d) is not a multiple "
                +"of the bytes per element (%d)",
                blockSize, BYTES_IN_ELEMENT));
        }
        if ((initialCapacity % blockSize) != 0) {
            throw new IllegalArgumentException(String.format(
                "The configured initialCapacity (%d) is not a multiple "
                +"of the block size (%d)",
                initialCapacity, blockSize));
        }

        // Compute the number of elements per block
        elementsPerBlock = blockSize / BYTES_IN_ELEMENT;

        // Compute the number of bytes per block.
        // Due to the modulus test at the beginning,
        // bytesPerBlock is asserted to be equal to the argument blockSize.
        bytesPerBlock = elementsPerBlock * BYTES_IN_ELEMENT;

        // Set initial size, capacity, and last block
        size = 0;
        capacity = 0;
        lastBlock = null;

        // Allocate the pool for the fixed-size arrays.
        blocks = new Vector<int[]>();

        // Allocate a number of fixed-size arrays to ensure
        // the required initial capacity.
        int numberNewBlocks = initialCapacity / bytesPerBlock;
        for (int i = 0; i < numberNewBlocks; i++) {
            lastBlock = new int[elementsPerBlock];
            blocks.add(lastBlock);
        }
        capacity += (numberNewBlocks * bytesPerBlock);

        // Set capacity lock status
        capacityLocked = locked;
    }

    public DynamicByteArray(
        int blockSize,
        int fixedCapacity
    ) {
        this(blockSize, fixedCapacity, true);
    }

    public DynamicByteArray(int fixedSize) {
        this(fixedSize, fixedSize, true);
    }

    public DynamicByteArray() {
        this(DEFAULT_BLOCK_SIZE, 0, false);
    }

    // OTHER METHODS
    //===============


    public int[] allocateBlocks(int numberNewBlocks) {
        if (numberNewBlocks <= 0) {
            throw new IllegalArgumentException();
        }
        if (capacityLocked) {
            throw new RuntimeException("Cannot allocate: capacity is locked");
        }

        for (int i = 0; i < numberNewBlocks; i++) {
            lastBlock = new int[elementsPerBlock];
            blocks.add(lastBlock);
        }
        capacity += (numberNewBlocks * bytesPerBlock);
        return lastBlock;
    }

    public int[] increaseSizeBy(int addition) {
        if ((size+addition) > capacity) {
            // Compute the number of blocks to allocate
            int numberNewBlocks = ((size+addition-capacity)+(bytesPerBlock-1))
                / bytesPerBlock;
            // increase capacity by allocating a new array
            allocateBlocks(numberNewBlocks);
        }
        size += addition;
        return lastBlock;
    }

    public int[] increaseSize() {
        return increaseSizeBy(1);
    }

    public int[] blockAt(int blockNumber) {
        return blocks.elementAt(blockNumber);
    }

    public int getNumberOfBlocks() {
        return blocks.size();
    }

    public int getSize() {
        return size;
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean isCapacityLocked() {
        return capacityLocked;
    }

    public int getBlockSize() {
        return bytesPerBlock;
    }
}
