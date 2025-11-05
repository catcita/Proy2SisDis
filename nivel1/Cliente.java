package nivel1;

import comun.Mensaje;
import comun.Transaccion;
import comun.TipoCombustible;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**********************************************************************************************************************/
 /* Clase Cliente (surtidor) - Nivel 1
 * Actúa como cliente del Distribuidor */
/**********************************************************************************************************************/

public class Cliente {
    //VARIABLES
    private final String id; //de cliente
    private String ipDistribuidor;
    private int puertoDistribuidor;
    private TipoCombustible tipoCombustible;

    private Map<TipoCombustible, Double> precios;
    private AtomicBoolean enOperacion;
    private int totalCargas;
    private double totalLitros;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private AtomicBoolean conectado;

    //CONSTRUCTOR
    public Cliente(String id, TipoCombustible tipoCombustible) {
        this.id = id;
        this.tipoCombustible = tipoCombustible;
        this.precios = new HashMap<>();
        this.enOperacion = new AtomicBoolean(false);
        this.totalCargas = 0;
        this.totalLitros = 0;
        this.conectado = new AtomicBoolean(false);

        //precios iniciales por defecto
        precios.put(TipoCombustible.GASOLINA_93, 1000.0);
        precios.put(TipoCombustible.GASOLINA_95, 1100.0);
        precios.put(TipoCombustible.GASOLINA_97, 1200.0);
        precios.put(TipoCombustible.DIESEL, 900.0);
        precios.put(TipoCombustible.KEROSENE, 800.0);
    }

    /**
     * conecta el cliente al distribuidor */
    public boolean conectar(String ip, int puerto) {
        this.ipDistribuidor = ip;
        this.puertoDistribuidor = puerto;

        try {
            socket = new Socket(ip, puerto);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            conectado.set(true);

            System.out.println("[" + id + "] Conectado al distribuidor en " + ip + ":" + puerto);

            // iniciar hilo para escuchar mensajes del distribuidor
            new Thread(this::escucharDistribuidor).start();

            return true;
        } catch (IOException e) {
            System.err.println("[" + id + "] Error al conectar: " + e.getMessage());
            conectado.set(false);
            return false;
        }
    }

    /**
     * HILO que escucha mensajes del distribuidor */
    private void escucharDistribuidor() {
        while (conectado.get()) {
            try {
                Mensaje mensaje = (Mensaje) in.readObject();
                procesarMensaje(mensaje);
            } catch (IOException | ClassNotFoundException e) {
                if (conectado.get()) {
                    System.err.println("[" + id + "] Error al recibir mensaje: " + e.getMessage());
                    conectado.set(false);
                    intentarReconectar();
                }
                break;
            }
        }
    }

    /**
     * Procesa mensajes recibidos del distribuidor
     */
    private void procesarMensaje(Mensaje mensaje) {
        switch (mensaje.getTipo()) {
            case CONSULTAR_ESTADO:
                responderEstado();
                break;

            case ACTUALIZAR_PRECIO_CLIENTE:
                if (!enOperacion.get()) {
                    actualizarPrecios(mensaje);
                } else {
                    System.out.println("[" + id + "] No se puede actualizar precio: surtidor en operación");
                    enviarMensaje(new Mensaje(Mensaje.Tipo.ERROR, id));
                }
                break;

            case PING:
                enviarMensaje(new Mensaje(Mensaje.Tipo.ACK, id));
                break;

            default:
                System.out.println("[" + id + "] Mensaje recibido: " + mensaje);
        }
    }

    /**
     * responde al distribuidor con el estado actual */
    private void responderEstado() {
        Mensaje respuesta = new Mensaje(Mensaje.Tipo.ESTADO_CLIENTE, id);
        respuesta.agregarDato("enOperacion", enOperacion.get());
        respuesta.agregarDato("totalCargas", totalCargas);
        respuesta.agregarDato("totalLitros", totalLitros);
        enviarMensaje(respuesta);
    }

    /**
     * Actualiza los precios recibidos del distribuidor
     */
    private void actualizarPrecios(Mensaje mensaje) {
        @SuppressWarnings("unchecked")
        Map<String, Double> nuevosPrecios = (Map<String, Double>) mensaje.obtenerDato("precios");

        if (nuevosPrecios != null) {
            for (Map.Entry<String, Double> entry : nuevosPrecios.entrySet()) {
                try {
                    TipoCombustible tipo = TipoCombustible.valueOf(entry.getKey());
                    precios.put(tipo, entry.getValue());
                } catch (IllegalArgumentException e) {
                    System.err.println("[" + id + "] Tipo de combustible inválido: " + entry.getKey());
                }
            }
            System.out.println("[" + id + "] Precios actualizados: " + precios);

            Mensaje confirmacion = new Mensaje(Mensaje.Tipo.ACK, id);
            enviarMensaje(confirmacion);
        }
    }

    /**
     * simula una operación de carga de combustible */
    public void realizarCarga(double litros) {
        if (!conectado.get()) {
            System.err.println("[" + id + "] No conectado al distribuidor");
            return;
        }

        enOperacion.set(true);
        System.out.println("[" + id + "] Iniciando carga de " + litros + " litros de " +
                tipoCombustible.getNombre());

        try {
            //simular tiempo de carga
            Thread.sleep((long)(litros * 100)); // 100ms por litro

            double precio = precios.get(tipoCombustible);
            Transaccion transaccion = new Transaccion(id, "DIST-001", tipoCombustible, litros, precio);

            totalCargas++;
            totalLitros += litros;

            // Enviar transacción al distribuidor
            Mensaje mensaje = new Mensaje(Mensaje.Tipo.REGISTRAR_TRANSACCION, id);
            mensaje.agregarDato("transaccion", transaccion);
            enviarMensaje(mensaje);

            System.out.println("[" + id + "] Carga completada: " + transaccion);

        } catch (InterruptedException e) {
            System.err.println("[" + id + "] Carga interrumpida");
        } finally {
            enOperacion.set(false);
        }
    }

    /**
     * envía un mensaje al distribuidor */
    private void enviarMensaje(Mensaje mensaje) {
        try {
            if (out != null && conectado.get()) {
                out.writeObject(mensaje);
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("[" + id + "] Error al enviar mensaje: " + e.getMessage());
            conectado.set(false);
        }
    }

    /**
     * intenta reconectar al distribuidor */
    private void intentarReconectar() {
        System.out.println("[" + id + "] Intentando reconectar...");
        int intentos = 0;
        while (!conectado.get() && intentos < 5) {
            try {
                Thread.sleep(5000); // esperar 5 segundos
                if (conectar(ipDistribuidor, puertoDistribuidor)) {
                    System.out.println("[" + id + "] Reconectado exitosamente");
                    return;
                }
                intentos++;
            } catch (InterruptedException e) {
                break;
            }
        }
        System.err.println("[" + id + "] No se pudo reconectar después de " + intentos + " intentos");
    }

    /**
     * desconecta el surtidor */
    public void desconectar() {
        conectado.set(false);
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
            System.out.println("[" + id + "] Desconectado");
        } catch (IOException e) {
            System.err.println("[" + id + "] Error al desconectar: " + e.getMessage());
        }
    }

    // GETTERS
    public String getId() {
        return id;
    }

    public boolean isEnOperacion() {
        return enOperacion.get();
    }

    public boolean isConectado() {
        return conectado.get();
    }

    public int getTotalCargas() {
        return totalCargas;
    }

    public double getTotalLitros() {
        return totalLitros;
    }
}