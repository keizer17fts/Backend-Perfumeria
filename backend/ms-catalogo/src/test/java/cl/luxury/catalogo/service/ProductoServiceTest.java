package cl.luxury.catalogo.service;

import cl.luxury.catalogo.exception.*;
import cl.luxury.catalogo.model.Producto;
import cl.luxury.catalogo.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.*;
import org.junit.jupiter.api.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductoServiceTest {
    ProductoRepository productos;
    CategoriaRepository categorias;
    OfertaRepository ofertas;
    AlertaStockService alertas;
    ProductoService servicio;
    ValidatorFactory factory;
    ObjectMapper mapper = new ObjectMapper();

    @BeforeEach void preparar() {
        productos = mock(ProductoRepository.class);
        categorias = mock(CategoriaRepository.class);
        ofertas = mock(OfertaRepository.class);
        alertas = mock(AlertaStockService.class);
        factory = Validation.buildDefaultValidatorFactory();
        servicio = new ProductoService(productos, categorias, ofertas, mapper, factory.getValidator(), alertas);
    }
    @AfterEach void cerrar() { factory.close(); }
    Producto producto() {
        Producto p = new Producto(); p.setCodigo("PRUEBA-1"); p.setNombre("Perfume");
        p.setMarca("Marca"); p.setPrecio(28000); p.setStock(5); return p;
    }
    @Test void rechazaCodigoDuplicadoSinGuardar() {
        when(productos.existsByCodigo("PRUEBA-1")).thenReturn(true);
        assertThrows(ConflictoException.class, () -> servicio.crear(producto()));
        verify(productos, never()).save(any());
    }
    @Test void rechazaCategoriaInexistente() {
        Producto p = producto(); p.setCategoria("no-existe");
        assertThrows(SolicitudInvalidaException.class, () -> servicio.crear(p));
        verify(productos, never()).save(any());
    }
    @Test void informaProductoInexistente() {
        assertThrows(NoEncontradoException.class, () -> servicio.obtener(99L));
    }
    @Test void rechazaStockNegativoEnModificacion() {
        when(productos.findById(1L)).thenReturn(Optional.of(producto()));
        assertThrows(ConstraintViolationException.class, () -> servicio.modificar(1L, mapper.createObjectNode().put("stock", -1)));
        verify(productos, never()).save(any());
        verifyNoInteractions(alertas);
    }
    @Test void rechazaPrecioNegativoEnModificacion() {
        when(productos.findById(1L)).thenReturn(Optional.of(producto()));
        assertThrows(ConstraintViolationException.class, () -> servicio.modificar(1L, mapper.createObjectNode().put("precio", -1)));
        verify(productos, never()).save(any());
    }
    @Test void avisaCuandoUnProductoAgotadoSeRepone() {
        Producto anterior = producto(); anterior.setStock(0);
        when(productos.findById(1L)).thenReturn(Optional.of(anterior));
        when(productos.save(any())).thenAnswer(i -> i.getArgument(0));
        Producto resultado = servicio.actualizar(1L, producto());
        assertEquals(1L, resultado.getId()); assertEquals(5, resultado.getStock());
        verify(alertas).avisarReposicion(1L);
    }
    @Test void noAvisaSiYaHabiaStock() {
        when(productos.findById(1L)).thenReturn(Optional.of(producto()));
        when(productos.save(any())).thenAnswer(i -> i.getArgument(0));
        servicio.actualizar(1L, producto()); verifyNoInteractions(alertas);
    }
}
