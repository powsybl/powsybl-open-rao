/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import com.powsybl.iidm.network.Network;
import java.util.List;

/**
 * Interface to manage CRAC
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface Crac extends Identifiable, Synchronizable {

    List<Cnec> getCnecs();

    void setCnecs(List<Cnec> cnecs);

    List<RangeAction> getRangeActions();

    void setRangeActions(List<RangeAction> rangeActions);

    List<NetworkAction> getNetworkActions();

    void setNetworkActions(List<NetworkAction> networkActions);

    void addCnec(Cnec cnec);

    void addContingency(Contingency contingency);

    void addNetworkRemedialAction(NetworkAction networkAction);

    void addRangeRemedialAction(RangeAction rangeAction);

    List<RangeAction> getRangeActions(Network network, UsageMethod usageMethod);

    List<NetworkAction> getNetworkActions(Network network, UsageMethod usageMethod);

    List<NetworkElement> getCriticalNetworkElements();

    List<Contingency> getContingencies();

    void generateValidityReport(Network network);
}
