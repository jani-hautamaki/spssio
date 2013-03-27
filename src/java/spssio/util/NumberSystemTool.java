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

package spssio.util;

// for locale-independent formatting
import java.util.Locale;

// for stdin/stdout handling
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

// for accuracy
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

// spssio utils
import spssio.util.NumberSystem;
import spssio.util.NumberParser;
import spssio.util.NumberFormatter;

/**
 * Test application for the number system related classes.
 * These classes are {@code NumberSystem}, {@code NumberParser}
 * and {@code NumberPrinter}.
 */
public class NumberSystemTool {
    
    // CONSTANTS
    //===========
    
    private static final int INPUT_NUMPARSER            = 0;
    private static final int INPUT_JAVA_BUILTIN         = 1;
    private static final int INPUT_HEX                  = 2;
    private static final int INPUT_RESHAPE              = 3;
    
    private static final int OUTPUT_NUMFORMATTER        = 0;
    private static final int OUTPUT_JAVA_FORMAT         = 1;
    private static final int OUTPUT_JAVA_TOSTRING       = 2;
    
    // MEMBER VARIABLES
    //==================
    
    private boolean quit = false;
    private NumberSystem sysin = null;
    private NumberSystem sysout = null;
    private NumberParser parser = null;
    private NumberFormatter formatter = null;
    
    
    private int inputMode  = INPUT_NUMPARSER;
    private int outputMode = OUTPUT_NUMFORMATTER;
    
    private String inputMathContextName = null;
    private String outputMathContextName = null;
    
    private String outputFormat = null;
    private boolean show_double_bits = false;
    private int lastPromptLength = 0;
    
    
    // CONSTRUCTORS
    //==============
    
    public NumberSystemTool() {
        sysin = new NumberSystem();
        sysout = new NumberSystem();
        
        parser = new NumberParser();
        formatter = new NumberFormatter();
        
        // Associate number system with the parser and with the formatter.
        parser.setNumberSystem(sysin);
        formatter.setNumberSystem(sysout);
    } // ctor
    
    // OTHER METHODS
    //===============
    
    public void run() {
        BufferedReader br = null;
        
        try {
            br = new BufferedReader(new InputStreamReader(System.in));
        } catch(Exception ex) {
            error("Cannot instantiate InputStreamReader for stdin");
        } // try-catch

        // Initialize number system to base 10.
        sysin.setBase(10);
        sysout.setBase(10);
        formatter.setDefaultPrecision();
        outputFormat = "%.18g";
        
        // Show number system details at the beginning.
        printLimits();
        
        quit = false;
        do {
            String line = null;
            String prompt = null;
            
            // Display prompt
            if (inputMode == INPUT_NUMPARSER) {
                if (inputMathContextName != null) {
                    prompt = String.format("b=%d:tool:m=%s >> ", 
                        sysin.getBase(), inputMathContextName);
                } else {
                    prompt = String.format("b=%d:tool >> ", 
                        sysin.getBase());
                } // if-else: math context set
            } else if (inputMode == INPUT_JAVA_BUILTIN) {
                prompt = String.format("b=10:java >> ");
            } else if (inputMode == INPUT_HEX) {
                prompt = String.format("b=16:hex >> ");
            } else if (inputMode == INPUT_RESHAPE) {
                if (outputMathContextName != null) {
                    prompt = String.format("b=%d:reshape=%d:m=%s >> ", 
                        sysout.getBase(), 
                        formatter.getPrecision(),
                        outputMathContextName
                    ); // format()
                } else {
                    prompt = String.format("b=%d:reshape=%d >> ", 
                        sysout.getBase(),
                        formatter.getPrecision()
                    ); // format()
                } // if-else: math context set
            } else {
                error("Unexpected input mode (this is a bug)");
            }
            
            try {
                // Output command prompt
                System.out.printf("\n");
                System.out.printf(prompt);
                lastPromptLength = prompt.length();
                // Read command
                line = br.readLine();
            } catch(IOException ex) {
                error("BufferedReader.readLine() raised an exception");
            } // try-catch
            
            // split into arguments
            String[] args = line.split(" ");
            try {
                parseCommand(args);
            } catch(Exception ex) {
                System.out.printf("Error: %s\n", ex.getMessage());
            } // try-catch
            
        } while (!quit);
        
    } // run()
    
    private void parseCommand(String[] args) {
        String carg = args[0];
        
        if (carg.equals("\\q")) {
            expectArgs(args, 1);
            System.out.printf("Exiting\n");
            quit = true;
        }
        else if (carg.equals("\\h")) {
            expectArgs(args, 1);
            printHelp();
        }
        else if (carg.equals("\\l")) {
            expectArgs(args, 1);
            printLimits();
        }
        else if (carg.equals("\\base")) {
            if (args.length == 1) {
                printBase();
            } else if (args.length == 2) {
                doSetBase(null, args[1]);
            } else if (args.length == 3) {
                doSetBase(args[1], args[2]);
            } else {
                error("Too many arguments");
            }
        }
        else if (carg.equals("\\precision")) {
            if (args.length == 1) {
                printPrecision();
            } else if (args.length == 2) {
                doSetPrecision(args[1]);
            } else {
                error("Too many arguments");
            }
        }
        else if (carg.equals("\\in")) {
            expectArgs(args, 2);
            doSetInputMode(args[1]);
        }
        else if (carg.equals("\\out")) {
            expectArgs(args, 2);
            doSetOutputMode(args[1]);
        }
        else if (carg.equals("\\format")) {
            doSetOutputFormat(args);
        }
        else if (carg.equals("\\context")) {
            doSetMathContext(args);
        }
        else if (carg.equals("\\bits")) {
            expectArgs(args, 1);
            toggleShowDoubleBits();
        }
        else if (carg.equals("\\trig")) {
            // Shortcut to trigesimals
            expectArgs(args, 1);
            doSetBase(null, "30");
        }
        else if (carg.equals("\\dec")) {
            // Shortcut to decimals
            expectArgs(args, 1);
            doSetBase(null, "10");
        }
        else if (carg.equals("\\dec2trig")) {
            sysin.setBase(10);
            sysout.setBase(30);
            inputMode = INPUT_JAVA_BUILTIN;
            outputMode = OUTPUT_NUMFORMATTER;
            printBase();
        }
        else if (carg.equals("\\trig2dec")) {
            sysin.setBase(30);
            sysout.setBase(10);
            inputMode = INPUT_NUMPARSER;
            outputMode = OUTPUT_JAVA_TOSTRING;
            printBase();
        }
        else if (carg.equals("\\dec2hex")) {
            sysin.setBase(10);
            sysout.setBase(16);
            inputMode = INPUT_JAVA_BUILTIN;
            outputMode = OUTPUT_NUMFORMATTER;
            printBase();
        }
        else if (carg.equals("\\hex2dec")) {
            sysin.setBase(16);
            sysout.setBase(10);
            inputMode = INPUT_NUMPARSER;
            outputMode = OUTPUT_JAVA_TOSTRING;
            printBase();
        }
        else if (args.length > 1) {
            error("Syntax error");
        } 
        else {
            // Otherwise assume it is a number
            parseNumber(carg);
        } // if-else
    } // parse()
    
    private void printHelp() {
        System.out.printf("Commands:\n");
        System.out.printf("\n");
        System.out.printf("\\l                  Show number system limits and details\n");
        System.out.printf("\\base [int]         Get/set number system radix\n");
        System.out.printf("\\base <sys> <int>   get radix, sys is either \"in\" or \"out\"\n");
        System.out.printf("\\precision [int]    Get/set output precision\n");
        System.out.printf("\\trig               Shortcut for trigesimals, ie. \\base 30\n");
        System.out.printf("\\dec                Shortcut for decimals, ie. \\base 10\n");
        System.out.printf("\\bits               Toggle bit-level value display\n");
        System.out.printf("\\q                  quit\n");

        System.out.printf("Shortcuts:\n");
        System.out.printf("\\dec2trig           Decimals to trigesimals\n");
        System.out.printf("\\trig2dec           Trigesimals to decimals\n");
        System.out.printf("\\dec2hex            Decimals to hexadecimals\n");
        System.out.printf("\\hex2dec            Hexadecimals to decimals\n");
        System.out.printf("\n");
        
        System.out.printf("Input control:\n");
        System.out.printf("\\in java            Use Double.parseDouble for input\n");
        System.out.printf("\\in tool            Use NumberParser.parseDouble for input\n");
        System.out.printf("\\in hex             Use Double.longBitsToDouble for hex input\n");
        System.out.printf("\\in reshape         Switch into reformat/reshape mode\n");
        System.out.printf("\n");
        System.out.printf("Output control\n");
        System.out.printf("\\out java           Use Double.toString for output\n");
        System.out.printf("\\out tool           Use NumberFormatter.formatDouble for output\n");
        System.out.printf("\\out string         Use String.format for output\n");
        System.out.printf("\\format [fmt]       Get/et format string for String.format\n");
        
        System.out.printf("MathContext control for input and output:\n");
        System.out.printf("\\context            Display current MathContext for input and output\n");
        System.out.printf("\\context <x>        Set MathContext to DECIMAL<x>. If n==0, unset\n");
        System.out.printf("\\context <n> <rm>   Set MathContext to precision=<n>, rounding=<rm>\n");
        System.out.printf("\n");
        
        System.out.printf("As above, solely for input or output MathContext only:\n");
        System.out.printf("\\context [in|out] <x>\n");
        System.out.printf("\\context [in|out] <n> <rm>\n");
        System.out.printf("\n");
        
        System.out.printf("\n");
        System.out.printf("Every other single argument input is considered a number!\n");
        System.out.printf("\n");
    } // printHelp()
    
    private void printLimits() {
        System.out.printf("Input number system details:\n");
        System.out.printf("Base:       %d\n", sysin.getBase());
        System.out.printf("Digits:     \"%s\"\n", sysin.getDigits());
        System.out.printf("\n");
        System.out.printf("Plus char:  \'%c\'\n", sysin.getPlusChar());
        System.out.printf("Minus char: \'%c\'\n", sysin.getMinusChar());
        System.out.printf("Point char: \'%c\'\n", sysin.getPointChar());
        System.out.printf("\n");
        System.out.printf("Max double / base: %.18g\n", sysin.getMaxDouble());
        System.out.printf("Min double * base: %.18g\n", sysin.getMinDouble());
        System.out.printf("\n");
        System.out.printf("Java double data type details:\n");
        System.out.printf("Double.MAX:        %.18g\n", Double.MAX_VALUE);
        System.out.printf("Double.MIN:        %.18g\n", Double.MIN_VALUE);
    } // printLimits()
    
    private void printDoubleBits(double d) {
        long bits = Double.doubleToLongBits(d);
        
        long neg =      bits & 0x8000000000000000L;
        long exponent = bits & 0x7ff0000000000000L;
        long mantissa = (bits & 0x000fffffffffffffL) | 0x0010000000000000L; // normalized

        exponent = (exponent >> 52) - 1023; // shift and unbiasing
        double frac = ((double) mantissa) * Math.pow(2.0, -52);

        System.out.printf("double:     %.18g\n", d);
        System.out.printf("toLongBits: %s (normal)\n", Long.toHexString(bits));
        System.out.printf("toLongBits: %s (raw)\n", Long.toHexString(Double.doubleToRawLongBits(d)));
        System.out.printf("sign:       %s\n", neg != 0 ? "-" : "+");
        System.out.printf("exponent:   2**%d = %.0f\n", exponent, Math.pow(2.0, exponent));
        System.out.printf("mantissa:   %s\n", Long.toHexString(mantissa));
        System.out.printf("frac:       %.16g\n", frac);
    } // printDoubleBits()

    private void printBase() {
        System.out.printf("Input base:       %d\n", sysin.getBase());
        System.out.printf("Output base:      %d\n", sysout.getBase());
        System.out.printf("Output precision: %d\n", formatter.getPrecision());
    }
    
    private void printPrecision() {
        System.out.printf("Output precision: %d\n", formatter.getPrecision());
    }
    
    private void printMathContext() {
        MathContext mc = null;
        
        mc = parser.getMathContext();
        System.out.printf("Input:\n");
        if (mc != null) {
            System.out.printf("   type:           BigDecimal\n");
            System.out.printf("   precision:      %d\n", mc.getPrecision());
            System.out.printf("   rounding mode:  %s\n", mc.getRoundingMode());
        } else {
            System.out.printf("   type:           double\n");
        }
        
        mc = formatter.getMathContext();
        System.out.printf("Output:\n");
        if (mc != null) {
            System.out.printf("   type:           BigDecimal\n");
            System.out.printf("   precision:      %d\n", mc.getPrecision());
            System.out.printf("   rounding mode:  %s\n", mc.getRoundingMode());
        } else {
            System.out.printf("   type:           double\n");
        }
    }
    
    private void printOutputFormat() {
        System.out.printf("Output format: %s\n", outputFormat);
    }
    
    
    private void outputIndent() {
        for (int i = 0; i < lastPromptLength; i++) {
            System.out.printf(" ");
        }
    }
    
    private void output(String fmt, Object... args) {
        outputIndent();
        System.out.printf(fmt, args);
    }
    
    private void parseNumber(String arg) {
        try {
            // result value is stored here
            double value = 0.0;
            
            // The parse result defaults to invalid
            boolean valid = false;
            
            if (inputMode == INPUT_NUMPARSER) {
                // Parse using NumberParser
                value = parser.parseDouble(arg);
                
                // Pick the error code and inspect error status
                int errno = parser.errno();
                if (errno != NumberParser.E_OK) {
                    // Error management
                    System.out.printf("Parse error: %s\n", parser.strerror());
                    if (errno == NumberParser.E_OVERFLOW) {
                        if (parser.lastsign() < 0) {
                            System.out.printf("A value saturated to MINIMUM could be used instead\n");
                        } else {
                            System.out.printf("A value saturated to MAXIMUM could be used instead\n");
                        }
                    } else if (errno == NumberParser.E_UNDERFLOW) {
                        System.out.printf("Zero could be used instead\n");
                    } else {
                        System.out.printf("The value should be marked missing\n");
                    }
                } else {
                    // Parse success. Show results
                    valid = true;
                } // if-else
            } else if (inputMode == INPUT_JAVA_BUILTIN) {
                // Use Java's built-in double parser.
                value = Double.parseDouble(arg);
                // Parse success (even though the result might be infinite)
                valid = true;
            } else if (inputMode == INPUT_HEX) {
                value = parseDoubleHex(arg);
                valid = true;
            } else if (inputMode == INPUT_RESHAPE) {
                int[] buffer = formatter.getBuffer();
                int len = arg.length();
                for (int i = 0; i < len; i++) {
                    buffer[i] = arg.charAt(i);
                }
                len = formatter.reformat(
                    buffer, len, formatter.getPrecision());
                output("%s", new String(buffer, 0, len));
                System.out.printf("   (reformat)\n");
                return;
            } else {
                error("Unexpected input mode (this is a bug)");
            }
            
            if (valid == false) {
                return;
            }
            
            if (outputMode == OUTPUT_NUMFORMATTER) {
                formatter.formatDouble(value);
                if (outputMathContextName != null) {
                    output("%s", formatter.getString());
                    System.out.printf("   (b=%d:tool:m=%s)\n", 
                        sysout.getBase(),
                        outputMathContextName
                    ); // printf()
                } else {
                    output("%s", formatter.getString());
                    System.out.printf("   (b=%d:tool)\n", 
                        sysout.getBase());
                }
            } else if (outputMode == OUTPUT_JAVA_FORMAT) {
                output("%s", String.format(
                    Locale.ROOT, outputFormat, value));
                System.out.printf("   (%s)\n", outputFormat);
            } else if (outputMode == OUTPUT_JAVA_TOSTRING) {
                output(Double.toString(value));
                System.out.printf("   (Double.toString)\n");
            } else {
                error("Unexpected output mode (this is a bug)");
            }

            if (show_double_bits) {
                printDoubleBits(value);
            } 
            
        }
        catch(NumberFormatException ex) {
            System.out.printf("ERROR: NumberFormatException: %s\n, ex.getMessage()");
        } 
        catch(Exception ex) {
            System.out.printf("Parse failed due to exception\n");
            ex.printStackTrace();
        } // try-catch
    } // parseNumber()
    
    private void doSetBase(String arg1, String arg2) {
        int value = parseInt(arg2);

        if (value < 1) {
            error("The base must be greater than zero");
        }
        
        if ((arg1 == null) || arg1.equals("both")) {
            // Set input number system base
            sysin.setBase(value);
            
            // Set output number system base, and update precision
            sysout.setBase(value);
            formatter.setDefaultPrecision();
            
            System.out.printf("Input and output base set\n");
        } else if (arg1.equals("in")) {
            sysin.setBase(value);
            System.out.printf("Input base set\n");
        } else if (arg1.equals("out")) {
            sysout.setBase(value);
            formatter.setDefaultPrecision();
            System.out.printf("Output base set\n");
        } else {
            error("Expected \"in\", \"out\" or \"both\", but found: %s", arg1);
        } // if-else
        
        printBase();
    } // doSetBase()

    private void doSetPrecision(String arg) {
        int value = parseInt(arg);
        formatter.setPrecision(value);
        printPrecision();
    }

    private void doSetInputMode(String arg) {
        if (arg.equals("tool")) {
            inputMode = INPUT_NUMPARSER;
        } 
        else if (arg.equals("java")) {
            inputMode = INPUT_JAVA_BUILTIN;
        }
        else if (arg.equals("hex")) {
            inputMode = INPUT_HEX;
        }
        else if (arg.equals("reshape")) {
            inputMode = INPUT_RESHAPE;
        }
        else {
            error("Unrecognized input mode: %s", arg);
        }
        System.out.printf("Input mode set.\n");
    } // doSetInputMode

    private void doSetOutputMode(String arg) {
        if (arg.equals("tool")) {
            outputMode = OUTPUT_NUMFORMATTER;
        } 
        else if (arg.equals("java")) {
            outputMode = OUTPUT_JAVA_TOSTRING;
        }
        else if (arg.equals("string")) {
            outputMode = OUTPUT_JAVA_FORMAT;
        }
        else {
            error("Unrecognized output mode: %s", arg);
        }
        System.out.printf("Output mode set.\n");
    } // doSetInputMode

    private void doSetOutputFormat(String[] args) {
        if (args.length == 1) {
            printOutputFormat();
            return;
        }
        
        String fmt = args[1];
        
        try {
            String.format(fmt, 0.0);
        } catch(Exception ex) {
            System.out.printf("Invalid format string\n");
            return;
        }
        outputFormat = fmt;
        printOutputFormat();
    }
    
    private void doSetMathContext(String[] args) {
        Tuple<String, MathContext> context = null;
        
        // null: both, otherwise either "in" or "out"
        String target = null;
        
        if (args.length == 1) {
            printMathContext();
            return;
        } else if (args.length == 2) {
            // assume: context <x>
            context = parseMathContext(args[1]);
        } else if (args.length == 3) {
            // two possibilities:
            // context <n> <rm>
            // context [in/out] <x>
            if (args[1].equals("in") || args[1].equals("out")) {
                target = args[1];
                context = parseMathContext(args[2]);
            } else {
                context = parseMathContext(args[1], args[2]);
            }
        } else if (args.length == 4) {
            // assume: context [in/out] <n> <rm>
            target = args[1];
            context = parseMathContext(args[2], args[3]);
        } else {
            error("Syntax error");
        }
        
        if (target == null) {
            // set both
            parser.setMathContext(context.b);
            formatter.setMathContext(context.b);
            
            inputMathContextName = context.a;
            outputMathContextName = context.a;
            System.out.printf("Input and output MathContext set\n");
        } 
        else if (target.equals("in")) {
            parser.setMathContext(context.b);
            inputMathContextName = context.a;
            System.out.printf("Input MathContext set\n");
        }
        else if (target.equals("out")) {
            formatter.setMathContext(context.b);
            outputMathContextName = context.a;
            System.out.printf("Output MathContext set\n");
        } else {
            error("Unexpected target: %s (this is a bug)", target);
        }
        
        printMathContext();
        
    } // doSetMathContext()
    
    
    private int parseInt(String s) {
        int value = 0;
        try {
            value = Integer.parseInt(s);
        } catch(Exception ex) {
            error("Cannot parse integer: \"%s\"", s);
        } // try-catch
        return value;
    }
    
    private static double parseDoubleHex(String s) {
        int len = s.length();
        if (len != 16) {
            throw new RuntimeException(
                "Input hex string length must be exaclty 16");
        }
        
        long val = 0;
        
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            // convert c from hex char into an int
            long nibble = 0;
            if (('0' <= c) && (c <= '9')) {
                nibble = c - '0';
            } else if (('a' <= c) && (c <= 'f')) {
                nibble = (c - 'a') + 10;
            } else if (('A' <= c) && (c <= 'F')) {
                nibble = (c - 'A') + 10;
            } else {
                throw new RuntimeException(String.format(
                    "Position %d contains an invalid hex char: \'%c\'", 
                    i+1, c));
            } // if-else
            
            val = val | (nibble << ((15-i)*4));
            //val = val | (nibble << (i*4));
        } // for
        
        return Double.longBitsToDouble(val);
    } // parseHexdouble()
    

    // Helper class
    private static class Tuple<S, T> {
        public S a;
        public T b;
        
        public Tuple(S a, T b) {
            this.a = a;
            this.b = b;
        }
    } // class Tuple

    private Tuple<String, MathContext> parseMathContext(
        String arg 
    ) {
        Tuple<String, MathContext> rval 
            = new Tuple<String, MathContext>(null, null);
        
        if (arg.equals("128")) {
            rval.a = "128";
            rval.b = MathContext.DECIMAL128;
        }
        else if (arg.equals("64")) {
            rval.a = "64";
            rval.b = MathContext.DECIMAL64;
        }
        else if (arg.equals("32")) {
            rval.a = "32";
            rval.b = MathContext.DECIMAL32;
        }
        else if (arg.equals("0")) {
            rval.a = null;
            rval.b = null;
        }
        else {
            error("Unrecognized MathContext: \"%s\"", arg);
        }
        
        return rval;
    } // parseMathContext()
    
    private Tuple<String, MathContext> parseMathContext(
        String arg1,
        String arg2
    ) {
        Tuple<String, MathContext> rval 
            = new Tuple<String, MathContext>(null, null);
        
        int digits = 0;
        try {
            digits = Integer.parseInt(arg1);
        } catch(Exception ex) {
            error("Cannot parse integer: \"%s\"", arg1);
        } // try-catch
        
        RoundingMode rm = null;
        try {
            rm = RoundingMode.valueOf(arg2);
        } catch(IllegalArgumentException ex) {
            error("Unrecognized rounding mode: \"%s\" (expected: UP, DOWN, CEILING, FLOOR, HALF_UP, HALF_DOWN, HALF_EVEN, UNNECESSARY)", arg2);
        } // try-catch
        
        MathContext mctx = new MathContext(digits, rm);
        String name = String.format("%d/%s", digits, arg2);
        
        return new Tuple<String, MathContext>(name, mctx);
    }
    

    private void toggleShowDoubleBits() {
        if (show_double_bits == true) {
            System.out.printf("Turning off double bit-level information\n");
        } else {
            System.out.printf("Turning on double bit-level information\n");
        }
        show_double_bits = !show_double_bits;
    } // toggleDoubleBits()

    
    private void expectArgs(String[] args, int len) {
        if (args.length != len) {
            error("Expected %d arguments, got %d\n", len, args.length);
        }
    }
    
    private final void error(String fmt, Object... args) {
        throw new RuntimeException(String.format(fmt, args));
    } // error()
    
    /**
     * Application entry point
     */
    public static void main(String[] args) {
        System.out.printf("Number systems testing tool (c) 2013 jani.hautamaki@hotmail.com\n");
        
        NumberSystemTool app = new NumberSystemTool();
        try {
            app.run();
        } catch(Exception ex) {
            String msg = ex.getMessage();
            if (msg != null) {
                System.out.printf("ERROR: %s\n", ex.getMessage());
            } else {
                ex.printStackTrace();
            } // if-else
        } // try-catch
        
        // TODO: System.exit(EXIT_SUCCESS) ?
    } // main()


    // LEGACY FROM NumberSystemHelper
    //================================

    // Very close to the Double.MAX_VALUE
    //      "A.9E17IR6IFL+6S"
    // Should result in the same as the values
    //      ".0A9E17IR6IFL+70"
    //      ".000A9E17IR6IFL+72"
    
    // These should overflow:
    //      ".000A9E17IR6IFL+80"        (exponent too big)
    //      ".000A9E17IR6JFL+72"        (mantissa too big)
    
    // Double.MAX_VALUE         = 1.79769313486231570e+308
    // Double.MIN_VALUE         = 4.90000000000000000e-324
    
    // max_mantissa = 10.3156020126482610
    // max_exponent = 208                 == 6S
    // compare to     10.3156020126482600 == A.9E17IR6IFL+6S
    
    // min_mantissa = 1.5
    // min_exponent = -219                == 79
    // compare to   = 
    
    // Very close to the Double.MIN_VALUE
    //      "1.F-79"                      == 1.0e-323 (double)
    
    // These should underflow:
    //      "1.F-7A"                    (exponent too small)
    //      "1.E-79"                    (mantissa too small)
    
    // Also look at
    //      http://steve.hollasch.net/cgindex/coding/ieeefloat.html
    // Approx decimal:
    //  double limits: ~10**(-323.3) to ~10**(308.3)
    
    // N-7A gives with BigDecimal:
    // 4.90000000000000000e-324 which seems to be the smallest non-zero
    // Minimum exponent is 218 (dec), ie. 78 (trig)
    // Minimum mantissa is then 0.05
    
    // Numeric limits in base-10:
    // Max double: 5.99231044954105300e+306
    // Min double: 1.50000000000000000e-322
} // class NumberSystemTool
