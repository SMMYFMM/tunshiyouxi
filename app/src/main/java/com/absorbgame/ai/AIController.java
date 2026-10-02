package com.absorbgame.ai;

import com.absorbgame.core.GameConfig;
import com.absorbgame.entity.AIEntity;
import com.absorbgame.entity.Creature;
import com.absorbgame.entity.Entity;
import com.absorbgame.world.World;

/**
 * Orchestrates the AI: senses the world, picks a target, chooses a state,
 * and commands movement. One instance shared across all AI (stateless
 * regarding the AI itself; all per-AI state lives in AIEntity).
 *
 * Decision runs at a reduced frequency (configurable, per AI) to save CPU.
 * Close threats raise the decision rate.
 */
public class AIController {
    private final AISensor sensor;
    private final AITargetSelector selector;
    private final AIMovementController movement;
    private final World world;

    public AIController(World world) {
        this.world = world;
        this.sensor = new AISensor(world);
        this.selector = new AITargetSelector();
        this.movement = new AIMovementController(world);
    }

    public void update(AIEntity self, float dt, GameConfig.Difficulty diff) {
        if (!self.alive) return;

        // decision timing: faster when threat nearby
        float interval = diff.decisionInterval;
        if (self.state == AIState.FLEE || self.state == AIState.EVADE) {
            interval = Math.min(interval, GameConfig.AI_DECISION_INTERVAL_CLOSE);
        }
        self.decisionTimer -= dt;
        self.stateTimer += dt;

        if (self.decisionTimer <= 0f) {
            self.decisionTimer = interval;
            decide(self, diff);
        }

        // movement: follow current state's intent
        executeMovement(self, dt);
    }

    private void decide(AIEntity self, GameConfig.Difficulty diff) {
        sensor.sense(self, diff.senseRadius);
        Creature threat = selector.selectPrimaryThreat(self, sensor.getThreats());

        // 1) If there's a real threat -> FLEE / EVADE
        if (threat != null) {
            self.fleeTarget = threat;
            self.currentTarget = null;
            int threatCount = sensor.getThreats().size();
            self.state = (threatCount >= 2 || threat.mass > self.mass * 1.8f)
                    ? AIState.EVADE : AIState.FLEE;
            self.stateTimer = 0f;
            return;
        }

        // 2) If in chase and target still valid & edible, keep chasing
        if (self.state == AIState.CHASE && self.currentTarget instanceof Creature) {
            Creature prey = (Creature) self.currentTarget;
            if (prey.alive && self.canEat(prey)
                    && self.stateTimer < GameConfig.AI_MAX_CHASE_TIME
                    && self.dist(prey) < diff.senseRadius * 1.3f) {
                return; // keep chasing
            }
            // chase expired/invalid -> recover
            self.currentTarget = null;
            self.state = AIState.RECOVER;
            self.stateTimer = 0f;
            return;
        }

        // 3) RECOVER -> after cooldown, resume searching
        if (self.state == AIState.RECOVER) {
            if (self.stateTimer > 1.2f) {
                self.state = AIState.SEARCH_FOOD;
                self.stateTimer = 0f;
            }
            return;
        }

        // 4) Look for prey
        Creature prey = selector.selectPreyTarget(self, sensor.getPrey());
        if (prey != null && self.personality.aggression * diff.aggressiveness > 0.35f) {
            self.currentTarget = prey;
            self.fleeTarget = null;
            self.state = AIState.CHASE;
            self.stateTimer = 0f;
            return;
        }

        // 5) Look for food
        if (self.personality.greed * diff.foodPriority > 0.2f) {
            Entity food = selector.selectFoodTarget(self, sensor.getFoods());
            if (food != null) {
                self.currentTarget = food;
                self.state = AIState.SEARCH_FOOD;
                return;
            }
        }

        // 6) default wander
        self.currentTarget = null;
        self.state = AIState.WANDER;
    }

    private void executeMovement(AIEntity self, float dt) {
        switch (self.state) {
            case FLEE:
            case EVADE:
                if (self.fleeTarget != null && self.fleeTarget.alive) {
                    movement.fleeFrom(self, self.fleeTarget, dt);
                } else {
                    self.state = AIState.RECOVER;
                    self.stateTimer = 0f;
                    movement.wander(self, dt);
                }
                break;
            case CHASE:
                if (self.currentTarget != null && self.currentTarget.alive) {
                    movement.moveToward(self, self.currentTarget, dt);
                } else {
                    self.state = AIState.RECOVER;
                    self.stateTimer = 0f;
                }
                break;
            case SEARCH_FOOD:
                if (self.currentTarget != null && self.currentTarget.alive) {
                    movement.moveToward(self, self.currentTarget, dt);
                } else {
                    self.state = AIState.WANDER;
                }
                break;
            case WANDER:
            case RECOVER:
            default:
                movement.wander(self, dt);
                break;
        }
    }
}
