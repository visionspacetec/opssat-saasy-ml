/* ----------------------------------------------------------------------------
 * Copyright (C) 2021      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : ESA NanoSat MO Framework
 * ----------------------------------------------------------------------------
 * Licensed under European Space Agency Public License (ESA-PL) Weak Copyleft – v2.4
 * You may not use this file except in compliance with the License.
 *
 * Except as expressly set forth in this License, the Software is provided to
 * You on an "as is" basis and without warranties of any kind, including without
 * limitation merchantability, fitness for a particular purpose, absence of
 * defects or errors, accuracy or non-infringement of intellectual property rights.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 * ----------------------------------------------------------------------------
 */
package esa.mo.nmf.apps;

import esa.mo.nmf.apps.saasyml.test.SaaSyMLTest;

import esa.mo.nmf.nanosatmoconnector.NanoSatMOConnectorImpl;
import esa.mo.nmf.MonitorAndControlNMFAdapter;
import esa.mo.nmf.MCRegistration;
import org.ccsds.moims.mo.mal.provider.MALInteraction;
import org.ccsds.moims.mo.mal.structures.Attribute;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.IdentifierList;
import org.ccsds.moims.mo.mal.structures.UInteger;
import org.ccsds.moims.mo.mc.parameter.structures.ParameterRawValueList;
import org.ccsds.moims.mo.mc.structures.AttributeValueList;

import java.util.*;

/**
 * SaaSy ML App
 */
public class SaaSyMLApp {

    private final NanoSatMOConnectorImpl connector;

    public SaaSyMLApp() {

        System.out.println("SaaSyML is running !");

        // testing the ML models
        SaaSyMLTest.testSettings();


        // handle the responses coming from the connector and pushing of requests and results to the connector.
        MCAdapter adapter = new MCAdapter();

        // handle communication with NMF, request services from the NMF ( camera )
        // and push results to NMF, so they are forwarded to the ground
        connector = new NanoSatMOConnectorImpl();

        // supply the adapter to the connector
        connector.init(adapter);

    }

    /**
     * Main command line entry point.
     *
     * @param args the command line arguments
     * @throws java.lang.Exception If there is an error
     */
    public static void main(final String[] args) throws Exception {
        SaaSyMLApp demo = new SaaSyMLApp();
    }

    /**
     * To use with the connector.
     * handles the responses coming from the connector and pushing of requests
     * and results to the connector. That’s why we supply the connector to
     * the adapter by calling adapter.setNMF(connector) and
     * vice versa by calling connector.init(adapter) on startup.
     */
    public class MCAdapter extends MonitorAndControlNMFAdapter {

        @Override
        public void initialRegistrations(MCRegistration registrationObject) {
        }

        @Override
        public Attribute onGetValue(Identifier identifier, Byte rawType) {
            return null;
        }

        @Override
        public Boolean onSetValue(IdentifierList identifiers, ParameterRawValueList values) {
            return false;
        }

        @Override
        public UInteger actionArrived(Identifier name, AttributeValueList attributeValues,
                                      Long actionInstanceObjId, boolean reportProgress, MALInteraction interaction) {
            return null;
        }
    }
}
