package com.motorepartidor.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;

import java.util.HashMap;
import java.util.Map;

/**
 * Gestor simple de audio.
 * - Cachea Music y Sound por ruta para evitar cargas repetidas.
 * - Maneja música única y efectos de sonido múltiples.
 * - Incluye control de volumen maestro, música y efectos.
 * - Falla en silencio si el archivo no existe (loggea el error).
 */
public class AudioManager {

    private final Map<String, Music> musics = new HashMap<>();
    private final Map<String, Sound> sounds = new HashMap<>();

    private Music currentMusic;
    private String currentMusicKey;

    private float musicVolume = 0.8f;   // volumen relativo música
    private float sfxVolume   = 0.9f;   // volumen relativo efectos
    private float masterVolume = 0.8f;  // volumen general
    private boolean muted = false;

    public AudioManager() {}

    // ========= MASTER =========
    public void setMasterVolume(float v) {
        masterVolume = clamp(v);
        applyVolumes();
    }

    public float getMasterVolume() {
        return masterVolume;
    }

    // ========= MÚSICA =========
    private Music getOrLoadMusic(String path) {
        if (musics.containsKey(path)) return musics.get(path);
        try {
            FileHandle fh = Gdx.files.internal(path);
            if (!fh.exists()) {
                Gdx.app.error("AudioManager", "Music no encontrada: " + path);
                return null;
            }
            Music m = Gdx.audio.newMusic(fh);
            musics.put(path, m);
            return m;
        } catch (Exception e) {
            Gdx.app.error("AudioManager", "Error cargando Music: " + path, e);
            return null;
        }
    }

    /**
     * Reproduce una música. Detiene la anterior si la hubiera.
     * @param path ruta dentro de assets (ej: "audio/menu.ogg")
     * @param loop si se repite
     * @param volume 0..1 (volumen relativo a musicVolume y masterVolume)
     */
    public void playMusic(String path, boolean loop, float volume) {
        if (currentMusic != null) {
            currentMusic.stop();
        }
        Music music = getOrLoadMusic(path);
        if (music == null) return;

        currentMusic = music;
        currentMusicKey = path;
        music.setLooping(loop);
        float finalVol = getEffectiveMusicVolume(volume);
        music.setVolume(finalVol);
        music.play();
    }

    public void stopMusic() {
        if (currentMusic != null) currentMusic.stop();
    }

    public void pauseMusic() {
        if (currentMusic != null) currentMusic.pause();
    }

    public void resumeMusic() {
        if (currentMusic != null && !muted) currentMusic.play();
    }

    public void setMusicVolume(float v) {
        musicVolume = clamp(v);
        applyVolumes();
    }

    public float getMusicVolume() { return musicVolume; }

    // ========= SFX =========
    private Sound getOrLoadSound(String path) {
        if (sounds.containsKey(path)) return sounds.get(path);
        try {
            FileHandle fh = Gdx.files.internal(path);
            if (!fh.exists()) {
                Gdx.app.error("AudioManager", "SFX no encontrado: " + path);
                return null;
            }
            Sound s = Gdx.audio.newSound(fh);
            sounds.put(path, s);
            return s;
        } catch (Exception e) {
            Gdx.app.error("AudioManager", "Error cargando SFX: " + path, e);
            return null;
        }
    }

    /**
     * Reproduce un efecto de sonido.
     * @param path ruta (ej: "audio/click.wav")
     * @param volume 0..1 (volumen relativo a sfxVolume y masterVolume)
     */
    public long playSound(String path, float volume) {
        Sound s = getOrLoadSound(path);
        if (s == null) return -1L;
        float finalVol = getEffectiveSfxVolume(volume);
        return s.play(finalVol);
    }

    public void setSfxVolume(float v) {
        sfxVolume = clamp(v);
    }

    public float getSfxVolume() { return sfxVolume; }

    // ========= GLOBAL =========
    public void setMuted(boolean muted) {
        this.muted = muted;
        applyVolumes();
    }

    public boolean isMuted() { return muted; }

    private void applyVolumes() {
        if (currentMusic != null) {
            currentMusic.setVolume(getEffectiveMusicVolume(1f));
        }
    }

    private float getEffectiveMusicVolume(float factor) {
        if (muted) return 0f;
        return clamp(factor) * musicVolume * masterVolume;
    }

    private float getEffectiveSfxVolume(float factor) {
        if (muted) return 0f;
        return clamp(factor) * sfxVolume * masterVolume;
    }

    public void dispose() {
        if (currentMusic != null) {
            currentMusic.stop();
        }
        for (Music m : musics.values()) {
            m.dispose();
        }
        musics.clear();

        for (Sound s : sounds.values()) {
            s.dispose();
        }
        sounds.clear();
        currentMusic = null;
        currentMusicKey = null;
    }

    private float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
}
