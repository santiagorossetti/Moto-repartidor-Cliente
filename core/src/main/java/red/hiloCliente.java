package red;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.motorepartidor.GameController;
import com.badlogic.gdx.math.Rectangle;


import java.io.IOException;
import java.net.*;

public class hiloCliente extends Thread {

    private DatagramSocket conexion;
    private InetAddress ipServer;
    private int port = 6767;
    private boolean fin = false;
    public GameController gameController;
    private int playerId = -1;

    public hiloCliente(){
        try {
            ipServer = InetAddress.getByName("255.255.255.255");
            conexion = new DatagramSocket();
        } catch (SocketException | UnknownHostException e){
            e.printStackTrace();
        }
    }


    public void enviarMensaje(String msg) {
        byte[] data = msg.getBytes();
        DatagramPacket dp = new DatagramPacket(data , data.length, ipServer , port);
        try {
            conexion.send(dp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run(){
        do {
            byte[] data = new byte[1024];
            DatagramPacket dp  = new DatagramPacket(data , data.length);
            try {
                conexion.receive(dp);
                procesarMensaje(dp);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }while (!fin);
    }

    public void terminarCliente(){
        this.fin = true;
        conexion.close();
        this.interrupt();
    }

    public void establecerConexion (){

        enviarMensaje("Conexion");
        System.out.println("Estableciendo conexion");
    }

    private void procesarMensaje(DatagramPacket dp) {
        String msg = (new String(dp.getData())).trim();
        String letras [] = msg.split(":");

        if (msg.equals("OK")) {
            ipServer = dp.getAddress();
            comprobarConexion();
        } else if (msg.equals("Comienza")) {

            Gdx.app.postRunnable(() -> {
                if (gameController != null) {
                    gameController.onStartMatch();
                }
            });


        }else if (letras[0].equals("Movimiento")){
            moverPersonaje(letras[1] , letras[2] , letras[3] ,  letras[4]);
        }else if (letras[0].equals("Gas")){

            //System.out.println(msg);
            actualizarGas(letras[1] , letras[2]);

        } else if (letras[0].equals("Dinero")) {

            System.out.println(msg);
            actualizarDinero(letras[1] , letras[2]);

        } else if (letras[0].equals("Vida")) {

            actualizarVida(letras[1] , letras[2]);
        }else if (letras[0].equals("Delivery")) {
            actualizarDeliveryDesdeMensaje(letras);
        }else if (letras[0].equals("DeliveryFin")){
            terminarDelivery(letras[1]);
        } else if (letras[0].equals("Hint")) {

            chequearHint (letras[1] , letras[2]);

        } else if (letras[0].equals("ID")){

            playerId = Integer.parseInt(letras[1]);
            Gdx.app.postRunnable(() -> {
                if (gameController != null) gameController.onConnected(playerId);
            });

        }else if (letras[0].equals("GameOver")) {

            onGameover(letras[1]);

        } else if (msg.equals("Reset")) {
            resetear();
        }

    }


    // Recive el hint del viewport

    private void chequearHint (String id , String tipo) {

        int idPlayer = Integer.parseInt(id);
        int tipoHint = Integer.parseInt(tipo);

        Gdx.app.postRunnable(() -> {
            if (gameController != null) {
                gameController.actualizarHint(idPlayer, tipoHint);
            }
        });

    }

    //resetea el juego

    public void resetear (){
        Gdx.app.postRunnable(() -> {
            if (gameController != null) {
                gameController.onReset();
            }
        });
    }

    public boolean isConnected() {
        return playerId != -1;
    }

    private void onGameover(String winnerIndex){

        int winner = Integer.parseInt(winnerIndex);

        Gdx.app.postRunnable(() -> {
            if (gameController != null) {
                gameController.onGameOver(winner);
            }
        });

    }

    public void desconectar() {
        try {
            // mandalo 2-3 veces por UDP (por si se pierde)
            for (int i = 0; i < 3; i++) {
                enviarMensaje("Disconnect:" + playerId);
            }
        } catch (Exception ignored) {}

        terminarCliente();       // cierra socket y thread
        playerId = -1;           // marcado como no conectado
    }

    public void intentarConexion() {
        enviarMensaje("Conexion");
    }


    private void terminarDelivery(String id) {

        int idJugador = Integer.parseInt(id);
        Gdx.app.postRunnable(() -> {
            if (gameController != null) {
                gameController.actualizarDelivery(null, false, 0, idJugador);
            }
        });
    }

    private void actualizarDeliveryDesdeMensaje(String[] letras) {
        // Formato: Delivery:x,y,w,h:dangerous:reward:id

        String[] rectParts = letras[1].split(",");
        float x = Float.parseFloat(rectParts[0]);
        float y = Float.parseFloat(rectParts[1]);
        float w = Float.parseFloat(rectParts[2]);
        float h = Float.parseFloat(rectParts[3]);

        boolean dangerous = letras[2].equals("1");
        int reward = Integer.parseInt(letras[3]);
        int idJugador = Integer.parseInt(letras[4]);

        Rectangle target = new Rectangle(x, y, w, h);

        Gdx.app.postRunnable(() -> {
            if (gameController != null) {
                gameController.actualizarDelivery(target, dangerous, reward, idJugador);
            }
        });
    }


    private void moverPersonaje(String pos1, String pos2 , String angulo1 , String angulo2) {

        Vector2 posicion1 = stringToVector2(pos1);
        Vector2 posicion2 = stringToVector2(pos2);
        float ang1 = Float.parseFloat(angulo1);
        float ang2 = Float.parseFloat(angulo2);


        Gdx.app.postRunnable(() -> {
            if (gameController != null )
                gameController.actualizarPosicion(posicion1 , posicion2 , ang1 , ang2 );
        });



    }

    public void actualizarGas (String gas , String id){

        int idPlayer = Integer.parseInt(id);
        float playerGas = Float.parseFloat(gas);

        Gdx.app.postRunnable(() -> {
            if (gameController != null )
                gameController.actualizarGas(playerGas , idPlayer);
        });

    }


    public void actualizarDinero (String dinero , String id){

        int dineroPlayer = Integer.parseInt(dinero);
        int idPlayer = Integer.parseInt(id);

        Gdx.app.postRunnable(() -> {
            if (gameController != null )
                gameController.actualizarDinero(dineroPlayer , idPlayer);
        });

    }

    public void actualizarVida(String vida , String id){

        int vidaPlayer = Integer.parseInt(vida);
        int idPlayer = Integer.parseInt(id);

        Gdx.app.postRunnable(() -> {
            if (gameController != null )
                gameController.actualizarVida(vidaPlayer , idPlayer);
        });

    }


    public boolean comprobarConexion(){

        System.out.println("Conectado");
        if (ipServer.equals("255.255.255.255")){
            return true;
        }
        return false;

    }

    public void enviarInput (int keycode){

            if (playerId == -1) return; // todavía no tengo ID
            enviarMensaje("Input:" + playerId + ":" + keycode);


    }



    public static Vector2 stringToVector2(String s) {
        // Limpiar posibles caracteres extra
        s = s.replace("Vector2", "")
            .replace("(", "")
            .replace(")", "")
            .trim();

        // Separar por coma
        String[] parts = s.split(",");

        float x = Float.parseFloat(parts[0]);
        float y = Float.parseFloat(parts[1]);

        return new Vector2(x, y);
    }

    public void setGameController(GameController gameController) {
        this.gameController = gameController;
    }

    public int getPlayerId() {
        return playerId;
    }

}

