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

package spssio.sample;

// core java
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.util.Vector;
import java.util.Arrays;
import java.util.Random;

// for profiling memory consumption
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

// spssio
import spssio.por.output.PORWriter;
import spssio.por.PORFile;

/**
 * Portable file noise generator.
 *
 */
public class PORNoiseGen {
    
    // CONSTANTS
    //===========
    
    /**
     * Exit code for succesful run.
     */
    public static final int EXIT_SUCCESS = 0;

    /**
     * Exit code for unsuccesful run.
     */
    public static final int EXIT_FAILURE = 1;
    
    /**
     * Command-line options
     */
    public static class Options {
        public boolean debugFlag = false;
        public int rows = 100;
        public int cols = 100;
        public double mean = 0.0;
        public double stddev = 1.0;
        public double sigmaSysmiss = 3.0;
        public String outputFilename = null;
        public int bufferSize = 0;
    } // class Options
    
    public static void parseArguments(String[] args, Options opt) {
        for (int i = 0; i < args.length; i++) {
            String carg = args[i];
            if (carg.equals("-debug")) {
                // Turn on stack trace.
                opt.debugFlag = true;
            }
            else if (carg.equals("-help")) {
                usage();
                System.exit(EXIT_SUCCESS);
            }
            else if (carg.startsWith("-rows=")) {
                opt.rows = parseIntArg(carg);
            }
            else if (carg.startsWith("-cols=")) {
                opt.cols = parseIntArg(carg);
            }
            else if (carg.startsWith("-stddev=")) {
                opt.stddev = parseDoubleArg(carg);
            }
            else if (carg.startsWith("-mean=")) {
                opt.mean = parseDoubleArg(carg);
            }
            else if (carg.startsWith("-sysmiss=")) {
                opt.sigmaSysmiss = parseDoubleArg(carg);
            }
            else if (carg.startsWith("-bufsize=")) {
                opt.bufferSize = parseIntArg(carg);
            }
            else if (carg.startsWith("-")) {
                error("Unrecognized option: %s", carg);
            } 
            else {
                // Not an option; assumed to be the output file.
                if (opt.outputFilename != null) {
                    error("More than one output file specified");
                }
                opt.outputFilename = carg;
            } // if-else
        } // for: each arg
    } // parseArgs()
    
    public static void usage() {
        System.out.printf("Options:\n");
        System.out.printf("   -rows=n         Set the number of rows (default: 100)\n");
        System.out.printf("   -cols=n         Set the number of columns (default: 100)\n");
    }
    
    protected static int parseIntArg(String carg) {
        String s = carg.substring(carg.indexOf('=')+1);
        try {
            return Integer.parseInt(s);
        } catch(NumberFormatException ex) {
            error("Not an integer: %s\n", s);
        } // try-catch
        
        // Never reached
        throw new RuntimeException("Impossible");
    } // parseIntArg()

    protected static double parseDoubleArg(String carg) {
        String s = carg.substring(carg.indexOf('=')+1);
        try {
            return Double.parseDouble(s);
        } catch(NumberFormatException ex) {
            error("Not a number: %s\n", s);
        } // try-catch
        
        // Never reached
        throw new RuntimeException("Impossible");
    } // parseIntArg()
    
    /** 
     * Serialize Portable file to {@code OutputStream} 
     * according to {@code Options}.
     */
    public static void outputPortable(
        OutputStream os,
        Options opt
    ) 
        throws IOException
    {
        
        // Create PORWriter for the OutputStream object
        PORWriter writer = new PORWriter(os);

        // Header
        writer.outputHeader(
            null, // splash
            null, // charset
            null, // signature
            0,    // version (zero means use default)
            null, // date
            null  // time
        );

        // Software
        writer.outputSoftware("PORNoiseGen");
        
        // Author
        writer.outputAuthor("John Doe");
        
        // Title
        writer.outputTitle(String.format(
            "normal distr (mean=%g, std=%g)", opt.mean, opt.stddev));

        // Variable count
        writer.outputVariableCount(opt.cols);
        
        // Numeric precision
        writer.outputNumericPrecision(
            writer.getNumberFormatter().getPrecision());
        
        // Save the generated variable names into this
        Vector<String> varnames = new Vector<String>(opt.cols);
        
        // Write variable records
        for (int i = 0; i < opt.cols; i++) {
            
            // Generate variable name, and record it to the vector
            String name = String.format("VAR_%03d", i);
            varnames.add(name);
            
            // For a list of print/write format types, see
            // http://www.gnu.org/software/pspp/pspp-dev/html_node/Variable-Record.html#Variable-Record
            
            // Write variable record
            writer.outputVariableRecord(
                0,          // variable width (0: numeric)
                name,       // variable name
                5, 40, 40,  // print type/width/decimals
                5, 40, 40   // write type/width/decimals
            );
            
            // Output missing values here, if any
            
            // Finally output the variable label
            /*
            String label = String.format(
                "[%s] Automatically generated variable", name);
            */
            StringBuilder sb = new StringBuilder(100);
            sb.append(String.format("%3d ", i+10));
            for (int j = 0; j < i+6; j++) {
                sb.append(String.format("%d", (j+4) % 10));
            }
            String label = sb.toString();

            writer.outputVariableLabel(label);
        } // for: each variable
        
        double std = opt.stddev;
        double mean = opt.mean;
        
        // Create value-label mapping as a linear array:
        Object[] vallabels = new Object[] {
            -3.0*std, "-3*sigma (cumulative: 0.1%) ",
            -2.0*std, "-2*sigma (cumulative: 2.5%)",
            -1.0*std, "-1*sigma (cumulative: 15.9%)",
             0, String.format("mean (%g)", mean),
             1.0*std, "+1*sigma (within CI: 68.3%)",
             2.0*std, "+2*sigma (within CI: 95.5%)",
             3.0*std, "+3*sigma (within CI: 99.7%)",
        };
        
        
        // Output value-label pairs
        writer.outputValueLabelsRecord(
            varnames, Arrays.asList(vallabels));
        
        /*
        // Serialize data
        double sysmiss_limit = opt.stddev * opt.sigmaSysmiss;
        double randval;
        boolean sysmiss;
        for (int y = 0; y < opt.rows; y++) {
            for (int x = 0;  x < opt.cols; x++) {
                // Draw a number from the normal distribution
                randval = randn(); // N(1,0)
                sysmiss = false;
                if ((randval < -sysmiss_limit) 
                    || (randval > sysmiss_limit)) 
                {
                    // Mark missing
                    sysmiss = true;
                } // if: over sysmiss sigma limit
                
                if (sysmiss == false) {
                    writer.outputNumeric(randval*opt.stddev+opt.mean);
                } else {
                    writer.outputSysmiss();
                }
            } // for: each col
        } // for: each row
        */
        
        System.out.printf("rows=%d, cols=%d\n", opt.rows, opt.cols);
        
        printStatusHeaders();

        int ynext = 0;
        int ystep = opt.rows / 20;
        
        // Write data matrix tag code
        writer.outputTag('F');
        
        for (int y = 0; y < opt.rows; y++) {
            for (int x = 0;  x < opt.cols; x++) {
                //writer.outputInt(x);
                writer.outputDouble(810000 + y*opt.cols+x);
            } // for: each col
            if (y > ynext) {
                ynext += ystep;
                printStatusLine(y, opt.rows);
            }
        } // for: each row
        
        printStatusLine(opt.rows, opt.rows);
        
        // Write end-of-file
        writer.outputEofMarkers();
    } // outputPortable()
    
    public static void outputPortable2(
        OutputStream os,
        Options opt
    ) 
        throws IOException
    {
        // Through PORFile object
        PORFile por = new PORFile();
        
    }
    
    
    public static void main(String[] args) {
        
        if (args.length == 0) {
            usage();
            System.exit(EXIT_SUCCESS);
        }
        
        // Instantiate new container for command-line options.
        Options opt = new Options();
        
        // Parse command-line arguments.
        try {
            parseArguments(args, opt);
            
            // Verify settings
            if (opt.outputFilename == null) {
                error("No output file specified");
            }
        } catch(Exception ex) {
            String msg = ex.getMessage();
            if (msg != null) {
                System.out.printf("ERROR: %s\n", msg);
            } else {
                ex.printStackTrace();
            } // if-else
            System.exit(EXIT_FAILURE);
        } // try-catch
        
        OutputStream os = null;
        try {
            
            // Try to open the output file
            try {
                // Create new file output stream
                os = new FileOutputStream(new File(
                    opt.outputFilename));
                
                // Wrap to a buffer
                if (opt.bufferSize > 0) {
                    os = new BufferedOutputStream(os, opt.bufferSize);
                } else {
                    os = new BufferedOutputStream(os);
                }
                
            } catch(Exception ex) {
                // rephrase error
                error(ex, "Cannot open file (%s)", opt.outputFilename);
            } // try-catch
            
            // Start timing
            long startTime = System.nanoTime();
        
            outputPortable(os, opt);

            // Stop timing
            long endTime = System.nanoTime();

            // Calculate duration
            long duration = endTime - startTime;
            
            // Notify
            System.out.printf("Spent %.2f seconds in writing\n", duration/1.0e9);
            
        } catch(Exception ex) {
            String msg = ex.getMessage();
            if ((opt.debugFlag == true) || (msg == null)) {
                ex.printStackTrace();
            } else {
                System.out.printf("ERROR: %s\n", msg);
            } // if-else
            System.exit(EXIT_FAILURE);
        } finally {
            // Close resources
            if (os != null) {
                try {
                    os.close();
                } catch(Exception ex) {
                    // Silently ignore
                } // try-catch
            } // if: file open
        }// try-catch
        
        // Execute
        /*
        open file
        write header
        write variable records
            for x=0; x < cols
                outputVariableRecord("x1", 0, 0, 0, "label")
        write data; 
            for y=0; y < rows;
                for x=0; x < cols; 
                    outputNumeric(rand())
        outputEndOfData()
        */
        
    } // main()

    /*
     * A convenience method for throwing exceptions
     */
    private static void error(
        String fmt, 
        Object... args
    ) {
        throw new RuntimeException(String.format(fmt, args));
    }

    /*
     * A convenience method for rephrasing the exception
     */
    private static void error(
        Exception ex,
        String fmt, 
        Object... args
    ) {
        String msg = ex.getMessage();
        if (msg == null) {
            msg = ex.getClass().getName();
        }
        
        throw new RuntimeException(String.format("%s: %s",
        String.format(fmt, args), msg), ex);
    } // rephrase()

    
    private static boolean s_randvalUsed = false;
    private static double s_randval = 0.0;
    private static Random s_rand = new Random();
    
    
    public static double randn() {
        if (s_randvalUsed == false) {
            s_randvalUsed = true;
            return s_randval;
        } else {
            double x1, x2;
            double w;
            do {
                x1 = 2.0 * s_rand.nextDouble() - 1.0;
                x2 = 2.0 * s_rand.nextDouble() - 1.0;
                w = (x1*x1)+(x2*x2);
            } while (w >= 1.0);
            
            // Apply Box-Muller transformation.
            // Generates two normal deviates
            w = Math.sqrt( (-2.0*Math.log(w)) / w );
            
            // Record one for later use...
            s_randval = x2;
            s_randvalUsed = false;
            /// .. And return the other now.
            return x1;
        } // if-else
        
    } // randn()


                
    // Memory bean
    private static MemoryMXBean 
        mem_bean = ManagementFactory.getMemoryMXBean();

    protected static void printStatusHeaders() {
        System.out.printf("progress        row           used    commit       max\n");
    }
    
    protected static void printStatusLine(int y, int sizey) {
        // Do garbage collectioning prior to memory status
        // (Enabling the gc will choke Java VM on big files)
        //Runtime.getRuntime().gc();
        
        MemoryUsage usage = mem_bean.getHeapMemoryUsage();
        
        //System.out.printf("%3d%%                       %4d      %4d      %4d\n", 
        System.out.printf(" %3d%%        %6d        %4d MB   %4d MB   %4d MB\n", 
            (y*100)/sizey,
            y,
            usage.getUsed()         / (1024*1024),
            usage.getCommitted()    / (1024*1024),
            usage.getMax()          / (1024*1024)
        );
    }

    
    
} // class PORNoiseGen
