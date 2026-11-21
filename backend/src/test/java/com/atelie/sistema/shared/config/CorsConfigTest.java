package com.atelie.sistema.shared.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.DefaultCorsProcessor;
import static org.junit.jupiter.api.Assertions.*;

class CorsConfigTest {
    @Test
    void navegadorPodeEnviarPutDaMetaEDoPrecoMinimoComCsrf() throws Exception {
        for (String caminho : java.util.List.of("/precificacao/metas/2026-11",
                "/precificacao/politica")) {
            var request = preflight(caminho, "http://localhost:5173");
            var response = new MockHttpServletResponse();
            var source = new CorsConfig().corsConfigurationSource("http://localhost:5173");
            boolean permitido = new DefaultCorsProcessor().processRequest(
                    source.getCorsConfiguration(request), request, response);
            assertTrue(permitido);
            assertEquals(200, response.getStatus());
            assertEquals("http://localhost:5173", response.getHeader("Access-Control-Allow-Origin"));
            assertTrue(response.getHeader("Access-Control-Allow-Methods").contains("PUT"));
            assertEquals("true", response.getHeader("Access-Control-Allow-Credentials"));
            assertTrue(response.getHeader("Access-Control-Allow-Headers").toLowerCase().contains("x-csrf-token"));
        }
    }

    @Test
    void continuaRejeitandoOrigemNaoAutorizada() throws Exception {
        var request = preflight("/precificacao/metas/2026-11", "https://outra-origem.example");
        var response = new MockHttpServletResponse();
        var source = new CorsConfig().corsConfigurationSource("http://localhost:5173");
        assertFalse(new DefaultCorsProcessor().processRequest(
                source.getCorsConfiguration(request), request, response));
        assertEquals(403, response.getStatus());
        assertNull(response.getHeader("Access-Control-Allow-Origin"));
    }

    private MockHttpServletRequest preflight(String caminho, String origem) {
        var request = new MockHttpServletRequest("OPTIONS", caminho);
        request.setServerName("localhost");
        request.setServerPort(8080);
        request.addHeader("Origin", origem);
        request.addHeader("Access-Control-Request-Method", "PUT");
        request.addHeader("Access-Control-Request-Headers", "content-type,x-csrf-token");
        return request;
    }
}
