package com.motorepartidor.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.motorepartidor.audio.AudioManager;

import java.util.ArrayList;

public class OptionsScreen implements Screen {

    private static final String PREFS_NAME = "settings";
    private static final String KEY_VOLUME = "master_volume";
    private static final String KEY_FULLSCREEN = "fullscreen";
    private static final String KEY_WIDTH = "width";
    private static final String KEY_HEIGHT = "height";

    private final Game game;
    private final Screen previousScreen;
    private final AudioManager audio;

    private final Viewport viewport;

    private Stage stage;
    private Skin skin;

    private Slider volumeSlider;
    private Label volumeValue;
    private CheckBox fullscreenCheck;
    private SelectBox<String> resolutionBox;

    private java.util.List<int[]> windowedResolutions;

    public OptionsScreen(Game game, Screen previousScreen, AudioManager audio) {
        this.game = game;
        this.previousScreen = previousScreen;
        this.audio = audio;
        this.viewport = new FitViewport(1280, 720);
    }

    @Override
    public void show() {
        if (stage == null) {
            stage = new Stage(viewport);
            skin = safeLoadSkin();
        }

        // ✅ IMPORTANTE: limpiar UI previa para no duplicar actores
        stage.clear();

        Gdx.input.setInputProcessor(stage);

        // ===== Cargar prefs =====
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        float savedVolume = prefs.getFloat(KEY_VOLUME, 0.8f);
        boolean savedFullscreen = prefs.getBoolean(KEY_FULLSCREEN, false);
        int savedW = prefs.getInteger(KEY_WIDTH, 1280);
        int savedH = prefs.getInteger(KEY_HEIGHT, 720);

        // ===== Layout =====
        Table root = new Table();
        root.setFillParent(true);
        root.defaults().pad(10);
        stage.addActor(root);

        Label title = new Label("Opciones", skin);
        title.setFontScale(1.1f);
        root.add(title).colspan(2);
        root.row();

        // ===== Volumen =====
        root.add(new Label("Volumen general:", skin));

        Table volRow = new Table(skin);
        volumeSlider = new Slider(0f, 1f, 0.01f, false, skin);
        volumeSlider.setValue(clamp(savedVolume));

        volumeValue = new Label(percentText(volumeSlider.getValue()), skin);

        volRow.add(volumeSlider).width(420);
        volRow.add(volumeValue).padLeft(10);

        root.add(volRow);
        root.row();

        volumeSlider.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                float v = clamp(volumeSlider.getValue());
                volumeValue.setText(percentText(v));
                if (audio != null) audio.setMasterVolume(v);
            }
        });

        // ===== Fullscreen =====
        root.add(new Label("Pantalla completa:", skin));
        fullscreenCheck = new CheckBox(" Activar", skin);
        fullscreenCheck.setChecked(savedFullscreen);
        root.add(fullscreenCheck);
        root.row();

        // ===== Resolución ventana =====
        root.add(new Label("Resolución (ventana):", skin));

        resolutionBox = new SelectBox<>(skin);
        windowedResolutions = defaultWindowedResolutions();

        String[] items = new String[windowedResolutions.size()];
        int preselect = 0;

        for (int i = 0; i < windowedResolutions.size(); i++) {
            int[] r = windowedResolutions.get(i);
            items[i] = r[0] + " x " + r[1];
            if (r[0] == savedW && r[1] == savedH) preselect = i;
        }

        resolutionBox.setItems(items);
        resolutionBox.setSelectedIndex(preselect);

        root.add(resolutionBox);
        root.row();

        // ===== Botones =====
        TextButton applyBtn = new TextButton("Aplicar", skin);
        TextButton backBtn  = new TextButton("Volver",  skin);
        TextButton exitBtn  = new TextButton("Salir",   skin);

        applyBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                applyChanges();
            }
        });

        backBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(previousScreen);
            }
        });

        exitBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.exit();
            }
        });

        root.add(applyBtn).width(220).height(50).padTop(15);
        root.add(backBtn).width(220).height(50).padTop(15);
        root.add(exitBtn).width(220).height(50).padTop(15);
    }

    private void applyChanges() {
        float vol = clamp(volumeSlider.getValue());
        boolean fs = fullscreenCheck.isChecked();

        int idx = Math.max(0, resolutionBox.getSelectedIndex());
        int[] wh = windowedResolutions.get(idx);
        int w = wh[0], h = wh[1];

        // Guardar prefs
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.putFloat(KEY_VOLUME, vol);
        prefs.putBoolean(KEY_FULLSCREEN, fs);
        prefs.putInteger(KEY_WIDTH, w);
        prefs.putInteger(KEY_HEIGHT, h);
        prefs.flush();

        // Aplicar volumen al audio global
        if (audio != null) audio.setMasterVolume(vol);

        // Aplicar modo ventana/pantalla completa
        try {
            if (fs) {
                DisplayMode dm = Gdx.graphics.getDisplayMode();
                Gdx.graphics.setFullscreenMode(dm);
            } else {
                Gdx.graphics.setWindowedMode(w, h);
            }
        } catch (Exception e) {
            Gdx.app.error("Options", "No se pudo aplicar configuración de video", e);
        }
    }

    private java.util.List<int[]> defaultWindowedResolutions() {
        java.util.List<int[]> list = new ArrayList<>();
        list.add(new int[]{1280, 720});
        list.add(new int[]{1366, 768});
        list.add(new int[]{1600, 900});
        list.add(new int[]{1920, 1080});
        list.add(new int[]{2560, 1440});
        return list;
    }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }

    private static String percentText(float v) {
        return String.format("%d%%", Math.round(clamp(v) * 100));
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
        // Solo soltamos input. No dispose acá porque reutilizamos la screen.
        if (Gdx.input.getInputProcessor() == stage) {
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
    }

    // ================== Skin Seguro ==================
    private Skin safeLoadSkin() {
        try {
            if (Gdx.files.internal("ui/uiskin.json").exists()) return new Skin(Gdx.files.internal("ui/uiskin.json"));
            if (Gdx.files.internal("uiskin.json").exists())    return new Skin(Gdx.files.internal("uiskin.json"));
        } catch (Exception e) {
            Gdx.app.error("Options", "Error leyendo uiskin.json, uso fallback", e);
        }
        return buildFallbackSkin();
    }

    private Skin buildFallbackSkin() {
        Skin s = new Skin();
        BitmapFont font = new BitmapFont();
        s.add("default", font);

        Drawable panel  = newDrawable(60, 60, 75, 255);
        Drawable accent = newDrawable(80, 160, 255, 255);

        Label.LabelStyle ls = new Label.LabelStyle(font, Color.WHITE);
        s.add("default", ls);

        TextButton.TextButtonStyle tbs = new TextButton.TextButtonStyle();
        tbs.up = panel; tbs.down = accent; tbs.over = accent; tbs.font = font;
        s.add("default", tbs);

        CheckBox.CheckBoxStyle cbs = new CheckBox.CheckBoxStyle();
        cbs.checkboxOff = panel; cbs.checkboxOn = accent; cbs.font = font; cbs.fontColor = Color.WHITE;
        s.add("default", cbs);

        Slider.SliderStyle ss = new Slider.SliderStyle();
        ss.background = panel; ss.knob = accent;
        s.add("default-horizontal", ss);

        com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle listStyle =
            new com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle(font, Color.WHITE, Color.LIGHT_GRAY, panel);

        ScrollPane.ScrollPaneStyle spStyle = new ScrollPane.ScrollPaneStyle();
        SelectBox.SelectBoxStyle sbs = new SelectBox.SelectBoxStyle(font, Color.WHITE, panel, spStyle, listStyle);
        s.add("default", sbs);

        return s;
    }

    private Drawable newDrawable(int r, int g, int b, int a) {
        Pixmap pm = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
        pm.setColor(r / 255f, g / 255f, b / 255f, a / 255f);
        pm.fill();
        TextureRegionDrawable dr = new TextureRegionDrawable(new Texture(pm));
        pm.dispose();
        return dr;
    }
}
