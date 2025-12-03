package com.motorepartidor.screens;

import com.badlogic.gdx.Audio;
import com.motorepartidor.Main;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.motorepartidor.Main;
import com.motorepartidor.audio.AudioManager;
import red.cliente;
import red.hiloCliente;

public class MainMenuScreen implements Screen {

    private final Game game;
    private final OrthographicCamera camera;
    private final Viewport viewport;
    public  hiloCliente cliente;
    private Stage stage;
    private Skin skin;
    private AudioManager audio;
    private Label resultP1Label;
    private Label resultP2Label;


    public MainMenuScreen(Game game, AudioManager audio , hiloCliente cliente) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(1280, 720, camera);
        this.audio = audio;
        this.cliente = cliente;
    }

    @Override
    public void show() {
        stage = new Stage(viewport);
        skin = safeLoadSkin();
        Gdx.input.setInputProcessor(stage);

        // Música de menú (opcional y seguro)
        try {
            if (this.audio != null) {
                this.audio.playMusic("audio/mainmenu.wav", true, 0.1f);
            }
        } catch (Exception e) {
            Gdx.app.error("MainMenu", "No se pudo reproducir musica de menú", e);
        }

        Table root = new Table();
        root.setFillParent(true);
        root.defaults().pad(12);
        stage.addActor(root);

        Label title = new Label("Moto Repartidor", skin);
        title.setFontScale(1.1f);

        // NUEVO: preparar labels de resultado
        resultP1Label = new Label("", skin);
        resultP2Label = new Label("", skin);

        if (game instanceof Main) {
            int lastWinner = ((Main) game).getLastWinner();
            String txtP1 = "";
            String txtP2 = "";

            if (lastWinner == 1) {
                // Ganó Jugador 1
                txtP1 = "Jugador 1: Has ganado";
                txtP2 = "Jugador 2: Has perdido";
                resultP1Label.setColor(Color.GREEN);
                resultP2Label.setColor(Color.RED);
            } else if (lastWinner == 2) {
                // Ganó Jugador 2
                txtP1 = "Jugador 1: Has perdido";
                txtP2 = "Jugador 2: Has ganado";
                resultP1Label.setColor(Color.RED);
                resultP2Label.setColor(Color.GREEN);
            }
            if(lastWinner == 3) {
                txtP1 = "Empate";
                txtP2 = "Empate";
                resultP1Label.setColor(Color.GRAY);
                resultP2Label.setColor(Color.GRAY);
            }
            // Si lastWinner == 0 no mostramos nada

            resultP1Label.setText(txtP1);
            resultP2Label.setText(txtP2);
        }

        TextButton playBtn = new TextButton("Jugar", skin);
        TextButton optionsBtn = new TextButton("Opciones", skin);
        TextButton exitBtn = new TextButton("Salir", skin);

        playBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                try {
                    // Si tu GameScreen requiere otra firma, cámbialo acá:
                    cliente.establecerConexion();
                    game.setScreen(new GameScreen(game, audio , cliente));
                   /* if (cliente.comprobarConexion()){


                    } else{
                        System.out.println("No se logro conectar");
                    }*/


                } catch (Throwable t) {
                    Gdx.app.error("MainMenu", "Error al abrir GameScreen", t);
                }
            }
        });
        optionsBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                try {
                    game.setScreen(new OptionsScreen(game, MainMenuScreen.this, audio));
                } catch (Throwable t) {
                    Gdx.app.error("MainMenu", "Error al abrir OptionsScreen", t);
                }
            }
        });
        exitBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                try {
                    Gdx.app.exit();
                } catch (Throwable t) {
                    Gdx.app.error("MainMenu", "Error al salir", t);
                }
            }
        });

        root.add(title).padBottom(20);
        root.row();

        // NUEVO: mostrar resultado solo si hay texto
        if (resultP1Label != null && resultP1Label.getText().length() > 0) {
            root.add(resultP1Label);
            root.row();
            root.add(resultP2Label);
            root.row();
        }

        root.add(playBtn).width(280).height(60);
        root.row();
        root.add(optionsBtn).width(280).height(60);
        root.row();
        root.add(exitBtn).width(280).height(60);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();

        // Parar música de menú al salir (si querés silencio en game/options)
        try {
            if (this.audio != null) this.audio.stopMusic();
        } catch (Exception ignored) {}
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
    }

    // ================== Skin Seguro ==================
    private Skin safeLoadSkin() {
        try {
            if (Gdx.files.internal("ui/uiskin.json").exists()) {
                return new Skin(Gdx.files.internal("ui/uiskin.json"));
            }
            if (Gdx.files.internal("uiskin.json").exists()) {
                return new Skin(Gdx.files.internal("uiskin.json"));
            }
        } catch (Exception e) {
            Gdx.app.error("MainMenu", "Error leyendo uiskin.json, uso fallback", e);
        }
        return buildFallbackSkin();
    }

    private Skin buildFallbackSkin() {
        Skin s = new Skin();
        BitmapFont font = new BitmapFont();
        s.add("default", font);

        Drawable panel = newDrawable(60, 60, 75, 255);
        Drawable accent = newDrawable(80, 160, 255, 255);

        Label.LabelStyle ls = new Label.LabelStyle();
        ls.font = font;
        ls.fontColor = Color.WHITE;
        s.add("default", ls);

        TextButton.TextButtonStyle tbs = new TextButton.TextButtonStyle();
        tbs.up = panel; tbs.down = accent; tbs.over = accent; tbs.font = font;
        s.add("default", tbs);

        return s;
    }

    private Drawable newDrawable(int r, int g, int b, int a) {
        Pixmap pm = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
        pm.setColor(r/255f, g/255f, b/255f, a/255f);
        pm.fill();
        TextureRegionDrawable dr = new TextureRegionDrawable(new TextureRegion(new Texture(pm)));
        pm.dispose();
        return dr;
    }
}
