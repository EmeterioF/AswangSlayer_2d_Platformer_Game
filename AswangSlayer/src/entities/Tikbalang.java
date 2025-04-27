package entities;

import static utilz.Constants.EnemyConstants.*;
import static utilz.HelpMethods.*;
import static utilz.Constants.Directions.*;
import static utilz.Constants.GRAVITY;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;

import audio.AudioManager;
import main.Game;

public class Tikbalang extends Enemy {
    
    // State constants for boss behavior
    private static final int STATE_PATROLLING = 0;
    private static final int STATE_CHASING = 1;
    private static final int STATE_ATTACKING = 2;
    private static final int STATE_SPECIAL_ATTACK = 3;
    
    // Track boss behavior
    private int bossBehaviorState = STATE_PATROLLING;
    private float chaseSpeed = 0.8f * Game.SCALE; // Boss is faster than regular enemies
    
    // Special attack variables
    private boolean isDoingSpecialAttack = false;
    private float jumpHeight = -4.0f * Game.SCALE; // Higher jump for special attack
    private boolean jumpingForSpecialAttack = false;
    private float specialAttackDistance = Game.TILES_SIZE * 5f; // Longer range for special attack
    private boolean specialAttackChecked = false;
    
    // Timers
    private long lastDirectionChangeTime = 0;
    private long minDirectionChangeDelay = 500; // milliseconds
    private int specialAttackCooldown = 3000; // 3 seconds cooldown
    private long lastSpecialAttackTime = 0;
    
    // Attack boxes
    private Rectangle2D.Float attackBox;
    private Rectangle2D.Float specialAttackBox;
    private int attackBoxOffsetX;
    private int specialAttackHitFrame = 10; // The animation frame when special attack hits
    
    public Tikbalang(float x, float y) {
        // Use TIKBALNG constant from EnemyConstants
        super(x, y - 80, TIKBALANG_WIDTH, TIKBALANG_HEIGHT, TIKBALANG);
        
        // Make hitbox smaller than sprite for better gameplay
        float hitboxWidth = TIKBALANG_WIDTH * 0.45f;
        float hitboxHeight = TIKBALANG_HEIGHT * 0.28f;
        float hitboxX = x + (TIKBALANG_WIDTH/2) - (hitboxWidth/2);
        
        initHitbox(hitboxX, y - 80, (int)hitboxWidth, (int)hitboxHeight);
        
        // Initialize attack boxes
        initAttackBoxes();
        
        // Boss has more health than regular enemies
        maxHealth = 200;
        currentHealth = maxHealth;
        
        // Boss moves faster
        walkSpeed = 0.5f * Game.SCALE;
    }
    
    private void initAttackBoxes() {
        // Normal attack box
        attackBox = new Rectangle2D.Float(x, y, (int)(120 * Game.SCALE), (int)(70 * Game.SCALE));
        attackBoxOffsetX = (int)(Game.SCALE * 40);
        
        // Special attack box is larger and positioned below the boss
        specialAttackBox = new Rectangle2D.Float(x, y, (int)(200 * Game.SCALE), (int)(50 * Game.SCALE));
    }
    
    public void update(int[][] lvlData, Player player) {
        // First update behavior based on environment
        updateBehavior(lvlData, player);
        
        // Then handle animation ticks
        updateAnimationTick();
        
        // Update attack box positions
        updateAttackBoxes();
    }
    
    private void updateAttackBoxes() {
        // Update normal attack box position
        attackBox.x = hitbox.x - attackBoxOffsetX;
        attackBox.y = hitbox.y;
        
        // Update special attack box position (centered under boss)
        specialAttackBox.x = hitbox.x - specialAttackBox.width/2 + hitbox.width/2;
        specialAttackBox.y = hitbox.y + hitbox.height;
    }
    
    private void updateBehavior(int[][] lvlData, Player player) {
        if (firstUpdate) {
            firstUpdateCheck(lvlData);
            return;
        }
        
        if (inAir) {
            // Handle special logic for diving attack when in air
            if (jumpingForSpecialAttack) {
                handleSpecialAttackJump(lvlData, player);
            } else {
                updateInAir(lvlData);
            }
            return;
        }
        
        // State machine for boss behavior
        switch (enemyState) {
            case IDLE:
                newState(RUNNING);
                break;
            case RUNNING:
                // Check if we can see the player 
                if (canSeePlayer(lvlData, player))
                    turnTowardsPlayer(player);
                
                // Check for attack opportunities
                if (isPlayerCloseForSpecialAttack(player) && canDoSpecialAttack())
                    startSpecialAttack();
                else if (isPlayerCloseForAttack(player))
                    newState(ATTACK);
                
                // Move based on behavior state
                if (bossBehaviorState == STATE_PATROLLING)
                    move(lvlData);
                else if (bossBehaviorState == STATE_CHASING)
                    chasePlayer(lvlData, player);
                break;
            case ATTACK:
                if (aniIndex == 0)
                    attackChecked = false;
                
                // Check for player hit at specific animation frame
                if (aniIndex == 15 && !attackChecked) {
                    checkPlayerHit(attackBox, player);
                    // Play attack sound
                    AudioManager.playSFX("res/audio/boss_attack.wav");
                }
                break;
            case SPRCIAL_ATTACK:
                if (aniIndex == 0) {
                    specialAttackChecked = false;
                    jumpingForSpecialAttack = true;
                    fallSpeed = jumpHeight; // Start the jump
                    inAir = true;
                }
                
                // The actual damage check happens in handleSpecialAttackJump
                break;
            case HIT:
                // No movement when hit
                break;
            case DEAD:
                // No movement when dead
                break;
        }
        
        // Update boss behavior state
        updateBossBehaviorState(player);
    }
    
    private boolean canDoSpecialAttack() {
        long currentTime = System.currentTimeMillis();
        return currentTime - lastSpecialAttackTime >= specialAttackCooldown;
    }
    
    private void startSpecialAttack() {
        isDoingSpecialAttack = true;
        newState(SPRCIAL_ATTACK);
        lastSpecialAttackTime = System.currentTimeMillis();
        
        // Play special attack sound
        AudioManager.playSFX("res/audio/boss_special.wav");
    }
    
    private void handleSpecialAttackJump(int[][] lvlData, Player player) {
        // Going up phase
        if (fallSpeed < 0) {
            if (CanMoveHere(hitbox.x, hitbox.y + fallSpeed, hitbox.width, hitbox.height, lvlData)) {
                hitbox.y += fallSpeed;
                fallSpeed += GRAVITY;
            } else {
                // Hit ceiling, reverse direction
                fallSpeed = 0;
            }
        } 
        // Coming down phase
        else {
            // Fall faster during special attack for dramatic effect
            fallSpeed += GRAVITY * 1.5f;
            
            // Check if we can move down
            if (CanMoveHere(hitbox.x, hitbox.y + fallSpeed, hitbox.width, hitbox.height, lvlData)) {
                hitbox.y += fallSpeed;
            } else {
                // We've hit the ground!
                inAir = false;
                hitbox.y = GetEntityYPosUnderRoofOrAboveFloor(hitbox, fallSpeed);
                fallSpeed = 0;
                jumpingForSpecialAttack = false;
                
                // Create ground impact effect
                if (!specialAttackChecked) {
                    specialAttackChecked = true;
                    checkSpecialAttackHit(player);
                    
                    // Play impact sound
                    AudioManager.playSFX("res/audio/boss_impact.wav");
                }
                
                // Return to running state after special attack
                newState(IDLE);
                isDoingSpecialAttack = false;
            }
        }
    }
    
    private void checkSpecialAttackHit(Player player) {
        // Check if player is hit by special attack
        if (!player.isInvincible() && specialAttackBox.intersects(player.getHitbox())) {
            // Special attack deals more damage
            player.changeHealth(-25);
            
            // Stronger knockback from special attack
            float knockbackDirection = player.getHitbox().x < hitbox.x ? -2 : 2;
            player.applyKnockback(knockbackDirection);
        }
    }
    
    protected void checkPlayerHit(Rectangle2D.Float attackBox, Player player) {
        if (!player.isInvincible() && attackBox.intersects(player.getHitbox())) {
            player.changeHealth(-GetEnemyDmg(enemyType));
            
            // Add knockback
            float knockbackDirection = player.getHitbox().x < hitbox.x ? -1 : 1;
            player.applyKnockback(knockbackDirection);
        }
        attackChecked = true;
    }
    
    private void updateBossBehaviorState(Player player) {
        // Don't change state if hit, dead, or doing special attack
        if (enemyState == HIT || enemyState == DEAD || isDoingSpecialAttack)
            return;
            
        // Calculate player distance
        float playerDistX = Math.abs(player.getHitbox().x - hitbox.x);
        float playerDistY = Math.abs(player.getHitbox().y - hitbox.y);
        
        // Different behavior states based on distance
        if (playerDistX <= attackDistance && playerDistY < 50 * Game.SCALE) {
            bossBehaviorState = STATE_ATTACKING;
        }
        else if (playerDistX <= specialAttackDistance && playerDistY < 150 * Game.SCALE && canDoSpecialAttack()) {
            bossBehaviorState = STATE_SPECIAL_ATTACK;
            
            // Occasionally use special attack when in range
            if (Math.random() < 0.01) { // 1% chance per frame when in range
                startSpecialAttack();
            }
        }
        else if (playerDistX < 400 * Game.SCALE) {
            bossBehaviorState = STATE_CHASING;
            
            // Turn towards player occasionally
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastDirectionChangeTime > minDirectionChangeDelay) {
                turnTowardsPlayer(player);
                lastDirectionChangeTime = currentTime;
            }
        }
        else {
            bossBehaviorState = STATE_PATROLLING;
        }
    }
    
    private boolean isPlayerCloseForSpecialAttack(Player player) {
        int absValueX = (int) Math.abs(player.hitbox.x - hitbox.x);
        int absValueY = (int) Math.abs(player.hitbox.y - hitbox.y);
        return absValueX <= specialAttackDistance && absValueY <= 150 * Game.SCALE;
    }
    
    // Sprite flipping methods
    public int flipX() {
        if (walkDir == RIGHT)
            return 0;
        else
            return width;
    }
    
    public int flipW() {
        if (walkDir == RIGHT)
            return 1;
        else
            return -1;
    }
    
    // Chase player at a faster speed
    private void chasePlayer(int[][] lvlData, Player player) {
        float xSpeed;
        
        // Use chase speed which is faster than walk speed
        if (walkDir == LEFT)
            xSpeed = -chaseSpeed;
        else
            xSpeed = chaseSpeed;
            
        // Move in current direction with floor check
        if (CanMoveHere(hitbox.x + xSpeed, hitbox.y, hitbox.width, hitbox.height, lvlData)) {
            if (IsFloor(hitbox, xSpeed, lvlData)) {
                hitbox.x += xSpeed;
            } else {
                // No floor ahead, change direction
                changeWalkDir();
                lastDirectionChangeTime = System.currentTimeMillis();
            }
        } else {
            // Obstacle ahead, change direction
            changeWalkDir();
            lastDirectionChangeTime = System.currentTimeMillis();
        }
    }
    
    // Draw debug info for development
    @Override
    public void drawHitbox(Graphics g, int xLvlOffset) {
        // Basic hitbox
        g.setColor(Color.RED);
        g.drawRect((int)hitbox.x - xLvlOffset, (int)hitbox.y, 
                  (int)hitbox.width, (int)hitbox.height);
        
        // Attack boxes
        g.setColor(Color.YELLOW);
        g.drawRect((int)(attackBox.x - xLvlOffset), (int)attackBox.y, 
                  (int)attackBox.width, (int)attackBox.height);
        
        if (isDoingSpecialAttack || enemyState == SPRCIAL_ATTACK) {
            g.setColor(Color.ORANGE);
            g.drawRect((int)(specialAttackBox.x - xLvlOffset), (int)specialAttackBox.y, 
                      (int)specialAttackBox.width, (int)specialAttackBox.height);
        }
        
        // Show state indicator above boss head
        g.setColor(getStateColor());
        g.fillRect((int)hitbox.x - xLvlOffset, (int)hitbox.y - 20, 30, 15);
        
        // Show state text and animation info
        g.setColor(Color.WHITE);
        String stateText = getStateText();
        g.drawString(stateText, (int)hitbox.x - xLvlOffset - 10, (int)hitbox.y - 25);
        
        // Show animation info
        String aniText = "A:" + aniIndex + "/" + GetSpriteAmount(enemyType, enemyState);
        g.drawString(aniText, (int)hitbox.x - xLvlOffset - 10, (int)hitbox.y - 40);
        
        // Show health
        String healthText = "HP:" + currentHealth + "/" + maxHealth;
        g.drawString(healthText, (int)hitbox.x - xLvlOffset - 10, (int)hitbox.y - 55);
    }
    
    private Color getStateColor() {
        switch (bossBehaviorState) {
            case STATE_PATROLLING: return Color.BLUE;
            case STATE_CHASING: return Color.GREEN;
            case STATE_ATTACKING: return Color.RED;
            case STATE_SPECIAL_ATTACK: return Color.MAGENTA;
            default: return Color.WHITE;
        }
    }
    
    private String getStateText() {
        switch (bossBehaviorState) {
            case STATE_PATROLLING: return "Patrol";
            case STATE_CHASING: return "Chase";
            case STATE_ATTACKING: return "Attack";
            case STATE_SPECIAL_ATTACK: return "Special";
            default: return "Unknown";
        }
    }
    
    // Reset boss to initial state
    public void resetEnemy() {
        hitbox.x = x;
        hitbox.y = y;
        firstUpdate = true;
        currentHealth = maxHealth;
        newState(IDLE);
        active = true;
        fallSpeed = 0;
        isDoingSpecialAttack = false;
        jumpingForSpecialAttack = false;
        lastSpecialAttackTime = 0;
    }
}