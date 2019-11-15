/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.csv_exporter;

import com.farao_community.farao.data.crac_file.Contingency;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.MonitoredBranch;
import com.farao_community.farao.data.flowbased_domain.DataMonitoredBranch;
import com.farao_community.farao.data.flowbased_domain.DataPtdfPerCountry;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationResult;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CsvFlowBasedComputationResultPrinter {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CsvFlowBasedComputationResultPrinter.class);

    private static String PRECONTINGENCY_NAME = "BASECASE";
    private static String PRECONTINGENCY_STATUS = "N";
    private static String CONTINGENCY_STATUS = "N-K";

    private FlowBasedComputationResult flowBasedComputationResult;
    private CracFile cracFile;

    private List<FlowBasedCountry> countryList;
    private List<Pair<FlowBasedCountry, FlowBasedCountry>> neighbouringCountryPairs;

    public CsvFlowBasedComputationResultPrinter(FlowBasedComputationResult flowBasedComputationResult, CracFile cracFile) {
        this.flowBasedComputationResult = flowBasedComputationResult;
        this.cracFile = cracFile;
        this.countryList = getCountryList();
        this.neighbouringCountryPairs = getCountryPairList(countryList);
    }

    public void export(CSVPrinter csvPrinter) throws IOException {
        exportHeaders(csvPrinter);
        exportRows(csvPrinter);
    }

    private void exportHeaders(CSVPrinter csvPrinter) throws IOException {
        csvPrinter.printRecord(getHeaders());
    }

    private void exportRows(CSVPrinter csvPrinter) throws IOException {

        List<Pair<Contingency, MonitoredBranch>> cbcoList = getCbcoList();
        for (Pair<Contingency, MonitoredBranch> cbco : cbcoList) {
            csvPrinter.printRecord(getResultsForBranch(cbco.getKey(), cbco.getValue(), Direction.DIRECT));
            csvPrinter.printRecord(getResultsForBranch(cbco.getKey(), cbco.getValue(), Direction.OPPOSITE));
        }
    }

    private List<String> getResultsForBranch(Contingency contingency, MonitoredBranch branch, Direction direction) {
        List<String> branchResultList = new ArrayList<>();
        DataMonitoredBranch branchResults = findDataMonitoredBranch(branch);

        double fmax = branch.getFmax();
        double fref = direction.getSign() * branchResults.getFref();
        double margin = fmax - fref;
        Double relativeMargin = getRelativeMargin(margin, branchResults);

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
                .map(country -> String.valueOf(findPtdf(branchResults, country) * direction.getSign()))
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
        headerList.addAll(countryList.stream().map(FlowBasedCountry::getName).collect(Collectors.toList()));
        return headerList;
    }

    private List<FlowBasedCountry> getCountryList() {
        return flowBasedComputationResult.getFlowBasedDomain().getDataPreContingency().getDataMonitoredBranches().stream()
                .flatMap(monitoredBranch -> monitoredBranch.getPtdfList().stream().map(DataPtdfPerCountry::getCountry))
                .distinct().map(FlowBasedCountry::new).collect(Collectors.toList());
    }

    private List<Pair<FlowBasedCountry, FlowBasedCountry>> getCountryPairList(List<FlowBasedCountry> countryList) {

        List<Pair<FlowBasedCountry, FlowBasedCountry>> countryPairs = new ArrayList<>();

        if (countryList.size() >= 2) {
            for (int i = 0; i < countryList.size(); i++) {
                for (int j = i + 1; j < countryList.size(); j++) {
                    if (NeighbouringCountryPairsInCore.belongs(countryList.get(i).getName(), countryList.get(j).getName())) {
                        countryPairs.add(Pair.of(countryList.get(i), countryList.get(j)));
                    }
                }
            }
        }
        if (countryPairs.size() == 0) {
            LOGGER.warn("Relative margins cannot be computed as the data set does not contain neighbouring bidding zones");
        }
        return countryPairs;
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

    private DataMonitoredBranch findDataMonitoredBranch(MonitoredBranch branch) {
        DataMonitoredBranch out = flowBasedComputationResult.getFlowBasedDomain().getDataPreContingency().findMonitoredBranchbyId(branch.getId());
        if (out != null) {
            return out;
        }
        throw new IllegalArgumentException(String.format("Branch with id '%s' not found in flow-based computation results", branch.getId()));
    }

    private double findPtdf(DataMonitoredBranch branch, FlowBasedCountry country) {
        DataPtdfPerCountry ptdf = branch.findPtdfByCountry(country.getEiCode());
        if (ptdf != null) {
            return ptdf.getPtdf();
        }
        throw new IllegalArgumentException(String.format("PTDF '%s' not found for branch '%s' in flow-based computation results", country, branch.getId()));
    }

    private Double getRelativeMargin(double margin, DataMonitoredBranch branchResults) {
        double sumOfAbsZoneToZonePtdf = 0;
        if (neighbouringCountryPairs.size() > 0) {
            sumOfAbsZoneToZonePtdf = neighbouringCountryPairs.stream()
                    .map(p -> findPtdf(branchResults, p.getLeft()) - findPtdf(branchResults, p.getRight()))
                    .mapToDouble(Math::abs).sum();
        }
        if (sumOfAbsZoneToZonePtdf > 0) {
            return margin / sumOfAbsZoneToZonePtdf;
        }
        return Double.POSITIVE_INFINITY;
    }
}
