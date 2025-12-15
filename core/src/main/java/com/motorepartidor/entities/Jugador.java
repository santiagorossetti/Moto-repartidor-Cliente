package com.motorepartidor.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.motorepartidor.entities.components.PlayerRenderer;

/**
 * CLIENTE: entidad visual.
 * NO calcula movimiento, colisiones ni gasolina. Todo eso lo decide el servidor.
 * Este objeto solo guarda estado (pos/ang/vida/gas/dinero) y renderiza.
 */
public class Jugador {

    private Texture textura;
    private TextureRegion[] frames;

    private final Vector2 posicion;
    private float angulo;
    private float velocidad; // opcional (si lo mostrás), pero NO se calcula acá

    private final Rectangle bounds;
    private final Polygon polygon;

    private int vida;
    private float gasolina;
    private int dinero;

    private static final int VIDA_MAXIMA = 100;
    private static final float GASOLINA_MAXIMA = 100f;

    private final PlayerRenderer renderer;

    public Jugador(String texturaPath, int frameWidth, int frameHeight, Vector2 posicionInicial) {
        this.textura = safeLoadTexture(texturaPath);
        this.frames = splitFrames(this.textura, frameWidth, frameHeight);

        this.posicion = new Vector2(posicionInicial);
        this.angulo = 0f;
        this.velocidad = 0f;

        this.vida = VIDA_MAXIMA;
        this.gasolina = GASOLINA_MAXIMA;
        this.dinero = 0;

        this.bounds = new Rectangle(posicion.x, posicion.y, frameWidth, frameHeight);

        float[] vertices = new float[]{
            0, 0,
            frameWidth, 0,
            frameWidth, frameHeight,
            0, frameHeight
        };
        this.polygon = new Polygon(vertices);
        this.polygon.setOrigin(frameWidth / 2f, frameHeight / 2f);
        this.polygon.setPosition(posicion.x, posicion.y);

        this.renderer = new PlayerRenderer(this);
    }

    /** CLIENTE: solo animación (si corresponde). No hay movimiento. */
    public void update(float dt) {
        renderer.update(dt);
    }

    public void dibujar(Batch batch) {
        renderer.render(batch);
    }

    public void dispose() {
        if (textura != null) textura.dispose();
    }

    // ===================== Estado sincronizado desde el servidor =====================

    public void setPosicion(Vector2 nuevaPos) {
        this.posicion.set(nuevaPos);
        this.bounds.setPosition(posicion.x, posicion.y);
        this.polygon.setPosition(posicion.x, posicion.y);
    }

    public Vector2 getPosicion() { return posicion; }

    public void setAngulo(float ang) {
        this.angulo = ang;
        this.polygon.setRotation(ang);
    }

    public float getAngulo() { return angulo; }

    public Rectangle getBounds() { return bounds; }

    public Polygon getPolygon() { return polygon; }

    public int getVida() { return vida; }
    public void setVida(int vida) { this.vida = clampInt(vida, 0, VIDA_MAXIMA); }

    public float getGasolina() { return gasolina; }
    public void setGasolina(float gasolina) { this.gasolina = clampFloat(gasolina, 0f, GASOLINA_MAXIMA); }

    public int getDinero() { return dinero; }
    public void setDinero(int dinero) { this.dinero = dinero; }

    public float getVelocidad() { return velocidad; }
    public void setVelocidad(float v) { this.velocidad = v; } // solo si el server la manda (si no, podés borrar)

    public Texture getTextura() { return textura; }
    public TextureRegion[] getFrames() { return frames; }

    // ===================== Helpers =====================

    private Texture safeLoadTexture(String path) {
        try {
            return new Texture(Gdx.files.internal(path));
        } catch (Exception e) {
            Gdx.app.error("Jugador", "No se pudo cargar textura: " + path, e);
            return new Texture(Gdx.files.internal("badlogic.jpg"));
        }
    }

    private TextureRegion[] splitFrames(Texture tex, int w, int h) {
        try {
            TextureRegion[][] temp = TextureRegion.split(tex, w, h);
            if (temp != null && temp.length > 0 && temp[0] != null && temp[0].length > 0) {
                TextureRegion[] out = new TextureRegion[temp[0].length];
                System.arraycopy(temp[0], 0, out, 0, temp[0].length);
                return out;
            }
        } catch (Exception ignored) {}
        return new TextureRegion[]{ new TextureRegion(tex) };
    }

    private int clampInt(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private float clampFloat(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
