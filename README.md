<h2>
    <p align="center">
        <img src="img/logo.png" /></br>
        On-board Machine Learning Software as a Service
    </p>
</h2>


<p align="justify">
<strong>SaaSy ML</strong> provides experimenters with on-board Machine Learning (ML) functionalities that any app can subscribe to via a Software as a Service (SaaS) app hosted on the Satellite Experimental Processing Platform (SEPP). The JSAT open-source java library for ML [1] is re-used thus making accessible over 100 training algorithms on-board a flying mission. SaaSy ML's service-oriented approach spares the experimenters the complexities of having to implement their own data provisioning and ML solutions so that they can focus instead on their experiments’ objectives. SaaSy ML is conceived as a plugin architecture that allows experimenters to inject custom code that address specific ML needs (e.g. calculating target labels/classes during supervised learning training operations). SaaSy ML's design also includes a data subscription service to feed training data directly into an experimenter’s app. An app can register to SaaSy ML's data feed and pull selected training data from any of the spacecraft’s instruments or its OBSW datapool. Our app takes advantage of JVM’s Thread Pool implementations as well as the SEPP’s dual core processor so that non-blocking ML training and prediction operations can run in the background while multiple researcher apps interact with SaaSy ML.
</p>

# Table of Content

- [Requirements](#requirements)
- [Install](#install)

# Requirements

- Java 8
- JSAT library: Fork of the original one. Use Java 8.

# Install

## Java 8

- Windows 11: Please, follows the instructions [here](https://www.techruzz.com/blog/how-to-download-and-install-java-jdk-8-on-windows-11)

## JSAT 

    $ cd src/lib-sat/
    
    $ git submodule init
    
    $ git submodule update
    
    $ mvn install

You can skip the test of maven with the following command:

    $ mvn install -Dmaven.test.skip

## SaaSy-ML as NMF App

Follow the instructions on the [Readme](src/saasy-ml-app/README.md)

# References

[1] Edward Raff. 2017. JSAT: Java statistical analysis tool, a library for machine learning. J. Mach. Learn. Res. 18, 1 (January 2017), 792–796. Url: https://github.com/EdwardRaff/JSAT
