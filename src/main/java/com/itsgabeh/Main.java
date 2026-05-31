package com.itsgabeh;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferStrategy;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class Game implements Runnable
{
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

    public Game()
    {
        for (int i = 0; i < BULLETS_POOL_CAPACITY; i++)
        {
            bullets[i] = new Bullet();
        }

        Random r = new Random();

        for (int i = 0; i < ASTEROIDS_POOL_CAPACITY; i++)
        {
            asteroids[i] = new Asteroid();
            asteroids[i].asteroidX = Math.abs(r.nextInt() % VIEWPORT_WIDTH);
            asteroids[i].asteroidY = Math.abs(r.nextInt() % VIEWPORT_HEIGHT);
            IO.println(asteroids[i].asteroidX + " " + asteroids[i].asteroidY);
            asteroids[i].isActive = true;
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
                b.bulletX = dukesX + (double) DUKES_WIDTH / 2;
                b.bulletY = dukesY + (double) DUKES_HEIGHT / 2;
                // Cos and Sin of an angle 0 gives the x and y components of the vector A with angle 0
                b.directionX = Math.cos(rotationAngle);
                b.directionY = Math.sin(rotationAngle);
            }
        }

        // TODO: check optimizations of Data-Oriented Design
        List<Bullet> activeBullets = Arrays.stream(bullets).filter(b -> b.isActive).toList();
        for (Bullet b : activeBullets)
        {
//            // Check collisions on asteroids
//            // TODO: optimize along Data-Oriented Design
//            for (Asteroid as : asteroids)
//            {
//                if (as.isActive)
//                {
//                    b.isActive = !(Math.sqrt(Math.pow(b.bulletX - as.asteroidX, 2) + Math.pow(b.bulletY - as.asteroidY, 2)) <= 20);
//                }
//            }

            if (b.bulletX < 0 || b.bulletX > VIEWPORT_WIDTH || b.bulletY < 0 || b.bulletY > VIEWPORT_HEIGHT) b.isActive = false;
            else {
                b.bulletX += b.directionX * 25;
                b.bulletY += b.directionY * 25;
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
        g2d.rotate(rotationAngle, dukesX + ((double) DUKES_WIDTH / 2), dukesY + ((double) DUKES_HEIGHT / 2));
        g2d.fillOval((int) dukesX, (int) dukesY, DUKES_WIDTH, DUKES_HEIGHT);
        g2d.setTransform(originalTransform);

        g2d.fillRoundRect(CORE_X - (CORE_WIDTH / 2), CORE_Y - (CORE_HEIGHT / 2), CORE_WIDTH, CORE_HEIGHT, 1, 1);

        // TODO: check optimizations of Data-Oriented Design
        g2d.setColor(new Color(8627608));
        List<Bullet> activeBullets = Arrays.stream(bullets).filter(b -> b.isActive).toList();
        for (Bullet b : activeBullets)
        {
            g2d.fillRect((int) b.bulletX, (int) b.bulletY, 2, 2);
        }

        g2d.setColor(new Color(6708308));
        List<Asteroid> activeAsteroids = Arrays.stream(asteroids).filter(a -> a.isActive).toList();
        for (Asteroid a : activeAsteroids)
        {
            g2d.fillOval((int) a.asteroidX, (int) a.asteroidY, 50, 50);
        }

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
