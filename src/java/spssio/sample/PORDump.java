//*******************************{begin:header}******************************//
//                 spssio - http://code.google.com/p/spssio/                 //
//***************************************************************************//
//
//      Java classes for reading and writing
//      SPSS/PSPP Portable and System files
//
//      Copyright (C) 2013-2014 Jani Hautamaki <jani.hautamaki@hotmail.com>
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
import java.util.Vector;
import java.util.Map;
import java.util.HashMap;

// spssio
import spssio.por.input.PORReader;
import spssio.por.PORFile;
import spssio.por.PORHeader;
import spssio.por.PORVariable;
import spssio.por.PORMissingValue;
import spssio.por.PORValueLabels;
import spssio.por.PORValue;
import spssio.por.PORSection;

public class PORDump {

    /**
     * Exit code for succesful execution.
     */
    public static final int EXIT_SUCCESS = 0;

    /**
     * Exit code for failed execution.
     */
    public static final int EXIT_FAILURE = 1;

    // MAIN
    //======

    public static void main(String[] args) {

        if (args.length < 1) {
            usage();
            System.exit(EXIT_SUCCESS);
        }

        String fname = args[0];
        System.out.printf("%s\n", fname);

        PORReader porReader = new PORReader();
        PORFile por = null;

        try {
            // Start timing
            long startTime = System.nanoTime();

            // ============== PARSING ==============
            por = porReader.parse(fname);
            // =====================================

            // Finish timing
            long endTime = System.nanoTime();

            // Calculate time spent
            long duration = endTime - startTime;

            // Display the time spent
            System.out.printf("Spent %.2f seconds\n", duration/1.0e9);

        } catch(Exception ex) {
            // Display more detailed error message
            System.out.printf("%s:%d: Column %d: %s\n",
                fname,
                porReader.getRow(),
                porReader.getColumn(),
                ex.getMessage()
            );

            // Dump the stack trace if debugging enabled
            ex.printStackTrace();

            //System.out.printf("Attempting to display the por file anyway\n");
            //displayPORFile(savReader.getLatestSAVFile(), savReader);

            System.exit(EXIT_FAILURE);
        } // try-catch

        // Display the contents of the Portable file.
        displayPORFile(por);

        System.exit(EXIT_SUCCESS);
    } // main()

    public static void usage() {
    }

    public static void displayPORFile(PORFile por) {
        displayPORHeader(por.header);

        System.out.printf("Various:\n");
        displayVarious(por);

        displayVariables(por.variables);

        //displayValueLabelMaps(por.labels);

        displaySections(por.sections);

    }

    public static void displayPORHeader(PORHeader header) {
        displaySplashStrings(header.splash);

        displayTranslation(header.translation);

        System.out.printf("Header:\n");
        System.out.printf("  Signature:                 %s\n", header.signature);
        System.out.printf("  Version:                   %c\n", header.version);
        System.out.printf("  Date:                      %s\n", header.date);
        System.out.printf("  Time:                      %s\n", header.time);

    }

    public static void displaySplashStrings(byte[] splash) {
        System.out.printf("Splash strings (bytes outside 0x20-0x7F are escaped):\n");
        for (int i = 0; i < 5; i++) {
            int base = i * 40;
            System.out.printf("  \"");
            int screenOffset = 3;

            for (int offset = 0; offset < 40; offset++) {
                // By default the length of a char in the screen is 1 char
                int screenWidth = 1;
                // unsigned conversion byte->int
                int membyte = ((int) splash[base+offset]) & 0xff;
                if ((membyte >= 020) && (membyte <= 0x7F)) {
                    System.out.printf("%c", membyte);
                } else {
                    //System.out.printf("?");
                    System.out.printf("\\u%04x", membyte);
                    screenWidth = 6; // <backslash>'u'<digit x4>
                }

                screenOffset += screenWidth;
                if (screenOffset > 45) {
                    System.out.printf("\n   "); // continuation line
                    screenOffset = 2;
                }
            }
            System.out.printf("\"\n");
        }
    }

    public static void displayTranslation(byte[] translation) {
        System.out.printf("Translation table (bytes outside 0x20-0x7F are escaped):\n");

        System.out.printf("  ");
        for (int i = 0; i < translation.length; i++) {
            if ((i > 0) && ((i % 32) == 0)) {
                System.out.printf("\n");
                System.out.printf("  ");
            }
            // Unsigned byte->int conversion
            int diskbyte = ((int) translation[i]) & 0xff;

            if ((diskbyte >= 020) && (diskbyte <= 0x7F)) {
                System.out.printf("%c", diskbyte);
            } else {
                System.out.printf("\\u%04x", diskbyte);
            }
        }
        System.out.printf("\n");
    }

    public static String maybeNull(String value) {
        StringBuilder sb = new StringBuilder(128);
        if (value == null) {
            sb.append("(unset)");
        } else {
            sb.append(String.format("\"%s\"", value));
        }

        return sb.toString();
    }

    public static void displayVarious(PORFile por) {
        // tag 1
        System.out.printf("  Software:                  %s\n", maybeNull(por.software));
        // tag 2
        System.out.printf("  Author:                    %s\n", maybeNull(por.author));
        // tag 3
        System.out.printf("  Title:                     %s\n", maybeNull(por.title));
        // tag 4
        System.out.printf("  Number of variables:       %d\n", por.variableCount);
        // tag 5
        System.out.printf("  Precision:                 %d\n", por.precision);
        // tag 6
        System.out.printf("  Weight variable name:      %s\n", maybeNull(por.weightVariableName));
    }


    public static void displayVariables(Vector<PORVariable> variables) {
        int num = 0;
        for (PORVariable v : variables) {
            System.out.printf("Variable #%d\n", num);
            displayPORVariable(v);
            num++;
        }
    }

    public static void displayPORVariable(PORVariable v) {
        System.out.printf("  Name:                      \"%s\"\n", v.name);
        System.out.printf("  Width:                     %d\n", v.width);
        System.out.printf("  Output format:             %d / %d / %d\n", v.printfmt.type, v.printfmt.width, v.printfmt.decimals);
        System.out.printf("  Input format:              %d / %d / %d\n", v.writefmt.type, v.writefmt.width, v.writefmt.decimals);
        int num = 0;
        for (PORMissingValue missing : v.missvalues) {
            num++;
        }
        System.out.printf("  Label:                     %s\n", maybeNull(v.label));
    }

    public static void displayValueLabelMaps(
        Vector<PORValueLabels> maps
    ) {
        int num = 0;
        for (PORValueLabels vls : maps) {
            System.out.printf("Value-Label Map #%d\n", num);
            displayPORValueLabels(vls);
            num++;
        }
    }

    public static void displayPORValueLabels(PORValueLabels vls) {
        System.out.printf("  Variables affected:\n");
        displayVariableList(vls.vars); // TODO
        System.out.printf("  Value-Label mappings:\n");
        displayVLMap(vls.mappings, vls.type); // TODO
    }

    public static void displayVariableList(Vector<PORVariable> list) {
        int count = 0;
        for (PORVariable v : list) {
            if (count != 0) {
                System.out.printf("   ");
            } else {
                System.out.printf("    ");
            }
            System.out.printf("%-8s", v.name);

            count++;
            if (count == 6) {
                System.out.printf("\n");
                count = 0;
            }
        }
        if (count != 0) {
            System.out.printf("\n");
        }
    }

    public static void displayVLMap(Map<PORValue, String> map, int type) {
        int count = 0;

        for (Map.Entry<PORValue, String> e : map.entrySet()) {
            count++;

            PORValue value = e.getKey();
            String label =  e.getValue();

            String valueString = null;
            // TODO
            /*
            switch(type) {
                case PORValue.TYPE_NUMERIC:
                    valueString = Double.toString(value.getDouble());
                    break;
                case PORValue.TYPE_STRING:
                    valueString = value.getString();
                    break;
                case PORValue.TYPE_RAW:
                    throw new RuntimeException(String.format(
                        "Value-Label map has uninterpreted values"));
            } // switch

            System.out.printf("    %-8s    %s\n", valueString, label);
            */
        }
    }

   public static void displaySections(Vector<PORSection> sections) {
        int num = 0;
        for (PORSection sect : sections) {
            String name = getPORSectionName(sect);
            System.out.printf(" #%-3d    %c      %s\n", num, sect.tag, name);
            num++;
        }
    }

    public static Map<Integer, String> s_porSectionNames = null;

    public static String getPORSectionName(PORSection section) {
        if (s_porSectionNames == null) {
            s_porSectionNames = newPORSectionNamesMap();
        }

        String rval = s_porSectionNames.get(section.tag);

        return rval;
    }

    public static Map<Integer, String> newPORSectionNamesMap() {
        Map<Integer, String> map = new HashMap<Integer, String>();


        map.put(PORSection.TAG_SOFTWARE, "Software");
        map.put(PORSection.TAG_AUTHOR, "Author");
        map.put(PORSection.TAG_TITLE, "Title");
        map.put(PORSection.TAG_VARIABLE_COUNT, "Variable count");
        map.put(PORSection.TAG_PRECISION, "Precision");
        map.put(PORSection.TAG_WEIGHT_VARIABLE, "Weight variable name");
        map.put(PORSection.TAG_VARIABLE_RECORD, "Variable");
        map.put(PORSection.TAG_MISSING_DISCRETE, "Missing value: discrete");
        map.put(PORSection.TAG_MISSING_OPEN_LO, "Missing value: open range low");
        map.put(PORSection.TAG_MISSING_OPEN_HI, "Missing value: open range high");
        map.put(PORSection.TAG_MISSING_RANGE, "Missing value: closed range");
        map.put(PORSection.TAG_VARIABLE_LABEL, "Variable label");
        map.put(PORSection.TAG_VALUE_LABELS, "Value-Label map");
        map.put(PORSection.TAG_DOCUMENTS_RECORD, "Documents"); // TODO: TAG_DOCUMENTS
        map.put(PORSection.TAG_DATA_MATRIX, "Data matrix");

        return map;
    }


} // class PORDump
