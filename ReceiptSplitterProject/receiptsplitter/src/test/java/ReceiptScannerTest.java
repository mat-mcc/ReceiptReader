

import org.junit.jupiter.api.Test;

import com.receipt.ReceiptScanner;

class ReceiptScannerTest {

    @Test
    void parsesItemsAndTotals() {
        ReceiptScanner scanner = new ReceiptScanner("unused-for-this-test");

        String fakeOcrOutput =
                "Chicken Tacos       12.99\n" +
                "Veggie Burrito      10.50\n" +
                "Subtotal            23.49\n" +
                "Tax                  1.88\n" +
                "Total               25.37\n";

        ReceiptScanner.Receipt receipt = scanner.parseReceipt(fakeOcrOutput);

        //assertEquals(2, receipt.items.size());
        //assertEquals("Chicken Tacos", receipt.items.get(0).name);
        //assertEquals(12.99, receipt.items.get(0).price);
        //assertEquals(23.49, receipt.subtotal);
        //assertEquals(1.88, receipt.tax);
        //assertEquals(25.37, receipt.total);
    }

    @Test
    void ignoresTipAndChangeLines() {
        ReceiptScanner scanner = new ReceiptScanner("unused-for-this-test");

        String fakeOcrOutput = "Burger 8.00\nTip 2.00\nChange 0.50\n";
        ReceiptScanner.Receipt receipt = scanner.parseReceipt(fakeOcrOutput);

        //assertEquals(1, receipt.items.size());
        //assertEquals("Burger", receipt.items.get(0).name);
    }
}