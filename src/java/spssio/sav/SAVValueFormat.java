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
 * How the value is formatted when printing (output),
 * or how the value is expected to be formatted when parsing (input).
 */
public class SAVValueFormat {
    
    // MEMBER VARIABLES
    //==================
    
    /**
     * The least-significant byte of the int32 represents 
     * the number of decimal places, 
     * and the next two bytes in order of increasing significance represent 
     * field width and format type, respectively. 
     * The most-significant byte is not used and should be set to zero. 
     */
    public int raw;
    
    
    // CONSTRUCTORS
    //==============
    
    public SAVValueFormat() {
        raw = 0;
    }

    public SAVValueFormat(int raw) {
        this.raw = raw;
    }
    
    public SAVValueFormat(int width, int decimals, int type) {
        // truncate
        width = width & 0xff;
        decimals = decimals & 0xff;
        type = type & 0xff;
        // assign
        this.raw = (decimals << 16) | (width << 8) | (type);
    }
    
    // OTHER METHODS
    //===============
    
    public int getWidth() {
        return (raw >>> 8) & 0xff;
    }

    public int getDecimals() {
        return (raw >>> 16) & 0xff;
    }

    public int getType() {
        return (raw >>> 0) & 0xff;
    }
    
    public void setWidth(int width) {
        raw &= ~(0xff << 8);
        raw |= (width & 0xff) << 8;
    }

    public void setDecimals(int decimals) {
        raw &= ~(0xff << 16);
        raw |= (decimals & 0xff) << 16;
    }

    public void setType(int type) {
        raw &= ~(0xff << 0);
        raw |= (type & 0xff) << 0;
    }
    
}