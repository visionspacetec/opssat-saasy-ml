<h2>
    <p align="center">
        <img src="img/logo.png" /></br>
        On-board Machine Learning Software as a Service
    </p>
</h2>

# Abstract
The **SaaSyML** app developed for the OPS-SAT spacecraft provides open access to on-board Machine Learning (ML) capabilities that an experimenter can interact with via RESTful Application Programming Interface (API) endpoints. The app's architecture follows the successes of Software as a Service (SaaS) in modern Web-based software engineering and implements the "**as-a-Service**" model, thus introducing the concept of Satellite Platform as a Service (SPaaS). An experimenter app on-board OPS-SAT can subscribe to SaaSyML's training data feed and pull measurement, telemetry, and housekeeping data from any of the spacecraft's instruments or its on-board software datapool. The ML features provided by the SaaSyML app cover both training and prediction operations. The Java Statistical Analysis Tool (JSAT) open-source java library for ML is used thus unlocking access to over 100 training algorithms on-board a flying mission. Past experiments have successfully implemented ML on-board OPS-SAT but have yet to offer any comprehensive re-usability. SaaSyML's service-oriented approach spares the experimenters the complexities of having to implement their data provisioning and ML solutions so that they can focus instead on expanding the field of experimentation and use-cases for applied ML in space. A further novelty is also introduced with a plugin design for an software extension mechanism that allows experimenters to inject custom code to address ML needs specific to their experiments (e.g. calculating target labels/classes during supervised learning training operations). SaaSyML is developed using the Eclipse Vert.x event-driven application toolkit that runs on the Java Virtual Machine (JVM). This design choice introduces event-driven software engineering and practical use of the spacecraft dual-core payload computer and Linux environment. SaaSyML is a reference in embracing and leveraging multi-threaded and multi-core software design for space applications. This translates to non-blocking ML training and prediction operations running in parallel while multiple experimenter apps interact with the service. SaaSyML demonstrates how a more capable space-grade processor enables a paradigm shift towards developing more sophisticated client facing space software with reduced development complexity, effort, and cost.

# Citation
A published peer reviewed paper on this project can be found on [ieeexplore.ieee.org](https://ieeexplore.ieee.org/document/10115531) and on [researchgate.net](https://www.researchgate.net/publication/367207387_SaaSyML_Software_as_a_Service_for_Machine_Learning_On-board_the_OPS-SAT_Spacecraft). We appreciate citations if you reference this work in your upcoming publication. Thank you!

## Plain Text
G. Labrèche and C. G. Alvarez, "SaaSyML: Software as a Service for Machine Learning On-board the OPS-SAT Spacecraft," _2023 IEEE Aerospace Conference_, Big Sky, MT, USA, 2023, pp. 1-9, doi: [10.1109/AERO55745.2023.10115531](https://ieeexplore.ieee.org/document/10115531).

## BibTex
```bibtex
@INPROCEEDINGS{10115531,
  author={Labrèche, Georges and Alvarez, Cesar Guzman},
  booktitle={2023 IEEE Aerospace Conference}, 
  title={SaaSyML: Software as a Service for Machine Learning On-board the OPS-SAT Spacecraft}, 
  year={2023},
  volume={},
  number={},
  pages={1-9},
  doi={10.1109/AERO55745.2023.10115531}}
```

# Requirements

- Java 8.
- [JSAT library](https://github.com/EdwardRaff/JSAT).

## Java 8 
Follow [these instructions](https://www.techruzz.com/blog/how-to-download-and-install-java-jdk-8-on-windows-11) for Windows 11.

## JSAT 

```bash
$ cd src/lib-jsat
$ git submodule init
$ git submodule update
$ cd JSAT
$ mvn install
```

You can skip tests in Maven with the following command:

```bash
$ mvn install -Dmaven.test.skip
```

Or with Maven 3.8.6
```bash
$ mvn install -DskipTests
```

# Project Modules
Start with instructions in [saasy-ml-app](src/saasy-ml-app):

- [saasy-ml-app](src/saasy-ml-app): the SaaSyML NMF app, i.e. the main application.
- [saasy-ml-layer-pipeline](src/saasy-ml-layer-pipeline): the ML pipeline that invokes JSAT algorithms.
- [saasy-ml-app-plugins-api](src/saasy-ml-app-plugins-api): defines _Interfaces_ that concrete plugin classes must implement.
- [saasy-ml-app-plugins](src/saasy-ml-app-plugins): collects class implementations for interfaces defined in [saasy-ml-app-plugins-api](src/saasy-ml-app-plugins-api).
- [saasy-ml-app-api-client](src/saasy-ml-app-api-client): a client app to send test API requests to the SaaSyML NMF App.

# Resources
- The European Space Agency's [OPS-SAT spacecraft](https://opssat1.esoc.esa.int/).
- The [NanoSat MO Framework (NNF) Documentation](https://nanosat-mo-framework.readthedocs.io/en/latest/).
- Raff, Edward. (2017). [JSAT: Java statistical analysis tool, a library for machine learning](https://www.jmlr.org/papers/v18/16-131.html). Journal of Machine Learning Research. 18. 1-5. 
- [JSAT on GitHub](https://github.com/EdwardRaff/JSAT).
