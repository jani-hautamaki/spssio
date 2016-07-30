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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;

// spssio sav
import spssio.sav.SAVFile;

// spssio sav parser
import spssio.sav.input.SAVReader;

// spssio sav writer
import spssio.sav.output.SAVWriter;


public class SAVInjectHidden {

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

    // HELPER CLASSES
    //================

    public static class SAVExtraWriter
        extends SAVWriter
    {

        // MEMBER VARIABLES
        //==================

        private InputStream istream;
        private byte[] buffer;

        // CONSTRUCTORS
        //==============

        public SAVExtraWriter() {
            istream = null;
            buffer = new byte[16]; // 16 bytes is enough
        }

        // OTHER METHODS
        //===============

        @Override
        public void writeAlignedStringPadding(
            String string,
            int paddingLength
        ) {
            boolean ok = true;
            try {
                int result = istream.read(buffer, 0, paddingLength);

                if (result != paddingLength) {
                    ok = false;
                }
            } catch(IOException ex) {
                throw new RuntimeException(ex);
            }

            if (ok == true) {
                writeBytes(buffer, 0, paddingLength);
                //super.writeAlignedStringPadding(string, paddingLength);
            } else {
                super.writeAlignedStringPadding(string, paddingLength);
            }
        }

        public void setInputStream(InputStream istream) {
            this.istream = istream;
        }
    }

    // MAIN
    //======

    public static void main(String[] args) {
        if (args.length < 3) {
            usage();
            System.exit(EXIT_SUCCESS);
        }

        SAVFile sav = null;
        String fname = null;

        SAVReader savReader = new SAVReader();

        String sourceFilename = args[0];
        String dataFilename = args[1];
        String targetFilename = args[2];


        // Start timing
        long startTime = System.nanoTime();

        try {

            // READING
            System.out.printf("Merging %s & %s -> %s\n",
                sourceFilename, dataFilename, targetFilename);


            // ============== PARSING ==============
            sav = savReader.parse(sourceFilename);
            // =====================================


        } catch(Exception ex) {
            // Display more detailed error message
            System.out.printf("%s: at %08x: %s\n",
                sourceFilename,
                savReader.tell(),
                ex.getMessage()
            );

            // Dump the stack trace if debugging enabled
            ex.printStackTrace();

            System.exit(EXIT_FAILURE);
        } // try-catch

        try {
            // Create a fname object
            File f = new File(dataFilename);

            // May throw FileNotFound
            FileInputStream fis
                = new FileInputStream(f);

            BufferedInputStream istream
                = new BufferedInputStream(fis);

            // Prepare extra writer
            SAVExtraWriter savExtraWriter = new SAVExtraWriter();

            savExtraWriter.setInputStream(istream);

            // Output the sections directly,
            // to guarantee otherwise verbatim copy
            savExtraWriter.outputSections(sav, targetFilename);

            // Close source data file
            istream.close();

        } catch(Exception ex) {
            ex.printStackTrace();
            System.exit(EXIT_FAILURE);
        }

        // Finish timing and calculate duration
        long endTime = System.nanoTime();
        long duration = endTime - startTime;

        // Display the time spent
        System.out.printf("Spent %.2f seconds\n", duration/1.0e9);

    } // main()

    // OTHER METHODS
    //===============

    public static void usage() {
        System.out.printf("SAVInjectHidden (C) 2014 Jani Hautamaki <jani.hautamaki@hotmail.com>\n");
        System.out.printf("\n");
        System.out.printf("Usage:\n");
        System.out.printf("\n");
        System.out.printf("     SAVInjectHidden <input_sav> <input_raw> <output_sav>\n");
        System.out.printf("\n");
    }

}
