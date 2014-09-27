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


package spssio.util;


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
public class DataEndianness {

    // PRE-DEFINED OBJECTS
    //=====================

    public static final DataEndianness BigEndian 
        = new DataEndianness(DataEndianness.BIG_ENDIAN);

    public static final DataEndianness LittleEndian 
        = new DataEndianness(DataEndianness.LITTLE_ENDIAN);

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

    // MEMBER VARIABLES
    //==================

    /** The current data endianness */
    private int endianness;

    // CONSTRUCTORS
    //==============

    /**
     * Endianness is left invalid
     */
    public DataEndianness() {
        endianness = INVALID;
    }

    /**
     * Endianness is left invalid
     */
    public DataEndianness(int endianness) {
        set(endianness);
    }

    // OTHER METHODS
    //===============

    public static boolean isValid(int endianness) {
        boolean rval = false;

        if (endianness == BIG_ENDIAN) {
            // Valid
            rval = true;
        } else if (endianness == LITTLE_ENDIAN) {
            // Valid
            rval = true;
        } else {
            // Invalid
        } // if-else

        return rval;
    }

    public String toString() {
        switch(endianness) {
            case DataEndianness.BIG_ENDIAN:
                return "Big-Endian";
            case DataEndianness.LITTLE_ENDIAN:
                return "Little-Endian";
            default:
                return "Invalid endianness";
        }
    }

    // FACTORY METHOD
    //================

    public static DataEndianness get(int endianness) {
        DataEndianness rval = null;
        switch(endianness) {
            case DataEndianness.BIG_ENDIAN:
                rval = BigEndian;
                break;
            case DataEndianness.LITTLE_ENDIAN:
                rval = LittleEndian;
                break;
            default:
                break;
        }
        return rval;
    }


    // GET/SET METHODS
    //=================

    public int get() {
        return endianness;
    }

    public void set(int endianness) {
        if (isValid(endianness) == false) {
            throw errorInvalidEndianness();
        }
        this.endianness = endianness;
    }

    public void unset() {
        this.endianness = INVALID;
    }

    // BYTES -> OBJECT
    //=================

    public int bytesToInteger(byte[] bytes, int offset) {
        int raw = 0;
        int cbyte = -1;

        // Construct in read order

        switch(endianness) {
            case DataEndianness.BIG_ENDIAN:
                for (int i = 0; i < 4; i++, offset++) {
                    // MSB first
                    cbyte = ((int)bytes[offset]) & 0xff;
                    raw = raw << 8;
                    raw = raw | cbyte;
                }
                break;
            case DataEndianness.LITTLE_ENDIAN:
                for (int i = 0; i < 4; i++, offset++) {
                    // LSB first
                    cbyte = ((int)bytes[offset]) & 0xff;
                    raw = raw | (cbyte << (i << 3));
                }
                break;
            default:
                throw errorInvalidEndianness();
        }

        return raw;
    }

    public short bytesToShort(byte[] bytes, int offset) {
        int raw = 0;
        int cbyte = -1;

        // Construct in read order

        switch(endianness) {
            case DataEndianness.BIG_ENDIAN:
                for (int i = 0; i < 2; i++, offset++) {
                    // MSB first
                    cbyte = ((int)bytes[offset]) & 0xff;
                    raw = raw << 8;
                    raw = raw | cbyte;
                }
                break;
            case DataEndianness.LITTLE_ENDIAN:
                for (int i = 0; i < 2; i++, offset++) {
                    // LSB first
                    cbyte = ((int)bytes[offset]) & 0xff;
                    raw = raw | (cbyte << (i << 3));
                }
                break;
            default:
                throw errorInvalidEndianness();
        }

        return (short)(raw);
    }

    public long bytesToLong(byte[] bytes, int offset) {
        long raw = 0;
        long cbyte = -1;

        // Construct in read order

        switch(endianness) {
            case DataEndianness.BIG_ENDIAN:
                for (int i = 0; i < 8; i++, offset++) {
                    // MSB first
                    cbyte = ((long)bytes[offset]) & 0xff;
                    raw = raw << 8; 
                    raw = raw | cbyte;
                }
                break;
            case DataEndianness.LITTLE_ENDIAN:
                for (int i = 0; i < 8; i++, offset++) {
                    // LSB first
                    cbyte = ((long)bytes[offset]) & 0xff;
                    raw = raw | (cbyte << (i << 3));
                }
                break;
            default:
                throw errorInvalidEndianness();
        }

        return raw;
    }



    public double bytesToDouble(byte[] bytes, int offset) {
        return Double.longBitsToDouble(
            bytesToLong(bytes, offset));
    }

    // OBJECT -> BYTES
    //=================

    public void integerToBytes(
        byte[] bytes, 
        int offset,
        int value
    ) {
        byte cbyte = 0;

        // Construct in writing order

        switch(endianness) {
            case DataEndianness.BIG_ENDIAN:
                for (int i = 3; i >= 0; i--, offset++) {
                    // MSB first
                    cbyte = (byte)(value >>> (i << 3)); // INT SHR
                    bytes[offset] = cbyte;
                }

                break;
            case DataEndianness.LITTLE_ENDIAN:
                for (int i = 0; i < 4; i++, offset++) {
                    // LSB first
                    cbyte = (byte)(value & 0xff);
                    value = value >>> 8; // INT SHR
                    bytes[offset] = cbyte;
                }
                break;
            default:
                throw errorInvalidEndianness();
        }
    }

    public void shortToBytes(
        byte[] bytes, 
        int offset,
        short svalue
    ) {
        byte cbyte = 0;
        int value = ((int) svalue) & 0xffff; // Use "int" data type internally

        // Construct in writing order

        switch(endianness) {

            case DataEndianness.BIG_ENDIAN:
                for (int i = 1; i >= 0; i--, offset++) {
                    // MSB first
                    cbyte = (byte)(value >>> (i << 3)); // INT SHR
                    bytes[offset] = cbyte;
                }

                break;
            case DataEndianness.LITTLE_ENDIAN:
                for (int i = 0; i < 2; i++, offset++) {
                    // LSB first
                    cbyte = (byte)(value & 0xff);
                    value = value >>> 8; // INT SHR
                    bytes[offset] = cbyte;
                }
                break;
            default:
                throw errorInvalidEndianness();
        }
    }

    public void longToBytes(byte[] bytes, int offset, long value) {
        byte cbyte = 0;

        // Construct in writing order

        switch(endianness) {

            case DataEndianness.BIG_ENDIAN:
                for (int i = 7; i >= 0; i--, offset++) {
                    // MSB first
                    cbyte = (byte)(value >>> (i << 3)); // LONG SHR
                    bytes[offset] = cbyte;
                }
                break;
            case DataEndianness.LITTLE_ENDIAN:
                for (int i = 0; i < 8; i++, offset++) {
                    // LSB first
                    cbyte = (byte)(value);
                    value = value >>> 8; // LONG SHR
                    bytes[offset] = cbyte;
                }
                break;
            default:
                throw errorInvalidEndianness();
        }
    }

    public void doubleToBytes(
        byte[] bytes, 
        int offset,
        double value
    ) {
        longToBytes(bytes, offset, Double.doubleToLongBits(value));
    }

    // ERROR HANDLING
    //=================

    protected static RuntimeException errorInvalidEndianness() {
        throw new RuntimeException("invalid endianness");
    }


    // TEST CODE
    //===========

    public static String bytesToString(byte[] bytes, int offset, int len) {
        StringBuilder sb = new StringBuilder(128);

        sb.append('[');
        for (int i = 0; i < len; i++, offset++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(String.format("%02x", ((int)bytes[offset]) & 0xff));
        }
        sb.append(']');

        return sb.toString();
    }

    public static void testInt(
        DataEndianness endianness, 
        byte[] buffer,
        int ivalue
    ) {
        System.out.printf("Input integer: %08x\n", ivalue);

        System.out.printf("endianness: %s\n", endianness);

        endianness.integerToBytes(buffer, 0, ivalue);
        System.out.printf("int->bytes: %s\n", bytesToString(buffer, 0, 4));
        ivalue = endianness.bytesToInteger(buffer, 0);
        System.out.printf("bytes->int: %08x\n", ivalue);
    }

    public static void testShort(
        DataEndianness endianness, 
        byte[] buffer,
        short svalue
    ) {
        System.out.printf("Input short: %04x\n", svalue);

        System.out.printf("endianness: %s\n", endianness);

        endianness.shortToBytes(buffer, 0, svalue);
        System.out.printf("short->bytes: %s\n", bytesToString(buffer, 0, 2));
        svalue = endianness.bytesToShort(buffer, 0);
        System.out.printf("bytes->short: %04x\n", svalue);
    }

    public static void testLong(
        DataEndianness endianness, 
        byte[] buffer,
        long lvalue
    ) {
        System.out.printf("Input long: %08x%08x\n", lvalue >>> 32, lvalue);
        System.out.printf("endianness: %s\n", endianness);

        endianness.longToBytes(buffer, 0, lvalue);
        System.out.printf("long->bytes: %s\n", bytesToString(buffer, 0, 8));
        lvalue = endianness.bytesToLong(buffer, 0);
        System.out.printf("bytes->long: %08x%08x\n", lvalue >>> 32, lvalue);
    }

    public static void testDouble(
        DataEndianness endianness, 
        byte[] buffer,
        double dvalue
    ) {
        System.out.printf("Input double: %g (%16x)\n", 
            dvalue, Double.doubleToLongBits(dvalue));

        System.out.printf("endianness: %s\n", endianness);

        endianness.doubleToBytes(buffer, 0, dvalue);
        System.out.printf("double->bytes: %s\n", bytesToString(buffer, 0, 8));
        dvalue = endianness.bytesToDouble(buffer, 0);
        System.out.printf("bytes->double: %g (%16x)\n",
            dvalue, Double.doubleToLongBits(dvalue));

    }

    public static void main(String[] args) {
        DataEndianness endianness = new DataEndianness();


        byte[] buffer = new byte[16];

        int ival = 0x12345678;
        endianness.set(DataEndianness.LITTLE_ENDIAN);
        testInt(endianness, buffer, ival);
        System.out.printf("--\n");
        endianness.set(DataEndianness.BIG_ENDIAN);
        testInt(endianness, buffer, ival);
        System.out.printf("--\n");

        short sval = 0x1234;
        endianness.set(DataEndianness.LITTLE_ENDIAN);
        testShort(endianness, buffer, sval);
        System.out.printf("--\n");
        endianness.set(DataEndianness.BIG_ENDIAN);
        testShort(endianness, buffer, sval);
        System.out.printf("--\n");


        long lval = 0x123456789ABCDEF0L;
        endianness.set(DataEndianness.LITTLE_ENDIAN);
        testLong(endianness, buffer, lval);
        System.out.printf("--\n");
        endianness.set(DataEndianness.BIG_ENDIAN);
        testLong(endianness, buffer, lval);
        System.out.printf("--\n");

        double dvalue = Double.longBitsToDouble(0x123456789ABCDEF0L);
        endianness.set(DataEndianness.LITTLE_ENDIAN);
        testDouble(endianness, buffer, dvalue);
        System.out.printf("--\n");
        endianness.set(DataEndianness.BIG_ENDIAN);
        testDouble(endianness, buffer, dvalue);
        System.out.printf("--\n");
    }


} // class DataEndianness

