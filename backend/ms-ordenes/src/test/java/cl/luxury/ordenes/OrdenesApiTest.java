package cl.luxury.ordenes;

import cl.luxury.ordenes.repository.MensajeRepository;
import cl.luxury.ordenes.service.InventarioClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import cl.luxury.ordenes.repository.OrdenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas de integración de ms-ordenes (MockMvc + H2 en memoria; no necesita MySQL). */
@SpringBootTest
@AutoConfigureMockMvc
class OrdenesApiTest {

    @MockBean InventarioClient inventario;
    @Autowired MockMvc mvc;
    @Autowired OrdenRepository ordenes;
    @Autowired MensajeRepository mensajes;

    @BeforeEach
    void limpiar() {
        ordenes.deleteAll();
        mensajes.deleteAll();
        when(inventario.consultarProducto(anyString())).thenAnswer(inv ->
                new InventarioClient.ProductoActual(inv.getArgument(0), "LUX001", "Turathi Blue",
                        "Afnan", "/img/a.jpg", 10000, 50));
    }

    /**
     * Orden de 2 x $10.000 = $20.000. Parámetros: usuarioId (o null = invitado), fecha, estado, total, descuento.
     * El cuerpo imita lo que envía Checkout.jsx (cupon null, departamento vacío, etc.).
     */
    private String orden(String usuarioId, String fecha, String estado, int total, int descuento) {
        String usuario = usuarioId == null ? "" : "\"usuarioId\": \"" + usuarioId + "\",";
        return """
                {
                  "fecha": "%s",
                  "estado": "%s",
                  %s
                  "cliente": {"nombre": "Camila", "apellidos": "Soto", "correo": "Cliente@Gmail.com"},
                  "envio": {"calle": "Av. Perú 456", "departamento": "", "region": "Valparaíso", "comuna": "Viña del Mar", "indicaciones": ""},
                  "items": [
                    {"productoId": "1", "codigo": "LUX001", "nombre": "Turathi Blue", "marca": "Afnan", "imagen": "/img/a.jpg",
                     "precio": 10000, "cantidad": 2, "subtotal": 20000}
                  ],
                  "subtotal": 20000, "cupon": null, "descuento": %d, "total": %d,
                  "pago": {"metodo": "Tarjeta", "tarjetaFinal": "4242"}
                }
                """.formatted(fecha, estado, usuario, descuento, total);
    }

    private String ordenPagada(String usuarioId, String fecha) {
        return orden(usuarioId, fecha, "Pagado", 20000, 0);
    }

    private void crear(String cuerpo) throws Exception {
        mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated());
    }

    @Test
    void rechazaPrecioDesactualizadoAntesDeRegistrarOrden() throws Exception {
        when(inventario.consultarProducto("1")).thenReturn(
                new InventarioClient.ProductoActual("1", "LUX001", "Turathi Blue",
                        "Afnan", "/img/a.jpg", 12000, 50));
        mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON)
                        .content(ordenPagada(null, "2026-10-05T22:40:00.000Z")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("cambió")));
        assertEquals(0, ordenes.count());
    }

    // ------------------------------------------------------------ crear

    @Test
    void crearDevuelve201ConIdYLaMismaFormaQueElMock() throws Exception {
        mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON)
                        .content(ordenPagada("20987654K", "2026-10-05T22:40:00.000Z")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.estado").value("Pagado"))
                .andExpect(jsonPath("$.fecha").value("2026-10-05T22:40:00Z"))
                .andExpect(jsonPath("$.usuarioId").value("20987654K"))
                .andExpect(jsonPath("$.cliente.correo").value("cliente@gmail.com"))
                .andExpect(jsonPath("$.envio.comuna").value("Viña del Mar"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productoId").value("1"))
                .andExpect(jsonPath("$.items[0].subtotal").value(20000))
                .andExpect(jsonPath("$.items[0].id").doesNotExist())
                .andExpect(jsonPath("$.subtotal").value(20000))
                .andExpect(jsonPath("$.total").value(20000))
                .andExpect(jsonPath("$.cupon").doesNotExist())
                .andExpect(jsonPath("$.pago.tarjetaFinal").value("4242"));
    }

    @Test
    void unaCompraComoInvitadoNoTraeUsuarioId() throws Exception {
        mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON)
                        .content(ordenPagada(null, "2026-10-05T22:40:00.000Z")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").doesNotExist());
    }

    @Test
    void elRunSeNormalizaAlGuardar() throws Exception {
        mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON)
                        .content(ordenPagada("20.987.654-k", "2026-10-05T22:40:00.000Z")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value("20987654K"));
    }

    @Test
    void rechazaUnaOrdenSinProductos() throws Exception {
        String sinItems = ordenPagada(null, "2026-10-05T22:40:00.000Z")
                .replaceAll("(?s)\"items\": \\[.*?\\],\\s*\"subtotal\"", "\"items\": [], \"subtotal\"");
        mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON).content(sinItems))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rechazaUnEstadoInvalido() throws Exception {
        mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON)
                        .content(orden(null, "2026-10-05T22:40:00.000Z", "Entregado", 20000, 0)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaUnSubtotalQueNoCoincideConLosProductos() throws Exception {
        String malo = ordenPagada(null, "2026-10-05T22:40:00.000Z").replace("\"subtotal\": 20000, \"cupon\"", "\"subtotal\": 99999, \"cupon\"");
        mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON).content(malo))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaUnTotalQueNoEsSubtotalMenosDescuentoEnUnaOrdenPagada() throws Exception {
        mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON)
                        .content(orden(null, "2026-10-05T22:40:00.000Z", "Pagado", 15000, 0)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aceptaUnDescuentoSiElTotalLoConsidera() throws Exception {
        when(inventario.porcentajeCupon("LUXURY10")).thenReturn(10);
        mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON)
                        .content(orden(null, "2026-10-05T22:40:00.000Z", "Pagado", 18000, 2000).replace("\"cupon\": null", "\"cupon\": \"LUXURY10\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descuento").value(2000))
                .andExpect(jsonPath("$.total").value(18000));
    }

    @Test
    void rechazaUnDescuentoMayorAlSubtotal() throws Exception {
        mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON)
                        .content(orden(null, "2026-10-05T22:40:00.000Z", "Pagado", 0, 30000)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unPagoFallidoPuedeQuedarConTotalCero() throws Exception {
        mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON)
                        .content(orden(null, "2026-10-05T22:40:00.000Z", "Pago fallido", 0, 0)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("Pago fallido"))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void rechazaUnaTarjetaFinalQueNoSonCuatroDigitos() throws Exception {
        String malo = ordenPagada(null, "2026-10-05T22:40:00.000Z").replace("\"tarjetaFinal\": \"4242\"", "\"tarjetaFinal\": \"4242424242424242\"");
        mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON).content(malo))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaJsonMalFormado() throws Exception {
        mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON).content("{ no es json"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------ leer

    @Test
    void obtenerDevuelveLaOrdenYUnIdInexistenteDa404() throws Exception {
        crear(ordenPagada("20987654K", "2026-10-05T22:40:00.000Z"));
        Long id = ordenes.findAll().get(0).getId();

        mvc.perform(get("/api/ordenes/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        mvc.perform(get("/api/ordenes/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void listarFiltraPorUsuarioYOrdenaPorFechaDescendente() throws Exception {
        crear(ordenPagada("20987654K", "2026-09-12T15:30:00.000Z"));
        crear(ordenPagada("20987654K", "2026-09-28T19:05:00.000Z"));
        crear(ordenPagada("152345674", "2026-10-01T10:00:00.000Z"));
        crear(ordenPagada(null, "2026-10-05T22:40:00.000Z"));

        mvc.perform(get("/api/ordenes").param("usuarioId", "20987654K").param("_sort", "fecha").param("_order", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].fecha").value("2026-09-28T19:05:00Z"))
                .andExpect(jsonPath("$[1].fecha").value("2026-09-12T15:30:00Z"));

        mvc.perform(get("/api/ordenes").param("_sort", "fecha").param("_order", "desc"))
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].fecha").value("2026-10-05T22:40:00Z"));
    }

    @Test
    void listarFiltraPorEstado() throws Exception {
        crear(ordenPagada(null, "2026-10-01T10:00:00.000Z"));
        crear(orden(null, "2026-10-02T10:00:00.000Z", "Pago fallido", 0, 0));

        mvc.perform(get("/api/ordenes").param("estado", "pagado"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].estado").value("Pagado"));
    }

    @Test
    void ordenarPorUnCampoInvalidoDa400() throws Exception {
        mvc.perform(get("/api/ordenes").param("_sort", "pago_metodo"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/ordenes").param("_order", "arriba"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------ modificar

    @Test
    void patchDeEstadoAPagadoRecalculaElTotalDeUnPagoFallido() throws Exception {
        crear(orden(null, "2026-10-05T22:40:00.000Z", "Pago fallido", 0, 0));
        Long id = ordenes.findAll().get(0).getId();

        mvc.perform(patch("/api/ordenes/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"Pagado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("Pagado"))
                .andExpect(jsonPath("$.total").value(20000));
    }

    @Test
    void patchRechazaEstadosInvalidosYCuerposSinEstado() throws Exception {
        crear(ordenPagada(null, "2026-10-05T22:40:00.000Z"));
        Long id = ordenes.findAll().get(0).getId();

        mvc.perform(patch("/api/ordenes/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"Entregado\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/ordenes/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"total\":1}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/ordenes/999999").contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"Pagado\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void putReemplazaLaOrdenCompletaConservandoElId() throws Exception {
        crear(orden(null, "2026-10-05T22:40:00.000Z", "Pendiente", 20000, 0));
        Long id = ordenes.findAll().get(0).getId();

        mvc.perform(put("/api/ordenes/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(orden("20987654K", "2026-10-06T10:00:00.000Z", "Pendiente", 20000, 0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.estado").value("Pendiente"))
                .andExpect(jsonPath("$.usuarioId").value("20987654K"))
                .andExpect(jsonPath("$.items.length()").value(1));
        assertEquals(1, ordenes.count());
    }

    @Test
    void soloPermiteTransicionesValidasDesdePendienteYFallido() throws Exception {
        crear(orden(null, "2026-10-05T22:40:00.000Z", "Pendiente", 20000, 0));
        Long id = ordenes.findAll().get(0).getId();
        for (String estado : new String[]{"En preparación", "Enviado", "Entregado"}) {
            mvc.perform(patch("/api/ordenes/" + id).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"estado\":\"" + estado + "\"}"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(patch("/api/ordenes/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"Pago fallido\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/ordenes/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"Enviado\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/ordenes/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"Pagado\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/ordenes/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"Enviado\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/ordenes/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"En preparación\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void putNoPuedeSaltarDesdePendienteDirectamenteAPagadoDespachado() throws Exception {
        crear(orden(null, "2026-10-05T22:40:00.000Z", "Pendiente", 20000, 0));
        Long id = ordenes.findAll().get(0).getId();
        mvc.perform(put("/api/ordenes/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(orden(null, "2026-10-06T10:00:00.000Z", "Enviado", 20000, 0)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/ordenes/" + id))
                .andExpect(jsonPath("$.estado").value("Pendiente"));
    }

    // ------------------------------------------------------------ eliminar

    @Test
    void eliminarDevuelve204YLuegoLaOrdenYaNoExiste() throws Exception {
        crear(orden(null, "2026-10-05T22:40:00.000Z", "Pendiente", 20000, 0));
        Long id = ordenes.findAll().get(0).getId();

        mvc.perform(delete("/api/ordenes/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/ordenes/" + id)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/ordenes/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void noSePuedeReemplazarNiEliminarUnaOrdenPagada() throws Exception {
        crear(ordenPagada(null, "2026-10-05T22:40:00.000Z"));
        Long id = ordenes.findAll().get(0).getId();

        mvc.perform(put("/api/ordenes/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(orden(null, "2026-10-06T10:00:00.000Z", "Pendiente", 20000, 0)))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/ordenes/" + id))
                .andExpect(status().isBadRequest());
        assertEquals(1, ordenes.count());
    }

    // ------------------------------------------------------------ mensajes y salud

    private static final String MENSAJE = """
            {"nombre": "Ana", "correo": "Ana@Gmail.com", "asunto": "Consulta", "mensaje": "Hola, quiero saber el stock.",
             "fecha": "2026-10-05T22:40:00.000Z", "estado": "Nuevo"}
            """;

    @Test
    void elFormularioDeContactoGuardaElMensaje() throws Exception {
        mvc.perform(post("/api/mensajes").contentType(MediaType.APPLICATION_JSON).content(MENSAJE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.correo").value("ana@gmail.com"))
                .andExpect(jsonPath("$.estado").value("Nuevo"));

        mvc.perform(get("/api/mensajes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void rechazaMensajesCortosOConCorreoInvalido() throws Exception {
        mvc.perform(post("/api/mensajes").contentType(MediaType.APPLICATION_JSON)
                        .content(MENSAJE.replace("Hola, quiero saber el stock.", "Hola")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/mensajes").contentType(MediaType.APPLICATION_JSON)
                        .content(MENSAJE.replace("Ana@Gmail.com", "no-es-correo")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sePuedeMarcarUnMensajeComoLeidoYBorrarlo() throws Exception {
        mvc.perform(post("/api/mensajes").contentType(MediaType.APPLICATION_JSON).content(MENSAJE))
                .andExpect(status().isCreated());
        Long id = mensajes.findAll().get(0).getId();

        mvc.perform(patch("/api/mensajes/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"Leído\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("Leído"));
        mvc.perform(delete("/api/mensajes/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/mensajes/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void saludResponde() throws Exception {
        mvc.perform(get("/api/ordenes/salud"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servicio").value("ms-ordenes"))
                .andExpect(jsonPath("$.estado").value("OK"));
    }
}
