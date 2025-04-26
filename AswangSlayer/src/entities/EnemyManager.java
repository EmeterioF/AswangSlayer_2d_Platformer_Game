package entities;

import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import gameStates.Playing;
import levels.Level;
import main.Game;
import utilz.LoadSave;
import static utilz.Constants.EnemyConstants.*;

public class EnemyManager {

    private Playing playing;
    private BufferedImage[][] sigbinArr;
    private ArrayList<Sigbin> sigbins = new ArrayList<>();

    public EnemyManager(Playing playing) {
        this.playing = playing;
        loadEnemyImgs();
    }
    
    
    public void loadEnemies(Level level) {
        sigbins = level.getSigbins();
    }

    // Update method to include player parameter
    public void update(int[][] lvlData, Player player) {
        boolean isAnyActive = false;
        for (Sigbin s : sigbins) {
            if(s.isActive()) {
                s.update(lvlData, player);
                isAnyActive = true;
            }
        }
        if(!isAnyActive)
            playing.setLevelCompleted(true);
    }

    public void draw(Graphics g, int xLvlOffset) {
        drawSigbins(g, xLvlOffset);
    }

    // In EnemyManager's drawSigbins method
    private void drawSigbins(Graphics g, int xLvlOffset) {
        for (Sigbin c : sigbins) {
            if(c.isActive()) {  // Show even when not active if in DEAD state
                
                // Draw the sprite with offset from hitbox
                g.drawImage(sigbinArr[c.getEnemyState()][c.getAniIndex()], 
                           (int) c.getHitbox().x - xLvlOffset - SIGBIN_DRAWOFFSET_X + c.flipX(),
                           (int) c.getHitbox().y - SIGBIN_DRAWOFFSET_Y, 
                           SIGBIN_WIDTH * c.flipW(), 
                           SIGBIN_HEIGHT, null);
                
                // Draw hitbox for debugging
                c.drawHitbox(g, xLvlOffset);
            }
        }
    }
    
    private void loadEnemyImgs() {
        sigbinArr = new BufferedImage[5][30];
        BufferedImage temp = LoadSave.GetSpriteAtlas(LoadSave.SIGBIN_ATLAS);
        for (int j = 0; j < sigbinArr.length; j++)
            for (int i = 0; i < sigbinArr[j].length; i++)
                sigbinArr[j][i] = temp.getSubimage(i * WIDTH_DEFAULT, j * HEIGHT_DEFAULT, WIDTH_DEFAULT, HEIGHT_DEFAULT);
    }
    
    // Fixed method to check for enemy-player collisions
    public void checkPlayerHit(Player player) {
        // Don't check for hits if player is already invincible
        if (player.isInvincible())
            return;
            
        // Logic to check if enemies touch player's hitbox
        for (Sigbin s : sigbins) {
            if (s.isActive()) {
                if (s.getHitbox().intersects(player.getHitbox())) {
                    // Apply damage to player
                    player.changeHealth(-10); // Adjust damage amount as needed
                    
                    // Apply knockback in the opposite direction of enemy
                    float knockbackDirection = player.getHitbox().x < s.getHitbox().x ? -1 : 1;
                    player.applyKnockback(knockbackDirection);
                    
                    // This return prevents multiple enemies from hitting player in the same frame
                    return;
                }
            }
        }
    }
    
    // Method to check if player attacks hit enemies
    public void checkEnemyHit(Rectangle2D.Float attackBox, int damage) {
        for (Sigbin s : sigbins)
            if (s.isActive())
                if (attackBox.intersects(s.getHitbox())) {
                    s.hurt(damage);
                    return; // Exit after hitting one enemy
                }
    }
    
    public void resetAllEnemies() {
        for (Sigbin s : sigbins)
            s.resetEnemy();
    }
}