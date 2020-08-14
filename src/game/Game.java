package game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class Game extends JFrame implements KeyListener, ActionListener, MouseListener {
    private static final int width;
    private static final int height;
    private static final String title;

    private enum Modes {
        welcome, game, over
    }

    private static Modes current_mode;

    static {
        width = 900;
        height = 715;
        title = "Space Invaders";
        current_mode = Modes.welcome;
    }

    abstract static class Plane {
        protected int plane_w;
        protected int plane_h;
        protected int plane_x;
        protected int plane_y;
        protected int bullet_w;
        protected int bullet_h;
        protected int life;
        protected ArrayList<ArrayList<Integer>> bullets = new ArrayList<>();
    }


    static class Player extends Plane {
        public Player() {
            this.plane_w = 50;
            this.plane_h = 30;
            this.plane_x = width / 2 - this.plane_w / 2;
            this.plane_y = 600;
            this.bullet_w = 10;
            this.bullet_h = 20;
            this.life = 10;
            this.bullets.add(new ArrayList<>(Arrays.asList(this.plane_x + this.plane_w / 2 - this.bullet_w / 2, this.plane_y)));
        }

        public void reset() {
            this.plane_x = width / 2 - this.plane_w / 2;
            this.plane_y = 600;
            this.life = 10;
            this.bullets.clear();
            this.bullets.add(new ArrayList<>(Arrays.asList(this.plane_x + this.plane_w / 2 - this.bullet_w / 2, this.plane_y)));
        }

        public void drawPlane(Graphics g) {
            g.setColor(Color.BLACK);
            g.fillRect(this.plane_x, this.plane_y, this.plane_w,this.plane_h);
        }

        public void drawBullets(Graphics g) {
            g.setColor(Color.YELLOW);
            this.bullets.forEach(bullet -> g.fillRect(bullet.get(0), bullet.get(1), this.bullet_w, this.bullet_h));
        }

        public void moveBullets() {
            this.bullets.forEach(bullet -> bullet.set(1, bullet.get(1) - 2));
        }

        public void addNewBullets() {
            ArrayList<Integer> bullet = this.bullets.get(this.bullets.size() - 1);
            if(this.plane_y - bullet.get(1) > 40)
                this.bullets.add(new ArrayList<>(Arrays.asList(this.plane_x + this.plane_w / 2 - this.bullet_w / 2, this.plane_y)));

        }

        public void deleteExtraBullets() {
            this.bullets.removeIf(bullet -> bullet.get(1) < -this.bullet_w);
        }

        public void drawHealthBar(Graphics g)
        {
            g.setColor(Color.GREEN);
            g.fillRect(this.plane_x, this.plane_y + 40, this.plane_w, 10);

            g.setColor(Color.RED);
            g.fillRect(this.plane_x, this.plane_y + 40, (10 - this.life) * 5, 10);
        }
    }

    static class Enemy extends Plane {
        public Enemy() {
            this.plane_w = 25;
            this.plane_h = 25;
            this.plane_x = ThreadLocalRandom.current().nextInt(1, width - this.plane_w);
            this.plane_y = -this.plane_h - 10;
            this.bullet_w = 10;
            this.bullet_h = 10;
            this.bullets.add(new ArrayList<>(Arrays.asList(this.plane_x + this.plane_w / 2 - this.bullet_w / 2, this.plane_y)));
        }

        public void drawPlane(Graphics g) {
            g.setColor(Color.RED);
            g.fillRect(this.plane_x, this.plane_y, this.plane_w, this.plane_h);
        }

        public void drawBullets(Graphics g) {
            g.setColor(Color.BLUE);
            this.bullets.forEach(bullet -> g.fillRect(bullet.get(0), bullet.get(1), this.bullet_w, this.bullet_h));
        }

        public void moveBullets() {
            this.bullets.forEach(bullet -> bullet.set(1, bullet.get(1) + 2));
        }

        public void moveEnemy()
        {
            this.plane_y += 1;
        }

        public void addNewBullets() {
            if(this.bullets.size() == 0)
                this.bullets.add(new ArrayList<>(Arrays.asList(this.plane_x + this.plane_w / 2 - this.bullet_w / 2, this.plane_y + this.plane_w - this.bullet_w)));

            ArrayList<Integer> number = this.bullets.get(this.bullets.size() - 1);
            if(number.get(1) - this.plane_y > ThreadLocalRandom.current().nextInt(220, 250))
            {
                this.bullets.add(new ArrayList<>(Arrays.asList(this.plane_x + this.plane_w / 2 - this.bullet_w / 2, this.plane_y + this.plane_w - this.bullet_w)));
            }
        }

        public void deleteExtraBullets() {
            this.bullets.removeIf(bullet -> bullet.get(1) > height + 10);
        }
    }

    static class Buttons {
        private Color color;
        private final int x, y, w, h;

        public Buttons(Color color, int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.color = color;
        }

        public void drawButton(Graphics g) {
            g.setColor(this.color);
            g.fillRect(this.x, this.y, this.w, this.h);
        }

        public boolean isHover(Point p) {
            if (this.x < p.getX() && this.x+this.w > p.getX()) {
                return this.y < p.getY() && this.y+this.h > p.getY();
            }
            return false;
        }
    }

    private final Buttons play_button = new Buttons(Color.GREEN, 420, 200, 65, 35);
    private final Buttons quit_button = new Buttons(Color.GREEN, 420, 300, 65, 35);
    private final Player player = new Player();
    private final ArrayList<Enemy> enemy = new ArrayList<>();
    private int change_x;
    private int score;
    private boolean reset_all = false;

    public Game() {
        Timer t = new Timer(4, this);
        t.start();
    }

    public void addNewEnemy()
    {
        Enemy e = this.enemy.get(this.enemy.size() - 1);
        if(e.plane_y > ThreadLocalRandom.current().nextInt(100, 150))
        {
            this.enemy.add(new Enemy());
        }
    }

    public void atLeastOneEnemy()
    {
        if(this.enemy.size() == 0)
            this.enemy.add(new Enemy());
    }

    public boolean planeDamage(Enemy e)
    {
        for(ArrayList<Integer> number : e.bullets)
        {
            if((this.player.plane_x < number.get(0) && number.get(0) < this.player.plane_x + this.player.plane_w) && (this.player.plane_y < number.get(1) + e.bullet_w && number.get(1) < this.player.plane_y + this.player.plane_h)) {
                this.player.life -= 1;
                number.set(1, height + 20);
            }

            if((this.player.plane_x < e.plane_x + e.plane_w && e.plane_x < this.player.plane_x + this.player.plane_w) && (this.player.plane_y < e.plane_y + e.plane_h && e.plane_y < this.player.plane_y + this.player.plane_h))
            {
                this.player.life -= 1;
                e.plane_y = height + 20;
            }
        }

        return this.player.life == 0;
    }

    public boolean enemyDamage(Enemy e)
    {
        for(ArrayList<Integer> number : this.player.bullets)
        {
            if((e.plane_x < number.get(0) + this.player.bullet_w && number.get(0) < e.plane_x + e.plane_w) && (e.plane_y < number.get(1) + this.player.bullet_h && number.get(1) < e.plane_y + e.plane_h))
            {
                number.set(1, -50);
                return true;
            }
        }
        return false;
    }

    public void welcome(Graphics g) {
        this.play_button.drawButton(g);
        this.quit_button.drawButton(g);
        this.reset_all = true;
    }

    public void game(Graphics g) {
        this.player.drawPlane(g);
        this.player.drawBullets(g);
        this.player.moveBullets();
        this.player.addNewBullets();
        this.player.deleteExtraBullets();
        this.player.drawHealthBar(g);
        this.atLeastOneEnemy();
        this.addNewEnemy();

        for (Enemy e: enemy) {
            e.drawPlane(g);
            e.drawBullets(g);
            e.moveEnemy();
            e.moveBullets();
            e.addNewBullets();
            e.deleteExtraBullets();

            if(this.planeDamage(e))
                current_mode = Modes.over;

            if (this.enemyDamage(e)) {
                e.plane_y = height + 20;
                this.score += 1;
                System.out.println(score);
            }

        }
    }

    public void gameOver(Graphics g) {
        g.setColor(Color.CYAN);
        g.fillRect(50, 50, 200, 200);
        if(this.reset_all) {
            this.player.reset();
            this.enemy.clear();
            this.score = 0;
            this.reset_all = false;
        }

    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (current_mode == Modes.welcome)
            this.welcome(g);

        if (current_mode == Modes.game)
            this.game(g);

        if (current_mode == Modes.over)
            this.gameOver(g);

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (current_mode == Modes.game) {
            if(this.player.plane_x < 0) {
                this.player.plane_x = 0;
                this.change_x = 0;
            }

            if(this.player.plane_x > width - player.plane_w - 20) {
                this.player.plane_x = width - player.plane_w - 20;
                this.change_x = 0;
            }
            this.player.plane_x += this.change_x;
        }
        repaint();
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        int c = e.getKeyCode();

        if (current_mode == Modes.welcome) {
            if(c == KeyEvent.VK_ENTER)
                current_mode = Modes.game;
        }

        if(current_mode == Modes.game) {
            if (c == KeyEvent.VK_LEFT) this.change_x = -2;
            if (c == KeyEvent.VK_RIGHT) this.change_x = 2;
        }

        if (current_mode == Modes.over) {
            if(c == KeyEvent.VK_ENTER) current_mode = Modes.welcome;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (current_mode == Modes.game) {
            this.change_x = 0;
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) { }

    @Override
    public void mousePressed(MouseEvent e) {
        if(current_mode == Modes.welcome) {
            if (play_button.isHover(new Point(e.getX(), e.getY())))
                current_mode = Modes.game;
            else if(quit_button.isHover(new Point(e.getX(), e.getY())))
                System.exit(0);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) { }

    @Override
    public void mouseEntered(MouseEvent e) { }

    @Override
    public void mouseExited(MouseEvent e) { }


    public static void main(String[] args) {
        Game game = new Game();
        JPanel panel = new JPanel();
        game.setTitle(title);
        game.setBounds(200, 20, width, height);
        game.setResizable(false);
        game.setVisible(true);
        game.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        game.add(panel);
        game.addKeyListener(game);
        game.addMouseListener(game);
    }
}
