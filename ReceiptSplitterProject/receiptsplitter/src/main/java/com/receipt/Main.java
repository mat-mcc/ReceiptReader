package com.receipt;

/**
 * 
 * DEPRECATED, UI ONLY NOW
 * Application entry point. Runs "live" terminal loop: type "scan" to pick and
 * process a receipt image, "quit" to exit. Stays running so you can
 * process multiple receipts without restarting Maven each time.
 *
 * All actual OCR parsing logic lives in ReceiptScanner 
 * ALL table definition and helper logic lives in ReceiptTableModel
 * ALL UI definition and helper logic lives in ReceiptEditorFrame
 * ALL help with placeholder/hint text lives in PlaceholderTextField
 */

// MAIN DEF
public class Main {
public static void main(String[] args) {

    // Tessdata Path argument
    if (args.length < 1) {
        System.out.println("Usage: mvn exec:java -Dexec.args=\"<tessdata_path>\"");
        return;
    }

    // otherwise call ReceiptEditorFrame, WITH NUMBER OF PEOPLE

    // !!! HARDCODED NUMBER OF PEOPLE INVOLVED !!!
        int numberOfPeople = 6;


    javax.swing.SwingUtilities.invokeLater(() ->
            new ReceiptEditorFrame(args[0], numberOfPeople));
}
}