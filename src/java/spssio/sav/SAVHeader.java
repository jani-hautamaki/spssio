//*******************************{begin:header}******************************//
//                 spssio - http://code.google.com/p/spssio/                 //
//***************************************************************************//
//
//      Java classes for reading and writing 
//      SPSS/PSPP Portable and System files
//
//      Copyright (C) 2013-2014 Jani Hautamaki <jani.hautamaki@hotmail.com>
//
//      Licensed under the terms of GNU General Public License v3.
//
//      You should have received a copy of the GNU General Public License v3
//      along with this program as the file LICENSE.txt; if not, please see
//      http://www.gnu.org/licenses/gpl-3.0.html
//
//********************************{end:header}*******************************//

package spssio.sav;

// core java
import java.util.Date;
import java.util.Calendar;
import java.util.Locale;
import java.text.SimpleDateFormat;



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
     * Number of cases in the file, if known. Otherwise, set to -1.
     * NOTE: SPSS 20.0 is pseudo-faithful to this number.
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
        variableCount = 0;
        compressed = 0;
        weightVariableIndex = -1;
        numberOfCases = -1;
        bias = 0;
        date = null;
        time = null;
        title = null;
        padding = new byte[3];
    }
    
    public static SAVHeader createNew() {
        SAVHeader header = new SAVHeader();
        
        header.setSignature(SAVConstants.FORMAT_SIGNATURE);
        header.setSoftware(SAVConstants.DEFAULT_SOFTWARE);
        
        // According to PSPP, "Nominally set to 2", and
        // "PSPP use this value to determine the file's integer endianness".
        // TODO: What is this really?
        header.layout = 2; 
        
        // No weight variable
        header.weightVariableIndex = -1;
        
        // Initially the data will be compressed
        header.setCompressed(1);
        header.setBias(SAVConstants.DEFAULT_COMPRESSION_BIAS);
        
        // Initialize date and time to now
        header.touchTimestamp();
        
        // No title
        header.setTitle(""); 
        
        // Padding
        header.padding[0] = 0x20;
        header.padding[1] = 0x20;
        header.padding[2] = 0x20;
        
        return header;
    }
    
    // OTHER METHODS
    //===============
    
    public String getSignature() {
        return signature;
    }
    
    public void setSignature(String signature) {
        // TODO: must be exactly 4 characters when encoded with what?
        this.signature = signature;
    }
    
    public String getSoftware() {
        return software;
    }
    
    public void setSoftware(String software) {
        this.software = software;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    
    public int getNumberOfCase() {
        return numberOfCases;
    }
    
    public void setNumberOfCases(int numberOfCases) {
        this.numberOfCases = numberOfCases;
    }
    
    public int getVariableCount() {
        return variableCount;
    }
    
    public void setVariableCount(int variableCount) {
        this.variableCount = variableCount;
    }
    
    public int getCompressed() {
        return compressed;
    }
    
    public void setCompressed(int compressed) {
        this.compressed = compressed;
    }

    public double getBias() {
        return bias;
    }
    
    public void setBias(double bias) {
        this.bias = bias;
    }
    
    
    // TIMESTAMP METHODS
    //===================
    
    
    public String getDateString() {
        return date;
    }
    
    public void setDateString(String date) {
        // Date must be exactly X chars when encoded with what?
        this.date = date;
    }
    
    public String getTimeString() {
        return time;
    }
    
    public void setTimeString(String time) {
        // Time must be exactly X chars when encoded with what?
        this.time = time;
    }
    
    public Date getTimestamp() {
        // Combine date and time strings.
        // It is easier to combine than add time to the date in Java,
        // you need to take my word for it...
        StringBuilder sb = new StringBuilder(9+1+8);
        sb.append(this.date);
        sb.append(' ');
        sb.append(this.time);
        String full = sb.toString();

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yy HH:mm:ss", Locale.US);
        Date rval = null;
        try {
            rval = sdf.parse(full);
        } catch(Exception ex) {
            // Pass up
            throw new RuntimeException(ex);
        }
        
        return rval;
    }
    
    public void setTimestamp(Date timestamp) {
        SimpleDateFormat sdfDate
            = new SimpleDateFormat("dd MMM yy", Locale.US);
        
        SimpleDateFormat sdfTime 
            = new SimpleDateFormat("HH:mm:ss", Locale.US);
        
        setDateString(sdfDate.format(timestamp));
        setTimeString(sdfTime.format(timestamp));
    }
    
    public void touchTimestamp() {
        Date now = new Date();
        setTimestamp(now);
    }

} // class SAVHeader

