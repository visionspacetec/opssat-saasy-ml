This folder contains SaaSy ML as NMF App. 

We should copy it in the folder **sdk/examples/space/**

# Compiling dependencies

Please, be sure you meet the dependencies specified in [Readme](../../README.md)

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

First, go into the root folder **sdk/examples/space/saasy-ml/** and call 

    $ mvn install 

Then, open a console in the **sdk/sdk-package** folder and execute:

    $ mvn install

That’s it, our app’s start scripts and properties are now residing in:

    sdk/sdk-package/target/nmf-sdk-2.1.0-SNAPSHOT/home/saasy-ml/

We can start the NMF supervisor with simulator, 

# Simulator

[Source](https://nanosat-mo-framework.readthedocs.io/en/latest/sdk.html#id6)

## 1. Run the supervisor

    $ cd sdk/sdk-package/target/nmf-sdk-2.1.0-SNAPSHOT/home/nmf/nanosat-mo-supervisor-sim/ 

    $ ./nanosat-mo-supervisor-sim.bat

## 2. Run CTT

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

# Packing and deployment

1. Clone [NMF Core](https://github.com/esa/nanosat-mo-framework.git)
2. Checkout last commit on **dev** branch
3. mvn install in repo's base dir
4. Clone [NMF Mission Ops-Sat](https://github.com/esa/nmf-mission-ops-sat.git)
5. Checkout last commit on **dev** branch
6. mvn install in repo's base dir
7. Open the pom.xml file in the opssat-package directory. 

   In the "exp profile", edit your experimenter ID expId, expApid (typically equals to sum of expId + 1024), and the Maven information for your app. Make sure that expVersion matches the version defined in your app’s POM.

        <profile>
         <id>exp</id>
         <properties>
          <isExp>true</isExp>
          <expId>213</expId>
          <expApid>1237</expApid>
          <expVersion>2.1.0-SNAPSHOT</expVersion>
         </properties>
         <dependencies>
          <dependency>
           <groupId>int.esa.nmf.sdk.space.saasyml</groupId>
           <artifactId>saasy-ml</artifactId>
           <version>${expVersion}</version>
          </dependency>
         </dependencies>


   8. In the **artifactItems** configuration of the **expLib** execution of the maven-dependency-plugin. 
   Add the following. We also add an **artifactItem** for each external dependency that the app has.

            <configuration>
              <artifactItems>
                <artifactItem>
                  <!-- Change the following 3 properties to match the information of your app -->
                  <groupId>int.esa.nmf.sdk.space.saasyml</groupId>
                  <artifactId>saasy-ml</artifactId>
                  <!-- The declared version is arbitrary and does not have to match the NMF version -->
                  <version>2.1.0-SNAPSHOT</version>
                  <!-- Do not change this -->
                  <type>jar</type>
                  <overWrite>true</overWrite>
                  <outputDirectory>${esa.nmf.mission.opssat.assembly.outputdir}/experimenter-package/home/exp${expId}/lib/</outputDirectory>
                </artifactItem>
    
            <artifactItem>
              <groupId>int.esa.nmf.sdk.space.saasyml</groupId>
              <artifactId>saasyml-service-layer</artifactId>
              <version>0.1.0-SNAPSHOT</version>
              <type>jar</type>
              <overWrite>true</overWrite>
              <outputDirectory>${esa.nmf.mission.opssat.assembly.outputdir}/experimenter-package/home/exp${expId}/lib/</outputDirectory>
            </artifactItem>
    
            <artifactItem>
              <groupId>com.edwardraff</groupId>
              <artifactId>JSAT</artifactId>
              <version>0.1.0-SNAPSHOT</version>
              <type>jar</type>
              <overWrite>true</overWrite>
              <outputDirectory>${esa.nmf.mission.opssat.assembly.outputdir}/experimenter-package/home/exp${expId}/lib/</outputDirectory>
            </artifactItem>
    
          </artifactItems>
          </configuration>

   9. Open the file **copy.xml** in the **opssat-package** folder. In the target **copyExp** edit the filter for **MAIN_CLASS_NAME**.
    
           <target name="copyExp">
            <copy todir="${esa.nmf.mission.opssat.assembly.outputdir}/experimenter-package/home/exp${expId}/">
              <fileset dir="${basedir}/src/main/resources/space-common"/>
              <fileset dir="${basedir}/src/main/resources/space-app-root"/>
              <filterset>
                <filter token="MAIN_CLASS_NAME" value="esa.mo.nmf.apps.SaaSyMLApp"/>
                <filter token="APID" value="${expApid}"/>
                <filter token="NMF_HOME" value="`cd ../nmf > /dev/null; pwd`"/>
                <filter token="NMF_LIB" value="`cd ../nmf/lib > /dev/null; pwd`"/>
                <filter token="USER" value="exp${expId}"/>
                <filter token="MAX_HEAP" value="128m"/>
              </filterset>
              <firstmatchmapper>
                <globmapper from="startscript.sh" to="start_exp${expId}.sh"/>
                <globmapper from="*" to="*"/>
              </firstmatchmapper>
            </copy>
            <chmod dir="${esa.nmf.mission.opssat.assembly.outputdir}" perm="ugo+rx" includes="**/*.sh"/>
          </target>

   10. Invoke mvn clean install -Pexp in the **opssat-package** directory. 
   11. Go to the folder **target/nmf-ops-sat-VERSION/experimenter-package/** and you will find the directory structure to package your app as an IPK for OPS-SAT.
   12. Zip the **home** folder and submitted **/home/exp213/toESOC/sepp_packages** in your home folder on OSDRS.
   
       Follow the naming convention: exp213_2.0.1_CesarGuzman.zip