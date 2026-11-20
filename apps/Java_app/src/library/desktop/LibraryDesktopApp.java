package library.desktop;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

/**
 * Library | Cliente de escritorio Java (Swing) detras del Reverse Proxy nginx.
 *
 *   /gateway/health            salud del propio nginx
 *   /api/books/books           -> books   :5001  (publico)
 *   /api/login/login|logout    -> login   :5000  (login publico; logout/session con JWT)
 *   /api/authors/authors       -> authors :5003  (JWT)
 *   /api/pedidos/pedidos       -> pedidos :5004  (JWT)
 *   /api/pagos/pagos           -> pagos   :5005  (JWT)
 */
public final class LibraryDesktopApp extends JFrame {

    static final String DEFAULT_GATEWAY = "http://34.51.105.144";
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Color RED = new Color(0xC0392B);
    private static final Color AMBER = new Color(0xB9770E);
    private static final Color GREEN = new Color(0x1E8449);

    private final GatewayClient client;
    private final JTextField gatewayField;
    private final JTextField identityField = new JTextField(16);
    private final JPasswordField passwordField = new JPasswordField(14);
    private final JCheckBox omitJwt = new JCheckBox("Omitir JWT (probar 401 del gateway)");
    private final JLabel sessionLabel = new JLabel("Sin sesion");
    private final JTextArea log = new JTextArea(9, 120);

    public LibraryDesktopApp(String gatewayUrl) {
        super("Library | Cliente Java via Reverse Proxy nginx");
        client = new GatewayClient(gatewayUrl);
        gatewayField = new JTextField(gatewayUrl, 22);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(6, 6));
        add(buildHeader(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Libros (publico)", resourceTab("/api/books/books", false,
                "isbn", "title", "authors", "category", "price", "stock"));
        tabs.addTab("Autores (JWT)", resourceTab("/api/authors/authors", true));
        tabs.addTab("Pedidos (JWT)", resourceTab("/api/pedidos/pedidos", true));
        tabs.addTab("Pagos (JWT)", resourceTab("/api/pagos/pagos", true));
        tabs.addTab("Sesion (JWT)", resourceTab("/api/login/session", true));

        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(log);
        logScroll.setBorder(BorderFactory.createTitledBorder(
                "Peticiones al gateway (metodo, URL, HTTP, X-Request-ID de nginx, ms, JWT)"));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabs, logScroll);
        split.setResizeWeight(0.72);
        add(split, BorderLayout.CENTER);

        updateSession(null);
        setSize(new Dimension(1200, 820));
        setLocationRelativeTo(null);
    }

    // ==================== INTERFAZ ====================

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(8, 10, 0, 10));

        JPanel gatewayRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        gatewayRow.setBorder(BorderFactory.createTitledBorder("Reverse Proxy / API Gateway nginx (puerto 80)"));
        gatewayRow.add(new JLabel("URL del gateway:"));
        gatewayRow.add(gatewayField);
        JButton apply = new JButton("Aplicar");
        apply.addActionListener(e -> applyGateway());
        gatewayRow.add(apply);
        JButton health = new JButton("Comprobar gateway y servicios");
        health.addActionListener(e -> checkHealth());
        gatewayRow.add(health);

        JPanel loginRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        loginRow.setBorder(BorderFactory.createTitledBorder("Autenticacion (JWT emitido por login a traves del gateway)"));
        loginRow.add(new JLabel("Usuario o correo:"));
        loginRow.add(identityField);
        loginRow.add(new JLabel("Contrasena:"));
        loginRow.add(passwordField);
        JButton login = new JButton("Login");
        login.addActionListener(e -> login());
        passwordField.addActionListener(e -> login());
        loginRow.add(login);
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> logout());
        loginRow.add(logout);
        loginRow.add(omitJwt);

        sessionLabel.setFont(sessionLabel.getFont().deriveFont(Font.BOLD));
        sessionLabel.setBorder(BorderFactory.createEmptyBorder(2, 6, 4, 6));

        header.add(gatewayRow, BorderLayout.NORTH);
        header.add(loginRow, BorderLayout.CENTER);
        header.add(sessionLabel, BorderLayout.SOUTH);
        return header;
    }

    /** Pestaña: boton Cargar + tabla + detalle JSON del recurso en el gateway. */
    private JPanel resourceTab(String path, boolean protectedRoute, String... preferredColumns) {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        JTextArea detail = new JTextArea(8, 80);
        detail.setEditable(false);
        detail.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton load = new JButton("Cargar");
        JLabel route = new JLabel("GET <gateway>" + path + (protectedRoute
                ? "   (ruta protegida: nginx valida el JWT con auth_request)"
                : "   (ruta publica)"));
        top.add(load);
        top.add(route);

        load.addActionListener(e -> {
            boolean useJwt = protectedRoute && !omitJwt.isSelected();
            if (useJwt && !client.hasToken()) {
                int choice = JOptionPane.showConfirmDialog(this,
                        "No hay sesion. ¿Enviar la peticion sin JWT para ver la respuesta del gateway?",
                        "Ruta protegida", JOptionPane.YES_NO_OPTION);
                if (choice != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            runRequest(() -> client.get(path, useJwt), result -> {
                Object data = result.json();
                detail.setText(Json.pretty(data));
                detail.setCaretPosition(0);
                fillTable(model, data, preferredColumns);
            });
        });

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(table), new JScrollPane(detail));
        split.setResizeWeight(0.7);
        panel.add(top, BorderLayout.NORTH);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    // ==================== ACCIONES ====================

    private void applyGateway() {
        try {
            client.setGatewayUrl(gatewayField.getText());
            gatewayField.setText(client.getGatewayUrl());
            appendLog("Gateway configurado: " + client.getGatewayUrl());
            checkHealth();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Escribe una URL valida, por ejemplo " + DEFAULT_GATEWAY,
                    "Gateway", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void checkHealth() {
        String[] paths = {"/gateway/health", "/api/books/health", "/api/login/health", "/api/users/health",
                "/api/authors/health", "/api/pedidos/health", "/api/pagos/health"};
        new SwingWorker<List<GatewayClient.Result>, Void>() {
            @Override
            protected List<GatewayClient.Result> doInBackground() {
                List<GatewayClient.Result> results = new ArrayList<>();
                for (String path : paths) {
                    results.add(client.get(path, false));
                }
                return results;
            }

            @Override
            protected void done() {
                try {
                    StringBuilder summary = new StringBuilder();
                    for (GatewayClient.Result r : get()) {
                        logResult(r);
                        String state = r.status == 0 ? "SIN RESPUESTA" : (r.ok() ? "OK" : "HTTP " + r.status);
                        summary.append(String.format("%-22s %s%n", r.url.replace(client.getGatewayUrl(), "")
                                .replace("?format=json", ""), state));
                    }
                    JOptionPane.showMessageDialog(LibraryDesktopApp.this, summary.toString(),
                            "Salud via gateway", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    appendLog("Error al comprobar salud: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void login() {
        String identity = identityField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (identity.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Captura usuario/correo y contrasena.");
            return;
        }
        passwordField.setText("");
        String body = "{\"identity\":" + Json.quote(identity) + ",\"password\":" + Json.quote(password) + "}";
        runRequest(() -> client.post("/api/login/login", body, false), result -> {
            Object data = result.json();
            if (!(data instanceof Map) || !(((Map<?, ?>) data).get("access_token") instanceof String)) {
                JOptionPane.showMessageDialog(this, "El servicio no devolvio un JWT.", "Login",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            Map<?, ?> map = (Map<?, ?>) data;
            client.setAccessToken((String) map.get("access_token"));
            updateSession(map);
            JOptionPane.showMessageDialog(this, "Sesion iniciada a traves del Reverse Proxy.");
        });
    }

    private void logout() {
        if (!client.hasToken()) {
            JOptionPane.showMessageDialog(this, "No hay sesion activa.");
            return;
        }
        runRequest(() -> client.post("/api/login/logout", "{}", true), result -> {
            client.setAccessToken(null);
            updateSession(null);
            JOptionPane.showMessageDialog(this, "Sesion cerrada; el JWT quedo revocado.");
        });
    }

    private void updateSession(Map<?, ?> loginData) {
        if (loginData == null) {
            sessionLabel.setText("● Sin sesion: solo las rutas publicas responden 200; las protegidas devuelven 401 en nginx.");
            sessionLabel.setForeground(RED);
            return;
        }
        Object user = loginData.get("user");
        String username = user instanceof Map ? String.valueOf(((Map<?, ?>) user).get("username")) : "?";
        Object role = loginData.get("role_id");
        Object expires = loginData.get("expires_in");
        sessionLabel.setText("● Sesion: " + username + " (rol " + role + ") - JWT en memoria, expira en "
                + expires + " s. Se envia como Authorization: Bearer en las rutas protegidas.");
        sessionLabel.setForeground("admin".equals(role) ? GREEN : AMBER);
    }

    // ==================== PETICIONES ====================

    /** Ejecuta la peticion fuera del hilo de Swing y maneja errores HTTP/red de forma comun. */
    private void runRequest(Supplier<GatewayClient.Result> call, Consumer<GatewayClient.Result> onSuccess) {
        new SwingWorker<GatewayClient.Result, Void>() {
            @Override
            protected GatewayClient.Result doInBackground() {
                return call.get();
            }

            @Override
            protected void done() {
                GatewayClient.Result result;
                try {
                    result = get();
                } catch (Exception ex) {
                    appendLog("Error inesperado: " + ex.getMessage());
                    return;
                }
                logResult(result);
                if (result.networkError != null) {
                    JOptionPane.showMessageDialog(LibraryDesktopApp.this, result.networkError,
                            "Gateway no disponible", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (!result.ok()) {
                    showHttpError(result);
                    return;
                }
                try {
                    onSuccess.accept(result);
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(LibraryDesktopApp.this,
                            "La respuesta no es JSON valido: " + ex.getMessage(), "Respuesta",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void showHttpError(GatewayClient.Result result) {
        String server = "";
        try {
            Object data = result.json();
            if (data instanceof Map) {
                Object message = ((Map<?, ?>) data).get("error");
                if (message == null) message = ((Map<?, ?>) data).get("message");
                if (message != null) server = "\n\nRespuesta: " + message;
            }
        } catch (IllegalArgumentException ignored) {
            // nginx puede responder HTML en errores 502/504
        }
        String friendly;
        switch (result.status) {
            case 401:
                friendly = result.sentJwt
                        ? "El JWT expiro o fue revocado. Inicia sesion de nuevo."
                        : "El gateway rechazo la peticion: falta Authorization: Bearer <JWT>.";
                if (result.sentJwt) {
                    client.setAccessToken(null);
                    updateSession(null);
                }
                break;
            case 403: friendly = "Acceso denegado: tu rol no tiene permiso para esta operacion."; break;
            case 404: friendly = "Ruta no encontrada en el gateway."; break;
            case 429: friendly = "Limite de peticiones de nginx (limit_req) superado. Espera un momento."; break;
            case 502:
            case 504: friendly = "nginx no pudo contactar al microservicio (esta detenido o tardo demasiado)."; break;
            default: friendly = "El gateway respondio con HTTP " + result.status + ".";
        }
        JOptionPane.showMessageDialog(this, friendly + server, "HTTP " + result.status,
                JOptionPane.WARNING_MESSAGE);
    }

    // ==================== TABLA Y LOG ====================

    @SuppressWarnings("unchecked")
    private static void fillTable(DefaultTableModel model, Object data, String... preferred) {
        List<Object> rows = null;
        if (data instanceof List) {
            rows = (List<Object>) data;
        } else if (data instanceof Map) {
            for (Object value : ((Map<String, Object>) data).values()) {
                if (value instanceof List) {
                    rows = (List<Object>) value;
                    break;
                }
            }
            if (rows == null) {
                rows = List.of(data);
            }
        }
        model.setRowCount(0);
        model.setColumnCount(0);
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Set<String> columns = new LinkedHashSet<>(List.of(preferred));
        if (columns.isEmpty()) {
            for (Object row : rows) {
                if (row instanceof Map) {
                    columns.addAll(((Map<String, Object>) row).keySet());
                }
            }
        }
        for (String column : columns) {
            model.addColumn(column);
        }
        for (Object row : rows) {
            if (!(row instanceof Map)) {
                continue;
            }
            Map<String, Object> map = (Map<String, Object>) row;
            Object[] values = new Object[columns.size()];
            int i = 0;
            for (String column : columns) {
                Object value = map.get(column);
                values[i++] = value instanceof Map || value instanceof List
                        ? Json.pretty(value).replaceAll("\\s+", " ") : value;
            }
            model.addRow(values);
        }
    }

    private void logResult(GatewayClient.Result r) {
        String status = r.status == 0 ? "ERR" : String.valueOf(r.status);
        appendLog(String.format("%-4s %-60s HTTP %-3s  X-Request-ID=%s  %4d ms  JWT=%s",
                r.method, r.url, status, r.requestId, r.millis, r.sentJwt ? "si" : "no"));
    }

    private void appendLog(String line) {
        log.append("[" + LocalTime.now().format(CLOCK) + "] " + line + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    // ==================== MAIN ====================

    public static void main(String[] args) {
        String gateway = args.length > 0 ? args[0] : System.getenv("LIBRARY_GATEWAY_URL");
        if (gateway == null || gateway.isBlank()) {
            gateway = DEFAULT_GATEWAY;
        }
        String initial = gateway;
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // se usa el look and feel por defecto
            }
            new LibraryDesktopApp(initial).setVisible(true);
        });
    }
}
