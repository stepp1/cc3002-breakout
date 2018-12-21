package gui;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.input.Input;
import com.almasb.fxgl.input.UserAction;
import com.almasb.fxgl.physics.CollisionHandler;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.almasb.fxgl.settings.GameSettings;
import com.almasb.fxgl.util.Consumer;
import controller.Game;
import facade.HomeworkTwoFacade;
import gui.control.BrickComponent;
import javafx.geometry.Point2D;
import javafx.scene.input.KeyCode;
import logic.brick.Brick;
import logic.brick.GlassBrick;
import logic.brick.MetalBrick;
import logic.brick.WoodenBrick;
import logic.level.Level;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import static gui.BreakoutEntityFactory.*;

/**
 * Main class for handling game gui and interactions of a Breakout Game
 *
 * @author Victor Faraggi
 *
 * @see GameApplication
 * @see HomeworkTwoFacade
 */

public class BreakoutGameApp extends GameApplication {


    public enum Type {
        PLAYER,
        BALL,
        WALL,
        BRICK
    }


    //  Instancing reusable objects

    /**
     * Instance of main {@link Game} controller.
     * <p>
     * Keeps score and ball counters, along with individual Hittable behaviour.
     */
    private HomeworkTwoFacade breakout = new HomeworkTwoFacade();

    /**
     * Original x coordinate of the Player
     */
    private final int playerX = 350;

    /**
     * Original y coordinate of the Player
     */
    private final int playerY = 770;

    /**
     * Height of the game window
     */
    private final int gameSizeX = 800;

    /**
     * Width of the game window
     */
    private final int gameSizeY = 800;

    /**
     * For muting game audio.
     */
    private boolean mute = false;

    /**
     * Background music for the game.
     *
     * Loaded from resources/assets/music/
     */
    // private Music background_song = getAssetLoader().loadMusic("hansatom_-_Drugs_of_Choice_2.mp3");

    /**
     * Random Number Generator
      */
    private Random random_number_generator = new Random();


    /**
     * Stores a boolean that indicates if the ball has been launched
     */
    private boolean launched = false;

    //  Overridden methods
    @Override
    protected void initSettings(GameSettings gameSettings) {
        gameSettings.setWidth(gameSizeX);
        gameSettings.setHeight(gameSizeY);
        gameSettings.setTitle("Breakout Game App");
        gameSettings.setVersion("0.1");
    }

    @Override
    protected void initGame() {

        breakout = new HomeworkTwoFacade();

        List<Entity> entities = new LinkedList<>();

        entities.add(newBackground());
        entities.add(newBall(350, 759));
        entities.add(newWalls());
        entities.add(newPlayer(playerX, playerY));

        for(Entity e : entities)
            getGameWorld().addEntity(e);

        // Doesn't play music u.u
        getAudioPlayer().setGlobalMusicVolume(0.6);
        getAudioPlayer().playMusic("hansatom_-_Drugs_of_Choice_2.mp3");

    }


    protected void initInput() {
        Input input = getInput();

        input.addAction(MoveRightAction, KeyCode.D);

        input.addAction(MoveLeftAction, KeyCode.A);

        input.addAction(LaunchBall, KeyCode.SPACE);

        input.addAction(MakeLevel, KeyCode.N);
    }


    @Override
    protected void initPhysics() {
        getPhysicsWorld().setGravity(0, 0);
        getPhysicsWorld().addCollisionHandler(
                new CollisionHandler(Type.BALL, Type.WALL) {
                    @Override
                    protected void onHitBoxTrigger(Entity ball, Entity wall,
                                                   HitBox boxBall, HitBox boxWall) {

                        if (boxWall.getName().equals("BOT")) {
                            ball.removeFromWorld();
                            breakout.dropBall();
                            launched = false;

                            if(!breakout.isGameOver()) {
                                newLife();
                            }
                            else{
                                getGameWorld().clear();
                                getGameWorld().addEntity(newBackground());
                                getDisplay().showConfirmationBox("Quieres jugar de nuevo?",  check);
                            }
                        }
                    }

                    private void newLife() {
                        Entity player = getGameWorld().getEntitiesByType(Type.PLAYER).get(0);
                        getGameWorld().addEntity(newBall(player.getX(), player.getY() - 20));
                    }
                }
        );


        getPhysicsWorld().addCollisionHandler(
                new CollisionHandler(Type.BALL, Type.BRICK) {
                    @Override
                    protected void onCollision(Entity ball, Entity brick) {
                        Level current = breakout.getCurrentLevel();
                        brick.getComponent(BrickComponent.class).getBrick().hit();
                        Level isCurrent = breakout.getCurrentLevel();

                        if(brick.getComponent(BrickComponent.class).getBrick().isDestroyed()){
                            getAudioPlayer().playSound("brick_hit.wav");
                            getGameWorld().removeEntity(brick);
                        }

                        if(!current.equals(isCurrent)){
                            getGameWorld().getEntitiesByType(Type.BRICK)
                                    .forEach(e -> getGameWorld().removeEntity(e));
                            setBricks(isCurrent);
                        }
                    }
                }
        );
    }


    // Consumer to check if user wants to keep playing

    private Consumer<Boolean> check = (x) ->{
        if(x){
            getGameWorld().clear();
            initGame();
        }
    };


    // Function to start a new Game

    private void startGame(boolean started){
        Level lvl = breakout.newLevelWithBricksFull("level", 30, 0.5, 0.34, 0);
        if(started){
            breakout.addPlayingLevel(lvl);
            return;
        }
        breakout.setCurrentLevel(lvl);
        setBricks(breakout.getCurrentLevel());
    }


    //Helper function

    private void setBricks(Level lvl){
        List<Entity> entities = new LinkedList<>();

        int posX = 25;
        int posY = 30;

        Collections.shuffle(lvl.getBricks());
        for(Brick brick : lvl.getBricks()){
            if(brick.getClass() == GlassBrick.class){
                entities.add(newGlassBrick(posX, posY, (GlassBrick) brick));
            }
            else if(brick.getClass() == WoodenBrick.class){
                entities.add(newWoodenBrick(posX, posY, (WoodenBrick) brick));
            }
            else{
                entities.add(newMetalBrick(posX, posY, (MetalBrick) brick));
            }

            if(posX > 800 - playerX/2 + 50){
                posX = 25;
                posY += 20;
            }
            else {
                posX += 76;
            }
        }

        for (Entity e : entities) {
            getGameWorld().addEntity(e);
        }
    }

    public static void main(String... args) {
        launch(args);
    }


    // Actions

    private UserAction MakeLevel = new UserAction("Make a new Level") {
        @Override
        protected void onAction() {
            if(breakout.hasCurrentLevel()){
                startGame(true);
            }
            else{
                //crear nivel desde cero
                startGame(false);
            }
        }
    };

    private UserAction MoveRightAction = new UserAction("Move Right") {
        @Override
        protected void onAction() {
            AtomicBoolean moves = new AtomicBoolean(true);
            getGameWorld().getEntitiesByType(Type.PLAYER)
                    .forEach(e -> e.getComponent(PhysicsComponent.class).reposition(new Point2D(e.getX() + 5, e.getY()))); // move right 5 pixels to the right
            getGameWorld().getEntitiesByType(Type.PLAYER)
                    .forEach(e -> moves.set(verifyPositionRight(e)));

            if (!launched && moves.get()){
                getGameWorld().getEntitiesByType(Type.BALL)
                        .forEach(e -> e.getComponent(PhysicsComponent.class).reposition(new Point2D(e.getX() + 5, e.getY()))); // move right 5 pixels to the right
            }
        }

        private boolean verifyPositionRight(Entity e) {
            if (e.getX() + playerWidth >= gameSizeX) {
                e.getComponent(PhysicsComponent.class).reposition(new Point2D(gameSizeX - playerWidth, playerY));
                return false;
            }
            else{
                return true;
            }
        }
    };

    private UserAction MoveLeftAction = new UserAction("Move Left") {
        @Override
        protected void onAction() {
            AtomicBoolean moves = new AtomicBoolean(true);
            getGameWorld().getEntitiesByType(Type.PLAYER)
                    .forEach(e -> e.getComponent(PhysicsComponent.class).reposition(new Point2D(e.getX() - 5, e.getY()))); // move left 5 pixels to the left
            getGameWorld().getEntitiesByType(Type.PLAYER)
                    .forEach(e -> moves.set(verifyPositionLeft(e)));
            if (!launched && moves.get()){
                getGameWorld().getEntitiesByType(Type.BALL)
                        .forEach(e -> e.getComponent(PhysicsComponent.class).reposition(new Point2D(e.getX() - 5, e.getY()))); // move right 5 pixels to the left

            }
        }

        private boolean verifyPositionLeft(Entity e) {
            if (e.getX() <= 0) {
                e.getComponent(PhysicsComponent.class).reposition(new Point2D(0, playerY));
                return false;
            }
            else {
                return true;
            }
        }
    };

    private UserAction LaunchBall = new UserAction("Launch Ball") {
        @Override
        protected void onActionBegin() {
                getGameWorld().getEntitiesByType(Type.BALL).forEach(this::ballLauncher);
                launched = true;
        }

        private void ballLauncher(Entity e) {
            int X = random_number_generator.nextInt(50) + 250;
            int Y = random_number_generator.nextInt(50) + 250;

            e.getComponent(PhysicsComponent.class).setLinearVelocity(X, Y);

        }
    };
}

