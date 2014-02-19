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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedOutputStream;
import java.io.OutputStream;

// spssio sav
import spssio.sav.SAVFile;

// spssio sav parser
import spssio.sav.input.SAVReader;

// spssio sav writer
import spssio.sav.output.SAVWriter;


public class SAVExtractHidden {
    
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
    
    public static class SAVExtraReader 
        extends SAVReader
    {
        
        // MEMBER VARIABLES
        //==================
        
        private OutputStream ostream;
        private byte[] buffer;
        
        // CONSTRUCTORS
        //==============
        
        public SAVExtraReader() {
            ostream = null;
            buffer = new byte[16]; // 16 bytes is enough
        }
        
        // OTHER METHODS
        //===============
        
        @Override
        public void readAlignedStringPadding(
            String string, 
            int paddingLength
        ) {
            readBytes(buffer, 0, paddingLength);
            // Write to ostream
            try {
                ostream.write(buffer, 0, paddingLength);
            } catch(IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        
        public void setOutputStream(OutputStream ostream) {
            this.ostream = ostream;
        }
    }
    
    // MAIN
    //======
    
    public static void main(String[] args) {
        if (args.length < 2) {
            usage();
            System.exit(EXIT_SUCCESS);
        }
        
        SAVFile sav = null;
        String fname = null;

        SAVExtraReader savExtraReader = new SAVExtraReader();
        
        String sourceFilename = args[0];
        String dataFilename = args[1];
        
        try {
            
            // READING
            System.out.printf("Extracting %s -> %s\n",
                sourceFilename, dataFilename);
            
            // Create a fname object
            File f = new File(dataFilename);
        
            // May throw FileNotFound
            FileOutputStream fis 
                = new FileOutputStream(f);
            
            BufferedOutputStream ostream
                = new BufferedOutputStream(fis);
            
            savExtraReader.setOutputStream(ostream);

            // Start timing
            long startTime = System.nanoTime();
            
            // ============== PARSING ==============
            sav = savExtraReader.parse(sourceFilename);
            // =====================================

            // Flush and close destination file
            ostream.flush();
            ostream.close();

            // Finish timing and calculate duration
            long endTime = System.nanoTime();
            long duration = endTime - startTime;
            
            // Display the time spent
            System.out.printf("Spent %.2f seconds\n", duration/1.0e9);

            
        } catch(Exception ex) {
            // Display more detailed error message
            System.out.printf("%s: at %08x: %s\n", 
                fname, 
                savExtraReader.tell(),
                ex.getMessage()
            );
            
            // Dump the stack trace if debugging enabled
            ex.printStackTrace();
            
            System.exit(EXIT_FAILURE);
        } // try-catch
    } // main()
    
    // OTHER METHODS
    //===============
    
    public static void usage() {
        System.out.printf("SAVExtractHidden (C) 2014 Jani Hautamaki <jani.hautamaki@hotmail.com>\n");
        System.out.printf("\n");
        System.out.printf("Usage:\n");
        System.out.printf("\n");
        System.out.printf("     SAVExtractHidden <input_sav> <raw_data_output>\n");
        System.out.printf("\n");
    }
    
}
