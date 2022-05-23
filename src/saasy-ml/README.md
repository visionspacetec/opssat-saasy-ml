This folder contains SaaSy ML as NMF App. 

We should copy it in the folder **sdk/examples/space/**

# Compiling dependencies

## JSAT

$ cd src/lib-sat/

$ git submodule init

$ git submodule update

$ mvn install -Dmaven.test.skip

## Service-layer

$ cd src/service-layer/

$ mvn install

# Deploying NMF app in the SDK

## 1. Update the SDK Package POM

Change the pom.xml in the folder sdk/sdk-package. 

First, add your app to the dependencies.

    <dependency>
      <groupId>int.esa.nmf.sdk.space.saasyml</groupId>
      <artifactId>saasy-ml</artifactId>
      <version>${project.version}</version>
    </dependency>

After that, we have to make sure that the different properties files needed by the NMF are present in the app’s execution directory. 
This is done in an execution of the Maven Antrun Plugin. 

Add a copy task with the execution folder of your app as the todir.

    <copy todir="${esa.nmf.sdk.assembly.outputdir}/home/saasy-ml">
      <fileset dir="${basedir}/src/main/resources/space-common"/>
      <fileset dir="${basedir}/src/main/resources/space-app-root"/>
    </copy>

## 2. Update the Build.xml

Update the sdk/sdk-package/antpkg/build.xml by adding the subtarget:

    <target name="emit-space-app-saasy-ml">
        <ant antfile="antpkg/build_shell_script.xml">
          <property name="mainClass" value="esa.mo.nmf.apps.SaaSyMLApp"/>
          <property name="id" value="start_saasy-ml"/>
          <property name="nmf_home" value="`cd ../nmf > /dev/null; pwd`"/>
          <property name="nmf_lib" value="`cd ../nmf/lib > /dev/null; pwd`"/>
          <property name="binDir" value="saasy-ml"/>
        </ant>
        <ant antfile="antpkg/build_batch_script.xml">
          <property name="mainClass" value="esa.mo.nmf.apps.SaaSyMLApp"/>
          <property name="id" value="start_saasy-ml"/>
          <property name="nmf_home" value="%cd%\..\nmf"/>
          <property name="nmf_lib" value="`cd ../nmf/lib > /dev/null; pwd`"/>
          <property name="binDir" value="saasy-ml"/>
        </ant>
    </target>

- The target **name** can be anything which is not already in use. 
We just use this name later to add the dependency.
- The **id** property’s value has to have the prefix “start_”, so it can be recognised by the supervisor.
- The property **mainClass** contains the fully qualified name for the class in our app containing the main methods.

A more detailed explanation of the file sdk/sdk-package/antpkg/build.xml can be found [here](https://nanosat-mo-framework.readthedocs.io/en/latest/apps/packaging.html).

Add the subtarget to the dependencies:

    <target name="build" depends="emit-ctt, emit-com-archive-tool, emit-simulator-gui, emit-space-supervisor, emit-space-app-all-mc-services,
    emit-space-app-publish-clock, emit-space-app-camera, emit-space-app-benchmark, emit-space-app-payloads-test,
    emit-space-app-waveform, emit-space-app-camera-acquisitor-system, emit-space-app-picture-processor,
    emit-space-app-mp-demo, emit-ground-app-mp-demo, emit-space-app-saasy-ml">
    <!--This empty target is used as the top level target. Add your app targets to the depends attribute! -->
    </target>

## 3. Deploy

First, go into the root folder "sdk/examples/space/saasy-ml/" and call 

    $ mvn install 

Then, open a console in the **sdk/sdk-package** folder and execute:

    $ mvn install

That’s it, our app’s start scripts and properties are now residing in:

    sdk/sdk-package/target/nmf-sdk-2.1.0-SNAPSHOT/home/saasy-ml/

We can start the NMF supervisor with simulator, 

# Simulator

[Source](https://nanosat-mo-framework.readthedocs.io/en/latest/sdk.html#id6)

## 1. Run the supervisor. 

    $ cd sdk/sdk-package/target/nmf-sdk-2.1.0-SNAPSHOT/home/nmf/nanosat-mo-supervisor-sim/ 

    $ ./nanosat-mo-supervisor-sim.bat

## 2. Run CTT.

    $ cd sdk/sdk-package/target/nmf-sdk-2.1.0-SNAPSHOT/home/nmf/consumer-test-tool/ 

    $ ./consumer-test-tool.bat

## 3. Connecting to the Supervisor using CTT

The supervisor outputs a URI on the console. 

This URI follows the pattern:

    maltcp://SOME_ADDRESS:PORT/nanosat-mo-supervisor-Directory 

Paste this URI into the field in the Communication Settings tab of the CTT and 
click the button Fetch information. 

In the Providers List, the supervisor should show up. 

The table on the right side should list some services. 
Now click the button Connect to Selected Provider which results in a new tab appearing next to the Communication Settings. 
You now have a working connection to the supervisor and are able to start apps and check messages.

