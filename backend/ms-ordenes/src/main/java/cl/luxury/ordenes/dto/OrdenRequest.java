package cl.luxury.ordenes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * Cuerpo de POST/PUT /api/ordenes. Tiene la misma forma que la orden que arma Checkout.jsx.
 * Los importes son enteros en CLP. El servicio vuelve a calcular los subtotales (no confía en el cliente).
 */
public record OrdenRequest(
        Instant fecha,
        @NotBlank @Pattern(regexp = "Pendiente|Pagado|Pago fallido", message = "debe ser Pendiente, Pagado o Pago fallido")
        String estado,
        @Size(max = 15) String usuarioId,   // se normaliza (sin puntos ni guion) y luego debe tener 9 caracteres como máximo
        @NotNull @Valid ClienteDto cliente,
        @NotNull @Valid EnvioDto envio,
        @NotEmpty @Size(max = 30) @Valid List<ItemDto> items,
        @Min(0) Integer subtotal,
        @Size(max = 30) String cupon,
        @Min(0) Integer descuento,
        @Min(0) Integer total,
        @NotNull @Valid PagoDto pago) {

    public record ClienteDto(
            @NotBlank @Size(max = 50) String nombre,
            @NotBlank @Size(max = 100) String apellidos,
            @NotBlank @Email @Size(max = 100) String correo) {
    }

    public record EnvioDto(
            @NotBlank @Size(max = 200) String calle,
            @Size(max = 50) String departamento,
            @NotBlank @Size(max = 100) String region,
            @NotBlank @Size(max = 100) String comuna,
            @Size(max = 500) String indicaciones) {
    }

    public record ItemDto(
            @NotBlank @Size(max = 20) String productoId,
            @Size(max = 50) String codigo,
            @NotBlank @Size(max = 150) String nombre,
            @Size(max = 100) String marca,
            @Size(max = 300) String imagen,
            @NotNull @Min(0) @Max(1_000_000) Integer precio,
            @NotNull @Min(1) @Max(99) Integer cantidad,
            Integer subtotal) {
    }

    public record PagoDto(
            @NotBlank @Size(max = 30) String metodo,
            @NotBlank @Pattern(regexp = "\\d{4}", message = "deben ser los 4 últimos dígitos de la tarjeta") String tarjetaFinal) {
    }
}
