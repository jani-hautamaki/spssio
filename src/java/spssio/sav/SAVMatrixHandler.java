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

/* 
 * Here is a list of all names considered:
 *
 *     SAVMatrixDataListener
 *     SAVMatrixDataReceiver
 *     SAVMatrixEventListener
 *     SAVMatrixDataStreamReceiver
 *     SAVMatrixHandler
 *     SAVMatrixReceiver
 *     SAVMatrixVisitor
 *
 * Visitor was a good candidate. However, one of the primary 
 * characteristics of the visitor pattern the double dispatching, 
 * which is not found in this situation. 
 * Hence, the visitor suffix was dismissed.
 *
 * Receiver and DataReceiver were both also good candidates.
 *
 */
 
/*
 * Here is a list of all method names considered for the Matrix class:
 *
 *     PORMatrix.toReceiver(dataReceiver)
 *     PORMatrix.sendToReceiver(dataReceiver)
 *     PORMatrix.acceptReceiver(dataReceiver)
 *     PORMatrix.acceptVisitor(matrixVisitor)
 *     PORMatrix.traverse(dataReceiver)
 *     PORMatrix.iterate(Iteratee x)
 *     PORMatrix.performTraversal()
 *     PORMatrix.executeTraversal(dataReceiver)
 *     PORMatrix.visitWith(visitor)
 *     PORMatrix.sendToReceiver()
 *     PORMatrix.emitDataTo()
 *     PORMatrix.sendTo(dataReceiver)
 *     PORMatrix.emit(receiver)
 *
 * The verb "send" was dismissed, because it is strongly associated
 * with the idea that the object which is being sent will no longer 
 * be owned by the sender once it has been sent.
 *
 * The verb "accept" was dismissed, because it can be easily 
 * thought of as a passive action: either something happens 
 * or it is not allowed to happen. This is misleading, since
 * the method actually will do a lot of work, and the name should
 * reflect this fact. Hence, names like "executeAccept()" or something
 * like that 
 *

// (transitive, computing) 
// To visit all parts of; to explore thoroughly; 
// as, to traverse all nodes in a network.

// send on huono sana, koska lähettämiseen liittyy käsitys siitä,
että jotain fyysistä vaihtaa paikkaa lähettäjältä vastaanottajalle,
minkä jälkeen lähettäjälle ei enää ole lähetettyä asiaa.

// visit on parempi, koska siihen sisältyy ymmärys, että paikat,
joissa vieraillaan, säilyvät. 

// visit on verbinä parempi kuin accept, koska visit ilmaisee enemmän
// aktiivista toimintaa (vierailua) kuin vastaanottaminen, jossa
// aktiivinen toiminta liittyy ehkä enemmän siihen, että huolitaanko
// vaiko eikö - näin ainakin ESL spiikkerinä.


*/


/*
The term "Reader" implies an active worker,
which does the reading (active work) on command.
For instance:

    dataReader.read()
    
The term "Receiver", however, implies a passive recipient,
which shouldn't be commanded to receive, since "receiving"
implies that someone has WILLINGLY sent the data.

According to 

    http://cafeconleche.org/books/xmljava/chapters/ch06s03.html
    
SAX uses the Observer pattern to tell client applications what's
in a document.

According to

    http://java.dzone.com/articles/efficient-xml-processing-using

SAX is an event-driven method where you write a processor which
receives eents while the XML is being read, and this is known
as "stream parser".

The Observer design pattern enables *a subscriber* to *register* with
and receive notifications from a provider.

This implies unpredictable event stream

According to

    http://www.saxproject.org/event.html
    
"An event-based API, on the other hand, reports parsing events 
 (such as the start and end of elements) directly to the application 
 through callbacks, and does not usually build an internal tree. 
 The application implements handlers to deal with the different events, 
 much like handling events in a graphical user interface. 
 SAX is the best known example of such an API. "
 


SAVMatrixHandler

SAVMatrixContentHandler
SAVMatrixDataHandler


SAVMatrixIteratee



*/



/**
 * Iteratee is called repeatedly to process new chunks of data.
 *
 * This is an inversion of control with respect to Iterator design pattern.
 * You may think of Iteratee pattern as a single-dispatch variant of 
 * the Visitor pattern.
 *
 * However, this does not come without problems; in the Iteratee design
 * pattern has a characterstic, that the iteratee has the control over
 * iteration, ie. whether to continue or stop (with a final return value).
 * The 
 *
 * traverse() transforms the matrix data into a stream of *EVENTS*.
 * The events are then acted on *REACTIVELY*
 * 
 * See
 * http://www.playframework.com/documentation/2.0.2/Iteratees
 * http://en.wikipedia.org/wiki/Iteratee
 *
 */
 
//public interface SAVMatrixIteratee {

public interface SAVMatrixHandler {
    
    public void onMatrixBegin(int xsize, int ysize, int[] columnWidths);
    
    public void onMatrixEnd();
    
    public void onRowBegin(int y);
    
    public void onRowEnd(int y);
    
    public void onCellSysmiss(int x);
    
    public void onCellNumber(int x, double value);
    
    public void onCellInvalid(int x);
    
    public void onCellString(int x, String value);
    
} // class

