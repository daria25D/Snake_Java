import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.awt.*;
import java.util.Random;


public class Snake extends JPanel implements Runnable {
    private static final int ROWS = 60;
    private static final int COLUMNS = 60;
    private final int DOT_SIZE = 10;
    private final int WALL = -1;
    private final int INITIAL_SIZE = 4;

    static final Random rand = new Random();

    private int score;
    private static int highScore = 0;
    private static int speedUp = 0;
    private static int Board[][] = new int[ROWS][COLUMNS];
    private volatile List<Point> snake;
    private Food food;

    private volatile boolean leftDir = true;
    private volatile boolean rightDir = false;
    private volatile boolean upDir = false;
    private volatile boolean downDir = false;

    private static boolean addWalls = false;

    private volatile boolean hasMoved = false;
    private volatile boolean gameOver = true;
    private volatile boolean firstGame = true;
    private volatile boolean inMenuScreen = true;

    Thread gameThread;

    public Snake() {
        setPreferredSize(new Dimension(600, 600));
        setBackground(Color.black);
        setFont(new Font("SansSerif", Font.BOLD, 48));
        setFocusable(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (gameOver) {
                    startNewGame();
                    repaint();
                }
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                if ((key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) && !inMenuScreen && !rightDir) {
                    while (!hasMoved && !gameOver) {
                        leftDir = true;
                        upDir = false;
                        downDir = false;
                    }
//                    System.out.println("left");
                }
                if ((key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) && !inMenuScreen && !leftDir) {
                    while (!hasMoved && !gameOver){
                        rightDir = true;
                        upDir = false;
                        downDir = false;
                    }
//                    System.out.println("right");
                }
                if ((key == KeyEvent.VK_UP || key == KeyEvent.VK_W) && !inMenuScreen && !downDir) {
                    while (!hasMoved && !gameOver) {
                        upDir = true;
                        leftDir = false;
                        rightDir = false;
                    }
//                    System.out.println("up");
                }
                if ((key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) && !inMenuScreen && !upDir) {
                    while (!hasMoved && !gameOver) {
                        downDir = true;
                        leftDir = false;
                        rightDir = false;
                    }
//                    System.out.println("down");
                }
                if (key == KeyEvent.VK_SPACE) {
                    speedUp = 25;
                } else {
                    speedUp = 0;
                }
                if (key == KeyEvent.VK_A && inMenuScreen) {
                    addWalls = true;
                }
                if (key == KeyEvent.VK_R && inMenuScreen) {
                    addWalls = false;
                }
                repaint();
            }
        });
    }

    private void startNewGame() {
        gameOver = false;
        firstGame = false;
        leftDir = true;
        rightDir = false;
        upDir = false;
        downDir = false;
        stop();
        initBoard();
        food = new Food();
        if (score > highScore)
            highScore = score;
        score = 0;
        snake = new ArrayList<>();
        for (int i = 0; i < INITIAL_SIZE; i++) {
            snake.add(new Point(COLUMNS / 2 + i, ROWS / 2));
        }
        addFood();
        (gameThread = new Thread(this)).start();
    }

    private void stop() {
        if (gameThread != null) {
            Thread tmp = gameThread;
            gameThread = null;
            tmp.interrupt();
        }
    }

    private void gameOver() {
        gameOver = true;
//        System.out.println("over");
        stop();
    }

    private void initBoard() {
        for (int i = 0; i < ROWS; i++)
            for (int j = 0; j < COLUMNS; j++)
                if (j == 0 || j == COLUMNS - 1 ||
                        i == 0 || i == ROWS - 1)
                    Board[i][j] = WALL;
                else Board[i][j] = 0;
//        System.out.println(addWalls);
        if (addWalls) {
            for (int k = 0; k < 15; k++) {
                int i = rand.nextInt(ROWS - 2) + 1;
                int j = rand.nextInt(COLUMNS - 2) + 1;
                Board[i][j] = WALL;
            }
        }
    }

    /**
     * TODO: array of food + if inside of the walls (snake can't get to the food) then try another spot/add one more
     */
    private void addFood() {
        while (true) {
            int x = rand.nextInt(ROWS - 2) + 1;
            int y = rand.nextInt(COLUMNS - 2) + 1;
            int probability = rand.nextInt(100);
            int t = 0;
            if (probability < 50)
                t = 0; // SMALL - 50% chance
            else if (probability < 80 && probability >= 50)
                t = 1; //MEDIUM - 30% chance
            else if (probability < 100 && probability >= 80)
                t = 2; //LARGE - 20% chance
//            System.out.println(probability);
            Point p = new Point(x, y);
            if (snake.contains(p) || food.getCoord() == p || Board[y][x] == WALL)
                continue;
            if ((Board[y - 1][x] == WALL && Board[y + 1][x] == WALL && Board[y][x - 1] == WALL) ||
                    (Board[y - 1][x] == WALL && Board[y + 1][x] == WALL && Board[y][x + 1] == WALL) ||
                    (Board[y][x - 1] == WALL && Board[y][x + 1] == WALL && Board[y - 1][x] == WALL) ||
                    (Board[y][x - 1] == WALL && Board[y][x + 1] == WALL && Board[y + 1][x] == WALL))
                continue;
            else {
                food.setCoord(p);
                food.setType(Food.Type.values()[t]);
                break;
            }
        }

    }

    private Point nextPointInDir(Point p) {
        Point next = new Point(p);
        if (leftDir) {
            next.x--;
            return next;
        }
        else if (rightDir) {
            next.x++;
            return next;
        }
        else if (upDir) {
            next.y--;
            return next;
        }
        else if (downDir)
            next.y++;
        return next;
    }

    private boolean collision() {
        Point head = snake.get(0);
        Point next = nextPointInDir(head);
        if (snake.contains(next)) {
//            System.out.println(head);
//            System.out.println(next);
//            System.out.println("Snake");
            return true;
        }
        if (Board[next.x][next.y] == WALL) {
//            System.out.println("Wall");
            return true;
        }
        return false;
    }

    private int moveSnake() {
        for (int i = snake.size() - 1; i > 0; i--) {
            Point p1 = snake.get(i - 1);
            Point p2 = snake.get(i);
            p2.move(p1.x, p1.y);
        }
        Point head = snake.get(0);
        if (leftDir) {
            head.x--;
            return 1;
        }
        else if (rightDir) {
            head.x++;
            return 1;
        }
        else if (upDir) {
            head.y--;
            return 1;
        }
        else if (downDir) {
            head.y++;
            return 1;
        }
        return 0;
    }

    private boolean eatFood() {
        Point head = snake.get(0);
        Point next = nextPointInDir(head);
        if (next.equals(food.getCoord())) {
            return true;
        }
        return false;
    }

    private void growSnake() {
        int growCount = food.getType().getNum();
        Point tail = snake.get(snake.size() - 1);
        for (int i = 0; i < growCount; i++) {
            snake.add(new Point(tail));
        }
    }

    @Override
    public void run() {
        while (Thread.currentThread() == gameThread) {
            hasMoved = false;
            try {
                Thread.sleep(Math.max(100 - score - speedUp, 20));
            } catch (InterruptedException e) {
                return;
            }
            if (collision()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }
                gameOver();
            } else {
                if (eatFood()) {
                    score += food.getType().getNum();
                    growSnake();
                    addFood();
                }
                moveSnake();
                hasMoved = true;
            }
            repaint();
        }
    }

    private void drawBoard(Graphics2D g) {
        g.setColor(Color.lightGray);
        for (int i = 0; i < ROWS; i++)
            for (int j = 0; j < COLUMNS; j++)
                if (Board[j][i] == WALL)
                    g.fillRect(j * DOT_SIZE, i * DOT_SIZE, DOT_SIZE, DOT_SIZE);
    }

    private void drawSnake(Graphics2D g) {
        g.setColor(Color.blue);
        Point head = snake.get(0);
        g.fillRect(head.x * DOT_SIZE, head.y * DOT_SIZE, DOT_SIZE, DOT_SIZE);
        g.setColor(Color.red);
        for (Point p : snake) {
            if (p == head) continue;
            g.fillRect(p.x * DOT_SIZE, p.y * DOT_SIZE, DOT_SIZE, DOT_SIZE);
        }
    }

    private void drawFood(Graphics2D g) {
        switch (food.getType().getNum()) {
            case 1:
                g.setColor(Color.green);
                break;
            case 2:
                g.setColor(Color.orange);
                break;
            case 3:
                g.setColor(Color.magenta);
                break;
        }
        g.fillRect(food.getX() * DOT_SIZE, food.getY() * DOT_SIZE, DOT_SIZE, DOT_SIZE);
    }

    private void drawCenteredString(Graphics2D g, String s, int w, int y, Font font, Color color) {
        g.setFont(font);
        g.setColor(color);
        FontMetrics metrics = g.getFontMetrics();
        int x = (w - metrics.stringWidth(s)) / 2;
        g.drawString(s, x, y);
    }

    private void drawStartScreen(Graphics2D g) {
        int width = this.getSize().width;
        drawCenteredString(g, "Snake", width, 260, getFont(), Color.magenta);
        drawCenteredString(g, "Click to start", width, 320, getFont().deriveFont(Font.BOLD, 18), Color.lightGray);
        g.setFont(getFont().deriveFont(Font.BOLD, 14));
        g.setColor(getForeground());
        g.drawString("Press A to add walls", 30, 530);
        g.drawString("Press R to remove walls", 30, 550);
    }

    private void drawGameOverScreen(Graphics2D g) {
        int width = this.getSize().width;
        drawCenteredString(g, "Game over", width, 260, getFont(), Color.red);
        drawCenteredString(g, "Click to restart", width, 320, getFont().deriveFont(Font.BOLD, 18), Color.lightGray);
        StringBuilder s = new StringBuilder();
        StringBuilder hs = new StringBuilder();
        hs.append("high score: ");
        hs.append(highScore);
        s.append("your score: ");
        s.append(score);
        g.setColor(Color.orange);
        drawCenteredString(g, s.toString(), width, 360, getFont().deriveFont(Font.BOLD, 18), Color.orange);
        drawCenteredString(g, hs.toString(), width, 400, getFont().deriveFont(Font.BOLD, 18), Color.orange);
        g.setFont(getFont().deriveFont(Font.BOLD, 14));
        g.setColor(getForeground());
        g.drawString("Press A to add walls", 30, 530);
        g.drawString("Press R to remove walls", 30, 550);
    }

    private void drawScore(Graphics2D g) {
        int h = getHeight();
        g.setFont(getFont().deriveFont(Font.BOLD, 16));
        g.setColor(getForeground());
        StringBuilder s = new StringBuilder();
        s.append("high score: ");
        s.append(highScore);
        s.append(" your score: ");
        s.append(score);
        g.drawString(s.toString(), 30, h - 30);
    }

    @Override
    public void paintComponent(Graphics G) {
        super.paintComponent(G);
        Graphics2D g = (Graphics2D) G;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawBoard(g);
        if (gameOver && firstGame) {
            inMenuScreen = true;
            drawStartScreen(g);
        } else if (gameOver && !firstGame) {
            inMenuScreen = true;
            drawGameOverScreen(g);
        } else {
            inMenuScreen = false;
            drawSnake(g);
            drawFood(g);
            drawScore(g);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame();
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setTitle("Snake");
            f.setResizable(false);
            f.add(new Snake(), BorderLayout.CENTER);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

}
