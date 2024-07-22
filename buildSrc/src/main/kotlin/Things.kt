import java.nio.file.Path
import org.gradle.api.file.FileSystemLocation

val FileSystemLocation.asPath: Path
    get() = asFile.toPath()