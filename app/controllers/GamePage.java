/**
 * @author      Mariah Flaim
 * @author      Evan Willner
 * @author      Elizabeth Dellea
 * @author      Nikhil Patel
 * @version     1.0
 * @since       2016-03-28
 **/

package controllers;

//import statements

import models.Game;
import models.Message;
import play.data.Form;
import play.db.DB;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.*;
import java.util.Random;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.BooleanSupplier;

import scala.collection.JavaConverters;

public class GamePage extends Controller {

    //TO LOAD NIKHILS HTML PAGES FOR SPRINT 2
    public static Result loadEnemySpotPage() {
        return (ok(enemySpot.render()));
    }

    public static Result loadAddModPage() {
        return (ok(addMod.render()));
    }

    public static Result loadhelpPage() {
        return (ok(helpPage.render()));
    }

    public static Result loadinGameModMessagePage() {
        return (ok(inGameModMessage.render()));
    }

    public static Result loadOMWpage() { return ok(omw.render());}

    public static Result loadmessageHistoryPage() {
        if(session("uname") != null) {
            String type = session("type");
            return getMessages(type);
        }
        return(forbidden(login.render()));
    }

    public static Result loadviewPlayersPage() {
        if (session("is_mod") != null) {
            if ((session("is_mod").equals("true") || session("is_mod").equals("1")) && (session("gCode") != null && !session("gCode").equals(" "))) {
                Map<String, String> playerMap = getPlayers();
                return (ok(viewPlayers.render(playerMap, session("gCode"))));
                // ^^ it gives an error in the ide but the code runs properly!
            }
            Result goTo = loadSettings();
            return(goTo);
        }
        return(forbidden(login.render()));

    }

    /**
     * loads the game page
     *
     * @param - none
     * @return - Result page of games
     */
    public static Result loadPage() {

        //TO DO: should put inGameModSettings into mod settings so moderator can access gamepage and then just do mod stuff from settings
        String uName = session("uname");
        String status = session("is_active");

        if (uName != null) {
            String gCode = session("gCode");
            String isMod = session("is_mod");
            if (!gCode.equals(" ")) {
                return ok(gamePage.render(uName, status));
                //works for regular player and moderator this way
            }
            if ((isMod.equals("1") || isMod.equals("true"))
                    && gCode.equals(" ")) {
                return ok(noGameModSettings.render(uName));
            } else {
                return forbidden(joinGame.render());
            }

        }
        return forbidden(login.render());

    }
    /**
     * load the appropriate pages for settings of the current user
     *
     * @param - none
     * @return - ok(inGameModSettings.render()), ok(noGameModSettings.render()), forbidden(login.render())
     */
    public static Result loadSettings() {
        String isMod = session("is_mod");
        String uName = session("uname");
        String status = session("is_active");
        String team = session("type");
        if (isMod != null) {
            if (status.equals("1"))
                status = "Active";
            if (status.equals("0"))
                status = "Inactive";


            if (isMod.equals("0") || isMod.equals("false")) {

                return ok(regularSettings.render(uName, status, team));
            }
            if (isMod.equals("1") || isMod.equals("true")) {
                //TO DO: return mod settings
                String gCode = session("gCode");

                if (!gCode.equals(" ")) {

                    return ok(inGameModSettings.render(uName, gCode, status, team));
                } else {

                    return ok(noGameModSettings.render(uName));
                }
            }
        }
        return forbidden(login.render());
    }
    /**
     * clears session variables and logs out the current user
     *
     * @param - none
     * @return - ok(login.render()) or forbidden(login.render())
     */
    public static Result logOut() {
        String uName = session("uname");
        if (uName != null) {
            session().clear();
            return ok(login.render());
        } else {
            return forbidden(login.render());
        }

    }
    /**
     * deactivate account of user that is currently logged in
     *
     * @param - none
     * @return - ok () or forbidden(login.render())
     */
    public static Result deactivateAccount() {
        String email = session("email");
        //System.out.println(email);
        if (email != null) {
            session().clear();
            return deleteUser(email);
        } else {
            return forbidden(login.render());
        }
    }

    /**
     * deletes a user from the database with an email
     *
     * @param - email(String)
     * @return - ok()
     */
    public static Result deleteUser(String email) {


        String sql1 = "SET SQL_SAFE_UPDATES = 0";
        String sql2 = "DELETE FROM user WHERE email = '" + email + "'";
        String sql3 = "SET SQL_SAFE_UPDATES = 1";
        java.sql.Connection conn2 = DB.getConnection();
        try {
            //http://stackoverflow.com/questions/18546223/play-framework-execute-raw-sql-at-start-of-request

            java.sql.Statement stmt = conn2.createStatement();
            try {
                //Boolean rst1 = stmt.execute(sql1);
                Boolean rst2 = stmt.execute(sql2);
                //Boolean rst3 = stmt.execute(sql3);
                // rst.close();

            } finally {

                stmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn2.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return ok(home.render());

    }

    //MODERATOR FUNCTIONS

    /**
     * creates a new game in the Game class database
     *
     * @param - none
     * @return - Result redirect to the in Game Mod Setting page or login if not logged in as moderator not in game
     */
    public static Result createGame() {
        //Game game = Form.form(Game.class).bindFromRequest().get();
        // game.save();
        //TO DO : create game and add moderator to it

        if (session("is_mod") != null) {
            if ((session("is_mod").equals("true") || session("is_mod").equals("1")) && (session("gCode") == null || session("gCode").equals(" "))) {
                String gc=" ";
                Boolean isGameCode=true;
                while (isGameCode){
                    gc = gameCode();
                    isGameCode=alreadyGameCode(gc);
                }
                if(!gc.equals(" ")){
                    addGame(gc);
                    session("gCode", gc);
                    JoinGame.verifyCode(gc);
                }
                return loadSettings();
            } else {
                String uName = session("uname");
                String status = session("is_active");
                return forbidden(gamePage.render(uName, status));
            }
        } else {
            return forbidden(login.render());

        }

    }

    public static Boolean alreadyGameCode(String gameCode){
        Boolean isGameCode=false;
        String sql2 = "SELECT * FROM game WHERE game_code='" + gameCode + "'";
        int i = 0;
        java.sql.Connection conn2 = DB.getConnection();
        try {
            //http://stackoverflow.com/questions/18546223/play-framework-execute-raw-sql-at-start-of-request

            java.sql.Statement stmt = conn2.createStatement();
            try {
                ResultSet rst = stmt.executeQuery(sql2);
                try {
                    while (rst.next()) {
                       isGameCode=true;
                    }

                } finally {
                    try {
                        rst.close();
                    } catch (Throwable ignore) { /* Propagate the original exception
instead of this one that you may want just logged */
                    }
                }

                // rst.close();
            } finally {

                stmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn2.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return isGameCode;

    }

    /**
     * removes game from database
     *
     * @param - none
     * @return - Result redirect to the no Game Mod Setting page or login if not logged in as mod in game
     */
    public static Result endGame() {
        if (session("is_mod") != null) {
            if ((session("is_mod").equals("true") || session("is_mod").equals("1")) && (session("gCode") != null && !session("gCode").equals(" "))) {
                removeGame();
                return loadPage();
            } else {
                String uName = session("uname");
                String status = session("is_active");
                return forbidden(gamePage.render(uName, status));
            }
        } else {
            return forbidden(login.render());
        }
    }

    /**
     * gets rid of moderator status of user and reloads page so that game page is not longer mod page
     *
     * @param - none
     * @return - game page
     */
    public static Result verifyChangeModStatus() {
        //TO DO: check if there are anyother moderators in the game. If there arent then you cant change status
        if (session("is_mod") != null) {
            if (session("is_mod").equals("true") || session("is_mod").equals("1")) {
                if (session("gCode").equals(" ")) {
                    //change status right away
                    changeModStatus();
                    return loadPage();
                }
                Boolean isNotOnlyMod = isNotOnlyMod();
                if (isNotOnlyMod) {
                    changeModStatus();
                    return loadPage();
                } else {
                   // System.out.println("ONLY MOD");
                    return forbidden("You are the only moderator, please add another moderator before switching your moderator status.");
                }
            }
            //System.out.println("NOT MOD");
            return forbidden(login.render());
        }
        //System.out.println("NOT LOGGED IN");
        return forbidden(login.render());
    }

    /**
     * checks to see if signed in moderator is not the only moderator in the game
     *
     * @param - none
     * @return - Boolean
     */
    public static Boolean isNotOnlyMod() {
        String sql2 = "SELECT * FROM user WHERE is_mod = 1 and game_code ='" + session("gCode") + "'";
        int i = 0;
        java.sql.Connection conn2 = DB.getConnection();
        try {
            //http://stackoverflow.com/questions/18546223/play-framework-execute-raw-sql-at-start-of-request

            java.sql.Statement stmt = conn2.createStatement();
            try {
                ResultSet rst = stmt.executeQuery(sql2);
                try {
                    while (rst.next()) {
                        i++;
                    }

                } finally {
                    try {
                        rst.close();
                    } catch (Throwable ignore) { /* Propagate the original exception
instead of this one that you may want just logged */
                    }
                }

                // rst.close();
            } finally {

                stmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn2.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (i > 1) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * changes moderator status of mod that is logged in
     *
     * @param - none
     * @return - none
     */
    public static void changeModStatus() {
        String isMod = "false";
        String sql2 = "UPDATE user SET is_mod = " + isMod + " WHERE id = " + session("id");
        java.sql.Connection conn2 = DB.getConnection();
        try {
            //http://stackoverflow.com/questions/18546223/play-framework-execute-raw-sql-at-start-of-request

            java.sql.Statement stmt = conn2.createStatement();
            try {
                Boolean rst = stmt.execute(sql2);
                // rst.close();
            } finally {

                stmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn2.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        //TO DO: Error checking
        session("is_mod", "false");

    }

    /**
     * gets all the players that are in the moderators game from the database
     *
     * @param - none
     * @return - render the view players page with the hashmap of players or login page if player  is not a mod in a game
     */
    public static Map getPlayers() {

                Map<String, String> players = new HashMap<String, String>();
                List<String> names = new LinkedList<String>();
                List<String> teams = new LinkedList<String>();


                String sql2 = "SELECT * FROM user WHERE game_code = '" + session("gCode") + "'";
                java.sql.Connection conn2 = DB.getConnection();
                try {
                    //http://stackoverflow.com/questions/18546223/play-framework-execute-raw-sql-at-start-of-request

                    java.sql.Statement stmt = conn2.createStatement();
                    try {
                        ResultSet rst = stmt.executeQuery(sql2);
                        try {
                            while (rst.next()) {
                                int numColumns = rst.getMetaData().getColumnCount();

                                names.add(rst.getString(2));
                                teams.add(rst.getString(6));

                            }
                                for (int i = 0; i < names.size(); i++) {
                                    players.put(names.get(i), teams.get(i));
                                }

                        } finally {
                            try {
                                rst.close();
                            } catch (Throwable ignore) { /* Propagate the original exception
instead of this one that you may want just logged */
                            }
                        }

                        // rst.close();
                    } finally {

                        stmt.close();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        conn2.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                return players;

    }
    /**
     * Adds mod status to a particular user with a particular email
     *
     * @param - email(String) email of the future moderator
     * @return - render log in if not logged in as mod in name, reloads page with success message
     */
    public static Result addModerator(String email) {
        //TO DO: if you are adding a mod who is already a mod then it should say that

        if (session("is_mod") != null) {
            if ((session("is_mod").equals("true") || session("is_mod").equals("1")) && (session("gCode") != null && !session("gCode").equals(" "))) {
                if (email.equals(session("email"))){
                    return(forbidden("addedYou"));

                }
                //String email = "mflaim1@ithaca.edu";
                String id = "none";
                String game_code="none";
                 String mod="none";

                String sql2 = "SELECT * FROM user WHERE email = '" + email + "' AND game_code = '" + session("gCode") + "'";

                java.sql.Connection conn2 = DB.getConnection();
                try {
                    //http://stackoverflow.com/questions/18546223/play-framework-execute-raw-sql-at-start-of-request

                    java.sql.Statement stmt = conn2.createStatement();
                    try {
                        ResultSet rst = stmt.executeQuery(sql2);
                        try {
                            while (rst.next()) {

                                int numColumns = rst.getMetaData().getColumnCount();
                                id = rst.getString(1);
                                game_code=rst.getString(8);
                                mod=rst.getString(5);

                            }

                        } finally {
                            try {
                                rst.close();
                            } catch (Throwable ignore) { /* Propagate the original exception
instead of this one that you may want just logged */
                            }
                        }

                        // rst.close();
                    } finally {

                        stmt.close();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        conn2.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                if (id != "none") {
                    if(game_code.equals(session("gCode"))&&mod.equals("1")){
                        return forbidden("repMod");
                    }
                    String sql3 = "UPDATE user SET is_mod = " + 1 + " WHERE id = " + Integer.parseInt(id);
                    java.sql.Connection conn3 = DB.getConnection();
                    try {
                        //http://stackoverflow.com/questions/18546223/play-framework-execute-raw-sql-at-start-of-request

                        java.sql.Statement stmt = conn3.createStatement();
                        try {
                            Boolean rst = stmt.execute(sql3);
                            // rst.close();
                        } finally {

                            stmt.close();
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            conn3.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    //TO DO: reload page I guess with message of success and if user is not found the load page with fail message
                    return ok();
                } else {
                    return forbidden("noUinGame");
                }


            }
            return forbidden(login.render());
        }
        return forbidden(login.render());


    }



    /**
     * creates an alphanumeric game code
     * @params - none
     * @return - game code as String
     */
    //TO DO: query database to make sure that no games have that code
    public static String gameCode() {

        Random ran = new Random();
        int top = 7;
        char data = ' ';
        String dat = "";

        for (int i = 0; i <= top; i++) {
            data = (char)(ran.nextInt(25) + 97);
            dat = data + dat;
        }

       // System.out.println(dat);

        return dat;
    }


    public static void clearGamePlayers(String gCode) {

        String sql = "Select * from user WHERE game_code = '" + gCode + "'";


            java.sql.Connection conn = DB.getConnection();
            try {

                java.sql.Statement stmt = conn.createStatement();
                try {
                    ResultSet rst = stmt.executeQuery(sql);
                    try {
                        while (rst.next()) {
                            int id = Integer.parseInt(rst.getString(1));
                            changeType(id, "human");
                            removeGCode(id);
                            int active = Integer.parseInt(rst.getString(7));
                            if(active == 0){
                                changeActiveById(id, active);
                            }

                        }

                        rst.close();
                    } finally {

                        stmt.close();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    /**
     * Called when a user presses the "change team"' button on their settings page
     * @return the appropriate view of the settings page
     */
    public static Result changeTeam(){
        String currTeam = session("type");
        if (currTeam != null) {
            if (currTeam.equals("human")) {
                changeType(Integer.parseInt(session("id")), "zombie");
                session("type", "zombie");
            } else {
                changeType(Integer.parseInt(session("id")), "human");
                session("type", "human");
            }
        }
        Result toGo = loadSettings();
        //loadSettings should handle whatever needs to happen if the user is mod/not or not logged in
        return(toGo);
    }

    /**
     * Chnges player (id) to a give type in the database.
     *
     * @param id   id of the player to change the type of
     * @param type desired type to change to
     */
    public static void changeType(int id, String type) {
        String sql = "UPDATE user SET type = '" + type + "' WHERE id = " + id;
        java.sql.Connection conn = DB.getConnection();
        try {

            java.sql.Statement stmt = conn.createStatement();
            try {
                Boolean rst = stmt.execute(sql);

            } finally {

                stmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sets a player's game code back to ' ' in the database
     *
     * @param id id of the player to change the type of
     */
    public static void removeGCode(int id) {
        String sql = "UPDATE user SET game_code = ' ' WHERE id = " + id;
        java.sql.Connection conn = DB.getConnection();
        try {

            java.sql.Statement stmt = conn.createStatement();
            try {
                Boolean rst = stmt.execute(sql);

            } finally {

                stmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Changes a player's status between active and inactive.
     *
     * @return sends back with success or failure
     */
    public static Result changeActiveStatus() {
        if (session("uname") != null) {
            int id = Integer.parseInt(session("id"));
            int activity = Integer.parseInt(session("is_active"));
            changeActiveById(id, activity);
            if( activity == 0){session("is_active", "1");}
            else{session("is_active", "0");}
        }
        Result toGo = loadSettings();
        return(toGo);
    }

    /**
     * Changes a player of a specific id's active status from the current to its oppposite (active -> inactive; inactive ->a active)
     * @param id id of the player to change the status of
     */
    public static void changeActiveById(int id, int activity){
        String sql = "UPDATE user SET is_active = " + 1 + " WHERE id = " + id;
        if (activity == 1) {
            sql = "UPDATE user SET is_active = " + 0 + " WHERE id = " + id;

        }
        java.sql.Connection conn = DB.getConnection();
        try {

            java.sql.Statement stmt = conn.createStatement();
            try {
                Boolean rst = stmt.execute(sql);

            } finally {

                stmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * Adds a game to the game database
     * @param gameCodeIn(String) the code of the game to add
     * @return - Result HTTP 200 ok status
     */
    public static Result addGame(String gameCodeIn) {

        Game newGame = new Game();

        newGame.gameCode = gameCodeIn;

        newGame.save();
        return ok();
    }


    /**
     * removes a game from the Game database given a game code
     * @return boolen to indicate success or failure
     */
    public static void removeGame() {
        if (session("uname") != null) {
            String code = session("gCode");
            clearGamePlayers(code);
            clearMessages(code);
            session("gCode", " ");
            session("type", "human");

            String sql = "DELETE from game WHERE game_code = '" + code + "'";
            java.sql.Connection conn = DB.getConnection();
            try {

                java.sql.Statement stmt = conn.createStatement();
                try {
                    Boolean rst = stmt.execute(sql);

                } finally {

                    stmt.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }


            //delete from the db where game code is gCode

        }
    }

    public static void clearMessages(String gameCode){
        String sql = "DELETE from message WHERE game_code = '" + gameCode + "'";
        java.sql.Connection conn = DB.getConnection();
        try {

            java.sql.Statement stmt = conn.createStatement();
            try {
                Boolean rst = stmt.execute(sql);

            } finally {

                stmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }


    /**
     * displays a players type
     * @param - none
     * @return - void
     *//*
    public static void getType(){
        User user = User.find.select("type").where().eq("type",type);
        System.out.println(user);

    }*/
    //MESSAGES

    /**
     * Fills all parameters required for sendMessage(). Allows for specific formatting based on source page
     * @param pageFrom The page the message was sent from
     * @param body The body of the message
     * @param location The location the user was at
     * @return 200 (ok) if successful. Forbidden if unsuccessful. Redirects to login if not logged in.
     */
    public static Result makeMessage(String pageFrom, String body, String location){
        String type = session("type");
        if (type !=null){
            Result msgRslt = loadSettings();
            if(pageFrom.equals("modMsg")){
                String body2 = "<p class='mod-msg'>" + body + "</p>";
                msgRslt = sendMessage("all", body2, location);
            }
            else if(pageFrom.equals("enemSpot")){
                body = "<p class='msg-title enem-msg'>Enemy Spotted!!</p>";
                msgRslt = sendMessage(type, body, location);
            }
            else if(pageFrom.equals("help")){
                body = "<p class='msg-title help-msg'>Help Requested!</p>";
                msgRslt = sendMessage(type, body, location);
            }
            else if( pageFrom.equals("omw")){
                body = "<p class='msg-title omw-msg'>On My Way!</p>";
                msgRslt = sendMessage(type, body, location);
            }
            if(msgRslt == ok()){
                return(ok());
            }
            else{
                return msgRslt;
            }
        }
        return(forbidden(login.render()));
    }
    /**
     * Adds a new message to the messages table
     * @param recipient humans/zombies/all --- who this message should go to
     * @param message the body of the message
     * @param location location the message is being sent from. Should be "" if a moderator message.
     * @return 200 http code if successful. Redirects if not logged in.
     */
    public static Result sendMessage(String recipient, String message, String location) {
        String gameCode = session("gCode");
        if (gameCode != null) {
            Message newMessage = new Message();
            newMessage.location = location;
            newMessage.recipient = recipient;
            newMessage.message = message;
            newMessage.gameCode = gameCode;
            newMessage.sender = session("uname");
            newMessage.save();
            return ok();
        }
        return forbidden(login.render());

    }

    public static Result getMessages(String type){

        List<String> times = new LinkedList<String>();
        List<String> senders = new LinkedList<String>();
        List<String> messages = new LinkedList<String>();
        List<String> locations = new LinkedList<String>();

        String recipient=type;
        boolean isMessages=false;
        String sql2 = "SELECT * FROM message WHERE recipient = '"+recipient+"' AND game_code='"+session("gCode")+ "' OR recipient = 'all' AND game_code='"+session("gCode")+"'";
        java.sql.Connection conn2 = DB.getConnection();
        try {
            //http://stackoverflow.com/questions/18546223/play-framework-execute-raw-sql-at-start-of-request

            java.sql.Statement stmt = conn2.createStatement();
            try {
                ResultSet rst = stmt.executeQuery(sql2);
                try {
                    while (rst.next()) {
                        int numColumns = rst.getMetaData().getColumnCount();
                        isMessages=true;
                        /*System.out.println(rst.getString(2));
                        System.out.println(rst.getString(5));
                        System.out.println(rst.getString(6));
                        System.out.println(rst.getString(7));*/
                        times.add(rst.getString(2));
                        messages.add(rst.getString(5));
                        locations.add(rst.getString(6));
                        senders.add(rst.getString(7));
                    }

                } finally {
                    try {
                        rst.close();
                    } catch (Throwable ignore) { /* Propagate the original exception
                            instead of this one that you may want just logged */
                    }
                }

                // rst.close();
            } finally {

                stmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn2.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if(!isMessages){
            return(forbidden("noMessages"));
        }

        return(ok(messageHistory.render(times, messages, locations, senders, session("type"), session("gCode"))));
    }

}


