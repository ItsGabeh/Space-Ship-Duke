package com.itsgabeh;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferStrategy;
import java.io.File;
import java.io.IOException;

class Game implements Runnable
{
    private final int DUKES_WIDTH = 25, DUKES_HEIGHT = 25;
    private final int VIEWPORT_WIDTH = 768, VIEWPORT_HEIGHT = 768;
    private final int BULLETS_POOL_CAPACITY = 50;
    private final int ASTEROIDS_POOL_CAPACITY = 10;
    private final int CORE_X = VIEWPORT_WIDTH / 2, CORE_Y = VIEWPORT_HEIGHT / 2;
    private final int CORE_WIDTH = 50, CORE_HEIGHT = 50;
    private final int INITIAL_FRAMES_TO_SHOOT = 50;

    private float dukesX = 0, dukesY = 0;
    private float rotationAngle = 0;
    private int dukesHealth = 100;
    private final Canvas canvas;
    private final GameInput gameInput;
    private boolean isRunning = false;
    private int selectedSlot = 0;
    private int framesToShoot = 0;

    // TODO: check optimizations of Data-Oriented Design
    private final Bullet[] bullets = new Bullet[BULLETS_POOL_CAPACITY];
    private final Bullet[] enemyBullets = new Bullet[BULLETS_POOL_CAPACITY * 2];
    private final Asteroid[] asteroids = new Asteroid[ASTEROIDS_POOL_CAPACITY];
    private final Enemy[] enemies = new Enemy[5];
    private final BackgroundStar[] backgroundStars = new BackgroundStar[20];

    private Clip dukesAudioClip;

    public Game()
    {
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

        for (int i = 0; i < 20; i++)
        {
            backgroundStars[i] = new BackgroundStar();
        }

        for (int i = 0; i < BULLETS_POOL_CAPACITY; i++)
        {
            bullets[i] = new Bullet();
        }

        for (int i = 0; i < BULLETS_POOL_CAPACITY * 2; i++)
        {
            enemyBullets[i] = new Bullet();
        }

        for (int i = 0; i < ASTEROIDS_POOL_CAPACITY; i++)
        {
            double randomAngle = Math.random() * (Math.PI * 2);
            asteroids[i] = new Asteroid();
            asteroids[i].x = (float) (CORE_X + 600 * Math.cos(randomAngle));
            asteroids[i].y = (float) (CORE_Y + 400 * Math.sin(randomAngle));
            asteroids[i].isActive = true;
        }

        for (int i = 0; i < 5; i++)
        {
            double randomAngle = Math.random() * (Math.PI * 2);
            enemies[i] = new Enemy();
            enemies[i].x = (float) (CORE_X + 800 * Math.cos(randomAngle));
            enemies[i].y = (float) (CORE_Y + 600 * Math.sin(randomAngle));
            enemies[i].isActive = true;
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
        dukesX += directionX * 1.2F;
        dukesY += directionY * 1.2F;

        // Get the direction vector pointing to mouse direction
        // Mouse Pos - Dukes Pos = Direction Vector (starting from 0,0)
        // Applying atan2 calculates the angle 0 within X line and the point (x,y)
        float mouseDistanceX = gameInput.mouseX - dukesX;
        float mouseDistanceY = gameInput.mouseY - dukesY;
        rotationAngle = (float) Math.atan2(mouseDistanceY, mouseDistanceX);

        boolean isShooting = gameInput.mouseLeftPressed;
        if (isShooting && selectedSlot == 0 && framesToShoot == 0)
        {
            if (dukesAudioClip != null)
            {
                dukesAudioClip.setFramePosition(0);
                dukesAudioClip.start();
            }

            // TODO: check optimizations of Data-Oriented Design
            Bullet bullet = null;
            for (int i = 0; i < BULLETS_POOL_CAPACITY; i++)
            {
                if (!bullets[i].isActive) bullet = bullets[i];
            }

            if (bullet != null)
            {
                bullet.isActive = true;
                // Spawn bullets at dukes pos
                bullet.x = dukesX;
                bullet.y = dukesY;
                // Cos and Sin of an angle 0 gives the x and y components of the vector A with angle 0
                bullet.directionX = (float) Math.cos(rotationAngle);
                bullet.directionY = (float) Math.sin(rotationAngle);
            }

            framesToShoot = INITIAL_FRAMES_TO_SHOOT;
        }


        // TODO: check optimizations of Data-Oriented Design
        for (Bullet bullet : bullets)
        {
            if (!bullet.isActive) continue;
            if (bullet.x < 0 || bullet.x > VIEWPORT_WIDTH || bullet.y < 0 || bullet.y > VIEWPORT_HEIGHT)
                bullet.isActive = false;
            else
            {
                bullet.x += bullet.directionX * 25;
                bullet.y += bullet.directionY * 25;
            }

            // Bullets can collide with asteroids
            // Check if bullet distance from the asteroid center
            for (Asteroid asteroid : asteroids)
            {
                if (asteroid.isActive)
                {
                    if (isColliding(asteroid.x, bullet.x, asteroid.y, bullet.y, 25, 1))
                    {
                        bullet.isActive = false;
                    }
                }
            }

            // bullets can collide with the core
            if ((Math.sqrt(Math.pow(bullet.x - CORE_X, 2) + Math.pow(bullet.y - CORE_Y, 2))) <= ((double) CORE_WIDTH / 2))
            {
                bullet.isActive = false;
            }

            // bullets collide with enemies and deactivate them
            for (Enemy enemy : enemies)
            {
                if (!enemy.isActive) continue;
                if (isColliding(enemy.x, bullet.x, enemy.y, bullet.y, 10, 1))
                {
                    bullet.isActive = false;
                    if (enemy.health <= 0) enemy.isActive = false;
                    else enemy.health -= 20;
                }
            }
        }

        // Move each active asteroid towards the center but not at all
        for (Asteroid asteroid : asteroids)
        {
            if (!asteroid.isActive) continue;
            float dirX = ((float) CORE_X) - asteroid.x;
            float dirY = ((float) CORE_Y) - asteroid.y;
            float mag = (float) Math.sqrt((dirX * dirX) + (dirY * dirY));
            dirX = mag > 0 ? dirX / mag : dirX;
            dirY = mag > 0 ? dirY / mag : dirY;


            if (Math.sqrt(Math.pow(asteroid.x - CORE_X, 2) + Math.pow(asteroid.y - CORE_Y, 2)) > 300)
            {
                asteroid.x += dirX;
                asteroid.y += (float) (dirY * 0.5);
            }


            float distanceFromMouse = (float) Math.sqrt(Math.pow(gameInput.mouseX - asteroid.x, 2) + Math.pow(gameInput.mouseY - asteroid.y, 2));
            float distanceFromDukes = (float) Math.sqrt(Math.pow(dukesX - asteroid.x, 2) + Math.pow(dukesY - asteroid.y, 2));
            if (selectedSlot == 1 && distanceFromMouse < 25 && distanceFromDukes < 50 && gameInput.mouseLeftPressed)
            {
                if (asteroid.health > 0)
                {
                    asteroid.health -= 1;
                }
                else
                {
                    asteroid.isActive = false;
                }
            }

            // Player can collide with asteroids, but player is pushed back on collision
            if (isColliding(dukesX, asteroid.x, dukesY, asteroid.y, 12.5F, 25))
            {
                float tempDirX = asteroid.x - dukesX;
                float tempDirY = asteroid.y - dukesY;
                dukesX -= (float) (tempDirX * 0.1);
                dukesY -= (float) (tempDirY * 0.1);
            }
        }

        // Make enemies follow the player
        for (Enemy enemy : enemies)
        {
            if (!enemy.isActive) continue;
            float dirX = dukesX - enemy.x;
            float dirY = dukesY - enemy.y;
            float mag = (float) Math.sqrt((dirX * dirX) + (dirY * dirY));
            dirX = mag > 0 ? dirX / mag : dirX;
            dirY = mag > 0 ? dirY / mag : dirY;

            if (Math.sqrt(Math.pow(enemy.x - dukesX, 2) + Math.pow(enemy.y - dukesY, 2)) > 100)
            {
                enemy.x += dirX;
                enemy.y += dirY;
            }
            else if (enemy.framesToShoot == 0)
            {
                for (Bullet enemyBullet : enemyBullets)
                {
                    if (!enemyBullet.isActive)
                    {
                        enemyBullet.isActive = true;
                        float tempRotation = (float) Math.atan2(dukesY - enemy.y, dukesX - enemy.x);
                        enemyBullet.x = enemy.x;
                        enemyBullet.y = enemy.y;
                        enemyBullet.directionY = (float) Math.sin(tempRotation);
                        enemyBullet.directionX = (float) Math.cos(tempRotation);
                        enemy.framesToShoot = 50;
                        break;
                    }
                }
            }

            // Player can collide with asteroids, but player is pushed back on collision
            if (isColliding(dukesX, enemy.x, dukesY, enemy.y, 12.5F, 10))
            {
                float tempDirX = enemy.x - dukesX;
                float tempDirY = enemy.y - dukesY;
                dukesX -= (float) (tempDirX * 0.1);
                dukesY -= (float) (tempDirY * 0.1);
            }

            for (Asteroid asteroid : asteroids)
            {
                if (!asteroid.isActive) continue;
                // Player can collide with asteroids, but player is pushed back on collision
                if (isColliding(asteroid.x, enemy.x, asteroid.y, enemy.y, 12.5F, 10))
                {
                    float tempDirX = enemy.x - asteroid.x;
                    float tempDirY = enemy.y - asteroid.y;
                    enemy.x += (float) (tempDirX * 0.1);
                    enemy.y += (float) (tempDirY * 0.1);
                }
            }

            for (Enemy nextEnemy : enemies)
            {
                if (nextEnemy.isActive && !nextEnemy.equals(enemy) && isColliding(enemy.x, nextEnemy.x, enemy.y, nextEnemy.y, 10, 10))
                {
                    float tempDirX = enemy.x - nextEnemy.x;
                    float tempDirY = enemy.y - nextEnemy.y;
                    enemy.x += (float) (tempDirX * 0.1);
                    enemy.y += (float) (tempDirY * 0.1);
                }
            }

            enemy.framesToShoot = enemy.framesToShoot > 0 ? enemy.framesToShoot - 1 : 0;
        }

        for (Bullet enemyBullet : enemyBullets)
        {
            if (!enemyBullet.isActive) continue;
            enemyBullet.x += enemyBullet.directionX * 10;
            enemyBullet.y += enemyBullet.directionY * 10;

            if (enemyBullet.x < 0 || enemyBullet.x > VIEWPORT_WIDTH || enemyBullet.y < 0 || enemyBullet.y > VIEWPORT_HEIGHT)
                enemyBullet.isActive = false;

            if (isColliding(enemyBullet.x, dukesX, enemyBullet.y, dukesY, 1, 12.5F))
            {
                dukesHealth -= 1;
            }
        }

        // Check input of selected slot
        if (gameInput.isPressed(KeyEvent.VK_1)) selectedSlot = 0;
        else if (gameInput.isPressed(KeyEvent.VK_2)) selectedSlot = 1;
        else if (gameInput.isPressed(KeyEvent.VK_3)) selectedSlot = 2;

        // Update frames to shoot
        framesToShoot = framesToShoot > 0 ? framesToShoot - 1 : 0;
    }

    private void render()
    {
        BufferStrategy bufferStrat = canvas.getBufferStrategy(); // Paint into backend buffer
        Graphics2D g2d = (Graphics2D) bufferStrat.getDrawGraphics();
        AffineTransform originalTransform = g2d.getTransform();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        g2d.setColor(new Color(28, 28, 28));
        g2d.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        for (BackgroundStar star : backgroundStars)
        {
            if (!star.isActive)
            {
                star.x = (float) (Math.random() * VIEWPORT_WIDTH);
                star.y = (float) (Math.random() * VIEWPORT_HEIGHT);
                star.isActive = true;
            }

            if (star.x < 0 || star.x > VIEWPORT_WIDTH || star.y < 0 || star.y > VIEWPORT_HEIGHT)
            {
                star.x = VIEWPORT_WIDTH;
                star.y = (float) (Math.random() * VIEWPORT_HEIGHT);
            }

            g2d.setColor(new Color(0xEBDBB2)); // 168 153 132
            star.x = (float) (star.x - 0.01);
            g2d.fillOval((int) (star.x - 2), (int) (star.y - 2), 2, 2);
        }

        g2d.setColor(new Color(0xEBDBB2));
        g2d.rotate(rotationAngle, dukesX, dukesY);
        g2d.fillOval((int) dukesX - (DUKES_WIDTH / 2), (int) dukesY - (DUKES_HEIGHT / 2), DUKES_WIDTH, DUKES_HEIGHT);
        g2d.setTransform(originalTransform);

        g2d.fillRoundRect(CORE_X - (CORE_WIDTH / 2), CORE_Y - (CORE_HEIGHT / 2), CORE_WIDTH, CORE_HEIGHT, 1, 1);

        // TODO: check optimizations of Data-Oriented Design
        g2d.setColor(new Color(8627608));
        for (Bullet bullet : bullets)
        {
            if (!bullet.isActive) continue;
            g2d.fillRect((int) bullet.x - 1, (int) bullet.y - 1, 2, 2);
        }

        for (Bullet enemyBullet : enemyBullets)
        {
            if (!enemyBullet.isActive) continue;
            g2d.fillRect((int) enemyBullet.x - 1, (int) enemyBullet.y - 1, 2, 2);
        }

        for (Asteroid asteroid : asteroids)
        {
            if (!asteroid.isActive) continue;
            g2d.setColor(new Color(6708308));
            g2d.fillOval((int) asteroid.x - 25, (int) asteroid.y - 25, 50, 50);

            // Print health bar only when asteroid is being drilled
            if (asteroid.health > 0)
            {
                g2d.setColor(Color.BLUE);
                g2d.fillRect((int) (asteroid.x - 25), (int) (asteroid.y - 25), (int) (asteroid.health * 0.5F), 3);
            }
        }

        for (Enemy enemy : enemies)
        {
            if (!enemy.isActive) continue;
            g2d.setColor(Color.RED);
            g2d.fillRect((int) (enemy.x - 10), (int) (enemy.y - 10), 20, 20);
            g2d.setColor(Color.BLUE);
            g2d.fillRect((int) (enemy.x - 25), (int) (enemy.y - 25), (int) (enemy.health * 0.5), 3);
        }

        // Inventory and Weapon System
        // 1. Drill for mining
        // 2. Basic weapon
        // 3. Laser? or ultimate
        int offset = 30;
        for (int i = 0; i < 3; i++)
        {
            if (i == selectedSlot) g2d.setColor(Color.WHITE);
            else g2d.setColor(Color.GRAY);
            g2d.drawRect((VIEWPORT_WIDTH / 2) - ( offset * 3 / 2) + (offset * i), VIEWPORT_HEIGHT - offset - 10, offset, offset);
        }

        g2d.setColor(Color.BLUE);
        g2d.fillRect((int) (dukesX - 25), (int) (dukesY - 25), (int) (dukesHealth * 0.5F), 3);

        // Debug String xd
        String sb = "Mouse: " +
                gameInput.mouseX +
                " " +
                gameInput.mouseY +
                " Dukes: " +
                dukesX +
                " " +
                dukesY +
                " Core: " +
                CORE_X + " " + CORE_Y +
                " MLP: " + gameInput.mouseLeftPressed;
        g2d.drawString(sb, 0, 10);

        g2d.dispose();
        bufferStrat.show();
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
