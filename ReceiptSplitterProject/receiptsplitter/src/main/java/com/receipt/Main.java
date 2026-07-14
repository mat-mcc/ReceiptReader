package com.receipt;

import java.io.File;
import java.util.Scanner;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.sourceforge.tess4j.TesseractException;

/**
 * CLI entry point. Runs a live terminal loop: type "scan" to pick and
 * process a receipt image, "quit" to exit. Stays running so you can
 * process multiple receipts without restarting Maven each time.
 *
 * All actual scanning/OCR/parsing logic lives in ReceiptScanner —
 * this class is just the driver that wires it together.
 */
public class Main {

    public static void main(String[] args) throws TesseractException {
        if (args.length < 1) {
            System.out.println("Usage: mvn exec:java -Dexec.args=\"<tessdata_path>\"");
            return;
        }

        String tessDataPath = args[0];
        ReceiptScanner scanner = new ReceiptScanner(tessDataPath);
        Scanner terminal = new Scanner(System.in);

        System.out.println("Receipt Scanner ready.");
        printHelp();

        while (true) {
            System.out.print("\n> ");
            String command = terminal.nextLine().trim().toLowerCase();

            switch (command) {
                case "scan":
                    handleScan(scanner);
                    break;
                case "quit":
                case "exit":
                    System.out.println("Goodbye.");
                    return;
                case "help":
                    printHelp();
                    break;
                default:
                    System.out.println("Unknown command. Type 'help' for options.");
            }
        }
    }

    private static void handleScan(ReceiptScanner scanner) {
        File imageFile = chooseImageFile();
        if (imageFile == null) {
            System.out.println("No file selected.");
            return;
        }

        try {
            ReceiptScanner.Receipt receipt = scanner.scan(imageFile);
            System.out.println(receipt.toFormattedText());
            System.out.println("--- CSV ---");
            System.out.println(receipt.toCsv());
        } catch (TesseractException e) {
            System.out.println("OCR failed: " + e.getMessage());
        }
    }

    private static void printHelp() {
        System.out.println("Commands:");
        System.out.println("  scan  - pick a receipt image and scan it");
        System.out.println("  quit  - exit the program");
        System.out.println("  help  - show this message");
    }

    /** Opens a native file picker dialog and returns the chosen image file, or null if cancelled. */
    private static File chooseImageFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a receipt image");
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Image files", "jpg", "jpeg", "png", "bmp", "tiff"));

        int result = chooser.showOpenDialog(null);
        return (result == JFileChooser.APPROVE_OPTION) ? chooser.getSelectedFile() : null;
    }
}