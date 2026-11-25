/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator;

import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.NcVersion;

import java.util.Set;

/**
 * Decides the NCP version of an NC CRAC input package from the signals collected
 * while scanning its files (see {@link NcCracImporter}):
 * <ul>
 *     <li>NCP 2.5 vocabulary namespaces found in a file body
 *     ({@code https://cim4.eu/...}, {@code https://cim.ucaiug.io/ns...});</li>
 *     <li>a {@code dcterms:conformsTo} header value referencing the NCP 2.5 NC vocabulary
 *     ({@code https://cim4.eu/ns/nc});</li>
 *     <li>a {@code dcterms:conformsTo} header value referencing the NCP 2.4 NC vocabulary
 *     ({@code http://entsoe.eu/ns/nc}).</li>
 * </ul>
 * Any NCP 2.5 signal makes the package NCP 2.5. A package with no NCP 2.5 signal
 * (including packages without any {@code conformsTo}) is read as NCP 2.4, which keeps
 * the current behavior for existing inputs.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class NcVersionDetector {

    private NcVersionDetector() {
    }

    public static NcVersion detectVersion(Set<String> ncp25NamespaceFiles,
                                          Set<String> ncp25ConformsToFiles,
                                          Set<String> ncp24ConformsToFiles) {
        boolean ncp25 = !ncp25NamespaceFiles.isEmpty() || !ncp25ConformsToFiles.isEmpty();
        if (!ncp25) {
            return NcVersion.V24;
        }
        if (!ncp24ConformsToFiles.isEmpty()) {
            OpenRaoLoggerProvider.BUSINESS_WARNS.warn(String.format(
                "NC crac import: conflicting NCP version signals. NCP 2.5 vocabulary found in file(s) %s (namespaces) / %s (conformsTo), "
                    + "while NCP 2.4 conformsTo was found in file(s) %s. The package will be read as NCP 2.5.",
                ncp25NamespaceFiles, ncp25ConformsToFiles, ncp24ConformsToFiles));
        }
        return NcVersion.V25;
    }
}
