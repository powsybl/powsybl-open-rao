/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.cse.remedial_action;

import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteBusHelper;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteNetworkAnalyzer;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Identifiable;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility to look for a generator using a bus name
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class GeneratorHelper {
    private ImportStatus importStatus;
    private boolean isAltered = false;
    private String generatorId = null;
    private String detail = null;

    private double pMin;
    private double pMax;

    // Find a generator using a bus ID
    public GeneratorHelper(String busIdInCrac, UcteNetworkAnalyzer ucteNetworkAnalyzer) {
        UcteBusHelper busHelper = new UcteBusHelper(busIdInCrac, ucteNetworkAnalyzer);
        if (!busHelper.isValid()) {
            importStatus = ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK;
            detail = busHelper.getInvalidReason();
            return;
        }
        findBusWithGenerator(busHelper.getBusMatchesInNetwork(), busIdInCrac);
    }

    private void findBusWithGenerator(Set<Bus> matchedBuses, String busIdInCrac) {

        boolean anyNotConnectedGenerator = false;
        Generator generator = null;

        for (Bus bus : matchedBuses) {

            // if bus is not in main component, just look if there is any generator for logging purposes,
            // and go to next bus
            if (!bus.isInMainConnectedComponent()) {
                if (bus.getGeneratorStream().findAny().isPresent()) {
                    anyNotConnectedGenerator = true;
                }
                continue;
            }

            // if one generator has already been identified on a previous bus, and a new one is found -> inconsistency
            if (bus.getGeneratorStream().findAny().isPresent() && generatorId != null) {
                importStatus = ImportStatus.INCONSISTENCY_IN_DATA;
                detail = String.format("Too many generators match node name %s", busIdInCrac);
                return;
            }

            // 1 generator connected to bus -> OK return generator
            if (bus.getGeneratorStream().count() == 1) {
                importStatus = ImportStatus.IMPORTED;
                generator = bus.getGenerators().iterator().next();
            }

            // > 1  generator connected to bus -> OK return 1st generator + warning
            if (bus.getGeneratorStream().count() > 1) {
                importStatus = ImportStatus.IMPORTED;
                isAltered = true;
                detail = String.format("More than 1 generator associated to %s. First generator is selected.", busIdInCrac);
                generator = bus.getGeneratorStream().sorted(Comparator.comparing(Identifiable::getId)).collect(Collectors.toList()).get(0);
            }
        }

        if (generator == null) {
            importStatus = ImportStatus.INCONSISTENCY_IN_DATA;
            if (anyNotConnectedGenerator) {
                detail = String.format("Buses matching %s in the network do not hold generators connected to the main grid", busIdInCrac);
            } else {
                detail = String.format("Buses matching %s in the network do not hold generators", busIdInCrac);
            }
        } else {
            generatorId = generator.getId();
            pMin = generator.getMinP();
            pMax = generator.getMaxP();
        }
    }

    public boolean isValid() {
        return importStatus.equals(ImportStatus.IMPORTED);
    }

    public ImportStatus getImportStatus() {
        return importStatus;
    }

    public boolean isAltered() {
        return isAltered;
    }

    public String getGeneratorId() {
        return generatorId;
    }

    public String getDetail() {
        return detail;
    }

    public double getPmin() {
        return pMin;
    }

    public double getPmax() {
        return pMax;
    }
}
