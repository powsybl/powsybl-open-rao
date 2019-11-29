/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.flow_decomposition_results;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.collect.Table;
import lombok.Data;

import java.util.Map;

/**
 * Per branch results business object
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@Data
@JsonDeserialize(builder = PerBranchResult.Builder.class)
public final class PerBranchResult {
    private final String branchId;
    private final String branchCountry1;
    private final String branchCountry2;
    private final double referenceFlows;
    private final double maximumFlows;
    private final Table<String, String, Double> countryExchangeFlows;
    private final Map<String, Double> countryPstFlows;

    @lombok.Builder(builderClassName = "Builder", builderMethodName = "builder", toBuilder = true)
    private PerBranchResult(String branchId,
                            String branchCountry1,
                            String branchCountry2,
                            double referenceFlows,
                            double maximumFlows,
                            Table<String, String, Double> countryExchangeFlows,
                            Map<String, Double> countryPstFlows) {
        this.branchId = branchId;
        this.branchCountry1 = branchCountry1;
        this.branchCountry2 = branchCountry2;
        this.referenceFlows = referenceFlows;
        this.maximumFlows = maximumFlows;
        this.countryExchangeFlows = countryExchangeFlows;
        this.countryPstFlows = countryPstFlows;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
    }

    @JsonIgnore
    public double getTotalInternalFlows() {
        return countryExchangeFlows.get(branchCountry1, branchCountry1);
    }

    @JsonIgnore
    public double getTotalLoopFlows() {
        return countryExchangeFlows.cellSet().stream()
                .filter(cell -> cell.getRowKey().equals(cell.getColumnKey())) // loop flow
                .filter(cell -> !cell.getRowKey().equals(branchCountry1)) // not internal flow
                .mapToDouble(Table.Cell::getValue)
                .sum();
    }

    @JsonIgnore
    public double getTotalImportFlows() {
        return countryExchangeFlows.cellSet().stream()
                .filter(cell -> cell.getColumnKey().equals(branchCountry1)) // to branch country
                .filter(cell -> !cell.getRowKey().equals(branchCountry1)) // not from branch country
                .mapToDouble(Table.Cell::getValue)
                .sum();
    }

    @JsonIgnore
    public double getTotalExportFlows() {
        return countryExchangeFlows.cellSet().stream()
                .filter(cell -> cell.getRowKey().equals(branchCountry1)) // from branch country
                .filter(cell -> !cell.getColumnKey().equals(branchCountry1)) // not to branch country
                .mapToDouble(Table.Cell::getValue)
                .sum();
    }

    @JsonIgnore
    public double getTotalTransitFlows() {
        return countryExchangeFlows.cellSet().stream()
                .filter(cell -> !cell.getRowKey().equals(branchCountry1)) // not from branch country
                .filter(cell -> !cell.getColumnKey().equals(branchCountry1)) // not to branch country
                .filter(cell -> !cell.getColumnKey().equals(cell.getRowKey())) // not loop flow
                .mapToDouble(Table.Cell::getValue)
                .sum();
    }

    @JsonIgnore
    public double getTotalPstFlows() {
        return countryPstFlows.values().stream()
                .mapToDouble(d -> d)
                .sum();
    }
}
