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



package spssio.por;

/**
 * Portable file header.
 *
 */
public class PORHeader {

    /**
     * 5x 40-byte splash string.
     */
    public byte[] splash;

    /**
     * 256-byte character-to-byte translation table.
     */
    public byte[] translation;

    /**
     * 8-byte signature string, {@code "SPSSPORT"}.
     */
    public String signature;

    /**
      * A 1-byte character identifying the file format version.
      * The letter A represents version 0, and so on.
      */
    public char version;

    /**
     * An 8-character string field giving the file creation date
     * in the format YYYYMMDD.
     */
    public String date;

    /**
     * A 6-character string field giving the file creation time
     * in the format HHMMSS.
     */
    public String time;

    // CONSTRUCTORS
    //==============

    public PORHeader() {

        splash = new byte[PORConstants.SPLASH_LENGTH];
        translation = new byte[PORConstants.TRANSLATION_LENGTH];

        //signature = new byte[PORConstants.SIGNATURE_LENGTH];
        signature = null; // This is a string

        version = 0;

        date = null;
        time = null;
    } // ctor

    public static PORHeader createNew() {
        PORHeader header = new PORHeader();

        // TODO: default splashes
        // header.splash =

        // TODO: default charset

        header.signature = PORConstants.FORMAT_SIGNATURE;
        header.version = PORConstants.FORMAT_VERSION;

        // TODO: use current date/time
        //header.date = null;
        //header.time = null;

        return header;
    }

    // OTHER METHODS
    //===============

} // class PORHeader

