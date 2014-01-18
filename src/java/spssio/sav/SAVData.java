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
public class SAVData {
    
    // CONSTRUCTORS
    //==============
    
    /**
     * Constructor is intentionally disabled.
     */
    private SAVData() {
    }
    
    // OTHER METHODS
    //===============
    
    public static double bytesToDouble(byte[] bytes, int offset, int endianness) {
        long raw = 0;
        int cbyte = -1;
        
        switch(endianness) {
            case SAVEndianness.BIG_ENDIAN:
                for (int i = 0; i < 8; i++) {
                    cbyte = ((int)bytes[offset]) & 0xff;
                    raw = raw << 8;
                    raw = raw | cbyte;
                    offset++;
                }
                break;
            case SAVEndianness.LITTLE_ENDIAN:
                offset += 7;
                for (int i = 0; i < 8; i++) {
                    cbyte = ((int)bytes[offset]) & 0xff;
                    raw = raw << 8;
                    raw = raw | cbyte;
                    offset--;
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
        
        return Double.longBitsToDouble(raw);
    }
    
    public static int bytesToInteger(byte[] bytes, int offset, int endianness) {
        int raw = 0;
        int cbyte = -1;
        
        switch(endianness) {
            case SAVEndianness.BIG_ENDIAN:
                for (int i = 0; i < 4; i++) {
                    cbyte = ((int)bytes[offset]) & 0xff;
                    raw = raw << 8;
                    raw = raw | cbyte;
                    offset++;
                }
                break;
            case SAVEndianness.LITTLE_ENDIAN:
                offset += 3;
                for (int i = 0; i < 4; i++) {
                    cbyte = ((int)bytes[offset]) & 0xff;
                    raw = raw << 8;
                    raw = raw | cbyte;
                    offset--;
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
        
        return raw;
    }
    
    
    public static void integerToBytes(
        byte[] bytes, 
        int offset,
        int value, 
        int endianness
    ) {
        byte cbyte = 0;
        switch(endianness) {
            case SAVEndianness.BIG_ENDIAN:
                offset += 3;
                for (int i = 3; i >= 0; i--) {
                    cbyte = (byte)(value & 0xff);
                    value = value >>> 8;
                    bytes[offset] = cbyte;
                    offset--;
                }
                break;
            case SAVEndianness.LITTLE_ENDIAN:
                for (int i = 0; i < 4; i++) {
                    cbyte = (byte)(value & 0xff);
                    value = value >>> 8;
                    bytes[offset] = cbyte;
                    offset++;
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    public static void doubleToBytes(
        byte[] bytes, 
        int offset,
        double value, 
        int endianness
    ) {
        long raw = Double.doubleToLongBits(value);
        byte cbyte = 0;
        
        switch(endianness) {
            case SAVEndianness.BIG_ENDIAN:
                offset += 7;
                for (int i = 7; i >= 0; i--) {
                    cbyte = (byte)(raw & 0xff);
                    raw = raw >>> 8;
                    bytes[offset] = cbyte;
                    offset--;
                }
                break;
            case SAVEndianness.LITTLE_ENDIAN:
                for (int i = 0; i < 8; i++) {
                    cbyte = (byte)(raw & 0xff);
                    raw = raw >>> 8;
                    bytes[offset] = cbyte;
                    offset++;
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
    }
    
    /*

    public static void doubleToBytes(byte[] bytes, int value, int endianness) {
    }
    */
    
    
} // class SAVEndianness

