# CRAC import & creation

## CRAC import/export

The [OpenRAO CRAC object model](json) can be directly imported and exported using the CRAC API.  

The JSON format - also called OpenRAO internal format - is a raw image of the CRAC object model of OpenRAO. It is particularly suited to exchange a CRAC java object through files, for instance to exchange CRAC data between microservices or Kubernetes pods of an application. It has an importer and an exporter. The complete round-trip (java object → export → json file → import → java object) has been designed so that the CRAC at the beginning of the chain is exactly the same as the one at the end of the chain.  

Examples of JSON formats are given on this [page](json).  

Examples of uses of CRAC import/export are given below:  

~~~java
// import a CRAC from a PATH
// (a network is required to reconstruct the contingency elements from the network elements id) 
Crac crac = Crac.read(Paths.get("/tmp/crac.json"), network);

// import a CRAC from an input stream
Crac crac = Crac.read("crac.json", new FileInputStream(new File("/tmp/crac.json")), network);

// export a CRAC in JSON in a file
crac.write("JSON", Paths.get("/tmp/crac.json"));
~~~

## Versioning of internal JSON CRAC files
Json files and json importer/exporter are versioned.  
The version number does not correspond to the version number of powsybl-open-rao. The version only increase when a modification is made within the JSON importer / exporter.  
- The number version of the json files corresponds to the number of version of the exporter by which it has been exported.
- A Json file can be imported by powsybl-open-rao only if the versions of the file and the importer are compatible (see below)  


| File version (example) | Importer version (example) | Is compatible? | Explanation                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
|------------------------|----------------------------|----------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1.0                    | 1.0                        | YES            |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| 1.0                    | 1.1                        | YES            | The importer is compatible with all the previous versions of the json file, **given that the first number of the version does not change**!                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| 1.1                    | 1.0                        | NO[^1]         | The importer is *a priori* not compatible with newer versions of the file.  <br> For instance, if a json CRAC is generated with powsybl-open-rao 5.3.0 (importer version = 1.3) and read with powsybl-open-rao 5.1.0 (importer version = 1.2), the importer should not work. <br> However, the import is not systematically rejected. It might even work in some situation. <br> For instance, in the example above, if a 1.1 crac does not contain the feature specific to its version, the newly introduced switchPair elementary action, it will still be importable by the 1.0 importer. |
| 1.0                    | 2.0                        | NO             | compatibility is not ensured anymore when first version number change                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| 2.0                    | 1.0                        | NO             | compatibility is not ensured anymore when first version number change                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |

[^1]: might work in some situations


## NativeCrac, CracCreators and CracCreationContext

The OpenRAO CRAC object model is not a bijection of all existing formats. To handle more complex formats, which do not have a one-to-one mapping with the OpenRAO CRAC object model, an overlay has been designed.  

![NativeCrac](/_static/img/NativeCrac.png)

- The **NativeCrac** is a java object which is a raw representation of the initial CRAC "complex" format. The NativeCrac contains all the information present in the initial file. For instance, for xml CRAC formats, their NativeCrac contains classes which are automatically generated from the XSD of the format.

- The NativeCrac can be imported from a file with a **NativeCracImporter**.

- The NativeCrac can be converted in a CRAC with a **CracCreator**, the CracCreator needs a network to interpret the data 
of the NativeCrac. Moreover, the creators of formats which contain more than one timestamp also need a timestamp in the 
form of a java OffsetDateTime as the created CRAC object only contains one timestamp. [CracCreationParameters](creation-parameters) 
can also be provided to the CracCreator, with some configurations which set the behaviour of the Creator.

- The CracCreator returns a [CracCreationContext](creation-context). It contains:  
-- the created CRAC object  
-- additional information which explains how the initial format has been mapped into the OpenRAO format. This mapping is often not straightforward (see below). The CracCreationContext enables to keep in memory a link between the NativeCrac and the CRAC objects.


> 💡  **NOTE**  
> The flow-based constraint document contains two types of CNECs: base-case CNECs and post-contingency CNECs. Each post-contingency CNECs corresponds to two FlowCnecs in OpenRAO: one FlowCnec associated with the outage instant and one FlowCnec associated with a curative instant. The initial id of the CNEC cannot be kept, as it would be duplicated into the OpenRAO format, and new ids are therefore created by the CracCreator on the fly.  
> 
> As a consequence, the interpretation of the created CRAC is not straightforward as it contains more Cnecs than the initial format, and with different ids.  
> 
> The CracCreationContext is here to ease the interpretation of the CRAC, and for instance store the information on how each CNEC of the initial format has been mapped - in one or two FlowCnecs - and for a given CNEC of the initial format, what are the id(s) of the created FlowCnec(s).
> 
> This is an example, but the CracCreationContext of the fb-constraint-document is also used for other reasons, such as:
> - keep track of the data which hasn't been imported in OpenRAO due to quality issue
> - keep track of the branch which has been inverted because the initial format was not consistent with the iidm network (the Network is needed for that operation, that is an example of the reason why it is required by the CracCreator)
> - keep some information of the initial format which are not imported in the OpenRAO CRAC.
> 
> In the CORE CC process, this CracCreationContext is re-used when results are exported at the end of the RAO, in order to roll back the modifications which has been made during the creation, and export at the end of the process a CNE file which is consistent with the initial CRAC file.

The formats handled by the CracCreator are:	
- [FlowBasedConstraint document](fbconstraint), also known as Merged-CB, CBCORA or F301 ([OPEN-RAO-crac-creator-fb-constraint](https://github.com/powsybl/powsybl-open-rao/tree/main/data/crac-creation/crac-creator-fb-constraint))
- [CSE CRAC](cse) ([OPEN-RAO-crac-creator-cse](https://github.com/powsybl/powsybl-open-rao/tree/main/data/crac-creation/crac-creator-cse))
- [CIM CRAC](cim) ([OPEN-RAO-crac-creator-cim](https://github.com/powsybl/powsybl-open-rao/tree/main/data/crac-creation/crac-creator-cim))
- [CSA PROFILES CRAC](csa) ([OPEN-RAO-crac-creator-csa-profiles](https://github.com/powsybl/powsybl-open-rao/tree/main/data/crac-creation/crac-creator-csa-profiles))

When creating a CRAC from one of these formats, the chain presented above can be coded step by step, or utility methods can be used to make all the import in one line of code. Some examples are given below:

~~~java
// use the crac-creator-api to import a Crac in one go
Crac crac = Crac.read(Paths.get("/complexCrac.xml"), network, null).getCrac();


// use the crac-creator-api to import a Crac in two steps, with one timestamp
OffsetDateTime offsetDateTime = OffsetDateTime.parse("2019-01-08T00:30Z");

NativeCrac nativeCrac = NativeCracImporters.importData(Paths.get("/complexCrac.xml"));
CracCreationContext cracCreationContext = CracCreators.createCrac(nativeCrac, network, offsetDateTime);

Crac crac = cracCreationContext.getCrac();

// if the format is known, use directly the suited implementations of NativeCracImporter and CracCreator
// if no configuration is explicitly given, use the default one
// this approach is preferred as the previous one is the format is known, as it returns directly the expected implementation of the CracCreationContext
FbConstraint nativeCrac = new FbConstraintImporter().importNativeCrac(new FileInputStream(new File("fbDocument.xml")));
CracCreationParameters paramaters = CracCreationParameters.load();
FbConstraintCreationContext cracCreationContext = new FbConstraintCracCreator().createCrac(nativeCrac, network, offsetDateTime, parameters);
Crac crac = cracCreationContext.getCrac();

// alternatively, create a CRAC using a specific import configuration load from a JSON format
CracCreationParameters parameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/cse-crac-creation-parameters-nok.json"));
~~~

## Implementing new CRAC formats
You are welcome to contribute to the project if you need to import a new native CRAC format to be used in OpenRAO.  
You can find inspiration in existing CRAC creators' code:
- [OPEN-RAO-crac-creator-fb-constraint](https://github.com/powsybl/powsybl-open-rao/tree/main/data/crac-creation/crac-creator-fb-constraint)
- [OPEN-RAO-crac-creator-cse](https://github.com/powsybl/powsybl-open-rao/tree/main/data/crac-creation/crac-creator-cse)
- [OPEN-RAO-crac-creator-cim](https://github.com/powsybl/powsybl-open-rao/tree/main/data/crac-creation/crac-creator-cim)
- [OPEN-RAO-crac-creator-csa-profiles](https://github.com/powsybl/powsybl-open-rao/tree/main/data/crac-creation/crac-creator-csa-profiles)

To help you with that, the package [OPEN-RAO-crac-creation-util](https://github.com/powsybl/powsybl-open-rao/tree/main/data/crac-creation/crac-creation-util)
offers utility classes that can make mapping the CRAC elements to the PowSyBl network elements much easier.
You should also get familiar with our java [CRAC creation API](json).  

## Example of application of CRAC creation / import / export

![flow-diagram](/_static/img/flow-diagram-nativeCrac.png)
