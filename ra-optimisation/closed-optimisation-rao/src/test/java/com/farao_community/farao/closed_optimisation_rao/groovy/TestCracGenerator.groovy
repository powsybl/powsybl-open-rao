/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
import com.farao_community.farao.data.crac_file.Contingency
import com.farao_community.farao.data.crac_file.ContingencyElement
import com.farao_community.farao.data.crac_file.CracFile
import com.farao_community.farao.data.crac_file.MonitoredBranch
import com.farao_community.farao.data.crac_file.PreContingency
import com.farao_community.farao.data.crac_file.PstElement
import com.farao_community.farao.data.crac_file.RedispatchRemedialActionElement
import com.farao_community.farao.data.crac_file.RemedialAction
import com.farao_community.farao.data.crac_file.TypeOfLimit
import com.farao_community.farao.data.crac_file.UsageRule
import com.farao_community.farao.data.crac_file.json.JsonCracFile
import com.powsybl.iidm.import_.Importers
import com.powsybl.iidm.network.Branch
import com.powsybl.iidm.network.Country
import com.powsybl.iidm.network.CurrentLimits
import com.powsybl.iidm.network.Generator
import com.powsybl.iidm.network.Network
import com.powsybl.iidm.network.TieLine
import com.powsybl.iidm.network.TwoWindingsTransformer

inputNetwork = args[0]
outputCrac = args[1]

Network network = Importers.loadNetwork(inputNetwork)
loadFlow(network)
constraintProbability = 0.01
constraintPut = false
constraintDepth = 0.01
cracFileBuilder = CracFile.builder()
        .id("Generated CRAC ID")
        .name("Generated CRAC name")
        .sourceFormat("script")

// define interest branches
def keepBranch(Branch branch) {
    return branch.getTerminal1().getVoltageLevel().getSubstation().getCountry().isPresent() &&
            branch.getTerminal1().getVoltageLevel().getSubstation().getCountry().get() == Country.FR &&
            branch.getTerminal1().getVoltageLevel().getNominalV() > 350 &&
            branch.getTerminal1().isConnected() && branch.getTerminal2().isConnected() &&
            branch.getTerminal1().getBusView().getBus() != null && branch.getTerminal2().getBusView().getBus() != null &&
            branch.getTerminal1().getBusView().getBus().isInMainSynchronousComponent() && branch.getTerminal2().getBusView().getBus().isInMainSynchronousComponent() &&
            !(branch instanceof TieLine)
}

keptBranches = network.branchStream.filter{br -> keepBranch(br)}.collect()

// define available generators
static def keepGenerator(Generator generator) {
    return generator.getTerminal().getVoltageLevel().getSubstation().getCountry().isPresent() &&
            generator.getTerminal().getVoltageLevel().getSubstation().getCountry().get() == Country.FR &&
            generator.getTerminal().isConnected() &&
            generator.getTerminal().getBusView().getBus() != null &&
            generator.getTerminal().getBusView().getBus().isInMainSynchronousComponent()
}

keptGenerators = network.generatorStream.filter{gen -> keepGenerator(gen)}.collect()


// define available PSTs
def keepTwoWindingsTransformer(TwoWindingsTransformer twt) {
    return twt.getTerminal1().isConnected() && twt.getTerminal2().isConnected() &&
        twt.getTerminal1().getBusView().getBus() != null && twt.getTerminal2().getBusView().getBus() != null &&
        twt.getTerminal1().getBusView().getBus().isInMainSynchronousComponent() && twt.getTerminal2().getBusView().getBus().isInMainSynchronousComponent() &&
        twt.getPhaseTapChanger() != null

}

keptTwoWindingsTransformers = network.twoWindingsTransformerStream.filter{twt -> keepTwoWindingsTransformer(twt)}.collect()


def createMonitoredBranch(Branch branch, String postfix) {
    double fmax

    // Create fake constraints on some monitored branches by shifting maximum available flow
    boolean onConstraint = Math.random() < constraintProbability

    if(!constraintPut && onConstraint) {
        constraintPut = true
        fmax = Math.abs(branch.terminal1.p * (1 - constraintDepth))
        postfix = "constrained " + postfix
    } else {
        CurrentLimits limits = branch.currentLimits1 != null ? branch.currentLimits1 : branch.currentLimits2

        fmax = limits.permanentLimit \
            * branch.terminal1.voltageLevel.nominalV \
            * Math.sqrt(3) / 1000
    }
    return MonitoredBranch.builder()
            .id(branch.id + " / " + postfix)
            .name(branch.id + " / " + postfix)
            .branchId(branch.id)
            .fmax(fmax)
            .build()
}

def createContingencyElement(Branch branch) {
    return ContingencyElement.builder()
            .elementId(branch.id)
            .name("N-1 element " + branch.id)
            .build()
}

def createContingency(Branch branch) {
    return Contingency.builder()
            .id("N-1 " + branch.id)
            .name("N-1 " + branch.id)
            .contingencyElements(Collections.singletonList(createContingencyElement(branch)))
            .monitoredBranches(keptBranches.collect{br -> createMonitoredBranch(br, "N-1 " + branch.id)})
            .build()
}

def createRemedialAction(Generator generator) {
    return RemedialAction.builder()
            .id("RD " + generator.id)
            .name("Redispatching " + generator.id)
            .remedialActionElements(Collections.singletonList(RedispatchRemedialActionElement.builder()
            .id(generator.id)
            .minimumPower(generator.minP)
            .maximumPower(generator.maxP)
            .startupCost(Math.random() * 10000)
            .marginalCost(Math.random() * 100)
            .build()))
            .usageRules(Collections.singletonList(UsageRule.builder()
            .id("UR " + generator.id)
            .instants(UsageRule.Instant.N)
            .usage(UsageRule.Usage.FREE_TO_USE)
            .build()))
            .build()
}

def createRemedialAction(TwoWindingsTransformer twt) {
    def phaseTapChanger = twt.phaseTapChanger
    return RemedialAction.builder()
            .id("PST " + twt.id)
            .name("PST taps " + twt.id)
            .remedialActionElements(Collections.singletonList(PstElement.builder()
            .id(twt.id)
            .typeOfLimit(TypeOfLimit.ABSOLUTE)
            .minStepRange(phaseTapChanger.lowTapPosition)
            .maxStepRange(phaseTapChanger.highTapPosition)
            .build()))
            .usageRules(Collections.singletonList(UsageRule.builder()
            .id("UR " + twt.id)
            .instants(UsageRule.Instant.N)
            .usage(UsageRule.Usage.FREE_TO_USE)
            .build()))
            .build()
}

// define preventive monitored branches
def preventiveMonitoredBranches = keptBranches.collect{br -> createMonitoredBranch(br as Branch, "preventive")}
cracFileBuilder.preContingency(PreContingency.builder().monitoredBranches(preventiveMonitoredBranches).build())

// Define contingencies
def contingencies = keptBranches.stream().limit(20).collect {br -> createContingency(br as Branch)}.collect()
cracFileBuilder.contingencies(contingencies)

// Define remedial actions
def rdRemedialActions = keptGenerators.collect{gen -> createRemedialAction(gen as Generator)}
def pstRemedialActions = keptTwoWindingsTransformers.collect{twt -> createRemedialAction(twt as TwoWindingsTransformer)}
cracFileBuilder.remedialActions(rdRemedialActions + pstRemedialActions)

// Finish CRAC file build
def cracFile = cracFileBuilder.build()
JsonCracFile.write(cracFile, new FileOutputStream(outputCrac))
