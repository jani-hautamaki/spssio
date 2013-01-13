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
 * TODO: Try manually leaving out some records and loading the file to SPSS to 
 * see whether they really are mandatory or not.<p>
 *
 * Relevant documentation:
 * <ul>
 *   <li>
 *      <a href="http://www.gnu.org/software/pspp/pspp-dev/html_node/Portable-File-Header.html#Portable-File-Header">
 *         A.3 Portable File Header
 *      </a>
 *   <li>
 *      <a href="http://www.gnu.org/software/pspp/pspp-dev/html_node/Version-and-Date-Info-Record.html#Version-and-Date-Info-Record">
 *         A.4 Version and Date Info Record
 *      </a>
 *   <li>
 *      <a href="http://www.gnu.org/software/pspp/pspp-dev/html_node/Identification-Records.html#Identification-Records">
 *         A.5 Identification Records
 *      </a>
 *   <li>
 *      <a href="http://www.gnu.org/software/pspp/pspp-dev/html_node/Variable-Count-Record.html#Variable-Count-Record">
 *         A.6 Variable Count Record
 *      </a>
 *   <li>
 *      <a href="http://www.gnu.org/software/pspp/pspp-dev/html_node/Case-Weight-Variable-Record.html#Case-Weight-Variable-Record">
 *         A.7 Case Weight Variable Record
 *      </a>
 * </ul>
 * 
 */
public class PORHeader {
    
    /** 
     * Five splash strings.
     */
    public String[] splash;
    
    /**
     * 256-byte character set translation table.
     */
    public byte[] translation;
    
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
    
    // Identification records
    //========================
    
    /**
     * Tag code '1', the product that wrote the portable file (mandatory?).
     * Portable file max 255 chars, while System file max 60 chars.
     */
    public String software;
    
    /**
     * Tag code '2', the name of the person who caused the portable file 
     * to be written (optional). Portable file max 255 chars, System files 
     * don't have author information in the header. If the field is not
     * present, this is set to {@code null}.
     */
    public String author;
    
    /**
     * Tag code '3', the file label (optional). Portable file max 255 chars, 
     * System files max 64 chars. PSPP calls this field as "subproduct 
     * identification" in Portable files, but in System files this is file label.
     * If the field is not present, this is set to {@code null}.
     */
    public String title;
    
    /**
     * Tag code '4', the number of variables in the file dictionary (mandatory).
     */
    public int nvariables;
    
    /**
     * Tag code '5', the precision used for Portable file base-30 floating point
     * numbers (mandatory). No equivalent in the System file, and PSPP has not 
     * documented this field. Typical value is 11.
     */
    public int precision;
    
    /**
     * Tag code '6', the case weight variable's index number (optional).
     * If the cases are unweighted, this is set to -1. In Portable file this 
     * is actually given as a string containing the name of the variable. 
     * Translation is done by the parser after the variables have been read.
     */
    public int weight_var_index;
    
} // class PORHeader

