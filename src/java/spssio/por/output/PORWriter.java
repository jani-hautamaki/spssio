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


package spssio.por.output;

// core java
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileNotFoundException;
import java.util.Map;         // for PORValueLabels
import java.util.Collection;  // for outputValueLabelsRecord()
import java.util.Vector;
// for timestamp formation
import java.text.SimpleDateFormat;
import java.util.Date;

// spssio
import spssio.util.NumberSystem;
import spssio.util.NumberFormatter;
import spssio.util.SequentialByteArray;
import spssio.por.PORCharset;
import spssio.por.PORFile;
import spssio.por.PORHeader;
import spssio.por.PORVariable;
import spssio.por.PORMissingValue;
import spssio.por.PORValueLabels;
import spssio.por.PORValue;
import spssio.por.PORMatrix;
import spssio.por.PORRawMatrix;
import spssio.por.PORConstants;
import spssio.por.PORSection;



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
    extends POROutputWriter
{
    
    // MEMBER VARIABLES
    //==================
    
    /**
     * Reference to the PORFile currently being serialized.
     * Valid only during the invocation of output.
     */
    private PORFile por;
    
    // CONSTRUCTORS
    //==============
    
    public PORWriter() {
        super();
        
        por = null;
    }
    
    public PORWriter(OutputStream os) {
        this(); // Call default ctor
        
        // Bind directly to the specified stream
        bind(os);
    }
    
    // OTHER METHODS
    //===============
    
    protected void addSection(int tag, Object obj) {
        por.sections.add(new PORSection(tag, obj));
    }
    
    public void output(PORFile por, String filename) 
        throws FileNotFoundException, IOException, SecurityException
    {
        // Remember the PORFile object being outputted
        this.por = por;
        
        // Remove previous sectionization
        por.sections.clear();
        
        // Sections:
        // 1.   Header (pseudo-section)
        // 2.   Software
        // 3.   Author
        // 4.   Title
        // 5.   Variable count
        // 6.   Precision
        // 7.   Weight variable name
        // 8a.  Variable record
        // 8b.  Any missing values
        // 8c.  Variable label
        // 9.   Value-Label map
        // 10.  Documents
        // 11.  Data matrix

        
        // This is a pseudo-section
        addSection(PORSection.TAG_HEADER, por.header);
        
        if (por.software != null) {
            addSection(PORSection.TAG_SOFTWARE, por.software);
        }
        
        if (por.author != null) {
            addSection(PORSection.TAG_AUTHOR, por.author);
        }

        if (por.title != null) {
            addSection(PORSection.TAG_TITLE, por.title);
        }
        
        // Variable count is mandatory
        addSection(PORSection.TAG_VARIABLE_COUNT, por.variableCount);
        
        // Precision is probably mandatory
        addSection(PORSection.TAG_PRECISION, por.precision);
        
        if (por.weightVariableName != null) {
            addSection(PORSection.TAG_WEIGHT_VARIABLE, 
                por.weightVariableName);
        }
        
        int len = por.variables.size();
        for (int index = 0; index < len; index++) {
            PORVariable v = por.variables.get(index);
            addSection(PORSection.TAG_VARIABLE_RECORD, v);
            // Serialize missing values, if any
            for (PORMissingValue missingValue : v.missvalues) {
                // Discriminate
                switch(missingValue.type) {
                    case PORMissingValue.TYPE_DISCRETE_VALUE:
                        addSection(PORSection.TAG_MISSING_DISCRETE, missingValue);
                        break;
                    case PORMissingValue.TYPE_RANGE_OPEN_LO:
                        addSection(PORSection.TAG_MISSING_OPEN_LO, missingValue);
                        break;
                    case PORMissingValue.TYPE_RANGE_OPEN_HI:
                        addSection(PORSection.TAG_MISSING_OPEN_HI, missingValue);
                        break;
                    case PORMissingValue.TYPE_RANGE_CLOSED:
                        addSection(PORSection.TAG_MISSING_RANGE, missingValue);
                        break;
                    default:
                        // Ignore. TODO: Error?
                        break;
                } // switch
            } // for: each missing value
            
            // Variable label, if any
            if (v.label != null) {
                addSection(PORSection.TAG_VARIABLE_LABEL, v.label);
            }
        } // for: each variable
        
        len = por.labels.size();
        for (int index = 0; index < len; index++) {
            PORValueLabels vlMap = por.labels.get(index);
            addSection(PORSection.TAG_VALUE_LABELS, vlMap);
        }
        
        // Documents, if any
        if (por.documents != null) {
            addSection(PORSection.TAG_DOCUMENTS_RECORD, por.documents);
        }
        
        // Data matrix
        addSection(PORSection.TAG_DATA_MATRIX, por.data);
        
        // Output the sections
        outputSections(por, filename);
        
        // Forget the PORFile
        this.por = por;
    }
    
    
    public void outputSections(PORFile por, String filename) 
        throws FileNotFoundException, IOException, SecurityException
    {
        
        // Create a fname object
        File f = new File(filename);
        
        // May throw FileNotFound
        FileOutputStream fis = new FileOutputStream(filename);
        
        // Remember the SAVFile object being outputted
        // TODO: This should probably be a parameter of bind()
        this.por = por;
        
        // Connect base writer to the created stream,
        // and reset location information
        bind(fis);
        
        // Open file, setup base writer
        for (PORSection section : por.sections) {
            outputPORSection(section);
        }
        
        // May throw IOException, close quietly.
        try {
            unbind(); // Does flushing
            fis.close();
        } catch(IOException ex) {
            System.err.printf("%s: FileInputStream.close() failed. Ignoring.\n", filename);
        } // try-catch
        
        // Forget the SAVFile
        this.por = null;
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
        // TODO: Should the implementation be here?
        return null;
    }

    // OUTPUT PRIMITIVES
    //===================
    
    public void outputPORSection(PORSection section) 
        throws IOException
    {
        // Pass-forward
        //outputSection(section.getTag(), section.getObject());
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
    @SuppressWarnings("unchecked")
    public void outputSection(int tag, Object obj) 
        throws IOException
    {
        switch(tag) {
            
            case PORSection.TAG_HEADER: // this is a pseudo-section
                outputHeader((PORHeader) obj);
                break;
            
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
                // NOTE: Unchecked cast
                outputDocumentsRecord((Vector<String>) obj);
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
    
    
    // This does not work yet
    
    public void outputHeader(PORHeader header)
        throws IOException
    {
        /*
        // Convert splash byte array into String array
        String[] splash = null;
        if (header.splash != null) {
            splash = new String[5];
        }
        
        // Convert charset byte array into int array
        int[] charset = null;
        if (header.charset != null) {
            charset = new int[header.charset.length];
            for (int i = 0; i < header.charset.length; i++) {
                charset[i] = ((int) header.charset[i]) & 0xff;
            }
        }
        */
        
        outputHeader(
            null,                 // TODO
            header.translation,
            header.signature,
            header.version,
            header.date,
            header.time
        );
    }
    

    public void outputHeader(
        String[] splash,
        byte[] translation,
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
        outputTranslation(translation);
        
        // TODO: Set encoding
        
        // Write the 8-byte signature
        outputFormatSignature(signature);
        
        // Write the 1-byte format identifier
        outputFormatVersion(version);
        
        // Write the 8-byte creation date, and the 6-byte creation time.
        outputCreationTimestamp(date, time);
        
    } // output_header
    
    /**
     * Output splash strings. Each splash string has to have a length 40,
     * and there must be 5 splash strings altogether.
     */
    public void outputSplashStrings(String[] splash) 
        throws IOException
    {
        // If unspecified, use defaults.
        if (splash == null) {
            splash = PORConstants.DEFAULT_SPLASHES;
        }
        
        if (splash.length != 5) throw new IllegalArgumentException();
        
        for (int i = 0; i < 5; i++) {
            String s = splash[i];
            if (s.length() != 40) throw new IllegalArgumentException();
            write(s);
        } // for: each splash string
    }

    /**
     * Output the character set used.
     * NOTE: The method sets the encoding table accordingly.
     *
     */
    public void outputTranslation(byte[] translation) 
        throws IOException
    {
        // If unspecified, use default charset
        // TODO: Don't do this...
        if (translation == null) {
            PORCharset.assignDefaultTranslation(translation);
        }
        
        // Verify the length of the charset
        if (translation.length != 256) {
            throw new IllegalArgumentException();
        }
        
        // Pick the value of zero for unmapped entries
        byte zero = translation[PORCharset.DIGIT_0];
        
        for (int i = 0; i < 256; i++) {
            
            int c = ((int) (translation[i])) & 0xff;
            
            if (c == -1) {
                // If the char is unmapped, output zero.
                write(zero);
            } else {
                // Otherwise, write the char
                // (value is ANDed with 0xff inside write() method)
                write(c);
            }
        } // for: each char
        
        // Set encoding table.
        //setEncoding(charset);
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
            time = sdf.format(new Date());
        }
        
        if (date.length() != 8) {
            throw new IllegalArgumentException(String.format(
                "Date must be exactly 8 chars long: %s", date));
        }
        if (time.length() != 6) {
            throw new IllegalArgumentException(String.format(
                "Time must be exactly 6 chars long: %s", time));
        }

        // NOTE: These are true strings with lengths, and they could 
        // therefore, in theory, be longer or shorter than their 
        // expected lengths are.
        
        // Output 8-byte date
        outputString(date);
        
        // Output 6-byte time
        outputString(time);
    }
    
    //=======================================================================
    // PORTABLE SECTIONS OUTPUT METHODS
    //=======================================================================
    
    /*
     * Output an author section.
     *
     */
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
    
    /*
     * Output a software section.
     *
     */
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
    
    /*
     * Output a title section.
     *
     */
    public void outputTitle(String title) 
        throws IOException
    {
        if (title.length() > PORConstants.MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException();
        }
        
        outputTag(PORSection.TAG_TITLE);
        outputString(title);
    }
    
    /*
     * Output a variable count section.
     *
     */
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

    /*
     * Output a numeric precision section.
     *
     */
    public void outputNumericPrecision(int precision) 
        throws IOException
    {
        if (precision < 1) {
            throw new IllegalArgumentException();
        }
        
        outputTag(PORSection.TAG_PRECISION);
        outputInt(precision);
    }
    
    /*
     * Output weight variable name section.
     *
     */
    public void outputWeightVariable(String weight_var_name) 
        throws IOException
    {
        if (weight_var_name.length() >= PORConstants.MAX_VARNAME_LENGTH) {
            throw new IllegalArgumentException();
        }
        
        outputTag(PORSection.TAG_WEIGHT_VARIABLE);
        outputString(weight_var_name);
    }
    
    /*
     * Output a variable record section.
     *
     */
    public void outputVariableRecord(PORVariable pvar) 
        throws IOException
    {
        if (pvar.name.length() == 0) {
            throw new IllegalArgumentException("Variable unnamed");
        }
        if (pvar.name.length() > PORConstants.MAX_VARNAME_LENGTH) {
            throw new IllegalArgumentException(String.format(
                "Variable name too long: %s (only 8 chars allowed)",
                pvar.name.length()));
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
    
    /*
     * Output a variable record section.
     *
     */
    public void outputVariableRecord(
        int width,
        String name,
        int printtype,
        int printwidth,
        int printdecimals,
        int writetype,
        int writewidth,
        int writedecimals
    ) 
        throws IOException
    {
        if (name.length() == 0) {
            throw new IllegalArgumentException("Variable unnamed");
        }
        if (name.length() >= PORConstants.MAX_VARNAME_LENGTH) {
            throw new IllegalArgumentException(String.format(
                "Variable name too long: %s (only 8 chars allowed)",
                name.length()));
        }
        
        // Tag code
        outputTag(PORSection.TAG_VARIABLE_RECORD);
        
        // width and name
        outputInt(width);
        outputString(name);
        
        // output format: type, width, decimals
        outputInt(printtype);
        outputInt(printwidth);
        outputInt(printdecimals);
        
        // input format: type, width, decimals
        outputInt(writetype);
        outputInt(writewidth);
        outputInt(writedecimals);
    }
    
    // Missing value: discrete
    //========================
    
    /*
     * Output a missing value record.
     *
     */
    public void outputMissingDiscrete(PORMissingValue miss) 
        throws IOException
    {
        // Tag code
        outputTag(PORSection.TAG_MISSING_DISCRETE);
        
        // Value (depends on the variable's type)
        outputPORValue(miss.values[0]);
    }
    
    /*
     * Output a missing value record.
     *
     */
    public void outputMissingDiscrete(double value) 
        throws IOException
    {
        // Tag code
        outputTag(PORSection.TAG_MISSING_DISCRETE);
        
        // Value
        outputDouble(value);
    }

    /*
     * Output a missing value record.
     *
     */
    public void outputMissingDiscrete(String value) 
        throws IOException
    {
        // Tag code
        outputTag(PORSection.TAG_MISSING_DISCRETE);
        
        // Value
        outputString(value);
    }

    // Missing value: range open LO
    //=============================
    
    /*
     * Output a missing value record.
     *
     */
    public void outputMissingRangeOpenLo(PORMissingValue miss) 
        throws IOException
    {
        // Tag code
        outputTag(PORSection.TAG_MISSING_OPEN_LO);
        
        // Value (depends on the variable's type)
        outputPORValue(miss.values[0]);
    }
    
    /*
     * Output a missing value record.
     *
     */
    public void outputMissingRangeOpenLo(double value) 
        throws IOException
    {
        // Tag code
        outputTag(PORSection.TAG_MISSING_OPEN_LO);
        
        // Value
        outputDouble(value);
    }

    // Missing value: range open HI
    //=============================
    
    /*
     * Output a missing value record.
     *
     */
    public void outputMissingRangeOpenHi(PORMissingValue miss) 
        throws IOException
    {
        // Tag code
        outputTag(PORSection.TAG_MISSING_OPEN_HI);
        
        // Value (depends on the variable's type)
        outputPORValue(miss.values[0]);
    }

    /*
     * Output a missing value record.
     *
     */
    public void outputMissingRangeOpenHi(double value) 
        throws IOException
    {
        // Tag code
        outputTag(PORSection.TAG_MISSING_OPEN_HI);
        
        // Value
        outputDouble(value);
    }

    // Missing value: range open closed
    //=================================
    
    /*
     * Output a missing value record.
     *
     */
    public void outputMissingRangeClosed(PORMissingValue miss) 
        throws IOException
    {
        // Tag code
        outputTag(PORSection.TAG_MISSING_RANGE);
        
        // Values (depends on the variable's type)
        outputPORValue(miss.values[0]);
        outputPORValue(miss.values[1]);
    }

    /*
     * Output a missing value record.
     *
     */
    public void outputMissingRangeClosed(double min, double max) 
        throws IOException
    {
        // Tag code
        outputTag(PORSection.TAG_MISSING_RANGE);
        
        // Values
        outputDouble(min);
        outputDouble(max);
    }
    
    // Variable label
    //================
    
    /*
     * Output a variable label section
     *
     */
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
    
    // Value labels
    //==============

    /*
     * Output a value-label mappings section
     *
     */
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

    /*
     * Output a value-label mappings section
     *
     */
    public void outputNumericValueLabelsRecord(
        Collection<String> varNames,
        Map<Double, String> map
    ) 
        throws IOException
    {
        // Tag code
        outputTag(PORSection.TAG_VALUE_LABELS);
        
        // Number of variables
        outputInt(varNames.size());
        
        // Output variable names
        for (String name : varNames) {
            outputString(name);
        }
        
        // Number of value-label pairs
        outputInt(map.size());
        for (Map.Entry<Double, String> entry : map.entrySet()) {
            outputDouble(entry.getKey());
            outputString(entry.getValue());
        }
    }
    
    // TODO: A method called outputStringValueLabelsRecord()

    /*
     * Output a value-label mappings section
     *
     */
    public void outputValueLabelsRecord(
        Collection<String> varNames,
        Collection<Object> map
    ) 
        throws IOException
    {
        // Get map array length
        int size = map.size();
        
        // Verify that the array has even length.
        if ((size & 0x01) == 1) {
            throw new RuntimeException(String.format(
                "Value labels array is must be even-lengthed, not: %d",
                size));
        }
        
        // Verify that the array is unemptys.
        if (size == 0) {
            throw new RuntimeException("Empty value-label map array");
        }
        
        // Output tag code
        outputTag(PORSection.TAG_VALUE_LABELS);
        
        // Output the number of variables
        outputInt(varNames.size());
        
        // Output the variable names
        for (String name : varNames) {
            outputString(name);
        }
        
        // Output the number of value-label pairs
        outputInt(map.size() / 2);
        
        int phase = 0;
        Class<?> valclass = null;
        
        // Output the value-albel pairs
        for (Object obj : map) {
            if (phase == 0) {
                // Output value
                
                if (valclass == null) {
                    if (obj instanceof Number) {
                        valclass = Number.class;
                    } else if (obj instanceof String) {
                        valclass = String.class;
                    }
                    // If neither Double nor String, an exception is raised
                    // in the below code.
                } else {
                    // Verify the dynamic type
                    if (valclass.isInstance(obj) == false) {
                        throw new RuntimeException(String.format(
                            "Object in value-label map array has incoherent class %s (expected %s)",
                            obj.getClass().getName(), valclass.getName()));
                    }
                } // if-else: valclass set?
                
                if (obj instanceof Integer) {
                    outputInt((Integer) obj);
                } else if (obj instanceof Double) {
                    outputDouble((Double) obj);
                } else if (obj instanceof String) {
                    outputString((String) obj);
                } else {
                    throw new RuntimeException(String.format(
                        "Object in value-label map array has unexpected class: %s",
                        obj.getClass().getName()));
                }
                
                phase = 1;
            } else {
                // Output label
                outputString((String) obj);
                
                phase = 0;
            } // if-else
        } // for: each entry
    } // outputValueLabelsRecord()

    /*
     * Output documents record
     *
     */
    public void outputDocumentsRecord(Vector<String> documents)
        throws IOException
    {
        // Tag code
        outputTag(PORSection.TAG_DOCUMENTS_RECORD);
        
        // Number of lines
        outputInt(documents.size());
        
        // Output variable names
        for (String line : documents) {
            outputString(line);
        }
    }
    
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
        // Select serialization method
        
        if (data instanceof PORRawMatrix) {
            PORRawMatrix rawMatrix = (PORRawMatrix) data;
            outputRawMatrixRecord(rawMatrix);
        } else {
            throw new RuntimeException(String.format(
                "Serialization of PORMatrix with dynamic type %s not implemented yet",
                data.getClass().getName()));
        } // if-else
    }
    
    protected void outputRawMatrixRecord(PORRawMatrix rawMatrix) 
        throws IOException
    {
        outputTag(PORSection.TAG_DATA_MATRIX);
        
        // Retrieve the raw SequentialByteArray object
        SequentialByteArray data = rawMatrix.getRawArray();
        
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
            
            if (c == PORConstants.EOF_MARKER) { // 'Z'
                if (lastc != PORConstants.EOF_MARKER) { // 'Z'
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
        
        if (endoffset == -1) {
            // default to end-of-data
            endoffset = data.size();
        } else {
            // Exclude the EOF_MARKER itself
            endoffset--;
        }
        
        // seek to the beginning
        data.seek(0);
        int data_col = rawMatrix.getTextColumn0();
        int data_row_length = rawMatrix.getTextRowLength();
        
        for (int i = 0; i < endoffset; i++) {
            c = data.read();
            if (c == '\n') {
                
                if (data_col < data_row_length) {
                    // fill the end of the line with whitespaces
                    for (; data_col < data_row_length; data_col++) {
                        write(PORConstants.WHITESPACE);
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
        
        // Finish with eof marker
        outputEofMarkers();
        
        // and we are done!
    }
    
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
                // Take the current precision settings into account
                // while serializing this number.
                outputNumberAfterReformat(pvalue.value);
                break;
            default:
                throw new RuntimeException();
        } // switch
    }

} // class PORWriter
