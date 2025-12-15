package com.motorepartidor.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.motorepartidor.audio.AudioManager;
import red.hiloCliente;

public class MensajeScreen implements Screen {

    private final Game game;
    private final AudioManager audio;
    private final String mensaje;
    private final String botonTexto;

    private Stage stage;
    private Skin skin;
    private Viewport viewport;

    public MensajeScreen(Game game, AudioManager audio, String mensaje, String botonTexto) {
        this.game = game;
        this.audio = audio;
        this.mensaje = mensaje;
        this.botonTexto = botonTexto;
    }

    @Override
    public void show() {
        viewport = new FitViewport(1280, 720);
        stage = new Stage(viewport);
        skin = safeLoadSkin();
        Gdx.input.setInputProcessor(stage);

        Table root = new Table();
        root.setFillParent(true);
        root.defaults().pad(14);
        stage.addActor(root);

        Label title = new Label("Moto Repartidor", skin);
        title.setFontScale(1.3f);

        Label msg = new Label(mensaje, skin);
        msg.setAlignment(Align.center);
        msg.setFontScale(1.2f);
        msg.setColor(Color.WHITE);

        TextButton backBtn = new TextButton(botonTexto, skin);
        backBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                // Volver a menú
                game.setScreen(new MainMenuScreen(game, audio, null));
            }
        });

        root.add(title).padBottom(30);
        root.row();
        root.add(msg).width(700).padBottom(25);
        root.row();
        root.add(backBtn).width(320).height(60);
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
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
    }

    //  Skin seguro (igual que tu patrón)
    private Skin safeLoadSkin() {
        try {
            if (Gdx.files.internal("ui/uiskin.json").exists()) return new Skin(Gdx.files.internal("ui/uiskin.json"));
            if (Gdx.files.internal("uiskin.json").exists()) return new Skin(Gdx.files.internal("uiskin.json"));
        } catch (Exception e) {
            Gdx.app.error("MensajeScreen", "Error leyendo skin, uso fallback", e);
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

