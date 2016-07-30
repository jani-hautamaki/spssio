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
 * These classes are {@link NumberSystem}, {@link NumberParser}
 * and {@link NumberFormatter}.
 */
public class NumberSystemTool {

    // CONSTANTS
    //===========

    private static final int INPUT_NUMPARSER            = 0;
    private static final int INPUT_JAVA_DOUBLE          = 1;
    private static final int INPUT_JAVA_FLOAT           = 2;
    private static final int INPUT_HEX                  = 3;
    private static final int INPUT_RESHAPE              = 4;

    private static final int OUTPUT_NUMFORMATTER        = 0;
    private static final int OUTPUT_JAVA_FORMAT         = 1;
    private static final int OUTPUT_JAVA_DOUBLE         = 2;

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
    private boolean outputDoubleBits = false;
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

        System.out.printf("numsystool (c) 2013 jani.hautamaki@hotmail.com\n");

        String revision = spssio.BuildInfo.REVISION;
        String btime = spssio.BuildInfo.TIMESTAMP;

        //revision = revision != null ? revision : "<untagged>";
        //btime = btime != null ? btime : "<untagged>";
        if ((revision != null) && (btime != null)) {
            System.out.printf("Revision    %s\n", revision);
            System.out.printf("Build time  %s\n", btime);
        }
        System.out.printf("\n");

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
            String stin = null;

            try {
                // Output command prompt
                System.out.printf("\n");

                stin = getInputMode();

                //prompt = "numsystool>> ";
                prompt = String.format("%s> ", stin);
                System.out.printf(prompt);
                lastPromptLength = prompt.length();
                // Read command
                line = br.readLine();
            } catch(IOException ex) {
                error("BufferedReader.readLine() raised an exception");
            } // try-catch

            // split into arguments
            String[] args = line.split(" ");

            if ((args.length == 0) || (line.length() == 0))  {
                System.out.printf("\n");
                System.out.printf("input:   %s\n", stin);
                System.out.printf("output:  %s\n", getOutputMode());
                continue;
            }

            try {
                parseCommand(args);
            } catch(Exception ex) {
                System.out.printf("Error: %s\n", ex.getMessage());
            } // try-catch

        } while (!quit);

    } // run()

    private void parseCommand(String[] args) {
        String carg = args[0];

        boolean iscmd = false;

        if (carg.startsWith("\\")) {
            iscmd = true;
            System.out.printf("\n");
        }

        if (carg.equals("\\q")) {
            expectArgs(args, 1);
            System.out.printf("Exiting\n");
            quit = true;
        }
        else if (carg.equals("\\version")) {
            expectArgs(args, 1);
            printVersion();
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
        else if (carg.equals("\\digits")) {
            doSetDigits(args);
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
            inputMode = INPUT_JAVA_DOUBLE;
            outputMode = OUTPUT_NUMFORMATTER;
            printBase();
        }
        else if (carg.equals("\\trig2dec")) {
            sysin.setBase(30);
            sysout.setBase(10);
            inputMode = INPUT_NUMPARSER;
            outputMode = OUTPUT_JAVA_DOUBLE;
            printBase();
        }
        else if (carg.equals("\\dec2hex")) {
            sysin.setBase(10);
            sysout.setBase(16);
            inputMode = INPUT_JAVA_DOUBLE;
            outputMode = OUTPUT_NUMFORMATTER;
            printBase();
        }
        else if (carg.equals("\\hex2dec")) {
            sysin.setBase(16);
            sysout.setBase(10);
            inputMode = INPUT_NUMPARSER;
            outputMode = OUTPUT_JAVA_DOUBLE;
            printBase();
        }
        else if (iscmd == true) {
            error("Unknown command");
        }
        else if (args.length > 1) {
            error("Syntax error");
        }
        else {
            // Otherwise assume it is a number
            parseNumber(carg);
        } // if-else
    } // parse()

    private void printVersion() {
        String version = spssio.BuildInfo.VERSION;
        String revision = spssio.BuildInfo.REVISION;
        String btime = spssio.BuildInfo.TIMESTAMP;


        System.out.printf("Version:       %s\n",
            version != null ? version : "<untagged>");
        System.out.printf("Revision:      %s\n",
            revision != null ? revision : "<untagged>");
        System.out.printf("Build time:    %s\n",
            btime != null ? btime : "<untagged>");
    }

    private void printHelp() {
        System.out.printf("Commands:\n");
        System.out.printf("\n");
        System.out.printf("\\l                  Show number system limits and details\n");
        System.out.printf("\\base [int]         Get/set number system radix\n");
        System.out.printf("\\base <sys> <int>   Get radix, sys is either \"in\" or \"out\"\n");
        System.out.printf("\\precision [int]    Get/set output precision\n");
        System.out.printf("\\trig               Shortcut for trigesimals, ie. \\base 30\n");
        System.out.printf("\\dec                Shortcut for decimals, ie. \\base 10\n");
        System.out.printf("\\bits               Toggle bit-level value display\n");
        System.out.printf("\\q                  Quit\n");
        System.out.printf("\\version            Display version/revision details\n");
        System.out.printf("\n");
        System.out.printf("Shortcuts:\n");
        System.out.printf("\\dec2trig           Decimals to trigesimals\n");
        System.out.printf("\\trig2dec           Trigesimals to decimals\n");
        System.out.printf("\\dec2hex            Decimals to hexadecimals\n");
        System.out.printf("\\hex2dec            Hexadecimals to decimals\n");
        System.out.printf("\n");
        System.out.printf("Input control:\n");
        System.out.printf("\\in java            Use Double.parseDouble for input\n");
        System.out.printf("\\in float           Use Float.parseFloat for input\n");
        System.out.printf("\\in tool            Use NumberParser.parseDouble for input\n");
        System.out.printf("\\in raw             Use Double.longBitsToDouble for hex input\n");
        System.out.printf("\\in reshape         Switch into reformat/reshape mode\n");
        System.out.printf("\n");
        System.out.printf("Output control\n");
        System.out.printf("\\out java           Use Double.toString for output\n");
        System.out.printf("\\out tool           Use NumberFormatter.formatDouble for output\n");
        System.out.printf("\\out string         Use String.format for output\n");
        System.out.printf("\\format [fmt]       Get/et format string for String.format\n");
        System.out.printf("\n");
        System.out.printf("MathContext control for input and output:\n");
        System.out.printf("\\context            Display current MathContext for input and output\n");
        System.out.printf("\\context <x>        Set MathContext to DECIMAL<x>. If n==0, unset\n");
        System.out.printf("\\context <n> <rm>   Set MathContext to precision=<n>, rounding=<rm>\n");
        System.out.printf("\n");
        System.out.printf("MathContext can be specified separately for either input or output:\n");
        System.out.printf("\\context [in|out] <x>\n");
        System.out.printf("\\context [in|out] <n> <rm>\n");
        System.out.printf("\n");
        System.out.printf("Setting the input/output digits:\n");
        System.out.printf("\\digits <digits>         Set the digits used by input and output\n");
        System.out.printf("\\digits <sys> <digits>   Set the digits used by <sys> (in or out)\n");
        System.out.printf("\n");
        System.out.printf("Note: every other single argument input is considered a number!\n");
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

        // Extract separate parts
        long msign =    bits & 0x8000000000000000L;
        long exponent = bits & 0x7ff0000000000000L;
        long mantissa = (bits & 0x000fffffffffffffL);

        //exponent = (exponent >> 52) - 1023; // shift and unbiasing
        exponent = exponent >> 52; // shift

        output("dec:       %s   (Double.toString)\n", Double.toString(d));
        output("hex:       %16s   (doubleToLongBits)\n", Long.toHexString(bits));
        output("exp/mant: %-3s %13s\n",
            Long.toHexString(exponent), Long.toHexString(mantissa));

        exponent = exponent - 1023;      // unbiasing
        mantissa |= 0x0010000000000000L; // normalizing
        double frac = ((double) mantissa) * Math.pow(2.0, -52);

        output("sign:      %s\n", msign != 0 ? "-" : "+");
        output("exponent:  %d\n", exponent);
        output("mantissa:  %s\n", Double.toString(frac));
        if (exponent >= 0) {
            output("*(2^exp):  2^(%d) = %.0f\n",
                exponent, Math.pow(2.0, exponent));
        } else {
            output("*(2^exp):  2^(%d) = 1/%.0f\n",
                exponent, Math.pow(2.0, -exponent));
        }
    } // printDoubleBits()

    private void printFloatBits(float f) {
        int bits = Float.floatToIntBits(f);

        // Extract separate parts
        int msign =    bits & 0x80000000;
        int exponent = bits & 0x7f800000;
        int mantissa = bits & 0x007fffff;

        exponent = exponent >> 23; // Shift

        output("dec:       %s   (Float.toString)\n", Float.toString(f));
        output("hex:       %8s  (floatToIntBits)\n", Integer.toHexString(bits));
        output("exp/mant: %-2s %6s\n",
            Integer.toHexString(exponent), Integer.toHexString(mantissa));

        exponent = exponent - 127; // Unbiasing
        mantissa |= 0x00800000;    // Normalizing
        float frac = ((float) mantissa) * (float) Math.pow(2.0, -23);

        output("sign:      %s\n", msign != 0 ? "-" : "+");
        output("exponent:  %d\n", exponent);
        output("mantissa:  %s\n", Float.toString(frac));
    } // printFloatBits()


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

    private void printDigits() {
        System.out.printf("       0         1         2         3         4         5         6         \n");
        System.out.printf("Digit: 0123456789012345678901234567890123456789012345678901234567890123456789\n");
        System.out.printf("Input: %s\n", sysin.getDigits());
        System.out.printf("Output %s\n", sysout.getDigits());
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

    private void output2(String prefix, String fmt, Object... args) {
        int indentLength = lastPromptLength - prefix.length();
        for (int i = 0; i < indentLength; i++) {
            System.out.printf(" ");
        }

        System.out.printf(prefix);
        System.out.printf(fmt, args);
    }


    private void parseNumber(String arg) {
        try {
            // result value is stored here
            double value = 0.0;
            float valueFloat = 0.0f;

            // The parse result defaults to invalid
            boolean valid = false;

            if (inputMode == INPUT_NUMPARSER) {
                // Parse using NumberParser
                value = parser.parseDouble(arg);

                // Pick the error code and inspect error status
                int errno = parser.errno();
                if (errno != NumberParser.E_OK) {
                    // Error management
                    System.out.printf("\n");
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
            } else if (inputMode == INPUT_JAVA_DOUBLE) {
                // Use Java's built-in double parser.
                value = Double.parseDouble(arg);
                // Parse success (even though the result might be infinite)
                valid = true;
            } else if (inputMode == INPUT_JAVA_FLOAT) {
                // Use Java's built-in float parser.
                valueFloat = Float.parseFloat(arg);
                // TODO: Refactor the code so that double is not necessary
                value = (double) valueFloat;
                // Parse success (even though the result might be infinite)
                valid = true;
            } else if (inputMode == INPUT_HEX) {
                // Read raw ieee 64 bit double
                value = parseDoubleHex(arg);
                valid = true;
            } else if (inputMode == INPUT_RESHAPE) {
                int[] buffer = formatter.getBuffer();
                int len = arg.length();
                for (int i = 0; i < len; i++) {
                    buffer[i] = arg.charAt(i);
                }
                // Perform actual reformat/reshape here.
                // Returns the new length of the reformatted string
                len = formatter.reformat(
                    buffer, len, formatter.getPrecision());
                output("%s\n", new String(buffer, 0, len));
                output("^ reshape to=%d\n", formatter.getPrecision());
                return;
            } else {
                error("Unexpected input mode (this is a bug)");
            }

            if (valid == false) {
                return;
            }

            // If succesfully parsed, output the value
            // in hexadecimal and in decimal.
            if (outputDoubleBits == false) {
                if (inputMode == INPUT_JAVA_FLOAT) {
                    output("dec: %s   (Float.toString)\n",
                        Float.toString(valueFloat)
                    );
                    output("raw: %s   (floatToRawIntBits)\n",
                        Integer.toHexString(Float.floatToRawIntBits(valueFloat))
                    );
                } else {
                    output("dec: %s   (Double.toString)\n",
                        Double.toString(value)
                    );
                    output("raw: %s   (doubleToRawLongBits)\n",
                        Long.toHexString(Double.doubleToRawLongBits(value))
                    );
                }
            } else {
                if (inputMode == INPUT_JAVA_FLOAT) {
                    printFloatBits(valueFloat);
                } else {
                    printDoubleBits(value);
                }
            }

            String result = null;

            if (outputMode == OUTPUT_NUMFORMATTER) {
                formatter.formatDouble(value);
                result = formatter.getString();
            } else if (outputMode == OUTPUT_JAVA_FORMAT) {
                result = String.format(Locale.ROOT, outputFormat, value);
            } else if (outputMode == OUTPUT_JAVA_DOUBLE) {
                result = Double.toString(value);
            } else {
                error("Unexpected output mode (this is a bug)");
            }

            //output("%s   (%s)\n", result, stout);
            System.out.printf("\n");
            output("%s\n", result);
            output("^ %s\n", getOutputMode());

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
            inputMode = INPUT_JAVA_DOUBLE;
        }
        else if (arg.equals("float")) {
            inputMode = INPUT_JAVA_FLOAT;
        }
        else if (arg.equals("raw")) {
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
            outputMode = OUTPUT_JAVA_DOUBLE;
        }
        else if (arg.equals("string")) {
            outputMode = OUTPUT_JAVA_FORMAT;
        }
        else {
            error("Unrecognized output mode: %s", arg);
        }
        System.out.printf("Output mode set.\n");
    } // doSetInputMode()

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

    private void doSetDigits(String[] args) {
        if (args.length == 1) {
        } else if (args.length == 2) {
            if (sysin.getBase() != sysout.getBase()) {
                error("Cannot use the same digits for both input and output, because they are using different bases");
            }
            // set both
            sysin.setNumberSystem(sysin.getBase(), args[1]);
            sysout.setNumberSystem(sysout.getBase(), args[1]);
            System.out.printf("Input and output digits set\n");
        } else if (args.length == 3) {
            if (args[1].equals("in")) {
                // set input
                sysin.setNumberSystem(sysin.getBase(), args[2]);
                System.out.printf("Input digits set\n");
            } else if (args[1].equals("out")) {
                // set output
                sysout.setNumberSystem(sysout.getBase(), args[2]);
                System.out.printf("Output digits set\n");
            } else {
                error("Expected either \"in\" or \"out\", but found: %s",
                    args[1]);
            }
        } else {
            error("Too many arguments");
        }
        printDigits();
    } // doSetDigits()

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

    private String getInputMode() {
        String rval = null;
        switch(inputMode) {
            case INPUT_NUMPARSER:
                if (inputMathContextName != null) {
                    rval = String.format("tool b=%d m=%s",
                        sysin.getBase(), inputMathContextName);
                } else {
                    rval = String.format("tool b=%d m=double",
                        sysin.getBase());
                } // if-else: math context set
                break;
            case INPUT_JAVA_DOUBLE:
                rval = String.format("java b=10/fixed");
                break;
            case INPUT_JAVA_FLOAT:
                rval = String.format("float b=10/fixed");
                break;
            case INPUT_HEX:
                rval = String.format("raw ieee64");
                break;
            case INPUT_RESHAPE:
                if (outputMathContextName != null) {
                    rval = String.format("reshape b=%d to=%d m=%s",
                        sysout.getBase(),
                        formatter.getPrecision(),
                        outputMathContextName);
                } else {
                    rval = String.format("reshape b=%d to=%d m=double",
                        sysout.getBase(),
                        formatter.getPrecision());
                } // if-else: math context set
            default:
                error("Unexpected input mode: %d (this is a bug)", inputMode);
                break;
        } // switch
        return rval;
    }

    private String getOutputMode() {
        String rval = null;
        switch(outputMode) {
            case OUTPUT_NUMFORMATTER:
                if (outputMathContextName != null) {
                    rval = String.format("tool b=%d prec=%d m=%s",
                        sysout.getBase(),
                        formatter.getPrecision(),
                        outputMathContextName);
                } else {
                    rval = String.format("tool b=%d prec=%d m=double",
                        sysout.getBase(),
                        formatter.getPrecision());
                }
                break;
            case OUTPUT_JAVA_FORMAT:
                rval = String.format("string b=?/implied fmt=%s",
                    outputFormat);
                break;
            case OUTPUT_JAVA_DOUBLE:
                rval = String.format("java b=10/fixed");
                break;
            default:
                error("Unexpected output mode: %d (this is a bug)", outputMode);
                break;
        } // switch

        return rval;
    }

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
        if (outputDoubleBits == true) {
            System.out.printf("Turning off double bit-level information\n");
        } else {
            System.out.printf("Turning on double bit-level information\n");
        }
        outputDoubleBits = !outputDoubleBits;
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


    /*
     * Some examples. First, converting IEEE 64-bit floating points into
     * base-30 string representations:
     * "27 23 f7 0c 92 52 93 3f" results in "GTECSL0R001-C"
     * "26 23 f7 0c 92 52 93 3f" results in "GTECSL0QTTT-C"
     */
    /*
     *                    0.623560537            => IL6411E5L01-B
     *  .IL6411E5L01    = 0.623560536999999900   => IL6411E5KTT-B
     * 1.FBOP0S65C      = 1.51314201400000000    => 1FBOP0S65C-9
     * MJD8FEMNTTT-C    = 0.0251645480000000020  => MJD8FEMO001-C
     * 4JGBA84O-9       = 0.00516836800000000040 => (ok)
     *
     */

} // class NumberSystemTool
