package com.motorepartidor.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.motorepartidor.Main;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.motorepartidor.Main;
import com.motorepartidor.audio.AudioManager;
import com.motorepartidor.entities.Jugador;
import com.motorepartidor.entities.components.PlayerController;
import com.motorepartidor.input.GameInputProcessor;
import com.motorepartidor.ui.HUD;
import com.motorepartidor.ui.DeliveryIndicator;
import com.badlogic.gdx.graphics.Color;
import jdk.internal.org.jline.terminal.TerminalBuilder;
import red.hiloCliente;

public class GameScreen implements Screen {

    private Game game;
    private boolean initialized = false;
    private String chosenSpritePath;
    private String chosenSpritePath2;

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;

    private OrthographicCamera camera1;
    private Viewport viewport1;
    private OrthographicCamera camera2;
    private Viewport viewport2;

    private HUD hud;

    private TiledMap tiledMap;
    private OrthogonalTiledMapRenderer tiledMapRenderer;
    private MapLayer collisionLayer;
    private MapLayer gasLayer;
    private MapLayer dealerLayer;
    private MapLayer entregasLayer;
    private final List<Rectangle> dealerAreas = new ArrayList<>();
    private final List<Rectangle> entregaAreas = new ArrayList<>();
    public hiloCliente cliente;



    private AudioManager audio;

    // Jugadores como array
    private Jugador[] jugadores = new Jugador[2];
    private final boolean[] playerInGasArea = new boolean[2];
    private final boolean[] nearDealer = new boolean[2];
    private final boolean[] nearDrop = new boolean[2];
    private boolean playersAreColliding = false;
    private boolean[] playerIsCollidingObstacle = new boolean[2];

    private GameInputProcessor inputProcessor;

    private boolean eKeyHandled = false;
    private boolean pKeyHandled = false;
    private boolean gKeyHandled = false, lKeyHandled = false;

    private static final String DEFAULT_SPRITE_PATH = "sprites/sprite.png";
    private static final String DEFAULT_SPRITE_PATH2 = "sprites/sprite2.png";



    private DeliveryIndicator p1Indicator = new DeliveryIndicator();
    private DeliveryIndicator p2Indicator = new DeliveryIndicator();

    private static final float UNIT_SCALE = 1 / 64f;
    private static final float VIRTUAL_WIDTH = 20f;
    private static final float VIRTUAL_HEIGHT = 15f;

    // Estado por jugador
    private static class ActiveDelivery {
        Rectangle target;
        boolean dangerous;
        int reward;
    }
    private ActiveDelivery p1Delivery = null;
    private ActiveDelivery p2Delivery = null;




    private final Random rng = new Random();

    public GameScreen(Game game, AudioManager audio , hiloCliente cliente) {
        this.game = game;
        this.audio = audio;
        this.chosenSpritePath = DEFAULT_SPRITE_PATH;
        this.chosenSpritePath2 = DEFAULT_SPRITE_PATH2;
        this.cliente = cliente;
    }

    @Override
    public void show() {
        // 1. Si ya inicializamos todo, solo reactivamos el Input y salimos.
        if (initialized) {
            Gdx.input.setInputProcessor(inputProcessor);
            // Opcional: reiniciar música si se detuvo en hide()
            return;
        }

        // ESTO SOLO SE EJECUTA LA PRIMERA VEZ:

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        hud = new HUD();

        // === AUDIO: usar AudioManager global (reemplaza Music local) ===
        try {
            if (this.audio != null) {
                this.audio.playMusic("audio/song.mp3", true, 0.1f);
            }
        } catch (Exception e) {
            Gdx.app.error("GameScreen", "Error al reproducir música de juego: audio/song.mp3", e);
        }

        tiledMap = new TmxMapLoader().load("map/Map.tmx");
        tiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap, UNIT_SCALE);

        collisionLayer = tiledMap.getLayers().get("colisiones");
        if (collisionLayer == null) {
            Gdx.app.error("GameScreen", "¡ERROR! Capa 'colisiones' no encontrada en el mapa.");
        }

        gasLayer = tiledMap.getLayers().get("Gasolina");
        if (gasLayer == null) {
            Gdx.app.error("GameScreen", "¡ERROR! Capa 'Gasolina' no encontrada en el mapa.");
        }

        // Ahora se crea el Jugador después de que las capas del mapa están cargadas.
        jugadores[0] = new Jugador(chosenSpritePath, 18, 36, new Vector2(1700, 500), collisionLayer);
        jugadores[1] = new Jugador(chosenSpritePath2, 18, 36, new Vector2(1700, 450), collisionLayer);

        camera1 = new OrthographicCamera();
        viewport1 = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera1);

        camera2 = new OrthographicCamera();
        viewport2 = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera2);

        inputProcessor = new GameInputProcessor();
        Gdx.input.setInputProcessor(inputProcessor);


        dealerLayer = tiledMap.getLayers().get("dealer");
        if (dealerLayer == null) {
            Gdx.app.error("GameScreen", "¡ERROR! Capa 'dealer' no encontrada en el mapa.");
        } else {
            for (MapObject obj : dealerLayer.getObjects()) {
                if (obj instanceof RectangleMapObject) {
                    dealerAreas.add(((RectangleMapObject) obj).getRectangle());
                }
            }
        }

        entregasLayer = tiledMap.getLayers().get("entregas");
        if (entregasLayer == null) {
            Gdx.app.error("GameScreen", "¡ERROR! Capa 'entregas' no encontrada en el mapa.");
        } else {
            for (MapObject obj : entregasLayer.getObjects()) {
                    if (obj instanceof RectangleMapObject) {
                    entregaAreas.add(((RectangleMapObject) obj).getRectangle());
                }
            }
        }

        p1Indicator.setColor(Color.CYAN);
        p2Indicator.setColor(Color.MAGENTA);

        // 2. Marcamos como inicializado para evitar que se ejecute de nuevo.
        initialized = true;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // --- GASOLINA / INPUT ---
            playerInGasArea[0] = checkPlayerInGasArea(jugadores[0]);
            playerInGasArea[1] = checkPlayerInGasArea(jugadores[1]);


        if (playerInGasArea[0] && inputProcessor.isEPressed() && !eKeyHandled) {
            int restaj1 = 100 - (int)jugadores[0].getGasolina();
            jugadores[0].restarDinero(restaj1);
            jugadores[0].recargarGasolina(100);
            eKeyHandled = true;
            if (this.audio != null) this.audio.playSound("audio/refuel.wav", 1.0f);
            Gdx.app.log("GameScreen", "¡Jugador 1 recargó gasolina!");
        } else if (!inputProcessor.isEPressed()) {
            eKeyHandled = false;
        }

        if (playerInGasArea[1] && inputProcessor.isPPressed() && !pKeyHandled) {
            int restaj2 = 100 - (int)jugadores[1].getGasolina();
            jugadores[1].restarDinero(restaj2);
            jugadores[1].recargarGasolina(100);
            pKeyHandled = true;
            if (this.audio != null) this.audio.playSound("audio/refuel.wav", 1.0f);
            Gdx.app.log("GameScreen", "¡Jugador 2 recargó gasolina!");
        } else if (!inputProcessor.isPPressed()) {
            pKeyHandled = false;
        }

        // --- PROXIMIDAD A ZONAS ---
        nearDealer[0] = isInAny(jugadores[0],dealerAreas);
        nearDealer[1] = isInAny(jugadores[1],dealerAreas);


        nearDrop[0]   = (p1Delivery != null) && jugadores[0].getBounds().overlaps(p1Delivery.target);
        nearDrop[1]   = (p2Delivery != null) && jugadores[1].getBounds().overlaps(p2Delivery.target);

        // --- ACEPTAR / ENTREGAR: JUGADOR 1 (G) ---
        if (inputProcessor.isGPressed()) {
            if (!gKeyHandled) {
                if (p1Delivery == null && nearDealer[0]) {
                    p1Delivery = createDelivery();
                    if (p1Delivery != null) {
                        if (this.audio != null) this.audio.playSound("audio/pickup.wav", 1f);
                        Gdx.app.log("GameScreen", p1Delivery.dangerous ? "P1 tomó pedido PELIGROSO" : "P1 tomó pedido");
                    }
                } else if (p1Delivery != null && nearDrop[0]) {
                    jugadores[0].sumarDinero(p1Delivery.reward);
                    if (this.audio != null) this.audio.playSound("audio/deliver.wav", 1f);
                    Gdx.app.log("GameScreen", "P1 entregó pedido. +$" + p1Delivery.reward);
                    p1Delivery = null;
                }
                gKeyHandled = true;
            }
        } else {
            gKeyHandled = false;
        }

        // --- ACEPTAR / ENTREGAR: JUGADOR 2 (L) ---
        if (inputProcessor.isLPressed()) {
            if (!lKeyHandled) {
                if (p2Delivery == null && nearDealer[1]) {
                    p2Delivery = createDelivery();
                    if (p2Delivery != null) {
                        if (this.audio != null) this.audio.playSound("audio/pickup.wav", 1f);
                        Gdx.app.log("GameScreen", p2Delivery.dangerous ? "P2 tomó pedido PELIGROSO" : "P2 tomó pedido");
                    }
                } else if (p2Delivery != null && nearDrop[1]) {
                    jugadores[1].sumarDinero(p2Delivery.reward);
                    if (this.audio != null) this.audio.playSound("audio/deliver.wav", 1f);
                    Gdx.app.log("GameScreen", "P2 entregó pedido. +$" + p2Delivery.reward);
                    p2Delivery = null;
                }
                lKeyHandled = true;
            }
        } else {
            lKeyHandled = false;
        }

        // Centro del destino -> ESCALADO a mundo
        if (p1Delivery != null && p1Delivery.target != null) {
            float cx = (p1Delivery.target.x + p1Delivery.target.width  * 0.5f) * UNIT_SCALE;
            float cy = (p1Delivery.target.y + p1Delivery.target.height * 0.5f) * UNIT_SCALE;
            p1Indicator.setTarget(cx, cy);
        } else {
            p1Indicator.clearTarget();
        }

        if (p2Delivery != null && p2Delivery.target != null) {
            float cx = (p2Delivery.target.x + p2Delivery.target.width  * 0.5f) * UNIT_SCALE;
            float cy = (p2Delivery.target.y + p2Delivery.target.height * 0.5f) * UNIT_SCALE;
            p2Indicator.setTarget(cx, cy);
        } else {
            p2Indicator.clearTarget();
        }

        // --- UPDATE JUGADORES ---
        jugadores[0].update(new PlayerController.PlayerInput() {
            @Override public boolean accelerate() { return inputProcessor.isUpPressed(); }
            @Override public boolean brake() { return inputProcessor.isDownPressed(); }
            @Override public boolean turnLeft() { return inputProcessor.isLeftPressed(); }
            @Override public boolean turnRight() { return inputProcessor.isRightPressed(); }
        }, delta);
        jugadores[1].update(new PlayerController.PlayerInput() {
            @Override public boolean accelerate() { return inputProcessor.isArrowUpPressed(); }
            @Override public boolean brake() { return inputProcessor.isArrowDownPressed(); }
            @Override public boolean turnLeft() { return inputProcessor.isArrowLeftPressed(); }
            @Override public boolean turnRight() { return inputProcessor.isArrowRightPressed(); }
        }, delta);


        // --- COLISIONES / DAÑO ---
        boolean[] playerCollidingWithObstacle = new boolean[2];
        for(int i = 0; i < jugadores.length; i++) {
            playerCollidingWithObstacle[i] = checkPolygonCollisions(jugadores[i].getPolygon());
            if(playerCollidingWithObstacle[i] && !playerIsCollidingObstacle[i]) {
                jugadores[i].restarVida(1);
                playerIsCollidingObstacle[i] = true;
            }
            else if (!playerCollidingWithObstacle[i]) {
                playerIsCollidingObstacle[i] = false;
            }
        }

        if (Intersector.overlapConvexPolygons(jugadores[0].getPolygon(), jugadores[1].getPolygon())) {
            if (!playersAreColliding) {
                Gdx.app.log("GameScreen", "¡Colisión entre jugadores detectada!");
                jugadores[0].restarVida(10);
                jugadores[1].restarVida(10);
                playersAreColliding = true;
            }
        } else {
            playersAreColliding = false;
        }

        checkEndMatch();



        // --- RENDER SPLIT-SCREEN ---
        int halfW = Gdx.graphics.getWidth() / 2;
        int h = Gdx.graphics.getHeight();

        // ===== VIEWPORT IZQUIERDO (P1) =====
        Gdx.gl.glViewport(0, 0, halfW, h);

        // cámara 1 sigue a P1
        camera1.position.set(
            jugadores[0].getPosicion().x * UNIT_SCALE,
            jugadores[0].getPosicion().y * UNIT_SCALE,
            0f
        );
        camera1.update();

        tiledMapRenderer.setView(camera1);
        tiledMapRenderer.render();

        batch.setProjectionMatrix(camera1.combined);
        batch.begin();
        // si querés solo P1 acá, podés dejar solo jugador1
        jugadores[0].dibujar(batch);
        jugadores[1].dibujar(batch);
        batch.end();

        // Indicador P1 en su mitad
        float p1x = (jugadores[0].getBounds().x + jugadores[0].getBounds().width  * 1.5f) * UNIT_SCALE;
        float p1y = (jugadores[0].getBounds().y + jugadores[0].getBounds().height * 1.5f) * UNIT_SCALE;
        p1Indicator.renderWorld(p1x, p1y, camera1, delta);




        // ===== VIEWPORT DERECHO (P2) =====
        Gdx.gl.glViewport(halfW, 0, halfW, h);

        // cámara 2 sigue a P2
        camera2.position.set(
            jugadores[1].getPosicion().x * UNIT_SCALE,
            jugadores[1].getPosicion().y * UNIT_SCALE,
            0f
        );
        camera2.update();

        tiledMapRenderer.setView(camera2);
        tiledMapRenderer.render();

        batch.setProjectionMatrix(camera2.combined);
        batch.begin();
        // si querés solo P2 acá, podés dejar solo jugador2
        jugadores[0].dibujar(batch);
        jugadores[1].dibujar(batch);
        batch.end();

        // Indicador P2 en su mitad
        float p2x = (jugadores[1].getBounds().x + jugadores[1].getBounds().width  * 1.5f) * UNIT_SCALE;
        float p2y = (jugadores[1].getBounds().y + jugadores[1].getBounds().height * 1.5f) * UNIT_SCALE;
        p2Indicator.renderWorld(p2x, p2y, camera2, delta);

        // ===== VOLVER A PANTALLA COMPLETA (UI / DIVISOR) =====
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Línea roja central
        shapeRenderer.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(1, 0, 0, 1);
        shapeRenderer.rect(Gdx.graphics.getWidth() / 2f - 2, 0, 4, Gdx.graphics.getHeight());
        shapeRenderer.end();

        // --- HUD ---
        hud.setP1NearDealer(nearDealer[0]);
        hud.setP2NearDealer(nearDealer[1]);
        hud.setP1NearDrop(nearDrop[0]);
        hud.setP2NearDrop(nearDrop[1]);
        hud.setDeliveryStatus1(
            (p1Delivery == null) ? "Pedido: ninguno"
                : (p1Delivery.dangerous ? "Pedido: PELIGROSO $" + p1Delivery.reward
                : "Pedido: Normal $" + p1Delivery.reward)
        );
        hud.setDeliveryStatus2(
            (p2Delivery == null) ? "Pedido: ninguno"
                : (p2Delivery.dangerous ? "Pedido: PELIGROSO $" + p2Delivery.reward
                : "Pedido: Normal $" + p2Delivery.reward)
        );
        hud.render(jugadores[0], jugadores[1], playerInGasArea[0], playerInGasArea[1]);

        // --- ESC → Options ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new OptionsScreen(game, this, audio));
        }
    }


    private boolean checkPlayerInGasArea(Jugador jugador) {
        if (gasLayer == null) return false;

        for (MapObject object : gasLayer.getObjects()) {
            if (object instanceof RectangleMapObject) {
                Rectangle gasRect = ((RectangleMapObject) object).getRectangle();
                if (jugador.getBounds().overlaps(gasRect)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkPolygonCollisions(Polygon polygon) {
        if (collisionLayer == null) return false;

        float[] playerVertices = polygon.getTransformedVertices();
        for (MapObject object : collisionLayer.getObjects()) {
            if (object instanceof RectangleMapObject) {
                Rectangle collisionRect = ((RectangleMapObject) object).getRectangle();

                if (!polygon.getBoundingRectangle().overlaps(collisionRect)) continue;

                for (int i = 0; i < playerVertices.length; i += 2) {
                    if (collisionRect.contains(playerVertices[i], playerVertices[i + 1])) return true;
                }

                float rectX = collisionRect.x;
                float rectY = collisionRect.y;
                float rectWidth = collisionRect.width;
                float rectHeight = collisionRect.height;

                if (polygon.contains(rectX, rectY) ||
                    polygon.contains(rectX + rectWidth, rectY) ||
                    polygon.contains(rectX + rectWidth, rectY + rectHeight) ||
                    polygon.contains(rectX, rectY + rectHeight)) return true;
            }
        }
        return false;
    }

    private boolean isInAny(Jugador jugador, List<Rectangle> zonas) {
        Rectangle b = jugador.getBounds();
        for (Rectangle r : zonas) {
            if (b.overlaps(r)) return true;
        }
        return false;
    }

    private Rectangle randomEntrega() {
        if (entregaAreas.isEmpty()) return null;
        return entregaAreas.get(rng.nextInt(entregaAreas.size()));
    }

    private ActiveDelivery createDelivery() {
        ActiveDelivery d = new ActiveDelivery();
        d.target = randomEntrega();
        if (d.target == null) return null;
        d.dangerous = rng.nextFloat() < 0.25f; // 25% peligroso
        d.reward = d.dangerous ? 130 : 65;
        return d;
    }

    private void checkEndMatch() {
        if (jugadores == null || jugadores.length < 2) return;

        int vida1 = jugadores[0].getVida();
        int vida2 = jugadores[1].getVida();
        int plata1 = jugadores[0].getDinero();
        int plata2 = jugadores[1].getDinero();

        // --- 1. Condición para SEGUIR JUGANDO (Guard Clause) ---
        // Si ambos siguen vivos Y ninguno llegó a 1000$, no hacemos nada.
        if ((vida1 > 0 && vida2 > 0) && (plata1 < 1000 && plata2 < 1000)) {
            return;
        }

        // --- 2. Si salimos del 'if' anterior, la partida TERMINÓ ---
        // Ahora determinamos el resultado usando una estructura if-else if-else
        // para asegurar que solo se elija UN resultado.

        int winnerIndex;

        // --- 3. Condición de EMPATE (Máxima prioridad) ---
        // Ambos mueren AL MISMO TIEMPO
        // O ambos llegan a 1000 AL MISMO TIEMPO.
        if ((vida1 <= 0 && vida2 <= 0) || (plata1 >= 1000 && plata2 >= 1000)) {
            winnerIndex = 3; // 3 = Empate
        }

        // --- 4. Condición de Victoria JUGADOR 2 (Segunda prioridad) ---
        // J1 murió (y J2 no) O J2 llegó a 1000 (y J1 no).
        else if (vida1 <= 0 || plata2 >= 1000) {
            winnerIndex = 2; // 2 = Gana Jugador 2
        }

        // --- 5. Condición de Victoria JUGADOR 1 (Última opción) ---
        // Si no es empate Y no ganó J2, J1 debe haber ganado.
        // (J2 murió O J1 llegó a 1000)
        else {
            // Esto cubre los casos restantes:
            // (vida2 <= 0 || plata1 >= 1000)
            winnerIndex = 1; // 1 = Gana Jugador 1
        }

        // --- 6. Finalizar la partida ---

        // Parar música del juego
        if (audio != null) {
            try {
                audio.stopMusic();
            } catch (Exception ignored) {}
        }

        // Llamar a la pantalla de resultado
        if (game instanceof Main) {
            ((Main) game).onMatchFinished(winnerIndex);
        } else {
            game.setScreen(new MainMenuScreen(game, audio , cliente));
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport1.update(width / 2, height, false);
        viewport2.update(width / 2, height, false);
        hud.resize(width, height);
    }

    @Override public void pause() { }
    @Override public void resume() { }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
        // Opcional: podés parar la música al salir de la pantalla si querés:
        // if (game.getAudio() != null) game.getAudio().stopMusic();
    }

    @Override
    public void dispose() {
        batch.dispose();
        tiledMap.dispose();
        tiledMapRenderer.dispose();
        for(int i = 0; i < jugadores.length; i++) {
            jugadores[i].dispose();
        }
        shapeRenderer.dispose();
        p1Indicator.dispose();
        p2Indicator.dispose();
        hud.dispose();
    }
}
