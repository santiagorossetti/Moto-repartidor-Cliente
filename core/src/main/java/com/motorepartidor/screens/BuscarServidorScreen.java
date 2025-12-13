package com.motorepartidor.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.motorepartidor.GameController;
import com.motorepartidor.audio.AudioManager;
import red.hiloCliente;

public class BuscarServidorScreen implements Screen, GameController {

    private enum Estado {
        BUSCANDO_SERVIDOR,
        SERVIDOR_ENCONTRADO,
        BUSCANDO_RIVAL,
        COMENZANDO_PARTIDA,
        TIMEOUT
    }

    private final Game game;
    private final AudioManager audio;
    private final hiloCliente cliente;

    private Stage stage;
    private Skin skin;
    private Viewport viewport;

    private Label titleLabel;
    private Label statusLabel;
    private TextButton retryBtn;
    private TextButton exitBtn;

    private Estado estado = Estado.BUSCANDO_SERVIDOR;

    private float stateTime = 0f;     // tiempo dentro del estado
    private float searchTime = 0f;    // tiempo total buscando servidor
    private float pingAccum = 0f;     // para reintentos

    private static final float TIMEOUT_SERVIDOR = 10f;
    private static final float INTERVALO_PING   = 0.5f;

    public BuscarServidorScreen(Game game, AudioManager audio, hiloCliente cliente) {
        this.game = game;
        this.audio = audio;
        this.cliente = cliente;

        // Muy importante: esta screen recibe callbacks de red
        this.cliente.setGameController(this);
    }

    @Override
    public void show() {
        viewport = new FitViewport(1280, 720);
        stage = new Stage(viewport);
        skin = safeLoadSkin();
        Gdx.input.setInputProcessor(stage);

        // UI
        Table root = new Table();
        root.setFillParent(true);
        root.defaults().pad(12);
        stage.addActor(root);

        titleLabel = new Label("Moto Repartidor", skin);
        titleLabel.setFontScale(1.3f);

        statusLabel = new Label("", skin);
        statusLabel.setAlignment(Align.center);
        statusLabel.setFontScale(1.1f);
        statusLabel.setColor(Color.WHITE);

        retryBtn = new TextButton("Volver a intentar", skin);
        exitBtn  = new TextButton("Salir", skin);

        retryBtn.setVisible(false);
        exitBtn.setVisible(false);

        retryBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                // Reinicia búsqueda de servidor
                setEstado(Estado.BUSCANDO_SERVIDOR);
            }
        });

        exitBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                // Salir: desconectar y volver al menú
                try {
                    if (cliente != null) cliente.desconectar(); // implementalo en hiloCliente
                } catch (Throwable ignored) {}
                game.setScreen(new MainMenuScreen(game, audio, null));
            }
        });

        root.add(titleLabel).padBottom(30);
        root.row();

        root.add(statusLabel).width(700).padBottom(20);
        root.row();

        root.add(retryBtn).width(320).height(60);
        root.row();

        root.add(exitBtn).width(320).height(60);

        // Arranca en búsqueda
        setEstado(Estado.BUSCANDO_SERVIDOR);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stateTime += delta;

        switch (estado) {

            case BUSCANDO_SERVIDOR:
                searchTime += delta;
                pingAccum += delta;

                // Reintenta "Conexion" cada 0.5s
                if (pingAccum >= INTERVALO_PING) {
                    pingAccum = 0f;
                    cliente.establecerConexion();
                }

                if (searchTime >= TIMEOUT_SERVIDOR) {
                    setEstado(Estado.TIMEOUT);
                }
                break;

            case SERVIDOR_ENCONTRADO:
                if (stateTime >= 3f) {
                    setEstado(Estado.BUSCANDO_RIVAL);
                }
                break;

            case BUSCANDO_RIVAL:
                // espera mensaje "Comienza" -> onStartMatch()
                break;

            case COMENZANDO_PARTIDA:
                if (stateTime >= 3f) {
                    game.setScreen(new GameScreen(game, audio, cliente));
                }
                break;

            case TIMEOUT:
                // solo UI
                break;
        }

        stage.act(delta);
        stage.draw();
    }


    private void setEstado(Estado nuevo) {
        estado = nuevo;
        stateTime = 0f;
        pingAccum = 0f;

        if (estado == Estado.BUSCANDO_SERVIDOR) {
            searchTime = 0f;
        }

        switch (estado) {
            case BUSCANDO_SERVIDOR:
                statusLabel.setText("Buscando servidor...");
                retryBtn.setVisible(false);
                exitBtn.setVisible(false);
                break;

            case TIMEOUT:
                statusLabel.setText("No se encontró servidor.\n¿Querés volver a intentar?");
                retryBtn.setVisible(true);
                exitBtn.setVisible(true);
                break;

            case SERVIDOR_ENCONTRADO:
                statusLabel.setText("Servidor encontrado ✅");
                retryBtn.setVisible(false);
                exitBtn.setVisible(false);
                break;

            case BUSCANDO_RIVAL:
                statusLabel.setText("Buscando rival...");
                retryBtn.setVisible(false);
                exitBtn.setVisible(true);
                break;

            case COMENZANDO_PARTIDA:
                statusLabel.setText("Comenzando partida...");
                retryBtn.setVisible(false);
                exitBtn.setVisible(false);
                break;
        }
    }

    // ======================
    // Callbacks desde red
    // ======================

    @Override
    public void onConnected(int playerId) {
        // Llega cuando el cliente recibe "ID:x"
        // Si estás buscando servidor o timeout, pasás a SERVIDOR_ENCONTRADO
        if (estado == Estado.BUSCANDO_SERVIDOR || estado == Estado.TIMEOUT) {
            setEstado(Estado.SERVIDOR_ENCONTRADO);
        }
    }

    @Override
    public void onStartMatch() {
        // Llega cuando el cliente recibe "Comienza"
        if (estado == Estado.BUSCANDO_RIVAL || estado == Estado.SERVIDOR_ENCONTRADO || estado == Estado.BUSCANDO_SERVIDOR) {
            setEstado(Estado.COMENZANDO_PARTIDA);
        }
    }

    @Override
    public void onGameOver(int winnerIndex) {
        // En esta screen no aplica (se usa en GameScreen). Lo dejo por compatibilidad.
    }

    @Override
    public void onReset() {
        // No aplica acá
    }

    // ======================
    // Métodos de GameController que acá no usamos
    // ======================

    @Override
    public void enviarInput(int tecla) {
        // En esta pantalla no se envían inputs del juego
    }

    @Override
    public void actualizarPosicion(Vector2 pos1, Vector2 pos2, float ang1, float ang2) {}

    @Override
    public void actualizarGas(float gas, int id) {}

    @Override
    public void actualizarDinero(int dinero, int id) {}

    @Override
    public void actualizarVida(int vida, int id) {}

    @Override
    public void actualizarDelivery(Rectangle target, boolean dangerous, int reward, int id) {}

    @Override
    public void actualizarHint(int id, int tipo) {}

    // ======================
    // Lifecycle
    // ======================

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
    }

    // ================== Skin Seguro ==================
    private Skin safeLoadSkin() {
        try {
            if (Gdx.files.internal("ui/uiskin.json").exists()) {
                return new Skin(Gdx.files.internal("ui/uiskin.json"));
            }
            if (Gdx.files.internal("uiskin.json").exists()) {
                return new Skin(Gdx.files.internal("uiskin.json"));
            }
        } catch (Exception e) {
            Gdx.app.error("BuscarServidor", "Error leyendo uiskin.json, uso fallback", e);
        }
        return buildFallbackSkin();
    }

    private Skin buildFallbackSkin() {
        Skin s = new Skin();
        BitmapFont font = new BitmapFont();
        s.add("default", font);

        Drawable panel = newDrawable(60, 60, 75, 255);
        Drawable accent = newDrawable(80, 160, 255, 255);

        Label.LabelStyle ls = new Label.LabelStyle();
        ls.font = font;
        ls.fontColor = Color.WHITE;
        s.add("default", ls);

        TextButton.TextButtonStyle tbs = new TextButton.TextButtonStyle();
        tbs.up = panel; tbs.down = accent; tbs.over = accent; tbs.font = font;
        s.add("default", tbs);

        return s;
    }

    private Drawable newDrawable(int r, int g, int b, int a) {
        Pixmap pm = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
        pm.setColor(r/255f, g/255f, b/255f, a/255f);
        pm.fill();
        TextureRegionDrawable dr = new TextureRegionDrawable(new TextureRegion(new Texture(pm)));
        pm.dispose();
        return dr;
    }
}
