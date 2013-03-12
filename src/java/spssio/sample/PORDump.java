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

//  for iterating value labels
import java.util.Map;
// for iterating variables and missing values
import java.util.Vector;
// for locale-independent formatting
import java.util.Locale;

// core java
import java.io.File;
import java.io.IOException;

// for csv output
import java.io.FileOutputStream;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.OutputStreamWriter;

// spssio por
import spssio.por.PORMissingValue;
import spssio.por.PORValueLabels;
import spssio.por.PORVariable;
import spssio.por.PORValue;
import spssio.por.PORMatrix;
import spssio.por.PORMatrixVisitor;
import spssio.por.PORHeader;
import spssio.por.PORFile;

import spssio.por.input.PORReader;

public class PORDump {
    
    /**
     * Exit code for succesful execution.
     */
    public static final int EXIT_SUCCESS = 0;
    
    /**
     * Exit code for failed execution.
     */
    public static final int EXIT_FAILURE = 1;
    
    
    public static class Options {
        
        public static final int METH_UNDEFINED          = -1;
        public static final int METH_JUST_VISIT         = 0;
        public static final int METH_STRING_FORMAT      = 1;
        public static final int METH_FROM_INPUT         = 2;
        public static final int METH_RECTANGLE          = 3;
        
        public boolean debug_flag                       = false;
        
        public boolean header_details_flag              = true;
        public boolean variable_details_flag            = false;
        public boolean value_details_flag               = false;
        
        public Vector<String> input_filenames 
            = new Vector<String>();
        
        public String data_output_filename              = null;
        public int data_output_method                   = METH_UNDEFINED;
    } // class Options
    
    
    
    
    
    public static String val2str(String s) {
        final String VALUE_UNSET = "<unset>";
        
        if (s == null) {
            return VALUE_UNSET;
        } 
        // Otherwise use a quoted value
        return String.format("\"%s\"", s);
    }
    
    /**
     * Prints Portable file's header information
     */
    public static void printPORHeader(PORHeader header) {
        String s = null;
        
        // These are mandatory
        System.out.printf("Signature:           \"%s\"\n", header.signature);
        System.out.printf("Version:             \'%c\'\n", header.version);
        System.out.printf("Creation date:       \"%s\"\n", header.date);
        System.out.printf("Creation time:       \"%s\"\n", header.time);
        
        // Software field is optional
        System.out.printf("Software:            %s\n", val2str(header.software));
        // Author field is optional
        System.out.printf("Author:              %s\n", val2str(header.author));
        // Title field is optional
        System.out.printf("Title:               %s\n", val2str(header.title));
        
        // These are mandatory, but in theory they could be missing.
        System.out.printf("# of variables:      %d\n", header.nvariables);
        System.out.printf("Precision:           %d base-30 digits\n", header.precision);

        // Weight variable name is optional
        System.out.printf("Weight variable:     %s\n", val2str(header.weight_var_name));
    } // printHeader()
    
    public static void printVariables(Vector<PORVariable> variables) {
        int size = variables.size();
        
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                System.out.printf("\n");
            }
            System.out.printf("Variable %d/%d details:\n", i+1, size);
            printPORVariable(variables.elementAt(i));
        } // for
    } // printVariables()
    
    public static void printPORVariable(PORVariable pvar) {
        System.out.printf("Name:                \"%s\"\n", pvar.name);
        System.out.printf("Width:               %d %s\n", 
            pvar.width, pvar.width==0 ? "(numeric)" : "(string)");
        
        // Label iso optional
        System.out.printf("Label:               %s\n", val2str(pvar.label));
        
        // Formatting details
        System.out.printf("Output format:       %d:%d.%d\n",
            pvar.printfmt.type, pvar.printfmt.width, pvar.printfmt.decimals);
        
        System.out.printf("Input format:        %d:%d.%d\n", 
            pvar.writefmt.type, pvar.writefmt.width, pvar.writefmt.decimals);
        
        // Missing value specifications are optional
        if (pvar.missvalues.size() == 0) {
            System.out.printf("No additional missing value specifications.\n");
        } else {
            System.out.printf("Has %d additional missing value specifications.\n",
                pvar.missvalues.size());
            
            printMissingValues(pvar.missvalues);
        } // if-else: has missing values
    } // printPORVariable()
    
    public static void printMissingValues(
        Vector<PORMissingValue> missvalues
    ) {
        int size = missvalues.size();
        for (int i = 0; i < size; i++) {
            printPORMissingValue(missvalues.elementAt(i));
        } // for
    } // printMissingValues()
    
    public static void printPORMissingValue(PORMissingValue pmv) {
        String s = null;
        switch(pmv.type) {
            case PORMissingValue.TYPE_DISCRETE_VALUE:
                s = String.format(
                    "Missing value:       Singular:     %s",
                    pmv.values[0].value);
                break;
            case PORMissingValue.TYPE_RANGE_OPEN_LO:
                s = String.format(
                    "Missing value:       Open range:   --%s", 
                    pmv.values[0].value);
                break;
            case PORMissingValue.TYPE_RANGE_OPEN_HI:
                s = String.format(
                    "Missing value:       Open range:   %s--", 
                    pmv.values[0].value);
                break;
            case PORMissingValue.TYPE_RANGE_CLOSED:
                s = String.format(
                    "Missing value:       Closed range: %s--%s", 
                    pmv.values[0].value,
                    pmv.values[1].value);
                break;
            
            default:
                s = String.format(
                    "Missing value:       <unknown type>     %d", 
                    pmv.type);
                break;
        } // switch
        System.out.printf("%s\n", s);
    } // printPORMissingValue()
    
    public static void printValueLabels(
        Vector<PORValueLabels> labels
    ) {
        int size = labels.size();
        
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                System.out.printf("\n");
            }
            System.out.printf("Value-Label set %d/%d details:\n", i+1, size);
            printPORValueLabels(labels.elementAt(i));
        } // for
    } // printValueLabels
    
    public static void printPORValueLabels(PORValueLabels labels) {
        // vars
        // mappings
        int nvars = labels.vars.size();
        int listed=0;
        for (int i = 0; i < nvars; i++) {
            PORVariable pvar = labels.vars.elementAt(i);
            
            if (listed == 0) {
                if (i == 0) {
                    System.out.printf("Variables:           ");
                } else {
                    System.out.printf("                     ");
                }
            } // if: first entry of the line
            
            if (listed < 6) {
                System.out.printf("%-8s ", pvar.name);
                listed++;
            } else {
                System.out.printf("\n");
                listed = 0;
            }
        } // for
        if (listed != 0) {
            // Finish the line
            System.out.printf("\n");
        }
        
        System.out.printf("Mappings:\n");
        
        for (Map.Entry<PORValue, String> entry : labels.mappings.entrySet()) {
            System.out.printf("%-20s %s\n",
                entry.getKey().value, entry.getValue());
        } // for: each mapping
    } // printPORValueLabels()
    
    public static void printOverview(PORFile por) {
        PORHeader header = por.header;
        System.out.printf("Portable file contents\n");

        System.out.printf("\n");
        System.out.printf("Header:\n");
        printPORHeader(por.header);

        System.out.printf("\n");
        System.out.printf("Data matrix overview:\n");
        System.out.printf("Dimensions:          %d x %d (cases x variables)\n", por.data.sizey(), por.data.sizex());
        System.out.printf("Size:                %d bytes\n", por.data.size());

        // Count the data types
        
        int[] types = por.data.getDataColumnTypes();
        int numeric_columns = 0;
        int string_columns = 0;
        for (int i = 0; i < types.length; i++) {
            if (types[i] == PORValue.TYPE_STRING) {
                string_columns++;
            } else if (types[i] == PORValue.TYPE_NUMERIC) {
                numeric_columns++;
            } else {
                // error
            }
        } // for
        System.out.printf("Numeric variables:   %d\n", numeric_columns);
        System.out.printf("String variables:    %d\n", string_columns);
    } // printHeader
    
    public static void printPortable(PORFile por) {
        // Print overview
        printOverview(por);
        
        // If variable details are not desired. skip this
        System.out.printf("\n");
        printVariables(por.variables);

        
        System.out.printf("\n");
        printValueLabels(por.labels);
    }
    
    public static void writeHeader(Writer out, PORFile por) 
        throws IOException
    {
        // The first line is variable names
        int nvars = por.variables.size();
        
        for (int i = 0; i < nvars; i++) {
            if (i > 0) {
                out.write(',');
            }
            out.write(por.variables.elementAt(i).name);
        }
        out.write('\n');
        
    } // writeHeader()
    
    public static void writeMatrix(Writer out, PORFile por, int method) 
        throws IOException
    {
        
        if (out != null) {
            writeHeader(out, por);
        }
        
        // Create the visitor
        MatrixOutputter visitor = new MatrixOutputter(out, method);
        
        // Start timing
        long startTime = System.nanoTime();
        
        // ========== VISIT ==========
        por.data.visit(visitor);
        // ===========================
        
        // Stop timing
        long endTime = System.nanoTime();

        // Calculate duration
        long duration = endTime - startTime;
        
        // Notify
        System.out.printf("Spent %.2f seconds\n", duration/1.0e9);
    } // writeMatrix()
    
    private static void parse_error(
        String fmt, 
        Object... args
    ) {
        throw new RuntimeException(String.format(
            "Parse error: %s", String.format(fmt, args)));
        
    }
    
    public static void parseArgs(String[] args, Options opt) {
        for (int i = 0; i < args.length; i++) {
            String carg = args[i];
            if (carg.equals("-silent")) {
                opt.header_details_flag     = false;
                opt.variable_details_flag   = false;
                opt.value_details_flag      = false;
            }
            else if (carg.equals("-all")) {
                opt.header_details_flag     = true;
                opt.variable_details_flag   = true;
                opt.value_details_flag      = true;
            }
            else if (carg.equals("-vars")) {
                opt.variable_details_flag   = true;
            }
            else if (carg.equals("-labels")) {
                opt.value_details_flag      = true;
            }
            else if (carg.startsWith("-output=")) {
                String s = carg.substring(carg.indexOf('=')+1);
                opt.data_output_filename = s;
            }
            else if (carg.startsWith("-method=")) {
                String s = carg.substring(carg.indexOf('=')+1);
                int val = Options.METH_UNDEFINED;
                
                if (s.equals("none")) {
                    val = Options.METH_JUST_VISIT;
                }
                else if (s.equals("string")) {
                    val = Options.METH_STRING_FORMAT;
                }
                else if (s.equals("array")) {
                    val = Options.METH_FROM_INPUT;
                }
                else {
                    parse_error("Unrecognized method: \"%s\"", s);
                } // if-else
                opt.data_output_method = val;
            }
            else if (carg.equals("-debug")) {
                opt.debug_flag = true;
            }
            else if (carg.startsWith("-")) {
                parse_error("Unrecognized option: %s\n", carg);
            }
            else {
                // Assume it is a file name
                opt.input_filenames.add(carg);
            } // if-else: carg
        } // for: each arg
    } // parseArgs()
    
    public static void usage() {
        System.out.printf("Usage: PORDump <input_por> [<input_por2> ...] [<options>]\n");
        System.out.printf("\n");
        System.out.printf("where\n");
        System.out.printf("     <input_por>         Input SPSS/PSPP Portable file\n");
        System.out.printf("     <options>           Options. See the list below\n");
        System.out.printf("\n");
        System.out.printf("Options:\n");
        System.out.printf("\n");
        System.out.printf("  Verbosity:\n");
        System.out.printf("     -silent             Do not display any details\n");
        System.out.printf("     -vars               Display variable details\n");
        System.out.printf("     -labels             Display value-label details\n");
        System.out.printf("     -all                Display all details\n");
        System.out.printf("  Data output:\n");
        System.out.printf("     -output=<dest>      Write data matrix into <dest>\n");
        System.out.printf("     -method=<meth>      Visitor method used for writing\n");
        System.out.printf("  Visitor methods for <meth>:\n");
        System.out.printf("     none                Just visit and do not write output\n");
        System.out.printf("     string              Use Java\'s String.format()\n");
        System.out.printf("     array               Use the input array directly\n");
        System.out.printf("  Error management:\n");
        System.out.printf("     -debug              Display stack trace on error\n");
        System.out.printf("\n");
        System.out.printf("Notes:\n");
        System.out.printf("1) When multiple input files, only the last one\'s data is written\n");
        System.out.printf("2) Output file is ignored with method \"none\"\n");
        System.out.printf("3) Output file is optional with method \"string\"\n");
        
    }
    
    public static void main(String[] args) {
        if (args.length < 1) {
            usage();
        System.exit(EXIT_SUCCESS);
        }
        
        Options opt = new Options();
        
        // Parse command-line arguments
        try {
            parseArgs(args, opt);
        } catch(Exception ex) {
            // Parse error
            System.out.printf("%s\n", ex.getMessage());
            System.exit(EXIT_FAILURE);
        } // try-catch
        
        
        
        PORReader preader = new PORReader();
        PORFile por = null;
        
        for (String fname : opt.input_filenames) {
            System.out.printf("Reading file %s\n", fname);
            
            // Parse the file
            try {
                // Start timing
                long startTime = System.nanoTime();
                
                // ============== PARSING ==============
                por = preader.parse(fname);
                // =====================================
                
                // Finish timing
                long endTime = System.nanoTime();
    
                // Calculate time spent
                long duration = endTime - startTime;
                
                // Display the time spent
                System.out.printf("Spent %.2f seconds\n", duration/1.0e9);
            } catch(Exception ex) {
                // Parse error. Display more detailed error message
                System.out.printf("%s:%d:%d: %s", 
                    fname, preader.getRow(), preader.getColumn(),
                    ex.getMessage());
                
                // Optionally stack trace
                if (opt.debug_flag) {
                    ex.printStackTrace();
                } // if
                
                // Exit with failure
                System.exit(EXIT_FAILURE);
            } // try-catch
            
            // Reaching this point implies the file was parsed succesfully.
            if (opt.header_details_flag) {
                printOverview(por);
            }
            if (opt.variable_details_flag) {
                printVariables(por.variables);
            }
            if (opt.value_details_flag) {
                printValueLabels(por.labels);
            }
        } // for: input files
        
        // Do visiting and writing
        if (opt.data_output_method != Options.METH_UNDEFINED) {
            
            Writer w = null;
            
            // Determine first if the output method doesn't ignore
            // the output filename (ie. if just visit)
            if (opt.data_output_method == Options.METH_JUST_VISIT) {
                // Ignore the destination
                opt.data_output_filename = null;
            }
            
            // If output filename has been specified
            if (opt.data_output_filename != null) {
                // Notify user
                System.out.printf("Writing data to file %s\n", 
                    opt.data_output_filename);
                
                try {
                    File fout = new File(opt.data_output_filename);
                    
                    FileOutputStream fos 
                        = new FileOutputStream(fout);
                    
                    // TODO: Parametrize the encoding
                    OutputStreamWriter osw 
                        = new OutputStreamWriter(fos, "iso-8859-15");
                    
                    // TODO: Parametrize the buffer size
                    w = new BufferedWriter(osw, 0x20000); // 128 kb
                    
                } catch(Exception ex) {
                    // There was an error while opening the file
                    if (opt.debug_flag) {
                        ex.printStackTrace();
                    } else {
                        System.out.printf(
                            "Error: unable to create a writer for the file\n");
                    } // if-else
                    
                    // Exit with failure
                    System.exit(EXIT_FAILURE);
                } // try-catch
                
            } else {
                // Notify user
                System.out.printf("Traversing data matrix\n");
            } // if-else
            
            try {
                writeMatrix(w, por, opt.data_output_method);
                if (w != null) w.close();
            } catch(Exception ex) {
                // Silently close
                try {
                    if (w != null) w.close();
                } catch(IOException ignored) {
                } // try-catch
                
                ex.printStackTrace();
                System.exit(EXIT_FAILURE);
            } // try-catch-finally
        }
        
        /*
        A matrix with dimensions 2448 x 136
        takes 8 seconds to write out if PrintStream is used.
        However, if BufferedWriter chain is used, 
        it takes only about 2 seconds.
        
        A matrix with dimensions 30780 x 719 ( bytes)
        takes 10 seconds to parse,
        and 105.47 seconds to dump as CSV,
        resulting in 46 200 000 bytes,
        the speed is then about 440 000 bytes/s
        
        Increasing the buffer size to 128 kilobytes,
        did not affect the writing time.
        Without any actual Writer.write() operations,
        the time spent traversing and visiting the matrix
        takes 103 seconds, so no actual improvements in there.
        
        Removing the String.format() operations reduces the duration
        of serialization down to 7 seconds!
        
        After converting the writing operations to dump the data
        directly from the buffer, the time spent is 9 seconds.
        
        */
    } // main()
    
    abstract public static class AbstractMatrixWriter
        implements PORMatrixVisitor
    {
        
        // MEMBER VARIABLES
        //==================
        
        Writer out;
        
        // CONSTRUCTORS
        //==============
        
        public AbstractMatrixWriter(Writer writer) {
            out = writer;
        }
        
        // HELPERS
        //=========
        
        protected void write(String s) {
            if (out != null) {
                try {
                    out.write(s);
                } catch(IOException ex) {
                    throw new RuntimeException(ex);
                } // try-catch
            } // if
        } // write()
        
        protected void write(int c) {
            if (out != null) {
                try {
                    out.write(c);
                } catch(IOException ex) {
                    throw new RuntimeException(ex);
                } // try-catch
            } // if
        } // write()
        
        protected void write(byte[] array, int from, int to) {
            if (out != null) {
                try {
                    for (int i = from; i < to; i++) {
                        out.write(array[i]);
                    }
                } catch(IOException ex) {
                    throw new RuntimeException(ex);
                } // try-catch
            } // if
        }
        
        
        // INTERFACE IMPLEMENTATION
        //==========================
        
        @Override
        public void matrixBegin(int xdim, int ydim, int[] xtypes) {
        }
        
        @Override
        public void matrixEnd() {
        }
        
        @Override
        public void rowBegin(int y) {
        }
        
        @Override
        public void rowEnd(int y) {
            write('\n');
        } // rowEnd()
        
        @Override
        public abstract void columnSysmiss(int x, int len, byte[] data);
        
        @Override
        public abstract void columnNumeric(
            int x, int len, byte[] data, double value);
        
        @Override
        public abstract void columnString(int x, int len, byte[] data);
        
    } // abstract class AbstractMatrixWriter
    
    public static class MatrixOutputter
        extends AbstractMatrixWriter
    {
        // CONFIGURATION VARIABLES
        //=========================
        
        private String numfmt;
        private int meth;
        
        // CONSTRUCTOR
        //=============
        
        public MatrixOutputter(Writer writer, int method) {
            super(writer);
            out = writer;
            numfmt = "%f";
            meth = method;
        }
        
        
        // METHODS
        //=========
        
        
        @Override
        public void columnSysmiss(int x, int len, byte[] data) {
            if (x > 0) write(',');
        } // columnSysmiss()
        
        @Override
        public void columnNumeric(
            int x, int len, byte[] data, double value) 
        {
            // separator
            if (x > 0) write(',');
            
            // Select method
            if (meth == Options.METH_STRING_FORMAT) {
                
                String valstr = null;
                // Determine whether an integer or decimal
                int ivalue = (int) value;
                if (value == (double) ivalue) {
                    // Integer
                    valstr = String.format("%d", ivalue);
                } else {
                    // Decimal
                    valstr = String.format(Locale.ROOT, numfmt, value);
                } // if-else
                
                // Output the number
                write(valstr);
                
            } else if (meth == Options.METH_FROM_INPUT) {
                
                // Output the number
                write(data, 0, len);
                
            } else {
                // Don't write anything
            } // if-else
        } // columnNumeric()
        
        @Override
        public void columnString(int x, int len, byte[] data) {
            // separator
            if (x > 0) write(','); 
            
            // TODO: Optimize empty strings (len=1, content=ws)
            
            if (meth == Options.METH_STRING_FORMAT) {
                
                //String valstr = escapeString(new String(data, 0, len));
                String valstr = new String(data, 0, len);
                
                // Write string
                write('\"');
                write(valstr);
                write('\"');
                
            } else if (meth == Options.METH_FROM_INPUT) {
                
                // Write string
                write('\"');
                write(data, 0, len);
                write('\"');
                
            } else {
                // Ignore
            } // if-else
        } // columnString()
    } // class MatrixOutputter

        
    protected static String escapeString(String s) {
        int len = s.length();
        
        StringBuilder sb = null;
        int capacity = 0;
        
        for (int round = 0; round < 2; round++) {
            if (round == 1) {
                sb = new StringBuilder(capacity);
            } // if
            
            
            capacity = sbAppend(sb, '\"', capacity);
            for (int i = 0; i < len; i++) {
                char c = s.charAt(i);
                if ((c == '\"') 
                    || (c == '\'')
                    || (c == '\\')
                    || (c == '\t')
                    || (c == '\n')
                    || (c == '\r'))
                {
                    // Escape the following character
                    capacity = sbAppend(sb, '\\', capacity);
                } // if: needs escaping
                
                // Translation here
                if (c == '\t') {
                    c = 't';
                } else if (c == '\n') {
                    c = 'n';
                } else if (c == '\r') {
                    c = 'r';
                }
                
                // Append the actual character
                capacity = sbAppend(sb, c, capacity);
            } // for: each char
            
            capacity = sbAppend(sb, '\"', capacity);
        } // for: two rounds
        
        return sb.toString();
    } // escapeString()
    
    private static int sbAppend(
        StringBuilder sb,
        char c,
        int len
    ) {
        if (sb != null) {
            sb.append(c);
        }
        
        return len+1;
    } // stringAppend()
    
} // class PORDump
