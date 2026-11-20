package cl.luxury.ordenes.service;

import cl.luxury.ordenes.dto.OrdenRequest;
import cl.luxury.ordenes.exception.SolicitudInvalidaException;
import cl.luxury.ordenes.model.Orden;
import cl.luxury.ordenes.repository.OrdenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.context.ApplicationEventPublisher;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrdenServiceTest {
    OrdenRepository repo; InventarioClient inventario; ApplicationEventPublisher eventos; OrdenService servicio;
    @BeforeEach void preparar() {
        repo=mock(OrdenRepository.class);inventario=mock(InventarioClient.class);eventos=mock(ApplicationEventPublisher.class);
        servicio=new OrdenService(repo,eventos,inventario);
    }
    OrdenRequest pedido(int precio,int cantidad,Integer total,String cupon,int descuento) {
        return new OrdenRequest(null,"Pendiente",null,
            new OrdenRequest.ClienteDto("Ana","Perez","ana@example.com"),
            new OrdenRequest.EnvioDto("Calle 123","","Metropolitana","Santiago",""),
            List.of(new OrdenRequest.ItemDto("1","P1","Perfume","Marca",null,precio,cantidad,null)),
            null,cupon,descuento,total,new OrdenRequest.PagoDto("Tarjeta","1234"));
    }
    void catalogo(int precio,int stock) {
        when(inventario.consultarProducto("1")).thenReturn(new InventarioClient.ProductoActual("1","P1","Perfume","Marca",null,precio,stock));
    }
    @Test void calculaSubtotalDesdePrecioYCantidad() {
        catalogo(28000,5);when(repo.saveAndFlush(any())).thenAnswer(i->i.getArgument(0));
        Orden o=servicio.crear(pedido(28000,2,null,null,0));
        assertEquals(56000,o.getSubtotal());assertEquals(56000,o.getTotal());assertEquals(56000,o.getItems().get(0).getSubtotal());
        verify(inventario,never()).descontar(any());verifyNoInteractions(eventos);
    }
    @Test void rechazaTotalAlterado() {
        catalogo(28000,5);assertThrows(SolicitudInvalidaException.class,()->servicio.crear(pedido(28000,2,1,null,0)));
        verify(repo,never()).saveAndFlush(any());
    }
    @Test void rechazaStockInsuficiente() {
        catalogo(28000,1);assertThrows(SolicitudInvalidaException.class,()->servicio.crear(pedido(28000,2,null,null,0)));
        verify(repo,never()).saveAndFlush(any());verify(inventario,never()).descontar(any());
    }
    @Test void rechazaPrecioDesactualizado() {
        catalogo(30000,5);assertThrows(SolicitudInvalidaException.class,()->servicio.crear(pedido(28000,1,null,null,0)));
        verify(repo,never()).saveAndFlush(any());
    }
    @Test void rechazaDescuentoSinCupon() {
        catalogo(28000,5);assertThrows(SolicitudInvalidaException.class,()->servicio.crear(pedido(28000,1,null,null,1000)));
        verify(repo,never()).saveAndFlush(any());
    }
    @Test void aceptaDescuentoCalculadoPorCatalogo() {
        catalogo(28000,5);when(inventario.porcentajeCupon("LUXURY10")).thenReturn(10);
        when(repo.saveAndFlush(any())).thenAnswer(i->i.getArgument(0));
        Orden o=servicio.crear(pedido(28000,2,50400,"LUXURY10",5600));assertEquals(50400,o.getTotal());
    }
    @Test void impideSaltarDePagadoAEntregado() {
        Orden o=new Orden();o.setEstado("Pagado");when(repo.findById(7L)).thenReturn(Optional.of(o));
        assertThrows(SolicitudInvalidaException.class,()->servicio.modificar(7L,new ObjectMapper().createObjectNode().put("estado","Entregado")));
        verify(repo,never()).save(any());verifyNoInteractions(eventos);
    }
    @Test void noEliminaUnaCompraPagada() {
        Orden o=new Orden();o.setEstado("Pagado");when(repo.findById(7L)).thenReturn(Optional.of(o));
        assertThrows(SolicitudInvalidaException.class,()->servicio.eliminar(7L));verify(repo,never()).delete(any(Orden.class));
    }
}
