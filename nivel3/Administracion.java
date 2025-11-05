package nivel3;

import comun.Mensaje;
import comun.Transaccion;
import comun.TipoCombustible;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/***********************************************************************************************************************
 * Clase Administración - Nivel 3
 * Servidor central que gestiona todos los distribuidores
 **********************************************************************************************************************/
public class Administracion {
    private final String id; //de administracion
    private int puerto;

    private ServerSocket serverSocket;
    private Map<String, ManejadorDistribuidor> distribuidoresConectados;
    private AtomicBoolean servidorActivo;

    private Map<TipoCombustible, Double> preciosBase;
    private List<Transaccion> historialCompleto;

    //CONSTRUCTOR
    public Administracion(String id, int puerto) {
        this.id = id;
        this.puerto = puerto;

        this.distribuidoresConectados = new ConcurrentHashMap<>();
        this.servidorActivo = new AtomicBoolean(false);
        this.historialCompleto = new ArrayList<>();

        inicializarPreciosBase();
    }

    /**
     * inicializa precios base por defecto */
    private void inicializarPreciosBase() {
        preciosBase = new HashMap<>();
        preciosBase.put(TipoCombustible.GASOLINA_93, 1000.0);
        preciosBase.put(TipoCombustible.GASOLINA_95, 1100.0);
        preciosBase.put(TipoCombustible.GASOLINA_97, 1200.0);
        preciosBase.put(TipoCombustible.DIESEL, 900.0);
        preciosBase.put(TipoCombustible.KEROSENE, 800.0);
    }

    /**
     * inicia el servidor de administración */
    public void iniciar() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(puerto);
                servidorActivo.set(true);
                System.out.println("[" + id + "] Administración iniciada en puerto " + puerto);
                System.out.println("[" + id + "] Esperando conexión de distribuidores...");

                while (servidorActivo.get()) {
                    Socket clienteSocket = serverSocket.accept();
                    ManejadorDistribuidor manejador = new ManejadorDistribuidor(clienteSocket);
                    new Thread(manejador).start();
                }
            } catch (IOException e) {
                if (servidorActivo.get()) {
                    System.err.println("[" + id + "] Error en servidor: " + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * Actualiza los precios base y los propaga a todos los distribuidores
     */
    public void actualizarPreciosBase(Map<TipoCombustible, Double> nuevosPrecios) {
        System.out.println("[" + id + "] Actualizando precios base...");

        for (Map.Entry<TipoCombustible, Double> entry : nuevosPrecios.entrySet()) {
            preciosBase.put(entry.getKey(), entry.getValue());
            System.out.println("[" + id + "] " + entry.getKey().getNombre() +
                    " -> $" + entry.getValue());
        }

        //crear mensaje para distribuidores
        Mensaje mensaje = new Mensaje(Mensaje.Tipo.ACTUALIZAR_PRECIO_BASE, id);
        Map<String, Double> preciosStr = new HashMap<>();

        for (Map.Entry<TipoCombustible, Double> entry : preciosBase.entrySet()) {
            preciosStr.put(entry.getKey().name(), entry.getValue());
        }

        mensaje.agregarDato("precios", preciosStr);

        //enviar a todos los distribuidores conectados
        int enviados = 0;
        for (ManejadorDistribuidor manejador : distribuidoresConectados.values()) {
            if (manejador.enviarMensaje(mensaje)) {
                enviados++;
            }
        }

        System.out.println("[" + id + "] Precios enviados a " + enviados + " distribuidores");
    }

    /**
     * Solicita reportes a todos los distribuidores */
    public void solicitarReportes() {
        System.out.println("[" + id + "] Solicitando reportes a distribuidores...");

        Mensaje mensaje = new Mensaje(Mensaje.Tipo.SOLICITAR_REPORTE, id);

        for (ManejadorDistribuidor manejador : distribuidoresConectados.values()) {
            manejador.enviarMensaje(mensaje);
        }
    }

    /**
     * genera un reporte consolidado de todos los distribuidores */
    public String generarReporteConsolidado() {
        StringBuilder reporte = new StringBuilder();
        reporte.append("=== REPORTE CONSOLIDADO ===\n");
        reporte.append("Fecha: ").append(new Date()).append("\n\n");

        double totalVentasGlobal = 0;
        int totalTransacciones = 0;
        Map<TipoCombustible, Double> ventasPorTipo = new HashMap<>();

        for (TipoCombustible tipo : TipoCombustible.values()) {
            ventasPorTipo.put(tipo, 0.0);
        }

        // procesar cada distribuidor
        reporte.append("Distribuidores Conectados: ").append(distribuidoresConectados.size()).append("\n\n");

        for (ManejadorDistribuidor manejador : distribuidoresConectados.values()) {
            String idDist = manejador.getIdDistribuidor();
            List<Transaccion> transacciones = manejador.getTransacciones();

            double totalDist = transacciones.stream()
                    .mapToDouble(Transaccion::getMontoTotal)
                    .sum();

            reporte.append("Distribuidor: ").append(idDist).append("\n");
            reporte.append("  Transacciones: ").append(transacciones.size()).append("\n");
            reporte.append("  Total Ventas: $").append(String.format("%.2f", totalDist)).append("\n\n");

            totalVentasGlobal += totalDist;
            totalTransacciones += transacciones.size();

            //agrupar por tipo de combustible
            for (Transaccion t : transacciones) {
                double actual = ventasPorTipo.get(t.getTipoCombustible());
                ventasPorTipo.put(t.getTipoCombustible(), actual + t.getMontoTotal());
            }
        }

        reporte.append("=== RESUMEN GLOBAL ===\n");
        reporte.append("Total Transacciones: ").append(totalTransacciones).append("\n");
        reporte.append("Total Ventas: $").append(String.format("%.2f", totalVentasGlobal)).append("\n\n");

        reporte.append("Ventas por Tipo de Combustible:\n");
        for (Map.Entry<TipoCombustible, Double> entry : ventasPorTipo.entrySet()) {
            if (entry.getValue() > 0) {
                reporte.append("  ").append(entry.getKey().getNombre())
                        .append(": $").append(String.format("%.2f", entry.getValue())).append("\n");
            }
        }

        return reporte.toString();
    }

    /**
     * detiene el servidor de administración */
    public void detener() {
        servidorActivo.set(false);

        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            System.out.println("[" + id + "] Administración detenida");
        } catch (IOException e) {
            System.err.println("[" + id + "] Error al detener: " + e.getMessage());
        }
    }

    //GETTERS
    public Map<TipoCombustible, Double> getPreciosBase() {
        return new HashMap<>(preciosBase);
    }

    public int getCantidadDistribuidores() {
        return distribuidoresConectados.size();
    }

    /**
     * Clase interna para manejar cada distribuidor conectado */
    private class ManejadorDistribuidor implements Runnable {
        private Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private String idDistribuidor;
        private AtomicBoolean activo;
        private List<Transaccion> transacciones;

        public ManejadorDistribuidor(Socket socket) {
            this.socket = socket;
            this.activo = new AtomicBoolean(true);
            this.transacciones = new ArrayList<>();
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                System.out.println("[" + id + "] Nuevo distribuidor conectado desde " +
                        socket.getInetAddress());

                while (activo.get()) {
                    Mensaje mensaje = (Mensaje) in.readObject();

                    if (idDistribuidor == null) {
                        idDistribuidor = mensaje.getIdOrigen();
                        distribuidoresConectados.put(idDistribuidor, this);
                        System.out.println("[" + id + "] Distribuidor registrado: " + idDistribuidor);
                    }

                    procesarMensaje(mensaje);
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("[" + id + "] Distribuidor desconectado: " + idDistribuidor);
            } finally {
                if (idDistribuidor != null) {
                    distribuidoresConectados.remove(idDistribuidor);
                }
                cerrar();
            }
        }

        private void procesarMensaje(Mensaje mensaje) {
            switch (mensaje.getTipo()) {
                case ENVIAR_REPORTE:
                    procesarReporte(mensaje);
                    break;

                case CONFIRMACION_PRECIO:
                    System.out.println("[" + id + "] Distribuidor " + idDistribuidor +
                            " confirmó actualización de precios");
                    break;

                case RECONEXION:
                    System.out.println("[" + id + "] Distribuidor " + idDistribuidor +
                            " se reconectó");
                    break;

                default:
                    System.out.println("[" + id + "] Mensaje de distribuidor: " + mensaje);
            }
        }

        @SuppressWarnings("unchecked")
        private void procesarReporte(Mensaje mensaje) {
            Integer totalTrans = mensaje.obtenerEntero("totalTransacciones");
            Double totalVentas = mensaje.obtenerDouble("totalVentas");

            List<Transaccion> transaccionesDist =
                    (List<Transaccion>) mensaje.obtenerDato("transacciones");

            if (transaccionesDist != null) {
                transacciones.clear();
                transacciones.addAll(transaccionesDist);
                historialCompleto.addAll(transaccionesDist);
            }

            System.out.println("[" + id + "] Reporte recibido de " + idDistribuidor + ":");
            System.out.println("  Transacciones: " + totalTrans);
            System.out.println("  Total Ventas: $" + totalVentas);
        }

        public boolean enviarMensaje(Mensaje mensaje) {
            try {
                if (out != null && activo.get()) {
                    out.writeObject(mensaje);
                    out.flush();
                    return true;
                }
            } catch (IOException e) {
                System.err.println("[" + id + "] Error enviando a distribuidor " +
                        idDistribuidor + ": " + e.getMessage());
                activo.set(false);
            }
            return false;
        }

        private void cerrar() {
            try {
                if (out != null) out.close();
                if (in != null) in.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                // Ignorar
            }
        }

        public String getIdDistribuidor() {
            return idDistribuidor;
        }

        public List<Transaccion> getTransacciones() {
            return new ArrayList<>(transacciones);
        }
    }
}