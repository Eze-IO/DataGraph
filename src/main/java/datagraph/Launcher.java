package datagraph;

/*
    Entry point for the packaged jar. JavaFX refuses to start
    from a jar whose main class extends Application unless it
    runs on the module path, so this class starts Main instead.
*/
public class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}
