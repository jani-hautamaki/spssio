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

// spssio sav
import spssio.sav.SAVFile;

// spssio sav parser
import spssio.sav.input.SAVReader;

// spssio sav writer
import spssio.sav.output.SAVWriter;


public class SAVRoundTrip {
    
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
        if (args.length < 2) {
            usage();
            System.exit(EXIT_SUCCESS);
        }
        
        SAVFile sav = null;
        String fname = null;

        SAVReader savReader = new SAVReader();
        
        try {
            
            // READING
            
            fname = args[0];
            System.out.printf("%s\n", fname);
            
            // Start timing
            long startTime = System.nanoTime();
            
            // ============== PARSING ==============
            sav = savReader.parse(fname);
            // =====================================
            
            // Finish timing and calculate duration
            long endTime = System.nanoTime();
            long duration = endTime - startTime;
            
            // Display the time spent
            System.out.printf("Reading took %.2f seconds\n", duration/1.0e9);

            
        } catch(Exception ex) {
            // Display more detailed error message
            System.out.printf("%s: at %08x: %s\n", 
                fname, 
                savReader.tell(),
                ex.getMessage()
            );
            
            // Dump the stack trace if debugging enabled
            ex.printStackTrace();
            
            System.exit(EXIT_FAILURE);
        } // try-catch

        SAVWriter savWriter = new SAVWriter();
        
        try {
            // WRITING

            fname = args[1];
            System.out.printf("%s\n", fname);
            
            // Start timing
            long startTime = System.nanoTime();

            // ============ FORMATTING =============
            savWriter.output(sav, fname);
            // =====================================
            
            // Finish timing and calculate duration
            long endTime = System.nanoTime();
            long duration = endTime - startTime;

            // Display the time spent
            System.out.printf("Writing took %.2f seconds\n", duration/1.0e9);
            
        } catch(Exception ex) {
            
            // Dump the stack trace if debugging enabled
            ex.printStackTrace();
            
            System.exit(EXIT_FAILURE);
        } // try-catch

        
        System.exit(EXIT_SUCCESS);
    }
    
    // OTHER METHODS
    //===============
    
    public static void usage() {
        System.out.printf("SAVRoundTrip (C) 2014 Jani Hautamaki <jani.hautamaki@hotmail.com>\n");
        System.out.printf("\n");
        System.out.printf("Usage:\n");
        System.out.printf("\n");
        System.out.printf("     SAVRoundTrip <input_sav> <output_sav>\n");
        System.out.printf("\n");
    }
    
}
