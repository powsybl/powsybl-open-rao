/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.flow_decomposition_results.xlsx;

import com.farao_community.farao.data.flow_decomposition_results.FlowDecompositionResults;
import com.farao_community.farao.data.flow_decomposition_results.PerBranchResult;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Utility class for exporting flow decomposition results in XLSX format
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public final class XlsxOutputExporter {
    private XlsxOutputExporter() {
        // No constructor
    }

    private static void exportCommonHeader(XSSFRow headerRow, Set<String> countries) {
        headerRow.createCell(0).setCellValue("branch_names");
        headerRow.createCell(1).setCellValue("FROM");
        headerRow.createCell(2).setCellValue("TO");
        headerRow.createCell(3).setCellValue("PACT");
        headerRow.createCell(4).setCellValue("PMAX");
        countries.stream().sorted().forEach(country -> headerRow.createCell(headerRow.getLastCellNum()).setCellValue(country));
    }

    private static void exportBranchCommonInfo(XSSFRow row, PerBranchResult perBranchResult) {
        row.createCell(0).setCellValue(perBranchResult.getBranchId());
        row.createCell(1).setCellValue(perBranchResult.getBranchCountry1());
        row.createCell(2).setCellValue(perBranchResult.getBranchCountry2());
        row.createCell(3).setCellValue(perBranchResult.getReferenceFlows());
        row.createCell(4).setCellValue(perBranchResult.getMaximumFlows());
    }

    private static void exportPerBranchResult(PerBranchResult perBranchResult, Set<String> countrySet, XSSFSheet sheet) {
        int numberOfCountries = countrySet.size();
        for (int i = 0; i < numberOfCountries + 1; i++) {
            XSSFRow row = sheet.createRow(i);
            for (int j = 0; j < numberOfCountries + 1; j++) {
                row.createCell(j);
            }
        }

        // Corner value :
        sheet.getRow(0).getCell(0).setCellValue("Var1");

        // Create countries map
        Map<String, Integer> indexPerCountry = new HashMap<>();
        countrySet.stream().sorted().forEach(country -> indexPerCountry.put(country, indexPerCountry.size() + 1));

        // Fill headers
        countrySet.forEach(country -> {
            int countryIndex = indexPerCountry.get(country);
            sheet.getRow(countryIndex).getCell(0).setCellValue(country);
            sheet.getRow(0).getCell(countryIndex).setCellValue(country);
        });

        // Fill with exchange flows
        perBranchResult.getCountryExchangeFlows().cellSet()
                .forEach(entry -> {
                    int countryFromIndex = indexPerCountry.get(entry.getRowKey());
                    int countryToIndex = indexPerCountry.get(entry.getColumnKey());
                    double value = entry.getValue() == null ? 0. : entry.getValue();
                    // matrix filled as upper triangle
                    if (countryFromIndex < countryToIndex) { // add value to position
                        double previousValue = sheet.getRow(countryFromIndex).getCell(countryToIndex).getNumericCellValue();
                        sheet.getRow(countryFromIndex).getCell(countryToIndex).setCellValue(previousValue + value);
                    } else if (countryFromIndex > countryToIndex) { // fill with 0 and add opposite value to symmetric position
                        double previousValue = sheet.getRow(countryToIndex).getCell(countryFromIndex).getNumericCellValue();
                        sheet.getRow(countryFromIndex).getCell(countryToIndex).setCellValue(0.);
                        sheet.getRow(countryToIndex).getCell(countryFromIndex).setCellValue(previousValue - value);
                    } else { // internal or loop flow : set value to position
                        sheet.getRow(countryFromIndex).getCell(countryToIndex).setCellValue(value);
                    }
                });
    }

    private static void exportInternalFlowsResults(FlowDecompositionResults results, Set<String> countrySet, XSSFSheet sheet) {
        XSSFRow headerRow = sheet.createRow(0);
        exportCommonHeader(headerRow, countrySet);

        results.getPerBranchResults().entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)).forEach(entry -> {
            PerBranchResult perBranchResult = entry.getValue();

            XSSFRow branchRow = sheet.createRow(sheet.getLastRowNum() + 1);

            exportBranchCommonInfo(branchRow, perBranchResult);

            countrySet.stream().sorted()
                    .forEach(country -> {
                        double flowValue = country.equals(perBranchResult.getBranchCountry1()) ? perBranchResult.getCountryExchangeFlows().get(country, country) : 0.f;
                        branchRow.createCell(branchRow.getLastCellNum()).setCellValue(flowValue);
                    });
        });
    }

    private static void exportLoopFlowsResults(FlowDecompositionResults results, Set<String> countrySet, XSSFSheet sheet) {
        XSSFRow headerRow = sheet.createRow(0);
        exportCommonHeader(headerRow, countrySet);

        results.getPerBranchResults().entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)).forEach(entry -> {
            PerBranchResult perBranchResult = entry.getValue();

            XSSFRow branchRow = sheet.createRow(sheet.getLastRowNum() + 1);

            exportBranchCommonInfo(branchRow, perBranchResult);

            countrySet.stream().sorted()
                .forEach(country -> {
                    double flowValue = !country.equals(perBranchResult.getBranchCountry1()) ? perBranchResult.getCountryExchangeFlows().get(country, country) : 0.f;
                    branchRow.createCell(branchRow.getLastCellNum()).setCellValue(flowValue);
                });
        });
    }

    private static void exportTotalsFlowsResults(FlowDecompositionResults results, XSSFSheet sheet) {
        XSSFRow headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("branch_names");
        headerRow.createCell(1).setCellValue("FROM");
        headerRow.createCell(2).setCellValue("TO");
        headerRow.createCell(3).setCellValue("PACT");
        headerRow.createCell(4).setCellValue("PMAX");
        headerRow.createCell(5).setCellValue("Internal");
        headerRow.createCell(6).setCellValue("Loop");
        headerRow.createCell(7).setCellValue("ImpExp");
        headerRow.createCell(8).setCellValue("Transit");
        headerRow.createCell(9).setCellValue("Transf");
        headerRow.createCell(10).setCellValue("Total");

        results.getPerBranchResults().entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)).forEach(entry -> {
            PerBranchResult perBranchResult = entry.getValue();

            XSSFRow branchRow = sheet.createRow(sheet.getLastRowNum() + 1);

            exportBranchCommonInfo(branchRow, perBranchResult);
            double totalInternalFlows = perBranchResult.getTotalInternalFlows();
            double totalLoopFlows = perBranchResult.getTotalLoopFlows();
            double totalImpExpFlows = perBranchResult.getTotalImportFlows() + perBranchResult.getTotalExportFlows();
            double totalTransitFlows = perBranchResult.getTotalTransitFlows();
            double totalPstFlows = perBranchResult.getTotalPstFlows();
            double totalFlow = totalInternalFlows + totalLoopFlows + totalImpExpFlows + totalTransitFlows + totalPstFlows;
            branchRow.createCell(5).setCellValue(totalInternalFlows);
            branchRow.createCell(6).setCellValue(totalLoopFlows);
            branchRow.createCell(7).setCellValue(totalImpExpFlows);
            branchRow.createCell(8).setCellValue(totalTransitFlows);
            branchRow.createCell(9).setCellValue(totalPstFlows);
            branchRow.createCell(10).setCellValue(totalFlow);
        });
    }

    private static void exportTransformerFlowsResults(FlowDecompositionResults results, Set<String> countrySet, XSSFSheet sheet) {
        XSSFRow headerRow = sheet.createRow(0);
        exportCommonHeader(headerRow, countrySet);

        results.getPerBranchResults().entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)).forEach(entry -> {
            PerBranchResult perBranchResult = entry.getValue();
            XSSFRow branchRow = sheet.createRow(sheet.getLastRowNum() + 1);

            exportBranchCommonInfo(branchRow, perBranchResult);

            countrySet.stream().sorted()
                .forEach(country -> {
                    double flowValue = perBranchResult.getCountryPstFlows().getOrDefault(country, 0.);
                    branchRow.createCell(branchRow.getLastCellNum()).setCellValue(flowValue);
                });
        });
    }

    public static void exportInStream(OutputStream outputStream, FlowDecompositionResults results) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {

            // Build country set
            final Set<String> countrySet;
            Optional<PerBranchResult> firstPerBranchResult = results.getPerBranchResults().values().stream().findFirst();
            if (firstPerBranchResult.isPresent()) {
                countrySet = firstPerBranchResult.get().getCountryExchangeFlows().columnKeySet();
            } else {
                countrySet = Collections.emptySet();
            }

            results.getPerBranchResults().entrySet()
                .stream().sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(entry -> exportPerBranchResult(entry.getValue(), countrySet, workbook.createSheet(entry.getKey())));

            exportInternalFlowsResults(results, countrySet, workbook.createSheet("Internal"));
            exportLoopFlowsResults(results, countrySet, workbook.createSheet("Loop"));
            exportTransformerFlowsResults(results, countrySet, workbook.createSheet("Transformer"));
            exportTotalsFlowsResults(results, workbook.createSheet("Totals"));
            workbook.write(outputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
