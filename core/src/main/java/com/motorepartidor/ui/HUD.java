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

    public void resize(int width, int height) {
        hudCamera.setToOrtho(false, width, height);
    }

    public void dispose() {
        hudBatch.dispose();
        font.dispose();
        shapeRenderer.dispose();
    }
}
