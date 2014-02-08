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

package spssio.sav.input;

// spssio
import spssio.sav.SAVConstants;
import spssio.util.DataEndianness;


public class SAVMatrixDecompressor {
    
    // ERROR CODES
    //=============

    /** Decompression is unfinished, more input is expected. */
    public static final int E_UNFINISHED               = -1;
    
    // The following error codes indicate that the parser has finished.
    
    /** Decompression was finished succesfully, no errors occurred. */
    public static final int E_OK                       = 0;
    
    /** The data has invalid format */
    public static final int E_FORMAT                   = 1;
    
    // STATES
    //========

    private static final int S_ERROR                    = -1;
    private static final int S_START                    = 0;
    private static final int S_EXPECT_CBYTE_DATA        = 1;
    private static final int S_EXPECT_RAW_DATA          = 2;
    private static final int S_NEXT_CBYTE               = 3;
    private static final int S_EMIT_RAW_DATA            = 4;
    private static final int S_EMIT_SYSMISS             = 5;
    private static final int S_EMIT_BIASED_NUMBER       = 6;
    private static final int S_EMIT_WHITESPACES         = 7;
    private static final int S_EXPECT_EOF               = 8;
    private static final int S_ACCEPT                   = 9;
    
    // MEMBER VARIABLES: STATE & ERROR
    //=================================
    
    /**
     * Current state
     */
    private int state;
    
    /**
     * Determines whether the current cycle consumes the input.
     */
    private boolean eps;
    
    /**
     * Error number code for the last operation
     */
    private int errno;
    
    /**
     * A human-readable error message related to the error code.
     * If no error, this is set to null
     */
    private String strerror;

    // MEMBER VARIABLES
    //==================
    
    /**
     * Latest control data
     */
    private byte[] control;
    
    /**
     * Buffer for emitting generated data objects
     */
    private byte[] buffer;
    
    /**
     * Current control byte index
      */
    private int cbyteIndex;
    
    /**
     * Current control byte.
     */
    private int cbyte;
    
    // MEMBER VARIABLES: SETTINGS
    //============================
    
    /**
     * Configured compression bias
     */ 
    private double bias;
    
    /**
     * Configured SYSMISS value
     */
    private double sysmiss;

    /**
     * Configured endianness of the double values.
     */
    private int endianness;
    

    // Derived data

    /**
     * Configured SYSMISS value as long bits
     */
    private long sysmissRaw;
    
    /**
     * Pre-calculated buffer of 8 whitespaces
     */
    private byte[] whitespacesBytes;
    
    /**
     * Pre-calcualted buffer for SYSMISS value
     * corresponding to the endianness setting.
     */
    private byte[] sysmissBytes;

    // MEMBER VARIABLES: OTHERS
    //==========================

    /**
     * Receiver of the uncompressed data, 
     * or {@code null} if no receiver.
     */
    private SAVMatrixParser dataReceiver;

    
    
    // CONSTRUCTORS
    //==============
    
    public SAVMatrixDecompressor() {
        // Allocate buffers
        control = new byte[8];
        buffer = new byte[8];
        
        // No receiver by default
        dataReceiver = null;
        
        // Reset state and error
        reset();
        
        // Set endianness, but do not update the sysmissBytes yet,
        // because the buffer and the value are not ready yet.
        this.endianness = DataEndianness.LITTLE_ENDIAN;
        
        whitespacesBytes = new byte[8];
        sysmissBytes = new byte[8];
        
        updateWhitespacesBytes();
        setSysmiss(-Double.MAX_VALUE);
        
        setBias(100.0);
    }

    // CONFIGURATION
    //===============
    
    public void setBias(double bias) {
        this.bias = bias;
    }
    
    public void setEndianness(int endianness) {
        // Validate argument
        if (DataEndianness.isValid(endianness) == false) {
            throw new IllegalArgumentException(String.format(
                "Illegal endianness: %d", endianness));
        }
        
        // Update endianness setting
        this.endianness = endianness;
        
        // Update values depending on the endianness
        updateSysmissBytes();
    }
    
    public void setSysmiss(double sysmiss) {
        this.sysmiss = sysmiss;
        this.sysmissRaw = Double.doubleToLongBits(this.sysmiss);
        
        updateSysmissBytes();
    }
    
    public void setSysmissRaw(long sysmissRaw) {
        this.sysmissRaw = sysmissRaw;
        this.sysmiss = Double.longBitsToDouble(this.sysmissRaw);
        
        updateSysmissBytes();
    }
    
    public void setDataReceiver(SAVMatrixParser dataReceiver) {
        this.dataReceiver = dataReceiver;
    }
    
    // OTHER METHODS
    //===============

    protected void updateSysmissBytes() {
        // Serialize sysmissRaw according to the opposite endianness
        serializeRawDouble(sysmissRaw);
        // Copy the buffer contents
        System.arraycopy(buffer, 0, sysmissBytes, 0, 8);
    }
    
    protected void updateWhitespacesBytes() {
        // Serialize whitespaces
        serializeWhitespaces();
        // Copy the buffer contents
        System.arraycopy(buffer, 0, whitespacesBytes, 0, 8);
    }

    private static long flip8(long value) {
        long rval = 0;
        for (int i = 0; i < 8; i++) {
            rval = (rval << 8) | (value & 0xff);
            value = value >>> 8;
        }
        
        /*
        rval = ((value & 0x00000000000000ffL) << 56)
             | ((value & 0x000000000000ff00L) << 32)
             | ((value & 0x0000000000ff0000L) << 16)
             | ((value & 0x00000000ff000000L) << 8)
             | ((value & 0x000000ff00000000L) >>> 8)
             | ((value & 0x0000ff0000000000L) >>> 16)
             | ((value & 0x00ff000000000000L) >>> 32)
             | ((value & 0xff00000000000000L) >>> 56)
        
        int hi = value >> 32;
        int lo = value & 0xffffffffL;
        return flip4(lo) << 32 | flip4(hi);
        */
        
        return rval;
    }
    
    /**
     * Serializes the specified raw double into the buffer
     * opposite to the specified endianness
     */
    private void serializeRawDouble(long raw) {
        if (endianness == DataEndianness.LITTLE_ENDIAN) {
            // Underlying parser expects Little-Endian
            for (int i = 0; i < 8; i++) {
                buffer[i] = (byte)(raw & 0xff);
                raw = raw >>> 8;
            }
        } else if (endianness == DataEndianness.BIG_ENDIAN) {
            // Underlying parser expects Big-Endian
            for (int i = 7; i >= 0; i--) {
                buffer[i] = (byte)(raw & 0xff);
                raw = raw >>> 8;
            }
        } else {
            // unconfigured
            throw new RuntimeException("Endianness is unconfigured");
        } // if-else: big-endian / little-endian
    }
    
    private void serializeWhitespaces() {
        for (int i = 0; i < 8; i++) {
            buffer[i] = ' ';
        }
    }

    private void emitRaw(byte[] data) {
        // Send to the parser
        if (dataReceiver != null) {
            // Send raw data to the data receiver 
            
            int recvErrno = dataReceiver.consume(data);
            
            // TODO: Error checking
        }
    }
    
    private void emitNumber(double value) {
        // Convert to bytes
        long raw = Double.doubleToLongBits(value);
        serializeRawDouble(raw);
        emitRaw(buffer);
    }

    private void emitSysmiss() {
        /*
        serializeRawDouble(sysmissRaw);
        emitRaw(buffer);
        */
        emitRaw(sysmissBytes);
    }
    
    private void emitWhitespaces() {
        /*
        serializeWhitespaces();
        emitRaw(buffer);
        */
        emitRaw(whitespacesBytes);
    }

    
    public void reset() {
        // Reset state
        state = S_START;
        eps = false;
        
        // Reset error state
        errno = E_UNFINISHED;
        strerror = "Decompression is unfinished; more input is expected";
        
        // Check configuration
        
        // Reset the underlying parser too, if any
        // TBC: Should the resetting of the data receiver
        // be encapsulated into a "emitReset()" method?
        if (dataReceiver != null) {
            dataReceiver.reset();
        }
    }
    
    public int errno() {
        return errno;
    }
    
    public String strerror() {
        return strerror;
    }
    
    /*
    private void error(int errno, String fmt, Object... args) {
        this.state = S_ERROR;
        this.errno = errno;
        this.strerror = String.format(fmt, args);
    }
    
    private void accept() {
        this.state = S_ACCEPT;
        this.errno = E_OK;
        this.strerror = null;
    }
    */
    
    // Expects an 8-byte array each time
    public int consume(byte[] data) {
        // Validate input: the data should 
        // either be null or have length == 8
        
        if ((data != null) && (data.length != 8)) {
            throw new RuntimeException(String.format(
                "Unexpected data array length: %d (should be 8, this is as strong indication of a programming error)",
                data.length));
        }
        
        do {
            // By default, the cycle consumes the input data
            eps = false;
            cycle(data);
        } while (eps == true);
        
        return errno;
    }
    
    private void cycle(byte[] data) {
        switch(state) {
            case S_START:
                if (data != null) {
                    state = S_EXPECT_CBYTE_DATA;
                    eps = true;
                } else {
                    // immediate eof
                }
                break;
                
            case S_EXPECT_CBYTE_DATA:
                if (data != null) {
                    // Copy data to control
                    System.arraycopy(data, 0, control, 0, 8);
                    cbyteIndex = 0;
                    state = S_NEXT_CBYTE;
                    eps = true;
                } else {
                    // EOF is accepted at this state,
                    // but use the "expect" state to finish.
                    state = S_EXPECT_EOF;
                    eps = true;
                }
                break;
            
            case S_EXPECT_RAW_DATA:
                if (data != null) {
                    state = S_EMIT_RAW_DATA;
                    eps = true;
                } else {
                    // Unexpected eof
                    state = S_ERROR;
                    errno = E_FORMAT;
                    strerror = String.format("Control byte indicated to expect raw data, but got EOF instead");
                }
                break;
            
            case S_NEXT_CBYTE:
                if (cbyteIndex == 8) {
                    // Move on to read next control bytes
                    state = S_EXPECT_CBYTE_DATA;
                } else {
                    // Otherwise look up the next control byte
                    cbyte = ((int) control[cbyteIndex]) & 0xff;
                    
                    // Increase current control byte index
                    cbyteIndex++;
                    
                    // State transition according to the control byte.
                    handleControlByte();
                }
                break;
            
            case S_EMIT_RAW_DATA:
                // Pass-through for the data
                emitRaw(data);
                state = S_NEXT_CBYTE;
                eps = true;
                break;
                
            case S_EMIT_SYSMISS:
                emitSysmiss();
                state = S_NEXT_CBYTE;
                eps = true;
                break;
            
            case S_EMIT_BIASED_NUMBER:
                // Convert to double, and unbias
                emitNumber((double)(cbyte) - bias);
                state = S_NEXT_CBYTE;
                eps = true;
                break;
            
            case S_EMIT_WHITESPACES:
                emitWhitespaces();
                state = S_NEXT_CBYTE;
                eps = true;
                break;
            
            case S_EXPECT_EOF:
                if (data == null) {
                    // EOF as expected. Emit and accept.
                    emitRaw(null);
                    state = S_ACCEPT;
                    errno = E_OK;
                    strerror = null;
                } else {
                    // Unexpected data
                    state = S_ERROR;
                    errno = E_FORMAT;
                    strerror = String.format("Control byte indicated to expect EOF, but got more data instead");
                } // if-else
                // No further data expected.
                // TODO: Error if lands here?
                break;
            
            case S_ACCEPT:
                // Stay here
                break;
            
            case S_ERROR:
                // Stay here
                break;
                
            default:
                throw new RuntimeException(String.format(
                    "Unhandled state: %d (programming error)", state));
        } // switch
    } // cycle()
    
    private void handleControlByte() {
        switch(cbyte) {
            case SAVConstants.CBYTE_NOP:             // 0 (0x00)
                // NOP operation
                state = S_NEXT_CBYTE;
                eps = true;
                break;
            
            case SAVConstants.CBYTE_EOF:             // 252 (0xFC)
                // End of file
                state = S_EXPECT_EOF;
                break;
            
            case SAVConstants.CBYTE_RAW_DATA:        // 253 (0xFD)
                // string or double: raw data
                state = S_EXPECT_RAW_DATA;
                break;
            
            case SAVConstants.CBYTE_WHITESPACES:     // 254 (0xFE)
                // string: 8x whitespace
                state = S_EMIT_WHITESPACES;
                eps = true;
                break;
            
            case SAVConstants.CBYTE_SYSMISS:         // 255 (0xFF)
                // double: sysmiss
                state = S_EMIT_SYSMISS;
                eps = true;
                break;
            
            default:
                // double: code-bias
                state = S_EMIT_BIASED_NUMBER;
                eps = true;
                break;
        } // switch()
    } // handleControlByte()
    
} // class

