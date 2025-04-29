package com.dinosaur.dinosaurexploder.controller;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.time.TimerAction;
import com.dinosaur.dinosaurexploder.components.*;
import com.dinosaur.dinosaurexploder.constants.EntityType;
import com.dinosaur.dinosaurexploder.constants.GameConstants;
import com.dinosaur.dinosaurexploder.model.*;
import com.dinosaur.dinosaurexploder.utils.LevelManager;
import com.dinosaur.dinosaurexploder.utils.SettingsProvider;
import com.dinosaur.dinosaurexploder.view.DinosaurGUI;
import com.dinosaur.dinosaurexploder.utils.LanguageManager;
import com.dinosaur.dinosaurexploder.view.GameOverDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import static com.almasb.fxgl.dsl.FXGL.*;
import static com.almasb.fxgl.dsl.FXGLForKtKt.getUIFactoryService;
import static com.almasb.fxgl.dsl.FXGLForKtKt.spawn;
import static javafx.util.Duration.seconds;

/**
 * Summary :
 *      The Factory handles the Dinosaur , player controls and collision detection of all entities in the game
 */
public class DinosaurController {
    LanguageManager languageManager = LanguageManager.getInstance();
    private Entity player;
    private Entity score;
    private Entity bomb;
    private CollectedCoinsComponent collectedCoinsComponent;
    private Entity levelDisplay;
    private Entity life;
    private LevelManager levelManager;
    private TimerAction enemySpawnTimer;
    private boolean isSpawningPaused = false;
    private BossSpawner bossSpawner;
    private final Settings settings = SettingsProvider.loadSettings();

    /**
     * Summary :
     *      Detecting the player damage to decrease the lives and checking if the game is over
     */
     public void damagePlayer() {
        if(player.getComponent(PlayerComponent.class).isInvincible()){
            return; 
        }
        int lives = life.getComponent(LifeComponent.class).decreaseLife(1);
        var flash = new Rectangle(DinosaurGUI.WIDTH, DinosaurGUI.HEIGHT, Color.rgb(190, 10, 15, 0.5));
        getGameScene().addUINode(flash);
        runOnce(() -> getGameScene().removeUINode(flash), seconds(0.5));

        if (lives <= 0) {
            // Added extra line of code to sync the lives counter after death
            // All hearts disappear after death
            life.getComponent(LifeComponent.class).onUpdate(lives);
            System.out.println("Game Over!");
            gameOver();
        } else {
            System.out.printf("%d lives remaining ! ", lives);
        }
    }
    /**
     * Summary :
     *      To move the space shuttle in forward , backward , right , left directions
     */
    public void initInput() {
        onKey(KeyCode.UP, () -> player.getComponent(PlayerComponent.class).moveUp());
        onKey(KeyCode.DOWN, () -> player.getComponent(PlayerComponent.class).moveDown());
        onKey(KeyCode.LEFT, () -> player.getComponent(PlayerComponent.class).moveLeft());
        onKey(KeyCode.RIGHT, () -> player.getComponent(PlayerComponent.class).moveRight());

        onKeyDown(KeyCode.SPACE, () -> player.getComponent(PlayerComponent.class).shoot(settings.isMuted()));

        onKey(KeyCode.W, () -> player.getComponent(PlayerComponent.class).moveUp());
        onKey(KeyCode.S, () -> player.getComponent(PlayerComponent.class).moveDown());
        onKey(KeyCode.A, () -> player.getComponent(PlayerComponent.class).moveLeft());
        onKey(KeyCode.D, () -> player.getComponent(PlayerComponent.class).moveRight());

        onKeyDown(KeyCode.B, () -> bomb.getComponent(BombComponent.class).useBomb(player));
    }

    public void initGame() {
        initGameEntities();
        levelManager = new LevelManager();
        bossSpawner = new BossSpawner(settings,levelManager);
        CoinSpawner coinSpawner = new CoinSpawner(10, 1.0);

        if(!settings.isMuted()) {
            FXGL.play(GameConstants.BACKGROUND_SOUND);
        }

        new CountdownAnimation(3).startCountdown(() -> {
            resumeEnemySpawning();
            spawnEnemies();
            coinSpawner.startSpawning();
        });
    }

    private void initGameEntities(){
        spawn("background", 0, 0);
        player = spawn("player", getAppCenter().getX() - 45, getAppHeight() - 200);
        levelDisplay = spawn("Level", getAppCenter().getX() - 270, getAppCenter().getY() + 350);
        levelDisplay.setZIndex(100);
        score = spawn("Score", getAppCenter().getX() - 270, getAppCenter().getY() - 350);
        life = spawn("Life", getAppCenter().getX() - 260, getAppCenter().getY() + 290);
        bomb = spawn("Bomb", getAppCenter().getX() - 260, getAppCenter().getY() - 280);
        Entity coin = spawn("Coins", getAppCenter().getX() - 260, getAppCenter().getY() - 235);
        collectedCoinsComponent = coin.getComponent(CollectedCoinsComponent.class);
        bomb.addComponent(new BombComponent());
    }

    /**
     * Summary :
     *      This method is used to spawn the enemies
     *      and set the spawn rate of the enemies
     */
    private void spawnEnemies(){
        if(enemySpawnTimer != null){
            enemySpawnTimer.expire();
        }

        enemySpawnTimer = run(() -> {
            if(levelManager.getCurrentLevel()%5==0){
                pauseEnemySpawning();
                bossSpawner.spawnNewBoss();
            }else {
                if (!isSpawningPaused && random(0, 2) < 2) {
                    Entity greenDino = spawn("greenDino", random(0, getAppWidth() - 80), -50);
                    greenDino.getComponent(GreenDinoComponent.class).setMuted(settings.isMuted());
                }
            }
        }, seconds(levelManager.getEnemySpawnRate()));
    }

    /**
     * Summary :
     *      This method is used to pause the enemy spawning
     */
    private void pauseEnemySpawning(){
        isSpawningPaused = true;
        if(enemySpawnTimer != null){
            enemySpawnTimer.pause();
        }
    }

    /**
     * Summary :
     *      This method is used to resume the enemy spawning
     */
    private void resumeEnemySpawning(){
        isSpawningPaused = false;
        if(enemySpawnTimer != null){
            enemySpawnTimer.resume();
        } else{
            spawnEnemies();
        }
    }

    /**
     * Summary :
     *      Handles level progression when enemies are defeated
     *      and shows a message when the level is changed
     */
    private void showLevelMessage(){
        //Pause game elements during level transition
        FXGL.getGameWorld().getEntitiesByType(EntityType.GREEN_DINO).forEach(e -> {
            if(e.hasComponent(GreenDinoComponent.class)) {
                e.getComponent(GreenDinoComponent.class).setPaused(true);
            }
        });

        pauseEnemySpawning();

        //Display centered level notification
        Text levelText = getUIFactoryService().newText(languageManager.getTranslation("level") + levelManager.getCurrentLevel(), Color.WHITE, 24);
        levelText.setStroke(Color.BLACK);
        levelText.setStrokeWidth(1.5);
        centerText(levelText);
        getGameScene().addUINode(levelText);

        // Trigger bomb regeneration for level advancement
        if (bomb.hasComponent(BombComponent.class)) {
            bomb.getComponent(BombComponent.class).checkLevelForBombRegeneration(levelManager.getCurrentLevel());
        }
        
        // Resume gameplay after a delay
        runOnce(() -> {
            getGameScene().removeUINode(levelText);
            updateLevelDisplay();

            FXGL.getGameWorld().getEntitiesByType(EntityType.GREEN_DINO).forEach(e -> {
                if(e.hasComponent(GreenDinoComponent.class)){
                    e.getComponent(GreenDinoComponent.class).setPaused(false);
                }
            });

            resumeEnemySpawning();

            player.getComponent(PlayerComponent.class).setInvincible(true);
            runOnce(() -> {
                if(player != null && player.isActive()){
                    player.getComponent(PlayerComponent.class).setInvincible(false);
                }
            }, seconds(3));
        }, seconds(2));
    }

    /**
     * Summary :
     *      Center the text on the screen
     */
    private void centerText(Text text){
        text.setX((getAppWidth() - text.getLayoutBounds().getWidth()) / 2.0);
        text.setY(getAppHeight() / 2.0);
    }

    public void updateLevelDisplay(){
        Text levelText = (Text) levelDisplay.getViewComponent().getChildren().get(0);
        levelText.setText(languageManager.getTranslation("level") + ": " + levelManager.getCurrentLevel());

        // Regenerate bombs when level changes
        if (bomb.hasComponent(BombComponent.class)) {
            bomb.getComponent(BombComponent.class).checkLevelForBombRegeneration(levelManager.getCurrentLevel());
        }
    }

    /**
     * Summary :
     *      Detect the collision between the game elements.
     */
    public void initPhysics() {
        /*
         * After collision between projectile and greenDino there hava explosion animation
         * and there have 5% chance to spawn a heart
         */
        onCollisionBegin(EntityType.PROJECTILE, EntityType.GREEN_DINO, (projectile, greenDino) -> {
            spawn("explosion", greenDino.getX() - 25, greenDino.getY() - 30);
            if (random(0, 100) < 5) {
                spawn("heart", greenDino.getX(), greenDino.getY());
            }
            if(!settings.isMuted()) {
                FXGL.play(GameConstants.ENEMY_EXPLODE_SOUND);
            }
            projectile.removeFromWorld();
            greenDino.removeFromWorld();
            score.getComponent(ScoreComponent.class).incrementScore(1);
            levelManager.incrementDefeatedEnemies();
            if(levelManager.shouldAdvanceLevel()){
                levelManager.nextLevel();
                showLevelMessage();
                System.out.println("Level up!");
            }
        });

        /*
         * After collision between projectile and redDino they
         * have an explosion animation,
         * and lose one life.
         * if defeated they
         * have a 100% chance to spawn a heart,
         * spawn coins depending on the current level
         *
         */
        onCollisionBegin(EntityType.PROJECTILE, EntityType.RED_DINO, (projectile, redDino) -> {
            spawn("explosion", redDino.getX() - 25, redDino.getY() - 30);
            projectile.removeFromWorld();
            if(!settings.isMuted()) {
                FXGL.play(GameConstants.ENEMY_EXPLODE_SOUND);
            }
            redDino.getComponent(RedDinoComponent.class).damage(1);

            if (redDino.getComponent(RedDinoComponent.class).getLives() == 0) {
                // if the boss is defeated it drops 100% a heart
                spawn("heart", redDino.getX(), redDino.getY());
                // if the boss dino is defeated it drops as many coins as the current level
                for (int i = 0; i<levelManager.getCurrentLevel(); i++){
                    spawn("coin", redDino.getX()+random(-25,25), redDino.getY()+random(-25,25));
                }
                bossSpawner.removeBossEntities();

                score.getComponent(ScoreComponent.class).incrementScore(levelManager.getCurrentLevel());
                levelManager.nextLevel();
                showLevelMessage();
                System.out.println("Level up!");
            } else{
                bossSpawner.updateHealthBar();
            }

        });

        onCollisionBegin(EntityType.PROJECTILE, EntityType.ENEMY_PROJECTILE, (projectile, enemyProjectile) -> {
            spawn("explosion", enemyProjectile.getX() - 25, enemyProjectile.getY() - 30);
            if(!settings.isMuted()) {
                FXGL.play(GameConstants.ENEMY_EXPLODE_SOUND);
            }
            projectile.removeFromWorld();
            enemyProjectile.removeFromWorld();
        });

        onCollisionBegin(EntityType.ENEMY_PROJECTILE, EntityType.PLAYER, (projectile, player) -> {
            if(!settings.isMuted()) {
                FXGL.play(GameConstants.PLAYER_HIT_SOUND);
            }
            projectile.removeFromWorld();
            System.out.println("You got hit !\n");
            damagePlayer();
        });

        onCollisionBegin(EntityType.PLAYER, EntityType.GREEN_DINO, (player, greenDino) -> {
            if(!settings.isMuted()) {
                FXGL.play(GameConstants.PLAYER_HIT_SOUND);
            }
            greenDino.removeFromWorld();
            System.out.println("You touched a dino !");
            damagePlayer();
        });

        onCollisionBegin(EntityType.PLAYER, EntityType.RED_DINO, (player, redDino) -> {
            if(!settings.isMuted()) {
                FXGL.play(GameConstants.PLAYER_HIT_SOUND);
            }
            System.out.println("You touched a red dino !");
            damagePlayer();
        });

        onCollisionBegin(EntityType.PLAYER, EntityType.COIN, (player, coin) -> {
            if(!settings.isMuted()){
                FXGL.play(GameConstants.COIN_GAIN);
            }
            coin.removeFromWorld();
            System.out.println("You touched a coin!");
            collectedCoinsComponent.incrementCoin();
            
            // Check for bomb regeneration when coin is collected
            if (bomb.hasComponent(BombComponent.class)) {
                bomb.getComponent(BombComponent.class).trackCoinForBombRegeneration();
            }
        });

        onCollisionBegin(EntityType.PLAYER, EntityType.HEART, (player, heart) -> {
            if(!settings.isMuted()){
                FXGL.play(GameConstants.HEART_HIT_SOUND);
            }
            heart.removeFromWorld();
            System.out.println("You touched a heart!");
            life.getComponent(LifeComponent.class).increaseLife(1);
        });
    }

    /**
     * Summary :
     *      To detect whether the player lives are empty or not
     */
    public void gameOver() {
        new GameOverDialog(languageManager).createDialog();
    }
}