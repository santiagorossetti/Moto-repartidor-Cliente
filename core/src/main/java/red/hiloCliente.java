package red;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.motorepartidor.GameController;

import java.io.IOException;
import java.net.*;

public class hiloCliente extends Thread {

    // ===== Config =====
    private static final int PORT = 6767;
    private static final String BROADCAST_IP = "255.255.255.255";
    private static final int SOCKET_TIMEOUT_MS = 1000; // receive timeout
    private long lastPingSentMs = 0;
    private static final long PING_INTERVAL_MS = 1000;

    // ===== UDP =====
    private DatagramSocket conexion;
    private InetAddress ipServer;          // arranca en broadcast, luego se reemplaza por IP real del server
    private final int port = PORT;

    // ===== Estado de hilo =====
    private volatile boolean fin = false;

    // ===== Estado de sesión =====
    private volatile int playerId = -1;
    private volatile long lastPongMs = System.currentTimeMillis();

    // ===== Callbacks hacia pantallas =====
    private volatile GameController gameController;

    public hiloCliente() {
        crearSocket();
        resetLocalState();
    }

    private void crearSocket() {
        try {
            conexion = new DatagramSocket();
            conexion.setSoTimeout(SOCKET_TIMEOUT_MS);
            conexion.setBroadcast(true);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    /** Limpia el estado para poder reconectar como si fuera la primera vez. */
    public void resetLocalState() {
        try {
            ipServer = InetAddress.getByName(BROADCAST_IP);
        } catch (UnknownHostException e) {
            // extremadamente raro
            e.printStackTrace();
        }
        playerId = -1;
        lastPongMs = System.currentTimeMillis();
    }

    // =========================================================
    // Lifecycle
    // =========================================================

    @Override
    public void run() {
        while (!fin) {

            long now = System.currentTimeMillis();
            if (playerId != -1 && now - lastPingSentMs >= PING_INTERVAL_MS) {
                enviarPing();
                lastPingSentMs = now;
            }

            byte[] data = new byte[1024];
            DatagramPacket dp = new DatagramPacket(data, data.length);

            try {
                conexion.receive(dp);
                procesarMensaje(dp);
            } catch (SocketTimeoutException ignored) {
                // normal: permite que el hilo "respire" y fin se pueda chequear
            } catch (IOException e) {
                if (!fin) e.printStackTrace();
            }
        }
    }

    /** Cierra el socket y termina el hilo. */
    public void terminarCliente() {
        fin = true;
        try {
            if (conexion != null && !conexion.isClosed()) conexion.close();
        } catch (Exception ignored) {}
        interrupt();
    }

    /**
     * Desconecta del server actual y apaga el hilo.
     * Útil cuando el usuario vuelve al menú o cierra el juego.
     */
    public void desconectar() {
        try {
            if (playerId >= 0) {
                // UDP: repetimos para aumentar probabilidad de llegada
                for (int i = 0; i < 3; i++) enviarMensaje("Disconnect:" + playerId);
            }
        } catch (Exception ignored) {}

        terminarCliente();
        resetLocalState();
    }

    // =========================================================
    // Envío
    // =========================================================

    public void enviarMensaje(String msg) {
        if (conexion == null || conexion.isClosed()) return;
        if (ipServer == null) return;

        byte[] data = msg.getBytes();
        DatagramPacket dp = new DatagramPacket(data, data.length, ipServer, port);

        try {
            conexion.send(dp);
        } catch (IOException e) {
            // si se cortó el socket o algo raro
            if (!fin) e.printStackTrace();
        }
    }

    /** Primer ping/handshake: se manda a broadcast (si no hay server, no pasa nada). */
    public void establecerConexion() {
        enviarMensaje("Conexion");
        System.out.println("Estableciendo conexion...");
    }

    /** Alias más “genérico” si querés usarlo desde BuscarServidorScreen. */


    public void enviarInput(int keycode) {
        if (playerId == -1) return; // todavía no tengo ID
        enviarMensaje("Input:" + playerId + ":" + keycode);
    }

    public void enviarPing() {
        if (playerId == -1) return;
        enviarMensaje("Ping:" + playerId);
    }

    // =========================================================
    // Recepción / Parser
    // =========================================================

    private void procesarMensaje(DatagramPacket dp) {
        // ✅ usar SOLO los bytes reales
        String msg = new String(dp.getData(), 0, dp.getLength()).trim();
        if (msg.isEmpty()) return;

        String[] partes = msg.split(":");
        String head = partes[0];

        // Pong puede llegar muy seguido: lo procesamos rápido
        if ("Pong".equals(head)) {
            lastPongMs = System.currentTimeMillis();
            return;
        }

        // Handshake OK: fijamos IP real del server para dejar de usar broadcast
        if ("OK".equals(msg)) {
            ipServer = dp.getAddress();
            // (Opcional) podrías notificar UI si querés un callback tipo "server encontrado"
            return;
        }

        // Switch clásico (Java 8)
        switch (head) {
            case "ID": {
                if (partes.length >= 2) {
                    playerId = Integer.parseInt(partes[1]);
                    Gdx.app.postRunnable(() -> {
                        if (gameController != null) gameController.onConnected(playerId);
                    });
                }
                break;
            }

            case "Comienza": {
                Gdx.app.postRunnable(() -> {
                    if (gameController != null) gameController.onStartMatch();
                });
                break;
            }

            case "Movimiento": {
                // Movimiento:pos1:pos2:ang1:ang2
                if (partes.length >= 5) moverPersonaje(partes[1], partes[2], partes[3], partes[4]);
                break;
            }

            case "Gas": {
                // Gas:valor:id
                if (partes.length >= 3) actualizarGas(partes[1], partes[2]);
                break;
            }

            case "Dinero": {
                // Dinero:valor:id
                if (partes.length >= 3) actualizarDinero(partes[1], partes[2]);
                break;
            }

            case "Vida": {
                // Vida:valor:id
                if (partes.length >= 3) actualizarVida(partes[1], partes[2]);
                break;
            }

            case "Delivery": {
                // Delivery:x,y,w,h:dangerous:reward:id
                if (partes.length >= 5) actualizarDeliveryDesdeMensaje(partes);
                break;
            }

            case "DeliveryFin": {
                if (partes.length >= 2) terminarDelivery(partes[1]);
                break;
            }

            case "Hint": {
                // Hint:id:tipo
                if (partes.length >= 3) chequearHint(partes[1], partes[2]);
                break;
            }

            case "GasHint": {
                // GasHint:id:0|1
                if (partes.length >= 3) {
                    int id = Integer.parseInt(partes[1]);
                    boolean enGas = "1".equals(partes[2]);
                    Gdx.app.postRunnable(() -> {
                        if (gameController != null) gameController.actualizarGasHint(id, enGas);
                    });
                }
                break;
            }

            case "GameOver": {
                if (partes.length >= 2) onGameover(partes[1]);
                break;
            }

            case "Reset": {
                resetear();
                break;
            }

            case "OpponentLeft": {
                Gdx.app.postRunnable(() -> {
                    if (gameController != null) gameController.onOpponentLeft();
                });
                break;
            }

            default:
                // ignorar mensajes desconocidos
                break;
        }
    }

    // =========================================================
    // Helpers → callbacks en el hilo de LibGDX
    // =========================================================

    private void moverPersonaje(String pos1, String pos2, String angulo1, String angulo2) {
        Vector2 posicion1 = stringToVector2(pos1);
        Vector2 posicion2 = stringToVector2(pos2);
        float ang1 = Float.parseFloat(angulo1);
        float ang2 = Float.parseFloat(angulo2);

        Gdx.app.postRunnable(() -> {
            if (gameController != null)
                gameController.actualizarPosicion(posicion1, posicion2, ang1, ang2);
        });
    }

    private void actualizarGas(String gas, String id) {
        int idPlayer = Integer.parseInt(id);
        float playerGas = Float.parseFloat(gas);

        Gdx.app.postRunnable(() -> {
            if (gameController != null)
                gameController.actualizarGas(playerGas, idPlayer);
        });
    }

    private void actualizarDinero(String dinero, String id) {
        int dineroPlayer = Integer.parseInt(dinero);
        int idPlayer = Integer.parseInt(id);

        Gdx.app.postRunnable(() -> {
            if (gameController != null)
                gameController.actualizarDinero(dineroPlayer, idPlayer);
        });
    }

    private void actualizarVida(String vida, String id) {
        int vidaPlayer = Integer.parseInt(vida);
        int idPlayer = Integer.parseInt(id);

        Gdx.app.postRunnable(() -> {
            if (gameController != null)
                gameController.actualizarVida(vidaPlayer, idPlayer);
        });
    }

    private void chequearHint(String id, String tipo) {
        int idPlayer = Integer.parseInt(id);
        int tipoHint = Integer.parseInt(tipo);

        Gdx.app.postRunnable(() -> {
            if (gameController != null)
                gameController.actualizarHint(idPlayer, tipoHint);
        });
    }

    private void resetear() {
        Gdx.app.postRunnable(() -> {
            if (gameController != null) gameController.onReset();
        });
    }

    private void onGameover(String winnerIndex) {
        int winner = Integer.parseInt(winnerIndex);

        Gdx.app.postRunnable(() -> {
            if (gameController != null) gameController.onGameOver(winner);
        });
    }

    private void terminarDelivery(String id) {
        int idJugador = Integer.parseInt(id);

        Gdx.app.postRunnable(() -> {
            if (gameController != null)
                gameController.actualizarDelivery(null, false, 0, idJugador);
        });
    }

    private void actualizarDeliveryDesdeMensaje(String[] partes) {
        // Delivery:x,y,w,h:dangerous:reward:id
        String[] rectParts = partes[1].split(",");
        float x = Float.parseFloat(rectParts[0]);
        float y = Float.parseFloat(rectParts[1]);
        float w = Float.parseFloat(rectParts[2]);
        float h = Float.parseFloat(rectParts[3]);

        boolean dangerous = "1".equals(partes[2]);
        int reward = Integer.parseInt(partes[3]);
        int idJugador = Integer.parseInt(partes[4]);

        Rectangle target = new Rectangle(x, y, w, h);

        Gdx.app.postRunnable(() -> {
            if (gameController != null)
                gameController.actualizarDelivery(target, dangerous, reward, idJugador);
        });
    }

    // =========================================================
    // Estado / utilidades
    // =========================================================

    public boolean isConnected() {
        return playerId != -1;
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setGameController(GameController gameController) {
        this.gameController = gameController;
    }

    /** true si recibimos Pong dentro del timeout */
    public boolean isServerAlive(long timeoutMs) {
        return (System.currentTimeMillis() - lastPongMs) <= timeoutMs;
    }

    public static Vector2 stringToVector2(String s) {
        s = s.replace("Vector2", "")
            .replace("(", "")
            .replace(")", "")
            .trim();

        String[] parts = s.split(",");
        float x = Float.parseFloat(parts[0]);
        float y = Float.parseFloat(parts[1]);
        return new Vector2(x, y);
    }
}

