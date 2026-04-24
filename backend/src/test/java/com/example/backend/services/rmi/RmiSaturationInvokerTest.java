package com.example.backend.services.rmi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.ecomap.rmi.scoring.ScoringRemote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RmiSaturationInvokerTest {

    @Test
    void invokeComputeSaturationScore_resolvesAndCallsRemote() throws Exception {
        ScoringRemote remote = mock(ScoringRemote.class);
        when(remote.computeSaturationScore(2, 3, 0.4)).thenReturn(55.0);
        Registry registry = mock(Registry.class);
        when(registry.lookup("ScoringService")).thenReturn(remote);
        RmiSaturationInvoker inv = new RmiSaturationInvoker();
        ReflectionTestUtils.setField(inv, "host", "127.0.0.1");
        ReflectionTestUtils.setField(inv, "port", 1099);
        ReflectionTestUtils.setField(inv, "serviceName", "ScoringService");

        try (MockedStatic<LocateRegistry> reg = Mockito.mockStatic(LocateRegistry.class)) {
            reg.when(() -> LocateRegistry.getRegistry("127.0.0.1", 1099)).thenReturn(registry);
            assertEquals(55.0, inv.invokeComputeSaturationScore(2, 3, 0.4));
        }
    }

    @Test
    void invokePing_returnsRemoteMessage() throws Exception {
        ScoringRemote remote = mock(ScoringRemote.class);
        when(remote.ping()).thenReturn("pong");
        Registry registry = mock(Registry.class);
        when(registry.lookup("S")).thenReturn(remote);
        RmiSaturationInvoker inv = new RmiSaturationInvoker();
        ReflectionTestUtils.setField(inv, "host", "h");
        ReflectionTestUtils.setField(inv, "port", 1);
        ReflectionTestUtils.setField(inv, "serviceName", "S");
        try (MockedStatic<LocateRegistry> reg = Mockito.mockStatic(LocateRegistry.class)) {
            reg.when(() -> LocateRegistry.getRegistry("h", 1)).thenReturn(registry);
            assertEquals("pong", inv.invokePing());
        }
    }

    @Test
    void resetStub_allowsReLookup() throws Exception {
        ScoringRemote remote1 = mock(ScoringRemote.class);
        when(remote1.computeSaturationScore(0, 0, 0)).thenReturn(1.0);
        ScoringRemote remote2 = mock(ScoringRemote.class);
        when(remote2.computeSaturationScore(0, 0, 0)).thenReturn(2.0);
        Registry registry = mock(Registry.class);
        when(registry.lookup("S")).thenReturn(remote1, remote2);
        RmiSaturationInvoker inv = new RmiSaturationInvoker();
        ReflectionTestUtils.setField(inv, "host", "h");
        ReflectionTestUtils.setField(inv, "port", 1);
        ReflectionTestUtils.setField(inv, "serviceName", "S");
        try (MockedStatic<LocateRegistry> reg = Mockito.mockStatic(LocateRegistry.class)) {
            reg.when(() -> LocateRegistry.getRegistry("h", 1)).thenReturn(registry);
            assertEquals(1.0, inv.invokeComputeSaturationScore(0, 0, 0));
            inv.resetStub();
            assertEquals(2.0, inv.invokeComputeSaturationScore(0, 0, 0));
        }
    }
}
