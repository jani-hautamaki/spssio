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

// spssio
import spssio.sav.input.SAVMatrixParser;
import spssio.sav.input.SAVMatrixDecompressor;
import spssio.util.SequentialByteArray;


public class SAVRawMatrix
    implements SAVMatrix
{

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
     * The sequential byte array holding all the data.
     */
    private SequentialByteArray array;

    /**
     * Used to cache the calculated size in X dimension.
     */
    private int xsize;

    // CONSTRUCTORS
    //==============

    public SAVRawMatrix(
        SequentialByteArray array,
        SAVMatrixParser parser,
        SAVMatrixDecompressor decompressor
    ) {
        this.array = array;
        this.parser = parser;
        this.decompressor = decompressor;

        // TODO: The parser knows the size in Y dimension,
        // after the matrix has been sent through it once...

        recalcSizeX();
    }

    // OTHER METHODS
    //===============

    protected void recalcSizeX() {
        // Calculate the size in X dimension
        int[] columnWidths = getColumnWidths();

        // Reset xsize
        xsize = 0;

        for (int i = 0; i < columnWidths.length; i++) {
            if (columnWidths[i] >= 0) {
                xsize++;
            }
        } // for
    }

    @Override
    public int sizeX() {
        //return parser.getNumberOfVariables();
        return xsize;
    }

    @Override
    public int sizeY() {
        return 0;
    }

    @Override
    public int sizeBytes() {
        return 0;
    }

    @Override
    public int[] getColumnWidths() {
        return parser.getColumnWidths();
    }

    @Override
    public void traverse(SAVMatrixHandler contentHandler) {
        // The buffer for read data
        byte[] data = new byte[8];

        // Set the content handler to the parser
        parser.setContentHandler(contentHandler);

        // Rewind the array head, and reset the parser
        array.seek(0);

        if (decompressor != null) {
            // Data is compressed
            decompressor.reset();

            int bytesRead = -1;
            int errno = 0;

            while ((bytesRead = array.read(data, 0, 8)) != -1) {

                // Assert that 8 bytes were read
                if (bytesRead != 8) {
                    throw new RuntimeException(String.format(
                        "Should not happen: read %d bytes instead of 8; This is a strong indication of a bug", bytesRead));
                }

                // Send them to the decompressor
                errno = decompressor.consume(data);

                if (errno > SAVMatrixDecompressor.E_OK) {
                    // Has finished with an error state
                    // This shouldn't happen.
                    throw new RuntimeException(String.format(
                        "Should not happen: SAVMatrixDecompressor finished with an error state. This is a strong indication of a bug. Details: %s (errno=%d)",
                        decompressor.strerror(), errno));
                }
            } // while: data left

            // Send EOF to the decompressor
            errno = decompressor.consume(null);

            // Verify success
            if (errno != SAVMatrixDecompressor.E_OK) {
                // Has finished with an error state
                // This shouldn't happen.
                throw new RuntimeException(String.format(
                    "Should not happen: SAVMatrixDecompressor finished with an error state. This is a strong indication of a bug. Details: %s (errno=%d)",
                    decompressor.strerror(), errno));
            }

        } else {
            // Data is uncompressed
            parser.reset();

            // TODO: Implementation
            throw new RuntimeException("Uncompressed traversal not implemented");

        } // if-else

        // Unset the content handler.
        // If an exception is thrown, this won't be executed.
        parser.setContentHandler(null);
    }

} // SAVMatrix
