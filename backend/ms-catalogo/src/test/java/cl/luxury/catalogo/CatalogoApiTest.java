package cl.luxury.catalogo;

import cl.luxury.catalogo.model.Categoria;
import cl.luxury.catalogo.model.Oferta;
import cl.luxury.catalogo.model.Producto;
import cl.luxury.catalogo.repository.CategoriaRepository;
import cl.luxury.catalogo.repository.OfertaRepository;
import cl.luxury.catalogo.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogoApiTest {

    @Autowired MockMvc mvc;
    @Autowired ProductoRepository productos;
    @Autowired CategoriaRepository categorias;
    @Autowired OfertaRepository ofertas;

    Producto sauvage;

    @BeforeEach
    void datosBase() {
        ofertas.deleteAll();
        productos.deleteAll();
        categorias.deleteAll();

        Categoria intensos = new Categoria();
        intensos.setId("intensos");
        intensos.setNombre("Intensos");
        categorias.save(intensos);
        Categoria moderados = new Categoria();
        moderados.setId("moderados");
        moderados.setNombre("Moderados");
        categorias.save(moderados);

        sauvage = producto("LUX010", "Sauvage Elixir", "intensos", 95000);
        productos.save(sauvage);
        productos.save(producto("LUX011", "Turathi Blue", "moderados", 28000));
    }

    private Producto producto(String codigo, String nombre, String categoria, int precio) {
        Producto p = new Producto();
        p.setCodigo(codigo);
        p.setMarca("Marca");
        p.setNombre(nombre);
        p.setPrecio(precio);
        p.setStock(10);
        p.setCategoria(categoria);
        return p;
    }

    @Test
    void listarDevuelveElIdComoTexto() throws Exception {
        mvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(String.valueOf(sauvage.getId())));
    }

    @Test
    void listarFiltraPorCategoria() throws Exception {
        mvc.perform(get("/api/productos").param("categoria", "intensos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Sauvage Elixir"));
    }

    @Test
    void crearGuardaElProductoConSusDatosAnidados() throws Exception {
        String cuerpo = """
                {"codigo":"LUX099","marca":"Afnan","nombre":"Nuevo","precio":30000,"stock":4,
                 "categoria":"moderados","humor":["fresco","elegante"],
                 "notas":{"salida":["Citricos"],"corazon":["Ambar"],"fondo":["Almizcle"]}}
                """;
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.humor[1]").value("elegante"))
                .andExpect(jsonPath("$.notas.salida[0]").value("Citricos"))
                .andExpect(jsonPath("$.stockCritico").value(2));
    }

    @Test
    void crearConCodigoRepetidoResponde409() throws Exception {
        String cuerpo = "{\"codigo\":\"LUX010\",\"marca\":\"X\",\"nombre\":\"Repetido\",\"precio\":1000}";
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict());
    }

    @Test
    void crearConPrecioNegativoResponde400() throws Exception {
        String cuerpo = "{\"codigo\":\"LUX098\",\"marca\":\"X\",\"nombre\":\"Malo\",\"precio\":-5}";
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearConCategoriaInexistenteResponde400() throws Exception {
        String cuerpo = "{\"codigo\":\"LUX097\",\"marca\":\"X\",\"nombre\":\"Sin cat\",\"precio\":1000,\"categoria\":\"nada\"}";
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchCambiaSoloLoEnviado() throws Exception {
        mvc.perform(patch("/api/productos/" + sauvage.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stock\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(3))
                .andExpect(jsonPath("$.nombre").value("Sauvage Elixir"));
    }

    @Test
    void productoInexistenteResponde404() throws Exception {
        mvc.perform(get("/api/productos/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void eliminarProductoBorraSusOfertas() throws Exception {
        Oferta oferta = new Oferta();
        oferta.setProductoId(sauvage.getId());
        oferta.setDescuento(15);
        oferta.setEtiqueta("Verano");
        oferta.setVigenteHasta(LocalDate.of(2026, 12, 31));
        ofertas.save(oferta);

        mvc.perform(delete("/api/productos/" + sauvage.getId())).andExpect(status().isNoContent());

        assertEquals(0, ofertas.count());
    }

    @Test
    void noSePuedeEliminarUnaCategoriaConProductos() throws Exception {
        mvc.perform(delete("/api/categorias/intensos")).andExpect(status().isConflict());
    }

    @Test
    void crearCategoriaGeneraElSlugDesdeElNombre() throws Exception {
        mvc.perform(post("/api/categorias").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Extra Intensos\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("extra-intensos"));
    }

    @Test
    void ofertasActivasIncluyenElProductoCuandoSePideExpand() throws Exception {
        Oferta oferta = new Oferta();
        oferta.setProductoId(sauvage.getId());
        oferta.setDescuento(20);
        oferta.setActiva(true);
        ofertas.save(oferta);

        mvc.perform(get("/api/ofertas").param("activa", "true").param("_expand", "producto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].descuento").value(20))
                .andExpect(jsonPath("$[0].productoId").value(String.valueOf(sauvage.getId())))
                .andExpect(jsonPath("$[0].producto.nombre").value("Sauvage Elixir"));
    }

    @Test
    void ofertaSobreProductoInexistenteResponde400() throws Exception {
        mvc.perform(post("/api/ofertas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\":\"999999\",\"descuento\":10}"))
                .andExpect(status().isBadRequest());
    }
}
