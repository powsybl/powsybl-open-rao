import com.powsybl.iidm.network.Bus
import com.powsybl.iidm.network.DanglingLine
import com.powsybl.iidm.network.EnergySource
import com.powsybl.iidm.network.LoadType
import com.powsybl.iidm.network.Network
import com.powsybl.iidm.network.VoltageLevel

/*
    UCTE-DEF file does not provide configuration for default nominal voltage setup.

    This post processor script modifies default nominal voltages in order to adapt it to FMAX
    calculation based on IMAX.

    By default, UCTE sets nominal voltage to 220 and 380kV for the voltage levels 6 and 7, whereas
    default values of Core countries are 225 and 400 kV instead.
 */


network.voltageLevelStream.forEach({ voltageLevel ->
    updateVoltageLevelNominalV(voltageLevel)
})

boolean safeDoubleEquals(double a, double b) {
    return Math.abs(a - b) < 1e-3
}

def updateVoltageLevelNominalV(VoltageLevel voltageLevel) {
    if (safeDoubleEquals(voltageLevel.nominalV, 380)) {
        voltageLevel.nominalV = 400
    } else if (safeDoubleEquals(voltageLevel.nominalV, 220)) {
        voltageLevel.nominalV = 225
    } else {
        // Should not be changed cause is not equal to the default nominal voltage of voltage levels 6 or 7
    }
}

/*
    When importing an UCTE network file, powsybl ignores generators and loads that do not have an initial power flow.

    It can cause an error if a GLSK file associated to this network includes some factors on
    these nodes. The GLSK importers looks for a Generator (GSK) or Load (LSK) associated to this
    node. If the Generator/Load does not exist, the GLSK cannot be created.

    This script fix this problem, by creating for all missing generators a generator (P, Q = 0),
    and all missing loads a load (P, Q = 0).
 */

createMissingGeneratorsAndLoads()

void createMissingGeneratorsAndLoads() {
    network.getVoltageLevelStream().forEach({voltageLevel -> createMissingGeneratorsAndLoads(voltageLevel)})
}

void createMissingGeneratorsAndLoads(VoltageLevel voltageLevel) {
    voltageLevel.getBusBreakerView().getBuses().forEach({bus -> createMissingGenerator(voltageLevel, bus.getId())})
    voltageLevel.getBusBreakerView().getBuses().forEach({bus -> createMissingLoad(voltageLevel, bus.getId())})
}

void createMissingGenerator(VoltageLevel voltageLevel, String busId) {
    String generatorId = busId + "_generator"
    if (network.getGenerator(generatorId) == null) {
        try {
            voltageLevel.newGenerator()
                    .setBus(busId)
                    .setEnsureIdUnicity(true)
                    .setId(generatorId)
                    .setMaxP(999999)
                    .setMinP(0)
                    .setTargetP(0)
                    .setTargetQ(0)
                    .setTargetV(voltageLevel.getNominalV())
                    .setVoltageRegulatorOn(false)
                    .add()
                    .setFictitious(true)
        } catch (Exception e) {
            // Can't create generator
        }
    }
}

void createMissingLoad(VoltageLevel voltageLevel, String busId) {
    String loadId = busId + "_load"
    if (network.getLoad(loadId) == null) {
        try {
            voltageLevel.newLoad()
                    .setBus(busId)
                    .setEnsureIdUnicity(true)
                    .setId(loadId)
                    .setP0(0)
                    .setQ0(0)
                    .setLoadType(LoadType.FICTITIOUS)
                    .add()
        } catch (Exception e) {
            // Can't create load
        }
    }
}

/*
    When importing an UCTE network file, powsybl merges its X-nodes into dangling lines.

    It can cause an error if a GLSK file associated to this network includes some factors on
    xNodes. The GLSK importers looks for a Generator (GSK) or Load (LSK) associated to this
    xNode. If the Generator/Load does not exist, the GLSK cannot be created.

    This problem has been observed on CORE CC data, on the two Alegro nodes :
       - XLI_OB1B (AL-BE)
       - XLI_OB1A (AL-DE)

    This script fix this problem, by creating for these two nodes a fictitious generator (P, Q = 0),
    connected to the voltage level on which the dangling lines are linked.
 */


createGeneratorOnXnode("XLI_OB1B")
createGeneratorOnXnode("XLI_OB1A")

void createGeneratorOnXnode(String xnodeId) {
    Optional<DanglingLine> danglingLine = network.danglingLineStream.filter({dl -> dl.getUcteXnodeCode().equals(xnodeId)}).findAny()

    if (!danglingLine.present) {
        // Xnode not found in network
        // do nothing : normal behaviour if the imported network is not a CORE UCTE network
    } else {
        // Xnode found in network
        // add a generator on the voltage level on which the X-node dangling line is connected
        
        Bus xNodeBus = danglingLine.get().getTerminal().getBusBreakerView().getConnectableBus()
        xNodeBus.getVoltageLevel().newGenerator()
        .setBus(xNodeBus.getId())
        .setEnsureIdUnicity(true)
        .setId(xnodeId + "_generator")
        .setMaxP(9999)
        .setMinP(0)
        .setTargetP(0)
        .setTargetQ(0)
        .setTargetV(xNodeBus.getVoltageLevel().getNominalV())
        .setVoltageRegulatorOn(false)
        .add()
    }
}
