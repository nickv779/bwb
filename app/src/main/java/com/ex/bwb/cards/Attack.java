package com.ex.bwb.cards;

import gl.shaders.Texture;
import com.ex.bwb.Player;
import com.ex.bwb.game.GameController;

public class Attack extends Card {

    int damage;
    String target; // Left, Right, Across, All, Any
    public Attack(String name, String description, String tagLine, CardType type, Texture artwork, int damage,
                  String target) {
        super(name, description, tagLine, CardType.ATTACK, artwork);
        this.damage = damage;
        this.target = target;
    }

    public void Activate(Player source, GameController controller) {

    }
}
