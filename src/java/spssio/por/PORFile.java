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

// core java
import java.util.Vector;

/**
 * Represents SPSS/PSPP's Portable file.
 */
public class PORFile {
    
    // MEMBER VARIABLES
    //==================
    
    /**
     * Portable file header. 
     * The header contains fields for the following tag codes:
     * <ul>
     *    <li>'1' (product)
     *    <li>'2' (author)
     *    <li>'3' (file label)
     *    <li>'4' (variable count)
     *    <li>'5' (precision)
     *    <li>'6' (case weight variable)
     * </ul>
     */
    public PORHeader header;
    
    /**
     * Sequence of variable records (tag code '7', struct).
     * The variable records contains the following tag codes:
     * <ul>
     *    <li>'7' (the variable record itself)
     *    <li>'8', '9', 'A' and 'B' (missing value specifications)
     *    <li>'C' (variable label)
     * </ul>
     */
    public Vector<PORVariable> variables;
    
    /**
     * Sequence of value labels records (tag code 'D').
     */
    public Vector<PORValueLabels> labels;
    
    /**
     * Document record (tag 'E', vector of strings)
     */
    // TODO
    
    /**
     * Data record (tag 'F', sequence of floating-point and string fields)
     */
    public PORMatrix data;
   

    /**
     * Sections of the Portable file in the order they were parsed.
     */
    public Vector<PORSection> sections;

    // CONSTRUCTORS
    //==============
    
    public PORFile() {
        header = new PORHeader();
        variables = new Vector<PORVariable>();
        labels = new Vector<PORValueLabels>();
        sections = new Vector<PORSection>();
        data = null;
    } // ctor
    
    // OTHER METHODS
    //===============
    
    
    // HEADER
    //========
    
    public byte[] getSplash() {
        return header.splash;
    }
    
    public byte[] getCharset() {
        return header.charset;
    }
    
    
    public String getSignature() {
        return header.signature;
    }

    public char getFormatVersion() {
        return header.version;
    }
    
    public String getCreationDate() {
        return header.date;
    }
    
    public String getCreationTime() {
        return header.time;
    }
    
    public void setSignature(String signature) {
        
        // Verify length, if not null
        if ((signature != null) &&
            (signature.length() != PORConstants.SIGNATURE_LENGTH))
        {
            throw new IllegalArgumentException(String.format(
                "Signature length must be exactly %d characters",
                PORConstants.SIGNATURE_LENGTH));
        }
        
        // Set new signature (can be null)
        header.signature = signature;
    }
    
    public void setFormatVersion(char version) {
        /*
        if (version < 0) {
            throw new IllegalArgumentException(String.format(
                "Format version must be non-negative"));
        }
        */
        
        header.version = (char) version;
    }
    
    public void setCreationDate(String date) {
        // Verify length, if not null
        if ((date != null) &&
            (date.length() != PORConstants.DATE_LENGTH))
        {
            throw new IllegalArgumentException(String.format(
                "Creation date length must be exactly %d characters",
                PORConstants.DATE_LENGTH));
        }
        
        // Set new creation date (can be null)
        header.date = date;
    }

    public void setCreationTime(String time) {
        // Verify length, if not null
        if ((time != null) &&
            (time.length() != PORConstants.TIME_LENGTH))
        {
            throw new IllegalArgumentException(String.format(
                "Creation time length must be exactly %d characters",
                PORConstants.TIME_LENGTH));
        }
        
        // Set new creation date (can be null)
        header.time = time;
    }
    
    // TODO:
    // Convert from/to a real calendar object
    
    
    // HEADER RECORDS
    //================
    
    public String getSoftware() {
        return header.software;
    }
    
    public String getAuthor() {
        return header.author;
    }
    
    public String getTitle() {
        return header.title;
    }
    
    public int getNumberOfVariables() {
        return header.nvariables;
    }
    
    public int getPrecision() {
        return header.precision;
    }
    
    public String getWeightVarName() {
        return header.weight_var_name;
    }
    
    public void setSoftware(String software) {
        if ((software != null) &&
            (software.length() > PORConstants.MAX_SOFTWARE_LENGTH))
        {
            throw new IllegalArgumentException(String.format(
                "Software length must be less than or equal to %d characters",
                PORConstants.MAX_SOFTWARE_LENGTH));
        }
        
        // Set software (can be null)
        header.software = software;
    }
    
    public void setAuthor(String author) {
        if ((author != null) &&
            (author.length() > PORConstants.MAX_AUTHOR_LENGTH))
        {
            throw new IllegalArgumentException(String.format(
                "Author length must be less than or equal to %d characters",
                PORConstants.MAX_AUTHOR_LENGTH));
        }
        
        // Set author (can be null)
        header.author = author;
    }
    
    public void setTitle(String title) {
        if ((title != null) &&
            (title.length() > PORConstants.MAX_TITLE_LENGTH))
        {
            throw new IllegalArgumentException(String.format(
                "Title length must be less than or equal to %d characters",
                PORConstants.MAX_TITLE_LENGTH));
        }
        
        // Set title (can be null)
        header.title = title;
    }
    
    public void setNumberOfVariables(int nVariables) {
        if (nVariables < 0) {
            throw new IllegalArgumentException(String.format(
                "Number of variables must be greather than or equal to 0"));
        }
        
        // Set number of variables.
        // This may differ from the actual value. Also, it could be zero.
        header.nvariables = nVariables;
    }

    public void setPrecision(int precision) {
        if (precision < 0) {
            throw new IllegalArgumentException(String.format(
                "Precision must be non-negative"));
        }
        
        // Can be zero
        header.precision = precision;
    }

    /*
    public void setWeightVarName(String weightName) {
        header.weight_var_name = weightName;
    }
    */
    
    // VARIABLE RECORDS
    //==================
    
    
    /**
     * Finds a variable with the specified name
     * @param name Variable's name.
     * @return The variable, or {@code null} if no such name found.
     */
    public PORVariable getVariable(String name) {
        for (int i = 0; i < variables.size(); i++) {
            PORVariable rval = variables.elementAt(i);
            if (name.equals(rval.name)) {
                return rval;
            }
        }
        
        return null;
    } // getVariable()
    
    
    // VALUE-LABEL MAPPING RECORDS
    //=============================
    
    
} // class PORFile

