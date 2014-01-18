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

package spssio.sav;


/** 
 * File Header record.
 * 
 * See documentation at {@url http://www.gnu.org/software/pspp/pspp-dev/html_node/File-Header-Record.html#File-Header-Record}
 */
public class SAVHeader {
    
    // MEMBER VARIABLES
    //==================
    
    /**
     * System file signature, 4 chars
     */
    public String signature;
    
    /**
     * Product identification string, 60 chars.
     * This always begins with the characters "{@code @(#) SPSS DATA FILE}".
     * The string is truncated if it would be longer than 60 characters; 
     * otherwise it is padded on the right with spaces. 
     */
    public String software;
    
    /**
     * File layout code. Normally set to 2. 
     * This value is used to determine the file's integer endianness.
     */
    public int layout;
    
    /**
     * Number of data elements per case. 
     */
    public int variableCount;
    
    /**
     * Set to 1 if the data in the file is compressed.
     *
     */
    public int compressed;
    
    /**
     * If one of the variables is used as a weighting variable,
     * this is the 1-based variable number, otherwise set to zero.
     */
    public int weightVariableIndex;
    //public PORVariable weightVariable; 
    
    /**
     * Number of cases in teh file, if known. Otherwise, set to -1.
     */
    public int numberOfCases;
    
    /**
     * Compression bias. Nominally set to 100.
     * Only integers between 1-bias and 251-bias can be compressed.
     * By assuming that its value is 100, this can be used to determine
     * the file's floating point format and endianess.
     */
    public double bias;

    /** 
     * Date of creation of the system file in {@code dd mmm yy} format,
     * with the mont as standard English abbreviations.
     */
    public String date;
    
    /**
     * Time of creation of the system file, in {@code hh:mm:ss} format
     * using 24-hour time.
     */ 
    public String time;
    
    /**
     * File label declared by the user (64 chars).
     *
     */
    public String title;
    
    /**
     * 3-byte padding to make the header a multiple of 32 bits in length.
     *
     */
    public byte[] padding;

    // CONSTRUCTORS
    //==============
    
    public SAVHeader() {
        signature = null;
        software = null;
        layout = 0;
        variableCount = -1;
        compressed = 0;
        weightVariableIndex = -1;
        numberOfCases = -1;
        bias = 0;
        date = null;
        time = null;
        title = null;
        padding = new byte[3];
    }
    
    // OTHER METHODS
    //===============
    

} // class SAVHeader

