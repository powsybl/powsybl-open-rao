/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

/**
 * @author Enrico Bettiol {@literal <enrico.bettiol at artelys.com>}
 */
public record GlskGeneratorAllocation(String generatorId, double weight, double availableCapacity, double saturationRatio, double layerCapacity) { }
