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

public class PORDump
{
    
    public static final int EXIT_SUCCESS = 0;
    public static final int EXIT_FAILURE = 1;
    
    
    
    
    
    public static void printPortable(PORFile por) {
        PORHeader header = por.header;
        System.out.printf("Portable file contents\n");
        System.out.printf("  Header:\n");
        System.out.printf("     Signature:          \"%s\"\n", header.signature);
        System.out.printf("     Version:            \'%c\'\n", header.version);
        System.out.printf("     Creation date:      \"%s\"\n", header.date);
        System.out.printf("     Creation time:      \"%s\"\n", header.time);
        
        if (header.software != null) {
        System.out.printf("     Software:           \"%s\"\n", header.software);
        } else {
        System.out.printf("     Software:           <unset>\n");
        } // if-else
        
        if (header.author != null) {
        System.out.printf("     Author:             \"%s\"\n", header.author);
        } else {
        System.out.printf("     Author:             <unset>\n");
        } // if-else

        if (header.title != null) {
        System.out.printf("     Title:              \"%s\"\n", header.title);
        } else {
        System.out.printf("     Title:              <unset>\n");
        } // if-else

        System.out.printf("     # of variables:     %d\n", header.nvariables);
        
        System.out.printf("     Precision:          %d base-30 digits\n", header.precision);
        
        if (header.weight_var_name != null) {
        System.out.printf("     Weight variable:    \"%s\"\n", header.weight_var_name);
        } else {
        System.out.printf("     Weight variable:    <unset>\n");
        } // if-else
        
        System.out.printf("     Data matrix:        %d x %d\n", por.data.sizey(), por.data.sizex());
        System.out.printf("     Data size:          %d bytes\n", por.data.size());
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
        System.out.printf("     Numeric variables:  %d\n", numeric_columns);
        System.out.printf("     String variables:   %d\n", string_columns);
        
        int size = por.variables.size();
        System.out.printf("  Variables (%d):\n", size);
        
        for (int i = 0; i < size; i++) {
        PORVariable cvar = por.variables.elementAt(i);
        System.out.printf("     Name:               \"%s\"\n", cvar.name);
        System.out.printf("     Width:              %d\n", cvar.width);
            
        if (cvar.label != null) {
        System.out.printf("     Label:              \"%s\"\n", cvar.label);
        } else {
        System.out.printf("     Label:              <unsert>\n");
        } // if-else
        
        System.out.printf("     Print fmt:          %d / %d / %d\n",
            cvar.printfmt.type, cvar.printfmt.width, cvar.printfmt.decimals);
        System.out.printf("     Wrint fmt:          %d / %d / %d\n",
            cvar.writefmt.type, cvar.writefmt.width, cvar.writefmt.decimals);
        
        System.out.printf("     Missing values (%d)\n", cvar.missvalues.size());
        for (int j = 0; j < cvar.missvalues.size(); j++) {
            PORMissingValue miss = cvar.missvalues.elementAt(j);
            String s = null;
            switch(miss.type) {
                case PORMissingValue.TYPE_DISCRETE_VALUE:
                    s = String.format(
                        "discrete: %s", 
                        miss.values[0].value);
                    break;
                case PORMissingValue.TYPE_RANGE_OPEN_LO:
                    s = String.format(
                        "range:    --%s",
                        miss.values[0].value);
                    break;
                case PORMissingValue.TYPE_RANGE_OPEN_HI:
                    s = String.format(
                        "range:    %s--",
                        miss.values[0].value);
                    break;
                case PORMissingValue.TYPE_RANGE_CLOSED:
                    s = String.format(
                        "range:    %s--%s",
                        miss.values[0].value,
                        miss.values[1].value);
                    break;
                
                default:
                    s = "????? error";
                    break;
                
            } // switch
            System.out.printf("         %s\n", s);
        } // for: missing values
        
        } // for: variables
        
        size = por.labels.size();
        
        System.out.printf("  Value labels lists (%d):\n", size);
        for (int i = 0; i < size; i++) {
            PORValueLabels labels = por.labels.elementAt(i);
            
        System.out.printf("     Variables:   %d\n", labels.vars.size());
        for (int j = 0; j < labels.vars.size(); j++) {
        System.out.printf("         \"%s\"\n", labels.vars.elementAt(j).name);
        } // for: variables
        System.out.printf("     Pairs:       %d\n", labels.mappings.size());
        for (Map.Entry<PORValue, String> entry : labels.mappings.entrySet()) {
        System.out.printf("          \"%s\" : \"%s\"\n",
            entry.getKey().value, entry.getValue());
        } // for: each mapping
        
        } // for: value labels lists
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
            por = preader.parse(curFilename);
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
                
                w = new BufferedWriter(osw);
                
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
            try {
                out.write('\n');
            } catch(IOException ex) {
                throw new RuntimeException(ex);
            }
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
            
            String valstr = null;
            int ivalue = (int) value;
            if (value == (double) ivalue) {
                // an integer
                valstr = String.format("%d", ivalue);
            } else {
                // decimal
                valstr = String.format(Locale.ROOT, numfmt, value);
            } // if-else
            
            try {
                if (x > 0) out.write(',');
                // print the value
                out.write(valstr);
            } catch(IOException ex) {
                throw new RuntimeException(ex);
            }
        } // columnNumeric()
        
        @Override
        public void columnString(int x, int len, byte[] data) {
            // TODO: Optimize empty strings (len=1, content=ws)
            
            String valstr = escapeString(new String(data, 0, len));

            try {
                if (x > 0) out.write(','); 
                out.write(valstr);
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
