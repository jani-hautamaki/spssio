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


package spssio.sav;

/**
 * See PSPP documentation:
 * http://www.gnu.org/software/pspp/pspp-dev/html_node/Machine-Floating_002dPoint-Info-Record.html#Machine-Floating_002dPoint-Info-Record
 *
 * The PSPP system-missing value is represented by the largest possible negative number
 * in the floating point format (-DBL_MAX).
 * Two other values are important for use as missing values: HIGHEST,
 * represented by the largest possible positive number (DBL_MAX),
 *  and LOWEST, represented by the second-largest negative number
 * (in IEEE 754 format, 0xffeffffffffffffe).
 *
 */
public class SAVExtSystemConfig
    extends SAVExtensionRecord
{
    // MEMBER VARIABLES
    //==================

    /**
     * Software version number, major part
     */
    public int versionMajor;

    /**
     * Software version number, minor part
     */
    public int versionMinor;

    /**
     * Software version number, revision part
     */
    public int versionRevision;

    /**
     * Machine code
     */
    public int machineCode;

    /**
     * Floating-point representation format
     */
    public int fpFormat;

    /**
     * Compression code ??
     */
    public int compression;

    /**
     * System endiannes.
     * Values: 1 (Big-Endian), 2 (Little-Endian).
     */
    public int systemEndianness;

    /**
     * Code page used in string encoding.
     * Values: 1 (EBCDIC), 2 (7-bit ASCII), 3 (8-bit ASCII), 4 (DEC Kanji),
     * Windows code page numbers are also valid.
     */
    public int stringCodepage;


    // CONSTRUCTORS
    //==============

    public SAVExtSystemConfig() {
        versionMajor = 0;
        versionMinor = 0;
        versionRevision = 0;
        machineCode = 0;
        fpFormat = 0;
        compression = 0;
        systemEndianness = 0;
        stringCodepage = 0;
    }

} // class SAVExtSystemConfig
