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


package spssio.util;

/**
 * Sequentially writable and readable byte array.<p>
 *
 * Converting the data matrix into a long array of {@code Double} 
 * and {@code String} objects is not a scalable solution. This is
 * due to the overhead caused by Java. The overhead for any {@code Object} 
 * is rather high in terms of both space (vtable) and time (gc).<p>
 *
 * The read and write operations are marked as {@code final} for speed.
 * 
 */
public class SequentialByteArray
{
    // CONSTANTS
    //===========

    /**
     * Number of bytes in an array element. For int[] array this needs
     * to be 4, and for long[] array this needs to be 8.
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

    /**
     * Creates an initialized {@code SequentialByteArray}.
     * 
     * @param size_bytes The amount of memory to allocate in bytes
     *
     */
    public SequentialByteArray(int size_bytes) {
        this(); // call default ctor

        // Allocate
        allocate(size_bytes);
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
        data = new int[elems];

        // Reset head position
        offset = 0;
        cbyte = 0;
        bytenum = 0;
        elem = 0;
    } // allocate

    /**
     * Limits the current size
     */
    public void limitSize(int size) {
        if (size >= this.size) {
            throw new IllegalArgumentException();
        }

        this.size = size;

        // TODO: Does not saturate the offset
    }

    public void reallocate(int size_bytes_new) {
        int size_new = size_bytes_new;
        int elems_new = (size_new + BYTES_IN_ELEMENT - 1) / BYTES_IN_ELEMENT;

        // Create a new array of the desired size.
        // TODO: What if out of memory? 
        int[] data_new = new int[elems_new];

        // If memory allocation was successful, ...

        // Copy the contents from the original array to the new array,
        // using java.lang.System.arraycopy
        int elems_to_copy;
        if (this.data.length < data_new.length) {
            // Array is expanded by realloc
            elems_to_copy = this.data.length;
        } else {
            // Array is shrunk by realloc
            elems_to_copy = data_new.length;
        }

        System.arraycopy(this.data, 0, data_new, 0, elems_to_copy);

        // Put new array into operation
        this.size = size_new;
        this.data = data_new;

        // Move head backwards if neccessary
        if (cbyte >= size) {
            seek(size);
        }

    } // reallocate()

    /**
     * Writes the specified byte into the array. 
     * The internal head is moved forward. The first byte of 
     * the element is written to the most signicant end. 
     * This enables easier sequential read, which is desirable
     * since it is expected that the data is written once, and
     * read multiple times.<p>
     *
     * TODO: The method should probably fail silently if not enough space,
     * similarly to read()
     * 
     * @param c 
     *      The byte to write. 
     *      Only the least-significant 8 bits are written.
     *
     * @exception RuntimeException
     *      Buffer overflow; the buffer is full, and the byte was not written.
     * 
     */
    public final void write(int c) {
        if (cbyte == size) {
            throw new RuntimeException("Cannot write; not enough space");
        }

        // Clear out the previous byte in the data element.
        elem &= ~((int)0xff << (bytenum << 3));

        // Use bitwise OR to include the input byte to the data element.
        elem |= (c & 0xff) << (bytenum << 3);

        // Head forward
        bytenum++;
        cbyte++;
        if (bytenum == BYTES_IN_ELEMENT) {
            // Write the data
            data[offset] = elem;
            // Increase offset
            offset++;
            // Reset current element.
            // TODO: Read the next offset from data[] instead?
            elem = 0;
            // Reset bytenum
            bytenum = 0;
        }
    } // write()

    /**
     * Writes {@code len} bytes from the specified byte array 
     * starting at offset {@code offset} to this output stream. 
     * 
     * @param buffer The data
     * 
     * @param offset The start offset in the data
     * 
     * @param len The number of bytes to write
     * 
     * @return
     *     The number of bytes written.
     *
     */
    public final int write(
        byte[] buffer,
        int offset,
        int len
    ) {
        int bytesWritten = 0;

        // Otherwise, there's at least one byte to read
        while ((bytesWritten < len) && (cbyte < size)) {
            write( ((int) buffer[offset]) & 0xff );
            offset++;
            bytesWritten++;
        }

        return bytesWritten;
    } // read()


    /**
     * For symmetry and completeness.
     * Currently identical to {@link #write(int)}.
     */
    public final void write1(int c) {
        if (cbyte == size) {
            throw new RuntimeException("Cannot write; not enough space");
        }

        write(c);
    }

    /** 
     * Writes the specified word (2-byte) into the array.
     *
     * @param word The word (2-byte) to write.
     *             Only the least-significant 16 bits are written.
     * 
     * @exception RuntimeException
     *      Buffer overflow; the buffer is full, and the word was not written.
     * 
     *
     */
    public final void write2(int word) {
        if (cbyte >= size-1) {
            throw new RuntimeException("Cannot write; not enough space");
        }

        for (int i = 0; i < 2; i++) {
            write(word & 0xff);
            word = word >>> 8; // Unsigned shift right
        }
    } // write2()

    /** 
     * Writes the specified dword (4-byte) into the array
     * using {@link #write(int)}.
     *
     * @param word The dword (4-byte) to write.
     *             All 32 bits of the int are written.
     * 
     * @exception RuntimeException
     *      Buffer overflow; the buffer is full, and the dword was not written.
     *
     */
    public final void write4(int dword) {
        if (cbyte >= size-3) {
            throw new RuntimeException("Cannot write; not enough space");
        }

        for (int i = 0; i < 4; i++) {
            write(dword & 0xff);
            dword = dword >>> 8; // Unsigned shift right
        }
    } // write4()

    /** 
     * Writes the specified qword (8-byte) into the array
     * using {@link #write(int)}.
     *
     * @param word The qword (8-byte) to write.
     *             All 64 bits of the long are written.
     * 
     * @exception RuntimeException
     *      Buffer overflow; the buffer is full, and the qword was not written.
     *
     */
    public final void write8(long qword) {
        if (cbyte >= size-7) {
            throw new RuntimeException("Cannot write; not enough space");
        }

        for (int i = 0; i < 8; i++) {
            write((int)(qword & 0xff));
            qword = qword >>> 8; // Unsigned shift right
        }
    } // write4()

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

        // Read the byte at the "bytenum" position in the data element
        int rval = (elem >>> (bytenum << 3)) & 0xff;

        bytenum++;
        cbyte++;

        return rval;
    } // read()


    /**
     * Reads up to {@code len} bytes of data into an array of bytes. 
     * An attempt is made to read as many as {@code len bytes}, 
     * but a smaller number may be read if end-of-array is met.
     *
     * @param buffer 
     *     The into which the data is read
     * @param offset 
     *     The start offset in the array at which the data is written
     * @param len
     *     The maximum number of bytes to read
     *
     * @return
     *     The total number of bytes read into the buffer, 
     *     or -1 if there is no more data because the end of the array
     *     has been reached.
     *
     */
    public final int read(
        byte[] buffer,
        int offset,
        int len
    ) {
        if (cbyte == size) {
            return -1;
        }

        int bytesRead = 0;

        // Otherwise, there's at least one byte to read
        while ((bytesRead < len) && (cbyte < size)) {
            buffer[offset] = (byte) read();
            offset++;
            bytesRead++;
        }

        return bytesRead;
    } // read()

    /**
     * Reads a byte (2-byte) currently under the read/write head.
     * The read/write head is moved forward by one byte.<p>
     * 
     * This method differs from {@link #read()} by throwing an exception
     * when not enough data available in the buffer.
     *
     * @return
     *      The byte read
     *
     * @exception RuntimeException
     *      Not enough data left in the buffer. Head is left unmoved.
     *
     */
    public final int read1() {
        if (cbyte >= size) {
            throw new RuntimeException("Cannot read; not enough data");
        }

        return read();
    }


    /**
     * Reads a word (2-byte) currently under the read/write head.
     * The read/write head is moved forward by one word.
     *
     * @return
     *      The word read
     *
     * @exception RuntimeException
     *      Not enough data left in the buffer. Head is left unmoved.
     *
     */
    public final int read2() {
        if (cbyte >= size-1) {
            throw new RuntimeException("Cannot read; not enough data");
        }

        int rval = 0;
        for (int i = 0; i < 2; i++) {
            int c = read();
            rval |= (c << (i*8));
        }

        return rval;
    }

    /**
     * Reads a dword (4-byte) currently under the read/write head.
     * The read/write head is moved forward by one dword.
     *
     * @return
     *      The dword read
     *
     * @exception RuntimeException
     *      Not enough data left in the buffer. Head is left unmoved.
     *
     */
    public final int read4() {
        if (cbyte >= size-3) {
            throw new RuntimeException("Cannot read; not enough data");
        }

        int rval = 0;
        for (int i = 0; i < 4; i++) {
            int c = read();
            rval |= (c << (i*8));
        }

        return rval;
    }

    /**
     * Reads a qdword (8-byte) currently under the read/write head.
     * The read/write head is moved forward by one qword.
     *
     * @return
     *      The qword read
     *
     * @exception RuntimeException
     *      Not enough data left in the buffer. Head is left unmoved.
     *
     */
    public final long read8() {
        if (cbyte >= size-7) {
            throw new RuntimeException("Cannot read; not enough data");
        }

        long rval = 0;
        for (int i = 0; i < 8; i++) {
            // Must cast from int to long!
            // Otherwise, the shift left might operate on
            // an insufficient data type.
            long c = (long) read();
            rval |= (c << (i*8));
        }

        return rval;
    }

    /**
     * Flushes the current data element to the array.
     * Flushing is required after a write sequence. 
     * Otherwise, some of the last bytes written may be lost.
     * 
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
     * 
     * @param tobyte 
     *      The byte offset to seek to.
     * 
     * @exception RuntimeException
     *      When the seek position is beyond the buffer boundaries.
     * 
     */
    public void seek(int tobyte) {
        if ((tobyte < 0) || (tobyte > size)) {
            throw new RuntimeException("Index out of bounds");
        }
        // Record the total byte offset
        cbyte = tobyte;
        // Calculate offset
        offset = tobyte / BYTES_IN_ELEMENT;
        // Set in-element offset
        bytenum = tobyte % BYTES_IN_ELEMENT;

        // Pick the corresponding data element, if within boundaries
        if (offset < data.length) {
            elem = data[offset];
        } else {
            elem = 0;
        }

    } // seek()

    /**
     * Get the size of the array in bytes.
     * @return The array size in bytes.
     */
    public int size() {
        return size;
    }

    /**
     * Get the reading/writing head position (in bytes).
     * @return The current byte offset
     */
    public int pos() {
        return cbyte;
    }

    // for testing
    public static void main(String[] args) {
        SequentialByteArray matrix = new SequentialByteArray();
        int len = 16; // divisible by 2, 4, and 8
        matrix.allocate(len);

        for (int i = 0; i < len; i++) {
            matrix.write(i);
        }
        matrix.flush();

        int pos = 5;
        matrix.seek(pos);
        for (int i = pos; i < len+6; i++) {
            int c = matrix.read();
            System.out.printf("data[%d] = %d\n", i, c);
        }

        // Test write2/read2
        System.out.printf("read/write 2\n");

        matrix.seek(0);
        for (int i = 0; i < len/2; i++) {
            matrix.write2(0xaabb);
        }

        matrix.seek(0);
        for (int i = 0; i < len/2; i++) {
            int c = matrix.read2();
            System.out.printf("data[%d] = %04x\n", i, c);
        }

        // Test write4/read4
        System.out.printf("read/write 4\n");


        matrix.seek(0);
        for (int i = 0; i < len/4; i++) {
            matrix.write4(0xaabbccdd);
        }

        matrix.seek(0);
        for (int i = 0; i < len/4; i++) {
            int c = matrix.read4();
            System.out.printf("data[%d] = %08x\n", i, c);
        }

        // Test write8/read8

        System.out.printf("read/write 8\n");

        matrix.seek(0);
        for (int i = 0; i < len/8; i++) {
            matrix.write8(0x12345678abcdef0L);
        }

        matrix.seek(0);
        for (int i = 0; i < len/8; i++) {
            long c = matrix.read8();
            System.out.printf("data[%d] = %08x%08x\n", 
                i, c >>> 32, (c & 0xffffffffL));
        }

        System.out.printf("reallocate: expand\n");

        len = matrix.size()*2;
        matrix.reallocate(len);

        matrix.seek(0);
        for (int i = 0; i < len/8; i++) {
            long c = matrix.read8();
            System.out.printf("data[%d] = %08x%08x\n", 
                i, c >>> 32, (c & 0xffffffffL));
        }

        // Test write8/read8

        System.out.printf("read/write 8 after realloc\n");

        matrix.seek(0);
        for (int i = 0; i < len/8; i++) {
            matrix.write8(0xfedcba9876543210L);
        }

        matrix.seek(0);
        for (int i = 0; i < len/8; i++) {
            long c = matrix.read8();
            System.out.printf("data[%d] = %08x%08x\n", 
                i, c >>> 32, (c & 0xffffffffL));
        }

        System.out.printf("reallocate: shrink\n");

        System.out.printf("Before reallocate, the head is at %d\n",
            matrix.pos());

        len = matrix.size()/2;
        matrix.reallocate(len);

        System.out.printf("After reallocate, the head is at %d\n",
            matrix.pos());


    } // main()

} // PORDataMatrix
