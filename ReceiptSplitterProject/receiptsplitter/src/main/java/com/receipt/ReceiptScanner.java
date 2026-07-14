package com.receipt;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

/**
 * Reads a receipt image via local OCR (Tesseract) and produces a
 * structured, formatted breakdown of items, subtotal, tax, and total.
 *
 * Parsing OCR text into a Receipt is handled by a chain of ReceiptParser
 * strategies (see nested classes below) — different stores/POS systems
 * format receipts differently, so we try each known format and keep
 * whichever parse looks most plausible (most items found).
 */



/*
        CLASS RECEIPT
        public final List<LineItem> items;
        public final Double subtotal;
        public final Double tax;
        public final Double total; 
        
*/

/*
        CLASS LINEITEM
        public final String name;
        public final double price; 
        
*/

public class ReceiptScanner {

    private final Tesseract tesseract;
    private final ReceiptParserChain parserChain;

    public ReceiptScanner(String tessDataPath) {
        this(tessDataPath, ReceiptParserChain.defaultChain());
    }

    public ReceiptScanner(String tessDataPath, ReceiptParserChain parserChain) {
        this.parserChain = parserChain;
        tesseract = new Tesseract();
        tesseract.setDatapath(tessDataPath);
        tesseract.setLanguage("eng");
        // Page segmentation mode 6 = "assume a single uniform block of text"
        // works well for receipts (as opposed to full-page documents)
        tesseract.setPageSegMode(6);
        // Tell Tesseract to assume 300 DPI rather than guessing from a
        // low-res phone photo, which is what causes "image too small" errors
        tesseract.setVariable("user_defined_dpi", "300");
    }

    /** Runs OCR on the given image and returns raw extracted text. */
    public String extractText(File imageFile) throws TesseractException {
        try {
            BufferedImage preprocessed = preprocess(ImageIO.read(imageFile));
            return tesseract.doOCR(preprocessed);
        } catch (IOException e) {
            throw new TesseractException("Could not read image file: " + e.getMessage(), e);
        }
    }

    /**
     * Full preprocessing pipeline: upscale if too small, convert to grayscale,
     * then apply adaptive thresholding to binarize the image (pure black/white).
     * This is what makes OCR robust to uneven lighting, shadows, and glare —
     * a single global brightness cutoff fails on real phone photos, but
     * comparing each pixel to its local neighborhood doesn't.
     */
    private BufferedImage preprocess(BufferedImage original) {
        BufferedImage upscaled = upscaleIfSmall(original);
        BufferedImage grayscale = toGrayscale(upscaled);
        return adaptiveThreshold(grayscale, 0.15);
    }

    /**
     * Upscales small images so Tesseract has enough resolution to work with.
     * Phone photos are often fine, but cropped/low-res images trigger
     * "Image too small to scale" errors without this.
     */
    private BufferedImage upscaleIfSmall(BufferedImage original) {
        int minWidth = 1500; // target width; receipts are narrow, so this gives good detail
        if (original.getWidth() >= minWidth) {
            return original;
        }

        double scale = (double) minWidth / original.getWidth();
        int newWidth = minWidth;
        int newHeight = (int) (original.getHeight() * scale);

        BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, newWidth, newHeight, null);
        g.dispose();
        return scaled;
    }

    /** Converts to 8-bit grayscale. */
    private BufferedImage toGrayscale(BufferedImage original) {
        BufferedImage gray = new BufferedImage(
                original.getWidth(), original.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(original, 0, 0, null);
        g.dispose();
        return gray;
    }

    /**
     * Adaptive (local) thresholding using Bradley's algorithm: each pixel is
     * compared against the average brightness of its surrounding window,
     * rather than one fixed cutoff for the whole image. This is what handles
     * shadows, glare, and uneven lighting — regions that a global threshold
     * would misjudge as "too dark" or "too bright" get evaluated relative
     * to their own neighborhood instead.
     *
     * @param t sensitivity, ~0.15 works well for text documents; higher = more aggressive
     */
    private BufferedImage adaptiveThreshold(BufferedImage grayscale, double t) {
        int width = grayscale.getWidth();
        int height = grayscale.getHeight();
        int windowSize = Math.max(width, height) / 8; // local neighborhood size
        int s2 = windowSize / 2;

        int[][] pixels = new int[height][width];
        long[][] integral = new long[height + 1][width + 1];

        // Read pixel values and build integral (summed-area) image for fast window sums
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y][x] = grayscale.getRaster().getSample(x, y, 0);
                integral[y + 1][x + 1] = pixels[y][x]
                        + integral[y][x + 1] + integral[y + 1][x] - integral[y][x];
            }
        }

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int x1 = Math.max(0, x - s2);
                int x2 = Math.min(width - 1, x + s2);
                int y1 = Math.max(0, y - s2);
                int y2 = Math.min(height - 1, y + s2);
                int count = (x2 - x1 + 1) * (y2 - y1 + 1);

                long sum = integral[y2 + 1][x2 + 1] - integral[y1][x2 + 1]
                        - integral[y2 + 1][x1] + integral[y1][x1];

                boolean isForeground = (pixels[y][x] * count) < (sum * (1 - t));
                int rgb = isForeground ? 0x000000 : 0xFFFFFF;
                result.setRGB(x, y, rgb);
            }
        }

        return result;
    }

    /** Parses raw OCR text into a structured Receipt, trying each known format. */
    public Receipt parseReceipt(String ocrText) {
        return parserChain.tryParse(ocrText);
    }

    /** Convenience: scan an image straight through to a formatted Receipt. */
    public Receipt scan(File imageFile) throws TesseractException {
        return parseReceipt(extractText(imageFile));
    }

    // =========================================================================
    // Data model
    // =========================================================================

    public static class LineItem {
        public final String name;
        public final double price;

        public LineItem(String name, double price) {
            this.name = name;
            this.price = price;
        }
    }

    /** Structured receipt output, ready to hand off to the splitting logic. */
    public static class Receipt {
        public final List<LineItem> items;
        public final Double subtotal;
        public final Double tax;
        public final Double total;

        public Receipt(List<LineItem> items, Double subtotal, Double tax, Double total) {
            this.items = items;
            this.subtotal = subtotal;
            this.tax = tax;
            this.total = total;
        }

        /** Human-readable formatted text output. */
        public String toFormattedText() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== RECEIPT ===\n");
            for (LineItem item : items) {
                sb.append(String.format("%-30s $%6.2f%n", item.name, item.price));
            }
            sb.append("-".repeat(38)).append("\n");
            if (subtotal != null) sb.append(String.format("%-30s $%6.2f%n", "Subtotal", subtotal));
            if (tax != null) sb.append(String.format("%-30s $%6.2f%n", "Tax", tax));
            if (total != null) sb.append(String.format("%-30s $%6.2f%n", "Total", total));
            return sb.toString();
        }

        /** Machine-readable output (simple CSV) for feeding into the app. */
        public String toCsv() {
            StringBuilder sb = new StringBuilder("name,price\n");
            for (LineItem item : items) {
                sb.append(String.format("\"%s\",%.2f%n", item.name.replace("\"", "'"), item.price));
            }
            if (subtotal != null) sb.append(String.format("Subtotal,%.2f%n", subtotal));
            if (tax != null) sb.append(String.format("Tax,%.2f%n", tax));
            if (total != null) sb.append(String.format("Total,%.2f%n", total));
            return sb.toString();
        }
    }

    // =========================================================================
    // Parser strategy interface + implementations
    // =========================================================================

    /** A strategy for turning raw OCR text into a structured Receipt. */
    public interface ReceiptParser {
        /** Never returns null — returns a Receipt with an empty items list if the format isn't recognized. */
        Receipt tryParse(String ocrText);
    }

    /**
     * Handles receipts where each item line includes a UPC/barcode column,
     * e.g.: "LIPTON       012000142080 F   3.47 N"
     * Common on grocery-store receipts (Walmart, Target, etc.).
     */
    public static class BarcodeFormatParser implements ReceiptParser {

        private static final Pattern SUMMARY_LINE = Pattern.compile("^(.+?)\\s+\\$?(\\d+\\.\\d{2})\\s*$");
        private static final Pattern BARCODE_TOKEN = Pattern.compile("\\d{8,14}[A-Z]{0,2}");
        private static final Pattern PRICE_TOKEN = Pattern.compile("\\$?\\d+\\.\\d{2}");

        @Override
        public Receipt tryParse(String ocrText) {
            List<LineItem> items = new ArrayList<>();
            Double subtotal = null, tax = null, total = null;

            for (String rawLine : ocrText.split("\\r?\\n")) {
                String line = rawLine.trim();
                if (line.isEmpty()) continue;

                String[] tokens = line.split("\\s+");

                int barcodeIdx = -1;
                for (int i = 0; i < tokens.length; i++) {
                    if (BARCODE_TOKEN.matcher(tokens[i]).matches()) {
                        barcodeIdx = i;
                        break;
                    }
                }

                if (barcodeIdx > 0) {
                    Double price = null;
                    for (int i = barcodeIdx + 1; i < tokens.length; i++) {
                        if (PRICE_TOKEN.matcher(tokens[i]).matches()) {
                            price = Double.parseDouble(tokens[i].replace("$", ""));
                            break;
                        }
                    }
                    if (price == null) continue;

                    String name = String.join(" ", Arrays.copyOfRange(tokens, 0, barcodeIdx)).trim();
                    if (!name.isEmpty()) {
                        items.add(new LineItem(name, price));
                    }
                } else {
                    Matcher m = SUMMARY_LINE.matcher(line);
                    if (!m.matches()) continue;

                    String lowerName = m.group(1).trim().toLowerCase();
                    double price = Double.parseDouble(m.group(2));

                    if (lowerName.contains("subtotal")) {
                        subtotal = price;
                    } else if (lowerName.contains("tax")) {
                        tax = price;
                    } else if (lowerName.contains("total")) {
                        total = price;
                    }
                }
            }

            return new Receipt(items, subtotal, tax, total);
        }
    }

    /**
     * Handles simple receipts where each line is just "NAME ... PRICE" with
     * no barcode or extra columns, e.g.: "Chicken Tacos       12.99"
     * Common on restaurant and small-retailer receipts.
     */
    public static class SimplePriceLineParser implements ReceiptParser {

        private static final Pattern PRICE_LINE = Pattern.compile("^(.+?)\\s+\\$?(\\d+\\.\\d{2})\\s*$");

        @Override
        public Receipt tryParse(String ocrText) {
            List<LineItem> items = new ArrayList<>();
            Double subtotal = null, tax = null, total = null;

            for (String rawLine : ocrText.split("\\r?\\n")) {
                String line = rawLine.trim();
                if (line.isEmpty()) continue;

                Matcher m = PRICE_LINE.matcher(line);
                if (!m.matches()) continue;

                String name = m.group(1).trim();
                double price = Double.parseDouble(m.group(2));
                String lower = name.toLowerCase();

                if (lower.contains("subtotal")) {
                    subtotal = price;
                } else if (lower.contains("tax")) {
                    tax = price;
                } else if (lower.contains("total")) {
                    total = price;
                } else if (lower.contains("tip") || lower.contains("balance") || lower.contains("change")) {
                    // ignore
                } else {
                    items.add(new LineItem(name, price));
                }
            }

            return new Receipt(items, subtotal, tax, total);
        }
    }

    /**
     * Tries multiple ReceiptParser strategies in order and keeps whichever
     * one extracted the most items — a simple but effective heuristic, since
     * a parser that doesn't match the receipt's format typically finds zero
     * or very few items.
     */
    public static class ReceiptParserChain implements ReceiptParser {

        private final List<ReceiptParser> parsers;

        public ReceiptParserChain(List<ReceiptParser> parsers) {
            this.parsers = parsers;
        }

        /** Default chain covering the formats we currently know how to handle. */
        public static ReceiptParserChain defaultChain() {
            return new ReceiptParserChain(Arrays.asList(
                    new BarcodeFormatParser(),
                    new SimplePriceLineParser()
            ));
        }

        @Override
        public Receipt tryParse(String ocrText) {
            Receipt best = null;

            for (ReceiptParser parser : parsers) {
                Receipt candidate = parser.tryParse(ocrText);
                if (best == null || candidate.items.size() > best.items.size()) {
                    best = candidate;
                }
            }

            return best != null ? best : new Receipt(new ArrayList<>(), null, null, null);
        }
    }

}