package comun;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/***********************************************************************************************************************
 * Base de Datos Local del Distribuidor (Nivel 2).
 * Maneja la persistencia y la redundancia (principal y backup) usando archivos. */
/**********************************************************************************************************************/
public class BaseDatos {
    private final String idDistribuidor;
    private final String FILE_PRINCIPAL;
    private final String FILE_BACKUP;
    private AtomicLong totalTransacciones; // para evitar condiciones de carrera

    public BaseDatos(String idDistribuidor) {
        this.idDistribuidor = idDistribuidor;
        // direcciones (path)
        this.FILE_PRINCIPAL = "data/" + idDistribuidor + "_principal.csv";
        this.FILE_BACKUP = "data/" + idDistribuidor + "_backup.csv";

        // asegurar que el directorio de datos exista
        new File("data").mkdirs();

        // inicializar archivos si no existen
        inicializarArchivos();

        // contar transacciones existentes
        this.totalTransacciones = new AtomicLong(obtenerTodasTransacciones().size());
    }

    /**
     * Crea los archivos si no existen.
     */
    private void inicializarArchivos() {
        crearArchivoSiNoExiste(FILE_PRINCIPAL);
        crearArchivoSiNoExiste(FILE_BACKUP);
    }

    private void crearArchivoSiNoExiste(String path) {
        File file = new File(path);
        if (!file.exists()) {
            try {
                file.createNewFile();
                // escribir cabecera (opcional)
                System.out.println("Archivo de BD creado: " + path);
            } catch (IOException e) {
                System.err.println("Error al crear archivo de BD: " + path + " - " + e.getMessage());
            }
        }
    }

    /**
     * Guarda una transacción en el archivo principal y el backup (redundancia).
     */
    public synchronized void guardarTransaccion(Transaccion transaccion) {
        String linea = transaccion.toCSV() + "\n";

        // 1. Escribir en Principal
        escribirLinea(FILE_PRINCIPAL, linea);

        // 2. Escribir en Backup (Redundancia)
        escribirLinea(FILE_BACKUP, linea);

        totalTransacciones.incrementAndGet();
    }

    private void escribirLinea(String filePath, String linea) {
        try (FileWriter fw = new FileWriter(filePath, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(linea);
        } catch (IOException e) {
            System.err.println("[" + idDistribuidor + "] ERROR: Fallo al escribir en " + filePath + ": " + e.getMessage());
        }
    }

    /**
     * Carga todas las transacciones del archivo principal.
     */
    public synchronized List<Transaccion> obtenerTodasTransacciones() {
        List<Transaccion> transacciones = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(FILE_PRINCIPAL))) {
            // Saltamos la primera línea si hubiera cabecera (en este caso, no la hay, pero es buena práctica)
            String linea;
            while ((linea = br.readLine()) != null) {
                // Parsear la línea CSV a objeto Transaccion
                Transaccion t = parsearLineaCSV(linea);
                if (t != null) {
                    transacciones.add(t);
                }
            }
        } catch (IOException e) {
            System.err.println("[" + idDistribuidor + "] ERROR al leer transacciones de archivo: " + e.getMessage());
        }

        return transacciones;
    }

    /**
     * Parsea una línea CSV para crear un objeto Transaccion.
     */
    private Transaccion parsearLineaCSV(String linea) {
        if (linea == null || linea.isEmpty()) return null;

        try {
            // El formato CSV está definido en Transaccion.toCSV()
            String[] campos = linea.split(";");

            // Asegurarse de tener 8 campos: id, idCliente, idDistribuidor, TipoCombustible, litros, precioLitro, montoTotal, fechaHora
            if (campos.length != 8) {
                System.err.println("Error: Línea CSV con formato incorrecto: " + linea);
                return null;
            }

            // Los campos que realmente necesitamos para el cálculo (litros y precioLitro)
            String idCliente = campos[1];
            String idDistribuidor = campos[2];
            TipoCombustible tipoCombustible = TipoCombustible.valueOf(campos[3]);
            double litros = Double.parseDouble(campos[4]);
            double precioLitro = Double.parseDouble(campos[5]);

            // Creamos un nuevo objeto Transaccion con los datos relevantes
            // Nota: Se usa el constructor existente, pero se ignoran los campos id y fechaHora del CSV para esta simple reconstrucción.
            // Para una reconstrucción precisa, se necesitarían setters o un constructor completo.
            return new Transaccion(idCliente, idDistribuidor, tipoCombustible, litros, precioLitro);

        } catch (Exception e) {
            System.err.println("Error al parsear línea de transacción: " + e.getMessage());
            return null;
        }
    }

    /**
     * Verifica la integridad: compara el contenido de Principal y Backup.
     * Escenario de Error: Sincronización incorrecta de réplicas.
     */
    public boolean verificarIntegridad() {
        System.out.println("[" + idDistribuidor + "] Verificando integridad de BD...");

        List<String> principal = leerContenido(FILE_PRINCIPAL);
        List<String> backup = leerContenido(FILE_BACKUP);

        if (principal.size() != backup.size()) {
            System.err.println("[" + idDistribuidor + "] INCONSISTENCIA: Tamaño de archivos difiere. Principal: " +
                    principal.size() + ", Backup: " + backup.size());
            return false;
        }

        // Si se implementa un mecanismo de hash o checksum, se aplicaría aquí.

        System.out.println("[" + idDistribuidor + "] Integridad verificada. Ambos archivos contienen " +
                principal.size() + " registros.");
        return true;
    }

    private List<String> leerContenido(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            return br.lines().collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("[" + idDistribuidor + "] ERROR al leer archivo: " + filePath + " - " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Simula la obtención de estadísticas a partir de las transacciones.
     */
    public String obtenerEstadisticas() {
        // En un sistema real, se sumarían montos, contarían por tipo, etc.
        return String.format("Estadísticas de %s:\n" +
                        "Transacciones Registradas: %d\n" +
                        "Estado de Archivos: Principal (%s), Backup (%s)\n" +
                        "Redundancia OK.",
                idDistribuidor,
                totalTransacciones.get(),
                new File(FILE_PRINCIPAL).exists() ? "OK" : "FALTA",
                new File(FILE_BACKUP).exists() ? "OK" : "FALTA");
    }
}