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
        
        
        
    }
    
    
    public static void printPortable(PORFile por) {
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

        // If variable details are not desired. skip this
        System.out.printf("\n");
        printVariables(por.variables);

        
        System.out.printf("\n");
        printValueLabels(por.labels);
    }
    
    public static void writeMatrix(Writer out, PORFile por) {
        // The first line is variable names
        int nvars = por.variables.size();
        try {
            for (int i = 0; i < nvars; i++) {
                if (i > 0) {
                    out.write(',');
                }
                out.write(por.variables.elementAt(i).name);
            }
            out.write('\n');
        } catch(IOException ex) {
            throw new RuntimeException(ex);
        }
        
        MatrixOutputter visitor = new MatrixOutputter(out);

        
        long startTime = System.nanoTime();
        por.data.visit(visitor);
        long endTime = System.nanoTime();

        long duration = endTime - startTime;
        
        System.out.printf("Spent %.2f seconds\n", duration/1.0e9);
    } // writeMatrix()
    
    
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.printf("Usage: PORDump <input_por> [<dump_text_file>] [<data_csv>]\n");
            System.out.printf("\n");
            System.out.printf("where\n");
            System.out.printf("     <input_por>         input Portable file\n");
            System.out.printf("     <dump_text_file>    output text file\n");
            System.out.printf("     <data_csv>          output csv file for data\n");
            System.exit(EXIT_SUCCESS);
        }
        
        String curFilename = args[0]; 
        
        PORReader preader = new PORReader();
        PORFile por = null;
        
        try {
            System.out.printf("%s\n", curFilename);
            
            long startTime = System.nanoTime();
            por = preader.parse(curFilename);
            long endTime = System.nanoTime();
    
            long duration = endTime - startTime;
            System.out.printf("Spent %.2f seconds\n", duration/1.0e9);
            
        } catch(Exception ex) {
            System.out.printf("%s:%d:%d: %s", 
                curFilename, preader.getRow(), preader.getColumn(),
                ex.getMessage());
            ex.printStackTrace();
            System.exit(EXIT_FAILURE);
        } // try-catch
        
        if (args.length >= 2) {
            // Otherwise write to a file
        } else {
            // Print information to the screen
            printPortable(por);
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
        if (args.length >= 3) {
            Writer w = null;
            
            System.out.printf("Writing data to file %s\n", args[2]);
            try {
                File fout = new File(args[2]);
                
                FileOutputStream fos 
                    = new FileOutputStream(fout);
                
                OutputStreamWriter osw 
                    = new OutputStreamWriter(fos, "iso-8859-15");
                
                w = new BufferedWriter(osw, 0x20000); // 128 kb
                
            } catch(Exception ex) {
                ex.printStackTrace();
                System.exit(EXIT_FAILURE);
            } // try-catch
            
            try {
                writeMatrix(w, por);
                w.close();
            } catch(Exception ex) {
                // Silently close
                try {
                    w.close();
                } catch(IOException ignored) {
                } // try-catch
                
                ex.printStackTrace();
                System.exit(EXIT_FAILURE);
            } // try-catch-finally
            
        } else {
            // Do nothing
        } // if-else
        
    } // main()
    
    
    public static class MatrixOutputter
        implements PORMatrixVisitor
    {
        // CONFIGURATION VARIABLES
        //=========================
        
        Writer out;
        String numfmt;
        
        // CONSTRUCTOR
        //=============
        
        public MatrixOutputter(Writer writer) {
            out = writer;
            numfmt = "%f";
        }
        
        
        // METHODS
        //=========
        
        @Override
        public void matrixBegin(int xdim, int ydim, int[] xtypes) {
            // do nothing
        }
        
        @Override
        public void matrixEnd() {
            // do nothing
        }
        
        @Override
        public void rowBegin(int y) {
            // do nothing
        }
        
        @Override
        public void rowEnd(int y) {
            /*
            try {
                out.write('\n');
            } catch(IOException ex) {
                throw new RuntimeException(ex);
            }
            */
        }
        
        @Override
        public void columnSysmiss(int x, int len, byte[] data) {
            
            try {
                if (x > 0) out.write(','); 
            } catch(IOException ex) {
                throw new RuntimeException(ex);
            }
            
        } // columnSysmiss()
        
        @Override
        public void columnNumeric(
            int x, int len, byte[] data, double value) 
        {
            // Numeric; determine if it an integer?
            
            /*
            String valstr = null;
            int ivalue = (int) value;
            if (value == (double) ivalue) {
                // an integer
                valstr = String.format("%d", ivalue);
            } else {
                // decimal
                valstr = String.format(Locale.ROOT, numfmt, value);
            } // if-else
            */
            
            try {
                if (x > 0) out.write(',');
                // print the value
                //out.write(valstr);
                for (int i = 0; i < len; i++) {
                    out.write((int) data[i]);
                }
            } catch(IOException ex) {
                throw new RuntimeException(ex);
            }
            
        } // columnNumeric()
        
        @Override
        public void columnString(int x, int len, byte[] data) {
            // TODO: Optimize empty strings (len=1, content=ws)
            
            //String valstr = escapeString(new String(data, 0, len));

            
            try {
                if (x > 0) out.write(','); 
                //out.write(valstr);
                out.write('\"');
                for (int i = 0; i < len; i++) {
                    out.write((int) data[i]);
                }
                out.write('\"');
            } catch(IOException ex) {
                throw new RuntimeException(ex);
            }
            
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
