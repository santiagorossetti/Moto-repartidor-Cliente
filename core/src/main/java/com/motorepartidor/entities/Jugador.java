package com.motorepartidor.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.motorepartidor.entities.components.PlayerController;
import com.motorepartidor.entities.components.PlayerRenderer;

public class Jugador {
    private Texture textura;
    private TextureRegion[] frames;
    private float velocidad = 0f;
    private Vector2 posicion;
    private float angulo;

    private Rectangle bounds;
    private Polygon polygon;

    private int vida;
    private static final int VIDA_MAXIMA = 100;

    private float gasolina;
    private static final float GASOLINA_MAXIMA = 100;
    private int dinero;

    private PlayerController controller;
    private PlayerRenderer renderer;

    public Jugador(String texturaPath, int frameWidth, int frameHeight, Vector2 posicionInicial, MapLayer collisionLayer) {
        try {
            this.textura = new Texture(Gdx.files.internal(texturaPath));

            TextureRegion[][] temp = TextureRegion.split(this.textura, frameWidth, frameHeight);
            if (temp != null && temp.length > 0 && temp[0] != null) {
                int numFrames = temp[0].length;
                this.frames = new TextureRegion[numFrames];
                for (int i = 0; i < numFrames; i++) {
                    this.frames[i] = temp[0][i];
                }
            } else {
                Gdx.app.error("Jugador", "¡ERROR! No se pudieron generar frames. Revisa frameWidth/frameHeight.");
                this.frames = new TextureRegion[]{new TextureRegion(this.textura)};
            }
        } catch (Exception e) {
            Gdx.app.error("Jugador", "¡ERROR CRÍTICO! No se pudo cargar textura: " + texturaPath, e);
            this.textura = new Texture(Gdx.files.internal("badlogic.jpg"));
            this.frames = new TextureRegion[]{new TextureRegion(this.textura)};
        }

        this.posicion = posicionInicial;
        this.angulo = 0;
        this.vida = VIDA_MAXIMA;
        this.gasolina = GASOLINA_MAXIMA;
        this.dinero = 100;

        this.bounds = new Rectangle(posicion.x, posicion.y, frameWidth, frameHeight);
        float[] vertices = new float[8];
        vertices[0] = 0; vertices[1] = 0;
        vertices[2] = frameWidth; vertices[3] = 0;
        vertices[4] = frameWidth; vertices[5] = frameHeight;
        vertices[6] = 0; vertices[7] = frameHeight;
        this.polygon = new Polygon(vertices);
        this.polygon.setOrigin(frameWidth / 2f, frameHeight / 2f);
        this.polygon.setPosition(posicionInicial.x, posicionInicial.y);

        this.controller = new PlayerController(collisionLayer);
        this.renderer = new PlayerRenderer(this);
    }

    public void update(PlayerController.PlayerInput input, float deltaTime) {
        controller.update(this, input, deltaTime);
        renderer.update(deltaTime);

        polygon.setPosition(posicion.x, posicion.y);
        bounds.setPosition(posicion.x, posicion.y);
    }

    public void dibujar(Batch batch) {
        renderer.render(batch);
    }

    public void dispose() {
        if (textura != null) {
            textura.dispose();
        }
    }

    public Texture getTextura() {
        return textura;
    }

    public TextureRegion[] getFrames() {
        return frames;
    }

    public void setAngulo(float angulo) {
        this.angulo = angulo;
    }

    public float getAngulo() {
        return this.angulo;
    }

    public Vector2 getPosicion() {
        return posicion;
    }

    public float getVelocidad() {
        return velocidad;
    }

    public void setVelocidad(float velocidad) {
        this.velocidad = velocidad;
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public Polygon getPolygon() {
        return polygon;
    }

    public int getVida() {
        return vida;
    }

    public void setVida(int vida) {
        this.vida = Math.max(0, Math.min(vida, VIDA_MAXIMA));
    }

    public void restarVida(int cantidad) {
        this.vida = Math.max(0, this.vida - cantidad);
    }

    public float getGasolina() {
        return gasolina;
    }

    public void gastarGasolina(float cantidad) {
        gasolina -= cantidad;
        if (gasolina < 0) gasolina = 0;
    }

    public void recargarGasolina(float cantidad) {
        gasolina += cantidad;
        if (gasolina > GASOLINA_MAXIMA) gasolina = GASOLINA_MAXIMA;
    }

    public int getDinero() {
        return dinero;
    }

    public void setDinero(int dinero) {
        this.dinero = dinero;
    }

    public void sumarDinero(int cantidad) {
        this.dinero += cantidad;
    }

    public void restarDinero(int cantidad) {
        this.dinero -= cantidad;
    }
}
