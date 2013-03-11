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

// spssio por
import spssio.por.PORMissingValue;
import spssio.por.PORValueLabels;
import spssio.por.PORVariable;
import spssio.por.PORValue;
import spssio.por.PORHeader;
import spssio.por.PORFile;

import spssio.por.input.PORReader;

public class PORDump
{
    
    
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
    
    
    
    public static void main(String[] args) {
        String curFilename = null; 
        PORReader preader = new PORReader();
        try {
            
            for (int i = 0; i < args.length; i++) {
                curFilename = args[i];
                System.out.printf("%s\n", curFilename);
                PORFile por = null;
                por = preader.parse(args[i]);
                
                printPortable(por);
            }
        } catch(Exception ex) {
            System.out.printf("%s:%d:%d: %s", 
                curFilename, preader.getRow(), preader.getColumn(),
                ex.getMessage());
            ex.printStackTrace();
        } // try-catch
    } // main()
    
} // class PORDump
