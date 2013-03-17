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

package spssio.por.output;

// core java
import java.io.OutputStream;
import java.io.IOException;
import java.util.Map; // for PORValueLabels
// for timestamp formation
import java.text.SimpleDateFormat;
import java.util.Date;

// spssio
import spssio.util.NumberSystem;
import spssio.por.PORCharset;
import spssio.por.PORFile;
import spssio.por.PORVariable;
import spssio.por.PORMissingValue;
import spssio.por.PORValueLabels;
import spssio.por.PORValue;
import spssio.por.PORMatrix;
import spssio.por.PORConstants;



// TODO:
// reconsider the need
import java.util.List;  // to_sections()

/**
 * Write SPSS/SPSS Portable files.<p>
 *
 * The class contains methods on three different levels of abstraction.
 * This layering enables customization of POR file production.<p>
 *
 * First level is the highest level, which is intended for most
 * applications:
 * <ul>
 *   <li>(@link TODO) - Serialize {@code PORFile} object into a Portable file.
 * </ul>
 * <p>
 *
 * Second level. These are meant for applications, which are interested 
 * in more fine-grained control over the serialization process. 
 * The Portable file on disk consists of various "records" each having 
 * a unique "tag code". The second level methods allow serialization
 * of individual "records":
 *
 * <ul>
 *   <li>(@link TODO) - Serialize Portable header (splash strings,
 *       charset, signature, format version, creation date, creation time).
 *   <li>(@link TODO) - Serialize tag='1': Author record.
 *   <li>(@link TODO) - Serialize tag='2': Software record.
 *   <li>(@link TODO) - Serialize tag='3': Title record.
 *   <li>(@link TODO) - Serialize tag='4': Variable count record.
 *   <li>(@link TODO) - Serialize tag='5': Precision record.
 *   <li>(@link TODO) - Serialize tag='6': Weight variable name record.
 *   <li>(@link TODO) - Serialize tag='7': Variable record.
 *   <li>(@link TODO) - Serialize tag='8': Missing value discrete record.
 *   <li>(@link TODO) - Serialize tag='9': Missing value open lo range record.
 *   <li>(@link TODO) - Serialize tag='A': Missing value open hi range record.
 *   <li>(@link TODO) - Serialize tag='B': Missing value closed range record.
 *   <li>(@link TODO) - Serialize tag='C': Variable label record.
 *   <li>(@link TODO) - Serialize tag='D': Value labels record.
 *   <li>(@link TODO) - Serialize tag='E': Documents record (unimplemented).
 *   <li>(@link TODO) - Serialize tag='F': Data matrix record.
 * </ul>
 * <p>
 *
 * Third level. This consists of method which are used to serialize
 * the primitive SPSS/PSPP data elements: strings, numbers and sysmiss values.
 * <ul>
 * </ul>
 *   <li>(@link TODO) - 
 * <p>
 *
 * Fourth level. This is the last level; it controls the individual character
 * serialization to the Portable file. This level takes care of 
 * the encoding of the characters, and splitting them into lines properly.
 * <ul>
 *   <li>(@link TODO) - 
 * </ul>
 * <p>
 *
 * A proper serialization sequence must satisfy some constraints:
 * <ol>
 *   <li>The header fields must be serialized first.
 *   <li>Missing value records and variable label records must be preceded
 *       by a variable record to which they relate to.
 *   <li>Before a value labels record can be serialized, all the referenced
 *       variables must be have a corresponding variable record serialized.
 *   <li>The data matrix must be serialized last.
 * </ol>
 * <p>
 * 
 *
 * 
 */
public class PORWriter
{
    
    // CONSTANTS
    //===========
    
    /**
     * Use Unix-style end-of-lines: LF ({@code '\n'}) only.
     * TBC: Use int[] array instead of a constant?
     */
    public static final int EOL_LF                  = 0x0A;
    
    /**
     * Use Windows-style end-of-lines: CR+LF ({@code '\r'}+{@code '\n'}).
     * TBC: Use int[] array instead of a constant?
     */
    public static final int EOL_CRLF                = 0x0D0A;
    
    // MEMBER VARIABLES
    //==================
    
    /**
     * Encoding table
     */
    private int[] enctab;
    
    /**
     * Column number of the next byte, 0-based.
     */
    private int col;
    
    /**
     * Row number of the next byte, 0-based.
     */
    private int row;

    
    /**
     * Row length (number of columns in a row).
     */
    private int row_length;
    
    /**
     * End-of-line type, either LF (unix) or CRLF (windows).
     */
    private int eol;
    
    /**
     * Output byte stream to which the serialization is sent.
     */
    private OutputStream ostream;

    /**
     * Number of bytes written.
     */
    private int size;
    
    /**
     * NumberSystem object for base-30 numbers
     */
    private NumberSystem numsys;
    
    /**
     * Formatter for the base-30 numbers
     */
    //private NumberFormatter numformatter;
    
    
    
    // CONSTRUCTORS
    //==============
    
    public void PORWriter() {
    }
    
    
    // OTHER METHODS
    //===============
    
    public void output(String filename, PORFile file) {
    }
    
    public void output(OutputStream os, PORFile file) {
        
        // setup stream
        
        /*
        outputPORHeader()
        outputPORVariables()
        outputPORValueLabels()
        outputDataRecord();
        */
    }
    
    
    /**
     * Convert {@code PORFile} into a sequence of {@code PORSection}s.
     * The sequence can then be assembled into a Portable file.
     *
     * @param file The Portable file to sectionize
     *
     * @return List of sections ready for serialization.
     */
    public static List<PORSection> sectionize(PORFile file) {
        // TODO: Write implementation
        return null;
    }
    

    // OUTPUT PRIMITIVES
    //===================
    
    public void outputSection(PORSection section) 
        throws IOException
    {
        // Pass-forward
        outputSection(section.tag, section.obj);
    }
    
    /**
     * Output a Portable file section with specified tag and object.
     * This method is provided for bypassing the creation of a temporary
     * {@code PORSection} object.
     *
     * @param tag The tag code. See {@code PORSection} for tag codes.
     * @param obj The associated object. See {@code PORSection} for
     *      the expected types of the associated objects.
     * 
     * @see PORSection
     *
     */
    public void outputSection(int tag, Object obj) 
        throws IOException
    {
        switch(tag) {
            case PORSection.TAG_SOFTWARE:
                outputSoftware((String) obj);
                break;
            
            case PORSection.TAG_AUTHOR:
                outputAuthor((String) obj);
                break;
            
            case PORSection.TAG_TITLE:
                outputTitle((String) obj);
                break;
            
            case PORSection.TAG_VARIABLE_COUNT:
                outputVariableCount((Integer) obj);
                break;
            
            case PORSection.TAG_PRECISION:
                outputNumericPrecision((Integer) obj);
                break;
            
            case PORSection.TAG_WEIGHT_VARIABLE:
                outputWeightVariable((String) obj);
                break;
            
            case PORSection.TAG_VARIABLE_RECORD:
                outputVariableRecord((PORVariable) obj);
                break;
            
            case PORSection.TAG_MISSING_DISCRETE:
                outputMissingDiscrete((PORMissingValue) obj);
                break;
            
            case PORSection.TAG_MISSING_OPEN_LO:
                outputMissingRangeOpenLo((PORMissingValue) obj);
                break;
            
            case PORSection.TAG_MISSING_OPEN_HI:
                outputMissingRangeOpenHi((PORMissingValue) obj);
                break;
            
            case PORSection.TAG_MISSING_RANGE:
                outputMissingRangeClosed((PORMissingValue) obj);
                break;
            
            case PORSection.TAG_VARIABLE_LABEL:
                outputVariableLabel((String) obj);
                break;
            
            case PORSection.TAG_VALUE_LABELS:
                outputValueLabelsRecord((PORValueLabels) obj);
                break;
            
            case PORSection.TAG_DOCUMENTS_RECORD:
                // Unimplemented
                break;
            
            case PORSection.TAG_DATA_MATRIX:
                outputDataMatrixRecord((PORMatrix) obj);
                break;
            
            default:
                throw new RuntimeException(String.format(
                    "Unrecognized tag code: \'%c\' (hex: %02x)", tag, tag));
        } // switch
    } // output_section()
    
    
    //=======================================================================
    // WRITE HEADER
    //=======================================================================
    
    /**
     * Convience method for using default values as a header.
     */
    public void outputHeader() 
        throws IOException
    {
        outputHeader(null, null, null, 0, null, null);
    }
    
    public void outputHeader(
        String[] splash,
        int[] charset,
        String signature,
        int version,
        String date,
        String time
    ) 
        throws IOException
    {
        // TODO: Set identity encoding (=no encoding)
        
        // Write the 200-byte header
        outputSplashStrings(splash);
        
        // Write the 256-byte character set
        outputCharset(charset);
        
        // TODO: Set encoding
        
        // Write the 8-byte signature
        outputFormatSignature(signature);
        
        // Write the 1-byte format identifier
        outputFormatVersion(version);
        
        // Write the 8-byte creation date, and the 6-byte creation time.
        outputCreationTimestamp(date, time);
        
    } // output_header
    
    // splash is an array of 40 chars
    public void outputSplashStrings(String[] splash) 
        throws IOException
    {
        // TODO: If unspecified, use defaults.
        
        if (splash.length != 5) throw new IllegalArgumentException();
        
        for (int i = 0; i < 5; i++) {
            String s = splash[i];
            if (s.length() != 40) throw new IllegalArgumentException();
            write(s);
        } // for: each splash string
    }

    
    public void outputCharset(int[] charset) 
        throws IOException
    {
        // If unspecified, use default charset
        if (charset == null) {
            charset = PORCharset.getDefaultCharset();
        }
        
        if (charset.length != 256) throw new IllegalArgumentException();
        for (int i = 0; i < 256; i++) {
            // Truncate the value, and write a single byte.
            write(charset[i] & 0xff);
        }
    }
    
    public void outputFormatSignature(String signature) 
        throws IOException
    {
        // If unspecified, use defalt
        if (signature == null) {
            signature = PORConstants.FORMAT_SIGNATURE;
        }
        
        if (signature.length() != 8) throw new IllegalArgumentException();
        
        // NOTE: This is NOT string, it is simply 8 bytes.
        write(signature);
    }
    
    public void outputFormatVersion(int version) 
        throws IOException
    {
        // If unspecified, use default version
        if (version == 0) {
            version = PORConstants.FORMAT_VERSION;
        }
        
        // NOTE: This is a single byte.
        write(version);
    }
    
    public void outputCreationTimestamp(String date, String time) 
        throws IOException
    {
        // TODO:
        // If date or time is left unspecified, use current date/time.
        if (date == null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            date = sdf.format(new Date());
        }
        if (time == null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HHmmss");
            date = sdf.format(new Date());
        }
        
        if ((date.length() != 8) || (time.length() != 6)) {
            throw new IllegalArgumentException();
        }

        // NOTE: These are true strings with lengths.
        
        // Output 8-byte date
        outputString(date);
        
        // Output 6-byte time
        outputString(time);
    }
    
    //=======================================================================
    // PORTABLE SECTIONS OUTPUT METHODS
    //=======================================================================

    public void outputSoftware(String software) 
        throws IOException
    {
        if (software.length() >= PORConstants.MAX_SOFTWARE_LENGTH) {
            throw new IllegalArgumentException();
        }
        
        // Tag code
        outputTag(PORSection.TAG_SOFTWARE);
        outputString(software);
    }
    
    public void outputAuthor(String author) 
        throws IOException
    {
        if (author.length() > PORConstants.MAX_AUTHOR_LENGTH) {
            throw new IllegalArgumentException();
        }
        
        // Tag code
        outputTag(PORSection.TAG_AUTHOR);
        outputString(author);
    } 
    
    public void outputTitle(String title) 
        throws IOException
    {
        if (title.length() > PORConstants.MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException();
        }
        
        outputTag(PORSection.TAG_TITLE);
        outputString(title);
    }
    
    public void outputVariableCount(int varcount) 
        throws IOException
    {
        // Validate variable count
        if (varcount < 1) {
            throw new IllegalArgumentException();
        }
        
        outputTag(PORSection.TAG_VARIABLE_COUNT);
        outputInt(varcount);
    }

    public void outputNumericPrecision(int precision) 
        throws IOException
    {
        if (precision < 1) {
            throw new IllegalArgumentException();
        }
        
        outputTag(PORSection.TAG_PRECISION);
        outputInt(precision);
    }
    
    public void outputWeightVariable(String weight_var_name) 
        throws IOException
    {
        if (weight_var_name.length() >= PORConstants.MAX_VARNAME_LENGTH) {
            throw new IllegalArgumentException();
        }
        
        outputTag(PORSection.TAG_WEIGHT_VARIABLE);
        outputString(weight_var_name);
    }
    
    public void outputVariableRecord(PORVariable pvar) 
        throws IOException
    {
        if (pvar.name.length() >= PORConstants.MAX_VARNAME_LENGTH) {
            throw new IllegalArgumentException();
        }
        
        // Tag code
        outputTag(PORSection.TAG_VARIABLE_RECORD);
        
        // width and name
        outputInt(pvar.width);
        outputString(pvar.name);
        
        // output format: type, width, decimals
        outputInt(pvar.printfmt.type);
        outputInt(pvar.printfmt.width);
        outputInt(pvar.printfmt.decimals);
        
        // input format: type, width, decimals
        outputInt(pvar.writefmt.type);
        outputInt(pvar.writefmt.width);
        outputInt(pvar.writefmt.decimals);
    }
    
    public void outputMissingDiscrete(PORMissingValue miss) 
        throws IOException
    {
        // Tag code
        outputTag(PORSection.TAG_MISSING_DISCRETE);
        
        // Value (depends on the variable's type)
        outputPORValue(miss.values[0]);
    }
    
    public void outputMissingRangeOpenLo(PORMissingValue miss) 
        throws IOException
    {
        // Tag code
        outputTag(PORSection.TAG_MISSING_OPEN_LO);
        
        // Value (depends on the variable's type)
        outputPORValue(miss.values[0]);
    }

    public void outputMissingRangeOpenHi(PORMissingValue miss) 
        throws IOException
    {
        // Tag code
        outputTag(PORSection.TAG_MISSING_OPEN_HI);
        
        // Value (depends on the variable's type)
        outputPORValue(miss.values[0]);
    }

    public void outputMissingRangeClosed(PORMissingValue miss) 
        throws IOException
    {
        // Tag code
        outputTag(PORSection.TAG_MISSING_RANGE);
        
        // Values (depends on the variable's type)
        outputPORValue(miss.values[0]);
        outputPORValue(miss.values[1]);
    }
    
    public void outputVariableLabel(String varlabel) 
        throws IOException
    {
        if (varlabel.length() >= PORConstants.MAX_VARLABEL_LENGTH) {
            throw new RuntimeException();
        }
        
        // Tag code
        outputTag(PORSection.TAG_VARIABLE_LABEL);
        
        // Variable label
        outputString(varlabel);
    }

    public void outputValueLabelsRecord(PORValueLabels vallabels) 
        throws IOException
    {
        outputTag(PORSection.TAG_VALUE_LABELS);
        
        // <variable_count>
        // NOTE: size() can't return negative value
        int size = vallabels.vars.size();
        outputInt(size);
        
        // <list_of_varnames>
        int valtype = PORValue.TYPE_UNASSIGNED;
        
        for (int i = 0; i < size; i++) {
            PORVariable cur_var = vallabels.vars.elementAt(i);
            
            if (valtype == PORValue.TYPE_NUMERIC) {
                if (cur_var.width != 0) {
                    // ERROR: Incompatible type
                    throw new RuntimeException();
                }
            } else if (valtype == PORValue.TYPE_STRING) {
                if (cur_var.width == 0) {
                    // ERROR: Incompatible type
                    throw new RuntimeException();
                }
            } else if (valtype == PORValue.TYPE_UNASSIGNED) {
                // Use first variable as variable type indicator
                if (cur_var.width == 0) {
                    valtype = PORValue.TYPE_NUMERIC;
                } else {
                    valtype = PORValue.TYPE_STRING;
                }
            } else {
                // Does not happen.
            } // if-else
            
            // Write variable name
            outputString(cur_var.name);
        } // for: each variable
        
        
        // <number_of_mappings> (aka label_count)
        // NOTE: size() cannot return negative value
        outputInt(vallabels.mappings.size());
        
        // [<value> <label>]+
        for (Map.Entry<PORValue, String> entry 
            : vallabels.mappings.entrySet())
        {
            outputPORValue(entry.getKey());
            outputString(entry.getValue());
        } // for: entry set
        
    } // outputValueLabelsRecord()

    //=======================================================================
    // PORDataMatrix OUTPUT METHOD
    //=======================================================================
    
    /*
     * TODO:
     * This is a "pass-through" serialization, which does not reformat
     * the data according to the precision specification.
     * However, it could be done as follows:
     * The data in PORMatrix is assumed to have a precision_data.
     * Output data is required to have a precision_output.
     * Now there are three different possibilities:
     *
     *      Case 1. precision_data < precision_output
     *          Now the output is required to have a higher precision,
     *          than the data has. There is nothing that can be done
     *          to increase precision. Pass-through serialization.
     *         
     *      Case 2. precision_data == precision_output
     *          The data has the required precision. Nothing needs to
     *          be done. Pass-through serialization.
     *
     *      Case 3. precision_data > precision_output
     *          Now this is the worst case. The data in the matrix
     *          has higher precision than the output allows. Consequently,
     *          the data in the matrix needs to be rounded further.
     *     
     *          How to do the rounding????
     *
     * Rounding:
     * The numbers come in ascii base-30 digits.
     * Generally they have the following format: [+-]<ABCD>[.]HIJK[<+->LMN]
     * Steps to determine the precision: 
     *
     *          1. Ignore leading sign
     *          2. Count all digits up to the next sign or end of data.
     *          3. Put the position of the dot into memory.
     * 
     * Now the precision is the number of digits.
     * Do the rounding: start at the required precision+1, and see
     * whether it is >=5. If so, pos-1 is rounded upwards recursively.
     *
     * At the end, eliminate trailing zeros. If the dot's position
     * is crossed, then the exponent needs to be increased as well.
     
     *
     * NOTE!!! When rounding upwards occurs, there's a possiblity
     * that the rounded value exceeds numeric limits.
     *
     * 
     */
    public void outputDataMatrixRecord(PORMatrix data) 
        throws IOException
    {
        outputTag(PORSection.TAG_DATA_MATRIX);
        
        // This is a bit dirty hack for finding out the end-marker 'Z'.
        // Anyway, if the lines are at most 80 chars wide, then 
        // it has a property: the written data matrix won't be longer
        // than the source data matrix (number of lines full of end-markers
        // can't increase gradually).
        
        int endoffset = data.size();
        endoffset -= 2*80; // count back 2 lines
        if (endoffset < 0) endoffset = 0; // saturate
        
        // seek and determine the end
        data.seek(endoffset);
        endoffset = -1;
        int c;
        int lastc = 0;
        while ((c = data.read()) != -1) {
            if ((c == '\n') || (c == '\r')) {
                // Ignore EOL chars.
                continue;
            }
            
            if (c == 'Z') {
                if (lastc != 'Z') {
                    // New 'Z'.
                    // Record current position
                    endoffset = data.pos();
                } else {
                    // Consequetive 'Z'.
                }
            } else {
                // Lose the position
                endoffset = -1;
            }
            
            lastc = c;
        } // while
        
        System.out.printf("endoffset: %d, size: %d\n",
            endoffset, data.size());
        
        if (endoffset == -1) {
            // default to end-of-data
            endoffset = data.size();
        }
        
        // seek to the beginning
        data.seek(0);
        int data_col = 0; // TODO: data.getColumn0()
        int data_row_length = 80; // TODO: data.getRowLength()
        
        for (int i = 0; i < endoffset; i++) {
            c = data.read();
            if (c == '\n') {
                
                if (data_col < data_row_length) {
                    // fill the end of the line with whitespaces
                    for (; data_col < data_row_length; data_col++) {
                        write(' ');
                    }
                } // if: incomplete line
                
                // New line
                data_col = 0;
            } else if (c == '\r') {
                // ignore
                continue;
            } else {
                // Pass-through
                write(c);
                // Next column in data
                data_col++;
            }
        } // for
        
        // Write end-marker
        write('Z');
        
        // Complete the last line with end-markers.
        int len = row_length-col;
        for (int i = 0; i < len; i++) {
            write('Z');
        }
        
        // and we are done!
    } // output_data_matrix()
    
    public void outputDataMatrixRecord2(PORMatrix data) 
        throws IOException
    {
        // TODO:
        // This uses PORMatrixVisitor to do the outputting
        
        // for a given numeric value:
        // if len < precision: pass-through
        // else: round(array, len, desired_precision)
    } // output_data_matrix2()
    

    //=======================================================================
    // PORValue OUTPUT METHOD
    //=======================================================================
    
    public void outputPORValue(PORValue pvalue) 
        throws IOException
    {
        switch(pvalue.type) {
            case PORValue.TYPE_STRING:
                outputString(pvalue.value);
                break;
            case PORValue.TYPE_NUMERIC:
                // TODO: The precision has to be accounted for.
                write(pvalue.value); // pass-through
                break;
            default:
                throw new RuntimeException();
        } // switch
    }

    
    //=======================================================================
    // SPSS/PSPP PRIMITIVES' OUTPUT METHODS
    //=======================================================================
    
    public void outputString(String string) 
        throws IOException
    {
        // TODO: Apply NumberFormatter to the length
    }
    
    // TODO:
    // Should the method be renamed to outputDouble()?
    public void outputNumeric(double value)
        throws IOException
    {
        // TODO: Apply NumberFormatter
    }
    
    public void outputSysmiss()
        throws IOException
    {
        // TODO: An asterisk and a dot
    }
    
    public void outputSysmiss(char sep)
        throws IOException
    {
        // TODO: An asterisk plus a separator, usullay dot.
    }

    public void outputInt(int value)
        throws IOException
    {
        // TODO
    }
    

    public void outputTag(int c)
        throws IOException
    {
        // TODO!!!!!!!!!! 
        // Study whether the tag codes are subject to decoding/encoding
        write(c);
    }
    
    
    //=======================================================================
    // LOW-LEVEL OUTPUT METHODS
    //=======================================================================
    
    /**
     * Set byte encoding according to charset.
     * 
     * @param charset The character set to use in encoding,
     *      or {@code null} to disable encoding.
     * 
     */
    private void setEncodingCharset(int[] charset) {
        // TODO: calculate encoding table
    }
    
    private void write(String string) 
        throws IOException
    {
        int len = string.length();
        int c;
        
        for (int i = 0; i < len; i++) {
            // Pick the current char
            c = string.charAt(i);
            
            // UNROLLED write(int)
            //=====================

            // Truncate and encode
            c = enctab[c & 0xff];
            
            // Write
            ostream.write(c);
            
            // Next column
            col++;
            
            // If line is full, write an end-of-line sequence,
            // reset column, and move to next row.
            if (col == row_length) {
                // Write end-of-line sequence.
                // If Windows-style, then precede LF by CR.
                if (eol == EOL_CRLF) {
                    ostream.write('\r'); // CR
                }
                ostream.write('\n'); // LF
                
                // Reset column and move to next row.
                col = 0;
                row++;
            } // if: row full
        } // for: each char
        
    } // write(String)

    // The most low-level writing operations.
    // These handle the encoding and line-wrapping
    private void write(int[] array, int from, int to) 
        throws IOException
    {
        int c;
        
        for (int i = from; i < to; i++) {
            // Pick the next byte
            c = array[i];
            
 
            // Finish with update sequence
            
            // UNROLLED write(int)
            //=====================

            // Encode
            c = enctab[c & 0xff];
            
           // Write
            ostream.write(c);
            
            // Next column
            col++;
            
            // If line is full, write an end-of-line sequence,
            // reset column, and move to next row.
            if (col == row_length) {
                // Write end-of-line sequence.
                // If Windows-style, then precede LF by CR.
                if (eol == EOL_CRLF) {
                    ostream.write('\r'); // CR
                }
                ostream.write('\n'); // LF
                
                // Reset column and move to next row.
                col = 0;
                row++;
            } // if: row full
        } // for
        
    } // write(array)
    
    /**
     * Writes a byte to output stream with encoding and line-wrapping.
     *
     * @param c The byte to write
     */
    private void write(int c) 
        throws IOException
    {
        // Apparently SPSS writes an end-of-line sequence after 'Z' sequence.
        
        // Encode
        c = enctab[c & 0xff];
        
        // Write output
        ostream.write(c);
        
        // Next column
        col++;
        
        // If line is full, write an end-of-line sequence,
        // reset column, and move to next row.
        if (col == row_length) {
            // Write end-of-line sequence.
            // If Windows-style, then precede with CR.
            if (eol == EOL_CRLF) {
                ostream.write('\r'); // CR
            }
            ostream.write('\n'); // LF
            
            // Reset column and move to next row.
            col = 0;
            row++;
        } // if: row full
        
    } // write(int)
} // class PORWriter
