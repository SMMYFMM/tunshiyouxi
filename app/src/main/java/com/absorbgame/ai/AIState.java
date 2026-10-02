package com.absorbgame.ai;

/** AI behavioral states. */
public enum AIState {
    WANDER,         // random roaming, low priority
    SEARCH_FOOD,    // moving toward nearby food
    CHASE,          // pursuing a smaller creature
    FLEE,           // running from a bigger creature
    EVADE,          // escaping multiple threats (higher urgency)
    RECOVER         // cooling down after chase/flee
}
