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

import spssio.sav.input.SAVMatrixParser;
import spssio.sav.input.SAVMatrixDecompressor;
import spssio.util.ByteCursor;

public class SAVMatrixIterator {

    // HELPER CLASSES
    //================

    private static class SAVValueBuffer {

        // CONSTANTS
        //===========

        private static final int MAX_BUFFER_LENGTH = 16;

        // MEMBER VARIABLES
        //==================

        /**
         * Buffer for received but unforwarded values
         */
        private SAVValue[] values;

        /**
         * Number of valid values in the buffer,
         * that is, offset to the next unused slot.
         */
        private int length;

        /**
         * Offset of the next unread value in the buffer.
         */
        private int next;

        // CONSTRUCTORS
        //==============

        public SAVValueBuffer() {
            values = new SAVValue[MAX_BUFFER_LENGTH];
            length = 0;
            next = 0;
        }

        public void clear() {
            length = 0;
            next = 0;
        }

        public void write(SAVValue value) {
            if (length >= MAX_BUFFER_LENGTH) {
                throw new RuntimeException("buffer full");
            }
            values[length++] = value;
        }

        public boolean canRead() {
            return next < length;
        }

        public boolean isEmpty() {
            return next == length;
        }

        public SAVValue read() {
            SAVValue value = null;
            if (next < length) {
                // Pop the next value
                value = values[next];

                // Move forward, and reset buffer if it became exhausted.
                next++;
                if (next == length) {
                    // Reset buffer
                    clear();
                }
            }
            return value;
        }

    } // class SAVValueBuffer

    // INTERNAL CALSSES
    //==================

    class SAVBufferingHandler
        implements SAVMatrixHandler
    {
        // CONSTRUCTORS
        //==============

        public SAVBufferingHandler() {
        }

        // OTHER METHODS
        //===============

        public void onMatrixBegin(int xsize, int ysize, int[] columnWidths) {
            valueBuffer.clear();
        }

        public void onMatrixEnd() {
        }

        public void onRowBegin(int y) {
        }

        public void onRowEnd(int y) {
        }

        public void onCellSysmiss(int x) {
            // SYSMISS cannot appear on string variables
            valueBuffer.write(SAVValue.newDoubleValue(null));
        }

        public void onCellNumber(int x, double value) {
            valueBuffer.write(SAVValue.newDoubleValue(value));
        }

        public void onCellString(int x, String value) {
            valueBuffer.write(SAVValue.newStringValue(value));
        }

        public void onCellInvalid(int x) {
            throw new RuntimeException("Impossible to land here");
        }
    } // class SAVBufferingHandler

    // MEMBER VARIABLES
    //==================

    /**
     * The streaming parser used to convert
     * the data stream into parsing events.
     */
    private SAVMatrixParser parser;

    /**
     * If the data is compressed, this filter is used
     * to decompress it. Otherwise, this is {@code null}.
     */
    private SAVMatrixDecompressor decompressor;

    /**
     * Reader for the byte container holding all the data.
     */
    private ByteCursor cursor;

    /**
     * Buffer for the 8-byte block read each time.
     */
    private byte[] byteBuffer;

    /**
     * Buffer for values received from the buffering handler.
     */
    private SAVValueBuffer valueBuffer;

    // CONSTRUCTORS
    //==============

    /**
     * A bit kludge here due to all the member variables
     * being received from elsewhere properly configured...
     * Need to fix this one.
     */
    SAVMatrixIterator(
        SAVMatrixParser parser,
        SAVMatrixDecompressor decompressor,
        ByteCursor cursor
    ) {
        this.parser = parser;
        this.decompressor = decompressor;
        this.cursor = cursor;
        this.byteBuffer = new byte[8];
        this.valueBuffer = new SAVValueBuffer();

        // Set the content handler to the parser
        this.parser.setContentHandler(new SAVBufferingHandler());
    }

    // OTHER METHODS
    //===============

    public void reset() {
        // Rewind the array head, and reset the parser
        cursor.setOffset(0);

        if (decompressor != null) {
            // Data is compressed; parser is reset by the decompressor
            decompressor.reset();
        } else {
            // Data is uncompressed
            parser.reset();
        }
    }

    public SAVValue next() {
        int bytesRead = -1;
        int errno = 0;

        if (!valueBuffer.canRead() && cursor.canRead()) {
            // Value buffer is empty, and there are bytes left in the array.
            // Therefore, attempt to refill the value buffer by parsing
            // some more, if possible.
            while (!valueBuffer.canRead()) {
                bytesRead = cursor.read(byteBuffer, 0, 8);
                if (bytesRead == -1) {
                    // Reached the end-of-array.
                    // Send EOF to the decompressor, and break out.
                    errno = decompressor.consume(null);
                    break;
                }

                // Assert that 8 bytes were read
                if (bytesRead != 8) {
                    throw new RuntimeException(String.format(
                        "Should not happen: read %d bytes instead of 8; This is a strong indication of a bug", bytesRead));
                }

                // Send the byte buffer to the decompressor
                errno = decompressor.consume(byteBuffer);

                if (errno > SAVMatrixDecompressor.E_OK) {
                    // Has finished with an error state
                    // This shouldn't happen.
                    throw new RuntimeException(String.format(
                        "Should not happen: SAVMatrixDecompressor finished with an error state. This is a strong indication of a bug. Details: %s (errno=%d)",
                        decompressor.strerror(), errno));
                }
            }
        } else {
            // Do not refill; consume the value buffer first.
        }

        if (valueBuffer.canRead()) {
            // Consume value buffer first.
            return valueBuffer.read();
        }
        // No more values; at the end-of-matrix.
        return null;
    }
}
