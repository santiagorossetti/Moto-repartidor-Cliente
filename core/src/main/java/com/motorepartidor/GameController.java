package com.motorepartidor;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Rectangle;

public interface GameController {


    void enviarInput (int tecla );
    void actualizarPosicion(Vector2 pos1 , Vector2 pos2 , float angulo1 , float angulo2);

    void actualizarGas(float gas , int id );
    void actualizarDinero(int dinero , int id);
    void actualizarVida(int vida , int id);
    void actualizarDelivery(Rectangle target, boolean dangerous, int reward, int id);

}
