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

public class Utf8Helper {

    // CONSTANTS
    //===========

    // MEMBER VARIABLES
    //==================

    // CONSTRUCTORS
    //==============

    /**
     * Constructor intentionally disabled.
     */
    private Utf8Helper() {
    }

    // OTHER METHODS
    //===============

    /**
     * Probes an encoded string whether it could be UTF-8 encoded or not.
     *
     * Return values:
     *   -1: invalid UTF-8
     *    0: probing inconclusive (uses only codepoints 0-127).
     *  >=1: number of valid multi-byte characters found.
     */
    public static int countMultibyteChars(
        byte[] encoded,
        int offset,
        int length
    ) {
        int bytesLeft = 0; // Continuation bytes left
        int mbChars = 0; // Number of multi-byte characters

        for (int i = 0; i < length; i++) {
            byte b = encoded[offset+i];

            if ((b & 0x80) == 0x00) { // 0b0xxxxxxx
                // 1-byte character.
                // This cannot occur within a multi-byte char.
                if (bytesLeft > 0) return -1;
            } else if ((b & 0xC0) == 0x80) { // 0b10xxxxxx
                // Continuation byte (of a multi-byte char).
                // This cannot occur now.
                if (bytesLeft == 0) return -1;
                // Decrease the number of continuation bytes left.
                bytesLeft--;
                if (bytesLeft == 0) mbChars++;
            } else if ((b & 0xE0) == 0xC0) { // 0b110xxxxx
                // Leading byte; 2-byte character
                // This cannot occur within another multi-byte char.
                if (bytesLeft > 0) return -1;
                bytesLeft = 1;
            } else if ((b & 0xF0) == 0xE0) { // 0b1110xxxx
                // Leading byte; 3-byte character
                // This cannot occur within another multi-byte char.
                if (bytesLeft > 0) return -1;
                bytesLeft = 2;
            } else if ((b & 0xF8) == 0xF0) { // 0b11110xxx
                // Leading byte; 4-byte character
                // This cannot occur within another multi-byte char.
                if (bytesLeft > 0) return -1;
                bytesLeft = 3;
            } // if-else
        } // for

        return mbChars;
    }
}
