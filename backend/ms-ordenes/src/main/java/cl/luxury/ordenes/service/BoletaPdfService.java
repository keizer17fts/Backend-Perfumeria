package cl.luxury.ordenes.service;

import cl.luxury.ordenes.model.Orden;
import cl.luxury.ordenes.model.OrdenItem;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class BoletaPdfService {
    private static final Locale CHILE = Locale.forLanguageTag("es-CL");

    public byte[] generar(Orden orden) throws IOException {
        try (PDDocument documento = new PDDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            PDPage pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);
            float y = 790;
            try (PDPageContentStream contenido = new PDPageContentStream(documento, pagina)) {
                y = linea(contenido, "LUXURY PERFUMERIA", 18, y);
                y = linea(contenido, "Comprobante de compra - Pedido #" + orden.getId(), 12, y - 7);
                y = linea(contenido, "Fecha: " + DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                        .withZone(ZoneId.of("America/Santiago")).format(orden.getFecha()), 10, y);
                y = linea(contenido, "Cliente: " + orden.getCliente().getNombre() + " " + orden.getCliente().getApellidos(), 10, y);
                y = linea(contenido, "Correo: " + orden.getCliente().getCorreo(), 10, y);
                y -= 13;
                y = linea(contenido, "DETALLE", 12, y);
                for (OrdenItem item : orden.getItems()) {
                    String texto = item.getCantidad() + " x " + item.getNombre() + " - " + pesos(item.getSubtotal());
                    y = linea(contenido, texto, 10, y);
                    if (y < 120) {
                        y = linea(contenido, "(Continua en detalle de la orden web)", 9, y);
                        break;
                    }
                }
                y -= 12;
                y = linea(contenido, "Subtotal: " + pesos(orden.getSubtotal()), 11, y);
                y = linea(contenido, "Descuento: " + pesos(orden.getDescuento()), 11, y);
                linea(contenido, "TOTAL PAGADO: " + pesos(orden.getTotal()), 14, y - 4);
            }
            documento.save(salida);
            return salida.toByteArray();
        }
    }

    private String pesos(int monto) { return NumberFormat.getCurrencyInstance(CHILE).format(monto); }

    private float linea(PDPageContentStream contenido, String texto, int tamaño, float y) throws IOException {
        // Helvetica estándar usa WinAnsi: reemplazar glifos incompatibles en vez de romper el PDF.
        String limpio = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("[^\\x20-\\x7E]", "?");
        if (limpio.length() > 94) limpio = limpio.substring(0, 91) + "...";
        contenido.beginText();
        contenido.setFont(tamaño >= 14 ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA, tamaño);
        contenido.newLineAtOffset(45, y);
        contenido.showText(limpio);
        contenido.endText();
        return y - tamaño - 9;
    }
}
