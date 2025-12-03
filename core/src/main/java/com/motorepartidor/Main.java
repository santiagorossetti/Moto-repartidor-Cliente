package com.motorepartidor;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.motorepartidor.audio.AudioManager;
import com.motorepartidor.screens.GameScreen;
import com.motorepartidor.screens.MainMenuScreen;
import red.hiloCliente;


/*
  Clase principal del juego que gestiona las diferentes pantallas (menús, juego, etc.).
  Ahora mantiene un único AudioManager compartido.
*/
public class Main extends Game {

    private MainMenuScreen mainMenuScreen;
    private GameScreen gameScreen;

    private AudioManager audio; // AudioManager global
    private int lastWinner = 0;
    hiloCliente cliente = new hiloCliente();




    @Override
    public void create() {
        // Inicializar el AudioManager una sola vez
        audio = new AudioManager();

        cliente.start();


        // Pantalla inicial
        mainMenuScreen = new MainMenuScreen(this, this.audio , cliente);
        setScreen(mainMenuScreen);
    }

    /** Acceso global al AudioManager. */
    public AudioManager getAudio() {
        return audio;
    }

    /** Vuelve al menú principal. */
    public void showMainMenu() {
        if (mainMenuScreen == null) {
            mainMenuScreen = new MainMenuScreen(this, audio , cliente);
        }
        setScreen(mainMenuScreen);
    }

    /** Inicia una nueva partida. */
    public void startGame() {
        if (gameScreen != null) {
            gameScreen.dispose();
        }
        gameScreen = new GameScreen(this, audio , cliente);
        setScreen(gameScreen);
    }

    public void onMatchFinished(int winnerIndex) {
        this.lastWinner = winnerIndex; // 0, 1 o 2

        // Volver al menú principal reutilizando la instancia si existe
        if (mainMenuScreen == null) {
            mainMenuScreen = new MainMenuScreen(this, audio , cliente);
        }

        setScreen(mainMenuScreen);
    }

    public int getLastWinner() {
        return lastWinner;
    }

    @Override
    public void dispose() {
        cliente.terminarCliente();
        super.dispose();
        if (mainMenuScreen != null) mainMenuScreen.dispose();
        if (gameScreen != null) gameScreen.dispose();

        // Liberar el audio global al cerrar el juego
        if (audio != null) {
            try {
                audio.dispose();
            } catch (Exception e) {
                Gdx.app.error("Main", "Error liberando AudioManager", e);
            }
        }
    }
}
