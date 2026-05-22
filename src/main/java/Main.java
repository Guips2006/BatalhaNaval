import persistence.DatabaseManager;
import persistence.MatchRepository;
import persistence.MoveRepository;
import persistence.PlayerRepository;
import ui.TerminalUI;
import java.util.Scanner;
import config.GameConfig;
import domain.Game;

public class Main {
    public static void main(String[] args) throws Exception {
        GameConfig config = GameConfig.load("game.properties");

        try (DatabaseManager db = new DatabaseManager(config)) {
            db.init();

            TerminalUI        ui         = new TerminalUI(config);
            MatchRepository   matchRepo  = new MatchRepository(db);
            PlayerRepository  playerRepo = new PlayerRepository(db);
            MoveRepository    moveRepo   = new MoveRepository(db);
            Scanner           scanner    = new Scanner(System.in);

            Game game = new Game(config, ui, scanner);

            long matchId = -1;
            if (db.isReady()) {
                matchId = matchRepo.insert(config, "Jogador");
                playerRepo.insert(matchId, "Jogador", "HUMAN");
                playerRepo.insert(matchId, "CPU",     "CPU");
            }

            game.run();

            if (db.isReady() && matchId > 0) {
                db.beginTransaction();
                moveRepo.insertBatch(matchId, game.getMoveEntries());
                matchRepo.finish(matchId, game.getWinner());
                db.commit();
            }
        }
    }
}