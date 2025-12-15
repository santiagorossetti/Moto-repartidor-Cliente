package com.motorepartidor;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.motorepartidor.audio.AudioManager;
import com.motorepartidor.screens.GameScreen;
import com.motorepartidor.screens.MainMenuScreen;
import red.hiloCliente;


public class Main extends Game {

    private MainMenuScreen mainMenuScreen;
    private GameScreen gameScreen;

    // Recursos globales
    private AudioManager audio;
    private hiloCliente cliente;

    // Resultado de la última partida (0 = ninguno / empate)
    private int lastWinner = 0;



    @Override
    public void create() {

        //  Audio global
        audio = new AudioManager();

        //  Cliente de red (único durante toda la app)
        cliente = new hiloCliente();
        cliente.setDaemon(true); // no bloquea cierre de la JVM
        cliente.start();

         // Pantalla inicial
        mainMenuScreen = new MainMenuScreen(this, audio, cliente);
        setScreen(mainMenuScreen);
    }


    // Navegación



     // Vuelve al menú principal.

    public void showMainMenu() {
        if (mainMenuScreen == null) {
            mainMenuScreen = new MainMenuScreen(this, audio, cliente);
        }
        setScreen(mainMenuScreen);
    }


     //Inicia una nueva partida.

    public void startGame() {

        // No forzamos dispose acá: LibGDX se encarga del ciclo de vida
        gameScreen = new GameScreen(this, audio, cliente);
        setScreen(gameScreen);
    }


    public void onMatchFinished(int winnerIndex) {
        this.lastWinner = winnerIndex;
        showMainMenu();
    }


    // Accesores


    public AudioManager getAudio() {
        return audio;
    }

    public int getLastWinner() {
        return lastWinner;
    }

    public hiloCliente getCliente() {
        return cliente;
    }

    // =========================================================
    // Cleanup final
    // =========================================================
    @Override
    public void dispose() {
        if (cliente != null) {
            cliente.desconectar();
            try { cliente.join(1500); } catch (InterruptedException ignored) {}
            cliente = null;
        }

        if (audio != null) {
            try { audio.dispose(); } catch (Exception e) { Gdx.app.error("Main", "Error liberando AudioManager", e); }
            audio = null;
        }

        super.dispose();
    }


}
