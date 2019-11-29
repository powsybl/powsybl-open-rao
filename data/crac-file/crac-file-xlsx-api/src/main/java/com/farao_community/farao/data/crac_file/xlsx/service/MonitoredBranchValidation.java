/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file.xlsx.service;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_file.MonitoredBranch;
import com.farao_community.farao.data.crac_file.xlsx.model.*;
import io.vavr.control.Validation;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
public class MonitoredBranchValidation {

    /**
     * Parsing and check the information of the row
     * @param monitoredBranchesValidation
     * @param branchTimeseriesValidation
     * @param timesSeries
     */
    public Map<String, List<MonitoredBranch>> monitoredBranchValidation(Validation<FaraoException, List<MonitoredBranchXlsx>> monitoredBranchesValidation, Validation<FaraoException, List<BranchTimeSeries>> branchTimeseriesValidation, TimesSeries timesSeries) {
        // creation hash map, String key have the Unique co name, the list of monitored branch have the same unique co name
        HashMap<String, List<MonitoredBranch>> monitoredBranchesHashMap = new HashMap<>();

        monitoredBranchesValidation.forEach(monitoredBranches -> {

            // Validation of monitored branch
            List<MonitoredBranchXlsx> monitoredBranchesPreContingencyValid = filterMonitorBranchActivation(monitoredBranches);

            monitoredBranchesPreContingencyValid.stream().forEach(monitoredBranch -> {
                Optional<BranchTimeSeries> branchTimeSerie = branchTimeseriesValidation.get().stream().filter(branchTimeserie -> branchTimeserie.getUniqueCbcoName().equals(monitoredBranch.getUniqueCbcoName())).findAny();
                String contingencyName = monitoredBranch.getUniqueCOName() != null ? monitoredBranch.getUniqueCOName() : "";
                if (branchTimeSerie.isPresent()) {
                    if (branchTimeSerie.get().getCurentLimit1(timesSeries) != 0) {
                        List<MonitoredBranch> monitoredBranchTempList = new ArrayList<>();
                        if (branchTimeSerie.get().getCurentLimit1(timesSeries) != 0) {
                            // initialisation of monitor branch
                            MonitoredBranch monitor = buildMonitoredBranch(monitoredBranch, branchTimeSerie.get().getCurentLimit1(timesSeries));
                            // if the hashmap is not null i need to implement the list
                            if (monitoredBranchesHashMap.containsKey(contingencyName)) {
                                // finding good monitored branch
                                List<MonitoredBranch> monitoredBranchTempListOn = monitoredBranchesHashMap.get(contingencyName);
                                // adding the actual monitor branch
                                monitoredBranchTempListOn.add(monitor);
                                // put in the hashmap, the last list is crushed
                                monitoredBranchesHashMap.put(contingencyName, monitoredBranchTempListOn);
                            } else {
                                // if the object is null i put a new list
                                monitoredBranchTempList.add(monitor);
                                monitoredBranchesHashMap.put(contingencyName, monitoredBranchTempList);
                            }
                        }
                    }
                }
            });

        });

        // return the hashmap complet
        return monitoredBranchesHashMap;
    }

    /**
     * Methode for specify how information if you want set in MonitoredBranch object
     * @param moXlsx
     * @param fmax
     */
    private MonitoredBranch buildMonitoredBranch(MonitoredBranchXlsx moXlsx, float fmax) {
        String id = CracTools.getOrderCodeElementName(moXlsx.getDescriptionMode(), moXlsx.getUctNodeFrom(), moXlsx.getUctNodeTo(), moXlsx.getOrderCodeElementName());
        return  MonitoredBranch.builder()
                .branchId(id)
                .id(moXlsx.getUniqueCbcoName())
                .name(moXlsx.getUniqueCbcoName())
                .fmax(fmax)
                .build();
    }

    /**
     * Add filter condition for accepte a XlsxCrac
     * @param monitoredBranches
     */
    public List<MonitoredBranchXlsx> filterMonitorBranchActivation(List <MonitoredBranchXlsx> monitoredBranches) {
        return    monitoredBranches.stream()
                .filter(Objects::nonNull)
                .filter(monitoredBranch -> monitoredBranch.getActivation().equals(Activation.YES))
                .filter(monitoredBranch -> monitoredBranch.getAbsoluteRelativeConstraint() == AbsoluteRelativeConstraint.ABS)
                .filter(monitoredBranch -> null != monitoredBranch.getUctNodeFrom())
                .filter(monitoredBranch -> null != monitoredBranch.getUctNodeTo())
                .filter(monitoredBranch -> null != monitoredBranch.getOrderCodeElementName())
                .filter(monitoredBranch -> monitoredBranch.getAbsoluteRelativeConstraint().getLabel().equals("ABSOLUTE"))
                .collect(Collectors.toList());
    }
}
