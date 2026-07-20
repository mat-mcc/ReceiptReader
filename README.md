# Overview
ReceiptReader is a Java app that allows for scanning or inputting of receipt information and subsequent splitting of receipts between 1 or more people. Instead of simple even splitting between members. ReceiptReader allows for extended options for splitting items. 

## Features
- **OCR capability** : Tesseract OCR for character recognition in receipt photos. Designed to run locally. No calls or API required.
- **Image and OCR processing** : Threshholding and grayscale conversion helps to "clean up" photos and improve OCR results. OCR attempts to maximize accurate output by attempting multiple receipt formats and regular expressions.
- **Editable information** : Add or remove data based on both simple input and OCR results. Review and correct price and name for each item.
- **Splitting Logic** : Adjust splitting rules for each item in list.

## Tools Used:
- Java with Swing
- Maven
- Tess4J & Tesseract OCR
- JUnit for testing

## Prerequisites & Dependencies
- JAVA 17+
- Maven https://maven.apache.org/
- Tesseract OCR https://tesseractocr.org/ (installed locally). You will need to know where the trained language data bundled as 'tessdata' is downloaded in your Tesseract install. 

### Build and Run (run in receiptsplitter directory)
mvn clean install  
mvn exac:java "-Dexec.args=<PATH_TO_TESSDATA_FOLDER>"


