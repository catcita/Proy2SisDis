package nivel2;

import comun.BaseDatos;
import comun.Mensaje;
import comun.Transaccion;
import comun.TipoCombustible;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**********************************************************************************************************************/
 /* Clase Distribuidor (estación de servicios) - Nivel 2
 * Actúa como servidor para CLIENTES y como cliente de Administración
 **********************************************************************************************************************/

public class Distribuidor {
    //VARIABLES
    private final String id; //de distribuidor
    private int puertoLocal;
    private double factorUtilidad; // por ej 1.15 = 15% de utilidad

    //servidor para clientes surtidores
    private ServerSocket serverSocket;
    private Map<String, ManejadorCliente> clientesConectados;
    private AtomicBoolean servidorActivo;

    //cliente hacia administración
    private String ipAdministracion;
    private int puertoAdministracion;
    private Socket socketAdmin;
    private ObjectOutputStream outAdmin;
    private ObjectInputStream inAdmin;
    private AtomicBoolean conectadoAdmin;

    //base de datos local
    private BaseDatos baseDatos;
    private Map<TipoCombustible, Double> preciosBase;
    private List<Transaccion> transaccionesPendientes;

    //CONSTRUCTOR
    public Distribuidor(String id, int puertoLocal, double factorUtilidad) {
        this.id = id;
        this.puertoLocal = puertoLocal;
        this.factorUtilidad = factorUtilidad;

        this.clientesConectados = new ConcurrentHashMap<>();
        this.servidorActivo = new AtomicBoolean(false);
        this.conectadoAdmin = new AtomicBoolean(false);

        this.baseDatos = new BaseDatos(id);
        this.preciosBase = new HashMap<>();
        this.transaccionesPendientes = new CopyOnWriteArrayList<>();

        inicializarPreciosDefecto();
    }

    private void inicializarPreciosDefecto() {
        preciosBase.put(TipoCombustible.GASOLINA_93, 1000.0);
        preciosBase.put(TipoCombustible.GASOLINA_95, 1100.0);
        preciosBase.put(TipoCombustible.GASOLINA_97, 1200.0);
        preciosBase.put(TipoCombustible.DIESEL, 900.0);
        preciosBase.put(TipoCombustible.KEROSENE, 800.0);
    }

    /**
     * inicia el servidor para clientes surtidores (con HILO) */
    public void iniciarServidor() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(puertoLocal);
                servidorActivo.set(true);
                System.out.println("[" + id + "] Servidor iniciado en puerto " + puertoLocal);

                while (servidorActivo.get()) {
                    Socket clienteSocket = serverSocket.accept();
                    ManejadorCliente manejador = new ManejadorCliente(clienteSocket);
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
     * conecta con la administración central */
    public boolean conectarAdministracion(String ip, int puerto) {
        this.ipAdministracion = ip;
        this.puertoAdministracion = puerto;

        try {
            socketAdmin = new Socket(ip, puerto);
            outAdmin = new ObjectOutputStream(socketAdmin.getOutputStream());
            inAdmin = new ObjectInputStream(socketAdmin.getInputStream());
            conectadoAdmin.set(true);

            System.out.println("[" + id + "] Conectado a administración en " + ip + ":" + puerto);

            //iniciar hilo para escuchar mensajes de administración
            new Thread(this::escucharAdministracion).start();

            //sincronizar transacciones pendientes
            sincronizarTransacciones();

            return true;
        } catch (IOException e) {
            System.err.println("[" + id + "] Error al conectar con administración: " + e.getMessage());
            conectadoAdmin.set(false);
            return false;
        }
    }

    /**
     * escucha mensajes de la administración */
    private void escucharAdministracion() {
        while (conectadoAdmin.get()) {
            try {
                Mensaje mensaje = (Mensaje) inAdmin.readObject();
                procesarMensajeAdmin(mensaje);
            } catch (IOException | ClassNotFoundException e) {
                if (conectadoAdmin.get()) {
                    System.err.println("[" + id + "] Conexión perdida con administración");
                    conectadoAdmin.set(false);
                    intentarReconectarAdmin();
                }
                break;
            }
        }
    }

    /**
     *procesa mensajes de la administración */
    private void procesarMensajeAdmin(Mensaje mensaje) {
        switch (mensaje.getTipo()) {
            case ACTUALIZAR_PRECIO_BASE:
                actualizarPreciosBase(mensaje);
                break;

            case SOLICITAR_REPORTE:
                enviarReporte();
                break;

            default:
                System.out.println("[" + id + "] Mensaje de admin: " + mensaje);
        }
    }

    /**
     * actualiza los precios base y propaga a surtidores */
    private void actualizarPreciosBase(Mensaje mensaje) {
        @SuppressWarnings("unchecked")
        Map<String, Double> nuevosPrecios = (Map<String, Double>) mensaje.obtenerDato("precios");

        if (nuevosPrecios != null) {
            System.out.println("[" + id + "] Actualizando precios base...");

            for (Map.Entry<String, Double> entry : nuevosPrecios.entrySet()) {
                try {
                    TipoCombustible tipo = TipoCombustible.valueOf(entry.getKey());
                    double precioBase = entry.getValue();
                    double precioFinal = precioBase * factorUtilidad;
                    preciosBase.put(tipo, precioFinal);

                    System.out.println("[" + id + "] " + tipo.getNombre() +
                            ": Base=$" + precioBase + " Final=$" + precioFinal);
                } catch (IllegalArgumentException e) {
                    System.err.println("[" + id + "] Tipo de combustible inválido: " + entry.getKey());
                }
            }

            //propagar precios a todos los clientes surtidores
            propagarPreciosClientes();

            //confirmar a administración
            Mensaje confirmacion = new Mensaje(Mensaje.Tipo.CONFIRMACION_PRECIO, id);
            enviarMensajeAdmin(confirmacion);
        }
    }

    /**
     * propaga los precios a todos los surtidores conectados */
    private void propagarPreciosClientes() {
        Mensaje mensaje = new Mensaje(Mensaje.Tipo.ACTUALIZAR_PRECIO_CLIENTE, id);
        Map<String, Double> preciosStr = new HashMap<>();

        for (Map.Entry<TipoCombustible, Double> entry : preciosBase.entrySet()) {
            preciosStr.put(entry.getKey().name(), entry.getValue());
        }

        mensaje.agregarDato("precios", preciosStr);

        for (ManejadorCliente manejador : clientesConectados.values()) {
            manejador.enviarMensaje(mensaje);
        }
    }

    /**
     * envía reporte a la administración */
    private void enviarReporte() {
        List<Transaccion> transacciones = baseDatos.obtenerTodasTransacciones();

        Mensaje reporte = new Mensaje(Mensaje.Tipo.ENVIAR_REPORTE, id);
        reporte.agregarDato("totalTransacciones", transacciones.size());
        reporte.agregarDato("transacciones", transacciones);

        double totalVentas = transacciones.stream()
                .mapToDouble(Transaccion::getMontoTotal)
                .sum();
        reporte.agregarDato("totalVentas", totalVentas);

        enviarMensajeAdmin(reporte);
        System.out.println("[" + id + "] Reporte enviado: " + transacciones.size() +
                " transacciones, Total: $" + totalVentas);
    }

    /**
     * sincroniza transacciones pendientes cuando se reconecta */
    private void sincronizarTransacciones() {
        if (!transaccionesPendientes.isEmpty()) {
            System.out.println("[" + id + "] Sincronizando " +
                    transaccionesPendientes.size() + " transacciones pendientes...");

            for (Transaccion t : transaccionesPendientes) {
                //aquí podrías enviar las transacciones a admin si es necesario
            }
            transaccionesPendientes.clear();
        }
    }

    /**
     * Intenta reconectar con administración
     */
    private void intentarReconectarAdmin() {
        System.out.println("[" + id + "] Modo local activado. Intentando reconectar...");
        int intentos = 0;

        while (!conectadoAdmin.get() && intentos < 10) {
            try {
                Thread.sleep(10000); // Esperar 10 segundos
                if (conectarAdministracion(ipAdministracion, puertoAdministracion)) {
                    System.out.println("[" + id + "] Reconectado a administración");
                    return;
                }
                intentos++;
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    /**
     * Envía mensaje a administración
     */
    private void enviarMensajeAdmin(Mensaje mensaje) {
        try {
            if (outAdmin != null && conectadoAdmin.get()) {
                outAdmin.writeObject(mensaje);
                outAdmin.flush();
            }
        } catch (IOException e) {
            System.err.println("[" + id + "] Error al enviar mensaje a admin: " + e.getMessage());
            conectadoAdmin.set(false);
        }
    }

    /**
     * Detiene el servidor
     */
    public void detener() {
        servidorActivo.set(false);
        conectadoAdmin.set(false);

        try {
            if (serverSocket != null) serverSocket.close();
            if (socketAdmin != null) socketAdmin.close();
            System.out.println("[" + id + "] Distribuidor detenido");
        } catch (IOException e) {
            System.err.println("[" + id + "] Error al detener: " + e.getMessage());
        }
    }

    /**
     * Clase interna para manejar cada surtidor conectado
     */
    private class ManejadorCliente implements Runnable {
        private Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private String idSurtidor;
        private AtomicBoolean activo;

        public ManejadorCliente(Socket socket) {
            this.socket = socket;
            this.activo = new AtomicBoolean(true);
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                System.out.println("[" + id + "] Nuevo surtidor conectado desde " +
                        socket.getInetAddress());

                while (activo.get()) {
                    Mensaje mensaje = (Mensaje) in.readObject();

                    if (idSurtidor == null) {
                        idSurtidor = mensaje.getIdOrigen();
                        clientesConectados.put(idSurtidor, this);
                        System.out.println("[" + id + "] Surtidor registrado: " + idSurtidor);
                    }

                    procesarMensajeCliente(mensaje);
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("[" + id + "] Surtidor desconectado: " + idSurtidor);
            } finally {
                if (idSurtidor != null) {
                    clientesConectados.remove(idSurtidor);
                }
                cerrar();
            }
        }

        private void procesarMensajeCliente(Mensaje mensaje) {
            switch (mensaje.getTipo()) {
                case REGISTRAR_TRANSACCION:
                    Transaccion t = (Transaccion) mensaje.obtenerDato("transaccion");
                    if (t != null) {
                        baseDatos.guardarTransaccion(t);
                        System.out.println("[" + id + "] Transacción registrada: " + t.getId());

                        if (!conectadoAdmin.get()) {
                            transaccionesPendientes.add(t);
                        }

                        Mensaje ack = new Mensaje(Mensaje.Tipo.ACK, id);
                        enviarMensaje(ack);
                    }
                    break;

                default:
                    System.out.println("[" + id + "] Mensaje de surtidor: " + mensaje);
            }
        }

        public void enviarMensaje(Mensaje mensaje) {
            try {
                if (out != null && activo.get()) {
                    out.writeObject(mensaje);
                    out.flush();
                }
            } catch (IOException e) {
                System.err.println("[" + id + "] Error al enviar a surtidor: " + e.getMessage());
                activo.set(false);
            }
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
    }

    //GETTERS
    public String getId() {
        return id;
    }

    public boolean isConectadoAdmin() {
        return conectadoAdmin.get();
    }

    public int getCantidadClientes() {
        return clientesConectados.size();
    }
}