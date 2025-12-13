package com.motorepartidor.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.motorepartidor.entities.Jugador;

public class HUD {

    // === Estado de pedidos (se actualiza desde GameScreen) ===
    private String deliveryStatus1 = "Pedido: ninguno";
    private String deliveryStatus2 = "Pedido: ninguno";
    private boolean p1NearDealer = false, p2NearDealer = false;
    private boolean p1NearDrop = false, p2NearDrop = false;

    public void setDeliveryStatus1(String text) { this.deliveryStatus1 = text; }
    public void setDeliveryStatus2(String text) { this.deliveryStatus2 = text; }
    public void setP1NearDealer(boolean v) { this.p1NearDealer = v; }
    public void setP2NearDealer(boolean v) { this.p2NearDealer = v; }
    public void setP1NearDrop(boolean v) { this.p1NearDrop = v; }
    public void setP2NearDrop(boolean v) { this.p2NearDrop = v; }

    private OrthographicCamera hudCamera;
    private SpriteBatch hudBatch;
    private BitmapFont font;
    private ShapeRenderer shapeRenderer;

    public HUD() {
        hudCamera = new OrthographicCamera();
        hudCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        hudBatch = new SpriteBatch();
        font = new BitmapFont();
        shapeRenderer = new ShapeRenderer();
    }

    public void render(Jugador jugador1, Jugador jugador2, boolean player1InGasArea, boolean player2InGasArea) {
        hudCamera.update();

        // 1) DIBUJAR BARRAS (ShapeRenderer SOLO)
        shapeRenderer.setProjectionMatrix(hudCamera.combined);

        // --- Jugador 1 ---
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        // Vida (fondo y fill)
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.rect(20, Gdx.graphics.getHeight() - 50, 100, 15);
        shapeRenderer.setColor(Color.GREEN);
        shapeRenderer.rect(20, Gdx.graphics.getHeight() - 50, Math.max(0, Math.min(100, jugador1.getVida())), 15);

        // Gasolina (fondo y fill)
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.rect(20, Gdx.graphics.getHeight() - 90, 100, 15);
        shapeRenderer.setColor(Color.ORANGE);
        shapeRenderer.rect(20, Gdx.graphics.getHeight() - 90, Math.max(0, Math.min(100, jugador1.getGasolina())), 15);
        shapeRenderer.end();

        // --- Jugador 2 ---
        float x2 = Gdx.graphics.getWidth() / 2f + 20;
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        // Vida
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.rect(x2, Gdx.graphics.getHeight() - 50, 100, 15);
        shapeRenderer.setColor(Color.GREEN);
        shapeRenderer.rect(x2, Gdx.graphics.getHeight() - 50, Math.max(0, Math.min(100, jugador2.getVida())), 15);

        // Gasolina
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.rect(x2, Gdx.graphics.getHeight() - 90, 100, 15);
        shapeRenderer.setColor(Color.ORANGE);
        shapeRenderer.rect(x2, Gdx.graphics.getHeight() - 90, Math.max(0, Math.min(100, jugador2.getGasolina())), 15);
        shapeRenderer.end();

        // 2) TEXTO (SpriteBatch SOLO)
        hudBatch.setProjectionMatrix(hudCamera.combined);
        hudBatch.begin();
        font.setColor(Color.WHITE);

        // --- P1 textos ---
        font.draw(hudBatch, "Vida:", 20, Gdx.graphics.getHeight() - 20);
        font.draw(hudBatch, "Gasolina: " + (int) jugador1.getGasolina(), 20, Gdx.graphics.getHeight() - 60);
        font.draw(hudBatch, "Dinero: $" + jugador1.getDinero(), 20, Gdx.graphics.getHeight() - 100);

        font.draw(hudBatch, deliveryStatus1, 20, Gdx.graphics.getHeight() - 130);
        int restaj1 = 100 - (int)jugador1.getGasolina();
        if (player1InGasArea) font.draw(hudBatch, "[E] Cargar nafta " + restaj1 + "$", 20, Gdx.graphics.getHeight() - 145);
        if (!deliveryStatus1.toLowerCase().contains("ninguno") && p1NearDrop)
            font.draw(hudBatch, "[G] Entregar", 20, Gdx.graphics.getHeight() - 160);
        else if (deliveryStatus1.toLowerCase().contains("ninguno") && p1NearDealer)
            font.draw(hudBatch, "[G] Aceptar pedido", 20, Gdx.graphics.getHeight() - 160);

        // --- P2 textos ---
        font.draw(hudBatch, "Vida:", x2, Gdx.graphics.getHeight() - 20);
        font.draw(hudBatch, "Gasolina: " + (int) jugador2.getGasolina(), x2, Gdx.graphics.getHeight() - 60);
        font.draw(hudBatch, "Dinero: $" + jugador2.getDinero(), x2, Gdx.graphics.getHeight() - 100);

        font.draw(hudBatch, deliveryStatus2, x2, Gdx.graphics.getHeight() - 130);
        int restaj2 = 100 - (int)jugador2.getGasolina();
        if (player2InGasArea) font.draw(hudBatch, "[P] Cargar nafta " + restaj2 + "$", x2, Gdx.graphics.getHeight() - 145);
        if (!deliveryStatus2.toLowerCase().contains("ninguno") && p2NearDrop)
            font.draw(hudBatch, "[L] Entregar", x2, Gdx.graphics.getHeight() - 160);
        else if (deliveryStatus2.toLowerCase().contains("ninguno") && p2NearDealer)
            font.draw(hudBatch, "[L] Aceptar pedido", x2, Gdx.graphics.getHeight() - 160);

        hudBatch.end();
    }

    public void renderSingle(Jugador player,
                             boolean inGas,
                             boolean nearDealer,
                             boolean nearDrop,
                             String deliveryStatus,
                             int localId) {

        // Actualizar cámara de HUD
        hudCamera.update();

        // ==== BARRAS (vida / gasolina) ====
        shapeRenderer.setProjectionMatrix(hudCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        float xBar = 20f;
        float vidaY = Gdx.graphics.getHeight() - 50;
        float gasY  = Gdx.graphics.getHeight() - 90;

        // Vida
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.rect(xBar, vidaY, 100, 15);
        shapeRenderer.setColor(Color.GREEN);
        shapeRenderer.rect(xBar, vidaY,
            Math.max(0, Math.min(100, player.getVida())),
            15);

        // Gasolina
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.rect(xBar, gasY, 100, 15);
        shapeRenderer.setColor(Color.ORANGE);
        shapeRenderer.rect(xBar, gasY,
            Math.max(0, Math.min(100, player.getGasolina())),
            15);

        shapeRenderer.end();

        // ==== TEXTO ====
        hudBatch.setProjectionMatrix(hudCamera.combined);
        hudBatch.begin();

        float x = 20f;
        float y = Gdx.graphics.getHeight() - 20f;

        font.setColor(Color.WHITE);

        // Vida / gasolina / dinero
        font.draw(hudBatch, "Vida:", x, y);
        y -= 40;
        font.draw(hudBatch, "Gasolina: " + (int) player.getGasolina(), x, y);
        y -= 40;
        font.draw(hudBatch, "Dinero: $" + player.getDinero(), x, y);

        // Estado del pedido
        y -= 40;
        font.draw(hudBatch, deliveryStatus, x, y);

        // Mensajes contextuales
        y -= 20;
        if (nearDealer && deliveryStatus.toLowerCase().contains("ninguno")) {
            String key = "[G]" ;
            font.draw(hudBatch, key + " Aceptar pedido", x, y);
        } else if (nearDrop && !deliveryStatus.toLowerCase().contains("ninguno")) {
            String key ="[G]";
            font.draw(hudBatch, key + " Entregar pedido", x, y);
        }

        // Mensaje de cargar nafta
        if (inGas) {
            y -= 20;
            int resta = 100 - (int) player.getGasolina();
            String key =  "[E]" ;
            font.draw(hudBatch, key + " Cargar nafta " + resta + "$", x, y);
        }

        hudBatch.end();
    }


    public void resize(int width, int height) {
        hudCamera.setToOrtho(false, width, height);
    }

    public void dispose() {
        hudBatch.dispose();
        font.dispose();
        shapeRenderer.dispose();
    }
}
