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
 * Represents SPSS/PSPP's Portable file.<p>
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
public class PORFile {

    // CONSTANTS
    //===========
    
    public static final int DEFAULT_PRECISION = 11;
   
    // MEMBER VARIABLES
    //==================
    
    /**
     * Portable file header. 
     *
     */
    public PORHeader header;
    
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
     * Tag code '4', the number of variables in the file 
     * dictionary (mandatory).
     */
    public int variableCount;
    
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
    public PORVariable weightVariable;
    
    /** 
     * The actual weight variable name found from the Portable file. 
     */
    public String weightVariableName;
    
    /**
     * Sequence of variable records (tag code '7', struct).
     * The variable records contains the following tag codes:
     * <ul>
     *    <li>'7' (the variable record itself)
     *    <li>'8' (missing discrete value)
     *    <li>'9' (missing open range lo)
     *    <li>'A' (missing open range hi)
     *    <li 'B' (missing closed range)
     *    <li>'C' (variable label)
     * </ul>
     */
    public Vector<PORVariable> variables;
    
    /**
     * Sequence of value labels records (tag code 'D').
     * TODO: Rename into valueLabelMaps?
     */
    public Vector<PORValueLabels> labels;
    
    /**
     * Document record (tag 'E', vector of strings)
     */
    public Vector<String> documents;
    
    /**
     * Data record (tag 'F', sequence of floating-point and string fields)
     * TODO: Rename into dataMatrix
     */
    public PORMatrix data;
   

    /**
     * Sections of the Portable file in the order they were parsed.
     */
    public Vector<PORSection> sections;

    // CONSTRUCTORS
    //==============
    
    public PORFile() {
        // Header
        header = new PORHeader();
        
        // Identification records
        software = null;
        author = null;
        title = null;
        variableCount = 0;
        precision = 0;
        weightVariable = null;
        weightVariableName = null;
        
        // Variables
        variables = new Vector<PORVariable>();
        
        // Value labels
        labels = new Vector<PORValueLabels>();

        // Documents
        documents = null;
        
        // Sections
        sections = new Vector<PORSection>();
        
        // Data matrix
        data = null;
    } // ctor
    
    public static PORFile createNew() {
        PORFile por = new PORFile();
        
        // Use default precision
        por.precision = DEFAULT_PRECISION;
        
        // TODO: use a default software
        
        
        
        return por;
    }
    
    // OTHER METHODS
    //===============
    
    public PORHeader getHeader() {
        return header;
    }
    
    // HEADER
    //========
    
    public byte[] getSplash() {
        return header.splash;
    }
    
    public byte[] getTranslation() {
        return header.translation;
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
    
    
    // HEADER RECORDS: GETTERS
    //=========================
    
    public String getSoftware() {
        return software;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public String getTitle() {
        return title;
    }
    
    public int getVariableCount() {
        return variableCount;
    }
    
    public int getPrecision() {
        return precision;
    }
    
    public String getWeightVariableName() {
        String name = null;
        
        if (weightVariable != null) {
            name = weightVariable.name;
        } else {
            name = weightVariableName;
        }
        return name;
    }
    
    public PORVariable getWeightVariable() {
        return weightVariable;
    }

    // HEADER RECORDS: SETTERS
    //=========================
    
    public void setSoftware(String software) {
        if ((software != null) &&
            (software.length() > PORConstants.MAX_SOFTWARE_LENGTH))
        {
            throw new IllegalArgumentException(String.format(
                "Software length must be less than or equal to %d characters",
                PORConstants.MAX_SOFTWARE_LENGTH));
        }
        
        // Set software (can be null)
        this.software = software;
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
        this.author = author;
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
        this.title = title;
    }
    
    public void setVariableCount(int vcount) {
        if (vcount < 0) {
            throw new IllegalArgumentException(String.format(
                "Variable count must be greather than or equal to 0"));
        }
        
        // Set variable count; the value may differ from the actual
        // number of variables. It could be zero, too.
        variableCount = vcount;
    }

    public void setPrecision(int precision) {
        if (precision < 0) {
            throw new IllegalArgumentException(String.format(
                "Precision must be non-negative"));
        }
        
        // Can be zero
        this.precision = precision;
    }
    
    public void setWeightVariableName(String name) {
        // Attempt resolving the name
        weightVariable = getVariable(name);
        
        // Inspect the result
        if (weightVariable == null) {
            // No match. Resort to using plain name
            weightVariableName = name;
        } else {
            // Match. Rely on the actual variable
            weightVariableName = null;
        }
    }
    
    public void setWeightVariable(PORVariable variable) {
        weightVariableName = null;
        weightVariable = variable;
    }
    
    // VARIABLE RECORDS
    //==================
    
    /**
     * Return the actual number of variables.
     *
     * @return The number of variables in the variables container.
     */
    public int numberOfVariables() {
        return variables.size();
    }
    
    
    /**
     * Find a variable with the specified name.
     *
     * @param name Variable's name.
     *
     * @return The variable, or {@code null} if no such name found.
     */
    public PORVariable getVariable(String name) {
        for (int i = 0; i < variables.size(); i++) {
            PORVariable variable = variables.elementAt(i);
            if (name.equals(variable.name)) {
                return variable;
            }
        }
        
        // Not found
        return null;
    } // getVariable()
    
    public Vector<PORVariable> getVariablesVector() {
        return variables;
    }
    
    public int getNumberOfVariables() {
        return variables.size();
    }
    
    public PORVariable getVariableAt(int index) {
        return variables.elementAt(index);
    }
    
    public void addVariable(PORVariable v) {
        // TODO:
        // Validate the variable,
        // Verify that there does not exist a variable with the same name?
        // Assert that data has not been added yet, or invalidate the data
        variables.add(v);
    }
    
    // TODO: Variable removal requires that the associations
    // from ValueLabels must be removed as well.
    public PORVariable removeVariableAt(int index) {
        PORVariable v = variables.elementAt(index);
        
        variables.removeElementAt(index);
        
        // Remove the variable also from PORValueLabels, if any
        
        Vector<PORValueLabels> empty_vallabels
            = new Vector<PORValueLabels>();
        
        for (PORValueLabels vallabel : labels) {
            boolean removed = false;
            
            // Removes all instances
            while (vallabel.vars.removeElement(v) == true) {
                removed = true;
            }
            
            // If the vallabels became empty, it is queued for removal too
            if ((removed == true) && (vallabel.vars.isEmpty() == true)) {
                empty_vallabels.add(vallabel);
            }
        } // for: each valuelabels
        
        // Remove all vallabels which became empty
        labels.removeAll(empty_vallabels);
        
        return v;
    } // removeVariableAt()
    
    
    // VALUE-LABEL MAPPING RECORDS
    //=============================
    
    
} // class PORFile
