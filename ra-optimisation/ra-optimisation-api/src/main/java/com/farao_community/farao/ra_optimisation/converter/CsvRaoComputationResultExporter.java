/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ra_optimisation.converter;

import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.google.auto.service.AutoService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(RaoComputationResultExporter.class)
public class CsvRaoComputationResultExporter implements RaoComputationResultExporter {
    class CsvColumn {
        private String name;
        private String surname;
        private String unit;
        private String type;

        CsvColumn(String name, String surname, String unit, String type) {
            this.name = name;
            this.surname = surname;
            this.unit = unit;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public String getSurname() {
            return surname;
        }

        public String getUnit() {
            return unit;
        }

        public String getType() {
            return type;
        }
    }

    class CsvSheet {
        private String sheetTitle;
        private Map<String, CsvColumn> columns = new LinkedHashMap<>();

        CsvSheet(String sheetTitle) {
            this.sheetTitle = sheetTitle;
        }

        public String getSheetTitle() {
            return sheetTitle;
        }

        public CsvSheet addCsvColumn(CsvColumn column) {
            this.columns.put(column.getName(), column);
            return this;
        }

        public Map<String, CsvColumn> getColumns() {
            return columns;
        }
    }

    @Override
    public String getFormat() {
        return "CSV";
    }

    @Override
    public String getComment() {
        return "Multiple CSV files exporter of RAO computation results";
    }

    @Override
    public void export(RaoComputationResult result, OutputStream os) {
        try (ZipOutputStream zos = new ZipOutputStream(os)) {
            String timestamp = "20170622";

            exportResults(zos, String.format("CBCO_Results_%s.csv", timestamp), this::printCbcoResults);
            exportResults(zos, String.format("RA_PST_Results_%s.csv", timestamp), this::printRaPstResults);
            exportResults(zos, String.format("RA_RD_Results_curative_%s.csv", timestamp), this::printRaRdCurativeResults);
            exportResults(zos, String.format("RA_RD_Results_preventive_%s.csv", timestamp), this::printRaRdPreventiveResults);
            exportResults(zos, String.format("RA_Topology_Results_%s.csv", timestamp), this::printRaTopologyResults);
            exportResults(zos, String.format("Result_status_%s.csv", timestamp), this::printResultsStatus);
            exportResults(zos, String.format("TSO_Exchange_%s.csv", timestamp), this::printTsoExchange);
            exportResults(zos, String.format("TSO_Results_%s.csv", timestamp), this::printTsoResults);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void printCsvSheet(CsvSheet sheet, CSVPrinter printer) {
        try {
            printer.printRecord(sheet.getSheetTitle());
            printer.printRecord(sheet.getColumns().keySet());
            printer.printRecord(sheet.getColumns().values().stream().map(CsvColumn::getSurname).collect(Collectors.toList()));
            printer.println();
            printer.printRecord(sheet.getColumns().values().stream().map(CsvColumn::getUnit).collect(Collectors.toList()));
            printer.printRecord(sheet.getColumns().values().stream().map(CsvColumn::getType).collect(Collectors.toList()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void printCbcoResults(CSVPrinter printer) {
        CsvSheet sheet = new CsvSheet("CBCO_Results");
        sheet.addCsvColumn(new CsvColumn("Name", "bezeichner", "", "string"))
                .addCsvColumn(new CsvColumn("Country Code From", "ukz_so_anf", "", "string"))
                .addCsvColumn(new CsvColumn("ISO Code From", "ngr_anf", "", "string"))
                .addCsvColumn(new CsvColumn("Location From", "bez_so_anf", "", "string"))
                .addCsvColumn(new CsvColumn("Node From", "bez_ss_kn_anf", "", "string"))
                .addCsvColumn(new CsvColumn("Un From", "un_anf", "kV", "double"))
                .addCsvColumn(new CsvColumn("Country Code To", "ukz_so_end", "", "string"))
                .addCsvColumn(new CsvColumn("ISO Code To", "ngr_end", "", "string"))
                .addCsvColumn(new CsvColumn("Location To", "bez_so_end", "", "string"))
                .addCsvColumn(new CsvColumn("Node To", "bez_ss_kn_end", "", "string"))
                .addCsvColumn(new CsvColumn("Un To", "un_end", "kV", "double"))
                .addCsvColumn(new CsvColumn("TATL", "tatl", "A", "double"))
                .addCsvColumn(new CsvColumn("PATL", "patl", "A", "double"))
                .addCsvColumn(new CsvColumn("Timestamp", "nnf", "", "string"))
                .addCsvColumn(new CsvColumn("Critical Outage", " asf", "", "string"))
                .addCsvColumn(new CsvColumn("P N-0 before Opt.", "p_n0_vor", "MW", "double"))
                .addCsvColumn(new CsvColumn("Q N-0 before Opt.", "q_n0_vor", "MVar", "double"))
                .addCsvColumn(new CsvColumn("I N-0 before Opt.", "i_n0_vor", "A", "double"))
                .addCsvColumn(new CsvColumn("Loading N-0 before Opt.", "ausl_n0_vor", "%", "double"))
                .addCsvColumn(new CsvColumn("P N-1 before Opt.", "p_n1_vor", "MW", "double"))
                .addCsvColumn(new CsvColumn("Q N-1 before Opt.", "q_n1_vor", "MVar", "double"))
                .addCsvColumn(new CsvColumn("I N-1 before opt.", "i_n1_vor", "A", "double"))
                .addCsvColumn(new CsvColumn("Loading N-1 before Opt.", "ausl_n1_vor", "%", "double"))
                .addCsvColumn(new CsvColumn("P N-0 after Opt.", "p_n0_nach", "MW", "double"))
                .addCsvColumn(new CsvColumn("Q N-0 after Opt.", "q_n0_nach", "MVar", "double"))
                .addCsvColumn(new CsvColumn("I N-0 after Opt.", "i_n0_nach", "A", "double"))
                .addCsvColumn(new CsvColumn("Loading N-0 after Opt.", "ausl_n0_nach", "%", "double"))
                .addCsvColumn(new CsvColumn("P N-1 after Opt.", "p_n1_nach", "MW", "double"))
                .addCsvColumn(new CsvColumn("Q N-1 after Opt.", "q_n1_nach", "MVar", "double"))
                .addCsvColumn(new CsvColumn("I N-1 after Opt.", "i_n1_nach", "A", "double"))
                .addCsvColumn(new CsvColumn("Loading N-1 after Opt.", "ausl_n1_nach", "%", "double"));
        printCsvSheet(sheet, printer);
    }

    private void printRaPstResults(CSVPrinter printer) {
        CsvSheet sheet = new CsvSheet("RA_PST_Results");
        sheet.addCsvColumn(new CsvColumn("Name", "bez", "", "string"))
                .addCsvColumn(new CsvColumn("Country Code From", "ukz_so_anf", "", "string"))
                .addCsvColumn(new CsvColumn("ISO Code From", "ngr_anf", "", "string"))
                .addCsvColumn(new CsvColumn("Location From", "bez_so_anf", "", "string"))
                .addCsvColumn(new CsvColumn("Node From", "bez_ss_kn_anf", "", "string"))
                .addCsvColumn(new CsvColumn("Un From", "un_anf", "kV", "double"))
                .addCsvColumn(new CsvColumn("Country Code To", "ukz_so_end", "", "string"))
                .addCsvColumn(new CsvColumn("ISO Code To", "ngr_end", "", "string"))
                .addCsvColumn(new CsvColumn("Location To", "bez_so_end", "", "string"))
                .addCsvColumn(new CsvColumn("Node To", "bez_ss_kn_end", "", "string"))
                .addCsvColumn(new CsvColumn("Un To", "un_end", "kV", "double"))
                .addCsvColumn(new CsvColumn("Timestamp", "nnf", "", "string"))
                 .addCsvColumn(new CsvColumn("Tap before Opt.", "stufe_vor_opt", "", "double"))
                .addCsvColumn(new CsvColumn("Tap after Opt.", "stufe_nach_opt", "", "double"))
                .addCsvColumn(new CsvColumn("Penalty Cost", "strafkosten", "Euro", "double"));
        printCsvSheet(sheet, printer);
    }

    private void printRaRdCurativeResults(CSVPrinter printer) {
        CsvSheet sheet = new CsvSheet("RA_RD_Results_curative");
        sheet.addCsvColumn(new CsvColumn("Name", "bez", "", "string"))
                .addCsvColumn(new CsvColumn("Fuel Type", "primaertyp", "", "string"))
                .addCsvColumn(new CsvColumn("ISO Code", "ngr", "", "string"))
                .addCsvColumn(new CsvColumn("Country Code", "ukz_so", "", "string"))
                .addCsvColumn(new CsvColumn("Location", "bez_so", "", "string"))
                .addCsvColumn(new CsvColumn("Node", "bez_ss_kn", "", "string"))
                .addCsvColumn(new CsvColumn("Un", "un", "kV", "double"))
                .addCsvColumn(new CsvColumn("NNF", "nnf", "", "string"))
                .addCsvColumn(new CsvColumn("Outage", "asf", "", "string"))
                .addCsvColumn(new CsvColumn("P Redisp.", "p_redisp", "MW", "double"));
        printCsvSheet(sheet, printer);
    }

    private void printRaRdPreventiveResults(CSVPrinter printer) {
        CsvSheet sheet = new CsvSheet("RA_RD_Results_preventive");
        sheet.addCsvColumn(new CsvColumn("Name", "bez", "", "string"))
                .addCsvColumn(new CsvColumn("Fuel Type", "primaertyp", "", "string"))
                .addCsvColumn(new CsvColumn("ISO Code", "ngr", "", "string"))
                .addCsvColumn(new CsvColumn("Country Code", "ukz_so", "", "string"))
                .addCsvColumn(new CsvColumn("Location", "bez_so", "", "string"))
                .addCsvColumn(new CsvColumn("Node", "bez_ss_kn", "", "string"))
                .addCsvColumn(new CsvColumn("Un", "un", "kV", "double"))
                .addCsvColumn(new CsvColumn("Time Stamp", "nnf", "", "string"))
                .addCsvColumn(new CsvColumn("P before Opt.", "p_vor", "MW", "double"))
                .addCsvColumn(new CsvColumn("P after Opt.", "p_nach", "MW", "double"))
                .addCsvColumn(new CsvColumn("P Redisp.", "p_redisp", "MW", "double"))
                .addCsvColumn(new CsvColumn("Redisp. Cost", "kosten_redisp", "Euro", "double"))
                .addCsvColumn(new CsvColumn("Penalty Cost", "Strafkosten", "Euro", "double"))
                .addCsvColumn(new CsvColumn("Potential - after Opt.", "potential_n", "MW", "double"))
                .addCsvColumn(new CsvColumn("Potential + after Opt.", "potential_p", "MW", "double"));
        printCsvSheet(sheet, printer);
    }

    private void printRaTopologyResults(CSVPrinter printer) {
        CsvSheet sheet = new CsvSheet("RA_Topology_Results");
        sheet.addCsvColumn(new CsvColumn("Id des Netzobjektes", "id", "", "unsigned"))
                .addCsvColumn(new CsvColumn("Schluessel", "schluessel", "", "string"))
                .addCsvColumn(new CsvColumn("Typ", "typ", "", "string"))
                .addCsvColumn(new CsvColumn("Bezeichner", "bez", "", "string"))
                .addCsvColumn(new CsvColumn("NNF", "nnf", "", "string"))
                .addCsvColumn(new CsvColumn("Iteration", "iter", "", "unsigned"))
                .addCsvColumn(new CsvColumn("EPA vor allen SSZ", "epa_vor", "", "double"))
                .addCsvColumn(new CsvColumn("EPA in aktueller Iteration vor SSZ", "epa_iter", "", "double"))
                .addCsvColumn(new CsvColumn("EPA im betrachteten SSZ", "epa_nach", "", "double"))
                .addCsvColumn(new CsvColumn("umgesetzt", "umgesetzt", "", "unsigned"));
        printCsvSheet(sheet, printer);
    }

    private void printResultsStatus(CSVPrinter printer) {
        CsvSheet sheet = new CsvSheet("Result_status");
        sheet.addCsvColumn(new CsvColumn("NNF", "nnf", "", "string"))
                .addCsvColumn(new CsvColumn("konvergiert", "konv", "", "string"))
                .addCsvColumn(new CsvColumn("optimiert", "opt", "", "string"))
                .addCsvColumn(new CsvColumn("Verl. AC vor Opt.", "verl_ac_vor", "MW", "double"))
                .addCsvColumn(new CsvColumn("Verl. DC vor Opt.", "verl_dc_vor", "MW", "double"))
                .addCsvColumn(new CsvColumn("Verl. AC Verlustmin.", "verl_ac_verlopt", "MW", "double"))
                .addCsvColumn(new CsvColumn("Verl. DC Verlustmin.", "verl_dc_verlopt", "MW", "double"))
                .addCsvColumn(new CsvColumn("Verl. AC nach Opt.", "verl_ac_nach", "MW", "double"))
                .addCsvColumn(new CsvColumn("Verl. DC nach Opt.", "verl_dc_nach", "MW", "double"))
                .addCsvColumn(new CsvColumn("Redisp. Kosten", "redisp_kosten", "Euro", "double"))
                .addCsvColumn(new CsvColumn("Redisp. Menge", "redisp_menge", "MW", "double"))
                .addCsvColumn(new CsvColumn("EPL vor Opt.", "epl_vor", "MVA", "double"))
                .addCsvColumn(new CsvColumn("EPL Verlustmin.", "epl_verlopt", "MVA", "double"))
                .addCsvColumn(new CsvColumn("EPL nach Opt.", "epl_nach", "MVA", "double"))
                .addCsvColumn(new CsvColumn("Strafkosten", "strafkosten", "Euro", "double"));
        printCsvSheet(sheet, printer);
    }

    private void printTsoExchange(CSVPrinter printer) {
        CsvSheet sheet = new CsvSheet("TSO_Exchange");
        sheet.addCsvColumn(new CsvColumn("Name from", "bez", "", "string"))
                .addCsvColumn(new CsvColumn("Name to", "bez_to", "", "string"))
                .addCsvColumn(new CsvColumn("Timestamp", "nnf", "", "string"))
                .addCsvColumn(new CsvColumn("Exchange before Opt.", "p_vor", "MW", "double"))
                .addCsvColumn(new CsvColumn("Exchange after Opt.", "p_nach", "MW", "double"));
        printCsvSheet(sheet, printer);
    }

    private void printTsoResults(CSVPrinter printer) {
        CsvSheet sheet = new CsvSheet("TSO_Results");
        sheet.addCsvColumn(new CsvColumn("Name", "bez", "", "string"))
                .addCsvColumn(new CsvColumn("Timestamp", "nnf", "", "string"))
                .addCsvColumn(new CsvColumn("Net Position before Opt.", "p_vor", "MW", "double"))
                .addCsvColumn(new CsvColumn("Net Position after Opt.", "p_nach", "MW", "double"))
                .addCsvColumn(new CsvColumn("P Redisp. -", "p_redisp_n", "MW", "double"))
                .addCsvColumn(new CsvColumn("P Redisp. +", "p_redisp_p", "MW", "double"))
                .addCsvColumn(new CsvColumn("Potential - after Opt.", "potential_n", "MW", "double"))
                .addCsvColumn(new CsvColumn("Potential + after Opt.", "potential_p", "MW", "double"));
        printCsvSheet(sheet, printer);
    }

    private void exportResults(ZipOutputStream zipOs, String fileName, Consumer<CSVPrinter> csvPrinterConsumer) throws IOException {
        ZipEntry entry = new ZipEntry(fileName);
        zipOs.putNextEntry(entry);
        CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(zipOs), CSVFormat.DEFAULT.withDelimiter(';'));
        csvPrinterConsumer.accept(printer);
        printer.flush();
        zipOs.closeEntry();
    }
}
