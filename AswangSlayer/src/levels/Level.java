package levels;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import static utilz.HelpMethods.GetLevelData;
import static utilz.HelpMethods.GetSigbin;
import entities.Sigbin;
import entities.Tikbalang;
import main.Game;
import static utilz.Constants.EnemyConstants.TIKBALANG;

public class Level {
	
	private BufferedImage img;
	private ArrayList<Sigbin> sigbins;
	private int [][] lvlData;
	
	private int lvlTilesWide;
	private int maxTilesOffset;
	private int maxLvlOffsetX;

	private ArrayList<Tikbalang> tikbalangs;

	// In the constructor
	
	public Level(BufferedImage img) {
		this.img = img;
		createLevelData();
		createEnemies();
		calculateLevelOffsets();
		tikbalangs = new ArrayList<>();
	}
	
	private void calculateLevelOffsets() {
	    lvlTilesWide = img.getWidth();
	    maxTilesOffset = lvlTilesWide - Game.TILES_IN_WIDTH;
	    maxLvlOffsetX = Game.TILES_SIZE * maxTilesOffset;  
	}

	private void createEnemies() {
		sigbins = GetSigbin(img);
	}

	private void createLevelData() {
		lvlData = GetLevelData(img);
	}

	public int getSpriteIndex(int x, int y) {
		return lvlData[y][x]; 
	}
	
	public int [][] getLvlData(){
		return lvlData;
	}
	
	public int getLvlOffset() {
		return maxLvlOffsetX;
	}
	
	public ArrayList<Sigbin> getSigbins(){
		return sigbins;
	}
	
	

	// Method to get tikbalangs
	public ArrayList<Tikbalang> getTikbalangs() {
	    return tikbalangs;
	}

	// In your level loading method, add code to find boss placement
	private void analyzeLevel() {
	    // Your existing code for other entities...
	    
	    // For Boss enemy
	    for (int y = 0; y < lvlData.length; y++) {
	        for (int x = 0; x < lvlData[y].length; x++) {
	            int color = lvlData[y][x];
	            if (color == TIKBALANG) {
	                tikbalangs.add(new Tikbalang(x * Game.TILES_SIZE, y * Game.TILES_SIZE));
	            }
	        }
	    }
	}
	
}
