import com.powsybl.iidm.network.Bus
import com.powsybl.iidm.network.BoundaryLine

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
    Optional<BoundaryLine> boundaryLine = network.boundaryLineStream.filter({dl -> dl.getUcteXnodeCode().equals(xnodeId)}).findAny()

    if (!boundaryLine.present) {
        // Xnode not found in network
        // do nothing : normal behaviour if the imported network is not a CORE UCTE network
    } else {
        // Xnode found in network
        // add a generator on the voltage level on which the X-node dangling line is connected
        
        Bus xNodeBus = boundaryLine.get().getTerminal().getBusBreakerView().getConnectableBus()
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
