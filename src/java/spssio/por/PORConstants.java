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
 * Parameters affecting the deserialization and serialization
 * of SPSS/PSPP Portable files.
 *
 */
public class PORConstants
{
    
    // CONSTANTS
    //===========
    
    /**
     * Text row width for Portable files (bytes).
     */
    public static final int ROW_WIDTH = 80;
    
    
    /**
     * Maximum length of a data element (bytes).
     * A data element is either a single number field or string contents.
     */
    public static final int MAX_DATA_ELEMENT_LENGTH = 1024;
    
    
    // THESE ARE TODO (Note: they dont have "final" qualifier)
    
    public static int MAX_SOFTWARE_LENGTH = 255;
    
    public static int MAX_AUTHOR_LENGTH = 255;
    
    public static int MAX_TITLE_LENGTH = 255;
    
    public static int MAX_VARNAME_LENGTH = 8;
    
    public static int MAX_VARLABEL_LENGTH = 255;
    
    
    /**
     * File format signature.
     */
    public static final String FORMAT_SIGNATURE     = "SPSSPORT";
    
    /**
     * File format version
     */
    public static final int FORMAT_VERSION          = 'A';
    
    
    // CONSTRUCTORS
    //==============
    
    /**
     * Constructor is intentionally disabled.
     */
    private PORConstants() {
    } // ctor
    
} // class PORConstants