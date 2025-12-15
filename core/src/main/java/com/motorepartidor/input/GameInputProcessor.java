package com.motorepartidor.input;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.motorepartidor.GameController;
import com.motorepartidor.screens.GameScreen;

public class GameInputProcessor implements InputProcessor {

    private final GameController gameController;

    public GameInputProcessor(GameController gameController) {
        this.gameController = gameController;
    }

    private boolean isAllowed(int keycode) {
        return keycode == Input.Keys.W ||
            keycode == Input.Keys.A ||
            keycode == Input.Keys.S ||
            keycode == Input.Keys.D ||
            keycode == Input.Keys.G ||
            keycode == Input.Keys.E;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (isAllowed(keycode)) {
           gameController.enviarInput(keycode);      // pressed
            return true;
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (isAllowed(keycode)) {
            gameController.enviarInput(-keycode);     // released
            return true;
        }
        return false;
    }

    @Override public boolean keyTyped(char character) { return false; }
    @Override public boolean touchDown(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean touchUp(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }
    @Override public boolean mouseMoved(int screenX, int screenY) { return false; }
    @Override public boolean scrolled(float amountX, float amountY) { return false; }
    @Override public boolean touchCancelled(int screenX, int screenY, int pointer, int button) { return false; }
}
