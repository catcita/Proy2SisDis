package nivel3;

import comun.TipoCombustible;

import javax.swing.*;
import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Interfaz gráfica para la Administración Central
 */
public class AdministracionGUI extends JFrame {
    private Administracion administracion;

    // Componentes de configuración
    private JTextField txtId;
    private JTextField txtPuerto;
    private JButton btnIniciar;
    private JButton btnDetener;

    // Componentes de precios
    private Map<TipoCombustible, JTextField> camposPrecios;
    private JButton btnActualizarPrecios;

    // Componentes de reportes
    private JButton btnSolicitarReportes;
    private JButton btnGenerarReporte;
    private JTextArea txtReporte;

    // Estado
    private JLabel lblEstado;
    private JLabel lblDistribuidores;

    // Panel de log
    private JTextArea txtLog;

    public AdministracionGUI() {
        setTitle("Administración Central - Sistema Distribuido");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        crearPanelSuperior();
        crearPanelCentral();
        crearPanelLog();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void crearPanelSuperior() {
        JPanel panel = new JPanel(new BorderLayout());

        // Panel de configuración
        JPanel panelConfig = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelConfig.setBorder(BorderFactory.createTitledBorder("Configuración"));

        panelConfig.add(new JLabel("ID:"));
        txtId = new JTextField("ADMIN-001", 10);
        panelConfig.add(txtId);

        panelConfig.add(new JLabel("Puerto:"));
        txtPuerto = new JTextField("6000", 8);
        panelConfig.add(txtPuerto);

        btnIniciar = new JButton("Iniciar");
        btnDetener = new JButton("Detener");
        btnDetener.setEnabled(false);

        btnIniciar.addActionListener(e -> iniciar());
        btnDetener.addActionListener(e -> detener());

        panelConfig.add(btnIniciar);
        panelConfig.add(btnDetener);

        // Panel de estado
        JPanel panelEstado = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelEstado.setBorder(BorderFactory.createTitledBorder("Estado"));

        panelEstado.add(new JLabel("Estado:"));
        lblEstado = new JLabel("Detenido");
        lblEstado.setForeground(Color.RED);
        panelEstado.add(lblEstado);

        panelEstado.add(Box.createHorizontalStrut(20));

        panelEstado.add(new JLabel("Distribuidores:"));
        lblDistribuidores = new JLabel("0");
        panelEstado.add(lblDistribuidores);

        panel.add(panelConfig, BorderLayout.NORTH);
        panel.add(panelEstado, BorderLayout.SOUTH);

        add(panel, BorderLayout.NORTH);
    }

    private void crearPanelCentral() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 10));

        // Panel de precios
        JPanel panelPrecios = new JPanel(new BorderLayout());
        panelPrecios.setBorder(BorderFactory.createTitledBorder("Gestión de Precios"));

        JPanel formularioPrecios = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        camposPrecios = new HashMap<>();
        int fila = 0;

        for (TipoCombustible tipo : TipoCombustible.values()) {
            gbc.gridx = 0; gbc.gridy = fila;
            formularioPrecios.add(new JLabel(tipo.getNombre() + ":"), gbc);

            gbc.gridx = 1;
            JTextField campo = new JTextField("1000", 10);
            camposPrecios.put(tipo, campo);
            formularioPrecios.add(campo, gbc);

            fila++;
        }

        btnActualizarPrecios = new JButton("Actualizar Precios en Red");
        btnActualizarPrecios.setEnabled(false);
        btnActualizarPrecios.addActionListener(e -> actualizarPrecios());

        gbc.gridx = 0; gbc.gridy = fila;
        gbc.gridwidth = 2;
        formularioPrecios.add(btnActualizarPrecios, gbc);

        panelPrecios.add(formularioPrecios, BorderLayout.NORTH);

        // Panel de reportes
        JPanel panelReportes = new JPanel(new BorderLayout());
        panelReportes.setBorder(BorderFactory.createTitledBorder("Reportes"));

        JPanel botonesReportes = new JPanel(new FlowLayout());
        btnSolicitarReportes = new JButton("Solicitar Reportes");
        btnGenerarReporte = new JButton("Generar Reporte");

        btnSolicitarReportes.setEnabled(false);
        btnGenerarReporte.setEnabled(false);

        btnSolicitarReportes.addActionListener(e -> solicitarReportes());
        btnGenerarReporte.addActionListener(e -> generarReporte());

        botonesReportes.add(btnSolicitarReportes);
        botonesReportes.add(btnGenerarReporte);

        txtReporte = new JTextArea();
        txtReporte.setEditable(false);
        txtReporte.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollReporte = new JScrollPane(txtReporte);

        panelReportes.add(botonesReportes, BorderLayout.NORTH);
        panelReportes.add(scrollReporte, BorderLayout.CENTER);

        panel.add(panelPrecios);
        panel.add(panelReportes);

        add(panel, BorderLayout.CENTER);
    }

    private void crearPanelLog() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Log del Sistema"));

        txtLog = new JTextArea(10, 70);
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Monospaced", Font.PLAIN, 11));
        JScrollPane scroll = new JScrollPane(txtLog);
        panel.add(scroll, BorderLayout.CENTER);

        add(panel, BorderLayout.SOUTH);
    }

    private void iniciar() {
        try {
            String id = txtId.getText().trim();
            int puerto = Integer.parseInt(txtPuerto.getText().trim());

            if (id.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Debe ingresar un ID",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            administracion = new Administracion(id, puerto);

            // Redirigir System.out al log
            redirigirSalidaALog();

            administracion.iniciar();

            lblEstado.setText("Activo");
            lblEstado.setForeground(Color.GREEN);

            btnIniciar.setEnabled(false);
            btnDetener.setEnabled(true);
            btnActualizarPrecios.setEnabled(true);
            btnSolicitarReportes.setEnabled(true);
            btnGenerarReporte.setEnabled(true);

            txtId.setEnabled(false);
            txtPuerto.setEnabled(false);

            // Iniciar actualización periódica
            iniciarActualizacionEstado();

            // Cargar precios iniciales en los campos
            cargarPreciosIniciales();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Puerto inválido", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void detener() {
        if (administracion != null) {
            administracion.detener();
            lblEstado.setText("Detenido");
            lblEstado.setForeground(Color.RED);

            btnIniciar.setEnabled(true);
            btnDetener.setEnabled(false);
            btnActualizarPrecios.setEnabled(false);
            btnSolicitarReportes.setEnabled(false);
            btnGenerarReporte.setEnabled(false);

            txtId.setEnabled(true);
            txtPuerto.setEnabled(true);
        }
    }

    private void cargarPreciosIniciales() {
        if (administracion != null) {
            Map<TipoCombustible, Double> precios = administracion.getPreciosBase();
            for (Map.Entry<TipoCombustible, Double> entry : precios.entrySet()) {
                JTextField campo = camposPrecios.get(entry.getKey());
                if (campo != null) {
                    campo.setText(String.valueOf(entry.getValue()));
                }
            }
        }
    }

    private void actualizarPrecios() {
        if (administracion == null) {
            return;
        }

        try {
            Map<TipoCombustible, Double> nuevosPrecios = new HashMap<>();

            for (Map.Entry<TipoCombustible, JTextField> entry : camposPrecios.entrySet()) {
                double precio = Double.parseDouble(entry.getValue().getText().trim());
                if (precio <= 0) {
                    JOptionPane.showMessageDialog(this,
                            "Los precios deben ser mayores a 0",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                nuevosPrecios.put(entry.getKey(), precio);
            }

            administracion.actualizarPreciosBase(nuevosPrecios);

            JOptionPane.showMessageDialog(this,
                    "Precios actualizados y enviados a distribuidores",
                    "Éxito", JOptionPane.INFORMATION_MESSAGE);

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Valores de precio inválidos", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void solicitarReportes() {
        if (administracion != null) {
            administracion.solicitarReportes();
            JOptionPane.showMessageDialog(this,
                    "Solicitud de reportes enviada. Espere unos segundos.",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void generarReporte() {
        if (administracion != null) {
            new Thread(() -> {
                // Esperar un poco para que lleguen los reportes
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Ignorar
                }

                String reporte = administracion.generarReporteConsolidado();
                SwingUtilities.invokeLater(() -> {
                    txtReporte.setText(reporte);
                });
            }).start();
        }
    }

    private void iniciarActualizacionEstado() {
        Timer timer = new Timer(2000, e -> actualizarEstado());
        timer.start();
    }

    private void actualizarEstado() {
        if (administracion != null) {
            lblDistribuidores.setText(String.valueOf(administracion.getCantidadDistribuidores()));
        }
    }

    private void redirigirSalidaALog() {
        System.setOut(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                txtLog.append(String.valueOf((char) b));
                txtLog.setCaretPosition(txtLog.getDocument().getLength());
            }
        }));

        System.setErr(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                txtLog.append(String.valueOf((char) b));
                txtLog.setCaretPosition(txtLog.getDocument().getLength());
            }
        }));
    }

    public static void main(String[] args) {
        // Configurar look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Usar el look and feel por defecto
        }

        SwingUtilities.invokeLater(() -> new AdministracionGUI());
    }
}