package com.example.backend.foottraffic.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.ws.test.client.RequestMatchers.anything;
import static org.springframework.ws.test.client.ResponseCreators.withSoapEnvelope;

import com.ecomap.ftsim.ws.xml.SimulateCellRequest;
import com.ecomap.ftsim.ws.xml.SimulateCellResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.test.client.MockWebServiceServer;

/**
 * Contract check: JAXB marshalling of {@link SimulateCellRequest} / response matches the WSDL/XSD
 * used by the SOAP simulation service (see {@code soap-foot-traffic-server}). Uses in-memory
 * {@link MockWebServiceServer} instead of a real HTTP port.
 */
class FootTrafficSoapContractMockServerTest {

    private MockWebServiceServer mockServer;
    private WebServiceTemplate template;

    @BeforeEach
    void setUp() throws Exception {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("com.ecomap.ftsim.ws.xml");
        marshaller.afterPropertiesSet();

        template = new WebServiceTemplate();
        template.setMarshaller(marshaller);
        template.setUnmarshaller(marshaller);
        template.setDefaultUri("http://localhost/ft-ws");

        mockServer = MockWebServiceServer.createServer(template);
    }

    @AfterEach
    void tearDown() {
        mockServer.reset();
    }

    @Test
    void webServiceTemplate_unmarshallsSimulateCellResponse() throws Exception {
        mockServer
                .expect(anything())
                .andRespond(
                        withSoapEnvelope(new ClassPathResource("soap-ft/simulate-cell-response-envelope.xml")));

        SimulateCellRequest req = new SimulateCellRequest();
        req.setH3Index("8939aab940fffff");
        req.setScenarioId(1);
        req.setJitterSalt(0L);
        req.setPopulationDensity(0.6);
        req.setAvgIncome(15000.0);

        Object out = template.marshalSendAndReceive(req);
        assertThat(out).isInstanceOf(SimulateCellResponse.class);
        SimulateCellResponse res = (SimulateCellResponse) out;
        assertThat(res.getArchetype()).isEqualTo("COMMERCIAL_CORE");
        assertThat(res.getBaselineDaily()).isEqualTo(500);
        assertThat(res.getPeakHourly()).isEqualTo(42);
        assertThat(res.getDriverPoiCount()).isEqualTo(3);
        mockServer.verify();
    }
}
