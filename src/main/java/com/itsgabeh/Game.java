package com.itsgabeh;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

class Game implements Runnable {
    private final int GAME_PLAYING_STATE = 0;
    private final int PAUSE_MENU_STATE = 1;
    private final int START_MENU_STATE = 2;
    private final int UPGRADE_MENU_STATE = 3;
    private final int GAME_OVER_STATE = 4;
    private String gameOverMsg = "";

    private final int HEALTH_UPGRADE = 0;
    private final int DAMAGE_UPGRADE = 1;
    private final int BIGGER_BULLETS_UPGRADE = 2;
    private final int MORE_BULLETS_UPGRADE = 3;
    private final String[] UPGRADE_NAMES = {"Health", "Damage", "Bullets", "Fire rate"};
    private final String[] UPGRADE_DESCRIPTIONS = {
            "Increase max health by 20",
            "Increase bullet damage by 5",
            "Increase bullet size x2 (can help you with bloom)",
            "Decrement Fire rate by 13%"
    };

    // Color palette, see https://github.com/morhetz/gruvbox
    private final Color BACKGROUND0_H = new Color(0x1d2021);
    private final Color BACKGROUND0_A = new Color(0x801D2021, true);
    private final Color BACKGROUND_0 = new Color(0x282828);
    private final Color FOREGROUND_0 = new Color(0xfbf1c7);
    private final Color FOREGROUND_1 = new Color(0xebdbb2);
    private final Color BLUE = new Color(0x83a598);
    private final Color BLUE_A = new Color(0x4D83A598, true);
    private final Color RED = new Color(0xcc241d);
    private final Color RED_A = new Color(0x4DCC241D, true);

    private final int SPRITE_SIZE_PX = 32;
    private BufferedImage dukesSprite;
    private BufferedImage rockSprite;
    private BufferedImage blueEnemySprite;
    private BufferedImage greenEnemySprite;
    private BufferedImage yellowEnemySprite;
    private BufferedImage redEnemySprite;
    private BufferedImage starCoreSprite;

    private final int ENEMIES_POOL_CAPACITY = 5;
    private final int[] ENEMIES_PER_WAVE = {5, 10, 15, 20, 25, 30, 40};

    private int currentWave = 0;
    private int currentEnemiesKilled = 0;
    private int currentActiveEnemies = 0;
    private int currentActiveRocks = 0;
    private int secondsToStartNextWave = 10;
    private final Timer nextWaveTimer;

    private final int VIEWPORT_WIDTH = 768, VIEWPORT_HEIGHT = 768;
    private final int BULLETS_POOL_CAPACITY = 50;
    private final int ROCKS_POOL_CAPACITY = 10;
    private final int STAR_CORE_X = VIEWPORT_WIDTH / 2, STAR_CORE_Y = VIEWPORT_HEIGHT / 2;

    private float dukesX = 100, dukesY = 100;
    private float dukesRotationAngle = 0;
    private int selectedSlot = 0;
    private int dukesBaseHealth = 100;
    private int dukesHealth = dukesBaseHealth;
    private int dukesBaseFramesToShoot = 45;
    private int dukesFramesToShoot = 0;
    private final int dukesBaseBulletRadius = 1;
    private int dukesBulletRadius = dukesBaseBulletRadius;
    private final int dukesBaseBulletDamage = 5;
    private int dukesBulletDamage = dukesBaseBulletDamage;
    private int starCoreHealth = 200;

    private int firstUpgradeSelectedIndex = 0;
    private int secondUpgradeSelectedIndex = 0;

    private final Canvas canvas;
    private final GameInput gameInput;
    private int currentGameState;
    private boolean isRunning = false;

    private final float[] bulletsX = new float[BULLETS_POOL_CAPACITY];
    private final float[] bulletsY = new float[BULLETS_POOL_CAPACITY];
    private final float[] bulletsDirX = new float[BULLETS_POOL_CAPACITY];
    private final float[] bulletsDirY = new float[BULLETS_POOL_CAPACITY];
    private final boolean[] bulletsActive = new boolean[BULLETS_POOL_CAPACITY];

    private final float[] enemyBulletsX = new float[BULLETS_POOL_CAPACITY];
    private final float[] enemyBulletsY = new float[BULLETS_POOL_CAPACITY];
    private final float[] enemyBulletsDirX = new float[BULLETS_POOL_CAPACITY];
    private final float[] enemyBulletsDirY = new float[BULLETS_POOL_CAPACITY];
    private final boolean[] enemyBulletsActive = new boolean[BULLETS_POOL_CAPACITY];

    private final float[] rocksX = new float[ROCKS_POOL_CAPACITY];
    private final float[] rocksY = new float[ROCKS_POOL_CAPACITY];
    private final int[] rocksHealth = new int[ROCKS_POOL_CAPACITY];
    private final boolean[] rocksActive = new boolean[ROCKS_POOL_CAPACITY];

    private final float[] enemiesX = new float[ENEMIES_POOL_CAPACITY];
    private final float[] enemiesY = new float[ENEMIES_POOL_CAPACITY];
    private final int[] enemiesHealth = new int[ENEMIES_POOL_CAPACITY];
    private final int[] enemiesDamage = new int[ENEMIES_POOL_CAPACITY];
    private final boolean[] enemiesType = new boolean[ENEMIES_POOL_CAPACITY];
    private final int[] enemiesSprite = new int[ENEMIES_POOL_CAPACITY];
    private final int[] enemiesFramesToShoot = new int[ENEMIES_POOL_CAPACITY];
    private final float[] enemiesRotation = new float[ENEMIES_POOL_CAPACITY];
    private final boolean[] enemiesActive = new boolean[ENEMIES_POOL_CAPACITY];

    private final float[] bgStarsX = new float[20];
    private final float[] bgStarsY = new float[20];
    private final boolean[] bgStarsActive = new boolean[20];

    private Clip dukesAudioClip;

    public Game()
    {
        currentGameState = START_MENU_STATE;
        nextWaveTimer = new Timer(1000, (ActionEvent _) -> secondsToStartNextWave--);
        nextWaveTimer.stop();

        try
        {
            URL spriteSheetURL = getClass().getResource("/SpriteSheet.png");
            if (spriteSheetURL != null)
            {
                BufferedImage spriteSheet = ImageIO.read(spriteSheetURL);
                dukesSprite = spriteSheet.getSubimage(0, 0, SPRITE_SIZE_PX, SPRITE_SIZE_PX);
                rockSprite = spriteSheet.getSubimage(32, 0, SPRITE_SIZE_PX, SPRITE_SIZE_PX);
                blueEnemySprite = spriteSheet.getSubimage(64, 0, SPRITE_SIZE_PX, SPRITE_SIZE_PX);
                starCoreSprite = spriteSheet.getSubimage(64, 32, SPRITE_SIZE_PX, SPRITE_SIZE_PX);
            }
            else System.out.println("Cannot load image");
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            File file = new File("/home/gabeh/IdeaProjects/Dukes-8-bit-challenge/src/main/resources/laserShoot.wav");
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
            dukesAudioClip = AudioSystem.getClip();
            dukesAudioClip.open(audioInputStream);
        }
        catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < ROCKS_POOL_CAPACITY; i++)
        {
            double randomAngle = Math.random() * (Math.PI * 2);
            rocksX[i] = (float) (STAR_CORE_X + 600 * Math.cos(randomAngle));
            rocksY[i] = (float) (STAR_CORE_Y + 600 * Math.sin(randomAngle));
            rocksHealth[i] = 100;
            rocksActive[i] = true;
            currentActiveRocks += 1;
        }

        for (int i = 0; i < ENEMIES_POOL_CAPACITY; i++)
        {
            double randomAngle = Math.random() * (Math.PI * 2);
            enemiesX[i] = (float) (STAR_CORE_X + 800 * Math.cos(randomAngle));
            enemiesY[i] = (float) (STAR_CORE_Y + 800 * Math.sin(randomAngle));
            enemiesHealth[i] = 30;
            enemiesActive[i] = true;
            enemiesType[i] = true;
            enemiesDamage[i] = 5;
            currentActiveEnemies += 1;
        }

        JFrame mainFrame = new JFrame("Game test");
        gameInput = new GameInput();
        canvas = new Canvas();
        canvas.addKeyListener(gameInput);
        canvas.addMouseMotionListener(gameInput);
        canvas.addMouseListener(gameInput);

        canvas.setPreferredSize(new Dimension(VIEWPORT_WIDTH, VIEWPORT_HEIGHT));

        mainFrame.add(canvas);
        mainFrame.pack();
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setResizable(false);
        mainFrame.setVisible(true);
    }

    public synchronized void start()
    {
        isRunning = true;
        Thread mainThread = new Thread(this, "GameLoop");
        mainThread.start();
    }

    private void physicsUpdate()
    {
        if (currentWave >= 6)
        {
            currentGameState = GAME_OVER_STATE;
            gameOverMsg = "Congratulations! YOU WON";
        }

        if (dukesHealth <= 0)
        {
            currentGameState = GAME_OVER_STATE;
            gameOverMsg = "Bad luck! YOU DIED";
        }

        if (currentGameState == GAME_PLAYING_STATE)
        {
            // Calculate a direction vector using a 'negative vector' + 'positive vector',
            // the result is a vector in the form (x, y) where -1 <= x <= 1 and -1 <= y <= 1
            int negativeX = 0, negativeY = 0;
            int positiveX = 0, positiveY = 0;

            if (gameInput.isPressed(KeyEvent.VK_W)) negativeY = -1;
            if (gameInput.isPressed(KeyEvent.VK_A)) negativeX = -1;
            if (gameInput.isPressed(KeyEvent.VK_S)) positiveY = 1;
            if (gameInput.isPressed(KeyEvent.VK_D)) positiveX = 1;

            float directionX = negativeX + positiveX;
            float directionY = negativeY + positiveY;

            // To prevent division by zero, check if magnitude > 0
            double magnitude = Math.sqrt(directionX * directionX + directionY * directionY);
            directionX = magnitude > 0 ? (float) (directionX / magnitude) : directionX;
            directionY = magnitude > 0 ? (float) (directionY / magnitude) : directionY;

            // Move dukes pointing to the direction vector
            dukesX += directionX * 1.25F;
            dukesY += directionY * 1.25F;

            // Player can collide with core, but player is pushed back on collision
            if (isColliding(dukesX, STAR_CORE_X, dukesY, STAR_CORE_Y, (float) SPRITE_SIZE_PX / 2, (float) SPRITE_SIZE_PX))
            {
                float tempDirX = STAR_CORE_X - dukesX;
                float tempDirY = STAR_CORE_Y - dukesY;
                dukesX -= (float) (tempDirX * 0.1);
                dukesY -= (float) (tempDirY * 0.1);
            }

            // Get the direction vector pointing to mouse direction
            // Mouse Pos - Dukes Pos = Direction Vector (starting from 0,0)
            // Applying atan2 calculates the angle 0 within X line and the point (x,y)
            float mouseDistanceX = gameInput.mouseX - dukesX;
            float mouseDistanceY = gameInput.mouseY - dukesY;
            dukesRotationAngle = (float) Math.atan2(mouseDistanceY, mouseDistanceX);

            boolean isShooting = gameInput.mouseLeftPressed;
            if (isShooting && selectedSlot == 0 && dukesFramesToShoot == 0)
            {
                if (dukesAudioClip != null)
                {
                    dukesAudioClip.setFramePosition(0);
                    dukesAudioClip.start();
                }

                for (int i = 0; i < BULLETS_POOL_CAPACITY; i++)
                {
                    if (!bulletsActive[i])
                    {
                       bulletsActive[i] = true;
                       bulletsX[i] = dukesX;
                       bulletsY[i] = dukesY;
                       bulletsDirX[i] = (float) Math.cos(dukesRotationAngle);
                       bulletsDirY[i] = (float) Math.sin(dukesRotationAngle);
                       break;
                    }
                }

                dukesFramesToShoot = dukesBaseFramesToShoot;
            }

            // Move duke's bullets
            for (int i = 0; i < BULLETS_POOL_CAPACITY; i++)
            {
                if (!bulletsActive[i]) continue;
                if (bulletsX[i] < 0 || bulletsX[i] > VIEWPORT_WIDTH || bulletsY[i] < 0 || bulletsY[i] > VIEWPORT_HEIGHT)
                {
                    bulletsActive[i] = false;
                }
                else
                {
                    bulletsX[i] += bulletsDirX[i] * 10;
                    bulletsY[i] += bulletsDirY[i] * 10;
                }

                // Bullets can collide with rocks
                // Check if bullet distance from the asteroid center
                for (int j = 0; j < ROCKS_POOL_CAPACITY; j++)
                {
                    if (rocksActive[j] && isColliding(rocksX[j], bulletsX[i], rocksY[j], bulletsY[i], (float) (SPRITE_SIZE_PX / 2), dukesBulletRadius)) {
                        bulletsActive[i] = false;
                        break;
                    }
                }

                // bullets can collide with the core
                if (isColliding(bulletsX[i], STAR_CORE_X, bulletsY[i], STAR_CORE_Y, dukesBulletRadius, SPRITE_SIZE_PX))
                {
                    bulletsActive[i] = false;
                }

                // bullets collide with enemies and deactivate them
                for (int j = 0; j < ENEMIES_POOL_CAPACITY; j++)
                {
                    if (!enemiesActive[j]) continue;
                    if (isColliding(enemiesX[j], bulletsX[i], enemiesY[j], bulletsY[i], (float) SPRITE_SIZE_PX / 2, dukesBulletRadius))
                    {
                        bulletsActive[i] = false;
                        enemiesHealth[j] -= dukesBulletDamage;

                        if (enemiesHealth[j] <= 0)
                        {
                            enemiesActive[j] = false;
                            currentEnemiesKilled += 1;
                            currentActiveEnemies -= 1;
                        }
                    }
                }
            }

            // Move each active asteroid towards the center but not at all
            for (int i = 0; i < ROCKS_POOL_CAPACITY; i++)
            {
                if (!rocksActive[i])
                {
                    if (currentActiveRocks < ROCKS_POOL_CAPACITY)
                    {
                        double randomAngle = Math.random() * (Math.PI * 2);
                        rocksX[i] = (float) (STAR_CORE_X + 600 * Math.cos(randomAngle));
                        rocksY[i] = (float) (STAR_CORE_Y + 600 * Math.sin(randomAngle));
                        rocksHealth[i] = 100;
                        rocksActive[i] = true;
                        currentActiveRocks += 1;
                    }

                    continue;
                }

                float dirX = ((float) STAR_CORE_X) - rocksX[i];
                float dirY = ((float) STAR_CORE_Y) - rocksY[i];
                float mag = (float) Math.sqrt((dirX * dirX) + (dirY * dirY));
                dirX = mag > 0 ? dirX / mag : dirX;
                dirY = mag > 0 ? dirY / mag : dirY;

                if (sqrDistance(rocksX[i], STAR_CORE_X, rocksY[i], STAR_CORE_Y) > 300 * 300)
                {
                    rocksX[i] += dirX;
                    rocksY[i] += dirY;
                }

                // Replace with func or sqr distance
                float distanceFromMouse = (float) Math.sqrt(Math.pow(gameInput.mouseX - rocksX[i], 2) + Math.pow(gameInput.mouseY - rocksY[i], 2));
                float distanceFromDukes = (float) Math.sqrt(Math.pow(dukesX - rocksX[i], 2) + Math.pow(dukesY - rocksY[i], 2));
                if (selectedSlot == 1 && distanceFromMouse < 25 && distanceFromDukes < 50 && gameInput.mouseLeftPressed)
                {
                    if (rocksHealth[i] > 0) rocksHealth[i] -= 1;
                    else
                    {
                        dukesHealth += dukesHealth + 20 < dukesBaseHealth ? 20 : 0;
                        rocksActive[i] = false;
                        currentActiveRocks -= 1;
                    }
                }

                // Player can collide with rocks, but player is pushed back on collision
                if (isColliding(dukesX, rocksX[i], dukesY, rocksY[i], (float) SPRITE_SIZE_PX / 2, (float) SPRITE_SIZE_PX / 2))
                {
                    float tempDirX = rocksX[i] - dukesX;
                    float tempDirY = rocksY[i] - dukesY;
                    dukesX -= (float) (tempDirX * 0.1);
                    dukesY -= (float) (tempDirY * 0.1);
                }
            }

            // Move enemies according to their type, and calculate collisions
            for (int i = 0; i < ENEMIES_POOL_CAPACITY; i++)
            {
                if (!enemiesActive[i])
                {
                    if ((currentEnemiesKilled + currentActiveEnemies) < ENEMIES_PER_WAVE[currentWave] && currentActiveEnemies < ENEMIES_POOL_CAPACITY)
                    {
                        double randomAngle = Math.random() * (Math.PI * 2);
                        enemiesX[i] = (float) (STAR_CORE_X + 800 * Math.cos(randomAngle));
                        enemiesY[i] = (float) (STAR_CORE_Y + 800 * Math.sin(randomAngle));

                        int waveModifier = (int) (Math.random() * (currentWave + 1));

                        enemiesDamage[i] = 5 + (waveModifier);
                        enemiesHealth[i] = 35 + (2 * waveModifier);
                        enemiesType[i] = waveModifier < ((currentWave / 2) + 1);

                        enemiesActive[i] = true;
                        currentActiveEnemies += 1;
                    }

                    continue;
                }

                if (enemiesType[i]) // Enemy that follows the player
                {
                    float dirX = dukesX - enemiesX[i];
                    float dirY = dukesY - enemiesY[i];
                    float mag = (float) Math.sqrt((dirX * dirX) + (dirY * dirY));
                    dirX = mag > 0 ? dirX / mag : dirX;
                    dirY = mag > 0 ? dirY / mag : dirY;

                    enemiesRotation[i] = (float) Math.atan2(dirY, dirX);

                    if (sqrDistance(enemiesX[i], dukesX, enemiesY[i], dukesY) > 100 * 100)
                    {
                        enemiesX[i] += dirX;
                        enemiesY[i] += dirY;

                        if (isColliding(enemiesX[i], STAR_CORE_X, enemiesY[i], STAR_CORE_Y, (float) SPRITE_SIZE_PX / 2, (float) SPRITE_SIZE_PX))
                        {
                            float tempDirX = STAR_CORE_X - enemiesX[i];
                            float tempDirY = STAR_CORE_Y - enemiesY[i];
                            enemiesX[i] -= (float) (tempDirX * 0.09);
                            enemiesY[i] -= (float) (tempDirY * 0.09);
                        }
                    }
                    else if (enemiesFramesToShoot[i] == 0)
                    {
                        for (int j = 0; j < BULLETS_POOL_CAPACITY; j++)
                        {
                            if (!enemyBulletsActive[j])
                            {
                                enemyBulletsActive[j] = true;
                                float tempRotation = (float) Math.atan2(dukesY - enemiesY[i], dukesX - enemiesX[i]);
                                enemyBulletsX[j] = enemiesX[i];
                                enemyBulletsY[j] = enemiesY[i];
                                enemyBulletsDirX[j] = (float) Math.cos(tempRotation);
                                enemyBulletsDirY[j] = (float) Math.sin(tempRotation);
                                enemiesFramesToShoot[i] = 50;
                                break;
                            }
                        }
                    }
                } else // enemy that impacts the core
                {
                    float dirX = STAR_CORE_X - enemiesX[i];
                    float dirY = STAR_CORE_Y - enemiesY[i];
                    float mag = (float) Math.sqrt((dirX * dirX) + (dirY * dirY));
                    dirX = mag > 0 ? dirX / mag : dirX;
                    dirY = mag > 0 ? dirY / mag : dirY;

                    enemiesX[i] += dirX;
                    enemiesY[i] += dirY;

                    enemiesRotation[i] = (float) Math.atan2(dirY, dirX);

                    if (isColliding(enemiesX[i], STAR_CORE_X, enemiesY[i], STAR_CORE_Y, (float) SPRITE_SIZE_PX / 2, SPRITE_SIZE_PX))
                    {
                        starCoreHealth -= enemiesDamage[i];

                        currentEnemiesKilled++;
                        currentActiveEnemies -= 1;
                        enemiesActive[i] = false;
                    }
                }

                // player can collide with rocks, but player is pushed back on collision
                if (isColliding(dukesX, enemiesX[i], dukesY, enemiesY[i], 12.5F, 10))
                {
                    float tempDirX = enemiesX[i] - dukesX;
                    float tempDirY = enemiesY[i] - dukesY;
                    dukesX -= (float) (tempDirX * 0.1);
                    dukesY -= (float) (tempDirY * 0.1);
                }

                // enemies can collide with rocks, but they're pushed back on collision
                for (int j = 0; j < ROCKS_POOL_CAPACITY; j++)
                {
                    if (!rocksActive[j]) continue;
                    if (isColliding(rocksX[j], enemiesX[i], rocksY[j], enemiesY[i], (float) SPRITE_SIZE_PX / 2, (float) SPRITE_SIZE_PX / 2))
                    {
                        float tempDirX = enemiesX[i] - rocksX[j];
                        float tempDirY = enemiesY[i] - rocksY[j];
                        enemiesX[i] += (float) (tempDirX * 0.1);
                        enemiesY[i] += (float) (tempDirY * 0.1);
                    }
                }

                // Collide enemies with other enemies, push them back
                for (int j = 0; j < ENEMIES_POOL_CAPACITY; j++)
                {
                    if (j == i) continue; // Cannot collide with itself !
                    if (enemiesActive[j] && isColliding(enemiesX[i], enemiesX[j], enemiesY[i], enemiesY[j], (float) SPRITE_SIZE_PX / 2, (float) SPRITE_SIZE_PX / 2))
                    {
                        float tempDirX = enemiesX[i] - enemiesX[j];
                        float tempDirY = enemiesY[i] - enemiesY[j];
                        enemiesX[i] += (float) (tempDirX * 0.1);
                        enemiesY[i] += (float) (tempDirY * 0.1);
                    }
                }

                enemiesFramesToShoot[i] = enemiesFramesToShoot[i] > 0 ? enemiesFramesToShoot[i] - 1 : 0;
            }

            // Update physics of enemy bullets and check collisions with duke
            for (int i = 0; i < BULLETS_POOL_CAPACITY; i++)
            {
                if (!enemyBulletsActive[i]) continue;
                enemyBulletsX[i] += enemyBulletsDirX[i] * 10;
                enemyBulletsY[i] += enemyBulletsDirY[i] * 10;

                // Deactivate bullets when they are off-screen
                if (enemyBulletsX[i] < 0 || enemyBulletsX[i] > VIEWPORT_WIDTH || enemyBulletsY[i] < 0 || enemyBulletsY[i] > VIEWPORT_HEIGHT)
                    enemyBulletsActive[i] = false;

                if (isColliding(enemyBulletsX[i], dukesX, enemyBulletsY[i], dukesY, 1, (float) (SPRITE_SIZE_PX / 2)))
                {
                    dukesHealth -= enemiesDamage[i];
                    enemyBulletsActive[i] = false;
                }
            }

            // Check input of selected slot
            if (gameInput.isPressed(KeyEvent.VK_1)) selectedSlot = 0;
            else if (gameInput.isPressed(KeyEvent.VK_2)) selectedSlot = 1;
            else if (gameInput.isPressed(KeyEvent.VK_3)) selectedSlot = 2;

            // Update frames to shoot
            dukesFramesToShoot = dukesFramesToShoot > 0 ? dukesFramesToShoot - 1 : 0;

            if (gameInput.isPressed(KeyEvent.VK_ESCAPE))
            {
                currentGameState = PAUSE_MENU_STATE;
                gameInput.releasePressed(KeyEvent.VK_ESCAPE);
            }

            if ( currentEnemiesKilled == ENEMIES_PER_WAVE[currentWave])
            {
                nextWaveTimer.start();
            }

            if (secondsToStartNextWave == 0)
            {
                nextWaveTimer.stop();
                secondsToStartNextWave = 10;
                currentEnemiesKilled = 0;
                currentWave++;
                currentGameState = UPGRADE_MENU_STATE;

                int randomIndex = (int) (Math.random() * 3) + 1;
                firstUpgradeSelectedIndex = randomIndex;
                secondUpgradeSelectedIndex = (randomIndex + 1) % 4;

                // Prevent accidental click
                gameInput.mouseLeftPressed = false;
            }
        }
        else if (currentGameState == START_MENU_STATE)
        {
            int startPlayButton = VIEWPORT_WIDTH / 2 - 120;
            boolean isOverPlayButton = gameInput.mouseX >= (startPlayButton) && gameInput.mouseX <= startPlayButton + 240
                    && gameInput.mouseY >= 200 && gameInput.mouseY <= 250;

            if (isOverPlayButton && gameInput.mouseLeftPressed)
            {
                currentGameState = GAME_PLAYING_STATE;
                gameInput.mouseLeftPressed = false;
            }
        }
        else if (currentGameState == PAUSE_MENU_STATE)
        {
            if (gameInput.isPressed(KeyEvent.VK_ESCAPE))
            {
                currentGameState = GAME_PLAYING_STATE;
                gameInput.releasePressed(KeyEvent.VK_ESCAPE);
            }
        }
        else if (currentGameState == UPGRADE_MENU_STATE)
        {
            boolean isOverFirstOption = gameInput.mouseX >= (VIEWPORT_WIDTH / 2 - 320) && gameInput.mouseX <= (VIEWPORT_WIDTH / 2 - 20)
                    && gameInput.mouseY >= 50 && gameInput.mouseY <= (VIEWPORT_HEIGHT - 100);
            boolean isOverSecondOption = gameInput.mouseX >= (VIEWPORT_WIDTH / 2 + 20) && gameInput.mouseX <= (VIEWPORT_WIDTH / 2 + 320)
                    && gameInput.mouseY >= 50 && gameInput.mouseY <= (VIEWPORT_HEIGHT - 100);

            int selectedIndex = -1;
            if (isOverFirstOption && gameInput.mouseLeftPressed)
            {
                selectedIndex = firstUpgradeSelectedIndex;
                currentGameState = GAME_PLAYING_STATE;
                gameInput.mouseLeftPressed = false;
            }

            if (isOverSecondOption && gameInput.mouseLeftPressed)
            {
                selectedIndex = secondUpgradeSelectedIndex;
                currentGameState = GAME_PLAYING_STATE;
                gameInput.mouseLeftPressed = false;
            }

            if (selectedIndex >= 0)
            {
                switch (selectedIndex)
                {
                    case HEALTH_UPGRADE -> dukesBaseHealth += 20;
                    case DAMAGE_UPGRADE -> dukesBulletDamage += 5;
                    case BIGGER_BULLETS_UPGRADE -> dukesBulletRadius += 1;
                    case MORE_BULLETS_UPGRADE -> dukesBaseFramesToShoot -= 6;
                    default -> System.out.println("Not valid");
                }
            }
        }
        else if (currentGameState == GAME_OVER_STATE)
        {
            int startMainMenuButton = (VIEWPORT_WIDTH / 2) - 120;
            boolean isOverButton = gameInput.mouseX >= startMainMenuButton && gameInput.mouseX <= startMainMenuButton + 240
                    && gameInput.mouseY >= 200 && gameInput.mouseY <= 250;

            if (isOverButton && gameInput.mouseLeftPressed)
            {
                System.out.println("HEYYY");
                currentGameState = START_MENU_STATE;
                currentWave = 0;
                currentEnemiesKilled = 0;
                currentActiveEnemies = 0;
                currentActiveRocks = 0;

                dukesX = STAR_CORE_X;
                dukesY = STAR_CORE_Y - 50;

                // Despawn all enemies and rocks
                for (int i = 0; i < ENEMIES_POOL_CAPACITY; i++) enemiesActive[i] = false;
                for (int i = 0; i < ROCKS_POOL_CAPACITY; i++) rocksActive[i] = false;

                dukesBaseHealth = 100;
                dukesHealth = dukesBaseHealth;
                dukesBulletDamage = dukesBaseBulletDamage;
                dukesBulletRadius = dukesBaseBulletRadius;
                dukesBaseFramesToShoot = 50;

                starCoreHealth = 200;

                gameInput.mouseLeftPressed = false;
            }
        }
    }

    private void render()
    {
        BufferStrategy bufferStrat = canvas.getBufferStrategy(); // Paint into backend buffer
        Graphics2D g2d = (Graphics2D) bufferStrat.getDrawGraphics();
        AffineTransform originalTransform = g2d.getTransform();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // Render dark space background
        g2d.setColor(BACKGROUND0_H);
        g2d.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Render stars in the background and update its position (no physics)
        for (int i = 0; i < 20; i++)
        {
            if (!bgStarsActive[i])
            {
                bgStarsX[i] = (float) (Math.random() * VIEWPORT_WIDTH);
                bgStarsY[i] = (float) (Math.random() * VIEWPORT_HEIGHT);
                bgStarsActive[i] = true;
            }

            if (bgStarsX[i] < 0 || bgStarsX[i] > VIEWPORT_WIDTH || bgStarsY[i] < 0 || bgStarsY[i] > VIEWPORT_HEIGHT)
            {
                bgStarsX[i] = VIEWPORT_WIDTH;
                bgStarsY[i] = (float) (Math.random() * VIEWPORT_HEIGHT);
            }

            g2d.setColor(FOREGROUND_0);
            bgStarsX[i] = (float) (bgStarsX[i] - 0.01);
            g2d.fillOval((int) (bgStarsX[i] - 2), (int) (bgStarsY[i] - 2), 2, 2);
        }

        if (currentGameState == START_MENU_STATE)
        {
            Font font = new Font(null, Font.PLAIN, 24);
            g2d.setFont(font);
            g2d.drawString("Space Ship Dukes", (VIEWPORT_WIDTH / 2) - 100 , 150);

            g2d.setColor(BACKGROUND_0);
            g2d.fillRect((VIEWPORT_WIDTH / 2) - 120, 200, 240, 50);

            g2d.setColor(FOREGROUND_1);
            g2d.drawString("Play", (VIEWPORT_WIDTH / 2) - 25, 235);
        }
        else if (currentGameState == GAME_OVER_STATE)
        {
            Font font = new Font(null, Font.PLAIN, 24);

            if (dukesHealth > 0 && starCoreHealth > 0) g2d.setColor(BLUE_A);
            else g2d.setColor(RED_A);

            g2d.fillRect(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);

            g2d.setFont(font);
            g2d.drawString("GAME OVER!", (VIEWPORT_WIDTH / 2) - 100 , 125);
            g2d.drawString(gameOverMsg, (VIEWPORT_WIDTH / 2) - 100 , 150);

            g2d.setColor(BACKGROUND_0);
            g2d.fillRect((VIEWPORT_WIDTH / 2) - 120, 200, 240, 50);

            g2d.setColor(FOREGROUND_1);
            g2d.drawString("Main menu", (VIEWPORT_WIDTH / 2) - 75, VIEWPORT_HEIGHT / 2 + 24);
        }
        else
        {
            g2d.setFont(null);

            // Render dukes
            g2d.rotate(dukesRotationAngle, dukesX, dukesY);
            g2d.drawImage(dukesSprite, null, (int) (dukesX - 16), (int) (dukesY - 16));
            g2d.setTransform(originalTransform);

            // Render star core
            g2d.setColor(FOREGROUND_0);
            g2d.drawOval(STAR_CORE_X - SPRITE_SIZE_PX, STAR_CORE_Y - SPRITE_SIZE_PX, SPRITE_SIZE_PX * 2, SPRITE_SIZE_PX * 2);
            g2d.drawImage(starCoreSprite, null, STAR_CORE_X - 16, STAR_CORE_Y - 16);

            // Render duke's bullets
            g2d.setColor(BLUE);
            for (int i = 0; i < BULLETS_POOL_CAPACITY; i++)
            {
                if (!bulletsActive[i]) continue;
                g2d.fillRect((int) bulletsX[i] - dukesBulletRadius, (int) bulletsY[i] - dukesBulletRadius, dukesBulletRadius * 2, dukesBulletRadius * 2);
            }

            // Render enemy bullets
            for (int i = 0; i < BULLETS_POOL_CAPACITY; i++)
            {
                if (!enemyBulletsActive[i]) continue;
                g2d.fillRect((int) enemyBulletsX[i] - 1, (int) enemyBulletsY[i] - 1, 2, 2);
            }

            // Render rocks
            for (int i = 0; i < ROCKS_POOL_CAPACITY; i++)
            {
                if (!rocksActive[i]) continue;
                g2d.drawImage(rockSprite, null, (int) rocksX[i] - 16, (int) rocksY[i] - 16);

                // Print health bar only when asteroid is being drilled
                if (rocksHealth[i] < 99)
                {
                    g2d.setColor(Color.BLUE);
                    g2d.fillRect((int) (rocksX[i] - 25), (int) (rocksY[i] - 25), (int) (rocksHealth[i] * 0.5F), 3);
                }
            }

            // Render enemies
            for (int i = 0; i < ENEMIES_POOL_CAPACITY; i++)
            {
                if (!enemiesActive[i]) continue;
                g2d.rotate(enemiesRotation[i], enemiesX[i], enemiesY[i]);
                g2d.drawImage(blueEnemySprite, null, (int) (enemiesX[i] - 16), (int) (enemiesY[i] - 16));
                g2d.setTransform(originalTransform);

                g2d.setColor(BLUE);
                g2d.fillRect((int) (enemiesX[i] - ((float) enemiesHealth[i] / 2)), (int) (enemiesY[i] - 25), enemiesHealth[i], 3);
            }

            int offset = 30;
            for (int i = 0; i < 2; i++)
            {
                if (i == selectedSlot) g2d.setColor(Color.WHITE);
                else g2d.setColor(Color.GRAY);
                g2d.drawRect((VIEWPORT_WIDTH / 2) - (offset) + (offset * i), VIEWPORT_HEIGHT - offset - 10, offset, offset);
            }

            // Render duke health
            g2d.setColor(BLUE);
            g2d.fillRect((int) (dukesX - 25), (int) (dukesY - 25), (int) (dukesHealth * 0.5F), 3);

            // Render star core health
            g2d.fillRect(STAR_CORE_X - 50, STAR_CORE_Y - 50, (int) (starCoreHealth * 0.5), 3);

            if (currentGameState == UPGRADE_MENU_STATE)
            {
                g2d.setColor(BACKGROUND0_A);
                g2d.fillRect(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);

                // Draw upgrade cards
                g2d.setColor(RED);
                g2d.drawRect((VIEWPORT_WIDTH / 2) - 320, (VIEWPORT_WIDTH / 2) - 100, 300, 200);
                g2d.drawRect((VIEWPORT_WIDTH / 2) + 20, (VIEWPORT_WIDTH / 2) - 100, 300, 200);

                // Draw texts
                g2d.setColor(FOREGROUND_1);
                g2d.drawString(UPGRADE_NAMES[firstUpgradeSelectedIndex], (VIEWPORT_WIDTH / 2) - 280, (VIEWPORT_WIDTH / 2) - 50);
                g2d.drawString(UPGRADE_NAMES[secondUpgradeSelectedIndex], (VIEWPORT_WIDTH / 2) + 60, (VIEWPORT_WIDTH / 2) - 50);

                g2d.drawString(UPGRADE_DESCRIPTIONS[firstUpgradeSelectedIndex], (VIEWPORT_WIDTH / 2) - 280, (VIEWPORT_WIDTH / 2));
                g2d.drawString(UPGRADE_DESCRIPTIONS[secondUpgradeSelectedIndex], (VIEWPORT_WIDTH / 2) + 60, (VIEWPORT_WIDTH / 2));
            }

            if (currentGameState == PAUSE_MENU_STATE)
            {
                Font font = new Font(null, Font.PLAIN, 24);
                g2d.setFont(font);
                g2d.setColor(BACKGROUND0_A);
                g2d.fillRect(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);

                g2d.setColor(FOREGROUND_1);
                g2d.drawString("Space Ship Dukes", (VIEWPORT_WIDTH / 2) - 100 , 150);
                g2d.drawString("Pause Menu", (VIEWPORT_WIDTH / 2) - 100 , 200);
                g2d.drawString("Press ESC to continue", (VIEWPORT_WIDTH / 2) - 100 , 225);
            }

            g2d.setColor(FOREGROUND_1);
            g2d.drawString("Wave " + currentWave, VIEWPORT_WIDTH / 2 - 15, 15);
            g2d.drawString("CEK: " + currentEnemiesKilled, VIEWPORT_WIDTH / 2 - 15, 30);
            g2d.drawString("EPW: " + ENEMIES_PER_WAVE[currentWave], VIEWPORT_WIDTH / 2 - 15, 45);
            g2d.drawString("CAE: " + currentActiveEnemies, VIEWPORT_WIDTH / 2 - 15, 60);

            if (nextWaveTimer.isRunning())
            {
                g2d.drawString(String.valueOf(secondsToStartNextWave), STAR_CORE_X - 4, STAR_CORE_Y - 60);
            }

            g2d.setFont(null);
            String sb = "Health: " + dukesHealth + " / " + dukesBaseHealth +
                    " Wave: " + currentWave +
                    " Enemies: " + currentEnemiesKilled + " / " + ENEMIES_PER_WAVE[currentWave];
            g2d.drawString(sb, 10, VIEWPORT_HEIGHT - 20);
        }

        g2d.dispose();
        bufferStrat.show();
    }

    private float sqrDistance(float x1, float x2, float y1, float y2)
    {
        float componentX = (x2 - x1) * (x2 - x1);
        float componentY = (y2 - y1) * (y2 - y1);

        return componentX + componentY;
    }

    private boolean isColliding(float x1, float x2, float y1, float y2, float r1, float r2)
    {
        float sumOfRadius = (r1 + r2) * (r1 + r2);
        float componentX = (x2 - x1) * (x2 - x1);
        float componentY = (y2 - y1) * (y2 - y1);

        return sumOfRadius > componentX + componentY;
    }

    @Override
    public void run()
    {
        canvas.createBufferStrategy(2);

        long prevTime = System.nanoTime();
        final double ticksPerSecond = 60f;
        final double nanosecondsPerTick = 1000000000 / ticksPerSecond;
        double delta = 0;

        while (isRunning)
        {
            long currentTime = System.nanoTime();
            delta += (currentTime - prevTime) / nanosecondsPerTick;
            prevTime = currentTime;

            while (delta >= 1)
            {
                physicsUpdate();
                delta--;
            }

            render();
        }
    }
}
