package com.motorepartidor.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * Indicador visual de delivery:
 * - Flecha desde el jugador apuntando al objetivo
 * - Beacon pulsante en el destino (si está en pantalla)
 *
 *
 */
public class DeliveryIndicator {

    private final ShapeRenderer renderer = new ShapeRenderer();

    // Destino en coordenadas de mundo
    private final Vector2 targetWorld = new Vector2();
    private boolean hasTarget = false;

    private Color color = Color.CYAN;
    private float pulseTime = 0f;

    // Tamaños en unidades de mundo (compatibles con UNIT_SCALE)
    private static final float ARROW_LENGTH = 0.6f;
    private static final float ARROW_WIDTH  = 0.35f;
    private static final float BEACON_RADIUS = 0.35f;
    private static final float BASE_OFFSET = 1.0f;

    // =========================================================
    // Configuración
    // =========================================================
    public void setColor(Color color) {
        if (color != null) this.color = color;
    }

    public void setTarget(float x, float y) {
        targetWorld.set(x, y);
        hasTarget = true;
    }

    public void clearTarget() {
        hasTarget = false;
    }

    // =========================================================
    // Render en mundo
    // =========================================================
    public void renderWorld(float playerX,
                            float playerY,
                            OrthographicCamera camera,
                            float delta) {

        if (!hasTarget || camera == null) return;

        pulseTime += delta;

        // Dirección jugador -> destino
        float dx = targetWorld.x - playerX;
        float dy = targetWorld.y - playerY;
        float angle = MathUtils.atan2(dy, dx);

        float cos = MathUtils.cos(angle);
        float sin = MathUtils.sin(angle);

        // Base de la flecha (adelante del jugador)
        float baseX = playerX + cos * BASE_OFFSET;
        float baseY = playerY + sin * BASE_OFFSET;

        // Punta de la flecha
        float tipX = baseX + cos * ARROW_LENGTH;
        float tipY = baseY + sin * ARROW_LENGTH;

        // Base izquierda / derecha
        float perpX = -sin;
        float perpY = cos;

        float halfWidth = ARROW_WIDTH * 0.5f;
        float leftX  = baseX + perpX * halfWidth;
        float leftY  = baseY + perpY * halfWidth;
        float rightX = baseX - perpX * halfWidth;
        float rightY = baseY - perpY * halfWidth;

        renderer.setProjectionMatrix(camera.combined);

        // ===== Flecha =====
        renderer.begin(ShapeRenderer.ShapeType.Filled);
        renderer.setColor(color);
        renderer.triangle(tipX, tipY, leftX, leftY, rightX, rightY);
        renderer.end();

        // ===== Beacon si el destino está visible =====
        if (camera.frustum.pointInFrustum(targetWorld.x, targetWorld.y, 0)) {

            float pulse = BEACON_RADIUS + MathUtils.sin(pulseTime * 4f) * 0.1f;

            renderer.begin(ShapeRenderer.ShapeType.Line);
            renderer.setColor(color);
            renderer.circle(targetWorld.x, targetWorld.y, pulse, 24);
            renderer.end();

            renderer.begin(ShapeRenderer.ShapeType.Filled);
            renderer.circle(targetWorld.x, targetWorld.y, 0.06f, 16);
            renderer.end();
        }
    }

    // =========================================================
    // Cleanup
    // =========================================================
    public void dispose() {
        renderer.dispose();
    }
}
