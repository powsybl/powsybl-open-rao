/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.swecneexporter;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.openrao.data.cneexportercommons.CneExporterParameters;

import java.util.Map;

/**
 * Specific parameters for SWE CNE export
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class SweCneExporterParameters extends AbstractExtension<CneExporterParameters> {
    private final Map<String, String> xNodeMrids;

    public SweCneExporterParameters(Map<String, String> xNodeMrids) {
        this.xNodeMrids = xNodeMrids;
    }

    /**
     * Get the mRID of an XNode given its ID (beginning with X...).
     * This is currently needed because PowSyBl's CGMES to IIDM reader ignores Xnode mRIDs. It can be removed in the
     * future if the IIDM adds a way to access these mRIDs.
     *
     * @param xNode: ID of the XNode (X...)
     * @return its CIM mRID
     */
    public String getXNodeMrid(String xNode) {
        return xNodeMrids.get(xNode);
    }

    @Override
    public String getName() {
        return "SweCneExporterParameters";
    }
}
