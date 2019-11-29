/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file.xlsx;

import com.farao_community.farao.commons.FaraoException;
import org.apache.poi.EmptyFileException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.util.SAXHelper;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Objects;

/**
 * Utility class for Reader implementations
 */
public final class ExcelReaderUtil {

    protected ExcelReaderUtil() {
    }

    /**
     * Reads a list of POJOs from the given excel file.
     */
    public static void process(InputStream inputStream, String sheetName, CellExcelReader reader) {
        try (PushbackInputStream p = new PushbackInputStream(inputStream, 16);
             OPCPackage opcPackage = OPCPackage.open(p)) {
            DataFormatter dataFormatter = new DataFormatter();
            ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(opcPackage);
            XSSFReader xssfReader = new XSSFReader(opcPackage);
            StylesTable stylesTable = xssfReader.getStylesTable();
            InputStream sheetInputStream = null;
            XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) xssfReader.getSheetsData();
            while (sheets.hasNext()) {
                sheetInputStream = sheets.next();
                if (sheets.getSheetName().equalsIgnoreCase(sheetName)) {
                    break;
                } else {
                    sheetInputStream = null;
                }
            }
            if (Objects.isNull(sheetInputStream)) {
                throw new SheetNotFoundException(sheetName);
            }
            XMLReader xmlReader = SAXHelper.newXMLReader();
            xmlReader.setContentHandler(new XSSFSheetXMLHandler(stylesTable, strings, reader, dataFormatter, false));
            xmlReader.parse(new InputSource(sheetInputStream));
            sheetInputStream.close();
        } catch (InvalidFormatException | EmptyFileException | NotOfficeXmlFileException ife) {
            throw new FaraoException("Cannot load file. The file must be an Excel 2007+ Workbook (.xlsx)");
        } catch (SheetNotFoundException ex) {
            throw new FaraoException(ex.getMessage());
        } catch (FaraoException ze) {
            throw ze; // Rethrow the Exception
        } catch (Exception e) {
            throw new FaraoException("Failed to process file", e);
        }
    }
}
