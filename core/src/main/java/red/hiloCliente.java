package red;

import java.io.IOException;
import java.net.*;

public class hiloCliente extends Thread {

    private DatagramSocket conexion;
    private InetAddress ipServer;
    private int port = 6767;
    private boolean fin = false;

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
        System.out.println(msg);

        if (msg.equals("OK")) {
            ipServer = dp.getAddress();
            comprobarConexion();
        } else if (msg.equals("Comienza")) {

        }
    }

    public boolean comprobarConexion(){

        System.out.println("Conectado");
        if (ipServer.equals("255.255.255.255")){
            return true;
        }
        return false;

    }

    public void enviarInput (int keycode){
        enviarMensaje("Input:" + keycode);
    }

}
