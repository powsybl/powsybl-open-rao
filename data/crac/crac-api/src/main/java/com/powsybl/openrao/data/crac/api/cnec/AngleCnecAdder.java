/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.cnec;

import com.powsybl.openrao.data.crac.api.threshold.AngleThresholdAdder;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public interface AngleCnecAdder extends CnecAdder<AngleCnecAdder> {

    AngleCnecAdder withExportingNetworkElement(String exportingNetworkElementId);

    /**
     * @deprecated since 7.4.0, use {@link #withExportingNetworkElement(String)} directly as the names of network
     * elements are not used anymore.
     */
    @Deprecated(since = "7.4.0")
    AngleCnecAdder withExportingNetworkElement(String exportingNetworkElementId, String exportingNetworkElementName);

    AngleCnecAdder withImportingNetworkElement(String importingNetworkElementId);

    /**
     * @deprecated since 7.4.0, use {@link #withImportingNetworkElement(String)} directly as the names of network
     * elements are not used anymore.
     */
    @Deprecated(since = "7.4.0")
    AngleCnecAdder withImportingNetworkElement(String importingNetworkElementId, String importingNetworkElementName);

    AngleThresholdAdder newThreshold();

    AngleCnec add();
}
