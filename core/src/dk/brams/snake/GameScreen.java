package dk.brams.snake;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.sun.prism.image.ViewPort;

public class GameScreen extends ScreenAdapter{
    private static final float WORLD_WIDTH = 640;
    private static final float WORLD_HEIGHT = 480;
    private static final float MOVE_TIME = 0.5F;
    public static final int GRID_CELL = 32;
    private static final int SNAKE_MOVEMENT=GRID_CELL;
    private static final int RIGHT=0;
    private static final int LEFT=1;
    private static final int UP = 2;
    private static final int DOWN = 3;

    private static final int POINTS_PER_APPLE = 20;
    private static final java.lang.String GAME_OVER_MSG = "Game over... Press space to restart";

    private SpriteBatch batch;
    private Texture snakeHead;
    private Texture snakeBody;
    private Texture apple;

    private float timer = MOVE_TIME;
    private int snakeX=0, snakeY=0;
    private int snakeXbeforeUpdate=0, snakeYbeforeUpdate = 0;
    private int snakeDirection = RIGHT;
    private boolean appleAvailable = false;
    private boolean directionSet = false;
    private int appleX, appleY;
    private int score = 0;

    private Array<BodyPart> bodyParts = new Array<BodyPart>();
    private ShapeRenderer shapeRenderer;

    private enum STATE {PLAYING, GAME_OVER}
    private STATE state=STATE.PLAYING;

    private BitmapFont bitmapFont;
    private GlyphLayout layout = new GlyphLayout();

    private FitViewport viewPort;
    private OrthographicCamera camera;


    @Override
    public void show() {
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(camera.viewportWidth / 2f, camera.viewportHeight / 2f, 0);
        camera.update();

        viewPort = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);

        shapeRenderer = new ShapeRenderer();
        bitmapFont = new BitmapFont();

        batch = new SpriteBatch();
        snakeHead = new Texture(Gdx.files.internal("snakehead.png"));
        apple = new Texture(Gdx.files.internal("apple.png"));
        snakeBody = new Texture(Gdx.files.internal("snakebody.png"));
    }

    @Override
    public void render(float delta) {
        switch(state) {
            case PLAYING:
                queryInput();
                updateSnake(delta);
                checkAppleCollision();
                checkAndPlaceApple();
            break;

            case GAME_OVER:
                checkForRestart();
                break;
        }

        clearScreen();
        drawGrid();
        draw();

        if (state==STATE.GAME_OVER){
            layout.setText(bitmapFont, GAME_OVER_MSG);
            batch.begin();
            bitmapFont.draw(batch, GAME_OVER_MSG, (viewPort.getWorldWidth() - layout.width) / 2,
                    (viewPort.getWorldHeight() - layout.height) / 2);
            batch.end();
        }
    }

    private void draw() {
        batch.setProjectionMatrix(camera.projection);
        batch.setTransformMatrix(camera.view);

        batch.begin();
        batch.draw(snakeHead, snakeX, snakeY);
        for (BodyPart bodyPart : bodyParts) {
            bodyPart.draw(batch);
        }

        if (appleAvailable){
            batch.draw(apple, appleX, appleY);
        }

        drawScore();
        batch.end();
    }


    private void drawGrid(){
        shapeRenderer.setProjectionMatrix(camera.projection);
        shapeRenderer.setTransformMatrix(camera.view);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (int x = 0; x < viewPort.getWorldWidth(); x+=GRID_CELL) {
            for (int y = 0; y < viewPort.getWorldHeight(); y+=GRID_CELL) {
                shapeRenderer.rect(x,y,GRID_CELL, GRID_CELL);
            }
        }
        shapeRenderer.end();
    }


    private void clearScreen() {
        Gdx.gl.glClearColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, Color.BLACK.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    private void checkAppleCollision() {
        if (appleAvailable && appleX==snakeX && appleY==snakeY) {
            BodyPart bodyPart = new BodyPart(snakeBody);
            bodyPart.updateBodyPosition(snakeX, snakeY);
            bodyParts.insert(0, bodyPart);
            addToScore();
            appleAvailable = false;
        }
    }


    private void addToScore() {
        score += POINTS_PER_APPLE;
    }

    private void drawScore() {
        if (state==STATE.PLAYING){
            String scoreAsString = Integer.toString(score);
            layout.setText(bitmapFont, scoreAsString);
            bitmapFont.draw(batch, scoreAsString,
                    (viewPort.getWorldWidth()-layout.width)/2,
                    (4*viewPort.getWorldHeight()/5)-layout.height);
        }
    }

    private void checkSnakeBodyCollision() {
        for (BodyPart bodyPart: bodyParts){
            if (bodyPart.x == snakeX && bodyPart.y == snakeY) {
                state = STATE.GAME_OVER;
                break;
            }
        }
    }


    private void moveSnake() {

        snakeXbeforeUpdate = snakeX;
        snakeYbeforeUpdate = snakeY;

        switch (snakeDirection){
            case RIGHT: {
                snakeX += SNAKE_MOVEMENT;
                return;
            }
            case LEFT: {
                snakeX -= SNAKE_MOVEMENT;
                return;
            }
            case UP:{
                snakeY += SNAKE_MOVEMENT;
                return;
            }
            case DOWN: {
                snakeY -= SNAKE_MOVEMENT;
                return;
            }
        }
    }

    private void updateIfNotOpporsiteDirection(int newSnakeDirection, int opporsiteDirection){
        if (snakeDirection!= opporsiteDirection || bodyParts.size==0)
            snakeDirection = newSnakeDirection;
    }

    private void updateDirection(int newSnakeDirection){
        if (!directionSet && snakeDirection!=newSnakeDirection){
            directionSet = true;
            switch (newSnakeDirection) {
                case LEFT:
                    updateIfNotOpporsiteDirection(newSnakeDirection, RIGHT);
                    break;
                case RIGHT:
                    updateIfNotOpporsiteDirection(newSnakeDirection, LEFT);
                    break;
                case UP:
                    updateIfNotOpporsiteDirection(newSnakeDirection, DOWN);
                    break;
                case DOWN:
                    updateIfNotOpporsiteDirection(newSnakeDirection, UP);
                    break;
            }

        }
    }


    private void updateBodyPartsPosition() {
        if (bodyParts.size>0){
            BodyPart bodyPart = bodyParts.removeIndex(0);
            bodyPart.updateBodyPosition(snakeXbeforeUpdate, snakeYbeforeUpdate);
            bodyParts.add(bodyPart);
        }
    }

    private void updateSnake(float delta){
        if (state==STATE.PLAYING) {
            timer -= delta;
            if (timer<=0){
                timer = MOVE_TIME;
                moveSnake();
                checkForOutOfBounds();
                updateBodyPartsPosition();
                checkSnakeBodyCollision();
                directionSet=false;
            }
        }
    }


    private void checkForOutOfBounds(){
        if (snakeX>=viewPort.getWorldWidth()) {
            snakeX=0;
        }
        if (snakeY>=viewPort.getWorldHeight()) {
            snakeY=0;
        }
        if (snakeX<0){
            snakeX = (int)viewPort.getWorldWidth() - SNAKE_MOVEMENT;
        }
        if (snakeY<0){
            snakeY = (int)viewPort.getWorldHeight() - SNAKE_MOVEMENT;
        }
    }

    private void queryInput() {
        boolean lPressed = Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean rPressed = Gdx.input.isKeyPressed(Input.Keys.RIGHT);
        boolean uPressed = Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean dPressed = Gdx.input.isKeyPressed(Input.Keys.DOWN);

        if (lPressed) updateDirection(LEFT);
        if (rPressed) updateDirection(RIGHT);
        if (uPressed) updateDirection(UP);
        if (dPressed) updateDirection(DOWN);
    }

    private void checkAndPlaceApple() {
        if (!appleAvailable) {
            do {
                appleX = (int)MathUtils.random(viewPort.getWorldWidth() / SNAKE_MOVEMENT - 1) * SNAKE_MOVEMENT;
                appleY = (int)MathUtils.random(viewPort.getWorldHeight() / SNAKE_MOVEMENT - 1) * SNAKE_MOVEMENT;
                appleAvailable = true;
            } while (appleX == snakeX && appleY == snakeY);
        }
    }


    private void checkForRestart() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
            doRestart();
    }

    private void doRestart(){
        state = STATE.PLAYING;
        bodyParts.clear();
        snakeDirection = RIGHT;
        directionSet = false;
        timer = MOVE_TIME;
        snakeX = 0;
        snakeY = 0;
        snakeXbeforeUpdate = 0;
        snakeYbeforeUpdate = 0;
        appleAvailable = false;
        score=0;
    }


    private class BodyPart {

        private int x, y;
        private Texture texture;

        public BodyPart(Texture texture) {
            this.texture = texture;
        }

        public void updateBodyPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void draw(Batch batch) {
            if (!(x == snakeX && y == snakeY)) batch.draw(texture, x, y);
        }
    }
}

