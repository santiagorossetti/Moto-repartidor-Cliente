package com.motorepartidor.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.motorepartidor.GameController;
import com.motorepartidor.Main;
import com.motorepartidor.audio.AudioManager;
import com.motorepartidor.entities.Jugador;
import com.motorepartidor.input.GameInputProcessor;
import com.motorepartidor.ui.DeliveryIndicator;
import com.motorepartidor.ui.HUD;
import com.badlogic.gdx.graphics.Color;
import red.hiloCliente;

public class GameScreen implements Screen, GameController {

    // =========================
    // Constantes / config
    // =========================
    private static final float UNIT_SCALE = 1f / 64f;
    private static final float VIRTUAL_WIDTH = 20f;
    private static final float VIRTUAL_HEIGHT = 15f;

    private static final String DEFAULT_SPRITE_PATH = "sprites/sprite.png";
    private static final String DEFAULT_SPRITE_PATH2 = "sprites/sprite2.png";

    public static final long SERVER_TIMEOUT_MS = 3500;

    // =========================
    // Dependencias
    // =========================
    private final Game game;
    private final AudioManager audio;
    private final hiloCliente cliente;

    // =========================
    // Render
    // =========================
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private Viewport viewport;

    private TiledMap tiledMap;
    private OrthogonalTiledMapRenderer tiledMapRenderer;
    private MapLayer collisionLayer;

    // =========================
    // UI
    // =========================
    private HUD hud;
    private GameInputProcessor inputProcessor;

    private final DeliveryIndicator p1Indicator = new DeliveryIndicator();
    private final DeliveryIndicator p2Indicator = new DeliveryIndicator();

    // =========================
    // Estado local (solo visual)
    // =========================
    private final Jugador[] jugadores = new Jugador[2];

    private final boolean[] nearDealer = new boolean[]{false, false};
    private final boolean[] nearDrop   = new boolean[]{false, false};
    private final boolean[] inGasFromServer = new boolean[]{false, false};

    private static class ActiveDelivery {
        Rectangle target;
        boolean dangerous;
        int reward;
    }
    private ActiveDelivery p1Delivery = null;
    private ActiveDelivery p2Delivery = null;

    private float pingTimer = 0f;

    public GameScreen(Game game, AudioManager audio, hiloCliente cliente) {
        this.game = game;
        this.audio = audio;
        this.cliente = cliente;
        this.cliente.setGameController(this);
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        hud = new HUD();
        camera = new OrthographicCamera();
        viewport = new com.badlogic.gdx.utils.viewport.ExtendViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);

        inputProcessor = new GameInputProcessor(this);
        Gdx.input.setInputProcessor(inputProcessor);

        try {
            if (audio != null) audio.playMusic("audio/song.mp3", true, 0.1f);
        } catch (Exception e) {
            Gdx.app.error("GameScreen", "No se pudo reproducir audio/song.mp3", e);
        }

        tiledMap = new TmxMapLoader().load("map/Map.tmx");
        tiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap, UNIT_SCALE);
        collisionLayer = tiledMap.getLayers().get("colisiones");

        jugadores[0] = new Jugador(DEFAULT_SPRITE_PATH, 18, 36, new Vector2(1700, 500));
        jugadores[1] = new Jugador(DEFAULT_SPRITE_PATH2, 18, 36, new Vector2(1700, 450));

        p1Indicator.setColor(Color.CYAN);
        p2Indicator.setColor(Color.MAGENTA);

        resetVisualState();
    }

    private void resetVisualState() {
        nearDealer[0] = nearDealer[1] = false;
        nearDrop[0]   = nearDrop[1]   = false;
        inGasFromServer[0] = inGasFromServer[1] = false;
        p1Delivery = null;
        p2Delivery = null;
        pingTimer = 0f;
    }

    @Override
    public void render(float delta) {
        // ===== Heartbeat =====
        if (!cliente.isServerAlive(SERVER_TIMEOUT_MS)) {
            onConnectionLost();
            return;
        }

        // ===== Render =====
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        int localId = cliente.getPlayerId();

        if (localId < 0 || localId > 1) {
            camera.position.set(VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT / 2f, 0f);
            camera.update();
            viewport.apply();
            tiledMapRenderer.setView(camera);
            tiledMapRenderer.render();
            return;
        }

        // ===== 1. ACTUALIZAR ANIMACIONES (CRUCIAL) =====
        
        if (jugadores[0] != null) jugadores[0].update(delta);
        if (jugadores[1] != null) jugadores[1].update(delta);

        // ===== Indicadores de delivery =====
        ActiveDelivery localDelivery = (localId == 0) ? p1Delivery : p2Delivery;
        if (localDelivery != null && localDelivery.target != null) {
            float cx = (localDelivery.target.x + localDelivery.target.width * 0.5f) * UNIT_SCALE;
            float cy = (localDelivery.target.y + localDelivery.target.height * 0.5f) * UNIT_SCALE;
            if (localId == 0) p1Indicator.setTarget(cx, cy);
            else p2Indicator.setTarget(cx, cy);
        } else {
            if (localId == 0) p1Indicator.clearTarget();
            else p2Indicator.clearTarget();
        }

        // ===== CÃ¡mara =====
        Jugador localPlayer = jugadores[localId];
        camera.position.set(
            localPlayer.getPosicion().x * UNIT_SCALE,
            localPlayer.getPosicion().y * UNIT_SCALE,
            0f
        );
        camera.update();

        // ===== Mapa =====
        tiledMapRenderer.setView(camera);
        tiledMapRenderer.render();

        // ===== Jugadores =====
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        if (jugadores[0] != null) jugadores[0].dibujar(batch);
        if (jugadores[1] != null) jugadores[1].dibujar(batch);
        batch.end();

        // ===== Indicador =====
        float px = localPlayer.getPosicion().x * UNIT_SCALE;
        float py = localPlayer.getPosicion().y * UNIT_SCALE;
        if (localId == 0) p1Indicator.renderWorld(px, py, camera, delta);
        else p2Indicator.renderWorld(px, py, camera, delta);

        // ===== HUD =====
        boolean localInGas      = inGasFromServer[localId];
        boolean localNearDealer = nearDealer[localId];
        boolean localNearDrop   = nearDrop[localId];

        String deliveryStatus = buildDeliveryStatus(localId);
        hud.renderSingle(localPlayer, localInGas, localNearDealer, localNearDrop, deliveryStatus, localId);

        // ===== Input =====
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new OptionsScreen(game, this, audio));
        }
    }

    private String buildDeliveryStatus(int localId) {
        ActiveDelivery d = (localId == 0) ? p1Delivery : p2Delivery;
        if (d == null) return "Pedido: ninguno";
        return d.dangerous
            ? "Pedido: PELIGROSO $" + d.reward
            : "Pedido: Normal $" + d.reward;
    }

    // =========================
    // GameController (cliente)
    // =========================

    @Override
    public void enviarInput(int tecla) {
        cliente.enviarInput(tecla);
    }

    @Override
    public void actualizarPosicion(Vector2 pos1, Vector2 pos2, float ang1, float ang2) {
        if (jugadores[0] == null || jugadores[1] == null) return;

        jugadores[0].setPosicion(pos1);
        jugadores[1].setPosicion(pos2);

        jugadores[0].getBounds().setPosition(pos1.x, pos1.y);
        jugadores[1].getBounds().setPosition(pos2.x, pos2.y);

        jugadores[0].setAngulo(ang1);
        jugadores[1].setAngulo(ang2);
    }

    @Override public void actualizarGas(float gas, int id) {
        if (id < 0 || id > 1 || jugadores[id] == null) return;
        jugadores[id].setGasolina(gas);
    }

    @Override public void actualizarDinero(int dinero, int id) {
        if (id < 0 || id > 1 || jugadores[id] == null) return;
        jugadores[id].setDinero(dinero);
    }

    @Override public void actualizarVida(int vida, int id) {
        if (id < 0 || id > 1 || jugadores[id] == null) return;
        jugadores[id].setVida(vida);
    }

    @Override
    public void actualizarDelivery(Rectangle target, boolean dangerous, int reward, int id) {
        if (id < 0 || id > 1) return;

        if (target == null) {
            if (id == 0) p1Delivery = null;
            else p2Delivery = null;
            return;
        }

        ActiveDelivery d = new ActiveDelivery();
        d.target = target;
        d.dangerous = dangerous;
        d.reward = reward;

        if (id == 0) p1Delivery = d;
        else p2Delivery = d;
    }

    @Override
    public void actualizarHint(int id, int tipo) {
        if (id < 0 || id > 1) return;
        nearDealer[id] = (tipo == 1);
        nearDrop[id]   = (tipo == 2);
    }

    @Override
    public void actualizarGasHint(int idJugador, boolean enGas) {
        if (idJugador < 0 || idJugador > 1) return;
        inGasFromServer[idJugador] = enGas;
    }

    @Override
    public void onGameOver(int winnerIndex) {
        try { if (audio != null) audio.stopMusic(); } catch (Exception ignored) {}
        try { if (cliente != null) cliente.desconectar(); } catch (Throwable ignored) {}

        if (game instanceof Main) ((Main) game).onMatchFinished(winnerIndex);
        else game.setScreen(new MainMenuScreen(game, audio, cliente));
    }

    @Override
    public void onReset() {
        p1Delivery = null;
        p2Delivery = null;
        nearDealer[0] = nearDealer[1] = false;
        nearDrop[0]   = nearDrop[1]   = false;

        jugadores[0].setVida(100);
        jugadores[1].setVida(100);
        jugadores[0].setDinero(0);
        jugadores[1].setDinero(0);
        jugadores[0].setGasolina(100);
        jugadores[1].setGasolina(100);

        jugadores[0].setPosicion(new Vector2(1700, 500));
        jugadores[1].setPosicion(new Vector2(1700, 450));
        jugadores[0].setAngulo(0);
        jugadores[1].setAngulo(0);
    }

    @Override
    public void onConnectionLost() {
        try { if (cliente != null) cliente.desconectar(); } catch (Throwable ignored) {}
        game.setScreen(new MensajeScreen(game, audio, "ConexiÃ³n perdida", "Volver al menÃº"));
    }

    @Override
    public void onOpponentLeft() {
        try { if (cliente != null) cliente.desconectar(); } catch (Throwable ignored) {}
        game.setScreen(new MensajeScreen(game, audio, "El oponente ha abandonado", "Volver al menÃº"));
    }

    @Override public void onConnected(int playerId) {}
    @Override public void onStartMatch() {}

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        hud.resize(width, height);
    }

    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (tiledMapRenderer != null) tiledMapRenderer.dispose();
        if (tiledMap != null) tiledMap.dispose();

        for (int i = 0; i < jugadores.length; i++) {
            if (jugadores[i] != null) jugadores[i].dispose();
        }

        p1Indicator.dispose();
        p2Indicator.dispose();

        if (hud != null) hud.dispose();


    }
}
