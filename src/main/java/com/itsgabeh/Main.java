package com.itsgabeh;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferStrategy;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class Game implements Runnable
{
    static class Enemy
    {
        double x = 0, y = 0;
        double life = 100;
    }

    static class Asteroid
    {
        double asteroidX = 0, asteroidY = 0;
        boolean isActive = false;
    }

    static class Bullet
    {
        double bulletX = 0, bulletY = 0;
        double directionX = 0, directionY = 0;
        boolean isActive = false;
    }

    // Game constants
    private final int DUKES_WIDTH = 25, DUKES_HEIGHT = 25;
    private final int VIEWPORT_WIDTH = 720, VIEWPORT_HEIGHT = 480;
    private final int BULLETS_POOL_CAPACITY = 50;
    private final int ASTEROIDS_POOL_CAPACITY = 10;
    private final int CORE_X = VIEWPORT_WIDTH / 2, CORE_Y = VIEWPORT_HEIGHT / 2;
    private final int CORE_WIDTH = 50, CORE_HEIGHT = 50;

    // Dukes variables
    private float dukesX = 0, dukesY = 0;
    private float rotationAngle = 0;
    private final Canvas canvas;
    private final GameInput gameInput;
    private final BlockingQueue<String> queue;
    private boolean isRunning = false;
    // TODO: check optimizations of Data-Oriented Design
    private final Bullet[] bullets = new Bullet[BULLETS_POOL_CAPACITY];
    private final Asteroid[] asteroids = new Asteroid[ASTEROIDS_POOL_CAPACITY];
    private final Enemy[] enemies = new Enemy[5];

    public Game()
    {
        for (int i = 0; i < BULLETS_POOL_CAPACITY; i++)
        {
            bullets[i] = new Bullet();
        }

        for (int i = 0; i < ASTEROIDS_POOL_CAPACITY; i++)
        {
            double randomAngle = Math.random() * (Math.PI * 2);
            asteroids[i] = new Asteroid();
            asteroids[i].asteroidX = CORE_X + 400 * Math.cos(randomAngle);
            asteroids[i].asteroidY = CORE_Y + 400 * Math.sin(randomAngle);
            asteroids[i].isActive = true;
        }

        for (int i = 0; i < 5; i++)
        {
            double randomAngle = Math.random() * (Math.PI * 2);
            enemies[i] = new Enemy();
            enemies[i].x = CORE_X + 400 * Math.cos(randomAngle);
            enemies[i].y = CORE_Y + 400 * Math.sin(randomAngle);
        }

        queue = new LinkedBlockingQueue<>(200);

        JFrame mainFrame = new JFrame("Game test");
        gameInput = new GameInput();
        canvas = new Canvas();
        canvas.addKeyListener(gameInput);
        canvas.addMouseMotionListener(gameInput);

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

        Thread debugThread = new Thread(() -> {
            while (true) {
                try {
                    String item = queue.take();
                    IO.println(item);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "DebugLoop");
        debugThread.start();
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
        dukesX += directionX * 3;
        dukesY += directionY * 3;

        // Get the direction vector pointing to mouse direction
        // Mouse Pos - Dukes Pos = Direction Vector (starting from 0,0)
        // Applying atan2 calculates the angle 0 within X line and the point (x,y)
        float mouseDistanceX = gameInput.mouseX - (dukesX + (float) DUKES_WIDTH / 2);
        float mouseDistanceY = gameInput.mouseY - (dukesY + (float) DUKES_HEIGHT / 2);
        rotationAngle = (float) Math.atan2(mouseDistanceY, mouseDistanceX);

        boolean isShooting = gameInput.isPressed(KeyEvent.VK_SPACE);
        if (isShooting)
        {
            // TODO: check optimizations of Data-Oriented Design
            Optional<Bullet> bullet = Arrays.stream(bullets).filter(b -> !b.isActive).findFirst();
            if (bullet.isPresent())
            {
                Bullet b = bullet.get();
                b.isActive = true;
                // Spawn bullets at dukes pos
                b.bulletX = dukesX;
                b.bulletY = dukesY;
                // Cos and Sin of an angle 0 gives the x and y components of the vector A with angle 0
                b.directionX = Math.cos(rotationAngle);
                b.directionY = Math.sin(rotationAngle);
            }
        }

        // TODO: check optimizations of Data-Oriented Design
        for (Bullet bullet : bullets)
        {
            if (!bullet.isActive) continue;
            if (bullet.bulletX < 0 || bullet.bulletX > VIEWPORT_WIDTH || bullet.bulletY < 0 || bullet.bulletY > VIEWPORT_HEIGHT) bullet.isActive = false;
            else {
                bullet.bulletX += bullet.directionX * 25;
                bullet.bulletY += bullet.directionY * 25;
            }

            // Bullets can collide with asteroids
            // Check if bullet distance from the asteroid center
            for (Asteroid as : asteroids)
            {
                if (as.isActive)
                {
                    if (Math.sqrt(Math.pow(bullet.bulletX - as.asteroidX, 2) + Math.pow(bullet.bulletY - as.asteroidY, 2)) <= 25)
                    {
                        bullet.isActive = false;
                    }
                }
            }

            // bullets can collide with the core
            if ((Math.sqrt(Math.pow(bullet.bulletX - CORE_X, 2) + Math.pow(bullet.bulletY - CORE_Y, 2))) <= ((double) CORE_WIDTH / 2))
            {
                bullet.isActive = false;
            }
        }

        // Move each active asteroid towards the center but not at all
        for (Asteroid asteroid : asteroids)
        {
            if (!asteroid.isActive) continue;
            float dirX = (float) (((float) CORE_X) - asteroid.asteroidX);
            float dirY = (float) (((float) CORE_Y) - asteroid.asteroidY);
            float mag = (float) Math.sqrt((dirX * dirX) + (dirY * dirY));
            dirX = mag > 0 ? dirX / mag : dirX;
            dirY = mag > 0 ? dirY / mag : dirY;


            if (Math.sqrt(Math.pow(asteroid.asteroidX - CORE_X, 2) + Math.pow(asteroid.asteroidY - CORE_Y, 2)) > 200)
            {
                asteroid.asteroidX += dirX;
                asteroid.asteroidY += dirY;
            }
        }


        // Debug Log
//        try {
//            queue.put("X: " + dukesX + " Y:" + dukesY + " Mouse " + gameInput.mouseX + " " + gameInput.mouseY);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
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

        g2d.setColor(new Color(15457202));
        g2d.rotate(rotationAngle, dukesX, dukesY);
        g2d.fillOval((int) dukesX - (DUKES_WIDTH / 2), (int) dukesY - (DUKES_HEIGHT / 2), DUKES_WIDTH, DUKES_HEIGHT);
        g2d.setTransform(originalTransform);

        g2d.fillRoundRect(CORE_X - (CORE_WIDTH / 2), CORE_Y - (CORE_HEIGHT / 2), CORE_WIDTH, CORE_HEIGHT, 1, 1);

        // TODO: check optimizations of Data-Oriented Design
        g2d.setColor(new Color(8627608));
        for (Bullet bullet : bullets)
        {
            if (!bullet.isActive) continue;
            g2d.fillRect((int) bullet.bulletX, (int) bullet.bulletY, 2, 2);
        }

        g2d.setColor(new Color(6708308));
        List<Asteroid> activeAsteroids = Arrays.stream(asteroids).filter(a -> a.isActive).toList();
        for (Asteroid a : activeAsteroids)
        {
            g2d.fillOval((int) a.asteroidX - 25, (int) a.asteroidY - 25, 50, 50);
        }

        g2d.setColor(Color.RED);
        for (Enemy enemy : enemies)
        {
            g2d.fillRect((int) (enemy.x - 10), (int) (enemy.y - 10), 20, 20);
        }

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
                CORE_X + " " + CORE_Y;
        g2d.drawString(sb, 0, 10);

        g2d.dispose();
        bufferStrat.show();
    }

    @Override
    public void run()
    {
        canvas.createBufferStrategy(2);

        long prevTime = System.nanoTime();
        final double ticksPerSecond = 60f;
        final double nanosecondsPerTick = 1000000000 / ticksPerSecond;
        double delta = 0;

        while(isRunning)
        {
            long currentTime = System.nanoTime();
            delta += (currentTime - prevTime) / nanosecondsPerTick;
            prevTime = currentTime;

            while (delta >= 1) {
                physicsUpdate();
                delta--;
            }

            render();
        }
    }
}

class GameInput implements KeyListener, MouseListener, MouseMotionListener
{
    private final boolean[] keys = new boolean[256];
    public int mouseX, mouseY;

    public boolean isPressed(int keyCode)
    {
        if (keyCode < 0 || keyCode >= keys.length) return  false;
        return keys[keyCode];
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        if (e.getKeyCode() < keys.length) keys[e.getKeyCode()] = true;
    }

    @Override
    public void keyReleased(KeyEvent e)
    {
        if (e.getKeyCode() < keys.length) keys[e.getKeyCode()] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void mouseDragged(MouseEvent e)
    {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}
}

public class Main
{
    static void main() {
        new Game().start();
    }
}
