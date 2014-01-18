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
 * The class is used to encapsulate constants related to 
 * the data endianness.<p>
 *
 * The average home computer (PC) has Little-Endian architecture.<p>
 *
 * The name "Big-Endian" suggests that the byte sequence ends with 
 * the most significant byte (MSB). However, when looking at 
 * a memory dump, it would appear that in Big-Endian systems 
 * the opposite is true. Remember that binary values are read in 
 * the opposite order, from left to right. Once you remember that, 
 * the endianness makes sense.<p>
 * 
 * Wikipedia article on the subject:
 * {@url http://en.wikipedia.org/wiki/Endianness}<p>
 *
 * According to the wiki article...<p>
 *
 * "The Intel x86 and x86-64 series of processors use the little-endian 
 * format, and for this reason, the little-endian format is also known 
 * as the <b>Intel convention</b>. Other well-known little-endian 
 * processor architectures are the 6502 (including 65802, 65C816), 
 * Z80 (including Z180, eZ80 etc.), MCS-48, 8051, DEC Alpha, 
 * Altera Nios II, Atmel AVR, VAX, and, largely, PDP-11.<p>
 *
 * "The Motorola 6800 and 68k series of processors use the big-endian 
 * format, and for this reason, the big-endian format is also known 
 * as the <b>Motorola convention</b>. Other well-known processors that 
 * use the big-endian format include the Xilinx Microblaze, SuperH, 
 * IBM POWER, and System/360 and its successors such as System/370, 
 * ESA/390, and z/Architecture. The PDP-10 also used big-endian 
 * addressing for byte-oriented instructions."<p>
 *
 */
public class SAVEndianness {

    // CONSTANTS
    //===========
    
    /**
     * Data is arranged in Big-Endian order.<p>
     *
     * In Big-Endian, the smallest address receives
     * the most signifcant byte (MSB).
     *
     */
    public static final int BIG_ENDIAN = 1;
    
    /**
     * Data is arranged in Little-Endian order.<p>
     *
     * In Little-Endian, the smallest address receives
     * the least significant byte (LSB).
     *
     */
    public static final int LITTLE_ENDIAN = -1;
    
    /**
     * An invalid endianness value; others can be used also,
     * but this is always guaranteed to be invalid.
     */
    public static final int INVALID = 0;
    
    // CONSTRUCTORS
    //==============
    
    /**
     * Constructor is intentionally disabled.
     */
    private SAVEndianness() {
    }
    
    // OTHER METHODS
    //===============
    
    public static boolean isValid(int endianness) {
        boolean rval = true;
        
        if (endianness == BIG_ENDIAN) {
            // Valid
        } else if (endianness == LITTLE_ENDIAN) {
            // Valid
        } else {
            // Invalid
            rval = false;
        } // if-else
        
        return rval;
    }
    
} // class SAVEndianness

