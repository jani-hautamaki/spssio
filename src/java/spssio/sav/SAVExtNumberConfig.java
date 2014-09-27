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

/**
 * See PSPP documentation:
 * http://www.gnu.org/software/pspp/pspp-dev/html_node/Machine-Floating_002dPoint-Info-Record.html#Machine-Floating_002dPoint-Info-Record
 *
 * The PSPP system-missing value is represented by the largest possible negative number 
 * in the floating point format (-DBL_MAX). 
 * Two other values are important for use as missing values: HIGHEST, 
 * represented by the largest possible positive number (DBL_MAX),
 *  and LOWEST, represented by the second-largest negative number 
 * (in IEEE 754 format, 0xffeffffffffffffe). 
 * 
 */
public class SAVExtNumberConfig
    extends SAVExtensionRecord
{
    // MEMBER VARIABLES
    //==================
    
    /**
     * The system missing value. 
     */
    public double sysmissValue;
    
    /**
     * The value used for HIGHEST in missing values.
     */
    public double highestValue;
    
    /**
     * The value used for LOWEST in missing values. 
     */
    public double lowestValue;
    
    
    // CONSTRUCTORS
    //==============
    
    public SAVExtNumberConfig() {
        sysmissValue = SAVConstants.DEFAULT_SYSMISS_VALUE;
        highestValue = SAVConstants.DEFAULT_HIGHEST_VALUE;
        lowestValue = SAVConstants.DEFAULT_LOWEST_VALUE;
    }
    
    public SAVExtNumberConfig(
        double sysmissValue, 
        double highestValue, 
        double lowestValue
    ) {
        this.sysmissValue = sysmissValue;
        this.highestValue = highestValue;
        this.lowestValue = lowestValue;
    }
    
    // OTHER METHODS
    //===============
    
    public void setSysmissValue(double sysmissValue) {
        this.sysmissValue = sysmissValue;
    }
    
    public void setHighestValue(double highestValue) {
        this.highestValue = highestValue;
    }
    
    public void setLowestValue(double lowestValue) {
        this.lowestValue = lowestValue;
    }
    
    public double getSysmissValue() {
        return sysmissValue;
    }
    
    public double getHighestValue() {
        return highestValue;
    }
    
    public double getLowestValue() {
        return lowestValue;
    }
    
    
} // class SAVExtNumberConfig
