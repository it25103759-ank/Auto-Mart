import java.nio.file.Path;


import static com.automart.AutoMartApplication.*;

abstract class BaseManager {
    protected final Path dataFile;
    BaseManager(Path dataFile) { this.dataFile = dataFile; }
    String fileName() { return dataFile.getFileName().toString(); }
}
