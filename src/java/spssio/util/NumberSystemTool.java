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

// for testing
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

/**
 * Test application for the number system related classes.
 * These classes are {@code NumberSystem}, {@code NumberParser}
 * and {@code NumberPrinter}.
 */
public class NumberSystemTool {
    
    // MEMBER VARIABLES
    //==================
    
    private boolean quit = false;
    private NumberSystem sys = null;
    private NumberParser parser = null;
    
    private boolean show_double_bits = false;
    private boolean use_builtin_parser = false;
    private String ctx = null;
    
    // CONSTRUCTORS
    //==============
    
    public NumberSystemTool() {
        sys = new NumberSystem();
        parser = new NumberParser();
        // Associate number system with the parser
        parser.setNumberSystem(sys);
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
        sys.setBase(10);
        
        // Show number system details at the beginning.
        printLimits();
        
        quit = false;
        do {
            String line = null;
            
            if (use_builtin_parser == false) {
                if (ctx == null) {
                    System.out.printf("b=%d>> ", sys.getBase());
                } else {
                    System.out.printf("b=%d|m=%s>> ", sys.getBase(), ctx);
                }
            } else {
                System.out.printf("java >> ");
            } // if-else
            
            try {
                line = br.readLine();
            } catch(IOException ex) {
                error("BufferedReader.readLine() raised an exception");
            } // try-catch
            
            String[] args = line.split(" ");
            try {
                parseCommand(args);
            } catch(Exception ex) {
                System.out.printf("%s\n", ex.getMessage());
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
            expectArgs(args, 2);
            doSetBase(args[1]);
        }
        else if (carg.equals("\\bits")) {
            expectArgs(args, 1);
            toggleShowDoubleBits();
        }
        else if (carg.equals("\\java")) {
            expectArgs(args, 1);
            toggleBuiltinParser();
        }
        else if (carg.equals("\\trig")) {
            // Shortcut to trigesimals
            expectArgs(args, 1);
            System.out.printf("Using trigesimals\n");
            sys.setBase(30);
        }
        else if (carg.equals("\\dec")) {
            // Shortcut to decimals
            expectArgs(args, 1);
            System.out.printf("Using decimals\n");
            sys.setBase(10);
        }
        else if (carg.equals("\\mode")) {
            if (args.length == 2) {
                doSetMode(args[1]);
            } else {
                expectArgs(args, 3);
                doSetMode(args[1], args[2]);
            } // if-else
        }
        else if (args.length > 1) {
            error("Syntax error");
        } 
        else {
            // Otherwise assume it is a number
            System.out.printf("Parser input: \"%s\"\n", carg);
            parseNumber(carg);
        } // if-else
    } // parse()
    
    private void printHelp() {
        System.out.printf("Commands:\n");
        System.out.printf("\n");
        System.out.printf("\\l                  show number system limits and details\n");
        System.out.printf("\\base <int>         set the radix/base of the number system\n");
        System.out.printf("\\tri                shortcut for trigesimals, ie. \\base 30\n");
        System.out.printf("\\dec                shortcut for decimals, ie. \\base 10\n");
        System.out.printf("\\bits               toggle bit-level value display\n");
        System.out.printf("\\java               switch between Double.parseDouble() and NumberParser\n");
        System.out.printf("\\mode <ctx>         set MathContext, ctx has to be 32, 64 or 128\n");
        System.out.printf("\\mode <d> <rm>      set arbitrary MathContext; d=precision, rm=rounding\n");
        System.out.printf("\\q                  quit\n");
        System.out.printf("\n");
        System.out.printf("Every other single argument input is considered a number!\n");
        System.out.printf("\n");
    } // printHelp()
    
    private void printLimits() {
        System.out.printf("Number system details:\n");
        System.out.printf("Base:       %d\n", sys.getBase());
        System.out.printf("Digits:     \"%s\"\n", sys.getDigits());
        System.out.printf("\n");
        System.out.printf("Plus char:  \'%c\'\n", sys.getPlusChar());
        System.out.printf("Minus char: \'%c\'\n", sys.getMinusChar());
        System.out.printf("Point char: \'%c\'\n", sys.getPointChar());
        System.out.printf("\n");
        System.out.printf("Max double / base: %.18g\n", sys.getMaxDouble());
        System.out.printf("Min double * base: %.18g\n", sys.getMinDouble());
        System.out.printf("\n");
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

        System.out.printf("   double:     %.18g\n", d);
        System.out.printf("   toLongBits: %s\n", Long.toHexString(bits));
        System.out.printf("   toLongBits: %s\n", Long.toHexString(Double.doubleToLongBits(d)));
        System.out.printf("   sign:       %s\n", neg != 0 ? "-" : "+");
        System.out.printf("   exponent:   2**%d = %.0f\n", exponent, Math.pow(2.0, exponent));
        System.out.printf("   mantissa:   %s\n", Long.toHexString(mantissa));
        System.out.printf("   frac:       %.16g\n", frac);
    } // printDoubleBits()
    
    
    private void parseNumber(String arg) {
        try {
            double value;
            
            // The parse result defaults to invalid
            boolean valid = false;
            
            if (use_builtin_parser == false) {
                // Parse using NumberParser
                value = parser.parseDouble(arg);
                // Pick the error code
                int errno = parser.errno();
                // Inspect error status
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
            } else {
                // Use Java's built-in double parser.
                value = Double.parseDouble(arg);
                // Parse success (even though the result might be infinite)
                valid = true;
            } // if-else
            
            if (valid) {
                if (show_double_bits) {
                    printDoubleBits(value);
                } else {
                    System.out.printf("Value: %.18g\n", value);
                } // if-else: show bits
            } // if: valid result
        }
        catch(NumberFormatException ex) {
            System.out.printf("ERROR: NumberFormatException: %s\n, ex.getMessage()");
        } 
        catch(Exception ex) {
            System.out.printf("Parse failed due to exception\n");
            ex.printStackTrace();
        } // try-catch
    } // parseNumber()
    
    private void doSetBase(String text) {
        int value = 0;
        try {
            value = Integer.parseInt(text);
        } catch(Exception ex) {
            error("Cannot parse integer: \"%s\"", text);
        } // try-catch
        
        if (value < 1) {
            error("The base must be greater than zero");
        }
        
        System.out.printf("Setting new radix: %d\n", value);
        sys.setBase(value);
    } // doSetBase()

    private void doSetMode(String arg) {
        if (arg.equals("128")) {
            System.out.printf("Using internally: MathContext.DECIMAL128\n");
            ctx = "128";
            parser.setMathContext(MathContext.DECIMAL128);
        }
        else if (arg.equals("64")) {
            System.out.printf("Using internally: MathContext.DECIMAL64\n");
            ctx = "64";
            parser.setMathContext(MathContext.DECIMAL64);
        }
        else if (arg.equals("32")) {
            System.out.printf("Using internally: MathContext.DECIMAL32\n");
            ctx = "32";
            parser.setMathContext(MathContext.DECIMAL32);
        }
        else if (arg.equals("0")) {
            System.out.printf("Using internally: double\n");
            ctx = null;
            parser.setMathContext(null);
        }
        else {
            error("Unrecognized mode: \"%s\"", arg);
        }
    } // doSetMode()
    
    private void doSetMode(String arg1, String arg2) {
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
        parser.setMathContext(mctx);
        ctx = String.format("%d/%s", digits, arg2);
    } // doSetMode()
    

    private void toggleShowDoubleBits() {
        if (show_double_bits == true) {
            System.out.printf("Turning off double bit-level information\n");
        } else {
            System.out.printf("Turning on double bit-level information\n");
        }
        show_double_bits = !show_double_bits;
    } // toggleDoubleBits()

    private void toggleBuiltinParser() {
        if (use_builtin_parser == true) {
            System.out.printf("Using NumberParser\n");
        } else {
            System.out.printf("Using Java\'s built-in Double.parseDouble()\n");
        }
        use_builtin_parser = !use_builtin_parser;
    } // toggleBuiltinParser()
    
    
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















