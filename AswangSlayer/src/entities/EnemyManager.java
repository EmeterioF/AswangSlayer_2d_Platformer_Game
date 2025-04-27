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
    private BufferedImage[][] tikbalangArr;
    
    private ArrayList<Sigbin> sigbins = new ArrayList<>();
    private ArrayList<Tikbalang> tikbalangs = new ArrayList<>();
    
    public EnemyManager(Playing playing) {
        this.playing = playing;
        loadEnemyImgs();
    }
    
    
    public void loadEnemies(Level level) {
        sigbins = level.getSigbins();
        tikbalangs = level.getTikbalangs();
    }

    // Update method to include player parameter
    public void update(int[][] lvlData, Player player) {
        // Update regular enemies
        boolean isAnyRegularEnemyActive = false;
        for (Sigbin s : sigbins) {
            if(s.isActive()) {
                s.update(lvlData, player);
                isAnyRegularEnemyActive = true;
            }
        }
        
        // Update boss enemies
        boolean isAnyBossActive = false;
        for (Tikbalang t : tikbalangs) {
            if(t.isActive()) {
                t.update(lvlData, player);
                isAnyBossActive = true;
            }
        }
        
        // Level is completed when no enemies are active
        if(!isAnyRegularEnemyActive && !isAnyBossActive)
            playing.setLevelCompleted(true);
    }

    public void draw(Graphics g, int xLvlOffset) {
        drawSigbins(g, xLvlOffset);
        drawTikbalangs(g, xLvlOffset); // Draw boss enemies
    }
    
    private void drawTikbalangs(Graphics g, int xLvlOffset) {
        for (Tikbalang t : tikbalangs) {
            if(t.isActive()) {
                // Draw the boss sprite with offset from hitbox
                g.drawImage(tikbalangArr[t.getEnemyState()][t.getAniIndex()], 
                           (int) t.getHitbox().x - xLvlOffset - TIKBALANG_DRAWOFFSET_X + t.flipX(),
                           (int) t.getHitbox().y - TIKBALANG_DRAWOFFSET_Y, 
                           TIKBALANG_WIDTH * t.flipW(), 
                           TIKBALANG_HEIGHT, null);
                
                // Uncomment to see hitboxes
                // t.drawHitbox(g, xLvlOffset);
            }
        }
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
//                c.drawHitbox(g, xLvlOffset);
            }
        }
    }
    
    private void loadEnemyImgs() {
        // Load regular enemy sprites
        sigbinArr = new BufferedImage[5][30];
        BufferedImage temp = LoadSave.GetSpriteAtlas(LoadSave.SIGBIN_ATLAS);
        for (int j = 0; j < sigbinArr.length; j++)
            for (int i = 0; i < sigbinArr[j].length; i++)
                sigbinArr[j][i] = temp.getSubimage(i * WIDTH_DEFAULT, j * HEIGHT_DEFAULT, WIDTH_DEFAULT, HEIGHT_DEFAULT);
                
        // Load boss sprites (with extra state for special attack)
        tikbalangArr = new BufferedImage[6][30];
        BufferedImage bossTemp = LoadSave.GetSpriteAtlas(LoadSave.TIKBALANG_ATLAS);
        for (int j = 0; j < tikbalangArr.length; j++)
            for (int i = 0; i < tikbalangArr[j].length; i++)
                tikbalangArr[j][i] = bossTemp.getSubimage(i * WIDTH_DEFAULT, j * HEIGHT_DEFAULT, WIDTH_DEFAULT, HEIGHT_DEFAULT);
    }
    
    // Fixed method to check for enemy-player collisions
    public void checkPlayerHit(Player player) {
        // Don't check for hits if player is already invincible
        if (player.isInvincible())
            return;
            
        // Check regular enemies
        for (Sigbin s : sigbins) {
            if (s.isActive() && s.getHitbox().intersects(player.getHitbox())) {
                player.changeHealth(-GetEnemyDmg(SIGBIN));
                float knockbackDirection = player.getHitbox().x < s.getHitbox().x ? -1 : 1;
                player.applyKnockback(knockbackDirection);
                return;
            }
        }
        
        // Check boss enemies with stronger impact
        for (Tikbalang t : tikbalangs) {
            if (t.isActive() && t.getHitbox().intersects(player.getHitbox())) {
                player.changeHealth(-GetEnemyDmg(TIKBALANG));
                float knockbackDirection = player.getHitbox().x < t.getHitbox().x ? -1.5f : 1.5f;
                player.applyKnockback(knockbackDirection);
                return;
            }
        }
    }
    
    // Method to check if player attacks hit enemies
    public void checkEnemyHit(Rectangle2D.Float attackBox, int damage) {
        // Check regular enemies first
        for (Sigbin s : sigbins) {
            if (s.isActive() && attackBox.intersects(s.getHitbox())) {
                s.hurt(damage);
                return; // Exit after hitting one enemy
            }
        }
        
        // Check boss enemies (takes less damage)
        for (Tikbalang t : tikbalangs) {
            if (t.isActive() && attackBox.intersects(t.getHitbox())) {
                t.hurt(damage / 2); // Boss takes half damage from regular attacks
                return; // Exit after hitting one enemy
            }
        }
    }
    
    public void resetAllEnemies() {
        for (Sigbin s : sigbins)
            s.resetEnemy();
            
        for (Tikbalang t : tikbalangs)
            t.resetEnemy();
    }
}