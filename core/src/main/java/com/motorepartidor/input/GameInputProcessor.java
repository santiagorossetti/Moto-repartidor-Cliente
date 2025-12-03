package com.motorepartidor.input;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;

public class GameInputProcessor implements InputProcessor {

    private boolean upPressed, downPressed, leftPressed, rightPressed;
    private boolean arrowUpPressed, arrowDownPressed, arrowLeftPressed, arrowRightPressed;
    private boolean ePressed, pPressed;
    private boolean gPressed, lPressed;

    public boolean isUpPressed() { return upPressed; }
    public boolean isDownPressed() { return downPressed; }
    public boolean isLeftPressed() { return leftPressed; }
    public boolean isRightPressed() { return rightPressed; }

    public boolean isArrowUpPressed() { return arrowUpPressed; }
    public boolean isArrowDownPressed() { return arrowDownPressed; }
    public boolean isArrowLeftPressed() { return arrowLeftPressed; }
    public boolean isArrowRightPressed() { return arrowRightPressed; }

    public boolean isEPressed() { return ePressed; }
    public boolean isPPressed() { return pPressed; }

    // Nuevas teclas para pedidos
    public boolean isGPressed() { return gPressed; } // Jugador 1 aceptar/entregar
    public boolean isLPressed() { return lPressed; } // Jugador 2 aceptar/entregar

    @Override
    public boolean keyDown(int keycode) {
        switch(keycode) {
            case Input.Keys.W: upPressed = true; break;
            case Input.Keys.S: downPressed = true; break;
            case Input.Keys.A: leftPressed = true; break;
            case Input.Keys.D: rightPressed = true; break;

            case Input.Keys.UP: arrowUpPressed = true; break;
            case Input.Keys.DOWN: arrowDownPressed = true; break;
            case Input.Keys.LEFT: arrowLeftPressed = true; break;
            case Input.Keys.RIGHT: arrowRightPressed = true; break;

            case Input.Keys.E: ePressed = true; break;
            case Input.Keys.P: pPressed = true; break;

            case Input.Keys.G: gPressed = true; break;
            case Input.Keys.L: lPressed = true; break;
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        switch(keycode) {
            case Input.Keys.W: upPressed = false; break;
            case Input.Keys.S: downPressed = false; break;
            case Input.Keys.A: leftPressed = false; break;
            case Input.Keys.D: rightPressed = false; break;

            case Input.Keys.UP: arrowUpPressed = false; break;
            case Input.Keys.DOWN: arrowDownPressed = false; break;
            case Input.Keys.LEFT: arrowLeftPressed = false; break;
            case Input.Keys.RIGHT: arrowRightPressed = false; break;

            case Input.Keys.E: ePressed = false; break;
            case Input.Keys.P: pPressed = false; break;

            case Input.Keys.G: gPressed = false; break;
            case Input.Keys.L: lPressed = false; break;
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
