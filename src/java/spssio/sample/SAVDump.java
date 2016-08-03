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
import java.util.Vector;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

// spssio sav
import spssio.sav.SAVFile;
import spssio.sav.SAVHeader;
import spssio.sav.SAVSection;
import spssio.sav.SAVVariable;
import spssio.sav.SAVValueLabels;
import spssio.sav.SAVValue;
import spssio.sav.SAVExtensionRecord;
import spssio.sav.SAVExtSystemConfig;
import spssio.sav.SAVExtNumberConfig;

// spssio util
import spssio.util.DataEndianness;


// spssio sav parser
import spssio.sav.input.SAVReader;

public class SAVDump {

    // CONSTANTS
    //===========

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

        SAVReader savReader = new SAVReader();
        SAVFile sav = null;

        try {
            // Start timing
            long startTime = System.nanoTime();

            // ============== PARSING ==============
            sav = savReader.parse(fname);
            // =====================================

            // Finish timing
            long endTime = System.nanoTime();

            // Calculate time spent
            long duration = endTime - startTime;

            // Display the time spent
            System.out.printf("Spent %.2f seconds\n", duration/1.0e9);

        } catch(Exception ex) {
            // Display more detailed error message
            System.out.printf("%s: at %08x: %s\n",
                fname,
                savReader.tell(),
                ex.getMessage()
            );

            // Dump the stack trace if debugging enabled
            ex.printStackTrace();

            System.out.printf("Attempting to display the sav file anyway\n");
            displaySAVFile(savReader.getLatestSAVFile(), savReader);

            System.exit(EXIT_FAILURE);
        } // try-catch

        // Printing...
        displaySAVFile(sav, savReader);

        System.exit(EXIT_SUCCESS);
    }

    // OTHER METHODS
    //===============

    public static void usage() {
        System.out.printf("Usage:\n");
        System.out.printf("\n");
        System.out.printf("     SAVDump <input_sav>\n");
        System.out.printf("\n");
        System.out.printf("SAVDump (C) 2014 Jani Hautamaki <jani.hautamaki@hotmail.com>\n");
    }

    public static void displaySAVFile(SAVFile sav, SAVReader savReader) {
        System.out.printf("Header:\n");
        displaySAVHeader(sav.header);

        System.out.printf("Overview:\n");
        System.out.printf("  Columns:                   %d\n",       sav.variables.size());
        System.out.printf("  Variables:                 %d\n",       sav.calculateNumberOfVariables());

        //displayVariables(sav.variables);

        //displayValueLabelMaps(sav.valueLabelMaps);

        displayDocuments(sav.documents);

        displayExtensionRecords(sav.extensionRecords, savReader);

        displaySections(sav.sections);
    }

    public static void displaySAVHeader(SAVHeader header) {
        System.out.printf("  Signature:                 \"%s\"\n",   header.signature);
        System.out.printf("  Software:                  \"%s\"\n",   header.software);
        System.out.printf("  Layout:                    %d\n",       header.layout);
        System.out.printf("  Number of variables:       %d\n",       header.variableCount);
        System.out.printf("  Compression:               %d\n",       header.compression);
        System.out.printf("  Weight variable index:     %d\n",       header.weightVariableIndex);
        System.out.printf("  Number of cases:           %d\n",       header.numberOfCases);
        System.out.printf("  Compression bias:          %f\n",       header.bias);
        System.out.printf("  Last modified date:        \"%s\"\n",   header.date);
        System.out.printf("  Last modified time:        \"%s\"\n",   header.time);
        System.out.printf("  Number of cases:           %d\n",       header.numberOfCases);
        System.out.printf("  File label:                \"%s\"\n",   header.title);
        System.out.printf("  Padding:                   [%02x, %02x, %02x]\n", header.padding[0], header.padding[1], header.padding[2]);
    }

    public static void displayVariables(Vector<SAVVariable> variables) {
        int num = 0;
        for (SAVVariable v : variables) {
            System.out.printf("Variable #%d\n", num);
            displaySAVVariable(v);
            num++;
        }
    }

    public static void displaySAVVariable(SAVVariable v) {
        System.out.printf("  Width:                     %d\n",       v.width);
        System.out.printf("  hasLabel:                  %d\n",       v.hasLabel);
        System.out.printf("  Number of missing values:  %d\n",       v.numberOfMissingValues);
        System.out.printf("  Input format:              %06x\n",     v.inputFormat.raw);
        System.out.printf("  Output format:             %06x\n",     v.outputFormat.raw);
        System.out.printf("  Name:                      \"%s\"\n",   v.name);
        System.out.printf("  Label:                     \"%s\"\n",   v.label);

        if (v.numberOfMissingValues > 0) {
            for (int i = 0; i < v.missingValues.length; i++) {
                System.out.printf("  Missing value #%d:         %f\n",   i+1, v.missingValues[i]);
            } // for
        }
    }


    public static void displayValueLabelMaps(
        Vector<SAVValueLabels> maps
    ) {
        int num = 0;
        for (SAVValueLabels vls : maps) {
            System.out.printf("Value-Label Map #%d\n", num);
            displaySAVValueLabels(vls);
            num++;
        }
    }

    public static void displaySAVValueLabels(SAVValueLabels vls) {
        System.out.printf("  Variables affected:\n");
        displayVariableList(vls.variables);
        System.out.printf("  Value-Label mappings:\n");
        displayVLMap(vls.map, vls.type);
    }

    public static void displayVariableList(Vector<SAVVariable> list) {
        int count = 0;
        for (SAVVariable v : list) {
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

    public static void displayVLMap(Map<SAVValue, String> map, int type) {
        int count = 0;

        for (Map.Entry<SAVValue, String> e : map.entrySet()) {
            count++;

            SAVValue value = e.getKey();
            String label =  e.getValue();

            String valueString = null;

            switch(type) {
                case SAVValue.TYPE_NUMERIC:
                    valueString = Double.toString(value.getDouble());
                    break;
                case SAVValue.TYPE_STRING:
                    valueString = value.getString();
                    break;
                case SAVValue.TYPE_RAW:
                    throw new RuntimeException(String.format(
                        "Value-Label map has uninterpreted values"));
            } // switch

            System.out.printf("    %-8s    %s\n", valueString, label);
        }
    }

    public static void displayDocuments(List<String> documents) {
        if (documents == null) {
            return;
        }

        System.out.printf("Documents/Comments (%d lines)\n", documents.size());
        for (String line : documents) {
            System.out.printf("  %s\n", line);
        }
        System.out.printf("  <end-of-documents>\n");
    }


    public static void displayExtensionRecords(
        Vector<SAVExtensionRecord> records,
        SAVReader savReader
    ) {
        int num = 0;
        for (SAVExtensionRecord extRecord : records) {
            System.out.printf("Extension record #%d\n", num);
            displaySAVExtensionRecord(extRecord, savReader);
            num++;
        }
    }

    public static void displaySAVExtensionRecord(
        SAVExtensionRecord extRecord,
        SAVReader savReader
    ) {
        System.out.printf("  Sub-tag:                   %d (hex %x)\n", extRecord.subtag, extRecord.subtag);
        System.out.printf("  Element size:              %d\n", extRecord.elementSize);
        System.out.printf("  Number of elements:        %d\n", extRecord.numberOfElements);

        boolean handled = false;
        switch(extRecord.subtag) {
            case 3: // Source platform integer info record
                displaySAVExtSystemConfig((SAVExtSystemConfig) extRecord);
                handled = true;
                break;
            case 4: // Source platform floating-point info record
                displaySAVExtNumberConfig((SAVExtNumberConfig) extRecord);
                handled = true;
                break;
            case 7: // Variable sets
                break;
            case 11: // Level, Width, Aligment
                break;
            case 13: // Long variable names record
                break;
            case 14: // Very long string record
                break;
            case 16: // int64 version of numberOfCases
                break;
            case 17: // Attributes
                break;
            case 18: // Attributes
                break;
            case 19: // Variable sets
                break;
            case 20: // Encoding
                break;
            case 21: // Long value label string map
                break;
            case 22: // Long missing value map
                break;

            default:
                // Unhandled extension record
                break;
        }

        if ((extRecord.data == null) && (handled == false)) {
            System.out.printf("   Unfortunately this cannot be displayed...\n");
        }

        if (extRecord.data == null) {
            return;
        }


        DataEndianness integerEndianness = new DataEndianness();
        DataEndianness floatingEndianness = new DataEndianness();

        integerEndianness.set(savReader.getIntegerEndianness());
        floatingEndianness.set(savReader.getFloatingEndianness());


        if (extRecord.elementSize == 1) {
            System.out.printf("  Data dump (char):\n");
            int col = 0;
            for (int i = 0; i < extRecord.numberOfElements; i++) {
                if (col == 0) {
                    System.out.printf("    ");
                }

                int c = ((int) extRecord.data[i]) & 0xff;
                if (c < 0x20) {
                    System.out.printf("<%02x>", c);
                    col += 4;

                    if (c == 0x0A) {
                        System.out.printf("\n");
                        col = 0;
                    }

                } else {
                    System.out.printf("%c", c);
                    col += 1;
                }

                if (col >= 69) {
                    System.out.printf("\n");
                    col = 0;
                }
            }
            // dump as characters
            if (col != 0) {
                System.out.printf("\n");
            }
        }
        else if (extRecord.elementSize == 4) {
            System.out.printf("  Data dump (int):\n");
            int col = 0;
            for (int i = 0; i < extRecord.numberOfElements; i++) {
                if (col == 0) {
                    System.out.printf("    ");
                } else {
                    System.out.printf("   ");
                }

                int value = integerEndianness.bytesToInteger(
                    extRecord.data, i*4);

                System.out.printf("%-10d", value);

                col++;
                if (col == 4) {
                    System.out.printf("\n");
                    col = 0;
                }
            }
            if (col != 0) {
                System.out.printf("\n");
            }
        }
        else if (extRecord.elementSize == 8) {
            System.out.printf("  Data dump (double):\n");
            int col = 0;
            for (int i = 0; i < extRecord.numberOfElements; i++) {
                if (col == 0) {
                    System.out.printf("    ");
                } else {
                    System.out.printf("   ");
                }

                double value = floatingEndianness.bytesToDouble(
                    extRecord.data, i*8);

                System.out.printf("%10s", Double.toString(value));

                col++;
                if (col == 4) {
                    System.out.printf("\n");
                    col = 0;
                }
            }
            if (col != 0) {
                System.out.printf("\n");
            }
        }
    }

    public static void displaySAVExtSystemConfig(
        SAVExtSystemConfig ext
    ) {
        System.out.printf("  System configuration\n");
        System.out.printf("  Version major.minor.rev:   %d.%d.%d\n", ext.versionMajor, ext.versionMinor, ext.versionRevision);
        System.out.printf("  Machine code:              %d\n", ext.machineCode);
        System.out.printf("  Floating-point format:     %d\n", ext.fpFormat);
        System.out.printf("  Compression code:          %d\n", ext.compression);
        System.out.printf("  Endianness:                %d\n", ext.systemEndianness);
        System.out.printf("  String encoding/codepage:  %d\n", ext.stringCodepage);
    }

    public static void displaySAVExtNumberConfig(
        SAVExtNumberConfig ext
    ) {
        System.out.printf("  Special symbols encoding\n");
        System.out.printf("  Sysmiss:                   %16x\n",
            Double.doubleToLongBits(ext.sysmissValue));
        System.out.printf("  Highest:                   %16x\n",
            Double.doubleToLongBits(ext.highestValue));
        System.out.printf("  Lowest:                    %16x\n",
            Double.doubleToLongBits(ext.lowestValue));

    }


    public static void displaySections(Vector<SAVSection> sections) {
        int num = 0;
        for (SAVSection sect : sections) {
            String name = getSAVSectionName(sect);
            System.out.printf(" #%-3d    %3d     %s\n", num, sect.tag, name);
            num++;
        }
    }

    public static Map<Integer, String> s_savSectionNames = null;

    public static String getSAVSectionName(SAVSection section) {
        if (s_savSectionNames == null) {
            s_savSectionNames = newSAVSectionNamesMap();
        }

        String rval = s_savSectionNames.get(section.tag);

        return rval;
    }

    public static Map<Integer, String> newSAVSectionNamesMap() {
        Map<Integer, String> map = new HashMap<Integer, String>();

        map.put(SAVSection.TAG_HEADER, "Header");
        map.put(SAVSection.TAG_VARIABLE, "Variable");
        map.put(SAVSection.TAG_VARIABLE_LIST, "Variable list");
        map.put(SAVSection.TAG_VALUE_LABELS, "Value-Label map");
        map.put(SAVSection.TAG_DOCUMENTS, "Documents/Comments");
        map.put(SAVSection.TAG_EXTENSION_RECORD, "Extension record");
        map.put(SAVSection.TAG_DATA_MATRIX, "Data matrix");

        return map;
    }
}
