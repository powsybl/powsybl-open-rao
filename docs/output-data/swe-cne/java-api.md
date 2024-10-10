After completing the RAO, the user can export the SWE CNE file using this method of [SweCneExporter](https://github.com/powsybl/powsybl-open-rao/blob/main/data/result-exporter/swe-cne-exporter/src/main/java/com/powsybl/openrao/data/swecneexporter/SweCneExporter.java):

~~~java
public void exportCne(Crac crac, 
        CimCracCreationContext cracCreationContext, 
        RaoResult raoResult, RaoParameters raoParameters, 
        CneExporterParameters exporterParameters, OutputStream outputStream)
~~~

With:
- **crac**: the [CRAC object](/input-data/crac/json.md) used for the RAO.
- **cracCreationContext**: the [CimCracCreationContext object](/input-data/crac/creation-context.md#cim-implementation) generated during
  [CRAC creation](/input-data/crac/import.md) from a native [CIM CRAC file](/input-data/crac/cim.md).
- **raoResult**: the [RaoResult](/output-data/rao-result.md) object containing selected remedial actions and flow
  results, as well as [angle results](/castor/monitoring.md) if the CRAC contains [Angle CNECs](/input-data/crac/json.md#angle-cnecs)
  > ⚠️  **NOTE**  
  > The exporter will fail if angle CNECs are present in the CRAC, but the RAO result does not contain angle results.  
  > See how to compute angle results [here](/castor/monitoring-algorithm.md).
- **raoParameters**: the [RaoParameters](/parameters.md) used in the RAO.
- **exporterParameters**: a specific object that the user should define, containing meta-information that will be written
  in the header of the CNE file:
  - **documentId**: document ID to be written in "mRID" field
  - **revisionNumber**: integer to be written in "revisionNumber" field
  - **domainId**: domain ID to be written in "domain.mRID" field (usually an [ENTSO-E EICode](https://www.entsoe.eu/data/energy-identification-codes-eic/))
  - **processType**: the ENTSO-E code of the process type, to be written in "process.processType" field:
    - ~~**A48**~~: Day-ahead capacity determination, used for CORE region (so don't use it here)
    - **Z01**: Day-ahead capacity determination, used for SWE  region
  - **senderId**: ID of the sender of the CNE document, to be written in "sender_MarketParticipant.mRID" field
    (usually an [ENTSO-E EICode](https://www.entsoe.eu/data/energy-identification-codes-eic/))
  - **senderRole**: ENTSO-E code defining the role of the sender of the CNE document, to be written in
    "sender_MarketParticipant.marketRole.type" field:
    - **A04**: system operator
    - **A36**: capacity coordinator
    - **A44**: regional security coordinator
  - **receiverId**: ID of the receiver of the CNE document, to be written in "receiver_MarketParticipant.mRID" field
    (usually an [ENTSO-E EICode](https://www.entsoe.eu/data/energy-identification-codes-eic/))
  - **receiverRole**: ENTSO-E code defining the role of the receiver of the CNE document, to be written in
    "receiver_MarketParticipant.marketRole.type" field. Same value options as senderRole.
  - **timeInterval**: time interval of document applicability, to be written in "time_Period.timeInterval" field. It should
    be formatted as follows: "YYYY-MM-DDTHH:MMZ/YYYY-MM-DDTHH:MMZ" (start date / end date).

Here is a complete example:

~~~java
// Fetch input data (network, glsk) and parameters
Network network = ...
CimGlskDocument glsk = ...
OffsetDateTime glskOffsetDateTime = ...
LoadFlowParameters loadFlowParameters = ...
RaoParameters raoParameters = ...
// Create CRAC
CimCracCreationContext cracCreationContext = new CimCracCreator().createCrac(...);
Crac crac = cracCreationContext.getCrac();
// Run RAO
RaoResult raoResult = Rao.find(...).run(...)
// Run angle monitoring and update RAO result
RaoResult RaoResultWithAngleMonitoring = new AngleMonitoring(crac, network, raoResult, glsk).runAndUpdateRaoResult("OpenLoadFlow", loadFlowParameters, 2, glskOffsetDateTime);
// Set CNE header parameters
CneExporterParameters exporterParameters = new CneExporterParameters("DOCUMENT_ID", 1, "DOMAIN_ID",
                                            CneExporterParameters.ProcessType.DAY_AHEAD_CC, "SENDER_ID",
                                            CneExporterParameters.RoleType.REGIONAL_SECURITY_COORDINATOR, "RECEIVER_ID",
                                            CneExporterParameters.RoleType.CAPACITY_COORDINATOR,
                                            "2021-10-30T22:00Z/2021-10-31T23:00Z");
// Export CNE to output stream
OutputStream os = ...
new SweCneExporter().exportCne(crac, cracCreationContext, raoResult, raoParameters, exporterParameters, os); 
~~~
