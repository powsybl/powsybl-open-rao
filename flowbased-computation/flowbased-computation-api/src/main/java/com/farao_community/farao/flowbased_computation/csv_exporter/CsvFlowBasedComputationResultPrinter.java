package com.farao_community.farao.flowbased_computation.csv_exporter;

import com.farao_community.farao.commons.data.glsk_file.EICode;
import com.farao_community.farao.data.crac_file.Contingency;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.MonitoredBranch;
import com.farao_community.farao.data.flowbased_domain.DataMonitoredBranch;
import com.farao_community.farao.data.flowbased_domain.DataPtdfPerCountry;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationResult;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class CsvFlowBasedComputationResultPrinter {

    private static String PRECONTINGENCY_NAME = "BASECASE";
    private static String PRECONTINGENCY_STATUS = "N";
    private static String CONTINGENCY_STATUS = "N-K";

    private FlowBasedComputationResult flowBasedComputationResult;
    private CracFile cracFile;

    private List<String> countryList;

    public CsvFlowBasedComputationResultPrinter(FlowBasedComputationResult flowBasedComputationResult, CracFile cracFile) {
        this.flowBasedComputationResult = flowBasedComputationResult;
        this.cracFile = cracFile;
        this.countryList = getCountryList();
    }

    public void export(CSVPrinter csvPrinter) {
        try {
            exportHeaders(csvPrinter);
            exportRows(csvPrinter);
        } catch (IOException e) {

        }
    }

    private void exportHeaders(CSVPrinter csvPrinter) throws IOException {
        csvPrinter.printRecord(getHeaders());
    }

    private void exportRows(CSVPrinter csvPrinter) throws IOException {

        List<Pair<Contingency, MonitoredBranch>> cbcoList = getCbcoList();
        for(Pair<Contingency, MonitoredBranch> cbco : cbcoList) {
            csvPrinter.printRecord(getResultsForBranch(cbco.getKey(), cbco.getValue(), Direction.DIRECT));
            csvPrinter.printRecord(getResultsForBranch(cbco.getKey(), cbco.getValue(), Direction.OPPOSITE));
        }
    }

    private List<String> getResultsForBranch(Contingency contingency, MonitoredBranch branch, Direction direction) {
        List<String> branchResultList = new ArrayList<>();
        System.out.println(branch.getId());
        DataMonitoredBranch branchResults = Objects.requireNonNull(flowBasedComputationResult.getFlowBasedDomain().getDataPreContingency().findMonitoredBranchbyId(branch.getId()));

        double fmax = branch.getFmax();
        double fref = direction.getSign() * branchResults.getFref();
        double margin = fmax - fref;
        double relativeMargin = margin / branchResults.getPtdfList().stream().map(DataPtdfPerCountry::getPtdf).mapToDouble(Math::abs).sum();

        branchResultList.add(branch.getName());
        branchResultList.add(branch.getBranchId());
        branchResultList.add((contingency == null) ? PRECONTINGENCY_NAME : contingency.getName());
        branchResultList.add((contingency == null) ? PRECONTINGENCY_STATUS : CONTINGENCY_STATUS);
        branchResultList.add(direction.getName());
        branchResultList.add(String.valueOf(fmax));
        branchResultList.add(String.valueOf(fref));
        branchResultList.add(String.valueOf(margin));
        branchResultList.add(String.valueOf(relativeMargin));
        branchResultList.addAll(countryList.stream()
                .map(country -> String.valueOf(Objects.requireNonNull(branchResults.findPtdfByCountry(country)).getPtdf() * direction.getSign()))
                .collect(Collectors.toList()));
        return branchResultList;
    }

    private List<String> getHeaders() {
        List<String> headerList = new ArrayList<>();
        headerList.add("Name");
        headerList.add("QuadripoleName");
        headerList.add("OutageName");
        headerList.add("OutageStatus");
        headerList.add("Direction");
        headerList.add("Fmax");
        headerList.add("Fref");
        headerList.add("Margin");
        headerList.add("RelativeMargin");
        headerList.addAll(countryList.stream()
                .map(this::convertEICodeToCountryNameIfPossible)
                .collect(Collectors.toList()));
        return headerList;
    }

    private List<String> getCountryList() {
        return flowBasedComputationResult.getFlowBasedDomain().getDataPreContingency().getDataMonitoredBranches().stream()
                .flatMap(monitoredBranch -> monitoredBranch.getPtdfList().stream().map(DataPtdfPerCountry::getCountry))
                .distinct().collect(Collectors.toList());
    }

    private String convertEICodeToCountryNameIfPossible(String countryEICode) {
        try {
            return new EICode(countryEICode).getCountry().getName();
        } catch (IllegalArgumentException e) {
            return countryEICode;
        }
    }

    private List<Pair<Contingency, MonitoredBranch>> getCbcoList() {
        List<Pair<Contingency, MonitoredBranch>> cbcoList = new ArrayList<>();
        cracFile.getPreContingency().getMonitoredBranches().forEach(branch -> {
            cbcoList.add(Pair.of(null, branch));
        });
        cracFile.getContingencies().forEach(contingency ->  {
            contingency.getMonitoredBranches().forEach(branch -> {
                cbcoList.add(Pair.of(contingency, branch));
            });
        });
        return cbcoList;
    }

}
