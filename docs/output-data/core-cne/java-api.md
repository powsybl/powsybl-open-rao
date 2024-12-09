After completing the RAO, the user can export the [`RaoResult`](/output-data/rao-result.md) object as a CORE CNE file using the `write` method with the `"CORE-CNE""` format:

~~~java
// RaoResult interface
public void write(String format, CracCreationContext cracCreationContext, Properties properties, OutputStream outputStream)
~~~

With:
- **raoResult**: the [RaoResult](/output-data/rao-result.md) object containing selected remedial actions and flow results.
- **cracCreationContext**: the [CracCreationContext object](/input-data/crac/creation-context.md) generated during 
  [CRAC creation](/input-data/crac/import.md). CORE CNE export only handles [UcteCracCreationContext](/input-data/crac/creation-context.md#ucte-implementation) 
  subtype, because it follows the UCTE conventions.
- **properties**: a specific object that te user should define, containing meta-information that will be written 
  in the header of the CNE file as well as relevant RAO parameters:
  - **`"rao-result.export.core-cne.document-id"`**: document ID to be written in "mRID" field
  - **`"rao-result.export.core-cne.revision-number"`**: integer to be written in "revisionNumber" field
  - **`"rao-result.export.core-cne.domain-id"`**: domain ID to be written in "domain.mRID" field (usually an [ENTSO-E EICode](https://www.entsoe.eu/data/energy-identification-codes-eic/))
  - **`"rao-result.export.core-cne.process-type"`**: the ENTSO-E code of the process type, to be written in "process.processType" field:
    - **A48**: Day-ahead capacity determination, used for CORE region
    - ~~**Z01**~~: Day-ahead capacity determination, used for SWE region (so don't use it here)
  - **`"rao-result.export.core-cne.sender-id"`**: ID of the sender of the CNE document, to be written in "sender_MarketParticipant.mRID" field 
    (usually an [ENTSO-E EICode](https://www.entsoe.eu/data/energy-identification-codes-eic/))
  - **`"rao-result.export.core-cne.sender-role"`**: ENTSO-E code defining the role of the sender of the CNE document, to be written in 
    "sender_MarketParticipant.marketRole.type" field:
    - **A04**: system operator
    - **A36**: capacity coordinator
    - **A44**: regional security coordinator
  - **`"rao-result.export.core-cne.receiver-id"`**: ID of the receiver of the CNE document, to be written in "receiver_MarketParticipant.mRID" field 
    (usually an [ENTSO-E EICode](https://www.entsoe.eu/data/energy-identification-codes-eic/))
  - **`"rao-result.export.core-cne.receiver-role"`**: ENTSO-E code defining the role of the receiver of the CNE document, to be written in
    "receiver_MarketParticipant.marketRole.type" field. Same value options as senderRole.
  - **`"rao-result.export.core-cne.time-interval"`**: time interval of document applicability, to be written in "time_Period.timeInterval" field. It should 
    be formatted as follows: "YYYY-MM-DDTHH:MMZ/YYYY-MM-DDTHH:MMZ" (start date / end date).
  - **`"rao-result.export.core-cne.relative-positive-margins"`** (optional, default is `"false"`)
  - **`"rao-result.export.core-cne.with-loop-flows"`** (optional, default is `"false"`)
  - **`"rao-result.export.core-cne.mnec-acceptable-margin-diminution"`** (optional, default is `"0"`)

Here is a complete example:

~~~java
// Fetch input data (network) and parameters
Network network = ...
RaoParameters raoParameters = ...
// Create CRAC
CracCreationContext cracCreationContext = CracCreators.createCrac(...);
Crac crac = cracCreationContext.getCrac();
// Run RAO
RaoResult raoResult = Rao.find(...).run(...)
// Set CORE-CNE export properties
Properties properties = new Properties();
properties.setProperty("rao-result.export.core-cne.document-id", "DOCUMENT_ID");
properties.setProperty("rao-result.export.core-cne.revision-number", "1");
properties.setProperty("rao-result.export.core-cne.domain-id", "DOMAIN_ID");
properties.setProperty("rao-result.export.core-cne.process-type", "A48"); // DAY_AHEAD_CC
properties.setProperty("rao-result.export.core-cne.sender-id", "SENDER_ID");
properties.setProperty("rao-result.export.core-cne.sender-role", "A44"); // REGIONAL_SECURITY_COORDINATOR
properties.setProperty("rao-result.export.core-cne.receiver-id", "RECEIVER_ID");
properties.setProperty("rao-result.export.core-cne.receiver-role", "A36"); // CAPACITY_COORDINATOR
properties.setProperty("rao-result.export.core-cne.time-interval", "2021-10-30T22:00Z/2021-10-31T23:00Z");
switch (raoParameters.getObjectiveFunctionParameters().getType()) {
    case MAX_MIN_RELATIVE_MARGIN -> properties.setProperty("rao-result.export.core-cne.relative-positive-margins", "true");
    case MAX_MIN_MARGIN -> properties.setProperty("rao-result.export.core-cne.relative-positive-margins", "false");
}
if (raoParameters.hasExtension(LoopFlowParametersExtension.class)) {
    properties.setProperty("rao-result.export.core-cne.with-loop-flows", "true");
}
if (raoParameters.hasExtension(MnecParametersExtension.class)) {
    properties.setProperty("rao-result.export.core-cne.mnec-acceptable-margin-diminution", String.valueOf(raoParameters.getExtension(MnecParametersExtension.class).getAcceptableMarginDecrease()));
}
// Export CNE to output stream
OutputStream os = ...
raoResult.write("CORE-CNE", cracCreationContext, properties, os);
~~~
