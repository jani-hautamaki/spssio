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



package spssio.por;

/**
 *
 * The splash string "ASCII SPSS PORT FILE" in different character sets.<p>
 *
 * According to PSPP's documentation
 * <a href="http://www.gnu.org/software/pspp/pspp-dev/html_node/Portable-File-Header.html#Portable-File-Header">
 *    A.3 Portable File Header
 * </a>:<p>
 *
 * The file begins with 200-byte header, which is divided into 5 sections
 * each having 40 bytes and representing the same string in a different character
 * character set encoding.<p>
 *
 * The splash string has the format "{@code <charset> SPSS PORT FILE}", where
 * {@code <charset>} is the name of the character set used in the file, eg. ASCII
 * or EBCDIC. Each 40-byte section is padded on the right with spaces in its
 * respective character set.<p>
 *
 * Citing the PSPP's documentation: "It appears that these strings exist only to
 * inform those who might view the file on a screen, and that they are not parsed
 * by SPSS products. Thus, they can be safely ignored."<p>
 *
 * According to the same documentation, the strings are suppoesd to be in
 * the following characters sets, in the specified order:
 * <ol>
 *    <li> EBCDIC,
 *    <li> 7-bit ASCII,
 *    <li> CDC 6-bit ASCII,
 *    <li> 6-bit ASCII,
 *    <li> Honeywell 6-bit ASCII.
 * </ol><p>
 *
 * <b>TODO</b>: The splash string in other character sets should be calculated
 * instead of using constants.<p>
 *
 */
public class PORSplashString
{

    /**
     * Literal <code>"SPSSPORT"</code>.
     * The 8-byte tag string consists of the exact characters SPSSPORT in
     * the portable file's character set, which can be used to verify that
     * the file is indeed a portable file.<p>
     */
    public static final int[] SIGNATURE = {
        0x53, 0x50, 0x53, 0x53, 0x50, 0x4f, 0x52, 0x54
    }; // SIGNATURE

    /**
     * 1st section:
     * <code>"?????@????@????@????@@@@@@@@@@@@@@@@"</code> (EBCDIC)
     */
    public static final int[] EBCDIC = {
        0xc1, 0xe2, 0xc3, 0xc9, 0xc9, 0x40, 0xe2, 0xd7, 0xe2, 0xe2,
        0x40, 0xd7, 0xd6, 0xd9, 0xe3, 0x40, 0xc6, 0xc9, 0xd3, 0xc5,
        0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40,
        0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40
    };

    /**
     * 2nd section:
     * <code>"ASCII SPSS PORT FILE&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"</code> (7-bit ASCII)
     */
    public static final int[] ASCII_7BIT = {
        0x41, 0x53, 0x43, 0x49, 0x49, 0x20, 0x53, 0x50, 0x53, 0x53,
        0x20, 0x50, 0x4f, 0x52, 0x54, 0x20, 0x46, 0x49, 0x4c, 0x45,
        0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20,
        0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20
    };

    /**
     * 3rd section:
     * <code>"00000-0000-0000-0000--------------------"</code> (CDC 6-bit ASCII)
     */
    public static final int[] CDC_ASCII_6BIT = {
        0x30, 0x30, 0x30, 0x30, 0x30, 0x2d, 0x30, 0x30, 0x30, 0x30,
        0x2d, 0x30, 0x30, 0x30, 0x30, 0x2d, 0x30, 0x30, 0x30, 0x30,
        0x2d, 0x2d, 0x2d, 0x2d, 0x2d, 0x2d, 0x2d, 0x2d, 0x2d, 0x2d,
        0x2d, 0x2d, 0x2d, 0x2d, 0x2d, 0x2d, 0x2d, 0x2d, 0x2d, 0x2d
    };

    /**
     * 4th section:
     * <code>"!3#))0303300/240&),%000000000000000000000"</code> (6-bit ASCII)
     */
    public static final int[] ASCII_6BIT = {
        0x21, 0x33, 0x23, 0x29, 0x29, 0x30, 0x33, 0x30, 0x33, 0x33,
        0x30, 0x30, 0x2f, 0x32, 0x34, 0x30, 0x26, 0x29, 0x2c, 0x25,
        0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30,
        0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30
    };

    /**
     * 5th section:
     * <code>"20200002'220'&)3000#000000000000000000000"</code> (Honeywell 6-bit ASCII)
     */
    public static final int[] HONEYWELL_ASCII_6BIT = {
        0x30, 0x32, 0x30, 0x30, 0x30, 0x30, 0x32, 0x27, 0x32, 0x32,
        0x30, 0x27, 0x26, 0x29, 0x33, 0x30, 0x30, 0x30, 0x23, 0x30,
        0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30,
        0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30
    };

    // CONSTRUCTORS
    //==============

    /**
     * Constructor intentionally disabled.
     */
    private PORSplashString() {
    } // ctor

} // class PORSplash