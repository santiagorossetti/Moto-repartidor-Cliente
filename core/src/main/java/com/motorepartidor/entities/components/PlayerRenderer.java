package com.motorepartidor.entities.components;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.motorepartidor.entities.Jugador;

public class PlayerRenderer {
    private Jugador jugador;
    private Texture textura;
    private TextureRegion[] frames;
    private float stateTime;

    private static final float UNIT_SCALE = 1 / 64f;

    public PlayerRenderer(Jugador jugador) {
        this.jugador = jugador;
        this.textura = jugador.getTextura(); // Obtiene la textura del objeto Jugador
        this.frames = jugador.getFrames(); // Obtiene los frames del objeto Jugador
        this.stateTime = 0f;
    }

    public void update(float deltaTime) {
        stateTime += deltaTime;
    }

    public void render(Batch batch) {
        if (frames != null && frames.length > 0) {
            int framesPorSegundo = 10;
            int frameActual = (int)(stateTime * framesPorSegundo) % frames.length;
            TextureRegion currentFrame = frames[frameActual];

            float drawX = jugador.getPosicion().x * UNIT_SCALE;
            float drawY = jugador.getPosicion().y * UNIT_SCALE;
            float drawWidth = currentFrame.getRegionWidth() * UNIT_SCALE;
            float drawHeight = currentFrame.getRegionHeight() * UNIT_SCALE;
            float originX = currentFrame.getRegionWidth() / 2f * UNIT_SCALE;
            float originY = currentFrame.getRegionHeight() / 2f * UNIT_SCALE;

            batch.draw(currentFrame, drawX, drawY,
                originX, originY,
                drawWidth, drawHeight,
                1f, 1f, jugador.getAngulo());
        } else {
            if (textura != null) {
                float drawX = jugador.getPosicion().x * UNIT_SCALE;
                float drawY = jugador.getPosicion().y * UNIT_SCALE;
                float drawWidth = textura.getWidth() * UNIT_SCALE;
                float drawHeight = textura.getHeight() * UNIT_SCALE;
                batch.draw(textura, drawX, drawY, drawWidth, drawHeight);
            }
        }
    }

    public void dispose() {
        // La textura ya se gestiona en Jugador, no es necesario hacer dispose aquí
    }
}
