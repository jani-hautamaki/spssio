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
 * Portable file header.
 *
 */
public class PORHeader {
    
    /** 
     * Five splash strings.
     */
    public byte[] splash;
    
    /**
     * 256-byte character set translation table.
     */
    public byte[] charset;
    
    /**
     * 8-byte signature string, {@code "SPSSPORT"}.
     */
    public String signature;

    /**
      * A single character identifying the file format version. 
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
        splash = null;
        charset = null;
        signature = null;
        version = 0;
        date = null;
        time = null;
    } // ctor
    
} // class PORHeader

