package comun;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Clase que representa un mensaje en el protocolo de comunicación
 * Serializable para poder enviarse a través de sockets TCP
 */
public class Mensaje implements Serializable {
    private static final long serialVersionUID = 1L;

    // TIPOS DE MENSAJE...
    public enum Tipo {
        // Nivel 3 -> Nivel 2
        ACTUALIZAR_PRECIO_BASE,
        SOLICITAR_REPORTE,

        // Nivel 2 -> Nivel 3
        ENVIAR_REPORTE,
        CONFIRMACION_PRECIO,
        SINCRONIZAR_TRANSACCIONES,

        // Nivel 2 -> Nivel 1
        CONSULTAR_ESTADO,
        ACTUALIZAR_PRECIO_CLIENTE,

        // Nivel 1 -> Nivel 2
        ESTADO_CLIENTE,
        REGISTRAR_TRANSACCION,

        // Generales
        ACK,
        ERROR,
        PING,
        RECONEXION
    }

    private Tipo tipo; //de mensaje
    private String idOrigen;
    private String idDestino;
    private Map<String, Object> datos;
    private long timestamp;

    //CONSTRUCTOR
    public Mensaje(Tipo tipo, String idOrigen) {
        this.tipo = tipo;
        this.idOrigen = idOrigen;
        this.datos = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    //métodos para agregar datos al mensaje
    public void agregarDato(String clave, Object valor) {
        datos.put(clave, valor);
    }

    public Object obtenerDato(String clave) {
        return datos.get(clave);
    }

    public Integer obtenerEntero(String clave) {
        Object valor = datos.get(clave);
        if (valor instanceof Integer) {
            return (Integer) valor;
        }
        return null;
    }

    public Double obtenerDouble(String clave) {
        Object valor = datos.get(clave);
        if (valor instanceof Double) {
            return (Double) valor;
        } else if (valor instanceof Integer) {
            return ((Integer) valor).doubleValue();
        }
        return null;
    }

    public String obtenerString(String clave) {
        Object valor = datos.get(clave);
        return valor != null ? valor.toString() : null;
    }

    public Boolean obtenerBoolean(String clave) {
        Object valor = datos.get(clave);
        if (valor instanceof Boolean) {
            return (Boolean) valor;
        }
        return null;
    }

    // Getters y Setters
    public Tipo getTipo() {
        return tipo;
    }

    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
    }

    public String getIdOrigen() {
        return idOrigen;
    }

    public void setIdOrigen(String idOrigen) {
        this.idOrigen = idOrigen;
    }

    public String getIdDestino() {
        return idDestino;
    }

    public void setIdDestino(String idDestino) {
        this.idDestino = idDestino;
    }

    public Map<String, Object> getDatos() {
        return datos;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Mensaje{" +
                "tipo=" + tipo +
                ", origen='" + idOrigen + '\'' +
                ", destino='" + idDestino + '\'' +
                ", datos=" + datos +
                ", timestamp=" + timestamp +
                '}';
    }
}