package com.ex.bwb.cards;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.IOException;
import java.io.InputStream;
import engine.J4Q;
import engine.shaders.AtlasTexture;
import engine.shaders.Texture;

public class TextureRepository {

    private static Bitmap repository = null;
    private static final String cardTextures = "textures/UV_BWB_ROW.png";

    public static void init() {
        try {
            InputStream is = J4Q.activity.getAssets().open(cardTextures);
            repository = BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            throw new RuntimeException("Error loading atlas texture.");
        }
    }

    public static void dispose() {
        if (repository != null) {
            repository.recycle();
            repository = null;
        }
    }

    private static Texture crop(int x) {
        if (repository == null) {
            throw new RuntimeException("Texture repository not initialized.");
        }
        Bitmap region = Bitmap.createBitmap(repository, x, 0, 750, 1050);
        return new AtlasTexture(region);

    }

    // --- Big Buddy Cards ---

    // public static Texture name() { return crop(x, y, w, h) }

    public static Texture Gerald() { return crop(750*52); }
    public static Texture Fernando() { return crop(750*53); }
    public static Texture Darrel() { return crop(750*54); }
    public static Texture MrOstrich() { return crop(750*55); }

    // -- Lil Buddy Cards ---

    public static Texture FighterSnail() { return crop(750*29); }
    public static Texture InferiorTowing() { return crop(750*30); }
    public static Texture WormholeWorm() { return crop(750*31); }
    public static Texture SockPuppet() { return crop(750*32); }
    public static Texture WashingMachine() { return crop(750*33); }
    public static Texture Ducks() { return crop(750*34); }
    public static Texture Duck() { return crop(750*35); }
    public static Texture FlyingFish() { return crop(750*36); }
    public static Texture Disco() { return crop(750*37); }
    public static Texture DarrelJr() { return crop(750*38); }
    public static Texture PiggyBank() { return crop(750*39); }
    public static Texture HeraldBerald() { return crop(750*40); }
    public static Texture MagicMirror() { return crop(750*41); }
    public static Texture SillyGoose() { return crop(750*42); }
    public static Texture JamJellyfish() { return crop(750*43); }
    public static Texture HotPotato() { return crop(750*44); }
    public static Texture LarryLlama() { return crop(750*45); }
    public static Texture AlbertAlpaca() { return crop(750*46); }
    public static Texture EarlyBird() { return crop(750*47); }
    public static Texture SleepyGiraffe() { return crop(750*48); }

    // --- Objective Card ---

    public static Texture Objective() { return crop(750*49); }

    // --- Card Backs ---

    public static Texture BlueBack() { return crop(750*50); }
    public static Texture RedBack() { return crop(750*51); }

    // --- Signature Cards ---

    public static Texture MassExtinction() { return crop(750*25); }
    public static Texture EconomicRegression() { return crop(750*26); }
    public static Texture HeatDeath() { return crop(750*27); }
    public static Texture Party() { return crop(750*28); }

    // --- Shake-up Cards ---

    public static Texture NuclearWinter() { return crop(750*21); }
    public static Texture FlipSide() { return crop(750*22); }
    public static Texture UncertaintyPrinciple() { return crop(750*23); }
    public static Texture AbsoluteZero() { return crop(750*24); }

    // --- Action Cards ---

    public static Texture BedTime() { return crop(750*5); }
    public static Texture DumpsterDiving() { return crop(750*6); }
    public static Texture BeachEpisode() { return crop(750*7); }
    public static Texture PickPocket() { return crop(750*8); }
    public static Texture NoSoliciting() { return crop(750*9); }
    public static Texture ChangeChannel() { return crop(750*10); }
    public static Texture SpareChange() { return crop(750*11); }
    public static Texture SpaDay() { return crop(750*12); }
    public static Texture OlReliable() { return crop(750*13); }
    public static Texture BarterTime() { return crop(750*14); }
    public static Texture ResetButton() { return crop(750*15); }
    public static Texture InItToWinIt() { return crop(750*16); }
    public static Texture GooglyEyedRock() { return crop(750*17); }
    public static Texture PhoneAFriend() { return crop(750*18); }
    public static Texture FickleFungus() { return crop(750*19); }
    public static Texture UnderdogDuel() { return crop(750*20); }

    // --- Attack Cards ---

    public static Texture Katana() { return crop(750*0); }
    public static Texture MagicWand() { return crop(750*1); }
    public static Texture Grenade() { return crop(750*2); }
    public static Texture Knife() { return crop(750*3); }
    public static Texture Tackle() { return crop(750*4); }


}