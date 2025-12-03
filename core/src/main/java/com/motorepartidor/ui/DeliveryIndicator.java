package com.motorepartidor.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public class DeliveryIndicator {

    private final ShapeRenderer sr = new ShapeRenderer();
    private final Vector2 targetWorld = new Vector2();
    private boolean hasTarget = false;
    private Color color = Color.CYAN;
    private float pulseT = 0f;

    // Tamaños en UNIDADES DE MUNDO (si usás UNIT_SCALE=1/64f, esto queda bien)
    private static final float ARROW_LEN  = 0.6f;  // largo de flecha (~38 px)
    private static final float ARROW_BASE = 0.35f; // ancho base (~22 px)
    private static final float BEACON_R   = 0.35f; // radio beacon (~22 px)

    public void setColor(Color c) { this.color = c; }
    public void clearTarget() { hasTarget = false; }
    public void setTarget(float targetX, float targetY) {
        targetWorld.set(targetX, targetY);
        hasTarget = true;
    }

    /**
     * Dibuja en COORDENADAS DE MUNDO usando la cámara dada:
     * - Flecha pegada al jugador apuntando al destino
     * - Beacon en el destino si está en frustum
     * IMPORTANTE: llamarlo cuando NO haya otro ShapeRenderer.begin() abierto.
     */
    public void renderWorld(float playerX, float playerY, OrthographicCamera camera, float delta) {
        if (!hasTarget || camera == null) return;

        pulseT += delta;

        // Dirección jugador -> destino (mundo)
        float dx = targetWorld.x - playerX;
        float dy = targetWorld.y - playerY;
        float ang = MathUtils.atan2(dy, dx);
        float cos = MathUtils.cos(ang), sin = MathUtils.sin(ang);

        // Proyección de mundo
        sr.setProjectionMatrix(camera.combined);

        // Flecha (triángulo) con vértice hacia el destino
        float tipX = playerX + cos * ARROW_LEN;
        float tipY = playerY + sin * ARROW_LEN;

        float baseX = playerX;
        float baseY = playerY;

        float perpX = -sin, perpY = cos;
        float leftX  = baseX + perpX * (ARROW_BASE * 0.5f);
        float leftY  = baseY + perpY * (ARROW_BASE * 0.5f);
        float rightX = baseX - perpX * (ARROW_BASE * 0.5f);
        float rightY = baseY - perpY * (ARROW_BASE * 0.5f);

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(color);
        sr.triangle(tipX, tipY, leftX, leftY, rightX, rightY);
        sr.end();

        // Si el destino está visible por la cámara, dibujar beacon
        if (camera.frustum.pointInFrustum(targetWorld.x, targetWorld.y, 0)) {
            float r = BEACON_R + MathUtils.sin(pulseT * 4f) * 0.1f;

            sr.begin(ShapeRenderer.ShapeType.Line);
            sr.setColor(color);
            sr.circle(targetWorld.x, targetWorld.y, r, 24);
            sr.end();

            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(color);
            sr.circle(targetWorld.x, targetWorld.y, 0.06f, 16);
            sr.end();
        }
    }

    public void dispose() { sr.dispose(); }
}
