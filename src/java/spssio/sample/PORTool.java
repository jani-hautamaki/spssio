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



package spssio.sample;


// core java
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Vector;
import java.util.Locale; // for locale-independent formatting

// for csv output
import java.io.FileOutputStream;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.OutputStreamWriter;

// spssio por
import spssio.por.PORSection;
import spssio.por.PORMissingValue;
import spssio.por.PORValueLabels;
import spssio.por.PORVariable;
import spssio.por.PORValue;
import spssio.por.PORMatrix;
import spssio.por.PORMatrixVisitor;
import spssio.por.PORHeader;
import spssio.por.PORFile;
// spssio por parser
import spssio.por.input.PORReader;

// for MatrixConverter
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

public class PORTool {

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
        public static final int METH_TOSTRING           = 2;
        public static final int METH_OBJECT_ARRAY       = 3;
        public static final int METH_FROM_INPUT         = 4;
        public static final int METH_RECTANGLE          = 5;

        public boolean debug_flag                       = false;

        public boolean header_details_flag              = true;
        public boolean variable_details_flag            = false;
        public boolean value_details_flag               = false;
        public boolean section_details_flag             = false;

        public Vector<String> input_filenames
            = new Vector<String>();

        public String data_output_filename              = null;
        public int data_output_method                   = METH_UNDEFINED;

        public int status_ysteps                        = 0;
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
    public static void printPORHeader(PORFile por) {
        String s = null;

        // These are mandatory
        System.out.printf("Signature:           \"%s\"\n", por.header.signature);
        System.out.printf("Version:             \'%c\'\n", por.header.version);
        System.out.printf("Creation date:       \"%s\"\n", por.header.date);
        System.out.printf("Creation time:       \"%s\"\n", por.header.time);

        // Software field is optional
        System.out.printf("Software:            %s\n", val2str(por.software));
        // Author field is optional
        System.out.printf("Author:              %s\n", val2str(por.author));
        // Title field is optional
        System.out.printf("Title:               %s\n", val2str(por.title));

        // These are mandatory, but in theory they could be missing.
        System.out.printf("Variable count:      %d\n", por.variableCount);
        System.out.printf("Precision:           %d base-30 digits\n", por.precision);

        // Weight variable name is optional
        System.out.printf("Weight variable:     %s\n", val2str(por.weightVariableName));

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

    public static void printSections(Vector<PORSection> sections) {
        System.out.printf("Portable file structure / sections:\n");
        int count = 0;
        String summary = null;
        System.out.printf("      #         tag summary\n");
        for (PORSection section : sections) {
            int tag = section.tag;
            switch(tag) {
                case PORSection.TAG_SOFTWARE:
                    summary = "Software";
                    break;
                case PORSection.TAG_AUTHOR:
                    summary = "Author";
                    break;
                case PORSection.TAG_TITLE:
                    summary = "Title";
                    break;
                case PORSection.TAG_VARIABLE_COUNT:
                    summary = "Variable count";
                    break;
                case PORSection.TAG_PRECISION:
                    summary = "Precision";
                    break;
                case PORSection.TAG_WEIGHT_VARIABLE:
                    summary = "Weight variable name";
                    break;
                case PORSection.TAG_VARIABLE_RECORD:
                    summary = String.format("Variable record: %s",
                        ((PORVariable) section.obj).getName());
                    break;
                case PORSection.TAG_MISSING_DISCRETE:
                    summary = "Missing value; discrete";
                    break;
                case PORSection.TAG_MISSING_OPEN_LO:
                    summary = "Missing value; open range low";
                    break;
                case PORSection.TAG_MISSING_OPEN_HI:
                    summary = "Missing value; open range high";
                    break;
                case PORSection.TAG_MISSING_RANGE:
                    summary = "Missing value; closed range";
                    break;
                case PORSection.TAG_VARIABLE_LABEL:
                    summary = "Variable label";
                    break;
                case PORSection.TAG_VALUE_LABELS:
                    summary = String.format("Value-label mappings: %d variables, %d pairs",
                        ((PORValueLabels) section.obj).vars.size(),
                        ((PORValueLabels) section.obj).mappings.size()
                    );
                    break;
                case PORSection.TAG_DOCUMENTS_RECORD:
                    summary = "Documents record";
                    break;
                case PORSection.TAG_DATA_MATRIX:
                    summary = "Data matrix";
                    break;
                default:
                    summary = "UNKNOWN";
                    break;
            }

            count++;
            System.out.printf("   %4d           %c %s\n",
                count, section.tag, summary);
        } // for: each section
    } // printSections()

    public static void printOverview(PORFile por) {
        PORHeader header = por.header;
        System.out.printf("Portable file contents\n");

        System.out.printf("\n");
        System.out.printf("Header:\n");
        printPORHeader(por);

        System.out.printf("\n");
        System.out.printf("Data matrix overview:\n");
        System.out.printf("Dimensions:          %d x %d (cases x variables)\n", por.data.sizeY(), por.data.sizeX());
        System.out.printf("Size:                %d bytes\n", por.data.sizeBytes());

        // Count the data types

        int[] types = por.data.getColumnLayout();
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
        System.out.printf("\n");
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

    public static void writeMatrix(
        Writer out,
        PORFile por,
        Options opt
    )
        throws IOException
    {
        // This is done differently.
        if (opt.data_output_method == Options.METH_OBJECT_ARRAY) {
            writeObjectsArray(out, por, opt);
            return;
        } // if:


        if (out != null) {
            writeHeader(out, por);
        }

        // Create the visitor
        MatrixOutputter visitor = new MatrixOutputter(
            out,
            opt.data_output_method,
            opt.status_ysteps
        );

        // Start timing
        long startTime = System.nanoTime();

        // ========== VISIT ==========
        por.data.accept(visitor);
        // ===========================

        // Stop timing
        long endTime = System.nanoTime();

        // Calculate duration
        long duration = endTime - startTime;

        // Notify
        System.out.printf("Spent %.2f seconds\n", duration/1.0e9);

    } // writeMatrix()

    public static void writeObjectsArray(
        Writer out,
        PORFile por,
        Options opt
    )
        throws IOException
    {

        // Create the visitor
        MatrixConverter converter = new MatrixConverter(opt.status_ysteps);

        // Start timing
        long startTime = System.nanoTime();

        // ========== VISIT ==========
        por.data.accept(converter);
        // ===========================

        // Stop timing
        long endTime = System.nanoTime();

        // Calculate duration
        long duration = endTime - startTime;

        // Notify
        System.out.printf("Spent %.2f seconds in converting\n", duration/1.0e9);

        // SERIALIZE
        //===========

        if (out != null) {
            // Write header as usual
            writeHeader(out, por);

            int xdim = por.data.sizeX();
            int ydim = por.data.sizeY();
            // Pop the array out of the converter.
            Object[] array = converter.popArray();

            // Start timing
            startTime = System.nanoTime();

            int offset = 0;
            for (int y = 0; y < ydim; y++) {
                for (int x = 0; x < xdim; x++) {
                    if (x > 0) out.write(',');
                    Object obj = array[offset++];
                    if (obj != null) {
                        out.write(obj.toString());
                    }
                } // for: x
                out.write('\n');
            } // for: y

            // End timing and calculate duration
            endTime = System.nanoTime();
            duration = endTime - startTime;

            // Display results
            System.out.printf("Spent %.2f seconds in writing\n", duration/1.0e9);
        } // if: out
    }

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
            if (carg.equals("-help")) {
                usage();
                System.exit(EXIT_SUCCESS);
            }
            else if (carg.equals("-silent")) {
                opt.header_details_flag     = false;
                opt.variable_details_flag   = false;
                opt.value_details_flag      = false;
                opt.section_details_flag    = false;
            }
            else if (carg.equals("-all")) {
                opt.header_details_flag     = true;
                opt.variable_details_flag   = true;
                opt.value_details_flag      = true;
                opt.section_details_flag    = true;
            }
            else if (carg.equals("-vars")) {
                opt.variable_details_flag   = true;
            }
            else if (carg.equals("-labels")) {
                opt.value_details_flag      = true;
            }
            else if (carg.equals("-sections")) {
                opt.section_details_flag    = true;
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
                else if (s.equals("tostring")) {
                    val = Options.METH_TOSTRING;
                }
                else if (s.equals("object")) {
                    val = Options.METH_OBJECT_ARRAY;
                }
                else if (s.equals("direct")) {
                    val = Options.METH_FROM_INPUT;
                }
                else {
                    parse_error("Unrecognized method: \"%s\"", s);
                } // if-else
                opt.data_output_method = val;
            }
            else if (carg.startsWith("-ysteps=")) {
                String s = carg.substring(carg.indexOf('=')+1);
                try {
                    opt.status_ysteps = Integer.parseInt(s);
                } catch(NumberFormatException ex) {
                    parse_error("Not an integer: %s\n", s);
                }
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
        System.out.printf("Usage:\n");
        System.out.printf("\n");
        System.out.printf("     PORDump <input_por> [<input_por2> ...] [<options>]\n");
        System.out.printf("\n");
        System.out.printf("where\n");
        System.out.printf("\n");
        System.out.printf("     <input_por>         Input SPSS/PSPP Portable file\n");
        System.out.printf("     <options>           Options. See the list below\n");
        System.out.printf("\n");
        System.out.printf("Options:\n");
        System.out.printf("\n");
        System.out.printf("  Verbosity:\n");
        System.out.printf("     -silent             Do not display any details\n");
        System.out.printf("     -vars               Display variable details\n");
        System.out.printf("     -labels             Display value-label details\n");
        System.out.printf("     -sections           Display parsed sections in order\n");
        System.out.printf("     -all                Display all details\n");
        System.out.printf("     -ysteps=<int>       If -output, display status <int> times\n");
        System.out.printf("  Data output:\n");
        System.out.printf("     -output=<dest>      Write data matrix into <dest>\n");
        System.out.printf("     -method=<meth>      Visitor method used for writing\n");
        System.out.printf("  Visitor methods for <meth>:\n");
        System.out.printf("     none                Just visit and do not write output\n");
        System.out.printf("     string              Use String.format()\n");
        System.out.printf("     tostring            Use Integer.toString() and Double.toString()\n");
        System.out.printf("     object              Convert data into Java Objects before writing\n");
        System.out.printf("     direct              Use the input byte array directly\n");
        System.out.printf("  Error management:\n");
        System.out.printf("     -debug              Display stack trace on error\n");
        System.out.printf("\n");
        System.out.printf("Notes:\n");
        System.out.printf("  1) When multiple input files, only the last one\'s data is written\n");
        System.out.printf("  2) Output file is ignored with method \"none\"\n");
        System.out.printf("  3) Output file is optional with method \"string\"\n");
        System.out.printf("\n");
        System.out.printf("PORDump (C) 2013 Jani Hautamaki <jani.hautamaki@hotmail.com>\n");

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
                System.out.printf("%s:%d:%d: %s\n",
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
            if (opt.section_details_flag) {
                printSections(por.sections);
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

                writeMatrix(w, por, opt);

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

        /**
         * For writing the output
         */
        protected Writer out;

        /* These member variables are for status display and gc */

        /**
         * Total number of rows; this is required for the progress indicator.
         */
        private int sizey;

        /**
         * Next row which triggers the status display
         */
        private int nexty;

        /**
         * Step size for {@code nexty}.
         */
        private int stepy;

        /**
         * How many times the status is displayed during the visiting.
         */
        private int steps;

        /**
         * For retrieving the heap status.
         */
        private MemoryMXBean mem_bean;


        // CONSTRUCTORS
        //==============

        public AbstractMatrixWriter(Writer writer) {
            out = writer;
            // Status display variables
            steps = 0;
            sizey = 0;
            nexty = 0;
            stepy = 0;
            mem_bean = ManagementFactory.getMemoryMXBean();
        } // default ctor

        public AbstractMatrixWriter(Writer writer, int ysteps) {
            this(writer);
            steps = ysteps;
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

        // STATUS DISPLAY
        //================

        protected void printStatusHeaders() {
            System.out.printf("progress        row           used    commit       max\n");
        }

        protected void printStatusLine(int y) {
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


        // INTERFACE IMPLEMENTATION
        //==========================

        @Override
        public void matrixBegin(int xdim, int ydim, int[] xtypes) {

            // Init status display
            if (steps > 0) {
                sizey = ydim;
                stepy = sizey / steps;
                nexty = 0;
            } // if: status enabled
        }

        @Override
        public void matrixEnd() {
            if (steps > 0) {
                printStatusLine(sizey);
            }
        }

        @Override
        public void rowBegin(int y) {
            if ((steps > 0) && (y >= nexty)) {
                // Display status
                printStatusLine(y);
                nexty += stepy;
            }
        }

        @Override
        public void rowEnd(int y) {
            // dummy
        }

        @Override
        public abstract void columnSysmiss(
            int x,
            byte[] data,
            int len
        );

        @Override
        public abstract void columnNumeric(
            int x,
            byte[] data,
            int len,
            double value
        );

        @Override
        public abstract void columnString(
            int x,
            byte[] data,
            int len,
            int offset
        );
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
            numfmt = "%g";
            meth = method;
        }

        public MatrixOutputter(Writer writer, int method, int ysteps) {
            super(writer, ysteps);
            numfmt = "%g";
            meth = method;
        }


        // METHODS
        //=========

        @Override
        public void rowEnd(int y) {
            write('\n');
        }

        @Override
        public void columnSysmiss(
            int x,
            byte[] data,
            int len
        ) {
            if (x > 0) write(',');
        } // columnSysmiss()

        @Override
        public void columnNumeric(
            int x,
            byte[] data,
            int len,
            double value
        ) {
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
            } else if (meth == Options.METH_TOSTRING) {
                String valstr = null;
                // Determine whether an integer or decimal
                int ivalue = (int) value;
                if (value == (double) ivalue) {
                    // Integer
                    valstr = Integer.toString(ivalue);
                } else {
                    // Decimal
                    valstr = Double.toString(value);
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
        public void columnString(
            int x,
            byte[] data,
            int base,
            int len
        ) {
            // separator
            if (x > 0) write(',');

            // Optimize empty strings (len=1, content=ws)
            if (((len-base) == 1) && (data[base] == ' ')) {
                // No quotes for empty strings.
                return;
            }

            if ((meth == Options.METH_STRING_FORMAT)
                || (meth == Options.METH_TOSTRING))
            {

                //String valstr = escapeString(new String(data, 0, len));
                String valstr = new String(data, base, len-base);

                // Write string
                write('\"');
                write(valstr);
                write('\"');

            } else if (meth == Options.METH_FROM_INPUT) {

                // Write string
                write('\"');
                write(data, base, len);
                write('\"');

            } else {
                // Ignore
            } // if-else
        } // columnString()
    } // class MatrixOutputter

    public static class MatrixConverter
        extends AbstractMatrixWriter
    {

        // MEMBER VARIABLES
        //==================

        private Object[] array;
        private int offset;

        // CONSTRUCTORS
        //==============

        public MatrixConverter(int ysteps) {
            super(null, ysteps);
            array = null;
            offset = 0;
        }

        // RETRIEVAL
        //===========

        public Object[] popArray() {
            Object[] rval = array;
            array = null;
            offset = 0;
            return rval;
        } // popArray()


        // INTERFACE IMPLEMENTATION
        //==========================

        @Override
        public void matrixBegin(int xdim, int ydim, int[] xtypes) {
            super.matrixBegin(xdim, ydim, xtypes);

            // Allocate properly sized array
            array = new Object[xdim*ydim];
        }

        @Override
        public void columnSysmiss(
            int x,
            byte[] data,
            int len
        ) {
            array[offset++] = null;
        }

        @Override
        public void columnNumeric(
            int x,
            byte[] data,
            int len,
            double value
        ) {
            // Determine whether integer or double
            int ivalue = (int) value;
            if (value == (double) ivalue) {
                array[offset++] = new Integer(ivalue);
            } else {
                array[offset++] = new Double(value);
            }
        }

        @Override
        public void columnString(
            int x,
            byte[] data,
            int base,
            int len
        ) {

            // Optimize empty strings (len=1, content=ws)
            if (((len-base) == 1) && (data[base] == ' ')) {
                // null value
                array[offset++] = null;
                return;
            } // if: empty string

            // Do some hacking; enclose the data into quotes.
            // This is possible, since there's always at least 2 chars
            // preceding the base (length is at least 1 char, and slash
            // is the second char). Also, the data[] array has at least
            // twice the size of maximum string allowed.
            data[--base] = '\"';
            data[len++] = '\"';

            array[offset++] = new String(data, base, len-base);
        }
    } // class MatrixConverter



    /*
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
    */

} // class PORDump
