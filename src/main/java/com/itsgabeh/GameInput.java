package com.itsgabeh;

import java.awt.event.*;

class GameInput implements KeyListener, MouseListener, MouseMotionListener
{
    private final boolean[] keys = new boolean[256];
    public int mouseX, mouseY;
    public boolean mouseLeftPressed = false;

    public boolean isPressed(int keyCode)
    {
        if (keyCode < 0 || keyCode >= keys.length) return false;
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
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        if (e.getButton() == MouseEvent.BUTTON1) mouseLeftPressed = true;
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        if (e.getButton() == MouseEvent.BUTTON1) mouseLeftPressed = false;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}
