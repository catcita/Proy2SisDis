package nivel2;

import comun.BaseDatos;

import javax.swing.*;
import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Interfaz gráfica para el Distribuidor */

public class DistribuidorGUI extends JFrame {
    private Distribuidor distribuidor;

    // Componentes de configuración
    private JTextField txtId;
    private JTextField txtPuertoLocal;
    private JTextField txtFactorUtilidad;
    private JTextField txtIpAdmin;
    private JTextField txtPuertoAdmin;
    private JButton btnIniciar;
    private JButton btnDetener;
    private JButton btnConectarAdmin;

    // Componentes de estado
    private JLabel lblEstadoServidor;
    private JLabel lblEstadoAdmin;
    private JLabel lblSurtidores;
    private JButton btnVerificarBD;
    private JButton btnEstadisticasBD;

    // Panel de log
    private JTextArea txtLog;

    public DistribuidorGUI() {
        setTitle("Distribuidor - Sistema Distribuido");
        setSize(700, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        crearPanelConfiguracion();
        crearPanelEstado();
        crearPanelLog();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void crearPanelConfiguracion() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Configuración"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // ID
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("ID Distribuidor:"), gbc);
        gbc.gridx = 1;
        txtId = new JTextField("DIST-001", 15);
        panel.add(txtId, gbc);

        // Puerto local
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Puerto Local:"), gbc);
        gbc.gridx = 1;
        txtPuertoLocal = new JTextField("5001", 15);
        panel.add(txtPuertoLocal, gbc);

        // Factor de utilidad
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Factor Utilidad:"), gbc);
        gbc.gridx = 1;
        txtFactorUtilidad = new JTextField("1.15", 15);
        txtFactorUtilidad.setToolTipText("1.15 = 15% de utilidad");
        panel.add(txtFactorUtilidad, gbc);

        // IP Administración
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("IP Administración:"), gbc);
        gbc.gridx = 1;
        txtIpAdmin = new JTextField("localhost", 15);
        panel.add(txtIpAdmin, gbc);

        // Puerto Administración
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("Puerto Admin:"), gbc);
        gbc.gridx = 1;
        txtPuertoAdmin = new JTextField("6000", 15);
        panel.add(txtPuertoAdmin, gbc);

        // Botones
        JPanel panelBotones = new JPanel(new FlowLayout());
        btnIniciar = new JButton("Iniciar Servidor");
        btnDetener = new JButton("Detener");
        btnConectarAdmin = new JButton("Conectar Admin");

        btnDetener.setEnabled(false);
        btnConectarAdmin.setEnabled(false);

        btnIniciar.addActionListener(e -> iniciarServidor());
        btnDetener.addActionListener(e -> detenerServidor());
        btnConectarAdmin.addActionListener(e -> conectarAdmin());

        panelBotones.add(btnIniciar);
        panelBotones.add(btnDetener);
        panelBotones.add(btnConectarAdmin);

        gbc.gridx = 0; gbc.gridy = 5;
        gbc.gridwidth = 2;
        panel.add(panelBotones, gbc);

        add(panel, BorderLayout.NORTH);
    }

    private void crearPanelEstado() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Estado"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Estado servidor
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Servidor:"), gbc);
        gbc.gridx = 1;
        lblEstadoServidor = new JLabel("Detenido");
        lblEstadoServidor.setForeground(Color.RED);
        panel.add(lblEstadoServidor, gbc);

        // Estado conexión admin
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Conexión Admin:"), gbc);
        gbc.gridx = 1;
        lblEstadoAdmin = new JLabel("Desconectado");
        lblEstadoAdmin.setForeground(Color.RED);
        panel.add(lblEstadoAdmin, gbc);

        // Surtidores conectados
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Surtidores:"), gbc);
        gbc.gridx = 1;
        lblSurtidores = new JLabel("0");
        panel.add(lblSurtidores, gbc);

        // Botones de BD
        JPanel panelBD = new JPanel(new FlowLayout());
        btnVerificarBD = new JButton("Verificar BD");
        btnEstadisticasBD = new JButton("Estadísticas BD");

        btnVerificarBD.setEnabled(false);
        btnEstadisticasBD.setEnabled(false);

        btnVerificarBD.addActionListener(e -> verificarBD());
        btnEstadisticasBD.addActionListener(e -> mostrarEstadisticas());

        panelBD.add(btnVerificarBD);
        panelBD.add(btnEstadisticasBD);

        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(panelBD, gbc);

        add(panel, BorderLayout.CENTER);
    }

    private void crearPanelLog() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Log"));

        txtLog = new JTextArea(15, 50);
        txtLog.setEditable(false);
        JScrollPane scroll = new JScrollPane(txtLog);
        panel.add(scroll, BorderLayout.CENTER);

        add(panel, BorderLayout.SOUTH);
    }

    private void iniciarServidor() {
        try {
            String id = txtId.getText().trim();
            int puerto = Integer.parseInt(txtPuertoLocal.getText().trim());
            double factor = Double.parseDouble(txtFactorUtilidad.getText().trim());

            if (id.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Debe ingresar un ID",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (factor <= 1.0) {
                JOptionPane.showMessageDialog(this,
                        "El factor de utilidad debe ser mayor a 1.0",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            distribuidor = new Distribuidor(id, puerto, factor);

            // Redirigir System.out al log
            redirigirSalidaALog();

            distribuidor.iniciarServidor();

            lblEstadoServidor.setText("Activo");
            lblEstadoServidor.setForeground(Color.GREEN);

            btnIniciar.setEnabled(false);
            btnDetener.setEnabled(true);
            btnConectarAdmin.setEnabled(true);
            btnVerificarBD.setEnabled(true);
            btnEstadisticasBD.setEnabled(true);

            // Deshabilitar campos de configuración
            txtId.setEnabled(false);
            txtPuertoLocal.setEnabled(false);
            txtFactorUtilidad.setEnabled(false);

            // Iniciar actualización periódica
            iniciarActualizacionEstado();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Valores numéricos inválidos", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void detenerServidor() {
        if (distribuidor != null) {
            distribuidor.detener();
            lblEstadoServidor.setText("Detenido");
            lblEstadoServidor.setForeground(Color.RED);
            lblEstadoAdmin.setText("Desconectado");
            lblEstadoAdmin.setForeground(Color.RED);

            btnIniciar.setEnabled(true);
            btnDetener.setEnabled(false);
            btnConectarAdmin.setEnabled(false);
            btnVerificarBD.setEnabled(false);
            btnEstadisticasBD.setEnabled(false);

            txtId.setEnabled(true);
            txtPuertoLocal.setEnabled(true);
            txtFactorUtilidad.setEnabled(true);
        }
    }

    private void conectarAdmin() {
        if (distribuidor == null) {
            JOptionPane.showMessageDialog(this,
                    "Debe iniciar el servidor primero",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            String ip = txtIpAdmin.getText().trim();
            int puerto = Integer.parseInt(txtPuertoAdmin.getText().trim());

            if (distribuidor.conectarAdministracion(ip, puerto)) {
                lblEstadoAdmin.setText("Conectado");
                lblEstadoAdmin.setForeground(Color.GREEN);
                txtIpAdmin.setEnabled(false);
                txtPuertoAdmin.setEnabled(false);
            } else {
                JOptionPane.showMessageDialog(this,
                        "No se pudo conectar con la administración",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Puerto inválido", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void verificarBD() {
        if (distribuidor != null) {
            new Thread(() -> {
                BaseDatos bd = new BaseDatos(distribuidor.getId());
                boolean integra = bd.verificarIntegridad();

                SwingUtilities.invokeLater(() -> {
                    if (integra) {
                        JOptionPane.showMessageDialog(this,
                                "Base de datos íntegra",
                                "Verificación", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "Se detectaron inconsistencias. Revisar log.",
                                "Verificación", JOptionPane.WARNING_MESSAGE);
                    }
                });
            }).start();
        }
    }

    private void mostrarEstadisticas() {
        if (distribuidor != null) {
            new Thread(() -> {
                BaseDatos bd = new BaseDatos(distribuidor.getId());
                String estadisticas = bd.obtenerEstadisticas();

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                            estadisticas,
                            "Estadísticas BD",
                            JOptionPane.INFORMATION_MESSAGE);
                });
            }).start();
        }
    }

    private void iniciarActualizacionEstado() {
        Timer timer = new Timer(2000, e -> actualizarEstado());
        timer.start();
    }

    private void actualizarEstado() {
        if (distribuidor != null) {
            lblSurtidores.setText(String.valueOf(distribuidor.getCantidadClientes()));

            if (distribuidor.isConectadoAdmin()) {
                lblEstadoAdmin.setText("Conectado");
                lblEstadoAdmin.setForeground(Color.GREEN);
            } else {
                lblEstadoAdmin.setText("Desconectado (Modo Local)");
                lblEstadoAdmin.setForeground(Color.ORANGE);
            }
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
        SwingUtilities.invokeLater(() -> new DistribuidorGUI());
    }
}