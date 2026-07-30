package com.banco.bancobienestar.service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import com.banco.bancobienestar.entity.CuentaEntity;
import com.banco.bancobienestar.entity.MovimientosEntity;
import com.banco.bancobienestar.entity.SolicitudCreditoEntity;
import com.banco.bancobienestar.entity.UsuarioEntity;

@Service
public class ComprobantePdfService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Font TITULO = new Font(Font.HELVETICA, 18, Font.BOLD);
    private static final Font SUBTITULO = new Font(Font.HELVETICA, 12, Font.BOLD);
    private static final Font TEXTO = new Font(Font.HELVETICA, 10);
    private static final Font TEXTO_BOLD = new Font(Font.HELVETICA, 10, Font.BOLD);

    public byte[] comprobanteMovimiento(UsuarioEntity usuario, CuentaEntity cuenta, MovimientosEntity movimiento) {
        return crearPdf(document -> {
            agregarEncabezado(document, "Comprobante de movimiento");
            agregarDato(document, "Cliente", usuario.getNombre());
            agregarDato(document, "Usuario", usuario.getUsername());
            agregarDato(document, "CLABE del cliente", cuenta.getClabe());
            agregarEspacio(document);

            PdfPTable tabla = tablaDatos();
            agregarFila(tabla, "Folio", String.valueOf(movimiento.getId()));
            agregarFila(tabla, "Fecha", FECHA.format(movimiento.getFecha()));
            agregarFila(tabla, "Tipo", valor(movimiento.getTipo()));
            agregarFila(tabla, "Estado", valor(movimiento.getEstadoMovimiento()));
            agregarFila(tabla, "Descripcion", valor(movimiento.getDescripcion()));
            agregarFila(tabla, "Cuenta origen", movimiento.getCuentaOrigen());
            agregarFila(tabla, "Cuenta destino", movimiento.getCuentaDestino());
            agregarFila(tabla, "Monto", moneda(movimiento.getMonto()));
            document.add(tabla);

            agregarPie(document);
        });
    }

    public byte[] historialMovimientos(UsuarioEntity usuario, CuentaEntity cuenta, List<MovimientosEntity> movimientos) {
        return crearPdf(document -> {
            agregarEncabezado(document, "Historial completo de movimientos");
            agregarDato(document, "Cliente", usuario.getNombre());
            agregarDato(document, "Usuario", usuario.getUsername());
            agregarDato(document, "CLABE", cuenta.getClabe());
            agregarDato(document, "Saldo actual", moneda(cuenta.getSaldo()));
            agregarEspacio(document);

            PdfPTable tabla = new PdfPTable(new float[] { 1.2f, 2f, 2f, 2f, 1.4f, 1.4f });
            tabla.setWidthPercentage(100);
            agregarEncabezadoTabla(tabla, "Folio");
            agregarEncabezadoTabla(tabla, "Fecha");
            agregarEncabezadoTabla(tabla, "Descripcion");
            agregarEncabezadoTabla(tabla, "Origen");
            agregarEncabezadoTabla(tabla, "Destino");
            agregarEncabezadoTabla(tabla, "Monto");

            for (MovimientosEntity movimiento : movimientos) {
                agregarCelda(tabla, String.valueOf(movimiento.getId()));
                agregarCelda(tabla, FECHA.format(movimiento.getFecha()));
                agregarCelda(tabla, valor(movimiento.getDescripcion()));
                agregarCelda(tabla, movimiento.getCuentaOrigen());
                agregarCelda(tabla, movimiento.getCuentaDestino());
                agregarCelda(tabla, moneda(movimiento.getMonto()));
            }

            document.add(tabla);
            agregarPie(document);
        });
    }

    public byte[] comprobanteCredito(UsuarioEntity usuario, CuentaEntity cuenta, SolicitudCreditoEntity solicitud) {
        return crearPdf(document -> {
            agregarEncabezado(document, "Comprobante de solicitud de credito");
            agregarDato(document, "Cliente", usuario.getNombre());
            agregarDato(document, "Usuario", usuario.getUsername());
            agregarDato(document, "CLABE", cuenta != null ? cuenta.getClabe() : "Sin cuenta asignada");
            agregarEspacio(document);

            PdfPTable tabla = tablaDatos();
            agregarFila(tabla, "Folio", String.valueOf(solicitud.getId()));
            agregarFila(tabla, "Fecha de solicitud", FECHA.format(solicitud.getFecha()));
            agregarFila(tabla, "Monto solicitado", moneda(solicitud.getMontoSolicitado()));
            agregarFila(tabla, "Saldo pendiente", solicitud.getSaldoPendiente() != null ? moneda(solicitud.getSaldoPendiente()) : "No registrado");
            agregarFila(tabla, "Estado", valor(solicitud.getEstado()));
            document.add(tabla);
            agregarEspacio(document);

            document.add(new Paragraph("Firma digital del cliente", SUBTITULO));
            Image firma = firmaDesdeBase64(solicitud.getFirmaBase64());
            if (firma != null) {
                firma.scaleToFit(260, 90);
                firma.setAlignment(Element.ALIGN_LEFT);
                document.add(firma);
            } else {
                document.add(new Paragraph("Firma no disponible para visualizar.", TEXTO));
            }

            agregarPie(document);
        });
    }

    private byte[] crearPdf(DocumentoBuilder builder) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.LETTER, 36, 36, 42, 36);
            PdfWriter.getInstance(document, salida);
            document.open();
            builder.build(document);
            document.close();
            return salida.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo generar el comprobante PDF.", e);
        }
    }

    private void agregarEncabezado(Document document, String titulo) throws Exception {
        Paragraph banco = new Paragraph("Banco Bienestar", TITULO);
        banco.setAlignment(Element.ALIGN_CENTER);
        document.add(banco);

        Paragraph comprobante = new Paragraph(titulo, SUBTITULO);
        comprobante.setAlignment(Element.ALIGN_CENTER);
        comprobante.setSpacingAfter(18);
        document.add(comprobante);
    }

    private void agregarDato(Document document, String etiqueta, String valor) throws Exception {
        document.add(new Paragraph(etiqueta + ": " + valor(valor), TEXTO));
    }

    private void agregarEspacio(Document document) throws Exception {
        Paragraph espacio = new Paragraph(" ");
        espacio.setSpacingAfter(6);
        document.add(espacio);
    }

    private PdfPTable tablaDatos() {
        PdfPTable tabla = new PdfPTable(new float[] { 1.3f, 2.7f });
        tabla.setWidthPercentage(100);
        return tabla;
    }

    private void agregarFila(PdfPTable tabla, String etiqueta, String valor) {
        PdfPCell celdaEtiqueta = new PdfPCell(new Phrase(etiqueta, TEXTO_BOLD));
        celdaEtiqueta.setPadding(6);
        PdfPCell celdaValor = new PdfPCell(new Phrase(valor(valor), TEXTO));
        celdaValor.setPadding(6);
        tabla.addCell(celdaEtiqueta);
        tabla.addCell(celdaValor);
    }

    private void agregarEncabezadoTabla(PdfPTable tabla, String valor) {
        PdfPCell celda = new PdfPCell(new Phrase(valor, TEXTO_BOLD));
        celda.setPadding(6);
        tabla.addCell(celda);
    }

    private void agregarCelda(PdfPTable tabla, String valor) {
        PdfPCell celda = new PdfPCell(new Phrase(valor(valor), TEXTO));
        celda.setPadding(5);
        tabla.addCell(celda);
    }

    private Image firmaDesdeBase64(String firmaBase64) {
        try {
            if (firmaBase64 == null || firmaBase64.isBlank()) {
                return null;
            }
            String contenido = firmaBase64.contains(",")
                    ? firmaBase64.substring(firmaBase64.indexOf(",") + 1)
                    : firmaBase64;
            byte[] bytes = Base64.getDecoder().decode(contenido);
            return Image.getInstance(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    private void agregarPie(Document document) throws Exception {
        Paragraph pie = new Paragraph("Documento generado automaticamente desde la banca en linea.", TEXTO);
        pie.setSpacingBefore(20);
        document.add(pie);
    }

    private String moneda(Double monto) {
        if (monto == null) {
            return "$0.00 MXN";
        }
        return String.format("$%,.2f MXN", monto);
    }

    private String valor(String valor) {
        return valor == null || valor.isBlank() ? "No registrado" : valor;
    }

    @FunctionalInterface
    private interface DocumentoBuilder {
        void build(Document document) throws Exception;
    }
}
