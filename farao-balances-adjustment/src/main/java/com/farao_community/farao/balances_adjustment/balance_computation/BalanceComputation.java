/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.balances_adjustment.balance_computation;

import java.util.concurrent.CompletableFuture;

/**
 * Balance computation feature interface.
 * Asynchronous computation is foreseen
 *
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public interface BalanceComputation {

    CompletableFuture<BalanceComputationResult> run(String workingStateId, BalanceComputationParameters parameters);

}
