package comun;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Clase para representar una transacción de carga de combustible.
 * Debe ser Serializable para ser enviada por sockets.*/

public class Transaccion implements Serializable {
    private static final long serialVersionUID = 1L;
    //contenido de una transaccion...
    private String id; // ID de transacción
    private String idCliente;
    private String idDistribuidor;
    private TipoCombustible tipoCombustible;
    private double litros;
    private double precioLitro;
    private double montoTotal;
    private LocalDateTime fechaHora;

    //CONSTRUCTOR
    public Transaccion(String idSurtidor, String idDistribuidor, TipoCombustible tipoCombustible,
                       double litros, double precioLitro) {
        this.id = UUID.randomUUID().toString();
        this.idCliente = idSurtidor;
        this.idDistribuidor = idDistribuidor;
        this.tipoCombustible = tipoCombustible;
        this.litros = litros;
        this.precioLitro = precioLitro;
        this.montoTotal = litros * precioLitro;
        this.fechaHora = LocalDateTime.now();
    }

    //GETTERS
    public String getId() { return id; }
    public String getIdCliente() { return idCliente; }
    public String getIdDistribuidor() { return idDistribuidor; }
    public TipoCombustible getTipoCombustible() { return tipoCombustible; }
    public double getLitros() { return litros; }
    public double getPrecioLitro() { return precioLitro; }
    public double getMontoTotal() { return montoTotal; }
    public LocalDateTime getFechaHora() { return fechaHora; }

    @Override
    public String toString() {
        return "Transaccion{" +
                "id='" + id + '\'' +
                ", surtidor='" + idCliente + '\'' +
                ", tipo=" + tipoCombustible.getNombre() +
                ", litros=" + String.format("%.2f", litros) +
                ", total=$" + String.format("%.2f", montoTotal) +
                '}';
    }

    //métodos para la persistencia (simulando Base de Datos)
    public String toCSV() {
        return String.join(";",
                id,
                idCliente,
                idDistribuidor,
                tipoCombustible.name(),
                String.valueOf(litros),
                String.valueOf(precioLitro),
                String.valueOf(montoTotal),
                fechaHora.toString());
    }
}