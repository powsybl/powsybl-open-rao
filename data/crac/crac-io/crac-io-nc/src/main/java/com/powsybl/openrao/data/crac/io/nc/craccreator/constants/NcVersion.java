/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator.constants;

/**
 * NCP profile version of an NC CRAC input package.
 * <p>
 * The version selects the read contract of the importer: the SPARQL query set
 * (see {@link NcConstants#getSparqlFile(NcVersion)}) and the expected vocabulary
 * namespaces. NCP 2.4 publishes its vocabulary as {@code http://entsoe.eu/ns/nc#} /
 * {@code http://iec.ch/TC57/CIM100#}; NCP 2.5 publishes it as {@code https://cim4.eu/ns/nc#} /
 * {@code https://cim.ucaiug.io/ns#}.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public enum NcVersion {

    V24("2.4"),
    V25("2.5");

    private final String label;

    NcVersion(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
