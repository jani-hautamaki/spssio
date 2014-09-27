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

public class SAVExtensionRecord
{

    // MEMBER VARIABLES
    //==================

    /**
     * Sub tag code
     */
    public int subtag;

    /**
     * Data element size in bytes
     */
    public int elementSize;

    /**
     * Number of data elements, each "elementSize" bytes long.
     */
    public int numberOfElements;

    /**
     * Byte array for the raw data
     * TODO: Use SequentialByteArray for easier access?
     */
    public byte[] data;

    // CONSTRUCTORS
    //==============

    public SAVExtensionRecord() {
        subtag = -1; // hopefully invalid. TODO
        elementSize = 0;
        numberOfElements = 0;
        data = null;
    }

    public SAVExtensionRecord(SAVExtensionRecord other) {
        this();
        copy(other);
    }

    // OTHER METHODS
    //===============

    public void copy(SAVExtensionRecord other) {
        subtag = other.subtag;
        elementSize = other.elementSize;
        numberOfElements = other.numberOfElements;
        data = other.data;
    }

}

