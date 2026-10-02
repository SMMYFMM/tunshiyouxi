package com.absorbgame.entity;

import com.absorbgame.core.GameConfig;

/** A food pellet. */
public class Food extends Entity {
    public float value;

    public Food(float x, float y, float value) {
        super(x, y, GameConfig.FOOD_RADIUS);
        this.value = value;
        setColor(GameConfig.COLOR_FOOD[0], GameConfig.COLOR_FOOD[1],
                GameConfig.COLOR_FOOD[2], GameConfig.COLOR_FOOD[3]);
    }
}
