//*******************************{begin:header}******************************//
//             spssio - https://github.com/jani-hautamaki/spssio             //
//***************************************************************************//
//
//      Java classes for reading and writing
//      SPSS/PSPP Portable and System files
//
//      Copyright (C) 2013-2016 Jani Hautamaki <jani.hautamaki@hotmail.com>
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

    /** Unexpected end-of-file. */
    public static final int E_EOF                      = 1;

    /** The data has invalid format */
    public static final int E_FORMAT                   = 2;

    /** Other node within the chain finished with an error. */
    public static final int E_OTHER                    = Integer.MAX_VALUE;

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
     * Set to true when the decompression is disabled.
     */
    private boolean passthrough;

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

        // By default decompression is enabled
        passthrough = false;

        // No receiver by default
        dataReceiver = null;

        // Reset state and error
        reset();

        // Set endianness, but do not update the sysmissBytes yet,
        // because the buffer and the value are not ready yet.
        this.endianness = SAVConstants.DEFAULT_ENDIANNESS;

        whitespacesBytes = new byte[8];
        sysmissBytes = new byte[8];

        updateWhitespacesBytes();
        setSysmiss(SAVConstants.DEFAULT_SYSMISS_VALUE);

        setBias(SAVConstants.DEFAULT_COMPRESSION_BIAS);
    }

    /**
     * Copy constructor
     */
    public SAVMatrixDecompressor(SAVMatrixDecompressor other) {
        this();

        // Copy bias
        setBias(other.getBias());

        // Copy endianness
        setEndianness(other.getEndianness());

        // Copy sysmiss (in raw format)
        setSysmissRaw(other.getSysmissRaw());

        // Copy passthrough
        setPassthrough(other.getPassthrough());
    }

    // CONFIGURATION
    //===============

    public void setBias(double bias) {
        this.bias = bias;
    }

    public double getBias() {
        return bias;
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

    public int getEndianness() {
        return endianness;
    }

    public void setSysmiss(double sysmiss) {
        this.sysmiss = sysmiss;
        this.sysmissRaw = Double.doubleToLongBits(this.sysmiss);

        updateSysmissBytes();
    }

    public double getSysmiss() {
        return sysmiss;
    }

    public void setSysmissRaw(long sysmissRaw) {
        this.sysmissRaw = sysmissRaw;
        this.sysmiss = Double.longBitsToDouble(this.sysmissRaw);

        updateSysmissBytes();
    }

    public long getSysmissRaw() {
        return sysmissRaw;
    }

    public void setDataReceiver(SAVMatrixParser dataReceiver) {
        this.dataReceiver = dataReceiver;
    }

    public void setPassthrough(boolean passthrough) {
        this.passthrough = passthrough;
    }

    public boolean getPassthrough() {
        return passthrough;
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
            int otherErrno = dataReceiver.consume(data);

            if (otherErrno > E_OK) {
               // Chained output stream (the data receiver) has finished
               // with an error state. The error is bubbled up in the stream,
               // by setting this output stream into an error state,
               // which indicates the failure of the subsequent stream.
               // Consequently, further state transition attempts
               // are ignored, and the error state set here is retained.
               error(E_OTHER, "Other data receiver/sink within "
                   +"the chain finished with an error.");
            }
        }
    }

    private void emitNumber(double value) {
        // Convert to bytes prior to emitting
        long raw = Double.doubleToLongBits(value);
        serializeRawDouble(raw);
        emitRaw(buffer);
    }

    private void emitSysmiss() {
        emitRaw(sysmissBytes);
    }

    private void emitWhitespaces() {
        emitRaw(whitespacesBytes);
    }

    public void reset() {
        // Reset state
        state = S_START;
        eps = false;

        // Reset error code
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

    public Object getFailedObject() {
        Object obj = null;
        if (errno > E_OK) {
            if (errno == E_OTHER) {
                obj = dataReceiver;
            } else {
                obj = this;
            }
        } else {
            // No error
        }
        return obj;
    }


    // Expects an 8-byte array each time
    public int consume(byte[] data) {
        // Validate input: the data should
        // either be null or have length == 8

        if ((data != null) && (data.length != 8)) {
            throw new RuntimeException(String.format(
                "Unexpected data array length: %d (should be 8, this is as strong indication of a programming error)",
                data.length));
        }

        // Short-circuit to emitRaw if passthrough is enabled
        if (passthrough) {
            emitRaw(data);
            if (data == null) {
                // EOF
                nextState(S_ACCEPT, false);
            }
            return errno;
        }

        do {
            // By default, the cycle consumes the input data
            eps = false;
            cycle(data);
        } while (eps == true);

        return errno;
    }

    private boolean error(int errno, String fmt, Object... args) {
        if ((state == S_ERROR) || (state == S_ACCEPT)) {
            // Cannot hide previous error with a new one.
            // Transitioning from a finishing state is not allowed either.
            return false;
        }

        state = S_ERROR;
        this.errno = errno;
        strerror = String.format(fmt, args);

        return true;
    }

    private boolean nextState(int nextState, boolean isNullTransition) {
        if ((state == S_ERROR) || (state == S_ACCEPT)) {
           // Cannot transit from finishing state.
           return false;
        }

        // State transition allowed.
        state = nextState;
        eps = isNullTransition;
        // If transitioning to accepting state, clear error
        if (state == S_ACCEPT) {
            errno = E_OK;
            strerror = null;
        }

        return true;
    }

    private void cycle(byte[] data) {
        switch(state) {
            case S_START:
                if (data != null) {
                    nextState(S_EXPECT_CBYTE_DATA, true);
                } else {
                    // immediate eof
                }
                break;

            case S_EXPECT_CBYTE_DATA:
                if (data != null) {
                    // Copy data to control
                    System.arraycopy(data, 0, control, 0, 8);
                    cbyteIndex = 0;
                    nextState(S_NEXT_CBYTE, true);
                } else {
                    // EOF is accepted at this state,
                    // but use the "expect" state to finish.
                    nextState(S_EXPECT_EOF, true);
                }
                break;

            case S_EXPECT_RAW_DATA:
                if (data != null) {
                    nextState(S_EMIT_RAW_DATA, true);
                } else {
                    // Unexpected eof
                    error(E_FORMAT, "Control byte indicated to "
                        +"expect raw data, but got EOF instead");
                }
                break;

            case S_NEXT_CBYTE:
                if (cbyteIndex == 8) {
                    // Move on to read next control bytes
                    nextState(S_EXPECT_CBYTE_DATA, false);
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
                nextState(S_NEXT_CBYTE, true);
                break;

            case S_EMIT_SYSMISS:
                emitSysmiss();
                nextState(S_NEXT_CBYTE, true);
                break;

            case S_EMIT_BIASED_NUMBER:
                // Convert to double, and unbias
                emitNumber((double)(cbyte) - bias);
                nextState(S_NEXT_CBYTE, true);
                break;

            case S_EMIT_WHITESPACES:
                emitWhitespaces();
                nextState(S_NEXT_CBYTE, true);
                break;

            case S_EXPECT_EOF:
                if (data == null) {
                    // EOF as expected. Emit and accept.
                    emitRaw(null);
                    nextState(S_ACCEPT, false);
                } else {
                    // Unexpected data
                    error(E_FORMAT, "Control byte indicated to "
                        +"expect EOF, but got more data instead");
                } // if-else
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
                nextState(S_NEXT_CBYTE, true);
                break;

            case SAVConstants.CBYTE_EOF:             // 252 (0xFC)
                // End of file
                nextState(S_EXPECT_EOF, false);
                break;

            case SAVConstants.CBYTE_RAW_DATA:        // 253 (0xFD)
                // string or double: raw data
                nextState(S_EXPECT_RAW_DATA, false);
                break;

            case SAVConstants.CBYTE_WHITESPACES:     // 254 (0xFE)
                // string: 8x whitespace
                nextState(S_EMIT_WHITESPACES, true);
                break;

            case SAVConstants.CBYTE_SYSMISS:         // 255 (0xFF)
                // double: sysmiss
                nextState(S_EMIT_SYSMISS, true);
                break;

            default:
                // double: code-bias
                nextState(S_EMIT_BIASED_NUMBER, true);
                break;
        } // switch()
    } // handleControlByte()

} // class

