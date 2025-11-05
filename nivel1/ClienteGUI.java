package nivel1;

import comun.TipoCombustible;

import javax.swing.*;
import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Interfaz gráfica para CLIENTE */
public class ClienteGUI extends JFrame {
    private Cliente cliente;

    // Componentes de configuración
    private JTextField txtId;
    private JComboBox<TipoCombustible> cmbTipoCombustible;
    private JTextField txtIpDistribuidor;
    private JTextField txtPuertoDistribuidor;
    private JButton btnConectar;
    private JButton btnDesconectar;

    // Componentes de operación
    private JTextField txtLitros;
    private JButton btnCargar;
    private JLabel lblEstado;
    private JLabel lblCargas;
    private JLabel lblLitrosTotales;

    // Panel de log
    private JTextArea txtLog;

    public ClienteGUI() {
        setTitle("Surtidor - Sistema Distribuido");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        crearPanelConfiguracion();
        crearPanelOperacion();
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
        panel.add(new JLabel("ID Surtidor:"), gbc);
        gbc.gridx = 1;
        txtId = new JTextField("SURT-001", 15);
        panel.add(txtId, gbc);

        // Tipo de combustible
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Tipo Combustible:"), gbc);
        gbc.gridx = 1;
        cmbTipoCombustible = new JComboBox<>(TipoCombustible.values());
        panel.add(cmbTipoCombustible, gbc);

        // IP Distribuidor
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("IP Distribuidor:"), gbc);
        gbc.gridx = 1;
        txtIpDistribuidor = new JTextField("localhost", 15);
        panel.add(txtIpDistribuidor, gbc);

        // Puerto Distribuidor
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Puerto Distribuidor:"), gbc);
        gbc.gridx = 1;
        txtPuertoDistribuidor = new JTextField("5001", 15);
        panel.add(txtPuertoDistribuidor, gbc);

        // Botones
        JPanel panelBotones = new JPanel(new FlowLayout());
        btnConectar = new JButton("Conectar");
        btnDesconectar = new JButton("Desconectar");
        btnDesconectar.setEnabled(false);

        btnConectar.addActionListener(e -> conectar());
        btnDesconectar.addActionListener(e -> desconectar());

        panelBotones.add(btnConectar);
        panelBotones.add(btnDesconectar);

        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        panel.add(panelBotones, gbc);

        add(panel, BorderLayout.NORTH);
    }

    private void crearPanelOperacion() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Operación"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Estado
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Estado:"), gbc);
        gbc.gridx = 1;
        lblEstado = new JLabel("Desconectado");
        lblEstado.setForeground(Color.RED);
        panel.add(lblEstado, gbc);

        // Cargas totales
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Total Cargas:"), gbc);
        gbc.gridx = 1;
        lblCargas = new JLabel("0");
        panel.add(lblCargas, gbc);

        // Litros totales
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Litros Totales:"), gbc);
        gbc.gridx = 1;
        lblLitrosTotales = new JLabel("0.0");
        panel.add(lblLitrosTotales, gbc);

        // Litros a cargar
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Litros a Cargar:"), gbc);
        gbc.gridx = 1;
        txtLitros = new JTextField("20.0", 15);
        panel.add(txtLitros, gbc);

        // Botón cargar
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        btnCargar = new JButton("Realizar Carga");
        btnCargar.setEnabled(false);
        btnCargar.addActionListener(e -> realizarCarga());
        panel.add(btnCargar, gbc);

        add(panel, BorderLayout.CENTER);
    }

    private void crearPanelLog() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Log"));

        txtLog = new JTextArea(8, 40);
        txtLog.setEditable(false);
        JScrollPane scroll = new JScrollPane(txtLog);
        panel.add(scroll, BorderLayout.CENTER);

        add(panel, BorderLayout.SOUTH);
    }

    private void conectar() {
        try {
            String id = txtId.getText().trim();
            TipoCombustible tipo = (TipoCombustible) cmbTipoCombustible.getSelectedItem();
            String ip = txtIpDistribuidor.getText().trim();
            int puerto = Integer.parseInt(txtPuertoDistribuidor.getText().trim());

            if (id.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Debe ingresar un ID",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            cliente = new Cliente(id, tipo);

            // Redirigir System.out al log
            redirigirSalidaALog();

            if (cliente.conectar(ip, puerto)) {
                lblEstado.setText("Conectado");
                lblEstado.setForeground(Color.GREEN);
                btnConectar.setEnabled(false);
                btnDesconectar.setEnabled(true);
                btnCargar.setEnabled(true);

                // Deshabilitar campos de configuración
                txtId.setEnabled(false);
                cmbTipoCombustible.setEnabled(false);
                txtIpDistribuidor.setEnabled(false);
                txtPuertoDistribuidor.setEnabled(false);

                // Iniciar actualización periódica de estadísticas
                iniciarActualizacionEstadisticas();
            } else {
                JOptionPane.showMessageDialog(this,
                        "No se pudo conectar al distribuidor",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Puerto inválido", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void desconectar() {
        if (cliente != null) {
            cliente.desconectar();
            lblEstado.setText("Desconectado");
            lblEstado.setForeground(Color.RED);
            btnConectar.setEnabled(true);
            btnDesconectar.setEnabled(false);
            btnCargar.setEnabled(false);

            txtId.setEnabled(true);
            cmbTipoCombustible.setEnabled(true);
            txtIpDistribuidor.setEnabled(true);
            txtPuertoDistribuidor.setEnabled(true);
        }
    }

    private void realizarCarga() {
        if (cliente == null || !cliente.isConectado()) {
            JOptionPane.showMessageDialog(this,
                    "No está conectado", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            double litros = Double.parseDouble(txtLitros.getText().trim());

            if (litros <= 0 || litros > 100) {
                JOptionPane.showMessageDialog(this,
                        "Los litros deben estar entre 0 y 100",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Ejecutar carga en hilo separado
            new Thread(() -> {
                btnCargar.setEnabled(false);
                cliente.realizarCarga(litros);
                btnCargar.setEnabled(true);
                actualizarEstadisticas();
            }).start();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Litros inválidos", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void iniciarActualizacionEstadisticas() {
        Timer timer = new Timer(2000, e -> actualizarEstadisticas());
        timer.start();
    }

    private void actualizarEstadisticas() {
        if (cliente != null) {
            lblCargas.setText(String.valueOf(cliente.getTotalCargas()));
            lblLitrosTotales.setText(String.format("%.2f", cliente.getTotalLitros()));
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
        SwingUtilities.invokeLater(() -> new ClienteGUI());
    }
}