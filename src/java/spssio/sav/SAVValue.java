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
import java.util.Arrays;

public class SAVValue
{
    // CONSTANTS
    //===========
    
    /** The cell has unassigned type. */
    public static final int TYPE_UNASSIGNED     = -1;
    
    /** The cell has numeric type. */
    public static final int TYPE_NUMERIC        =  0;
    
    /** The cell has textual type. */
    public static final int TYPE_STRING         =  1;
    
    /** The cell has raw type. */
    public static final int TYPE_RAW            =  2;
    
    // MEMBER VARIABLES
    //==================
    
    /**
     * Determines the type of the cell:
     */
    public int type;
    
    /**
     * The literal contents of the cell, or {@code null} if the cell is empty.
     * If the type is numeric, then this is the numeric value represented as
     * a string, and the string is identical to the one that was found from or will
     * be written to the Portable file.
     */
     
    public String valueString;
    
    public Double valueDouble;
    
    public byte[] valueRaw;

    // CONSTRUCTORS
    //==============
    
    /**
     * Creates an empty cell with unassigned type.
     */
    public SAVValue() {
        type = TYPE_UNASSIGNED;
        
        valueString = null;
        valueDouble = null;
        valueRaw = null;
    } // ctor

    /**
     * Creates a cell with raw value
     */
    public SAVValue(byte[] value) {
        type = TYPE_RAW;
        
        valueString = null;
        valueDouble = null;
        valueRaw = value;
    } // ctor

    // OTHER METHODS
    //===============
    
    // TODO:
    // Type mutation is unallowed!
    
    public void setString(String value) {
        if (type != TYPE_UNASSIGNED) {
            throw new RuntimeException("Mutation not allowed");
        }
        
        valueString = value;
        valueDouble = null;
        valueRaw = null;
        
        type = TYPE_STRING;
    }
    
    public void setDouble(Double value) {
        if (type != TYPE_UNASSIGNED) {
            throw new RuntimeException("Mutation not allowed");
        }
        
        valueString = null;
        valueDouble = value;
        valueRaw = null;
        
        type = TYPE_NUMERIC;
    }
    
    public void setRaw(byte[] value) {
        if (type != TYPE_UNASSIGNED) {
            throw new RuntimeException("Mutation not allowed");
        }
        
        valueString = null;
        valueDouble = null;
        valueRaw = Arrays.copyOf(value, value.length);
    }
    
    
    public Double getDouble() {
        return valueDouble;
    }
    
    public String getString() {
        return valueString;
    }
    
    public byte[] getRaw() {
        return valueRaw;
    }
    
    public int getType() {
        return type;
    }
    
    
    public boolean isNumeric() {
        return type == TYPE_NUMERIC;
    }

    public boolean isString() {
        return type == TYPE_STRING;
    }

    public boolean isRaw() {
        return type == TYPE_RAW;
    }
    
    public boolean isUnassigned() {
        return type == TYPE_UNASSIGNED;
    }
    
    
    public String toString() {
        String rval = null;
        switch(type) {
            case TYPE_UNASSIGNED:
                rval = new String();
                break;
            
            case TYPE_NUMERIC:
                rval = Double.toString(valueDouble);
                break;
            
            case TYPE_STRING:
                rval = valueString;
                break;
            
            case TYPE_RAW:
                rval = String.format(
                    "%02x%02x%02x%02x%02x%02x%02x%02x",
                    valueRaw[0], valueRaw[1], valueRaw[2], valueRaw[3],
                    valueRaw[4], valueRaw[5], valueRaw[6], valueRaw[7]
                ); // String.format()
                break;
            default:
                throw new RuntimeException(String.format(
                    "Unhandled type: %d (programming error)", type));
        }
        return rval;
    }
    
} // class PORValue